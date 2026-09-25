package com.ripple.filemanager.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import com.ripple.filemanager.ui.theme.JetBrainsMonoFamily
import com.ripple.filemanager.ui.theme.ManropeFontFamily
import com.ripple.filemanager.AppAction
import com.ripple.filemanager.AppState
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.ripple.filemanager.R
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.TextButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import com.ripple.filemanager.ui.expressive.CookieIcon
import com.ripple.filemanager.ui.expressive.CookieIconButton
import com.ripple.filemanager.ui.expressive.ExpressiveTheme
import com.ripple.filemanager.ui.expressive.ExpressiveTokens
import com.ripple.filemanager.ui.theme.LocalAppFont

data class BottomNavItem(val id: String, val activeIcon: ImageVector, val inactiveIcon: ImageVector, val label: String)

@Composable
fun BottomNavBar(
    items: List<BottomNavItem>,
    selectedId: String,
    tapCounters: ImmutableMap<String, Int>,
    cornerRoundness: Float,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val navShape = com.ripple.filemanager.ui.getDynamicCornerShape(32f, cornerRoundness)
    
    Row(
        modifier = modifier
            .height(54.dp)
            .border(1.dp, SkylineColors.Amber.copy(alpha = 0.5f), navShape)
            .clip(navShape)
            .background(Color.Transparent)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, item ->
            NavTab(
                item = item,
                isActive = item.id == selectedId,
                tapCount = tapCounters[item.id] ?: 0,
                cornerRoundness = cornerRoundness,
                onSelect = { onSelect(item.id) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            
            // Vertical Divider
            if (index < items.size - 1) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(32.dp)
                        .background(SkylineColors.Border)
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
    cornerRoundness: Float,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    val iconTint by animateColorAsState(
        targetValue = if (isActive) SkylineColors.Background else SkylineColors.Amber.copy(alpha = 0.6f),
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "navTint_${item.id}"
    )
    
    val bgTint by animateColorAsState(
        targetValue = if (isActive) SkylineColors.Amber else Color.Transparent,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "bgTint_${item.id}"
    )

    Box(
        modifier = modifier
            .clip(com.ripple.filemanager.ui.getDynamicCornerShape(24f, cornerRoundness))
            .background(bgTint)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { haptics.tap(); onSelect() }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Icon
        Icon(
            imageVector = if (isActive) item.activeIcon else item.inactiveIcon,
            contentDescription = item.label,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun UnifiedBottomPill(
    state: AppState,
    archiveProgress: com.ripple.filemanager.archive.ArchiveProgress?,
    onCancelExtract: () -> Unit,
    onAction: (AppAction) -> Unit,
    capturedTargetFolderName: String?,
    onCaptureTargetFolder: (String?) -> Unit,
    selectionModeContent: @Composable () -> Unit,
    rippleNavContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomState = when {
        archiveProgress is com.ripple.filemanager.archive.ArchiveProgress.Running || archiveProgress is com.ripple.filemanager.archive.ArchiveProgress.Complete -> 4
        state.pasteProgress != null -> 0
        state.isSelectionMode -> 1
        state.organisePendingFiles.isNotEmpty() -> 5
        state.clipboardPaths.isNotEmpty() -> 2
        else -> 3
    }
    
    val reduced = com.ripple.filemanager.ui.expressive.ExpressiveMotion.isReducedMotion()
    
    AnimatedContent(
        targetState = bottomState,
        transitionSpec = {
            if (reduced) {
                fadeIn(androidx.compose.animation.core.snap()) togetherWith fadeOut(androidx.compose.animation.core.snap())
            } else {
                (fadeIn(com.ripple.filemanager.ui.expressive.ExpressiveMotion.FastTween) +
                 scaleIn(initialScale = 0.95f, animationSpec = com.ripple.filemanager.ui.expressive.ExpressiveMotion.SpringSpec))
                    .togetherWith(
                        fadeOut(com.ripple.filemanager.ui.expressive.ExpressiveMotion.FadeTween) +
                        scaleOut(targetScale = 0.95f, animationSpec = com.ripple.filemanager.ui.expressive.ExpressiveMotion.FastTween)
                    )
            }
        },
        modifier = modifier.animateContentSize(
            animationSpec = if (reduced) androidx.compose.animation.core.snap() else androidx.compose.animation.core.tween(200, easing = androidx.compose.animation.core.FastOutSlowInEasing)
        ),
        label = "unified_pill"
    ) { target ->
        when (target) {
            4 -> if (archiveProgress != null) ExtractingStatePill(state, archiveProgress, onCancelExtract, Modifier.fillMaxWidth())
            0 -> PastingStatePill(state, onAction, capturedTargetFolderName, Modifier.fillMaxWidth())
            1 -> selectionModeContent()
            5 -> OrganisePendingPill(state, onAction, Modifier.fillMaxWidth())
            2 -> ClipboardArmedPill(state, onAction, onCaptureTargetFolder, Modifier.fillMaxWidth())
            3 -> rippleNavContent()
        }
    }
}

@Composable
fun OrganisePendingPill(
    state: AppState,
    onAction: (AppAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var filesExpanded by remember { mutableStateOf(false) }
    val haptics = com.ripple.filemanager.haptics.rememberHapticsController { state.haptics }
    
    val fileCount = state.organisePendingFiles.size
    val fileNames = state.organisePendingFiles.map { it.name }
    
    val cornerRadius by animateDpAsState(
        targetValue = if (filesExpanded) 20.dp else 999.dp,
        animationSpec = tween(180),
        label = "pill_corner"
    )
    val chevronRotation by animateFloatAsState(
        targetValue = if (filesExpanded) 180f else 0f,
        animationSpec = tween(180),
        label = "chevron_rot"
    )
    
    Column(
        modifier = modifier
            .border(1.dp, SkylineColors.Border, RoundedCornerShape(cornerRadius))
            .clip(RoundedCornerShape(cornerRadius))
            .background(SkylineColors.Surface)
            .animateContentSize()
    ) {
        if (filesExpanded) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 190.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(fileNames) { name ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SkylineColors.Border, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = SkylineColors.TextDim, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = name,
                            fontFamily = ManropeFontFamily,
                            fontSize = 12.5.sp,
                            color = SkylineColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoFixHigh,
                contentDescription = null,
                tint = SkylineColors.Amber,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f).clickable { haptics.tap(); filesExpanded = !filesExpanded }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = pluralStringResource(R.plurals.items_ready, fileCount, fileCount).uppercase(),
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp,
                        color = SkylineColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = SkylineColors.TextDim,
                        modifier = Modifier.size(13.dp).rotate(chevronRotation)
                    )
                }
                Text(
                    text = "WILL ORGANISE",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 9.sp,
                    color = SkylineColors.TextDim2
                )
            }
            TextButton(
                onClick = { haptics.tap(); onAction(AppAction.CancelOrganiseDownloads) },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = stringResource(R.string.cancel).uppercase(),
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 10.sp,
                    color = SkylineColors.TextDim
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(SkylineColors.Amber)
                    .clickable { 
                        haptics.tap()
                        onAction(AppAction.ConfirmOrganiseDownloads) 
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = SkylineColors.Background, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "PROCEED",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SkylineColors.Background
                )
            }
        }
    }
}

@Composable
fun ClipboardArmedPill(
    state: AppState,
    onAction: (AppAction) -> Unit,
    onCaptureTargetFolder: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val haptics = com.ripple.filemanager.haptics.rememberHapticsController { state.haptics }
    val colors = ExpressiveTheme.colors
    
    val fileCount = state.clipboardPaths.size
    val fileNames = state.clipboardPaths.map { it.substringAfterLast("/") }
    val isCut = state.clipboardAction == "cut"
    
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "clipboard_chevron_rot"
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

        // Header Row: CookieIcon + title/subtitle + quick paste + chevron toggle + cancel button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val pastel = if (isCut) ExpressiveTokens.categoryPastel("video") else ExpressiveTokens.categoryPastel("docs")
            CookieIcon(
                icon = if (isCut) Icons.Outlined.ContentCut else Icons.Outlined.ContentCopy,
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
                        indication = null,
                        onClick = {
                            haptics.tap()
                            isExpanded = !isExpanded
                        }
                    )
            ) {
                Text(
                    text = if (fileCount == 1) {
                        "1 item to ${if (isCut) "move" else "copy"}"
                    } else {
                        "$fileCount items to ${if (isCut) "move" else "copy"}"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = LocalAppFont.current,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isExpanded) {
                        "Ready in ${state.currentFolderName ?: "current folder"}"
                    } else {
                        "Tap to expand • ${state.currentFolderName ?: "current folder"}"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = LocalAppFont.current,
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Quick paste button (available right in header for immediate 1-tap pasting from collapsed state)
            CookieIconButton(
                onClick = {
                    haptics.copyPaste()
                    onCaptureTargetFolder(state.currentFolderName ?: "Current Folder")
                    onAction(AppAction.PasteClipboard(state.location))
                },
                icon = Icons.Default.ContentPaste,
                bgColor = colors.accent,
                iconTint = colors.ink,
                description = if (isCut) "Move here" else "Paste here",
                size = 36.dp,
                iconSize = 18.dp,
                lobes = 8
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Expand / Collapse Chevron button (points up when collapsed, rotates down when expanded)
            CookieIconButton(
                onClick = {
                    haptics.tap()
                    isExpanded = !isExpanded
                },
                icon = Icons.Default.KeyboardArrowUp,
                bgColor = colors.card,
                iconTint = colors.text,
                description = if (isExpanded) "Collapse" else "Expand details",
                size = 36.dp,
                iconSize = 20.dp,
                modifier = Modifier.rotate(chevronRotation),
                lobes = 8
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Cancel / Dismiss clipboard button
            CookieIconButton(
                onClick = {
                    haptics.tap()
                    onAction(AppAction.ClearClipboard)
                },
                icon = Icons.Default.Close,
                bgColor = colors.card,
                iconTint = colors.text,
                description = "Cancel clipboard",
                size = 36.dp,
                iconSize = 18.dp,
                lobes = 8
            )
        }

        // Expanded content: File preview list + Dual action buttons
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(180)),
            exit = shrinkVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(150))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Expandable list of files
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 140.dp)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(fileNames) { name ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.card)
                                .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.InsertDriveFile,
                                contentDescription = null,
                                tint = colors.muted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = name,
                                fontFamily = LocalAppFont.current,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                color = colors.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Dual side-by-side pill buttons matching login / delete sheets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel button (secondary style)
                    Button(
                        onClick = {
                            haptics.copyPaste()
                            onAction(AppAction.ClearClipboard)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.card,
                            contentColor = colors.text
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = LocalAppFont.current
                        )
                    }

                    // Paste button (primary style)
                    Button(
                        onClick = {
                            haptics.copyPaste()
                            onCaptureTargetFolder(state.currentFolderName ?: "Current Folder")
                            onAction(AppAction.PasteClipboard(state.location))
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.ink
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            tint = colors.ink,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCut) "Move Here" else "Paste Here",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = LocalAppFont.current
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PastingStatePill(
    state: AppState,
    onAction: (AppAction) -> Unit,
    capturedTargetFolderName: String?,
    modifier: Modifier = Modifier
) {
    val haptics = com.ripple.filemanager.haptics.rememberHapticsController { state.haptics }
    val fileCount = state.clipboardPaths.size
    val fileNames = state.clipboardPaths.map { it.substringAfterLast("/") }
    val progress = state.pasteProgress ?: 0f
    val currentFileIndex = (progress * fileCount).toInt().coerceIn(0, (fileCount - 1).coerceAtLeast(0))
    val currentFileName = fileNames.getOrNull(currentFileIndex) ?: ""
    val targetName = capturedTargetFolderName ?: state.currentFolderName ?: "Folder"
    
    Column(
        modifier = modifier
            .border(1.dp, SkylineColors.Border, RoundedCornerShape(999.dp))
            .clip(RoundedCornerShape(999.dp))
            .background(SkylineColors.Surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (state.clipboardAction == "cut") Icons.Outlined.ContentCut else Icons.Outlined.ContentCopy,
                contentDescription = null,
                tint = SkylineColors.Amber,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                val statusText = if (state.isPastePaused) stringResource(R.string.paused) else if (state.clipboardAction == "cut") stringResource(R.string.moving) else stringResource(R.string.copying)
                Text(
                    text = stringResource(R.string.status_and_filename, statusText, currentFileName),
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 10.5.sp,
                    color = SkylineColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.into_target, targetName),
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 9.sp,
                    color = SkylineColors.TextDim2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.percentage, (progress * 100).roundToInt()),
                fontFamily = JetBrainsMonoFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = SkylineColors.Amber
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .border(1.dp, SkylineColors.Border, RoundedCornerShape(999.dp))
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { haptics.copyPaste(); onAction(AppAction.TogglePastePause) },
                contentAlignment = Alignment.Center
            ) {
                val iconTint = if (state.isPastePaused) SkylineColors.Dust else SkylineColors.TextDim
                Icon(
                    imageVector = if (state.isPastePaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(12.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(R.string.cancel),
                tint = SkylineColors.TextDim2,
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { haptics.copyPaste(); onAction(AppAction.CancelPaste) }
                    .padding(8.dp)
            )
        }
        
        val animatedProgress by animateFloatAsState(
            targetValue = progress,
            animationSpec = tween(120, easing = LinearEasing),
            label = "paste_prog"
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 10.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(SkylineColors.Surface2)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (state.isPastePaused) SkylineColors.TextDim2 else SkylineColors.Amber)
            )
        }
    }
}

@Composable
fun ExtractingStatePill(
    state: AppState,
    progressState: com.ripple.filemanager.archive.ArchiveProgress,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = com.ripple.filemanager.haptics.rememberHapticsController { state.haptics }
    val isComplete = progressState is com.ripple.filemanager.archive.ArchiveProgress.Complete
    val runningState = progressState as? com.ripple.filemanager.archive.ArchiveProgress.Running
    val progress = if (isComplete) 1f else if (runningState != null && runningState.filesTotal > 0) runningState.filesDone.toFloat() / runningState.filesTotal else 0f
    
    val statusText = if (isComplete) "COMPLETED" else "UNZIPPING..."
    val entryName = if (isComplete) "All files extracted" else runningState?.currentEntryName ?: ""
    
    Column(
        modifier = modifier
            .border(1.dp, SkylineColors.Border, RoundedCornerShape(999.dp))
            .clip(RoundedCornerShape(999.dp))
            .background(SkylineColors.Surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = null,
                tint = SkylineColors.Amber,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = statusText,
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 10.5.sp,
                    color = SkylineColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = entryName,
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 9.sp,
                    color = SkylineColors.TextDim2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${(progress * 100).roundToInt()}%",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = SkylineColors.Amber
            )
            Spacer(modifier = Modifier.width(12.dp))
            if (!isComplete) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.cancel),
                    tint = SkylineColors.TextDim2,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { haptics.copyPaste(); onCancel() }
                        .padding(6.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(18.dp))
            }
        }
        
        val animatedProgress by animateFloatAsState(
            targetValue = progress,
            animationSpec = tween(120, easing = LinearEasing),
            label = "extract_prog"
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 10.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(SkylineColors.Surface2)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(999.dp))
                    .background(SkylineColors.Amber)
            )
        }
    }
}
