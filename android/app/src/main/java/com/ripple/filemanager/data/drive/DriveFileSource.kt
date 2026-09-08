package com.ripple.filemanager.data.drive

import com.ripple.filemanager.FileItem
import com.ripple.filemanager.FileRepository
import com.ripple.filemanager.data.core.FileSource
import java.io.File

/**
 * Wraps Google Drive access (which lives inside FileRepository, since
 * driveService is private there) so it can be used anywhere a generic
 * FileSource is expected — e.g. in the dual-pane screen.
 *
 * Unlike SMB/FTP/SFTP/WebDAV, Drive doesn't have real hierarchical paths —
 * only IDs. Locations look like "drive" (root) or "drive_id:<id>" (a folder).
 * Individual files/folders are referenced as "drive_id:<id>" once listed.
 *
 * createFolder() convention: pass "<parentLocation>/<newFolderName>",
 * e.g. "drive/New Folder" or "drive_id:abc123/New Folder".
 */
class DriveFileSource(
    private val repository: FileRepository
) : FileSource {

    override val sourceLabel: String = "Google Drive"

    override suspend fun listFiles(path: String): Result<List<FileItem>> {
        val location = path.ifBlank { "drive" }
        return try {
            Result.success(repository.getFiles(location))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadToLocal(path: String, destination: File): Result<File> {
        if (!path.startsWith("drive_id:")) {
            return Result.failure(IllegalArgumentException("Expected a drive_id: path, got: $path"))
        }
        val fileId = path.removePrefix("drive_id:")
        return repository.downloadDriveFileToLocal(fileId, destination)
    }

    override suspend fun uploadFromLocal(localFile: File, destinationPath: String): Result<Unit> {
        // destinationPath here is the target Drive location: "drive" or "drive_id:<id>"
        return repository.uploadLocalFileToDrive(localFile, destinationPath)
    }

    override suspend fun delete(path: String, isDirectory: Boolean): Result<Unit> {
        if (!path.startsWith("drive_id:")) {
            return Result.failure(IllegalArgumentException("Expected a drive_id: path, got: $path"))
        }
        val fileId = path.removePrefix("drive_id:")
        val errorMessage = repository.deleteDriveFile(fileId)
        return if (errorMessage == null) Result.success(Unit) else Result.failure(Exception(errorMessage))
    }

    override suspend fun createFolder(path: String): Result<Unit> {
        // Convention: "<parentLocation>/<newFolderName>"
        val parentLocation = path.substringBeforeLast("/", "drive")
        val name = path.substringAfterLast("/")
        val success = repository.createFolder(parentLocation, name)
        return if (success) Result.success(Unit) else Result.failure(Exception("Failed to create Drive folder"))
    }
}
