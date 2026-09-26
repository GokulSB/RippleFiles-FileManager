package com.ripple.filemanager.data.share

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.Immutable
import java.io.File
import java.util.Locale

@Immutable
data class SharedIncomingFile(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String?
) {
    val type: String
        get() {
            val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
            return when {
                mimeType?.startsWith("image/") == true || ext in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "svg") -> "image"
                mimeType?.startsWith("video/") == true || ext in listOf("mp4", "mkv", "webm", "avi", "mov", "3gp", "flv") -> "video"
                mimeType?.startsWith("audio/") == true || ext in listOf("mp3", "wav", "ogg", "m4a", "flac", "aac", "opus") -> "audio"
                mimeType == "application/pdf" || ext == "pdf" -> "doc"
                mimeType?.startsWith("text/") == true || ext in listOf("txt", "md", "json", "xml", "csv", "html", "log", "kt", "java", "py") -> "doc"
                ext == "apk" || mimeType == "application/vnd.android.package-archive" -> "apk"
                ext in listOf("zip", "rar", "7z", "tar", "gz") -> "archive"
                else -> "file"
            }
        }

    val formattedSize: String
        get() = if (sizeBytes > 0) {
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            when {
                gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
                kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
                else -> "$sizeBytes B"
            }
        } else {
            "--"
        }

    companion object {
        fun fromUri(context: Context, uri: Uri, fallbackMime: String? = null): SharedIncomingFile {
            var name: String? = null
            var size: Long = -1L
            val mime: String? = try {
                context.contentResolver.getType(uri) ?: fallbackMime
            } catch (e: Exception) {
                fallbackMime
            }

            if (uri.scheme == "content") {
                try {
                    context.contentResolver.query(
                        uri,
                        arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (nameIdx != -1) {
                                name = cursor.getString(nameIdx)
                            }
                            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                            if (sizeIdx != -1 && !cursor.isNull(sizeIdx)) {
                                size = cursor.getLong(sizeIdx)
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SharedIncomingFile", "Failed to query ContentResolver for $uri", e)
                }
            } else if (uri.scheme == "file") {
                uri.path?.let { p ->
                    val f = File(p)
                    name = f.name
                    size = if (f.exists()) f.length() else -1L
                }
            }

            if (name.isNullOrBlank()) {
                val lastPath = uri.lastPathSegment ?: "shared_file"
                name = lastPath.substringAfterLast('/')
                if (!name!!.contains('.') && mime != null) {
                    val ext = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
                    if (!ext.isNullOrBlank()) {
                        name = "$name.$ext"
                    }
                }
            }

            return SharedIncomingFile(
                uri = uri,
                name = name ?: "shared_file",
                sizeBytes = size.coerceAtLeast(0L),
                mimeType = mime
            )
        }
    }
}

@Immutable
data class IncomingSharePrompt(
    val files: List<SharedIncomingFile>,
    val selectedDestination: String,
    val isSaving: Boolean = false,
    val progress: Float? = null
)
