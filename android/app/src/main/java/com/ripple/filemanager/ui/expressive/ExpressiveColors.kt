package com.ripple.filemanager.ui.expressive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Expressive design tokens – warm cocoa palette.
 */
@Immutable
object ExpressiveTokens {
    // ── Dark palette ──
    val DarkBg = Color(0xFF1B1210)
    val DarkContainer = Color(0xFF5E352D)
    val DarkCard = Color(0xFF71443A)
    val DarkInsetCard = Color(0xFF3F2420)
    val DarkAccent = Color(0xFFF8B8A8)
    val DarkInk = Color(0xFF4D241E)
    val DarkSand = Color(0xFFF6D391)
    val DarkText = Color(0xFFFFE4DC)
    val DarkMuted = Color(0xFFD6A598)
    val DarkLine = Color(0xFFFFCDBE)
    val DarkLineAlpha = 0.14f
    val DarkShadow = Color(0x47000000)
    val DarkGlow = Color(0xFF7A4A3F)

    // ── Light palette ──
    val LightBg = Color(0xFFFDF8F3)
    val LightContainer = Color(0xFFF2E1CB)
    val LightCard = Color(0xFFE8CBA5)
    val LightInsetCard = Color(0xFFD9AE78)
    val LightAccent = Color(0xFFB85A2C)
    val LightInk = Color(0xFFFFF8F2)
    val LightSand = Color(0xFF7C5518)
    val LightText = Color(0xFF2B1A10)
    val LightMuted = Color(0xFF7C5A3E)
    val LightLine = Color(0xFF2B1A10)
    val LightLineAlpha = 0.09f
    val LightShadow = Color(0xFF6E4123).copy(alpha = 0.22f)
    val LightGlow = Color(0xFFEEC9A3)

    // ── Category pastels ──
    val PastelRose = Color(0xFFF5A9C3)
    val PastelCoral = Color(0xFFFF9F88)
    val PastelLilac = Color(0xFFCDB4F6)
    val PastelTeal = Color(0xFF7FD9C8)
    val PastelSage = Color(0xFFB6DB9C)
    val PastelSky = Color(0xFF9FCBF5)
    val PastelSand = Color(0xFFF6D391)
    val PastelViolet = Color(0xFFB892F2)
    val PastelPeach = Color(0xFFF8B8A8)

    val OnPastel = Color(0xFF3B1C17)

    fun categoryPastel(category: String): Color = when (category.lowercase()) {
        "images", "image" -> PastelCoral
        "videos", "video" -> PastelRose
        "audio", "music" -> PastelViolet
        "docs", "doc", "documents" -> PastelSky
        "apps", "apk" -> PastelSage
        "downloads", "download" -> PastelTeal
        "archives", "archive", "zip", "rar", "7z" -> PastelLilac
        "large", "big" -> PastelSand
        "folder" -> PastelPeach
        else -> PastelPeach
    }
}

@Immutable
data class ExpressiveColorScheme(
    val bg: Color,
    val container: Color,
    val card: Color,
    val insetCard: Color,
    val accent: Color,
    val ink: Color,
    val sand: Color,
    val text: Color,
    val muted: Color,
    val line: Color,
    val lineAlpha: Float,
    val shadow: Color = Color(0x47000000),
    val glow: Color = Color(0xFF7A4A3F)
)

val DarkExpressiveColorScheme = ExpressiveColorScheme(
    bg = ExpressiveTokens.DarkBg,
    container = ExpressiveTokens.DarkContainer,
    card = ExpressiveTokens.DarkCard,
    insetCard = ExpressiveTokens.DarkInsetCard,
    accent = ExpressiveTokens.DarkAccent,
    ink = ExpressiveTokens.DarkInk,
    sand = ExpressiveTokens.DarkSand,
    text = ExpressiveTokens.DarkText,
    muted = ExpressiveTokens.DarkMuted,
    line = ExpressiveTokens.DarkLine,
    lineAlpha = ExpressiveTokens.DarkLineAlpha,
    shadow = ExpressiveTokens.DarkShadow,
    glow = ExpressiveTokens.DarkGlow
)

val LightExpressiveColorScheme = ExpressiveColorScheme(
    bg = ExpressiveTokens.LightBg,
    container = ExpressiveTokens.LightContainer,
    card = ExpressiveTokens.LightCard,
    insetCard = ExpressiveTokens.LightInsetCard,
    accent = ExpressiveTokens.LightAccent,
    ink = ExpressiveTokens.LightInk,
    sand = ExpressiveTokens.LightSand,
    text = ExpressiveTokens.LightText,
    muted = ExpressiveTokens.LightMuted,
    line = ExpressiveTokens.LightLine,
    lineAlpha = ExpressiveTokens.LightLineAlpha,
    shadow = ExpressiveTokens.LightShadow,
    glow = ExpressiveTokens.LightGlow
)

val LocalExpressiveColors = staticCompositionLocalOf { DarkExpressiveColorScheme }

object ExpressiveTheme {
    val colors: ExpressiveColorScheme
        @Composable get() = LocalExpressiveColors.current
}

/** Corner radius scaling factor from slider (0..1) */
fun cornerScale(cornerRoundness: Float): Float {
    return 0.15f + (cornerRoundness.coerceIn(0f, 1f)) * 0.85f
}
