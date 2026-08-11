package com.ripple.filemanager

import androidx.compose.runtime.Immutable

import android.content.Context
import android.os.Environment
import android.provider.DocumentsContract
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import java.io.File
import java.io.InputStream
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.accounts.Account
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import org.json.JSONArray
import org.json.JSONObject

enum class SortMode { ALPHABETICAL, DATE, SIZE }

@Immutable
data class FileItem(
    val id: Int,
    val path: String,
    val name: String,
    val type: String,
    val kind: String,
    val size: String,
    val changed: String,
    val owner: String,
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val isEmptyFolder: Boolean = false,
    val sizeBytes: Long = 0,
    val lastModified: Long = 0,
    val originalPath: String? = null,
    val encodedTrashName: String? = null,
    val duration: String? = null,
    val thumbnailLink: String? = null
)

data class FileDetails(
    val name: String,
    val path: String,
    val isFolder: Boolean,
    val size: String,
    val changed: String,
    val owner: String,
    val itemCount: Int? = null,
    val resolution: String? = null,
    val duration: String? = null,
    val format: String? = null
)

class FileRepository(private val context: Context) {
    private suspend fun listRestrictedFiles(location: String, dateFormat: java.text.SimpleDateFormat): List<FileItem>? {
        if (!location.contains("Android/data") && !location.contains("Android/obb")) return null
        
        val items = mutableListOf<FileItem>()
        var idCounter = 10000

        // 1. Try Shizuku via direct newProcess (avoids binder timeouts and Shevery IPC crashes)
        if (hasShizuku()) {
            try {
                val m = rikka.shizuku.Shizuku::class.java.getDeclaredMethods().find { it.name == "newProcess" }
                m?.isAccessible = true
                val process = m?.invoke(null, arrayOf("sh", "-c", "ls -1Ap \"$location\" 2>/dev/null"), null, null) as? Process
                if (process == null) return emptyList()
                val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val p = line ?: continue
                    if (p.isEmpty()) continue
                    val isDir = p.endsWith("/")
                    val name = if (isDir) p.dropLast(1) else p
                    if (name == "." || name == ".." || name.startsWith(".")) continue
                    
                    val fileTypeStr = if (isDir) "directory" else "file"
                    val sizeBytes = 0L
                    val lastModSec = 0L
                                
                    val type = when {
                        isDir -> "folder"
                        else -> {
                            val ext = name.substringAfterLast(".", "").lowercase(java.util.Locale.getDefault())
                            when (ext) {
                                "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif", "svg" -> "image"
                                "mp4", "mkv", "avi", "mov", "webm" -> "video"
                                "mp3", "wav", "ogg", "flac", "m4a" -> "audio"
                                "zip", "rar", "7z", "tar", "gz" -> "archive"
                                "pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx" -> "doc"
                                "apk" -> "apk"
                                else -> "file"
                            }
                        }
                    }
                    val kind = when (type) {
                        "folder" -> "folder"
                        "image", "video", "audio" -> "media"
                        "archive" -> "archive"
                        "doc" -> "doc"
                        else -> "file"
                    }
                    val sizeStr = if (isDir) "" else formatSize(sizeBytes)
                    val changedStr = ""
                    val filePath = if (location.endsWith("/")) location + name else "$location/$name"
                    
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
                        isEmptyFolder = (isDir && sizeBytes == 0L),
                        sizeBytes = if (isDir) 0 else sizeBytes,
                        lastModified = lastModSec * 1000,
                        duration = null
                    ))
                }
                process.waitFor()
                if (items.isNotEmpty()) return items
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return emptyList()
    }

    private fun runShizukuCommand(cmd: String): Boolean {
        return try {
            val m = rikka.shizuku.Shizuku::class.java.getDeclaredMethods().find { it.name == "newProcess" }
            m?.isAccessible = true
            val process = m?.invoke(null, arrayOf("sh", "-c", cmd), null, null) as? Process
            process?.waitFor() == 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun deleteRestrictedPath(path: String): Boolean {
        if (path.contains("Android/data") || path.contains("Android/obb")) {
            if (hasShizuku()) {
                return runShizukuCommand("rm -rf \"$path\"")
            }
        }
        return java.io.File(path).deleteRecursively()
    }

    fun hasShizuku(): Boolean {
        return try {
            Shizuku.pingBinder() && Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    private val prefs = context.getSharedPreferences("sift_prefs", Context.MODE_PRIVATE)
    private var googleAccountEmail: String? = null
    private var driveService: Drive? = null
    
    val currentGoogleAccountEmail: String?
        get() = prefs.getString("google_drive_email", null)

    init {
        val email = prefs.getString("google_drive_email", null)
        if (email != null) {
            setGoogleAccount(email)
        }
    }

    fun setGoogleAccount(email: String?) {
        googleAccountEmail = email
        if (email != null) {
            val credential = GoogleAccountCredential.usingOAuth2(
                context, listOf(DriveScopes.DRIVE)
            )
            credential.selectedAccount = Account(email, "com.google")
            driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("Sift").build()
            prefs.edit().putString("google_drive_email", email).apply()
        } else {
            driveService = null
            prefs.edit().remove("google_drive_email").apply()
        }
    }

    fun getPinnedFiles(): Set<String> {
        return prefs.getStringSet("pinned_files", emptySet()) ?: emptySet()
    }

    fun togglePin(path: String) {
        val current = getPinnedFiles().toMutableSet()
        if (current.contains(path)) current.remove(path) else current.add(path)
        prefs.edit().putStringSet("pinned_files", current).apply()
    }

    fun getLockedFiles(): Set<String> = prefs.getStringSet("locked_files", emptySet()) ?: emptySet()
    
    fun setLocked(path: String, locked: Boolean) {
        val current = getLockedFiles().toMutableSet()
        if (locked) current.add(path) else current.remove(path)
        prefs.edit().putStringSet("locked_files", current).apply()
    }
    
    fun getGlobalPasswordHash(): String = prefs.getString("lock_password_hash", null) ?: hashPassword("0000")
    fun setGlobalPasswordHash(hash: String?) = prefs.edit().putString("lock_password_hash", hash).apply()
    fun isBiometricEnabled(): Boolean = prefs.getBoolean("lock_biometric_enabled", false)
    fun setBiometricEnabled(enabled: Boolean) = prefs.edit().putBoolean("lock_biometric_enabled", enabled).apply()
    
    fun hashPassword(password: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }


    fun logRecentAction(path: String, action: String) {
        if (!path.startsWith("/")) return
        try {
            val historyStr = prefs.getString("recent_actions", "[]")
            val history = JSONArray(historyStr)
            val newAction = JSONObject().apply {
                put("path", path)
                put("action", action)
                put("timestamp", System.currentTimeMillis())
            }
            
            val newHistory = JSONArray()
            newHistory.put(newAction)
            
            var addedCount = 1
            for (i in 0 until history.length()) {
                if (addedCount >= 10) break
                val item = history.getJSONObject(i)
                if (item.getString("path") != path) {
                    newHistory.put(item)
                    addedCount++
                }
            }
            prefs.edit().putString("recent_actions", newHistory.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun createFolder(parentPath: String, name: String): Boolean = withContext(Dispatchers.IO) {
        if (parentPath == "recent" || parentPath == "pinned") return@withContext false
        if (parentPath.startsWith("drive_id:") || parentPath == "drive") {
            if (driveService == null) return@withContext false
            return@withContext try {
                val folder = com.google.api.services.drive.model.File().apply {
                    this.name = name
                    this.mimeType = "application/vnd.google-apps.folder"
                    if (parentPath != "drive") {
                        val parentId = parentPath.removePrefix("drive_id:")
                        this.parents = listOf(parentId)
                    }
                }
                driveService!!.files().create(folder).execute()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        val actualPath = if (parentPath == "home") Environment.getExternalStorageDirectory().absolutePath else parentPath
        val newFolder = File(actualPath, name)
        if (!newFolder.exists()) {
            return@withContext newFolder.mkdirs()
        }
        return@withContext false
    }

    suspend fun createFile(parentPath: String, name: String): Boolean = withContext(Dispatchers.IO) {
        if (parentPath == "recent" || parentPath == "pinned") return@withContext false
        if (parentPath.startsWith("drive_id:") || parentPath == "drive") {
            if (driveService == null) return@withContext false
            return@withContext try {
                val file = com.google.api.services.drive.model.File().apply {
                    this.name = name
                    if (parentPath != "drive") {
                        val parentId = parentPath.removePrefix("drive_id:")
                        this.parents = listOf(parentId)
                    }
                }
                driveService!!.files().create(file).execute()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        val actualPath = if (parentPath == "home") Environment.getExternalStorageDirectory().absolutePath else parentPath
        val newFile = File(actualPath, name)
        if (!newFile.exists()) {
            return@withContext try {
                newFile.createNewFile()
            } catch (e: Exception) {
                false
            }
        }
        return@withContext false
    }

    suspend fun getFiles(location: String): List<FileItem> = withContext(Dispatchers.IO) {
        val pinned = getPinnedFiles()
        val locked = getLockedFiles()
        
        if (location == "drive" || location.startsWith("drive_id:")) {
            if (driveService != null) {
                return@withContext fetchDriveFiles(pinned, location)
            } else {
                return@withContext emptyList()
            }
        }
        
        val files = if (location == "pinned") {
            pinned.mapNotNull { path ->
                if (path.startsWith("drive_id:")) null else {
                    val f = File(path)
                    if (f.exists()) f else null
                }
            }.toTypedArray()
        } else if (location == "recent") {
            emptyArray<File>() // handled differently below
        } else {
            val dateFormatForRestricted = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            if (location.contains("Android/data") || location.contains("Android/obb")) {
                val restrictedItems = listRestrictedFiles(location, dateFormatForRestricted)
                if (restrictedItems != null) return@withContext restrictedItems
            }
            val rootDir = when (location) {
                "home" -> Environment.getExternalStorageDirectory()
                else -> File(location)
            }
            rootDir.listFiles() ?: return@withContext emptyList()
        }

        val items = mutableListOf<FileItem>()
        var idCounter = 1

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        if (location == "recent") {
            try {
                val historyStr = prefs.getString("recent_actions", "[]")
                val history = JSONArray(historyStr)
                for (i in 0 until history.length()) {
                    val item = history.getJSONObject(i)
                    val path = item.getString("path")
                    val action = item.getString("action")
                    val timestamp = item.getLong("timestamp")
                    
                    val file = File(path)
                    val name = if (file.exists()) file.name else path.substringAfterLast("/")
                    val isFolder = file.exists() && file.isDirectory
                    val ext = file.extension.lowercase(Locale.getDefault())

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
                    
                    // Display relative time for timestamp, e.g. "Just now", "2 mins ago"
                    val diff = System.currentTimeMillis() - timestamp
                    val timeStr = when {
                        diff < 60_000 -> "Just now"
                        diff < 3600_000 -> "${diff / 60_000} mins ago"
                        diff < 86400_000 -> "${diff / 3600_000} hours ago"
                        else -> "${diff / 86400_000} days ago"
                    }
                    
                    val displayAction = action.replace("Explored", "Visited")
                    
                    var durationStr: String? = null
                    if (type == "video" && file.exists()) {
                        try {
                            val retriever = android.media.MediaMetadataRetriever()
                            retriever.setDataSource(file.absolutePath)
                            val timeMillis = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                            if (timeMillis != null) {
                                val secs = (timeMillis / 1000) % 60
                                val mins = (timeMillis / (1000 * 60)) % 60
                                val hours = timeMillis / (1000 * 60 * 60)
                                durationStr = if (hours > 0) String.format("%02d:%02d:%02d", hours, mins, secs)
                                              else String.format("%02d:%02d", mins, secs)
                            }
                            retriever.release()
                        } catch(e: Exception) {}
                    }
                    
                    items.add(
                        FileItem(
                            id = idCounter++,
                            path = path,
                            name = name,
                            type = type,
                            kind = kind,
                            size = displayAction,
                            changed = timeStr,
                            owner = "Me",
                            isPinned = pinned.contains(path),
                            isLocked = locked.contains(path),
                            isEmptyFolder = false,
                            sizeBytes = 0,
                            lastModified = timestamp,
                            duration = durationStr
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext items
        }

        for (file in files) {
            if (file.name.startsWith(".")) continue

            items.add(fileToItem(file, idCounter++, pinned, locked, dateFormat))
        }

        items.sortedWith(compareBy({ !it.isPinned }, { !it.type.equals("folder") }, { it.name.lowercase() }))
    }

    private fun fileToItem(file: File, id: Int, pinned: Set<String>, locked: Set<String>, dateFormat: SimpleDateFormat): FileItem {
        val isFolder = file.isDirectory
        val ext = file.extension.lowercase(Locale.getDefault())

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

        var actualSizeBytes = file.length()
        if (isFolder) {
            try {
                var sum = 0L
                file.walkTopDown().onEnter { !it.isHidden && it.name != "Android" }.filter { it.isFile }.take(5000).forEach { sum += it.length() }
                if (sum > 0) actualSizeBytes = sum
            } catch(e: Exception) {}
        }

        val count = if (isFolder) {
            file.listFiles()?.count { !it.name.startsWith(".") } ?: 0
        } else 0
        
        val sizeStr = if (isFolder) {
            "$count items"
        } else {
            formatSize(file.length())
        }

        val changedStr = dateFormat.format(Date(file.lastModified()))
        
        var durationStr: String? = null
        if (type == "video" && file.exists()) {
            try {
                val retriever = android.media.MediaMetadataRetriever()
                val fis = java.io.FileInputStream(file)
                retriever.setDataSource(fis.fd)
                val timeMillis = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                fis.close()
                if (timeMillis != null) {
                    val secs = (timeMillis / 1000) % 60
                    val mins = (timeMillis / (1000 * 60)) % 60
                    val hours = timeMillis / (1000 * 60 * 60)
                    durationStr = if (hours > 0) String.format("%02d:%02d:%02d", hours, mins, secs)
                                  else String.format("%02d:%02d", mins, secs)
                }
                retriever.release()
            } catch(e: Exception) {}
        }

        return FileItem(
            id = id,
            path = file.absolutePath,
            name = file.name,
            type = type,
            kind = kind,
            size = sizeStr,
            changed = changedStr,
            owner = "Me",
            isPinned = pinned.contains(file.absolutePath),
            isLocked = locked.contains(file.absolutePath),
            isEmptyFolder = isFolder && count == 0,
            sizeBytes = actualSizeBytes,
            lastModified = file.lastModified(),
            duration = durationStr
        )
    }

    suspend fun searchLocalFiles(baseLocation: String, query: String): List<FileItem> = withContext(Dispatchers.IO) {
        if (baseLocation == "mega" || baseLocation.startsWith("mega_id:") || baseLocation.startsWith("smb_") || baseLocation == "drive" || baseLocation.startsWith("drive_id:") || baseLocation == "recent" || baseLocation == "pinned") {
            return@withContext emptyList()
        }

        val pinned = getPinnedFiles()
        val locked = getLockedFiles()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        
        val rootPath = if (baseLocation == "home") Environment.getExternalStorageDirectory().absolutePath else baseLocation
        val rootDir = File(rootPath)
        val extStorageRoot = Environment.getExternalStorageDirectory().absolutePath
        
        val stack = java.util.Stack<File>()
        stack.push(rootDir)
        
        val results = mutableListOf<FileItem>()
        var idCounter = 1

        while (stack.isNotEmpty() && isActive) {
            val dir = stack.pop()
            val children = dir.listFiles() ?: continue
            
            for (child in children) {
                if (!isActive) break
                if (child.name.startsWith(".")) continue
                
                // Exclude Android folder at the root of internal storage
                if (child.name == "Android" && child.parentFile?.absolutePath == extStorageRoot) {
                    continue
                }
                
                if (child.name.contains(query, ignoreCase = true)) {
                    results.add(fileToItem(child, idCounter++, pinned, locked, dateFormat))
                }
                
                if (child.isDirectory) {
                    stack.push(child)
                }
            }
        }
        
        results
    }

    suspend fun getFileDetails(path: String): FileDetails = withContext(Dispatchers.IO) {
        val file = File(path)
        val isFolder = file.isDirectory
        val ext = file.extension.lowercase(Locale.getDefault())
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        
        var itemCount: Int? = null
        var totalSize = file.length()
        var resolution: String? = null
        var duration: String? = null
        val format = ext.uppercase(Locale.getDefault()).takeIf { it.isNotEmpty() }

        if (isFolder) {
            var filesCount = 0
            fun calculateDirSize(dir: File): Long {
                var size = 0L
                val children = dir.listFiles()
                if (children != null) {
                    for (child in children) {
                        if (child.isFile) {
                            size += child.length()
                            filesCount++
                        } else {
                            size += calculateDirSize(child)
                            filesCount++
                        }
                    }
                }
                return size
            }
            totalSize = calculateDirSize(file)
            itemCount = filesCount
        } else {
            if (ext in listOf("jpg", "jpeg", "png", "webp", "bmp")) {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, options)
                if (options.outWidth > 0 && options.outHeight > 0) {
                    resolution = "${options.outWidth} x ${options.outHeight}"
                }
            } else if (ext in listOf("mp4", "mkv", "avi", "mov", "mp3", "wav", "m4a", "ogg")) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(path)
                    val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    if (width != null && height != null) {
                        resolution = "$width x $height"
                    }
                    val timeMillis = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                    if (timeMillis != null) {
                        val secs = (timeMillis / 1000) % 60
                        val mins = (timeMillis / (1000 * 60)) % 60
                        val hours = timeMillis / (1000 * 60 * 60)
                        duration = if (hours > 0) String.format("%02d:%02d:%02d", hours, mins, secs)
                                   else String.format("%02d:%02d", mins, secs)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    try { retriever.release() } catch (e: Exception) {}
                }
            }
        }

        FileDetails(
            name = file.name,
            path = file.absolutePath,
            isFolder = isFolder,
            size = formatSize(totalSize),
            changed = dateFormat.format(Date(file.lastModified())),
            owner = "Me",
            itemCount = itemCount,
            resolution = resolution,
            duration = duration,
            format = format
        )
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var digitGroups = 0
        while (value >= 1024 && digitGroups < units.size - 1) {
            value /= 1024.0
            digitGroups++
        }
        return String.format(java.util.Locale.US, "%.2f %s", value, units[digitGroups])
    }

    private fun calculateTotalSize(paths: List<String>): Long {
        var total = 0L
        paths.forEach { path ->
            val file = File(path)
            if (file.exists()) {
                if (file.isDirectory) {
                    file.walkTopDown().forEach { if (it.isFile) total += it.length() }
                } else {
                    total += file.length()
                }
            }
        }
        return total
    }

    private suspend fun copyRecursivelyWithProgress(
        source: File,
        dest: File,
        onProgress: (Long) -> Unit,
        checkPause: suspend () -> Unit
    ) {
        if (source.isDirectory) {
            dest.mkdirs()
            source.listFiles()?.forEach { child ->
                copyRecursivelyWithProgress(child, File(dest, child.name), onProgress, checkPause)
            }
        } else {
            source.inputStream().use { input ->
                dest.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } >= 0) {
                        checkPause()
                        output.write(buffer, 0, bytesRead)
                        onProgress(bytesRead.toLong())
                    }
                }
            }
        }
    }

    suspend fun copyFiles(
        sourcePaths: List<String>, 
        destDir: String, 
        onProgress: (Float) -> Unit = {},
        checkPause: suspend () -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        val destIsDrive = destDir == "drive" || destDir.startsWith("drive_id:")
        val destParentId = if (destDir == "drive") "root" else destDir.removePrefix("drive_id:")

        val totalBytes = calculateTotalSize(sourcePaths)
        var copiedBytes = 0L
        
        val dest = if (!destIsDrive) {
            when (destDir) {
                "home" -> Environment.getExternalStorageDirectory()
                "recent" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                else -> File(destDir)
            }
        } else null

        val isDestRestricted = if (destIsDrive) false else destDir.contains("Android/data") || destDir.contains("Android/obb")
        val createdFiles = mutableListOf<String>()

        try {
            sourcePaths.forEach { path ->
                val sourceIsDrive = path.startsWith("drive_id:")
                val sourceId = if (sourceIsDrive) path.removePrefix("drive_id:") else ""
                val sourceName = path.substringAfterLast("/")
                
                checkPause()

                if (sourceIsDrive && destIsDrive) {
                    if (driveService != null) {
                        val newFile = com.google.api.services.drive.model.File()
                        newFile.parents = listOf(destParentId)
                        driveService!!.files().copy(sourceId, newFile).execute()
                        createdFiles.add("drive_id:$sourceId")
                    }
                } else if (sourceIsDrive && !destIsDrive) {
                    if (driveService != null) {
                        val driveFile = driveService!!.files().get(sourceId).setSupportsAllDrives(true).setFields("name, size, mimeType").execute()
                        val actualName = driveFile.name ?: sourceName.replace(":", "_")
                        val size = driveFile.size ?: 0L
                        val destPath = File(dest, actualName).absolutePath
                        val isGoogleDoc = driveFile.mimeType?.startsWith("application/vnd.google-apps.") == true
                        
                        if (isDestRestricted && hasShizuku()) {
                            val tempFile = File(context.cacheDir, actualName)
                            if (size == 0L || isGoogleDoc) {
                                tempFile.createNewFile()
                            } else {
                                val outputStream = java.io.FileOutputStream(tempFile)
                                driveService!!.files().get(sourceId).setSupportsAllDrives(true).executeMediaAndDownloadTo(outputStream)
                                outputStream.close()
                            }
                            val success = runShizukuCommand("cp \"${tempFile.absolutePath}\" \"$destPath\"")
                            tempFile.delete()
                            if (success) createdFiles.add(destPath)
                        } else {
                            val outputFile = File(destPath)
                            if (size == 0L || isGoogleDoc) {
                                outputFile.createNewFile()
                            } else {
                                val outputStream = java.io.FileOutputStream(outputFile)
                                driveService!!.files().get(sourceId).setSupportsAllDrives(true).executeMediaAndDownloadTo(outputStream)
                                outputStream.close()
                            }
                            createdFiles.add(destPath)
                        }
                    }
                } else if (!sourceIsDrive && destIsDrive) {
                    if (driveService != null) {
                        val sourceFile = File(path)
                        if (sourceFile.exists()) {
                            val fileContent = com.google.api.client.http.FileContent("application/octet-stream", sourceFile)
                            val driveFile = com.google.api.services.drive.model.File()
                            driveFile.name = sourceName
                            driveFile.parents = listOf(destParentId)
                            driveService!!.files().create(driveFile, fileContent).execute()
                            createdFiles.add(path)
                        }
                    }
                } else {
                    val isSourceRestricted = path.contains("Android/data") || path.contains("Android/obb")
                    val destPath = File(dest, sourceName).absolutePath

                    if ((isSourceRestricted || isDestRestricted) && hasShizuku()) {
                        val success = runShizukuCommand("cp -r \"$path\" \"$destPath\"")
                        if (success) createdFiles.add(destPath)
                    } else {
                        val sourceFile = File(path)
                        if (sourceFile.exists()) {
                            val targetFile = File(destPath)
                            createdFiles.add(destPath)
                            copyRecursivelyWithProgress(sourceFile, targetFile, { bytesRead ->
                                copiedBytes += bytesRead
                                if (totalBytes > 0) {
                                    onProgress(copiedBytes.toFloat() / totalBytes.toFloat())
                                }
                            }, checkPause)
                        }
                    }
                }
            }
            onProgress(1f)
        } catch (e: kotlinx.coroutines.CancellationException) {
            createdFiles.forEach { path ->
                if (!path.startsWith("drive_id:")) {
                    if (isDestRestricted && hasShizuku()) {
                        runShizukuCommand("rm -rf \"$path\"")
                    } else {
                        File(path).deleteRecursively()
                    }
                }
            }
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun moveFiles(
        sourcePaths: List<String>, 
        destDir: String, 
        onProgress: (Float) -> Unit = {},
        checkPause: suspend () -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        val destIsDrive = destDir == "drive" || destDir.startsWith("drive_id:")
        val destParentId = if (destDir == "drive") "root" else destDir.removePrefix("drive_id:")

        val totalBytes = calculateTotalSize(sourcePaths)
        var copiedBytes = 0L
        
        val dest = if (!destIsDrive) {
            when (destDir) {
                "home" -> Environment.getExternalStorageDirectory()
                "recent" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                else -> File(destDir)
            }
        } else null

        val isDestRestricted = if (destIsDrive) false else destDir.contains("Android/data") || destDir.contains("Android/obb")
        val createdFiles = mutableListOf<String>()

        try {
            sourcePaths.forEach { path ->
                val sourceIsDrive = path.startsWith("drive_id:")
                val sourceId = if (sourceIsDrive) path.removePrefix("drive_id:") else ""
                val sourceName = path.substringAfterLast("/")
                
                checkPause()

                if (sourceIsDrive && destIsDrive) {
                    if (driveService != null) {
                        val file = driveService!!.files().get(sourceId).setFields("parents").execute()
                        val previousParents = file.parents?.joinToString(",")
                        driveService!!.files().update(sourceId, null)
                            .setAddParents(destParentId)
                            .setRemoveParents(previousParents)
                            .setFields("id, parents")
                            .execute()
                    }
                } else if (sourceIsDrive && !destIsDrive) {
                    if (driveService != null) {
                        val driveFile = driveService!!.files().get(sourceId).setSupportsAllDrives(true).setFields("name, size, mimeType").execute()
                        val actualName = driveFile.name ?: sourceName.replace(":", "_")
                        val size = driveFile.size ?: 0L
                        val destPath = File(dest, actualName).absolutePath
                        val isGoogleDoc = driveFile.mimeType?.startsWith("application/vnd.google-apps.") == true
                        
                        if (isDestRestricted && hasShizuku()) {
                            val tempFile = File(context.cacheDir, actualName)
                            if (size == 0L || isGoogleDoc) {
                                tempFile.createNewFile()
                            } else {
                                val outputStream = java.io.FileOutputStream(tempFile)
                                driveService!!.files().get(sourceId).setSupportsAllDrives(true).executeMediaAndDownloadTo(outputStream)
                                outputStream.close()
                            }
                            val success = runShizukuCommand("cp \"${tempFile.absolutePath}\" \"$destPath\"")
                            tempFile.delete()
                            if (success) {
                                driveService!!.files().delete(sourceId).setSupportsAllDrives(true).execute()
                                createdFiles.add(destPath)
                            }
                        } else {
                            val outputFile = File(destPath)
                            if (size == 0L || isGoogleDoc) {
                                outputFile.createNewFile()
                            } else {
                                val outputStream = java.io.FileOutputStream(outputFile)
                                driveService!!.files().get(sourceId).setSupportsAllDrives(true).executeMediaAndDownloadTo(outputStream)
                                outputStream.close()
                            }
                            driveService!!.files().delete(sourceId).setSupportsAllDrives(true).execute()
                            createdFiles.add(destPath)
                        }
                    }
                } else if (!sourceIsDrive && destIsDrive) {
                    if (driveService != null) {
                        val sourceFile = File(path)
                        if (sourceFile.exists()) {
                            val fileContent = com.google.api.client.http.FileContent("application/octet-stream", sourceFile)
                            val driveFile = com.google.api.services.drive.model.File()
                            driveFile.name = sourceName
                            driveFile.parents = listOf(destParentId)
                            driveService!!.files().create(driveFile, fileContent).execute()
                            sourceFile.deleteRecursively()
                            createdFiles.add(path)
                        }
                    }
                } else {
                    val isSourceRestricted = path.contains("Android/data") || path.contains("Android/obb")
                    val destPath = File(dest, sourceName).absolutePath

                    if ((isSourceRestricted || isDestRestricted) && hasShizuku()) {
                        val success = runShizukuCommand("mv \"$path\" \"$destPath\"")
                        if (success) createdFiles.add(destPath)
                    } else {
                        val sourceFile = File(path)
                        if (sourceFile.exists()) {
                            val targetFile = File(destPath)
                            if (sourceFile.renameTo(targetFile)) {
                                createdFiles.add(destPath)
                                copiedBytes += sourceFile.length()
                                if (totalBytes > 0) {
                                    onProgress(copiedBytes.toFloat() / totalBytes.toFloat())
                                }
                            } else {
                                createdFiles.add(destPath)
                                copyRecursivelyWithProgress(sourceFile, targetFile, { bytesRead ->
                                    copiedBytes += bytesRead
                                    if (totalBytes > 0) {
                                        onProgress(copiedBytes.toFloat() / totalBytes.toFloat())
                                    }
                                }, checkPause)
                                sourceFile.deleteRecursively()
                            }
                        }
                    }
                }
            }
            onProgress(1f)
        } catch (e: kotlinx.coroutines.CancellationException) {
            createdFiles.forEach { path ->
                if (!path.startsWith("drive_id:")) {
                    if (isDestRestricted && hasShizuku()) {
                        runShizukuCommand("rm -rf \"$path\"")
                    } else {
                        File(path).deleteRecursively()
                    }
                }
            }
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun renameFile(path: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        if (path.startsWith("drive_id:")) {
            val fileId = path.removePrefix("drive_id:")
            try {
                val driveFile = com.google.api.services.drive.model.File().apply {
                    name = newName
                }
                driveService?.files()?.update(fileId, driveFile)?.execute()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        } else {
            val file = File(path)
            if (file.exists()) {
                val newFile = File(file.parentFile, newName)
                file.renameTo(newFile)
            } else {
                false
            }
        }
    }
    private suspend fun fetchDriveFiles(pinned: Set<String>, location: String): List<FileItem> = withContext(Dispatchers.IO) {
        val locked = getLockedFiles()
        val query = if (location == "drive") {
            "trashed = false and 'root' in parents"
        } else {
            val folderId = location.removePrefix("drive_id:")
            "trashed = false and '$folderId' in parents"
        }
        
        val result = driveService!!.files().list()
            .setSpaces("drive")
            .setQ(query)
            .setFields("nextPageToken, files(id, name, mimeType, size, modifiedTime, owners, thumbnailLink)")
            .execute()
            val driveFiles = result.files ?: emptyList()
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            
            val items = mutableListOf<FileItem>()
            var idCounter = 1000
            
            for (file in driveFiles) {
                val isFolder = file.mimeType == "application/vnd.google-apps.folder"
                val name = file.name ?: "Unknown"
                val ext = name.substringAfterLast('.', "").lowercase(Locale.getDefault())
                
                val type = when {
                    isFolder -> "folder"
                    file.mimeType?.startsWith("image/") == true -> "image"
                    file.mimeType?.startsWith("video/") == true -> "video"
                    file.mimeType?.startsWith("audio/") == true -> "audio"
                    file.mimeType?.contains("pdf") == true -> "doc"
                    file.mimeType?.contains("document") == true -> "doc"
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
                
                val sizeStr = if (isFolder) {
                    "--"
                } else {
                    file.getSize()?.toLong()?.let { formatSize(it) } ?: "Unknown"
                }
                
                val changedStr = file.modifiedTime?.value?.let { dateFormat.format(Date(it)) } ?: "Unknown"
                
                items.add(
                    FileItem(
                        id = idCounter++,
                        path = "drive_id:${file.id}",
                        name = name,
                        type = type,
                        kind = kind,
                        size = sizeStr,
                        changed = changedStr,
                        owner = file.owners?.firstOrNull()?.displayName ?: "Me",
                        isPinned = pinned.contains("drive_id:${file.id}"),
                        isLocked = locked.contains("drive_id:${file.id}"),
                        isEmptyFolder = isFolder,
                        sizeBytes = file.getSize()?.toLong() ?: 0L,
                        lastModified = file.modifiedTime?.value ?: 0L,
                        thumbnailLink = file.thumbnailLink
                    )
                )
            }
            return@withContext items
    }



    suspend fun deleteDriveFile(fileId: String): String? = withContext(Dispatchers.IO) {
        if (driveService == null) return@withContext "Drive service not initialized"
        try {
            val file = com.google.api.services.drive.model.File().setTrashed(true)
            driveService!!.files().update(fileId, file).setSupportsAllDrives(true).execute()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                driveService!!.files().delete(fileId).setSupportsAllDrives(true).execute()
                null
            } catch (e2: Exception) {
                e2.printStackTrace()
                e2.message ?: "Unknown error"
            }
        }
    }
    suspend fun getDriveStorageQuota(): Pair<Long, Long>? = withContext(Dispatchers.IO) {
        if (driveService == null) return@withContext null
        try {
            val about = driveService!!.about().get().setFields("storageQuota").execute()
            val quota = about.storageQuota
            val limit = quota.limit ?: 0L
            val usage = quota.usage ?: 0L
            return@withContext Pair(usage, limit)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun downloadDriveFile(fileId: String, fileName: String): File? = withContext(Dispatchers.IO) {
        if (driveService == null) return@withContext null
        try {
            val cacheDir = File(context.cacheDir, "drive_cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val outputFile = File(cacheDir, fileName)
            
            // First check if the file is 0 bytes or a google docs file (which has no media)
            val driveFile = driveService!!.files().get(fileId).setFields("size, mimeType").execute()
            val size = driveFile.size ?: 0L
            
            if (size == 0L || driveFile.mimeType?.startsWith("application/vnd.google-apps.") == true) {
                // For empty files or google native docs without exports, just create an empty local file
                outputFile.createNewFile()
                return@withContext outputFile
            }

            val outputStream = java.io.FileOutputStream(outputFile)
            driveService!!.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            outputStream.close()
            return@withContext outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun scanDeviceForCleaner(): CleanerData = withContext(Dispatchers.IO) {
        val rootDir = Environment.getExternalStorageDirectory()
        val docFiles = mutableListOf<FileItem>()
        val imgFiles = mutableListOf<FileItem>()
        val vidFiles = mutableListOf<FileItem>()
        val audioFiles = mutableListOf<FileItem>()
        val appFiles = mutableListOf<FileItem>()
        val emptyFolderFiles = mutableListOf<FileItem>()
        val allFilesBySize = mutableMapOf<Long, MutableList<FileItem>>()
        
        var docsSize = 0L
        var imgSize = 0L
        var vidSize = 0L
        var audioSize = 0L
        var appSize = 0L
        
        val docExts = setOf("pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx", "csv")
        val imgExts = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic")
        val vidExts = setOf("mp4", "mkv", "avi", "mov", "wmv", "flv")
        val audioExts = setOf("mp3", "wav", "flac", "ogg", "m4a", "aac")
        val appExts = setOf("apk", "xapk", "aab")
        
        val pinned = getPinnedFiles()
        val locked = getLockedFiles()
        var idCounter = 100000

        try {
            rootDir.walkTopDown().onEnter { dir ->
                val path = dir.absolutePath
                !dir.name.startsWith(".") && !path.contains("/Android/data") && !path.contains("/Android/obb")
            }.forEach { file ->
                if (file.name.startsWith(".")) return@forEach // skip hidden
                
                if (file.isDirectory) {
                    val children = file.list()
                    if (children != null && children.isEmpty()) {
                        val modified = file.lastModified()
                        emptyFolderFiles.add(
                            FileItem(
                                id = idCounter++,
                                path = file.absolutePath,
                                name = file.name,
                                type = "folder",
                                kind = "folder",
                                size = "--",
                                changed = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(modified)),
                                owner = "Me",
                                isPinned = pinned.contains(file.absolutePath),
                                isLocked = locked.contains(file.absolutePath),
                                isEmptyFolder = true,
                                sizeBytes = 0L,
                                lastModified = modified
                            )
                        )
                    }
                } else {
                    val size = file.length()
                    val ext = file.extension.lowercase(Locale.getDefault())
                    val modified = file.lastModified()
                    val changedStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(modified))
                    
                    var durationStr: String? = null
                    
                    val typeStr = when {
                        ext in setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic") -> "image"
                        ext in setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm") -> "video"
                        ext in setOf("mp3", "wav", "flac", "ogg", "m4a", "aac") -> "audio"
                        ext in setOf("zip", "rar", "7z", "tar", "gz") -> "archive"
                        ext in setOf("pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx", "csv") -> "doc"
                        ext in setOf("apk", "xapk", "aab") -> "apk"
                        else -> "file"
                    }
                    val item = FileItem(
                        id = idCounter++,
                        path = file.absolutePath,
                        name = file.name,
                        type = typeStr,
                        kind = "file",
                        size = formatSize(size),
                        changed = changedStr,
                        owner = "Me",
                        isPinned = pinned.contains(file.absolutePath),
                        isLocked = locked.contains(file.absolutePath),
                        isEmptyFolder = false,
                        sizeBytes = size,
                        lastModified = modified,
                        duration = durationStr
                    )
                    
                    if (ext in docExts) {
                        docFiles.add(item)
                        docsSize += size
                    } else if (ext in imgExts) {
                        imgFiles.add(item)
                        imgSize += size
                    } else if (ext in vidExts) {
                        vidFiles.add(item)
                        vidSize += size
                    } else if (ext in audioExts) {
                        audioFiles.add(item)
                        audioSize += size
                    } else if (ext in appExts) {
                        appFiles.add(item)
                        appSize += size
                    }
                    
                    if (size > 1024L) { // Only consider files larger than 1KB for duplicates
                        allFilesBySize.getOrPut(size) { mutableListOf() }.add(item)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val stat = android.os.StatFs(Environment.getDataDirectory().absolutePath)
        val totalStorageBytes = stat.totalBytes
        val freeStorageBytes = stat.availableBytes
        val usedStorageBytes = totalStorageBytes - freeStorageBytes

        // Calculate other bytes by subtracting known categories from the actual used space
        val knownScannedBytes = docsSize + imgSize + vidSize + audioSize + appSize
        val otherBytes = java.lang.Math.max(0L, usedStorageBytes - knownScannedBytes)
        
        // Find duplicates
        val duplicateFiles = mutableListOf<FileItem>()
        var duplicateSize = 0L
        for ((size, files) in allFilesBySize) {
            if (files.size > 1) {
                // Potential duplicates based on size. Compute full file hash.
                val filesByHash = mutableMapOf<String, MutableList<FileItem>>()
                for (fileItem in files) {
                    try {
                        val digest = java.security.MessageDigest.getInstance("SHA-256")
                        val file = java.io.File(fileItem.path)
                        file.inputStream().use { input ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            var totalRead = 0
                            while (input.read(buffer).also { read = it } != -1) {
                                digest.update(buffer, 0, read)
                                totalRead += read
                                if (totalRead >= 16384) break // Hash only first 16KB for extreme speedup
                            }
                        }
                        val hash = digest.digest().joinToString("") { "%02x".format(it) }
                        filesByHash.getOrPut(hash) { mutableListOf() }.add(fileItem)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                // Add exact duplicates to the list (excluding the original)
                for ((_, hashFiles) in filesByHash) {
                    if (hashFiles.size > 1) {
                        // Function to check if a file can be deleted
                        fun canDelete(path: String): Boolean {
                            if (File(path).canWrite()) return true
                            if (hasShizuku() && (path.contains("Android/data") || path.contains("Android/obb"))) return true
                            return false
                        }

                        // Sort by deleteability (false first, so undeletable becomes original) and then by modified time
                        val sorted = hashFiles.sortedWith(compareBy({ canDelete(it.path) }, { it.lastModified }))
                        
                        // Keep the rest as extras, but only if they are actually deletable
                        val extras = sorted.drop(1).filter { canDelete(it.path) }
                        if (extras.isNotEmpty()) {
                            duplicateFiles.addAll(extras)
                            duplicateSize += size * extras.size
                        }
                    }
                }
            }
        }
        
        return@withContext CleanerData(
            documents = CleanerCategoryData("Documents", docFiles.sortedByDescending { it.sizeBytes }.toImmutableList(), docsSize),
            images = CleanerCategoryData("Images", imgFiles.sortedByDescending { it.sizeBytes }.toImmutableList(), imgSize),
            videos = CleanerCategoryData("Videos", vidFiles.sortedByDescending { it.sizeBytes }.toImmutableList(), vidSize),
            audio = CleanerCategoryData("Audio", audioFiles.sortedByDescending { it.sizeBytes }.toImmutableList(), audioSize),
            apps = CleanerCategoryData("Apps", appFiles.sortedByDescending { it.sizeBytes }.toImmutableList(), appSize),
            emptyFolders = CleanerCategoryData("Empty folders", emptyFolderFiles.toImmutableList(), 0L),
            duplicates = CleanerCategoryData("Duplicates", duplicateFiles.sortedByDescending { it.sizeBytes }.toImmutableList(), duplicateSize),
            otherBytes = otherBytes,
            totalStorageBytes = totalStorageBytes,
            freeStorageBytes = freeStorageBytes
        )
    }

    val trashDir = File(Environment.getExternalStorageDirectory(), ".ripple_trash")

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
                      success = runShizukuCommand("cp -r \"$path\" \"${destFile.absolutePath}\"")
                      if (success) runShizukuCommand("rm -rf \"$path\"")
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

    suspend fun restoreFromTrash(encodedNames: List<String>) = withContext(Dispatchers.IO) {
        if (!trashDir.exists()) return@withContext
        val registryFile = File(trashDir, "trash_registry.json")
        if (!registryFile.exists()) return@withContext
        
        val registry = JSONArray(registryFile.readText())
        val newRegistry = JSONArray()
        
        for (i in 0 until registry.length()) {
            val entry = registry.getJSONObject(i)
            val encName = entry.getString("encodedName")
            if (encodedNames.contains(encName)) {
                val originalPath = entry.getString("originalPath")
                val trashFile = File(trashDir, encName)
                if (trashFile.exists()) {
                    val destFile = File(originalPath)
                    destFile.parentFile?.mkdirs()
                    if (!trashFile.renameTo(destFile)) {
                        trashFile.copyRecursively(destFile, overwrite = true)
                        trashFile.deleteRecursively()
                    }
                }
            } else {
                newRegistry.put(entry)
            }
        }
        registryFile.writeText(newRegistry.toString())
    }

    suspend fun permanentlyDeleteTrash(encodedNames: List<String>) = withContext(Dispatchers.IO) {
        if (!trashDir.exists()) return@withContext
        val registryFile = File(trashDir, "trash_registry.json")
        if (!registryFile.exists()) return@withContext
        
        val registry = JSONArray(registryFile.readText())
        val newRegistry = JSONArray()
        
        for (i in 0 until registry.length()) {
            val entry = registry.getJSONObject(i)
            val encName = entry.getString("encodedName")
            if (encodedNames.contains(encName)) {
                val trashFile = File(trashDir, encName)
                if (trashFile.exists()) {
                    trashFile.deleteRecursively()
                }
            } else {
                newRegistry.put(entry)
            }
        }
        registryFile.writeText(newRegistry.toString())
    }

    suspend fun getTrashFiles(expiryTimeMs: Long = 7L * 24 * 60 * 60 * 1000L): List<FileItem> = withContext(Dispatchers.IO) {
        val trashFiles = mutableListOf<FileItem>()
        if (!trashDir.exists()) return@withContext emptyList()
        
        val registryFile = File(trashDir, "trash_registry.json")
        if (!registryFile.exists()) return@withContext emptyList()
        
        val registry = JSONArray(registryFile.readText())
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val currentTime = System.currentTimeMillis()
        
        val newRegistry = JSONArray()
        
        for (i in 0 until registry.length()) {
            val entry = registry.getJSONObject(i)
            val deletedAt = entry.getLong("deletedAt")
            val encName = entry.getString("encodedName")
            val originalName = entry.getString("originalName")
            val isDirectory = entry.getBoolean("isDirectory")
            val originalPath = entry.getString("originalPath")
            
            val trashFile = File(trashDir, encName)
            if (trashFile.exists()) {
                if (currentTime - deletedAt > expiryTimeMs) {
                    trashFile.deleteRecursively()
                } else {
                    newRegistry.put(entry)
                    val daysLeft = ((expiryTimeMs - (currentTime - deletedAt)) / (1000 * 60 * 60 * 24)).toInt()
                    val ext = if (isDirectory) "" else originalName.substringAfterLast(".", "")
                    val type = if (isDirectory) {
                        "folder"
                    } else {
                        when {
                            ext in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif", "svg") -> "image"
                            ext in listOf("mp4", "mkv", "avi", "mov", "webm") -> "video"
                            ext in listOf("mp3", "wav", "ogg", "flac", "m4a") -> "audio"
                            ext in listOf("zip", "rar", "7z", "tar", "gz") -> "archive"
                            ext in listOf("pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx") -> "doc"
                            ext == "apk" -> "apk"
                            else -> "file"
                        }
                    }
                    
                    val sizeStr = if (isDirectory) "" else formatSize(trashFile.length())
                    val changedStr = dateFormat.format(Date(deletedAt))
                    
                    trashFiles.add(
                        FileItem(
                            id = encName.hashCode(),
                            path = trashFile.absolutePath,
                            name = originalName,
                            type = type,
                            kind = type,
                            size = sizeStr,
                            changed = changedStr,
                            owner = "me",
                            isPinned = false,
                            isEmptyFolder = isDirectory && trashFile.list()?.isEmpty() == true,
                            sizeBytes = trashFile.length(),
                            lastModified = deletedAt,
                            originalPath = originalPath,
                            encodedTrashName = encName
                        )
                    )
                }
            }
        }
        
        if (newRegistry.length() != registry.length()) {
            registryFile.writeText(newRegistry.toString())
        }
        
        return@withContext trashFiles
    }
}

data class CleanerCategoryData(
    val categoryName: String,
    val files: ImmutableList<FileItem>,
    val totalSizeBytes: Long
)

data class CleanerData(
    val documents: CleanerCategoryData,
    val images: CleanerCategoryData,
    val videos: CleanerCategoryData,
    val audio: CleanerCategoryData,
    val apps: CleanerCategoryData,
    val emptyFolders: CleanerCategoryData,
    val duplicates: CleanerCategoryData,
    val otherBytes: Long,
    val totalStorageBytes: Long,
    val freeStorageBytes: Long
)

