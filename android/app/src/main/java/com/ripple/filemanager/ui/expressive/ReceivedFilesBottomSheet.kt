package com.ripple.filemanager.ui.expressive

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.ripple.filemanager.FileItem
import com.ripple.filemanager.ReceivedFilesPrompt
import com.ripple.filemanager.R
import com.ripple.filemanager.ui.theme.LocalAppFont
import java.io.File

/**
 * Expressive bottom sheet displayed after receiving files via LocalSend:
 * - Shown after the file reception toast notification
 * - Lets the user immediately Open the received file or Cancel/dismiss
 * - If multiple files were received, previews the files and provides "Open Folder" option
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceivedFilesBottomSheet(
    prompt: ReceivedFilesPrompt,
    onOpen: (FileItem) -> Unit,
    onOpenFolder: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = ExpressiveTheme.colors
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    val context = LocalContext.current

    val files = prompt.files
    val isSingleFile = files.size == 1
    val singleFile = files.firstOrNull()
    val targetFolder = singleFile?.path?.let { File(it).parent } ?: ""

    val totalBytes = files.sumOf { it.sizeBytes }
    val totalSizeFormatted = Formatter.formatFileSize(context, totalBytes)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                    Icons.Outlined.CheckCircle
                }

                val headerBg = if (isSingleFile && singleFile != null) {
                    ExpressiveTokens.categoryPastel(singleFile.type)
                } else {
                    colors.accent
                }

                CookieIcon(
                    icon = headerIcon,
                    bgColor = headerBg,
                    iconTint = if (isSingleFile) ExpressiveTokens.OnPastel else colors.ink,
                    size = 48.dp,
                    lobes = 8
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isSingleFile) "File Received" else "${files.size} Files Received",
                        fontFamily = LocalAppFont.current,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.text
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "From ${prompt.peerAlias}",
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
                        .clickable {
                            haptics.tap()
                            onOpen(singleFile)
                        }
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
                            Text(
                                text = "${singleFile.size} · Saved to RippleReceived",
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
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "Saved in RippleReceived",
                                fontFamily = LocalAppFont.current,
                                fontSize = 11.5.sp,
                                color = colors.muted
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 150.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(files) { file ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.container)
                                        .border(1.dp, colors.line.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            haptics.tap()
                                            onOpen(file)
                                        }
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
                                        text = file.size,
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

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons: Dual side-by-side pill buttons matching design system
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel button (secondary style)
                Button(
                    onClick = {
                        haptics.tap()
                        onDismiss()
                    },
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

                // Open button (primary accent style)
                Button(
                    onClick = {
                        haptics.tap()
                        if (isSingleFile && singleFile != null) {
                            onOpen(singleFile)
                        } else {
                            onOpenFolder(targetFolder)
                        }
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.ink
                    )
                ) {
                    Icon(
                        imageVector = if (isSingleFile) Icons.Outlined.Visibility else Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        tint = colors.ink,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSingleFile) "Open File" else "Open Folder",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = LocalAppFont.current
                    )
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
