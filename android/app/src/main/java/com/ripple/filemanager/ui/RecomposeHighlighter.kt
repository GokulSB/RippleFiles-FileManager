package com.ripple.filemanager.ui

import androidx.compose.foundation.border
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * A modifier that draws a border around a composable that changes color every time it recomposes.
 * Useful for debugging excessive recompositions.
 */
@Stable
fun Modifier.recomposeHighlighter(): Modifier = this.composed {
    val recompositions = remember { mutableLongStateOf(0L) }
    recompositions.longValue++
    
    LaunchedEffect(recompositions.longValue) {
        delay(3000)
        recompositions.longValue = 0L
    }
    
    val color = when (recompositions.longValue) {
        0L -> Color.Transparent
        1L -> Color.Blue
        2L -> Color.Green
        3L -> Color.Yellow
        4L -> Color.Red
        else -> Color.Magenta
    }
    
    if (color != Color.Transparent) {
        Modifier.border(2.dp, color)
    } else {
        Modifier
    }
}
