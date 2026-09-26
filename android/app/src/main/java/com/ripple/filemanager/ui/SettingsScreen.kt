package com.ripple.filemanager.ui

import androidx.compose.ui.text.style.TextOverflow

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lens
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripple.filemanager.AppAction
import com.ripple.filemanager.AppState
import com.ripple.filemanager.ThemeMode
import com.ripple.filemanager.R
import com.ripple.filemanager.ui.expressive.*
import com.ripple.filemanager.ui.theme.hsl

@Composable
fun SettingsScreen(
    state: AppState,
    onAction: (AppAction) -> Unit
) {
    var expandedSection by remember { mutableStateOf<String?>("appearance") }
    val colors = ExpressiveTheme.colors
    val haptics = com.ripple.filemanager.haptics.rememberHapticsController { state.haptics }

    val hapticOnAction: (AppAction) -> Unit = remember(onAction, haptics) {
        { action ->
            if (action !is AppAction.SetShowSettingsScreen) {
                haptics.settingsToggle()
            }
            onAction(action)
        }
    }

    val currentPickingCategory = remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null && currentPickingCategory.value != null) {
            val path = uri.path
            if (path != null && path.startsWith("/tree/primary:")) {
                val relPath = path.removePrefix("/tree/primary:")
                val absolutePath = "/storage/emulated/0/$relPath"
                hapticOnAction(AppAction.SetOrganiserPath(currentPickingCategory.value!!, absolutePath))
            } else if (path != null && path == "/tree/primary") {
                val absolutePath = "/storage/emulated/0"
                hapticOnAction(AppAction.SetOrganiserPath(currentPickingCategory.value!!, absolutePath))
            } else {
                hapticOnAction(AppAction.ShowToast(context.getString(R.string.select_internal_storage_error)))
            }
        }
        currentPickingCategory.value = null
    }

    val nearbyFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val path = uri.path
            val absolutePath = if (path != null && path.startsWith("/tree/primary:")) {
                val relPath = path.removePrefix("/tree/primary:")
                "/storage/emulated/0/$relPath"
            } else if (path != null && path == "/tree/primary") {
                "/storage/emulated/0"
            } else {
                null
            }
            if (absolutePath != null) {
                hapticOnAction(AppAction.NearbyShareAction.SetReceivePath(absolutePath))
                hapticOnAction(AppAction.ShowToast("Received files folder updated"))
            } else {
                hapticOnAction(AppAction.ShowToast(context.getString(R.string.select_internal_storage_error)))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        // PageHeader "Settings"
        PageHeader(
            title = "Settings",
            onBack = { onAction(AppAction.SetShowSettingsScreen(false)) }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Card: Vibration Feedback with ExpressiveSwitch
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
                                icon = Icons.Outlined.Vibration,
                                bgColor = ExpressiveTokens.PastelSand,
                                size = 46.dp,
                                lobes = 8
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Vibration Feedback",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.text
                                )
                                Text(
                                    text = "Haptic feedback for touches",
                                    fontSize = 12.sp,
                                    color = colors.muted
                                )
                            }
                        }

                        ExpressiveSwitch(
                            checked = state.haptics.masterEnabled,
                            onCheckedChange = { hapticOnAction(AppAction.ToggleHapticsMaster(it)) }
                        )
                    }
                }
            }

            // Accordion 1: Appearance
            ExpressiveAccordionSection(
                title = "Appearance",
                subtitle = "Theme, colors, font & layout",
                icon = Icons.Outlined.Palette,
                pastel = ExpressiveTokens.PastelCoral,
                isExpanded = expandedSection == "appearance",
                onToggle = {
                    expandedSection = if (expandedSection == "appearance") null else "appearance"
                }
            ) {
                SettingsThemeContent(state, hapticOnAction)
            }

            // Accordion 2: Security
            ExpressiveAccordionSection(
                title = "Security",
                subtitle = "Password & biometric access",
                icon = Icons.Outlined.Security,
                pastel = ExpressiveTokens.PastelLilac,
                isExpanded = expandedSection == "security",
                onToggle = {
                    expandedSection = if (expandedSection == "security") null else "security"
                }
            ) {
                SettingsSecurityContent(state, hapticOnAction)
            }

            // Accordion 3: Viewers
            ExpressiveAccordionSection(
                title = "Viewers",
                subtitle = "File open preferences",
                icon = Icons.Outlined.Visibility,
                pastel = ExpressiveTokens.PastelSky,
                isExpanded = expandedSection == "viewers",
                onToggle = {
                    expandedSection = if (expandedSection == "viewers") null else "viewers"
                }
            ) {
                SettingsViewersContent(state, hapticOnAction)
            }

            // Accordion 4: File Organiser
            ExpressiveAccordionSection(
                title = "File organiser",
                subtitle = "Category destination folders",
                icon = Icons.Outlined.FolderSpecial,
                pastel = ExpressiveTokens.PastelSage,
                isExpanded = expandedSection == "organiser",
                onToggle = {
                    expandedSection = if (expandedSection == "organiser") null else "organiser"
                }
            ) {
                SettingsOrganiserContent(state, hapticOnAction, launcher, currentPickingCategory)
            }

            // Accordion 5: Nearby Share
            ExpressiveAccordionSection(
                title = "Nearby Share",
                subtitle = "Device name, save location & quick transfer",
                icon = Icons.Outlined.Share,
                pastel = ExpressiveTokens.PastelPeach,
                isExpanded = expandedSection == "nearby_share",
                onToggle = {
                    expandedSection = if (expandedSection == "nearby_share") null else "nearby_share"
                }
            ) {
                SettingsNearbyShareContent(state, hapticOnAction, nearbyFolderLauncher)
            }

            Spacer(modifier = Modifier.height(130.dp))
        }
    }
}

@Composable
private fun ExpressiveAccordionSection(
    title: String,
    subtitle: String,
    icon: ImageVector,
    pastel: Color,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val colors = ExpressiveTheme.colors
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(200),
        label = "accordion_rotation"
    )

    SectionContainer {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggle
                )
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                CookieIcon(
                    icon = icon,
                    bgColor = pastel,
                    size = 46.dp,
                    lobes = 8
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = colors.muted
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = colors.muted,
                modifier = Modifier.rotate(rotation)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(tween(250)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(250)) + fadeOut(tween(150))
        ) {
            InnerCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsThemeContent(state: AppState, onAction: (AppAction) -> Unit) {
    val colors = ExpressiveTheme.colors

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        // Theme mode (System / Light / Dark)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Theme mode", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.text)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple(ThemeMode.SYSTEM, "System", Icons.Outlined.BrightnessAuto),
                    Triple(ThemeMode.LIGHT, "Light", Icons.Outlined.LightMode),
                    Triple(ThemeMode.DARK, "Dark", Icons.Outlined.DarkMode)
                ).forEach { (mode, label, icon) ->
                    ExpressiveChip(
                        label = label,
                        icon = icon,
                        selected = state.themeMode == mode,
                        onClick = { onAction(AppAction.SetThemeMode(mode)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Dynamic system theme
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Dynamic system theme", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.text)
                Text("Match system wallpaper palette", fontSize = 12.sp, color = colors.muted)
            }
            ExpressiveSwitch(
                checked = state.useDynamicSystemTheme,
                onCheckedChange = { onAction(AppAction.SetDynamicSystemTheme(it)) }
            )
        }

        // Accent color (6 cookie swatches, dimmed at 40% and non-interactive while dynamic theme is on)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Accent color",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (state.useDynamicSystemTheme) colors.muted.copy(alpha = 0.4f) else colors.text
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val presets = listOf(14f, 262f, 178f, 44f, 340f, 104f)
                presets.forEach { hue ->
                    val color = hsl(hue, 65f, 66f)
                    val isActive = kotlin.math.abs(state.themeHue - hue) < 1f && !state.useDynamicSystemTheme
                    val cookieShape = remember { CookieShape(lobes = 8, amplitude = 0.08f) }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(cookieShape)
                            .background(if (state.useDynamicSystemTheme) color.copy(alpha = 0.35f) else color)
                            .clickable(enabled = !state.useDynamicSystemTheme) { onAction(AppAction.SetThemeHue(hue)) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isActive) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Invert text color
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Invert text color", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.text)
                Text("Text on accent surfaces", fontSize = 12.sp, color = colors.muted)
            }
            ExpressiveSwitch(
                checked = state.invertText,
                onCheckedChange = { onAction(AppAction.SetInvertText(it)) }
            )
        }

        // Font style chips: Outfit, System, Skyline Ledger, Roboto, Google Sans, Poppins
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Font style", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.text)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Outfit", "System", "Skyline Ledger", "Roboto", "Google Sans", "Poppins").forEach { style ->
                    ExpressiveChip(
                        label = style,
                        selected = state.fontStyle == style,
                        onClick = { onAction(AppAction.SetFontStyle(style)) }
                    )
                }
            }
        }

        // Corner roundness slider (square icon ← → circle icon)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Corner roundness", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.text)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CropSquare, contentDescription = null, tint = colors.muted, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                ExpressiveSlider(
                    value = state.cornerRoundness,
                    onValueChange = { onAction(AppAction.SetCornerRoundness(it)) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Lens, contentDescription = null, tint = colors.muted, modifier = Modifier.size(18.dp))
            }
        }

        // Default view mode (List vs Grid)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Default view mode", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.text)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExpressiveChip(
                    label = "List",
                    icon = Icons.Outlined.ViewList,
                    selected = state.isListMode,
                    onClick = { if (!state.isListMode) onAction(AppAction.SetListMode(true)) },
                    modifier = Modifier.weight(1f)
                )
                ExpressiveChip(
                    label = "Grid",
                    icon = Icons.Outlined.GridView,
                    selected = !state.isListMode,
                    onClick = { if (state.isListMode) onAction(AppAction.SetListMode(false)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Grid columns slider (value shown at left, 2–4)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Grid columns", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.text)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${state.gridColumns}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text,
                    modifier = Modifier.width(28.dp)
                )
                ExpressiveSlider(
                    value = state.gridColumns.toFloat(),
                    onValueChange = {
                        val rounded = kotlin.math.round(it).toInt().coerceIn(2, 4)
                        if (rounded != state.gridColumns) {
                            onAction(AppAction.SetGridColumns(rounded))
                        }
                    },
                    valueRange = 2f..4f,
                    steps = 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SettingsSecurityContent(state: AppState, onAction: (AppAction) -> Unit) {
    val colors = ExpressiveTheme.colors
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("sift_prefs", Context.MODE_PRIVATE) }
    var oldPassword by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var biometricEnabled by remember { mutableStateOf(prefs.getBoolean("lock_biometric_enabled", false)) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = oldPassword,
            onValueChange = { oldPassword = it },
            label = { Text("Old password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.insetCard,
                unfocusedContainerColor = colors.insetCard,
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.line.copy(alpha = colors.lineAlpha),
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text
            ),
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
        )

        OutlinedTextField(
            value = currentPassword,
            onValueChange = { currentPassword = it },
            label = { Text("New password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.insetCard,
                unfocusedContainerColor = colors.insetCard,
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.line.copy(alpha = colors.lineAlpha),
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text
            ),
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
        )

        if (state.securitySettingsErrorMessage != null) {
            Text(
                text = state.securitySettingsErrorMessage,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        Button(
            onClick = {
                if (currentPassword.isNotEmpty() && oldPassword.isNotEmpty()) {
                    onAction(AppAction.UpdateGlobalPassword(oldPassword, currentPassword))
                    oldPassword = ""
                    currentPassword = ""
                    onAction(AppAction.ShowToast("Password updated"))
                } else {
                    onAction(AppAction.ShowToast("Enter both passwords"))
                }
            },
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.ink
            ),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("UPDATE PASSWORD", fontWeight = FontWeight.Bold)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.line.copy(alpha = colors.lineAlpha))
        )

        // Biometrics switch row ("Enable fingerprint access")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Enable fingerprint access", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.text)
                Text("Use biometric unlock", fontSize = 12.sp, color = colors.muted)
            }
            ExpressiveSwitch(
                checked = biometricEnabled,
                onCheckedChange = {
                    biometricEnabled = it
                    prefs.edit().putBoolean("lock_biometric_enabled", it).apply()
                    onAction(AppAction.SetBiometricEnabled(it))
                }
            )
        }
    }
}

@Composable
private fun SettingsViewersContent(state: AppState, onAction: (AppAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ExpressiveViewerRow("Text / PDF", state.viewerTextPdf) {
            onAction(AppAction.SetViewerPreference("Text/PDF", it))
        }
        ExpressiveViewerRow("Music", state.viewerMusic) {
            onAction(AppAction.SetViewerPreference("Music", it))
        }
        ExpressiveViewerRow("Image", state.viewerImage) {
            onAction(AppAction.SetViewerPreference("Image", it))
        }
    }
}

@Composable
private fun ExpressiveViewerRow(label: String, currentValue: String, onValueChange: (String) -> Unit) {
    val colors = ExpressiveTheme.colors
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.text)

        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.insetCard)
                    .clickable { expanded = true }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(currentValue, fontSize = 13.sp, color = colors.text)
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = colors.muted, modifier = Modifier.size(16.dp))
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(colors.card)
            ) {
                DropdownMenuItem(
                    text = { Text("In-app", color = colors.text) },
                    onClick = { onValueChange("In-app"); expanded = false }
                )
                DropdownMenuItem(
                    text = { Text("External", color = colors.text) },
                    onClick = { onValueChange("External"); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun SettingsOrganiserContent(
    state: AppState,
    onAction: (AppAction) -> Unit,
    launcher: androidx.activity.result.ActivityResultLauncher<android.net.Uri?>,
    currentCategory: MutableState<String?>
) {
    val colors = ExpressiveTheme.colors
    val paths = listOf(
        "Docs" to ("Documents" to state.orgDestDocs),
        "Images" to ("Images" to state.orgDestImages),
        "Apks" to ("APKs" to state.orgDestApks),
        "Music" to ("Music" to state.orgDestMusic),
        "Videos" to ("Videos" to state.orgDestVideos)
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        paths.forEach { (cat, info) ->
            val (label, pathValue) = info

            val roundness = LocalCornerRoundness.current
            Card(
                shape = RoundedCornerShape((18f * (roundness * 2).coerceIn(0f, 2f)).dp),
                colors = CardDefaults.cardColors(containerColor = colors.insetCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = pathValue.ifEmpty { "Not set" },
                            fontSize = 13.sp,
                            color = colors.text,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = {
                            currentCategory.value = cat
                            launcher.launch(null)
                        }
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Edit path", tint = colors.muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsNearbyShareContent(
    state: AppState,
    onAction: (AppAction) -> Unit,
    folderLauncher: androidx.activity.result.ActivityResultLauncher<android.net.Uri?>
) {
    val colors = ExpressiveTheme.colors
    val context = LocalContext.current
    val roundness = LocalCornerRoundness.current
    val cardShape = RoundedCornerShape((18f * (roundness * 2).coerceIn(0f, 2f)).dp)

    var showNameDialog by remember { mutableStateOf(false) }
    var tempDeviceName by remember(state.nearbyDeviceName) { mutableStateOf(state.nearbyDeviceName) }

    val defaultPath = "${android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS).path}/RippleReceived"
    val displayPath = state.nearbyReceivePath.ifEmpty { defaultPath }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 1. Device Visible Name Card
        Card(
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = colors.insetCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        tempDeviceName = state.nearbyDeviceName.ifEmpty { "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}" }
                        showNameDialog = true
                    }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Device visible name",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = state.nearbyDeviceName.ifEmpty { "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Name visible to other nearby devices during discovery",
                        fontSize = 11.sp,
                        color = colors.muted
                    )
                }

                IconButton(
                    onClick = {
                        tempDeviceName = state.nearbyDeviceName.ifEmpty { "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}" }
                        showNameDialog = true
                    }
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit device name", tint = colors.accent)
                }
            }
        }

        // 2. Received Files Destination Card
        Card(
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = colors.insetCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                Text(
                    text = "Received files location",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = displayPath,
                    fontSize = 13.sp,
                    color = colors.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Incoming files will be saved in this folder",
                    fontSize = 11.sp,
                    color = colors.muted
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { folderLauncher.launch(null) },
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(19.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.ink
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Change folder", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    if (state.nearbyReceivePath.isNotEmpty() && state.nearbyReceivePath != defaultPath) {
                        OutlinedButton(
                            onClick = {
                                onAction(AppAction.NearbyShareAction.SetReceivePath(defaultPath))
                                onAction(AppAction.ShowToast("Reset to default download folder"))
                            },
                            modifier = Modifier.height(38.dp),
                            shape = RoundedCornerShape(19.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Reset", fontSize = 12.sp, color = colors.muted)
                        }
                    }
                }
            }
        }

        // 3. Ask before receiving (Confirmation popup switch)
        Card(
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = colors.insetCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ask before receiving",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.text
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (state.nearbyAskBeforeReceiving)
                            "Shows a confirmation popup to accept or decline incoming files"
                        else
                            "Directly accepts and saves files without asking",
                        fontSize = 11.sp,
                        color = colors.muted
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                ExpressiveSwitch(
                    checked = state.nearbyAskBeforeReceiving,
                    onCheckedChange = { onAction(AppAction.NearbyShareAction.SetAskBeforeReceiving(it)) }
                )
            }
        }
    }

    // Dialog to change device visible name
    if (showNameDialog) {
        GradientAlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = {
                Text(
                    text = "Change Device Name",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter the name other devices will see when discovering this device.",
                        fontSize = 12.sp,
                        color = colors.muted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempDeviceName,
                        onValueChange = { tempDeviceName = it },
                        singleLine = true,
                        placeholder = { Text("Device name", color = colors.muted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.insetCard,
                            unfocusedContainerColor = colors.insetCard,
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.line.copy(alpha = colors.lineAlpha),
                            focusedTextColor = colors.text,
                            unfocusedTextColor = colors.text
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = tempDeviceName.trim()
                        if (trimmed.isNotEmpty()) {
                            onAction(AppAction.NearbyShareAction.SetDeviceName(trimmed))
                            onAction(AppAction.ShowToast("Device name updated to $trimmed"))
                            showNameDialog = false
                        } else {
                            onAction(AppAction.ShowToast("Device name cannot be empty"))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.ink
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Cancel", color = colors.muted)
                }
            }
        )
    }
}
