package com.ripple.filemanager.archive

import android.content.Context
import android.net.Uri
import com.ripple.filemanager.archive.engine.ArchiveEngineManager
import com.ripple.filemanager.archive.sevenzip.SevenZipArchiveHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

class UnsupportedArchiveFormatException(message: String) : Exception(message)

object ArchiveHandlerFactory {
    suspend fun create(context: Context, source: Uri): ArchiveHandler = withContext(Dispatchers.IO) {
        val path = (source.path ?: source.toString()).lowercase()
        val magicBytes = ByteArray(8)
        var bytesRead = 0
        try {
            val stream: InputStream? = if (source.scheme == "file" || source.scheme == null) {
                val f = File(source.path ?: source.toString())
                if (f.exists()) java.io.FileInputStream(f) else context.contentResolver.openInputStream(source)
            } else {
                context.contentResolver.openInputStream(source)
            }
            stream?.use { input ->
                bytesRead = input.read(magicBytes)
            }
        } catch (_: Exception) {
            // Fall back to path extension
        }

        val isZip = (bytesRead >= 4 && magicBytes[0] == 'P'.code.toByte() && magicBytes[1] == 'K'.code.toByte() &&
                magicBytes[2] == 0x03.toByte() && magicBytes[3] == 0x04.toByte()) ||
                path.endsWith(".zip") || path.endsWith(".jar") || path.endsWith(".apk")

        val isRar4 = (bytesRead >= 7 && magicBytes[0] == 'R'.code.toByte() && magicBytes[1] == 'a'.code.toByte() &&
                magicBytes[2] == 'r'.code.toByte() && magicBytes[3] == '!'.code.toByte() &&
                magicBytes[4] == 0x1A.toByte() && magicBytes[5] == 0x07.toByte() && magicBytes[6] == 0x00.toByte())

        val isRar5 = (bytesRead >= 8 && magicBytes[0] == 'R'.code.toByte() && magicBytes[1] == 'a'.code.toByte() &&
                magicBytes[2] == 'r'.code.toByte() && magicBytes[3] == '!'.code.toByte() &&
                magicBytes[4] == 0x1A.toByte() && magicBytes[5] == 0x07.toByte() &&
                magicBytes[6] == 0x01.toByte() && magicBytes[7] == 0x00.toByte())

        val isRar = isRar4 || isRar5 || path.endsWith(".rar")

        // Parity switch: route ZIP and RAR to legacy engine until new engine passes parity tests
        if (isZip) {
            return@withContext if (ArchiveEngineManager.useNewEngineForZipRar) {
                SevenZipArchiveHandler(context)
            } else {
                ZipArchiveHandler(context)
            }
        }

        if (isRar) {
            return@withContext if (ArchiveEngineManager.useNewEngineForZipRar) {
                SevenZipArchiveHandler(context)
            } else {
                RarArchiveHandler(context)
            }
        }

        // All other formats are routed directly to the advanced SevenZip engine
        return@withContext SevenZipArchiveHandler(context)
    }
}
