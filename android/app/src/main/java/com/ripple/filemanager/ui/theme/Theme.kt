package com.ripple.filemanager.ui.theme

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

fun hsl(h: Float, s: Float, l: Float): Color {
    return Color(ColorUtils.HSLToColor(floatArrayOf(h, s / 100f, l / 100f)))
}

// ── Skyline Ledger design tokens ──────────────────────────────────────────────
object SkylineColors {
    val Background    = Color(0xFF161009)
    val Surface       = Color(0xFF1F1710)
    val Surface2      = Color(0xFF271C12)
    val Border        = Color(0xFF3A2C1C)
    val Amber         = Color(0xFFE0AC70)
    val AmberDim      = Color(0xFF8A6A44)
    val Dust          = Color(0xFF7C93A0)   // folders / generic
    val Sage          = Color(0xFF8BA888)   // media: photo / video
    val Rust          = Color(0xFFC1654A)   // alerts / destructive
    val TextPrimary   = Color(0xFFF0E4D0)
    val TextPrimary2  = Color(0xFFF4EAD9)
    val TextDim       = Color(0xFF8A7A63)
    val TextDim2      = Color(0xFF6F6250)
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
    "zip","rar","7z" -> "ARC"
    "apk"     -> "APK"
    else      -> "SYS"
}

val SkylineLedgerColorScheme: ColorScheme = darkColorScheme(
    primary              = SkylineColors.Amber,
    onPrimary            = SkylineColors.Background,
    primaryContainer     = SkylineColors.AmberDim,
    onPrimaryContainer   = SkylineColors.TextPrimary,
    secondary            = SkylineColors.Dust,
    onSecondary          = SkylineColors.Background,
    secondaryContainer   = Color(0xFF2A1F14),
    onSecondaryContainer = SkylineColors.TextPrimary,
    tertiary             = SkylineColors.Sage,
    onTertiary           = SkylineColors.Background,
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
    inverseOnSurface     = SkylineColors.Background,
    inversePrimary       = SkylineColors.AmberDim,
    scrim                = Color(0xCC000000)
)

// ── Legacy HSL schemes (kept for light mode + user hue picker) ────────────────
fun customLightColors(h: Float): ColorScheme = lightColorScheme(
    primary = hsl(h, 60f, 40f),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = hsl(h, 70f, 90f),
    onPrimaryContainer = hsl(h, 70f, 10f),
    secondaryContainer = hsl(h, 30f, 90f),
    onSecondaryContainer = hsl(h, 30f, 10f),
    surface = hsl(h, 15f, 98f),
    surfaceContainer = hsl(h, 15f, 94f),
    surfaceContainerHigh = hsl(h, 15f, 90f),
    onSurface = hsl(h, 15f, 10f),
    onSurfaceVariant = hsl(h, 15f, 30f),
    outlineVariant = hsl(h, 15f, 80f),
    background = hsl(h, 15f, 98f),
    onBackground = hsl(h, 15f, 10f)
)

fun customDarkColors(h: Float): ColorScheme = darkColorScheme(
    primary = hsl(h, 60f, 65f),
    onPrimary = hsl(h, 60f, 10f),
    primaryContainer = hsl(h, 40f, 30f),
    onPrimaryContainer = hsl(h, 40f, 90f),
    secondaryContainer = hsl(h, 30f, 30f),
    onSecondaryContainer = hsl(h, 30f, 90f),
    surface = hsl(h, 15f, 6f),
    surfaceContainer = hsl(h, 15f, 12f),
    surfaceContainerHigh = hsl(h, 15f, 18f),
    onSurface = hsl(h, 15f, 90f),
    onSurfaceVariant = hsl(h, 15f, 70f),
    outlineVariant = hsl(h, 15f, 25f),
    background = hsl(h, 15f, 6f),
    onBackground = hsl(h, 15f, 90f)
)

@Composable
fun SiftTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    customHue: Float = 262f,
    fontStyle: String = "System",
    textDecorations: Set<String> = emptySet(),
    mainTextScale: Float = 1.0f,
    subTextScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Skyline Ledger is the default dark scheme when dynamic color is off
        darkTheme -> SkylineLedgerColorScheme
        else -> customLightColors(customHue)
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
