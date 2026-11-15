package com.ripple.filemanager.archive.engine

import kotlinx.coroutines.flow.Flow
import java.io.File

data class ArchiveEntry(
    val index: Int,
    val path: String,
    val size: Long,
    val packSize: Long = 0L,
    val isDirectory: Boolean = false,
    val crc: Long = 0L,
    val mtime: Long = 0L,
    val isEncrypted: Boolean = false,
    val method: String = ""
)

enum class ConflictResolution {
    OVERWRITE,
    SKIP,
    OVERWRITE_ALL,
    SKIP_ALL
}

data class ExtractOptions(
    val selectedIndices: List<Int>? = null,
    val password: String? = null,
    val onConflict: ((relativePath: String) -> ConflictResolution)? = null
)

data class CreateOptions(
    val format: String, // e.g. "7z", "zip", "tar", "tar.gz", "tar.zst", "wim", etc.
    val level: Int = 5, // 0..9 (0=Store, 1=Fastest, 3=Fast, 5=Normal, 7=Maximum, 9=Ultra)
    val password: String? = null,
    val encryptNames: Boolean = false,
    val splitSizeBytes: Long = 0L,
    val charset: String? = null // For zip entry names, e.g. CP437, GBK, Shift-JIS, Windows-1252
)

sealed class EngineProgress {
    data class Progress(
        val completedBytes: Long,
        val totalBytes: Long,
        val currentItem: String
    ) : EngineProgress()

    data class NeedsPassword(val isRetry: Boolean = false) : EngineProgress()
    data class Success(val outputPath: String, val filesCount: Long) : EngineProgress()
    data class Error(val message: String, val throwable: Throwable? = null) : EngineProgress()
}

interface ArchiveEngine {
    val engineName: String

    suspend fun canHandle(file: File): Boolean
    suspend fun list(file: File, password: String? = null): List<ArchiveEntry>
    fun extract(file: File, destDir: File, options: ExtractOptions = ExtractOptions()): Flow<EngineProgress>
    fun test(file: File, password: String? = null): Flow<EngineProgress>
    fun create(inputs: List<File>, outFile: File, options: CreateOptions): Flow<EngineProgress>
}
