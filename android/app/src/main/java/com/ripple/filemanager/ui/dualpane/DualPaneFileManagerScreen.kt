package com.ripple.filemanager.ui.dualpane

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripple.filemanager.AppState
import com.ripple.filemanager.FileItem
import com.ripple.filemanager.PaneSide
import com.ripple.filemanager.data.core.ConflictResolution
import com.ripple.filemanager.data.core.TransferMode
import com.ripple.filemanager.ui.getDynamicCornerShape
import com.ripple.filemanager.ui.theme.JetBrainsMonoFamily
import com.ripple.filemanager.ui.theme.ManropeFontFamily
import com.ripple.filemanager.ui.theme.SkylineColors
import kotlin.math.roundToInt

private data class PendingDrop(val file: FileItem, val sourceSide: PaneSide, val destSide: PaneSide)

private fun suggestNonConflictingName(originalName: String, existingNames: Set<String>): String {
    val dotIndex = originalName.lastIndexOf('.')
    val base = if (dotIndex > 0) originalName.substring(0, dotIndex) else originalName
    val ext = if (dotIndex > 0) originalName.substring(dotIndex) else ""
    var counter = 1
    var candidate: String
    do {
        candidate = "$base ($counter)$ext"
        counter++
    } while (existingNames.contains(candidate))
    return candidate
}

private fun proceedOrConflict(
    drop: PendingDrop,
    mode: TransferMode,
    existingNames: Set<String>,
    onDropFile: (PaneSide, FileItem, PaneSide, TransferMode, ConflictResolution?) -> Unit,
    showConflict: (Pair<PendingDrop, TransferMode>) -> Unit
) {
    if (existingNames.contains(drop.file.name)) {
        showConflict(drop to mode)
    } else {
        onDropFile(drop.sourceSide, drop.file, drop.destSide, mode, null)
    }
}

@Composable
fun DualPaneFileManagerScreen(
    state: AppState,
    mode: com.ripple.filemanager.DualPaneMode,
    onNavigate: (PaneSide, path: String, name: String?) -> Unit,
    onBack: (PaneSide) -> Unit,
    onSetActivePane: (PaneSide) -> Unit,
    onOpenFile: (PaneSide, FileItem) -> Unit,
    onDropFile: (sourceSide: PaneSide, file: FileItem, destinationSide: PaneSide, mode: TransferMode, conflictResolution: ConflictResolution?) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragFile by remember { mutableStateOf<FileItem?>(null) }
    var dragSourceSide by remember { mutableStateOf<PaneSide?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    var leftPaneBounds by remember { mutableStateOf<Rect?>(null) }
    var rightPaneBounds by remember { mutableStateOf<Rect?>(null) }

    var pendingDrop by remember { mutableStateOf<PendingDrop?>(null) }
    var pendingConflict by remember { mutableStateOf<Pair<PendingDrop, TransferMode>?>(null) }

    // How much of the total size the first pane gets (0.2–0.8, so neither
    // pane can be dragged down to nothing). In SIDE_BY_SIDE this is a
    // fraction of width (first pane = left); in STACKED it's a fraction of
    // height (first pane = top). The second pane always gets the rest.
    // Shared across both modes on purpose — switching modes keeps your ratio.
    var splitFraction by remember { mutableStateOf(0.5f) }
    var containerSizePx by remember { mutableStateOf(0f) }

    fun endDrag() {
        val file = dragFile
        val sourceSide = dragSourceSide
        if (file != null && sourceSide != null) {
            val destBounds = if (sourceSide == PaneSide.LEFT) rightPaneBounds else leftPaneBounds
            val destSide = if (sourceSide == PaneSide.LEFT) PaneSide.RIGHT else PaneSide.LEFT
            if (destBounds != null && destBounds.contains(dragPosition)) {
                pendingDrop = PendingDrop(file, sourceSide, destSide)
            }
        }
        dragFile = null
        dragSourceSide = null
    }

    fun destinationFileNames(destSide: PaneSide): Set<String> {
        return if (destSide == PaneSide.LEFT) state.files.map { it.name }.toSet()
        else state.secondPaneState.files.map { it.name }.toSet()
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (mode == com.ripple.filemanager.DualPaneMode.STACKED) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SkylineColors.Background)
                    .onGloballyPositioned { containerSizePx = it.size.height.toFloat() }
            ) {
                FilePane(
                    title = state.currentFolderName ?: "Home",
                    files = state.files,
                    isLoading = state.isLoading,
                    isActive = state.activePaneSide == PaneSide.LEFT,
                    canGoBack = state.driveFolderStack.isNotEmpty() || state.location != "home",
                    isDropTarget = dragSourceSide == PaneSide.RIGHT && dragFile != null &&
                        leftPaneBounds?.contains(dragPosition) == true,
                    cornerRoundness = state.cornerRoundness,
                    onFileClick = { file ->
                        onSetActivePane(PaneSide.LEFT)
                        if (file.type == "folder") onNavigate(PaneSide.LEFT, file.path, file.name)
                        else onOpenFile(PaneSide.LEFT, file)
                    },
                    onBackClick = { onBack(PaneSide.LEFT) },
                    onPaneClick = { onSetActivePane(PaneSide.LEFT) },
                    onDragArm = { file, rootOffset ->
                        dragFile = file
                        dragSourceSide = PaneSide.LEFT
                        dragPosition = rootOffset
                    },
                    onDragMove = { rootOffset -> dragPosition = rootOffset },
                    onDragEnd = { endDrag() },
                    modifier = Modifier
                        .weight(splitFraction)
                        .onGloballyPositioned { leftPaneBounds = it.boundsInRoot() }
                )

                // Horizontal divider (drag up/down to resize top vs bottom).
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                if (containerSizePx > 0f) {
                                    val deltaFraction = dragAmount.y / containerSizePx
                                    splitFraction = (splitFraction + deltaFraction).coerceIn(0.2f, 0.8f)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth().height(1.dp),
                        color = SkylineColors.Border
                    )
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .width(36.dp)
                            .background(SkylineColors.Border, RoundedCornerShape(2.dp))
                    )
                }

                FilePane(
                    title = state.secondPaneState.currentFolderName ?: "Home",
                    files = state.secondPaneState.files,
                    isLoading = state.secondPaneState.isLoading,
                    isActive = state.activePaneSide == PaneSide.RIGHT,
                    canGoBack = state.secondPaneState.folderStack.isNotEmpty() || state.secondPaneState.location != "home",
                    isDropTarget = dragSourceSide == PaneSide.LEFT && dragFile != null &&
                        rightPaneBounds?.contains(dragPosition) == true,
                    cornerRoundness = state.cornerRoundness,
                    onFileClick = { file ->
                        onSetActivePane(PaneSide.RIGHT)
                        if (file.type == "folder") onNavigate(PaneSide.RIGHT, file.path, file.name)
                        else onOpenFile(PaneSide.RIGHT, file)
                    },
                    onBackClick = { onBack(PaneSide.RIGHT) },
                    onPaneClick = { onSetActivePane(PaneSide.RIGHT) },
                    onDragArm = { file, rootOffset ->
                        dragFile = file
                        dragSourceSide = PaneSide.RIGHT
                        dragPosition = rootOffset
                    },
                    onDragMove = { rootOffset -> dragPosition = rootOffset },
                    onDragEnd = { endDrag() },
                    modifier = Modifier
                        .weight(1f - splitFraction)
                        .onGloballyPositioned { rightPaneBounds = it.boundsInRoot() }
                )
            }
        } else {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(SkylineColors.Background)
                .onGloballyPositioned { containerSizePx = it.size.width.toFloat() }
        ) {
            FilePane(
                title = state.currentFolderName ?: "Home",
                files = state.files,
                isLoading = state.isLoading,
                isActive = state.activePaneSide == PaneSide.LEFT,
                canGoBack = state.driveFolderStack.isNotEmpty() || state.location != "home",
                isDropTarget = dragSourceSide == PaneSide.RIGHT && dragFile != null &&
                    leftPaneBounds?.contains(dragPosition) == true,
                cornerRoundness = state.cornerRoundness,
                onFileClick = { file ->
                    onSetActivePane(PaneSide.LEFT)
                    if (file.type == "folder") onNavigate(PaneSide.LEFT, file.path, file.name)
                    else onOpenFile(PaneSide.LEFT, file)
                },
                onBackClick = { onBack(PaneSide.LEFT) },
                onPaneClick = { onSetActivePane(PaneSide.LEFT) },
                onDragArm = { file, rootOffset ->
                    dragFile = file
                    dragSourceSide = PaneSide.LEFT
                    dragPosition = rootOffset
                },
                onDragMove = { rootOffset -> dragPosition = rootOffset },
                onDragEnd = { endDrag() },
                modifier = Modifier
                    .weight(splitFraction)
                    .onGloballyPositioned { leftPaneBounds = it.boundsInRoot() }
            )

            // Draggable divider: a wider invisible touch target (24dp) around
            // a thin visible line + grip pill, so it's easy to grab without
            // needing pixel-perfect precision.
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(24.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            if (containerSizePx > 0f) {
                                val deltaFraction = dragAmount.x / containerSizePx
                                splitFraction = (splitFraction + deltaFraction).coerceIn(0.2f, 0.8f)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                VerticalDivider(
                    modifier = Modifier.fillMaxHeight().width(1.dp),
                    color = SkylineColors.Border
                )
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(36.dp)
                        .background(SkylineColors.Border, RoundedCornerShape(2.dp))
                )
            }

            FilePane(
                title = state.secondPaneState.currentFolderName ?: "Home",
                files = state.secondPaneState.files,
                isLoading = state.secondPaneState.isLoading,
                isActive = state.activePaneSide == PaneSide.RIGHT,
                canGoBack = state.secondPaneState.folderStack.isNotEmpty() || state.secondPaneState.location != "home",
                isDropTarget = dragSourceSide == PaneSide.LEFT && dragFile != null &&
                    rightPaneBounds?.contains(dragPosition) == true,
                cornerRoundness = state.cornerRoundness,
                onFileClick = { file ->
                    onSetActivePane(PaneSide.RIGHT)
                    if (file.type == "folder") onNavigate(PaneSide.RIGHT, file.path, file.name)
                    else onOpenFile(PaneSide.RIGHT, file)
                },
                onBackClick = { onBack(PaneSide.RIGHT) },
                onPaneClick = { onSetActivePane(PaneSide.RIGHT) },
                onDragArm = { file, rootOffset ->
                    dragFile = file
                    dragSourceSide = PaneSide.RIGHT
                    dragPosition = rootOffset
                },
                onDragMove = { rootOffset -> dragPosition = rootOffset },
                onDragEnd = { endDrag() },
                modifier = Modifier
                    .weight(1f - splitFraction)
                    .onGloballyPositioned { rightPaneBounds = it.boundsInRoot() }
            )
        }
        }

        // Floating preview that follows the finger while dragging.
        dragFile?.let { file ->
            val shape = getDynamicCornerShape(10f, state.cornerRoundness)
            val toneColor = toneColorFor(file)
            val iconVector = fileIconFor(file.type)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .offset { IntOffset(dragPosition.x.roundToInt() - 60, dragPosition.y.roundToInt() - 24) }
                    .background(MaterialTheme.colorScheme.surface, shape)
                    .border(1.dp, toneColor, shape)
                    .clip(shape)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = toneColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = file.name,
                    color = SkylineColors.TextPrimary,
                    maxLines = 1,
                    fontFamily = ManropeFontFamily,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    // --- Step 1: Copy or Move? (uses the app's normal Material theming) ---
    pendingDrop?.let { drop ->
        AlertDialog(
            onDismissRequest = { pendingDrop = null },
            title = { Text("Move \"${drop.file.name}\"?") },
            text = { Text("Copy keeps the original in place. Move removes it from the source.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDrop = null
                    proceedOrConflict(drop, TransferMode.MOVE, destinationFileNames(drop.destSide), onDropFile) { pendingConflict = it }
                }) { Text("Move") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { pendingDrop = null }) { Text("Cancel") }
                    TextButton(onClick = {
                        pendingDrop = null
                        proceedOrConflict(drop, TransferMode.COPY, destinationFileNames(drop.destSide), onDropFile) { pendingConflict = it }
                    }) { Text("Copy") }
                }
            }
        )
    }

    // --- Step 2 (only on a name conflict): Skip / Overwrite / Rename ---
    pendingConflict?.let { (drop, mode) ->
        val existingNames = destinationFileNames(drop.destSide)
        val suggestedName = remember(drop) { suggestNonConflictingName(drop.file.name, existingNames) }
        AlertDialog(
            onDismissRequest = { pendingConflict = null },
            title = { Text("\"${drop.file.name}\" already exists") },
            text = { Text("The destination already has a file with this name. What would you like to do?") },
            confirmButton = {
                TextButton(onClick = {
                    pendingConflict = null
                    onDropFile(drop.sourceSide, drop.file, drop.destSide, mode, ConflictResolution.Overwrite)
                }) { Text("Overwrite") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { pendingConflict = null }) { Text("Skip") }
                    TextButton(onClick = {
                        pendingConflict = null
                        onDropFile(drop.sourceSide, drop.file, drop.destSide, mode, ConflictResolution.Rename(suggestedName))
                    }) { Text("Rename to \"$suggestedName\"") }
                }
            }
        )
    }
}

@Composable
private fun FilePane(
    title: String,
    files: List<FileItem>,
    isLoading: Boolean,
    isActive: Boolean,
    canGoBack: Boolean,
    isDropTarget: Boolean,
    cornerRoundness: Float,
    onFileClick: (FileItem) -> Unit,
    onBackClick: () -> Unit,
    onPaneClick: () -> Unit,
    onDragArm: (FileItem, Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = getDynamicCornerShape(12f, cornerRoundness)

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { onPaneClick() }
            .then(
                when {
                    isDropTarget -> Modifier.border(3.dp, SkylineColors.Amber)
                    isActive -> Modifier.border(2.dp, SkylineColors.Amber)
                    else -> Modifier
                }
            )
    ) {
        // Header — mirrors the hard-square icon-button style used in SkylineTopBar.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isActive) SkylineColors.Amber.copy(alpha = 0.08f) else Color.Transparent)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (canGoBack) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, SkylineColors.Border, shape)
                        .background(MaterialTheme.colorScheme.surface, shape)
                        .clip(shape)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                            onClick = onBackClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SkylineColors.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Spacer(Modifier.width(36.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                fontFamily = ManropeFontFamily,
                style = MaterialTheme.typography.titleMedium,
                color = SkylineColors.TextPrimary,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(color = SkylineColors.Border)

        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = SkylineColors.Amber,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (files.isEmpty()) {
                Text(
                    text = "Empty folder",
                    fontFamily = JetBrainsMonoFamily,
                    color = SkylineColors.TextDim,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    items(files, key = { it.id }) { file ->
                        DraggableFileRow(
                            file = file,
                            cornerRoundness = cornerRoundness,
                            onOpen = { onFileClick(file) },
                            onDragArm = { offset -> onDragArm(file, offset) },
                            onDragMove = onDragMove,
                            onDragEnd = onDragEnd
                        )
                    }
                }
            }
        }
    }
}

/** Same icon-per-type logic used for the floating drag preview, factored
 * out so both places stay in sync. */
private fun fileIconFor(type: String): androidx.compose.ui.graphics.vector.ImageVector = when (type) {
    "folder" -> Icons.Outlined.Folder
    "image" -> Icons.Outlined.Image
    "video" -> Icons.Outlined.OndemandVideo
    "audio" -> Icons.Outlined.AudioFile
    else -> Icons.Outlined.InsertDriveFile
}

private fun toneColorFor(file: FileItem): Color {
    return if (file.type == "folder") {
        com.ripple.filemanager.ui.getFolderAccent(file.name)
    } else {
        com.ripple.filemanager.ui.theme.fileTypeTone(file.type)
    }
}

/**
 * A row built specifically for dual-pane's narrow columns (~150-190dp).
 * SkylineFolderListRow is designed for full single-pane width — its name,
 * item-count badge, and date all sit inline, which wraps into a cramped
 * mess at half-screen width. This keeps the same colors/fonts/icon logic
 * but forces everything onto one line each, trading some detail (no
 * separate item-count badge, no date shown) for actual readability at
 * this width.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactFileRow(
    file: FileItem,
    cornerRoundness: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = getDynamicCornerShape(8f, cornerRoundness)
    val tone = toneColorFor(file)
    val metaText = if (file.type == "folder") {
        // FileItem's `size` field is blank for folders (see FileItemMapper),
        // so fall back to a generic label rather than showing nothing.
        file.size.ifBlank { "Folder" }
    } else {
        file.size
    }

    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, SkylineColors.Border, shape)
            .clip(shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(tone.copy(alpha = 0.18f), getDynamicCornerShape(6f, cornerRoundness)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = fileIconFor(file.type),
                contentDescription = null,
                tint = tone,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = SkylineColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (metaText.isNotBlank()) {
                Text(
                    text = metaText,
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 9.sp,
                    color = SkylineColors.TextDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Adds drag-and-drop on top of CompactFileRow. Since this row is fully
 * custom (not shared with any other screen), long-press can trigger the
 * drag directly — no need for the "peek without consuming" workaround
 * that was needed when wrapping SkylineFolderListRow's own gesture logic.
 */
@Composable
private fun DraggableFileRow(
    file: FileItem,
    cornerRoundness: Float,
    onOpen: () -> Unit,
    onDragArm: (Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    var rowRootPosition by remember { mutableStateOf(Offset.Zero) }
    var lastPointerOffset by remember { mutableStateOf(Offset.Zero) }
    var isArmed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .onGloballyPositioned { rowRootPosition = it.positionInRoot() }
            .pointerInput(file.id) {
                awaitEachGesture {
                    isArmed = false
                    val down = awaitFirstDown(pass = PointerEventPass.Initial)
                    lastPointerOffset = down.position
                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            if (isArmed) onDragEnd()
                            isArmed = false
                            break
                        }
                        lastPointerOffset = change.position
                        if (isArmed) {
                            onDragMove(rowRootPosition + lastPointerOffset)
                        }
                    }
                }
            }
    ) {
        CompactFileRow(
            file = file,
            cornerRoundness = cornerRoundness,
            onClick = onOpen,
            onLongClick = {
                isArmed = true
                onDragArm(rowRootPosition + lastPointerOffset)
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
