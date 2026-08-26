package com.ripple.filemanager.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Fires haptic feedback for a given event, respecting per-category settings.
 *
 * Uses the Vibrator API directly rather than View.performHapticFeedback().
 * This is intentional: performHapticFeedback() is gated by the phone's
 * system-wide "touch vibration" setting, so if a user has that disabled
 * system-wide, in-app haptics would silently never fire even with this
 * toggle on. Going through Vibrator directly means this in-app setting
 * is the only gate — it always fires when enabled here, regardless of
 * the system-wide setting. Requires the VIBRATE permission (see manifest
 * note below).
 */
class HapticsController(
    private val vibrator: Vibrator?,
    private val settingsProvider: () -> com.ripple.filemanager.HapticsSettings,
) {

    fun fire(event: com.ripple.filemanager.HapticEvent) {
        val settings = settingsProvider()
        if (!settings.isEnabled(event)) return
        performVibratorHaptic(event)
    }

    fun fileOpen() = fire(com.ripple.filemanager.HapticEvent.FileOpen)
    fun copyPaste() = fire(com.ripple.filemanager.HapticEvent.CopyPaste)
    fun delete() = fire(com.ripple.filemanager.HapticEvent.Delete)
    fun cleaner() = fire(com.ripple.filemanager.HapticEvent.Cleaner)
    fun settingsToggle() = fire(com.ripple.filemanager.HapticEvent.SettingsToggle)
    fun fab() = fire(com.ripple.filemanager.HapticEvent.Fab)
    /** Use for anything not covered by the named categories above. */
    fun tap() = fire(com.ripple.filemanager.HapticEvent.General)

    private fun performVibratorHaptic(event: com.ripple.filemanager.HapticEvent) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = when (event) {
                // Delete now feels the same weight as other confirmations —
                // a single short, neutral tick, not a distinct "warning" buzz.
                com.ripple.filemanager.HapticEvent.Delete -> VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE)
                com.ripple.filemanager.HapticEvent.Cleaner -> VibrationEffect.createOneShot(22L, VibrationEffect.DEFAULT_AMPLITUDE)
                com.ripple.filemanager.HapticEvent.FileOpen -> VibrationEffect.createOneShot(12L, VibrationEffect.DEFAULT_AMPLITUDE)
                com.ripple.filemanager.HapticEvent.CopyPaste -> VibrationEffect.createOneShot(14L, VibrationEffect.DEFAULT_AMPLITUDE)
                com.ripple.filemanager.HapticEvent.SettingsToggle -> VibrationEffect.createOneShot(12L, VibrationEffect.DEFAULT_AMPLITUDE)
                com.ripple.filemanager.HapticEvent.Fab -> VibrationEffect.createOneShot(16L, VibrationEffect.DEFAULT_AMPLITUDE)
                com.ripple.filemanager.HapticEvent.General -> VibrationEffect.createOneShot(12L, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            vib.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(18L)
        }
    }
}

/** Resolves the system Vibrator across API levels. */
fun Context.getSystemVibrator(): Vibrator? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
}

/**
 * Compose entry point.
 *
 * Usage:
 *   val haptics = rememberHapticsController { appState.hapticsSettings }
 *   ...
 *   onClick = {
 *       haptics.fileOpen()
 *       viewModel.onAction(AppAction.OpenFile(file))
 *   }
 */
@Composable
fun rememberHapticsController(
    settingsProvider: () -> com.ripple.filemanager.HapticsSettings,
): HapticsController {
    val context = LocalContext.current
    val currentSettingsProvider = androidx.compose.runtime.rememberUpdatedState(settingsProvider)
    return remember(context) {
        HapticsController(
            vibrator = context.getSystemVibrator(),
            settingsProvider = { currentSettingsProvider.value() },
        )
    }
}

val LocalHaptics = androidx.compose.runtime.staticCompositionLocalOf<HapticsController> {
    error("No HapticsController provided")
}
