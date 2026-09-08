package com.ripple.filemanager.data.core

import com.ripple.filemanager.FileItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shared helpers used by every FileSource adapter (Smb, Ftp, Sftp, WebDav,
 * Drive, Mega, Local) to build a consistent FileItem regardless of where
 * the file actually came from.
 */
object FileItemMapper {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    fun formatSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(Locale.getDefault(), "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    fun inferType(name: String, isDirectory: Boolean): String {
        if (isDirectory) return "folder"
        val ext = name.substringAfterLast('.', "")
        return when (ext.lowercase()) {
            "jpg", "jpeg", "png", "gif", "webp" -> "image"
            "mp4", "mkv", "avi", "mov" -> "video"
            "mp3", "wav", "flac", "m4a", "ogg" -> "music"
            "pdf", "txt", "json", "doc", "docx" -> "document"
            "zip", "rar", "7z", "tar", "gz" -> "archive"
            "apk" -> "apk"
            else -> "unknown"
        }
    }

    fun buildFileItem(
        prefixedPath: String,
        name: String,
        isDirectory: Boolean,
        sizeBytes: Long,
        lastModified: Long,
        owner: String
    ): FileItem {
        val type = inferType(name, isDirectory)
        return FileItem(
            id = prefixedPath.hashCode(),
            path = prefixedPath,
            name = name,
            type = type,
            kind = if (isDirectory) "Folder" else type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            size = if (isDirectory) "" else formatSize(sizeBytes),
            changed = if (lastModified > 0) dateFormat.format(Date(lastModified)) else "",
            owner = owner,
            sizeBytes = sizeBytes,
            lastModified = lastModified
        )
    }
}
