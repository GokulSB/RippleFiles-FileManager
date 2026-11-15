package com.ripple.filemanager

import com.ripple.filemanager.archive.engine.ArchiveEngineManager
import com.ripple.filemanager.archive.engine.EngineProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ArchiveService {

    suspend fun listContents(file: File): List<String> = withContext(Dispatchers.IO) {
        try {
            val engine = ArchiveEngineManager.getEngineForFile(file)
            val entries = engine.list(file)
            entries.map { it.path }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun extractArchive(file: File, destDir: File): Boolean = withContext(Dispatchers.IO) {
        if (!destDir.exists()) {
            destDir.mkdirs()
        }
        try {
            val engine = ArchiveEngineManager.getEngineForFile(file)
            var success = false
            engine.extract(file, destDir).collect { progress ->
                if (progress is EngineProgress.Success) {
                    success = true
                }
            }
            success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
