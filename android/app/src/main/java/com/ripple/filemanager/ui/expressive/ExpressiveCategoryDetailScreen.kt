package com.ripple.filemanager.ui.expressive

import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.ripple.filemanager.AppAction
import com.ripple.filemanager.AppState
import com.ripple.filemanager.FileItem
import com.ripple.filemanager.ui.formatSize
import com.ripple.filemanager.ui.getFolderAccent
import com.ripple.filemanager.ui.theme.LocalAppFont
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExpressiveCategoryDetailScreen(
    categoryKey: String,
    state: AppState,
    onAction: (AppAction) -> Unit,
    onBack: () -> Unit,
    onTrashClick: () -> Unit,
    onFileClick: (FileItem) -> Unit,
    onFileMenuClick: (FileItem) -> Unit,
    modifier: Modifier = Modifier,
    files: List<FileItem>? = null
) {
    val colors = ExpressiveTheme.colors
    val categoryTitle = remember(categoryKey) { getCategoryTitle(categoryKey) }

    val rawFiles = files ?: state.files
    val sortedFiles = remember(rawFiles) {
        rawFiles.sortedByDescending { it.lastModified }
    }

    val totalBytes = remember(sortedFiles) {
        sortedFiles.sumOf { it.sizeBytes }
    }

    val subtitle = remember(sortedFiles.size, totalBytes) {
        if (sortedFiles.isEmpty()) {
            "0 files"
        } else {
            val countStr = if (sortedFiles.size == 1) "1 file" else "${sortedFiles.size} files"
            "$countStr • ${formatSize(totalBytes)}"
        }
    }

    val groupedByDate = remember(sortedFiles) {
        val map = linkedMapOf<String, MutableList<FileItem>>()
        for (file in sortedFiles) {
            val group = getDateGroupLabel(file.lastModified)
            map.getOrPut(group) { mutableListOf() }.add(file)
        }
        map
    }

    RippleBackground(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // PageHeader with Back cookie, category title, count+size subtitle, and 3 right cookie buttons
            PageHeader(
                title = categoryTitle,
                subtitle = subtitle,
                onBack = onBack,
                actions = {
                    val allSelected = sortedFiles.isNotEmpty() && sortedFiles.all { state.selectedFiles.contains(it.id) }
                    CookieIconButton(
                        onClick = {
                            if (allSelected) {
                                onAction(AppAction.SelectNone)
                            } else {
                                onAction(AppAction.SelectAll(sortedFiles.map { it.id }))
                            }
                        },
                        icon = if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                        bgColor = colors.card,
                        description = if (allSelected) "Deselect all" else "Select all",
                        iconTint = colors.text,
                        size = 38.dp,
                        lobes = 8
                    )
                    CookieIconButton(
                        onClick = { onAction(AppAction.ToggleViewMode) },
                        icon = if (state.isListMode) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                        bgColor = colors.card,
                        description = if (state.isListMode) "Switch to grid view" else "Switch to list view",
                        iconTint = colors.text,
                        size = 38.dp,
                        lobes = 8
                    )
                    CookieIconButton(
                        onClick = onTrashClick,
                        icon = Icons.Outlined.Delete,
                        bgColor = colors.card,
                        description = "Open bin",
                        iconTint = colors.text,
                        size = 38.dp,
                        lobes = 8
                    )
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (state.isLoading && sortedFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.accent)
                }
            } else if (sortedFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SectionContainer(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = getEmptyMessage(categoryKey),
                                fontSize = 16.sp,
                                fontFamily = LocalAppFont.current,
                                fontWeight = FontWeight.Normal,
                                color = colors.muted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp)
                ) {
                    groupedByDate.forEach { (dateLabel, filesInGroup) ->
                        item(key = "header_$dateLabel") {
                            Text(
                                text = dateLabel,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = LocalAppFont.current,
                                color = colors.muted,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                        }
                        item(key = "section_$dateLabel") {
                            SectionContainer {
                                val reduced = ExpressiveMotion.isReducedMotion()
                                androidx.compose.animation.AnimatedContent(
                                    targetState = state.isListMode,
                                    transitionSpec = {
                                        if (reduced) {
                                            androidx.compose.animation.fadeIn(androidx.compose.animation.core.snap()) togetherWith androidx.compose.animation.fadeOut(androidx.compose.animation.core.snap())
                                        } else {
                                            androidx.compose.animation.fadeIn(ExpressiveMotion.FastTween) togetherWith androidx.compose.animation.fadeOut(ExpressiveMotion.FadeTween)
                                        }
                                    },
                                    label = "category_list_grid_toggle"
                                ) { listMode ->
                                    if (listMode) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            filesInGroup.forEach { file ->
                                                CategoryFileRow(
                                                    file = file,
                                                    isSelected = state.selectedFiles.contains(file.id),
                                                    onSelect = { onAction(AppAction.ToggleSelection(file.id)) },
                                                    onClick = { onFileClick(file) },
                                                    onMenuClick = { onFileMenuClick(file) }
                                                )
                                            }
                                        }
                                    } else {
                                        val columns = state.gridColumns.coerceIn(2, 4)
                                        val rows = filesInGroup.chunked(columns)
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            rows.forEach { rowFiles ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    rowFiles.forEach { file ->
                                                        CategoryGridCard(
                                                            file = file,
                                                            isSelected = state.selectedFiles.contains(file.id),
                                                            columns = columns,
                                                            onSelect = { onAction(AppAction.ToggleSelection(file.id)) },
                                                            onClick = { onFileClick(file) },
                                                            onMenuClick = { onFileMenuClick(file) },
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                    }
                                                    if (rowFiles.size < columns) {
                                                        repeat(columns - rowFiles.size) {
                                                            Spacer(modifier = Modifier.weight(1f))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryFileRow(
    file: FileItem,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val colors = ExpressiveTheme.colors
    val ext = file.name.substringAfterLast('.', "").lowercase(Locale.getDefault())
    val pastel = if (file.type == "folder") {
        getFolderAccent(file.name)
    } else {
        ExpressiveTokens.categoryPastel(fileTypeCategory(file.name))
    }

    val roundness = LocalCornerRoundness.current
    val rowShape = RoundedCornerShape((26f * (roundness * 2).coerceIn(0f, 2f)).dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(if (isSelected) colors.accent.copy(alpha = 0.25f) else colors.card)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onSelect
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelect
            )
        ) {
            val icon = if (isSelected) {
                Icons.Outlined.Check
            } else {
                fileIconForCategory(file.type, ext)
            }
            CookieIcon(
                icon = icon,
                bgColor = if (isSelected) colors.accent else pastel,
                iconTint = if (isSelected) colors.ink else ExpressiveTokens.OnPastel,
                size = 48.dp,
                lobes = cookieLobesForName(file.name)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                fontFamily = LocalAppFont.current,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                color = colors.text,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            val meta = if (file.type == "folder") {
                formatFolderMeta(file)
            } else {
                formatFileMeta(file)
            }
            if (meta.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = meta,
                    fontFamily = LocalAppFont.current,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    color = colors.muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(onClick = onMenuClick) {
            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = colors.muted, modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryGridCard(
    file: FileItem,
    isSelected: Boolean,
    columns: Int,
    onSelect: () -> Unit,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ExpressiveTheme.colors
    val roundness = LocalCornerRoundness.current
    val baseRadius = when {
        columns >= 4 -> 16f
        columns == 3 -> 22f
        else -> 26f
    }
    val cardShape = RoundedCornerShape((baseRadius * (roundness * 2).coerceIn(0f, 2f)).dp)
    val ext = file.name.substringAfterLast('.', "").lowercase(Locale.getDefault())

    val hasPreview = remember(file.name, file.path) {
        file.path.isNotEmpty() && (
            file.type in setOf("image", "video") ||
            ext in setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "mp4", "mkv", "mov", "webm", "avi", "pdf", "docx", "pptx", "xlsx")
        )
    }

    val pastel = remember(file.name, file.type) {
        if (file.type == "folder") getFolderAccent(file.name)
        else ExpressiveTokens.categoryPastel(fileTypeCategory(file.name))
    }

    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = if (isSelected) BorderStroke(2.dp, colors.accent) else null,
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onSelect
            )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // 4:3 or 1:1 aspect ratio thumbnail box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (columns >= 4) 1f else 4f / 3f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.insetCard)
            ) {
                if (hasPreview) {
                    var loadFailed by remember { mutableStateOf(false) }
                    if (!loadFailed) {
                        val context = LocalContext.current
                        val request = remember(file.path) {
                            ImageRequest.Builder(context)
                                .data(File(file.path))
                                .apply {
                                    if (file.type == "video" || ext in setOf("mp4", "mkv", "mov", "webm", "avi")) {
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
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CookieIcon(
                                icon = fileIconForCategory(file.type, ext),
                                bgColor = pastel,
                                iconTint = ExpressiveTokens.OnPastel,
                                size = if (columns >= 4) 34.dp else 44.dp,
                                lobes = cookieLobesForName(file.name)
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CookieIcon(
                            icon = fileIconForCategory(file.type, ext),
                            bgColor = pastel,
                            iconTint = ExpressiveTokens.OnPastel,
                            size = if (columns >= 4) 34.dp else 44.dp,
                            lobes = cookieLobesForName(file.name)
                        )
                    }
                }

                // 3-dot options button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable(onClick = onMenuClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Selection check-cookie overlay
                val reduced = ExpressiveMotion.isReducedMotion()
                androidx.compose.animation.AnimatedVisibility(
                    visible = isSelected,
                    enter = androidx.compose.animation.scaleIn(
                        initialScale = 0f,
                        animationSpec = if (reduced) androidx.compose.animation.core.snap() else ExpressiveMotion.SpringSpec
                    ) + androidx.compose.animation.fadeIn(ExpressiveMotion.FastTween),
                    exit = androidx.compose.animation.scaleOut(
                        targetScale = 0f,
                        animationSpec = if (reduced) androidx.compose.animation.core.snap() else ExpressiveMotion.FastTween
                    ) + androidx.compose.animation.fadeOut(ExpressiveMotion.FadeTween),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(6.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onSelect
                            )
                    ) {
                        CookieIcon(
                            icon = Icons.Outlined.Check,
                            bgColor = colors.accent,
                            iconTint = colors.ink,
                            size = 28.dp,
                            lobes = 8
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // File name (15-16sp medium, tight to content)
            Text(
                text = file.name,
                fontFamily = LocalAppFont.current,
                fontSize = if (columns >= 4) 12.sp else if (columns == 3) 14.sp else 15.sp,
                lineHeight = if (columns >= 4) 14.sp else if (columns == 3) 17.sp else 18.sp,
                fontWeight = FontWeight.Medium,
                color = colors.text,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            val meta = if (file.type == "folder") {
                formatFolderMeta(file)
            } else {
                formatFileMeta(file)
            }
            Text(
                text = meta,
                fontFamily = LocalAppFont.current,
                fontSize = if (columns >= 4) 10.sp else 12.sp,
                lineHeight = if (columns >= 4) 13.sp else 15.sp,
                color = colors.muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun getCategoryTitle(categoryKey: String): String {
    return when (categoryKey.lowercase(Locale.getDefault())) {
        "image", "images" -> "Images"
        "video", "videos" -> "Videos"
        "audio" -> "Audio"
        "doc", "docs", "documents" -> "Documents"
        "apk", "app", "apps" -> "Apps"
        "download", "downloads" -> "Downloads"
        "archive", "archives" -> "Archives"
        "large", "largefiles", "other files", "other" -> "Other files"
        "duplicates", "duplicate" -> "Duplicates"
        "empty folders", "empty_folders" -> "Empty folders"
        else -> categoryKey.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}

private fun getEmptyMessage(categoryKey: String): String {
    return when (categoryKey.lowercase(Locale.getDefault())) {
        "image", "images" -> "No images found"
        "video", "videos" -> "No videos found"
        "audio" -> "No audio files found"
        "doc", "docs", "documents" -> "No documents found"
        "apk", "app", "apps" -> "No apps found"
        "download", "downloads" -> "No downloads found"
        "archive", "archives" -> "No archives found"
        "large", "largefiles", "other files", "other" -> "No other files found"
        "duplicates", "duplicate" -> "No duplicates found"
        "empty folders", "empty_folders" -> "No empty folders found"
        else -> "No files found"
    }
}

private fun getDateGroupLabel(timestamp: Long): String {
    if (timestamp <= 0L) return "Earlier"
    val calFile = Calendar.getInstance().apply { timeInMillis = timestamp }
    val calNow = Calendar.getInstance()

    val fileYear = calFile.get(Calendar.YEAR)
    val fileDay = calFile.get(Calendar.DAY_OF_YEAR)
    val nowYear = calNow.get(Calendar.YEAR)
    val nowDay = calNow.get(Calendar.DAY_OF_YEAR)

    if (fileYear == nowYear && fileDay == nowDay) {
        return "Today"
    }

    calNow.add(Calendar.DAY_OF_YEAR, -1)
    if (fileYear == calNow.get(Calendar.YEAR) && fileDay == calNow.get(Calendar.DAY_OF_YEAR)) {
        return "Yesterday"
    }

    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
}

private fun fileTypeCategory(name: String): String {
    val ext = name.substringAfterLast('.', "").lowercase(Locale.getDefault())
    return when (ext) {
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic" -> "images"
        "mp4", "mkv", "mov", "webm", "avi" -> "videos"
        "mp3", "flac", "wav", "m4a", "ogg" -> "audio"
        "pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx" -> "docs"
        "apk", "xapk" -> "apps"
        "zip", "rar", "7z", "tar", "gz" -> "archives"
        else -> "large"
    }
}

private fun fileIconForCategory(type: String, ext: String): ImageVector {
    return when {
        type == "folder" -> Icons.Outlined.Folder
        ext in setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic") -> Icons.Outlined.Image
        ext in setOf("mp4", "mkv", "mov", "webm", "avi") -> Icons.Outlined.VideoFile
        ext in setOf("mp3", "flac", "wav", "m4a", "ogg") -> Icons.Outlined.AudioFile
        ext in setOf("pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx") -> Icons.Outlined.Description
        ext in setOf("apk", "xapk") -> Icons.Outlined.Apps
        ext in setOf("zip", "rar", "7z", "tar", "gz") -> Icons.Outlined.Archive
        else -> Icons.AutoMirrored.Outlined.InsertDriveFile
    }
}
