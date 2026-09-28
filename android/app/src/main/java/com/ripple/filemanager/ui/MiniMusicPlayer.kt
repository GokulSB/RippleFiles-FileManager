package com.ripple.filemanager.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ripple.filemanager.AppAction
import com.ripple.filemanager.AppState
import com.ripple.filemanager.R
import com.ripple.filemanager.haptics.LocalHaptics
import com.ripple.filemanager.ui.expressive.CookieIcon
import com.ripple.filemanager.ui.expressive.ExpressiveTheme
import com.ripple.filemanager.ui.expressive.ExpressiveTokens
import com.ripple.filemanager.ui.expressive.pressScale
import com.ripple.filemanager.ui.theme.LocalAppFont
import java.util.Locale

/**
 * Revamped Mini Music Player:
 * - Formatted as a collapsed action sheet matching the rest of the expressive app style
 * - Centered grab handle indicating action sheet affordance
 * - Integrated slim playback progress line tracking track progress
 * - Album artwork / Pastel CookieIcon badge
 * - Track title and artist with LocalAppFont
 * - Tactile playback controls: Previous, Play/Pause with accent pill, Next, Expand chevron, Close/Stop
 * - Expandable action sheet panel with interactive scrubber, -10s / +10s scrub chips, and Full Player shortcut
 */
/** Playback position, provided from MainActivity so only the players redraw on each tick. */
val LocalAudioPosition = staticCompositionLocalOf<kotlinx.coroutines.flow.StateFlow<Long>> {
    kotlinx.coroutines.flow.MutableStateFlow(0L)
}

@Composable
fun MiniMusicPlayer(
    state: AppState,
    onAction: (AppAction) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.currentAudioFile == null) return
    val audioPosition by LocalAudioPosition.current.collectAsState()

    val colors = ExpressiveTheme.colors
    val haptics = LocalHaptics.current

    var isExpanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "mini_player_chevron"
    )

    val progressFraction = if (state.audioDuration > 0) {
        (audioPosition.toFloat() / state.audioDuration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(150),
        label = "mini_player_progress"
    )

    val artworkBitmap = remember(state.audioArtworkData) {
        state.audioArtworkData?.let { bytes ->
            try {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) {
                null
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = Color.Transparent,
                spotColor = colors.shadow.copy(alpha = 0.28f)
            )
            .clip(RoundedCornerShape(26.dp))
            .background(colors.container)
            .border(
                width = 1.dp,
                color = colors.line.copy(alpha = colors.lineAlpha),
                shape = RoundedCornerShape(26.dp)
            )
            .animateContentSize(animationSpec = tween(220, easing = FastOutSlowInEasing))
    ) {
        // Centered Action Sheet Grab Handle
        Box(
            modifier = Modifier
                .padding(top = 8.dp, bottom = 4.dp)
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.muted.copy(alpha = 0.35f))
                .align(Alignment.CenterHorizontally)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    haptics.tap()
                    isExpanded = !isExpanded
                }
        )

        // Collapsed Main Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 10.dp, top = 2.dp, bottom = if (isExpanded) 4.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art or Pastel CookieIcon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable {
                        haptics.tap()
                        onAction(AppAction.SetShowFullScreenPlayer(true))
                    },
                contentAlignment = Alignment.Center
            ) {
                if (artworkBitmap != null) {
                    AsyncImage(
                        model = artworkBitmap,
                        contentDescription = stringResource(R.string.album_art),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, colors.line.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    )
                } else {
                    CookieIcon(
                        icon = Icons.Outlined.MusicNote,
                        bgColor = ExpressiveTokens.categoryPastel("audio"),
                        iconTint = ExpressiveTokens.OnPastel,
                        size = 44.dp,
                        lobes = 8
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Track title & artist
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        haptics.tap()
                        onAction(AppAction.SetShowFullScreenPlayer(true))
                    }
            ) {
                Text(
                    text = state.audioTitle.ifEmpty { stringResource(R.string.now_playing) },
                    fontFamily = LocalAppFont.current,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val artistText = state.audioArtist.ifEmpty { "Unknown Artist" }
                val timeText = if (state.audioDuration > 0) {
                    " · ${formatTime(audioPosition)} / ${formatTime(state.audioDuration)}"
                } else ""
                Text(
                    text = "$artistText$timeText",
                    fontFamily = LocalAppFont.current,
                    fontSize = 11.5.sp,
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Playback controls: Prev, Play/Pause, Next, Chevron, Stop
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = {
                        haptics.tap()
                        onAction(AppAction.PlayPreviousAudio)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = stringResource(R.string.previous_track),
                        tint = colors.muted,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Accent Play/Pause Pill Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .pressScale(pressedScale = 0.92f)
                        .shadow(
                            elevation = 4.dp,
                            shape = CircleShape,
                            ambientColor = Color.Transparent,
                            spotColor = colors.shadow.copy(alpha = 0.25f)
                        )
                        .clip(CircleShape)
                        .background(colors.accent)
                        .clickable {
                            haptics.tap()
                            onAction(AppAction.ToggleAudioPlayback)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isAudioPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                        tint = colors.ink,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = {
                        haptics.tap()
                        onAction(AppAction.PlayNextAudio)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = stringResource(R.string.next_track),
                        tint = colors.muted,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = {
                        haptics.tap()
                        isExpanded = !isExpanded
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = colors.muted,
                        modifier = Modifier
                            .size(19.dp)
                            .rotate(chevronRotation)
                    )
                }

                IconButton(
                    onClick = {
                        haptics.tap()
                        onAction(AppAction.StopAudio)
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = colors.muted.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Integrated Slim Progress Line (visible in collapsed state)
        if (!isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(colors.line.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .background(colors.accent)
                )
            }
        }

        // Expanded Action Sheet Content
        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp)
            ) {
                HorizontalDivider(
                    color = colors.line.copy(alpha = colors.lineAlpha),
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // Interactive Scrubber Slider
                var isSeeking by remember { mutableStateOf(false) }
                var seekPos by remember { mutableStateOf(0f) }

                Slider(
                    value = if (isSeeking) seekPos else audioPosition.toFloat(),
                    onValueChange = {
                        isSeeking = true
                        seekPos = it
                    },
                    onValueChangeFinished = {
                        isSeeking = false
                        onAction(AppAction.SeekAudio(seekPos.toLong()))
                    },
                    valueRange = 0f..(state.audioDuration.takeIf { it > 0 }?.toFloat() ?: 100f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = colors.accent,
                        activeTrackColor = colors.accent,
                        inactiveTrackColor = colors.card
                    )
                )

                // Timestamps Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(if (isSeeking) seekPos.toLong() else audioPosition),
                        fontFamily = LocalAppFont.current,
                        fontSize = 11.5.sp,
                        color = colors.muted
                    )
                    Text(
                        text = formatTime(state.audioDuration),
                        fontFamily = LocalAppFont.current,
                        fontSize = 11.5.sp,
                        color = colors.muted
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Action Chips Row (-10s, +10s, Full Player)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // -10s button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.card)
                            .border(1.dp, colors.line.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable {
                                haptics.tap()
                                val target = (audioPosition - 10000L).coerceAtLeast(0L)
                                onAction(AppAction.SeekAudio(target))
                            }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = null,
                            tint = colors.text,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "-10s",
                            fontFamily = LocalAppFont.current,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.text
                        )
                    }

                    // +10s button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.card)
                            .border(1.dp, colors.line.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable {
                                haptics.tap()
                                val target = (audioPosition + 10000L).coerceAtMost(state.audioDuration)
                                onAction(AppAction.SeekAudio(target))
                            }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward,
                            contentDescription = null,
                            tint = colors.text,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+10s",
                            fontFamily = LocalAppFont.current,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.text
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Full Player Pill Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.accent.copy(alpha = 0.18f))
                            .border(1.dp, colors.accent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable {
                                haptics.tap()
                                onAction(AppAction.SetShowFullScreenPlayer(true))
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.OpenInFull,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Full Player",
                            fontFamily = LocalAppFont.current,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.accent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
