package com.ripple.filemanager.archive.engine

import be.stef.rar.Unrar5j
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.File
import java.io.FileOutputStream

class LegacyEngine : ArchiveEngine {
    override val engineName: String = "LegacyEngine"

    override suspend fun canHandle(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext in listOf("zip", "jar", "apk", "rar")
    }

    override suspend fun list(file: File, password: String?): List<ArchiveEntry> = withContext(Dispatchers.IO) {
        val ext = file.extension.lowercase()
        when (ext) {
            "zip", "jar", "apk" -> listZip(file, password)
            "rar" -> listRar(file, password)
            else -> emptyList()
        }
    }

    private fun listZip(file: File, password: String?): List<ArchiveEntry> {
        val zipFile = if (password != null) ZipFile(file, password.toCharArray()) else ZipFile(file)
        if (!zipFile.isValidZipFile) {
            throw IllegalArgumentException("Invalid ZIP archive: ${file.name}")
        }
        return zipFile.fileHeaders.mapIndexed { index, header ->
            ArchiveEntry(
                index = index,
                path = header.fileName,
                size = header.uncompressedSize,
                packSize = header.compressedSize,
                isDirectory = header.isDirectory,
                crc = header.crc,
                mtime = header.lastModifiedTimeEpoch,
                isEncrypted = header.isEncrypted || zipFile.isEncrypted,
                method = header.compressionMethod?.name ?: ""
            )
        }
    }

    private fun listRar(file: File, password: String?): List<ArchiveEntry> {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "rar_list_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        try {
            val result = Unrar5j.extract(file.absolutePath, tempDir.absolutePath, password)
            if (result.passwordStatus == 2) {
                throw SecurityException("Password required for RAR archive")
            }
            val entries = mutableListOf<ArchiveEntry>()
            var idx = 0
            tempDir.walkTopDown().forEach { extracted ->
                if (extracted != tempDir) {
                    val relPath = extracted.relativeTo(tempDir).path.replace('\\', '/')
                    entries.add(
                        ArchiveEntry(
                            index = idx++,
                            path = relPath,
                            size = extracted.length(),
                            packSize = 0L,
                            isDirectory = extracted.isDirectory,
                            crc = 0L,
                            mtime = extracted.lastModified(),
                            isEncrypted = false,
                            method = "RAR"
                        )
                    )
                }
            }
            return entries
        } finally {
            tempDir.deleteRecursively()
        }
    }

    override fun extract(file: File, destDir: File, options: ExtractOptions): Flow<EngineProgress> = flow {
        if (!destDir.exists()) destDir.mkdirs()
        val ext = file.extension.lowercase()

        try {
            emit(EngineProgress.Progress(0, file.length(), file.name))
            when (ext) {
                "zip", "jar", "apk" -> extractZip(file, destDir, options)
                "rar" -> extractRar(file, destDir, options)
                else -> throw UnsupportedOperationException("LegacyEngine does not support format: $ext")
            }
            emit(EngineProgress.Success(destDir.absolutePath, 0L))
        } catch (e: Exception) {
            emit(EngineProgress.Error(e.message ?: "Extraction failed", e))
        }
    }.flowOn(Dispatchers.IO)

    private fun extractZip(file: File, destDir: File, options: ExtractOptions) {
        val zipFile = if (options.password != null) ZipFile(file, options.password.toCharArray()) else ZipFile(file)
        if (!zipFile.isValidZipFile) throw IllegalArgumentException("Invalid ZIP file")

        val headers = zipFile.fileHeaders
        val selected = options.selectedIndices?.let { idxs ->
            headers.filterIndexed { index, _ -> index in idxs }
        } ?: headers

        for (header in selected) {
            val targetFile = File(destDir, header.fileName)
            // Zip-Slip protection
            if (!targetFile.canonicalPath.startsWith(destDir.canonicalPath)) {
                continue
            }
            if (header.isDirectory) {
                targetFile.mkdirs()
            } else {
                targetFile.parentFile?.mkdirs()
                zipFile.extractFile(header, destDir.absolutePath)
            }
        }
    }

    private fun extractRar(file: File, destDir: File, options: ExtractOptions) {
        val result = Unrar5j.extract(file.absolutePath, destDir.absolutePath, options.password)
        if (result.passwordStatus == 2) {
            throw SecurityException("Password required for RAR archive")
        }
        if (result.errorCount > 0) {
            throw RuntimeException("Extraction finished with ${result.errorCount} errors")
        }
    }

    override fun test(file: File, password: String?): Flow<EngineProgress> = flow {
        val ext = file.extension.lowercase()
        try {
            emit(EngineProgress.Progress(0, file.length(), file.name))
            when (ext) {
                "zip", "jar", "apk" -> {
                    val zipFile = if (password != null) ZipFile(file, password.toCharArray()) else ZipFile(file)
                    if (!zipFile.isValidZipFile) throw IllegalArgumentException("Invalid ZIP")
                    for (header in zipFile.fileHeaders) {
                        if (!header.isDirectory) {
                            zipFile.getInputStream(header).use { input ->
                                val buf = ByteArray(8192)
                                while (input.read(buf) != -1) { /* verify CRC */ }
                            }
                        }
                    }
                }
                "rar" -> {
                    val tempDir = File.createTempFile("rar_test_", "")
                    tempDir.delete()
                    tempDir.mkdirs()
                    try {
                        val result = Unrar5j.extract(file.absolutePath, tempDir.absolutePath, password)
                        if (result.passwordStatus == 2) throw SecurityException("Password required")
                        if (result.errorCount > 0) throw RuntimeException("RAR test failed")
                    } finally {
                        tempDir.deleteRecursively()
                    }
                }
                else -> throw UnsupportedOperationException("Unsupported: $ext")
            }
            emit(EngineProgress.Success(file.absolutePath, 0L))
        } catch (e: Exception) {
            emit(EngineProgress.Error(e.message ?: "Test failed", e))
        }
    }.flowOn(Dispatchers.IO)

    override fun create(inputs: List<File>, outFile: File, options: CreateOptions): Flow<EngineProgress> = flow {
        if (options.format.lowercase() != "zip") {
            throw UnsupportedOperationException("LegacyEngine only supports creating ZIP archives")
        }

        try {
            emit(EngineProgress.Progress(0, 0, outFile.name))
            val zipFile = if (options.password != null) ZipFile(outFile, options.password.toCharArray()) else ZipFile(outFile)
            val zipParams = ZipParameters().apply {
                compressionMethod = CompressionMethod.DEFLATE
                compressionLevel = when (options.level) {
                    0 -> CompressionLevel.NO_COMPRESSION
                    1, 2, 3 -> CompressionLevel.FASTEST
                    4, 5, 6 -> CompressionLevel.NORMAL
                    7, 8 -> CompressionLevel.MAXIMUM
                    else -> CompressionLevel.ULTRA
                }
                if (options.password != null) {
                    isEncryptFiles = true
                    encryptionMethod = EncryptionMethod.AES
                    aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                }
            }

            for (input in inputs) {
                if (input.isDirectory) {
                    zipFile.addFolder(input, zipParams)
                } else {
                    zipFile.addFile(input, zipParams)
                }
            }
            emit(EngineProgress.Success(outFile.absolutePath, inputs.size.toLong()))
        } catch (e: Exception) {
            emit(EngineProgress.Error(e.message ?: "ZIP creation failed", e))
        }
    }.flowOn(Dispatchers.IO)
}
