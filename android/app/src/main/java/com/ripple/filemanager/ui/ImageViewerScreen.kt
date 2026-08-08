package com.ripple.filemanager.ui

import android.content.Context
import android.content.Intent
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ripple.filemanager.AppAction
import com.ripple.filemanager.FileItem
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(
    files: List<FileItem>,
    initialIndex: Int,
    onAction: (AppAction) -> Unit,
    onClose: () -> Unit,
    onDeleteClick: (FileItem) -> Unit,
    onNavigateToFolder: (String) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { files.size })
    var showOverlays by remember { mutableStateOf(true) }
    var showDetailsSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    BackHandler {
        if (showDetailsSheet) {
            showDetailsSheet = false
        } else if (showDeleteConfirm) {
            showDeleteConfirm = false
        } else {
            onClose()
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { com.ripple.filemanager.ui.MonoLabel("DELETE IMAGE?", color = com.ripple.filemanager.ui.theme.SkylineColors.Amber, fontSize = 14) },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    val currentFile = files.getOrNull(pagerState.currentPage)
                    if (currentFile != null) {
                        onDeleteClick(currentFile)
                        onClose() // Close viewer after deletion
                    }
                }) { Text("DELETE", color = com.ripple.filemanager.ui.theme.SkylineColors.Amber) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("CANCEL", color = com.ripple.filemanager.ui.theme.SkylineColors.TextDim) }
            },
            containerColor = com.ripple.filemanager.ui.theme.SkylineColors.Surface,
            shape = com.ripple.filemanager.ui.getDynamicCornerShape(12f, 0.5f)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            val file = files[page]
            ZoomableImage(
                file = file,
                onTap = { showOverlays = !showOverlays }
            )
        }

        // Page Indicator
        if (files.size > 1) {
            AnimatedVisibility(
                visible = showOverlays,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 96.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in files.indices) {
                        val isActive = i == pagerState.currentPage
                        Box(
                            modifier = Modifier
                                .height(4.dp)
                                .width(if (isActive) 14.dp else 4.dp)
                                .clip(CircleShape)
                                .background(if (isActive) Color(0xFFE0AC70) else Color.White.copy(alpha = 0.35f))
                        )
                    }
                }
            }
        }

        // Overlays
        AnimatedVisibility(
            visible = showOverlays,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val currentFile = files.getOrNull(pagerState.currentPage)
                if (currentFile != null) {
                    TopOverlay(
                        file = currentFile,
                        currentIndex = pagerState.currentPage,
                        totalCount = files.size,
                        onBack = onClose,
                        onAction = onAction,
                        onDeleteClick = { showDeleteConfirm = true }
                    )
                    BottomOverlay(
                        file = currentFile,
                        onAction = onAction,
                        onDetailsClick = { showDetailsSheet = true },
                        onDeleteClick = { showDeleteConfirm = true }
                    )
                }
            }
        }
    }

    if (showDetailsSheet) {
        val currentFile = files.getOrNull(pagerState.currentPage)
        if (currentFile != null) {
            ModalBottomSheet(
                onDismissRequest = { showDetailsSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                ImageDetailsContent(
                    file = currentFile,
                    onClose = { showDetailsSheet = false },
                    onNavigateToFolder = {
                        showDetailsSheet = false
                        onClose()
                        onNavigateToFolder(it)
                    }
                )
            }
        }
    }
}

@Composable
fun ZoomableImage(file: FileItem, onTap: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        scale = if (scale > 1f) 1f else 2.5f
                        offset = Offset.Zero
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale == 1f) {
                        offset = Offset.Zero
                    } else {
                        val maxPanX = (scale - 1) * size.width.toFloat() / 2
                        val maxPanY = (scale - 1) * size.height.toFloat() / 2
                        offset = Offset(
                            x = (offset.x + pan.x * scale).coerceIn(-maxPanX, maxPanX),
                            y = (offset.y + pan.y * scale).coerceIn(-maxPanY, maxPanY)
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = File(file.path),
            contentDescription = file.name,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun TopOverlay(
    file: FileItem,
    currentIndex: Int,
    totalCount: Int,
    onBack: () -> Unit,
    onAction: (AppAction) -> Unit,
    onDeleteClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                )
            )
            .padding(top = 48.dp, start = 8.dp, end = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            
            Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(
                    text = file.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                )
                if (totalCount > 1) {
                    Text(
                        text = "${currentIndex + 1} of $totalCount",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }

        }
    }
}

@Composable
fun BottomOverlay(
    file: FileItem,
    onAction: (AppAction) -> Unit,
    onDetailsClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                    )
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionButton(icon = Icons.Filled.Edit, label = "Edit", onClick = {
                    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(file.path))
                    val intent = Intent(Intent.ACTION_EDIT).apply {
                        setDataAndType(uri, "image/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try { context.startActivity(Intent.createChooser(intent, "Edit Image")) } catch (e: Exception) {}
                })
                ActionButton(icon = Icons.Filled.Share, label = "Share", onClick = {
                    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(file.path))
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Image"))
                })
                ActionButton(icon = Icons.Filled.Star, label = "Favorite", isActive = file.isPinned, onClick = {
                    onAction(AppAction.TogglePin(file.path))
                })
                ActionButton(icon = Icons.Filled.Delete, label = "Delete", onClick = onDeleteClick)
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onDetailsClick() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = Color(0xFFE0AC70), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Details", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, tint = Color(0xFFE0AC70), modifier = Modifier.size(18.dp))
                }
            }
        }
        }
    }
}

@Composable
fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isActive: Boolean = false, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isActive) Color(0xFFE0AC70).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = if (isActive) Color(0xFFE0AC70) else Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Color.White, fontSize = 10.sp)
    }
}

@Composable
fun ImageDetailsContent(file: FileItem, onClose: () -> Unit, onNavigateToFolder: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Details", fontSize = 19.sp, color = MaterialTheme.colorScheme.onSurface, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif)
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        val exifData = remember(file) { extractExifData(file.path) }
        val sizeFormatted = formatSize(File(file.path).length())
        val modifiedFormatted = SimpleDateFormat("MMM dd, yyyy, h:mm a", Locale.getDefault()).format(Date(file.lastModified))
        
        DetailRow(icon = Icons.Filled.PhotoSizeSelectActual, label = "Dimensions", value = exifData.dimensions ?: "Unknown")
        DetailRow(icon = Icons.Filled.SdStorage, label = "File size", value = sizeFormatted)
        DetailRow(icon = Icons.Filled.CalendarToday, label = "Date modified", value = modifiedFormatted)
        if (exifData.source != null) {
            DetailRow(icon = Icons.Filled.CameraAlt, label = "Source", value = exifData.source)
        }
        
        val folderPath = File(file.path).parent ?: "/"
        DetailRow(icon = Icons.Filled.Folder, label = "Location", value = folderPath, onClick = { onNavigateToFolder(folderPath) })
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, onClick: (() -> Unit)? = null) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0AC70).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFFE0AC70), modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        HorizontalDivider(color = Color(0xFF2A221A), thickness = 1.dp)
    }
}

data class ExifData(val dimensions: String?, val source: String?)

fun extractExifData(path: String): ExifData {
    try {
        val exif = ExifInterface(path)
        val width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
        val length = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
        val make = exif.getAttribute(ExifInterface.TAG_MAKE)
        val model = exif.getAttribute(ExifInterface.TAG_MODEL)
        
        val dims = if (width > 0 && length > 0) "${width}x${length}" else null
        val source = if (make != null && model != null) "$make $model" else model ?: make
        
        return ExifData(dims, source)
    } catch (e: Exception) {
        return ExifData(null, null)
    }
}


