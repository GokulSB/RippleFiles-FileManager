import re

with open('app/src/main/java/com/ripple/filemanager/FileRepository.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Fix moveToTrash
old_move_to_trash = '''        paths.forEach { path ->
            val sourceFile = File(path)
            if (sourceFile.exists()) {
                val encodedName = android.util.Base64.encodeToString(path.toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                val destFile = File(trashDir, encodedName)
                if (sourceFile.renameTo(destFile) || (sourceFile.copyRecursively(destFile, overwrite = true) && sourceFile.deleteRecursively())) {
                    val entry = JSONObject().apply {
                        put("encodedName", encodedName)
                        put("originalPath", path)
                        put("deletedAt", System.currentTimeMillis())
                        put("originalName", sourceFile.name)
                        put("isDirectory", sourceFile.isDirectory)
                    }
                    registry.put(entry)
                }
            }
        }'''
new_move_to_trash = '''        paths.forEach { path ->
            try {
                val sourceFile = File(path)
                if (sourceFile.exists()) {
                    val encodedName = android.util.Base64.encodeToString(path.toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                    val destFile = File(trashDir, encodedName)
                    if (sourceFile.renameTo(destFile) || (sourceFile.copyRecursively(destFile, overwrite = true) && sourceFile.deleteRecursively())) {
                        val entry = JSONObject().apply {
                            put("encodedName", encodedName)
                            put("originalPath", path)
                            put("deletedAt", System.currentTimeMillis())
                            put("originalName", sourceFile.name)
                            put("isDirectory", sourceFile.isDirectory)
                        }
                        registry.put(entry)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }'''
content = content.replace(old_move_to_trash, new_move_to_trash)

# Fix moveFiles
old_move_files = '''        sourcePaths.forEach { path ->
            val sourceFile = File(path)
            if (sourceFile.exists()) {
                val targetFile = File(dest, sourceFile.name)
                if (!sourceFile.renameTo(targetFile)) {
                    copyRecursivelyWithProgress(sourceFile, targetFile) { bytesRead ->
                        copiedBytes += bytesRead
                        if (totalBytes > 0) {
                            onProgress(copiedBytes.toFloat() / totalBytes.toFloat())
                        }
                    }
                    sourceFile.deleteRecursively()
                } else {
                    // Estimate size for fast rename
                    if (targetFile.isDirectory) {
                        val size = targetFile.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                        copiedBytes += size
                    } else {
                        copiedBytes += targetFile.length()
                    }
                    if (totalBytes > 0) {
                        onProgress(copiedBytes.toFloat() / totalBytes.toFloat())
                    }
                }
            }
        }'''
new_move_files = '''        sourcePaths.forEach { path ->
            try {
                val sourceFile = File(path)
                if (sourceFile.exists()) {
                    val targetFile = File(dest, sourceFile.name)
                    if (!sourceFile.renameTo(targetFile)) {
                        copyRecursivelyWithProgress(sourceFile, targetFile) { bytesRead ->
                            copiedBytes += bytesRead
                            if (totalBytes > 0) {
                                onProgress(copiedBytes.toFloat() / totalBytes.toFloat())
                            }
                        }
                        sourceFile.deleteRecursively()
                    } else {
                        // Estimate size for fast rename
                        if (targetFile.isDirectory) {
                            val size = targetFile.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                            copiedBytes += size
                        } else {
                            copiedBytes += targetFile.length()
                        }
                        if (totalBytes > 0) {
                            onProgress(copiedBytes.toFloat() / totalBytes.toFloat())
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }'''
content = content.replace(old_move_files, new_move_files)

# Fix deleteFiles
old_delete_files = '''    suspend fun deleteFiles(paths: List<String>) = withContext(Dispatchers.IO) {
        paths.forEach { path ->
            val file = File(path)
            if (file.exists()) {
                file.deleteRecursively()
            }
        }
    }'''
new_delete_files = '''    suspend fun deleteFiles(paths: List<String>) = withContext(Dispatchers.IO) {
        paths.forEach { path ->
            try {
                val file = File(path)
                if (file.exists()) {
                    file.deleteRecursively()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }'''
content = content.replace(old_delete_files, new_delete_files)

# Fix copyFiles
old_copy_files = '''        sourcePaths.forEach { path ->
            val sourceFile = File(path)
            if (sourceFile.exists()) {
                val targetFile = File(dest, sourceFile.name)
                copyRecursivelyWithProgress(sourceFile, targetFile) { bytesRead ->
                    copiedBytes += bytesRead
                    if (totalBytes > 0) {
                        onProgress(copiedBytes.toFloat() / totalBytes.toFloat())
                    }
                }
            }
        }'''
new_copy_files = '''        sourcePaths.forEach { path ->
            try {
                val sourceFile = File(path)
                if (sourceFile.exists()) {
                    val targetFile = File(dest, sourceFile.name)
                    copyRecursivelyWithProgress(sourceFile, targetFile) { bytesRead ->
                        copiedBytes += bytesRead
                        if (totalBytes > 0) {
                            onProgress(copiedBytes.toFloat() / totalBytes.toFloat())
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }'''
content = content.replace(old_copy_files, new_copy_files)

with open('app/src/main/java/com/ripple/filemanager/FileRepository.kt', 'w', encoding='utf-8') as f:
    f.write(content)
