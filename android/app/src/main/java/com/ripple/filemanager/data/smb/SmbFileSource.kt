package com.ripple.filemanager.data.smb

import com.ripple.filemanager.FileItem
import com.ripple.filemanager.data.core.FileSource
import java.io.File

/**
 * Wraps the existing SmbStorageProvider (unchanged) so it can be used
 * anywhere a generic FileSource is expected — e.g. in the dual-pane screen.
 *
 * One SmbFileSource = one active connection. Create a new instance per
 * connection the same way you already create SmbStorageProvider instances.
 */
class SmbFileSource(
    private val provider: SmbStorageProvider,
    private val connectionId: String
) : FileSource {

    override val sourceLabel: String = "SMB"

    override suspend fun listFiles(path: String): Result<List<FileItem>> {
        return provider.listFiles(path, connectionId)
    }

    override suspend fun downloadToLocal(path: String, destination: File): Result<File> {
        return provider.download(path, destination)
    }

    override suspend fun uploadFromLocal(localFile: File, destinationPath: String): Result<Unit> {
        // destinationPath is the target FOLDER; build the full remote path here.
        val folderPath = destinationPath.substringAfter(":")
        val fullPath = if (folderPath.endsWith("\\") || folderPath.isEmpty()) "$folderPath${localFile.name}" else "$folderPath\\${localFile.name}"
        return provider.upload(localFile, "smb_${connectionId}:$fullPath")
    }

    override suspend fun delete(path: String, isDirectory: Boolean): Result<Unit> {
        // SmbStorageProvider already detects folder vs file itself, so the
        // isDirectory hint isn't needed here — it's only used by providers
        // (like FTP) that require it upfront.
        return provider.delete(path)
    }

    override suspend fun createFolder(path: String): Result<Unit> {
        return provider.createFolder(path)
    }
}
