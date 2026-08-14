package com.ripple.filemanager.archive

import android.net.Uri
import kotlinx.coroutines.flow.Flow

data class ArchiveEntryInfo(
    val name: String,
    val sizeBytes: Long,
    val isDirectory: Boolean,
    val isEncrypted: Boolean
)

sealed class ArchiveProgress {
    data class Running(val filesDone: Int, val filesTotal: Int, val currentEntryName: String) : ArchiveProgress()
    data class NeedsPassword(val attemptFailed: Boolean) : ArchiveProgress()
    data class Complete(val outputDir: String) : ArchiveProgress()
    data class Failed(val reason: String) : ArchiveProgress()
}

interface ArchiveHandler {
    suspend fun listEntries(source: Uri): List<ArchiveEntryInfo>
    suspend fun testPassword(source: Uri, password: String): Boolean
    fun extract(source: Uri, destDir: Uri, password: String? = null): Flow<ArchiveProgress>
    suspend fun create(sources: List<Uri>, destArchive: Uri): Flow<ArchiveProgress>
}
