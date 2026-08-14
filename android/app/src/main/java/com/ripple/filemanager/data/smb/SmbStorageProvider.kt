package com.ripple.filemanager.data.smb

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File
import com.ripple.filemanager.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.hierynomus.smbj.SmbConfig
import java.util.concurrent.TimeUnit
import java.io.FileOutputStream
import java.io.FileInputStream

class SmbStorageProvider {

    private var smbClient: SMBClient? = null
    private var session: Session? = null
    private var diskShare: DiskShare? = null
    private var currentConnectionId: String? = null
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    suspend fun connect(connection: SmbConnection, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            disconnect()
            
            val config = SmbConfig.builder()
                .withTimeout(15, TimeUnit.SECONDS)
                .withSoTimeout(15, TimeUnit.SECONDS)
                .build()
                
            smbClient = SMBClient(config)
            
            val connectionResult = smbClient!!.connect(connection.host, connection.port)
            
            val authContext = AuthenticationContext(
                connection.username,
                password.toCharArray(),
                connection.domain
            )
            
            session = connectionResult.authenticate(authContext)
            diskShare = session!!.connectShare(connection.shareName) as DiskShare
            currentConnectionId = connection.id
            
            Result.success(Unit)
        } catch (e: Exception) {
            disconnect()
            Result.failure(e)
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            diskShare?.close()
        } catch (e: Exception) {}
        try {
            session?.close()
        } catch (e: Exception) {}
        try {
            smbClient?.close()
        } catch (e: Exception) {}
        
        diskShare = null
        session = null
        smbClient = null
        currentConnectionId = null
    }

    suspend fun listFiles(path: String, connectionId: String): Result<List<FileItem>> = withContext(Dispatchers.IO) {
        try {
            if (diskShare == null || currentConnectionId != connectionId) {
                return@withContext Result.failure(Exception("Not connected"))
            }

            // Normalize path (empty string for root)
            val sharePath = path.removePrefix("smb_${connectionId}:").removePrefix("/")
            val files = diskShare!!.list(if (sharePath.isEmpty()) "" else sharePath)
            
            val items = files.mapNotNull { fileInfo ->
                val name = fileInfo.fileName
                if (name == "." || name == "..") return@mapNotNull null
                
                val isDir = com.hierynomus.msfscc.FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value and fileInfo.fileAttributes != 0L
                val sizeBytes = fileInfo.endOfFile
                val lastModified = fileInfo.changeTime.toEpochMillis()
                
                val fullPath = if (sharePath.isEmpty()) name else "$sharePath\\$name"
                val prefixPath = "smb_${connectionId}:$fullPath"
                
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
                    owner = "SMB",
                    sizeBytes = sizeBytes,
                    lastModified = lastModified
                )
            }.sortedWith(compareBy({ it.type != "folder" }, { it.name.lowercase() }))
            
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun download(path: String, destination: java.io.File): Result<java.io.File> = withContext(Dispatchers.IO) {
        try {
            if (diskShare == null) return@withContext Result.failure(Exception("Not connected"))
            
            val sharePath = path.substringAfter(":")
            val file = diskShare!!.openFile(
                sharePath,
                setOf(AccessMask.GENERIC_READ),
                setOf(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                setOf(SMB2ShareAccess.FILE_SHARE_READ),
                SMB2CreateDisposition.FILE_OPEN,
                setOf()
            )
            
            file.use { smbFile ->
                FileOutputStream(destination).use { out ->
                    smbFile.read(out)
                }
            }
            Result.success(destination)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upload(localFile: java.io.File, destinationPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (diskShare == null) return@withContext Result.failure(Exception("Not connected"))
            
            val sharePath = destinationPath.substringAfter(":")
            val file = diskShare!!.openFile(
                sharePath,
                setOf(AccessMask.GENERIC_WRITE),
                setOf(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                setOf(SMB2ShareAccess.FILE_SHARE_WRITE),
                SMB2CreateDisposition.FILE_OVERWRITE_IF,
                setOf()
            )
            
            file.use { smbFile ->
                FileInputStream(localFile).use { input ->
                    val buffer = ByteArray(65536)
                    var offset = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        smbFile.write(buffer, offset, read, 0)
                        offset += read
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun delete(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (diskShare == null) return@withContext Result.failure(Exception("Not connected"))
            val sharePath = path.substringAfter(":")
            
            val isDir = try {
                diskShare!!.getFileInformation(sharePath).standardInformation.isDirectory
            } catch (e: Exception) { false }
            
            if (isDir) {
                diskShare!!.rmdir(sharePath, true)
            } else {
                diskShare!!.rm(sharePath)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFolder(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (diskShare == null) return@withContext Result.failure(Exception("Not connected"))
            val sharePath = path.substringAfter(":")
            diskShare!!.mkdir(sharePath)
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
