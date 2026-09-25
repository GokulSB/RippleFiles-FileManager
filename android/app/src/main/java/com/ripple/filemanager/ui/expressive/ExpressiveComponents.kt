package com.ripple.filemanager.ui.expressive

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.ripple.filemanager.ui.theme.OutfitFontFamily
import com.ripple.filemanager.ui.theme.LocalAppFont
import com.ripple.filemanager.FileItem

// ═══════════════════════════════════════════════════════════════════════
// SECTION TITLE + CONTAINER
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = ExpressiveTheme.colors.text
) {
    Text(
        text = title,
        fontSize = 22.sp,
        fontWeight = FontWeight.Normal,
        fontFamily = LocalAppFont.current,
        color = color,
        modifier = modifier
    )
}

val LocalCornerRoundness = compositionLocalOf { 0.5f }

@Composable
fun SectionContainer(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = (34f * (LocalCornerRoundness.current * 2).coerceIn(0f, 2f)).dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = ExpressiveTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                8.dp,
                RoundedCornerShape(cornerRadius),
                ambientColor = colors.bg,
                spotColor = colors.shadow
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(colors.container)
            .border(
                1.dp,
                colors.line.copy(alpha = colors.lineAlpha),
                RoundedCornerShape(cornerRadius)
            )
            .padding(12.dp),
        content = content
    )
}

@Composable
fun InnerCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = (26f * (LocalCornerRoundness.current * 2).coerceIn(0f, 2f)).dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val colors = ExpressiveTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(colors.card)
            .padding(contentPadding),
        content = content
    )
}

// ═══════════════════════════════════════════════════════════════════════
// HEADERS
// ═══════════════════════════════════════════════════════════════════════

/**
 * Shared Expressive Screen Header used across Home, Browse, Cloud, Send, Bin, Settings, and About.
 * Features:
 * - Status-bar aware padding
 * - Left: 46dp 8-lobed cookie icon button (hamburger menu or back arrow)
 * - Center: Large expressive title (~42sp base, Outfit ExtraLight 200, letterSpacing -0.6sp) with adaptive font sizing (prevents truncation on long titles like "Send & Receive")
 *           + 15sp subtitle (Outfit Light 300) underneath
 * - Right: 46dp 8-lobed cookie icon button (trash or action) or custom action slot
 * - Optional 56dp search bar pill with AI sparkle button (38dp)
 */
@Composable
fun ExpressiveScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    leftIcon: ImageVector = Icons.Default.Menu,
    onLeftClick: () -> Unit = {},
    leftDescription: String = "Open menu",
    rightIcon: ImageVector? = Icons.Outlined.Delete,
    onRightClick: () -> Unit = {},
    rightDescription: String = "Open trash",
    rightAction: (@Composable () -> Unit)? = null,
    showSearch: Boolean = false,
    query: String = "",
    onQueryChange: (String) -> Unit = {},
    searchPlaceholder: String = "Search files & folders...",
    onAiSearchClick: (() -> Unit)? = null
) {
    val colors = ExpressiveTheme.colors
    val titleFontSize = when {
        rightAction != null -> when {
            title.length > 14 -> 17.sp
            title.length > 10 -> 19.sp
            title.length > 7 -> 21.sp
            else -> 23.sp
        }
        title.length > 16 -> 24.sp
        title.length > 12 -> 28.sp
        title.length > 8 -> 32.sp
        else -> 36.sp
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        // Top row: Left cookie, Centered Title + Subtitle, Right cookie / actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CookieIconButton(
                onClick = onLeftClick,
                icon = leftIcon,
                bgColor = colors.card,
                description = leftDescription,
                iconTint = colors.text,
                size = if (rightAction != null) 40.dp else 46.dp,
                lobes = 8
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = titleFontSize,
                    fontWeight = if (rightAction != null) FontWeight.Normal else FontWeight.ExtraLight,
                    fontFamily = LocalAppFont.current,
                    color = colors.text,
                    letterSpacing = if (rightAction != null) (-0.4).sp else (-0.6).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = if (rightAction != null) 12.sp else 14.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = LocalAppFont.current,
                        color = colors.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (rightAction != null) {
                rightAction()
            } else if (rightIcon != null) {
                CookieIconButton(
                    onClick = onRightClick,
                    icon = rightIcon,
                    bgColor = colors.card,
                    description = rightDescription,
                    iconTint = colors.text,
                    size = 46.dp,
                    lobes = 8
                )
            } else {
                Spacer(modifier = Modifier.size(46.dp))
            }
        }

        if (showSearch) {
            Spacer(modifier = Modifier.height(8.dp))

            // 56dp Search Pill with AI search sparkle cookie
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(colors.insetCard)
                    .border(1.dp, colors.line.copy(alpha = colors.lineAlpha), RoundedCornerShape(28.dp))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = colors.muted,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))

                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = colors.text,
                        fontWeight = FontWeight.Normal,
                        fontFamily = LocalAppFont.current
                    ),
                    cursorBrush = SolidColor(colors.accent),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text(
                                text = searchPlaceholder,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                fontFamily = LocalAppFont.current,
                                color = colors.muted.copy(alpha = 0.7f)
                            )
                        }
                        innerTextField()
                    }
                )

                if (onAiSearchClick != null) {
                    CookieIconButton(
                        onClick = onAiSearchClick,
                        icon = Icons.Default.AutoAwesome,
                        bgColor = colors.card,
                        description = "AI Search / Organise",
                        iconTint = colors.text,
                        size = 38.dp,
                        lobes = 8
                    )
                }
            }
        }
    }
}

@Composable
fun ExpressiveHeader(
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier,
    leftAction: (@Composable () -> Unit)? = null,
    rightAction: (@Composable () -> Unit)? = null
) {
    val colors = ExpressiveTheme.colors
    ExpressiveScreenHeader(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        leftIcon = Icons.Default.Menu,
        rightAction = rightAction,
        rightIcon = if (rightAction == null) Icons.Outlined.Delete else null,
        showSearch = false
    )
}

@Composable
fun PageHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    ExpressiveScreenHeader(
        title = title,
        subtitle = subtitle,
        modifier = modifier.padding(horizontal = 12.dp),
        leftIcon = Icons.AutoMirrored.Filled.ArrowBack,
        onLeftClick = onBack,
        leftDescription = "Back",
        rightIcon = null,
        rightAction = if (actions != null) {
            {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions
                )
            }
        } else null,
        showSearch = false
    )
}

// ═══════════════════════════════════════════════════════════════════════
// EXPRESSIVE SWITCH
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun ExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = ExpressiveTheme.colors
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "switch_knob"
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) colors.accent else colors.insetCard,
        animationSpec = tween(200),
        label = "switch_track"
    )
    val knobColor = if (checked) colors.ink else colors.muted

    Box(
        modifier = modifier
            .width(58.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .offset(x = thumbOffset)
                .clip(CircleShape)
                .background(knobColor)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// EXPRESSIVE SLIDER
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun ExpressiveSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0
) {
    val colors = ExpressiveTheme.colors
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        modifier = modifier.height(38.dp),
        colors = SliderDefaults.colors(
            thumbColor = colors.text,
            activeTrackColor = colors.accent,
            inactiveTrackColor = colors.insetCard
        )
    )
}

// ═══════════════════════════════════════════════════════════════════════
// EXPRESSIVE CHIP
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun ExpressiveChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconSize: androidx.compose.ui.unit.Dp = 20.dp,
    iconSpacing: androidx.compose.ui.unit.Dp = 8.dp
) {
    val colors = ExpressiveTheme.colors
    val bgColor = if (selected) colors.accent else colors.card
    val textColor = if (selected) colors.ink else colors.text

    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .height(38.dp)
            .pressScale(pressedScale = 0.95f, interactionSource = interactionSource)
            .clip(RoundedCornerShape(19.dp))
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(iconSize)
            )
            Spacer(modifier = Modifier.width(iconSpacing))
        }
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = LocalAppFont.current,
            color = textColor,
            maxLines = 1
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// BOTTOM SHEET CONTENT
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun ExpressiveSheetContent(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = ExpressiveTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.container)
            .padding(bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .padding(top = 10.dp, bottom = 8.dp)
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.muted.copy(alpha = 0.4f))
        )
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = LocalAppFont.current,
            color = colors.text,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        content()
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    return String.format(java.util.Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

fun formatFolderMeta(file: FileItem): String {
    if (file.size.contains("file") && file.size.contains("·")) return file.size
    val count = file.folderItemCount ?: try {
        val f = java.io.File(file.path)
        if (f.exists() && f.isDirectory) f.listFiles()?.count { !it.name.startsWith(".") } ?: 0 else null
    } catch (e: Exception) { null }

    val sizeBytes = if (file.sizeBytes > 0) file.sizeBytes else try {
        val f = java.io.File(file.path)
        if (f.exists() && f.isDirectory) {
            var sum = 0L
            f.walkTopDown().onEnter { !it.isHidden && it.name != "Android" }.filter { it.isFile }.take(5000).forEach { sum += it.length() }
            sum
        } else 0L
    } catch (e: Exception) { 0L }

    val countStr = if (count != null) {
        if (count == 1) "1 file" else "$count files"
    } else if (file.size.isNotEmpty() && !file.size.equals("folder", true)) file.size else "0 files"

    val sizeStr = formatFileSize(sizeBytes)
    return "$countStr · $sizeStr"
}

fun formatFileDateTime(file: FileItem): String {
    if (file.lastModified > 0) {
        val sdf = java.text.SimpleDateFormat("MMM d, yyyy · hh:mm a", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(file.lastModified))
    }
    return file.changed
}

fun formatFileMeta(file: FileItem): String {
    val dateTime = formatFileDateTime(file)
    val rawSize = file.size
    val sizeStr = if (rawSize.isNotEmpty() && !rawSize.contains("file") && !rawSize.contains("item")) rawSize else formatFileSize(file.sizeBytes)
    return if (sizeStr.isNotEmpty() && dateTime.isNotEmpty()) {
        "$sizeStr · $dateTime"
    } else {
        sizeStr.ifEmpty { dateTime }
    }
}

