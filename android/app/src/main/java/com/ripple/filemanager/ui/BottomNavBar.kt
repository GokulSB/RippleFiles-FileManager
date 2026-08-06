package com.ripple.filemanager.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripple.filemanager.ui.theme.SkylineColors
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class BottomNavItem(val id: String, val icon: ImageVector, val label: String)

@Composable
fun BottomNavBar(
    items: List<BottomNavItem>,
    selectedId: String,
    tapCounters: ImmutableMap<String, Int>,
    cornerRoundness: Float,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val navShape = getDynamicCornerShape(32f, cornerRoundness)
    var tabOffsets by remember { mutableStateOf(persistentMapOf<String, Float>()) }

    Box(
        modifier = modifier
            .clip(navShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, SkylineColors.Border, navShape)
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        val selectedOffset = tabOffsets[selectedId]
        val markerOffset = remember { Animatable(0f) }

        LaunchedEffect(selectedOffset) {
            if (selectedOffset != null) {
                if (markerOffset.targetValue == 0f && markerOffset.value == 0f) {
                    markerOffset.snapTo(selectedOffset)
                } else {
                    markerOffset.animateTo(
                        targetValue = selectedOffset,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
            }
        }

        // Rail Slide Marker (Background Highlight)
        if (selectedOffset != null) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(markerOffset.value.roundToInt(), 0) }
                    .width(64.dp) // Fixed width for the marker, roughly tab size
                    .matchParentSize()
                    .clip(navShape)
                    .background(SkylineColors.Amber.copy(alpha = 0.14f))
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                NavTab(
                    item = item,
                    isActive = item.id == selectedId,
                    tapCount = tapCounters[item.id] ?: 0,
                    navShape = navShape,
                    onSelect = { onSelect(item.id) },
                    onPositioned = { x ->
                        if (tabOffsets[item.id] != x) {
                            tabOffsets = tabOffsets.put(item.id, x)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun NavTab(
    item: BottomNavItem,
    isActive: Boolean,
    tapCount: Int,
    navShape: Shape,
    onSelect: () -> Unit,
    onPositioned: (Float) -> Unit
) {
    val iconTint by animateColorAsState(
        targetValue = if (isActive) SkylineColors.Amber else SkylineColors.TextDim,
        animationSpec = tween(200),
        label = "navTint_${item.id}"
    )

    // Icon Lift & Scale on Activate
    val transition = updateTransition(targetState = isActive, label = "tabTransition_${item.id}")
    
    val iconOffsetY by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                keyframes {
                    durationMillis = 480
                    0f at 0
                    -8f at 192 // 40% overshoot
                    -6f at 480 with FastOutSlowInEasing // 100% settle
                }
            } else {
                tween(200)
            }
        },
        label = "iconOffsetY_${item.id}"
    ) { active ->
        if (active) -6f else 0f
    }
    
    val iconScale by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                keyframes {
                    durationMillis = 480
                    1f at 0
                    1.20f at 192
                    1.12f at 480 with FastOutSlowInEasing
                }
            } else {
                tween(200)
            }
        },
        label = "iconScale_${item.id}"
    ) { active ->
        if (active) 1.12f else 1f
    }

    Box(
        modifier = Modifier
            .width(64.dp)
            .height(48.dp) // Fixed height to accommodate icon + revealed text
            .clip(navShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelect
            )
            .onGloballyPositioned { layoutCoordinates ->
                onPositioned(layoutCoordinates.positionInParent().x)
            },
        contentAlignment = Alignment.Center
    ) {
        // Ripple Pulse (One-shot)
        val rippleRadius = remember { Animatable(6f) }
        val rippleAlpha = remember { Animatable(0f) }
        
        LaunchedEffect(tapCount) {
            if (tapCount > 0) {
                rippleRadius.snapTo(6f)
                rippleAlpha.snapTo(0.55f)
                launch {
                    rippleRadius.animateTo(24f, animationSpec = tween(650, easing = EaseOut))
                }
                launch {
                    rippleAlpha.animateTo(0f, animationSpec = tween(650, easing = EaseOut))
                }
            }
        }

        if (rippleAlpha.value > 0f) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = SkylineColors.Amber.copy(alpha = rippleAlpha.value),
                    radius = rippleRadius.value.dp.toPx(),
                    center = center.copy(y = center.y + iconOffsetY.dp.toPx()) // Follow icon position
                )
            }
        }

        // Icon
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = iconTint,
            modifier = Modifier
                .offset { IntOffset(0, iconOffsetY.roundToInt()) }
                .size((20 * iconScale).dp) // Scale the icon manually since Modifier.scale might affect rendering
        )
        
        // Label Reveal
        AnimatedVisibility(
            visible = isActive,
            enter = fadeIn(tween(320, delayMillis = 80)) + slideInVertically(
                animationSpec = tween(320, delayMillis = 80),
                initialOffsetY = { it / 4 }
            ),
            exit = fadeOut(tween(100)), // Fast fade out when inactive
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
        ) {
            Text(
                text = item.label,
                color = SkylineColors.Amber,
                fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                fontSize = 10.sp
            )
        }
    }
}
