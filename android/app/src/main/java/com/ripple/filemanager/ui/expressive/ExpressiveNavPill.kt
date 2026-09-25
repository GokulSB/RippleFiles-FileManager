package com.ripple.filemanager.ui.expressive

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripple.filemanager.R

import com.ripple.filemanager.ui.theme.OutfitFontFamily
import com.ripple.filemanager.ui.theme.LocalAppFont

enum class ExpressiveTab(val id: String, val label: String, val icon: ImageVector) {
    HOME("home", "Home", Icons.Outlined.Home),
    BROWSE("browse", "Browse", Icons.Outlined.Folder),
    CLOUD("cloud", "Cloud", Icons.Outlined.Cloud),
    SEND("send", "Send", Icons.AutoMirrored.Outlined.Send)
}

/**
 * Floating navigation pill with cookie FAB to the right.
 * Morphs into multi-select action bar when selectionMode is active.
 */
@Composable
fun ExpressiveNavShell(
    activeTab: ExpressiveTab,
    onTabSelected: (ExpressiveTab) -> Unit,
    onFabClick: () -> Unit,
    isSelectionMode: Boolean,
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onSelectAll: (() -> Unit)? = null,
    onSelectNone: (() -> Unit)? = null,
    onBatchRename: (() -> Unit)? = null,
    isFabExpanded: Boolean = false
) {
    if (isSelectionMode) {
        ExpressiveSelectionBar(
            selectedCount = selectedCount,
            onClearSelection = onClearSelection,
            onSelectAll = onSelectAll ?: {},
            onSelectNone = onSelectNone ?: {},
            onBatchRename = onBatchRename ?: {},
            onShare = onShare,
            onCopy = onCopy,
            onCut = onMove,
            onDelete = onDelete,
            modifier = modifier
        )
    } else {
        val colors = ExpressiveTheme.colors
        val reduced = ExpressiveMotion.isReducedMotion()
        var selectedTab by remember(activeTab) { mutableStateOf(activeTab) }

        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Floating pill with Home, Browse, Cloud, Send
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(30.dp),
                        ambientColor = Color.Transparent,
                        spotColor = colors.shadow.copy(alpha = 0.25f)
                    )
                    .clip(RoundedCornerShape(30.dp))
                    .background(colors.insetCard)
                    .border(1.dp, colors.line.copy(alpha = colors.lineAlpha), RoundedCornerShape(30.dp))
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ExpressiveTab.values().forEach { tab ->
                    val isActive = tab == selectedTab
                    ExpressiveNavTabItem(
                        tab = tab,
                        isActive = isActive,
                        onClick = {
                            if (selectedTab != tab) {
                                selectedTab = tab
                            }
                            onTabSelected(tab)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Cookie FAB (66dp, accent, "+")
            val fabShape = remember { CookieShape(lobes = 10, amplitude = 0.08f) }
            val fabInteractionSource = remember { MutableInteractionSource() }
            val fabRotation by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isFabExpanded) 45f else 0f,
                animationSpec = if (reduced) androidx.compose.animation.core.snap() else ExpressiveMotion.SpringSpec,
                label = "fab_rotation"
            )

            Box(
                modifier = Modifier
                    .size(66.dp)
                    .pressScale(pressedScale = 0.94f, interactionSource = fabInteractionSource)
                    .shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        ambientColor = Color.Transparent,
                        spotColor = colors.shadow.copy(alpha = 0.25f)
                    )
                    .clip(fabShape)
                    .background(colors.accent)
                    .clickable(
                        interactionSource = fabInteractionSource,
                        indication = null,
                        onClick = onFabClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (isFabExpanded) "Close" else "Create",
                    tint = colors.ink,
                    modifier = Modifier
                        .size(30.dp)
                        .graphicsLayer { rotationZ = fabRotation }
                )
            }
        }
    }
}

@Composable
private fun ExpressiveNavTabItem(
    tab: ExpressiveTab,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val colors = ExpressiveTheme.colors
    val reduced = ExpressiveMotion.isReducedMotion()
    val interactionSource = remember { MutableInteractionSource() }

    val tabBgColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isActive) colors.card else Color.Transparent,
        animationSpec = if (reduced) androidx.compose.animation.core.snap() else androidx.compose.animation.core.tween(160, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "tab_bg_color"
    )

    val iconTint by androidx.compose.animation.animateColorAsState(
        targetValue = if (isActive) colors.text else colors.muted,
        animationSpec = if (reduced) androidx.compose.animation.core.snap() else androidx.compose.animation.core.tween(160, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "tab_icon_tint"
    )

    Box(
        modifier = Modifier
            .height(46.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(tabBgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .animateContentSize(
                animationSpec = if (reduced) androidx.compose.animation.core.snap() else androidx.compose.animation.core.tween(160, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            androidx.compose.animation.AnimatedVisibility(
                visible = isActive,
                enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(140, delayMillis = 20)) +
                        androidx.compose.animation.expandHorizontally(animationSpec = androidx.compose.animation.core.tween(160, easing = androidx.compose.animation.core.FastOutSlowInEasing)),
                exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(80)) +
                       androidx.compose.animation.shrinkHorizontally(animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.FastOutSlowInEasing))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tab.label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = LocalAppFont.current,
                        color = colors.text,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
