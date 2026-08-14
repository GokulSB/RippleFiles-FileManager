package com.ripple.filemanager.ui

import com.ripple.filemanager.ui.theme.ProvideSkylineLedgerColors
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import coil.decode.VideoFrameDecoder
import coil.request.videoFrameMillis
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.composed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.SolidColor
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Popup
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.PopupProperties
import com.ripple.filemanager.FileItem
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.ui.res.stringResource
import com.ripple.filemanager.R

private fun isNativeRootFolder(path: String): Boolean {
    val rootPath = android.os.Environment.getExternalStorageDirectory().path
    val protectedFolders = setOf("Android", "DCIM", "Download", "Documents", "Pictures", "Music", "Movies", "Podcasts", "Alarms", "Notifications")
    return protectedFolders.any { path == "$rootPath/$it" }
}


@androidx.compose.runtime.Immutable
sealed class FolderListUiState {
    object Loading : FolderListUiState()
    data class Loaded(val items: kotlinx.collections.immutable.ImmutableList<com.ripple.filemanager.FileItem>) : FolderListUiState()
    data class Error(val message: String = "") : FolderListUiState()
}

@Composable
fun FolderSkeletonCard(index: Int, isListMode: Boolean, cornerRoundness: Float, gridColumns: Int) {
    val shimmerColors = listOf(
        androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh,
        androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
        androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh
    )
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val durationScale = android.provider.Settings.Global.getFloat(
        context.contentResolver,
        android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
        1f
    )
    
    val brush = if (durationScale == 0f) {
        androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.Color(0xFF1F1810))
    } else {
        val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "shimmer")
        val translateAnim by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(1300, easing = androidx.compose.animation.core.LinearEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Restart
            ),
            label = "shimmerTranslate"
        )
        androidx.compose.ui.graphics.Brush.linearGradient(
            colors = shimmerColors,
            start = androidx.compose.ui.geometry.Offset(translateAnim - 300f - index * 80f, 0f),
            end = androidx.compose.ui.geometry.Offset(translateAnim - index * 80f, 0f)
        )
    }

    val dynamicRadius = (24f * (cornerRoundness * 2)).coerceIn(0f, 100f)
    val shape = RoundedCornerShape(dynamicRadius.dp)
    
    // Instead of using BlobShape for the whole card, wait! The real folder card uses RoundedCornerShape(dynamicRadius.dp).
    // Let's use BlobShape as the user requested "die-cut folder-tab shape (same clip/notch as real folder cards)".
    
    if (isListMode) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .clip(shape)
                .background(brush)
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .background(brush)
        )
    }
}

@Composable
fun FileGrid(
    folderState: FolderListUiState,
    pasteLoadingCount: Int? = null,
    selectedFiles: ImmutableSet<Int>,
    deletingIds: Set<Int> = emptySet(),
    isListMode: Boolean,
    iconShape: com.ripple.filemanager.IconShapeType,
    cornerRoundness: Float,
    onFileClick: (FileItem) -> Unit,
    onFileLongClick: (FileItem) -> Unit,
    onPinClick: (FileItem) -> Unit,
    onInfoClick: (FileItem) -> Unit,
    onRenameClick: (FileItem, String) -> Unit,
    onExtractClick: (FileItem) -> Unit,
    onLockClick: (FileItem) -> Unit = {},
    onUnlockClick: (FileItem) -> Unit = {},
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {},
    emptyState: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 100.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FileShapeIcon("folder", size = 64)
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.no_matching_files), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.try_another_search), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    },
    searchQuery: String = "",
    gridColumns: Int = 2,
    onItemPositioned: (Int, androidx.compose.ui.geometry.Rect) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val customImageLoader = remember(context) {
        coil.ImageLoader.Builder(context).components {
            add(coil.decode.VideoFrameDecoder.Factory())
            add(DocumentThumbnailFetcher.Factory())
        }.build()
    }
    
    var showFullNameSheetFor by remember { mutableStateOf<MediaFileUiModel?>(null) }
    
    val currentFiles = if (folderState is FolderListUiState.Loaded) folderState.items else persistentListOf()
    val currentIsLoading = folderState is FolderListUiState.Loading

    Column(modifier = modifier.fillMaxSize()) {
        header()
        
        if (isListMode) {
            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 96.dp, top = 8.dp, start = 8.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                
                if (currentIsLoading) {
                    items(6) { index ->
                        FolderSkeletonCard(index = index, isListMode = true, cornerRoundness = cornerRoundness, gridColumns = gridColumns)
                    }
                } else if (currentFiles.isEmpty() && pasteLoadingCount == null) {
                    item { emptyState() }
                } else {
                    val skeletonCount = pasteLoadingCount ?: 0
                    items(count = currentFiles.size + skeletonCount, key = { if (it < currentFiles.size) currentFiles[it].id else "skeleton_$it" }, contentType = { if (it < currentFiles.size) (if (currentFiles[it].type == "folder") 1 else 0) else 2 }) { index ->
                        if (index < currentFiles.size) {
                            val file = currentFiles[index]
                            androidx.compose.animation.AnimatedVisibility(
                                visible = !deletingIds.contains(file.id),
                                exit = androidx.compose.animation.shrinkOut(
                                    shrinkTowards = Alignment.Center,
                                    animationSpec = androidx.compose.animation.core.tween(220, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160)),
                                modifier = Modifier.animateItem()
                            ) {
                                if (file.type == "video" || file.type == "audio" || file.type == "image" || file.type == "doc") {
                                    ProvideSkylineLedgerColors {
                                        MediaFileListCard(
                                            modifier = Modifier.depthStackEffect(index = index, listState = listState),
                                            file = file.toMediaFileUiModel(),
                                            isSelected = selectedFiles.contains(file.id),
                                            imageLoader = customImageLoader,
                                            cornerRoundness = cornerRoundness,
                                            onExpandFullName = { showFullNameSheetFor = it },
                                            onClick = { onFileClick(file) },
                                            onLongClick = { onFileLongClick(file) },
                                            onPinClick = { onPinClick(file) },
                                            onLockClick = { onLockClick(file) },
                                            onUnlockClick = { onUnlockClick(file) },
                                            onInfoClick = { onInfoClick(file) }
                                        )
                                    }
                                } else {
                                    FileListCard(
                                        modifier = Modifier.depthStackEffect(index = index, listState = listState),
                                        imageLoader = customImageLoader,
                                        file = file,
                                        isSelected = selectedFiles.contains(file.id),
                                        iconShape = iconShape,
                                        cornerRoundness = cornerRoundness,
                                        searchQuery = searchQuery,
                                        onClick = { onFileClick(file) },
                                        onLongClick = { onFileLongClick(file) },
                                        onPinClick = { onPinClick(file) },
                                        onInfoClick = { onInfoClick(file) },
                                        onRenameClick = { name -> onRenameClick(file, name) },
                                        onExtractClick = { onExtractClick(file) },
                                        onLockClick = { onLockClick(file) },
                                        onUnlockClick = { onUnlockClick(file) }
                                    )
                                }
                            }
                        } else {
                            FolderSkeletonCard(index = index, isListMode = true, cornerRoundness = cornerRoundness, gridColumns = gridColumns)
                        }
                    }
                }
            }
        } else {
            val gridState = androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState()
            val gridSpacing = if (gridColumns > 2) 8.dp else 12.dp
            androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid(
                state = gridState,
                columns = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells.Fixed(gridColumns),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 96.dp, top = 8.dp, start = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                verticalItemSpacing = gridSpacing
            ) {
                
                if (currentIsLoading) {
                    items(6) { index ->
                        FolderSkeletonCard(index = index, isListMode = false, cornerRoundness = cornerRoundness, gridColumns = gridColumns)
                    }
                } else if (currentFiles.isEmpty() && pasteLoadingCount == null) {
                    item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) { emptyState() }
                } else {
                    val skeletonCount = pasteLoadingCount ?: 0
                    items(count = currentFiles.size + skeletonCount, key = { if (it < currentFiles.size) "${currentFiles[it].path}_$it" else "skeleton_$it" }, contentType = { if (it < currentFiles.size) (if (currentFiles[it].type == "folder") 1 else 0) else 2 }) { index ->
                        if (index < currentFiles.size) {
                            val file = currentFiles[index]
                            androidx.compose.animation.AnimatedVisibility(
                                visible = !deletingIds.contains(file.id),
                                exit = androidx.compose.animation.shrinkOut(
                                    shrinkTowards = Alignment.Center,
                                    animationSpec = androidx.compose.animation.core.tween(220, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160)),
                                modifier = Modifier.animateItem()
                            ) {
                                if (file.type == "video" || file.type == "audio" || file.type == "image" || file.type == "doc") {
                                    ProvideSkylineLedgerColors {
                                        MediaFileGridCard(
                                            modifier = Modifier.gridDepthStackEffect(index = index, gridState = gridState),
                                            file = file.toMediaFileUiModel(),
                                            isSelected = selectedFiles.contains(file.id),
                                            columns = gridColumns,
                                            imageLoader = customImageLoader,
                                            cornerRoundness = cornerRoundness,
                                            onExpandFullName = { showFullNameSheetFor = it },
                                            onClick = { onFileClick(file) },
                                            onLongClick = { onFileLongClick(file) },
                                            onPinClick = { onPinClick(file) },
                                            onLockClick = { onLockClick(file) },
                                            onUnlockClick = { onUnlockClick(file) },
                                            onInfoClick = { onInfoClick(file) }
                                        )
                                    }
                                } else {
                                    FileGridCard(
                                        modifier = Modifier.gridDepthStackEffect(index = index, gridState = gridState),
                                        imageLoader = customImageLoader,
                                        file = file,
                                        isSelected = selectedFiles.contains(file.id),
                                        iconShape = iconShape,
                                        cornerRoundness = cornerRoundness,
                                        gridColumns = gridColumns,
                                        searchQuery = searchQuery,
                                        onClick = { onFileClick(file) },
                                        onLongClick = { onFileLongClick(file) },
                                        onPinClick = { onPinClick(file) },
                                        onInfoClick = { onInfoClick(file) },
                                        onRenameClick = { name -> onRenameClick(file, name) },
                                        onExtractClick = { onExtractClick(file) },
                                        onLockClick = { onLockClick(file) },
                                        onUnlockClick = { onUnlockClick(file) }
                                    )
                                }
                            }
                        } else {
                            FolderSkeletonCard(index = index, isListMode = false, cornerRoundness = cornerRoundness, gridColumns = gridColumns)
                        }
                    }
                }
            }
        }
    }
    
    showFullNameSheetFor?.let { model ->
        ProvideSkylineLedgerColors {
            FullFileNameSheet(
                file = model,
                cornerRoundness = cornerRoundness,
                onDismiss = { showFullNameSheetFor = null }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridCard(file: FileItem, isSelected: Boolean, imageLoader: coil.ImageLoader, iconShape: com.ripple.filemanager.IconShapeType, cornerRoundness: Float, gridColumns: Int = 2, modifier: Modifier = Modifier, searchQuery: String = "", onClick: () -> Unit, onLongClick: () -> Unit, onPinClick: () -> Unit, onInfoClick: () -> Unit, onRenameClick: (String) -> Unit, onExtractClick: () -> Unit, onLockClick: () -> Unit = {}, onUnlockClick: () -> Unit = {}) {
    val dynamicRadius = (24f * (cornerRoundness * 2)).coerceIn(0f, 100f)
    val shape = if (isSelected) RoundedCornerShape(topStart = 32.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 26.dp)
                else RoundedCornerShape(dynamicRadius.dp)
    
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val folderBgColor = if (isDarkTheme) Color(0xFF0A0704) else Color(0xFFFAF6F0)

    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.64f)
                  else if (file.type == "folder" && !file.isEmptyFolder) folderBgColor
                  else MaterialTheme.colorScheme.surface
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    var showMenu by remember { mutableStateOf(false) }
    var isEditingName by remember { mutableStateOf(false) }
    var editNameValue by remember { mutableStateOf(file.name) }
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    LaunchedEffect(isEditingName) {
        if (isEditingName) {
            focusRequester.requestFocus()
        }
    }

    val isMediaOrDoc = file.type == "image" || file.type == "video" || file.type == "doc" || listOf(".txt", ".json", ".md", ".csv", ".xml", ".log", ".kt", ".java", ".py", ".html").any { file.name.endsWith(it, ignoreCase = true) }

    // SKYLINE LEDGER RESKIN: Use hard-edged tiles for folders (and other non-media)
    if (!isMediaOrDoc) {
        com.ripple.filemanager.ui.SkylineFolderGridTile(
            name = file.name,
            type = file.type,
            itemCountOrMeta = file.size,
            date = file.changed,
            sizeBytes = file.sizeBytes,
            isSelected = isSelected,
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier,
            cornerRoundness = cornerRoundness,
            onPinClick = onPinClick,
            onInfoClick = onInfoClick,
            isPinned = file.isPinned,
            isLocked = file.isLocked,
            onLockClick = onLockClick,
            onUnlockClick = onUnlockClick
        )
        return
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        color = bgColor,
        shape = shape,
        shadowElevation = if (!isDarkTheme) 3.dp else 0.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth().wrapContentHeight().defaultMinSize(minHeight = 100.dp).then(if (isMediaOrDoc) Modifier.aspectRatio(if (gridColumns > 2) 1f else 0.85f) else Modifier)) {
            if (isMediaOrDoc) {
                val isImageOrVideo = file.type == "image" || file.type == "video"

                
                val requestBuilder = remember(file.path) {
                    coil.request.ImageRequest.Builder(context)
                        .data(file.thumbnailLink ?: java.io.File(file.path))
                        .apply {
                            if (file.type == "video") {
                                videoFrameMillis(1000)
                            }
                        }
                        .build()
                }

                coil.compose.AsyncImage(
                    model = requestBuilder,
                    imageLoader = imageLoader,
                    contentDescription = file.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                
                if (file.type == "video") {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.play),
                        tint = Color.White,
                        modifier = Modifier.align(Alignment.Center).size(48.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp, top = 32.dp)
                ) {
                    Column {
                        if (isEditingName) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                androidx.compose.foundation.text.BasicTextField(
                                    value = editNameValue,
                                    onValueChange = { editNameValue = it },
                                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                                    textStyle = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, color = Color.White),
                                    singleLine = true,
                                    cursorBrush = SolidColor(Color.White)
                                )
                                IconButton(onClick = { 
                                    isEditingName = false
                                    if (editNameValue.isNotBlank() && editNameValue != file.name) onRenameClick(editNameValue)
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Check, contentDescription = stringResource(R.string.confirm), modifier = Modifier.size(16.dp), tint = Color.White)
                                }
                            }
                        } else {
                            Text(buildHighlightedString(file.name, searchQuery), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = Int.MAX_VALUE, overflow = androidx.compose.ui.text.style.TextOverflow.Visible)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        if (gridColumns > 2) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(file.size, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
                                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.25f)) {
                                    Text(file.changed, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(file.size, modifier = Modifier.weight(1f).padding(end = 8.dp), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
                                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.25f)) {
                                    Text(file.changed, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = Color.White)
                                }
                            }
                        }
                    }
                }
                
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options), modifier = Modifier.size(20.dp), tint = Color.White)
                    }
                    if (showMenu) {
                        val density = LocalDensity.current
                        val yOffset = with(density) { (-40).dp.roundToPx() }
                        Popup(alignment = Alignment.TopEnd, offset = androidx.compose.ui.unit.IntOffset(0, yOffset), onDismissRequest = { showMenu = false }, properties = androidx.compose.ui.window.PopupProperties(focusable = true)) {
                            Surface(
                                shape = com.ripple.filemanager.ui.getDynamicCornerShape(12f, cornerRoundness),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, com.ripple.filemanager.ui.theme.SkylineColors.Border),
                                shadowElevation = 4.dp
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { showMenu = false; onPinClick() }, modifier = Modifier.size(36.dp)) { Icon(if (file.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin, contentDescription = stringResource(R.string.pin_file), tint = com.ripple.filemanager.ui.theme.SkylineColors.Amber) }
                                    IconButton(onClick = { showMenu = false; if (file.isLocked) onUnlockClick() else onLockClick() }, modifier = Modifier.size(36.dp)) { Icon(if (file.isLocked) Icons.Outlined.LockOpen else Icons.Outlined.Lock, contentDescription = if (file.isLocked) stringResource(R.string.unlock_file) else stringResource(R.string.lock_file), tint = com.ripple.filemanager.ui.theme.SkylineColors.Amber) }
                                    IconButton(onClick = { showMenu = false; onInfoClick() }, modifier = Modifier.size(36.dp)) { Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.file_info), tint = com.ripple.filemanager.ui.theme.SkylineColors.Amber) }
                                    if (file.name.endsWith(".zip", ignoreCase = true)) {
                                        IconButton(onClick = { showMenu = false; onExtractClick() }, modifier = Modifier.size(36.dp)) { Icon(Icons.Outlined.FolderZip, contentDescription = stringResource(R.string.extract_archive), tint = com.ripple.filemanager.ui.theme.SkylineColors.Amber) }
                                    }
                                }
                            }
                        }
                    }
                }
            } // Close if (isMediaOrDoc)
            
            if (file.isPinned) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = stringResource(R.string.pinned_badge),
                    tint = com.ripple.filemanager.ui.theme.SkylineColors.Amber,
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 2.dp).size(28.dp)
                )
            }
            if (file.isLocked) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = stringResource(R.string.locked_badge),
                    tint = com.ripple.filemanager.ui.theme.SkylineColors.Amber,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).size(24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListCard(file: FileItem, isSelected: Boolean, imageLoader: coil.ImageLoader, iconShape: com.ripple.filemanager.IconShapeType, cornerRoundness: Float, modifier: Modifier = Modifier, searchQuery: String = "", onClick: () -> Unit, onLongClick: () -> Unit, onPinClick: () -> Unit, onInfoClick: () -> Unit, onRenameClick: (String) -> Unit, onExtractClick: () -> Unit, onLockClick: () -> Unit = {}, onUnlockClick: () -> Unit = {}) {
    val dynamicRadius = (24f * (cornerRoundness * 2)).coerceIn(0f, 100f)
    val shape = if (isSelected) RoundedCornerShape(topStart = 32.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 26.dp)
                else RoundedCornerShape(dynamicRadius.dp)
    
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val folderBgColor = if (isDarkTheme) Color(0xFF0A0704) else Color(0xFFFAF6F0)

    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.64f)
                  else if (file.type == "folder" && !file.isEmptyFolder) folderBgColor
                  else MaterialTheme.colorScheme.surface
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    var showMenu by remember { mutableStateOf(false) }
    var isEditingName by remember { mutableStateOf(false) }
    var editNameValue by remember { mutableStateOf(file.name) }
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    LaunchedEffect(isEditingName) {
        if (isEditingName) {
            focusRequester.requestFocus()
        }
    }

    val isMediaOrDoc = file.type == "image" || file.type == "video" || file.type == "doc" || listOf(".txt", ".json", ".md", ".csv", ".xml", ".log", ".kt", ".java", ".py", ".html").any { file.name.endsWith(it, ignoreCase = true) }

    // SKYLINE LEDGER RESKIN: Use hard-edged rows for folders (and other non-media)
    if (!isMediaOrDoc) {
        com.ripple.filemanager.ui.SkylineFolderListRow(
            name = file.name,
            type = file.type,
            subline = file.changed,
            trailingMeta = file.size,
            sizeBytes = file.sizeBytes,
            isSelected = isSelected,
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier,
            cornerRoundness = cornerRoundness,
            onPinClick = onPinClick,
            onInfoClick = onInfoClick,
            isPinned = file.isPinned,
            isLocked = file.isLocked,
            onLockClick = onLockClick,
            onUnlockClick = onUnlockClick
        )
        return
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        color = bgColor,
        shape = shape,
        shadowElevation = if (!isDarkTheme) 3.dp else 0.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth().wrapContentHeight().defaultMinSize(minHeight = 68.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FileShapeIcon(file.type, name = file.name, size = 42, path = file.path, iconShape = iconShape, duration = file.duration, thumbnailLink = file.thumbnailLink)
                Spacer(modifier = Modifier.width(16.dp))
                if (isEditingName) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = editNameValue,
                            onValueChange = { editNameValue = it },
                            modifier = Modifier.weight(1f).focusRequester(focusRequester),
                            textStyle = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold, 
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                        )
                        IconButton(onClick = { 
                            isEditingName = false
                            if (editNameValue.isNotBlank() && editNameValue != file.name) {
                                onRenameClick(editNameValue)
                            }
                        }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.confirm), modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    Text(buildHighlightedString(file.name, searchQuery), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface, maxLines = Int.MAX_VALUE, overflow = androidx.compose.ui.text.style.TextOverflow.Visible)
                }
                
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.widthIn(min = 120.dp)
                ) {
                    Text(file.size, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(file.changed, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box(contentAlignment = Alignment.TopEnd) {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options), modifier = Modifier.size(17.dp))
                        }
                        if (showMenu) {
                            val density = LocalDensity.current
                            val yOffset = with(density) { (-52).dp.roundToPx() }
                            Popup(
                                alignment = Alignment.TopEnd,
                                offset = IntOffset(0, yOffset),
                                onDismissRequest = { showMenu = false },
                                properties = PopupProperties(focusable = true)
                            ) {
                                Surface(
                                    shape = com.ripple.filemanager.ui.getDynamicCornerShape(12f, cornerRoundness),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, com.ripple.filemanager.ui.theme.SkylineColors.Border),
                                    shadowElevation = 4.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(onClick = { showMenu = false; onPinClick() }, modifier = Modifier.size(36.dp)) {
                                            Icon(if (file.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin, contentDescription = stringResource(R.string.pin_file), tint = com.ripple.filemanager.ui.theme.SkylineColors.Amber)
                                        }
                                        IconButton(onClick = { showMenu = false; if (file.isLocked) onUnlockClick() else onLockClick() }, modifier = Modifier.size(36.dp)) {
                                            Icon(if (file.isLocked) Icons.Outlined.LockOpen else Icons.Outlined.Lock, contentDescription = if (file.isLocked) stringResource(R.string.unlock_file) else stringResource(R.string.lock_file), tint = com.ripple.filemanager.ui.theme.SkylineColors.Amber)
                                        }
                                        IconButton(onClick = { showMenu = false; onInfoClick() }, modifier = Modifier.size(36.dp)) {
                                            Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.file_info), tint = com.ripple.filemanager.ui.theme.SkylineColors.Amber)
                                        }
                                        if (file.name.endsWith(".zip", ignoreCase = true)) {
                                            IconButton(onClick = { showMenu = false; onExtractClick() }, modifier = Modifier.size(36.dp)) { Icon(Icons.Outlined.FolderZip, contentDescription = stringResource(R.string.extract_archive), tint = com.ripple.filemanager.ui.theme.SkylineColors.Amber) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (file.isPinned) {
                Icon(
                    Icons.Default.PushPin,
                    contentDescription = stringResource(R.string.pinned_badge),
                    tint = com.ripple.filemanager.ui.theme.SkylineColors.Amber,
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp).size(24.dp)
                )
            }
            if (file.isLocked) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = stringResource(R.string.locked_badge),
                    tint = com.ripple.filemanager.ui.theme.SkylineColors.Amber,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).size(24.dp)
                )
            }
        }
    }
}

@Composable
fun FileShapeIcon(type: String, name: String = "", size: Int, path: String = "", iconShape: com.ripple.filemanager.IconShapeType = com.ripple.filemanager.IconShapeType.SYSTEM, duration: String? = null, thumbnailLink: String? = null) {
    val shapeData = getShapeData(type, name, iconShape)
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(shapeData.bgColor, shapeData.shape)
            .clip(shapeData.shape),
        contentAlignment = Alignment.Center
    ) {
        val isMediaOrDoc = type == "image" || type == "video" || type == "doc" || listOf(".txt", ".json", ".md", ".csv", ".xml", ".log", ".kt", ".java", ".py", ".html").any { name.endsWith(it, ignoreCase = true) }
        
        if (isMediaOrDoc && path.isNotEmpty()) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val imageLoader = androidx.compose.runtime.remember {
                coil.ImageLoader.Builder(context)
                    .components {
                        add(coil.decode.VideoFrameDecoder.Factory())
                        add(DocumentThumbnailFetcher.Factory())
                    }
                    .build()
            }
            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                val requestBuilder = coil.request.ImageRequest.Builder(context)
                    .data(thumbnailLink ?: java.io.File(path))
                if (type == "video") {
                    requestBuilder.videoFrameMillis(1000)
                }
                
                AsyncImage(
                    model = requestBuilder.build(),
                    imageLoader = imageLoader,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (type == "video") {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.play),
                        modifier = Modifier.align(Alignment.Center).size((size * 0.4).dp),
                        tint = Color.White
                    )
                    if (duration != null) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = duration,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        } else {
            Icon(shapeData.icon, contentDescription = type, tint = shapeData.iconColor, modifier = Modifier.size((size * 0.4).dp))
        }
    }
}

data class ShapeData(val shape: androidx.compose.ui.graphics.Shape, val bgColor: Color, val iconColor: Color, val icon: ImageVector)

val BlobShape = GenericShape { size, _ ->
    moveTo(size.width * 0.46f, 0f)
    cubicTo(size.width * 1f, 0f, size.width * 1f, size.height * 1f, size.width * 0.37f, size.height * 1f)
    cubicTo(0f, size.height * 1f, 0f, 0f, size.width * 0.46f, 0f)
}
val HexShape = GenericShape { size, _ ->
    moveTo(size.width * 0.25f, size.height * 0.04f)
    lineTo(size.width * 0.75f, size.height * 0.04f)
    lineTo(size.width * 1f, size.height * 0.5f)
    lineTo(size.width * 0.75f, size.height * 0.96f)
    lineTo(size.width * 0.25f, size.height * 0.96f)
    lineTo(0f, size.height * 0.5f)
    close()
}
val ArchShape = RoundedCornerShape(topStartPercent = 60, topEndPercent = 60, bottomStartPercent = 8, bottomEndPercent = 8)
val CookieShape = RoundedCornerShape(topStartPercent = 62, topEndPercent = 38, bottomEndPercent = 45, bottomStartPercent = 42)
val DiamondShape = GenericShape { size, _ ->
    moveTo(size.width * 0.5f, 0f)
    lineTo(size.width, size.height * 0.5f)
    lineTo(size.width * 0.5f, size.height)
    lineTo(0f, size.height * 0.5f)
    close()
}
val StarShape = GenericShape { size, _ ->
    val cx = size.width / 2f
    val cy = size.height / 2f
    val outR = size.width / 2f
    val inR = size.width / 2.5f
    moveTo(cx, cy - outR)
    for (i in 1..4) {
        val a1 = i * Math.PI / 2.5 - Math.PI / 2
        val a2 = a1 - Math.PI / 5
        lineTo(cx + (Math.cos(a2) * inR).toFloat(), cy + (Math.sin(a2) * inR).toFloat())
        lineTo(cx + (Math.cos(a1) * outR).toFloat(), cy + (Math.sin(a1) * outR).toFloat())
    }
    close()
}
val FlowerShape = GenericShape { size, _ ->
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.width / 2f
    val spikes = 8
    moveTo(cx + r, cy)
    for (i in 1..spikes * 2) {
        val radius = if (i % 2 == 0) r else r * 0.85f
        val angle = i * Math.PI / spikes
        lineTo(cx + (Math.cos(angle) * radius).toFloat(), cy + (Math.sin(angle) * radius).toFloat())
    }
    close()
}

fun getCustomShape(type: com.ripple.filemanager.IconShapeType): androidx.compose.ui.graphics.Shape {
    return when (type) {
        com.ripple.filemanager.IconShapeType.CIRCLE -> CircleShape
        com.ripple.filemanager.IconShapeType.SQUARE -> RoundedCornerShape(12.dp)
        com.ripple.filemanager.IconShapeType.SQUIRCLE -> BlobShape
        com.ripple.filemanager.IconShapeType.DIAMOND -> DiamondShape
        com.ripple.filemanager.IconShapeType.HEXAGON -> HexShape
        com.ripple.filemanager.IconShapeType.ARCH -> ArchShape
        com.ripple.filemanager.IconShapeType.COOKIE -> CookieShape
        com.ripple.filemanager.IconShapeType.STAR -> StarShape
        com.ripple.filemanager.IconShapeType.FLOWER -> FlowerShape
        com.ripple.filemanager.IconShapeType.SYSTEM -> CircleShape
    }
}

@Composable
fun getShapeData(type: String, name: String, iconShape: com.ripple.filemanager.IconShapeType = com.ripple.filemanager.IconShapeType.SYSTEM): ShapeData {
    val baseShape = when (type) {
        "folder" -> BlobShape
        "image" -> HexShape
        "video" -> ArchShape
        "audio" -> CircleShape
        "doc" -> CookieShape
        "archive" -> DiamondShape
        else -> BlobShape
    }
    
    val finalShape = if (iconShape == com.ripple.filemanager.IconShapeType.SYSTEM) baseShape else getCustomShape(iconShape)

    return when (type) {
        "folder" -> {
            val icon = when {
                name.contains("movie", ignoreCase = true) || name.contains("video", ignoreCase = true) -> Icons.Default.PlayArrow
                name.contains("music", ignoreCase = true) || name.contains("audio", ignoreCase = true) -> Icons.Default.MusicNote
                name.contains("picture", ignoreCase = true) || name.contains("image", ignoreCase = true) -> Icons.Default.Photo
                name.contains("notification", ignoreCase = true) -> Icons.Default.Notifications
                name.contains("podcast", ignoreCase = true) -> Icons.Default.Podcasts
                name.contains("recording", ignoreCase = true) || name.contains("voice", ignoreCase = true) -> Icons.Default.Mic
                else -> Icons.Outlined.Folder
            }
            ShapeData(finalShape, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, icon)
        }
        "image" -> ShapeData(finalShape, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, Icons.Outlined.Image)
        "video" -> ShapeData(finalShape, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Icons.Outlined.VideoFile)
        "audio" -> ShapeData(finalShape, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Icons.Outlined.Audiotrack)
        "doc" -> ShapeData(finalShape, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, Icons.Outlined.Description)
        "archive" -> ShapeData(finalShape, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Icons.Outlined.Inventory2)
        "apk" -> ShapeData(finalShape, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, Icons.Outlined.Android)
        else -> ShapeData(finalShape, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Icons.Outlined.Folder)
    }
}




fun buildHighlightedString(text: String, query: String): androidx.compose.ui.text.AnnotatedString {
    if (query.isBlank()) return androidx.compose.ui.text.AnnotatedString(text)
    val startIndex = text.indexOf(query, ignoreCase = true)
    if (startIndex == -1) return androidx.compose.ui.text.AnnotatedString(text)
    val builder = androidx.compose.ui.text.AnnotatedString.Builder(text)
    builder.addStyle(androidx.compose.ui.text.SpanStyle(color = androidx.compose.ui.graphics.Color.Red), startIndex, startIndex + query.length)
    return builder.toAnnotatedString()
}

@Composable
fun Modifier.depthStackEffect(
    index: Int,
    listState: androidx.compose.foundation.lazy.LazyListState,
    topPaddingPx: Float = with(androidx.compose.ui.platform.LocalDensity.current) { 8.dp.toPx() },
    edgeZonePx: Float = with(androidx.compose.ui.platform.LocalDensity.current) { 60.dp.toPx() },
    maxScaleDrop: Float = 0.12f,
    maxAlphaDrop: Float = 0.5f,
): Modifier = this.composed {
    val durationScale = android.provider.Settings.Global.getFloat(
        androidx.compose.ui.platform.LocalContext.current.contentResolver,
        android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
        1f
    )
    if (durationScale == 0f) return@composed this

    this.graphicsLayer {
        val itemInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
        if (itemInfo != null) {
            val distFromTop = itemInfo.offset.toFloat()
            val t = if (distFromTop < topPaddingPx) {
                ((topPaddingPx - distFromTop) / edgeZonePx).coerceIn(0f, 1f)
            } else {
                0f
            }
            val scale = 1f - (maxScaleDrop * t)
            scaleX = scale
            scaleY = scale
            alpha = 1f - (maxAlphaDrop * t)
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
        }
    }
}

@Composable
fun Modifier.gridDepthStackEffect(
    index: Int,
    gridState: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState,
    topPaddingPx: Float = with(androidx.compose.ui.platform.LocalDensity.current) { 8.dp.toPx() },
    edgeZonePx: Float = with(androidx.compose.ui.platform.LocalDensity.current) { 60.dp.toPx() },
    maxScaleDrop: Float = 0.12f,
    maxAlphaDrop: Float = 0.5f,
): Modifier = this.composed {
    val durationScale = android.provider.Settings.Global.getFloat(
        androidx.compose.ui.platform.LocalContext.current.contentResolver,
        android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
        1f
    )
    if (durationScale == 0f) return@composed this

    val layoutInfo by androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { gridState.layoutInfo } }
    val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }

    if (itemInfo == null) {
        this
    } else {
        val distFromTop = itemInfo.offset.y.toFloat()
        val t = if (distFromTop < topPaddingPx) {
            ((topPaddingPx - distFromTop) / edgeZonePx).coerceIn(0f, 1f)
        } else {
            0f
        }

        this.graphicsLayer {
            val scale = 1f - (maxScaleDrop * t)
            scaleX = scale
            scaleY = scale
            alpha = 1f - (maxAlphaDrop * t)
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
        }
    }
}
