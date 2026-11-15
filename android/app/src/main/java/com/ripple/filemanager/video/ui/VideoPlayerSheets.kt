package com.ripple.filemanager.video.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripple.filemanager.ui.expressive.ExpressiveTheme
import com.ripple.filemanager.ui.theme.LocalAppFont
import com.ripple.filemanager.video.VideoPlayerPrefs
import com.ripple.filemanager.video.engine.*
import com.ripple.filemanager.video.subtitles.SidecarSubtitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioTracksSheet(
    tracks: List<VideoTrack>,
    selectedTrackId: Int,
    audioDelayMs: Long,
    onSelectTrack: (Int) -> Unit,
    onAddExternalAudio: () -> Unit,
    onSetAudioDelay: (Long) -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = ExpressiveTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = colors.container,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Audio",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LocalAppFont.current,
                color = colors.text,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Tracks list
            Text(
                text = "Audio Tracks",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.muted,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            if (tracks.isEmpty()) {
                Text(
                    text = "No audio tracks detected",
                    fontSize = 14.sp,
                    color = colors.muted,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                tracks.forEach { track ->
                    val isSelected = track.id == selectedTrackId || track.isSelected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) colors.accent.copy(alpha = 0.15f) else colors.insetCard)
                            .clickable { onSelectTrack(track.id) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = track.name,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = colors.text,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Add external audio
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.insetCard)
                    .clickable { onAddExternalAudio() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Add external audio track...",
                    fontSize = 15.sp,
                    color = colors.accent,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Audio Delay Card
            Text(
                text = "Audio Delay",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.muted,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.insetCard)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (audioDelayMs >= 0) "+$audioDelayMs ms" else "$audioDelayMs ms",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.text
                    )
                    Text(
                        text = "Synchronize audio with video",
                        fontSize = 12.sp,
                        color = colors.muted
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(
                        onClick = { onSetAudioDelay(audioDelayMs - 50L) },
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("-50", fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    FilledTonalButton(
                        onClick = { onSetAudioDelay(audioDelayMs + 50L) },
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("+50", fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = { onSetAudioDelay(0L) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset delay", tint = colors.muted)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleSheet(
    tracks: List<VideoTrack>,
    sidecars: List<SidecarSubtitle>,
    selectedTrackId: Int,
    subtitleDelayMs: Long,
    subtitleStyle: SubtitleStyleConfig,
    currentEncoding: String,
    onSelectTrack: (Int) -> Unit,
    onSelectSidecar: (SidecarSubtitle) -> Unit,
    onAddSubtitleFile: () -> Unit,
    onSetSubtitleDelay: (Long) -> Unit,
    onUpdateStyle: (SubtitleStyleConfig) -> Unit,
    onSetEncoding: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = ExpressiveTheme.colors
    var activeTab by remember { mutableStateOf(0) } // 0 = Tracks, 1 = Style, 2 = Encoding

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = colors.container,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Subtitles",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LocalAppFont.current,
                color = colors.text,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Tabs: Tracks | Style | Encoding
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.insetCard)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Tracks & Sync", "Appearance", "Encoding").forEachIndexed { idx, label ->
                    val isTabSelected = activeTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isTabSelected) colors.container else Color.Transparent)
                            .clickable { activeTab = idx }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (isTabSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isTabSelected) colors.accent else colors.muted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (activeTab) {
                0 -> {
                    // Tracks & Sync
                    Text(
                        text = "Tracks",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.muted,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    // Embedded & active tracks
                    tracks.forEach { track ->
                        val isSelected = track.id == selectedTrackId || track.isSelected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) colors.accent.copy(alpha = 0.15f) else colors.insetCard)
                                .clickable { onSelectTrack(track.id) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = track.name,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = colors.text,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = colors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Auto-detected sidecars (if any)
                    if (sidecars.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Detected in Folder",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.muted,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        sidecars.forEach { sidecar ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(colors.insetCard)
                                    .clickable { onSelectSidecar(sidecar) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Subtitles,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = sidecar.name,
                                    fontSize = 14.sp,
                                    color = colors.text,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Add external file
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.insetCard)
                            .clickable { onAddSubtitleFile() }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Add subtitle file (.srt, .vtt, .ass)...",
                            fontSize = 15.sp,
                            color = colors.accent,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Subtitle Delay
                    Text(
                        text = "Subtitle Delay",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.muted,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.insetCard)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (subtitleDelayMs >= 0) "+$subtitleDelayMs ms" else "$subtitleDelayMs ms",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.text
                            )
                            Text(
                                text = "Adjust text appearance timing",
                                fontSize = 12.sp,
                                color = colors.muted
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalButton(
                                onClick = { onSetSubtitleDelay(subtitleDelayMs - 50L) },
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                Text("-50", fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            FilledTonalButton(
                                onClick = { onSetSubtitleDelay(subtitleDelayMs + 50L) },
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                Text("+50", fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(onClick = { onSetSubtitleDelay(0L) }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reset delay", tint = colors.muted)
                            }
                        }
                    }
                }

                1 -> {
                    // Subtitle Styling
                    Text(
                        text = "Font Size",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.muted,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.insetCard)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SubtitleSize.values().forEach { size ->
                            val isSel = subtitleStyle.size == size
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) colors.container else Color.Transparent)
                                    .clickable { onUpdateStyle(subtitleStyle.copy(size = size)) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = size.label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) colors.accent else colors.text
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Text Colour",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.muted,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.insetCard)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SubtitleColor.values().forEach { col ->
                            val isSel = subtitleStyle.color == col
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) colors.container else Color.Transparent)
                                    .clickable { onUpdateStyle(subtitleStyle.copy(color = col)) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = col.label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) colors.accent else colors.text
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Background",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.muted,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.insetCard)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SubtitleBackground.values().forEach { bg ->
                            val isSel = subtitleStyle.background == bg
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) colors.container else Color.Transparent)
                                    .clickable { onUpdateStyle(subtitleStyle.copy(background = bg)) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = bg.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) colors.accent else colors.text
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Outline",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.muted,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.insetCard)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SubtitleOutline.values().forEach { out ->
                            val isSel = subtitleStyle.outline == out
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) colors.container else Color.Transparent)
                                    .clickable { onUpdateStyle(subtitleStyle.copy(outline = out)) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = out.label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) colors.accent else colors.text
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.insetCard)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Bold Subtitles",
                            fontSize = 15.sp,
                            color = colors.text,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = subtitleStyle.bold,
                            onCheckedChange = { onUpdateStyle(subtitleStyle.copy(bold = it)) }
                        )
                    }
                }

                2 -> {
                    // Encoding
                    Text(
                        text = "Text Encoding",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.muted,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    SUPPORTED_SUBTITLE_ENCODINGS.forEach { enc ->
                        val isSelected = currentEncoding == enc.charsetName
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) colors.accent.copy(alpha = 0.15f) else colors.insetCard)
                                .clickable { onSetEncoding(enc.charsetName) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = enc.displayName,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = colors.text
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = colors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoSettingsSheet(
    currentSpeed: Float,
    currentAspect: VideoAspectRatio,
    currentHw: HwAccelerationMode,
    prefs: VideoPlayerPrefs,
    onSetSpeed: (Float) -> Unit,
    onSetAspect: (VideoAspectRatio) -> Unit,
    onSetHw: (HwAccelerationMode) -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = ExpressiveTheme.colors
    var backgroundPlay by remember { mutableStateOf(prefs.backgroundPlay) }
    var pipEnabled by remember { mutableStateOf(prefs.pictureInPicture) }
    var resumePlayback by remember { mutableStateOf(prefs.resumePlayback) }

    // Gestures
    var doubleTapSeek by remember { mutableStateOf(prefs.gestureDoubleTapSeek) }
    var swipeSeek by remember { mutableStateOf(prefs.gestureSwipeSeek) }
    var swipeBrightnessVolume by remember { mutableStateOf(prefs.gestureSwipeBrightnessVolume) }
    var pinchZoom by remember { mutableStateOf(prefs.gesturePinchZoom) }
    var longPressSpeed by remember { mutableStateOf(prefs.gestureLongPressSpeed) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = colors.container,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Player Settings",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LocalAppFont.current,
                color = colors.text,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Playback Speed
            Text(
                text = "Playback Speed",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.muted,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 3.0f, 4.0f).forEach { spd ->
                    val isSel = kotlin.math.abs(currentSpeed - spd) < 0.05f
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSel) colors.accent else colors.insetCard)
                            .clickable { onSetSpeed(spd) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${spd}x",
                            fontSize = 13.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) Color.White else colors.text
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Aspect Ratio
            Text(
                text = "Aspect Ratio",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.muted,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.insetCard)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                VideoAspectRatio.values().forEach { aspect ->
                    val isSel = currentAspect == aspect
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) colors.container else Color.Transparent)
                            .clickable { onSetAspect(aspect) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = aspect.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) colors.accent else colors.text
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Hardware Decoding
            Text(
                text = "Hardware Acceleration",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.muted,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.insetCard)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HwAccelerationMode.values().forEach { hw ->
                    val isSel = currentHw == hw
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) colors.container else Color.Transparent)
                            .clickable { onSetHw(hw) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = hw.label,
                            fontSize = 13.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) colors.accent else colors.text
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Playback Options
            Text(
                text = "Playback Options",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.muted,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            SettingsSwitchRow(
                title = "Resume playback",
                subtitle = "Remember position where you left off",
                checked = resumePlayback,
                onCheckedChange = {
                    resumePlayback = it
                    prefs.resumePlayback = it
                }
            )

            SettingsSwitchRow(
                title = "Picture-in-Picture",
                subtitle = "Keep playing in mini overlay when leaving app",
                checked = pipEnabled,
                onCheckedChange = {
                    pipEnabled = it
                    prefs.pictureInPicture = it
                }
            )

            SettingsSwitchRow(
                title = "Background audio",
                subtitle = "Continue audio playback when screen turns off",
                checked = backgroundPlay,
                onCheckedChange = {
                    backgroundPlay = it
                    prefs.backgroundPlay = it
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Gestures
            Text(
                text = "Gestures",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.muted,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            SettingsSwitchRow(
                title = "Double tap to seek",
                subtitle = "Tap sides to jump 10 seconds",
                checked = doubleTapSeek,
                onCheckedChange = {
                    doubleTapSeek = it
                    prefs.gestureDoubleTapSeek = it
                }
            )

            SettingsSwitchRow(
                title = "Swipe to seek",
                subtitle = "Horizontal drag on screen to scrub",
                checked = swipeSeek,
                onCheckedChange = {
                    swipeSeek = it
                    prefs.gestureSwipeSeek = it
                }
            )

            SettingsSwitchRow(
                title = "Swipe for brightness & volume",
                subtitle = "Left side for brightness, right side for volume",
                checked = swipeBrightnessVolume,
                onCheckedChange = {
                    swipeBrightnessVolume = it
                    prefs.gestureSwipeBrightnessVolume = it
                }
            )

            SettingsSwitchRow(
                title = "Pinch to zoom",
                subtitle = "Change aspect ratio or zoom level",
                checked = pinchZoom,
                onCheckedChange = {
                    pinchZoom = it
                    prefs.gesturePinchZoom = it
                }
            )

            SettingsSwitchRow(
                title = "Long press for 2x speed",
                subtitle = "Hold screen to temporarily boost speed",
                checked = longPressSpeed,
                onCheckedChange = {
                    longPressSpeed = it
                    prefs.gestureLongPressSpeed = it
                }
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = ExpressiveTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.insetCard)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.text
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = colors.muted
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
