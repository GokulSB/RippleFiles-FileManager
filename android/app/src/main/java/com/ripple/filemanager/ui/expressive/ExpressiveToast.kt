package com.ripple.filemanager.ui.expressive

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripple.filemanager.ui.theme.LocalAppFont

/**
 * Shared Expressive Toast / Snackbar component.
 * - Theme-matched pill shape: dark pill in dark theme, light pill in light theme.
 * - Optional leading cookie-shaped glyph for info/warning/error/success states.
 * - Sentence-case typography in Outfit font (max 2 lines).
 * - Sized to content with max width constraint.
 */
@Composable
fun ExpressiveToast(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    val colors = ExpressiveTheme.colors
    val icon = remember(message) { getToastIcon(message) }

    // Theme-matched colors: dark background in dark theme, light background in light theme
    val containerColor = colors.container
    val contentColor = colors.text

    Box(
        modifier = modifier
            .padding(horizontal = 20.dp)
            .widthIn(min = 140.dp, max = 360.dp)
            .shadow(10.dp, CircleShape, spotColor = colors.shadow)
            .clip(CircleShape)
            .background(containerColor)
            .border(1.dp, colors.line.copy(alpha = 0.2f), CircleShape)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Leading cookie icon
            if (icon != null) {
                CookieIcon(
                    icon = icon,
                    bgColor = colors.accent,
                    iconTint = colors.ink,
                    size = 28.dp,
                    lobes = 8
                )
            }

            // Message text in normal sentence case
            Text(
                text = message,
                fontFamily = LocalAppFont.current,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 17.sp,
                color = contentColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            // Optional action button
            if (actionLabel != null && onAction != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.accent.copy(alpha = 0.2f))
                        .pressScale(0.95f)
                        .clickable(onClick = onAction)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = actionLabel,
                        fontFamily = LocalAppFont.current,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent
                    )
                }
            }
        }
    }
}

/**
 * Expressive host that wraps SnackbarHost and delegates presentation to ExpressiveToast.
 * Automatically queues multiple messages sequentially without overlapping.
 */
@Composable
fun ExpressiveToastHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val reduced = ExpressiveMotion.isReducedMotion()

    SnackbarHost(
        hostState = hostState,
        modifier = modifier
    ) { data: SnackbarData ->
        AnimatedVisibility(
            visible = true,
            enter = if (reduced) fadeIn() else slideInVertically(
                initialOffsetY = { it },
                animationSpec = ExpressiveMotion.IntOffsetSpringSpec
            ) + fadeIn(ExpressiveMotion.FastTween),
            exit = if (reduced) fadeOut() else slideOutVertically(
                targetOffsetY = { it },
                animationSpec = ExpressiveMotion.IntOffsetFastTween
            ) + fadeOut(ExpressiveMotion.FadeTween)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                ExpressiveToast(
                    message = data.visuals.message,
                    actionLabel = data.visuals.actionLabel,
                    onAction = { data.performAction() },
                    onDismiss = { data.dismiss() }
                )
            }
        }
    }
}

private fun getToastIcon(message: String): ImageVector? {
    val lower = message.lowercase()
    return when {
        lower.contains("success") || lower.contains("installed") || lower.contains("copied") || lower.contains("moved") || lower.contains("updated") -> Icons.Outlined.Check
        lower.contains("error") || lower.contains("fail") || lower.contains("not detected") || lower.contains("denied") -> Icons.Outlined.ErrorOutline
        lower.contains("warning") || lower.contains("warn") || lower.contains("wipe") -> Icons.Outlined.WarningAmber
        lower.contains("shizuku") || lower.contains("silent") || lower.contains("require") || lower.contains("need") || lower.contains("please") || lower.contains("info") -> Icons.Outlined.Info
        else -> Icons.Outlined.Info
    }
}
