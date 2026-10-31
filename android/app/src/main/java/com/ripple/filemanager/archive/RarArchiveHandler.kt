package com.ripple.filemanager.archive

import android.content.Context
import android.net.Uri
import be.stef.rar.Unrar5j
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

class RarArchiveHandler(private val context: Context) : ArchiveHandler {



    private suspend fun getSourceFile(source: Uri): File = withContext(Dispatchers.IO) {
        if (source.scheme == "file" || source.scheme == null) {
            File(source.path ?: source.toString())
        } else {
            val tempFile = File.createTempFile("archive_", ".rar", context.cacheDir)
            context.contentResolver.openInputStream(source)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw Exception("Cannot read source URI")
            tempFile
        }
    }

    private fun isTempFile(source: Uri, file: File): Boolean {
        return source.scheme != "file" && source.scheme != null
    }

    override suspend fun isEncrypted(source: Uri): Boolean = withContext(Dispatchers.IO) {
        val file = getSourceFile(source)
        try {
            if (Unrar5j.isEncrypted(file.absolutePath)) return@withContext true
            val tempDir = File(context.cacheDir, "rar_check_enc_${System.currentTimeMillis()}")
            tempDir.mkdirs()
            try {
                val result = Unrar5j.extract(file.absolutePath, tempDir.absolutePath, null)
                val needsPwd = result.passwordStatus == 2 ||
                        result.errors.any {
                            it.isEncryptedBlock ||
                            (it.errorMessage ?: "").contains("password", ignoreCase = true) ||
                            (it.exception?.message ?: "").contains("password", ignoreCase = true)
                        } || (result.failedFiles.isNotEmpty() && result.successCount == 0)
                needsPwd
            } finally {
                tempDir.deleteRecursively()
            }
        } catch (e: Exception) {
            val msg = e.message ?: ""
            msg.contains("password", ignoreCase = true) || msg.contains("encrypt", ignoreCase = true)
        } finally {
            if (isTempFile(source, file)) {
                file.delete()
            }
        }
    }

    override suspend fun listEntries(source: Uri): List<ArchiveEntryInfo> = listEntries(source, null)

    override suspend fun listEntries(source: Uri, password: String?): List<ArchiveEntryInfo> = withContext(Dispatchers.IO) {
        val file = getSourceFile(source)
        try {
            val isEncrypted = try { Unrar5j.isEncrypted(file.absolutePath) } catch (e: Exception) { false }
            
            // unrar5j has no listing API, so extract to a temp dir to enumerate entries
            val tempDir = File(context.cacheDir, "rar_list_${System.currentTimeMillis()}")
            tempDir.mkdirs()
            
            try {
                val result = Unrar5j.extract(file.absolutePath, tempDir.absolutePath, password)
                val needsPwd = result.passwordStatus == 2 ||
                        result.errors.any {
                            it.isEncryptedBlock ||
                            (it.errorMessage ?: "").contains("password", ignoreCase = true) ||
                            (it.exception?.message ?: "").contains("password", ignoreCase = true)
                        } || (isEncrypted && password == null && result.successCount == 0)
                
                // If encrypted and no password provided, return a single encrypted marker entry
                if (needsPwd && password == null) {
                    return@withContext listOf(
                        ArchiveEntryInfo(
                            name = "(encrypted archive)",
                            sizeBytes = file.length(),
                            isDirectory = false,
                            isEncrypted = true
                        )
                    )
                }
                
                val entries = mutableListOf<ArchiveEntryInfo>()
                tempDir.walkTopDown().forEach { extracted ->
                    if (extracted != tempDir) {
                        val relativePath = extracted.relativeTo(tempDir).path
                        entries.add(
                            ArchiveEntryInfo(
                                name = relativePath,
                                sizeBytes = if (extracted.isFile) extracted.length() else 0L,
                                java.io.File(extracted.path).isDirectory,
                                isEncrypted = isEncrypted
                            )
                        )
                    }
                }
                entries
            } finally {
                tempDir.deleteRecursively()
            }
        } finally {
            if (isTempFile(source, file)) {
                file.delete()
            }
        }
    }

    override suspend fun testPassword(source: Uri, password: String): Boolean = withContext(Dispatchers.IO) {
        val file = getSourceFile(source)
        val tempDir = File(context.cacheDir, "rar_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        try {
            val result = Unrar5j.extract(file.absolutePath, tempDir.absolutePath, password)
            val hasPwdError = result.passwordStatus == 2 ||
                    result.errors.any {
                        it.isEncryptedBlock ||
                        (it.errorMessage ?: "").contains("password", ignoreCase = true) ||
                        (it.exception?.message ?: "").contains("password", ignoreCase = true)
                    }
            !hasPwdError && result.errorCount == 0
        } catch (e: Exception) {
            false
        } finally {
            tempDir.deleteRecursively()
            if (isTempFile(source, file)) {
                file.delete()
            }
        }
    }

    override fun extract(source: Uri, destDir: Uri, password: String?): Flow<ArchiveProgress> = flow {
        val file = getSourceFile(source)
        try {
            val destPath = destDir.path ?: throw Exception("Invalid destination")
            val destFolder = File(destPath)
            if (!destFolder.exists()) destFolder.mkdirs()

            emit(ArchiveProgress.Running(0, 0, "Extracting..."))

            val result = Unrar5j.extract(file.absolutePath, destPath, password)

            val isPasswordRequired = result.passwordStatus == 2 ||
                    result.errors.any {
                        it.isEncryptedBlock ||
                        (it.errorMessage ?: "").contains("password", ignoreCase = true) ||
                        (it.exception?.message ?: "").contains("password", ignoreCase = true)
                    }

            if (isPasswordRequired) {
                emit(ArchiveProgress.NeedsPassword(attemptFailed = password != null))
            } else if (result.errorCount > 0 && result.successCount == 0) {
                val errorMsg = if (result.failedFiles.isNotEmpty()) {
                    "Failed to extract: ${result.failedFiles.joinToString(", ")}"
                } else {
                    "Extraction failed with ${result.errorCount} errors"
                }
                emit(ArchiveProgress.Failed(errorMsg))
            } else if (result.totalFiles == 0 && result.successCount == 0 && (destFolder.listFiles()?.isEmpty() != false)) {
                emit(ArchiveProgress.Failed("Invalid RAR archive or unsupported format"))
            } else {
                val total = if (result.totalFiles > 0) result.totalFiles else result.successCount
                emit(ArchiveProgress.Running(result.successCount, total, "Done"))
                emit(ArchiveProgress.Complete(destPath))
            }
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("password", ignoreCase = true) || msg.contains("encrypt", ignoreCase = true)) {
                emit(ArchiveProgress.NeedsPassword(attemptFailed = password != null))
            } else {
                emit(ArchiveProgress.Failed(msg.ifEmpty { "Unknown RAR extraction error" }))
            }
        } finally {
            if (isTempFile(source, file)) {
                file.delete()
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun create(sources: List<Uri>, destArchive: Uri): Flow<ArchiveProgress> {
        throw UnsupportedOperationException("RAR creation not supported")
    }
}
