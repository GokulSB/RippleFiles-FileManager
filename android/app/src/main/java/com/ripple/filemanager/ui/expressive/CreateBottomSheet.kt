package com.ripple.filemanager.ui.expressive

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripple.filemanager.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBottomSheet(
    onDismissRequest: () -> Unit,
    onNewFolder: () -> Unit,
    onNewFile: () -> Unit,
    onAddCloud: () -> Unit,
    onReceive: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = ExpressiveTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = colors.container,
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
    ) {
        ExpressiveSheetContent(title = stringResource(R.string.create_title)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CreateTileItem(
                    title = stringResource(R.string.new_folder),
                    icon = Icons.Outlined.CreateNewFolder,
                    category = "folder",
                    onClick = {
                        onDismissRequest()
                        onNewFolder()
                    }
                )
                CreateTileItem(
                    title = stringResource(R.string.new_file),
                    icon = Icons.Outlined.NoteAdd,
                    category = "docs",
                    onClick = {
                        onDismissRequest()
                        onNewFile()
                    }
                )
                CreateTileItem(
                    title = stringResource(R.string.add_cloud),
                    icon = Icons.Outlined.CloudUpload,
                    category = "archives",
                    onClick = {
                        onDismissRequest()
                        onAddCloud()
                    }
                )
                CreateTileItem(
                    title = stringResource(R.string.receive_action),
                    icon = Icons.AutoMirrored.Outlined.Send,
                    category = "large",
                    onClick = {
                        onDismissRequest()
                        onReceive()
                    }
                )
            }
        }
    }
}

@Composable
private fun CreateTileItem(
    title: String,
    icon: ImageVector,
    category: String,
    onClick: () -> Unit
) {
    val colors = ExpressiveTheme.colors
    val pastel = ExpressiveTokens.categoryPastel(category)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(8.dp)
    ) {
        CookieIcon(
            icon = icon,
            bgColor = pastel,
            iconTint = ExpressiveTokens.OnPastel,
            size = 56.dp,
            lobes = cookieLobesForName(title)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = colors.text
        )
    }
}
