package com.ripple.filemanager.data.local

import com.ripple.filemanager.FileItem
import com.ripple.filemanager.FileRepository
import com.ripple.filemanager.data.core.FileSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Wraps local device storage (via FileRepository) so it can be used
 * anywhere a generic FileSource is expected — e.g. in the dual-pane screen.
 *
 * Unlike the network/cloud sources, Local is both the "source" and the
 * universal staging point every cross-source transfer already passes
 * through, so downloadToLocal/uploadFromLocal here are just plain file
 * copies on the same device.
 *
 * Path convention: same as the rest of the app — a raw absolute path
 * (e.g. "/storage/emulated/0/Download"), or the "home" alias for the
 * root of device storage.
 */
class LocalFileSource(
    private val repository: FileRepository
) : FileSource {

    override val sourceLabel: String = "Local"

    override suspend fun listFiles(path: String): Result<List<FileItem>> {
        val location = path.ifBlank { "home" }
        return try {
            Result.success(repository.getFiles(location))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadToLocal(path: String, destination: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            val source = File(path)
            if (!source.exists()) {
                return@withContext Result.failure(java.io.FileNotFoundException(path))
            }
            if (source.isDirectory) {
                source.copyRecursively(destination, overwrite = true)
            } else {
                source.copyTo(destination, overwrite = true)
            }
            Result.success(destination)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadFromLocal(localFile: File, destinationPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // destinationPath is the target FOLDER; place the file inside it by name.
            val destFolder = File(destinationPath)
            val destination = File(destFolder, localFile.name)
            if (localFile.isDirectory) {
                localFile.copyRecursively(destination, overwrite = true)
            } else {
                localFile.copyTo(destination, overwrite = true)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun delete(path: String, isDirectory: Boolean): Result<Unit> {
        // deleteRestrictedPath already handles both ordinary local files
        // and Shizuku-protected ones (Android/data, Android/obb) in one call.
        return try {
            val success = repository.deleteRestrictedPath(path)
            if (success) Result.success(Unit) else Result.failure(Exception("Delete failed for $path"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createFolder(path: String): Result<Unit> {
        val parentPath = path.substringBeforeLast("/", "home")
        val name = path.substringAfterLast("/")
        return try {
            val success = repository.createFolder(parentPath, name)
            if (success) Result.success(Unit) else Result.failure(Exception("Failed to create folder at $path"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
