package com.ripple.filemanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import com.ripple.filemanager.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ripple.filemanager.AppState
import com.ripple.filemanager.AppAction
import com.ripple.filemanager.ui.getDynamicCornerShape

@Composable
fun MiniMusicPlayer(
    state: AppState,
    onAction: (AppAction) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.currentAudioFile == null) return

    val shadowColor = Color(0xFFE0AC70).copy(alpha = 0.2f)
    val borderColor = Color(0xFFE0AC70)
    val bgColor = Color(0xFF1D1610)
    
    Box(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .drawBehind {
                drawRect(
                    color = shadowColor,
                    topLeft = androidx.compose.ui.geometry.Offset(6.dp.toPx(), 6.dp.toPx()),
                    size = size
                )
            }
            .drawWithContent {
                drawContent()
                // Top border 3dp solid
                drawLine(
                    color = borderColor,
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    strokeWidth = 3.dp.toPx()
                )
            }
            .background(bgColor)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            // Play/Pause button
            Surface(
                onClick = { onAction(AppAction.ToggleAudioPlayback) },
                shape = androidx.compose.ui.graphics.RectangleShape,
                color = Color(0xFFE0AC70),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (state.isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isAudioPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                        tint = Color(0xFF161009),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Track info column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = state.audioTitle,
                    fontFamily = com.ripple.filemanager.ui.theme.FrauncesFontFamily,
                    fontWeight = FontWeight.W600,
                    fontSize = 16.sp,
                    color = Color(0xFFF2E9DC),
                    letterSpacing = 0.2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = state.audioArtist.uppercase(),
                    fontFamily = com.ripple.filemanager.ui.theme.JetBrainsMonoFamily,
                    fontSize = 11.sp,
                    color = Color(0xFFA89A86),
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Skip controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onAction(AppAction.PlayPreviousAudio) },
                    modifier = Modifier.padding(8.dp).size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = stringResource(R.string.previous_track),
                        tint = Color(0xFFA89A86),
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                IconButton(
                    onClick = { onAction(AppAction.PlayNextAudio) },
                    modifier = Modifier.padding(8.dp).size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = stringResource(R.string.next_track),
                        tint = Color(0xFFE0AC70),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
