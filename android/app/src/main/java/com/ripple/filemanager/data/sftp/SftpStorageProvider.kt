package com.ripple.filemanager.data.sftp

import com.ripple.filemanager.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.FingerprintVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Plain class, no interface — matches SmbStorageProvider / FtpStorageProvider shape:
 * connect / disconnect / listFiles / download / upload / delete / createFolder.
 *
 * Host key handling: if the saved SftpConnection has a pinned hostKeyFingerprint, it's
 * verified strictly. Otherwise this connects on trust-first-use and returns the
 * fingerprint via connect()'s Result so the caller can persist it via SftpStore.
 */
class SftpStorageProvider {

    private var ssh: SSHClient? = null
    private var sftp: SFTPClient? = null
    private var currentConnectionId: String? = null
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    data class ConnectResult(val observedFingerprint: String)

    suspend fun connect(connection: SftpConnection, password: String): Result<ConnectResult> = withContext(Dispatchers.IO) {
        try {
            disconnect()

            val client = SSHClient()
            client.connectTimeout = 10_000

            var observedFingerprint = connection.hostKeyFingerprint ?: ""
            if (connection.hostKeyFingerprint != null) {
                client.addHostKeyVerifier(FingerprintVerifier.getInstance(connection.hostKeyFingerprint))
            } else {
                // trust-on-first-use: accept and capture fingerprint for pinning
                client.addHostKeyVerifier(object : net.schmizz.sshj.transport.verification.HostKeyVerifier {
                    override fun verify(hostname: String?, port: Int, key: java.security.PublicKey?): Boolean {
                        if (key != null) {
                            observedFingerprint = net.schmizz.sshj.common.SecurityUtils.getFingerprint(key)
                        }
                        return true
                    }
                    override fun findExistingAlgorithms(hostname: String?, port: Int): MutableList<String> {
                        return mutableListOf()
                    }
                })
            }

            client.connect(connection.host, connection.port)

            when (connection.authType) {
                SftpAuthType.PASSWORD -> {
                    client.authPassword(connection.username, password)
                }
                SftpAuthType.KEY -> {
                    val keyProvider: KeyProvider = if (connection.privateKeyPassphrase.isNotBlank()) {
                        client.loadKeys(
                            connection.privateKeyPem,
                            null,
                            net.schmizz.sshj.userauth.password.PasswordUtils.createOneOff(
                                connection.privateKeyPassphrase.toCharArray()
                            )
                        )
                    } else {
                        client.loadKeys(connection.privateKeyPem, null, null)
                    }
                    client.authPublickey(connection.username, keyProvider)
                }
            }

            ssh = client
            sftp = client.newSFTPClient()
            currentConnectionId = connection.id
            Result.success(ConnectResult(observedFingerprint))
        } catch (e: Exception) {
            runCatching { ssh?.disconnect() }
            ssh = null
            sftp = null
            currentConnectionId = null
            Result.failure(e)
        }
    }

    fun disconnect() {
        try {
            sftp?.close()
            ssh?.disconnect()
        } catch (_: Exception) {
            // best-effort cleanup
        } finally {
            sftp = null
            ssh = null
            currentConnectionId = null
        }
    }

    suspend fun listFiles(path: String, connectionId: String): Result<List<FileItem>> = withContext(Dispatchers.IO) {
        try {
            if (sftp == null || currentConnectionId != connectionId) {
                return@withContext Result.failure(Exception("Not connected"))
            }

            // Normalize path: strip the "sftp_<id>:" prefix to get the remote path
            val remotePath = path.removePrefix("sftp_${connectionId}:").let { if (it.isEmpty()) "/" else it }
            val entries: List<RemoteResourceInfo> = sftp!!.ls(remotePath)

            val items = entries
                .filter { it.name != "." && it.name != ".." }
                .map { entry ->
                    val name = entry.name
                    val attrs = entry.attributes
                    val isDir = attrs.type == FileMode.Type.DIRECTORY
                    val sizeBytes = attrs.size
                    val lastModified = attrs.mtime * 1000L

                    val fullPath = if (remotePath.endsWith("/")) "$remotePath$name" else "$remotePath/$name"
                    val prefixPath = "sftp_${connectionId}:$fullPath"

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
                        owner = "SFTP",
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
        val client = sftp ?: return@withContext Result.failure(IllegalStateException("Not connected"))
        try {
            client.get(remotePath, localFile.absolutePath)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upload(localFile: File, remotePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        val client = sftp ?: return@withContext Result.failure(IllegalStateException("Not connected"))
        try {
            client.put(localFile.absolutePath, remotePath)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun delete(remotePath: String, isDirectory: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val client = sftp ?: return@withContext Result.failure(IllegalStateException("Not connected"))
        try {
            if (isDirectory) client.rmdir(remotePath) else client.rm(remotePath)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFolder(path: String, name: String): Result<Unit> = withContext(Dispatchers.IO) {
        val client = sftp ?: return@withContext Result.failure(IllegalStateException("Not connected"))
        try {
            val target = if (path.endsWith("/")) "$path$name" else "$path/$name"
            client.mkdir(target)
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
