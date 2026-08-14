package com.ripple.filemanager.archive

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.FileHeader
import net.lingala.zip4j.progress.ProgressMonitor
import java.io.File

class ZipArchiveHandler(private val context: Context) : ArchiveHandler {

    private suspend fun getSourceFile(source: Uri): File = withContext(Dispatchers.IO) {
        if (source.scheme == "file" || source.scheme == null) {
            File(source.path ?: source.toString())
        } else {
            // Need to copy to a temp file to use Zip4j
            val tempFile = File.createTempFile("archive_", ".zip", context.cacheDir)
            context.contentResolver.openInputStream(source)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw Exception("Cannot read source URI")
            tempFile
        }
    }

    override suspend fun listEntries(source: Uri): List<ArchiveEntryInfo> = withContext(Dispatchers.IO) {
        val file = getSourceFile(source)
        try {
            val zipFile = ZipFile(file)
            if (!zipFile.isValidZipFile) {
                throw Exception("Invalid ZIP file")
            }
            zipFile.fileHeaders.map { header ->
                ArchiveEntryInfo(
                    name = header.fileName,
                    sizeBytes = header.uncompressedSize,
                    isDirectory = header.isDirectory,
                    isEncrypted = header.isEncrypted
                )
            }
        } finally {
            if (source.scheme != "file" && source.scheme != null) {
                file.delete()
            }
        }
    }

    override suspend fun testPassword(source: Uri, password: String): Boolean = withContext(Dispatchers.IO) {
        val file = getSourceFile(source)
        try {
            val zipFile = ZipFile(file, password.toCharArray())
            if (!zipFile.isValidZipFile) return@withContext false
            if (!zipFile.isEncrypted) return@withContext true
            
            // Try to extract just one file to memory/null to test the password
            val headers = zipFile.fileHeaders
            val fileHeader = headers.find { !it.isDirectory } ?: return@withContext true // If all dirs, pwd is fine if we can read headers, but Zip4j tests it during extraction
            
            try {
                zipFile.getInputStream(fileHeader).use { it.read() }
                true
            } catch (e: Exception) {
                false
            }
        } finally {
            if (source.scheme != "file" && source.scheme != null) {
                file.delete()
            }
        }
    }

    override fun extract(source: Uri, destDir: Uri, password: String?): Flow<ArchiveProgress> = flow {
        val file = getSourceFile(source)
        try {
            val zipFile = if (password != null) ZipFile(file, password.toCharArray()) else ZipFile(file)
            if (!zipFile.isValidZipFile) {
                emit(ArchiveProgress.Failed("Invalid ZIP file"))
                return@flow
            }
            
            val destPath = destDir.path ?: throw Exception("Invalid destination")
            zipFile.isRunInThread = true
            
            val progressMonitor = zipFile.progressMonitor
            zipFile.extractAll(destPath)
            
            while (progressMonitor.state == ProgressMonitor.State.BUSY) {
                emit(
                    ArchiveProgress.Running(
                        filesDone = progressMonitor.percentDone,
                        filesTotal = 100,
                        currentEntryName = progressMonitor.fileName ?: "Extracting..."
                    )
                )
                delay(100)
            }
            
            if (progressMonitor.result == ProgressMonitor.Result.SUCCESS) {
                emit(ArchiveProgress.Complete(destPath))
            } else if (progressMonitor.result == ProgressMonitor.Result.ERROR) {
                emit(ArchiveProgress.Failed(progressMonitor.exception?.message ?: "Extraction failed"))
            } else if (progressMonitor.result == ProgressMonitor.Result.CANCELLED) {
                emit(ArchiveProgress.Failed("Extraction cancelled"))
            }

        } catch (e: Exception) {
            emit(ArchiveProgress.Failed(e.message ?: "Unknown error"))
        } finally {
            if (source.scheme != "file" && source.scheme != null) {
                file.delete()
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun create(sources: List<Uri>, destArchive: Uri): Flow<ArchiveProgress> {
        throw UnsupportedOperationException("ZIP Creation not implemented yet") // Can be added later
    }
}
