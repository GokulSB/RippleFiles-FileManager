package com.ripple.filemanager.ui.expressive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * App background with warm radial glow (top-right) and faint concentric
 * "ripple" rings drawn behind content.
 * Optimized with drawWithCache for 0-allocation, high-FPS animations.
 */
@Composable
fun RippleBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = ExpressiveTheme.colors
    val glowColor = colors.glow
    val lineColor = colors.line.copy(alpha = colors.lineAlpha)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .drawWithCache {
                // Layer 1: Radial gradient centered at top-right corner (~90% width, 0% height), radius ~46% of height
                val trCenter = Offset(size.width * 0.90f, 0f)
                val trRadius = size.height * 0.46f
                val trBrush = Brush.radialGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    center = trCenter,
                    radius = trRadius
                )

                // Layer 2: Second faint radial gradient top-left (~10% width, 0% height), ~35% opacity, radius ~30% of height
                val tlCenter = Offset(size.width * 0.10f, 0f)
                val tlRadius = size.height * 0.30f
                val tlBrush = Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = 0.35f), Color.Transparent),
                    center = tlCenter,
                    radius = tlRadius
                )

                // Layer 3: Concentric ring outlines, stroke colour = line colour at lineAlpha, stroke width ~1.4dp, drawn off-canvas bottom-right
                val brCenter = Offset(size.width * 1.05f, size.height * 0.95f)
                val maxRadius = size.width * 0.90f
                val ringCount = 5
                val strokeWidthPx = 1.4f.dp.toPx()
                val stroke = Stroke(width = strokeWidthPx)

                onDrawBehind {
                    drawCircle(brush = trBrush, radius = trRadius, center = trCenter)
                    drawCircle(brush = tlBrush, radius = tlRadius, center = tlCenter)
                    for (i in 1..ringCount) {
                        val r = maxRadius * (i.toFloat() / ringCount)
                        drawCircle(color = lineColor, radius = r, center = brCenter, style = stroke)
                    }
                }
            }
    ) {
        content()
    }
}
