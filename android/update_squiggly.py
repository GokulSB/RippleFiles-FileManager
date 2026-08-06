import os
import re

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'r', encoding='utf-8') as f:
    content = f.read()

target = '''                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )'''

replacement = '''                        SquigglyProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth().height(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )'''

content = content.replace(target, replacement)

squiggly_impl = '''
@Composable
fun SquigglyProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    trackColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainerHigh
) {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "squiggly")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "phase"
    )

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val progressWidth = width * progress
        val strokeW = 4.dp.toPx()

        // Draw track
        drawLine(
            color = trackColor,
            start = androidx.compose.ui.geometry.Offset(0f, height / 2),
            end = androidx.compose.ui.geometry.Offset(width, height / 2),
            strokeWidth = strokeW,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        // Draw squiggly progress
        if (progressWidth > 0) {
            val path = androidx.compose.ui.graphics.Path()
            val amplitude = (height - strokeW) / 2 * 0.7f
            val frequency = 0.1f // frequency of waves
            
            path.moveTo(0f, height / 2)
            
            var x = 0f
            while (x <= progressWidth) {
                val y = height / 2 + (Math.sin((x * frequency - phase).toDouble()) * amplitude).toFloat()
                path.lineTo(x, y)
                x += 2f
            }
            
            androidx.compose.ui.graphics.drawscope.drawIntoCanvas { canvas ->
                val paint = androidx.compose.ui.graphics.Paint().apply {
                    this.color = color
                    this.strokeWidth = strokeW
                    this.style = androidx.compose.ui.graphics.PaintingStyle.Stroke
                    this.strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    this.isAntiAlias = true
                }
                canvas.drawPath(path, paint)
            }
        }
    }
}
'''
if "fun SquigglyProgressIndicator" not in content:
    content += squiggly_impl

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'w', encoding='utf-8') as f:
    f.write(content)
