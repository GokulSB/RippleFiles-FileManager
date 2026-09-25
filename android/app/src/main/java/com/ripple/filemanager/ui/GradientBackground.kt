package com.ripple.filemanager.ui
import androidx.compose.material3.ColorScheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import kotlin.math.min
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import com.ripple.filemanager.ui.background.rememberDiagonalHatchBrush
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.BoxWithConstraints
import android.util.Log



@Composable
fun AuroraBackground(colorScheme: ColorScheme, modifier: Modifier = Modifier) {
    // Log the actual colorScheme values
    val pHex = Integer.toHexString(colorScheme.primary.hashCode())
    val sHex = Integer.toHexString(colorScheme.secondary.hashCode())
    val tHex = Integer.toHexString(colorScheme.tertiary.hashCode())
    Log.d("AuroraColors", "Primary: #${pHex}, Secondary: #${sHex}, Tertiary: #${tHex}")

    // Temporarily hardcode a saturated test palette
    val testPrimary = Color(0xFF7B5CFA)   // violet
    val testSecondary = Color(0xFFB45CFA) // magenta
    val testTertiary = Color(0xFF5C7DFA)  // blue

    // Increase blob alpha to 0.65 at center
    val centerAlpha = 0.65f

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(colorScheme.surface)) {
        val width = maxWidth
        
        // Blob 1 - top left
        Box(
            Modifier
                .size(280.dp)
                .offset(x = (-80).dp, y = (-100).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            testPrimary.copy(alpha = centerAlpha),
                            testPrimary.copy(alpha = 0f)
                        )
                    ),
                    shape = CircleShape
                )
        )
        // Blob 2 - top right
        Box(
            Modifier
                .size(280.dp)
                .offset(x = width - 200.dp, y = (-60).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            testSecondary.copy(alpha = centerAlpha),
                            testSecondary.copy(alpha = 0f)
                        )
                    ),
                    shape = CircleShape
                )
        )
        // Blob 3 - lower left
        Box(
            Modifier
                .size(280.dp)
                .offset(x = (-40).dp, y = (380).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            testTertiary.copy(alpha = centerAlpha),
                            testTertiary.copy(alpha = 0f)
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}



data class AuroraColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val surfaceBase: Color,
    val onPrimary: Color
)

@Composable
fun rememberAuroraColors(): AuroraColors {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    return remember(isDark, context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            AuroraColors(
                primary = scheme.primary,
                secondary = scheme.secondary,
                tertiary = scheme.tertiary,
                surfaceBase = scheme.surface,
                onPrimary = scheme.onPrimary
            )
        } else {
            val fallbackPrimary = Color(0xFF6D98BA)
            val fallbackSecondary = Color(0xFF5B7065)
            val fallbackTertiary = Color(0xFF8B6C7B)
            val fallbackSurface = if (isDark) Color(0xFF101010) else Color(0xFFF7F7F7)
            AuroraColors(
                primary = fallbackPrimary,
                secondary = fallbackSecondary,
                tertiary = fallbackTertiary,
                surfaceBase = fallbackSurface,
                onPrimary = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
            )
        }
    }
}

fun Modifier.appGradientBackground(): Modifier = composed {
    val exprColors = com.ripple.filemanager.ui.expressive.LocalExpressiveColors.current
    val fallback = Color(0xFF1B1210)
    val bg = if (exprColors.bg != Color.Unspecified) exprColors.bg else fallback
    val textColor = if (exprColors.text != Color.Unspecified) exprColors.text else Color(0xFFFFE4DC)
    val glowColor = if (exprColors.glow != Color.Unspecified) exprColors.glow else Color(0xFF7A4A3F)
    val lineStrokeColor = if (exprColors.line != Color.Unspecified) exprColors.line.copy(alpha = exprColors.lineAlpha) else textColor.copy(alpha = 0.14f)
    this.drawWithCache {
        onDrawBehind {
            drawRect(color = bg)

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
                    color = lineStrokeColor,
                    radius = r,
                    center = brCenter,
                    style = Stroke(width = strokeWidthPx)
                )
            }
        }
    }
}

fun Modifier.auroraSurface(
    shape: Shape, 
    isStorageCard: Boolean = false,
    customBorderColor: Color? = null,
    customBorderWidth: androidx.compose.ui.unit.Dp? = null
): Modifier = composed {
    val colorScheme = androidx.compose.material3.MaterialTheme.colorScheme
    val fillAlpha = if (isStorageCard) 0.16f else 0.10f
    val borderAlpha = 0.22f
    
    this
        .background(colorScheme.primary.copy(alpha = fillAlpha), shape)
        .border(
            customBorderWidth ?: 1.dp, 
            customBorderColor ?: colorScheme.primary.copy(alpha = borderAlpha), 
            shape
        )
}
