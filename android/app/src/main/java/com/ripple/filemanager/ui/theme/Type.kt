package com.ripple.filemanager.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.isUnspecified
import com.ripple.filemanager.R

// ── Existing font families ────────────────────────────────────────────────────
val RobotoFontFamily      = FontFamily(Font(R.font.roboto))
val GoogleSansFontFamily  = FontFamily(Font(R.font.google_sans))
val PoppinsFontFamily     = FontFamily(Font(R.font.poppins))

// ── Skyline Ledger font families ──────────────────────────────────────────────
val FrauncesFontFamily    = FontFamily(Font(R.font.fraunces))
val ManropeFontFamily     = FontFamily(Font(R.font.manrope))
val JetBrainsMonoFamily   = FontFamily(Font(R.font.jetbrains_mono))

/** Skyline typography: Fraunces display, Manrope body, JetBrains Mono labels */
val SkylineTypography = Typography(
    displayLarge  = TextStyle(fontFamily = FrauncesFontFamily, fontWeight = FontWeight.Normal, fontSize = 57.sp),
    displayMedium = TextStyle(fontFamily = FrauncesFontFamily, fontWeight = FontWeight.Normal, fontSize = 45.sp),
    displaySmall  = TextStyle(fontFamily = FrauncesFontFamily, fontWeight = FontWeight.Normal, fontSize = 36.sp),
    headlineLarge = TextStyle(fontFamily = FrauncesFontFamily, fontWeight = FontWeight.Normal, fontSize = 32.sp),
    headlineMedium= TextStyle(fontFamily = FrauncesFontFamily, fontWeight = FontWeight.Normal, fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = FrauncesFontFamily, fontWeight = FontWeight.Normal, fontSize = 24.sp),
    titleLarge    = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium   = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall    = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    bodyLarge     = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium    = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall     = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge    = TextStyle(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.5.sp),
    labelMedium   = TextStyle(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.8.sp),
    labelSmall    = TextStyle(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Medium, fontSize = 10.sp, letterSpacing = 1.5.sp)
)

fun getSkylineTypography(
    fontStyleStr: String,
    decorations: Set<String>,
    mainTextScale: Float = 1.0f,
    subTextScale: Float = 1.0f
): Typography {
    val isBold      = decorations.contains("Bold")
    val isItalic    = decorations.contains("Italics")
    val isUnderline = decorations.contains("Underline")
    val isStrike    = decorations.contains("Strikethrough")

    var textDecoration = TextDecoration.None
    if (isUnderline && isStrike) textDecoration = TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
    else if (isUnderline) textDecoration = TextDecoration.Underline
    else if (isStrike)    textDecoration = TextDecoration.LineThrough

    val fontWeight = if (isBold) FontWeight.Bold else null
    val fontStyle  = if (isItalic) FontStyle.Italic else null

    // Override font families based on user's font picker
    val displayFamily: FontFamily
    val bodyFamily: FontFamily
    val monoFamily: FontFamily
    when (fontStyleStr) {
        "Skyline Ledger" -> {
            displayFamily = FrauncesFontFamily
            bodyFamily = ManropeFontFamily
            monoFamily = JetBrainsMonoFamily
        }
        "Roboto" -> {
            displayFamily = RobotoFontFamily
            bodyFamily = RobotoFontFamily
            monoFamily = FontFamily.Monospace
        }
        "Google Sans" -> {
            displayFamily = GoogleSansFontFamily
            bodyFamily = GoogleSansFontFamily
            monoFamily = FontFamily.Monospace
        }
        "Poppins" -> {
            displayFamily = PoppinsFontFamily
            bodyFamily = PoppinsFontFamily
            monoFamily = FontFamily.Monospace
        }
        "Monospace" -> {
            displayFamily = JetBrainsMonoFamily
            bodyFamily = JetBrainsMonoFamily
            monoFamily = JetBrainsMonoFamily
        }
        else -> {
            displayFamily = FontFamily.Default
            bodyFamily = FontFamily.Default
            monoFamily = FontFamily.Monospace
        }
    }

    fun applyBody(base: TextStyle, scale: Float) = base.copy(
        fontFamily    = bodyFamily,
        fontWeight    = fontWeight ?: base.fontWeight,
        fontStyle     = fontStyle  ?: base.fontStyle,
        textDecoration = textDecoration,
        fontSize      = if (base.fontSize.isUnspecified) base.fontSize else base.fontSize * scale,
        lineHeight    = if (base.lineHeight.isUnspecified) base.lineHeight else base.lineHeight * scale
    )

    fun applyDisplay(base: TextStyle, scale: Float) = base.copy(
        fontFamily    = displayFamily,
        fontWeight    = fontWeight ?: base.fontWeight,
        fontStyle     = fontStyle  ?: base.fontStyle,
        textDecoration = textDecoration,
        fontSize      = if (base.fontSize.isUnspecified) base.fontSize else base.fontSize * scale,
        lineHeight    = if (base.lineHeight.isUnspecified) base.lineHeight else base.lineHeight * scale
    )

    fun applyMono(base: TextStyle) = base.copy(
        fontFamily    = monoFamily,
        fontWeight    = fontWeight ?: base.fontWeight,
        fontStyle     = fontStyle  ?: base.fontStyle,
        textDecoration = textDecoration
    )

    return Typography(
        displayLarge   = applyDisplay(SkylineTypography.displayLarge, mainTextScale),
        displayMedium  = applyDisplay(SkylineTypography.displayMedium, mainTextScale),
        displaySmall   = applyDisplay(SkylineTypography.displaySmall, mainTextScale),
        headlineLarge  = applyDisplay(SkylineTypography.headlineLarge, mainTextScale),
        headlineMedium = applyDisplay(SkylineTypography.headlineMedium, mainTextScale),
        headlineSmall  = applyDisplay(SkylineTypography.headlineSmall, mainTextScale),
        titleLarge     = applyBody(SkylineTypography.titleLarge, mainTextScale),
        titleMedium    = applyBody(SkylineTypography.titleMedium, mainTextScale),
        titleSmall     = applyBody(SkylineTypography.titleSmall, mainTextScale),
        bodyLarge      = applyBody(SkylineTypography.bodyLarge, subTextScale),
        bodyMedium     = applyBody(SkylineTypography.bodyMedium, subTextScale),
        bodySmall      = applyBody(SkylineTypography.bodySmall, subTextScale),
        labelLarge     = applyMono(SkylineTypography.labelLarge),
        labelMedium    = applyMono(SkylineTypography.labelMedium),
        labelSmall     = applyMono(SkylineTypography.labelSmall)
    )
}

// Legacy helper kept for settings screen that still calls getCustomTypography
fun getCustomTypography(
    fontStyleStr: String,
    decorations: Set<String>,
    mainTextScale: Float = 1.0f,
    subTextScale: Float = 1.0f
): Typography = getSkylineTypography(fontStyleStr, decorations, mainTextScale, subTextScale)
