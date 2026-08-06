package com.ripple.filemanager.shizuku

import java.io.File
import com.ripple.filemanager.shizuku.IFileService

class FileUserService : IFileService.Stub() {
    private fun getAbsolutePath(path: String): String {
        return if (path.startsWith("/")) {
            path
        } else {
            "/storage/emulated/0/$path"
        }
    }

    override fun listFiles(path: String): List<String> {
        val absolutePath = getAbsolutePath(path)
        val dir = File(absolutePath)
        val files = dir.listFiles()
        
        if (files == null || files.isEmpty()) {
            // Fallback to shell command if Java API is blocked by FUSE
            return try {
                val process = ProcessBuilder("sh", "-c", "ls -1Ap \"$absolutePath\"")
                    .redirectErrorStream(true)
                    .start()
                val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                val result = mutableListOf<String>()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val p = line ?: continue
                    if (p.isEmpty()) continue
                    val isDir = p.endsWith("/")
                    val name = if (isDir) p.dropLast(1) else p
                    if (name == "." || name == "..") continue
                    val type = if (isDir) "directory" else "file"
                    result.add("$type|$name|0|0")
                }
                process.waitFor()
                result
            } catch (e: Exception) {
                emptyList()
            }
        }
        
        return files.map { file ->
            // Format: type|name|sizeBytes|lastModified
            val isDir = file.isDirectory
            val type = if (isDir) "directory" else "file"
            val size = file.length()
            val modified = file.lastModified() / 1000 // Convert to seconds to match previous logic
            "$type|${file.name}|$size|$modified"
        }
    }

    override fun deleteFile(path: String): Boolean {
        val absolutePath = getAbsolutePath(path)
        val success = File(absolutePath).deleteRecursively()
        if (!success) {
            return try {
                val process = ProcessBuilder("sh", "-c", "rm -rf \"$absolutePath\"")
                    .redirectErrorStream(true)
                    .start()
                process.inputStream.bufferedReader().readText() // Consume output to prevent hang
                process.waitFor() == 0
            } catch (e: Exception) {
                false
            }
        }
        return true
    }

    override fun copyFile(src: String, dest: String): Boolean {
        val absoluteSrc = getAbsolutePath(src)
        val absoluteDest = getAbsolutePath(dest)
        return try {
            File(absoluteSrc).copyRecursively(File(absoluteDest), overwrite = true)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to shell command if physical path fails
            try {
                val process = ProcessBuilder("sh", "-c", "cp -r \"$absoluteSrc\" \"$absoluteDest\"")
                    .redirectErrorStream(true)
                    .start()
                process.inputStream.bufferedReader().readText() // Consume output
                process.waitFor() == 0
            } catch (ex: Exception) {
                false
            }
        }
    }

    override fun renameFile(src: String, dest: String): Boolean {
        val absoluteSrc = getAbsolutePath(src)
        val absoluteDest = getAbsolutePath(dest)
        val success = File(absoluteSrc).renameTo(File(absoluteDest))
        if (!success) {
            return try {
                val process = ProcessBuilder("sh", "-c", "mv \"$absoluteSrc\" \"$absoluteDest\"")
                    .redirectErrorStream(true)
                    .start()
                process.inputStream.bufferedReader().readText() // Consume output
                process.waitFor() == 0
            } catch (e: Exception) {
                false
            }
        }
        return true
    }
}
