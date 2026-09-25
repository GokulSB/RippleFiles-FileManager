package com.ripple.filemanager.ui.expressive

import android.os.Environment
import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripple.filemanager.AppAction
import com.ripple.filemanager.AppState
import com.ripple.filemanager.FileItem
import com.ripple.filemanager.SortMode

import com.ripple.filemanager.ui.getFolderAccent
import java.io.File

/**
 * Expressive Browse Screen:
 * - Centered header
 * - Breadcrumb (current segment as accent pill)
 * - Chips row (Sort -> sheet, Filter, List/Grid toggle)
 * - SectionContainer with list rows or grid cards
 * - Folder cards keep per-folder colour + item-count pill
 */
@Composable
fun ExpressiveBrowseScreen(
    state: AppState,
    onAction: (AppAction) -> Unit,
    onFileClick: (FileItem) -> Unit,
    onFileMenuClick: (FileItem) -> Unit,
    onMenuClick: () -> Unit = {},
    onTrashClick: () -> Unit = { onAction(AppAction.SetTrashScreenVisible(true)) },
    onOrganiseClick: () -> Unit = { onAction(AppAction.OrganiseDownloads) },
    modifier: Modifier = Modifier
) {
    val colors = ExpressiveTheme.colors
    var showSortSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val rootPath = Environment.getExternalStorageDirectory().absolutePath

    val folderTitle = remember(state.location) {
        if (state.location == rootPath || state.location == "home") "Browse"
        else if (state.location.contains("/")) state.location.substringAfterLast("/").ifEmpty { "Browse" }
        else when {
            state.location.startsWith("drive") -> "Google Drive"
            state.location.startsWith("mega") -> "Mega"
            state.location.startsWith("dropbox") -> "Dropbox"
            state.location.startsWith("smb") -> "SMB"
            state.location == "recent" -> "Recent"
            state.location == "pinned" -> "Pinned"
            else -> state.location.replaceFirstChar { it.uppercase() }
        }
    }

    val isAtRoot = state.location == rootPath || state.location == "home" || !state.location.contains("/")
    val browseSubtitle = if (isAtRoot) "Internal storage" else state.location.replace(rootPath, "Storage")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Shared Expressive Header with search bar
        ExpressiveScreenHeader(
            title = folderTitle,
            subtitle = browseSubtitle,
            leftIcon = if (isAtRoot) Icons.Default.Menu else Icons.AutoMirrored.Filled.ArrowBack,
            onLeftClick = if (isAtRoot) onMenuClick else {
                {
                    val parent = java.io.File(state.location).parent
                    if (parent != null && parent.length >= rootPath.length) {
                        onAction(AppAction.SetLocation(parent))
                    } else {
                        onAction(AppAction.SetLocation(rootPath))
                    }
                }
            },
            leftDescription = if (isAtRoot) "Open menu" else "Back",
            rightIcon = Icons.Outlined.Delete,
            onRightClick = onTrashClick,
            rightDescription = "Open trash",
            showSearch = true,
            query = state.query,
            onQueryChange = { onAction(AppAction.SetQuery(it)) },
            searchPlaceholder = "Search files & folders...",
            onAiSearchClick = onOrganiseClick
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Breadcrumb: Horizontal row with path segments, current segment as accent pill
        BrowseBreadcrumbs(
            currentLocation = state.location,
            rootPath = rootPath,
            onNavigate = { onAction(AppAction.SetLocation(it)) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Chips Row: Sort, Filter, List/Grid Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sort Chip
            ExpressiveChip(
                label = when (state.sortMode) {
                    SortMode.ALPHABETICAL -> "Name"
                    SortMode.DATE -> "Date"
                    SortMode.SIZE -> "Size"
                },
                selected = state.sortMode != SortMode.ALPHABETICAL,
                icon = Icons.Outlined.Sort,
                onClick = { showSortSheet = true }
            )

            // Filter Chip
            ExpressiveChip(
                label = if (state.filter != "all") state.filter.replaceFirstChar { it.uppercase() } else "Filter",
                selected = state.filter != "all",
                icon = Icons.Outlined.FilterList,
                onClick = { showFilterSheet = true }
            )

            Spacer(modifier = Modifier.weight(1f))

            // List / Grid Toggle
            IconButton(
                onClick = { onAction(AppAction.ToggleViewMode) },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.card)
            ) {
                Icon(
                    imageVector = if (state.isListMode) Icons.Default.GridView else Icons.Default.ViewList,
                    contentDescription = "Toggle view",
                    tint = colors.text,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Content in SectionContainer
        SectionContainer(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (state.files.isEmpty()) {
                InnerCard(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "This folder is empty",
                            fontSize = 15.sp,
                            color = colors.muted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (state.isListMode) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 130.dp)
                ) {
                    items(state.files, key = { it.id }) { file ->
                        BrowseListRow(
                            file = file,
                            isSelected = state.selectedFiles.contains(file.id),
                            onSelect = { onAction(AppAction.ToggleSelection(file.id)) },
                            onClick = { onFileClick(file) },
                            onMenuClick = { onFileMenuClick(file) }
                        )
                    }
                }
            } else {
                // Grid mode
                val columns = state.gridColumns.coerceIn(2, 4)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 130.dp)
                ) {
                    items(state.files, key = { it.id }) { file ->
                        BrowseGridCard(
                            file = file,
                            isSelected = state.selectedFiles.contains(file.id),
                            onSelect = { onAction(AppAction.ToggleSelection(file.id)) },
                            onClick = { onFileClick(file) },
                            onMenuClick = { onFileMenuClick(file) },
                            columns = columns
                        )
                    }
                }
            }
        }
    }

    // Sort Bottom Sheet
    if (showSortSheet) {
        SortBottomSheet(
            currentMode = state.sortMode,
            onDismiss = { showSortSheet = false },
            onSelectMode = {
                onAction(AppAction.SetSortMode(it))
                showSortSheet = false
            }
        )
    }

    // Filter Bottom Sheet
    if (showFilterSheet) {
        FilterBottomSheet(
            currentFilter = state.filter,
            onDismiss = { showFilterSheet = false },
            onSelectFilter = {
                onAction(AppAction.SetFilter(it))
                showFilterSheet = false
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// BREADCRUMBS
// ═══════════════════════════════════════════════════════════════════════

@Composable
internal fun BrowseBreadcrumbs(
    currentLocation: String,
    rootPath: String,
    onNavigate: (String) -> Unit
) {
    val colors = ExpressiveTheme.colors
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val rootLabel = when {
            currentLocation.startsWith("drive") -> "Google Drive"
            currentLocation.startsWith("mega") -> "Mega"
            currentLocation.startsWith("dropbox") -> "Dropbox"
            currentLocation.startsWith("smb") -> "SMB"
            currentLocation.startsWith("ftp") -> "FTP"
            currentLocation.startsWith("sftp") -> "SFTP"
            currentLocation.startsWith("nextcloud") -> "Nextcloud"
            currentLocation.startsWith("webdav") -> "WebDAV"
            currentLocation == "recent" -> "Recent"
            currentLocation == "pinned" -> "Pinned"
            else -> "Storage"
        }
        val isRoot = currentLocation == rootPath || currentLocation == "home" || currentLocation == "drive" || currentLocation == "mega" || currentLocation == "dropbox" || currentLocation == "recent" || currentLocation == "pinned"
        
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (isRoot) colors.accent else colors.card)
                .clickable { 
                    if (currentLocation.startsWith("drive")) onNavigate("drive")
                    else if (currentLocation.startsWith("mega")) onNavigate("mega")
                    else if (currentLocation.startsWith("dropbox")) onNavigate("dropbox")
                    else onNavigate(rootPath) 
                }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = rootLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isRoot) colors.ink else colors.text
            )
        }

        // Relative segments
        if (currentLocation.startsWith(rootPath) && currentLocation.length > rootPath.length) {
            val relative = currentLocation.removePrefix(rootPath).removePrefix("/")
            val segments = relative.split("/")
            var accumulated = rootPath

            segments.forEachIndexed { index, seg ->
                Text("/", color = colors.muted, fontSize = 12.sp)
                accumulated = "$accumulated/$seg"
                val target = accumulated
                val isLast = index == segments.size - 1

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isLast) colors.accent else colors.card)
                        .clickable { onNavigate(target) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = seg,
                        fontSize = 12.sp,
                        fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                        color = if (isLast) colors.ink else colors.text
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// LIST ROW & GRID CARD
// ═══════════════════════════════════════════════════════════════════════

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun BrowseListRow(
    file: FileItem,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val colors = ExpressiveTheme.colors
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
            CookieIcon(
                icon = if (isSelected) Icons.Outlined.Check else (if (file.type == "folder") Icons.Outlined.Folder else Icons.AutoMirrored.Outlined.InsertDriveFile),
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
                fontFamily = com.ripple.filemanager.ui.theme.LocalAppFont.current,
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
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = meta,
                fontFamily = com.ripple.filemanager.ui.theme.LocalAppFont.current,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                color = colors.muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onMenuClick) {
            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = colors.muted, modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun BrowseGridCard(
    file: FileItem,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    columns: Int = 2
) {
    val colors = ExpressiveTheme.colors
    val pastel = if (file.type == "folder") {
        getFolderAccent(file.name)
    } else {
        ExpressiveTokens.categoryPastel(fileTypeCategory(file.name))
    }
    val roundness = LocalCornerRoundness.current
    val baseRadius = when {
        columns >= 4 -> 16f
        columns == 3 -> 22f
        else -> 28f
    }
    val cardShape = RoundedCornerShape((baseRadius * (roundness * 2).coerceIn(0f, 2f)).dp)

    val iconSize = when {
        columns >= 4 -> 36.dp
        columns == 3 -> 44.dp
        else -> 52.dp
    }
    val cardPadding = when {
        columns >= 4 -> 8.dp
        columns == 3 -> 10.dp
        else -> 14.dp
    }
    val titleFontSize = when {
        columns >= 4 -> 11.sp
        columns == 3 -> 12.sp
        else -> 13.sp
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(colors.card)
            .border(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected) colors.accent else colors.line.copy(alpha = colors.lineAlpha),
                cardShape
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onSelect
            )
    ) {
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(if (columns >= 4) 2.dp else 6.dp)
                .size(if (columns >= 4) 22.dp else 28.dp)
        ) {
            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = colors.muted, modifier = Modifier.size(if (columns >= 4) 14.dp else 16.dp))
        }

        Column(
            modifier = Modifier.padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(if (columns >= 4) 6.dp else if (columns == 3) 8.dp else 12.dp)
        ) {
            Box(
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSelect
                )
            ) {
                CookieIcon(
                    icon = if (isSelected) Icons.Outlined.Check else (if (file.type == "folder") Icons.Outlined.Folder else Icons.AutoMirrored.Outlined.InsertDriveFile),
                    bgColor = if (isSelected) colors.accent else pastel,
                    iconTint = if (isSelected) colors.ink else ExpressiveTokens.OnPastel,
                    size = iconSize,
                    lobes = cookieLobesForName(file.name)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = file.name,
                    fontFamily = com.ripple.filemanager.ui.theme.LocalAppFont.current,
                    fontSize = titleFontSize,
                    lineHeight = if (columns >= 4) 14.sp else if (columns == 3) 16.sp else 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.text,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                val metaText = if (file.type == "folder") {
                    formatFolderMeta(file)
                } else {
                    formatFileMeta(file)
                }
                Text(
                    text = metaText,
                    fontFamily = com.ripple.filemanager.ui.theme.LocalAppFont.current,
                    fontSize = if (columns >= 4) 10.sp else 11.sp,
                    lineHeight = if (columns >= 4) 13.sp else 14.sp,
                    color = colors.muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun fileTypeCategory(name: String): String {
    val ext = name.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "jpg", "jpeg", "png", "gif", "webp" -> "images"
        "mp4", "mkv", "mov", "webm" -> "videos"
        "mp3", "flac", "wav", "m4a", "ogg" -> "audio"
        "pdf", "doc", "docx", "txt" -> "docs"
        "apk" -> "apps"
        "zip", "rar", "7z", "tar" -> "archives"
        else -> "large"
    }
}

// ═══════════════════════════════════════════════════════════════════════
// SORT & FILTER BOTTOM SHEETS
// ═══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SortBottomSheet(
    currentMode: SortMode,
    onDismiss: () -> Unit,
    onSelectMode: (SortMode) -> Unit
) {
    val colors = ExpressiveTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
        containerColor = colors.container,
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
    ) {
        ExpressiveSheetContent(title = "Sort by") {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                listOf(
                    SortMode.ALPHABETICAL to "Name",
                    SortMode.DATE to "Date modified",
                    SortMode.SIZE to "Size"
                ).forEach { (mode, label) ->
                    val isSelected = mode == currentMode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) colors.card else Color.Transparent)
                            .clickable { onSelectMode(mode) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = colors.text,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.accent)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterBottomSheet(
    currentFilter: String,
    onDismiss: () -> Unit,
    onSelectFilter: (String) -> Unit
) {
    val colors = ExpressiveTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
        containerColor = colors.container,
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
    ) {
        ExpressiveSheetContent(title = "Filter") {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                listOf(
                    "all" to "All files",
                    "image" to "Images",
                    "video" to "Videos",
                    "audio" to "Audio",
                    "doc" to "Documents",
                    "apk" to "Apps",
                    "archive" to "Archives"
                ).forEach { (filter, label) ->
                    val isSelected = filter == currentFilter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) colors.card else Color.Transparent)
                            .clickable { onSelectFilter(filter) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = colors.text,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.accent)
                        }
                    }
                }
            }
        }
    }
}