package com.ripple.filemanager.ui

import android.os.Environment
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ripple.filemanager.FileItem
import com.ripple.filemanager.FileRepository
import com.ripple.filemanager.R
import com.ripple.filemanager.haptics.LocalHaptics
import com.ripple.filemanager.ui.expressive.*
import com.ripple.filemanager.ui.theme.LocalAppFont
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun FilePickerDialog(
    title: String = "SELECT FILES",
    confirmText: String = "Select",
    initialPath: String = Environment.getExternalStorageDirectory().path,
    onDismiss: () -> Unit,
    onFilesSelected: (List<FileItem>) -> Unit,
    onOpenSystemPicker: (() -> Unit)? = null,
    cornerRoundness: Float = 0.5f
) {
    val context = LocalContext.current
    val colors = ExpressiveTheme.colors
    val haptics = LocalHaptics.current

    var currentPath by remember { mutableStateOf(initialPath) }
    var folders by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var files by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Selected files: key is path, value is FileItem
    val selectedFiles = remember { mutableStateMapOf<String, FileItem>() }
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { FileRepository(context) }

    val standardChips = remember {
        listOf(
            "Storage" to Environment.getExternalStorageDirectory().path,
            "Downloads" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path,
            "Documents" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).path,
            "Pictures" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).path,
            "DCIM" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).path,
            "Music" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).path
        ).filter { File(it.second).exists() }
    }

    fun loadPath(path: String) {
        isLoading = true
        coroutineScope.launch {
            try {
                val allItems = repository.getFiles(path)
                folders = allItems.filter { it.type == "folder" || it.kind == "folder" }.sortedBy { it.name.lowercase() }
                files = allItems.filter { it.type != "folder" && it.kind != "folder" }.sortedBy { it.name.lowercase() }
                currentPath = path
            } catch (e: Exception) {
                folders = emptyList()
                files = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadPath(currentPath)
    }

    val dialogRadius = (28f * (cornerRoundness * 2).coerceIn(0.6f, 1.8f)).dp
    val dialogShape = RoundedCornerShape(dialogRadius)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = dialogShape,
            color = colors.container,
            border = BorderStroke(1.dp, colors.line.copy(alpha = colors.lineAlpha)),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 14.dp)
            ) {
                // ══════════════════════════════════════════════════════════════════════
                // 1. TOP HEADER
                // ══════════════════════════════════════════════════════════════════════
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        val isRoot = currentPath == Environment.getExternalStorageDirectory().path
                        if (!isRoot) {
                            CookieIconButton(
                                onClick = {
                                    val parent = File(currentPath).parent
                                    if (parent != null) {
                                        haptics.tap()
                                        loadPath(parent)
                                    }
                                },
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                bgColor = colors.card,
                                iconTint = colors.text,
                                size = 38.dp,
                                lobes = 8,
                                description = "Back"
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        Column {
                            Text(
                                text = title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = LocalAppFont.current,
                                color = colors.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (selectedFiles.isEmpty()) "Tap files to select" else "${selectedFiles.size} file${if (selectedFiles.size == 1) "" else "s"} selected",
                                fontSize = 12.sp,
                                fontFamily = LocalAppFont.current,
                                color = if (selectedFiles.isNotEmpty()) colors.accent else colors.muted
                            )
                        }
                    }

                    CookieIconButton(
                        onClick = {
                            haptics.tap()
                            onDismiss()
                        },
                        icon = Icons.Default.Close,
                        bgColor = colors.card,
                        iconTint = colors.muted,
                        size = 38.dp,
                        lobes = 8,
                        description = "Close"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ══════════════════════════════════════════════════════════════════════
                // 2. SHORTCUT DIRECTORY CHIPS
                // ══════════════════════════════════════════════════════════════════════
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    standardChips.forEach { (name, path) ->
                        val isSelected = currentPath.equals(path, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) colors.accent else colors.card,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) colors.accent else colors.line.copy(alpha = colors.lineAlpha * 0.5f)
                            ),
                            modifier = Modifier
                                .pressScale(0.95f)
                                .clickable {
                                    if (currentPath != path) {
                                        haptics.tap()
                                        loadPath(path)
                                    }
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Icon(
                                    imageVector = when (name) {
                                        "Downloads" -> Icons.Outlined.FileDownload
                                        "Pictures", "DCIM" -> Icons.Outlined.Image
                                        "Documents" -> Icons.Outlined.Description
                                        "Music" -> Icons.Outlined.MusicNote
                                        else -> Icons.Outlined.Folder
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) colors.ink else colors.accent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) colors.ink else colors.text,
                                    fontFamily = LocalAppFont.current
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ══════════════════════════════════════════════════════════════════════
                // 3. CURRENT BREADCRUMB & MULTI-SELECT BAR
                // ══════════════════════════════════════════════════════════════════════
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.insetCard)
                        .border(1.dp, colors.line.copy(alpha = colors.lineAlpha * 0.4f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val displayPath = currentPath
                            .replace(Environment.getExternalStorageDirectory().path, "Internal Storage")
                        Text(
                            text = displayPath,
                            fontSize = 11.sp,
                            fontFamily = LocalAppFont.current,
                            color = colors.muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (files.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Select All",
                                fontSize = 11.sp,
                                color = colors.accent,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = LocalAppFont.current,
                                modifier = Modifier
                                    .clickable {
                                        haptics.tap()
                                        files.forEach { file ->
                                            selectedFiles[file.path] = file
                                        }
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                            if (selectedFiles.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Clear",
                                    fontSize = 11.sp,
                                    color = colors.muted,
                                    fontFamily = LocalAppFont.current,
                                    modifier = Modifier
                                        .clickable {
                                            haptics.tap()
                                            selectedFiles.clear()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ══════════════════════════════════════════════════════════════════════
                // 4. FILE & FOLDER LIST
                // ══════════════════════════════════════════════════════════════════════
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = colors.accent)
                        }
                    } else if (folders.isEmpty() && files.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CookieIcon(
                                icon = Icons.Outlined.FolderOpen,
                                bgColor = colors.card,
                                iconTint = colors.muted,
                                size = 56.dp,
                                lobes = 8
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Folder is empty",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = LocalAppFont.current,
                                color = colors.text,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "No files or subfolders found here",
                                fontSize = 12.sp,
                                fontFamily = LocalAppFont.current,
                                color = colors.muted,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            // Folders first
                            items(folders, key = { "folder_${it.path}" }) { folder ->
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = colors.card),
                                    border = BorderStroke(1.dp, colors.line.copy(alpha = colors.lineAlpha * 0.4f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pressScale(0.97f)
                                        .clickable {
                                            haptics.tap()
                                            loadPath(folder.path)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CookieIcon(
                                            icon = Icons.Default.Folder,
                                            bgColor = ExpressiveTokens.PastelPeach,
                                            iconTint = ExpressiveTokens.OnPastel,
                                            size = 40.dp,
                                            lobes = 8
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = folder.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                fontFamily = LocalAppFont.current,
                                                color = colors.text,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Folder",
                                                fontSize = 11.sp,
                                                fontFamily = LocalAppFont.current,
                                                color = colors.muted
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Outlined.ChevronRight,
                                            contentDescription = null,
                                            tint = colors.muted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Files next
                            items(files, key = { "file_${it.path}" }) { file ->
                                val isSelected = selectedFiles.containsKey(file.path)
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) colors.accent.copy(alpha = 0.12f) else colors.card
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) colors.accent else colors.line.copy(alpha = colors.lineAlpha * 0.4f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pressScale(0.97f)
                                        .clickable {
                                            haptics.tap()
                                            if (isSelected) {
                                                selectedFiles.remove(file.path)
                                            } else {
                                                selectedFiles[file.path] = file
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ExpressiveFileThumbnail(
                                            file = file,
                                            isSelected = isSelected,
                                            size = 42.dp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = file.name,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                                fontFamily = LocalAppFont.current,
                                                color = colors.text,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${file.size} • ${file.changed}",
                                                fontSize = 11.sp,
                                                fontFamily = LocalAppFont.current,
                                                color = colors.muted
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        // Circular selection indicator
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) colors.accent else Color.Transparent)
                                                .border(
                                                    width = 1.5.dp,
                                                    color = if (isSelected) colors.accent else colors.muted.copy(alpha = 0.4f),
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = colors.ink,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ══════════════════════════════════════════════════════════════════════
                // 5. BOTTOM ACTION FOOTER
                // ══════════════════════════════════════════════════════════════════════
                HorizontalDivider(color = colors.line.copy(alpha = colors.lineAlpha * 0.5f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onOpenSystemPicker != null) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = colors.card,
                            border = BorderStroke(1.dp, colors.line.copy(alpha = colors.lineAlpha * 0.5f)),
                            modifier = Modifier
                                .pressScale(0.95f)
                                .clickable {
                                    haptics.tap()
                                    onDismiss()
                                    onOpenSystemPicker()
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.FileOpen,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "System Picker",
                                    color = colors.text,
                                    fontSize = 12.sp,
                                    fontFamily = LocalAppFont.current,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = {
                                haptics.tap()
                                onDismiss()
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.cancel),
                                color = colors.muted,
                                fontFamily = LocalAppFont.current,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = {
                                haptics.tap()
                                onFilesSelected(selectedFiles.values.toList())
                            },
                            enabled = selectedFiles.isNotEmpty(),
                            shape = RoundedCornerShape(22.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accent,
                                contentColor = colors.ink,
                                disabledContainerColor = colors.insetCard,
                                disabledContentColor = colors.muted
                            ),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = if (selectedFiles.isEmpty()) confirmText else "$confirmText (${selectedFiles.size})",
                                fontFamily = LocalAppFont.current,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
