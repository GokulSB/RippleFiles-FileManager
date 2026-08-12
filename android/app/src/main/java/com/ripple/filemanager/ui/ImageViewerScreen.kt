package com.ripple.filemanager.ui

import androidx.compose.ui.res.stringResource
import com.ripple.filemanager.R

import android.content.Context
import android.content.Intent
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawBehind
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
    cornerRoundness: Float = 0.5f,
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
        val actualIndex = if (files.isNotEmpty()) pagerState.currentPage % files.size else 0
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { com.ripple.filemanager.ui.MonoLabel("DELETE IMAGE?", color = com.ripple.filemanager.ui.theme.SkylineColors.Amber, fontSize = 14) },
            text = { Text(stringResource(R.string.delete_warning_undone)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    val currentFile = files.getOrNull(pagerState.currentPage)
                    if (currentFile != null) {
                        onDeleteClick(currentFile)
                        // Note: We don't close here anymore. LaunchedEffect below handles closing if no files remain.
                    }
                }) { Text(stringResource(R.string.delete_action), color = com.ripple.filemanager.ui.theme.SkylineColors.Amber) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel), color = com.ripple.filemanager.ui.theme.SkylineColors.TextDim) }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = com.ripple.filemanager.ui.getDynamicCornerShape(12f, cornerRoundness)
        )
    }

    LaunchedEffect(files) {
        if (files.isEmpty()) {
            onClose()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        val currentFile = files.getOrNull(pagerState.currentPage)
        
        if (currentFile != null) {
            TopOverlay(
                file = currentFile,
                currentIndex = pagerState.currentPage,
                totalCount = files.size,
                cornerRoundness = cornerRoundness,
                onBack = onClose,
                onAction = onAction,
                onDeleteClick = { showDeleteConfirm = true }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF0D0904)),
            contentAlignment = Alignment.Center
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                if (files.isNotEmpty()) {
                    val file = files[page]
                    ZoomableImage(
                        file = file,
                        onTap = { /* Removed overlay toggle */ }
                    )
                }
            }
        }

        if (currentFile != null) {
            BottomOverlay(
                file = currentFile,
                cornerRoundness = cornerRoundness,
                onAction = onAction,
                onDetailsClick = { showDetailsSheet = !showDetailsSheet },
                onDeleteClick = { showDeleteConfirm = true },
                showDetails = showDetailsSheet
            )
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
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        
                        // Let HorizontalPager handle standard swipes when not zoomed
                        if (scale == 1f && event.changes.size == 1) {
                            continue
                        }
                        
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        
                        val newScale = (scale * zoom).coerceIn(1f, 5f)
                        scale = newScale
                        
                        if (scale > 1f) {
                            val maxPanX = (scale - 1) * size.width.toFloat() / 2
                            val maxPanY = (scale - 1) * size.height.toFloat() / 2
                            offset = Offset(
                                x = (offset.x + pan.x * scale).coerceIn(-maxPanX, maxPanX),
                                y = (offset.y + pan.y * scale).coerceIn(-maxPanY, maxPanY)
                            )
                            // Consume pointer position changes so HorizontalPager doesn't swipe while zoomed
                            event.changes.forEach { 
                                if (it.positionChanged()) it.consume() 
                            }
                        } else {
                            offset = Offset.Zero
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = File(file.path),
            contentDescription = file.name,
            modifier = Modifier
                .fillMaxSize(0.88f)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .drawBehind {
                    drawRect(
                        color = Color.Black.copy(alpha = 0.55f),
                        topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                        size = size
                    )
                }
                .border(1.dp, Color(0xFF3a2f24)),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun TopOverlay(
    file: FileItem,
    currentIndex: Int,
    totalCount: Int,
    cornerRoundness: Float,
    onBack: () -> Unit,
    onAction: (AppAction) -> Unit,
    onDeleteClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            LedgerChipButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                cornerRoundness = cornerRoundness,
                onClick = onBack
            )
            
            // Counter
            Surface(
                shape = com.ripple.filemanager.ui.getDynamicCornerShape(12f, cornerRoundness),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(
                        text = "${currentIndex + 1}",
                        fontFamily = com.ripple.filemanager.ui.theme.JetBrainsMonoFamily,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " / $totalCount",
                        fontFamily = com.ripple.filemanager.ui.theme.JetBrainsMonoFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Rotate button (FLAGGED: No AppAction.RotateImage exists)
            LedgerChipButton(
                icon = Icons.Filled.RotateRight,
                cornerRoundness = cornerRoundness,
                onClick = { /* TODO: Missing rotate action */ }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress rule
        val progress = if (totalCount > 1) (currentIndex.toFloat() / (totalCount - 1).coerceAtLeast(1)) else 1f
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
fun LedgerChipButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    cornerRoundness: Float,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(44.dp),
        shape = com.ripple.filemanager.ui.getDynamicCornerShape(12f, cornerRoundness),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun BottomOverlay(
    file: FileItem,
    cornerRoundness: Float,
    onAction: (AppAction) -> Unit,
    onDetailsClick: () -> Unit,
    onDeleteClick: () -> Unit,
    showDetails: Boolean = false
) {
    val context = LocalContext.current
    val exifData = remember(file) { extractExifData(file.path) }
    val sizeFormatted = formatSize(File(file.path).length())
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(bottom = 24.dp)
    ) {
        // Filename block
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = file.name,
                fontFamily = com.ripple.filemanager.ui.theme.FrauncesFontFamily,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            val dim = exifData.dimensions ?: "—"
            val src = exifData.source ?: "—"
            Text(
                text = "$sizeFormatted · $dim · $src",
                fontFamily = com.ripple.filemanager.ui.theme.JetBrainsMonoFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LedgerToolbarAction(icon = Icons.Filled.Edit, label = "EDIT", cornerRoundness = cornerRoundness) {
                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(file.path))
                val intent = Intent(Intent.ACTION_EDIT).apply {
                    setDataAndType(uri, "image/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try { context.startActivity(Intent.createChooser(intent, "Edit Image")) } catch (e: Exception) {}
            }
            LedgerToolbarAction(icon = Icons.Filled.Share, label = "SHARE", cornerRoundness = cornerRoundness) {
                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(file.path))
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share Image"))
            }
            val starTint = if (file.isPinned) com.ripple.filemanager.ui.theme.SkylineColors.Amber else MaterialTheme.colorScheme.onSurface
            LedgerToolbarAction(icon = Icons.Filled.Star, label = "FAV", iconTint = starTint, cornerRoundness = cornerRoundness) {
                onAction(AppAction.TogglePin(file.path))
            }
            LedgerToolbarAction(icon = Icons.Filled.Info, label = "INFO", cornerRoundness = cornerRoundness) {
                onDetailsClick()
            }
            LedgerToolbarAction(
                icon = Icons.Filled.Delete, 
                label = "DEL", 
                iconTint = MaterialTheme.colorScheme.error,
                borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                cornerRoundness = cornerRoundness
            ) {
                onDeleteClick()
            }
        }
        
        // Details Slide-up
        AnimatedVisibility(
            visible = showDetails,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            LedgerImageDetails(file = file, exifData = exifData, cornerRoundness = cornerRoundness)
        }
    }
}

@Composable
fun LedgerToolbarAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    cornerRoundness: Float,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LedgerChipButton(icon = icon, iconTint = iconTint, borderColor = borderColor, cornerRoundness = cornerRoundness, onClick = onClick)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontFamily = com.ripple.filemanager.ui.theme.JetBrainsMonoFamily,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 9.sp,
            letterSpacing = 0.54.sp
        )
    }
}

@Composable
fun LedgerImageDetails(file: FileItem, exifData: ExifData, cornerRoundness: Float) {
    val modifiedFormatted = SimpleDateFormat("MMM dd, yyyy, h:mm a", Locale.getDefault()).format(Date(file.lastModified))
    val ext = file.name.substringAfterLast('.', "—").uppercase()
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, start = 16.dp, end = 16.dp),
        shape = com.ripple.filemanager.ui.getDynamicCornerShape(12f, cornerRoundness),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                DetailColumn("MODIFIED", modifiedFormatted, modifier = Modifier.weight(1f))
                DetailColumn("FORMAT", ext, modifier = Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                DetailColumn("RESOLUTION", exifData.dimensions ?: "—", modifier = Modifier.weight(1f))
                val sizeFormatted = formatSize(File(file.path).length())
                DetailColumn("SIZE", sizeFormatted, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun DetailColumn(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontFamily = com.ripple.filemanager.ui.theme.JetBrainsMonoFamily,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontFamily = com.ripple.filemanager.ui.theme.JetBrainsMonoFamily,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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


