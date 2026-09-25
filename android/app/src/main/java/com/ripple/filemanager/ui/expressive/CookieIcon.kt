package com.ripple.filemanager.ui.expressive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Icon inside a cookie-shaped container filled with a category pastel.
 * Glyph is ~46% of container size with dark on-color.
 */
@Composable
fun CookieIcon(
    icon: ImageVector,
    bgColor: Color,
    modifier: Modifier = Modifier,
    iconTint: Color = ExpressiveTokens.OnPastel,
    size: Dp = 44.dp,
    lobes: Int = 8
) {
    val shape = remember(lobes) { CookieShape(lobes = lobes) }
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(size * 0.48f)
        )
    }
}

/**
 * Clickable cookie icon button with accessibility description.
 */
@Composable
fun CookieIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    bgColor: Color,
    description: String,
    modifier: Modifier = Modifier,
    iconTint: Color = ExpressiveTokens.OnPastel,
    size: Dp = 44.dp,
    iconSize: Dp = size * 0.48f,
    lobes: Int = 8
) {
    val shape = remember(lobes) { CookieShape(lobes = lobes) }
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(size)
            .pressScale(pressedScale = 0.94f, interactionSource = interactionSource)
            .clip(shape)
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}
