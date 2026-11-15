package com.ripple.filemanager.archive.sevenzip

import android.content.Context
import android.net.Uri
import com.ripple.filemanager.archive.ArchiveEntryInfo
import com.ripple.filemanager.archive.ArchiveHandler
import com.ripple.filemanager.archive.ArchiveProgress
import com.ripple.filemanager.archive.engine.ArchiveEngineManager
import com.ripple.filemanager.archive.engine.EngineProgress
import com.ripple.filemanager.archive.engine.ExtractOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

class SevenZipArchiveHandler(private val context: Context) : ArchiveHandler {

    private suspend fun getSourceFile(source: Uri): Pair<File, Boolean> = withContext(Dispatchers.IO) {
        if (source.scheme == "file" || source.scheme == null) {
            Pair(File(source.path ?: source.toString()), false)
        } else {
            val extension = source.lastPathSegment?.substringAfterLast('.', "tmp") ?: "tmp"
            val tempFile = File.createTempFile("arc_cache_", ".$extension", context.cacheDir)
            context.contentResolver.openInputStream(source)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalArgumentException("Cannot read archive from URI: $source")
            Pair(tempFile, true)
        }
    }

    override suspend fun isEncrypted(source: Uri): Boolean = withContext(Dispatchers.IO) {
        val (file, isTemp) = getSourceFile(source)
        try {
            Native7zBridge.init(context)
            val handle = Native7zBridge.nativeOpen(file.absolutePath, null, null)
            if (handle <= 0) {
                // If it fails to open without password, it could be password protected
                return@withContext true
            }
            try {
                val entries = Native7zBridge.nativeList(handle)
                entries?.any { it.isEncrypted } ?: false
            } finally {
                Native7zBridge.nativeClose(handle)
            }
        } catch (e: Exception) {
            false
        } finally {
            if (isTemp) file.delete()
        }
    }

    override suspend fun listEntries(source: Uri): List<ArchiveEntryInfo> = listEntries(source, null)

    override suspend fun listEntries(source: Uri, password: String?): List<ArchiveEntryInfo> =
        listEntries(source, password, null)

    override suspend fun listEntries(source: Uri, password: String?, charset: String?): List<ArchiveEntryInfo> = withContext(Dispatchers.IO) {
        val (file, isTemp) = getSourceFile(source)
        try {
            val engine = ArchiveEngineManager.getSevenZipEngine(context)
            val entries = engine.list(file, password)
            entries.map {
                ArchiveEntryInfo(
                    name = it.path,
                    sizeBytes = it.size,
                    isDirectory = it.isDirectory,
                    isEncrypted = it.isEncrypted,
                    index = it.index
                )
            }
        } finally {
            if (isTemp) file.delete()
        }
    }

    override suspend fun testPassword(source: Uri, password: String): Boolean = testArchive(source, password)

    override suspend fun testArchive(source: Uri, password: String?): Boolean = withContext(Dispatchers.IO) {
        val (file, isTemp) = getSourceFile(source)
        try {
            val engine = ArchiveEngineManager.getSevenZipEngine(context)
            var passed = false
            engine.test(file, password).collect { progress ->
                when (progress) {
                    is EngineProgress.Success -> passed = true
                    is EngineProgress.Error -> passed = false
                    else -> {}
                }
            }
            passed
        } catch (e: Exception) {
            false
        } finally {
            if (isTemp) file.delete()
        }
    }

    override fun extract(source: Uri, destDir: Uri, password: String?): Flow<ArchiveProgress> =
        extract(source, destDir, password, null, null)

    override fun extract(source: Uri, destDir: Uri, password: String?, selectedIndices: List<Int>?): Flow<ArchiveProgress> =
        extract(source, destDir, password, selectedIndices, null)

    override fun extract(
        source: Uri,
        destDir: Uri,
        password: String?,
        selectedIndices: List<Int>?,
        charset: String?
    ): Flow<ArchiveProgress> = flow {
        val (file, isTemp) = getSourceFile(source)
        val destFile = if (destDir.scheme == "file" || destDir.scheme == null) {
            File(destDir.path ?: destDir.toString())
        } else {
            File(context.filesDir, "extracted_${System.currentTimeMillis()}")
        }

        try {
            val engine = ArchiveEngineManager.getSevenZipEngine(context)
            var totalFiles = selectedIndices?.size ?: 0
            if (totalFiles == 0) {
                try {
                    totalFiles = engine.list(file, password).size
                } catch (_: Exception) {}
            }

            var filesDone = 0

            engine.extract(file, destFile, ExtractOptions(selectedIndices = selectedIndices, password = password)).collect { progress ->
                when (progress) {
                    is EngineProgress.Progress -> {
                        filesDone++
                        emit(ArchiveProgress.Running(filesDone, totalFiles, progress.currentItem))
                    }
                    is EngineProgress.NeedsPassword -> {
                        emit(ArchiveProgress.NeedsPassword(attemptFailed = progress.isRetry))
                    }
                    is EngineProgress.Success -> {
                        emit(ArchiveProgress.Complete(destFile.absolutePath))
                    }
                    is EngineProgress.Error -> {
                        emit(ArchiveProgress.Failed(progress.message))
                    }
                }
            }
        } finally {
            if (isTemp) file.delete()
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun create(sources: List<Uri>, destArchive: Uri): Flow<ArchiveProgress> = flow {
        // Handled via ArchiveCreateManager / SevenZipEngine
        emit(ArchiveProgress.Complete(destArchive.toString()))
    }.flowOn(Dispatchers.IO)
}
