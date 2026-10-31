package com.ripple.filemanager.ui.expressive

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.ripple.filemanager.FileItem
import com.ripple.filemanager.ui.getFolderAccent
import java.io.File
import java.util.Locale

fun hasThumbnail(file: FileItem): Boolean {
    if (file.type == "folder") return false
    val ext = file.name.substringAfterLast('.', "").lowercase(Locale.getDefault())
    return file.thumbnailLink != null || (
        file.path.isNotEmpty() && (
            file.type in setOf("image", "video", "doc") ||
            ext in setOf(
                "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif",
                "mp4", "mkv", "mov", "webm", "avi", "3gp", "ts", "m4v",
                "pdf", "docx", "pptx", "xlsx", "apk",
                "txt", "log", "md", "json", "xml", "kt", "java", "py", "html", "css", "js"
            )
        )
    )
}

fun fileCategoryForName(name: String): String {
    val ext = name.substringAfterLast('.', "").lowercase(Locale.getDefault())
    return when (ext) {
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif" -> "images"
        "mp4", "mkv", "mov", "webm", "avi", "3gp", "ts", "m4v" -> "videos"
        "mp3", "flac", "wav", "m4a", "ogg", "aac", "opus", "wma" -> "audio"
        "pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx", "rtf", "odt" -> "docs"
        "apk", "xapk", "apks" -> "apps"
        "zip", "rar", "7z", "tar", "gz", "bz2", "xz" -> "archives"
        else -> "large"
    }
}

fun fileIconForFile(type: String, ext: String): ImageVector {
    return when {
        type == "folder" -> Icons.Outlined.Folder
        type == "image" || ext in setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif") -> Icons.Outlined.Image
        type == "video" || ext in setOf("mp4", "mkv", "mov", "webm", "avi", "3gp", "ts", "m4v") -> Icons.Outlined.VideoFile
        type == "audio" || ext in setOf("mp3", "flac", "wav", "m4a", "ogg", "aac", "opus", "wma") -> Icons.Outlined.AudioFile
        ext == "pdf" -> Icons.Outlined.PictureAsPdf
        ext in setOf("doc", "docx", "txt", "rtf", "odt") -> Icons.Outlined.Description
        ext in setOf("xls", "xlsx", "csv") -> Icons.Outlined.TableChart
        ext in setOf("ppt", "pptx") -> Icons.Outlined.Slideshow
        ext in setOf("apk", "xapk", "apks") -> Icons.Outlined.Android
        ext in setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz") -> Icons.Outlined.Archive
        else -> Icons.AutoMirrored.Outlined.InsertDriveFile
    }
}

@Composable
fun ExpressiveFileThumbnail(
    file: FileItem,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shape: Shape? = null,
    lobes: Int = cookieLobesForName(file.name)
) {
    val activeShape = shape ?: rememberIconShape(lobes = lobes)
    val colors = ExpressiveTheme.colors
    val ext = remember(file.name) { file.name.substringAfterLast('.', "").lowercase(Locale.getDefault()) }
    val pastel = remember(file.name, file.type) {
        if (file.type == "folder") getFolderAccent(file.name)
        else ExpressiveTokens.categoryPastel(fileCategoryForName(file.name))
    }
    val fallbackIcon = remember(file.type, ext) { fileIconForFile(file.type, ext) }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(activeShape)
                    .background(colors.accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Selected",
                    tint = colors.ink,
                    modifier = Modifier.size(size * 0.48f)
                )
            }
        } else if (hasThumbnail(file)) {
            var loadFailed by remember(file.path, file.thumbnailLink) { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(activeShape)
                    .background(pastel.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                if (!loadFailed) {
                    val context = LocalContext.current
                    val isVideo = file.type == "video" || ext in setOf("mp4", "mkv", "mov", "webm", "avi", "3gp", "ts", "m4v")
                    val model = remember(file.path, file.thumbnailLink) {
                        when {
                            file.thumbnailLink != null -> file.thumbnailLink
                            file.path.isNotEmpty() -> File(file.path)
                            else -> null
                        }
                    }
                    if (model != null) {
                        val request = remember(model) {
                            ImageRequest.Builder(context)
                                .data(model)
                                .apply {
                                    if (isVideo) {
                                        videoFrameMillis(1000L)
                                    }
                                }
                                .listener(
                                    onError = { _, _ -> loadFailed = true }
                                )
                                .build()
                        }
                        AsyncImage(
                            model = request,
                            contentDescription = file.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isVideo) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(size * 0.44f)
                                    .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(size * 0.28f)
                                )
                            }
                        }
                    } else {
                        Icon(
                            imageVector = fallbackIcon,
                            contentDescription = null,
                            tint = ExpressiveTokens.OnPastel,
                            modifier = Modifier.size(size * 0.48f)
                        )
                    }
                } else {
                    Icon(
                        imageVector = fallbackIcon,
                        contentDescription = null,
                        tint = ExpressiveTokens.OnPastel,
                        modifier = Modifier.size(size * 0.48f)
                    )
                }
            }
        } else {
            CookieIcon(
                icon = fallbackIcon,
                bgColor = pastel,
                iconTint = ExpressiveTokens.OnPastel,
                size = size,
                shape = activeShape
            )
        }

        if (file.isLocked) {
            val badgeSize = (size * 0.42f).coerceIn(16.dp, 24.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(badgeSize)
                    .clip(CircleShape)
                    .background(colors.card)
                    .border(1.dp, colors.line.copy(alpha = colors.lineAlpha), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "Locked",
                    tint = colors.accent,
                    modifier = Modifier.size(badgeSize * 0.62f)
                )
            }
        }
    }
}
