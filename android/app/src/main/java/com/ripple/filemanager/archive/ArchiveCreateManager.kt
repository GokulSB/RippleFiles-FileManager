package com.ripple.filemanager.archive

import android.content.Context
import com.ripple.filemanager.archive.engine.ArchiveEngineManager
import com.ripple.filemanager.archive.engine.CreateOptions
import com.ripple.filemanager.archive.engine.EngineProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

enum class ArchiveFormat(val extension: String, val label: String, val shortName: String, val description: String) {
    ZIP("zip", "ZIP (.zip)", "ZIP", "Standard & universal"),
    SEVEN_Z("7z", "7-Zip (.7z)", "7Z", "High compression ratio"),
    TAR("tar", "TAR (.tar)", "TAR", "Uncompressed UNIX archive"),
    TAR_GZ("tar.gz", "TAR.GZ (.tar.gz)", "TAR.GZ", "Standard UNIX compressed"),
    TAR_BZ2("tar.bz2", "TAR.BZ2 (.tar.bz2)", "TAR.BZ2", "High compression BZip2"),
    TAR_XZ("tar.xz", "TAR.XZ (.tar.xz)", "TAR.XZ", "High compression XZ"),
    TAR_ZST("tar.zst", "TAR.ZST (.tar.zst)", "TAR.ZST", "Ultra-fast Zstandard"),
    GZIP("gz", "GZIP (.gz)", "GZ", "Single file compression"),
    BZIP2("bz2", "BZIP2 (.bz2)", "BZ2", "Single file BZip2"),
    XZ("xz", "XZ (.xz)", "XZ", "Single file LZMA2"),
    ZSTD("zst", "Zstandard (.zst)", "ZST", "Single file Zstandard"),
    LZ4("lz4", "LZ4 (.lz4)", "LZ4", "Extremely fast LZ4"),
    LZ5("lz5", "LZ5 (.lz5)", "LZ5", "High throughput LZ5"),
    LIZARD("liz", "Lizard (.liz)", "LIZ", "Fast Lizard algorithm"),
    WIM("wim", "WIM (.wim)", "WIM", "Windows Imaging Format")
}

enum class CompressionMethodType(val label: String, val levelNumber: Int, val description: String) {
    STORE("Store", 0, "No compression, instantaneous"),
    FAST("Fast", 1, "Low compression, very fast"),
    NORMAL("Normal", 5, "Balanced compression & speed"),
    MAXIMUM("Maximum", 7, "High compression ratio"),
    ULTRA("Ultra", 9, "Smallest file size (slowest)")
}

enum class SplitVolumeOption(val label: String, val sizeBytes: Long) {
    NO_SPLIT("No Split", 0L),
    SPLIT_10MB("10 MB", 10L * 1024 * 1024),
    SPLIT_50MB("50 MB", 50L * 1024 * 1024),
    SPLIT_100MB("100 MB", 100L * 1024 * 1024),
    SPLIT_650MB("650 MB (CD)", 650L * 1024 * 1024),
    SPLIT_700MB("700 MB (CD)", 700L * 1024 * 1024),
    SPLIT_1GB("1 GB", 1024L * 1024 * 1024),
    SPLIT_2GB("2 GB", 2048L * 1024 * 1024),
    SPLIT_4GB("4 GB (FAT32)", 4096L * 1024 * 1024)
}

val SUPPORTED_ZIP_CHARSETS = listOf(
    "UTF-8",
    "CP437",
    "Windows-1252",
    "GBK",
    "Big5",
    "Shift-JIS",
    "EUC-KR",
    "CP866",
    "Windows-1251"
)

data class ArchiveCreateConfig(
    val name: String,
    val format: ArchiveFormat = ArchiveFormat.ZIP,
    val compressionMethod: CompressionMethodType = CompressionMethodType.NORMAL,
    val deleteAfterArchive: Boolean = false,
    val password: String? = null,
    val encryptFileNames: Boolean = false,
    val splitVolume: SplitVolumeOption = SplitVolumeOption.NO_SPLIT,
    val charset: String = "UTF-8"
)

object ArchiveCreateManager {

    suspend fun createArchive(
        sourceFiles: List<File>,
        destinationFile: File,
        config: ArchiveCreateConfig,
        onProgress: (percent: Int, currentFile: String) -> Unit,
        isCancelled: () -> Boolean = { false },
        context: Context? = null
    ): File = withContext(Dispatchers.IO) {
        if (sourceFiles.isEmpty()) {
            throw IllegalArgumentException("No files selected for archiving")
        }

        // STRICT LICENSE CHECK: RAR creation forbidden
        if (config.format.extension.equals("rar", ignoreCase = true)) {
            throw IllegalArgumentException("RAR creation is strictly forbidden by license")
        }

        if (destinationFile.exists()) {
            destinationFile.delete()
        }

        val engine = ArchiveEngineManager.getEngineForCreation(config.format.extension, context)
        val createOptions = CreateOptions(
            format = config.format.extension,
            level = config.compressionMethod.levelNumber,
            password = config.password?.ifBlank { null },
            encryptNames = config.encryptFileNames && config.format == ArchiveFormat.SEVEN_Z,
            splitSizeBytes = config.splitVolume.sizeBytes,
            charset = if (config.format == ArchiveFormat.ZIP) config.charset else null
        )

        var lastError: Throwable? = null

        try {
            engine.create(sourceFiles, destinationFile, createOptions).collect { progress ->
                if (isCancelled()) {
                    destinationFile.delete()
                    throw CancellationException("Archiving was cancelled")
                }
                when (progress) {
                    is EngineProgress.Progress -> {
                        val pct = if (progress.totalBytes > 0) {
                            ((progress.completedBytes * 100) / progress.totalBytes).toInt().coerceIn(0, 99)
                        } else {
                            0
                        }
                        onProgress(pct, progress.currentItem)
                    }
                    is EngineProgress.Success -> {
                        // success
                    }
                    is EngineProgress.Error -> {
                        lastError = progress.throwable ?: Exception(progress.message)
                    }
                    is EngineProgress.NeedsPassword -> {
                        // Handled beforehand
                    }
                }
            }

            if (isCancelled()) {
                destinationFile.delete()
                throw CancellationException("Archiving was cancelled")
            }

            if (lastError != null) {
                if (destinationFile.exists()) destinationFile.delete()
                throw lastError!!
            }

            onProgress(100, "Done")

            // Delete source files if requested
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
