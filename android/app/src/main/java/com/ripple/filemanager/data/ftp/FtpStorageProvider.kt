package com.ripple.filemanager.data.ftp

import com.ripple.filemanager.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPReply
import org.apache.commons.net.ftp.FTPSClient
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Plain class, no interface — matches SmbStorageProvider's shape:
 * connect / disconnect / listFiles / download / upload / delete / createFolder.
 */
class FtpStorageProvider {

    private var client: FTPClient? = null
    private var currentConnectionId: String? = null
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    suspend fun connect(connection: FtpConnection, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            disconnect()

            val ftpClient: FTPClient = when {
                connection.useImplicitFtps -> FTPSClient(true)
                connection.useFtps -> FTPSClient(false)
                else -> FTPClient()
            }

            ftpClient.connectTimeout = 10_000
            ftpClient.connect(connection.host, connection.port)

            val replyCode = ftpClient.replyCode
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                ftpClient.disconnect()
                return@withContext Result.failure(Exception("Server refused connection ($replyCode)"))
            }

            val loggedIn = ftpClient.login(connection.username, password)
            if (!loggedIn) {
                ftpClient.disconnect()
                return@withContext Result.failure(Exception("Authentication failed"))
            }

            if (connection.passiveMode) {
                ftpClient.enterLocalPassiveMode()
            } else {
                ftpClient.enterLocalActiveMode()
            }
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
            ftpClient.controlKeepAliveTimeout = 15

            client = ftpClient
            currentConnectionId = connection.id
            Result.success(Unit)
        } catch (e: Exception) {
            disconnect()
            Result.failure(e)
        }
    }

    fun disconnect() {
        try {
            client?.let {
                if (it.isConnected) {
                    it.logout()
                    it.disconnect()
                }
            }
        } catch (_: Exception) {
            // best-effort cleanup
        } finally {
            client = null
            currentConnectionId = null
        }
    }

    suspend fun listFiles(path: String, connectionId: String): Result<List<FileItem>> = withContext(Dispatchers.IO) {
        try {
            if (client == null || currentConnectionId != connectionId) {
                return@withContext Result.failure(Exception("Not connected"))
            }

            // Normalize path: strip the "ftp_<id>:" prefix to get the remote path
            val remotePath = path.removePrefix("ftp_${connectionId}:").let { if (it.isEmpty()) "/" else it }
            val entries: Array<FTPFile> = client!!.listFiles(remotePath)

            val items = entries
                .filter { it.name != "." && it.name != ".." }
                .map { ftpFile ->
                    val name = ftpFile.name
                    val isDir = ftpFile.isDirectory
                    val sizeBytes = ftpFile.size
                    val lastModified = ftpFile.timestamp?.timeInMillis ?: 0L

                    val fullPath = if (remotePath.endsWith("/")) "$remotePath$name" else "$remotePath/$name"
                    val prefixPath = "ftp_${connectionId}:$fullPath"

                    val type = if (isDir) "folder" else {
                        val ext = name.substringAfterLast('.', "")
                        when (ext.lowercase()) {
                            "jpg", "jpeg", "png", "gif", "webp" -> "image"
                            "mp4", "mkv", "avi", "mov" -> "video"
                            "mp3", "wav", "flac", "m4a", "ogg" -> "music"
                            "pdf", "txt", "json", "doc", "docx" -> "document"
                            "zip", "rar", "7z", "tar", "gz" -> "archive"
                            "apk" -> "apk"
                            else -> "unknown"
                        }
                    }

                    FileItem(
                        id = fullPath.hashCode(),
                        path = prefixPath,
                        name = name,
                        type = type,
                        kind = if (isDir) "Folder" else type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                        size = if (isDir) "" else formatSize(sizeBytes),
                        changed = dateFormat.format(Date(lastModified)),
                        owner = "FTP",
                        sizeBytes = sizeBytes,
                        lastModified = lastModified
                    )
                }
                .sortedWith(compareBy({ it.type != "folder" }, { it.name.lowercase() }))

            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun download(remotePath: String, localFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        val ftpClient = client ?: return@withContext Result.failure(IllegalStateException("Not connected"))
        try {
            BufferedOutputStream(FileOutputStream(localFile)).use { out ->
                val ok = ftpClient.retrieveFile(remotePath, out)
                if (!ok) return@withContext Result.failure(Exception("Download failed: ${ftpClient.replyString}"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upload(localFile: File, remotePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        val ftpClient = client ?: return@withContext Result.failure(IllegalStateException("Not connected"))
        try {
            BufferedInputStream(FileInputStream(localFile)).use { input ->
                val ok = ftpClient.storeFile(remotePath, input)
                if (!ok) return@withContext Result.failure(Exception("Upload failed: ${ftpClient.replyString}"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun delete(remotePath: String, isDirectory: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val ftpClient = client ?: return@withContext Result.failure(IllegalStateException("Not connected"))
        try {
            val ok = if (isDirectory) ftpClient.removeDirectory(remotePath) else ftpClient.deleteFile(remotePath)
            if (!ok) return@withContext Result.failure(Exception("Delete failed: ${ftpClient.replyString}"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFolder(path: String, name: String): Result<Unit> = withContext(Dispatchers.IO) {
        val ftpClient = client ?: return@withContext Result.failure(IllegalStateException("Not connected"))
        try {
            val target = if (path.endsWith("/")) "$path$name" else "$path/$name"
            val ok = ftpClient.makeDirectory(target)
            if (!ok) return@withContext Result.failure(Exception("Create folder failed: ${ftpClient.replyString}"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(Locale.getDefault(), "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
