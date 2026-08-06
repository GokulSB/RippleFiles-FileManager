import os
import re

filepath = 'app/src/main/java/com/ripple/filemanager/FileRepository.kt'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Add suspend to listRestrictedFiles
content = content.replace("private fun listRestrictedFiles(", "private suspend fun listRestrictedFiles(")

# 2. Add Shizuku Service fetching logic and remove executeShizukuCommand
service_logic = """
    private var shizukuFileService: com.ripple.filemanager.shizuku.IFileService? = null

    private suspend fun getShizukuService(): com.ripple.filemanager.shizuku.IFileService? {
        if (!hasShizuku()) return null
        if (shizukuFileService != null) return shizukuFileService
        
        return kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            val args = rikka.shizuku.Shizuku.UserServiceArgs(
                android.content.ComponentName(context.packageName, com.ripple.filemanager.shizuku.FileUserService::class.java.name)
            )
            .daemon(false)
            .processNameSuffix("file_service")
            .debuggable(BuildConfig.DEBUG)
            .version(1)

            val connection = object : android.content.ServiceConnection {
                override fun onServiceConnected(name: android.content.ComponentName?, binder: android.os.IBinder?) {
                    shizukuFileService = com.ripple.filemanager.shizuku.IFileService.Stub.asInterface(binder)
                    if (cont.isActive) cont.resume(shizukuFileService, null)
                }

                override fun onServiceDisconnected(name: android.content.ComponentName?) {
                    shizukuFileService = null
                }
            }
            
            try {
                rikka.shizuku.Shizuku.bindUserService(args, connection)
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(null, null)
            }
        }
    }
    
    suspend fun deleteRestrictedPath(path: String): Boolean {
        if (path.contains("Android/data") || path.contains("Android/obb")) {
            if (hasShizuku()) {
                return getShizukuService()?.deleteFile(path) ?: false
            }
            if (hasRoot()) {
                return com.topjohnwu.superuser.io.SuFile(path).deleteRecursively()
            }
        }
        return java.io.File(path).deleteRecursively()
    }
"""
# Replace executeShizukuCommand and deleteRestrictedPath
import re
pattern = re.compile(r'    fun executeShizukuCommand.*?    fun hasRoot', re.DOTALL)
content = re.sub(pattern, service_logic.strip() + '\n\n    fun hasRoot', content)

# 3. Replace listRestrictedFiles shizuku branch
shizuku_list_branch = """
        // 1.5 Try Shizuku
        if (hasShizuku()) {
            try {
                val service = getShizukuService()
                if (service != null) {
                    val rawFiles = service.listFiles(location)
                    if (rawFiles != null && rawFiles.isNotEmpty()) {
                        for (line in rawFiles) {
                            val parts = line.split("|", limit = 4)
                            if (parts.size == 4) {
                                val fileTypeStr = parts[0]
                                val name = parts[1]
                                val sizeBytes = parts[2].toLongOrNull() ?: 0L
                                val lastModSec = parts[3].toLongOrNull() ?: 0L
                                
                                if (name == "." || name == ".." || name.startsWith(".")) continue
                                
                                val isFolder = fileTypeStr == "directory"
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
                                val sizeStr = if (isFolder) "" else formatSize(sizeBytes)
                                val changedStr = dateFormat.format(java.util.Date(lastModSec * 1000))
                                val filePath = "/"
                                
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
                        if (items.isNotEmpty()) return items
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
"""
pattern_shizuku_list = re.compile(r'        // 1\.5 Try Shizuku.*?        // 2\. Try SAF', re.DOTALL)
content = re.sub(pattern_shizuku_list, shizuku_list_branch.strip() + '\n\n        // 2. Try SAF', content)

# 4. Replace moveToTrash shizuku usage
trash_replace = """
                if (isRestricted && hasShizuku()) {
                    val service = getShizukuService()
                    if (service != null) {
                        success = service.copyFile(path, destFile.absolutePath)
                        if (success) service.deleteFile(path)
                    }
                }
"""
pattern_trash = re.compile(r'                if \(isRestricted && hasShizuku\(\)\) \{.*?                \} else if', re.DOTALL)
content = re.sub(pattern_trash, trash_replace.strip() + ' else if', content)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)
print("Updated FileRepository with AIDL logic")
