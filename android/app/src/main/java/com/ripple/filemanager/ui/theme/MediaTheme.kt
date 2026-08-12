package com.ripple.filemanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class SkylineLedgerColors(
    val background: Color,
    val surface: Color,
    val amberAccent: Color,
    val textPrimary: Color, // cream in dark, ink in light
    val hairline: Color,
    val textFaintPrimary: Color,
    val textFaintSecondary: Color,
    val shadow: Color,
    val thumbnailBackground: Color,
    val chipBackground: Color
)

val LocalSkylineLedgerColors = staticCompositionLocalOf {
    // Default to dark mode colors if not provided
    DarkSkylineLedgerColors
}

val DarkSkylineLedgerColors = SkylineLedgerColors(
    background = Color(0xFF101010),
    surface = Color(0xFF1E1E1E),
    amberAccent = Color(0xFFFFB300),
    textPrimary = Color(0xFFF0F0F0), // cream
    hairline = Color(0xFF333333),
    textFaintPrimary = Color(0xFFAAAAAA),
    textFaintSecondary = Color(0xFF777777),
    shadow = Color(0x66000000),
    thumbnailBackground = Color(0xFF2C2C2C),
    chipBackground = Color(0xFF332A1C)
)

val LightSkylineLedgerColors = SkylineLedgerColors(
    background = Color(0xFFF5F5F5),
    surface = Color(0xFFFFFFFF),
    amberAccent = Color(0xFFFF8F00),
    textPrimary = Color(0xFF121212), // ink
    hairline = Color(0xFFE0E0E0),
    textFaintPrimary = Color(0xFF666666),
    textFaintSecondary = Color(0xFF999999),
    shadow = Color(0x1A000000),
    thumbnailBackground = Color(0xFFE8E8E8),
    chipBackground = Color(0xFFFFF3E0)
)

@Composable
fun ProvideSkylineLedgerColors(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkSkylineLedgerColors else LightSkylineLedgerColors
    CompositionLocalProvider(LocalSkylineLedgerColors provides colors) {
        content()
    }
}
