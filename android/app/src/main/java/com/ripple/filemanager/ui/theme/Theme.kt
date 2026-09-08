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

// Fixed light palette (fallback + base for everything non-accent)
val LightBackground = Color(0xFFFAF6EF)   // parchment, was the striped bg
val LightSurface    = Color(0xFFFFFFFF)
val LightBorder     = Color(0xFFE4DFD3)
val LightTextPrimary   = Color(0xFF2C2C2A)
val LightTextSecondary = Color(0xFF8C897E)

// Fixed dark palette (fallback + base for everything non-accent)
val DarkBackground = Color(0xFF1E1711)   // deepest warm black/brown (header background)
val DarkSurface    = Color(0xFF33281E)   // storage cards & chip area (elevated brown)
val DarkSurface2   = Color(0xFF2B2218)   // bottom sheet layout (middle elevation)
val DarkBorder     = Color(0xFF4F4235)
val DarkTextPrimary   = Color(0xFFEBE0D1)
val DarkTextSecondary = Color(0xFFB5A99A)

// Fixed amber fallback accent — used when dynamic color is off/unavailable
val AmberPrimary          = Color(0xFFEF9F27)
val AmberPrimaryContainer = Color(0xFFFAEEDA)
val AmberOnPrimary        = Color(0xFF412402)

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

    var Background    by mutableStateOf(Color(0xFF161514))
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

        if (isDark) {
            // Dynamic Dark Mode using the exact depth & contrast ratios approved from the reference
            // Background: ~9% lightness, 28% saturation
            Background = hsl(effectiveHue, 28f, (9.2f + lightnessOffset).coerceIn(0f, 100f))
            // Surface2 (Bottom sheet): ~13% lightness, 28% saturation 
            Surface2 = hsl(effectiveHue, 28f, (13.1f + lightnessOffset).coerceIn(0f, 100f))
            // Surface (Cards/Chips): ~16% lightness, 26% saturation
            Surface = hsl(effectiveHue, 26f, (15.8f + lightnessOffset).coerceIn(0f, 100f))
            // Border: ~26% lightness, 20% saturation
            Border = hsl(effectiveHue, 20f, (25.8f + lightnessOffset).coerceIn(0f, 100f))
            
            if (dynamicColor && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val dynamic = androidx.compose.material3.dynamicDarkColorScheme(context)
                Amber = dynamic.primary
                AmberDim = hsl(effectiveHue, 45f, 22f)
                ContainerSecondary = hsl(effectiveHue, 35f, 22f)
                ContainerTertiary = hsl(effectiveHue + 45f, 40f, 22f)
            } else {
                Amber = hsl(effectiveHue, 80f, 68f)
                AmberDim = hsl(effectiveHue, 45f, 22f)
                ContainerSecondary = hsl(effectiveHue, 35f, 22f)
                ContainerTertiary = hsl(effectiveHue + 45f, 40f, 22f)
            }
            
            Dust = hsl(effectiveHue + 180f, 35f, 58f)
            Sage = hsl(effectiveHue + 90f, 35f, 62f)
            Rust = hsl(12f, 65f, 54f)
            
            AccentGreen = hsl(101f, 40f, 59f)
            AccentBlue = hsl(203f, 75f, 64f)
            AccentTeal = hsl(173f, 61f, 54f)
            AccentPrimary = Amber
            AccentRed = hsl(7f, 100f, 68f)
            AccentPink = hsl(339f, 100f, 78f)
            AccentViolet = hsl(263f, 78f, 74f)
        } else {
            // Dynamic Light Mode preserving the clean parchment curves
            Background = hsl(effectiveHue, 35f, 96f)
            Surface = hsl(effectiveHue, 0f, 100f)
            Surface2 = hsl(effectiveHue, 35f, 90f)
            Border = hsl(effectiveHue, 25f, 86f)
            
            if (dynamicColor && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val dynamic = androidx.compose.material3.dynamicLightColorScheme(context)
                Amber = dynamic.primary
                AmberDim = dynamic.primaryContainer
                ContainerSecondary = dynamic.secondaryContainer
                ContainerTertiary = dynamic.tertiaryContainer
            } else {
                Amber = hsl(effectiveHue, 80f, 60f)
                AmberDim = hsl(effectiveHue, 60f, 90f)
                ContainerSecondary = hsl(effectiveHue, 40f, 90f)
                ContainerTertiary = hsl(effectiveHue + 45f, 40f, 90f)
            }
            
            Dust = hsl(effectiveHue + 180f, 35f, 58f)
            Sage = hsl(effectiveHue + 90f, 35f, 62f)
            Rust = hsl(12f, 65f, 54f)
            
            AccentGreen = hsl(101f, 40f, 59f)
            AccentBlue = hsl(203f, 75f, 64f)
            AccentTeal = hsl(173f, 61f, 54f)
            AccentPrimary = Amber
            AccentRed = hsl(7f, 100f, 68f)
            AccentPink = hsl(339f, 100f, 78f)
            AccentViolet = hsl(263f, 78f, 74f)
        }
        
        if (invertText) {
            TextPrimary = Color.Black
            TextPrimary2 = Color(0xFF222222)
            TextDim = Color(0xFF444444)
            TextDim2 = Color(0xFF666666)
        } else {
            if (isDark) {
                TextPrimary = Color.White
                TextPrimary2 = Color(0xFFF0F0F0)
                TextDim = Color(0xFFCCCCCC)
                TextDim2 = Color(0xFFAAAAAA)
            } else {
                TextPrimary = LightTextPrimary
                TextPrimary2 = LightTextPrimary
                TextDim = LightTextSecondary
                TextDim2 = LightTextSecondary
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
            background           = SkylineColors.Background,
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
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val customTypography = getSkylineTypography(fontStyle, textDecorations, mainTextScale, subTextScale)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = customTypography,
        content = content
    )
}
