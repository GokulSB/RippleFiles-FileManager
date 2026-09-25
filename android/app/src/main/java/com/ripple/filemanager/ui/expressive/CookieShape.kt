package com.ripple.filemanager.ui.expressive

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * A scalloped "cookie" shape with soft lobes.
 *
 * @param lobes Number of scallops (6–12)
 * @param amplitude Scallop depth (0.0–0.15; default 0.07)
 */
class CookieShape(
    private val lobes: Int = 8,
    private val amplitude: Float = 0.07f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        // Divide by (1f + amplitude) so scallop peaks at cos == 1 never exceed the bounding box
        val rx = (size.width / 2f) / (1f + amplitude)
        val ry = (size.height / 2f) / (1f + amplitude)
        val steps = lobes * 12

        for (i in 0..steps) {
            val angle = (i.toFloat() / steps) * 2f * Math.PI.toFloat()
            val lobeFactor = 1f + amplitude * cos(lobes * angle)
            val x = cx + rx * lobeFactor * cos(angle)
            val y = cy + ry * lobeFactor * sin(angle)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

/** Stable lobe count (6–12) from a name hash. */
fun cookieLobesForName(name: String): Int {
    val hash = abs(name.hashCode())
    return 6 + (hash % 7)
}

object CookieShapes {
    val Small = CookieShape(lobes = 8, amplitude = 0.06f)
    val Medium = CookieShape(lobes = 10, amplitude = 0.07f)
    val Large = CookieShape(lobes = 12, amplitude = 0.07f)
    val Fab = CookieShape(lobes = 10, amplitude = 0.08f)
}
