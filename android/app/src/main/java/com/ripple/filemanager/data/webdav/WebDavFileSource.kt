package com.ripple.filemanager.data.webdav

import com.ripple.filemanager.FileItem
import com.ripple.filemanager.data.core.FileSource
import java.io.File

/**
 * Wraps the existing WebDavStorageProvider (after the listFiles/createFolder
 * path-building fix) so it can be used anywhere a generic FileSource is
 * expected — e.g. in the dual-pane screen.
 *
 * One WebDavFileSource = one active connection (WebDAV or Nextcloud).
 * isNextcloud controls which prefix ("webdav_" vs "nextcloud_") this
 * source uses, matching the two connection types in your nav drawer.
 */
class WebDavFileSource(
    private val provider: WebDavStorageProvider,
    private val connectionId: String,
    private val isNextcloud: Boolean
) : FileSource {

    override val sourceLabel: String = if (isNextcloud) "Nextcloud" else "WebDAV"

    private val prefix = if (isNextcloud) "nextcloud_${connectionId}:" else "webdav_${connectionId}:"

    override suspend fun listFiles(path: String): Result<List<FileItem>> {
        // WebDavStorageProvider.listFiles already expects the full prefixed
        // path (it strips the scheme prefix internally) and returns
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
        // WebDavStorageProvider's delete doesn't need an isDirectory hint.
        val remotePath = path.removePrefix(prefix)
        return provider.delete(remotePath)
    }

    override suspend fun createFolder(path: String): Result<Unit> {
        val remotePath = path.removePrefix(prefix)
        val parentPath = remotePath.substringBeforeLast("/", "/")
        val name = remotePath.substringAfterLast("/")
        return provider.createFolder(parentPath, name)
    }
}
