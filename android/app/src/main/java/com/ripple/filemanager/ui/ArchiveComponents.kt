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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveExtractDialog(
    cornerRoundness: Float = 0.5f,
    archiveName: String,
    itemCount: String,
    isRar: Boolean,
    onExtractHere: () -> Unit,
    onExtractTo: () -> Unit,
    onViewContents: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    val colors = ExpressiveTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                    Text(
                        text = if (itemCount.isNotBlank()) "$itemCount • ${if (isRar) "RAR Archive" else "Archive"}" else if (isRar) "RAR Archive" else "Archive",
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

            // Action Items
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExpressiveArchiveActionCard(
                    icon = Icons.Outlined.Unarchive,
                    title = "Extract here",
                    subtitle = "Extract files to the current folder",
                    onClick = {
                        haptics.tap()
                        onExtractHere()
                    }
                )

                ExpressiveArchiveActionCard(
                    icon = Icons.AutoMirrored.Outlined.DriveFileMove,
                    title = "Extract to...",
                    subtitle = "Choose destination folder",
                    onClick = {
                        haptics.tap()
                        onExtractTo()
                    }
                )

                ExpressiveArchiveActionCard(
                    icon = Icons.Outlined.Visibility,
                    title = "View contents",
                    subtitle = "Browse files inside this archive",
                    onClick = {
                        haptics.tap()
                        onViewContents()
                    }
                )

                ExpressiveArchiveActionCard(
                    icon = Icons.Outlined.Delete,
                    title = "Delete archive",
                    subtitle = "Move this archive to trash",
                    isDestructive = true,
                    onClick = {
                        haptics.delete()
                        onDelete()
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Cancel button
            Button(
                onClick = {
                    haptics.tap()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(24.dp),
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
