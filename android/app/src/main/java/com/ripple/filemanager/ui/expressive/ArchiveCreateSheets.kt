package com.ripple.filemanager.ui.expressive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ripple.filemanager.FileItem
import com.ripple.filemanager.archive.ArchiveCreateConfig
import com.ripple.filemanager.archive.ArchiveFormat
import com.ripple.filemanager.archive.CompressionMethodType
import com.ripple.filemanager.archive.SplitVolumeOption
import com.ripple.filemanager.archive.SUPPORTED_ZIP_CHARSETS
import com.ripple.filemanager.ui.formatSize
import com.ripple.filemanager.ui.theme.LocalAppFont
import java.io.File

/**
 * Bottom sheet dialog for configuring and initiating archive creation.
 * Allows user to choose format (ZIP / RAR), compression method, delete after archive,
 * and lock with password.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveCreateBottomSheet(
    selectedFiles: List<FileItem>,
    defaultName: String,
    onDismissRequest: () -> Unit,
    onConfirm: (config: ArchiveCreateConfig) -> Unit
) {
    val colors = ExpressiveTheme.colors
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var format by remember { mutableStateOf(ArchiveFormat.ZIP) }
    var archiveName by remember { mutableStateOf(defaultName) }
    var compressionMethod by remember { mutableStateOf(CompressionMethodType.NORMAL) }
    var deleteAfterArchive by remember { mutableStateOf(false) }
    var lockWithPassword by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var encryptFileNames by remember { mutableStateOf(false) }
    var splitVolume by remember { mutableStateOf(SplitVolumeOption.NO_SPLIT) }
    var charset by remember { mutableStateOf("UTF-8") }

    val totalBytes = remember(selectedFiles) {
        selectedFiles.sumOf { it.sizeBytes }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = colors.container,
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            // Centered grab handle
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 16.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.muted.copy(alpha = 0.4f))
                    .align(Alignment.CenterHorizontally)
            )

            // Header Row: Icon + Title + Count/Size
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val pastel = ExpressiveTokens.categoryPastel("archive")
                CookieIcon(
                    icon = Icons.Outlined.FolderZip,
                    bgColor = pastel,
                    iconTint = ExpressiveTokens.OnPastel,
                    size = 46.dp,
                    lobes = 8
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Create Archive",
                        fontFamily = LocalAppFont.current,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${selectedFiles.size} items selected (${formatSize(totalBytes)})",
                        fontFamily = LocalAppFont.current,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = colors.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Archive Name input
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "Archive Name",
                    fontFamily = LocalAppFont.current,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.text
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = archiveName,
                    onValueChange = { archiveName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.insetCard,
                        unfocusedContainerColor = colors.insetCard,
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.line.copy(alpha = colors.lineAlpha),
                        focusedTextColor = colors.text,
                        unfocusedTextColor = colors.text
                    ),
                    trailingIcon = {
                        if (archiveName.isNotBlank()) {
                            IconButton(onClick = { archiveName = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = colors.muted)
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Format selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "Archive Format",
                    fontFamily = LocalAppFont.current,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.text
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ArchiveFormat.values().forEach { fmt ->
                        val isSelected = fmt == format
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) colors.accent else colors.card)
                                .border(
                                    1.dp,
                                    if (isSelected) colors.accent else colors.line.copy(alpha = colors.lineAlpha),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    haptics.tap()
                                    format = fmt
                                    val base = archiveName.substringBeforeLast('.', archiveName)
                                    archiveName = "$base.${fmt.extension}"
                                }
                                .padding(horizontal = 14.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text = fmt.shortName,
                                fontFamily = LocalAppFont.current,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) colors.ink else colors.text
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.insetCard)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${format.label} • ${format.description}",
                        fontFamily = LocalAppFont.current,
                        fontSize = 12.sp,
                        color = colors.muted
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Compression Method
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "Compression Method",
                    fontFamily = LocalAppFont.current,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.text
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompressionMethodType.values().forEach { method ->
                        val isSelected = method == compressionMethod
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) colors.accent else colors.card)
                                .border(
                                    1.dp,
                                    if (isSelected) colors.accent else colors.line.copy(alpha = colors.lineAlpha),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    haptics.tap()
                                    compressionMethod = method
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = method.label,
                                fontFamily = LocalAppFont.current,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) colors.ink else colors.text
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                // Descriptive explanation of selected method
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.insetCard)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = compressionMethod.description,
                        fontFamily = LocalAppFont.current,
                        fontSize = 12.sp,
                        color = colors.muted
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Options Switches: Delete after archive + Lock with password
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.card)
                    .border(1.dp, colors.line.copy(alpha = colors.lineAlpha), RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Option A: Delete files after archiving
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            haptics.tap()
                            deleteAfterArchive = !deleteAfterArchive
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = colors.text,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Delete original files",
                            fontFamily = LocalAppFont.current,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.text
                        )
                        Text(
                            text = "Remove source items once archived",
                            fontFamily = LocalAppFont.current,
                            fontSize = 12.sp,
                            color = colors.muted
                        )
                    }
                    Switch(
                        checked = deleteAfterArchive,
                        onCheckedChange = {
                            haptics.tap()
                            deleteAfterArchive = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.ink,
                            checkedTrackColor = colors.accent
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = colors.line.copy(alpha = colors.lineAlpha), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Option B: Lock archive with password
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            haptics.tap()
                            lockWithPassword = !lockWithPassword
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = colors.text,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lock with password",
                            fontFamily = LocalAppFont.current,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.text
                        )
                        Text(
                            text = "Encrypt files using AES-256",
                            fontFamily = LocalAppFont.current,
                            fontSize = 12.sp,
                            color = colors.muted
                        )
                    }
                    Switch(
                        checked = lockWithPassword,
                        onCheckedChange = {
                            haptics.tap()
                            lockWithPassword = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.ink,
                            checkedTrackColor = colors.accent
                        )
                    )
                }

                // Password field when lock is enabled
                AnimatedVisibility(
                    visible = lockWithPassword,
                    enter = fadeIn(tween(160)) + androidx.compose.animation.expandVertically(tween(200)),
                    exit = fadeOut(tween(120)) + androidx.compose.animation.shrinkVertically(tween(160))
                ) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = { Text("Enter archive password", color = colors.muted, fontSize = 13.sp) },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = colors.muted
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = colors.insetCard,
                                unfocusedContainerColor = colors.insetCard,
                                focusedBorderColor = colors.accent,
                                unfocusedBorderColor = colors.line.copy(alpha = colors.lineAlpha),
                                focusedTextColor = colors.text,
                                unfocusedTextColor = colors.text
                            )
                        )
                        if (lockWithPassword && password.isBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "A password is required when encryption is enabled",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                fontFamily = LocalAppFont.current
                            )
                        }

                        // 7z Header Encryption (Encrypt file names)
                        if (format == ArchiveFormat.SEVEN_Z) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        haptics.tap()
                                        encryptFileNames = !encryptFileNames
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Encrypt file names",
                                        fontFamily = LocalAppFont.current,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.text
                                    )
                                    Text(
                                        text = "Hide filenames from unauthorized view (7z header encryption)",
                                        fontFamily = LocalAppFont.current,
                                        fontSize = 11.sp,
                                        color = colors.muted
                                    )
                                }
                                Switch(
                                    checked = encryptFileNames,
                                    onCheckedChange = {
                                        haptics.tap()
                                        encryptFileNames = it
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = colors.ink,
                                        checkedTrackColor = colors.accent
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Split Volume selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "Split into Volumes",
                    fontFamily = LocalAppFont.current,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.text
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SplitVolumeOption.values().forEach { opt ->
                        val isSelected = opt == splitVolume
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) colors.accent else colors.card)
                                .border(
                                    1.dp,
                                    if (isSelected) colors.accent else colors.line.copy(alpha = colors.lineAlpha),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    haptics.tap()
                                    splitVolume = opt
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = opt.label,
                                fontFamily = LocalAppFont.current,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) colors.ink else colors.text
                            )
                        }
                    }
                }
            }

            // 6. ZIP Filename Encoding (when ZIP is selected)
            if (format == ArchiveFormat.ZIP) {
                Spacer(modifier = Modifier.height(20.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = "ZIP Filename Encoding",
                        fontFamily = LocalAppFont.current,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.text
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SUPPORTED_ZIP_CHARSETS.forEach { cs ->
                            val isSelected = cs == charset
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) colors.accent else colors.card)
                                    .border(
                                        1.dp,
                                        if (isSelected) colors.accent else colors.line.copy(alpha = colors.lineAlpha),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        haptics.tap()
                                        charset = cs
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = cs,
                                    fontFamily = LocalAppFont.current,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) colors.ink else colors.text
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Action Buttons (Cancel + Create Archive)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cancel
                Button(
                    onClick = {
                        haptics.tap()
                        onDismissRequest()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.card,
                        contentColor = colors.text
                    )
                ) {
                    Text(
                        text = "Cancel",
                        fontFamily = LocalAppFont.current,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Create Archive
                val canCreate = archiveName.isNotBlank() && (!lockWithPassword || password.isNotBlank())
                Button(
                    onClick = {
                        haptics.tap()
                        // Ensure filename has proper extension
                        val ext = format.extension
                        val cleanName = if (archiveName.endsWith(".$ext", ignoreCase = true)) {
                            archiveName
                        } else {
                            val base = archiveName.substringBeforeLast('.', archiveName)
                            "$base.$ext"
                        }

                        onConfirm(
                            ArchiveCreateConfig(
                                name = cleanName,
                                format = format,
                                compressionMethod = compressionMethod,
                                deleteAfterArchive = deleteAfterArchive,
                                password = if (lockWithPassword) password else null,
                                encryptFileNames = encryptFileNames && format == ArchiveFormat.SEVEN_Z,
                                splitVolume = splitVolume,
                                charset = charset
                            )
                        )
                    },
                    enabled = canCreate,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.ink,
                        disabledContainerColor = colors.accent.copy(alpha = 0.4f),
                        disabledContentColor = colors.ink.copy(alpha = 0.4f)
                    )
                ) {
                    Text(
                        text = "Create Archive",
                        fontFamily = LocalAppFont.current,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Progress dialog displayed while archiving is in progress.
 */
@Composable
fun ArchiveProgressDialog(
    archiveName: String,
    currentFileName: String,
    percent: Int,
    onCancel: () -> Unit
) {
    val colors = ExpressiveTheme.colors
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current

    Dialog(
        onDismissRequest = { /* non-dismissable by tapping outside */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.container,
            border = borderStroke(colors),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Header row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CookieIcon(
                        icon = Icons.Outlined.FolderZip,
                        bgColor = ExpressiveTokens.categoryPastel("archive"),
                        iconTint = ExpressiveTokens.OnPastel,
                        size = 46.dp,
                        lobes = 8
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Creating Archive…",
                            fontFamily = LocalAppFont.current,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.text
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = archiveName,
                            fontFamily = LocalAppFont.current,
                            fontSize = 13.sp,
                            color = colors.muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Current file
                Text(
                    text = if (currentFileName.isNotBlank()) "Compressing: $currentFileName" else "Preparing files…",
                    fontFamily = LocalAppFont.current,
                    fontSize = 12.sp,
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { percent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = colors.accent,
                    trackColor = colors.insetCard
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Percentage indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$percent%",
                        fontFamily = LocalAppFont.current,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accent
                    )
                    Text(
                        text = "Please wait…",
                        fontFamily = LocalAppFont.current,
                        fontSize = 12.sp,
                        color = colors.muted
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Cancel Button
                Button(
                    onClick = {
                        haptics.tap()
                        onCancel()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(23.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.card,
                        contentColor = colors.text
                    )
                ) {
                    Text(
                        text = "Cancel",
                        fontFamily = LocalAppFont.current,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Completion dialog shown when the archive has been successfully created.
 * Provides "Open Archive", "Share", and "Close" actions.
 */
@Composable
fun ArchiveCompletedDialog(
    archiveFile: File,
    onOpenArchive: () -> Unit,
    onShare: () -> Unit,
    onClose: () -> Unit
) {
    val colors = ExpressiveTheme.colors
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.container,
            border = borderStroke(colors),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success Badge
                CookieIcon(
                    icon = Icons.Outlined.CheckCircle,
                    bgColor = ExpressiveTokens.categoryPastel("docs"),
                    iconTint = ExpressiveTokens.OnPastel,
                    size = 54.dp,
                    lobes = 8
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Archive Created!",
                    fontFamily = LocalAppFont.current,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.text
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Your files have been safely compressed",
                    fontFamily = LocalAppFont.current,
                    fontSize = 13.sp,
                    color = colors.muted
                )

                Spacer(modifier = Modifier.height(16.dp))

                // File Details Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.insetCard)
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.FolderZip,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = archiveFile.name,
                                fontFamily = LocalAppFont.current,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatSize(archiveFile.length()),
                                fontFamily = LocalAppFont.current,
                                fontSize = 12.sp,
                                color = colors.muted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Action 1: Open Archive
                Button(
                    onClick = {
                        haptics.tap()
                        onOpenArchive()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.ink
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderZip,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Archive",
                        fontFamily = LocalAppFont.current,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action 2: Share Archive
                Button(
                    onClick = {
                        haptics.tap()
                        onShare()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.card,
                        contentColor = colors.text
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share",
                        fontFamily = LocalAppFont.current,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Action 3: Close
                TextButton(
                    onClick = {
                        haptics.tap()
                        onClose()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Close",
                        fontFamily = LocalAppFont.current,
                        fontSize = 14.sp,
                        color = colors.muted
                    )
                }
            }
        }
    }
}

private fun borderStroke(colors: ExpressiveColorScheme): androidx.compose.foundation.BorderStroke {
    return androidx.compose.foundation.BorderStroke(1.dp, colors.line.copy(alpha = colors.lineAlpha))
}
