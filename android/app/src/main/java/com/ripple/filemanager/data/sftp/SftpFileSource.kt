package com.ripple.filemanager.data.sftp

import com.ripple.filemanager.FileItem
import com.ripple.filemanager.data.core.FileSource
import java.io.File

/**
 * Wraps the existing SftpStorageProvider (unchanged) so it can be used
 * anywhere a generic FileSource is expected — e.g. in the dual-pane screen.
 *
 * One SftpFileSource = one active connection, matching how you already
 * create one SftpStorageProvider per connection.
 */
class SftpFileSource(
    private val provider: SftpStorageProvider,
    private val connectionId: String
) : FileSource {

    override val sourceLabel: String = "SFTP"

    private val prefix = "sftp_${connectionId}:"

    override suspend fun listFiles(path: String): Result<List<FileItem>> {
        // SftpStorageProvider.listFiles already expects the full prefixed
        // path (it strips "sftp_<id>:" internally) and already returns
        // ready-to-use FileItems, so no extra conversion is needed here.
        return provider.listFiles(path, connectionId)
    }

    override suspend fun downloadToLocal(path: String, destination: File): Result<File> {
        val remotePath = path.removePrefix(prefix)
        val result = provider.download(remotePath, destination)
        return result.map { destination }
    }

    override suspend fun uploadFromLocal(localFile: File, destinationPath: String): Result<Unit> {
        // destinationPath is the target FOLDER; build the full remote path here.
        val folderPath = destinationPath.removePrefix(prefix)
        val remotePath = if (folderPath.endsWith("/") || folderPath.isEmpty()) "$folderPath${localFile.name}" else "$folderPath/${localFile.name}"
        return provider.upload(localFile, remotePath)
    }

    override suspend fun delete(path: String, isDirectory: Boolean): Result<Unit> {
        val remotePath = path.removePrefix(prefix)
        return provider.delete(remotePath, isDirectory)
    }

    override suspend fun createFolder(path: String): Result<Unit> {
        val remotePath = path.removePrefix(prefix)
        val parentPath = remotePath.substringBeforeLast("/", "/")
        val name = remotePath.substringAfterLast("/")
        return provider.createFolder(parentPath, name)
    }
}
