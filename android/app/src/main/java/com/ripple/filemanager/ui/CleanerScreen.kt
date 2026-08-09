package com.ripple.filemanager.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.videoFrameMillis
import com.ripple.filemanager.*
import kotlinx.collections.immutable.ImmutableSet
import com.ripple.filemanager.AppAction
import androidx.compose.material3.SnackbarHostState
import com.ripple.filemanager.CleanerData
import com.ripple.filemanager.CleanerCategoryData
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.ripple.filemanager.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanerScreen(state: com.ripple.filemanager.AppState, onAction: (AppAction) -> Unit, snackbarHostState: SnackbarHostState) {
      val context = androidx.compose.ui.platform.LocalContext.current
    BackHandler {
        if (state.currentCleanerCategory != null) {
            onAction(AppAction.SetCleanerCategory(null))
        } else {
            onAction(AppAction.SetCleanerScreenVisible(false))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val data = state.cleanerData
        if (data != null) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { 
                            Text(state.currentCleanerCategory ?: "Storage", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        },
                        navigationIcon = {
                            if (state.currentCleanerCategory != null) {
                                IconButton(onClick = { onAction(AppAction.SetCleanerCategory(null)) }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                                }
                            } else {
                                IconButton(onClick = { onAction(AppAction.SetCleanerScreenVisible(false)) }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                                }
                            }
                        },
                        actions = {
                            if (state.currentCleanerCategory != null) {
                                IconButton(onClick = {
                                    val catData = when (state.currentCleanerCategory) {
                                        "Documents" -> data.documents
                                        "Images" -> data.images
                                        "Videos" -> data.videos
                                        "Audio" -> data.audio
                                        "Apps" -> data.apps
                                        "Empty folders" -> data.emptyFolders
                                        "Duplicates" -> data.duplicates
                                        else -> null
                                    }
                                    if (catData != null) onAction(AppAction.SelectAllCleanerFiles(catData.files.map { it.id }))
                                }) {
                                    Icon(Icons.Default.SelectAll, contentDescription = stringResource(R.string.select_all))
                                }
                                IconButton(onClick = { onAction(AppAction.ClearCleanerSelection) }) {
                                    Icon(Icons.Default.Deselect, contentDescription = stringResource(R.string.select_none))
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                },
                bottomBar = {
                    if (state.currentCleanerCategory != null && state.cleanerSelectedFiles.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth().height(64.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(pluralStringResource(R.plurals.files_selected, state.cleanerSelectedFiles.size, state.cleanerSelectedFiles.size), color = MaterialTheme.colorScheme.onSurface)
                                Button(
                                    onClick = { onAction(AppAction.DeleteSelectedCleanerFiles) },
                                    shape = com.ripple.filemanager.ui.getDynamicCornerShape(0f, state.cornerRoundness),
                                    colors = ButtonDefaults.buttonColors(containerColor = com.ripple.filemanager.ui.theme.SkylineColors.Rust, contentColor = androidx.compose.ui.graphics.Color(0xFF161009))
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    com.ripple.filemanager.ui.MonoLabel("DELETE", color = androidx.compose.ui.graphics.Color(0xFF161009), fontSize = 12)
                                }
                            }
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    if (state.currentCleanerCategory == null) {
                        CleanerOverview(data = data, cornerRoundness = state.cornerRoundness, gridColumns = state.gridColumns, onCategoryClick = { onAction(AppAction.SetCleanerCategory(it)) })
                    } else {
                        val categoryData = when (state.currentCleanerCategory) {
                            "Documents" -> data.documents
                            "Images" -> data.images
                            "Videos" -> data.videos
                            "Audio" -> data.audio
                            "Apps" -> data.apps
                            "Empty folders" -> data.emptyFolders
                            "Duplicates" -> data.duplicates
                            else -> null
                        }
                        if (categoryData != null) {
                            CategoryDetailView(
                                categoryData = categoryData,
                                selectedFiles = state.cleanerSelectedFiles,
                                cornerRoundness = state.cornerRoundness,
                                gridColumns = state.gridColumns,
                                iconShape = state.activeIconShape,
                                onFileToggle = { onAction(AppAction.ToggleCleanerSelection(it)) }
                            )
                        }
                    }
                }
            }
        }
        
        if (state.cleanerLoading) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun CleanerOverview(data: CleanerData, cornerRoundness: Float, gridColumns: Int, onCategoryClick: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val usedBytes = data.totalStorageBytes - data.freeStorageBytes
    
    val colors = listOf(
        Color(0xFF8A6A44), // Documents
        Color(0xFFE0AC70), // Images
        Color(0xFFC1654A), // Videos
        Color(0xFFB08A52), // Audio
        Color(0xFF7C93A0), // Apps
        Color(0xFF5A4A36), // Empty folders
        Color(0xFF6F7A4A), // Other files
        Color(0xFFD98A7A)  // Duplicates
    )
    
    val categories = listOf(
        Triple("Documents", data.documents.totalSizeBytes, Icons.Outlined.Description),
        Triple("Images", data.images.totalSizeBytes, Icons.Outlined.Image),
        Triple("Videos", data.videos.totalSizeBytes, Icons.Outlined.VideoFile),
        Triple("Audio", data.audio.totalSizeBytes, Icons.Outlined.AudioFile),
        Triple("Apps", data.apps.totalSizeBytes, Icons.Outlined.Apps),
        Triple("Empty folders", data.emptyFolders.totalSizeBytes, Icons.Outlined.FolderOpen),
        Triple("Other files", data.otherBytes, Icons.Outlined.InsertDriveFile),
        Triple("Duplicates", data.duplicates.totalSizeBytes, Icons.Outlined.FolderOpen)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Column {
                com.ripple.filemanager.ui.BlueprintCard(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    heroEmphasis = true,
                    cornerRoundness = cornerRoundness
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        // 1. Header
                        Text(
                            text = "INTERNAL STORAGE",
                            fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp,
                            color = com.ripple.filemanager.ui.theme.SkylineColors.AmberDim,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = formatSize(usedBytes),
                                fontFamily = MaterialTheme.typography.headlineMedium.fontFamily,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = com.ripple.filemanager.ui.theme.SkylineColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "used of ${formatSize(data.totalStorageBytes)}",
                                fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
                                fontSize = 16.sp,
                                color = com.ripple.filemanager.ui.theme.SkylineColors.TextDim,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 2. Segmented bar
                        var hoveredSegment by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Int?>(null) }
                        val totalUsed = usedBytes.toFloat()
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().height(24.dp)
                        ) {
                            if (totalUsed > 0) {
                                categories.forEachIndexed { index, cat ->
                                    val pct = (cat.second.toFloat() / totalUsed) * 100f
                                    if (pct > 0 || cat.second > 0) {
                                        val weight = maxOf(pct, 1.2f)
                                        val alpha = if (hoveredSegment == null || hoveredSegment == index) 1f else 0.35f
                                        Box(
                                            modifier = Modifier
                                                .weight(weight)
                                                .fillMaxHeight()
                                                .background(colors[index].copy(alpha = alpha))
                                                .clickable { 
                                                    if (cat.first != "Other files") {
                                                        onCategoryClick(cat.first)
                                                    }
                                                }
                                        )
                                        if (index < categories.size - 1) {
                                            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 3. Legend List
                        Column(modifier = Modifier.fillMaxWidth()) {
                            categories.forEachIndexed { index, cat ->
                                val pct = if (totalUsed > 0) (cat.second.toFloat() / totalUsed) * 100f else 0f
                                if (pct > 0 || cat.second > 0) {
                                    val alpha = if (hoveredSegment == null || hoveredSegment == index) 1f else 0.35f
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(9.dp)
                                                .background(colors[index].copy(alpha = alpha))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = cat.first,
                                            fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                                            fontSize = 12.sp,
                                            color = com.ripple.filemanager.ui.theme.SkylineColors.TextPrimary2,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = formatSize(cat.second),
                                            fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                                            fontSize = 12.sp,
                                            color = com.ripple.filemanager.ui.theme.SkylineColors.TextDim,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Free space + Manage storage row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, com.ripple.filemanager.ui.theme.SkylineColors.Border)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SdStorage,
                            contentDescription = null,
                            tint = com.ripple.filemanager.ui.theme.SkylineColors.Amber,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "${formatSize(data.freeStorageBytes)} free",
                                fontFamily = MaterialTheme.typography.headlineMedium.fontFamily,
                                fontSize = 16.sp,
                                color = com.ripple.filemanager.ui.theme.SkylineColors.TextPrimary
                            )
                            val freePercent = if (data.totalStorageBytes > 0) (data.freeStorageBytes * 100 / data.totalStorageBytes).toInt() else 0
                            Text(
                                text = "$freePercent% FREE OF TOTAL STORAGE",
                                fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                                fontSize = 10.sp,
                                color = com.ripple.filemanager.ui.theme.SkylineColors.TextDim
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))

                    com.ripple.filemanager.ui.OffsetFab(
                        onClick = { 
                            val intent = android.content.Intent(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
                            try { context.startActivity(intent) } catch (e: Exception) {
                                val fallback = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                                try { context.startActivity(fallback) } catch (e2: Exception) {}
                            }
                        },
                        cornerRoundness = cornerRoundness,
                        width = null,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "MANAGE",
                            fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                            fontWeight = FontWeight.Bold,
                            color = com.ripple.filemanager.ui.theme.SkylineColors.Background,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
        
        // 5. "Storage breakdown" section label
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Text(
                text = "STORAGE BREAKDOWN",
                fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
                color = com.ripple.filemanager.ui.theme.SkylineColors.AmberDim,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        items(categories.size) { index ->
            val cat = categories[index]
            StorageBreakdownCard(
                title = cat.first,
                sizeBytes = cat.second,
                totalUsedBytes = usedBytes,
                icon = cat.third,
                color = colors[index],
                cornerRoundness = cornerRoundness,
                onClick = {
                    if (cat.first != "Other files") {
                        onCategoryClick(cat.first)
                    }
                }
            )
        }
    }
}

@Composable
fun StorageBreakdownCard(title: String, sizeBytes: Long, totalUsedBytes: Long, icon: ImageVector, color: Color, cornerRoundness: Float, onClick: () -> Unit) {
    Surface(
        shape = getDynamicCornerShape(0f, cornerRoundness),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, com.ripple.filemanager.ui.theme.SkylineColors.Border),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(color))
            
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, color)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = title,
                    fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = com.ripple.filemanager.ui.theme.SkylineColors.TextPrimary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatSize(sizeBytes),
                    fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                    color = com.ripple.filemanager.ui.theme.SkylineColors.TextPrimary2,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                val pct = if (totalUsedBytes > 0) (sizeBytes * 100 / totalUsedBytes).toInt() else 0
                Text(
                    text = "$pct%",
                    fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                    color = com.ripple.filemanager.ui.theme.SkylineColors.TextDim,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun CategoryDetailView(
    categoryData: CleanerCategoryData,
    selectedFiles: ImmutableSet<Int>,
    cornerRoundness: Float,
    gridColumns: Int,
    iconShape: com.ripple.filemanager.IconShapeType,
    onFileToggle: (Int) -> Unit
) {
    if (categoryData.files.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_files_to_clean))
        }
        return
    }

    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(categoryData.files) { file ->
            val isSelected = selectedFiles.contains(file.id)
            val isMedia = file.type in setOf("image", "video") // FileRepository maps jpg/png etc to "image", mp4 etc to "video"
            
            Box(
                modifier = Modifier
                    .aspectRatio(if (gridColumns > 2) 1f else 0.85f)
                    .clip(getDynamicCornerShape(8f, cornerRoundness))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onFileToggle(file.id) }
            ) {
                if (isMedia) {
                    val requestBuilder = remember(file.path) {
                        coil.request.ImageRequest.Builder(context)
                            .data(java.io.File(file.path))
                            .apply { if (file.type == "video") videoFrameMillis(1000) }
                            .build()
                    }
                    AsyncImage(
                        model = requestBuilder,
                        imageLoader = imageLoader,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(modifier = Modifier.size(48.dp)) {
                            FileShapeIcon(type = file.type, name = file.name, size = 48, path = file.path, iconShape = iconShape, duration = file.duration)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(file.name, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (!file.isEmptyFolder) {
                            Text(file.size, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                
                RadioButton(
                    selected = isSelected,
                    onClick = { onFileToggle(file.id) },
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                )
            }
        }
    }
}

fun formatSize(bytes: Long): String {
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
