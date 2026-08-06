package com.ripple.filemanager.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ExpressiveCircularLoader(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    strokeWidth: Dp = 4.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    animationSpeed: Float = 1f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ExpressiveLoaderTransition")
    
    val currentPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (2000 / animationSpeed).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val baseRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (2500 / animationSpeed).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "baseRotation"
    )
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (1250 / animationSpeed).toInt(),
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = baseRotation
            }
    ) {
        val isGrowing = currentPhase < 1f
        val fraction = if (isGrowing) currentPhase else currentPhase - 1f
        val easedFraction = FastOutSlowInEasing.transform(fraction)
        
        val sweep = if (isGrowing) {
            15f + 270f * easedFraction
        } else {
            285f - 270f * easedFraction
        }
        
        val phaseOffset = currentPhase * 180f
        
        val start = if (isGrowing) {
            phaseOffset
        } else {
            phaseOffset + (270f * easedFraction)
        }

        val strokePx = strokeWidth.toPx()
        val inset = strokePx / 2f
        drawArc(
            color = color,
            startAngle = start,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(
                this.size.width - strokePx, 
                this.size.height - strokePx
            ),
            style = Stroke(
                width = strokePx,
                cap = StrokeCap.Round
            )
        )
    }
}
