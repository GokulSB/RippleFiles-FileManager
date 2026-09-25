package com.ripple.filemanager.ui.expressive

import android.os.Build
import android.provider.OpenableColumns
import android.provider.Settings
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripple.filemanager.AppAction
import com.ripple.filemanager.AppState
import com.ripple.filemanager.FileItem
import com.ripple.filemanager.localsend.NearbyPeer
import com.ripple.filemanager.localsend.TransferStatus
import com.ripple.filemanager.ui.theme.LocalAppFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Expressive Send Screen:
 * - Concentric-ring radar (Canvas, one slow pulse ring; respect reduce-motion)
 * - Real device alias in the centre
 * - Real discovered nearby devices (LocalSend v2 protocol) positioned on radar & listed below
 * - Receive toggle row with ExpressiveSwitch hooked to foreground service
 * - Staged files row & file picker launcher
 * - Real-time active transfer card with progress & speed
 */
@Composable
fun ExpressiveSendScreen(
    state: AppState,
    onAction: (AppAction) -> Unit = {},
    onMenuClick: () -> Unit = {},
    onTrashClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = ExpressiveTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val localAlias = state.nearbyDeviceName.ifEmpty { "${Build.MANUFACTURER} ${Build.MODEL}" }

    // File Picker for staging files
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                val stagingDir = File(context.cacheDir, "send_staging").apply { mkdirs() }
                val staged = uris.mapNotNull { uri ->
                    try {
                        var name = "file_${System.currentTimeMillis()}"
                        var size = 0L
                        val cursor = context.contentResolver.query(uri, null, null, null, null)
                        cursor?.use {
                            if (it.moveToFirst()) {
                                val nameIdx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                val sizeIdx = it.getColumnIndex(OpenableColumns.SIZE)
                                if (nameIdx >= 0) name = it.getString(nameIdx)
                                if (sizeIdx >= 0) size = it.getLong(sizeIdx)
                            }
                        }

                        val targetFile = File(stagingDir, "${System.currentTimeMillis()}_$name")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(targetFile).use { output ->
                                input.copyTo(output)
                            }
                        }

                        val finalSize = if (size > 0) size else targetFile.length()
                        FileItem(
                            id = targetFile.hashCode(),
                            path = targetFile.absolutePath,
                            name = name,
                            type = name.substringAfterLast(".", "file"),
                            kind = "File",
                            size = Formatter.formatFileSize(context, finalSize),
                            changed = "",
                            owner = "",
                            sizeBytes = finalSize,
                            lastModified = targetFile.lastModified()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                if (staged.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        onAction(AppAction.NearbyShareAction.StageFiles(staged))
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 130.dp)
    ) {
        item {
            ExpressiveScreenHeader(
                title = "Send & Receive",
                subtitle = "Nearby share",
                leftIcon = Icons.Default.Menu,
                onLeftClick = onMenuClick,
                leftDescription = "Open menu",
                rightIcon = Icons.Outlined.Delete,
                onRightClick = onTrashClick,
                rightDescription = "Open trash",
                showSearch = false
            )
        }

        // Active Transfer Progress Card (if any transfer is ongoing)
        if (state.activeNearbyTransfer != null) {
            val session = state.activeNearbyTransfer
            item {
                SectionContainer {
                    InnerCard {
                        Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    CookieIcon(
                                        icon = if (session.isIncoming) Icons.Outlined.Download else Icons.Outlined.Upload,
                                        bgColor = colors.accent,
                                        iconTint = colors.ink,
                                        size = 42.dp,
                                        lobes = 8
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (session.isIncoming) "Receiving from ${session.peer.alias}" else "Sending to ${session.peer.alias}",
                                            fontFamily = LocalAppFont.current,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.text,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = when (session.status) {
                                                TransferStatus.INITIALIZING -> "Connecting..."
                                                TransferStatus.WAITING_ACCEPT -> "Waiting for acceptance..."
                                                TransferStatus.IN_PROGRESS -> "Transferring (${session.currentFileIndex + 1}/${session.files.size})"
                                                TransferStatus.COMPLETED -> "Completed"
                                                TransferStatus.FAILED -> session.errorMessage ?: "Failed"
                                                TransferStatus.REJECTED -> "Declined"
                                                TransferStatus.CANCELLED -> "Cancelled"
                                            },
                                            fontFamily = LocalAppFont.current,
                                            fontSize = 12.sp,
                                            color = when (session.status) {
                                                TransferStatus.COMPLETED -> colors.accent
                                                TransferStatus.FAILED, TransferStatus.REJECTED, TransferStatus.CANCELLED -> colors.muted
                                                else -> colors.muted
                                            }
                                        )
                                    }
                                }

                                if (session.status == TransferStatus.IN_PROGRESS || session.status == TransferStatus.WAITING_ACCEPT) {
                                    TextButton(
                                        onClick = { onAction(AppAction.NearbyShareAction.CancelTransfer) }
                                    ) {
                                        Text("Cancel", color = colors.muted, fontSize = 13.sp)
                                    }
                                } else {
                                    TextButton(
                                        onClick = { onAction(AppAction.NearbyShareAction.DismissTransfer) }
                                    ) {
                                        Text("Done", color = colors.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (session.status == TransferStatus.IN_PROGRESS) {
                                Spacer(modifier = Modifier.height(12.dp))
                                val reduced = ExpressiveMotion.isReducedMotion()
                                val animatedProgress by animateFloatAsState(
                                    targetValue = session.progress,
                                    animationSpec = if (reduced) snap() else ExpressiveMotion.FastTween,
                                    label = "transfer_progress"
                                )
                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = colors.accent,
                                    trackColor = colors.insetCard
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val transferred = Formatter.formatFileSize(context, session.bytesTransferred)
                                    val total = Formatter.formatFileSize(context, session.totalBytes)
                                    Text(
                                        text = "$transferred / $total",
                                        fontSize = 11.sp,
                                        color = colors.muted
                                    )
                                    if (session.speedBytesPerSec > 0) {
                                        Text(
                                            text = "${Formatter.formatFileSize(context, session.speedBytesPerSec)}/s",
                                            fontSize = 11.sp,
                                            color = colors.muted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Radar Box
        item {
            SectionContainer {
                InnerCard(contentPadding = PaddingValues(0.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RadarCanvas(
                            localAlias = localAlias,
                            peers = state.nearbyPeers,
                            onPeerClick = { peer ->
                                if (state.stagedNearbyFiles.isEmpty()) {
                                    filePicker.launch("*/*")
                                } else {
                                    onAction(AppAction.NearbyShareAction.SendToPeer(peer))
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(290.dp)
                        )
                    }
                }
            }
        }

        // Staged Files Section
        if (state.stagedNearbyFiles.isNotEmpty()) {
            item {
                SectionContainer {
                    InnerCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Staged files (${state.stagedNearbyFiles.size})",
                                    fontFamily = LocalAppFont.current,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.text
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { filePicker.launch("*/*") }) {
                                        Text("+ Add", color = colors.accent, fontSize = 12.sp)
                                    }
                                    TextButton(onClick = { onAction(AppAction.NearbyShareAction.ClearStagedFiles) }) {
                                        Text("Clear", color = colors.muted, fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(state.stagedNearbyFiles) { fileItem ->
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(colors.insetCard)
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = fileItem.name,
                                            fontFamily = LocalAppFont.current,
                                            fontSize = 12.sp,
                                            color = colors.text,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = 140.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = colors.muted,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable {
                                                    onAction(AppAction.NearbyShareAction.RemoveStagedFile(fileItem))
                                                }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap a nearby device on the radar to start sending",
                                fontFamily = LocalAppFont.current,
                                fontSize = 11.sp,
                                color = colors.accent
                            )
                        }
                    }
                }
            }
        } else {
            item {
                SectionContainer {
                    InnerCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Files to send",
                                    fontFamily = LocalAppFont.current,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.text
                                )
                                Text(
                                    text = "Select files to send to nearby devices",
                                    fontFamily = LocalAppFont.current,
                                    fontSize = 12.sp,
                                    color = colors.muted
                                )
                            }

                            Button(
                                onClick = { filePicker.launch("*/*") },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.accent,
                                    contentColor = colors.ink
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Choose Files",
                                    fontFamily = LocalAppFont.current,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Receive Row with ExpressiveSwitch
        item {
            SectionContainer {
                InnerCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            CookieIcon(
                                icon = Icons.Outlined.PhoneAndroid,
                                bgColor = colors.insetCard,
                                iconTint = colors.text,
                                size = 46.dp,
                                lobes = 8
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Receive files",
                                    fontFamily = LocalAppFont.current,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.text
                                )
                                Text(
                                    text = if (state.isNearbyReceiving) "Visible to nearby devices" else "Not visible",
                                    fontFamily = LocalAppFont.current,
                                    fontSize = 12.sp,
                                    color = colors.muted
                                )
                            }
                        }

                        ExpressiveSwitch(
                            checked = state.isNearbyReceiving,
                            onCheckedChange = { onAction(AppAction.NearbyShareAction.ToggleReceive(it)) }
                        )
                    }
                }
            }
        }

        // Nearby Devices Section
        item {
            SectionTitle(title = "Nearby devices (${state.nearbyPeers.size})")
        }

        item {
            SectionContainer {
                if (state.nearbyPeers.isEmpty()) {
                    InnerCard {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (state.isNearbyReceiving) "Searching for nearby devices..." else "Receiving is disabled",
                                    fontFamily = LocalAppFont.current,
                                    fontSize = 13.sp,
                                    color = colors.muted,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Make sure LocalSend or Ripple Files is open on the same Wi-Fi",
                                    fontFamily = LocalAppFont.current,
                                    fontSize = 11.sp,
                                    color = colors.muted.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.nearbyPeers.forEach { peer ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(26.dp))
                                    .background(colors.card)
                                    .clickable {
                                        if (state.stagedNearbyFiles.isEmpty()) {
                                            filePicker.launch("*/*")
                                        } else {
                                            onAction(AppAction.NearbyShareAction.SendToPeer(peer))
                                        }
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CookieIcon(
                                    icon = getDeviceIcon(peer.deviceType),
                                    bgColor = ExpressiveTokens.PastelPeach,
                                    size = 42.dp,
                                    lobes = 8
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = peer.alias,
                                        fontFamily = LocalAppFont.current,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.text,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${peer.deviceModel ?: peer.deviceType} · ${peer.ip}",
                                        fontFamily = LocalAppFont.current,
                                        fontSize = 11.sp,
                                        color = colors.muted
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (state.stagedNearbyFiles.isEmpty()) {
                                            filePicker.launch("*/*")
                                        } else {
                                            onAction(AppAction.NearbyShareAction.SendToPeer(peer))
                                        }
                                    },
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.accent,
                                        contentColor = colors.ink
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Send",
                                        fontFamily = LocalAppFont.current,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RadarCanvas(
    localAlias: String,
    peers: List<NearbyPeer>,
    onPeerClick: (NearbyPeer) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ExpressiveTheme.colors
    val reduceMotion = ExpressiveMotion.isReducedMotion()

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseProgress by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (reduceMotion) 1000000 else 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_progress"
    )

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val width = maxWidth
        val height = maxHeight

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = min(size.width, size.height) * 0.44f

            // Concentric rings (3 fixed)
            listOf(0.33f, 0.66f, 1f).forEach { fraction ->
                drawCircle(
                    color = colors.line.copy(alpha = colors.lineAlpha * 1.5f),
                    radius = maxR * fraction,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Pulse ring
            if (!reduceMotion) {
                val pulseRadius = maxR * pulseProgress
                val pulseAlpha = (1f - pulseProgress) * 0.25f
                drawCircle(
                    color = colors.accent.copy(alpha = pulseAlpha),
                    radius = pulseRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Center Cookie ("You" / Local Device Alias)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
        ) {
            CookieIcon(
                icon = Icons.Outlined.Person,
                bgColor = colors.accent,
                iconTint = colors.ink,
                size = 54.dp,
                lobes = 10
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = localAlias,
                fontFamily = LocalAppFont.current,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 110.dp),
                textAlign = TextAlign.Center
            )
        }

        // Render Discovered Peers around concentric rings
        peers.forEachIndexed { index, peer ->
            val angleDeg = (index * (360f / maxOf(peers.size, 1)) - 60f)
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val ringFraction = if (index % 2 == 0) 0.66f else 0.88f

            val maxRVal = min(width.value, height.value) * 0.44f
            val offsetX = (cos(angleRad) * (maxRVal * ringFraction)).dp
            val offsetY = (sin(angleRad) * (maxRVal * ringFraction)).dp

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(x = offsetX, y = offsetY)
                    .clickable { onPeerClick(peer) }
            ) {
                CookieIcon(
                    icon = getDeviceIcon(peer.deviceType),
                    bgColor = ExpressiveTokens.PastelPeach,
                    iconTint = Color(0xFF1E1E1E),
                    size = 40.dp,
                    lobes = 8
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = peer.alias,
                    fontFamily = LocalAppFont.current,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.card.copy(alpha = 0.85f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                        .widthIn(max = 70.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun getDeviceIcon(deviceType: String): ImageVector {
    return when (deviceType.lowercase()) {
        "desktop", "web", "server" -> Icons.Outlined.Computer
        "tablet" -> Icons.Outlined.Tablet
        else -> Icons.Outlined.PhoneAndroid
    }
}
