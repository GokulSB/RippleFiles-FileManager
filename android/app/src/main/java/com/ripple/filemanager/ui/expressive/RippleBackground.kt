package com.ripple.filemanager.ui.expressive

import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import kotlin.math.min

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp

/**
 * App background with warm radial glow (top-right) and faint concentric
 * "ripple" rings drawn behind content.
 */
@Composable
fun RippleBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = ExpressiveTheme.colors
    val context = LocalContext.current
    val reduceMotion = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        } else false
    }

    val glowColor = colors.glow

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Layer 1: Radial gradient centered at top-right corner (~90% width, 0% height), radius ~46% of height
            val trCenter = Offset(size.width * 0.90f, 0f)
            val trRadius = size.height * 0.46f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    center = trCenter,
                    radius = trRadius
                ),
                radius = trRadius,
                center = trCenter
            )

            // Layer 2: Second faint radial gradient top-left (~10% width, 0% height), ~35% opacity, radius ~30% of height
            val tlCenter = Offset(size.width * 0.10f, 0f)
            val tlRadius = size.height * 0.30f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = 0.35f), Color.Transparent),
                    center = tlCenter,
                    radius = tlRadius
                ),
                radius = tlRadius,
                center = tlCenter
            )

            // Layer 3: Concentric ring outlines, stroke colour = line colour at lineAlpha, stroke width ~1.4dp, drawn off-canvas bottom-right
            val brCenter = Offset(size.width * 1.05f, size.height * 0.95f)
            val maxRadius = size.width * 0.90f
            val ringCount = 5
            val strokeWidthPx = 1.4f.dp.toPx()
            for (i in 1..ringCount) {
                val r = maxRadius * (i.toFloat() / ringCount)
                drawCircle(
                    color = colors.line.copy(alpha = colors.lineAlpha),
                    radius = r,
                    center = brCenter,
                    style = Stroke(width = strokeWidthPx)
                )
            }
        }

        content()
    }
}
