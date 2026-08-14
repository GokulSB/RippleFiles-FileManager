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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
                .clickable(onClick = onClick)
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
@Composable
fun ArchiveExtractDialog(
    cornerRoundness: Float,
    archiveName: String,
    itemCount: String,
    isRar: Boolean,
    onExtractHere: () -> Unit,
    onExtractTo: () -> Unit,
    onViewContents: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
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
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            // Card container
            Box(
                modifier = Modifier
                    .padding(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Consume clicks to avoid dismissal
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
                        text = "ARCHIVE", 
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
                    
                    // Action rows
                    ArchiveActionRow("Extract here", itemCount, onClick = onExtractHere)
                    ArchiveActionRow("Extract to...", "choose folder", onClick = onExtractTo)
                    ArchiveActionRow("View contents", "", onClick = onViewContents)
                    ArchiveActionRow(
                        label = "Add to archive", 
                        hint = if (isRar) "RAR unsupported" else "", 
                        disabled = isRar, 
                        onClick = {}
                    )
                    ArchiveActionRow("Delete", "", hideDivider = true, onClick = onDelete)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Cancel button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha=0.5f))
                            .clickable(onClick = onDismiss)
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
                }
            }
        }
    }
}
@Composable
private fun ArchiveActionRow(
    label: String,
    hint: String,
    disabled: Boolean = false,
    hideDivider: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !disabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = TextStyle(
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (disabled) androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                )
            )
            if (hint.isNotEmpty()) {
                Text(
                    text = hint,
                    style = TextStyle(
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 10.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
        if (!hideDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0x338A6A45))
            )
        }
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
                    onClick = { if (isComplete) onDismissWhenComplete() }
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
                            .clickable(enabled = isComplete, onClick = onDismissWhenComplete)
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
                    onClick = onDismiss
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
                                .clickable(onClick = onDismiss)
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
                    onClick = onDismiss
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
                            .clickable(onClick = onDismiss)
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
