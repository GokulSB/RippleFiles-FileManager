package nz.mega.sdk

import com.ripple.filemanager.FileItem
import com.ripple.filemanager.data.core.FileSource
import java.io.File

/**
 * Wraps the existing MegaClient so it can be used anywhere a generic
 * FileSource is expected — e.g. in the dual-pane screen.
 *
 * Locations follow the same style as Drive: "mega" (root) or
 * "mega_id:<handle>" (a folder). Individual items are "mega_id:<handle>".
 *
 * createFolder() convention (matches DriveFileSource): pass
 * "<parentLocation>/<newFolderName>", e.g. "mega/New Folder" or
 * "mega_id:12345/New Folder".
 */
class MegaFileSource(
    private val client: MegaClient
) : FileSource {

    override val sourceLabel: String = "MEGA"

    override suspend fun listFiles(path: String): Result<List<FileItem>> {
        return try {
            val folderId = if (path == "mega" || path.isBlank()) null else path.removePrefix("mega_id:")
            Result.success(client.getChildren(folderId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadToLocal(path: String, destination: File): Result<File> {
        if (!path.startsWith("mega_id:")) {
            return Result.failure(IllegalArgumentException("Expected a mega_id: path, got: $path"))
        }
        val handle = path.removePrefix("mega_id:")
        return try {
            val success = client.downloadFile(handle, destination.absolutePath)
            if (success) Result.success(destination) else Result.failure(Exception("MEGA download failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun delete(path: String, isDirectory: Boolean): Result<Unit> {
        if (!path.startsWith("mega_id:")) {
            return Result.failure(IllegalArgumentException("Expected a mega_id: path, got: $path"))
        }
        val handle = path.removePrefix("mega_id:")
        return try {
            val success = client.deleteNode(handle)
            if (success) Result.success(Unit) else Result.failure(Exception("MEGA delete failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadFromLocal(localFile: File, destinationPath: String): Result<Unit> {
        val parentHandle = if (destinationPath == "mega" || destinationPath.isBlank()) {
            null
        } else {
            destinationPath.removePrefix("mega_id:")
        }
        return try {
            val success = client.uploadFile(localFile.absolutePath, parentHandle)
            if (success) Result.success(Unit) else Result.failure(Exception("MEGA upload failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createFolder(path: String): Result<Unit> {
        // Convention (matches DriveFileSource): "<parentLocation>/<newFolderName>"
        val parentLocation = path.substringBeforeLast("/", "mega")
        val name = path.substringAfterLast("/")
        val parentHandle = if (parentLocation == "mega" || parentLocation.isBlank()) {
            null
        } else {
            parentLocation.removePrefix("mega_id:")
        }
        return try {
            val success = client.createFolder(name, parentHandle)
            if (success) Result.success(Unit) else Result.failure(Exception("MEGA folder creation failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
