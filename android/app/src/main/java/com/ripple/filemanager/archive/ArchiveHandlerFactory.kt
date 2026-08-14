package com.ripple.filemanager.archive

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class UnsupportedArchiveFormatException(message: String) : Exception(message)

object ArchiveHandlerFactory {
    suspend fun create(context: Context, source: Uri): ArchiveHandler = withContext(Dispatchers.IO) {
        val magicBytes = ByteArray(8)
        var bytesRead = 0
        context.contentResolver.openInputStream(source)?.use { input ->
            bytesRead = input.read(magicBytes)
        } ?: throw Exception("Could not read source archive")

        if (bytesRead < 4) {
            throw UnsupportedArchiveFormatException("File is too small to be a valid archive")
        }

        // ZIP magic bytes: PK\x03\x04
        if (magicBytes[0] == 'P'.code.toByte() && magicBytes[1] == 'K'.code.toByte() &&
            magicBytes[2] == 0x03.toByte() && magicBytes[3] == 0x04.toByte()) {
            return@withContext ZipArchiveHandler(context)
        }

        if (bytesRead >= 7) {
            // RAR4 magic bytes: Rar!\x1A\x07\x00
            val isRar4 = magicBytes[0] == 'R'.code.toByte() && magicBytes[1] == 'a'.code.toByte() &&
                    magicBytes[2] == 'r'.code.toByte() && magicBytes[3] == '!'.code.toByte() &&
                    magicBytes[4] == 0x1A.toByte() && magicBytes[5] == 0x07.toByte() && magicBytes[6] == 0x00.toByte()

            if (isRar4) {
                return@withContext RarArchiveHandler(context)
            }
        }

        if (bytesRead >= 8) {
            // RAR5 magic bytes: Rar!\x1A\x07\x01\x00
            val isRar5 = magicBytes[0] == 'R'.code.toByte() && magicBytes[1] == 'a'.code.toByte() &&
                    magicBytes[2] == 'r'.code.toByte() && magicBytes[3] == '!'.code.toByte() &&
                    magicBytes[4] == 0x1A.toByte() && magicBytes[5] == 0x07.toByte() &&
                    magicBytes[6] == 0x01.toByte() && magicBytes[7] == 0x00.toByte()

            if (isRar5) {
                return@withContext RarArchiveHandler(context)
            }
        }

        throw UnsupportedArchiveFormatException("Unsupported archive format. Only ZIP and RAR are supported.")
    }
}
