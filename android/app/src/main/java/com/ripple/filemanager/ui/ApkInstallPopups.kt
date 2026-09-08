package com.ripple.filemanager.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripple.filemanager.ui.theme.SkylineColors
import com.ripple.filemanager.ui.theme.SiftTheme

data class ApkVersionInfo(
    val packageName: String,
    val versionCode: Long,
    val versionName: String
)

enum class LockReason(val message: String) {
    NOT_INSTALLED("App not installed"),
    NOT_A_DOWNGRADE("Version mismatch — cannot downgrade"),
    REQUIRES_SHIZUKU("Shizuku not connected — tap to set up"),
    NONE("")
}

data class ModeEvaluation(
    val visible: Boolean = true,
    val enabled: Boolean = true,
    val lockReason: LockReason = LockReason.NONE
)

data class InstallModesResult(
    val normal: ModeEvaluation,
    val downgrade: ModeEvaluation,
    val silent: ModeEvaluation
)

fun evaluateInstallModes(
    selectedApk: ApkVersionInfo,
    installedApp: ApkVersionInfo?,
    shizukuConnected: Boolean
): InstallModesResult {
    val normal = ModeEvaluation(visible = true, enabled = true, lockReason = LockReason.NONE)
    
    val downgradeVisible = installedApp != null
    val isDowngradeCandidate = installedApp != null && selectedApk.versionCode < installedApp.versionCode
    
    val downgradeEnabled = isDowngradeCandidate && shizukuConnected
    val downgradeLockReason = when {
        !downgradeVisible -> LockReason.NOT_INSTALLED
        !isDowngradeCandidate -> LockReason.NOT_A_DOWNGRADE
        !shizukuConnected -> LockReason.REQUIRES_SHIZUKU
        else -> LockReason.NONE
    }
    
    val downgrade = ModeEvaluation(
        visible = downgradeVisible,
        enabled = downgradeEnabled,
        lockReason = downgradeLockReason
    )
    
    val silent = ModeEvaluation(
        visible = true,
        enabled = shizukuConnected,
        lockReason = if (shizukuConnected) LockReason.NONE else LockReason.REQUIRES_SHIZUKU
    )
    
    return InstallModesResult(normal, downgrade, silent)
}

@Composable
fun SingleApkInstallPopup(
    appName: String,
    versionText: String,
    modes: InstallModesResult,
    onNormalInstall: () -> Unit,
    onDowngradeInstall: () -> Unit,
    onSilentInstall: () -> Unit,
    onFooterClick: () -> Unit = {},
    modifier: Modifier = Modifier
,
    cornerRoundness: Float
) {
    val showFooter = (!modes.silent.enabled && modes.silent.lockReason == LockReason.REQUIRES_SHIZUKU) ||
                     (modes.downgrade.visible && !modes.downgrade.enabled)

    val footerMessage = if (modes.silent.lockReason == LockReason.REQUIRES_SHIZUKU || modes.downgrade.lockReason == LockReason.REQUIRES_SHIZUKU) {
        LockReason.REQUIRES_SHIZUKU.message
    } else if (modes.downgrade.visible && !modes.downgrade.enabled) {
        modes.downgrade.lockReason.message
    } else {
        ""
    }

    val shape = com.ripple.filemanager.ui.getDynamicCornerShape(16f, cornerRoundness)

    Column(
        modifier = modifier
            .width(280.dp)
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp), shape)
            .border(1.dp, SkylineColors.Border, shape)
            .clip(shape)
    ) {
        Text(
            text = "$appName · $versionText",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 8.dp)
        )

        HorizontalDivider(color = SkylineColors.Border, thickness = 1.dp)

        InstallRow(cornerRoundness = cornerRoundness,
            icon = Icons.Default.ArrowDownward,
            title = "Normal Install",
            subtitle = "Uses system package installer",
            enabled = modes.normal.enabled,
            onClick = { if (modes.normal.enabled) onNormalInstall() }
        )

        if (modes.downgrade.visible) {
            HorizontalDivider(color = SkylineColors.Border, thickness = 1.dp)
            InstallRow(cornerRoundness = cornerRoundness,
                icon = Icons.Default.KeyboardDoubleArrowDown,
                title = "Downgrade",
                subtitle = "Force install older version",
                enabled = modes.downgrade.enabled,
                onClick = { if (modes.downgrade.enabled) onDowngradeInstall() }
            )
        }

        if (modes.silent.visible) {
            HorizontalDivider(color = SkylineColors.Border, thickness = 1.dp)
            InstallRow(cornerRoundness = cornerRoundness,
                icon = Icons.Default.Bolt,
                title = "Silent Install",
                subtitle = "Background install without prompts",
                enabled = modes.silent.enabled,
                onClick = { if (modes.silent.enabled) onSilentInstall() }
            )
        }

        if (showFooter && footerMessage.isNotEmpty()) {
            HorizontalDivider(color = SkylineColors.Border, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onFooterClick)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.size(8.dp).background(Color(0xFFE57373), CircleShape))
                Text(
                    text = footerMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE57373)
                )
            }
        }
    }
}

@Composable
fun InstallRow(cornerRoundness: Float, 
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val purpleColor = Color(0xFF9C27B0)
    val alpha = if (enabled) 1f else 0.4f
    val iconBgColor = if (enabled) purpleColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val iconColor = if (enabled) purpleColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconBgColor, com.ripple.filemanager.ui.getDynamicCornerShape(8f, cornerRoundness)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
            )
        }

        if (trailingContent != null) {
            trailingContent()
        } else if (!enabled) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}


data class ParsedApkItem(
    val file: com.ripple.filemanager.FileItem,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val isDowngrade: Boolean,
    val installedVersionName: String?,
    val installedVersionCode: Long?
)

data class BatchApkItem(
    val name: String,
    val initial: String,
    val versionTransition: String,
    val note: String? = null,
    val isNoteWarning: Boolean = false,
    val overrideMode: String? = null
)

@Composable
fun BatchCountPill(text: String, isCoral: Boolean, showLock: Boolean = false, cornerRoundness: Float = 1.0f) {
    val bgColor = if (isCoral) Color(0xFFE57373).copy(alpha = 0.15f) else Color(0xFF9C27B0).copy(alpha = 0.15f)
    val textColor = if (isCoral) Color(0xFFE57373) else Color(0xFF9C27B0)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.background(bgColor, com.ripple.filemanager.ui.getDynamicCornerShape(12f, cornerRoundness)).padding(horizontal = 8.dp, vertical = 2.dp)) {
            Text(text = text, color = textColor, style = MaterialTheme.typography.labelSmall)
        }
        if (showLock) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun BatchApkInstallPopup(
    selectedCount: Int,
    normalPillText: String, normalPillIsCoral: Boolean,
    downgradePillText: String, downgradePillIsCoral: Boolean,
    silentPillText: String, silentPillIsCoral: Boolean,
    isShizukuConnected: Boolean,
    onNormalClick: () -> Unit,
    onDowngradeClick: () -> Unit,
    onSilentClick: () -> Unit,
    modifier: Modifier = Modifier
,
    cornerRoundness: Float
) {
    val normalEnabled = true
    val downgradeEnabled = isShizukuConnected
    val silentEnabled = isShizukuConnected
    val showFooter = !isShizukuConnected

    val shape = com.ripple.filemanager.ui.getDynamicCornerShape(16f, cornerRoundness)
    Column(
        modifier = modifier
            .width(280.dp)
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp), shape)
            .border(1.dp, SkylineColors.Border, shape)
            .clip(shape)
    ) {
        Text(
            text = "INSTALL METHOD · $selectedCount APKS SELECTED",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 8.dp)
        )
        HorizontalDivider(color = SkylineColors.Border, thickness = 1.dp)

        InstallRow(cornerRoundness = cornerRoundness,
            icon = Icons.Default.ArrowDownward, title = "Normal Install", subtitle = "Uses system package installer",
            enabled = normalEnabled, onClick = onNormalClick,
            trailingContent = { BatchCountPill(normalPillText, normalPillIsCoral, !normalEnabled, cornerRoundness) }
        )
        HorizontalDivider(color = SkylineColors.Border, thickness = 1.dp)
        InstallRow(cornerRoundness = cornerRoundness,
            icon = Icons.Default.KeyboardDoubleArrowDown, title = "Downgrade", subtitle = "Force install older version",
            enabled = downgradeEnabled, onClick = onDowngradeClick,
            trailingContent = { BatchCountPill(downgradePillText, downgradePillIsCoral, !downgradeEnabled, cornerRoundness) }
        )
        HorizontalDivider(color = SkylineColors.Border, thickness = 1.dp)
        InstallRow(cornerRoundness = cornerRoundness,
            icon = Icons.Default.Bolt, title = "Silent Install", subtitle = "Background install without prompts",
            enabled = silentEnabled, onClick = onSilentClick,
            trailingContent = { BatchCountPill(silentPillText, silentPillIsCoral, !silentEnabled, cornerRoundness) }
        )

        if (showFooter) {
            HorizontalDivider(color = SkylineColors.Border, thickness = 1.dp)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.size(8.dp).background(Color(0xFFE57373), CircleShape))
                Text(text = "Shizuku not connected — tap to set up", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE57373))
            }
        }
    }
}

enum class InstallMode { NORMAL, DOWNGRADE, SILENT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchInstallSheet(
    fileCount: Int,
    selectedMode: InstallMode,
    apks: List<BatchApkItem>,
    isShizukuConnected: Boolean,
    onRemove: (BatchApkItem) -> Unit,
    onCancel: () -> Unit,
    onInstall: () -> Unit,
    modifier: Modifier = Modifier
,
    cornerRoundness: Float
) {
    val modeName = when(selectedMode) {
        InstallMode.NORMAL -> "Normal"
        InstallMode.DOWNGRADE -> "Downgrade"
        InstallMode.SILENT -> "Silent"
    }
    
    ModalBottomSheet(
        onDismissRequest = onCancel,
        modifier = modifier,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = com.ripple.filemanager.ui.getDynamicCornerShape(16f, cornerRoundness)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "$modeName Install", style = MaterialTheme.typography.titleLarge)
                Text(text = "$fileCount files", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(24.dp))
            
            // List box
            Box(modifier = Modifier.fillMaxWidth().weight(1f, fill = false).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), com.ripple.filemanager.ui.getDynamicCornerShape(12f, cornerRoundness)).border(1.dp, SkylineColors.Border, com.ripple.filemanager.ui.getDynamicCornerShape(12f, cornerRoundness))) {
                androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    items(apks) { apk ->
                        BatchApkRow(apk = apk, onRemove = { onRemove(apk) }, cornerRoundness = cornerRoundness)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isShizukuConnected) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF81C784), CircleShape))
                    Text(text = "Shizuku connected", style = MaterialTheme.typography.labelSmall, color = Color(0xFF81C784))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onCancel, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface), border = androidx.compose.foundation.BorderStroke(1.dp, SkylineColors.Border)) {
                    Text("Cancel")
                }
                Button(onClick = onInstall, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = SkylineColors.Amber, contentColor = Color(0xFF161009))) {
                    Text("Install $fileCount")
                }
            }
        }
    }
}

@Composable
fun BatchApkRow(apk: BatchApkItem, onRemove: () -> Unit, cornerRoundness: Float) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.size(32.dp).background(SkylineColors.Amber.copy(alpha = 0.2f), com.ripple.filemanager.ui.getDynamicCornerShape(6f, cornerRoundness)), contentAlignment = Alignment.Center) {
            Text(text = apk.initial, style = MaterialTheme.typography.labelMedium, color = SkylineColors.Amber)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = apk.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(text = apk.versionTransition, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (apk.note != null) {
                Spacer(modifier = Modifier.height(4.dp))
                val noteBg = if (apk.isNoteWarning) Color(0xFFE57373).copy(alpha = 0.15f) else Color(0xFF81C784).copy(alpha = 0.15f)
                val noteFg = if (apk.isNoteWarning) Color(0xFFE57373) else Color(0xFF81C784)
                Box(modifier = Modifier.background(noteBg, com.ripple.filemanager.ui.getDynamicCornerShape(8f, cornerRoundness)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(text = apk.note, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = noteFg)
                }
            }
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun ApkInstallPopupPreviews() {
    SiftTheme(darkTheme = true) {
        Column(
            modifier = Modifier.padding(24.dp).background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // State 1: All three rows enabled
            val modes1 = evaluateInstallModes(
                selectedApk = ApkVersionInfo("com.teslacoilsw.launcher", 810, "8.1"),
                installedApp = ApkVersionInfo("com.teslacoilsw.launcher", 820, "8.2"),
                shizukuConnected = true
            )
            SingleApkInstallPopup("Nova Launcher", "8.2 -> 8.1", modes1, {}, {}, {}, cornerRoundness = 1.0f)

            // State 2: Downgrade locked + Silent locked
            val modes2 = evaluateInstallModes(
                selectedApk = ApkVersionInfo("com.teslacoilsw.launcher", 810, "8.1"),
                installedApp = ApkVersionInfo("com.teslacoilsw.launcher", 820, "8.2"),
                shizukuConnected = false
            )
            SingleApkInstallPopup("Nova Launcher", "8.2 -> 8.1", modes2, {}, {}, {}, cornerRoundness = 1.0f)

            // State 3: Downgrade locked only (Not lower version)
            val modes3 = evaluateInstallModes(
                selectedApk = ApkVersionInfo("com.teslacoilsw.launcher", 820, "8.2"),
                installedApp = ApkVersionInfo("com.teslacoilsw.launcher", 810, "8.1"),
                shizukuConnected = true
            )
            SingleApkInstallPopup("Nova Launcher", "8.1 -> 8.2", modes3, {}, {}, {}, cornerRoundness = 1.0f)

            // State 4: Silent locked only (Not installed)
            val modes4 = evaluateInstallModes(
                selectedApk = ApkVersionInfo("com.teslacoilsw.launcher", 820, "8.2"),
                installedApp = null,
                shizukuConnected = false
            )
            SingleApkInstallPopup("Nova Launcher", "8.2", modes4, {}, {}, {}, cornerRoundness = 1.0f)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun BatchInstallPreviews() {
    SiftTheme(darkTheme = true) {
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BatchApkInstallPopup(
                selectedCount = 2,
                normalPillText = "2 apps", normalPillIsCoral = false,
                downgradePillText = "0 apps", downgradePillIsCoral = true,
                silentPillText = "2 apps", silentPillIsCoral = false,
                isShizukuConnected = false,
                onNormalClick = {}, onDowngradeClick = {}, onSilentClick = {}, cornerRoundness = 1.0f
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun BatchInstallSheetPreview1() {
    SiftTheme(darkTheme = true) {
        val apks = listOf(
            BatchApkItem("Nova Launcher", "N", "v1.0 -> v1.1"),
            BatchApkItem("Spotify", "S", "Not currently installed")
        )
        BatchInstallSheet(
            fileCount = 2, selectedMode = InstallMode.NORMAL, apks = apks,
            isShizukuConnected = true, onRemove = {}, onCancel = {}, onInstall = {}, cornerRoundness = 1.0f
        )
    }
}
