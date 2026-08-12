package com.ripple.filemanager.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import coil.request.videoFrameMillis
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripple.filemanager.FileItem
import com.ripple.filemanager.ui.theme.LocalSkylineLedgerColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class MediaFileUiModel(
    val id: Int,
    val path: String,
    val title: String,
    val episodeTrackName: String?,
    val releaseTag: String?,
    val size: String,
    val date: String,
    val duration: String?,
    val extension: String,
    val mediaType: String,
    val thumbnailLink: String?,
    val isPinned: Boolean,
    val isLocked: Boolean,
    val originalFullName: String
)

fun FileItem.toMediaFileUiModel(): MediaFileUiModel {
    val extension = this.name.substringAfterLast('.', "")
    val nameWithoutExt = this.name.substringBeforeLast('.')
    
    val epRegex = Regex("""(S\d+E\d+|E\d+|Track\s*\d+)""", RegexOption.IGNORE_CASE)
    val epMatch = epRegex.find(nameWithoutExt)
    
    val tagRegex = Regex("""(\[.*?\]|\(.*?\)|1080p|720p|4K|WEB-DL|BluRay)""", RegexOption.IGNORE_CASE)
    val tagMatches = tagRegex.findAll(nameWithoutExt).toList()
    
    val episode = epMatch?.value
    val releaseTag = if (tagMatches.isNotEmpty()) tagMatches.joinToString(" ") { it.value } else null
    
    var title = nameWithoutExt
    epMatch?.let { title = title.replace(it.value, "") }
    tagMatches.forEach { title = title.replace(it.value, "") }
    title = title.replace(Regex("""[-_.]+"""), " ").trim()
    if (title.isEmpty()) title = nameWithoutExt
    
    return MediaFileUiModel(
        id = this.id,
        path = this.path,
        title = title,
        episodeTrackName = episode,
        releaseTag = releaseTag,
        size = this.size,
        date = this.changed,
        duration = this.duration,
        extension = extension.uppercase(),
        mediaType = this.type,
        thumbnailLink = this.thumbnailLink,
        isPinned = this.isPinned,
        isLocked = this.isLocked,
        originalFullName = this.name
    )
}

@Composable
fun MediaFileListCard(
    file: MediaFileUiModel,
    isSelected: Boolean = false,
    imageLoader: coil.ImageLoader,
    cornerRoundness: Float,
    modifier: Modifier = Modifier,
    onExpandFullName: (MediaFileUiModel) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPinClick: () -> Unit,
    onLockClick: () -> Unit,
    onUnlockClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val colors = LocalSkylineLedgerColors.current
    val outerShape = getDynamicCornerShape(16f, cornerRoundness)
    val chipShape = getDynamicCornerShape(10f, cornerRoundness * 0.6f)
    
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.64f) else MaterialTheme.colorScheme.surface
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .shadow(2.dp, outerShape)
            .clip(outerShape)
            .background(bgColor)
            .border(if (isSelected) 2.dp else 0.dp, borderColor, outerShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            val isAudio = file.mediaType == "audio"
            val isVideo = file.mediaType == "video"
            val audioBgColor = remember(file.originalFullName) {
                val hue = (Math.abs(file.originalFullName.hashCode()) % 360).toFloat()
                Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.4f, 0.7f)))
            }
            
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .clip(chipShape)
                    .background(if (isAudio) audioBgColor else colors.thumbnailBackground),
                contentAlignment = Alignment.Center
            ) {
                if (!isAudio) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val requestBuilder = remember(file.path) {
                        coil.request.ImageRequest.Builder(context)
                            .data(file.thumbnailLink ?: java.io.File(file.path))
                            .apply {
                                if (isVideo) {
                                    videoFrameMillis(1000)
                                }
                            }
                            .build()
                    }

                    coil.compose.AsyncImage(
                        model = requestBuilder,
                        imageLoader = imageLoader,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }

                if (isVideo || isAudio) {
                    Icon(
                        imageVector = if (isVideo) Icons.Filled.PlayArrow else Icons.Filled.MusicNote,
                        contentDescription = "Play",
                        tint = if (isAudio) Color.White else colors.textPrimary,
                        modifier = Modifier.size(32.dp).background(if (isAudio) Color.Transparent else Color.Black.copy(alpha = 0.3f), CircleShape).clip(CircleShape).padding(4.dp)
                    )
                }
                
                if (file.duration != null && file.duration.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp)
                            .background(colors.chipBackground.copy(alpha = 0.8f), chipShape)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = file.duration,
                            fontFamily = com.ripple.filemanager.ui.theme.JetBrainsMonoFamily,
                            fontSize = 8.sp,
                            color = colors.amberAccent
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Content
            Column(
                modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = file.title,
                    fontFamily = com.ripple.filemanager.ui.theme.FrauncesFontFamily,
                    color = colors.textPrimary,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (file.episodeTrackName != null) {
                    Text(
                        text = file.episodeTrackName,
                        fontFamily = com.ripple.filemanager.ui.theme.JetBrainsMonoFamily,
                        color = colors.amberAccent,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                if (file.releaseTag != null) {
                    Text(
                        text = file.releaseTag,
                        fontFamily = com.ripple.filemanager.ui.theme.JetBrainsMonoFamily,
                        color = colors.textFaintSecondary,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { onExpandFullName(file) }
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = file.size,
                        fontFamily = com.ripple.filemanager.ui.theme.JetBrainsMonoFamily,
                        color = colors.textFaintPrimary,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(colors.chipBackground, chipShape)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = file.date,
                            fontFamily = com.ripple.filemanager.ui.theme.JetBrainsMonoFamily,
                            color = colors.amberAccent,
                            fontSize = 9.sp
                        )
                    }
                }
            }
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More",
                        tint = colors.textFaintPrimary
                    )
                }
                if (showMenu) {
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    val yOffset = with(density) { (-40).dp.roundToPx() }
                    androidx.compose.ui.window.Popup(alignment = Alignment.TopEnd, offset = androidx.compose.ui.unit.IntOffset(0, yOffset), onDismissRequest = { showMenu = false }, properties = androidx.compose.ui.window.PopupProperties(focusable = true)) {
                        Surface(
                            shape = com.ripple.filemanager.ui.getDynamicCornerShape(12f, cornerRoundness),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, com.ripple.filemanager.ui.theme.SkylineColors.Border),
                            shadowElevation = 4.dp
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { showMenu = false; onPinClick() }, modifier = Modifier.size(36.dp)) { Icon(if (file.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin, contentDescription = null, tint = com.ripple.filemanager.ui.theme.SkylineColors.Amber) }
                                IconButton(onClick = { showMenu = false; if (file.isLocked) onUnlockClick() else onLockClick() }, modifier = Modifier.size(36.dp)) { Icon(if (file.isLocked) Icons.Outlined.LockOpen else Icons.Outlined.Lock, contentDescription = null, tint = com.ripple.filemanager.ui.theme.SkylineColors.Amber) }
                                IconButton(onClick = { showMenu = false; onInfoClick() }, modifier = Modifier.size(36.dp)) { Icon(Icons.Outlined.Info, contentDescription = null, tint = com.ripple.filemanager.ui.theme.SkylineColors.Amber) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MediaFileGridCard(
    file: MediaFileUiModel,
    isSelected: Boolean = false,
    columns: Int,
    imageLoader: coil.ImageLoader,
    cornerRoundness: Float,
    modifier: Modifier = Modifier,
    onExpandFullName: (MediaFileUiModel) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPinClick: () -> Unit,
    onLockClick: () -> Unit,
    onUnlockClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val colors = LocalSkylineLedgerColors.current
    val outerShape = getDynamicCornerShape(16f, cornerRoundness)
    val innerShape = getDynamicCornerShape(8f, cornerRoundness * 0.4f)
    
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.64f) else MaterialTheme.colorScheme.surface
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, outerShape)
            .clip(outerShape)
            .background(bgColor)
            .border(if (isSelected) 2.dp else 0.dp, borderColor, outerShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Column {
            // Thumbnail Area
            val isAudio = file.mediaType == "audio"
            val isVideo = file.mediaType == "video"
            val audioBgColor = remember(file.originalFullName) {
                val hue = (Math.abs(file.originalFullName.hashCode()) % 360).toFloat()
                Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.4f, 0.7f)))
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(if (isAudio) audioBgColor else colors.thumbnailBackground)
            ) {
                if (!isAudio) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val requestBuilder = remember(file.path) {
                        coil.request.ImageRequest.Builder(context)
                            .data(file.thumbnailLink ?: java.io.File(file.path))
                            .apply {
                                if (isVideo) {
                                    videoFrameMillis(1000)
                                }
                            }
                            .build()
                    }

                    coil.compose.AsyncImage(
                        model = requestBuilder,
                        imageLoader = imageLoader,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }

                if (isVideo || isAudio) {
                    Icon(
                        imageVector = if (isVideo) Icons.Filled.PlayArrow else Icons.Filled.MusicNote,
                        contentDescription = "Play",
                        tint = if (isAudio) Color.White else colors.textPrimary,
                        modifier = Modifier.size(if (columns == 4) 24.dp else 40.dp).align(Alignment.Center).background(if (isAudio) Color.Transparent else Color.Black.copy(alpha = 0.3f), CircleShape).clip(CircleShape).padding(if (columns == 4) 2.dp else 4.dp)
                    )
                }
                
                if (file.duration != null && file.duration.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(if (columns == 4) 4.dp else 8.dp)
                            .background(colors.chipBackground.copy(alpha = 0.8f), innerShape)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = file.duration,
                            fontFamily = com.ripple.filemanager.ui.theme.JetBrainsMonoFamily,
                            fontSize = if (columns == 4) 7.sp else 9.sp,
                            color = colors.amberAccent
                        )
                    }
                }
                
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(if (columns == 4) 2.dp else 4.dp)) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (showMenu) {
                        val density = androidx.compose.ui.platform.LocalDensity.current
                        val yOffset = with(density) { (-40).dp.roundToPx() }
                        androidx.compose.ui.window.Popup(alignment = Alignment.TopEnd, offset = androidx.compose.ui.unit.IntOffset(0, yOffset), onDismissRequest = { showMenu = false }, properties = androidx.compose.ui.window.PopupProperties(focusable = true)) {
                            Surface(
                                shape = com.ripple.filemanager.ui.getDynamicCornerShape(12f, cornerRoundness),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, com.ripple.filemanager.ui.theme.SkylineColors.Border),
                                shadowElevation = 4.dp
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { showMenu = false; onPinClick() }, modifier = Modifier.size(36.dp)) { Icon(if (file.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin, contentDescription = null, tint = com.ripple.filemanager.ui.theme.SkylineColors.Amber) }
                                    IconButton(onClick = { showMenu = false; if (file.isLocked) onUnlockClick() else onLockClick() }, modifier = Modifier.size(36.dp)) { Icon(if (file.isLocked) Icons.Outlined.LockOpen else Icons.Outlined.Lock, contentDescription = null, tint = com.ripple.filemanager.ui.theme.SkylineColors.Amber) }
                                    IconButton(onClick = { showMenu = false; onInfoClick() }, modifier = Modifier.size(36.dp)) { Icon(Icons.Outlined.Info, contentDescription = null, tint = com.ripple.filemanager.ui.theme.SkylineColors.Amber) }
                                }
                            }
                        }
                    }
                }
            }
            
            // Content Area
            Column(modifier = Modifier.padding(8.dp)) {
                // Title
                Text(
                    text = file.title,
                    fontFamily = com.ripple.filemanager.ui.theme.FrauncesFontFamily,
                    color = colors.textPrimary,
                    fontSize = if (columns == 4) 10.sp else 13.sp,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = if (columns == 4) 12.sp else 16.sp
                )
                
                if (columns < 4) {
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    if (file.episodeTrackName != null) {
                        Text(
                            text = file.episodeTrackName,
                            fontFamily = com.ripple.filemanager.ui.theme.JetBrainsMonoFamily,
                            color = colors.amberAccent,
                            fontSize = if (columns == 3) 9.sp else 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Text(
                        text = file.releaseTag ?: file.originalFullName,
                        fontFamily = com.ripple.filemanager.ui.theme.JetBrainsMonoFamily,
                        color = colors.textFaintSecondary,
                        fontSize = if (columns == 3) 8.sp else 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { onExpandFullName(file) }.padding(vertical = 2.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = file.size,
                        fontFamily = com.ripple.filemanager.ui.theme.JetBrainsMonoFamily,
                        color = colors.textFaintPrimary,
                        fontSize = if (columns == 4) 8.sp else 9.sp
                    )
                    
                    if (columns < 4) {
                        Box(
                            modifier = Modifier
                                .background(colors.chipBackground, innerShape)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = file.date,
                                fontFamily = com.ripple.filemanager.ui.theme.JetBrainsMonoFamily,
                                color = colors.amberAccent,
                                fontSize = if (columns == 3) 7.sp else 9.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullFileNameSheet(
    file: MediaFileUiModel,
    cornerRoundness: Float,
    onDismiss: () -> Unit
) {
    val colors = LocalSkylineLedgerColors.current
    val sheetShape = getDynamicCornerShape(28f, cornerRoundness * 1.5f)
    val innerShape = getDynamicCornerShape(12f, cornerRoundness * 0.5f)
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    var showCopied by remember { mutableStateOf(false) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = sheetShape
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 48.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "FILE DETAILS",
                fontFamily = com.ripple.filemanager.ui.theme.JetBrainsMonoFamily,
                color = colors.textFaintPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.thumbnailBackground, innerShape)
                    .padding(16.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = file.originalFullName,
                        fontFamily = com.ripple.filemanager.ui.theme.JetBrainsMonoFamily,
                        color = colors.textPrimary,
                        fontSize = 14.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(file.originalFullName))
                    showCopied = true
                    coroutineScope.launch {
                        delay(1400)
                        showCopied = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.amberAccent, contentColor = colors.chipBackground),
                shape = innerShape,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                AnimatedContent(
                    targetState = showCopied,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) }
                ) { copied ->
                    if (copied) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copied!", fontFamily = com.ripple.filemanager.ui.theme.FrauncesFontFamily, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copy Full Name", fontFamily = com.ripple.filemanager.ui.theme.FrauncesFontFamily, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
