package com.ripple.filemanager.ui

import androidx.compose.ui.res.stringResource
import com.ripple.filemanager.R
import com.ripple.filemanager.ui.expressive.ExpressiveConfirmationSheet

import android.content.Context
import android.content.Intent
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ripple.filemanager.AppAction
import com.ripple.filemanager.FileItem
import com.ripple.filemanager.haptics.LocalHaptics
import com.ripple.filemanager.ui.expressive.CookieIcon
import com.ripple.filemanager.ui.expressive.CookieIconButton
import com.ripple.filemanager.ui.expressive.CookieShape
import com.ripple.filemanager.ui.expressive.ExpressiveTheme
import com.ripple.filemanager.ui.expressive.ExpressiveTokens
import com.ripple.filemanager.ui.expressive.pressScale
import com.ripple.filemanager.ui.theme.LocalAppFont
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
    val colors = ExpressiveTheme.colors
    val haptics = LocalHaptics.current

    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = initialIndex,
        pageCount = { files.size }
    )
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isBottomExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    BackHandler {
        if (showDeleteConfirm) {
            showDeleteConfirm = false
        } else if (isBottomExpanded) {
            isBottomExpanded = false
        } else {
            onClose()
        }
    }

    // Delete confirmation sheet
    if (showDeleteConfirm) {
        val currentFile = files.getOrNull(pagerState.currentPage)
        val deleteTitle = if (currentFile != null) "Delete \"${currentFile.name}\"?" else "Delete image?"
        val warningText = stringResource(R.string.delete_warning_undone)
        ExpressiveConfirmationSheet(
            title = deleteTitle,
            subtitle = warningText,
            confirmLabel = stringResource(R.string.delete_action),
            cancelLabel = stringResource(R.string.cancel),
            onConfirm = {
                haptics.delete()
                showDeleteConfirm = false
                if (currentFile != null) {
                    onDeleteClick(currentFile)
                }
            },
            onDismissRequest = {
                haptics.tap()
                showDeleteConfirm = false
            }
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
            .background(colors.bg)
    ) {
        val currentFile = files.getOrNull(pagerState.currentPage)

        // ═══ TOP BAR ═══
        if (currentFile != null) {
            ExpressiveImageTopBar(
                file = currentFile,
                currentIndex = pagerState.currentPage,
                totalCount = files.size,
                onBack = onClose
            )
        }

        // ═══ IMAGE PAGER ═══
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                if (files.isNotEmpty()) {
                    val file = files[page]
                    ExpressiveZoomableImage(file = file)
                }
            }
        }

        // ═══ BOTTOM ACTION SHEET ═══
        if (currentFile != null) {
            ExpressiveImageBottomSheet(
                file = currentFile,
                isExpanded = isBottomExpanded,
                onToggleExpand = { isBottomExpanded = !isBottomExpanded },
                onAction = onAction,
                onDeleteClick = { showDeleteConfirm = true },
                context = context
            )
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════
//  Zoomable Image
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun ExpressiveZoomableImage(file: FileItem) {
    val colors = ExpressiveTheme.colors
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
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
        // Soft card container behind the image
        Box(
            modifier = Modifier
                .fillMaxSize(0.90f)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.container.copy(alpha = 0.35f))
                .border(
                    1.dp,
                    colors.line.copy(alpha = colors.lineAlpha),
                    RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = File(file.path),
                contentDescription = file.name,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
                    .clip(RoundedCornerShape(16.dp))
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
}


// ═══════════════════════════════════════════════════════════════════════════
//  Expressive Top Bar
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun ExpressiveImageTopBar(
    file: FileItem,
    currentIndex: Int,
    totalCount: Int,
    onBack: () -> Unit
) {
    val colors = ExpressiveTheme.colors
    val haptics = LocalHaptics.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bg)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button — cookie icon
            CookieIconButton(
                onClick = {
                    haptics.tap()
                    onBack()
                },
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                bgColor = colors.card,
                iconTint = colors.text,
                description = "Back",
                size = 42.dp,
                iconSize = 20.dp,
                lobes = 8
            )

            // Filename — centered title
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = file.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = LocalAppFont.current,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${currentIndex + 1} of $totalCount",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = LocalAppFont.current,
                    color = colors.muted
                )
            }

            // Image counter badge — pastel coral cookie
            CookieIcon(
                icon = Icons.Outlined.Image,
                bgColor = ExpressiveTokens.PastelCoral,
                iconTint = ExpressiveTokens.OnPastel,
                size = 42.dp,
                lobes = 8
            )
        }

        // Slim progress rule showing position in gallery
        val progress = if (totalCount > 1) {
            (currentIndex.toFloat() / (totalCount - 1).coerceAtLeast(1))
        } else 1f

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(colors.container)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(colors.accent)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}


// ═══════════════════════════════════════════════════════════════════════════
//  Expressive Bottom Action Sheet
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun ExpressiveImageBottomSheet(
    file: FileItem,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAction: (AppAction) -> Unit,
    onDeleteClick: () -> Unit,
    context: Context
) {
    val colors = ExpressiveTheme.colors
    val haptics = LocalHaptics.current
    val exifData = remember(file) { extractExifData(file.path) }
    val sizeFormatted = formatSize(File(file.path).length())

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "img_sheet_chevron"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
                ambientColor = Color.Transparent,
                spotColor = colors.shadow.copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
            .background(colors.container)
            .border(
                1.dp,
                colors.line.copy(alpha = colors.lineAlpha),
                RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
            )
            .animateContentSize(animationSpec = tween(220, easing = FastOutSlowInEasing))
            .navigationBarsPadding()
            .padding(top = 10.dp, bottom = if (isExpanded) 16.dp else 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Grab handle ──
        Box(
            modifier = Modifier
                .padding(bottom = 8.dp)
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.muted.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    haptics.tap()
                    onToggleExpand()
                }
        )

        // ── Header row: CookieIcon + filename + subtitle + chevron ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image category pastel icon
            CookieIcon(
                icon = Icons.Outlined.Image,
                bgColor = ExpressiveTokens.PastelCoral,
                iconTint = ExpressiveTokens.OnPastel,
                size = 46.dp,
                lobes = 8
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        haptics.tap()
                        onToggleExpand()
                    }
            ) {
                Text(
                    text = file.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = LocalAppFont.current,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val dim = exifData.dimensions ?: "—"
                Text(
                    text = "$sizeFormatted · $dim",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = LocalAppFont.current,
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Chevron toggle
            CookieIconButton(
                onClick = {
                    haptics.tap()
                    onToggleExpand()
                },
                icon = Icons.Default.KeyboardArrowUp,
                bgColor = colors.card,
                iconTint = colors.text,
                description = if (isExpanded) "Collapse" else "Expand",
                size = 36.dp,
                iconSize = 20.dp,
                modifier = Modifier.rotate(chevronRotation),
                lobes = 8
            )
        }

        // ── Action cards row (always visible) ──
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Share
            ImageActionCard(
                icon = Icons.Outlined.Share,
                label = "Share",
                onClick = {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        File(file.path)
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Image"))
                }
            )

            // Edit
            ImageActionCard(
                icon = Icons.Outlined.Edit,
                label = "Edit",
                onClick = {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        File(file.path)
                    )
                    val intent = Intent(Intent.ACTION_EDIT).apply {
                        setDataAndType(uri, "image/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        context.startActivity(Intent.createChooser(intent, "Edit Image"))
                    } catch (_: Exception) {}
                }
            )

            // Favourite
            val isFav = file.isPinned
            ImageActionCard(
                icon = if (isFav) Icons.Filled.Star else Icons.Outlined.StarBorder,
                label = if (isFav) "Unfav" else "Fav",
                isHighlight = isFav,
                onClick = { onAction(AppAction.TogglePin(file.path)) }
            )

            // Set as wallpaper
            ImageActionCard(
                icon = Icons.Outlined.Wallpaper,
                label = "Wallpaper",
                onClick = {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        File(file.path)
                    )
                    val intent = Intent(Intent.ACTION_ATTACH_DATA).apply {
                        setDataAndType(uri, "image/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        putExtra("mimeType", "image/*")
                    }
                    try {
                        context.startActivity(Intent.createChooser(intent, "Set as"))
                    } catch (_: Exception) {}
                }
            )

            // Delete
            ImageActionCard(
                icon = Icons.Outlined.Delete,
                label = "Delete",
                isDestructive = true,
                onClick = onDeleteClick
            )
        }

        // ── Expandable details panel ──
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(180)),
            exit = shrinkVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(150))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(14.dp))

                ExpressiveImageDetailsCard(file = file, exifData = exifData)

                Spacer(modifier = Modifier.height(14.dp))

                // "Open in Gallery" pill button
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(44.dp)
                        .pressScale(pressedScale = 0.96f, interactionSource = interactionSource)
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.accent)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            haptics.tap()
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                File(file.path)
                            )
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "image/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try {
                                context.startActivity(Intent.createChooser(intent, "Open with"))
                            } catch (_: Exception) {}
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.OpenInNew,
                            contentDescription = null,
                            tint = colors.ink,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Open in Gallery",
                            fontFamily = LocalAppFont.current,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.ink
                        )
                    }
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════
//  Image Action Card (matching SelectionActionCard from ExpressiveSelectionBar)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ImageActionCard(
    icon: ImageVector,
    label: String,
    isDestructive: Boolean = false,
    isHighlight: Boolean = false,
    onClick: () -> Unit
) {
    val colors = ExpressiveTheme.colors
    val haptics = LocalHaptics.current

    val containerBg = when {
        isDestructive -> colors.accent.copy(alpha = 0.12f)
        isHighlight -> colors.accent
        else -> colors.card
    }
    val contentColor = when {
        isDestructive -> colors.accent
        isHighlight -> colors.ink
        else -> colors.text
    }
    val borderColor = when {
        isDestructive -> colors.accent.copy(alpha = 0.35f)
        isHighlight -> colors.accent
        else -> colors.line.copy(alpha = colors.lineAlpha * 2f)
    }

    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .width(68.dp)
            .pressScale(pressedScale = 0.94f, interactionSource = interactionSource)
            .clip(RoundedCornerShape(16.dp))
            .background(containerBg)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                haptics.tap()
                onClick()
            }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = LocalAppFont.current,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


// ═══════════════════════════════════════════════════════════════════════════
//  Expressive Details Card (EXIF, modified date, format, resolution, size)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun ExpressiveImageDetailsCard(file: FileItem, exifData: ExifData) {
    val colors = ExpressiveTheme.colors
    val modifiedFormatted = SimpleDateFormat("MMM dd, yyyy · h:mm a", Locale.getDefault())
        .format(Date(file.lastModified))
    val ext = file.name.substringAfterLast('.', "—").uppercase()
    val sizeFormatted = formatSize(File(file.path).length())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.card)
            .border(1.dp, colors.line.copy(alpha = colors.lineAlpha), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Row 1: Modified + Format
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExpressiveDetailItem(
                label = "Modified",
                value = modifiedFormatted,
                icon = Icons.Outlined.CalendarToday,
                modifier = Modifier.weight(1f)
            )
            ExpressiveDetailItem(
                label = "Format",
                value = ext,
                icon = Icons.Outlined.Image,
                modifier = Modifier.weight(0.6f)
            )
        }

        // Row 2: Resolution + File size
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExpressiveDetailItem(
                label = "Resolution",
                value = exifData.dimensions ?: "—",
                icon = Icons.Outlined.AspectRatio,
                modifier = Modifier.weight(1f)
            )
            ExpressiveDetailItem(
                label = "Size",
                value = sizeFormatted,
                icon = Icons.Outlined.Storage,
                modifier = Modifier.weight(0.6f)
            )
        }

        // Row 3: Camera source (if available)
        val source = exifData.source
        if (source != null) {
            ExpressiveDetailItem(
                label = "Camera",
                value = source,
                icon = Icons.Outlined.CameraAlt,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // File path
        Text(
            text = file.path,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = LocalAppFont.current,
            color = colors.muted.copy(alpha = 0.7f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ExpressiveDetailItem(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val colors = ExpressiveTheme.colors

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.muted,
            modifier = Modifier.size(16.dp)
        )
        Column {
            Text(
                text = label.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = LocalAppFont.current,
                color = colors.muted,
                letterSpacing = 0.6.sp
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = LocalAppFont.current,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════
//  Retained helpers — LedgerChipButton / LedgerToolbarAction kept for
//  backward-compat if referenced elsewhere, but no longer used internally
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun LedgerChipButton(
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    cornerRoundness: Float,
    onClick: () -> Unit
) {
    val haptics = LocalHaptics.current
    Surface(
        modifier = Modifier.size(44.dp),
        shape = getDynamicCornerShape(12f, cornerRoundness),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        onClick = { haptics.tap(); onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun LedgerToolbarAction(
    icon: ImageVector,
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


// ═══════════════════════════════════════════════════════════════════════════
//  Data helpers (unchanged)
// ═══════════════════════════════════════════════════════════════════════════

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
