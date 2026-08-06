import os

with open('app/src/main/java/com/ripple/filemanager/FileRepository.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Fix dateFormat scope
content = content.replace(
'''            if (location.contains("Android/data") || location.contains("Android/obb")) {
                val restrictedItems = listRestrictedFiles(location, dateFormat)
                if (restrictedItems != null) return@withContext restrictedItems
            }''',
'''            val dateFormatForRestricted = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            if (location.contains("Android/data") || location.contains("Android/obb")) {
                val restrictedItems = listRestrictedFiles(location, dateFormatForRestricted)
                if (restrictedItems != null) return@withContext restrictedItems
            }'''
)

# Remove Shizuku ls logic due to API issues
shizuku_block = '''        // 3. Try Shizuku via shell fallback
        if (hasShizuku()) {
            try {
                val process = Shizuku.newProcess(arrayOf("sh", "-c", "stat -c '%F|%s|%Y|%n' '\/'*"), null, null)
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val parts = line!!.split("|", limit = 4)
                    if (parts.size == 4) {
                        val isFolder = parts[0] == "directory"
                        val size = parts[1].toLongOrNull() ?: 0L
                        val lastMod = (parts[2].toLongOrNull() ?: 0L) * 1000L
                        val fullPath = parts[3].trim('\\'')
                        val name = fullPath.substringAfterLast("/")
                        if (name.startsWith(".") || name == "*") continue
                        
                        val ext = name.substringAfterLast(".", "").lowercase(java.util.Locale.getDefault())
                        val type = when {
                            isFolder -> "folder"
                            ext in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif", "svg") -> "image"
                            ext in listOf("mp4", "mkv", "avi", "mov", "webm") -> "video"
                            ext in listOf("mp3", "wav", "ogg", "flac", "m4a") -> "audio"
                            ext in listOf("zip", "rar", "7z", "tar", "gz") -> "archive"
                            ext in listOf("pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx") -> "doc"
                            ext == "apk" -> "apk"
                            else -> "file"
                        }
                        val kind = when (type) {
                            "folder" -> "folder"
                            "image", "video", "audio" -> "media"
                            "archive" -> "archive"
                            "doc" -> "doc"
                            else -> "file"
                        }
                        
                        val sizeStr = if (isFolder) "Folder" else formatSize(size)
                        val changedStr = dateFormat.format(java.util.Date(lastMod))
                        
                        items.add(FileItem(
                            id = idCounter++,
                            path = fullPath,
                            name = name,
                            type = type,
                            kind = kind,
                            size = sizeStr,
                            changed = changedStr,
                            owner = "Me",
                            isPinned = false,
                            isEmptyFolder = false,
                            sizeBytes = if (isFolder) 0 else size,
                            lastModified = lastMod,
                            duration = null
                        ))
                    }
                }
                return items
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }'''

content = content.replace(shizuku_block, "")

with open('app/src/main/java/com/ripple/filemanager/FileRepository.kt', 'w', encoding='utf-8') as f:
    f.write(content)
