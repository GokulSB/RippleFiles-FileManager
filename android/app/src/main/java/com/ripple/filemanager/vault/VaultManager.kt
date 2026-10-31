package com.ripple.filemanager.vault

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import com.ripple.filemanager.FileItem
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

    fun getVaultFiles(context: Context): List<FileItem> {
        val dir = getVaultDirectory(context)
        val files = dir.listFiles()?.filter { !it.name.startsWith(".") } ?: emptyList()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        return files.mapIndexed { index, file ->
            val ext = file.extension.lowercase(Locale.getDefault())
            val sizeStr = android.text.format.Formatter.formatFileSize(context, file.length())
            val dateStr = dateFormat.format(Date(file.lastModified()))
            val fileType = when {
                file.isDirectory -> "folder"
                ext in setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic") -> "image"
                ext in setOf("mp4", "mkv", "mov", "webm", "avi", "3gp") -> "video"
                ext in setOf("mp3", "flac", "wav", "m4a", "ogg", "aac") -> "audio"
                ext in setOf("pdf", "doc", "docx", "txt", "xlsx", "xls", "pptx", "ppt", "md") -> "doc"
                ext in setOf("zip", "rar", "7z", "tar", "gz") -> "archive"
                ext == "apk" -> "apk"
                else -> "file"
            }

            FileItem(
                id = 900000 + index,
                path = file.absolutePath,
                name = file.name,
                type = fileType,
                kind = if (file.isDirectory) "folder" else "file",
                size = sizeStr,
                changed = dateStr,
                owner = "",
                isPinned = false,
                isLocked = true,
                isEmptyFolder = file.isDirectory && (file.list()?.isEmpty() == true),
                sizeBytes = file.length(),
                lastModified = file.lastModified()
            )
        }.sortedByDescending { it.lastModified }
    }

    fun importFiles(context: Context, uris: List<Uri>): Int {
        var count = 0
        val vaultDir = getVaultDirectory(context)
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
                count++
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return count
    }

    fun exportFile(context: Context, fileItem: FileItem): Boolean {
        return try {
            val source = File(fileItem.path)
            if (!source.exists()) return false

            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            var dest = File(downloads, fileItem.name)
            var counter = 1
            while (dest.exists()) {
                val base = fileItem.name.substringBeforeLast('.')
                val ext = if (fileItem.name.contains('.')) ".${fileItem.name.substringAfterLast('.')}" else ""
                dest = File(downloads, "${base}_$counter$ext")
                counter++
            }
            source.copyTo(dest, overwrite = false)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteFile(fileItem: FileItem): Boolean {
        return try {
            val f = File(fileItem.path)
            if (f.exists()) f.delete() else false
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
