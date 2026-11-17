package com.ripple.filemanager.vault

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import com.ripple.filemanager.FileItem
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object VaultManager {
    private const val PREFS_NAME = "sift_vault_prefs"
    private const val KEY_SETUP_COMPLETED = "vault_setup_completed"
    private const val KEY_PASSWORD_HASH = "vault_password_hash"
    private const val KEY_PASSWORD_HINT = "vault_password_hint"
    private const val KEY_CREATED_AT = "vault_created_at"

    fun isVaultSetup(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SETUP_COMPLETED, false) && prefs.getString(KEY_PASSWORD_HASH, null) != null
    }

    fun getVaultPasswordHint(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PASSWORD_HINT, "") ?: ""
    }

    fun setupVault(context: Context, password: String, hint: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hash = hashPassword(password)
        prefs.edit()
            .putBoolean(KEY_SETUP_COMPLETED, true)
            .putString(KEY_PASSWORD_HASH, hash)
            .putString(KEY_PASSWORD_HINT, hint.trim())
            .putLong(KEY_CREATED_AT, System.currentTimeMillis())
            .apply()

        val dir = getVaultDirectory(context)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val noMedia = File(dir, ".nomedia")
        if (!noMedia.exists()) {
            try { noMedia.createNewFile() } catch (_: Exception) {}
        }
        return true
    }

    fun verifyPassword(context: Context, password: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedHash = prefs.getString(KEY_PASSWORD_HASH, null) ?: return false
        return storedHash == hashPassword(password)
    }

    fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun getVaultDirectory(context: Context): File {
        val dir = File(context.filesDir, "vault_files")
        if (!dir.exists()) {
            dir.mkdirs()
            try { File(dir, ".nomedia").createNewFile() } catch (_: Exception) {}
        }
        return dir
    }

    fun getVaultFiles(context: Context, customDir: File? = null): List<FileItem> {
        val dir = customDir ?: getVaultDirectory(context)
        val files = dir.listFiles()?.filter { !it.name.startsWith(".") } ?: emptyList()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        return files.mapIndexed { index, file ->
            val isDir = file.isDirectory
            val ext = if (isDir) "" else file.extension.lowercase(Locale.getDefault())
            val sizeBytes = if (isDir) {
                try { file.walkTopDown().filter { it.isFile }.sumOf { it.length() } } catch (_: Exception) { 0L }
            } else {
                file.length()
            }
            val sizeStr = if (isDir) {
                val itemsCount = file.list()?.filter { !it.startsWith(".") }?.size ?: 0
                "$itemsCount item${if (itemsCount == 1) "" else "s"}"
            } else {
                android.text.format.Formatter.formatFileSize(context, sizeBytes)
            }
            val dateStr = dateFormat.format(Date(file.lastModified()))
            val fileType = when {
                isDir -> "folder"
                ext in setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic") -> "image"
                ext in setOf("mp4", "mkv", "mov", "webm", "avi", "3gp") -> "video"
                ext in setOf("mp3", "flac", "wav", "m4a", "ogg", "aac") -> "audio"
                ext in setOf("pdf", "doc", "docx", "txt", "xlsx", "xls", "pptx", "ppt", "md") -> "doc"
                com.ripple.filemanager.archive.ArchiveUtils.isArchiveExtension(ext) -> "archive"
                ext == "apk" -> "apk"
                else -> "file"
            }

            FileItem(
                id = 900000 + index,
                path = file.absolutePath,
                name = file.name,
                type = fileType,
                kind = if (isDir) "folder" else "file",
                size = sizeStr,
                changed = dateStr,
                owner = "",
                isPinned = false,
                isLocked = true,
                isEmptyFolder = isDir && (file.list()?.isEmpty() == true),
                sizeBytes = sizeBytes,
                lastModified = file.lastModified()
            )
        }.sortedWith(compareByDescending<FileItem> { it.kind == "folder" }.thenByDescending { it.lastModified })
    }

    fun importFiles(context: Context, uris: List<Uri>): Int {
        var count = 0
        val vaultDir = getVaultDirectory(context)
        val metadata = loadMetadata(context)
        for (uri in uris) {
            try {
                val fileName = getFileNameFromUri(context, uri) ?: "imported_file_${System.currentTimeMillis()}"
                var destFile = File(vaultDir, fileName)
                var counter = 1
                while (destFile.exists()) {
                    val base = fileName.substringBeforeLast('.')
                    val ext = if (fileName.contains('.')) ".${fileName.substringAfterLast('.')}" else ""
                    destFile = File(vaultDir, "${base}_$counter$ext")
                    counter++
                }

                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val possiblePath = if (uri.scheme == "file") {
                    uri.path
                } else if (uri.scheme == "content" && uri.authority == "com.android.externalstorage.documents") {
                    try {
                        val docId = android.provider.DocumentsContract.getDocumentId(uri)
                        val split = docId.split(":")
                        if (split.size >= 2) {
                            if (split[0].equals("primary", ignoreCase = true)) {
                                "${Environment.getExternalStorageDirectory().absolutePath}/${split[1]}"
                            } else {
                                "/storage/${split[0]}/${split[1]}"
                            }
                        } else null
                    } catch (_: Exception) { null }
                } else null

                if (!possiblePath.isNullOrBlank()) {
                    val entry = JSONObject().apply {
                        put("originalPath", possiblePath)
                        put("originalName", fileName)
                        put("timestamp", System.currentTimeMillis())
                    }
                    metadata[destFile.name] = entry
                }

                count++
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (metadata.isNotEmpty()) {
            saveMetadata(context, metadata)
        }
        purgeVaultFilesFromRecent(context)
        return count
    }

    private fun getMetadataFile(context: Context): File {
        return File(context.filesDir, "vault_metadata.json")
    }

    private fun loadMetadata(context: Context): MutableMap<String, JSONObject> {
        val file = getMetadataFile(context)
        val map = mutableMapOf<String, JSONObject>()
        if (!file.exists()) return map
        try {
            val content = file.readText()
            if (content.isNotBlank()) {
                val json = JSONObject(content)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = json.getJSONObject(key)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    @Volatile
    private var cachedOriginalPaths: Set<String>? = null

    private fun saveMetadata(context: Context, map: Map<String, JSONObject>) {
        cachedOriginalPaths = null
        try {
            val json = JSONObject()
            for ((k, v) in map) {
                json.put(k, v)
            }
            getMetadataFile(context).writeText(json.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteSourceFileOrDir(context: Context, file: File): Boolean {
        if (!file.exists()) return true
        try {
            if (file.isDirectory) {
                file.walkBottomUp().forEach { child ->
                    if (child.absolutePath != file.absolutePath) {
                        deleteSingleFileOrDir(context, child)
                    }
                }
                deleteSingleFileOrDir(context, file)
            } else {
                deleteSingleFileOrDir(context, file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val uri = android.provider.MediaStore.Files.getContentUri("external")
            context.contentResolver.delete(
                uri,
                "${android.provider.MediaStore.MediaColumns.DATA} = ? OR ${android.provider.MediaStore.MediaColumns.DATA} LIKE ?",
                arrayOf(file.absolutePath, "${file.absolutePath}/%")
            )
        } catch (_: Exception) {}

        return !file.exists()
    }

    private fun deleteSingleFileOrDir(context: Context, file: File): Boolean {
        if (!file.exists()) return true
        var deleted = try { file.delete() } catch (_: Exception) { false }
        if (!deleted && file.exists()) {
            try {
                val uri = android.provider.MediaStore.Files.getContentUri("external")
                context.contentResolver.delete(
                    uri,
                    "${android.provider.MediaStore.MediaColumns.DATA} = ?",
                    arrayOf(file.absolutePath)
                )
            } catch (_: Exception) {}
            deleted = !file.exists()
        }
        if (!deleted && file.exists()) {
            deleted = try { file.delete() } catch (_: Exception) { false }
        }
        return deleted || !file.exists()
    }

    fun purgeVaultSourceFiles(context: Context) {
        try {
            val originalPaths = getVaultOriginalPaths(context)
            for (path in originalPaths) {
                val file = File(path)
                if (file.exists()) {
                    deleteSourceFileOrDir(context, file)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun importFilesFromPaths(context: Context, filePaths: List<String>): Int {
        var count = 0
        val vaultDir = getVaultDirectory(context)
        val metadata = loadMetadata(context)
        val scannedPaths = mutableListOf<String>()

        for (path in filePaths) {
            try {
                val srcFile = File(path)
                if (!srcFile.exists()) continue

                if (srcFile.isDirectory) {
                    val folderName = srcFile.name
                    var destDir = File(vaultDir, folderName)
                    var counter = 1
                    while (destDir.exists()) {
                        destDir = File(vaultDir, "${folderName}_$counter")
                        counter++
                    }

                    val copied = srcFile.copyRecursively(destDir, overwrite = false)
                    if (!copied) {
                        destDir.deleteRecursively()
                        continue
                    }

                    val entry = JSONObject().apply {
                        put("originalPath", srcFile.absolutePath)
                        put("originalName", folderName)
                        put("isFolder", true)
                        put("timestamp", System.currentTimeMillis())
                    }
                    metadata[destDir.name] = entry

                    srcFile.walkTopDown().filter { it.isFile }.forEach { f ->
                        scannedPaths.add(f.absolutePath)
                    }
                    deleteSourceFileOrDir(context, srcFile)
                    count++
                } else {
                    val fileName = srcFile.name
                    var destFile = File(vaultDir, fileName)
                    var counter = 1
                    while (destFile.exists()) {
                        val base = fileName.substringBeforeLast('.')
                        val ext = if (fileName.contains('.')) ".${fileName.substringAfterLast('.')}" else ""
                        destFile = File(vaultDir, "${base}_$counter$ext")
                        counter++
                    }

                    srcFile.inputStream().use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    val entry = JSONObject().apply {
                        put("originalPath", srcFile.absolutePath)
                        put("originalName", fileName)
                        put("timestamp", System.currentTimeMillis())
                    }
                    metadata[destFile.name] = entry

                    val originalPath = srcFile.absolutePath
                    deleteSourceFileOrDir(context, srcFile)
                    scannedPaths.add(originalPath)
                    count++
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (metadata.isNotEmpty()) {
            saveMetadata(context, metadata)
        }

        if (scannedPaths.isNotEmpty()) {
            try {
                MediaScannerConnection.scanFile(
                    context,
                    scannedPaths.toTypedArray(),
                    null,
                    null
                )
            } catch (_: Exception) {}
        }

        purgeVaultFilesFromRecent(context)
        purgeVaultSourceFiles(context)
        return count
    }

    fun getVaultOriginalPaths(context: Context): Set<String> {
        cachedOriginalPaths?.let { return it }
        val metadata = loadMetadata(context)
        val paths = mutableSetOf<String>()
        for ((_, obj) in metadata) {
            val originalPath = obj.optString("originalPath")
            if (!originalPath.isNullOrBlank()) {
                paths.add(originalPath)
            }
        }
        cachedOriginalPaths = paths
        return paths
    }

    fun isVaultFileOrPath(context: Context, path: String): Boolean {
        if (path.isBlank()) return false
        val vaultDir = getVaultDirectory(context).absolutePath
        val vaultCanonical = try { getVaultDirectory(context).canonicalPath } catch (_: Exception) { vaultDir }
        if (path.startsWith(vaultDir) || path.startsWith(vaultCanonical) || path.contains("vault_files")) return true
        val originalPaths = getVaultOriginalPaths(context)
        if (originalPaths.isEmpty()) return false

        val normPath = path.trimEnd('/', '\\')
        for (orig in originalPaths) {
            val normOrig = orig.trimEnd('/', '\\')
            if (normPath.equals(normOrig, ignoreCase = true)) return true
            if (normPath.startsWith("$normOrig/", ignoreCase = true) || normPath.startsWith("$normOrig\\", ignoreCase = true)) return true
        }

        try {
            val fileCanonical = File(path).canonicalPath.trimEnd('/', '\\')
            for (orig in originalPaths) {
                val origCanonical = try { File(orig).canonicalPath.trimEnd('/', '\\') } catch (_: Exception) { orig.trimEnd('/', '\\') }
                if (fileCanonical.equals(origCanonical, ignoreCase = true)) return true
                if (fileCanonical.startsWith("$origCanonical/", ignoreCase = true) || fileCanonical.startsWith("$origCanonical\\", ignoreCase = true)) return true
            }
        } catch (_: Exception) {}

        return false
    }

    fun purgeVaultFilesFromRecent(context: Context) {
        try {
            val prefs = context.getSharedPreferences("sift_prefs", Context.MODE_PRIVATE)
            val historyStr = prefs.getString("recent_actions", "[]") ?: "[]"
            val history = org.json.JSONArray(historyStr)
            val updated = org.json.JSONArray()
            for (i in 0 until history.length()) {
                val item = history.getJSONObject(i)
                val path = item.optString("path", "")
                if (path.isNotEmpty() && !isVaultFileOrPath(context, path)) {
                    updated.put(item)
                }
            }
            prefs.edit().putString("recent_actions", updated.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun restoreFile(context: Context, fileItem: FileItem): Boolean {
        return try {
            val vaultFile = File(fileItem.path)
            if (!vaultFile.exists()) return false

            val metadata = loadMetadata(context)
            val fileMeta = metadata[vaultFile.name]
            val originalPath = fileMeta?.optString("originalPath")
            val originalName = fileMeta?.optString("originalName") ?: vaultFile.name

            val targetFile: File
            if (!originalPath.isNullOrBlank()) {
                val candidate = File(originalPath)
                val parentDir = candidate.parentFile
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs()
                }
                if (parentDir != null && parentDir.exists()) {
                    var dest = candidate
                    var counter = 1
                    while (dest.exists()) {
                        if (vaultFile.isDirectory) {
                            dest = File(parentDir, "${originalName}_$counter")
                        } else {
                            val base = originalName.substringBeforeLast('.')
                            val ext = if (originalName.contains('.')) ".${originalName.substringAfterLast('.')}" else ""
                            dest = File(parentDir, "${base}_$counter$ext")
                        }
                        counter++
                    }
                    targetFile = dest
                } else {
                    val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloads.exists()) downloads.mkdirs()
                    var dest = File(downloads, originalName)
                    var counter = 1
                    while (dest.exists()) {
                        if (vaultFile.isDirectory) {
                            dest = File(downloads, "${originalName}_$counter")
                        } else {
                            val base = originalName.substringBeforeLast('.')
                            val ext = if (originalName.contains('.')) ".${originalName.substringAfterLast('.')}" else ""
                            dest = File(downloads, "${base}_$counter$ext")
                        }
                        counter++
                    }
                    targetFile = dest
                }
            } else {
                val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloads.exists()) downloads.mkdirs()
                var dest = File(downloads, originalName)
                var counter = 1
                while (dest.exists()) {
                    if (vaultFile.isDirectory) {
                        dest = File(downloads, "${originalName}_$counter")
                    } else {
                        val base = originalName.substringBeforeLast('.')
                        val ext = if (originalName.contains('.')) ".${originalName.substringAfterLast('.')}" else ""
                        dest = File(downloads, "${base}_$counter$ext")
                    }
                    counter++
                }
                targetFile = dest
            }

            if (vaultFile.isDirectory) {
                vaultFile.copyRecursively(targetFile, overwrite = false)
                vaultFile.deleteRecursively()
            } else {
                vaultFile.copyTo(targetFile, overwrite = false)
                vaultFile.delete()
            }

            metadata.remove(vaultFile.name)
            saveMetadata(context, metadata)

            val scannedPaths = if (targetFile.isDirectory) {
                val list = mutableListOf(targetFile.absolutePath)
                targetFile.walkTopDown().forEach { list.add(it.absolutePath) }
                list.toTypedArray()
            } else {
                arrayOf(targetFile.absolutePath)
            }

            try {
                MediaScannerConnection.scanFile(
                    context,
                    scannedPaths,
                    null,
                    null
                )
            } catch (_: Exception) {}

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun restoreFiles(context: Context, fileItems: List<FileItem>): Int {
        var restoredCount = 0
        for (item in fileItems) {
            if (restoreFile(context, item)) {
                restoredCount++
            }
        }
        return restoredCount
    }

    fun exportFile(context: Context, fileItem: FileItem): Boolean {
        return try {
            val source = File(fileItem.path)
            if (!source.exists()) return false

            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            var dest = File(downloads, fileItem.name)
            var counter = 1
            while (dest.exists()) {
                if (source.isDirectory) {
                    dest = File(downloads, "${fileItem.name}_$counter")
                } else {
                    val base = fileItem.name.substringBeforeLast('.')
                    val ext = if (fileItem.name.contains('.')) ".${fileItem.name.substringAfterLast('.')}" else ""
                    dest = File(downloads, "${base}_$counter$ext")
                }
                counter++
            }
            if (source.isDirectory) {
                source.copyRecursively(dest, overwrite = false)
            } else {
                source.copyTo(dest, overwrite = false)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteFile(fileItem: FileItem): Boolean {
        return try {
            val f = File(fileItem.path)
            if (f.isDirectory) f.deleteRecursively() else if (f.exists()) f.delete() else false
        } catch (e: Exception) {
            false
        }
    }

    fun deleteFile(context: Context, fileItem: FileItem): Boolean {
        return try {
            val f = File(fileItem.path)
            val deleted = if (f.isDirectory) f.deleteRecursively() else if (f.exists()) f.delete() else false
            if (deleted) {
                val metadata = loadMetadata(context)
                if (metadata.containsKey(f.name)) {
                    metadata.remove(f.name)
                    saveMetadata(context, metadata)
                }
            }
            deleted
        } catch (e: Exception) {
            false
        }
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            return cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return uri.path?.substringAfterLast('/')
    }

    fun generateBackupText(password: String, hint: String): String {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        return """
=======================================================
RIPPLE FILES - SECURE VAULT PASSWORD BACKUP
=======================================================
Generated: $dateStr

Vault Master Password: $password
Password Hint: ${hint.ifBlank { "(None provided)" }}

IMPORTANT NOTICE:
• Ripple Files is a 100% offline file manager.
• Your vault is stored and encrypted locally on this device.
• There are NO cloud servers, accounts, or recovery emails.
• If you forget your password, your vaulted files CANNOT be recovered.
• Store this backup file in a secure location (e.g. USB drive or safe notes).
=======================================================
        """.trimIndent()
    }
}
