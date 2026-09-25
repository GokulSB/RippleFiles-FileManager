package com.ripple.filemanager.localsend

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.StandardSocketOptions
import java.util.concurrent.ConcurrentHashMap

/**
 * Implements LocalSend v2 UDP Multicast Discovery.
 * Group: 224.0.0.167:53317
 */
class LocalSendDiscovery(
    private val context: Context,
    var localPort: Int = LocalSendServer.DEFAULT_HTTP_PORT,
    private val protocol: String = "https",
    private val httpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "LocalSendDiscovery"
        const val MULTICAST_ADDRESS = "224.0.0.167"
        const val MULTICAST_PORT = 53317
        private const val PEER_EXPIRY_MS = 25_000L
        private const val ANNOUNCE_INTERVAL_MS = 4_000L
    }

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var multicastLock: WifiManager.MulticastLock? = null
    private var multicastSocket: MulticastSocket? = null

    private val peersMap = ConcurrentHashMap<String, NearbyPeer>()
    private val _peers = MutableStateFlow<List<NearbyPeer>>(emptyList())
    val peers: StateFlow<List<NearbyPeer>> = _peers.asStateFlow()

    private val localFingerprint: String
        get() = LocalSendSecurity.getFingerprint(context)

    fun getLocalDevice(): DeviceDto {
        val prefs = context.getSharedPreferences("sift_prefs", Context.MODE_PRIVATE)
        val customName = prefs.getString("device_name", null)
        val alias = if (!customName.isNullOrBlank()) customName else "${Build.MANUFACTURER} ${Build.MODEL}"

        return DeviceDto(
            alias = alias,
            version = "2.0",
            deviceModel = Build.MODEL,
            deviceType = "mobile",
            fingerprint = localFingerprint,
            port = localPort,
            protocol = protocol,
            download = false,
            announce = true
        )
    }

    fun start() {
        stop()

        // Acquire Android MulticastLock
        try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifi?.createMulticastLock("RippleLocalSendMulticastLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire multicast lock", e)
        }

        // Launch UDP listener
        scope.launch {
            listenForAnnouncements()
        }

        // Launch periodic broadcaster
        scope.launch {
            while (isActive) {
                sendAnnouncement(announce = true)
                delay(ANNOUNCE_INTERVAL_MS)
            }
        }

        // Launch peer pruner
        scope.launch {
            while (isActive) {
                delay(5000L)
                pruneInactivePeers()
            }
        }
    }

    fun stop() {
        scope.coroutineContext.cancelChildren()
        try {
            multicastSocket?.leaveGroup(InetAddress.getByName(MULTICAST_ADDRESS))
        } catch (e: Exception) {}
        try {
            multicastSocket?.close()
        } catch (e: Exception) {}
        multicastSocket = null

        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (e: Exception) {}
        multicastLock = null

        peersMap.clear()
        _peers.value = emptyList()
    }

    fun registerPeer(peer: NearbyPeer) {
        if (peer.fingerprint.equals(localFingerprint, ignoreCase = true)) return
        peersMap[peer.id] = peer
        LocalSendSecurity.pinnedFingerprints[peer.id] = peer.fingerprint
        _peers.value = peersMap.values.toList().sortedBy { it.alias }
    }

    private fun listenForAnnouncements() {
        try {
            val socket = MulticastSocket(null).apply {
                reuseAddress = true
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        setOption(StandardSocketOptions.SO_REUSEPORT, true)
                    }
                } catch (e: Throwable) {
                    Log.d(TAG, "SO_REUSEPORT not supported: ${e.message}")
                }
            }

            try {
                socket.bind(InetSocketAddress(MULTICAST_PORT))
                Log.i(TAG, "Bound multicast listener to default port $MULTICAST_PORT")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to bind multicast socket to $MULTICAST_PORT, falling back to ephemeral port", e)
                try {
                    socket.bind(InetSocketAddress(0))
                    Log.i(TAG, "Bound multicast listener to fallback ephemeral port ${socket.localPort}")
                } catch (bindErr: Exception) {
                    Log.e(TAG, "Failed to bind multicast socket to fallback port", bindErr)
                    throw bindErr
                }
            }

            val group = InetAddress.getByName(MULTICAST_ADDRESS)
            socket.joinGroup(group)
            multicastSocket = socket

            val buffer = ByteArray(65535)

            while (scope.isActive) {
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)

                val message = String(packet.data, packet.offset, packet.length, Charsets.UTF_8)
                val senderIp = packet.address.hostAddress ?: continue

                handleIncomingAnnouncement(message, senderIp)
            }
        } catch (e: Exception) {
            if (scope.isActive) {
                Log.e(TAG, "Error in multicast listener", e)
            }
        }
    }

    private fun handleIncomingAnnouncement(jsonString: String, senderIp: String) {
        try {
            val device = gson.fromJson(jsonString, DeviceDto::class.java) ?: return

            // Filter out self-announcements
            if (device.fingerprint.equals(localFingerprint, ignoreCase = true)) {
                return
            }

            val peer = NearbyPeer(
                ip = senderIp,
                port = device.port,
                alias = device.alias,
                deviceModel = device.deviceModel,
                deviceType = device.deviceType ?: "mobile",
                protocol = device.protocol,
                fingerprint = device.fingerprint,
                lastSeenMs = System.currentTimeMillis()
            )

            registerPeer(peer)

            // If the peer sent announce: true, respond so they learn about us immediately
            if (device.announce == true) {
                scope.launch {
                    // Send UDP unicast/multicast announcement with announce: false
                    sendAnnouncement(announce = false)

                    // Also attempt HTTP /register endpoint fallback per LocalSend spec
                    sendHttpRegister(peer)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Failed to parse announcement: $jsonString", e)
        }
    }

    fun sendAnnouncement(announce: Boolean = true) {
        scope.launch(Dispatchers.IO) {
            try {
                val device = getLocalDevice().copy(announce = announce)
                val json = gson.toJson(device)
                val bytes = json.toByteArray(Charsets.UTF_8)
                val group = InetAddress.getByName(MULTICAST_ADDRESS)
                val packet = DatagramPacket(bytes, bytes.size, group, MULTICAST_PORT)

                // Send via multicast
                val existingSocket = multicastSocket
                if (existingSocket != null && !existingSocket.isClosed) {
                    existingSocket.send(packet)
                } else {
                    MulticastSocket(null).use { tempSocket ->
                        tempSocket.reuseAddress = true
                        try {
                            tempSocket.bind(InetSocketAddress(0))
                        } catch (e: Exception) {}
                        tempSocket.send(packet)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send announcement", e)
            }
        }
    }

    private fun sendHttpRegister(peer: NearbyPeer) {
        try {
            val localDevice = getLocalDevice().copy(announce = false)
            val json = gson.toJson(localDevice)
            val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url("${peer.baseUrl}/api/localsend/v2/register")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val replyDevice = gson.fromJson(body, DeviceDto::class.java)
                        if (replyDevice != null) {
                            registerPeer(
                                NearbyPeer(
                                    ip = peer.ip,
                                    port = replyDevice.port,
                                    alias = replyDevice.alias,
                                    deviceModel = replyDevice.deviceModel,
                                    deviceType = replyDevice.deviceType ?: "mobile",
                                    protocol = replyDevice.protocol,
                                    fingerprint = replyDevice.fingerprint,
                                    lastSeenMs = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Peer may be offline or firewall blocking HTTP register; multicast handles it
            Log.d(TAG, "HTTP register to ${peer.id} failed: ${e.message}")
        }
    }

    private fun pruneInactivePeers() {
        val now = System.currentTimeMillis()
        var changed = false

        for ((id, peer) in peersMap) {
            if (now - peer.lastSeenMs > PEER_EXPIRY_MS) {
                peersMap.remove(id)
                LocalSendSecurity.pinnedFingerprints.remove(id)
                changed = true
            }
        }

        if (changed) {
            _peers.value = peersMap.values.toList().sortedBy { it.alias }
        }
    }
}
