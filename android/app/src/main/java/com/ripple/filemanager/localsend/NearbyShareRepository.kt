package com.ripple.filemanager.localsend

import android.content.Context
import android.util.Log
import com.ripple.filemanager.FileItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

sealed interface NearbyShareEvent {
    data class Success(val message: String) : NearbyShareEvent
    data class Error(val message: String) : NearbyShareEvent
    data class FilesReceived(val peerAlias: String, val files: List<File>) : NearbyShareEvent
}

/**
 * Singleton repository coordinating Nearby Share (LocalSend Protocol v2) operations:
 * - Device discovery
 * - File staging and sending
 * - File receiving and acceptance
 * - Foreground service management
 */
object NearbyShareRepository {

    private const val TAG = "NearbyShareRepository"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var discoveryInstance: LocalSendDiscovery? = null
    private var serverInstance: LocalSendServer? = null
    private var clientInstance: LocalSendClient? = null

    private val _isReceiving = MutableStateFlow(false)
    val isReceiving: StateFlow<Boolean> = _isReceiving.asStateFlow()

    private val _peers = MutableStateFlow<List<NearbyPeer>>(emptyList())
    val peers: StateFlow<List<NearbyPeer>> = _peers.asStateFlow()

    private val _stagedFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val stagedFiles: StateFlow<List<FileItem>> = _stagedFiles.asStateFlow()

    private val _activeTransfer = MutableStateFlow<TransferSession?>(null)
    val activeTransfer: StateFlow<TransferSession?> = _activeTransfer.asStateFlow()

    private val _incomingRequest = MutableStateFlow<IncomingTransferRequest?>(null)
    val incomingRequest: StateFlow<IncomingTransferRequest?> = _incomingRequest.asStateFlow()

    private val _events = MutableSharedFlow<NearbyShareEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<NearbyShareEvent> = _events.asSharedFlow()

    private var peerCollectorJob: Job? = null
    private var clientCollectorJob: Job? = null

    fun emitError(message: String) {
        scope.launch {
            _events.emit(NearbyShareEvent.Error(message))
        }
    }

    fun emitSuccess(message: String) {
        scope.launch {
            _events.emit(NearbyShareEvent.Success(message))
        }
    }

    fun setReceiving(receiving: Boolean) {
        _isReceiving.value = receiving
        if (!receiving) {
            _incomingRequest.value = null
        }
    }

    fun setDiscovery(discovery: LocalSendDiscovery?) {
        discoveryInstance = discovery
        peerCollectorJob?.cancel()

        if (discovery != null) {
            peerCollectorJob = scope.launch {
                discovery.peers.collect { list ->
                    _peers.value = list
                }
            }
        } else {
            _peers.value = emptyList()
        }
    }

    fun setServer(server: LocalSendServer?) {
        serverInstance = server
    }

    private fun getClient(context: Context): LocalSendClient {
        return clientInstance ?: synchronized(this) {
            val disc = discoveryInstance ?: LocalSendDiscovery(
                context.applicationContext,
                httpClient = okhttp3.OkHttpClient.Builder().apply {
                    LocalSendSecurity.configureOkHttpClient(this)
                }.build()
            )
            LocalSendClient(context.applicationContext, disc).also {
                clientInstance = it
                clientCollectorJob?.cancel()
                clientCollectorJob = scope.launch {
                    it.activeSession.collect { session ->
                        // Only update if current active session is outgoing
                        if (session != null && !session.isIncoming) {
                            _activeTransfer.value = session
                        }
                    }
                }
            }
        }
    }

    fun startService(context: Context) {
        NearbyShareService.start(context)
    }

    fun stopService(context: Context) {
        NearbyShareService.stop(context)
    }

    fun toggleReceiving(context: Context, enabled: Boolean) {
        if (enabled) {
            startService(context)
        } else {
            stopService(context)
        }
    }

    fun stageFiles(files: List<FileItem>) {
        val current = _stagedFiles.value.toMutableList()
        for (f in files) {
            if (current.none { it.path == f.path }) {
                current.add(f)
            }
        }
        _stagedFiles.value = current
    }

    fun removeStagedFile(file: FileItem) {
        _stagedFiles.value = _stagedFiles.value.filter { it.path != file.path }
    }

    fun clearStagedFiles() {
        _stagedFiles.value = emptyList()
    }

    fun sendStagedFiles(context: Context, peer: NearbyPeer) {
        val staged = _stagedFiles.value
        if (staged.isEmpty()) {
            scope.launch { _events.emit(NearbyShareEvent.Error("No files selected to send")) }
            return
        }

        val localFiles = staged.map { File(it.path) }.filter { it.exists() }
        if (localFiles.isEmpty()) {
            scope.launch { _events.emit(NearbyShareEvent.Error("Selected files could not be found")) }
            return
        }

        // Ensure service / discovery is initialized
        val client = getClient(context)
        client.startSend(peer, localFiles, scope) { success, errorMsg ->
            scope.launch {
                if (success) {
                    _events.emit(NearbyShareEvent.Success("Files sent successfully to ${peer.alias}"))
                    clearStagedFiles()
                } else if (errorMsg != null) {
                    _events.emit(NearbyShareEvent.Error(errorMsg))
                }
            }
        }
    }

    fun acceptIncomingRequest(sessionId: String) {
        val req = _incomingRequest.value ?: return
        if (req.sessionId == sessionId) {
            val peer = NearbyPeer(
                ip = req.senderIp,
                port = req.sender.port,
                alias = req.sender.alias,
                deviceModel = req.sender.deviceModel,
                deviceType = req.sender.deviceType ?: "mobile",
                protocol = req.sender.protocol,
                fingerprint = req.sender.fingerprint,
                lastSeenMs = System.currentTimeMillis()
            )

            _activeTransfer.value = TransferSession(
                sessionId = sessionId,
                peer = peer,
                files = req.files,
                isIncoming = true,
                status = TransferStatus.IN_PROGRESS,
                totalBytes = req.totalBytes
            )

            req.decisionChannel.complete(true)
            _incomingRequest.value = null
        }
    }

    fun declineIncomingRequest(sessionId: String) {
        val req = _incomingRequest.value ?: return
        if (req.sessionId == sessionId) {
            req.decisionChannel.complete(false)
            _incomingRequest.value = null
        }
    }

    fun startIncomingDirectly(req: IncomingTransferRequest) {
        val peer = NearbyPeer(
            ip = req.senderIp,
            port = req.sender.port,
            alias = req.sender.alias,
            deviceModel = req.sender.deviceModel,
            deviceType = req.sender.deviceType ?: "mobile",
            protocol = req.sender.protocol,
            fingerprint = req.sender.fingerprint,
            lastSeenMs = System.currentTimeMillis()
        )

        _activeTransfer.value = TransferSession(
            sessionId = req.sessionId,
            peer = peer,
            files = req.files,
            isIncoming = true,
            status = TransferStatus.IN_PROGRESS,
            totalBytes = req.totalBytes
        )
    }

    fun broadcastAnnouncement() {
        discoveryInstance?.sendAnnouncement(announce = true)
    }

    fun cancelActiveTransfer() {
        val current = _activeTransfer.value ?: return
        if (current.isIncoming) {
            // Server-side cancel
            _activeTransfer.value = current.copy(status = TransferStatus.CANCELLED)
        } else {
            clientInstance?.cancelCurrentTransfer()
        }
    }

    fun dismissTransferCard() {
        _activeTransfer.value = null
        clientInstance?.clearSession()
    }

    // Callbacks from LocalSendServer
    fun onIncomingRequest(request: IncomingTransferRequest) {
        scope.launch {
            _incomingRequest.value = request
        }
    }

    fun onIncomingProgress(sessionId: String, fileId: String, bytesRead: Long, totalBytes: Long) {
        scope.launch {
            val current = _activeTransfer.value
            if (current != null && current.sessionId == sessionId) {
                _activeTransfer.value = current.copy(
                    bytesTransferred = bytesRead,
                    status = TransferStatus.IN_PROGRESS
                )
            }
        }
    }

    fun onIncomingComplete(sessionId: String, peer: DeviceDto, savedFiles: List<File>) {
        scope.launch {
            val current = _activeTransfer.value
            if (current != null && current.sessionId == sessionId) {
                _activeTransfer.value = current.copy(
                    status = TransferStatus.COMPLETED,
                    bytesTransferred = current.totalBytes
                )
            }
            _events.emit(NearbyShareEvent.Success("Received ${savedFiles.size} file(s) from ${peer.alias}"))
            if (savedFiles.isNotEmpty()) {
                _events.emit(NearbyShareEvent.FilesReceived(peer.alias, savedFiles))
            }
        }
    }

    fun onIncomingCancelled(sessionId: String) {
        scope.launch {
            val current = _activeTransfer.value
            if (current != null && current.sessionId == sessionId) {
                _activeTransfer.value = current.copy(status = TransferStatus.CANCELLED)
            }
        }
    }
}
