import os

filepath = 'app/src/main/java/com/ripple/filemanager/FileRepository.kt'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

shizuku_listing = """
        // 1.5 Try Shizuku
        if (hasShizuku()) {
            try {
                val process = rikka.shizuku.Shizuku.newProcess(arrayOf("sh", "-c", "stat -c '%F|%n|%s|%Y' \\"\\"/* \\"\\"/.* 2>/dev/null"), null, null)
                val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val parts = line!!.split("|", limit = 4)
                    if (parts.size == 4) {
                        val fileTypeStr = parts[0]
                        val filePath = parts[1]
                        val sizeBytes = parts[2].toLongOrNull() ?: 0L
                        val lastModSec = parts[3].toLongOrNull() ?: 0L
                        
                        val name = filePath.substringAfterLast("/")
                        if (name == "." || name == ".." || name.startsWith(".")) continue
                        
                        val isFolder = fileTypeStr.contains("directory")
                        val ext = filePath.substringAfterLast(".", "").lowercase(java.util.Locale.getDefault())
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
                        
                        val sizeStr = if (isFolder) "" else formatSize(sizeBytes)
                        val changedStr = dateFormat.format(java.util.Date(lastModSec * 1000))
                        
                        items.add(FileItem(
                            id = idCounter++,
                            path = filePath,
                            name = name,
                            type = type,
                            kind = kind,
                            size = sizeStr,
                            changed = changedStr,
                            owner = "Me",
                            isPinned = false,
                            isEmptyFolder = (isFolder && sizeBytes == 0L),
                            sizeBytes = if (isFolder) 0 else sizeBytes,
                            lastModified = lastModSec * 1000,
                            duration = null
                        ))
                    }
                }
                process.waitFor()
                if (items.isNotEmpty()) return items
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // 2. Try SAF
"""

target = """        // 2. Try SAF"""
content = content.replace(target, shizuku_listing)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)
print("Added Shizuku listing logic")
