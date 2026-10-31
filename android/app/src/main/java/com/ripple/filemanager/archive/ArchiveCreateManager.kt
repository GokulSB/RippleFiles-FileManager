package com.ripple.filemanager.archive

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import net.lingala.zip4j.progress.ProgressMonitor
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

enum class ArchiveFormat(val extension: String, val label: String) {
    ZIP("zip", "ZIP (.zip)"),
    RAR("rar", "RAR (.rar)")
}

enum class CompressionMethodType(val label: String, val description: String) {
    STORE("Store", "No compression, instantaneous"),
    FAST("Fast", "Low compression, very fast"),
    NORMAL("Normal", "Balanced compression & speed"),
    MAXIMUM("Maximum", "High compression ratio"),
    ULTRA("Ultra", "Smallest file size")
}

data class ArchiveCreateConfig(
    val name: String,
    val format: ArchiveFormat = ArchiveFormat.ZIP,
    val compressionMethod: CompressionMethodType = CompressionMethodType.NORMAL,
    val deleteAfterArchive: Boolean = false,
    val password: String? = null
)

object ArchiveCreateManager {

    suspend fun createArchive(
        sourceFiles: List<File>,
        destinationFile: File,
        config: ArchiveCreateConfig,
        onProgress: (percent: Int, currentFile: String) -> Unit,
        isCancelled: () -> Boolean = { false }
    ): File = withContext(Dispatchers.IO) {
        if (sourceFiles.isEmpty()) {
            throw IllegalArgumentException("No files selected for archiving")
        }

        // Delete destination if it already exists or was left over from a previous cancelled run
        if (destinationFile.exists()) {
            destinationFile.delete()
        }

        val zipParameters = ZipParameters().apply {
            when (config.compressionMethod) {
                CompressionMethodType.STORE -> {
                    compressionMethod = CompressionMethod.STORE
                }
                CompressionMethodType.FAST -> {
                    compressionMethod = CompressionMethod.DEFLATE
                    compressionLevel = CompressionLevel.FAST
                }
                CompressionMethodType.NORMAL -> {
                    compressionMethod = CompressionMethod.DEFLATE
                    compressionLevel = CompressionLevel.NORMAL
                }
                CompressionMethodType.MAXIMUM -> {
                    compressionMethod = CompressionMethod.DEFLATE
                    compressionLevel = CompressionLevel.MAXIMUM
                }
                CompressionMethodType.ULTRA -> {
                    compressionMethod = CompressionMethod.DEFLATE
                    compressionLevel = CompressionLevel.ULTRA
                }
            }

            if (!config.password.isNullOrBlank()) {
                isEncryptFiles = true
                encryptionMethod = EncryptionMethod.AES
                aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
            }
        }

        val zipFile = if (!config.password.isNullOrBlank()) {
            ZipFile(destinationFile, config.password.toCharArray())
        } else {
            ZipFile(destinationFile)
        }

        zipFile.isRunInThread = true
        val monitor = zipFile.progressMonitor

        try {
            val regularFiles = sourceFiles.filter { it.isFile }
            val folders = sourceFiles.filter { it.isDirectory }

            // 1. Add regular files
            if (regularFiles.isNotEmpty()) {
                zipFile.addFiles(regularFiles, zipParameters)
                while (monitor.state == ProgressMonitor.State.BUSY) {
                    if (isCancelled()) {
                        monitor.isCancelAllTasks = true
                        destinationFile.delete()
                        throw CancellationException("Archiving was cancelled")
                    }
                    val percent = monitor.percentDone.coerceIn(0, 100)
                    val fileName = monitor.fileName?.substringAfterLast(File.separator) ?: regularFiles.first().name
                    onProgress(percent, fileName)
                    delay(50)
                }

                if (monitor.result == ProgressMonitor.Result.ERROR) {
                    destinationFile.delete()
                    throw monitor.exception ?: Exception("Failed to add files to archive")
                }
            }

            // 2. Add folders
            for (folder in folders) {
                if (isCancelled()) {
                    monitor.isCancelAllTasks = true
                    destinationFile.delete()
                    throw CancellationException("Archiving was cancelled")
                }
                zipFile.addFolder(folder, zipParameters)
                while (monitor.state == ProgressMonitor.State.BUSY) {
                    if (isCancelled()) {
                        monitor.isCancelAllTasks = true
                        destinationFile.delete()
                        throw CancellationException("Archiving was cancelled")
                    }
                    val percent = monitor.percentDone.coerceIn(0, 100)
                    val fileName = monitor.fileName?.substringAfterLast(File.separator) ?: folder.name
                    onProgress(percent, fileName)
                    delay(50)
                }

                if (monitor.result == ProgressMonitor.Result.ERROR) {
                    destinationFile.delete()
                    throw monitor.exception ?: Exception("Failed to add folder '${folder.name}' to archive")
                }
            }

            onProgress(100, "Done")

            // 3. Delete source files only after verified success
            if (config.deleteAfterArchive) {
                for (source in sourceFiles) {
                    try {
                        if (source.isDirectory) {
                            source.deleteRecursively()
                        } else {
                            source.delete()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            destinationFile
        } catch (e: Exception) {
            if (destinationFile.exists()) {
                destinationFile.delete()
            }
            throw e
        }
    }
}
