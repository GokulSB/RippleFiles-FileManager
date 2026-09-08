package com.ripple.filemanager.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ripple.filemanager.R
import com.ripple.filemanager.AppState
import com.ripple.filemanager.AppAction
import com.ripple.filemanager.ThemeMode
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.ripple.filemanager.ui.theme.hsl
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

fun Modifier.hardShadow(
    color: Color,
    offsetX: Dp = 4.dp,
    offsetY: Dp = 4.dp,
    radiusDp: Dp
) = this.drawBehind {
    val radiusPx = radiusDp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(offsetX.toPx(), offsetY.toPx()),
        size = size,
        cornerRadius = CornerRadius(radiusPx, radiusPx)
    )
}

@Composable
fun SettingsSectionCard(
    title: String,
    eyebrow: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    isExpanded: Boolean,
    cornerRoundness: Float,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val shapeRadius = (16f * (cornerRoundness * 2)).coerceIn(0f, 100f).dp
    val shape = RoundedCornerShape(shapeRadius)
    val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "rotation")
    val shadowColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .hardShadow(color = shadowColor, radiusDp = shapeRadius)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(3.dp)
                        .background(accentColor)
                )
                
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val iconShapeRadius = (8f * (cornerRoundness * 2)).coerceIn(0f, 100f).dp
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(iconShapeRadius))
                                    .clip(RoundedCornerShape(iconShapeRadius)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            Column {
                                Text(
                                    text = eyebrow,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.graphicsLayer { rotationZ = rotation }
                        )
                    }
                    
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(animationSpec = tween(250)) + fadeIn(animationSpec = tween(200)),
                        exit = shrinkVertically(animationSpec = tween(250)) + fadeOut(animationSpec = tween(150))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                            content()
                        }
                    }
                }
            }
        }
    }
}

private object NoRippleIndication : androidx.compose.foundation.IndicationNodeFactory {
    override fun create(interactionSource: androidx.compose.foundation.interaction.InteractionSource): androidx.compose.ui.node.DelegatableNode {
        return object : androidx.compose.ui.Modifier.Node() {}
    }
    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = 0
}

@Composable
fun SettingsScreen(
    state: AppState,
    onAction: (AppAction) -> Unit
) {
    var expandedSection by remember { mutableStateOf<String?>("theme") }
    
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
                Toast.makeText(context, context.getString(R.string.select_internal_storage_error), Toast.LENGTH_SHORT).show()
            }
        }
        currentPickingCategory.value = null
    }

    CompositionLocalProvider(androidx.compose.foundation.LocalIndication provides NoRippleIndication) {
        Surface(modifier = Modifier.fillMaxSize().appGradientBackground(), color = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground) {
            Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { haptics.settingsToggle(); onAction(AppAction.SetShowSettingsScreen(false)) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium) 
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape((16f * (state.cornerRoundness * 2)).coerceIn(0f, 100f).dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth().hardShadow(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), radiusDp = (16f * (state.cornerRoundness * 2)).coerceIn(0f, 100f).dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val iconShapeRadius = (8f * (state.cornerRoundness * 2)).coerceIn(0f, 100f).dp
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(iconShapeRadius))
                                    .clip(RoundedCornerShape(iconShapeRadius)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Vibration, contentDescription = null, tint = Color(0xFFE0AC70), modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Vibration Feedback", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text("Global haptics toggle", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = state.haptics.masterEnabled,
                            onCheckedChange = { hapticOnAction(AppAction.ToggleHapticsMaster(it)) }
                        )
                    }
                }

                SettingsSectionCard(
                    title = "Appearance",
                    eyebrow = "01 · APPEARANCE",
                    icon = Icons.Default.Palette,
                    accentColor = MaterialTheme.colorScheme.primary,
                    isExpanded = expandedSection == "theme",
                    cornerRoundness = state.cornerRoundness,
                    onClick = { 
                        haptics.settingsToggle()
                        expandedSection = if (expandedSection == "theme") null else "theme" 
                    }
                ) {
                    SettingsThemeContent(state, hapticOnAction)
                }
                
                SettingsSectionCard(
                    title = "Security",
                    eyebrow = "02 · ACCESS",
                    icon = Icons.Default.Security,
                    accentColor = MaterialTheme.colorScheme.error,
                    isExpanded = expandedSection == "security",
                    cornerRoundness = state.cornerRoundness,
                    onClick = { 
                        haptics.settingsToggle()
                        expandedSection = if (expandedSection == "security") null else "security" 
                    }
                ) {
                    SettingsSecurityContent(state, hapticOnAction)
                }
                
                SettingsSectionCard(
                    title = "Viewers",
                    eyebrow = "03 · DISPLAY",
                    icon = Icons.Default.Visibility,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    isExpanded = expandedSection == "viewers",
                    cornerRoundness = state.cornerRoundness,
                    onClick = { 
                        haptics.settingsToggle()
                        expandedSection = if (expandedSection == "viewers") null else "viewers" 
                    }
                ) {
                    SettingsViewersContent(state, hapticOnAction)
                }

                SettingsSectionCard(
                    title = "File Organiser",
                    eyebrow = "04 · AUTOMATION",
                    icon = Icons.Default.FolderSpecial,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    isExpanded = expandedSection == "organiser",
                    cornerRoundness = state.cornerRoundness,
                    onClick = { 
                        haptics.settingsToggle()
                        expandedSection = if (expandedSection == "organiser") null else "organiser" 
                    }
                ) {
                    SettingsOrganiserContent(state, hapticOnAction, launcher, currentPickingCategory)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    }
}

@Composable
fun SettingsThemeContent(state: AppState, onAction: (AppAction) -> Unit) {
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Invert Text Color", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text("Toggle between white and black text", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = state.invertText,
                onCheckedChange = { onAction(AppAction.SetInvertText(it)) }
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Dynamic System Theme", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text("Follow wallpaper colors", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = state.useDynamicSystemTheme,
                onCheckedChange = { onAction(AppAction.SetDynamicSystemTheme(it)) }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Accent Color", style = MaterialTheme.typography.labelMedium, color = if (state.useDynamicSystemTheme) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val presets = listOf(262f, 14f, 178f, 44f, 340f, 104f)
                presets.forEach { hue ->
                    val color = hsl(hue, 65f, 66f)
                    val isActive = kotlin.math.abs(state.themeHue - hue) < 1f && !state.useDynamicSystemTheme
                    val swatchRadius = (if (isActive) 21f else 12f * (state.cornerRoundness * 2)).coerceIn(0f, 100f).dp
                    
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .then(
                                if (isActive) Modifier.hardShadow(MaterialTheme.colorScheme.onSurface.copy(alpha=0.2f), 2.dp, 2.dp, swatchRadius)
                                else Modifier
                            )
                            .clip(RoundedCornerShape(swatchRadius))
                            .background(if (state.useDynamicSystemTheme) color.copy(alpha = 0.3f) else color)
                            .clickable(enabled = !state.useDynamicSystemTheme) { onAction(AppAction.SetThemeHue(hue)) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isActive) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Font Style", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("System", "Skyline Ledger", "Roboto", "Google Sans").forEach { style ->
                    val isActive = state.fontStyle == style
                    val bgColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    val contentColor = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    val pillRadius = (16f * (state.cornerRoundness * 2)).coerceIn(0f, 100f).dp
                    
                    Surface(
                        shape = RoundedCornerShape(pillRadius),
                        color = bgColor,
                        contentColor = contentColor,
                        onClick = { onAction(AppAction.SetFontStyle(style)) }
                    ) {
                        Text(
                            text = style,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Corner Roundness", style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CropSquare, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Slider(
                    value = state.cornerRoundness,
                    onValueChange = { onAction(AppAction.SetCornerRoundness(it)) },
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
                Icon(Icons.Default.Lens, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Grid Columns", style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${state.gridColumns}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                Slider(
                    value = state.gridColumns.toFloat(),
                    onValueChange = { onAction(AppAction.SetGridColumns(it.toInt())) },
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    valueRange = 2f..4f,
                    steps = 1,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
fun SettingsSecurityContent(state: AppState, onAction: (AppAction) -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("sift_prefs", android.content.Context.MODE_PRIVATE)
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    
    var oldPassword by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var biometricEnabled by remember { mutableStateOf(prefs.getBoolean("lock_biometric_enabled", false)) }
    
    val inputRadius = (8f * (state.cornerRoundness * 2)).coerceIn(0f, 100f).dp
    val buttonRadius = (12f * (state.cornerRoundness * 2)).coerceIn(0f, 100f).dp

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        
        OutlinedTextField(
            value = oldPassword,
            onValueChange = { oldPassword = it },
            label = { Text(stringResource(R.string.old_password_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(inputRadius),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
        )
        
        OutlinedTextField(
            value = currentPassword,
            onValueChange = { currentPassword = it },
            label = { Text(stringResource(R.string.set_new_password_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(inputRadius),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
        )
        
        Button(
            onClick = { 
                haptics.settingsToggle()
                if (currentPassword.isNotEmpty() && oldPassword.isNotEmpty()) {
                    onAction(AppAction.UpdateGlobalPassword(oldPassword, currentPassword))
                    oldPassword = ""
                    currentPassword = ""
                } else {
                    Toast.makeText(context, context.getString(R.string.enter_both_passwords_error), Toast.LENGTH_SHORT).show()
                }
            },
            shape = RoundedCornerShape(buttonRadius),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .hardShadow(MaterialTheme.colorScheme.onSurface.copy(alpha=0.3f), 4.dp, 4.dp, buttonRadius)
        ) {
            Text("UPDATE PASSWORD", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 4.dp))
        }
        
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(stringResource(R.string.biometrics_label), style = MaterialTheme.typography.labelMedium)
                Text(stringResource(R.string.enable_fingerprint_access), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            val toggleRadius = (4f * (state.cornerRoundness * 2)).coerceIn(0f, 100f).dp
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(toggleRadius))
                    .background(if (biometricEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, if (biometricEnabled) Color.Transparent else MaterialTheme.colorScheme.outline, RoundedCornerShape(toggleRadius))
                    .clickable { 
                        biometricEnabled = !biometricEnabled 
                        onAction(AppAction.SetBiometricEnabled(biometricEnabled))
                    },
                contentAlignment = Alignment.Center
            ) {
                if (biometricEnabled) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun SettingsViewersContent(state: AppState, onAction: (AppAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingsViewerRow(stringResource(R.string.text_pdf_viewer), state.viewerTextPdf, state.cornerRoundness) { onAction(AppAction.SetViewerPreference("Text/PDF", it)) }
        SettingsViewerRow(stringResource(R.string.music_viewer), state.viewerMusic, state.cornerRoundness) { onAction(AppAction.SetViewerPreference("Music", it)) }
        SettingsViewerRow(stringResource(R.string.image_viewer), state.viewerImage, state.cornerRoundness) { onAction(AppAction.SetViewerPreference("Image", it)) }
    }
}

@Composable
fun SettingsViewerRow(label: String, currentValue: String, cornerRoundness: Float, onValueChange: (String) -> Unit) {
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    var expanded by remember { mutableStateOf(false) }
    val pillRadius = (16f * (cornerRoundness * 2)).coerceIn(0f, 100f).dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        
        Box {
            Surface(
                shape = RoundedCornerShape(pillRadius),
                color = MaterialTheme.colorScheme.surfaceVariant,
                onClick = { haptics.settingsToggle(); expanded = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(currentValue, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("In-app") },
                    onClick = { onValueChange("In-app"); expanded = false }
                )
                DropdownMenuItem(
                    text = { Text("External") },
                    onClick = { onValueChange("External"); expanded = false }
                )
            }
        }
    }
}

@Composable
fun SettingsOrganiserContent(
    state: AppState, 
    onAction: (AppAction) -> Unit, 
    launcher: androidx.activity.result.ActivityResultLauncher<android.net.Uri?>,
    currentCategory: MutableState<String?>
) {
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        val paths = listOf(
            "Docs" to ("Documents" to state.orgDestDocs),
            "Images" to ("Images" to state.orgDestImages),
            "Apks" to ("APKs" to state.orgDestApks),
            "Music" to ("Music" to state.orgDestMusic),
            "Videos" to ("Videos" to state.orgDestVideos)
        )

        paths.forEach { (cat, info) ->
            val (label, pathValue) = info
            val rowRadius = (12f * (state.cornerRoundness * 2)).coerceIn(0f, 100f).dp

            Surface(
                shape = RoundedCornerShape(rowRadius),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.5f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = pathValue.ifEmpty { "Not set" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            softWrap = true
                        )
                    }
                    IconButton(
                        onClick = { 
                            haptics.settingsToggle()
                            currentCategory.value = cat
                            launcher.launch(null) 
                        },
                        modifier = Modifier.size(32.dp).offset(x = 8.dp, y = (-4).dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Change path", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

