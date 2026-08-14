package com.ripple.filemanager

import android.content.Context
import be.stef.rar.Unrar5j
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object ArchiveService {

    suspend fun listContents(file: File): List<String> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<String>()
        val extension = file.extension.lowercase()

        try {
            when (extension) {
                "zip" -> {
                    ZipFile(file).use { zip ->
                        val iter = zip.entries()
                        while (iter.hasMoreElements()) {
                            entries.add(iter.nextElement().name)
                        }
                    }
                }
                "rar" -> {

                    val tempDir = File(System.getProperty("java.io.tmpdir"), "rar_list_${System.currentTimeMillis()}")
                    tempDir.mkdirs()
                    try {
                        val result = Unrar5j.extract(file.absolutePath, tempDir.absolutePath, null)
                        if (result.passwordStatus == 2) {
                            throw Exception("Archive is password-protected")
                        }
                        tempDir.walkTopDown().forEach { extracted ->
                            if (extracted != tempDir) {
                                entries.add(extracted.relativeTo(tempDir).path.replace('\\', '/'))
                            }
                        }
                    } finally {
                        tempDir.deleteRecursively()
                    }
                }
                "iso" -> {
                    // val reader = com.palantir.isofilereader.IsoFileReader(file)
                    // val files = reader.allFiles
                    // for (isoFile in files) {
                    //     entries.add(isoFile.name)
                    // }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }

        entries
    }

    suspend fun extractArchive(file: File, destDir: File): Boolean = withContext(Dispatchers.IO) {
        if (!destDir.exists()) {
            destDir.mkdirs()
        }
        val extension = file.extension.lowercase()

        try {
            when (extension) {
                "zip" -> extractZip(file, destDir)
                "rar" -> extractRar(file, destDir)
                "iso" -> extractIso(file, destDir)
                else -> false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun extractZip(file: File, destDir: File): Boolean {
        try {
            ZipFile(file).use { zip ->
                val iter = zip.entries()
                while (iter.hasMoreElements()) {
                    val entry = iter.nextElement()
                    val targetFile = File(destDir, entry.name)
                    
                    if (!targetFile.canonicalPath.startsWith(destDir.canonicalPath)) {
                        continue
                    }

                    if (entry.isDirectory) {
                        targetFile.mkdirs()
                    } else {
                        targetFile.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(targetFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun extractRar(file: File, destDir: File): Boolean {
        return try {

            val result = Unrar5j.extract(file.absolutePath, destDir.absolutePath, null)
            result.errorCount == 0 && result.passwordStatus != 2
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun extractIso(file: File, destDir: File): Boolean {
        try {
            // val reader = com.palantir.isofilereader.IsoFileReader(file)
            // val files = reader.allFiles
            // for (isoFile in files) {
            //     val targetFile = File(destDir, isoFile.name)
            //     if (!targetFile.canonicalPath.startsWith(destDir.canonicalPath)) {
            //         continue
            //     }
            //     targetFile.parentFile?.mkdirs()
            //     isoFile.inputStream.use { input ->
            //         FileOutputStream(targetFile).use { output ->
            //             input.copyTo(output)
            //         }
            //     }
            // }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
