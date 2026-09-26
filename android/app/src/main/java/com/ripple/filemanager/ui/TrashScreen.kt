package com.ripple.filemanager.ui

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import com.ripple.filemanager.ui.expressive.ExpressiveMotion
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ripple.filemanager.*
import com.ripple.filemanager.R
import com.ripple.filemanager.ui.expressive.*
import com.ripple.filemanager.ui.theme.LocalAppFont
import kotlinx.collections.immutable.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    state: AppState,
    onAction: (AppAction) -> Unit,
    onClose: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    var showSettings by remember { mutableStateOf(false) }
    var fileToDeleteForever by remember { mutableStateOf<FileItem?>(null) }
    var showEmptyBinConfirm by remember { mutableStateOf(false) }
    val colors = ExpressiveTheme.colors

    BackHandler(enabled = true) {
        onClose()
    }

    LaunchedEffect(Unit) {
        if (state.trashFiles.isEmpty()) {
            onAction(AppAction.RefreshTrash)
        }
    }

    val count = state.trashFiles.size
    val countText = pluralStringResource(R.plurals.items_count, count, count)
    val retentionSubtitle = if (state.isRecycleBinEnabled) {
        "$countText · ${state.recycleBinRetentionValue} ${state.recycleBinRetentionUnit.lowercase()}"
    } else {
        "$countText (disabled)"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        // PageHeader "Bin" + subtitle + right actions
        PageHeader(
            title = "Bin",
            subtitle = retentionSubtitle,
            onBack = onClose,
            actions = {
                // List / Grid toggle
                CookieIconButton(
                    onClick = {
                        haptics.tap()
                        onAction(AppAction.ToggleViewMode)
                    },
                    icon = if (state.isListMode) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                    bgColor = colors.card,
                    iconTint = colors.text,
                    description = "Toggle view",
                    size = 38.dp,
                    lobes = 8
                )

                // Settings gear
                CookieIconButton(
                    onClick = {
                        haptics.tap()
                        showSettings = true
                    },
                    icon = Icons.Default.Settings,
                    bgColor = colors.card,
                    iconTint = colors.text,
                    description = "Bin settings",
                    size = 38.dp,
                    lobes = 8
                )
            }
        )

        // Chips: Restore all, Empty bin
        if (state.trashFiles.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExpressiveChip(
                    label = "Restore all",
                    selected = false,
                    icon = Icons.Outlined.Restore,
                    onClick = {
                        val allFiles = state.trashFiles.mapNotNull { it.encodedTrashName }
                        onAction(AppAction.RestoreTrashFiles(allFiles))
                    }
                )
                ExpressiveChip(
                    label = "Empty bin",
                    selected = false,
                    icon = Icons.Outlined.Delete,
                    onClick = { showEmptyBinConfirm = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Content
        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            if (state.trashIsLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.accent)
                }
            } else if (state.trashFiles.isEmpty()) {
                SectionContainer(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                    InnerCard {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.bin_is_empty),
                                fontSize = 15.sp,
                                fontFamily = LocalAppFont.current,
                                color = colors.muted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                SectionContainer(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                    val reduced = ExpressiveMotion.isReducedMotion()
                    AnimatedContent(
                        targetState = state.isListMode,
                        transitionSpec = {
                            if (reduced) {
                                fadeIn(androidx.compose.animation.core.snap()) togetherWith fadeOut(androidx.compose.animation.core.snap())
                            } else {
                                fadeIn(ExpressiveMotion.FastTween) togetherWith fadeOut(ExpressiveMotion.FadeTween)
                            }
                        },
                        label = "trash_list_grid_toggle"
                    ) { isListMode ->
                        if (isListMode) {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                itemsIndexed(
                                    items = state.trashFiles,
                                    key = { index, file -> file.encodedTrashName?.let { "${it}_$index" } ?: "${file.id}_$index" }
                                ) { _, file ->
                                    TrashListRow(
                                        file = file,
                                        modifier = Modifier.animateItem(fadeInSpec = null),
                                        onRestore = {
                                            file.encodedTrashName?.let {
                                                onAction(AppAction.RestoreTrashFiles(listOf(it)))
                                            }
                                        },
                                        onDeleteForever = {
                                            fileToDeleteForever = file
                                        }
                                    )
                                }
                            }
                        } else {
                            // Grid Mode
                            val columns = state.gridColumns.coerceIn(2, 4)
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(columns),
                                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                itemsIndexed(
                                    items = state.trashFiles,
                                    key = { index, file -> file.encodedTrashName?.let { "${it}_$index" } ?: "${file.id}_$index" }
                                ) { _, file ->
                                    TrashGridCard(
                                        file = file,
                                        modifier = Modifier.animateItem(fadeInSpec = null),
                                        onRestore = {
                                            file.encodedTrashName?.let {
                                                onAction(AppAction.RestoreTrashFiles(listOf(it)))
                                            }
                                        },
                                        onDeleteForever = {
                                            fileToDeleteForever = file
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
}

    // Delete Forever Sheet
    if (fileToDeleteForever != null) {
        val file = fileToDeleteForever!!
        ExpressiveConfirmationSheet(
            title = "Delete permanently?",
            subtitle = "\"${file.name}\" will be permanently deleted.",
            confirmLabel = "Delete",
            cancelLabel = "Cancel",
            onConfirm = {
                haptics.delete()
                val encoded = file.encodedTrashName
                fileToDeleteForever = null
                if (encoded != null) {
                    onAction(AppAction.PermanentlyDeleteTrashFiles(listOf(encoded)))
                }
            },
            onDismissRequest = {
                fileToDeleteForever = null
            }
        )
    }

    // Empty Bin Sheet
    if (showEmptyBinConfirm) {
        ExpressiveConfirmationSheet(
            title = "Empty bin?",
            subtitle = "All items in the bin will be permanently deleted.",
            confirmLabel = "Empty bin",
            cancelLabel = "Cancel",
            onConfirm = {
                haptics.delete()
                showEmptyBinConfirm = false
                val allFiles = state.trashFiles.mapNotNull { it.encodedTrashName }
                onAction(AppAction.PermanentlyDeleteTrashFiles(allFiles))
            },
            onDismissRequest = {
                showEmptyBinConfirm = false
            }
        )
    }

    // Bin Settings Dialog
    if (showSettings) {
        var isEnabled by remember { mutableStateOf(state.isRecycleBinEnabled) }
        var retentionValue by remember { mutableStateOf(state.recycleBinRetentionValue.toString()) }
        var retentionUnit by remember { mutableStateOf(state.recycleBinRetentionUnit) }

        AlertDialog(
            onDismissRequest = { showSettings = false },
            containerColor = colors.container,
            shape = RoundedCornerShape(26.dp),
            title = { Text("Bin settings", color = colors.text) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable recycle bin", color = colors.text)
                        ExpressiveSwitch(checked = isEnabled, onCheckedChange = { isEnabled = it })
                    }
                    if (isEnabled) {
                        OutlinedTextField(
                            value = retentionValue,
                            onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) retentionValue = it },
                            label = { Text("Days to keep files") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSettings = false
                        val valueInt = retentionValue.toIntOrNull() ?: 7
                        onAction(AppAction.SetRecycleBinSettings(isEnabled, valueInt, retentionUnit))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.ink)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettings = false }) {
                    Text("Cancel", color = colors.muted)
                }
            }
        )
    }
}

@Composable
private fun TrashListRow(
    file: FileItem,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ExpressiveTheme.colors
    val roundness = LocalCornerRoundness.current
    val rowShape = RoundedCornerShape((26f * (roundness * 2).coerceIn(0f, 2f)).dp)
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(colors.card)
            .clickable { menuExpanded = true }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CookieIcon(
            icon = Icons.Outlined.Delete,
            bgColor = colors.insetCard,
            iconTint = colors.text,
            size = 46.dp,
            lobes = 8
        )
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
            val dateStr = DateFormat.format("MMM d", file.lastModified)
            Text(
                text = "${file.size} · Deleted $dateStr",
                fontFamily = LocalAppFont.current,
                fontSize = 12.sp,
                color = colors.muted
            )
        }

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = colors.muted
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(colors.card)
            ) {
                DropdownMenuItem(
                    text = { Text("Restore", color = colors.text) },
                    onClick = {
                        menuExpanded = false
                        onRestore()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete forever", color = Color(0xFFFF6B6B)) },
                    onClick = {
                        menuExpanded = false
                        onDeleteForever()
                    }
                )
            }
        }
    }
}

@Composable
private fun TrashGridCard(
    file: FileItem,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ExpressiveTheme.colors
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        modifier = modifier
            .fillMaxWidth()
            .clickable { menuExpanded = true }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // 4:3 thumbnail box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.insetCard)
            ) {
                // Thumbnail
                val isMedia = file.name.endsWith(".jpg", true) || file.name.endsWith(".png", true) || file.name.endsWith(".mp4", true)
                if (isMedia && file.path.isNotEmpty()) {
                    AsyncImage(
                        model = File(file.path),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CookieIcon(
                            icon = Icons.AutoMirrored.Outlined.InsertDriveFile,
                            bgColor = colors.card,
                            iconTint = colors.muted,
                            size = 44.dp,
                            lobes = 8
                        )
                    }
                }

                // 30dp translucent 3-dot button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable { menuExpanded = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(colors.card)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Restore", color = colors.text) },
                            onClick = { onRestore(); menuExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete forever", color = colors.accent) },
                            onClick = { onDeleteForever(); menuExpanded = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2-line name
            Text(
                text = file.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Size pill (sand) + deleted date
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.sand)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = file.size,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.ink
                    )
                }

                Text(
                    text = DateFormat.format("MMM d", file.lastModified).toString(),
                    fontSize = 10.sp,
                    color = colors.muted
                )
            }
        }
    }
}
