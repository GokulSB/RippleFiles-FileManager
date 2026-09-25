package com.ripple.filemanager.localsend

import android.content.Context
import android.util.Log
import android.webkit.MimeTypeMap
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * LocalSend Protocol v2 Client.
 * Handles initiating transfers, sending files with progress tracking,
 * and cancellation against LocalSend v2 compatible peers.
 */
class LocalSendClient(
    private val context: Context,
    private val discovery: LocalSendDiscovery
) {
    companion object {
        private const val TAG = "LocalSendClient"
    }

    private val gson = Gson()
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.MINUTES)
            .readTimeout(5, TimeUnit.MINUTES)
            .apply {
                LocalSendSecurity.configureOkHttpClient(this)
            }
            .build()
    }

    private val _activeSession = MutableStateFlow<TransferSession?>(null)
    val activeSession: StateFlow<TransferSession?> = _activeSession.asStateFlow()

    private var activeJob: Job? = null
    private var currentSessionId: String? = null
    private var currentPeer: NearbyPeer? = null

    /**
     * Sends the list of local files to the specified peer.
     */
    fun startSend(
        peer: NearbyPeer,
        files: List<File>,
        scope: CoroutineScope,
        onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        cancelCurrentTransfer()

        activeJob = scope.launch(Dispatchers.IO) {
            val totalBytes = files.sumOf { it.length() }
            val fileItems = files.map { file ->
                val ext = file.extension.lowercase(Locale.ROOT)
                val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
                TransferFileItem(
                    fileId = UUID.randomUUID().toString(),
                    fileName = file.name,
                    size = file.length(),
                    fileType = mime,
                    localPath = file.absolutePath
                )
            }

            var session = TransferSession(
                sessionId = UUID.randomUUID().toString(),
                peer = peer,
                files = fileItems,
                isIncoming = false,
                status = TransferStatus.INITIALIZING,
                totalBytes = totalBytes
            )
            _activeSession.value = session
            currentPeer = peer

            try {
                // Ensure peer fingerprint is pinned
                LocalSendSecurity.pinnedFingerprints[peer.id] = peer.fingerprint

                // Step 1: /api/localsend/v2/prepare-upload
                session = session.copy(status = TransferStatus.WAITING_ACCEPT)
                _activeSession.value = session

                val localDevice = discovery.getLocalDevice().copy(announce = false)
                val fileDtoMap = fileItems.associate { item ->
                    item.fileId to FileDto(
                        id = item.fileId,
                        fileName = item.fileName,
                        size = item.size,
                        fileType = item.fileType
                    )
                }
                val prepareReq = PrepareUploadRequestDto(
                    info = localDevice,
                    files = fileDtoMap
                )
                val prepareBody = gson.toJson(prepareReq)
                    .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

                val prepareRequest = Request.Builder()
                    .url("${peer.baseUrl}/api/localsend/v2/prepare-upload")
                    .post(prepareBody)
                    .build()

                val prepareResponse = try {
                    okHttpClient.newCall(prepareRequest).execute()
                } catch (e: Exception) {
                    throw IOException("Could not connect to ${peer.alias}: ${e.message}", e)
                }

                if (prepareResponse.code == 403) {
                    session = session.copy(status = TransferStatus.REJECTED, errorMessage = "Transfer declined by ${peer.alias}")
                    _activeSession.value = session
                    onComplete(false, "Transfer declined by ${peer.alias}")
                    return@launch
                }

                if (!prepareResponse.isSuccessful) {
                    val code = prepareResponse.code
                    prepareResponse.close()
                    throw IOException("Peer responded with HTTP $code")
                }

                val respStr = prepareResponse.body?.string() ?: throw IOException("Empty response from peer")
                val prepareResp = gson.fromJson(respStr, PrepareUploadResponseDto::class.java)
                val sessionId = prepareResp.sessionId
                currentSessionId = sessionId
                val tokens = prepareResp.files // fileId -> token

                session = session.copy(
                    sessionId = sessionId,
                    status = TransferStatus.IN_PROGRESS,
                    files = session.files.map { item ->
                        item.copy(token = tokens[item.fileId])
                    }
                )
                _activeSession.value = session

                // Step 2: Upload each file
                var cumulativeSent = 0L
                var lastTime = System.currentTimeMillis()
                var lastBytes = 0L

                for ((index, item) in session.files.withIndex()) {
                    if (!isActive) break

                    val localFile = File(item.localPath ?: "")
                    if (!localFile.exists()) {
                        Log.w(TAG, "File ${item.fileName} not found at ${item.localPath}")
                        continue
                    }

                    val token = item.token ?: tokens[item.fileId] ?: ""
                    val uploadUrl = "${peer.baseUrl}/api/localsend/v2/upload?sessionId=$sessionId&fileId=${item.fileId}&token=$token"

                    session = session.copy(currentFileIndex = index)
                    _activeSession.value = session

                    val countingBody = CountingRequestBody(
                        file = localFile,
                        contentType = "application/octet-stream".toMediaTypeOrNull()
                    ) { fileBytesSent, _ ->
                        val now = System.currentTimeMillis()
                        val dt = now - lastTime
                        val currentTotalSent = cumulativeSent + fileBytesSent

                        var speed = session.speedBytesPerSec
                        if (dt >= 500) {
                            speed = ((currentTotalSent - lastBytes) * 1000) / dt
                            lastTime = now
                            lastBytes = currentTotalSent
                        }

                        _activeSession.value = _activeSession.value?.copy(
                            bytesTransferred = currentTotalSent,
                            speedBytesPerSec = speed
                        )
                    }

                    val uploadRequest = Request.Builder()
                        .url(uploadUrl)
                        .post(countingBody)
                        .build()

                    val uploadResponse = okHttpClient.newCall(uploadRequest).execute()
                    if (!uploadResponse.isSuccessful) {
                        val code = uploadResponse.code
                        uploadResponse.close()
                        throw IOException("Failed to send ${item.fileName} (HTTP $code)")
                    }
                    uploadResponse.close()

                    cumulativeSent += item.size
                    session = session.copy(
                        files = session.files.mapIndexed { idx, itm ->
                            if (idx == index) itm.copy(isFinished = true, bytesTransferred = item.size) else itm
                        },
                        bytesTransferred = cumulativeSent
                    )
                    _activeSession.value = session
                }

                session = session.copy(
                    status = TransferStatus.COMPLETED,
                    bytesTransferred = totalBytes
                )
                _activeSession.value = session
                onComplete(true, null)

            } catch (e: CancellationException) {
                Log.i(TAG, "Transfer cancelled")
                session = session.copy(status = TransferStatus.CANCELLED)
                _activeSession.value = session
                sendCancelToPeer(peer, session.sessionId)
                onComplete(false, "Transfer cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Transfer error", e)
                session = session.copy(
                    status = TransferStatus.FAILED,
                    errorMessage = e.message ?: "Transfer failed"
                )
                _activeSession.value = session
                sendCancelToPeer(peer, session.sessionId)
                onComplete(false, e.message ?: "Transfer failed")
            }
        }
    }

    fun cancelCurrentTransfer() {
        val session = _activeSession.value
        val peer = currentPeer
        val sessionId = currentSessionId ?: session?.sessionId
        if (session != null && peer != null && sessionId != null &&
            (session.status == TransferStatus.IN_PROGRESS || session.status == TransferStatus.WAITING_ACCEPT)
        ) {
            sendCancelToPeer(peer, sessionId)
        }
        activeJob?.cancel()
        activeJob = null
        if (_activeSession.value?.status == TransferStatus.IN_PROGRESS ||
            _activeSession.value?.status == TransferStatus.WAITING_ACCEPT
        ) {
            _activeSession.value = _activeSession.value?.copy(status = TransferStatus.CANCELLED)
        }
    }

    fun clearSession() {
        _activeSession.value = null
        currentSessionId = null
        currentPeer = null
    }

    private fun sendCancelToPeer(peer: NearbyPeer, sessionId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder()
                    .url("${peer.baseUrl}/api/localsend/v2/cancel?sessionId=$sessionId")
                    .post(ByteArray(0).toRequestBody(null))
                    .build()
                okHttpClient.newCall(request).execute().close()
            } catch (e: Exception) {
                Log.d(TAG, "Cancel notification to peer failed: ${e.message}")
            }
        }
    }

    private class CountingRequestBody(
        private val file: File,
        private val contentType: MediaType?,
        private val onProgress: (bytesWritten: Long, totalBytes: Long) -> Unit
    ) : RequestBody() {
        override fun contentType(): MediaType? = contentType
        override fun contentLength(): Long = file.length()

        override fun writeTo(sink: BufferedSink) {
            val totalLength = file.length()
            file.inputStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                var uploaded = 0L
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    sink.write(buffer, 0, read)
                    sink.flush()
                    uploaded += read
                    onProgress(uploaded, totalLength)
                }
            }
        }
    }
}
