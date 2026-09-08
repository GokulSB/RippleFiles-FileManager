package com.ripple.filemanager.data.ftp

import com.ripple.filemanager.FileItem
import com.ripple.filemanager.data.core.FileSource
import java.io.File

/**
 * Wraps the existing FtpStorageProvider (unchanged) so it can be used
 * anywhere a generic FileSource is expected — e.g. in the dual-pane screen.
 *
 * One FtpFileSource = one active connection, matching how you already
 * create one FtpStorageProvider per connection.
 *
 * Path convention: paths coming in from the rest of the app look like
 * "ftp_<connectionId>:/some/remote/path" (matching the SMB prefix pattern).
 * This adapter strips that prefix before calling the underlying provider,
 * and adds it back when building FileItems for the UI.
 */
class FtpFileSource(
    private val provider: FtpStorageProvider,
    private val connectionId: String
) : FileSource {

    override val sourceLabel: String = "FTP"

    private val prefix = "ftp_${connectionId}:"

    private fun stripPrefix(path: String): String {
        val remote = path.removePrefix(prefix)
        return if (remote.isEmpty()) "/" else remote
    }

    override suspend fun listFiles(path: String): Result<List<FileItem>> =
        provider.listFiles(path, connectionId)

    override suspend fun downloadToLocal(path: String, destination: File): Result<File> =
        provider.download(stripPrefix(path), destination).map { destination }

    override suspend fun uploadFromLocal(localFile: File, destinationPath: String): Result<Unit> {
        val folderPath = stripPrefix(destinationPath)
        val remotePath = if (folderPath.endsWith("/") || folderPath.isEmpty()) "$folderPath${localFile.name}" else "$folderPath/${localFile.name}"
        return provider.upload(localFile, remotePath)
    }

    override suspend fun delete(path: String, isDirectory: Boolean): Result<Unit> =
        provider.delete(stripPrefix(path), isDirectory)

    override suspend fun createFolder(path: String): Result<Unit> {
        val remotePath = stripPrefix(path)
        val parentPath = remotePath.substringBeforeLast("/", "/")
        val name = remotePath.substringAfterLast("/")
        return provider.createFolder(parentPath, name)
    }
}
