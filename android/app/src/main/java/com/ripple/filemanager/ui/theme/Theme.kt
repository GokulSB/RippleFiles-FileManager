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

fun hsl(h: Float, s: Float, l: Float): Color {
    return Color(ColorUtils.HSLToColor(floatArrayOf(h, s / 100f, l / 100f)))
}

// ── Skyline Ledger design tokens ──────────────────────────────────────────────
object SkylineColors {
    var Background    by mutableStateOf(Color(0xFF161009))
    var Surface       by mutableStateOf(Color(0xFF1F1710))
    var Surface2      by mutableStateOf(Color(0xFF271C12))
    var Border        by mutableStateOf(Color(0xFF3A2C1C))
    var Amber         by mutableStateOf(Color(0xFFE0AC70))
    var AmberDim      by mutableStateOf(Color(0xFF8A6A44))
    var Dust          by mutableStateOf(Color(0xFF7C93A0))   // folders / generic
    var Sage          by mutableStateOf(Color(0xFF8BA888))   // media: photo / video
    var Rust          by mutableStateOf(Color(0xFFC1654A))   // alerts / destructive
    var TextPrimary   by mutableStateOf(Color(0xFFF0E4D0))
    var TextPrimary2  by mutableStateOf(Color(0xFFF4EAD9))
    var TextDim       by mutableStateOf(Color(0xFF8A7A63))
    var TextDim2      by mutableStateOf(Color(0xFF6F6250))
    
    fun updateColors(isDark: Boolean, dynamicColor: Boolean, customHue: Float, context: android.content.Context) {
        val useSystem = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val effectiveHue = if (useSystem) {
            val scheme = if (isDark) androidx.compose.material3.dynamicDarkColorScheme(context) else androidx.compose.material3.dynamicLightColorScheme(context)
            val hsl = FloatArray(3)
            androidx.core.graphics.ColorUtils.colorToHSL(scheme.primary.toArgb(), hsl)
            hsl[0]
        } else {
            customHue
        }
        
        if (isDark) {
            Background = hsl(effectiveHue, 41f, 6f)
            Surface = hsl(effectiveHue, 31f, 9f)
            Surface2 = hsl(effectiveHue, 30f, 12f)
            Border = hsl(effectiveHue, 34f, 16f)
            Amber = hsl(effectiveHue, 65f, 66f)
            AmberDim = hsl(effectiveHue, 35f, 40f)
            Dust = hsl(effectiveHue + 180f, 20f, 55f) // Complementary / cool
            Sage = hsl(effectiveHue + 90f, 20f, 60f)  // Analogous
            Rust = hsl(12f, 50f, 52f) // Keep alerts reddish
            TextPrimary = hsl(effectiveHue, 47f, 87f)
            TextPrimary2 = hsl(effectiveHue, 47f, 83f)
            TextDim = hsl(effectiveHue, 25f, 46f)
            TextDim2 = hsl(effectiveHue, 20f, 37f)
        } else {
            Background = hsl(40f, 65f, 95f)
            Surface = hsl(40f, 55f, 90f)
            Surface2 = hsl(40f, 50f, 85f)
            Border = hsl(effectiveHue, 31f, 71f)
            Amber = hsl(effectiveHue, 65f, 66f)
            AmberDim = hsl(effectiveHue, 48f, 48f)
            Dust = hsl(effectiveHue + 180f, 25f, 45f)
            Sage = hsl(effectiveHue + 90f, 25f, 45f)
            Rust = hsl(12f, 55f, 45f)
            TextPrimary = hsl(effectiveHue, 36f, 12f)
            TextPrimary2 = hsl(effectiveHue, 35f, 17f)
            TextDim = hsl(effectiveHue, 23f, 33f)
            TextDim2 = hsl(effectiveHue, 16f, 46f)
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
    "zip","rar","7z" -> "ARC"
    "apk"     -> "APK"
    else      -> "SYS"
}

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
    val context = LocalContext.current
    SkylineColors.updateColors(darkTheme, dynamicColor, customHue, context)
    
    val colorScheme = if (darkTheme) {
        darkColorScheme(
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
    } else {
        lightColorScheme(
            primary              = SkylineColors.Amber,
            onPrimary            = SkylineColors.Background,
            primaryContainer     = SkylineColors.AmberDim,
            onPrimaryContainer   = SkylineColors.TextPrimary,
            secondary            = SkylineColors.Dust,
            onSecondary          = SkylineColors.Background,
            secondaryContainer   = SkylineColors.Surface2,
            onSecondaryContainer = SkylineColors.TextPrimary,
            tertiary             = SkylineColors.Sage,
            onTertiary           = SkylineColors.Background,
            error                = SkylineColors.Rust,
            onError              = SkylineColors.TextPrimary,
            errorContainer       = SkylineColors.Rust.copy(alpha = 0.2f),
            onErrorContainer     = SkylineColors.TextPrimary,
            background           = SkylineColors.Background,
            onBackground         = SkylineColors.TextPrimary,
            surface              = SkylineColors.Surface,
            onSurface            = SkylineColors.TextPrimary,
            surfaceVariant       = SkylineColors.Surface2,
            onSurfaceVariant     = SkylineColors.TextDim,
            surfaceContainer     = SkylineColors.Surface2,
            surfaceContainerHigh = SkylineColors.Border,
            outline              = SkylineColors.Border,
            outlineVariant       = SkylineColors.Surface2,
            inverseSurface       = SkylineColors.TextPrimary,
            inverseOnSurface     = SkylineColors.Background,
            inversePrimary       = SkylineColors.AmberDim,
            scrim                = Color(0xCC000000)
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
