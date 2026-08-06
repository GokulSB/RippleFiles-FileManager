package com.ripple.filemanager.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripple.filemanager.ui.theme.FrauncesFontFamily
import com.ripple.filemanager.ui.theme.JetBrainsMonoFamily
import com.ripple.filemanager.ui.theme.ManropeFontFamily
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
 * Uppercase, JetBrains Mono, amber-dim — used for all section headers and metadata.
 */
@Composable
fun MonoLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = SkylineColors.AmberDim,
    fontSize: Int = 10
) {
    Text(
        text = text.uppercase(),
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = fontSize.sp,
        letterSpacing = 1.5.sp,
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
            MonoLabel(text = label, fontSize = 9, color = SkylineColors.AmberDim)
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
                onClick = onClick
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
                    onClick = { if (isListMode) onToggle() }
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.GridView, contentDescription = "Grid", tint = if (!isListMode) SkylineColors.Amber else SkylineColors.TextDim, modifier = Modifier.size(18.dp))
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
                    onClick = { if (!isListMode) onToggle() }
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ViewList, contentDescription = "List", tint = if (isListMode) SkylineColors.Amber else SkylineColors.TextDim, modifier = Modifier.size(18.dp))
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
    val shadowColor = SkylineColors.AmberDim
    val shape = getDynamicCornerShape(16f, cornerRoundness)

    val sizeModifier = if (width != null) Modifier.size(width, 48.dp) else Modifier.height(48.dp).wrapContentWidth()

    Box(
        modifier = modifier
            .then(sizeModifier)
            .drawBehind {
                // Hard offset shadow (3dp x, 3dp y, 0 blur)
                drawRect(
                    color = shadowColor,
                    topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
                    size = size,
                    // If we want rounded shadow, drawRoundRect can be used, but since it's hard offset, we'll keep hard edge for now or use shape
                )
            }
            .background(SkylineColors.Amber, shape)
            .border(1.dp, SkylineColors.AmberDim, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
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

@Composable
fun SkylineTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onMenuClick: () -> Unit,
    onTrashClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRoundness: Float = 0f
) {
    val shape = getDynamicCornerShape(12f, cornerRoundness)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .background(MaterialTheme.colorScheme.background)
            .border(width = 0.dp, color = Color.Transparent)  // no top border on outermost
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
                    onClick = onMenuClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = SkylineColors.TextPrimary, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.width(8.dp))

        // Search field — 1dp border, 0dp radius, mono placeholder
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    "SEARCH FILES & FOLDERS",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = SkylineColors.TextDim2,
                    maxLines = 1
                )
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = SkylineColors.AmberDim, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = SkylineColors.TextDim, modifier = Modifier.size(16.dp))
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
                    onClick = onTrashClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Trash", tint = SkylineColors.TextDim, modifier = Modifier.size(20.dp))
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
    isPinned: Boolean = false
) {
    val shape = getDynamicCornerShape(16f, cornerRoundness)
    val baseTone = fileTypeTone(type)
    val toneColor = when {
        type == "folder" -> {
            val count = itemCountOrMeta.substringBefore(" ").toIntOrNull() ?: 0
            when {
                count > 50 -> SkylineColors.Rust // Red-ish for >50 items
                count > 10 -> SkylineColors.Amber // Yellow-ish for >10 items
                else -> SkylineColors.Sage // Green-ish for small folders
            }
        }
        else -> {
            when {
                sizeBytes > 1024L * 1024L * 1024L -> SkylineColors.Rust // Red-ish for Large (>1GB)
                sizeBytes > 100L * 1024L * 1024L -> SkylineColors.Amber // Yellow-ish for Medium (>100MB)
                sizeBytes > 0L -> SkylineColors.Sage // Green-ish for Small (>0B)
                else -> baseTone // Default fallback
            }
        }
    }
    val typeCode  = fileTypeCode(type)

    val borderColor = if (isSelected) SkylineColors.Amber else SkylineColors.Border
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 110.dp)
            .border(borderWidth, borderColor, shape)
            .background(MaterialTheme.colorScheme.surface, shape)
            .clip(shape)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        // 4dp color strip at the top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(toneColor)
        )

        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            // Top row: outline icon + type-code badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                val iconVector = when (type) {
                    "folder" -> Icons.Outlined.Folder
                    "image" -> Icons.Outlined.Image
                    "video" -> Icons.Outlined.OndemandVideo
                    "audio" -> Icons.Outlined.AudioFile
                    else -> Icons.Outlined.InsertDriveFile
                }
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = toneColor,
                    modifier = Modifier.size(16.dp)
                )
                if (isPinned) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Pinned",
                        tint = SkylineColors.Amber,
                        modifier = Modifier.size(12.dp)
                    )
                }
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
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = SkylineColors.TextDim, modifier = Modifier.size(16.dp))
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, SkylineColors.Border, getDynamicCornerShape(12f, cornerRoundness))
                                .clip(getDynamicCornerShape(12f, cornerRoundness))
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Pin", fontFamily = ManropeFontFamily, color = SkylineColors.TextPrimary) },
                                onClick = { showMenu = false; onPinClick?.invoke() },
                                leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null, tint = SkylineColors.Amber) }
                            )
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("App info", fontFamily = ManropeFontFamily, color = SkylineColors.TextPrimary) },
                                onClick = { showMenu = false; onInfoClick?.invoke() },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = SkylineColors.Amber) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // File name
            Text(
                text = name,
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = SkylineColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp
            )

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(6.dp))

            // Bottom meta row: items · date, single line to prevent wrap bug
            Text(
                text = "$itemCountOrMeta · $date".uppercase(),
                fontFamily = JetBrainsMonoFamily,
                fontSize = 9.sp,
                color = SkylineColors.TextDim,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
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
    isPinned: Boolean = false
) {
    val shape = getDynamicCornerShape(16f, cornerRoundness)
    val baseTone = fileTypeTone(type)
    val toneColor = when {
        type == "folder" -> {
            val count = trailingMeta.substringBefore(" ").toIntOrNull() ?: 0
            when {
                count > 50 -> SkylineColors.Rust // Red-ish for >50 items
                count > 10 -> SkylineColors.Amber // Yellow-ish for >10 items
                else -> SkylineColors.Sage // Green-ish for small folders
            }
        }
        else -> {
            when {
                sizeBytes > 1024L * 1024L * 1024L -> SkylineColors.Rust // Red-ish for Large (>1GB)
                sizeBytes > 100L * 1024L * 1024L -> SkylineColors.Amber // Yellow-ish for Medium (>100MB)
                sizeBytes > 0L -> SkylineColors.Sage // Green-ish for Small (>0B)
                else -> baseTone // Default fallback
            }
        }
    }
    val borderColor = if (isSelected) SkylineColors.Amber else SkylineColors.Border

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .border(1.dp, borderColor, shape)
            .background(if (isSelected) SkylineColors.Amber.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface, shape)
            .clip(shape)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 5dp left-edge color bar
        Box(
            modifier = Modifier
                .width(5.dp)
                .height(64.dp)
                .background(toneColor)
        )

        Spacer(Modifier.width(12.dp))

        if (isPinned) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Pinned",
                tint = SkylineColors.Amber,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
        }

        Column(modifier = Modifier.weight(1f).padding(vertical = 10.dp)) {
            Text(
                text = name,
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = SkylineColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subline.uppercase(),
                fontFamily = JetBrainsMonoFamily,
                fontSize = 9.sp,
                letterSpacing = 0.8.sp,
                color = SkylineColors.TextDim
            )
        }

        Text(
            text = trailingMeta,
            fontFamily = JetBrainsMonoFamily,
            fontSize = 10.sp,
            letterSpacing = 0.5.sp,
            color = SkylineColors.TextDim,
            modifier = Modifier.padding(end = 4.dp),
            textAlign = TextAlign.End
        )

        if (isSelected) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SkylineColors.Amber, modifier = Modifier.size(16.dp).padding(end = 12.dp))
        } else {
            var showMenu by remember { mutableStateOf(false) }
            Box(modifier = Modifier.padding(end = 6.dp)) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = SkylineColors.TextDim, modifier = Modifier.size(18.dp))
                }
                androidx.compose.material3.DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, SkylineColors.Border, getDynamicCornerShape(12f, cornerRoundness))
                        .clip(getDynamicCornerShape(12f, cornerRoundness))
                ) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Pin", fontFamily = ManropeFontFamily, color = SkylineColors.TextPrimary) },
                        onClick = { showMenu = false; onPinClick?.invoke() },
                        leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null, tint = SkylineColors.Amber) }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("App info", fontFamily = ManropeFontFamily, color = SkylineColors.TextPrimary) },
                        onClick = { showMenu = false; onInfoClick?.invoke() },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = SkylineColors.Amber) }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// (Bottom Nav removed in favor of BottomNavBar.kt)
// ─────────────────────────────────────────────────────────────────────────────
