package com.ripple.filemanager.ui

import coil.request.videoFrameMillis

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripple.filemanager.*
import com.ripple.filemanager.R
import com.ripple.filemanager.ui.expressive.*

@Composable
fun CleanerScreen(
    state: AppState,
    onAction: (AppAction) -> Unit,
    snackbarHostState: SnackbarHostState,
    onNavigateToCategory: ((String) -> Unit)? = null
) {
    val colors = ExpressiveTheme.colors
    val context = LocalContext.current
    val haptics = com.ripple.filemanager.haptics.rememberHapticsController { state.haptics }

    BackHandler(enabled = state.currentCleanerCategory != null) {
        if (state.currentCleanerCategory != null) {
            onAction(AppAction.SetCleanerCategory(null))
        } else {
            onAction(AppAction.SetCleanerScreenVisible(false))
        }
    }

    RippleBackground(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
        val data = state.cleanerData

        if (data != null) {
            if (state.currentCleanerCategory == null) {
                Column(modifier = Modifier.fillMaxSize()) {
                    PageHeader(
                        title = "Storage",
                        onBack = { onAction(AppAction.SetCleanerScreenVisible(false)) }
                    )

                    ExpressiveStorageOverview(
                        data = data,
                        onCategoryClick = { categoryName ->
                            haptics.cleaner()
                            val filterKey = when (categoryName.lowercase()) {
                                "documents", "docs", "doc" -> "doc"
                                "images", "image" -> "image"
                                "videos", "video" -> "video"
                                "audio" -> "audio"
                                "apps", "apk" -> "apk"
                                "downloads", "download" -> "download"
                                "archives", "archive" -> "archive"
                                "duplicates" -> "duplicates"
                                "other files", "large" -> "large"
                                else -> categoryName.lowercase()
                            }
                            if (onNavigateToCategory != null) {
                                onNavigateToCategory(filterKey)
                            } else {
                                onAction(AppAction.SetCleanerCategory(categoryName))
                            }
                        },
                        onManageClick = {
                            val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val fallback = Intent(Settings.ACTION_SETTINGS)
                                try { context.startActivity(fallback) } catch (e2: Exception) {}
                            }
                        }
                    )
                }
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
                    ExpressiveCategoryDetailScreen(
                        categoryKey = state.currentCleanerCategory!!,
                        files = categoryData.files,
                        state = state,
                        onAction = onAction,
                        onBack = { onAction(AppAction.SetCleanerCategory(null)) },
                        onTrashClick = { onAction(AppAction.SetTrashScreenVisible(true)) },
                        onFileClick = { file ->
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", java.io.File(file.path))
                                setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try { context.startActivity(intent) } catch (e: Exception) {}
                        },
                        onFileMenuClick = {},
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (state.cleanerLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colors.accent)
            }
        }
    }
}
}

@Composable
private fun ExpressiveStorageOverview(
    data: CleanerData,
    onCategoryClick: (String) -> Unit,
    onManageClick: () -> Unit
) {
    val colors = ExpressiveTheme.colors
    val usedBytes = (data.totalStorageBytes - data.freeStorageBytes).coerceAtLeast(0L)
    val totalGb = data.totalStorageBytes.toFloat() / (1024f * 1024f * 1024f)
    val usedGb = usedBytes.toFloat() / (1024f * 1024f * 1024f)
    val freeGb = data.freeStorageBytes.toFloat() / (1024f * 1024f * 1024f)
    val freePercent = if (data.totalStorageBytes > 0) (data.freeStorageBytes * 100 / data.totalStorageBytes).toInt() else 0

    val categoryList = remember(data) {
        listOf(
            StorageBreakdownItem("Documents", data.documents.totalSizeBytes, ExpressiveTokens.PastelSky, Icons.Outlined.Description, "docs"),
            StorageBreakdownItem("Images", data.images.totalSizeBytes, ExpressiveTokens.PastelCoral, Icons.Outlined.Image, "images"),
            StorageBreakdownItem("Videos", data.videos.totalSizeBytes, ExpressiveTokens.PastelRose, Icons.Outlined.VideoFile, "videos"),
            StorageBreakdownItem("Audio", data.audio.totalSizeBytes, ExpressiveTokens.PastelViolet, Icons.Outlined.AudioFile, "audio"),
            StorageBreakdownItem("Apps", data.apps.totalSizeBytes, ExpressiveTokens.PastelSage, Icons.Outlined.Apps, "apps"),
            StorageBreakdownItem("Other files", data.otherBytes, ExpressiveTokens.PastelTeal, Icons.AutoMirrored.Outlined.InsertDriveFile, "large"),
            StorageBreakdownItem("Duplicates", data.duplicates.totalSizeBytes, ExpressiveTokens.PastelSand, Icons.Outlined.FolderOpen, "archives")
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 130.dp)
    ) {
        // Container -> Inner Card: Storage Bar & Legend
        item {
            SectionContainer {
                InnerCard {
                    Column {
                        Text(
                            text = "Internal storage".uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = colors.muted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "%.1f GB".format(usedGb),
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Light,
                                color = colors.text
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "used of %.1f GB".format(totalGb),
                                fontSize = 14.sp,
                                color = colors.muted,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 22dp Segmented Bar (2dp gaps, 6dp min width)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(colors.insetCard)
                        ) {
                            val totalUsed = usedBytes.toFloat().coerceAtLeast(1f)
                            categoryList.forEachIndexed { index, cat ->
                                val fraction = (cat.sizeBytes.toFloat() / totalUsed).coerceIn(0f, 1f)
                                if (fraction > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .weight(fraction.coerceAtLeast(0.04f))
                                            .fillMaxHeight()
                                            .background(cat.color)
                                    )
                                    if (index < categoryList.size - 1) {
                                        Spacer(modifier = Modifier.width(2.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Legend rows
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            categoryList.forEach { cat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onCategoryClick(cat.name) }
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(cat.color)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = cat.name,
                                        fontSize = 13.sp,
                                        color = colors.text,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = formatFileSize(cat.sizeBytes),
                                        fontSize = 12.sp,
                                        color = colors.muted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Free Space Card with SD Cookie Icon and MANAGE Pill Button
        item {
            SectionContainer {
                InnerCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            CookieIcon(
                                icon = Icons.Outlined.SdCard,
                                bgColor = ExpressiveTokens.PastelSand,
                                size = 48.dp,
                                lobes = 8
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "%.1f GB free".format(freeGb),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.text
                                )
                                Text(
                                    text = "$freePercent% free of total storage",
                                    fontSize = 12.sp,
                                    color = colors.muted
                                )
                            }
                        }

                        // MANAGE pill button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.accent)
                                .clickable(onClick = onManageClick)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "MANAGE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.ink
                            )
                        }
                    }
                }
            }
        }

        // Storage Breakdown Section Title
        item {
            SectionTitle(title = "Storage breakdown")
        }

        // 2-column cards (cookie icon, name, size, %) – last odd card spans 2 columns
        item {
            val totalUsed = usedBytes.toFloat().coerceAtLeast(1f)
            val rows = categoryList.chunked(2)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEach { pair ->
                    if (pair.size == 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StorageCategoryCard(
                                item = pair[0],
                                totalUsed = totalUsed,
                                onClick = { onCategoryClick(pair[0].name) },
                                modifier = Modifier.weight(1f)
                            )
                            StorageCategoryCard(
                                item = pair[1],
                                totalUsed = totalUsed,
                                onClick = { onCategoryClick(pair[1].name) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        // Last odd card spans full 2 columns
                        StorageCategoryCard(
                            item = pair[0],
                            totalUsed = totalUsed,
                            onClick = { onCategoryClick(pair[0].name) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

private data class StorageBreakdownItem(
    val name: String,
    val sizeBytes: Long,
    val color: Color,
    val icon: ImageVector,
    val categoryKey: String
)

@Composable
private fun StorageCategoryCard(
    item: StorageBreakdownItem,
    totalUsed: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ExpressiveTheme.colors
    val pct = ((item.sizeBytes.toFloat() / totalUsed) * 100).toInt()

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CookieIcon(
                    icon = item.icon,
                    bgColor = item.color,
                    size = 42.dp,
                    lobes = 8
                )
                Text(
                    text = "$pct%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.muted
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = formatFileSize(item.sizeBytes),
                fontSize = 12.sp,
                color = colors.muted
            )
        }
    }
}


@Composable
fun CategoryDetailView(
    categoryData: CleanerCategoryData,
    selectedFiles: kotlinx.collections.immutable.ImmutableSet<Int>,
    cornerRoundness: Float,
    gridColumns: Int,
    iconShape: com.ripple.filemanager.IconShapeType,
    onFileToggle: (Int) -> Unit
) {
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    if (categoryData.files.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_files_to_clean))
        }
        return
    }

    val context = LocalContext.current
    val imageLoader = remember {
        coil.ImageLoader.Builder(context)
            .components {
                add(coil.decode.VideoFrameDecoder.Factory())
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
            val isMedia = file.type in setOf("image", "video")
            
            Box(
                modifier = Modifier
                    .aspectRatio(if (gridColumns > 2) 1f else 0.85f)
                    .clip(com.ripple.filemanager.ui.getDynamicCornerShape(8f, cornerRoundness))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { haptics.tap(); onFileToggle(file.id) }
            ) {
                if (isMedia) {
                    val requestBuilder = remember(file.path) {
                        coil.request.ImageRequest.Builder(context)
                            .data(java.io.File(file.path))
                            .apply { if (file.type == "video") videoFrameMillis(1000L) }
                            .build()
                    }
                    coil.compose.AsyncImage(
                        model = requestBuilder,
                        imageLoader = imageLoader,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
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
                    onClick = { haptics.tap(); onFileToggle(file.id) },
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
