package com.ripple.filemanager.archive.engine

import android.content.Context
import java.io.File

object ArchiveEngineManager {
    // Parity flag: Kept false until RAR and RAR5 parity tests pass completely.
    var useNewEngineForZipRar: Boolean = false

    @Volatile
    private var legacyEngine: LegacyEngine? = null

    @Volatile
    private var sevenZipEngine: SevenZipEngine? = null

    fun getLegacyEngine(): LegacyEngine {
        return legacyEngine ?: synchronized(this) {
            legacyEngine ?: LegacyEngine().also { legacyEngine = it }
        }
    }

    fun getSevenZipEngine(context: Context? = null): SevenZipEngine {
        return sevenZipEngine ?: synchronized(this) {
            sevenZipEngine ?: SevenZipEngine(context?.applicationContext).also { sevenZipEngine = it }
        }
    }

    suspend fun getEngineForFile(file: File, context: Context? = null): ArchiveEngine {
        val ext = file.extension.lowercase()
        val isZipOrRar = ext in listOf("zip", "jar", "apk", "rar")

        if (isZipOrRar && !useNewEngineForZipRar) {
            return getLegacyEngine()
        }

        val nativeEngine = getSevenZipEngine(context)
        if (nativeEngine.canHandle(file)) {
            return nativeEngine
        }

        if (getLegacyEngine().canHandle(file)) {
            return getLegacyEngine()
        }

        return nativeEngine
    }

    fun getEngineForCreation(format: String, context: Context? = null): ArchiveEngine {
        val fmt = format.lowercase().trim()
        if (fmt == "zip" && !useNewEngineForZipRar) {
            return getLegacyEngine()
        }
        return getSevenZipEngine(context)
    }
}
