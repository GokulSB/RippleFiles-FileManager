package com.ripple.filemanager.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.compose.runtime.CompositionLocalProvider
import com.ripple.filemanager.ui.expressive.DarkExpressiveColorScheme
import com.ripple.filemanager.ui.expressive.LightExpressiveColorScheme
import com.ripple.filemanager.ui.expressive.LocalExpressiveColors
import com.ripple.filemanager.ui.expressive.ExpressiveColorScheme
import com.ripple.filemanager.ui.expressive.ExpressiveTokens

// Fixed light palette (fallback + base for everything non-accent)
val LightBackground = Color(0xFFFDF6F0)   // crisp warm cream base
val LightSurface    = Color(0xFFEAD0B4)   // warm card surface
val LightBorder     = Color(0xFF2E1B10).copy(alpha = 0.14f)
val LightTextPrimary   = Color(0xFF2E1B10)
val LightTextSecondary = Color(0xFF7A5136)

// Fixed dark palette (fallback + base for everything non-accent)
val DarkBackground = Color(0xFF1B1210)   // expressive warm cocoa base
val DarkSurface    = Color(0xFF33281E)   // storage cards & chip area (elevated brown)
val DarkSurface2   = Color(0xFF2B2218)   // bottom sheet layout (middle elevation)
val DarkBorder     = Color(0xFF4F4235)
val DarkTextPrimary   = Color(0xFFEBE0D1)
val DarkTextSecondary = Color(0xFFB5A99A)

// Fixed amber fallback accent — used when dynamic color is off/unavailable
val AmberPrimary          = Color(0xFFC1602F)
val AmberPrimaryContainer = Color(0xFFF3E3D2)
val AmberOnPrimary        = Color(0xFFFFF5EE)

// Folder category tints — NEVER dynamic, these encode folder identity
val FolderGreen  = Color(0xFF3B6D11) to Color(0xFFEAF3DE)
val FolderBlue   = Color(0xFF185FA5) to Color(0xFFE6F1FB)
val FolderTeal   = Color(0xFF0F6E56) to Color(0xFFE1F5EE)
val FolderCoral  = Color(0xFF993C1D) to Color(0xFFFAECE7)
val FolderPink   = Color(0xFF993556) to Color(0xFFFBEAF0)
val FolderViolet = Color(0xFF6B4BA3) to Color(0xFFEFE6FB)

fun hsl(h: Float, s: Float, l: Float): Color {
    val wrappedHue = ((h % 360f) + 360f) % 360f
    return Color(ColorUtils.HSLToColor(floatArrayOf(wrappedHue, s / 100f, l / 100f)))
}

// ── Skyline Ledger design tokens ──────────────────────────────────────────────
object SkylineColors {

    var Background    by mutableStateOf(Color(0xFF1B1210))
    var Surface       by mutableStateOf(Color(0xFF1F1D1B))
    var Surface2      by mutableStateOf(Color(0xFF282522))
    var Border        by mutableStateOf(Color(0xFF3D3833))
    
    var Amber         by mutableStateOf(Color(0xFFF1B761))
    var AmberDim      by mutableStateOf(Color(0xFFC08630))
    var ContainerSecondary by mutableStateOf(Color(0xFF2A1F14))
    var ContainerTertiary  by mutableStateOf(Color(0xFF2A1F14))
    var Dust          by mutableStateOf(Color(0xFF7A8686))
    var Sage          by mutableStateOf(Color(0xFF7A867A))
    var Rust          by mutableStateOf(Color(0xFF867A7A))

    var TextPrimary   by mutableStateOf(Color(0xFFE4E4E4))
    var TextPrimary2  by mutableStateOf(Color(0xFFCCCCCC))
    var TextDim       by mutableStateOf(Color(0xFF999999))
    var TextDim2      by mutableStateOf(Color(0xFF666666))

    var AccentGreen   by mutableStateOf(Color(0xFF81C784))
    var AccentBlue    by mutableStateOf(Color(0xFF64B5F6))
    var AccentTeal    by mutableStateOf(Color(0xFF4DB6AC))
    var AccentPrimary by mutableStateOf(Color(0xFFFFB74D))
    var AccentRed     by mutableStateOf(Color(0xFFE57373))
    var AccentPink    by mutableStateOf(Color(0xFFF06292))
    var AccentViolet  by mutableStateOf(Color(0xFFBA68C8))

    fun updateColors(isDark: Boolean, dynamicColor: Boolean, customHue: Float, lightnessOffset: Float, invertText: Boolean, context: android.content.Context) {
        val effectiveHue = if (dynamicColor && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val scheme = if (isDark) androidx.compose.material3.dynamicDarkColorScheme(context) else androidx.compose.material3.dynamicLightColorScheme(context)
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(scheme.primary.toArgb(), hsv)
            hsv[0]
        } else {
            customHue
        }
        val isCustomPreset = !dynamicColor && kotlin.math.abs(customHue - 14f) >= 4f
        val isCocoa = !isCustomPreset

        if (isDark) {
            if (isCocoa) {
                Background = Color(0xFF1B1210)
                Surface2 = Color(0xFF3F2420)
                Surface = Color(0xFF5E352D)
                Border = Color(0xFFFFCDBE).copy(alpha = 0.14f)
                Amber = Color(0xFFF8B8A8)
                AmberDim = Color(0xFF5E352D)
                ContainerSecondary = Color(0xFF3F2420)
                ContainerTertiary = Color(0xFF71443A)
                Dust = Color(0xFFD6A598)
                Sage = Color(0xFF7FD9C8)
                Rust = Color(0xFFFF9F88)
                AccentGreen = Color(0xFFB6DB9C)
                AccentBlue = Color(0xFF9FCBF5)
                AccentTeal = Color(0xFF7FD9C8)
                AccentPrimary = Amber
                AccentRed = Color(0xFFF5A9C3)
                AccentPink = Color(0xFFF5A9C3)
                AccentViolet = Color(0xFFB892F2)
            } else {
                Background = hsl(effectiveHue, 18f, (8f + lightnessOffset).coerceIn(4f, 20f))
                Surface2 = hsl(effectiveHue, 26f, (11f + lightnessOffset).coerceIn(5f, 25f))
                Surface = hsl(effectiveHue, 24f, (16f + lightnessOffset).coerceIn(8f, 35f))
                Border = hsl(effectiveHue, 25f, 85f).copy(alpha = 0.14f)
                Amber = hsl(effectiveHue, 82f, (74f + lightnessOffset).coerceIn(45f, 90f))
                AmberDim = hsl(effectiveHue, 24f, 16f)
                ContainerSecondary = hsl(effectiveHue, 26f, 11f)
                ContainerTertiary = hsl(effectiveHue, 22f, 22f)
                Dust = hsl(effectiveHue, 20f, 72f)
                Sage = hsl(effectiveHue + 90f, 40f, 75f)
                Rust = hsl(effectiveHue + 180f, 40f, 75f)
                AccentGreen = hsl(101f, 40f, 75f)
                AccentBlue = hsl(203f, 75f, 75f)
                AccentTeal = hsl(173f, 61f, 75f)
                AccentPrimary = Amber
                AccentRed = hsl(7f, 80f, 75f)
                AccentPink = hsl(339f, 80f, 78f)
                AccentViolet = hsl(263f, 78f, 78f)
            }
        } else {
            if (isCocoa) {
                Background = Color(0xFFFDF8F3)
                Surface = Color(0xFFE8CBA5)
                Surface2 = Color(0xFFF2E1CB)
                Border = Color(0xFF2B1A10).copy(alpha = 0.09f)
                Amber = Color(0xFFB85A2C)
                AmberDim = Color(0xFFF2E1CB)
                ContainerSecondary = Color(0xFFD9AE78)
                ContainerTertiary = Color(0xFFE8CBA5)
                Dust = Color(0xFF7C5A3E)
                Sage = Color(0xFF7FD9C8)
                Rust = Color(0xFFFF9F88)
                AccentGreen = Color(0xFF3B6D11)
                AccentBlue = Color(0xFF185FA5)
                AccentTeal = Color(0xFF0F6E56)
                AccentPrimary = Amber
                AccentRed = Color(0xFF993C1D)
                AccentPink = Color(0xFF993556)
                AccentViolet = Color(0xFF6B4BA3)
            } else {
                Background = hsl(effectiveHue, 25f, (97f + lightnessOffset).coerceIn(90f, 99f))
                Surface = hsl(effectiveHue, 30f, (81f + lightnessOffset).coerceIn(68f, 88f))
                Surface2 = hsl(effectiveHue, 32f, (89f + lightnessOffset).coerceIn(78f, 95f))
                Border = hsl(effectiveHue, 25f, 25f).copy(alpha = 0.14f)
                Amber = hsl(effectiveHue, 72f, (48f + lightnessOffset).coerceIn(30f, 70f))
                AmberDim = hsl(effectiveHue, 32f, 89f)
                ContainerSecondary = hsl(effectiveHue, 32f, 71f)
                ContainerTertiary = hsl(effectiveHue, 30f, 81f)
                Dust = hsl(effectiveHue, 25f, 35f)
                Sage = hsl(effectiveHue + 90f, 45f, 50f)
                Rust = hsl(effectiveHue + 180f, 45f, 50f)
                AccentGreen = hsl(101f, 40f, 50f)
                AccentBlue = hsl(203f, 75f, 50f)
                AccentTeal = hsl(173f, 61f, 45f)
                AccentPrimary = Amber
                AccentRed = hsl(7f, 70f, 50f)
                AccentPink = hsl(339f, 70f, 55f)
                AccentViolet = hsl(263f, 70f, 55f)
            }
        }
        
        if (invertText) {
            TextPrimary = if (isDark) Color.Black else Color.White
            TextPrimary2 = if (isDark) Color(0xFF222222) else Color(0xFFEEEEEE)
            TextDim = if (isDark) Color(0xFF444444) else Color(0xFFCCCCCC)
            TextDim2 = if (isDark) Color(0xFF666666) else Color(0xFFAAAAAA)
        } else {
            if (isDark) {
                TextPrimary = if (isCocoa) Color(0xFFFFE4DC) else hsl(effectiveHue, 25f, 94f)
                TextPrimary2 = TextPrimary
                TextDim = if (isCocoa) Color(0xFFD6A598) else hsl(effectiveHue, 20f, 72f)
                TextDim2 = TextDim
            } else {
                TextPrimary = if (isCocoa) Color(0xFF2B1A10) else hsl(effectiveHue, 40f, 12f)
                TextPrimary2 = TextPrimary
                TextDim = if (isCocoa) Color(0xFF7C5A3E) else hsl(effectiveHue, 25f, 35f)
                TextDim2 = TextDim
            }
        }
    }
}

/** Maps a file type string to its Skyline type-tone color. */
fun fileTypeTone(type: String): Color = when {
    type == "folder"                              -> SkylineColors.Dust
    type == "audio"                               -> SkylineColors.Amber
    type in listOf("image", "video")              -> SkylineColors.Sage
    type in listOf("apk", "system")               -> SkylineColors.AmberDim
    else                                          -> SkylineColors.Dust
}

/** Returns a 2–4 char uppercase type-code badge label. */
fun fileTypeCode(type: String): String = when (type) {
    "folder"  -> "DIR"
    "image"   -> "IMG"
    "video"   -> "VID"
    "audio"   -> "AUD"
    "pdf"     -> "PDF"
    "doc","docx" -> "DOC"
    "txt","md"   -> "TXT"
    "json"       -> "JSON"
    "zip","rar","7z" -> "ARC"
    "apk"     -> "APK"
    else      -> "SYS"
}

@Composable
fun SiftTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    customHue: Float = 262f,
    lightnessOffset: Float = 0f,
    fontStyle: String = "System",
    textDecorations: Set<String> = emptySet(),
    mainTextScale: Float = 1.0f,
    subTextScale: Float = 1.0f,
    invertText: Boolean = false,
    cornerRoundness: Float = 0.5f,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    SkylineColors.updateColors(darkTheme, dynamicColor, customHue, lightnessOffset, invertText, context)
    
    val colorScheme = when {
        darkTheme -> darkColorScheme(
            primary              = SkylineColors.Amber,
            onPrimary            = Color(0xFF111111),
            primaryContainer     = SkylineColors.AmberDim,
            onPrimaryContainer   = SkylineColors.TextPrimary,
            secondary            = SkylineColors.Dust,
            onSecondary          = Color(0xFF111111),
            secondaryContainer   = SkylineColors.ContainerSecondary,
            onSecondaryContainer = SkylineColors.TextPrimary,
            tertiary             = SkylineColors.Sage,
            onTertiary           = Color(0xFF111111),
            tertiaryContainer    = SkylineColors.ContainerTertiary,
            onTertiaryContainer  = SkylineColors.TextPrimary,
            error                = SkylineColors.Rust,
            onError              = SkylineColors.TextPrimary,
            errorContainer       = Color(0xFF5A2015),
            onErrorContainer     = SkylineColors.TextPrimary,
            background           = Color(0xFF1B1210),
            onBackground         = SkylineColors.TextPrimary,
            surface              = SkylineColors.Surface,
            onSurface            = SkylineColors.TextPrimary,
            surfaceVariant       = SkylineColors.Surface2,
            onSurfaceVariant     = SkylineColors.TextDim,
            surfaceContainer     = SkylineColors.Surface2,
            surfaceContainerHigh = Color(0xFF2F2218),
            outline              = SkylineColors.Border,
            outlineVariant       = Color(0xFF2A1F14),
            inverseSurface       = SkylineColors.TextPrimary,
            inverseOnSurface     = Color(0xFF111111),
            inversePrimary       = SkylineColors.AmberDim,
            scrim                = Color(0xCC000000)
        )

        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val dynamic = androidx.compose.material3.dynamicLightColorScheme(context)
            lightColorScheme(
                primary            = dynamic.primary,
                onPrimary          = dynamic.onPrimary,
                primaryContainer   = dynamic.primaryContainer,
                onPrimaryContainer = dynamic.onPrimaryContainer,
                secondaryContainer = dynamic.secondaryContainer,
                onSecondaryContainer = dynamic.onSecondaryContainer,
                tertiaryContainer  = dynamic.tertiaryContainer,
                onTertiaryContainer = dynamic.onTertiaryContainer,
                background         = SkylineColors.Background,
                surface            = SkylineColors.Surface,
                onSurface          = LightTextPrimary,
                onSurfaceVariant   = LightTextSecondary,
                outline            = SkylineColors.Border,
                outlineVariant     = SkylineColors.Border,
                surfaceVariant     = SkylineColors.Surface2,
                surfaceContainer   = SkylineColors.Surface2,
                surfaceContainerHigh = SkylineColors.Border
            )
        }

        else -> lightColorScheme(
            primary            = AmberPrimary,
            onPrimary          = AmberOnPrimary,
            primaryContainer   = AmberPrimaryContainer,
            onPrimaryContainer = AmberOnPrimary,
            secondaryContainer = AmberPrimaryContainer,
            onSecondaryContainer = AmberOnPrimary,
            tertiaryContainer  = AmberPrimaryContainer,
            onTertiaryContainer = AmberOnPrimary,
            background         = SkylineColors.Background,
            surface            = SkylineColors.Surface,
            onSurface          = LightTextPrimary,
            onSurfaceVariant   = LightTextSecondary,
            outline            = SkylineColors.Border,
            outlineVariant     = SkylineColors.Border,
            surfaceVariant     = SkylineColors.Surface2,
            surfaceContainer   = SkylineColors.Surface2,
            surfaceContainerHigh = SkylineColors.Border
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
                window.isStatusBarContrastEnforced = false
            }
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val customTypography = getSkylineTypography(fontStyle, textDecorations, mainTextScale, subTextScale)

    val effectiveHue = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val scheme = if (darkTheme) androidx.compose.material3.dynamicDarkColorScheme(context) else androidx.compose.material3.dynamicLightColorScheme(context)
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(scheme.primary.toArgb(), hsv)
        hsv[0]
    } else {
        customHue
    }
    val isCustomPreset = !dynamicColor && kotlin.math.abs(customHue - 14f) >= 4f
    val isCocoa = !isCustomPreset

    val expressiveColors = if (darkTheme) {
        if (isCocoa) {
            DarkExpressiveColorScheme.copy(
                accent = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) SkylineColors.Amber else ExpressiveTokens.DarkAccent,
                text = if (invertText) Color.Black else ExpressiveTokens.DarkText,
                muted = if (invertText) Color(0xFF444444) else ExpressiveTokens.DarkMuted
            )
        } else {
            ExpressiveColorScheme(
                bg = hsl(effectiveHue, 18f, (8f + lightnessOffset).coerceIn(4f, 20f)),
                container = hsl(effectiveHue, 24f, (16f + lightnessOffset).coerceIn(8f, 35f)),
                card = hsl(effectiveHue, 22f, (22f + lightnessOffset).coerceIn(12f, 45f)),
                insetCard = hsl(effectiveHue, 26f, (11f + lightnessOffset).coerceIn(5f, 25f)),
                accent = hsl(effectiveHue, 82f, (74f + lightnessOffset).coerceIn(45f, 90f)),
                ink = hsl(effectiveHue, 40f, 15f),
                sand = hsl(effectiveHue + 25f, 70f, 75f),
                text = if (invertText) Color.Black else hsl(effectiveHue, 25f, 94f),
                muted = if (invertText) Color(0xFF444444) else hsl(effectiveHue, 20f, 72f),
                line = hsl(effectiveHue, 25f, 85f),
                lineAlpha = 0.14f,
                shadow = Color(0x47000000),
                glow = hsl(effectiveHue, 25f, 35f)
            )
        }
    } else {
        if (isCocoa) {
            LightExpressiveColorScheme.copy(
                accent = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) SkylineColors.Amber else ExpressiveTokens.LightAccent,
                text = if (invertText) Color.White else ExpressiveTokens.LightText,
                muted = if (invertText) Color(0xFFCCCCCC) else ExpressiveTokens.LightMuted
            )
        } else {
            ExpressiveColorScheme(
                bg = hsl(effectiveHue, 25f, (97f + lightnessOffset).coerceIn(90f, 99f)),
                container = hsl(effectiveHue, 32f, (89f + lightnessOffset).coerceIn(78f, 95f)),
                card = hsl(effectiveHue, 30f, (81f + lightnessOffset).coerceIn(68f, 88f)),
                insetCard = hsl(effectiveHue, 32f, (71f + lightnessOffset).coerceIn(58f, 80f)),
                accent = hsl(effectiveHue, 72f, (48f + lightnessOffset).coerceIn(30f, 70f)),
                ink = Color(0xFFFFF8F2),
                sand = hsl(effectiveHue + 25f, 65f, 35f),
                text = if (invertText) Color.White else hsl(effectiveHue, 40f, 12f),
                muted = if (invertText) Color(0xFFCCCCCC) else hsl(effectiveHue, 25f, 35f),
                line = hsl(effectiveHue, 25f, 25f),
                lineAlpha = 0.09f,
                shadow = hsl(effectiveHue, 35f, 25f).copy(alpha = 0.22f),
                glow = hsl(effectiveHue, 45f, (80f + lightnessOffset).coerceIn(60f, 85f))
            )
        }
    }

    val currentAppFont = getFontFamilyForStyle(fontStyle)

    CompositionLocalProvider(
        LocalExpressiveColors provides expressiveColors,
        com.ripple.filemanager.ui.expressive.LocalCornerRoundness provides cornerRoundness,
        LocalAppFont provides currentAppFont
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = customTypography,
            content = content
        )
    }
}
