package com.ripple.filemanager.ui

import android.os.Environment
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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripple.filemanager.FileItem
import com.ripple.filemanager.FileRepository
import com.ripple.filemanager.R
import com.ripple.filemanager.ui.expressive.ExpressiveFileThumbnail
import com.ripple.filemanager.ui.expressive.ExpressiveTheme
import com.ripple.filemanager.ui.theme.LocalAppFont
import com.ripple.filemanager.ui.theme.SkylineColors
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
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
    var currentPath by remember { mutableStateOf(initialPath) }
    var folders by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var files by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Map of selected files: key is file path, value is FileItem
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

    GradientAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxHeight(0.88f)
            .fillMaxWidth(0.94f),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (currentPath != Environment.getExternalStorageDirectory().path) {
                            IconButton(
                                onClick = {
                                    val parent = File(currentPath).parent
                                    if (parent != null) {
                                        loadPath(parent)
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                    tint = SkylineColors.Amber
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        MonoLabel(
                            title,
                            color = SkylineColors.Amber,
                            fontSize = 15
                        )
                    }

                    if (selectedFiles.isNotEmpty()) {
                        Surface(
                            shape = CircleShape,
                            color = colors.accent,
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Text(
                                text = "${selectedFiles.size} selected",
                                color = colors.ink,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = LocalAppFont.current,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick directory shortcut chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    standardChips.forEach { (name, path) ->
                        val isSelected = currentPath.equals(path, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) SkylineColors.Amber.copy(alpha = 0.2f) else colors.insetCard,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, SkylineColors.Amber) else null,
                            modifier = Modifier.clickable {
                                if (currentPath != path) {
                                    loadPath(path)
                                }
                            }
                        ) {
                            Text(
                                text = name,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) SkylineColors.Amber else colors.text,
                                fontFamily = LocalAppFont.current,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxSize()) {
                // Current path & quick select/clear bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = currentPath.substringAfterLast("/"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (files.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Select All",
                                fontSize = 11.sp,
                                color = SkylineColors.Amber,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable {
                                        files.forEach { file ->
                                            selectedFiles[file.path] = file
                                        }
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                            if (selectedFiles.isNotEmpty()) {
                                Text(
                                    text = "Clear",
                                    fontSize = 11.sp,
                                    color = colors.muted,
                                    modifier = Modifier
                                        .clickable {
                                            selectedFiles.clear()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                            }
                        }
                    }
                }

                HorizontalDivider(color = colors.line.copy(alpha = colors.lineAlpha))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SkylineColors.Amber)
                    }
                } else if (folders.isEmpty() && files.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No files or folders here",
                            fontFamily = LocalAppFont.current,
                            color = colors.muted
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Folders first
                        items(folders, key = { "folder_${it.path}" }) { folder ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = folder.name,
                                        fontFamily = LocalAppFont.current,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = SkylineColors.Amber,
                                        modifier = Modifier.size(28.dp)
                                    )
                                },
                                modifier = Modifier.clickable {
                                    loadPath(folder.path)
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent
                                )
                            )
                            HorizontalDivider(color = colors.line.copy(alpha = 0.3f))
                        }

                        // Files next
                        items(files, key = { "file_${it.path}" }) { file ->
                            val isSelected = selectedFiles.containsKey(file.path)
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = file.name,
                                        fontFamily = LocalAppFont.current,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text = file.size,
                                        fontSize = 11.sp,
                                        color = colors.muted
                                    )
                                },
                                leadingContent = {
                                    ExpressiveFileThumbnail(
                                        file = file,
                                        isSelected = isSelected,
                                        size = 36.dp
                                    )
                                },
                                trailingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) colors.accent else Color.Transparent)
                                            .border(
                                                width = 1.5.dp,
                                                color = if (isSelected) colors.accent else colors.muted.copy(alpha = 0.5f),
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
                                },
                                modifier = Modifier
                                    .clickable {
                                        if (isSelected) {
                                            selectedFiles.remove(file.path)
                                        } else {
                                            selectedFiles[file.path] = file
                                        }
                                    },
                                colors = ListItemDefaults.colors(
                                    containerColor = if (isSelected) colors.accent.copy(alpha = 0.1f) else Color.Transparent
                                )
                            )
                            HorizontalDivider(color = colors.line.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onFilesSelected(selectedFiles.values.toList())
                },
                enabled = selectedFiles.isNotEmpty(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.ink,
                    disabledContainerColor = colors.insetCard,
                    disabledContentColor = colors.muted
                )
            ) {
                Text(
                    text = if (selectedFiles.isEmpty()) confirmText else "$confirmText (${selectedFiles.size})",
                    fontFamily = LocalAppFont.current,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onOpenSystemPicker != null) {
                    TextButton(
                        onClick = {
                            onDismiss()
                            onOpenSystemPicker()
                        }
                    ) {
                        Icon(
                            Icons.Outlined.FileOpen,
                            contentDescription = null,
                            tint = SkylineColors.Amber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "System Picker",
                            color = SkylineColors.Amber,
                            fontSize = 12.sp
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(
                        stringResource(R.string.cancel),
                        color = SkylineColors.TextDim
                    )
                }
            }
        },
        containerColor = SkylineColors.Surface,
        shape = getDynamicCornerShape(16f, cornerRoundness)
    )
}
