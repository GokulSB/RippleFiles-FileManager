package com.ripple.filemanager.ui.expressive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripple.filemanager.FileDetails
import com.ripple.filemanager.FileItem
import com.ripple.filemanager.ui.theme.LocalAppFont

/**
 * Expressive bottom sheet for file/folder details and primary actions (Share / Rename / Delete).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileInfoBottomSheet(
    file: FileItem,
    fileDetails: FileDetails?,
    onDismissRequest: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = ExpressiveTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = colors.container,
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.container)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Grab handle centered at top
            Box(
                modifier = Modifier
                    .padding(top = 4.dp, bottom = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.muted.copy(alpha = 0.4f))
            )

            // Header row: left-aligned title (Outfit 400 22sp) + "✕" close cookie button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = file.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = LocalAppFont.current,
                    color = colors.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                CookieIconButton(
                    onClick = onDismissRequest,
                    icon = Icons.Default.Close,
                    bgColor = colors.card,
                    iconTint = colors.text,
                    description = "Close",
                    size = 40.dp,
                    lobes = 8
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Primary actions row (Share / Rename / Delete)
            // Each rendered as cookie-icon-over-label tile (cookie shape ~48dp, label below 13-14sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionTileItem(
                    title = "Share",
                    icon = Icons.Outlined.Share,
                    iconTint = colors.text,
                    labelColor = colors.text,
                    onClick = onShare
                )
                ActionTileItem(
                    title = "Rename",
                    icon = Icons.Outlined.Edit,
                    iconTint = colors.text,
                    labelColor = colors.text,
                    onClick = onRename
                )
                ActionTileItem(
                    title = "Delete",
                    icon = Icons.Outlined.Delete,
                    iconTint = colors.accent,
                    labelColor = colors.accent,
                    onClick = onDelete
                )
            }

            // Section divider line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .height(1.dp)
                    .background(colors.line.copy(alpha = colors.lineAlpha))
            )

            // Stat rows (Type, Size, Items, Modified, Owner) inside an InnerCard
            InnerCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
            ) {
                if (fileDetails == null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = colors.accent,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Loading details...",
                            fontSize = 13.sp,
                            fontFamily = LocalAppFont.current,
                            color = colors.muted
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val stats = listOfNotNull(
                            "Type" to if (file.type == "folder" || fileDetails.isFolder) "Folder" else (file.type.replaceFirstChar { it.uppercase() }),
                            "Size" to fileDetails.size,
                            fileDetails.itemCount?.let { "Items" to it.toString() },
                            "Modified" to fileDetails.changed,
                            "Owner" to fileDetails.owner,
                            fileDetails.format?.let { "Format" to it },
                            fileDetails.resolution?.let { "Resolution" to it },
                            fileDetails.duration?.let { "Duration" to it }
                        )

                        stats.forEachIndexed { index, (label, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = LocalAppFont.current,
                                    color = colors.muted
                                )
                                Text(
                                    text = value,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal,
                                    fontFamily = LocalAppFont.current,
                                    color = colors.text,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                            if (index < stats.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(colors.line.copy(alpha = colors.lineAlpha * 0.6f))
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Path row inside its own InnerCard (allows wrapping so long paths are never clipped)
            InnerCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Location",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = LocalAppFont.current,
                        color = colors.muted
                    )
                    Text(
                        text = fileDetails?.path ?: file.path,
                        fontSize = 13.sp,
                        fontFamily = LocalAppFont.current,
                        color = colors.text,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionTileItem(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    labelColor: Color,
    onClick: () -> Unit
) {
    val colors = ExpressiveTheme.colors

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        CookieIcon(
            icon = icon,
            bgColor = colors.card,
            iconTint = iconTint,
            size = 48.dp,
            lobes = 8
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = LocalAppFont.current,
            color = labelColor
        )
    }
}
