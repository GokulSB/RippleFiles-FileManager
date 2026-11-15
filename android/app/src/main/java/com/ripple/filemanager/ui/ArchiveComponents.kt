package com.ripple.filemanager.ui
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.ripple.filemanager.ui.getDynamicCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.ripple.filemanager.ui.expressive.CookieIcon
import com.ripple.filemanager.ui.expressive.ExpressiveTheme
import com.ripple.filemanager.ui.expressive.ExpressiveTokens
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ripple.filemanager.ui.theme.LocalAppFont
import com.ripple.filemanager.ui.theme.FrauncesFontFamily
import com.ripple.filemanager.ui.theme.JetBrainsMonoFamily
import com.ripple.filemanager.ui.theme.ManropeFontFamily



@Composable
fun ArchiveFileCard(
    cornerRoundness: Float,
    name: String,
    meta: String,
    itemCount: String,
    onClick: () -> Unit
) {
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 4.dp, bottom = 4.dp)
    ) {
        // Shadow layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(4.dp, 4.dp)
                .clip(getDynamicCornerShape(12f, cornerRoundness))
                .background(androidx.compose.material3.MaterialTheme.colorScheme.outline)
        )
        
        // Content layer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(getDynamicCornerShape(12f, cornerRoundness)).background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline)
                .clickable(onClick = { haptics.tap(); onClick() })
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fanned three-layer stack glyph
            Box(
                modifier = Modifier.size(42.dp) // 34dp + 8dp offset
            ) {
                for (i in 0..2) {
                    val isTop = i == 2
                    val bgColor = if (isTop) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh
                    Box(
                        modifier = Modifier
                            .offset(x = (i * 4).dp, y = ((2 - i) * 4).dp)
                            .size(34.dp)
                            .background(bgColor)
                            .border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = TextStyle(
                        fontFamily = FrauncesFontFamily,
                        fontSize = 18.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = meta,
                    style = TextStyle(
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 12.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Item count badge
            Box(
                modifier = Modifier
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.primary)
                    .border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = itemCount,
                    style = TextStyle(
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 10.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.outline
                    )
                )
            }
        }
    }
}
enum class ExtractDestinationMode {
    NEW_FOLDER,
    HERE,
    CHOOSE_FOLDER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveExtractDialog(
    cornerRoundness: Float = 0.5f,
    archiveName: String,
    itemCount: String,
    isRar: Boolean,
    onExtractHere: (password: String?) -> Unit,
    onExtractToNewFolder: (password: String?) -> Unit,
    onExtractTo: (password: String?) -> Unit,
    onViewContents: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    val colors = ExpressiveTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var destinationMode by remember { mutableStateOf(ExtractDestinationMode.NEW_FOLDER) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var hasPassword by remember { mutableStateOf(false) }

    val baseFolderName = remember(archiveName) {
        archiveName.substringBeforeLast('.').ifEmpty { "Extracted" }
    }

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
                .padding(bottom = 24.dp),
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

            // Header: CookieIcon + archiveName + format/itemCount
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
                        text = archiveName,
                        fontFamily = LocalAppFont.current,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val formatLabel = remember(archiveName, isRar) {
                        val ext = archiveName.substringAfterLast('.', "").uppercase(java.util.Locale.getDefault())
                        when {
                            isRar -> "RAR Archive"
                            ext in setOf("7Z", "ISO", "ZST", "TAR", "GZ", "BZ2", "XZ", "ZIP", "CAB", "WIM", "LZ4") -> "$ext Archive"
                            else -> "Archive"
                        }
                    }
                    Text(
                        text = if (itemCount.isNotBlank()) "$itemCount • $formatLabel" else formatLabel,
                        fontFamily = LocalAppFont.current,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = colors.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Destination selection
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "Extract Destination",
                    fontFamily = LocalAppFont.current,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.text
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isNewFolder = destinationMode == ExtractDestinationMode.NEW_FOLDER
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isNewFolder) colors.accent else colors.card)
                            .border(1.dp, if (isNewFolder) colors.accent else colors.line, RoundedCornerShape(12.dp))
                            .clickable {
                                haptics.tap()
                                destinationMode = ExtractDestinationMode.NEW_FOLDER
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "New Folder",
                            fontFamily = LocalAppFont.current,
                            fontSize = 12.sp,
                            fontWeight = if (isNewFolder) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isNewFolder) colors.ink else colors.text
                        )
                    }

                    val isHere = destinationMode == ExtractDestinationMode.HERE
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isHere) colors.accent else colors.card)
                            .border(1.dp, if (isHere) colors.accent else colors.line, RoundedCornerShape(12.dp))
                            .clickable {
                                haptics.tap()
                                destinationMode = ExtractDestinationMode.HERE
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Extract Here",
                            fontFamily = LocalAppFont.current,
                            fontSize = 12.sp,
                            fontWeight = if (isHere) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isHere) colors.ink else colors.text
                        )
                    }

                    val isChoose = destinationMode == ExtractDestinationMode.CHOOSE_FOLDER
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isChoose) colors.accent else colors.card)
                            .border(1.dp, if (isChoose) colors.accent else colors.line, RoundedCornerShape(12.dp))
                            .clickable {
                                haptics.tap()
                                destinationMode = ExtractDestinationMode.CHOOSE_FOLDER
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Choose…",
                            fontFamily = LocalAppFont.current,
                            fontSize = 12.sp,
                            fontWeight = if (isChoose) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isChoose) colors.ink else colors.text
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = when (destinationMode) {
                        ExtractDestinationMode.NEW_FOLDER -> "Will extract to: ./$baseFolderName/"
                        ExtractDestinationMode.HERE -> "Will extract directly into current folder"
                        ExtractDestinationMode.CHOOSE_FOLDER -> "Will prompt to choose target destination folder"
                    },
                    fontFamily = LocalAppFont.current,
                    fontSize = 11.sp,
                    color = colors.muted
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Password field toggle
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptics.tap()
                            hasPassword = !hasPassword
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Archive has password?",
                        fontFamily = LocalAppFont.current,
                        fontSize = 13.sp,
                        color = colors.text
                    )
                    Switch(
                        checked = hasPassword,
                        onCheckedChange = {
                            haptics.tap()
                            hasPassword = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.ink,
                            checkedTrackColor = colors.accent
                        )
                    )
                }

                if (hasPassword) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("Enter archive password", color = colors.muted, fontSize = 13.sp) },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.insetCard,
                            unfocusedContainerColor = colors.insetCard,
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.line,
                            focusedTextColor = colors.text,
                            unfocusedTextColor = colors.text
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Primary Extract Action Button
            Button(
                onClick = {
                    haptics.tap()
                    val pwd = if (hasPassword && password.isNotBlank()) password else null
                    when (destinationMode) {
                        ExtractDestinationMode.NEW_FOLDER -> onExtractToNewFolder(pwd)
                        ExtractDestinationMode.HERE -> onExtractHere(pwd)
                        ExtractDestinationMode.CHOOSE_FOLDER -> onExtractTo(pwd)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.ink
                )
            ) {
                Icon(Icons.Outlined.Unarchive, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Extract Now",
                    fontFamily = LocalAppFont.current,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Actions: View Contents & Delete
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // View Contents
                OutlinedButton(
                    onClick = {
                        haptics.tap()
                        onViewContents()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
                ) {
                    Icon(Icons.Outlined.Visibility, contentDescription = null, tint = colors.text, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Browse",
                        fontFamily = LocalAppFont.current,
                        fontSize = 13.sp,
                        color = colors.text
                    )
                }

                // Delete Archive
                OutlinedButton(
                    onClick = {
                        haptics.delete()
                        onDelete()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Delete",
                        fontFamily = LocalAppFont.current,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Cancel button
            TextButton(
                onClick = {
                    haptics.tap()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "Cancel",
                    fontFamily = LocalAppFont.current,
                    fontSize = 14.sp,
                    color = colors.muted
                )
            }
        }
    }
}

@Composable
private fun ExpressiveArchiveActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val colors = ExpressiveTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .border(1.dp, colors.line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isDestructive) colors.accent.copy(alpha = 0.15f) else colors.container),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) colors.accent else colors.text,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = LocalAppFont.current,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDestructive) colors.accent else colors.text
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontFamily = LocalAppFont.current,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = colors.muted
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
            contentDescription = null,
            tint = colors.muted.copy(alpha = 0.5f),
            modifier = Modifier.size(14.dp)
        )
    }
}
@Composable
fun ArchiveExtractProgressDialog(
    cornerRoundness: Float,
    archiveName: String,
    progress: Float,
    filesDone: Int,
    filesTotal: Int,
    onDismissWhenComplete: () -> Unit
) {
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    val isComplete = progress >= 1f
    
    Dialog(
        onDismissRequest = { if (isComplete) onDismissWhenComplete() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false, 
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x8C000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { haptics.tap(); if (isComplete) onDismissWhenComplete() }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Card
            Box(
                modifier = Modifier
                    .padding(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                // Shadow
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(5.dp, 5.dp)
                        .clip(getDynamicCornerShape(12f, cornerRoundness))
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.outline)
                )
                
                // Content
                Column(
                    modifier = Modifier
                        .clip(getDynamicCornerShape(12f, cornerRoundness)).background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline)
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = if (isComplete) "EXTRACTED" else "EXTRACTING", 
                        style = TextStyle(
                            fontFamily = JetBrainsMonoFamily, 
                            fontSize = 10.sp, 
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = archiveName, 
                        style = TextStyle(
                            fontFamily = FrauncesFontFamily, 
                            fontSize = 18.sp, 
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Fan-unstack visual
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp), 
                        contentAlignment = Alignment.Center
                    ) {
                        for (i in 0..4) {
                            val threshold = (i + 1) / 5f
                            val isRevealed = progress >= threshold
                            
                            val targetX = if (isRevealed) ((i - 2) * 56).dp else ((i - 2) * 4).dp
                            val targetY = if (isRevealed) 0.dp else (-(i - 2) * 4).dp
                            
                            val animX by animateDpAsState(
                                targetValue = targetX, 
                                animationSpec = tween(
                                    durationMillis = 350, 
                                    easing = CubicBezierEasing(0.2f, 0.8f, 0.3f, 1.2f)
                                ), label = "animX_$i"
                            )
                            val animY by animateDpAsState(
                                targetValue = targetY, 
                                animationSpec = tween(
                                    durationMillis = 350, 
                                    easing = CubicBezierEasing(0.2f, 0.8f, 0.3f, 1.2f)
                                ), label = "animY_$i"
                            )
                            
                            val bgColor = if (isRevealed) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh
                            
                            Box(
                                modifier = Modifier
                                    .offset(x = animX, y = animY)
                                    .size(48.dp, 60.dp)
                                    .background(bgColor)
                                    .border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Progress text row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$filesDone / $filesTotal files",
                            style = TextStyle(
                                fontFamily = JetBrainsMonoFamily, 
                                fontSize = 12.sp, 
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = TextStyle(
                                fontFamily = JetBrainsMonoFamily, 
                                fontSize = 12.sp, 
                                color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                            .border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (progress > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .background(androidx.compose.material3.MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Bottom button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (isComplete) androidx.compose.material3.MaterialTheme.colorScheme.outline else androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha=0.5f))
                            .background(if (isComplete) androidx.compose.material3.MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable(enabled = isComplete, onClick = { haptics.tap(); onDismissWhenComplete() })
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isComplete) "DONE" else "WORKING...", 
                            style = TextStyle(
                                fontFamily = JetBrainsMonoFamily, 
                                fontSize = 11.sp, 
                                color = if (isComplete) androidx.compose.material3.MaterialTheme.colorScheme.outline else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun ArchivePasswordDialog(
    cornerRoundness: Float,
    archiveName: String,
    attemptFailed: Boolean,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    var password by remember { mutableStateOf("") }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false, 
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x8C000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { haptics.tap(); onDismiss() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .padding(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                // Shadow
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(5.dp, 5.dp)
                        .clip(getDynamicCornerShape(12f, cornerRoundness))
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.outline)
                )
                
                // Content
                Column(
                    modifier = Modifier
                        .clip(getDynamicCornerShape(12f, cornerRoundness)).background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline)
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "PASSWORD REQUIRED", 
                        style = TextStyle(
                            fontFamily = JetBrainsMonoFamily, 
                            fontSize = 10.sp, 
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = archiveName, 
                        style = TextStyle(
                            fontFamily = FrauncesFontFamily, 
                            fontSize = 18.sp, 
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    androidx.compose.foundation.text.BasicTextField(
                        value = password,
                        onValueChange = { password = it },
                        textStyle = TextStyle(
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 14.sp,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                            .border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline)
                            .padding(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Fixed height row for error message
                    Box(modifier = Modifier.height(16.dp)) {
                        if (attemptFailed) {
                            Text(
                                text = "Incorrect password",
                                style = TextStyle(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 11.sp,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cancel
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha=0.5f))
                                .clickable(onClick = { haptics.tap(); onDismiss() })
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "CANCEL", 
                                style = TextStyle(
                                    fontFamily = JetBrainsMonoFamily, 
                                    fontSize = 11.sp, 
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                        // Unlock
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline)
                                .background(androidx.compose.material3.MaterialTheme.colorScheme.primary)
                                .clickable { onSubmit(password) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "UNLOCK", 
                                style = TextStyle(
                                    fontFamily = JetBrainsMonoFamily, 
                                    fontSize = 11.sp, 
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.outline,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun ArchiveFailedDialog(
    cornerRoundness: Float,
    archiveName: String,
    reason: String,
    onDismiss: () -> Unit
) {
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    val errorAccent = Color(0xFFE57373) // Red-leaning accent
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false, 
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x8C000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { haptics.tap(); onDismiss() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .padding(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                // Shadow
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(5.dp, 5.dp)
                        .clip(getDynamicCornerShape(12f, cornerRoundness))
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.outline)
                )
                
                // Content
                Column(
                    modifier = Modifier
                        .clip(getDynamicCornerShape(12f, cornerRoundness)).background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline)
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "EXTRACTION FAILED", 
                        style = TextStyle(
                            fontFamily = JetBrainsMonoFamily, 
                            fontSize = 10.sp, 
                            color = errorAccent
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = archiveName, 
                        style = TextStyle(
                            fontFamily = FrauncesFontFamily, 
                            fontSize = 18.sp, 
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = reason,
                        style = TextStyle(
                            fontFamily = ManropeFontFamily,
                            fontSize = 14.sp,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, errorAccent)
                            .background(Color.Transparent)
                            .clickable(onClick = { haptics.tap(); onDismiss() })
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "DISMISS", 
                            style = TextStyle(
                                fontFamily = JetBrainsMonoFamily, 
                                fontSize = 11.sp, 
                                color = errorAccent
                            )
                        )
                    }
                }
            }
        }
    }
}
