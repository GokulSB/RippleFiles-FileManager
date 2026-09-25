package com.ripple.filemanager.ui.expressive

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.ripple.filemanager.R
import com.ripple.filemanager.AppAction
import com.ripple.filemanager.AppState
import com.ripple.filemanager.FileItem
import com.ripple.filemanager.haptics.LocalHaptics
import com.ripple.filemanager.ui.theme.LocalAppFont

data class CloudConnection(
    val id: String,
    val name: String,
    val accountInfo: String,
    val rootLocation: String,
    val icon: ImageVector,
    val category: String,
    val totalBytes: Long = 0L,
    val freeBytes: Long = 0L
)

/**
 * Expressive Cloud Screen:
 * - When no connections: displays "Add storage" options.
 * - When connected: swipeable title area switching between connected accounts,
 *   with the related files displayed below.
 */
@Composable
fun ExpressiveCloudScreen(
    state: AppState,
    onAction: (AppAction) -> Unit,
    onConnectGoogleDrive: () -> Unit,
    onConnectMega: () -> Unit,
    onConnectDropbox: () -> Unit,
    onConnectNextcloud: () -> Unit,
    onConnectWebDav: () -> Unit,
    onConnectSftp: () -> Unit,
    onConnectFtp: () -> Unit,
    onConnectSmb: () -> Unit,
    onLogoutGoogleDrive: () -> Unit = {},
    onFileClick: (FileItem) -> Unit = {},
    onFileMenuClick: (FileItem) -> Unit = {},
    onMenuClick: () -> Unit = {},
    onTrashClick: () -> Unit = { onAction(AppAction.SetTrashScreenVisible(true)) },
    modifier: Modifier = Modifier
) {
    val colors = ExpressiveTheme.colors
    val haptics = LocalHaptics.current
    var showSortSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showLogoutConfirmForConn by remember { mutableStateOf<CloudConnection?>(null) }

    // Build the list of active/authenticated cloud connections
    val activeConnections = remember(
        state.isGoogleDriveAuthenticated,
        state.googleDriveAccountEmail,
        state.driveStorageTotalBytes,
        state.driveStorageFreeBytes,
        state.isMegaAuthenticated,
        state.megaAccountEmail,
        state.isDropboxAuthenticated,
        state.dropboxAccountEmail,
        state.smbState.savedConnections,
        state.ftpState.savedConnections,
        state.sftpState.savedConnections,
        state.webDavState.savedConnections
    ) {
        val list = mutableListOf<CloudConnection>()
        if (state.isGoogleDriveAuthenticated) {
            list.add(
                CloudConnection(
                    id = "drive",
                    name = "Google Drive",
                    accountInfo = state.googleDriveAccountEmail ?: "Connected",
                    rootLocation = "drive",
                    icon = Icons.Outlined.Cloud,
                    category = "docs",
                    totalBytes = state.driveStorageTotalBytes,
                    freeBytes = state.driveStorageFreeBytes
                )
            )
        }
        if (state.isMegaAuthenticated) {
            list.add(
                CloudConnection(
                    id = "mega",
                    name = "Mega",
                    accountInfo = state.megaAccountEmail ?: "Connected",
                    rootLocation = "mega",
                    icon = Icons.Outlined.Cloud,
                    category = "archives"
                )
            )
        }
        if (state.isDropboxAuthenticated) {
            list.add(
                CloudConnection(
                    id = "dropbox",
                    name = "Dropbox",
                    accountInfo = state.dropboxAccountEmail ?: "Connected",
                    rootLocation = "dropbox",
                    icon = Icons.Outlined.Cloud,
                    category = "videos"
                )
            )
        }
        state.smbState.savedConnections.forEach { conn ->
            list.add(
                CloudConnection(
                    id = "smb_${conn.id}",
                    name = conn.displayName.ifBlank { conn.host },
                    accountInfo = "SMB · ${conn.host}",
                    rootLocation = "smb_${conn.id}:/",
                    icon = Icons.Outlined.Dns,
                    category = "large"
                )
            )
        }
        state.ftpState.savedConnections.forEach { conn ->
            list.add(
                CloudConnection(
                    id = "ftp_${conn.id}",
                    name = conn.displayName.ifBlank { conn.host },
                    accountInfo = "FTP · ${conn.host}",
                    rootLocation = "ftp_${conn.id}:/",
                    icon = Icons.Outlined.FolderShared,
                    category = "audio"
                )
            )
        }
        state.sftpState.savedConnections.forEach { conn ->
            list.add(
                CloudConnection(
                    id = "sftp_${conn.id}",
                    name = conn.displayName.ifBlank { conn.host },
                    accountInfo = "SFTP · ${conn.host}",
                    rootLocation = "sftp_${conn.id}:/",
                    icon = Icons.Outlined.Security,
                    category = "apps"
                )
            )
        }
        state.webDavState.savedConnections.forEach { conn ->
            val prefix = if (conn.isNextcloud) "nextcloud" else "webdav"
            list.add(
                CloudConnection(
                    id = "${prefix}_${conn.id}",
                    name = conn.displayName.ifBlank { if (conn.isNextcloud) "Nextcloud" else "WebDAV" },
                    accountInfo = if (conn.isNextcloud) "Nextcloud · ${conn.serverUrl}" else "WebDAV · ${conn.serverUrl}",
                    rootLocation = "${prefix}_${conn.id}:/",
                    icon = Icons.Outlined.CloudSync,
                    category = "downloads"
                )
            )
        }
        list
    }

    // Determine currently active connection index matching state.location
    val currentConnectionIndex = remember(state.location, activeConnections) {
        val idx = activeConnections.indexOfFirst { conn ->
            state.location == conn.rootLocation ||
            state.location.startsWith("${conn.id}:") ||
            (conn.id == "drive" && state.location.startsWith("drive")) ||
            (conn.id == "mega" && state.location.startsWith("mega")) ||
            (conn.id == "dropbox" && state.location.startsWith("dropbox"))
        }
        if (idx >= 0) idx else 0
    }

    // When on "cloud" and connections exist, automatically route to the first connection
    LaunchedEffect(activeConnections, state.location) {
        if (state.location == "cloud" && activeConnections.isNotEmpty()) {
            onAction(AppAction.SetLocation(activeConnections[0].rootLocation))
        }
    }

    if (activeConnections.isEmpty()) {
        // Empty state: No connections yet
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 130.dp)
        ) {
            item {
                ExpressiveScreenHeader(
                    title = "Cloud",
                    subtitle = "Connected storage",
                    leftIcon = Icons.Default.Menu,
                    onLeftClick = onMenuClick,
                    leftDescription = "Open menu",
                    rightIcon = Icons.Outlined.Delete,
                    onRightClick = onTrashClick,
                    rightDescription = "Open trash",
                    showSearch = false
                )
            }

            item {
                SectionTitle(title = "Connected")
            }

            item {
                SectionContainer {
                    InnerCard {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No cloud providers or servers connected yet.\nTap an option below to connect.",
                                fontSize = 13.sp,
                                color = colors.muted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            item {
                SectionTitle(title = "Add storage")
            }

            item {
                SectionContainer {
                    val providers = listOf(
                        AddStorageOption("Mega", Icons.Outlined.Cloud, "archives", onConnectMega),
                        AddStorageOption("Dropbox", Icons.Outlined.Cloud, "videos", onConnectDropbox),
                        AddStorageOption("Nextcloud", Icons.Outlined.CloudSync, "downloads", onConnectNextcloud),
                        AddStorageOption("WebDAV", Icons.Outlined.Http, "docs", onConnectWebDav),
                        AddStorageOption("SFTP", Icons.Outlined.Security, "apps", onConnectSftp),
                        AddStorageOption("FTP", Icons.Outlined.FolderShared, "audio", onConnectFtp),
                        AddStorageOption("SMB", Icons.Outlined.Dns, "large", onConnectSmb),
                        AddStorageOption("Google", Icons.Outlined.Cloud, "images", onConnectGoogleDrive)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            providers.take(4).forEach { p ->
                                AddStorageTile(option = p, modifier = Modifier.weight(1f))
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            providers.drop(4).take(4).forEach { p ->
                                AddStorageTile(option = p, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Active connections present: Render swipeable header with related files below
        val activeConn = activeConnections.getOrElse(currentConnectionIndex) { activeConnections[0] }
        val isAtRoot = state.location == activeConn.rootLocation

        var dragOffset by remember { mutableFloatStateOf(0f) }
        val density = LocalDensity.current
        val swipeThresholdPx = with(density) { 36.dp.toPx() }

        fun switchConnection(targetIndex: Int) {
            val safeIndex = (targetIndex + activeConnections.size) % activeConnections.size
            if (safeIndex != currentConnectionIndex) {
                haptics.tap()
                val targetConn = activeConnections[safeIndex]
                onAction(AppAction.SetLocation(targetConn.rootLocation))
            }
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // Expressive Swipeable Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Action: Back if in subfolder, otherwise Menu
                if (!isAtRoot) {
                    CookieIconButton(
                        onClick = {
                            if (state.location.startsWith("drive") || state.location.startsWith("mega") || state.location.startsWith("dropbox")) {
                                onAction(AppAction.NavigateBackInDrive)
                            } else if (state.location.contains("/")) {
                                val parent = state.location.substringBeforeLast("/")
                                onAction(AppAction.SetLocation(parent.ifEmpty { activeConn.rootLocation }))
                            } else {
                                onAction(AppAction.SetLocation(activeConn.rootLocation))
                            }
                        },
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        bgColor = colors.card,
                        description = "Navigate back",
                        iconTint = colors.text,
                        size = 46.dp,
                        iconSize = 24.dp,
                        lobes = 8
                    )
                } else {
                    CookieIconButton(
                        onClick = onMenuClick,
                        icon = Icons.Default.Menu,
                        bgColor = colors.card,
                        description = "Open menu",
                        iconTint = colors.text,
                        size = 46.dp,
                        iconSize = 24.dp,
                        lobes = 8
                    )
                }

                // Center Title Area: Swipeable left and right to switch connections
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .pointerInput(activeConnections.size, currentConnectionIndex) {
                            if (activeConnections.size > 1) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        if (dragOffset < -swipeThresholdPx) {
                                            switchConnection(currentConnectionIndex + 1)
                                        } else if (dragOffset > swipeThresholdPx) {
                                            switchConnection(currentConnectionIndex - 1)
                                        }
                                        dragOffset = 0f
                                    },
                                    onDragCancel = { dragOffset = 0f },
                                    onHorizontalDrag = { _, dragAmount ->
                                        dragOffset += dragAmount
                                    }
                                )
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val titleText = activeConn.name
                    val titleFontSize = when {
                        titleText.length > 16 -> 24.sp
                        titleText.length > 12 -> 28.sp
                        titleText.length > 8 -> 32.sp
                        else -> 36.sp
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (activeConnections.size > 1) {
                            IconButton(
                                onClick = { switchConnection(currentConnectionIndex - 1) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = "Previous connection",
                                    tint = colors.muted.copy(alpha = 0.8f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(2.dp))
                        }

                        Text(
                            text = titleText,
                            fontSize = titleFontSize,
                            fontWeight = FontWeight.ExtraLight,
                            fontFamily = LocalAppFont.current,
                            color = colors.text,
                            letterSpacing = (-0.6).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )

                        if (activeConnections.size > 1) {
                            Spacer(modifier = Modifier.width(2.dp))
                            IconButton(
                                onClick = { switchConnection(currentConnectionIndex + 1) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Next connection",
                                    tint = colors.muted.copy(alpha = 0.8f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    val subtitleText = activeConn.accountInfo.ifEmpty { "Connected storage" }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptics.tap()
                                showLogoutConfirmForConn = activeConn
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = subtitleText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = LocalAppFont.current,
                            color = colors.muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Pagination indicator dots
                    if (activeConnections.size > 1) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            activeConnections.forEachIndexed { index, _ ->
                                val isSelected = index == currentConnectionIndex
                                Box(
                                    modifier = Modifier
                                        .height(4.dp)
                                        .width(if (isSelected) 18.dp else 5.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) colors.accent else colors.muted.copy(alpha = 0.35f))
                                        .clickable { switchConnection(index) }
                                )
                            }
                        }
                    }
                }

                // Right Action: Trash
                CookieIconButton(
                    onClick = onTrashClick,
                    icon = Icons.Outlined.Delete,
                    bgColor = colors.card,
                    description = "Open trash",
                    iconTint = colors.text,
                    size = 46.dp,
                    iconSize = 24.dp,
                    lobes = 8
                )
            }

            // Breadcrumbs for subfolder navigation within cloud connection
            if (!isAtRoot) {
                BrowseBreadcrumbs(
                    currentLocation = state.location,
                    rootPath = activeConn.rootLocation,
                    onNavigate = { onAction(AppAction.SetLocation(it)) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Controls row: Sort, Filter, ViewMode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExpressiveChip(
                        label = "Sort: ${state.sortMode.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        selected = false,
                        icon = Icons.Outlined.Sort,
                        onClick = { showSortSheet = true }
                    )
                    ExpressiveChip(
                        label = if (state.filter == "all") "Filter" else state.filter.replaceFirstChar { it.uppercase() },
                        selected = state.filter != "all",
                        icon = Icons.Outlined.FilterList,
                        onClick = { showFilterSheet = true }
                    )
                }

                // View Mode Toggle (List / Grid)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.card)
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onAction(AppAction.SetListMode(true)) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ViewList,
                            contentDescription = "List view",
                            tint = if (state.isListMode) colors.accent else colors.muted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { onAction(AppAction.SetListMode(false)) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.GridView,
                            contentDescription = "Grid view",
                            tint = if (!state.isListMode) colors.accent else colors.muted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Related Files Display
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = colors.accent,
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading files...", fontSize = 13.sp, color = colors.muted)
                    }
                }
            } else if (state.files.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 60.dp)
                    ) {
                        CookieIcon(
                            icon = Icons.Outlined.FolderOpen,
                            bgColor = colors.card,
                            iconTint = colors.muted,
                            size = 64.dp,
                            lobes = 8
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "This folder is empty",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.text
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No files in ${activeConn.name}",
                            fontSize = 12.sp,
                            color = colors.muted
                        )
                    }
                }
            } else {
                if (state.isListMode) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 130.dp)
                    ) {
                        items(state.files, key = { it.id }) { file ->
                            BrowseListRow(
                                file = file,
                                isSelected = state.selectedFiles.contains(file.id),
                                onSelect = { onAction(AppAction.ToggleSelection(file.id)) },
                                onClick = { onFileClick(file) },
                                onMenuClick = { onFileMenuClick(file) }
                            )
                        }
                    }
                } else {
                    val columns = state.gridColumns.coerceIn(2, 4)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 130.dp)
                    ) {
                        items(state.files, key = { it.id }) { file ->
                            BrowseGridCard(
                                file = file,
                                isSelected = state.selectedFiles.contains(file.id),
                                onSelect = { onAction(AppAction.ToggleSelection(file.id)) },
                                onClick = { onFileClick(file) },
                                onMenuClick = { onFileMenuClick(file) },
                                columns = columns
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSortSheet) {
        SortBottomSheet(
            currentMode = state.sortMode,
            onDismiss = { showSortSheet = false },
            onSelectMode = {
                onAction(AppAction.SetSortMode(it))
                showSortSheet = false
            }
        )
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            currentFilter = state.filter,
            onDismiss = { showFilterSheet = false },
            onSelectFilter = {
                onAction(AppAction.SetFilter(it))
                showFilterSheet = false
            }
        )
    }

    val connToDisconnect = showLogoutConfirmForConn
    if (connToDisconnect != null) {
        val isCloudService = connToDisconnect.id in listOf("drive", "mega", "dropbox")
        ExpressiveConfirmationSheet(
            title = if (isCloudService) "Log out of ${connToDisconnect.name}?" else "Disconnect ${connToDisconnect.name}?",
            subtitle = "You will be disconnected from ${connToDisconnect.accountInfo}.",
            confirmLabel = if (isCloudService) "Log out" else "Disconnect",
            cancelLabel = "Cancel",
            onConfirm = {
                when {
                    connToDisconnect.id == "drive" -> {
                        onLogoutGoogleDrive()
                        onAction(AppAction.SetGoogleDriveAuthStatus(false, null))
                    }
                    connToDisconnect.id == "mega" -> {
                        onAction(AppAction.SetMegaAuthStatus(false, null, null))
                    }
                    connToDisconnect.id == "dropbox" -> {
                        onAction(AppAction.SetDropboxAuthStatus(false, null))
                    }
                    connToDisconnect.id.startsWith("smb_") -> {
                        val smbId = connToDisconnect.id.removePrefix("smb_")
                        onAction(AppAction.SmbAction.DeleteConnection(smbId))
                        onAction(AppAction.SmbAction.Disconnect(smbId))
                    }
                    connToDisconnect.id.startsWith("ftp_") -> {
                        val ftpId = connToDisconnect.id.removePrefix("ftp_")
                        onAction(AppAction.FtpAction.DeleteConnection(ftpId))
                        onAction(AppAction.FtpAction.Disconnect(ftpId))
                    }
                    connToDisconnect.id.startsWith("sftp_") -> {
                        val sftpId = connToDisconnect.id.removePrefix("sftp_")
                        onAction(AppAction.SftpAction.DeleteConnection(sftpId))
                        onAction(AppAction.SftpAction.Disconnect(sftpId))
                    }
                    connToDisconnect.id.startsWith("webdav_") || connToDisconnect.id.startsWith("nextcloud_") -> {
                        val webdavId = connToDisconnect.id.substringAfter("_")
                        onAction(AppAction.WebDavAction.DeleteConnection(webdavId))
                        onAction(AppAction.WebDavAction.Disconnect(webdavId))
                    }
                }
                showLogoutConfirmForConn = null
                onAction(AppAction.SetLocation("cloud"))
            },
            onDismissRequest = {
                showLogoutConfirmForConn = null
            }
        )
    }
}

private data class AddStorageOption(
    val name: String,
    val icon: ImageVector,
    val pastelKey: String,
    val onClick: () -> Unit
)

@Composable
private fun AddStorageTile(
    option: AddStorageOption,
    modifier: Modifier = Modifier
) {
    val colors = ExpressiveTheme.colors
    val pastel = ExpressiveTokens.categoryPastel(option.pastelKey)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = option.onClick
            )
            .padding(4.dp)
    ) {
        CookieIcon(
            icon = option.icon,
            bgColor = pastel,
            size = 52.dp,
            lobes = cookieLobesForName(option.name)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = option.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = LocalAppFont.current,
            color = colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
