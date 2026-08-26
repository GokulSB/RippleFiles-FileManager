package com.ripple.filemanager.ui.background

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Shader
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun rememberDiagonalHatchBrush(
    lineSpacing: Dp = 10.dp,
    lightLineAlpha: Float = 0.10f,
    darkLineAlpha: Float = 0.18f,
    lineOffset: Dp = lineSpacing / 2,
): Brush {
    val density = LocalDensity.current

    return remember(lineSpacing, lightLineAlpha, darkLineAlpha, lineOffset, density) {
        val spacingPx = with(density) { lineSpacing.toPx() }
        val offsetPx = with(density) { lineOffset.toPx() }

        val tileSizePx = spacingPx.roundToInt().coerceAtLeast(2)

        val bitmap = Bitmap.createBitmap(tileSizePx, tileSizePx, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)

        val strokePx = with(density) { 1.dp.toPx() }

        val lightPaint = AndroidPaint().apply {
            color = android.graphics.Color.argb(
                (lightLineAlpha * 255).roundToInt(),
                255, 255, 255,
            )
            strokeWidth = strokePx
            isAntiAlias = true
        }

        val darkPaint = AndroidPaint().apply {
            color = android.graphics.Color.argb(
                (darkLineAlpha * 255).roundToInt(),
                0, 0, 0,
            )
            strokeWidth = strokePx
            isAntiAlias = true
        }

        canvas.drawLine(0f, tileSizePx.toFloat(), tileSizePx.toFloat(), 0f, lightPaint)
        
        // Draw the dark line offset by half the tile size. 
        // To tile seamlessly, a diagonal offset by half must be drawn in two segments (corners).
        val halfTile = tileSizePx / 2f
        canvas.drawLine(0f, halfTile, halfTile, 0f, darkPaint)
        canvas.drawLine(halfTile, tileSizePx.toFloat(), tileSizePx.toFloat(), halfTile, darkPaint)

        val shader = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        ShaderBrush(shader)
    }
}

@Composable
fun GradientHatchBackground(
    gradientBrush: Brush,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val hatchBrush = rememberDiagonalHatchBrush()

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(hatchBrush)
        )
        content()
    }
}
