package com.ripple.filemanager.ui.expressive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripple.filemanager.R
import com.ripple.filemanager.ui.theme.LocalAppFont

/**
 * Expressive selection action sheet matching the login / delete sheet design system:
 * - Collapsible style sheet, starts collapsed for maximum visibility of selected files
 * - 34dp rounded top corners
 * - Container background color
 * - 40x4dp centered grab handle
 * - CookieIcon header with Outfit typography
 * - Clean action cards with icons and text labels
 * - Secondary pill Cancel/Done button
 */
@Composable
fun ExpressiveSelectionBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    onBatchRename: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onInstallApks: (() -> Unit)? = null,
    isSingleApk: Boolean = false,
    onPaste: (() -> Unit)? = null
) {
    val colors = ExpressiveTheme.colors
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current

    var isExpanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "selection_chevron_rot"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
                ambientColor = Color.Transparent,
                spotColor = colors.shadow.copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
            .background(colors.container)
            .border(1.dp, colors.line.copy(alpha = colors.lineAlpha), RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
            .animateContentSize(animationSpec = tween(220, easing = FastOutSlowInEasing))
            .navigationBarsPadding()
            .padding(top = 10.dp, bottom = if (isExpanded) 16.dp else 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Centered grab handle (toggles expansion on tap)
        Box(
            modifier = Modifier
                .padding(bottom = 8.dp)
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.muted.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    haptics.tap()
                    isExpanded = !isExpanded
                }
        )

        // Header Row: CookieIcon + title/subtitle + chevron toggle + close button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val pastel = ExpressiveTokens.categoryPastel("docs")
            CookieIcon(
                icon = Icons.Outlined.Checklist,
                bgColor = pastel,
                iconTint = ExpressiveTokens.OnPastel,
                size = 46.dp,
                lobes = 8
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        haptics.tap()
                        isExpanded = !isExpanded
                    }
            ) {
                Text(
                    text = if (selectedCount == 1) "1 item selected" else "$selectedCount items selected",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = LocalAppFont.current,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isExpanded) "Choose an action below" else "Tap to view actions",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = LocalAppFont.current,
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Chevron toggle button (pointing up when collapsed, rotating to down when expanded)
            CookieIconButton(
                onClick = {
                    haptics.tap()
                    isExpanded = !isExpanded
                },
                icon = Icons.Default.KeyboardArrowUp,
                bgColor = colors.card,
                iconTint = colors.text,
                description = if (isExpanded) "Collapse actions" else "Expand actions",
                size = 36.dp,
                iconSize = 20.dp,
                modifier = Modifier.rotate(chevronRotation),
                lobes = 8
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Direct close/clear selection button
            CookieIconButton(
                onClick = {
                    haptics.tap()
                    onClearSelection()
                },
                icon = Icons.Default.Close,
                bgColor = colors.card,
                iconTint = colors.text,
                description = "Clear selection",
                size = 36.dp,
                iconSize = 18.dp,
                lobes = 8
            )
        }

        // Expandable actions section
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(180)),
            exit = shrinkVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(150))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Actions: Horizontal scroll row of expressive cards with labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Copy
            SelectionActionCard(
                icon = Icons.Outlined.ContentCopy,
                label = stringResource(R.string.copy_action),
                onClick = onCopy
            )

            // 2. Cut
            SelectionActionCard(
                icon = Icons.Outlined.ContentCut,
                label = stringResource(R.string.cut_action),
                onClick = onCut
            )

            // Optional Paste (when clipboard has active items)
            if (onPaste != null) {
                SelectionActionCard(
                    icon = Icons.Outlined.ContentPaste,
                    label = "Paste",
                    onClick = onPaste
                )
            }

            // 3. Delete (Destructive)
            SelectionActionCard(
                icon = Icons.Outlined.Delete,
                label = stringResource(R.string.delete_label),
                isDestructive = true,
                onClick = onDelete
            )

            // 4. Rename / Batch Rename
            SelectionActionCard(
                icon = Icons.Outlined.Edit,
                label = if (selectedCount == 1) "Rename" else stringResource(R.string.batch_rename),
                onClick = onBatchRename
            )

            // 5. Share
            SelectionActionCard(
                icon = Icons.Outlined.Share,
                label = stringResource(R.string.share),
                onClick = onShare
            )

            // 6. Select All
            SelectionActionCard(
                icon = Icons.Outlined.SelectAll,
                label = stringResource(R.string.select_all),
                onClick = onSelectAll
            )

            // 7. Deselect / Select None
            SelectionActionCard(
                icon = Icons.Default.Deselect,
                label = stringResource(R.string.select_none),
                onClick = onSelectNone
            )

            // Optional: APK Install
            if (onInstallApks != null) {
                SelectionActionCard(
                    icon = if (isSingleApk) Icons.Default.Android else Icons.Default.Apps,
                    label = if (isSingleApk) "Install" else "Batch Install",
                    isHighlight = true,
                    onClick = onInstallApks
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Full-width pill cancel/done button
        Button(
            onClick = {
                haptics.tap()
                onClearSelection()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.card,
                contentColor = colors.text
            )
        ) {
            Text(
                text = "Done",
                fontFamily = LocalAppFont.current,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
}
}

@Composable
private fun SelectionActionCard(
    icon: ImageVector,
    label: String,
    isDestructive: Boolean = false,
    isHighlight: Boolean = false,
    onClick: () -> Unit
) {
    val colors = ExpressiveTheme.colors
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current

    val containerBg = when {
        isDestructive -> colors.accent.copy(alpha = 0.12f)
        isHighlight -> colors.accent
        else -> colors.card
    }
    val contentColor = when {
        isDestructive -> colors.accent
        isHighlight -> colors.ink
        else -> colors.text
    }
    val borderColor = when {
        isDestructive -> colors.accent.copy(alpha = 0.35f)
        isHighlight -> colors.accent
        else -> colors.line
    }

    Column(
        modifier = Modifier
            .width(68.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(containerBg)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable {
                haptics.tap()
                onClick()
            }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = LocalAppFont.current,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
