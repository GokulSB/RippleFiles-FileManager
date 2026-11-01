package com.ripple.filemanager.ui.expressive

import android.content.Context
import android.os.Environment
import android.text.format.DateFormat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import com.ripple.filemanager.AppAction
import com.ripple.filemanager.AppState
import com.ripple.filemanager.FileItem
import com.ripple.filemanager.R
import java.util.Date
import kotlin.math.cos
import kotlin.math.sin
import android.animation.ValueAnimator
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.ui.graphics.compositeOver
import com.ripple.filemanager.ui.formatSize
import com.ripple.filemanager.ui.theme.LocalAppFont
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private var hasPlayedStorageGaugeAnimationInSession = false

/**
 * Expressive Home Screen:
 * 1. Expressive Home Header (centered "Files", date line, menu & trash cookies, 56dp search pill with AI sparkle)
 * 2. Compact storage card (78dp 24-dot cookie gauge, free space, storage switcher, 8dp meter)
 * 3. Categories (SectionContainer with 4-column cookie tiles)
 * 4. Recent (SectionContainer with file rows, cookie icon, 3-dot menu)
 */
@Composable
fun ExpressiveHomeScreen(
    state: AppState,
    onAction: (AppAction) -> Unit,
    onMenuClick: () -> Unit,
    onTrashClick: () -> Unit,
    onOrganiseClick: () -> Unit,
    onFileClick: (FileItem) -> Unit,
    onFileMenuClick: (FileItem) -> Unit,
    onOpenNewConnection: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    SideEffect {
        com.ripple.filemanager.ui.TabSwitchLatencyTracker.onHomeCompose(state.recentFiles.size)
    }
    val colors = ExpressiveTheme.colors
    var showStorageSheet by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 130.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
    ) {
        // 1. Home Header
        item(key = "header") {
            ExpressiveHomeHeader(
                query = state.query,
                onQueryChange = { onAction(AppAction.SetQuery(it)) },
                onMenuClick = onMenuClick,
                onTrashClick = onTrashClick,
                onAiSearchClick = onOrganiseClick
            )
        }

        // 2. Compact Storage Card (shown when not searching)
        if (state.query.isEmpty()) {
            item(key = "storage_card") {
                ExpressiveStorageCard(
                    state = state,
                    onOpenStorageSwitcher = { showStorageSheet = true },
                    onAction = onAction
                )
            }

            // 3. Categories Grid
            item(key = "categories") {
                ExpressiveCategoriesSection(
                    state = state,
                    onAction = onAction
                )
            }
        }

        // 4. Section Title (Search results vs Recent files)
        item(key = "files_title") {
            SectionTitle(
                title = if (state.query.isNotEmpty()) "Search results (${state.files.size})" else "Recent files"
            )
        }

        // 5. Section Container with rows
        item(key = "files_container") {
            val displayedFiles = if (state.query.isNotEmpty()) {
                state.files
            } else {
                val pool = state.recentFiles.filter { state.showHiddenFiles || !it.isHidden }
                    .ifEmpty { state.files.filter { state.showHiddenFiles || !it.isHidden } }
                pool.take(10)
            }

            if (displayedFiles.isEmpty()) {
                if (!state.hasStoragePermission && state.query.isEmpty()) {
                    SectionContainer {
                        InnerCard {
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
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
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
                                    onClick = { onAction(AppAction.RequestStorageAccess) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.accent,
                                        contentColor = colors.ink
                                    )
                                ) {
                                    Text(stringResource(R.string.permission_grant_access), fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                } else {
                    SectionContainer {
                        InnerCard {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (state.query.isNotEmpty()) {
                                        stringResource(R.string.no_files_found_query, state.query)
                                    } else {
                                        stringResource(R.string.no_recent_files)
                                    },
                                    fontSize = 14.sp,
                                    color = colors.muted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                SectionContainer {
                    InnerCard(contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                        Column {
                            displayedFiles.forEachIndexed { index, file ->
                                ExpressiveRecentFileRow(
                                    file = file,
                                    isSelected = state.selectedFiles.contains(file.id),
                                    onSelect = { onAction(AppAction.ToggleSelection(file.id)) },
                                    onClick = { onFileClick(file) },
                                    onMenuClick = { onFileMenuClick(file) }
                                )
                                if (index < displayedFiles.size - 1) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(colors.line.copy(alpha = colors.lineAlpha))
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
        val currentStId = com.ripple.filemanager.getStorageIdForLocation(
            state.location,
            state.sdCardPath,
            state.googleDriveAccountEmail,
            state.megaAccountEmail
        )
        StorageSwitcherBottomSheet(
            state = state,
            currentStorageId = currentStId,
            onDismissRequest = { showStorageSheet = false },
            onSelectStorage = { stId, location ->
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
}

// ═══════════════════════════════════════════════════════════════════════
// HOME HEADER
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun ExpressiveHomeHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onMenuClick: () -> Unit,
    onTrashClick: () -> Unit,
    onAiSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentDateStr = remember {
        DateFormat.format("EEEE, MMM d", Date()).toString()
    }

    ExpressiveScreenHeader(
        title = stringResource(R.string.app_name_brand),
        subtitle = currentDateStr,
        modifier = modifier,
        leftIcon = Icons.Default.Menu,
        onLeftClick = onMenuClick,
        leftDescription = "Open menu",
        rightIcon = Icons.Outlined.Delete,
        onRightClick = onTrashClick,
        rightDescription = "Open trash",
        showSearch = true,
        query = query,
        onQueryChange = onQueryChange,
        searchPlaceholder = stringResource(R.string.search_files_hint),
        onAiSearchClick = onAiSearchClick
    )
}

// ═══════════════════════════════════════════════════════════════════════
// CONNECTED STORAGES
// ═══════════════════════════════════════════════════════════════════════

data class ConnectedStorageItem(
    val id: String,
    val name: String,
    val detail: String,
    val totalGb: Float,
    val freeGb: Float,
    val location: String,
    val icon: ImageVector,
    val category: String,
    val gaugeColor: Color
)

fun getConnectedStorages(state: AppState): List<ConnectedStorageItem> {
    val items = mutableListOf<ConnectedStorageItem>()
    val rootPath = Environment.getExternalStorageDirectory().absolutePath

    // 1. Internal storage (always present)
    items.add(
        ConnectedStorageItem(
            id = "internal",
            name = "Internal storage",
            detail = "%.1f GB free".format(state.storageFreeGb),
            totalGb = state.storageTotalGb.coerceAtLeast(1f),
            freeGb = state.storageFreeGb.coerceAtLeast(0f),
            location = rootPath,
            icon = Icons.Outlined.PhoneAndroid,
            category = "large",
            gaugeColor = ExpressiveTokens.PastelPeach
        )
    )

    // 2. SD Card (if present)
    val sdLoc = state.sdCardPath ?: "sd"
    if (state.sdCardStorageTotalGb > 0f || (state.sdCardPath != null && state.sdCardPath != "sd")) {
        items.add(
            ConnectedStorageItem(
                id = "sd",
                name = "SD Card",
                detail = "%.1f GB free".format(state.sdCardStorageFreeGb),
                totalGb = state.sdCardStorageTotalGb.coerceAtLeast(0.1f),
                freeGb = state.sdCardStorageFreeGb.coerceAtLeast(0f),
                location = sdLoc,
                icon = Icons.Outlined.SdCard,
                category = "large",
                gaugeColor = ExpressiveTokens.PastelSand
            )
        )
    }

    // 3. Google Drive (if authenticated)
    if (state.googleDriveAccounts.isNotEmpty()) {
        state.googleDriveAccounts.forEach { email ->
            val driveTotal = if (state.driveStorageTotalBytes > 0L) {
                state.driveStorageTotalBytes.toFloat() / (1024f * 1024f * 1024f)
            } else {
                15f
            }
            val driveFree = if (state.driveStorageTotalBytes > 0L) {
                state.driveStorageFreeBytes.toFloat() / (1024f * 1024f * 1024f)
            } else {
                15f
            }
            items.add(
                ConnectedStorageItem(
                    id = "drive_$email",
                    name = "Google Drive",
                    detail = email,
                    totalGb = driveTotal,
                    freeGb = driveFree,
                    location = "drive:$email",
                    icon = Icons.Outlined.Cloud,
                    category = "docs",
                    gaugeColor = ExpressiveTokens.PastelSky
                )
            )
        }
    } else if (state.isGoogleDriveAuthenticated) {
        val driveTotal = if (state.driveStorageTotalBytes > 0L) {
            state.driveStorageTotalBytes.toFloat() / (1024f * 1024f * 1024f)
        } else {
            15f
        }
        val driveFree = if (state.driveStorageTotalBytes > 0L) {
            state.driveStorageFreeBytes.toFloat() / (1024f * 1024f * 1024f)
        } else {
            15f
        }
        items.add(
            ConnectedStorageItem(
                id = "drive",
                name = "Google Drive",
                detail = state.googleDriveAccountEmail ?: "Connected",
                totalGb = driveTotal,
                freeGb = driveFree,
                location = "drive",
                icon = Icons.Outlined.Cloud,
                category = "docs",
                gaugeColor = ExpressiveTokens.PastelSky
            )
        )
    }

    // 4. Mega (if authenticated)
    if (state.megaAccounts.isNotEmpty()) {
        state.megaAccounts.forEach { acc ->
            items.add(
                ConnectedStorageItem(
                    id = "mega_${acc.email}",
                    name = "Mega",
                    detail = acc.email,
                    totalGb = 20f,
                    freeGb = 20f,
                    location = "mega:${acc.email}",
                    icon = Icons.Outlined.Cloud,
                    category = "archives",
                    gaugeColor = ExpressiveTokens.PastelRose
                )
            )
        }
    } else if (state.isMegaAuthenticated) {
        items.add(
            ConnectedStorageItem(
                id = "mega",
                name = "Mega",
                detail = state.megaAccountEmail ?: "Connected",
                totalGb = 20f,
                freeGb = 20f,
                location = "mega",
                icon = Icons.Outlined.Cloud,
                category = "archives",
                gaugeColor = ExpressiveTokens.PastelRose
            )
        )
    }

    // 5. Dropbox (if authenticated)
    if (state.isDropboxAuthenticated) {
        items.add(
            ConnectedStorageItem(
                id = "dropbox",
                name = "Dropbox",
                detail = state.dropboxAccountEmail ?: "Connected",
                totalGb = 2f,
                freeGb = 2f,
                location = "dropbox",
                icon = Icons.Outlined.Cloud,
                category = "videos",
                gaugeColor = ExpressiveTokens.PastelCoral
            )
        )
    }

    // 6. SMB (Home NAS / local shares)
    state.smbState.savedConnections.forEach { conn ->
        items.add(
            ConnectedStorageItem(
                id = "smb_${conn.id}",
                name = conn.displayName.ifBlank { conn.host },
                detail = "SMB · ${conn.host}",
                totalGb = 0f,
                freeGb = 0f,
                location = "smb_${conn.id}:/",
                icon = Icons.Outlined.Dns,
                category = "large",
                gaugeColor = ExpressiveTokens.PastelTeal
            )
        )
    }

    // 7. FTP
    state.ftpState.savedConnections.forEach { conn ->
        items.add(
            ConnectedStorageItem(
                id = "ftp_${conn.id}",
                name = conn.displayName.ifBlank { conn.host },
                detail = "FTP · ${conn.host}",
                totalGb = 0f,
                freeGb = 0f,
                location = "ftp_${conn.id}:/",
                icon = Icons.Outlined.FolderShared,
                category = "audio",
                gaugeColor = ExpressiveTokens.PastelLilac
            )
        )
    }

    // 8. SFTP
    state.sftpState.savedConnections.forEach { conn ->
        items.add(
            ConnectedStorageItem(
                id = "sftp_${conn.id}",
                name = conn.displayName.ifBlank { conn.host },
                detail = "SFTP · ${conn.host}",
                totalGb = 0f,
                freeGb = 0f,
                location = "sftp_${conn.id}:/",
                icon = Icons.Outlined.Security,
                category = "apps",
                gaugeColor = ExpressiveTokens.PastelSky
            )
        )
    }

    // 9. WebDAV / Nextcloud
    state.webDavState.savedConnections.forEach { conn ->
        val prefix = if (conn.isNextcloud) "nextcloud" else "webdav"
        items.add(
            ConnectedStorageItem(
                id = "${prefix}_${conn.id}",
                name = conn.displayName.ifBlank { if (conn.isNextcloud) "Nextcloud" else "WebDAV" },
                detail = if (conn.isNextcloud) "Nextcloud · ${conn.serverUrl}" else "WebDAV · ${conn.serverUrl}",
                totalGb = 0f,
                freeGb = 0f,
                location = "${prefix}_${conn.id}:/",
                icon = Icons.Outlined.CloudSync,
                category = "downloads",
                gaugeColor = ExpressiveTokens.PastelSand
            )
        )
    }

    return items
}

// ═══════════════════════════════════════════════════════════════════════
// COMPACT STORAGE CARD & CAROUSEL
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun ExpressiveStorageCard(
    state: AppState,
    onOpenStorageSwitcher: () -> Unit,
    modifier: Modifier = Modifier,
    onAction: (AppAction) -> Unit = {}
) {
    val colors = ExpressiveTheme.colors
    val storages = remember(state) { getConnectedStorages(state) }
    val pagerState = rememberPagerState(pageCount = { storages.size })
    val coroutineScope = rememberCoroutineScope()
    val reduced = ExpressiveMotion.isReducedMotion()
    val animEnabled = !reduced && remember { ValueAnimator.getDurationScale() > 0f }

    // Auto-advance every 5 seconds when >1 storage and not being actively touched/dragged
    if (storages.size > 1 && animEnabled) {
        LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
            if (!pagerState.isScrollInProgress) {
                delay(5000)
                val nextPage = (pagerState.currentPage + 1) % storages.size
                pagerState.animateScrollToPage(
                    page = nextPage,
                    animationSpec = if (reduced) androidx.compose.animation.core.snap() else ExpressiveMotion.SpringSpec
                )
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionContainer {
            Box(modifier = Modifier.fillMaxWidth()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { pageIndex ->
                    val storage = storages.getOrElse(pageIndex) { storages[0] }
                    val totalGb = storage.totalGb.coerceAtLeast(0.1f)
                    val freeGb = storage.freeGb.coerceAtLeast(0f)
                    val usedGb = (totalGb - freeGb).coerceAtLeast(0f)
                    val usedPercent = ((usedGb / totalGb) * 100).toInt().coerceIn(0, 100)

                    val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction).let { kotlin.math.abs(it) }
                    val cardModifier = if (reduced) {
                        Modifier
                    } else {
                        Modifier.graphicsLayer {
                            val scale = 1f - (pageOffset * 0.05f).coerceIn(0f, 0.05f)
                            val alpha = 1f - (pageOffset * 0.35f).coerceIn(0f, 0.35f)
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                    }

                    InnerCard(
                        modifier = cardModifier.clickable {
                            val stId = com.ripple.filemanager.getStorageIdForLocation(
                                storage.location,
                                state.sdCardPath,
                                state.googleDriveAccountEmail,
                                state.megaAccountEmail
                            )
                            onAction(AppAction.SwitchToStorage(stId, storage.location))
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: 78dp Cookie Gauge with 24 dots
                            StorageDotsGauge(
                                usedPercent = usedPercent,
                                gaugeColor = storage.gaugeColor,
                                modifier = Modifier.size(78.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            // Right: Big thin free-space number + "GB free" pill, tappable storage name line, 8dp meter
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "%.1f".format(freeGb),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Light,
                                        fontFamily = LocalAppFont.current,
                                        color = colors.text
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(colors.sand)
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.storage_gb_free),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = LocalAppFont.current,
                                            color = colors.ink
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Tappable storage name line: "<storage name> · X of Y GB ▾"
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(onClick = onOpenStorageSwitcher)
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(
                                            R.string.used_of_storage,
                                            storage.name,
                                            "%.1f".format(usedGb),
                                            "%.1f".format(totalGb)
                                        ),
                                        fontSize = 12.sp,
                                        fontFamily = LocalAppFont.current,
                                        color = colors.muted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = stringResource(R.string.switch_storage),
                                        tint = colors.muted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // 8dp Progress meter
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(colors.insetCard)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction = (usedPercent / 100f).coerceIn(0.02f, 1f))
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(colors.accent)
                                    )
                                }
                            }

                            // If user has >=1 connected cloud storages, add space for chevron button
                            if (storages.size > 1) {
                                Spacer(modifier = Modifier.width(36.dp))
                            }
                        }
                    }
                }

                // If user has >=1 connected cloud storage, show subtle right chevron affordance on right edge
                if (storages.size > 1) {
                    CookieIconButton(
                        onClick = {
                            coroutineScope.launch {
                                val next = (pagerState.currentPage + 1) % storages.size
                                pagerState.animateScrollToPage(
                                    page = next,
                                    animationSpec = if (reduced) androidx.compose.animation.core.snap() else ExpressiveMotion.SpringSpec
                                )
                            }
                        },
                        icon = Icons.Default.ChevronRight,
                        bgColor = colors.card,
                        iconTint = colors.muted,
                        description = "Next storage",
                        size = 32.dp,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp)
                    )
                }
            }
        }

        // Page indicator dots centered below card (if > 1 storage)
        if (storages.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                storages.indices.forEach { index ->
                    val isActive = index == pagerState.currentPage
                    val dotWidth by animateDpAsState(
                        targetValue = if (isActive) 14.dp else 6.dp,
                        animationSpec = if (reduced) androidx.compose.animation.core.snap() else ExpressiveMotion.DpSpringSpec,
                        label = "dot_width"
                    )
                    val dotColor by androidx.compose.animation.animateColorAsState(
                        targetValue = if (isActive) colors.accent else colors.muted.copy(alpha = 0.35f),
                        animationSpec = if (reduced) androidx.compose.animation.core.snap() else ExpressiveMotion.ColorTween,
                        label = "dot_color"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(6.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }
        }
    }
}

/**
 * 78dp Cookie Gauge with 24 dots around perimeter.
 * Pop in sequentially on first load only (single session).
 */
@Composable
fun StorageDotsGauge(
    usedPercent: Int,
    modifier: Modifier = Modifier,
    gaugeColor: Color = ExpressiveTokens.PastelPeach
) {
    val colors = ExpressiveTheme.colors
    val totalDots = 24
    val usedDots = ((usedPercent / 100f) * totalDots).toInt().coerceIn(0, totalDots)
    val peachShape = rememberIconShape(lobes = 10, amplitude = 0.08f)
    val reduced = ExpressiveMotion.isReducedMotion()

    // Sequential pop-in animation on first load (single session)
    val animFraction = remember {
        Animatable(if (hasPlayedStorageGaugeAnimationInSession || reduced) 1f else 0f)
    }
    LaunchedEffect(Unit) {
        if (!hasPlayedStorageGaugeAnimationInSession && !reduced) {
            hasPlayedStorageGaugeAnimationInSession = true
            animFraction.animateTo(1f, animationSpec = tween(600))
        }
    }

    Box(
        modifier = modifier
            .clip(peachShape)
            .background(gaugeColor),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val currentVisibleDots = (totalDots * animFraction.value).toInt()

            for (i in 0 until totalDots) {
                if (i > currentVisibleDots) continue

                // Start from top (-90 degrees) and go clockwise
                val angle = (-Math.PI / 2 + (i.toDouble() / totalDots) * 2 * Math.PI).toFloat()
                val dotRadius = radius * 0.82f
                val dx = center.x + dotRadius * cos(angle)
                val dy = center.y + dotRadius * sin(angle)

                val isUsed = i < usedDots
                val dotColor = if (isUsed) {
                    ExpressiveTokens.DarkInk
                } else {
                    ExpressiveTokens.DarkInk.copy(alpha = 0.28f)
                }

                drawCircle(
                    color = dotColor,
                    radius = 2.2.dp.toPx(),
                    center = Offset(dx, dy)
                )
            }
        }

        // Used percent in the center
        Text(
            text = "$usedPercent%",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = ExpressiveTokens.DarkInk
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// CATEGORIES SECTION
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun ExpressiveCategoriesSection(
    state: AppState,
    onAction: (AppAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = remember {
        listOf(
            CategoryItem(R.string.cat_images, Icons.Outlined.Image, "images", "image"),
            CategoryItem(R.string.cat_videos, Icons.Outlined.VideoFile, "videos", "video"),
            CategoryItem(R.string.cat_audio, Icons.Outlined.AudioFile, "audio", "audio"),
            CategoryItem(R.string.cat_docs, Icons.Outlined.Description, "docs", "doc"),
            CategoryItem(R.string.cat_apps, Icons.Outlined.Apps, "apps", "apk"),
            CategoryItem(R.string.cat_downloads, Icons.Outlined.Download, "downloads", "download"),
            CategoryItem(R.string.cat_archives, Icons.Outlined.Archive, "archives", "archive"),
            CategoryItem(R.string.cat_large, Icons.Outlined.Storage, "large", "large")
        )
    }

    val cleaner = state.cleanerData
    val initialDownloadsCount = remember {
        try {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .listFiles()?.count { !it.name.startsWith(".") } ?: 0
        } catch (e: Exception) { 0 }
    }

    fun getCategorySubtitle(filterKey: String): String {
        if (cleaner == null) {
            return if (filterKey == "download") "$initialDownloadsCount items" else "--"
        }
        val formatted = when (filterKey) {
            "image" -> com.ripple.filemanager.ui.formatSize(cleaner.images.totalSizeBytes)
            "video" -> com.ripple.filemanager.ui.formatSize(cleaner.videos.totalSizeBytes)
            "audio" -> com.ripple.filemanager.ui.formatSize(cleaner.audio.totalSizeBytes)
            "doc" -> com.ripple.filemanager.ui.formatSize(cleaner.documents.totalSizeBytes)
            "apk" -> com.ripple.filemanager.ui.formatSize(cleaner.apps.totalSizeBytes)
            "download" -> "${cleaner.downloadsCount.takeIf { it > 0 } ?: initialDownloadsCount} items"
            "archive" -> com.ripple.filemanager.ui.formatSize(cleaner.archives.totalSizeBytes)
            "large" -> com.ripple.filemanager.ui.formatSize(if (cleaner.largeFilesSizeBytes > 0L) cleaner.largeFilesSizeBytes else cleaner.otherBytes)
            else -> ""
        }
        return formatted.replace(".00", "")
    }

    SectionContainer(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Row 1 (4 items)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.take(4).forEach { cat ->
                    CategoryCookieTile(
                        item = cat,
                        subtitle = getCategorySubtitle(cat.filterKey),
                        onClick = { handleCategoryClick(cat, onAction) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Row 2 (4 items)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.drop(4).take(4).forEach { cat ->
                    CategoryCookieTile(
                        item = cat,
                        subtitle = getCategorySubtitle(cat.filterKey),
                        onClick = { handleCategoryClick(cat, onAction) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private data class CategoryItem(
    @androidx.annotation.StringRes val titleRes: Int,
    val icon: ImageVector,
    val pastelKey: String,
    val filterKey: String
)

private fun handleCategoryClick(category: CategoryItem, onAction: (AppAction) -> Unit) {
    onAction(AppAction.SetLocation("category/${category.filterKey}"))
}

@Composable
private fun CategoryCookieTile(
    item: CategoryItem,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ExpressiveTheme.colors
    val pastel = ExpressiveTokens.categoryPastel(item.pastelKey)
    val roundness = LocalCornerRoundness.current
    val tileRadius = (22f * (roundness * 2).coerceIn(0f, 2f)).dp
    val tileShape = RoundedCornerShape(tileRadius)

    // Tinted card surface: category's accent pastel @ 6% in light mode, 8% in dark mode blended over neutral card color
    val isDark = colors.bg.luminance() < 0.5f
    val tintAlpha = if (isDark) 0.08f else 0.06f
    val tileBg = remember(pastel, colors.card, tintAlpha) {
        pastel.copy(alpha = tintAlpha).compositeOver(colors.card)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(tileShape)
            .background(tileBg)
            .border(1.dp, colors.line.copy(alpha = colors.lineAlpha * 0.4f), tileShape)
            .pressScale(0.95f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp, horizontal = 2.dp)
    ) {
        CookieIcon(
            icon = item.icon,
            bgColor = pastel,
            size = 46.dp,
            lobes = cookieLobesForName(item.pastelKey)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(item.titleRes),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = LocalAppFont.current,
            color = colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        if (subtitle.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = LocalAppFont.current,
                color = colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// RECENT FILE ROW
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun ExpressiveRecentFileRow(
    file: FileItem,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ExpressiveTheme.colors
    val ext = file.name.substringAfterLast('.', "").lowercase()
    val category = when (ext) {
        "jpg", "jpeg", "png", "gif", "webp" -> "images"
        "mp4", "mkv", "mov", "webm" -> "videos"
        "mp3", "flac", "wav", "m4a", "ogg" -> "audio"
        "pdf", "doc", "docx", "txt" -> "docs"
        "apk" -> "apps"
        "zip", "rar", "7z", "tar" -> "archives"
        else -> "large"
    }
    val isFolder = file.type == "folder"
    val pastel = if (isFolder) {
        com.ripple.filemanager.ui.getFolderAccent(file.name)
    } else {
        ExpressiveTokens.categoryPastel(category)
    }
    val icon = if (isFolder) {
        Icons.Outlined.Folder
    } else {
        when (category) {
            "images" -> Icons.Outlined.Image
            "videos" -> Icons.Outlined.VideoFile
            "audio" -> Icons.Outlined.AudioFile
            "docs" -> Icons.Outlined.Description
            "apps" -> Icons.Outlined.Apps
            "archives" -> Icons.Outlined.Archive
            else -> Icons.Outlined.InsertDriveFile
        }
    }

    val roundness = LocalCornerRoundness.current
    val rowShape = RoundedCornerShape((12f * (roundness * 2).coerceIn(0f, 2f)).dp)
    val reduced = ExpressiveMotion.isReducedMotion()
    val rowBg by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) colors.card.copy(alpha = 0.6f) else Color.Transparent,
        animationSpec = if (reduced) androidx.compose.animation.core.snap() else ExpressiveMotion.ColorTween,
        label = "recent_row_bg"
    )

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (file.isHidden) 0.55f else 1f)
            .clip(rowShape)
            .background(rowBg)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onSelect
            )
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // File thumbnail / icon (tap = toggle selection)
        Box(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSelect
                ),
            contentAlignment = Alignment.Center
        ) {
            ExpressiveFileThumbnail(
                file = file,
                isSelected = isSelected,
                size = 42.dp,
                lobes = cookieLobesForName(file.name)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name + "size · time"
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                color = colors.text,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            val metaStr = if (isFolder) {
                rememberFolderMeta(file)
            } else {
                formatFileMeta(file)
            }
            Text(
                text = metaStr,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                color = colors.muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 3-dot menu button
        IconButton(onClick = onMenuClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = colors.muted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// STORAGE SWITCHER BOTTOM SHEET
// ═══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageSwitcherBottomSheet(
    state: AppState,
    onDismissRequest: () -> Unit,
    onSelectStorage: (storageId: String, location: String) -> Unit,
    onAddConnection: () -> Unit,
    currentStorageId: String? = null,
    onLogoutStorage: ((ConnectedStorageItem) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState()
    val colors = ExpressiveTheme.colors
    var storageToDisconnect by remember { mutableStateOf<ConnectedStorageItem?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = colors.container,
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
    ) {
        ExpressiveSheetContent(title = stringResource(R.string.storage_locations)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val storages = remember(state) { getConnectedStorages(state) }
                storages.forEach { storage ->
                    val isSelected = currentStorageId != null && storage.id == currentStorageId
                    val isCloudOrNetwork = storage.id != "internal" && storage.id != "sd"
                    StorageSourceTile(
                        name = storage.name,
                        detail = storage.detail,
                        icon = storage.icon,
                        category = storage.category,
                        isSelected = isSelected,
                        onClick = { onSelectStorage(storage.id, storage.location) },
                        onDisconnectClick = if (isCloudOrNetwork && onLogoutStorage != null) {
                            { storageToDisconnect = storage }
                        } else null
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val cloudStorages = remember(storages) {
                    storages.filter { it.id != "internal" && it.id != "sd" }
                }
                val activeCloudStorage = remember(currentStorageId, cloudStorages) {
                    cloudStorages.find { it.id == currentStorageId } ?: cloudStorages.firstOrNull()
                }

                // Row with Add connection and Logout button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onAddConnection,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.ink
                        )
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.add_connection), fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }

                    if (onLogoutStorage != null && activeCloudStorage != null) {
                        val isCloud = activeCloudStorage.id in listOf("drive", "mega", "dropbox")
                        Button(
                            onClick = {
                                storageToDisconnect = activeCloudStorage
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.card,
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = if (isCloud) stringResource(R.string.logout_action) else stringResource(R.string.disconnect_action),
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isCloud) stringResource(R.string.logout_action) else stringResource(R.string.disconnect_action),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    val itemToDisconnect = storageToDisconnect
    if (itemToDisconnect != null) {
        val isCloud = itemToDisconnect.id in listOf("drive", "mega", "dropbox")
        ExpressiveConfirmationSheet(
            title = if (isCloud) stringResource(R.string.logout_prompt, itemToDisconnect.name) else stringResource(R.string.disconnect_prompt, itemToDisconnect.name),
            subtitle = "You will be disconnected from ${itemToDisconnect.detail}.",
            confirmLabel = if (isCloud) stringResource(R.string.logout_action) else stringResource(R.string.disconnect_action),
            cancelLabel = stringResource(R.string.cancel),
            onConfirm = {
                onLogoutStorage?.invoke(itemToDisconnect)
                storageToDisconnect = null
                onDismissRequest()
            },
            onDismissRequest = {
                storageToDisconnect = null
            }
        )
    }
}

@Composable
private fun StorageSourceTile(
    name: String,
    detail: String,
    icon: ImageVector,
    category: String,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onDisconnectClick: (() -> Unit)? = null
) {
    val colors = ExpressiveTheme.colors
    val pastel = ExpressiveTokens.categoryPastel(category)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) colors.card.copy(alpha = 0.85f) else colors.card)
            .then(
                if (isSelected) Modifier.border(1.5.dp, colors.accent, RoundedCornerShape(16.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CookieIcon(
            icon = icon,
            bgColor = pastel,
            size = 44.dp,
            lobes = 8
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = colors.text
            )
            Text(
                text = detail,
                fontSize = 12.sp,
                color = colors.muted
            )
        }
        if (onDisconnectClick != null) {
            IconButton(
                onClick = onDisconnectClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = "Disconnect",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Active",
                tint = colors.accent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
