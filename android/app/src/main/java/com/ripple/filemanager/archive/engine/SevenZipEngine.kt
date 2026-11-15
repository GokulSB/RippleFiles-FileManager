package com.ripple.filemanager.archive.engine

import android.content.Context
import android.os.StatFs
import android.util.Log
import com.ripple.filemanager.archive.sevenzip.Native7zBridge
import com.ripple.filemanager.archive.sevenzip.NativeArchiveCallback
import com.ripple.filemanager.archive.sevenzip.NativeArchiveEntry
import com.ripple.filemanager.archive.sevenzip.NativeFormatInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.atomic.AtomicBoolean

class SevenZipEngine(private val context: Context? = null) : ArchiveEngine {
    override val engineName: String = "SevenZipEngine"

    companion object {
        private const val TAG = "SevenZipEngine"

        val SINGLE_STREAM_FORMATS = setOf(
            "gzip", "bzip2", "xz", "zstd", "lz4", "lz5", "lizard", "lzma", "z"
        )

        val SINGLE_STREAM_EXTENSIONS = setOf(
            "gz", "bz2", "xz", "zst", "lz4", "lz5", "liz", "lzma", "z"
        )

        fun resolveFirstVolume(file: File): File {
            val name = file.name
            val parent = file.parentFile ?: return file

            // 1. .001, .002, .003, ... (e.g. archive.7z.002 or archive.zip.002)
            val dotNumRegex = Regex("""^(.*)\.(\d{3,})$""", RegexOption.IGNORE_CASE)
            val dotMatch = dotNumRegex.matchEntire(name)
            if (dotMatch != null) {
                val base = dotMatch.groupValues[1]
                val numStr = dotMatch.groupValues[2]
                val num = numStr.toIntOrNull() ?: 1
                if (num > 1) {
                    val firstNumStr = String.format("%0${numStr.length}d", 1)
                    val firstVol = File(parent, "$base.$firstNumStr")
                    if (firstVol.exists()) return firstVol
                }
                return file
            }

            // 2. .part1.rar, .part2.rar, ... or .part01.rar
            val partRegex = Regex("""^(.*)\.part(\d+)(\.rar)$""", RegexOption.IGNORE_CASE)
            val partMatch = partRegex.matchEntire(name)
            if (partMatch != null) {
                val base = partMatch.groupValues[1]
                val numStr = partMatch.groupValues[2]
                val ext = partMatch.groupValues[3]
                val num = numStr.toIntOrNull() ?: 1
                if (num > 1) {
                    val firstNumStr = String.format("%0${numStr.length}d", 1)
                    val firstVol = File(parent, "$base.part$firstNumStr$ext")
                    if (firstVol.exists()) return firstVol
                }
                return file
            }

            // 3. Split zip: archive.z01, archive.z02, ... -> main is archive.zip
            val zipSplitRegex = Regex("""^(.*)\.z(\d{2,})$""", RegexOption.IGNORE_CASE)
            val zipMatch = zipSplitRegex.matchEntire(name)
            if (zipMatch != null) {
                val base = zipMatch.groupValues[1]
                val mainZip = File(parent, "$base.zip")
                if (mainZip.exists()) return mainZip
                val firstZ01 = File(parent, "$base.z01")
                if (firstZ01.exists()) return firstZ01
            }

            return file
        }
    }

    private fun ensureInit(): Boolean {
        return Native7zBridge.init(context)
    }

    override suspend fun canHandle(file: File): Boolean = withContext(Dispatchers.IO) {
        val targetFile = resolveFirstVolume(file)
        if (!ensureInit()) return@withContext false
        if (!targetFile.exists() || targetFile.length() == 0L) return@withContext false

        // 1. Try magic bytes detection
        val formats = Native7zBridge.nativeSupportedFormats() ?: return@withContext false
        val header = ByteArray(65536)
        var bytesRead = 0
        try {
            FileInputStream(targetFile).use { input ->
                bytesRead = input.read(header)
            }
        } catch (e: Exception) {
            return@withContext false
        }

        for (fmt in formats) {
            if (fmt.sigSize > 0 && fmt.sigOffset + fmt.sigSize <= bytesRead) {
                // If signature matches, it's definitely supported
                val handle = Native7zBridge.nativeOpen(targetFile.absolutePath, null, null)
                if (handle > 0) {
                    Native7zBridge.nativeClose(handle)
                    return@withContext true
                }
            }
        }

        // 2. Extension check
        val ext = targetFile.extension.lowercase()
        for (fmt in formats) {
            val supportedExts = fmt.extension.lowercase().split(" ")
            if (ext in supportedExts) {
                return@withContext true
            }
        }

        // 3. Fallback try open
        val handle = Native7zBridge.nativeOpen(targetFile.absolutePath, null, null)
        if (handle > 0) {
            Native7zBridge.nativeClose(handle)
            return@withContext true
        }

        false
    }

    override suspend fun list(file: File, password: String?): List<ArchiveEntry> = withContext(Dispatchers.IO) {
        if (!ensureInit()) throw IllegalStateException("7-Zip native engine could not be initialized")

        val targetFile = resolveFirstVolume(file)
        val handle = Native7zBridge.nativeOpen(targetFile.absolutePath, password, null)
        if (handle <= 0) {
            throw IllegalArgumentException("Could not open archive: ${targetFile.name}. Possibly password-protected or unsupported format.")
        }

        try {
            val rawEntries = Native7zBridge.nativeList(handle) ?: emptyArray()
            rawEntries.map { raw ->
                ArchiveEntry(
                    index = raw.index,
                    path = raw.path,
                    size = raw.size,
                    packSize = raw.packSize,
                    isDirectory = raw.isDir,
                    crc = raw.crc,
                    mtime = raw.mtime,
                    isEncrypted = raw.isEncrypted,
                    method = raw.method
                )
            }
        } finally {
            Native7zBridge.nativeClose(handle)
        }
    }

    override fun extract(file: File, destDir: File, options: ExtractOptions): Flow<EngineProgress> = callbackFlow {
        if (!ensureInit()) {
            trySend(EngineProgress.Error("Failed to initialize 7-Zip native engine"))
            close()
            return@callbackFlow
        }

        val cancelled = AtomicBoolean(false)
        val targetFile = resolveFirstVolume(file)
        val handle = Native7zBridge.nativeOpen(targetFile.absolutePath, options.password, null)
        if (handle <= 0) {
            trySend(EngineProgress.NeedsPassword(isRetry = options.password != null))
            close()
            return@callbackFlow
        }

        // Check required disk space
        try {
            val entries = Native7zBridge.nativeList(handle)
            var totalUncompressedBytes = 0L
            entries?.forEach { entry ->
                if (options.selectedIndices == null || entry.index in options.selectedIndices) {
                    totalUncompressedBytes += entry.size
                }
            }

            if (!destDir.exists()) destDir.mkdirs()
            val stat = StatFs(destDir.absolutePath)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            if (availableBytes < totalUncompressedBytes) {
                Native7zBridge.nativeClose(handle)
                trySend(EngineProgress.Error("Not enough free disk space. Required: ${totalUncompressedBytes / (1024 * 1024)} MB, Available: ${availableBytes / (1024 * 1024)} MB"))
                close()
                return@callbackFlow
            }

            val callback = object : NativeArchiveCallback {
                override fun isCancelled(): Boolean = cancelled.get()

                override fun onProgress(completedBytes: Long, totalBytes: Long, currentItem: String?) {
                    trySend(EngineProgress.Progress(completedBytes, totalBytes, currentItem ?: ""))
                }

                override fun onPasswordRequired(): String? {
                    trySend(EngineProgress.NeedsPassword(isRetry = true))
                    return options.password
                }

                override fun onFileConflict(relativePath: String): Int {
                    val resolution = options.onConflict?.invoke(relativePath) ?: ConflictResolution.OVERWRITE
                    return when (resolution) {
                        ConflictResolution.OVERWRITE -> 0
                        ConflictResolution.SKIP -> 1
                        ConflictResolution.OVERWRITE_ALL -> 2
                        ConflictResolution.SKIP_ALL -> 3
                    }
                }
            }

            val indicesArray = options.selectedIndices?.toIntArray()
            val success = Native7zBridge.nativeExtract(
                handle,
                indicesArray,
                destDir.absolutePath,
                options.password,
                callback
            )

            if (cancelled.get()) {
                try {
                    destDir.listFiles()?.forEach { it.deleteRecursively() }
                } catch (_: Exception) {}
                trySend(EngineProgress.Error("Extraction cancelled by user"))
            } else if (success) {
                trySend(EngineProgress.Success(destDir.absolutePath, entries?.size?.toLong() ?: 0L))
            } else {
                try {
                    destDir.listFiles()?.forEach { it.deleteRecursively() }
                } catch (_: Exception) {}
                trySend(EngineProgress.Error("Extraction failed or data error in archive"))
            }
        } catch (e: Exception) {
            trySend(EngineProgress.Error(e.message ?: "Extraction error", e))
        } finally {
            Native7zBridge.nativeClose(handle)
            close()
        }

        awaitClose {
            cancelled.set(true)
        }
    }.flowOn(Dispatchers.IO)

    override fun test(file: File, password: String?): Flow<EngineProgress> = callbackFlow {
        if (!ensureInit()) {
            trySend(EngineProgress.Error("Failed to initialize 7-Zip native engine"))
            close()
            return@callbackFlow
        }

        val cancelled = AtomicBoolean(false)
        val targetFile = resolveFirstVolume(file)
        val handle = Native7zBridge.nativeOpen(targetFile.absolutePath, password, null)
        if (handle <= 0) {
            trySend(EngineProgress.NeedsPassword(isRetry = password != null))
            close()
            return@callbackFlow
        }

        try {
            val callback = object : NativeArchiveCallback {
                override fun isCancelled(): Boolean = cancelled.get()
                override fun onProgress(completedBytes: Long, totalBytes: Long, currentItem: String?) {
                    trySend(EngineProgress.Progress(completedBytes, totalBytes, currentItem ?: ""))
                }
                override fun onPasswordRequired(): String? = password
            }

            val success = Native7zBridge.nativeTest(handle, null, password, callback)
            if (success) {
                trySend(EngineProgress.Success(file.absolutePath, 0L))
            } else {
                trySend(EngineProgress.Error("Archive integrity test failed (CRC or header error)"))
            }
        } catch (e: Exception) {
            trySend(EngineProgress.Error(e.message ?: "Test failed", e))
        } finally {
            Native7zBridge.nativeClose(handle)
            close()
        }

        awaitClose {
            cancelled.set(true)
        }
    }.flowOn(Dispatchers.IO)

    override fun create(inputs: List<File>, outFile: File, options: CreateOptions): Flow<EngineProgress> = callbackFlow {
        if (!ensureInit()) {
            trySend(EngineProgress.Error("Failed to initialize 7-Zip native engine"))
            close()
            return@callbackFlow
        }

        val rawFormat = options.format.lowercase().trim()

        // STRICT LICENSE CHECK: RAR creation forbidden
        if (rawFormat == "rar" || rawFormat == "rar5") {
            trySend(EngineProgress.Error("RAR creation is strictly forbidden by license"))
            close()
            return@callbackFlow
        }

        val cancelled = AtomicBoolean(false)
        var tempTarFile: File? = null

        try {
            val isCompoundTar = rawFormat.startsWith("tar.") || rawFormat in setOf("tgz", "tbz2", "txz")
            val isSingleStream = rawFormat in SINGLE_STREAM_FORMATS || rawFormat in SINGLE_STREAM_EXTENSIONS || isCompoundTar
            val needsTarWrap = isCompoundTar || (isSingleStream && (inputs.size > 1 || (inputs.size == 1 && inputs[0].isDirectory)))

            val actualInputs: Array<String>
            val targetFormat: String

            if (needsTarWrap) {
                // Step 1: Wrap inputs in tar first
                trySend(EngineProgress.Progress(0, 0, "Creating intermediate tar archive..."))
                tempTarFile = File.createTempFile("wrap_", ".tar", outFile.parentFile ?: context?.cacheDir)
                tempTarFile.delete()
                val tarSuccess = Native7zBridge.nativeCreate(
                    inputs.map { it.absolutePath }.toTypedArray(),
                    tempTarFile.absolutePath,
                    "tar",
                    0, // Store
                    null,
                    false,
                    0L,
                    null
                )
                if (!tarSuccess || !tempTarFile.exists()) {
                    trySend(EngineProgress.Error("Failed to create intermediate TAR for single-stream compression"))
                    close()
                    return@callbackFlow
                }

                actualInputs = arrayOf(tempTarFile.absolutePath)
                targetFormat = when (rawFormat) {
                    "tar.gz", "gz", "gzip", "tgz" -> "gzip"
                    "tar.bz2", "bz2", "bzip2", "tbz2" -> "bzip2"
                    "tar.xz", "xz", "txz" -> "xz"
                    "tar.zst", "zst", "zstd" -> "zstd"
                    "tar.lz4", "lz4" -> "lz4"
                    "tar.lz5", "lz5" -> "lz5"
                    "tar.liz", "tar.lizard", "liz", "lizard" -> "lizard"
                    else -> rawFormat
                }
            } else {
                actualInputs = inputs.map { it.absolutePath }.toTypedArray()
                targetFormat = when (rawFormat) {
                    "tar.gz", "gz", "gzip", "tgz" -> "gzip"
                    "tar.bz2", "bz2", "bzip2", "tbz2" -> "bzip2"
                    "tar.xz", "xz", "txz" -> "xz"
                    "tar.zst", "zst", "zstd" -> "zstd"
                    "tar.lz4", "lz4" -> "lz4"
                    "tar.lz5", "lz5" -> "lz5"
                    "tar.liz", "tar.lizard", "liz", "lizard" -> "lizard"
                    else -> rawFormat
                }
            }

            val callback = object : NativeArchiveCallback {
                override fun isCancelled(): Boolean = cancelled.get()
                override fun onProgress(completedBytes: Long, totalBytes: Long, currentItem: String?) {
                    trySend(EngineProgress.Progress(completedBytes, totalBytes, currentItem ?: outFile.name))
                }
            }

            val success = Native7zBridge.nativeCreate(
                actualInputs,
                outFile.absolutePath,
                targetFormat,
                options.level,
                options.password,
                options.encryptNames,
                options.splitSizeBytes,
                callback
            )

            if (cancelled.get()) {
                outFile.delete()
                trySend(EngineProgress.Error("Creation cancelled by user"))
            } else if (success && outFile.exists()) {
                trySend(EngineProgress.Success(outFile.absolutePath, inputs.size.toLong()))
            } else {
                trySend(EngineProgress.Error("Failed to create archive: ${outFile.name}"))
            }
        } catch (e: Exception) {
            trySend(EngineProgress.Error(e.message ?: "Archive creation failed", e))
        } finally {
            tempTarFile?.delete()
            close()
        }

        awaitClose {
            cancelled.set(true)
        }
    }.flowOn(Dispatchers.IO)
}
