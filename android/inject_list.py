import os

with open('app/src/main/java/com/ripple/filemanager/FileRepository.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Add listRestrictedFiles function
list_restricted = '''
    private fun listRestrictedFiles(location: String, dateFormat: java.text.SimpleDateFormat): List<FileItem>? {
        if (!location.contains("Android/data") && !location.contains("Android/obb")) return null
        
        val items = mutableListOf<FileItem>()
        var idCounter = 10000

        // 1. Try Root
        if (hasRoot()) {
            val files = SuFile(location).listFiles() ?: return emptyList()
            for (file in files) {
                if (file.name.startsWith(".")) continue
                val isFolder = file.isDirectory
                val ext = file.extension.lowercase(java.util.Locale.getDefault())
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
                val count = if (isFolder) file.listFiles()?.count { !it.name.startsWith(".") } ?: 0 else 0
                val sizeStr = if (isFolder) "\ items" else formatSize(file.length())
                val changedStr = dateFormat.format(java.util.Date(file.lastModified()))
                
                items.add(FileItem(
                    id = idCounter++,
                    path = file.absolutePath,
                    name = file.name,
                    type = type,
                    kind = kind,
                    size = sizeStr,
                    changed = changedStr,
                    owner = "Me",
                    isPinned = false,
                    isEmptyFolder = (isFolder && count == 0),
                    sizeBytes = if (isFolder) 0 else file.length(),
                    lastModified = file.lastModified(),
                    duration = null
                ))
            }
            return items
        }
        
        // 2. Try SAF
        if (hasSafPermission(location)) {
            val docs = getDocumentFile(location)?.listFiles() ?: return emptyList()
            for (doc in docs) {
                val name = doc.name ?: continue
                if (name.startsWith(".")) continue
                val isFolder = doc.isDirectory
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
                val count = if (isFolder) doc.listFiles().count { !(it.name?.startsWith(".") ?: true) } else 0
                val sizeStr = if (isFolder) "\ items" else formatSize(doc.length())
                val changedStr = dateFormat.format(java.util.Date(doc.lastModified()))
                
                items.add(FileItem(
                    id = idCounter++,
                    path = location + "/" + name,
                    name = name,
                    type = type,
                    kind = kind,
                    size = sizeStr,
                    changed = changedStr,
                    owner = "Me",
                    isPinned = false,
                    isEmptyFolder = (isFolder && count == 0),
                    sizeBytes = if (isFolder) 0 else doc.length(),
                    lastModified = doc.lastModified(),
                    duration = null
                ))
            }
            return items
        }
        
        // 3. Try Shizuku via shell fallback
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
                        val fullPath = parts[3].trim('\'')
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
        }
        
        return emptyList()
    }
'''

content = content.replace("class FileRepository(private val context: Context) {", "class FileRepository(private val context: Context) {" + list_restricted)

with open('app/src/main/java/com/ripple/filemanager/FileRepository.kt', 'w', encoding='utf-8') as f:
    f.write(content)
