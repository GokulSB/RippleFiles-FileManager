package com.ripple.filemanager.localsend

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import com.google.gson.Gson
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import fi.iki.elonen.NanoHTTPD.Response
import fi.iki.elonen.NanoHTTPD.Method
import fi.iki.elonen.NanoHTTPD.SOCKET_READ_TIMEOUT
import fi.iki.elonen.NanoHTTPD.MIME_PLAINTEXT
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Embedded NanoHTTPD server implementing LocalSend v2 endpoints:
 * - POST /api/localsend/v2/register
 * - GET  /api/localsend/v2/info
 * - POST /api/localsend/v2/prepare-upload
 * - POST /api/localsend/v2/upload
 * - POST /api/localsend/v2/cancel
 */
class LocalSendServer(
    private val context: Context,
    private val defaultPort: Int = DEFAULT_HTTP_PORT,
    private val protocol: String = "https",
    private val discovery: LocalSendDiscovery,
    private val onIncomingTransferRequest: (IncomingTransferRequest) -> Unit,
    private val onProgressUpdate: (sessionId: String, fileId: String, bytesRead: Long, totalBytes: Long) -> Unit,
    private val onTransferComplete: (sessionId: String, peer: DeviceDto, savedFiles: List<File>) -> Unit,
    private val onTransferCancelled: (sessionId: String) -> Unit
) {

    companion object {
        private const val TAG = "LocalSendServer"
        const val DEFAULT_HTTP_PORT = 53318
        private const val BUFFER_SIZE = 64 * 1024
        private const val DECISION_TIMEOUT_MS = 60_000L
    }

    private inner class NanoServer(portToBind: Int) : NanoHTTPD(portToBind) {
        override fun serve(session: IHTTPSession): Response {
            return this@LocalSendServer.serveRequest(session)
        }
    }

    private var nanoServer: NanoServer? = null
    var boundPort: Int = defaultPort
        private set

    private fun newFixedLengthResponse(status: Response.IStatus, mimeType: String, txt: String): Response {
        return NanoHTTPD.newFixedLengthResponse(status, mimeType, txt)
    }

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private class ActiveSession(
        val sessionId: String,
        val sender: DeviceDto,
        val tokens: Map<String, String>, // fileId -> token
        val fileDtos: Map<String, FileDto>,
        val savedFiles: MutableList<File> = Collections.synchronizedList(mutableListOf()),
        var currentReceivingFile: File? = null,
        var isCancelled: Boolean = false
    )

    private val activeSessions = ConcurrentHashMap<String, ActiveSession>()

    fun startServer(): Result<Int> {
        // Attempt 1: Bind default port (e.g. 53318)
        try {
            val srv = NanoServer(defaultPort)
            if (protocol == "https") {
                srv.makeSecure(LocalSendSecurity.getServerSslSocketFactory(context), null)
            }
            srv.start(SOCKET_READ_TIMEOUT, false)
            val actualPort = srv.listeningPort
            if (actualPort > 0) {
                boundPort = actualPort
                nanoServer = srv
                Log.i(TAG, "LocalSend server started on default port $actualPort ($protocol)")
                return Result.success(actualPort)
            } else {
                srv.stop()
                Log.w(TAG, "Default port $defaultPort returned invalid listening port: $actualPort")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start LocalSend server on default port $defaultPort, attempting fallback to ephemeral port", e)
        }

        // Attempt 2: Fall back to ephemeral port (0)
        return try {
            val srv = NanoServer(0)
            if (protocol == "https") {
                srv.makeSecure(LocalSendSecurity.getServerSslSocketFactory(context), null)
            }
            srv.start(SOCKET_READ_TIMEOUT, false)
            val actualPort = srv.listeningPort
            if (actualPort > 0) {
                boundPort = actualPort
                nanoServer = srv
                Log.i(TAG, "LocalSend server started on fallback ephemeral port $actualPort ($protocol)")
                Result.success(actualPort)
            } else {
                srv.stop()
                Log.e(TAG, "Fallback ephemeral port returned invalid listening port: $actualPort")
                Result.failure(java.io.IOException("Invalid listening port: $actualPort"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start LocalSend server on fallback port as well", e)
            Result.failure(e)
        }
    }

    fun stopServer() {
        scope.cancel()
        activeSessions.clear()
        try {
            nanoServer?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping NanoHTTPD server", e)
        }
        nanoServer = null
    }

    private fun serveRequest(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        Log.d(TAG, "Incoming ${method.name} $uri from ${session.remoteIpAddress}")

        return when {
            uri == "/api/localsend/v2/info" && method == Method.GET -> handleInfo()
            uri == "/api/localsend/v2/register" && method == Method.POST -> handleRegister(session)
            uri == "/api/localsend/v2/prepare-upload" && method == Method.POST -> handlePrepareUpload(session)
            uri.startsWith("/api/localsend/v2/upload") && method == Method.POST -> handleUpload(session)
            uri.startsWith("/api/localsend/v2/cancel") && method == Method.POST -> handleCancel(session)
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
        }
    }

    private fun handleInfo(): Response {
        val localDevice = discovery.getLocalDevice()
        val json = gson.toJson(localDevice)
        return newFixedLengthResponse(Response.Status.OK, "application/json", json)
    }

    private fun handleRegister(session: IHTTPSession): Response {
        try {
            val body = getBodyString(session)
            val remoteDevice = gson.fromJson(body, DeviceDto::class.java)

            if (remoteDevice != null) {
                val remoteIp = session.remoteIpAddress ?: "unknown"
                discovery.registerPeer(
                    NearbyPeer(
                        ip = remoteIp,
                        port = remoteDevice.port,
                        alias = remoteDevice.alias,
                        deviceModel = remoteDevice.deviceModel,
                        deviceType = remoteDevice.deviceType ?: "mobile",
                        protocol = remoteDevice.protocol,
                        fingerprint = remoteDevice.fingerprint,
                        lastSeenMs = System.currentTimeMillis()
                    )
                )
            }

            val reply = discovery.getLocalDevice().copy(announce = false)
            return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(reply))
        } catch (e: Exception) {
            Log.e(TAG, "Error handling register", e)
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Invalid body")
        }
    }

    private fun handlePrepareUpload(session: IHTTPSession): Response {
        try {
            val body = getBodyString(session)
            val request = gson.fromJson(body, PrepareUploadRequestDto::class.java)
                ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Invalid JSON")

            val sessionId = UUID.randomUUID().toString()
            val tokens = request.files.mapValues { UUID.randomUUID().toString() }
            val decisionChannel = CompletableDeferred<Boolean>()

            val transferFiles = request.files.values.map { fileDto ->
                TransferFileItem(
                    fileId = fileDto.id,
                    token = tokens[fileDto.id],
                    fileName = fileDto.fileName,
                    size = fileDto.size,
                    fileType = fileDto.fileType
                )
            }
            val totalBytes = transferFiles.sumOf { it.size }

            val incomingRequest = IncomingTransferRequest(
                sessionId = sessionId,
                sender = request.info,
                senderIp = session.remoteIpAddress ?: "unknown",
                files = transferFiles,
                totalBytes = totalBytes,
                decisionChannel = decisionChannel
            )

            val prefs = context.getSharedPreferences("sift_prefs", Context.MODE_PRIVATE)
            val askBeforeReceiving = prefs.getBoolean("nearby_ask_before_receiving", true)

            val accepted = if (askBeforeReceiving) {
                // Notify UI to display incoming confirmation bottom sheet
                onIncomingTransferRequest(incomingRequest)

                // Wait for user confirmation (or timeout after 60s)
                runBlocking {
                    try {
                        withTimeout(DECISION_TIMEOUT_MS) {
                            decisionChannel.await()
                        }
                    } catch (e: TimeoutCancellationException) {
                        false
                    }
                }
            } else {
                // Automatically accept without confirmation popup
                NearbyShareRepository.startIncomingDirectly(incomingRequest)
                true
            }

            if (!accepted) {
                return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Declined by user")
            }

            // Record active session
            activeSessions[sessionId] = ActiveSession(
                sessionId = sessionId,
                sender = request.info,
                tokens = tokens,
                fileDtos = request.files
            )

            val responseBody = PrepareUploadResponseDto(
                sessionId = sessionId,
                files = tokens
            )
            return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(responseBody))
        } catch (e: Exception) {
            Log.e(TAG, "Error in prepare-upload", e)
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.message ?: "Server error")
        }
    }

    private fun handleUpload(session: IHTTPSession): Response {
        val params = session.parameters
        val sessionId = params["sessionId"]?.firstOrNull()
        val fileId = params["fileId"]?.firstOrNull()
        val token = params["token"]?.firstOrNull()

        if (sessionId == null || fileId == null || token == null) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing parameters")
        }

        val activeSession = activeSessions[sessionId]
            ?: return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Invalid or expired session")

        if (activeSession.isCancelled) {
            return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Session cancelled")
        }

        val expectedToken = activeSession.tokens[fileId]
        if (expectedToken != token) {
            return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Invalid token")
        }

        val fileDto = activeSession.fileDtos[fileId]
            ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Unknown fileId")

        // Prepare destination directory and unique filename
        val targetDir = getReceivedDirectory()
        val targetFile = getUniqueFile(targetDir, fileDto.fileName)
        activeSession.currentReceivingFile = targetFile

        val contentLengthHeader = session.headers["content-length"]
        val contentLength = contentLengthHeader?.toLongOrNull() ?: fileDto.size

        try {
            val inputStream = session.inputStream
            val md = if (!fileDto.sha256.isNullOrBlank()) MessageDigest.getInstance("SHA-256") else null

            FileOutputStream(targetFile).use { outputStream ->
                val buffer = ByteArray(BUFFER_SIZE)
                var remaining = contentLength
                var bytesWritten = 0L

                while (remaining > 0) {
                    if (activeSession.isCancelled) {
                        targetFile.delete()
                        return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Transfer cancelled")
                    }

                    val toRead = if (remaining > buffer.size) buffer.size else remaining.toInt()
                    val read = inputStream.read(buffer, 0, toRead)
                    if (read == -1) break

                    outputStream.write(buffer, 0, read)
                    md?.update(buffer, 0, read)
                    bytesWritten += read
                    remaining -= read

                    onProgressUpdate(sessionId, fileId, bytesWritten, contentLength)
                }
            }

            // Verify SHA-256 if supplied
            if (md != null && !fileDto.sha256.isNullOrBlank()) {
                val computedHash = md.digest().joinToString("") { "%02x".format(it) }
                if (!computedHash.equals(fileDto.sha256, ignoreCase = true)) {
                    targetFile.delete()
                    return newFixedLengthResponse(Response.Status.lookup(422) ?: Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Checksum mismatch")
                }
            }

            // Add to session saved files
            activeSession.savedFiles.add(targetFile)

            // Trigger MediaScanner so files immediately appear in media store
            MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), null, null)

            // Check if all files in session are finished
            if (activeSession.savedFiles.size >= activeSession.fileDtos.size) {
                activeSessions.remove(sessionId)
                onTransferComplete(sessionId, activeSession.sender, activeSession.savedFiles.toList())
            }

            return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving uploaded file: ${targetFile.name}", e)
            targetFile.delete()
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.message ?: "Upload failed")
        } finally {
            activeSession.currentReceivingFile = null
        }
    }

    private fun handleCancel(session: IHTTPSession): Response {
        val sessionId = session.parameters["sessionId"]?.firstOrNull()
        if (sessionId != null) {
            val activeSession = activeSessions.remove(sessionId)
            if (activeSession != null) {
                activeSession.isCancelled = true
                activeSession.currentReceivingFile?.delete()
                onTransferCancelled(sessionId)
            }
        }
        return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "")
    }

    private fun getBodyString(session: IHTTPSession): String {
        val contentLength = session.headers["content-length"]?.toIntOrNull() ?: 0
        val buffer = ByteArray(contentLength)
        var totalRead = 0
        val inputStream: InputStream = session.inputStream
        while (totalRead < contentLength) {
            val read = inputStream.read(buffer, totalRead, contentLength - totalRead)
            if (read == -1) break
            totalRead += read
        }
        return String(buffer, 0, totalRead, Charsets.UTF_8)
    }

    private fun getReceivedDirectory(): File {
        val prefs = context.getSharedPreferences("sift_prefs", Context.MODE_PRIVATE)
        val customPath = prefs.getString("nearby_receive_path", null)
        if (!customPath.isNullOrBlank()) {
            val customDir = File(customPath)
            if (customDir.exists() || customDir.mkdirs()) {
                return customDir
            }
        }
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val receivedDir = File(downloads, "RippleReceived")
        if (!receivedDir.exists()) {
            receivedDir.mkdirs()
        }
        return receivedDir
    }

    private fun getUniqueFile(directory: File, fileName: String): File {
        var file = File(directory, fileName)
        if (!file.exists()) return file

        val nameWithoutExt = fileName.substringBeforeLast(".", fileName)
        val ext = if (fileName.contains(".")) ".${fileName.substringAfterLast(".")}" else ""
        var count = 1

        while (file.exists()) {
            file = File(directory, "$nameWithoutExt ($count)$ext")
            count++
        }
        return file
    }
}
