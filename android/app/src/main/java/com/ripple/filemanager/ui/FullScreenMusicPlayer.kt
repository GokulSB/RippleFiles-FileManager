package com.ripple.filemanager.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ripple.filemanager.AppAction
import com.ripple.filemanager.AppState
import com.ripple.filemanager.R
import com.ripple.filemanager.haptics.LocalHaptics
import com.ripple.filemanager.ui.expressive.*
import com.ripple.filemanager.ui.theme.LocalAppFont
import java.util.Locale

@Composable
fun FullScreenMusicPlayer(
    state: AppState,
    onAction: (AppAction) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.currentAudioFile == null) return
    val audioPosition by LocalAudioPosition.current.collectAsState()

    val colors = ExpressiveTheme.colors
    val haptics = LocalHaptics.current
    val cornerRoundness = LocalCornerRoundness.current

    // Back gesture or hardware back collapses back to mini player
    BackHandler {
        onAction(AppAction.SetShowFullScreenPlayer(false))
    }

    val artworkBitmap = remember(state.audioArtworkData) {
        state.audioArtworkData?.let { bytes ->
            try {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) {
                null
            }
        }
    }

    // Touch absorption modifier: consumes all pointer and click events on background/empty areas
    // so nothing can ever pass through to underlying file items or screens.
    val blockPassThrough = Modifier
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    event.changes.forEach { change ->
                        if (!change.isConsumed) {
                            change.consume()
                        }
                    }
                }
            }
        }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            // Absorb clicks on empty space
        }

    RippleBackground(
        modifier = modifier
            .fillMaxSize()
            .then(blockPassThrough)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(blockPassThrough)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row: Collapse chevron (cookie button) + Now Playing title + Balanced spacer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CookieIconButton(
                    onClick = {
                        haptics.tap()
                        onAction(AppAction.SetShowFullScreenPlayer(false))
                    },
                    icon = Icons.Default.KeyboardArrowDown,
                    bgColor = colors.card,
                    iconTint = colors.text,
                    description = stringResource(R.string.close_player),
                    size = 42.dp,
                    iconSize = 22.dp,
                    lobes = 8
                )

                Text(
                    text = stringResource(R.string.now_playing),
                    fontFamily = LocalAppFont.current,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = colors.text,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.size(42.dp))
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Large Album Art: Cookie-rounded-square frame matching large surfaces
            val artShape = getDynamicCornerShape(32f, cornerRoundness)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .aspectRatio(1f)
                    .shadow(
                        elevation = 14.dp,
                        shape = artShape,
                        ambientColor = Color.Transparent,
                        spotColor = colors.shadow.copy(alpha = 0.35f)
                    )
                    .clip(artShape)
                    .background(colors.card)
                    .border(
                        width = 1.dp,
                        color = colors.line.copy(alpha = colors.lineAlpha),
                        shape = artShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // Absorb tap on album art
                    },
                contentAlignment = Alignment.Center
            ) {
                if (artworkBitmap != null) {
                    AsyncImage(
                        model = artworkBitmap,
                        contentDescription = stringResource(R.string.album_art),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.MusicNote,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(80.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.4f))

            // Track Title & Artist Metadata
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = state.audioTitle.ifEmpty { stringResource(R.string.now_playing) },
                    fontFamily = LocalAppFont.current,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = state.audioArtist.ifEmpty { "Unknown Artist" },
                    fontFamily = LocalAppFont.current,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Scrubber: Continuous track using ExpressiveSlider with tabular numerals
            var isDragging by remember { mutableStateOf(false) }
            var dragProgress by remember { mutableFloatStateOf(0f) }

            val currentPosition = if (isDragging) dragProgress.toLong() else audioPosition
            val totalDuration = state.audioDuration.coerceAtLeast(0L)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                ExpressiveSlider(
                    value = if (isDragging) dragProgress else audioPosition.toFloat().coerceIn(0f, (if (totalDuration > 0) totalDuration.toFloat() else 100f)),
                    onValueChange = {
                        isDragging = true
                        dragProgress = it
                    },
                    onValueChangeFinished = {
                        isDragging = false
                        onAction(AppAction.SeekAudio(dragProgress.toLong()))
                    },
                    valueRange = 0f..(if (totalDuration > 0) totalDuration.toFloat() else 100f),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(currentPosition),
                        fontFamily = LocalAppFont.current,
                        fontSize = 12.5.sp,
                        color = colors.muted,
                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                    )
                    Text(
                        text = formatTime(totalDuration),
                        fontFamily = LocalAppFont.current,
                        fontSize = 12.5.sp,
                        color = colors.muted,
                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.6f))

            // Transport Controls: Skip Previous / Play-Pause / Skip Next
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CookieIconButton(
                    onClick = {
                        haptics.tap()
                        onAction(AppAction.PlayPreviousAudio)
                    },
                    icon = Icons.Default.SkipPrevious,
                    bgColor = colors.card,
                    iconTint = colors.text,
                    description = stringResource(R.string.previous_track),
                    size = 58.dp,
                    iconSize = 28.dp,
                    lobes = 8
                )

                CookieIconButton(
                    onClick = {
                        haptics.tap()
                        onAction(AppAction.ToggleAudioPlayback)
                    },
                    icon = if (state.isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    bgColor = colors.accent,
                    iconTint = colors.ink,
                    description = if (state.isAudioPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                    size = 78.dp,
                    iconSize = 38.dp,
                    lobes = 10
                )

                CookieIconButton(
                    onClick = {
                        haptics.tap()
                        onAction(AppAction.PlayNextAudio)
                    },
                    icon = Icons.Default.SkipNext,
                    bgColor = colors.card,
                    iconTint = colors.text,
                    description = stringResource(R.string.next_track),
                    size = 58.dp,
                    iconSize = 28.dp,
                    lobes = 8
                )
            }

            Spacer(modifier = Modifier.weight(0.5f))
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
