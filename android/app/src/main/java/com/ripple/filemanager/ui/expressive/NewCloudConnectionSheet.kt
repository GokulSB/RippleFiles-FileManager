package com.ripple.filemanager.ui.expressive

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewCloudConnectionSheet(
    onDismissRequest: () -> Unit,
    onConnectGoogleDrive: () -> Unit,
    onConnectMega: () -> Unit,
    onConnectDropbox: () -> Unit,
    onConnectNextcloud: () -> Unit,
    onConnectWebDav: () -> Unit,
    onConnectSftp: () -> Unit,
    onConnectFtp: () -> Unit,
    onConnectSmb: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = ExpressiveTheme.colors

    val options = listOf(
        CloudOption("Mega", Icons.Outlined.Cloud, "archives") {
            onDismissRequest()
            onConnectMega()
        },
        CloudOption("Dropbox", Icons.Outlined.Cloud, "videos") {
            onDismissRequest()
            onConnectDropbox()
        },
        CloudOption("Nextcloud", Icons.Outlined.CloudSync, "downloads") {
            onDismissRequest()
            onConnectNextcloud()
        },
        CloudOption("WebDAV", Icons.Outlined.Http, "docs") {
            onDismissRequest()
            onConnectWebDav()
        },
        CloudOption("SFTP", Icons.Outlined.Security, "apps") {
            onDismissRequest()
            onConnectSftp()
        },
        CloudOption("FTP", Icons.Outlined.FolderShared, "audio") {
            onDismissRequest()
            onConnectFtp()
        },
        CloudOption("SMB", Icons.Outlined.Dns, "large") {
            onDismissRequest()
            onConnectSmb()
        },
        CloudOption("Google", Icons.Outlined.Cloud, "images") {
            onDismissRequest()
            onConnectGoogleDrive()
        }
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = colors.container,
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
    ) {
        ExpressiveSheetContent(title = "New cloud connection") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Row 1 (4 items)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    options.take(4).forEach { option ->
                        CloudOptionTile(option = option, modifier = Modifier.weight(1f))
                    }
                }

                // Row 2 (4 items)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    options.drop(4).take(4).forEach { option ->
                        CloudOptionTile(option = option, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private data class CloudOption(
    val title: String,
    val icon: ImageVector,
    val category: String,
    val onClick: () -> Unit
)

@Composable
private fun CloudOptionTile(
    option: CloudOption,
    modifier: Modifier = Modifier
) {
    val colors = ExpressiveTheme.colors
    val pastel = ExpressiveTokens.categoryPastel(option.category)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = option.onClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CookieIcon(
            icon = option.icon,
            bgColor = pastel,
            iconTint = ExpressiveTokens.OnPastel,
            size = 52.dp,
            lobes = cookieLobesForName(option.title)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = option.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = com.ripple.filemanager.ui.theme.LocalAppFont.current,
            color = colors.text,
            maxLines = 1
        )
    }
}
