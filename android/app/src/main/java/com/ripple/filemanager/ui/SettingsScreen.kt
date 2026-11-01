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
import com.ripple.filemanager.DarkSurfaceStyle
import com.ripple.filemanager.IconShapeType
import com.ripple.filemanager.R
import com.ripple.filemanager.ui.expressive.*
import com.ripple.filemanager.ui.theme.LocalAppFont
import com.ripple.filemanager.ui.theme.hsl
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.ripple.filemanager.data.AppLanguage
import com.ripple.filemanager.data.AppLanguages
import android.content.Intent
import androidx.compose.material.icons.filled.Clear
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.ripple.filemanager.data.LauncherPreset

@Composable
fun SettingsScreen(
    state: AppState,
    onAction: (AppAction) -> Unit
) {
    var expandedSection by rememberSaveable { mutableStateOf<String?>("appearance") }
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
            title = stringResource(R.string.settings_title),
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
                                    text = stringResource(R.string.settings_vibration),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.text
                                )
                                Text(
                                    text = stringResource(R.string.settings_vibration_desc),
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
                title = stringResource(R.string.settings_appearance),
                subtitle = stringResource(R.string.settings_appearance_desc),
                icon = Icons.Outlined.Palette,
                pastel = ExpressiveTokens.PastelCoral,
                isExpanded = expandedSection == "appearance",
                onToggle = {
                    expandedSection = if (expandedSection == "appearance") null else "appearance"
                }
            ) {
                SettingsThemeContent(state, hapticOnAction)
            }

            // Accordion 2: Language
            val currentLangTag = state.currentLanguage
            val defaultLangLabel = stringResource(R.string.settings_language_default)
            val languageSubtitle = remember(currentLangTag, defaultLangLabel) {
                if (currentLangTag == "device_default" || currentLangTag.isBlank()) {
                    defaultLangLabel
                } else {
                    val active = AppLanguages.SUPPORTED_LANGUAGES.find { lang ->
                        lang.tag.equals(currentLangTag, ignoreCase = true)
                    }
                    active?.nativeName ?: currentLangTag
                }
            }

            ExpressiveAccordionSection(
                title = stringResource(R.string.settings_language),
                subtitle = languageSubtitle,
                icon = Icons.Outlined.Translate,
                pastel = ExpressiveTokens.PastelTeal,
                isExpanded = expandedSection == "language",
                onToggle = {
                    expandedSection = if (expandedSection == "language") null else "language"
                }
            ) {
                SettingsLanguageContent(
                    currentLanguage = state.currentLanguage,
                    onSelectLanguage = { tag -> onAction(AppAction.SetLanguage(tag)) },
                    onHaptic = { haptics.settingsToggle() }
                )
            }

            // Accordion 3: Security
            ExpressiveAccordionSection(
                title = stringResource(R.string.settings_security),
                subtitle = stringResource(R.string.settings_security_desc),
                icon = Icons.Outlined.Security,
                pastel = ExpressiveTokens.PastelLilac,
                isExpanded = expandedSection == "security",
                onToggle = {
                    expandedSection = if (expandedSection == "security") null else "security"
                }
            ) {
                SettingsSecurityContent(state, hapticOnAction)
            }

            // Accordion 4: Viewers
            ExpressiveAccordionSection(
                title = stringResource(R.string.settings_viewers),
                subtitle = stringResource(R.string.settings_viewers_desc),
                icon = Icons.Outlined.Visibility,
                pastel = ExpressiveTokens.PastelSky,
                isExpanded = expandedSection == "viewers",
                onToggle = {
                    expandedSection = if (expandedSection == "viewers") null else "viewers"
                }
            ) {
                SettingsViewersContent(state, hapticOnAction)
            }

            // Accordion 5: File Organiser
            ExpressiveAccordionSection(
                title = stringResource(R.string.settings_organizer),
                subtitle = stringResource(R.string.settings_organizer_desc),
                icon = Icons.Outlined.FolderSpecial,
                pastel = ExpressiveTokens.PastelSage,
                isExpanded = expandedSection == "organiser",
                onToggle = {
                    expandedSection = if (expandedSection == "organiser") null else "organiser"
                }
            ) {
                SettingsOrganiserContent(state, hapticOnAction, launcher, currentPickingCategory)
            }

            // Accordion 6: Nearby Share
            ExpressiveAccordionSection(
                title = stringResource(R.string.settings_nearby_share),
                subtitle = stringResource(R.string.settings_nearby_desc),
                icon = Icons.Outlined.Share,
                pastel = ExpressiveTokens.PastelPeach,
                isExpanded = expandedSection == "nearby_share",
                onToggle = {
                    expandedSection = if (expandedSection == "nearby_share") null else "nearby_share"
                }
            ) {
                SettingsNearbyShareContent(state, hapticOnAction, nearbyFolderLauncher)
            }

            // Accordion 7: Music Player
            ExpressiveAccordionSection(
                title = stringResource(R.string.settings_music_player),
                subtitle = stringResource(R.string.settings_music_player_desc),
                icon = Icons.Outlined.MusicNote,
                pastel = ExpressiveTokens.categoryPastel("audio"),
                isExpanded = expandedSection == "music_player",
                onToggle = {
                    expandedSection = if (expandedSection == "music_player") null else "music_player"
                }
            ) {
                SettingsMusicPlayerContent(state, hapticOnAction)
            }

            Spacer(modifier = Modifier.height(130.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsThemeContent(state: AppState, onAction: (AppAction) -> Unit) {
    val colors = ExpressiveTheme.colors
    val context = LocalContext.current
    var showLauncherPresetSheet by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        // App Name & Launcher Branding
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.settings_app_name),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.text
            )

            // Freeform text field for in-app display name
            OutlinedTextField(
                value = state.customAppName,
                onValueChange = { onAction(AppAction.SetCustomAppName(it)) },
                placeholder = { Text("Ripple Files", color = colors.muted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                trailingIcon = if (state.customAppName.isNotEmpty() && state.customAppName != "Ripple Files") {
                    {
                        IconButton(onClick = { onAction(AppAction.SetCustomAppName("Ripple Files")) }) {
                            Icon(Icons.Default.Clear, contentDescription = "Reset", tint = colors.muted, modifier = Modifier.size(18.dp))
                        }
                    }
                } else null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.insetCard,
                    unfocusedContainerColor = colors.insetCard,
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.line.copy(alpha = colors.lineAlpha),
                    focusedTextColor = colors.text,
                    unfocusedTextColor = colors.text
                )
            )

            // Pin to home screen button / row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.insetCard)
                    .clickable {
                        val currentName = state.customAppName.ifBlank { "Ripple Files" }
                        if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                            val shortcut = ShortcutInfoCompat.Builder(context, "custom_name_shortcut")
                                .setShortLabel(currentName)
                                .setLongLabel(currentName)
                                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                                .setIntent(Intent(context, com.ripple.filemanager.MainActivity::class.java).setAction(Intent.ACTION_MAIN))
                                .build()
                            ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
                        } else {
                            val msg = context.getString(R.string.pin_shortcut_not_supported)
                            onAction(AppAction.ShowToast(msg))
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_pin_shortcut),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.text
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.settings_pin_shortcut_desc),
                        fontSize = 12.sp,
                        color = colors.muted
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                CookieIcon(
                    icon = Icons.Outlined.PushPin,
                    bgColor = colors.accent,
                    size = 36.dp,
                    lobes = 8
                )
            }

            // Launcher icon label preset row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.insetCard)
                    .clickable {
                        showLauncherPresetSheet = true
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_launcher_icon_label),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.text
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(state.launcherPreset.labelRes),
                        fontSize = 12.sp,
                        color = colors.muted
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Select launcher icon label",
                    tint = colors.muted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.line.copy(alpha = colors.lineAlpha))
        )

        // Theme mode (System / Light / Dark)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.theme_mode_label), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.text)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple(ThemeMode.SYSTEM, stringResource(R.string.theme_system), Icons.Outlined.BrightnessAuto),
                    Triple(ThemeMode.LIGHT, stringResource(R.string.theme_light), Icons.Outlined.LightMode),
                    Triple(ThemeMode.DARK, stringResource(R.string.theme_dark), Icons.Outlined.DarkMode)
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

        // AMOLED black
        val isDarkThemeActive = when (state.themeMode) {
            ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.amoled_black),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDarkThemeActive) colors.text else colors.text.copy(alpha = 0.45f)
                )
                Text(
                    text = if (isDarkThemeActive) {
                        stringResource(R.string.amoled_black_subtitle)
                    } else {
                        stringResource(R.string.amoled_black_disabled_subtitle)
                    },
                    fontSize = 12.sp,
                    color = colors.muted.copy(alpha = if (isDarkThemeActive) 1f else 0.45f)
                )
            }
            ExpressiveSwitch(
                checked = state.darkSurfaceStyle == DarkSurfaceStyle.AMOLED,
                onCheckedChange = { 
                    onAction(AppAction.SetDarkSurfaceStyle(if (it) DarkSurfaceStyle.AMOLED else DarkSurfaceStyle.STANDARD)) 
                },
                enabled = isDarkThemeActive
            )
        }

        // Dynamic system theme
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.dynamic_color_label), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.text)
                Text(stringResource(R.string.use_system_theme_color), fontSize = 12.sp, color = colors.muted)
            }
            ExpressiveSwitch(
                checked = state.useDynamicSystemTheme,
                onCheckedChange = { onAction(AppAction.SetDynamicSystemTheme(it)) }
            )
        }

        if (state.useDynamicSystemTheme) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.card)
                    .border(1.dp, colors.line.copy(alpha = colors.lineAlpha), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(colors.accent)
                )
                Text(
                    text = "Active wallpaper accent applied",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.text
                )
            }
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
                    val cookieShape = rememberIconShape(lobes = 8, amplitude = 0.08f)

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

        // Icon shape
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Icon shape",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.text
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconShapeType.selectableShapes.forEach { shapeType ->
                    val isSelected = state.iconShapeSetting == shapeType
                    val previewShape = IconShapes.forType(shapeType)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onAction(AppAction.SetIconShape(shapeType))
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(previewShape)
                                .background(colors.accent)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.dp, colors.text, previewShape)
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = colors.ink,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else if (shapeType == IconShapeType.AUTO) {
                                Icon(
                                    Icons.Outlined.Casino,
                                    contentDescription = "Auto",
                                    tint = colors.ink.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = shapeType.label,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontFamily = LocalAppFont.current,
                            color = if (isSelected) colors.text else colors.muted,
                            maxLines = 1
                        )
                    }
                }
            }
            if (state.iconShapeSetting == IconShapeType.AUTO) {
                Text(
                    text = "Changes each time you open the app",
                    fontSize = 12.sp,
                    fontFamily = LocalAppFont.current,
                    color = colors.muted
                )
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

        // Show hidden files
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_hidden_files), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.text)
                Text(stringResource(R.string.settings_hidden_files_desc), fontSize = 12.sp, color = colors.muted)
            }
            ExpressiveSwitch(
                checked = state.showHiddenFiles,
                onCheckedChange = { onAction(AppAction.SetShowHiddenFiles(it)) }
            )
        }
    }

    if (showLauncherPresetSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLauncherPresetSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = null,
            containerColor = colors.container,
            shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
        ) {
            ExpressiveSheetContent(title = stringResource(R.string.settings_launcher_icon_label)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LauncherPreset.values().forEach { preset ->
                        val isSelected = state.launcherPreset == preset
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) colors.card.copy(alpha = 0.85f) else Color.Transparent)
                                .clickable {
                                    if (!isSelected) {
                                        onAction(AppAction.SetLauncherPreset(preset))
                                        val toastMsg = context.getString(R.string.launcher_icon_updated_toast)
                                        onAction(AppAction.ShowToast(toastMsg))
                                        Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                    }
                                    showLauncherPresetSheet = false
                                }
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(preset.labelRes),
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                fontFamily = LocalAppFont.current,
                                color = colors.text
                            )
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected) colors.accent else colors.muted.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(colors.accent)
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
            label = { Text(stringResource(R.string.old_password_label)) },
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
            label = { Text(stringResource(R.string.set_new_password_label)) },
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
                Text(stringResource(R.string.enable_fingerprint_access), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.text)
                Text(stringResource(R.string.settings_security_desc), fontSize = 12.sp, color = colors.muted)
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.line.copy(alpha = colors.lineAlpha))
        )

        // Always show Vault switch row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Always show Vault", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.text)
                Text("Show Vault in navigation bar and drawer. When hidden, long press the '+' button to reveal it temporarily.", fontSize = 12.sp, color = colors.muted)
            }
            ExpressiveSwitch(
                checked = state.isVaultAlwaysShown,
                onCheckedChange = {
                    onAction(AppAction.SetVaultAlwaysShown(it))
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

@Composable
private fun SettingsLanguageContent(
    currentLanguage: String,
    onSelectLanguage: (String) -> Unit,
    onHaptic: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        val isDefaultSelected = currentLanguage == "device_default" || currentLanguage.isBlank()
        LanguageOptionRow(
            nativeName = stringResource(R.string.settings_language_default),
            englishGloss = null,
            isSelected = isDefaultSelected,
            onClick = {
                if (!isDefaultSelected) {
                    onHaptic()
                    onSelectLanguage("device_default")
                }
            }
        )

        AppLanguages.SUPPORTED_LANGUAGES.forEach { lang ->
            val isSelected = currentLanguage.equals(lang.tag, ignoreCase = true)

            LanguageOptionRow(
                nativeName = lang.nativeName,
                englishGloss = lang.englishGloss,
                isSelected = isSelected,
                onClick = {
                    if (!isSelected) {
                        onHaptic()
                        onSelectLanguage(lang.tag)
                    }
                }
            )
        }
    }
}

@Composable
private fun LanguageOptionRow(
    nativeName: String,
    englishGloss: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = ExpressiveTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) colors.card.copy(alpha = 0.85f) else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nativeName,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                fontFamily = LocalAppFont.current,
                color = colors.text
            )
            if (englishGloss != null) {
                Text(
                    text = englishGloss,
                    fontSize = 12.sp,
                    fontFamily = LocalAppFont.current,
                    color = colors.muted
                )
            }
        }

        // Radio-style filled / outlined circle indicator
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(
                    width = 2.dp,
                    color = if (isSelected) colors.accent else colors.muted.copy(alpha = 0.5f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(colors.accent)
                )
            }
        }
    }
}

@Composable
private fun SettingsMusicPlayerContent(
    state: AppState,
    onAction: (AppAction) -> Unit
) {
    val colors = ExpressiveTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.settings_mini_player_auto_close),
                fontFamily = LocalAppFont.current,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = colors.text
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.settings_mini_player_auto_close_desc),
                fontFamily = LocalAppFont.current,
                fontSize = 12.sp,
                color = colors.muted
            )
        }

        val options = listOf(
            0 to stringResource(R.string.auto_close_never),
            5 to stringResource(R.string.auto_close_5s),
            10 to stringResource(R.string.auto_close_10s),
            30 to stringResource(R.string.auto_close_30s)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (seconds, label) ->
                val isSelected = state.miniPlayerAutoCloseSeconds == seconds
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) colors.accent else colors.card)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) colors.accent else colors.line.copy(alpha = colors.lineAlpha),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            onAction(AppAction.SetMiniPlayerAutoCloseSeconds(seconds))
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontFamily = LocalAppFont.current,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 12.sp,
                        color = if (isSelected) colors.ink else colors.text,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
