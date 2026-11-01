package com.ripple.filemanager.ui.expressive

import android.os.Environment
import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripple.filemanager.AppAction
import com.ripple.filemanager.AppState
import com.ripple.filemanager.FileItem
import com.ripple.filemanager.R
import com.ripple.filemanager.SortMode

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.filled.ArrowDropDown
import com.ripple.filemanager.ui.theme.LocalAppFont
import com.ripple.filemanager.ui.getFolderAccent
import kotlinx.coroutines.launch
import java.io.File
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext

/**
 * Expressive Browse Screen:
 * - Centered header
 * - Breadcrumb (current segment as accent pill)
 * - Chips row (Sort -> sheet, Filter, List/Grid toggle)
 * - SectionContainer with list rows or grid cards
 * - Folder cards keep per-folder colour + item-count pill
 */
sealed class BrowseStoragePage {
    data class Storage(val item: ConnectedStorageItem) : BrowseStoragePage()
    object ConnectPrompt : BrowseStoragePage()
}

@Composable
fun ExpressiveBrowseScreen(
    state: AppState,
    onAction: (AppAction) -> Unit,
    onFileClick: (FileItem) -> Unit,
    onFileMenuClick: (FileItem) -> Unit,
    onMenuClick: () -> Unit = {},
    onTrashClick: () -> Unit = { onAction(AppAction.SetTrashScreenVisible(true)) },
    onOrganiseClick: () -> Unit = { onAction(AppAction.OrganiseDownloads) },
    onOpenNewConnection: () -> Unit = {},
    onConnectGoogleDrive: () -> Unit = {},
    onLaunchDrivePicker: (() -> Unit)? = null,
    onResolveAuthIntent: (() -> Unit)? = null,
    onConnectMega: () -> Unit = {},
    onConnectDropbox: () -> Unit = {},
    onConnectNextcloud: () -> Unit = {},
    onConnectWebDav: () -> Unit = {},
    onConnectSftp: () -> Unit = {},
    onConnectFtp: () -> Unit = {},
    onConnectSmb: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    SideEffect {
        com.ripple.filemanager.ui.TabSwitchLatencyTracker.onBrowseCompose(state.location, state.files.size, state.isLoading)
    }
    val colors = ExpressiveTheme.colors
    val coroutineScope = rememberCoroutineScope()
    var showSortSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showStorageSheet by remember { mutableStateOf(false) }

    val rootPath = Environment.getExternalStorageDirectory().absolutePath

    // 1. All connected storages
    val connectedStorages = remember(state) { getConnectedStorages(state) }
    val hasCloudStorages = remember(connectedStorages) {
        connectedStorages.any { it.id != "internal" && it.id != "sd" }
    }

    // 2. Ordered list of pages: Internal -> SD -> Cloud/Network -> ConnectPrompt (when no cloud connected)
    val pages = remember(connectedStorages, hasCloudStorages) {
        val list = connectedStorages.map { BrowseStoragePage.Storage(it) }.toMutableList<BrowseStoragePage>()
        if (!hasCloudStorages) {
            list.add(BrowseStoragePage.ConnectPrompt)
        }
        list
    }

    // 3. Current active storage ID based on state.location
    val currentStorageId = remember(state.location, state.sdCardPath, state.googleDriveAccountEmail, state.megaAccountEmail) {
        com.ripple.filemanager.getStorageIdForLocation(
            state.location,
            state.sdCardPath,
            state.googleDriveAccountEmail,
            state.megaAccountEmail
        )
    }

    val initialPageIndex = remember(pages) {
        val idx = pages.indexOfFirst { it is BrowseStoragePage.Storage && it.item.id == currentStorageId }
        if (idx >= 0) idx else 0
    }

    val pagerState = rememberPagerState(
        initialPage = initialPageIndex,
        pageCount = { pages.size }
    )

    val activeStorageItem = remember(currentStorageId, connectedStorages) {
        connectedStorages.find { it.id == currentStorageId } ?: connectedStorages.firstOrNull()
    }

    // 4. Sync pager with currentStorageId (from dropdown / external navigation)
    LaunchedEffect(currentStorageId) {
        val targetIndex = pages.indexOfFirst { it is BrowseStoragePage.Storage && it.item.id == currentStorageId }
        if (targetIndex >= 0 && targetIndex != pagerState.currentPage && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(targetIndex)
        }
    }

    // 5. Sync currentStorageId with pager swipe
    LaunchedEffect(pagerState.currentPage) {
        val page = pages.getOrNull(pagerState.currentPage)
        if (page is BrowseStoragePage.Storage) {
            val isSameStorage = page.item.id == currentStorageId ||
                (page.item.id.startsWith("drive") && currentStorageId.startsWith("drive")) ||
                (page.item.id.startsWith("mega") && currentStorageId.startsWith("mega"))
            if (!isSameStorage) {
                onAction(AppAction.SwitchToStorage(page.item.id, page.item.location))
            }
        }
    }

    // 6. Root detection and Back navigation
    val currentPageObj = pages.getOrNull(pagerState.currentPage)
    val isAtRoot = remember(state.location, currentStorageId, state.sdCardPath, state.driveFolderStack, currentPageObj) {
        when (currentPageObj) {
            is BrowseStoragePage.ConnectPrompt -> true
            is BrowseStoragePage.Storage -> {
                when {
                    currentStorageId == "internal" -> state.location == rootPath || state.location == "home" || !state.location.contains("/")
                    currentStorageId == "sd" -> {
                        val sdRoot = state.sdCardPath ?: "sd"
                        state.location == sdRoot || state.location == "sd"
                    }
                    currentStorageId.startsWith("drive") -> state.location == "drive" || state.location.startsWith("drive:") || state.driveFolderStack.isEmpty()
                    currentStorageId.startsWith("mega") -> state.location == "mega" || state.location.startsWith("mega:") || state.driveFolderStack.isEmpty()
                    currentStorageId == "dropbox" -> state.location == "dropbox" || state.driveFolderStack.isEmpty()
                    else -> state.location.endsWith(":/") || state.driveFolderStack.isEmpty()
                }
            }
            null -> true
        }
    }

    fun navigateUp() {
        if (state.location.startsWith("/")) {
            val parent = java.io.File(state.location).parent
            val minRoot = if (currentStorageId == "sd" && state.sdCardPath != null) state.sdCardPath!! else rootPath
            if (parent != null && parent.length >= minRoot.length) {
                onAction(AppAction.SetLocation(parent))
            } else {
                onAction(AppAction.SetLocation(minRoot))
            }
        } else if (state.driveFolderStack.isNotEmpty()) {
            onAction(AppAction.NavigateBackInDrive)
        } else {
            onAction(AppAction.SetLocation("home"))
        }
    }

    BackHandler(enabled = true) {
        if (!isAtRoot) {
            navigateUp()
        } else {
            onAction(AppAction.SetLocation("home"))
        }
    }

    // Header Subtitle: active storage name with chevron
    val headerSubtitle = when (currentPageObj) {
        is BrowseStoragePage.ConnectPrompt -> "Connect storage"
        is BrowseStoragePage.Storage -> currentPageObj.item.name
        null -> activeStorageItem?.name ?: "Internal storage"
    }

    val currentStorageRoot = when (currentStorageId) {
        "internal" -> rootPath
        "sd" -> state.sdCardPath ?: "sd"
        else -> activeStorageItem?.location ?: rootPath
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // 1. Expressive Screen Header with storage switcher subtitle
        ExpressiveScreenHeader(
            title = stringResource(R.string.nav_browse),
            subtitle = headerSubtitle,
            onSubtitleClick = { showStorageSheet = true },
            subtitleChevron = true,
            leftIcon = if (isAtRoot) Icons.Default.Menu else Icons.AutoMirrored.Filled.ArrowBack,
            onLeftClick = if (isAtRoot) onMenuClick else { { navigateUp() } },
            leftDescription = if (isAtRoot) "Open menu" else "Back",
            rightIcon = Icons.Outlined.Delete,
            onRightClick = onTrashClick,
            rightDescription = "Open trash",
            showSearch = currentPageObj !is BrowseStoragePage.ConnectPrompt,
            query = state.query,
            onQueryChange = { onAction(AppAction.SetQuery(it)) },
            searchPlaceholder = stringResource(R.string.search_files_hint),
            onAiSearchClick = onOrganiseClick
        )


        // 3. Breadcrumbs (independent horizontal scroll, placed above pager to avoid conflict)
        if (currentPageObj !is BrowseStoragePage.ConnectPrompt) {
            BrowseBreadcrumbs(
                currentLocation = state.location,
                rootPath = currentStorageRoot,
                driveFolderStack = state.driveFolderStack,
                currentFolderName = state.currentFolderName,
                storageName = activeStorageItem?.name ?: "Storage",
                onNavigate = { onAction(AppAction.SetLocation(it)) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Controls row: Sort, Filter, List/Grid toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExpressiveChip(
                    label = when (state.sortMode) {
                        SortMode.ALPHABETICAL -> stringResource(R.string.sort_name)
                        SortMode.DATE -> stringResource(R.string.sort_date_modified)
                        SortMode.SIZE -> stringResource(R.string.sort_size_label)
                    },
                    selected = state.sortMode != SortMode.ALPHABETICAL,
                    icon = Icons.Outlined.Sort,
                    onClick = { showSortSheet = true }
                )

                ExpressiveChip(
                    label = if (state.filter != "all") {
                        when (state.filter) {
                            "image", "images" -> stringResource(R.string.cat_images)
                            "video", "videos" -> stringResource(R.string.cat_videos)
                            "audio" -> stringResource(R.string.cat_audio)
                            "doc", "docs" -> stringResource(R.string.cat_docs)
                            "apk", "apps" -> stringResource(R.string.cat_apps)
                            "archive", "archives" -> stringResource(R.string.cat_archives)
                            else -> state.filter.replaceFirstChar { it.uppercase() }
                        }
                    } else stringResource(R.string.filter_label),
                    selected = state.filter != "all",
                    icon = Icons.Outlined.FilterList,
                    onClick = { showFilterSheet = true }
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = { onAction(AppAction.ToggleViewMode) },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(colors.card)
                ) {
                    Icon(
                        imageVector = if (state.isListMode) Icons.Default.GridView else Icons.Default.ViewList,
                        contentDescription = "Toggle view",
                        tint = colors.text,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 5. HorizontalPager for swiping between storages
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { pageIndex ->
            val page = pages[pageIndex]
            when (page) {
                is BrowseStoragePage.ConnectPrompt -> {
                    ConnectCloudPromptContent(
                        onConnectGoogleDrive = onConnectGoogleDrive,
                        onConnectMega = onConnectMega,
                        onConnectDropbox = onConnectDropbox,
                        onConnectNextcloud = onConnectNextcloud,
                        onConnectWebDav = onConnectWebDav,
                        onConnectSftp = onConnectSftp,
                        onConnectFtp = onConnectFtp,
                        onConnectSmb = onConnectSmb
                    )
                }
                is BrowseStoragePage.Storage -> {
                    val isMatchingStorage = page.item.id == currentStorageId ||
                        (currentStorageId.startsWith("drive") && page.item.id.startsWith("drive")) ||
                        (currentStorageId.startsWith("mega") && page.item.id.startsWith("mega"))
                    val isLocal = currentStorageId == "internal" || currentStorageId == "sd"
                    val isDriveAuth = currentStorageId.startsWith("drive") && state.recoverableAuthIntent != null

                    SectionContainer(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (state.isLoading && isMatchingStorage) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                com.ripple.filemanager.ui.ExpressiveCircularLoader(
                                    size = 46.dp,
                                    color = colors.accent
                                )
                            }
                        } else if (!state.hasStoragePermission && state.files.isEmpty() && isLocal) {
                            StoragePermissionCard(
                                onGrantAccess = { onAction(AppAction.RequestStorageAccess) }
                            )
                        } else if (state.errorMessage != null && state.files.isEmpty() && isMatchingStorage) {
                            StorageErrorCard(
                                message = state.errorMessage,
                                onRetry = { onAction(AppAction.Reload) },
                                actionLabel = if (isDriveAuth) stringResource(R.string.grant_permission) else if (isLocal && !state.hasStoragePermission) stringResource(R.string.permission_grant_access) else null,
                                onGrantAccess = when {
                                    isDriveAuth -> {
                                        { onResolveAuthIntent?.invoke() }
                                    }
                                    isLocal && !state.hasStoragePermission -> {
                                        { onAction(AppAction.RequestStorageAccess) }
                                    }
                                    else -> null
                                }
                            )
                        } else if ((state.location.contains("Android/data") || state.location.contains("Android/obb")) && !state.hasShizuku) {
                            ShizukuRestrictedAccessCard(
                                location = state.location,
                                onAction = onAction
                            )
                        } else if (state.files.isEmpty()) {
                            InnerCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.this_folder_empty),
                                        fontSize = 15.sp,
                                        color = colors.muted,
                                        textAlign = TextAlign.Center
                                    )
                                    if (currentStorageId.startsWith("drive") && onLaunchDrivePicker != null) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Button(
                                            onClick = onLaunchDrivePicker,
                                            shape = RoundedCornerShape(24.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = colors.accent,
                                                contentColor = colors.ink
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Cloud,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(R.string.select_files_from_drive),
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (state.isListMode) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 130.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
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
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 130.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
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
        }
    }

    // Storage Switcher Bottom Sheet
    if (showStorageSheet) {
        StorageSwitcherBottomSheet(
            state = state,
            currentStorageId = currentStorageId,
            onDismissRequest = { showStorageSheet = false },
            onSelectStorage = { location ->
                val stId = com.ripple.filemanager.getStorageIdForLocation(
                    location,
                    state.sdCardPath,
                    state.googleDriveAccountEmail,
                    state.megaAccountEmail
                )
                onAction(AppAction.SwitchToStorage(stId, location))
                showStorageSheet = false
            },
            onAddConnection = {
                showStorageSheet = false
                onOpenNewConnection()
            },
            onLogoutStorage = { storage ->
                when {
                    storage.id == "drive" || storage.id.startsWith("drive_") -> {
                        val email = if (storage.id.startsWith("drive_")) storage.id.removePrefix("drive_") else state.googleDriveAccountEmail
                        onAction(AppAction.SetGoogleDriveAuthStatus(false, email))
                    }
                    storage.id == "mega" || storage.id.startsWith("mega_") -> {
                        val email = if (storage.id.startsWith("mega_")) storage.id.removePrefix("mega_") else state.megaAccountEmail
                        onAction(AppAction.SetMegaAuthStatus(false, email, null))
                    }
                    storage.id == "dropbox" -> {
                        onAction(AppAction.SetDropboxAuthStatus(false, null))
                    }
                    storage.id.startsWith("smb_") -> {
                        val smbId = storage.id.removePrefix("smb_")
                        onAction(AppAction.SmbAction.DeleteConnection(smbId))
                        onAction(AppAction.SmbAction.Disconnect(smbId))
                    }
                    storage.id.startsWith("ftp_") -> {
                        val ftpId = storage.id.removePrefix("ftp_")
                        onAction(AppAction.FtpAction.DeleteConnection(ftpId))
                        onAction(AppAction.FtpAction.Disconnect(ftpId))
                    }
                    storage.id.startsWith("sftp_") -> {
                        val sftpId = storage.id.removePrefix("sftp_")
                        onAction(AppAction.SftpAction.DeleteConnection(sftpId))
                        onAction(AppAction.SftpAction.Disconnect(sftpId))
                    }
                    storage.id.startsWith("webdav_") || storage.id.startsWith("nextcloud_") -> {
                        val webdavId = storage.id.substringAfter("_")
                        onAction(AppAction.WebDavAction.DeleteConnection(webdavId))
                        onAction(AppAction.WebDavAction.Disconnect(webdavId))
                    }
                }
                val internalRoot = Environment.getExternalStorageDirectory().absolutePath
                onAction(AppAction.SwitchToStorage("internal", internalRoot))
                showStorageSheet = false
            }
        )
    }

    // Sort Bottom Sheet
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

    // Filter Bottom Sheet
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
}

// ═══════════════════════════════════════════════════════════════════════
// BREADCRUMBS
// ═══════════════════════════════════════════════════════════════════════

@Composable
internal fun BrowseBreadcrumbs(
    currentLocation: String,
    rootPath: String,
    driveFolderStack: List<Pair<String, String>> = emptyList(),
    currentFolderName: String? = null,
    storageName: String = "Storage",
    onNavigate: (String) -> Unit
) {
    val colors = ExpressiveTheme.colors
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val isLocalPath = currentLocation.startsWith("/")
        val isRoot = if (isLocalPath) {
            currentLocation == rootPath || currentLocation.length <= rootPath.length
        } else {
            driveFolderStack.isEmpty() && (currentLocation == rootPath || currentLocation == "drive" || currentLocation == "mega" || currentLocation == "dropbox" || currentLocation.endsWith(":/"))
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (isRoot) colors.accent else colors.card)
                .clickable { onNavigate(rootPath) }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = storageName,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isRoot) colors.ink else colors.text
            )
        }

        if (isLocalPath) {
            // Local path segments
            if (currentLocation.startsWith(rootPath) && currentLocation.length > rootPath.length) {
                val relative = currentLocation.removePrefix(rootPath).removePrefix("/")
                val segments = relative.split("/").filter { it.isNotEmpty() }
                var accumulated = rootPath

                segments.forEachIndexed { index, seg ->
                    Text("/", color = colors.muted, fontSize = 12.sp)
                    accumulated = "$accumulated/$seg"
                    val target = accumulated
                    val isLast = index == segments.size - 1

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isLast) colors.accent else colors.card)
                            .clickable { onNavigate(target) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = seg,
                            fontSize = 12.sp,
                            fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                            color = if (isLast) colors.ink else colors.text
                        )
                    }
                }
            }
        } else {
            // Cloud folder stack segments
            driveFolderStack.forEach { (stackLoc, stackName) ->
                Text("/", color = colors.muted, fontSize = 12.sp)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.card)
                        .clickable { onNavigate(stackLoc) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stackName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = colors.text
                    )
                }
            }
            if (currentFolderName != null && !isRoot) {
                Text("/", color = colors.muted, fontSize = 12.sp)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.accent)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = currentFolderName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.ink
                    )
                }
            }
        }
    }
}


@Composable
private fun StoragePermissionCard(
    onGrantAccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ExpressiveTheme.colors
    InnerCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CookieIcon(
                icon = Icons.Outlined.FolderShared,
                bgColor = ExpressiveTokens.PastelCoral,
                size = 52.dp,
                lobes = 8
            )
            Text(
                text = stringResource(R.string.permission_dialog_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.text,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.permission_dialog_message),
                fontSize = 13.sp,
                color = colors.muted,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onGrantAccess,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.ink
                )
            ) {
                Text(stringResource(R.string.permission_grant_access), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StorageErrorCard(
    message: String,
    onRetry: () -> Unit,
    actionLabel: String? = null,
    onGrantAccess: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = ExpressiveTheme.colors
    InnerCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CookieIcon(
                icon = Icons.Outlined.CloudOff,
                bgColor = ExpressiveTokens.PastelCoral,
                size = 52.dp,
                lobes = 8
            )
            Text(
                text = stringResource(R.string.failed_to_load_files),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.text,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                fontSize = 13.sp,
                color = colors.muted,
                textAlign = TextAlign.Center
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onGrantAccess != null) {
                    Button(
                        onClick = onGrantAccess,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.ink
                        )
                    ) {
                        Text(actionLabel ?: stringResource(R.string.permission_grant_access), fontWeight = FontWeight.SemiBold)
                    }
                }
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (onGrantAccess != null) colors.card else colors.accent,
                        contentColor = if (onGrantAccess != null) colors.text else colors.ink
                    )
                ) {
                    Text(stringResource(R.string.retry), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ShizukuRestrictedAccessCard(
    location: String,
    onAction: (AppAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ExpressiveTheme.colors
    val context = LocalContext.current
    var isShizukuRunning by remember { mutableStateOf(false) }

    val isShizukuInstalled = remember(context) {
        try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0) != null
        } catch (e: Exception) {
            false
        }
    }

    LaunchedEffect(location) {
        while (true) {
            val running = try { rikka.shizuku.Shizuku.pingBinder() } catch (e: Exception) { false }
            isShizukuRunning = running
            if (running) {
                val hasPerm = try {
                    rikka.shizuku.Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
                } catch (e: Exception) {
                    false
                }
                if (hasPerm) {
                    onAction(AppAction.Reload)
                    break
                }
            }
            kotlinx.coroutines.delay(2000)
        }
    }

    InnerCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CookieIcon(
                icon = Icons.Outlined.Lock,
                bgColor = ExpressiveTokens.PastelCoral,
                size = 56.dp,
                lobes = 8
            )

            Text(
                text = stringResource(R.string.access_restricted_title),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.text,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.access_restricted_desc),
                fontSize = 13.sp,
                color = colors.muted,
                textAlign = TextAlign.Center
            )

            val statusText = when {
                isShizukuRunning -> stringResource(R.string.shizuku_running_desc)
                isShizukuInstalled -> stringResource(R.string.shizuku_not_running_desc)
                else -> stringResource(R.string.shizuku_not_installed_desc)
            }

            Text(
                text = statusText,
                fontSize = 12.sp,
                color = colors.text.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            when {
                isShizukuRunning -> {
                    Button(
                        onClick = { onAction(AppAction.RequestShizukuAccess) },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.ink
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.grant_access_action),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                isShizukuInstalled -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val launchIntent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                                if (launchIntent != null) {
                                    context.startActivity(launchIntent)
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accent,
                                contentColor = colors.ink
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Launch,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.open_shizuku_action),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        OutlinedButton(
                            onClick = { onAction(AppAction.Reload) },
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.check_again_action),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
                else -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val playIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=moe.shizuku.privileged.api")).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                try {
                                    context.startActivity(playIntent)
                                } catch (e: Exception) {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    })
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accent,
                                contentColor = colors.ink
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.install_shizuku_action),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        OutlinedButton(
                            onClick = { onAction(AppAction.RequestShizukuAccess) },
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.grant_access_action),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class BrowseAddStorageOption(
    val name: String,
    val icon: ImageVector,
    val pastelKey: String,
    val onClick: () -> Unit
)

@Composable
private fun BrowseAddStorageTile(
    option: BrowseAddStorageOption,
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

@Composable
private fun ConnectCloudPromptContent(
    onConnectGoogleDrive: () -> Unit,
    onConnectMega: () -> Unit,
    onConnectDropbox: () -> Unit,
    onConnectNextcloud: () -> Unit,
    onConnectWebDav: () -> Unit,
    onConnectSftp: () -> Unit,
    onConnectFtp: () -> Unit,
    onConnectSmb: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ExpressiveTheme.colors
    LazyColumn(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 130.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
    ) {
        item {
            SectionContainer {
                InnerCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CookieIcon(
                            icon = Icons.Outlined.Cloud,
                            bgColor = ExpressiveTokens.PastelSky,
                            size = 56.dp,
                            lobes = 10
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.connect_cloud_storage),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.text,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.connect_cloud_desc),
                            fontSize = 13.sp,
                            color = colors.muted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        item {
            SectionTitle(title = stringResource(R.string.cloud_network_drives))
        }

        item {
            SectionContainer {
                val providers = listOf(
                    BrowseAddStorageOption("Google", Icons.Outlined.Cloud, "images", onConnectGoogleDrive),
                    BrowseAddStorageOption("Mega", Icons.Outlined.Cloud, "archives", onConnectMega),
                    BrowseAddStorageOption("Dropbox", Icons.Outlined.Cloud, "videos", onConnectDropbox),
                    BrowseAddStorageOption("SMB (NAS)", Icons.Outlined.Dns, "large", onConnectSmb),
                    BrowseAddStorageOption("Nextcloud", Icons.Outlined.CloudSync, "downloads", onConnectNextcloud),
                    BrowseAddStorageOption("WebDAV", Icons.Outlined.Http, "docs", onConnectWebDav),
                    BrowseAddStorageOption("SFTP", Icons.Outlined.Security, "apps", onConnectSftp),
                    BrowseAddStorageOption("FTP", Icons.Outlined.FolderShared, "audio", onConnectFtp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        providers.take(4).forEach { p ->
                            BrowseAddStorageTile(option = p, modifier = Modifier.weight(1f))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        providers.drop(4).take(4).forEach { p ->
                            BrowseAddStorageTile(option = p, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// LIST ROW & GRID CARD
// ═══════════════════════════════════════════════════════════════════════

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun BrowseListRow(
    file: FileItem,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val colors = ExpressiveTheme.colors
    val pastel = if (file.type == "folder") {
        getFolderAccent(file.name)
    } else {
        ExpressiveTokens.categoryPastel(fileTypeCategory(file.name))
    }

    val roundness = LocalCornerRoundness.current
    val rowShape = RoundedCornerShape((26f * (roundness * 2).coerceIn(0f, 2f)).dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (file.isHidden) 0.55f else 1f)
            .clip(rowShape)
            .background(if (isSelected) colors.accent.copy(alpha = 0.25f) else colors.card)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onSelect
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelect
            )
        ) {
            ExpressiveFileThumbnail(
                file = file,
                isSelected = isSelected,
                size = 48.dp,
                lobes = cookieLobesForName(file.name)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = file.name,
                    fontFamily = com.ripple.filemanager.ui.theme.LocalAppFont.current,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.text,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (file.isLocked) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = "Locked",
                        tint = colors.accent,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            val meta = if (file.type == "folder") {
                rememberFolderMeta(file)
            } else {
                formatFileMeta(file)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = meta,
                fontFamily = com.ripple.filemanager.ui.theme.LocalAppFont.current,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                color = colors.muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onMenuClick) {
            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = colors.muted, modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun BrowseGridCard(
    file: FileItem,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    columns: Int = 2
) {
    val colors = ExpressiveTheme.colors
    val pastel = if (file.type == "folder") {
        getFolderAccent(file.name)
    } else {
        ExpressiveTokens.categoryPastel(fileTypeCategory(file.name))
    }
    val roundness = LocalCornerRoundness.current
    val baseRadius = when {
        columns >= 4 -> 16f
        columns == 3 -> 22f
        else -> 28f
    }
    val cardShape = RoundedCornerShape((baseRadius * (roundness * 2).coerceIn(0f, 2f)).dp)

    val iconSize = when {
        columns >= 4 -> 36.dp
        columns == 3 -> 44.dp
        else -> 52.dp
    }
    val cardPadding = when {
        columns >= 4 -> 8.dp
        columns == 3 -> 10.dp
        else -> 14.dp
    }
    val titleFontSize = when {
        columns >= 4 -> 11.sp
        columns == 3 -> 12.sp
        else -> 13.sp
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (file.isHidden) 0.55f else 1f)
            .clip(cardShape)
            .background(colors.card)
            .border(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected) colors.accent else colors.line.copy(alpha = colors.lineAlpha),
                cardShape
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onSelect
            )
    ) {
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(if (columns >= 4) 2.dp else 6.dp)
                .size(if (columns >= 4) 22.dp else 28.dp)
        ) {
            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = colors.muted, modifier = Modifier.size(if (columns >= 4) 14.dp else 16.dp))
        }

        Column(
            modifier = Modifier.padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(if (columns >= 4) 6.dp else if (columns == 3) 8.dp else 12.dp)
        ) {
            ExpressiveFileThumbnail(
                file = file,
                isSelected = isSelected,
                size = iconSize,
                lobes = cookieLobesForName(file.name)
            )

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = file.name,
                        fontFamily = com.ripple.filemanager.ui.theme.LocalAppFont.current,
                        fontSize = titleFontSize,
                        lineHeight = if (columns >= 4) 14.sp else if (columns == 3) 16.sp else 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (file.isLocked) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Locked",
                            tint = colors.accent,
                            modifier = Modifier.size(if (columns >= 4) 10.dp else 12.dp)
                        )
                    }
                }

                val metaText = if (file.type == "folder") {
                    rememberFolderMeta(file)
                } else {
                    formatFileMeta(file)
                }
                Text(
                    text = metaText,
                    fontFamily = com.ripple.filemanager.ui.theme.LocalAppFont.current,
                    fontSize = if (columns >= 4) 10.sp else 11.sp,
                    lineHeight = if (columns >= 4) 13.sp else 14.sp,
                    color = colors.muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun fileTypeCategory(name: String): String {
    val ext = name.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "jpg", "jpeg", "png", "gif", "webp" -> "images"
        "mp4", "mkv", "mov", "webm" -> "videos"
        "mp3", "flac", "wav", "m4a", "ogg" -> "audio"
        "pdf", "doc", "docx", "txt" -> "docs"
        "apk" -> "apps"
        "zip", "rar", "7z", "tar" -> "archives"
        else -> "large"
    }
}

// ═══════════════════════════════════════════════════════════════════════
// SORT & FILTER BOTTOM SHEETS
// ═══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SortBottomSheet(
    currentMode: SortMode,
    onDismiss: () -> Unit,
    onSelectMode: (SortMode) -> Unit
) {
    val colors = ExpressiveTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
        containerColor = colors.container,
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
    ) {
        ExpressiveSheetContent(title = stringResource(R.string.sort_by)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                listOf(
                    SortMode.ALPHABETICAL to stringResource(R.string.sort_name),
                    SortMode.DATE to stringResource(R.string.sort_date_modified),
                    SortMode.SIZE to stringResource(R.string.sort_size_label)
                ).forEach { (mode, label) ->
                    val isSelected = mode == currentMode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) colors.card else Color.Transparent)
                            .clickable { onSelectMode(mode) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = colors.text,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.accent)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterBottomSheet(
    currentFilter: String,
    onDismiss: () -> Unit,
    onSelectFilter: (String) -> Unit
) {
    val colors = ExpressiveTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
        containerColor = colors.container,
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
    ) {
        ExpressiveSheetContent(title = stringResource(R.string.filter_label)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                listOf(
                    "all" to stringResource(R.string.all_files),
                    "image" to stringResource(R.string.cat_images),
                    "video" to stringResource(R.string.cat_videos),
                    "audio" to stringResource(R.string.cat_audio),
                    "doc" to stringResource(R.string.cat_docs),
                    "apk" to stringResource(R.string.cat_apps),
                    "archive" to stringResource(R.string.cat_archives)
                ).forEach { (filter, label) ->
                    val isSelected = filter == currentFilter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) colors.card else Color.Transparent)
                            .clickable { onSelectFilter(filter) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = colors.text,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.accent)
                        }
                    }
                }
            }
        }
    }
}