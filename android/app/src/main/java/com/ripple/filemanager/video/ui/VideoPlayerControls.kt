package com.ripple.filemanager.video.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.res.Configuration
import com.ripple.filemanager.ui.expressive.CookieIconButton
import com.ripple.filemanager.ui.expressive.ExpressiveSlider
import com.ripple.filemanager.ui.expressive.ExpressiveTheme
import com.ripple.filemanager.ui.expressive.rememberIconShape
import com.ripple.filemanager.ui.theme.LocalAppFont
import com.ripple.filemanager.video.engine.PlaybackState
import com.ripple.filemanager.video.engine.VideoAspectRatio

fun formatVideoTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
    }
}

@Composable
fun VideoPlayerControlsOverlay(
    title: String,
    playbackState: PlaybackState,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    speed: Float,
    aspectRatio: VideoAspectRatio,
    isControlsVisible: Boolean,
    isControlsLocked: Boolean,
    hasNext: Boolean,
    hasPrevious: Boolean,
    onToggleControls: () -> Unit,
    onBackClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onLockToggle: () -> Unit,
    onAudioClick: () -> Unit,
    onSubtitleClick: () -> Unit,
    onPipClick: () -> Unit,
    onAspectClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onRotationLockToggle: () -> Unit,
    isRotationLocked: Boolean,
    modifier: Modifier = Modifier
) {
    var isSeeking by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableStateOf(0L) }

    val displayPosition = if (isSeeking) scrubPositionMs else currentPositionMs
    val progress = if (durationMs > 0L) {
        (displayPosition.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val context = LocalContext.current
    val density = LocalDensity.current

    // Measure system status bar height directly from platform resources
    val systemStatusBarHeight = remember(context, isPortrait) {
        val resId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resId > 0) {
            val px = context.resources.getDimensionPixelSize(resId)
            (px / context.resources.displayMetrics.density).dp
        } else {
            36.dp
        }
    }

    val insetsStatusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val insetsCutout = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val insetsSafeDrawing = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()

    // In portrait mode, ensure top padding is safely below the status bar and camera cutout
    val topBarPadding = if (isPortrait) {
        maxOf(systemStatusBarHeight, insetsStatusBar, insetsCutout, insetsSafeDrawing, 36.dp) + 8.dp
    } else {
        maxOf(insetsCutout, insetsStatusBar, insetsSafeDrawing, 8.dp)
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Locked mode indicator pill
        if (isControlsLocked) {
            AnimatedVisibility(
                visible = isControlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = topBarPadding, end = 20.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    contentColor = Color.White,
                    modifier = Modifier.clickable { onLockToggle() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Unlock controls",
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Controls Locked (tap to unlock)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            return@Box
        }

        // Animated Controls Overlay
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isPortrait) 160.dp else 120.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                            )
                        )
                )

                // Bottom Gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                )

                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = topBarPadding, start = 16.dp, end = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cookie Back Button
                    CookieIconButton(
                        onClick = onBackClick,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        bgColor = Color.White.copy(alpha = 0.2f),
                        iconTint = Color.White,
                        description = "Back",
                        size = 40.dp
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = LocalAppFont.current,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Top action icons:
                    // In landscape or large screens, show aspect ratio in the bar as well (also available in Settings).
                    val showAspectInBar = !isPortrait || configuration.screenWidthDp >= 480

                    if (showAspectInBar) {
                        IconButton(
                            onClick = onAspectClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Outlined.AspectRatio, contentDescription = "Aspect ratio", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }

                    IconButton(
                        onClick = onRotationLockToggle,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isRotationLocked) Icons.Default.ScreenLockRotation else Icons.Default.ScreenRotation,
                            contentDescription = "Rotation lock",
                            tint = if (isRotationLocked) ExpressiveTheme.colors.accent else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onPipClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Outlined.PictureInPictureAlt, contentDescription = "PiP", tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = onAudioClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Outlined.Audiotrack, contentDescription = "Audio tracks", tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = onSubtitleClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Outlined.Subtitles, contentDescription = "Subtitles", tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                // Center Controls (Previous - Play/Pause - Next) & Lock
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    if (hasPrevious) {
                        IconButton(
                            onClick = onPreviousClick,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous video",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Big Cookie Play/Pause Button
                    val cookieShape = rememberIconShape(lobes = 8)
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(cookieShape)
                            .background(ExpressiveTheme.colors.accent)
                            .clickable { onPlayPauseClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    if (hasNext) {
                        IconButton(
                            onClick = onNextClick,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next video",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Lock Controls Button (bottom left above progress bar)
                IconButton(
                    onClick = onLockToggle,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp, bottom = 80.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LockOpen,
                        contentDescription = "Lock controls",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Bottom Controls (Time, Seek Bar, Speed)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    // Time and Speed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${formatVideoTime(displayPosition)} / ${formatVideoTime(durationMs)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = LocalAppFont.current,
                            color = Color.White
                        )

                        // Speed Pill Button
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White,
                            modifier = Modifier.clickable { onSettingsClick() }
                        ) {
                            Text(
                                text = "${speed}x",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Seek Slider
                    ExpressiveSlider(
                        value = progress,
                        onValueChange = { frac ->
                            isSeeking = true
                            scrubPositionMs = (frac * durationMs).toLong()
                        },
                        onValueChangeFinished = {
                            isSeeking = false
                            onSeek(scrubPositionMs)
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Gestures HUD overlay for volume, brightness, seek, and 2x boost feedback.
 */
@Composable
fun VideoPlayerGestureHud(
    brightnessPercent: Float?, // 0f..1f
    volumePercent: Float?,     // 0f..1f
    seekDeltaMs: Long?,        // +/- delta
    isSpeedBoosting: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 2x Speed Boost Pill (top center)
        if (isSpeedBoosting) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.75f),
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = null,
                        tint = ExpressiveTheme.colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "2.0x Boost",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Brightness HUD
        if (brightnessPercent != null) {
            HudPill(
                icon = if (brightnessPercent > 0.5f) Icons.Default.BrightnessHigh else Icons.Default.BrightnessMedium,
                label = "Brightness",
                percent = brightnessPercent
            )
        }

        // Volume HUD
        if (volumePercent != null) {
            val icon = when {
                volumePercent <= 0.01f -> Icons.AutoMirrored.Filled.VolumeOff
                volumePercent < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
                else -> Icons.AutoMirrored.Filled.VolumeUp
            }
            HudPill(
                icon = icon,
                label = "Volume",
                percent = volumePercent
            )
        }

        // Seek Delta HUD
        if (seekDeltaMs != null) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.75f),
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (seekDeltaMs >= 0) Icons.Default.FastForward else Icons.Default.FastRewind,
                        contentDescription = null,
                        tint = ExpressiveTheme.colors.accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    val sign = if (seekDeltaMs >= 0) "+" else ""
                    Text(
                        text = "$sign${formatVideoTime(seekDeltaMs)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun HudPill(
    icon: ImageVector,
    label: String,
    percent: Float
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.75f),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { percent.coerceIn(0f, 1f) },
                modifier = Modifier
                    .width(100.dp)
                    .height(6.dp)
                    .clip(CircleShape),
                color = ExpressiveTheme.colors.accent,
                trackColor = Color.White.copy(alpha = 0.3f),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${(percent * 100).toInt()}%",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
