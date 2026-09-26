package com.ripple.filemanager.ui.expressive

import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripple.filemanager.R
import com.ripple.filemanager.data.share.IncomingSharePrompt
import com.ripple.filemanager.data.share.SharedIncomingFile
import com.ripple.filemanager.ui.FolderPickerDialog
import com.ripple.filemanager.ui.theme.LocalAppFont
import java.io.File

/**
 * Dedicated "Save to Ripple Files" bottom sheet displayed when receiving shared files:
 * - Lists incoming file(s) with metadata (name, size, type).
 * - Allows picking destination folder (quick chips: Downloads, Documents, Pictures, Storage + full folder picker).
 * - Confirms saving files directly into the destination with duplicate avoidance and media scanning.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveToRippleSheet(
    prompt: IncomingSharePrompt,
    onSave: (destinationPath: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = ExpressiveTheme.colors
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    val context = LocalContext.current

    var selectedPath by remember(prompt.selectedDestination) {
        mutableStateOf(prompt.selectedDestination)
    }
    var showFolderPicker by remember { mutableStateOf(false) }

    val files = prompt.files
    val isSingleFile = files.size == 1
    val singleFile = files.firstOrNull()

    val totalBytes = files.sumOf { it.sizeBytes }
    val totalSizeFormatted = if (totalBytes > 0) {
        android.text.format.Formatter.formatFileSize(context, totalBytes)
    } else {
        "--"
    }

    val quickDestinations = remember {
        listOf(
            Triple("Downloads", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath, Icons.Outlined.Download),
            Triple("Documents", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath, Icons.Outlined.Description),
            Triple("Pictures", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath, Icons.Outlined.Image),
            Triple("Storage", Environment.getExternalStorageDirectory().absolutePath, Icons.Outlined.SdStorage)
        )
    }

    if (showFolderPicker) {
        FolderPickerDialog(
            initialPath = selectedPath,
            onDismiss = { showFolderPicker = false },
            onFolderSelected = { folder ->
                selectedPath = folder
                showFolderPicker = false
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!prompt.isSaving) onDismiss()
        },
        sheetState = sheetState,
        dragHandle = null,
        containerColor = colors.container,
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.container)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 16.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.muted.copy(alpha = 0.4f))
                    .align(Alignment.CenterHorizontally)
            )

            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val headerIcon = if (isSingleFile && singleFile != null) {
                    getFileCategoryIcon(singleFile.type)
                } else {
                    Icons.Outlined.SaveAlt
                }

                val headerBg = if (isSingleFile && singleFile != null) {
                    ExpressiveTokens.categoryPastel(singleFile.type)
                } else {
                    ExpressiveTokens.PastelTeal
                }

                CookieIcon(
                    icon = headerIcon,
                    bgColor = headerBg,
                    iconTint = ExpressiveTokens.OnPastel,
                    size = 48.dp,
                    lobes = 8
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Save to Ripple Files",
                        fontFamily = LocalAppFont.current,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.text
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isSingleFile) "1 incoming file" else "${files.size} incoming files",
                        fontFamily = LocalAppFont.current,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = colors.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Body Card: Single file preview or multi-file list
            if (isSingleFile && singleFile != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.card)
                        .border(1.dp, colors.line, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CookieIcon(
                            icon = getFileCategoryIcon(singleFile.type),
                            bgColor = ExpressiveTokens.categoryPastel(singleFile.type),
                            iconTint = ExpressiveTokens.OnPastel,
                            size = 42.dp,
                            lobes = 8
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = singleFile.name,
                                fontFamily = LocalAppFont.current,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val typeLabel = singleFile.mimeType ?: singleFile.type.uppercase()
                            Text(
                                text = "${singleFile.formattedSize} · $typeLabel",
                                fontFamily = LocalAppFont.current,
                                fontSize = 12.sp,
                                color = colors.muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                // Multi-file summary and scrollable items
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.card)
                        .border(1.dp, colors.line, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${files.size} files ($totalSizeFormatted)",
                                fontFamily = LocalAppFont.current,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.text
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(files) { file ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.container)
                                        .border(1.dp, colors.line.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = getFileCategoryIcon(file.type),
                                        contentDescription = null,
                                        tint = colors.muted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = file.name,
                                        fontFamily = LocalAppFont.current,
                                        fontSize = 12.5.sp,
                                        color = colors.text,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = file.formattedSize,
                                        fontFamily = LocalAppFont.current,
                                        fontSize = 11.sp,
                                        color = colors.muted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Destination Folder Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "SAVE DESTINATION",
                    fontFamily = LocalAppFont.current,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.muted,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Current selected folder pill
                val isStorageRoot = selectedPath == Environment.getExternalStorageDirectory().absolutePath
                val folderDisplayName = if (isStorageRoot) "Internal Storage" else File(selectedPath).name.ifEmpty { "Internal Storage" }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.card)
                        .border(1.dp, colors.line, RoundedCornerShape(16.dp))
                        .clickable {
                            haptics.tap()
                            showFolderPicker = true
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = folderDisplayName,
                            fontFamily = LocalAppFont.current,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = selectedPath,
                            fontFamily = LocalAppFont.current,
                            fontSize = 11.sp,
                            color = colors.muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Change",
                        fontFamily = LocalAppFont.current,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.accent
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick destination chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    quickDestinations.forEach { (name, path, icon) ->
                        val isSelected = selectedPath == path
                        val chipBg = if (isSelected) colors.accent.copy(alpha = 0.22f) else colors.card
                        val chipBorder = if (isSelected) colors.accent else colors.line.copy(alpha = 0.4f)
                        val chipContentColor = if (isSelected) colors.accent else colors.text

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(chipBg)
                                .border(1.dp, chipBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    haptics.tap()
                                    selectedPath = path
                                }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = chipContentColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = name,
                                fontFamily = LocalAppFont.current,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = chipContentColor
                            )
                        }
                    }

                    // Browse custom folder chip
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.card)
                            .border(1.dp, colors.line.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .clickable {
                                haptics.tap()
                                showFolderPicker = true
                            }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            tint = colors.muted,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Browse...",
                            fontFamily = LocalAppFont.current,
                            fontSize = 12.sp,
                            color = colors.muted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel button
                Button(
                    onClick = {
                        haptics.tap()
                        onDismiss()
                    },
                    enabled = !prompt.isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.card,
                        contentColor = colors.text
                    )
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = LocalAppFont.current
                    )
                }

                // Save button
                Button(
                    onClick = {
                        haptics.tap()
                        onSave(selectedPath)
                    },
                    enabled = !prompt.isSaving,
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.ink
                    )
                ) {
                    if (prompt.isSaving) {
                        CircularProgressIndicator(
                            color = colors.ink,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.SaveAlt,
                            contentDescription = null,
                            tint = colors.ink,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Here",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = LocalAppFont.current
                        )
                    }
                }
            }
        }
    }
}

private fun getFileCategoryIcon(type: String): ImageVector {
    return when (type) {
        "image" -> Icons.Outlined.Image
        "video" -> Icons.Outlined.VideoFile
        "audio" -> Icons.Outlined.AudioFile
        "doc" -> Icons.Outlined.Description
        "apk" -> Icons.Default.Android
        "archive" -> Icons.Outlined.FolderZip
        else -> Icons.Outlined.InsertDriveFile
    }
}
