package com.ripple.filemanager.data.webdav

import com.ripple.filemanager.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WebDavStorageProvider {

    private var client: WebDavClient? = null
    private var currentConnectionId: String? = null
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    suspend fun connect(connection: WebDavConnection, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            disconnect()
            
            val davClient = WebDavClient(connection, password)
            // The connection logic is handled by sardine on-demand, but we can test it to verify auth
            val isValid = davClient.testConnection()
            if (!isValid) {
                return@withContext Result.failure(Exception("Authentication failed"))
            }

            client = davClient
            currentConnectionId = connection.id
            Result.success(Unit)
        } catch (e: Exception) {
            disconnect()
            Result.failure(e)
        }
    }

    fun disconnect() {
        client = null
        currentConnectionId = null
    }

    suspend fun listFiles(path: String, connectionId: String): Result<List<FileItem>> = withContext(Dispatchers.IO) {
        try {
            if (client == null || currentConnectionId != connectionId) {
                return@withContext Result.failure(Exception("Not connected"))
            }

            // Normalize path: strip the "webdav_<id>:" prefix to get the remote path
            val schemePrefix = if (path.startsWith("nextcloud_")) "nextcloud_" else "webdav_"
            val prefixToRemove = "${schemePrefix}${connectionId}:"
            var remotePath = path.removePrefix(prefixToRemove)
            if (remotePath.isEmpty()) remotePath = "/"

            val entries = client!!.list(remotePath)

            val items = entries
                .map { davEntry ->
                    val name = davEntry.name
                    val isDir = davEntry.isDirectory
                    val sizeBytes = davEntry.sizeBytes
                    val lastModified = davEntry.lastModifiedMillis

                    val fullPath = if (remotePath.endsWith("/")) "" else "/"
                    val prefixPath = ":"

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
                        owner = if (schemePrefix == "nextcloud_") "Nextcloud" else "WebDAV",
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
        val davClient = client ?: return@withContext Result.failure(IllegalStateException("Not connected"))
        try {
            davClient.download(remotePath, localFile)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upload(localFile: File, remotePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        val davClient = client ?: return@withContext Result.failure(IllegalStateException("Not connected"))
        try {
            davClient.upload(localFile, remotePath)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun delete(remotePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        val davClient = client ?: return@withContext Result.failure(IllegalStateException("Not connected"))
        try {
            davClient.delete(remotePath)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFolder(path: String, name: String): Result<Unit> = withContext(Dispatchers.IO) {
        val davClient = client ?: return@withContext Result.failure(IllegalStateException("Not connected"))
        try {
            val target = if (path.endsWith("/")) "" else "/"
            davClient.createFolder(target)
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
