package com.ripple.filemanager.ui

import androidx.compose.ui.res.stringResource
import com.ripple.filemanager.R

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.ripple.filemanager.ui.theme.FrauncesFontFamily
import com.ripple.filemanager.ui.theme.JetBrainsMonoFamily
import com.ripple.filemanager.ui.theme.ManropeFontFamily
import com.ripple.filemanager.ui.theme.OutfitFontFamily
import com.ripple.filemanager.ui.theme.SkylineColors
import com.ripple.filemanager.ui.theme.fileTypeCode
import com.ripple.filemanager.ui.theme.fileTypeTone

// ─────────────────────────────────────────────────────────────────────────────
// SHAPE
// ─────────────────────────────────────────────────────────────────────────────

/** 0dp corner = hard square. The Skyline global shape for every non-nav element. */
val SkylineShape: Shape = RoundedCornerShape(0.dp)

/** Full pill used exclusively for the bottom nav. */
val SkylinePillShape: Shape = RoundedCornerShape(50)

// ─────────────────────────────────────────────────────────────────────────────
// MONO LABEL
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Uppercase, Outfit, amber-dim — used for all section headers and metadata.
 */
@Composable
fun MonoLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = SkylineColors.TextDim,
    fontSize: Int = 10
) {
    Text(
        text = text.uppercase(),
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = fontSize.sp,
        letterSpacing = 1.sp,
        color = color,
        modifier = modifier
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// CORNER TICK OVERLAY
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Draws four L-shaped corner brackets (2dp stroke, amber) as a Canvas overlay.
 * Compose into a Box containing the card content.
 */
@Composable
fun CornerTickOverlay(
    modifier: Modifier = Modifier,
    color: Color = SkylineColors.Amber,
    tickLength: Dp = 10.dp,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = strokeWidth.toPx())
        val tick   = tickLength.toPx()
        val w = size.width
        val h = size.height

        // Top-left
        drawPath(path = Path().apply { moveTo(0f, tick); lineTo(0f, 0f); lineTo(tick, 0f) }, color = color, style = stroke)
        // Top-right
        drawPath(path = Path().apply { moveTo(w - tick, 0f); lineTo(w, 0f); lineTo(w, tick) }, color = color, style = stroke)
        // Bottom-left
        drawPath(path = Path().apply { moveTo(0f, h - tick); lineTo(0f, h); lineTo(tick, h) }, color = color, style = stroke)
        // Bottom-right
        drawPath(path = Path().apply { moveTo(w - tick, h); lineTo(w, h); lineTo(w, h - tick) }, color = color, style = stroke)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BLUEPRINT CARD
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A container with a subtle dot-grid background + CornerTickOverlay.
 * Wrap any card content inside this for the "Skyline blueprint" look.
 */
@Composable
fun BlueprintCard(
    modifier: Modifier = Modifier,
    heroEmphasis: Boolean = false,
    cornerRoundness: Float = 0f,
    content: @Composable BoxScope.() -> Unit
) {
    val borderColor = if (heroEmphasis) SkylineColors.Amber else SkylineColors.Border
    val borderWidth = if (heroEmphasis) 2.dp else 1.dp
    val shape = getDynamicCornerShape(16f, cornerRoundness)

    Box(
        modifier = modifier
            .border(borderWidth, borderColor, shape)
            .background(MaterialTheme.colorScheme.surface, shape)
            .clip(shape)
            .drawBehind {
                // Dot grid: 1px dots spaced 16dp apart
                val spacing = 16.dp.toPx()
                val dotColor = Color(0xFF2A1F14)
                var x = spacing
                while (x < size.width) {
                    var y = spacing
                    while (y < size.height) {
                        drawCircle(color = dotColor, radius = 1f, center = Offset(x, y))
                        y += spacing
                    }
                    x += spacing
                }
            }
    ) {
        content()
        CornerTickOverlay(modifier = Modifier.matchParentSize())
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STAT CHIP
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StatChip(
    label: String,
    value: String,
    toneColor: Color = SkylineColors.Amber,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(1.dp, toneColor.copy(alpha = 0.33f), SkylineShape)
            .background(toneColor.copy(alpha = 0.06f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column {
            MonoLabel(text = label, fontSize = 9, color = SkylineColors.TextDim)
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = SkylineColors.TextPrimary
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TYPE FILTER CHIP
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TypeFilterChip(
    label: String,
    selected: Boolean,
    toneColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    val bg     = if (selected) toneColor else Color.Transparent
    val fg     = if (selected) MaterialTheme.colorScheme.background else toneColor
    val border = toneColor

    Box(
        modifier = modifier
            .border(1.dp, border, SkylineShape)
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { haptics.tap(); onClick() }
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        MonoLabel(text = label, fontSize = 10, color = fg)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// VIEW MODE TOGGLE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ViewModeToggle(
    isListMode: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    Row(
        modifier = modifier.border(1.dp, SkylineColors.Border, SkylineShape)
    ) {
        // Grid icon
        val gridBg = if (!isListMode) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
        Box(
            modifier = Modifier
                .background(gridBg)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { haptics.tap(); if (isListMode) onToggle() }
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.GridView, contentDescription = stringResource(R.string.grid_view), tint = if (!isListMode) SkylineColors.Amber else SkylineColors.TextDim, modifier = Modifier.size(18.dp))
        }
        // Divider
        Box(modifier = Modifier.width(1.dp).height(34.dp).background(SkylineColors.Border))
        // List icon
        val listBg = if (isListMode) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
        Box(
            modifier = Modifier
                .background(listBg)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { haptics.tap(); if (!isListMode) onToggle() }
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ViewList, contentDescription = stringResource(R.string.list_view), tint = if (isListMode) SkylineColors.Amber else SkylineColors.TextDim, modifier = Modifier.size(18.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// OFFSET FAB
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Square FAB with hard amber offset shadow drawn via drawBehind (no blur).
 * The "+" is drawn as two Canvas line strokes.
 */
@Composable
fun OffsetFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    cornerRoundness: Float = 0f,
    width: androidx.compose.ui.unit.Dp? = 48.dp,
    content: (@Composable BoxScope.() -> Unit)? = null
) {
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    val shadowColor = SkylineColors.AmberDim
    val shape = getDynamicCornerShape(16f, cornerRoundness)

    val sizeModifier = if (width != null) Modifier.size(width, 48.dp) else Modifier.height(48.dp).wrapContentWidth()

    Box(
        modifier = modifier
            .then(sizeModifier)
            .background(SkylineColors.Amber, shape)
            .border(1.dp, SkylineColors.AmberDim, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { haptics.fab(); onClick() }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (content != null) {
            content()
        } else {
            // Default: draw "+" via Canvas
            Canvas(modifier = Modifier.size(20.dp)) {
                val cx = size.width / 2
                val cy = size.height / 2
                val half = size.width * 0.4f
                drawLine(Color(0xFF161009), Offset(cx - half, cy), Offset(cx + half, cy), strokeWidth = 2.dp.toPx())
                drawLine(Color(0xFF161009), Offset(cx, cy - half), Offset(cx, cy + half), strokeWidth = 2.dp.toPx())
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SKYLINE TOP BAR
// ─────────────────────────────────────────────────────────────────────────────

fun getFolderAccent(folderName: String): Color {
    val name = folderName.lowercase()
    return when {
        name == "android" -> SkylineColors.AccentGreen
        name == "dcim" || name == "camera" -> SkylineColors.AccentBlue
        name == "documents" || name == "docs" -> SkylineColors.AccentTeal
        name == "download" || name == "downloads" -> SkylineColors.AccentPrimary
        name == "movies" || name == "video" || name == "videos" -> SkylineColors.AccentRed
        name == "pictures" || name == "images" -> SkylineColors.AccentPink
        name == "audio" || name == "ringtones" || name == "music" -> SkylineColors.AccentViolet
        else -> SkylineColors.Dust
    }
}

@Composable
fun SkylineTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onMenuClick: () -> Unit,
    onTrashClick: () -> Unit,
    onOrganiseClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRoundness: Float = 0f,
    organiseProgress: Float? = null
) {
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    val shape = getDynamicCornerShape(14f, cornerRoundness)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.statusBars)
            .padding(horizontal = 12.dp)
            .padding(top = 8.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hamburger button — hard square
        Box(
            modifier = Modifier
                .size(44.dp)
                .border(1.dp, SkylineColors.Border, shape)
                .background(MaterialTheme.colorScheme.surface, shape)
                .clip(shape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { haptics.tap(); onMenuClick() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.menu_content_desc), tint = SkylineColors.TextPrimary, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.width(8.dp))

        // Search field — 1dp border, 0dp radius, mono placeholder
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    "Search files & folders",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = SkylineColors.TextDim2,
                    maxLines = 1
                )
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = SkylineColors.TextPrimary, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_content_desc), tint = SkylineColors.TextDim, modifier = Modifier.size(16.dp))
                    }
                } else if (organiseProgress != null) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
                        CircularProgressIndicator(
                            progress = { organiseProgress },
                            color = SkylineColors.TextPrimary,
                            trackColor = SkylineColors.TextPrimary.copy(alpha = 0.2f),
                            strokeWidth = 2.dp,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    IconButton(onClick = onOrganiseClick) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = stringResource(R.string.organise_files), tint = SkylineColors.TextPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            },
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .border(1.dp, SkylineColors.Border, shape),
            shape = shape,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor   = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor   = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                cursorColor             = SkylineColors.Amber
            ),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = JetBrainsMonoFamily,
                fontSize   = 12.sp,
                color      = SkylineColors.TextPrimary
            )
        )

        Spacer(Modifier.width(8.dp))

        // Trash icon button
        Box(
            modifier = Modifier
                .size(44.dp)
                .border(1.dp, SkylineColors.Border, shape)
                .background(MaterialTheme.colorScheme.surface, shape)
                .clip(shape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { haptics.tap(); onTrashClick() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.trash), tint = SkylineColors.TextDim, modifier = Modifier.size(20.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SKYLINE FOLDER GRID TILE
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SkylineFolderGridTile(
    name: String,
    type: String,
    itemCountOrMeta: String,
    date: String,
    sizeBytes: Long,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRoundness: Float = 0f,
    onPinClick: (() -> Unit)? = null,
    onInfoClick: (() -> Unit)? = null,
    isPinned: Boolean = false,
    isLocked: Boolean = false,
    onLockClick: (() -> Unit)? = null,
    onUnlockClick: (() -> Unit)? = null,
    isRecentGlow: Boolean = false
) {
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    val shape = getDynamicCornerShape(16f, cornerRoundness)
    val baseTone = fileTypeTone(type)
    val toneColor = when {
        type == "folder" -> getFolderAccent(name)
        else -> {
            when {
                sizeBytes > 1024L * 1024L * 1024L -> SkylineColors.Rust
                sizeBytes > 100L * 1024L * 1024L -> SkylineColors.Amber
                sizeBytes > 0L -> SkylineColors.Sage
                else -> baseTone
            }
        }
    }
    val typeCode  = fileTypeCode(type)

    val borderColor = if (isRecentGlow && type == "folder") {
        toneColor.copy(alpha = 0.55f) // Glow border
    } else if (isSelected) {
        SkylineColors.Amber 
    } else {
        SkylineColors.Border
    }
    val borderWidth = if (isSelected) 2.dp else 1.dp
    
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val glowMod = if (isRecentGlow && type == "folder") {
        Modifier.shadow(24.dp, shape, spotColor = toneColor.copy(alpha = 0.6f), ambientColor = toneColor.copy(alpha = 0.6f))
    } else if (!isDark) {
        Modifier.shadow(3.dp, shape, spotColor = Color(0x33000000))
    } else Modifier

    Box(modifier = modifier.padding(top = if (type == "folder") 9.dp else 0.dp)) {
        if (type == "folder") {
            Box(
                modifier = Modifier
                    .offset(x = 16.dp, y = (-9).dp)
                    .size(34.dp, 9.dp)
                    .background(toneColor, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
            )
        }

        Column(
            modifier = Modifier
                .defaultMinSize(minHeight = 110.dp)
                .then(glowMod)
                .border(borderWidth, borderColor, shape)
                .background(MaterialTheme.colorScheme.surface, shape)
                .clip(shape)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { haptics.tap(); onClick() },
                    onLongClick = onLongClick
                )
        ) {
            if (type != "folder") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(toneColor)
                )
            }

            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                // Top row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val iconVector = when (type) {
                        "folder" -> Icons.Outlined.Folder
                        "image" -> Icons.Outlined.Image
                        "video" -> Icons.Outlined.OndemandVideo
                        "audio" -> Icons.Outlined.AudioFile
                        else -> Icons.Outlined.InsertDriveFile
                    }
                    
                    if (type == "folder") {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(toneColor.copy(alpha = 0.2f), RoundedCornerShape(11.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = iconVector, contentDescription = null, tint = toneColor, modifier = Modifier.size(18.dp))
                        }
                    } else {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = toneColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (isPinned) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = stringResource(R.string.pinned_badge),
                            tint = SkylineColors.Amber,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    if (isLocked) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = stringResource(R.string.locked_badge),
                            tint = SkylineColors.Rust,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    
                    if (type != "folder") {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .border(1.dp, toneColor.copy(alpha = 0.5f), getDynamicCornerShape(4f, cornerRoundness))
                                .background(toneColor.copy(alpha = 0.12f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = typeCode,
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 8.sp,
                                letterSpacing = 1.sp,
                                color = toneColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (isSelected) {
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SkylineColors.Amber, modifier = Modifier.size(14.dp))
                    } else {
                        Spacer(Modifier.weight(1f))
                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options), tint = SkylineColors.TextDim, modifier = Modifier.size(16.dp))
                            }
                            if (showMenu) {
                                androidx.compose.ui.window.Popup(alignment = androidx.compose.ui.Alignment.TopEnd, onDismissRequest = { showMenu = false }, properties = androidx.compose.ui.window.PopupProperties(focusable = true)) {
                                    Surface(
                                        shape = getDynamicCornerShape(12f, cornerRoundness),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, SkylineColors.Border),
                                        shadowElevation = 4.dp
                                    ) {
                                        Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                            IconButton(onClick = { showMenu = false; onPinClick?.invoke() }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.PushPin, contentDescription = stringResource(R.string.pin_file), tint = SkylineColors.Amber) }
                                            if (onLockClick != null || onUnlockClick != null) {
                                                IconButton(onClick = { showMenu = false; if (isLocked) onUnlockClick?.invoke() else onLockClick?.invoke() }, modifier = Modifier.size(36.dp)) {
                                                    Icon(if (isLocked) Icons.Outlined.LockOpen else Icons.Outlined.Lock, contentDescription = if (isLocked) stringResource(R.string.unlock_file) else stringResource(R.string.lock_file), tint = SkylineColors.Amber)
                                                }
                                            }
                                            IconButton(onClick = { showMenu = false; onInfoClick?.invoke() }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Info, contentDescription = stringResource(R.string.app_info), tint = SkylineColors.Amber) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // File name
                Text(
                    text = name,
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = SkylineColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )

                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(6.dp))

                // Bottom meta row
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    if (type == "folder") {
                        // Count pill
                        Box(
                            modifier = Modifier
                                .background(toneColor.copy(alpha = 0.26f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = itemCountOrMeta.substringBefore(" "), // extracts the number
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                color = toneColor
                            )
                        }
                        Text(
                            text = date.uppercase(),
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 10.sp,
                            color = SkylineColors.TextDim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Text(
                            text = "$itemCountOrMeta · $date".uppercase(),
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 10.sp,
                            color = SkylineColors.TextDim,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SKYLINE FOLDER LIST ROW
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SkylineFolderListRow(
    name: String,
    type: String,
    subline: String,        // "DATE · TYPE" string
    trailingMeta: String,   // item count or size
    sizeBytes: Long,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRoundness: Float = 0f,
    onPinClick: (() -> Unit)? = null,
    onInfoClick: (() -> Unit)? = null,
    isPinned: Boolean = false,
    isLocked: Boolean = false,
    onLockClick: (() -> Unit)? = null,
    onUnlockClick: (() -> Unit)? = null,
    isRecentGlow: Boolean = false
) {
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    val shape = getDynamicCornerShape(16f, cornerRoundness)
    val baseTone = fileTypeTone(type)
    val toneColor = when {
        type == "folder" -> getFolderAccent(name)
        else -> {
            when {
                sizeBytes > 1024L * 1024L * 1024L -> SkylineColors.Rust
                sizeBytes > 100L * 1024L * 1024L -> SkylineColors.Amber
                sizeBytes > 0L -> SkylineColors.Sage
                else -> baseTone
            }
        }
    }
    val typeCode  = fileTypeCode(type)
    val borderColor = if (isRecentGlow && type == "folder") {
        toneColor.copy(alpha = 0.55f)
    } else if (isSelected) {
        SkylineColors.Amber 
    } else {
        SkylineColors.Border
    }
    
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val glowMod = if (isRecentGlow && type == "folder") {
        Modifier.shadow(24.dp, shape, spotColor = toneColor.copy(alpha = 0.6f), ambientColor = toneColor.copy(alpha = 0.6f))
    } else if (!isDark) {
        Modifier.shadow(3.dp, shape, spotColor = Color(0x33000000))
    } else Modifier

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .then(glowMod)
            .border(1.dp, borderColor, shape)
            .background(if (isSelected) SkylineColors.Amber.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface, shape)
            .clip(shape)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { haptics.tap(); onClick() },
                onLongClick = onLongClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (type != "folder") {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(64.dp)
                    .background(toneColor)
            )
        }

        Spacer(Modifier.width(12.dp))

        val iconVector = when (type) {
            "folder" -> Icons.Outlined.Folder
            "image" -> Icons.Outlined.Image
            "video" -> Icons.Outlined.OndemandVideo
            "audio" -> Icons.Outlined.AudioFile
            else -> Icons.Outlined.InsertDriveFile
        }
        
        if (type == "folder") {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(toneColor.copy(alpha = 0.2f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = iconVector, contentDescription = null, tint = toneColor, modifier = Modifier.size(18.dp))
            }
        }
        
        Spacer(Modifier.width(12.dp))

        if (isPinned) {
            Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = stringResource(R.string.pinned_badge),
                tint = SkylineColors.Amber,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        if (isLocked) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = stringResource(R.string.locked_badge),
                tint = SkylineColors.Rust,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
        }

        Column(modifier = Modifier.weight(1f).padding(vertical = 10.dp)) {
            Text(
                text = name,
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = SkylineColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (type == "folder") {
                    Box(
                        modifier = Modifier
                            .background(toneColor.copy(alpha = 0.26f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = trailingMeta.substringBefore(" "),
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            color = toneColor
                        )
                    }
                }
                Text(
                    text = if (type == "folder") "items · ${subline.substringBefore(" ·")}".uppercase() else subline.uppercase(),
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 9.sp,
                    letterSpacing = 0.8.sp,
                    color = SkylineColors.TextDim
                )
            }
        }
        
        if (type != "folder") {
            Text(
                text = trailingMeta,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp,
                color = SkylineColors.TextDim,
                modifier = Modifier.padding(end = 4.dp),
                textAlign = TextAlign.End
            )
        }

        if (isSelected) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SkylineColors.Amber, modifier = Modifier.size(16.dp).padding(end = 12.dp))
        } else {
            var showMenu by remember { mutableStateOf(false) }
            Box(modifier = Modifier.padding(end = 6.dp)) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options), tint = SkylineColors.TextDim, modifier = Modifier.size(18.dp))
                }
                if (showMenu) {
                    androidx.compose.ui.window.Popup(alignment = androidx.compose.ui.Alignment.TopEnd, onDismissRequest = { showMenu = false }, properties = androidx.compose.ui.window.PopupProperties(focusable = true)) {
                        Surface(
                            shape = getDynamicCornerShape(12f, cornerRoundness),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SkylineColors.Border),
                            shadowElevation = 4.dp
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                IconButton(onClick = { showMenu = false; onPinClick?.invoke() }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.PushPin, contentDescription = stringResource(R.string.pin_file), tint = SkylineColors.Amber) }
                                if (onLockClick != null || onUnlockClick != null) {
                                    IconButton(onClick = { showMenu = false; if (isLocked) onUnlockClick?.invoke() else onLockClick?.invoke() }, modifier = Modifier.size(36.dp)) {
                                        Icon(if (isLocked) Icons.Outlined.LockOpen else Icons.Outlined.Lock, contentDescription = if (isLocked) stringResource(R.string.unlock_file) else stringResource(R.string.lock_file), tint = SkylineColors.Amber)
                                    }
                                }
                                IconButton(onClick = { showMenu = false; onInfoClick?.invoke() }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Info, contentDescription = stringResource(R.string.app_info), tint = SkylineColors.Amber) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// (Bottom Nav removed in favor of BottomNavBar.kt)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ExpandingPillNav(
    selected: com.ripple.filemanager.NavTab,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onTabSelected: (com.ripple.filemanager.NavTab) -> Unit,
    onNewFile: () -> Unit,
    onNewFolder: () -> Unit,
    cornerRoundness: Float = 0f,
    modifier: Modifier = Modifier
) {
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    val pillColor = SkylineColors.Amber
    val pillBgExpanded = SkylineColors.Surface
    val strokeColor = SkylineColors.Border
    val iconActiveColor = SkylineColors.Background
    val iconInactiveColor = SkylineColors.TextDim
    val shadowColor = SkylineColors.Border

    val currentTabIcon = when(selected) {
        com.ripple.filemanager.NavTab.HOME -> Icons.Default.Home
        com.ripple.filemanager.NavTab.RECENT -> Icons.Default.Schedule
        com.ripple.filemanager.NavTab.PINNED -> Icons.Default.PushPin
        com.ripple.filemanager.NavTab.CLOUD -> Icons.Default.Cloud
        else -> Icons.Default.Home
    }

    Box(modifier = modifier) {
        val pillShape = getDynamicCornerShape(25f, cornerRoundness)
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .background(shadowColor, pillShape)
                .zIndex(-1f)
        )
        
        Row(
            modifier = Modifier
                .height(50.dp)
                .background(if (expanded) pillBgExpanded else pillColor, pillShape)
                .then(if (expanded) Modifier.border(1.dp, strokeColor, pillShape) else Modifier)
                .clip(pillShape)
                .animateContentSize(animationSpec = spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessLow))
                .padding(start = if (expanded) 8.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            val tabs = listOf(
                com.ripple.filemanager.NavTab.HOME to Icons.Default.Home,
                com.ripple.filemanager.NavTab.RECENT to Icons.Default.Schedule,
                com.ripple.filemanager.NavTab.PINNED to Icons.Default.PushPin,
                com.ripple.filemanager.NavTab.CLOUD to Icons.Default.Cloud
            )
            
            tabs.forEachIndexed { index, (tab, icon) ->
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn(tween(200, delayMillis = 60 + (index * 25))),
                    exit = fadeOut(tween(50))
                ) {
                    val isActive = selected == tab
                    val activeShape = getDynamicCornerShape(17f, cornerRoundness)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(if (isActive) pillColor else Color.Transparent, activeShape)
                            .clip(activeShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                haptics.tap()
                                onTabSelected(tab)
                                onExpandedChange(false)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = if (isActive) iconActiveColor else iconInactiveColor, modifier = Modifier.size(14.dp))
                    }
                }
            }
            
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(200, delayMillis = 60 + (4 * 25))),
                exit = fadeOut(tween(50))
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .width(1.dp)
                        .height(22.dp)
                        .background(strokeColor)
                )
            }
            
            val actions = listOf(
                Icons.Outlined.InsertDriveFile to onNewFile,
                Icons.Default.CreateNewFolder to onNewFolder
            )
            actions.forEachIndexed { index, (icon, onClick) ->
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn(tween(200, delayMillis = 60 + ((5 + index) * 25))),
                    exit = fadeOut(tween(50))
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                haptics.fab()
                                onClick()
                                onExpandedChange(false)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = iconInactiveColor, modifier = Modifier.size(14.dp))
                    }
                }
            }
            
            if (expanded) Spacer(modifier = Modifier.width(8.dp))
            
            val rotation by animateFloatAsState(
                targetValue = if (expanded) 45f else 0f, 
                animationSpec = tween(200)
            )
            val toggleShape = getDynamicCornerShape(25f, cornerRoundness)
            Box(
                modifier = Modifier
                    .size(if (expanded) 48.dp else 50.dp)
                    .background(if (expanded) Color.Transparent else pillColor, toggleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() }, indication = null
                    ) { haptics.fab(); onExpandedChange(!expanded) },
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.graphicsLayer(rotationZ = rotation)) {
                    AnimatedContent(
                        targetState = expanded,
                        transitionSpec = {
                            (fadeIn(tween(160, delayMillis = 60)) + scaleIn(initialScale = 0.6f, animationSpec = tween(200, delayMillis = 60))) togetherWith
                            (fadeOut(tween(160)) + scaleOut(targetScale = 0.6f, animationSpec = tween(200)))
                        },
                        label = "toggle_icon"
                    ) { isExp ->
                        if (isExp) {
                            Icon(Icons.Default.Add, contentDescription = "Close", tint = pillColor, modifier = Modifier.size(16.dp))
                        } else {
                            Icon(currentTabIcon, contentDescription = "Current Tab", tint = iconActiveColor, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                
                if (!expanded && selected != com.ripple.filemanager.NavTab.HOME) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .size(6.dp)
                            .background(iconActiveColor.copy(alpha = 0.7f))
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Immutable
data class StorageSourceUi(
    val key: String,
    val displayName: String,
    val freeLabel: String?,
    val totalLabel: String?,
    val usedPercent: Int?,
    val barColor: Color
)

@Composable
fun ConnectedStorageCard(
    sources: List<StorageSourceUi>,
    cornerRoundness: Float = 0f,
    modifier: Modifier = Modifier
) {
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    var expanded by remember { mutableStateOf(false) }
    
    val borderColor = SkylineColors.Border
    val bgColor = SkylineColors.Surface
    
    // Fallback if empty (shouldn't happen on Home screen, but just in case)
    if (sources.isEmpty()) return
    
    val cardShape = getDynamicCornerShape(14f, cornerRoundness)
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor, cardShape)
            .border(1.dp, borderColor, cardShape)
            .clip(cardShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { haptics.tap(); expanded = !expanded }
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "STORAGE · ${sources.size} CONNECTED",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 9.sp,
                    letterSpacing = 0.8.sp,
                    color = SkylineColors.TextDim
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = sources.joinToString(" · ") { it.displayName },
                    fontFamily = FrauncesFontFamily,
                    fontSize = 15.sp,
                    color = SkylineColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            val rotation by animateFloatAsState(
                targetValue = if (expanded) 180f else 0f, 
                animationSpec = tween(120),
                label = "chevron_rotation"
            )
            
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Expand",
                tint = SkylineColors.TextPrimary,
                modifier = Modifier.size(24.dp).graphicsLayer(rotationZ = rotation)
            )
        }
        
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(320, easing = FastOutSlowInEasing)) + fadeIn(tween(320)),
            exit = shrinkVertically(tween(320, easing = FastOutSlowInEasing)) + fadeOut(tween(320))
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .height(1.dp)
                        .background(SkylineColors.Border)
                )
                
                sources.forEach { source ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = source.displayName,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 10.sp,
                            color = SkylineColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(84.dp)
                        )
                        
                        Box(
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (source.usedPercent != null) {
                                Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(SkylineColors.TextDim.copy(alpha = 0.2f)))
                                Box(modifier = Modifier.fillMaxWidth(source.usedPercent / 100f).height(6.dp).background(source.barColor))
                            }
                        }
                        
                        Text(
                            text = if (source.freeLabel != null && source.totalLabel != null) "${source.freeLabel} / ${source.totalLabel}" else "not synced",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 10.sp,
                            color = SkylineColors.TextDim,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            modifier = Modifier.width(120.dp)
                        )
                    }
                }
            }
        }
    }
}
