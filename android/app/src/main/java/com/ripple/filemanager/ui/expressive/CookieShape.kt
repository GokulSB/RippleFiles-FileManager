package com.ripple.filemanager.ui.expressive

import android.content.Context
import android.util.LruCache
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.ripple.filemanager.IconShapeType
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

/**
 * A scalloped "cookie" shape with soft lobes.
 *
 * @param lobes Number of scallops (6–12)
 * @param amplitude Scallop depth (0.0–0.15; default 0.07)
 * @param exponent Curvature exponent (2.0 = circular base, > 2.0 = squarish base for lower roundness)
 */
class CookieShape(
    val lobes: Int = 8,
    val amplitude: Float = 0.07f,
    val exponent: Float = 2.0f
) : Shape {
    private val outlineCache = LruCache<Long, Outline>(16)

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        if (size.width <= 0f || size.height <= 0f) {
            return Outline.Rectangle(androidx.compose.ui.geometry.Rect.Zero)
        }
        val key = (size.width.toRawBits().toLong() shl 32) or (size.height.toRawBits().toLong() and 0xFFFFFFFFL)
        outlineCache.get(key)?.let { return it }

        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        // Divide by (1f + amplitude) so scallop peaks at cos == 1 never exceed the bounding box
        val rx = (size.width / 2f) / (1f + amplitude)
        val ry = (size.height / 2f) / (1f + amplitude)
        val steps = lobes * 12
        val power = 2.0 / exponent.toDouble()

        for (i in 0..steps) {
            val angle = (i.toFloat() / steps) * 2f * Math.PI.toFloat()
            val lobeFactor = 1f + amplitude * cos(lobes * angle)
            val cosA = cos(angle)
            val sinA = sin(angle)
            val x = if (exponent == 2f) {
                cx + rx * lobeFactor * cosA
            } else {
                cx + rx * lobeFactor * sign(cosA) * abs(cosA).toDouble().pow(power).toFloat()
            }
            val y = if (exponent == 2f) {
                cy + ry * lobeFactor * sinA
            } else {
                cy + ry * lobeFactor * sign(sinA) * abs(sinA).toDouble().pow(power).toFloat()
            }
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        val outline = Outline.Generic(path)
        outlineCache.put(key, outline)
        return outline
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CookieShape) return false
        return lobes == other.lobes && amplitude == other.amplitude && exponent == other.exponent
    }

    override fun hashCode(): Int {
        var result = lobes
        result = 31 * result + amplitude.hashCode()
        result = 31 * result + exponent.hashCode()
        return result
    }
}

/**
 * Superellipse shape |x/a|^n + |y/b|^n = 1
 * iOS app-icon curvature: n ≈ 5.0f
 * Samsung One UI curvature: n ≈ 3.2f
 */
class SuperellipseShape(
    val n: Float = 5f,
    private val steps: Int = 96
) : Shape {
    private val outlineCache = LruCache<Long, Outline>(16)

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        if (size.width <= 0f || size.height <= 0f) {
            return Outline.Rectangle(androidx.compose.ui.geometry.Rect.Zero)
        }
        val key = (size.width.toRawBits().toLong() shl 32) or (size.height.toRawBits().toLong() and 0xFFFFFFFFL)
        outlineCache.get(key)?.let { return it }

        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val a = size.width / 2f
        val b = size.height / 2f
        val power = 2.0 / n.toDouble()

        for (i in 0..steps) {
            val t = (i.toFloat() / steps) * 2f * Math.PI.toFloat()
            val cosT = cos(t)
            val sinT = sin(t)
            val x = cx + a * sign(cosT) * abs(cosT).toDouble().pow(power).toFloat()
            val y = cy + b * sign(sinT) * abs(sinT).toDouble().pow(power).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        val outline = Outline.Generic(path)
        outlineCache.put(key, outline)
        return outline
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SuperellipseShape) return false
        return n == other.n
    }

    override fun hashCode(): Int = n.hashCode()
}

/** Stable lobe count (6–12) from a name hash. */
fun cookieLobesForName(name: String): Int {
    val hash = abs(name.hashCode())
    return 6 + (hash % 7)
}

/**
 * In-memory session holder for the Auto icon shape.
 * Picks one of the fixed shapes at random on launch/session start.
 * Held in memory for the duration of the app session.
 */
object AutoIconShapeSession {
    @Volatile
    var sessionAutoShape: IconShapeType = IconShapeType.fixedShapes.random()
        private set

    /**
     * Re-rolls a new random shape, guaranteed to be different from the previous one (if >1 candidate),
     * and persists the last chosen auto shape in SharedPreferences.
     */
    fun refresh(context: Context): IconShapeType {
        val prefs = context.getSharedPreferences("sift_prefs", Context.MODE_PRIVATE)
        val last = prefs.getString("last_auto_shape", null)
        val candidates = IconShapeType.fixedShapes.filter { it.name != last }
        val chosen = (if (candidates.isNotEmpty()) candidates else IconShapeType.fixedShapes).random()
        prefs.edit().putString("last_auto_shape", chosen.name).apply()
        sessionAutoShape = chosen
        return chosen
    }
}

object CookieShapes {
    val Small = CookieShape(lobes = 8, amplitude = 0.06f)
    val Medium = CookieShape(lobes = 10, amplitude = 0.07f)
    val Large = CookieShape(lobes = 12, amplitude = 0.07f)
    val Fab = CookieShape(lobes = 10, amplitude = 0.08f)
}

object IconShapes {
    val Circle: Shape = CircleShape
    val Square: Shape = RoundedCornerShape(4.dp)
    val RoundedSquare: Shape = RoundedCornerShape(percent = 28)
    val Ios: Shape = SuperellipseShape(n = 5.0f)
    val OneUi: Shape = SuperellipseShape(n = 3.2f)

    private val cookieCache = LruCache<String, CookieShape>(32)

    fun cookieShape(lobes: Int = 8, amplitude: Float = 0.07f, exponent: Float = 2.0f): CookieShape {
        val key = "$lobes:$amplitude:$exponent"
        cookieCache.get(key)?.let { return it }
        val s = CookieShape(lobes, amplitude, exponent)
        cookieCache.put(key, s)
        return s
    }

    fun forType(
        type: IconShapeType,
        lobes: Int = 8,
        amplitude: Float = 0.07f,
        cornerRoundness: Float = 0.5f
    ): Shape {
        val roundness = cornerRoundness.coerceIn(0f, 1f)
        if (roundness <= 0.05f) {
            return RoundedCornerShape(0.dp)
        }
        return when (type) {
            IconShapeType.COOKIE -> {
                if (roundness >= 0.95f) {
                    CircleShape
                } else if (roundness in 0.05f..0.5f) {
                    val t = (0.5f - roundness) / 0.45f
                    val exponent = 2.0f + 14.0f * t
                    val amp = amplitude * (1f - 0.7f * t)
                    cookieShape(lobes, amp, exponent)
                } else {
                    val t = (roundness - 0.5f) / 0.45f
                    val amp = amplitude * (1f + 0.35f * t)
                    cookieShape(lobes, amp, 2.0f)
                }
            }
            IconShapeType.CIRCLE -> {
                val percent = if (roundness <= 0.5f) {
                    (28f * (roundness / 0.5f)).roundToInt()
                } else {
                    (28f + 22f * ((roundness - 0.5f) / 0.5f)).roundToInt()
                }
                if (percent >= 50) CircleShape else RoundedCornerShape(percent = percent.coerceIn(0, 50))
            }
            IconShapeType.SQUARE -> {
                val r = (8f * roundness).dp
                RoundedCornerShape(r)
            }
            IconShapeType.ROUNDED_SQUARE -> {
                val percent = if (roundness <= 0.5f) {
                    (28f * (roundness / 0.5f)).roundToInt()
                } else {
                    (28f + 22f * ((roundness - 0.5f) / 0.5f)).roundToInt()
                }
                if (percent >= 50) CircleShape else RoundedCornerShape(percent = percent.coerceIn(0, 50))
            }
            IconShapeType.IOS -> {
                if (roundness >= 0.98f) {
                    CircleShape
                } else if (roundness <= 0.5f) {
                    val t = (0.5f - roundness) / 0.48f
                    val n = 5.0f + 19.0f * t
                    SuperellipseShape(n = n.coerceIn(2.0f, 24.0f))
                } else {
                    val t = (roundness - 0.5f) / 0.48f
                    val n = 5.0f - 3.0f * t
                    SuperellipseShape(n = n.coerceIn(2.0f, 24.0f))
                }
            }
            IconShapeType.ONE_UI -> {
                if (roundness >= 0.98f) {
                    CircleShape
                } else if (roundness <= 0.5f) {
                    val t = (0.5f - roundness) / 0.48f
                    val n = 3.2f + 20.8f * t
                    SuperellipseShape(n = n.coerceIn(2.0f, 24.0f))
                } else {
                    val t = (roundness - 0.5f) / 0.48f
                    val n = 3.2f - 1.2f * t
                    SuperellipseShape(n = n.coerceIn(2.0f, 24.0f))
                }
            }
            IconShapeType.AUTO -> {
                forType(AutoIconShapeSession.sessionAutoShape, lobes, amplitude, roundness)
            }
        }
    }
}

val LocalIconShapeType = staticCompositionLocalOf<IconShapeType> { IconShapeType.COOKIE }

@Composable
fun rememberIconShape(
    type: IconShapeType = LocalIconShapeType.current,
    lobes: Int = 8,
    amplitude: Float = 0.07f,
    cornerRoundness: Float = LocalCornerRoundness.current
): Shape {
    return remember(type, lobes, amplitude, cornerRoundness) {
        IconShapes.forType(type, lobes = lobes, amplitude = amplitude, cornerRoundness = cornerRoundness)
    }
}
