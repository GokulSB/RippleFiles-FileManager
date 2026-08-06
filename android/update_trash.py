import os

filepath = 'app/src/main/java/com/ripple/filemanager/FileRepository.kt'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

moveToTrash_replacement = """
    suspend fun moveToTrash(paths: List<String>) = withContext(Dispatchers.IO) {
        if (!trashDir.exists()) trashDir.mkdirs()
        
        val noMedia = File(trashDir, ".nomedia")
        if (!noMedia.exists()) noMedia.createNewFile()

        val registryFile = File(trashDir, "trash_registry.json")
        val registry = if (registryFile.exists()) {
            JSONArray(registryFile.readText())
        } else {
            JSONArray()
        }

        paths.forEach { path ->
            try {
                val sourceFile = File(path)
                val encodedName = android.util.Base64.encodeToString(path.toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                val destFile = File(trashDir, encodedName)
                
                var success = false
                val isRestricted = path.contains("Android/data") || path.contains("Android/obb")
                
                if (isRestricted && hasShizuku()) {
                    val destPath = destFile.absolutePath
                    val cpCmd = "cp -a \\"\\" \\"\\" && rm -rf \\"\\""
                    success = executeShizukuCommand(cpCmd)
                } else if (isRestricted && hasRoot()) {
                    val suSource = com.topjohnwu.superuser.io.SuFile(path)
                    success = suSource.renameTo(destFile)
                    if (!success) {
                        success = suSource.copyRecursively(destFile, overwrite = true)
                        if (success) suSource.deleteRecursively()
                    }
                } else {
                    if (sourceFile.exists()) {
                        success = sourceFile.renameTo(destFile) || (sourceFile.copyRecursively(destFile, overwrite = true) && sourceFile.deleteRecursively())
                    }
                }

                if (success) {
                    val entry = JSONObject().apply {
                        put("encodedName", encodedName)
                        put("originalPath", path)
                        put("deletedAt", System.currentTimeMillis())
                        put("originalName", sourceFile.name)
                        put("isDirectory", sourceFile.isDirectory)
                    }
                    registry.put(entry)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        registryFile.writeText(registry.toString())
    }
"""

# Extract the existing moveToTrash function string using a regex or simple split
import re
pattern = re.compile(r'    suspend fun moveToTrash\(paths: List<String>\).*?registryFile\.writeText\(registry\.toString\(\)\)\n    \}', re.DOTALL)
content = re.sub(pattern, moveToTrash_replacement.strip(), content)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)
print("Updated moveToTrash")
