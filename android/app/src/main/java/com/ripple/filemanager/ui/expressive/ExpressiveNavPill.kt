package com.ripple.filemanager.ui.expressive

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.ripple.filemanager.IconShapeType

import com.ripple.filemanager.ui.theme.OutfitFontFamily
import com.ripple.filemanager.ui.theme.LocalAppFont

enum class ExpressiveTab(val id: String, val label: String, val icon: ImageVector) {
    HOME("home", "Home", Icons.Outlined.Home),
    BROWSE("browse", "Browse", Icons.Outlined.Folder),
    VAULT("vault", "Vault", Icons.Outlined.Lock),
    SEND("send", "Send", Icons.AutoMirrored.Outlined.Send)
}

/**
 * Expressive navigation shell.
 * The bottom navigation bar is removed entirely.
 * Only the FAB button is kept, with a small send FAB stacked directly on top.
 * Morphs into multi-select action bar when selectionMode is active.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
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
    onArchive: (() -> Unit)? = null,
    isFabExpanded: Boolean = false,
    onToggleHide: (() -> Unit)? = null,
    isAllHidden: Boolean = false,
    isVaultVisible: Boolean = false,
    onFabLongClick: (() -> Unit)? = null,
    onSendClick: (() -> Unit)? = null,
    iconShape: IconShapeType = LocalIconShapeType.current
) {
    if (isSelectionMode) {
        ExpressiveSelectionBar(
            selectedCount = selectedCount,
            onClearSelection = onClearSelection,
            onSelectAll = onSelectAll ?: {},
            onSelectNone = onSelectNone ?: {},
            onBatchRename = onBatchRename ?: {},
            onArchive = onArchive,
            onShare = onShare,
            onCopy = onCopy,
            onCut = onMove,
            onDelete = onDelete,
            modifier = modifier,
            onToggleHide = onToggleHide,
            isAllHidden = isAllHidden
        )
    } else {
        val colors = ExpressiveTheme.colors
        val reduced = ExpressiveMotion.isReducedMotion()

        // ── Main Primary FAB (+) ──
        val roundness = LocalCornerRoundness.current
        val fabShape = rememberIconShape(type = iconShape, lobes = 10, amplitude = 0.08f, cornerRoundness = roundness)
        val fabMaxR = when (iconShape) {
            IconShapeType.SQUARE -> 8f
            IconShapeType.ROUNDED_SQUARE -> 24f
            IconShapeType.IOS -> 26f
            IconShapeType.ONE_UI -> 28f
            IconShapeType.CIRCLE, IconShapeType.COOKIE, IconShapeType.AUTO -> 30f
        }
        val fabShadowShape = RoundedCornerShape((fabMaxR * roundness.coerceIn(0f, 1f)).dp)
        val fabInteractionSource = remember { MutableInteractionSource() }
        val fabRotation by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (isFabExpanded) 45f else 0f,
            animationSpec = if (reduced) androidx.compose.animation.core.snap() else ExpressiveMotion.SpringSpec,
            label = "fab_rotation"
        )

        // ── Small Send FAB on Top ──
        val sendShape = rememberIconShape(type = iconShape, lobes = 8, amplitude = 0.08f, cornerRoundness = roundness)
        val sendMaxR = when (iconShape) {
            IconShapeType.SQUARE -> 8f
            IconShapeType.ROUNDED_SQUARE -> 18f
            IconShapeType.IOS -> 20f
            IconShapeType.ONE_UI -> 22f
            IconShapeType.CIRCLE, IconShapeType.COOKIE, IconShapeType.AUTO -> 22f
        }
        val sendShadowShape = RoundedCornerShape((sendMaxR * roundness.coerceIn(0f, 1f)).dp)
        val sendInteractionSource = remember { MutableInteractionSource() }
        val isSendActive = activeTab == ExpressiveTab.SEND
        val sendBg = if (isSendActive) colors.accent else colors.card
        val sendIconTint = if (isSendActive) colors.ink else colors.accent

        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                modifier = Modifier
                    .padding(end = 16.dp, bottom = 0.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Small FAB-like button on top with Send icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .pressScale(pressedScale = 0.92f, interactionSource = sendInteractionSource)
                        .shadow(
                            elevation = 4.dp,
                            shape = sendShadowShape,
                            ambientColor = Color.Transparent,
                            spotColor = colors.shadow.copy(alpha = 0.20f)
                        )
                        .clip(sendShape)
                        .background(sendBg)
                        .border(
                            width = 1.dp,
                            color = if (isSendActive) Color.Transparent else colors.line.copy(alpha = colors.lineAlpha),
                            shape = sendShape
                        )
                        .clickable(
                            interactionSource = sendInteractionSource,
                            indication = null,
                            onClick = {
                                if (onSendClick != null) {
                                    onSendClick()
                                } else {
                                    if (isSendActive) {
                                        onTabSelected(ExpressiveTab.HOME)
                                    } else {
                                        onTabSelected(ExpressiveTab.SEND)
                                    }
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send & Receive",
                        tint = sendIconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Primary Creation FAB on bottom
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .pressScale(pressedScale = 0.94f, interactionSource = fabInteractionSource)
                        .shadow(
                            elevation = 6.dp,
                            shape = fabShadowShape,
                            ambientColor = Color.Transparent,
                            spotColor = colors.shadow.copy(alpha = 0.25f)
                        )
                        .clip(fabShape)
                        .background(colors.accent)
                        .combinedClickable(
                            interactionSource = fabInteractionSource,
                            indication = null,
                            onClick = onFabClick,
                            onLongClick = onFabLongClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = if (isFabExpanded) "Close" else "Create",
                        tint = colors.ink,
                        modifier = Modifier
                            .size(26.dp)
                            .graphicsLayer { rotationZ = fabRotation }
                    )
                }
            }
        }
    }
}
