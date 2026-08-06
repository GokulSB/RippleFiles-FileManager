import os
import re

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'r', encoding='utf-8') as f:
    app_content = f.read()

# 1. Update FAB Popup Offset
target_popup = '''                                    androidx.compose.ui.window.Popup(
                                        alignment = Alignment.BottomCenter,
                                        offset = androidx.compose.ui.unit.IntOffset(0, yOffset),'''
replacement_popup = '''                                    androidx.compose.ui.window.Popup(
                                        alignment = Alignment.BottomCenter,
                                        offset = androidx.compose.ui.unit.IntOffset(with(density) { (-24).dp.roundToPx() }, yOffset),'''
app_content = app_content.replace(target_popup, replacement_popup)

# 2. Replace SquigglyProgressIndicator invocation
target_squiggly_call = '''                        SquigglyProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth().height(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )'''
replacement_squiggly_call = '''                        GradientProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )'''
app_content = app_content.replace(target_squiggly_call, replacement_squiggly_call)

# 3. Replace SquigglyProgressIndicator implementation with GradientProgressIndicator
squiggly_impl_pattern = r"@Composable\nfun SquigglyProgressIndicator[\s\S]*?\}\n\}"
gradient_impl = '''@Composable
fun GradientProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainerHigh
) {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "gradient")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(2000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "offset"
    )

    val brush = androidx.compose.ui.graphics.Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.primary
        ),
        start = androidx.compose.ui.geometry.Offset(offset, 0f),
        end = androidx.compose.ui.geometry.Offset(offset + 500f, 0f),
        tileMode = androidx.compose.ui.graphics.TileMode.Mirror
    )

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val progressWidth = width * progress
        val strokeW = height

        // Draw track
        drawLine(
            color = trackColor,
            start = androidx.compose.ui.geometry.Offset(0f, height / 2),
            end = androidx.compose.ui.geometry.Offset(width, height / 2),
            strokeWidth = strokeW,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        // Draw progress with gradient
        if (progressWidth > 0) {
            drawLine(
                brush = brush,
                start = androidx.compose.ui.geometry.Offset(0f, height / 2),
                end = androidx.compose.ui.geometry.Offset(progressWidth, height / 2),
                strokeWidth = strokeW,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}'''
app_content = re.sub(squiggly_impl_pattern, gradient_impl, app_content)

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'w', encoding='utf-8') as f:
    f.write(app_content)
