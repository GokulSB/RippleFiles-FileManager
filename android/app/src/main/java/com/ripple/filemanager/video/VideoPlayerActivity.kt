package com.ripple.filemanager.video

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ripple.filemanager.ui.expressive.*
import com.ripple.filemanager.video.engine.*
import com.ripple.filemanager.video.subtitles.SidecarSubtitle
import com.ripple.filemanager.video.subtitles.SubtitleManager
import com.ripple.filemanager.video.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.videolan.libvlc.util.VLCVideoLayout
import java.io.File
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

enum class DragDirection {
    NONE, HORIZONTAL, VERTICAL
}

class VideoPlayerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_VIDEO_PATH = "extra_video_path"
        const val EXTRA_VIDEO_URI = "extra_video_uri"
        const val EXTRA_VIDEO_TITLE = "extra_video_title"
        const val EXTRA_PLAYLIST_PATHS = "extra_playlist_paths"
        const val EXTRA_PLAYLIST_INDEX = "extra_playlist_index"
        const val ACTION_PIP_PLAY_PAUSE = "com.ripple.filemanager.video.PIP_PLAY_PAUSE"
        private const val TAG = "VideoPlayerActivity"
    }

    private lateinit var prefs: VideoPlayerPrefs
    private lateinit var subtitleManager: SubtitleManager
    private var engine: LibVlcPlayerEngine? = null

    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false

    private var playlist = mutableListOf<String>()
    private var playlistIndex = 0
    private var currentFileKey = ""
    private var isInPipMode = false
    private var wasInPipMode = false
    private var isEnteringPip = false

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                engine?.pause()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Allow rendering into cutout area
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        // Configure system bars based on initial orientation
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        updateSystemBars(isLandscape)

        prefs = VideoPlayerPrefs(this)
        subtitleManager = SubtitleManager(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        // Initialize video engine
        engine = LibVlcPlayerEngine(this, subtitleManager.getSubtitleStyle())

        // Register noisy receiver
        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(noisyReceiver, filter)

        // Handle Back navigation: cleanly finish and exit player
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        // Read orientation preference
        applyOrientationPreference(prefs.defaultOrientation)

        // Parse Intent
        parseIncomingIntent(intent)

        setContent {
            CompositionLocalProvider(
                LocalExpressiveColors provides DarkExpressiveColorScheme
            ) {
                VideoPlayerScreen()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        updateSystemBars(isLandscape)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        isInPipMode = false
        wasInPipMode = false
        isEnteringPip = false
        parseIncomingIntent(intent)
        loadCurrentVideo()
    }

    private fun updateSystemBars(isLandscape: Boolean) {
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (isLandscape) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            // In portrait, show status bar with transparent background and light/white text & icons
            insetsController.show(WindowInsetsCompat.Type.statusBars())
            insetsController.hide(WindowInsetsCompat.Type.navigationBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.isAppearanceLightStatusBars = false
        }
    }

    private fun getWindowBrightness(): Float {
        val lp = window.attributes
        return if (lp != null && lp.screenBrightness >= 0f) {
            lp.screenBrightness
        } else {
            try {
                val sys = Settings.System.getInt(
                    contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS
                )
                (sys / 255f).coerceIn(0.01f, 1f)
            } catch (_: Exception) {
                0.5f
            }
        }
    }

    private fun setWindowBrightness(brightness: Float) {
        val lp = window.attributes ?: return
        lp.screenBrightness = brightness.coerceIn(0.01f, 1.0f)
        window.attributes = lp
    }

    private fun getVolumeFraction(): Float {
        val am = audioManager ?: return 0.5f
        val maxV = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (maxV <= 0) return 0f
        val currV = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        return (currV.toFloat() / maxV.toFloat()).coerceIn(0f, 1f)
    }

    private fun setVolumeFraction(fraction: Float): Float {
        val am = audioManager ?: return fraction
        val maxV = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (maxV <= 0) return 0f
        val targetV = (fraction * maxV).roundToInt().coerceIn(0, maxV)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, targetV, 0)
        return targetV.toFloat() / maxV.toFloat()
    }

    private fun applyOrientationPreference(pref: String) {
        requestedOrientation = when (pref.lowercase()) {
            "landscape" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            "portrait" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "sensor" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            else -> ActivityInfo.SCREEN_ORIENTATION_USER
        }
    }

    private fun parseIncomingIntent(intent: Intent?) {
        if (intent == null) return

        playlist.clear()
        val extraPlaylist = intent.getStringArrayListExtra(EXTRA_PLAYLIST_PATHS)
        if (!extraPlaylist.isNullOrEmpty()) {
            playlist.addAll(extraPlaylist)
            playlistIndex = intent.getIntExtra(EXTRA_PLAYLIST_INDEX, 0).coerceIn(0, playlist.size - 1)
        } else {
            val path = intent.getStringExtra(EXTRA_VIDEO_PATH)
            if (!path.isNullOrBlank()) {
                playlist.add(path)
                playlistIndex = 0
                // Auto-detect sibling video files in same directory to form folder playlist
                try {
                    val file = File(path)
                    val parent = file.parentFile
                    if (parent != null && parent.isDirectory) {
                        val videoExts = setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "ts", "m2ts", "3gp", "ogv", "vob")
                        val siblings = parent.listFiles { f -> f.isFile && f.extension.lowercase() in videoExts }
                            ?.sortedBy { it.name.lowercase() }
                            ?.map { it.absolutePath }
                        if (!siblings.isNullOrEmpty() && siblings.contains(path)) {
                            playlist.clear()
                            playlist.addAll(siblings)
                            playlistIndex = playlist.indexOf(path).coerceAtLeast(0)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error building folder playlist", e)
                }
            } else if (intent.data != null) {
                // ACTION_VIEW from external app or SAF
                val uri = intent.data!!
                val resolvedPath = if (uri.scheme == "file") uri.path else null
                if (resolvedPath != null && File(resolvedPath).exists()) {
                    playlist.add(resolvedPath)
                } else {
                    playlist.add(uri.toString())
                }
                playlistIndex = 0
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) return true
        val am = audioManager ?: return true

        hasAudioFocus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .build()
                )
                .setOnAudioFocusChangeListener { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS -> {
                            engine?.pause()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode) {
                                finish()
                            }
                        }
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> engine?.pause()
                        AudioManager.AUDIOFOCUS_GAIN -> engine?.play()
                    }
                }
                .build()
            audioFocusRequest = req
            am.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS -> {
                            engine?.pause()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode) {
                                finish()
                            }
                        }
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> engine?.pause()
                        AudioManager.AUDIOFOCUS_GAIN -> engine?.play()
                    }
                },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        return hasAudioFocus
    }

    private fun abandonAudioFocus() {
        if (!hasAudioFocus) return
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(null)
        }
        hasAudioFocus = false
    }

    fun togglePlayPause() {
        val eng = engine ?: return
        val isPlaying = eng.isPlaying || eng.playbackState.value is PlaybackState.Playing
        if (isPlaying) {
            eng.pause()
        } else {
            requestAudioFocus()
            eng.play()
        }
    }

    private fun updatePipParams() {
        if (!prefs.pictureInPicture) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                return
            }
            try {
                val eng = engine
                val w = (eng?.videoWidth?.value ?: 16).coerceAtLeast(1)
                val h = (eng?.videoHeight?.value ?: 9).coerceAtLeast(1)
                val floatRatio = w.toFloat() / h.toFloat()
                val safeRatio = when {
                    floatRatio < 0.45f -> Rational(9, 20)
                    floatRatio > 2.2f -> Rational(22, 10)
                    else -> Rational(w, h)
                }

                val builder = PictureInPictureParams.Builder()
                    .setAspectRatio(safeRatio)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    builder.setAutoEnterEnabled(true)
                }

                setPictureInPictureParams(builder.build())
            } catch (e: Exception) {
                Log.e(TAG, "Error updating PiP params", e)
            }
        }
    }

    private fun enterPip(): Boolean {
        if (!prefs.pictureInPicture) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                return false
            }
            try {
                isEnteringPip = true
                val eng = engine
                val w = (eng?.videoWidth?.value ?: 16).coerceAtLeast(1)
                val h = (eng?.videoHeight?.value ?: 9).coerceAtLeast(1)
                val floatRatio = w.toFloat() / h.toFloat()
                val safeRatio = when {
                    floatRatio < 0.45f -> Rational(9, 20)
                    floatRatio > 2.2f -> Rational(22, 10)
                    else -> Rational(w, h)
                }

                val builder = PictureInPictureParams.Builder()
                    .setAspectRatio(safeRatio)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    builder.setAutoEnterEnabled(true)
                }

                val result = enterPictureInPictureMode(builder.build())
                if (!result) {
                    isEnteringPip = false
                }
                return result
            } catch (e: Exception) {
                Log.e(TAG, "Error entering PiP", e)
                isEnteringPip = false
                return false
            }
        }
        return false
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (prefs.pictureInPicture && !isInPipMode) {
            enterPip()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
        isEnteringPip = false
        if (isInPictureInPictureMode) {
            wasInPipMode = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isInPipMode) {
            wasInPipMode = false
        }
        updatePipParams()
    }

    override fun onPause() {
        super.onPause()
        val eng = engine
        if (eng != null && currentFileKey.isNotBlank()) {
            prefs.savePosition(currentFileKey, eng.currentPosition.value, eng.duration.value)
        }
    }

    override fun onStop() {
        super.onStop()
        val currentlyInPip = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode) || isEnteringPip || isInPipMode
        if (currentlyInPip) {
            return
        }

        if (wasInPipMode && !isInPipMode) {
            // User closed the PiP window ('X' clicked on PiP overlay)
            finish()
            return
        }

        if (isFinishing) {
            engine?.detachVideoLayout()
        }

        if (!prefs.backgroundPlay) {
            engine?.pause()
            abandonAudioFocus()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(noisyReceiver)
        } catch (_: Exception) {}

        abandonAudioFocus()
        engine?.release()
        engine = null
    }

    private fun loadCurrentVideo() {
        if (playlist.isEmpty()) return
        val currentItem = playlist[playlistIndex]
        currentFileKey = currentItem

        val (uri, title, path) = resolveVideoSource(currentItem)
        val savedPos = prefs.getSavedPosition(currentFileKey)
        val style = subtitleManager.getSubtitleStyle()
        val encoding = subtitleManager.getDefaultEncoding()

        val options = VideoPlayerOptions(
            hwAcceleration = prefs.hwAcceleration,
            startPositionMs = savedPos,
            speed = prefs.defaultSpeed,
            subtitleEncoding = encoding
        )

        requestAudioFocus()
        engine?.open(VideoSource(uri = uri, title = title, path = path), options)
        updatePipParams()

        // Check for preferred subtitle language after short delay
        val preferredLang = subtitleManager.getPreferredSubtitleLanguage()
        if (preferredLang.isNotBlank()) {
            // will match embedded or sidecar subtitle
        }
    }

    private fun resolveVideoSource(raw: String): Triple<Uri, String, String?> {
        return if (raw.startsWith("content://")) {
            val uri = Uri.parse(raw)
            var name: String? = null
            try {
                contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) name = cursor.getString(idx)
                    }
                }
            } catch (_: Exception) {}
            Triple(uri, name ?: uri.lastPathSegment ?: "Video", null)
        } else if (raw.startsWith("file://")) {
            val uri = Uri.parse(raw)
            val path = uri.path
            val name = if (path != null) File(path).name else "Video"
            Triple(uri, name, path)
        } else {
            val file = File(raw)
            val uri = Uri.fromFile(file)
            Triple(uri, file.name, file.absolutePath)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun VideoPlayerScreen() {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        val playerEngine = engine ?: remember {
            LibVlcPlayerEngine(context, subtitleManager.getSubtitleStyle()).also { engine = it }
        }

        LaunchedEffect(Unit) {
            loadCurrentVideo()
        }

        // State flows from engine
        val playbackState by playerEngine.playbackState.collectAsState()
        val currentPosition by playerEngine.currentPosition.collectAsState()
        val duration by playerEngine.duration.collectAsState()
        val audioTracks by playerEngine.audioTracks.collectAsState()
        val subtitleTracks by playerEngine.subtitleTracks.collectAsState()
        val selectedAudioTrackId by playerEngine.selectedAudioTrackId.collectAsState()
        val selectedSubtitleTrackId by playerEngine.selectedSubtitleTrackId.collectAsState()
        val speed by playerEngine.speed.collectAsState()
        val aspectRatio by playerEngine.aspectRatio.collectAsState()
        val videoWidth by playerEngine.videoWidth.collectAsState()
        val videoHeight by playerEngine.videoHeight.collectAsState()

        LaunchedEffect(videoWidth, videoHeight) {
            if (videoWidth > 0 && videoHeight > 0) {
                updatePipParams()
            }
        }

        // UI state
        var isControlsVisible by remember { mutableStateOf(true) }
        var isControlsLocked by remember { mutableStateOf(false) }
        var isRotationLocked by remember { mutableStateOf(false) }

        // Sheets state
        var showAudioSheet by remember { mutableStateOf(false) }
        var showSubtitleSheet by remember { mutableStateOf(false) }
        var showSettingsSheet by remember { mutableStateOf(false) }

        // Gestures state
        var hudBrightness by remember { mutableStateOf<Float?>(null) }
        var hudVolume by remember { mutableStateOf<Float?>(null) }
        var hudSeekDelta by remember { mutableStateOf<Long?>(null) }
        var isSpeedBoosting by remember { mutableStateOf(false) }

        // Subtitles sidecars
        val currentPath = if (playlist.isNotEmpty() && !playlist[playlistIndex].startsWith("content://")) playlist[playlistIndex] else null
        val sidecars = remember(currentPath) { subtitleManager.findSidecarSubtitles(currentPath) }
        var subtitleStyle by remember { mutableStateOf(subtitleManager.getSubtitleStyle()) }
        var currentEncoding by remember { mutableStateOf(subtitleManager.getDefaultEncoding()) }
        var audioDelayMs by remember { mutableStateOf(0L) }
        var subtitleDelayMs by remember { mutableStateOf(0L) }

        val currentTitle = if (playlist.isNotEmpty()) {
            val item = playlist[playlistIndex]
            if (item.startsWith("content://")) {
                Uri.parse(item).lastPathSegment ?: "Video"
            } else {
                File(item).name
            }
        } else "Video"

        // Auto-hide controls timer
        LaunchedEffect(isControlsVisible, playbackState, isControlsLocked) {
            if (isControlsVisible && playbackState is PlaybackState.Playing && !isControlsLocked) {
                delay(4500)
                isControlsVisible = false
            }
        }

        // Subtitle file picker launcher
        val subtitlePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}
                playerEngine.addSubtitle(uri, select = true)
            }
        }

        // External audio picker launcher
        val audioPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}
                playerEngine.addExternalAudio(uri)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // VLC Video View container
            AndroidView(
                factory = { ctx ->
                    VLCVideoLayout(ctx).apply {
                        playerEngine.attachVideoLayout(this)
                    }
                },
                update = { layout ->
                    playerEngine.attachVideoLayout(layout)
                },
                modifier = Modifier.fillMaxSize()
            )

            var lastTapTime by remember { mutableLongStateOf(0L) }
            var lastTapPos by remember { mutableStateOf(Offset.Zero) }
            var singleTapJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

            // Gesture surface overlay (only when controls are not locked and not in PiP)
            if (!isInPipMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(
                            isControlsLocked,
                            prefs.gestureSwipeBrightnessVolume,
                            prefs.gestureSwipeSeek,
                            prefs.gestureDoubleTapSeek,
                            prefs.gestureLongPressSpeed,
                            duration,
                            playbackState
                        ) {
                            if (isControlsLocked) return@pointerInput

                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val downTime = System.currentTimeMillis()
                                val downPos = down.position
                                val sizeWidth = size.width.toFloat()
                                val sizeHeight = size.height.toFloat()
                                val isLeft = downPos.x < sizeWidth / 2f
                                val touchSlop = viewConfiguration.touchSlop

                                var dragDirection = DragDirection.NONE
                                var totalDragX = 0f
                                var totalDragY = 0f

                                val initialBrightness = getWindowBrightness()
                                val initialVolumeFraction = getVolumeFraction()
                                val initialPosition = currentPosition
                                var longPressJob: kotlinx.coroutines.Job? = null
                                var isBoostingSpeed = false

                                // Start timer for 2.0x long-press speed boost
                                if (prefs.gestureLongPressSpeed && playbackState is PlaybackState.Playing) {
                                    longPressJob = coroutineScope.launch {
                                        delay(450)
                                        if (dragDirection == DragDirection.NONE) {
                                            isBoostingSpeed = true
                                            isSpeedBoosting = true
                                            playerEngine.setSpeed(2.0f)
                                        }
                                    }
                                }

                                while (true) {
                                    val event = awaitPointerEvent()

                                    // Pinch to zoom aspect ratio
                                    if (prefs.gesturePinchZoom && event.changes.size >= 2) {
                                        longPressJob?.cancel()
                                        longPressJob = null
                                        val zoom = event.calculateZoom()
                                        if (zoom > 1.25f && aspectRatio != VideoAspectRatio.ZOOM) {
                                            playerEngine.setAspectRatio(VideoAspectRatio.ZOOM)
                                        } else if (zoom < 0.8f && aspectRatio != VideoAspectRatio.FIT) {
                                            playerEngine.setAspectRatio(VideoAspectRatio.FIT)
                                        }
                                        event.changes.forEach { it.consume() }
                                        continue
                                    }

                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break

                                    if (!change.pressed) {
                                        break
                                    }

                                    val deltaX = change.position.x - change.previousPosition.x
                                    val deltaY = change.position.y - change.previousPosition.y
                                    totalDragX += deltaX
                                    totalDragY += deltaY

                                    if (dragDirection == DragDirection.NONE) {
                                        if (abs(totalDragX) > touchSlop || abs(totalDragY) > touchSlop) {
                                            longPressJob?.cancel()
                                            longPressJob = null

                                            if (abs(totalDragX) > abs(totalDragY)) {
                                                if (prefs.gestureSwipeSeek && duration > 0) {
                                                    dragDirection = DragDirection.HORIZONTAL
                                                }
                                            } else {
                                                if (prefs.gestureSwipeBrightnessVolume) {
                                                    dragDirection = DragDirection.VERTICAL
                                                }
                                            }
                                        }
                                    }

                                    when (dragDirection) {
                                        DragDirection.VERTICAL -> {
                                            change.consume()
                                            // Swiping UP decreases Y (delta is positive)
                                            val swipeDistanceY = downPos.y - change.position.y
                                            val fractionDelta = swipeDistanceY / (sizeHeight * 0.65f)
                                            if (isLeft) {
                                                // Left side swipe navigation: Brightness
                                                val newB = (initialBrightness + fractionDelta).coerceIn(0.01f, 1.0f)
                                                setWindowBrightness(newB)
                                                hudBrightness = newB
                                            } else {
                                                // Right side swipe navigation: Volume
                                                val newV = (initialVolumeFraction + fractionDelta).coerceIn(0f, 1f)
                                                val actualV = setVolumeFraction(newV)
                                                hudVolume = actualV
                                            }
                                        }
                                        DragDirection.HORIZONTAL -> {
                                            change.consume()
                                            // Swiping RIGHT increases X, swiping LEFT decreases X
                                            val swipeDistanceX = change.position.x - downPos.x
                                            val scrubMs = ((swipeDistanceX / sizeWidth) * 90000L).toLong()
                                            hudSeekDelta = scrubMs
                                        }
                                        DragDirection.NONE -> {}
                                    }
                                }

                                longPressJob?.cancel()
                                longPressJob = null

                                if (isBoostingSpeed) {
                                    isSpeedBoosting = false
                                    playerEngine.setSpeed(speed)
                                } else if (dragDirection == DragDirection.HORIZONTAL) {
                                    hudSeekDelta?.let { delta ->
                                        val targetPos = (initialPosition + delta).coerceIn(0L, duration.coerceAtLeast(0L))
                                        playerEngine.seekTo(targetPos)
                                    }
                                    coroutineScope.launch {
                                        delay(600)
                                        hudSeekDelta = null
                                    }
                                } else if (dragDirection == DragDirection.VERTICAL) {
                                    coroutineScope.launch {
                                        delay(600)
                                        hudBrightness = null
                                        hudVolume = null
                                    }
                                } else if (dragDirection == DragDirection.NONE) {
                                    // Tap handling (distinguish between single and double tap)
                                    val tapDuration = System.currentTimeMillis() - downTime
                                    if (tapDuration < 350) {
                                        val now = System.currentTimeMillis()
                                        val prevTapTime = lastTapTime
                                        val prevPos = lastTapPos
                                        val dist = hypot(downPos.x - prevPos.x, downPos.y - prevPos.y)

                                        if (now - prevTapTime < 320 && dist < 120f && prefs.gestureDoubleTapSeek) {
                                            // Double tap seek!
                                            singleTapJob?.cancel()
                                            singleTapJob = null
                                            lastTapTime = 0L

                                            val isRight = downPos.x >= sizeWidth / 2f
                                            val delta = if (isRight) 10000L else -10000L
                                            val newPos = (currentPosition + delta).coerceIn(0L, duration.coerceAtLeast(0L))
                                            playerEngine.seekTo(newPos)
                                            hudSeekDelta = delta
                                            coroutineScope.launch {
                                                delay(700)
                                                hudSeekDelta = null
                                            }
                                        } else {
                                            lastTapTime = now
                                            lastTapPos = downPos
                                            singleTapJob = coroutineScope.launch {
                                                delay(260)
                                                isControlsVisible = !isControlsVisible
                                            }
                                        }
                                    }
                                }
                            }
                        }
                )

                // Gesture HUD feedback
                VideoPlayerGestureHud(
                    brightnessPercent = hudBrightness,
                    volumePercent = hudVolume,
                    seekDeltaMs = hudSeekDelta,
                    isSpeedBoosting = isSpeedBoosting
                )

                val isPlaying = playerEngine.isPlaying || playbackState is PlaybackState.Playing

                // Expressive Controls Overlay
                VideoPlayerControlsOverlay(
                    title = currentTitle,
                    playbackState = playbackState,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPosition,
                    durationMs = duration,
                    speed = speed,
                    aspectRatio = aspectRatio,
                    isControlsVisible = isControlsVisible && !isInPipMode,
                    isControlsLocked = isControlsLocked,
                    hasNext = playlistIndex < playlist.size - 1,
                    hasPrevious = playlistIndex > 0,
                    onToggleControls = { isControlsVisible = !isControlsVisible },
                    onBackClick = { finish() },
                    onPlayPauseClick = {
                        if (isPlaying) {
                            playerEngine.pause()
                        } else {
                            requestAudioFocus()
                            playerEngine.play()
                        }
                        // Second tap on play/pause dismisses controls for immersive view
                        isControlsVisible = false
                        updatePipParams()
                    },
                    onSeek = { targetMs -> playerEngine.seekTo(targetMs) },
                    onNextClick = {
                        if (playlistIndex < playlist.size - 1) {
                            playlistIndex++
                            loadCurrentVideo()
                        }
                    },
                    onPreviousClick = {
                        if (playlistIndex > 0) {
                            playlistIndex--
                            loadCurrentVideo()
                        }
                    },
                    onLockToggle = { isControlsLocked = !isControlsLocked },
                    onAudioClick = { showAudioSheet = true },
                    onSubtitleClick = { showSubtitleSheet = true },
                    onPipClick = { enterPip() },
                    onAspectClick = {
                        val nextAspect = when (aspectRatio) {
                            VideoAspectRatio.FIT -> VideoAspectRatio.FILL
                            VideoAspectRatio.FILL -> VideoAspectRatio.ZOOM
                            VideoAspectRatio.ZOOM -> VideoAspectRatio.RATIO_16_9
                            VideoAspectRatio.RATIO_16_9 -> VideoAspectRatio.RATIO_4_3
                            VideoAspectRatio.RATIO_4_3 -> VideoAspectRatio.ORIGINAL
                            VideoAspectRatio.ORIGINAL -> VideoAspectRatio.FIT
                        }
                        playerEngine.setAspectRatio(nextAspect)
                    },
                    onSettingsClick = { showSettingsSheet = true },
                    onRotationLockToggle = {
                        isRotationLocked = !isRotationLocked
                        if (isRotationLocked) {
                            // Lock to current orientation
                            val currConfig = resources.configuration.orientation
                            requestedOrientation = if (currConfig == Configuration.ORIENTATION_LANDSCAPE) {
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            }
                        } else {
                            applyOrientationPreference(prefs.defaultOrientation)
                        }
                    },
                    isRotationLocked = isRotationLocked
                )
            }

            // Error Overlay
            if (playbackState is PlaybackState.Error) {
                val errorMsg = (playbackState as PlaybackState.Error).message
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.88f))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = ExpressiveTheme.colors.container,
                        modifier = Modifier.fillMaxWidth().wrapContentHeight()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = ExpressiveTheme.colors.accent,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Unable to play video",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpressiveTheme.colors.text
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMsg,
                                fontSize = 14.sp,
                                color = ExpressiveTheme.colors.muted,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { finish() },
                                    modifier = Modifier.weight(1f),
                                    shape = CircleShape
                                ) {
                                    Text("Back")
                                }
                                Button(
                                    onClick = {
                                        // Open with another app chooser
                                        if (playlist.isNotEmpty()) {
                                            val raw = playlist[playlistIndex]
                                            val (uri, _, _) = resolveVideoSource(raw)
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, "video/*")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            try {
                                                startActivity(Intent.createChooser(intent, "Open with"))
                                                finish()
                                            } catch (e: Exception) {
                                                Log.e(TAG, "Error opening with external app", e)
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = ExpressiveTheme.colors.accent)
                                ) {
                                    Text("Open with...", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Audio Tracks Sheet
            if (showAudioSheet) {
                AudioTracksSheet(
                    tracks = audioTracks,
                    selectedTrackId = selectedAudioTrackId,
                    audioDelayMs = audioDelayMs,
                    onSelectTrack = { id -> playerEngine.setAudioTrack(id) },
                    onAddExternalAudio = { audioPicker.launch(arrayOf("audio/*")) },
                    onSetAudioDelay = { delay ->
                        audioDelayMs = delay
                        playerEngine.setAudioDelay(delay)
                    },
                    onDismissRequest = { showAudioSheet = false }
                )
            }

            // Subtitle Sheet
            if (showSubtitleSheet) {
                SubtitleSheet(
                    tracks = subtitleTracks,
                    sidecars = sidecars,
                    selectedTrackId = selectedSubtitleTrackId,
                    subtitleDelayMs = subtitleDelayMs,
                    subtitleStyle = subtitleStyle,
                    currentEncoding = currentEncoding,
                    onSelectTrack = { id -> playerEngine.setSubtitleTrack(id) },
                    onSelectSidecar = { sidecar -> playerEngine.addSubtitle(sidecar.uri, select = true) },
                    onAddSubtitleFile = { subtitlePicker.launch(arrayOf("*/*")) },
                    onSetSubtitleDelay = { delay ->
                        subtitleDelayMs = delay
                        playerEngine.setSubtitleDelay(delay)
                    },
                    onUpdateStyle = { newStyle ->
                        subtitleStyle = newStyle
                        subtitleManager.saveSubtitleStyle(newStyle)
                        playerEngine.updateSubtitleStyle(newStyle)
                    },
                    onSetEncoding = { enc ->
                        currentEncoding = enc
                        subtitleManager.setDefaultEncoding(enc)
                        playerEngine.setSubtitleEncoding(enc)
                    },
                    onDismissRequest = { showSubtitleSheet = false }
                )
            }

            // Video Settings Sheet
            if (showSettingsSheet) {
                VideoSettingsSheet(
                    currentSpeed = speed,
                    currentAspect = aspectRatio,
                    currentHw = prefs.hwAcceleration,
                    prefs = prefs,
                    onSetSpeed = { spd -> playerEngine.setSpeed(spd) },
                    onSetAspect = { asp -> playerEngine.setAspectRatio(asp) },
                    onSetHw = { hw ->
                        prefs.hwAcceleration = hw
                        playerEngine.setHardwareAcceleration(hw)
                    },
                    onDismissRequest = { showSettingsSheet = false }
                )
            }
        }
    }
}
