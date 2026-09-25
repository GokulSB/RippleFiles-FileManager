package com.ripple.filemanager.ui.expressive

import android.animation.ValueAnimator
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Centralized motion tokens and helpers for the Expressive design system.
 *
 * PRINCIPLES:
 * - One motion vocabulary reused app-wide:
 *   - [SpringSpec]: for anything that settles into place (sheets, FAB rotation, selection checks, pill/tab switches, item layout).
 *   - [FastTween], [FadeTween]: for elements that appear or disappear (toasts, scrims, content crossfades).
 * - Full reduce-motion support: checks system animation scales and provides instant / simplified fallbacks.
 */
object ExpressiveMotion {

    /**
     * Shared spring spec for anything that settles into place.
     * Medium bouncy damping with medium stiffness provides a lively yet purposeful tactile feel.
     */
    val SpringSpec: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val DpSpringSpec: SpringSpec<Dp> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val IntOffsetSpringSpec: SpringSpec<IntOffset> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val IntSizeSpringSpec: SpringSpec<IntSize> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /**
     * Fast tween for appearance transitions (200ms).
     */
    val FastTween: TweenSpec<Float> = tween(
        durationMillis = 200,
        easing = FastOutSlowInEasing
    )

    val FastDpTween: TweenSpec<Dp> = tween(
        durationMillis = 200,
        easing = FastOutSlowInEasing
    )

    val IntOffsetFastTween: TweenSpec<IntOffset> = tween(
        durationMillis = 200,
        easing = FastOutSlowInEasing
    )

    val IntOffsetExitTween: TweenSpec<IntOffset> = tween(
        durationMillis = 180,
        easing = FastOutSlowInEasing
    )

    val IntSizeFastTween: TweenSpec<IntSize> = tween(
        durationMillis = 200,
        easing = FastOutSlowInEasing
    )

    val IntSizeExitTween: TweenSpec<IntSize> = tween(
        durationMillis = 180,
        easing = FastOutSlowInEasing
    )

    /**
     * Standard fade tween for disappearing elements and scrims (180ms).
     */
    val FadeTween: TweenSpec<Float> = tween(
        durationMillis = 180,
        easing = LinearEasing
    )

    /**
     * Exit tween for collapsing items (180ms).
     */
    val ExitTween: TweenSpec<Float> = tween(
        durationMillis = 180,
        easing = FastOutSlowInEasing
    )

    /**
     * Color transition tween (200ms).
     */
    val ColorTween: TweenSpec<Color> = tween(
        durationMillis = 200,
        easing = FastOutSlowInEasing
    )

    /**
     * Returns true if animations should be disabled or simplified due to:
     * - Compose preview mode (LocalInspectionMode)
     * - System reduce-motion / animator duration scale == 0
     * - System transition animation scale == 0
     */
    @Composable
    fun isReducedMotion(): Boolean {
        if (LocalInspectionMode.current) return true
        val context = LocalContext.current
        return remember(context) {
            try {
                val resolver = context.contentResolver
                val transitionScale = Settings.Global.getFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
                val animatorScale = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
                val valueAnimatorScale = ValueAnimator.getDurationScale()
                transitionScale == 0f || animatorScale == 0f || valueAnimatorScale == 0f
            } catch (e: Exception) {
                ValueAnimator.getDurationScale() == 0f
            }
        }
    }
}

/**
 * Reusable press-scale modifier for tappable elements (cookie buttons, chips, cards, rows).
 * Scales down to [pressedScale] (default 96%) on touch down, springs back on touch release.
 * Respects system reduce-motion settings.
 */
fun Modifier.pressScale(
    pressedScale: Float = 0.96f,
    interactionSource: MutableInteractionSource? = null
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val reduced = ExpressiveMotion.isReducedMotion()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reduced) pressedScale else 1f,
        animationSpec = if (reduced) snap() else ExpressiveMotion.SpringSpec,
        label = "press_scale"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
