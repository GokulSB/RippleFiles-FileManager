package com.ripple.filemanager.localsend

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.ripple.filemanager.MainActivity
import com.ripple.filemanager.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.File

/**
 * Foreground service for LocalSend file transfer and background device discovery.
 * Ensures the device remains discoverable, maintains Wi-Fi multicast lock,
 * and keeps HTTP/HTTPS transfer server alive with a low-priority notification.
 */
class NearbyShareService : Service() {

    companion object {
        private const val TAG = "NearbyShareService"
        const val CHANNEL_ID = "nearby_share_channel"
        const val NOTIFICATION_ID = 2001
        private const val IDLE_TIMEOUT_MS = 15 * 60 * 1000L // 15 minutes idle timeout

        const val ACTION_START = "com.ripple.filemanager.localsend.ACTION_START"
        const val ACTION_STOP = "com.ripple.filemanager.localsend.ACTION_STOP"

        fun start(context: Context) {
            try {
                val intent = Intent(context, NearbyShareService::class.java).apply {
                    action = ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start NearbyShareService", e)
            }
        }

        fun stop(context: Context) {
            try {
                // Don't start a service just to stop it
                context.stopService(Intent(context, NearbyShareService::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop NearbyShareService", e)
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val handler = Handler(Looper.getMainLooper())
    private val idleStop = Runnable { stopSelf() }
    private var foregroundOk = false
    private var lastNotifyMs = 0L

    private var wakeLock: PowerManager.WakeLock? = null
    private var discovery: LocalSendDiscovery? = null
    private var server: LocalSendServer? = null
    private var isEngineRunning = false
    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        foregroundOk = startInForeground() // first thing, before any other work
        if (!foregroundOk) {
            stopSelf()
            return
        }

        // Acquire partial wake lock
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "RippleFiles:NearbyShareWakeLock"
            ).apply {
                setReferenceCounted(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create wake lock", e)
        }

        scheduleIdleStop()
        serviceScope.launch(Dispatchers.IO) { initializeEngine() } // off the main thread
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!foregroundOk || intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        when (intent.action) {
            ACTION_STOP -> {
                isEngineRunning = false
                NearbyShareRepository.setReceiving(false)
                stopSelf()
            }
            ACTION_START -> {
                scheduleIdleStop()
                if (server != null && server?.boundPort == 53317) {
                    Log.i(TAG, "Rebinding server from legacy port 53317 to ${LocalSendServer.DEFAULT_HTTP_PORT}")
                    serviceScope.launch(Dispatchers.IO) {
                        server?.stopServer()
                        discovery?.stop()
                        initializeEngine()
                    }
                } else if (isEngineRunning) {
                    NearbyShareRepository.setReceiving(true)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startInForeground(): Boolean = try {
        val notification = buildNotification("Ready to send and receive files", isOngoing = false)
        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            fgsType
        )
        true
    } catch (e: Exception) { // ForegroundServiceStartNotAllowedException, SecurityException
        Log.w(TAG, "Can't enter foreground", e)
        false
    }

    // Android 15+: required for time-limited types. Must stop within a few seconds.
    override fun onTimeout(startId: Int, fgsType: Int) {
        Log.w(TAG, "FGS timeout, type=$fgsType")
        NearbyShareRepository.emitError("Nearby Share stopped (system time limit)")
        stopSelf()
    }

    // Call on every transfer event (request, complete, cancel). Also bounds the wake lock.
    private fun scheduleIdleStop() {
        handler.removeCallbacks(idleStop)
        handler.postDelayed(idleStop, IDLE_TIMEOUT_MS)
        try {
            wakeLock?.acquire(IDLE_TIMEOUT_MS) // non-ref-counted: re-acquire extends it
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extend wake lock", e)
        }
    }

    private fun initializeEngine() {
        val httpClient = OkHttpClient.Builder()
            .apply { LocalSendSecurity.configureOkHttpClient(this) }
            .build()

        val disc = LocalSendDiscovery(
            context = applicationContext,
            httpClient = httpClient
        )
        discovery = disc
        NearbyShareRepository.setDiscovery(disc)

        val srv = LocalSendServer(
            context = applicationContext,
            discovery = disc,
            onIncomingTransferRequest = { request ->
                scheduleIdleStop()
                NearbyShareRepository.onIncomingRequest(request)
                updateNotification("Incoming transfer from ${request.sender.alias}", isOngoing = true)
            },
            onProgressUpdate = { sessionId, fileId, bytesRead, totalBytes ->
                scheduleIdleStop()
                NearbyShareRepository.onIncomingProgress(sessionId, fileId, bytesRead, totalBytes)
                val percent = if (totalBytes > 0) (bytesRead * 100 / totalBytes).toInt() else 0
                updateNotification("Receiving files ($percent%)", isOngoing = true, force = false)
            },
            onTransferComplete = { sessionId, peer, files ->
                scheduleIdleStop()
                NearbyShareRepository.onIncomingComplete(sessionId, peer, files)
                updateNotification("Received ${files.size} file(s) from ${peer.alias}", isOngoing = false)
            },
            onTransferCancelled = { sessionId ->
                scheduleIdleStop()
                NearbyShareRepository.onIncomingCancelled(sessionId)
                updateNotification("Transfer cancelled", isOngoing = false)
            }
        )
        server = srv

        val startResult = srv.startServer()
        if (startResult.isFailure) {
            Log.e(TAG, "Failed to start LocalSend server on default and ephemeral ports", startResult.exceptionOrNull())
            isEngineRunning = false
            NearbyShareRepository.emitError("Couldn't start Receive — check your network connection")
            NearbyShareRepository.setReceiving(false)
            stopSelf()
            return
        }

        val actualPort = startResult.getOrThrow()
        disc.localPort = actualPort
        disc.start()

        NearbyShareRepository.setServer(srv)
        isEngineRunning = true
        NearbyShareRepository.setReceiving(true)
        disc.sendAnnouncement(announce = true)
    }

    private fun updateNotification(status: String, isOngoing: Boolean, force: Boolean = true) {
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastNotifyMs < 1000) return
        lastNotifyMs = now
        try {
            val notification = buildNotification(status, isOngoing)
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update notification", e)
        }
    }

    private fun buildNotification(status: String, isOngoing: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val stopIntent = Intent(this, NearbyShareService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val appName = getSharedPreferences("sift_prefs", MODE_PRIVATE)
            .getString("custom_app_name", "Ripple Files")?.takeIf { it.isNotBlank() } ?: "Ripple Files"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("$appName Nearby Share")
            .setContentText(status)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(isOngoing)
            .setContentIntent(openAppPendingIntent)
            .addAction(0, "Stop", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nearby Share",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Nearby Share discovery and file transfer active"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(idleStop)
        serviceScope.cancel()
        isEngineRunning = false

        val disc = discovery
        val srv = server
        discovery = null
        server = null
        NearbyShareRepository.setDiscovery(null)
        NearbyShareRepository.setServer(null)
        NearbyShareRepository.setReceiving(false)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                disc?.stop()
                srv?.stopServer()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping server or discovery", e)
            }
        }

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wake lock", e)
        }
        wakeLock = null
    }
}
