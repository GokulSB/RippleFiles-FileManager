package com.ripple.filemanager.archive

import android.net.Uri
import kotlinx.coroutines.flow.Flow

data class ArchiveEntryInfo(
    val name: String,
    val sizeBytes: Long,
    val isDirectory: Boolean,
    val isEncrypted: Boolean,
    val index: Int = 0
)

sealed class ArchiveProgress {
    data class Running(val filesDone: Int, val filesTotal: Int, val currentEntryName: String) : ArchiveProgress()
    data class NeedsPassword(val attemptFailed: Boolean) : ArchiveProgress()
    data class Complete(val outputDir: String) : ArchiveProgress()
    data class Failed(val reason: String) : ArchiveProgress()
}

interface ArchiveHandler {
    suspend fun isEncrypted(source: Uri): Boolean = false
    suspend fun listEntries(source: Uri): List<ArchiveEntryInfo> = listEntries(source, null)
    suspend fun listEntries(source: Uri, password: String?): List<ArchiveEntryInfo> = listEntries(source, password, null)
    suspend fun listEntries(source: Uri, password: String?, charset: String?): List<ArchiveEntryInfo>
    suspend fun testPassword(source: Uri, password: String): Boolean
    fun extract(source: Uri, destDir: Uri, password: String? = null): Flow<ArchiveProgress> = extract(source, destDir, password, null, null)
    fun extract(source: Uri, destDir: Uri, password: String? = null, selectedIndices: List<Int>? = null): Flow<ArchiveProgress> = extract(source, destDir, password, selectedIndices, null)
    fun extract(source: Uri, destDir: Uri, password: String? = null, selectedIndices: List<Int>? = null, charset: String? = null): Flow<ArchiveProgress>
    suspend fun testArchive(source: Uri, password: String? = null): Boolean = testPassword(source, password ?: "")
    suspend fun create(sources: List<Uri>, destArchive: Uri): Flow<ArchiveProgress>
}
