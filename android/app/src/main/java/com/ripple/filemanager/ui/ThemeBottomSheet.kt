package com.ripple.filemanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.ripple.filemanager.IconShapeType
import com.ripple.filemanager.ThemeMode
import com.ripple.filemanager.ui.theme.hsl

@Composable
fun ThemeSettingsContent(
    currentMode: ThemeMode,
    currentHue: Float,
    useDynamicTheme: Boolean,
    onModeChange: (ThemeMode) -> Unit,
    onHueChange: (Float) -> Unit,
    onDynamicThemeChange: (Boolean) -> Unit,
    currentIconShape: IconShapeType,
    onIconShapeChange: (IconShapeType) -> Unit,
    fontStyle: String,
    textDecorations: Set<String>,
    mainTextScale: Float,
    subTextScale: Float,
    onFontStyleChange: (String) -> Unit,
    onTextDecorationToggle: (String) -> Unit,
    onMainTextScaleChange: (Float) -> Unit,
    onSubTextScaleChange: (Float) -> Unit,
    cornerRoundness: Float,
    onCornerRoundnessChange: (Float) -> Unit,
    gridColumns: Int,
    onGridColumnsChange: (Int) -> Unit
) {
        Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, com.ripple.filemanager.ui.theme.SkylineColors.Border, com.ripple.filemanager.ui.getDynamicCornerShape(24f, cornerRoundness))
                    .clip(com.ripple.filemanager.ui.getDynamicCornerShape(24f, cornerRoundness))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(stringResource(R.string.theme_mode_label), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeButton("System", Icons.Default.Computer, currentMode == ThemeMode.SYSTEM, cornerRoundness) { onModeChange(ThemeMode.SYSTEM) }
                    ModeButton("Light", Icons.Default.LightMode, currentMode == ThemeMode.LIGHT, cornerRoundness) { onModeChange(ThemeMode.LIGHT) }
                    ModeButton("Dark", Icons.Default.DarkMode, currentMode == ThemeMode.DARK, cornerRoundness) { onModeChange(ThemeMode.DARK) }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, com.ripple.filemanager.ui.theme.SkylineColors.Border, com.ripple.filemanager.ui.getDynamicCornerShape(24f, cornerRoundness))
                    .clip(com.ripple.filemanager.ui.getDynamicCornerShape(24f, cornerRoundness))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(stringResource(R.string.dynamic_color_label), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.use_system_theme_color), style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = useDynamicTheme,
                        onCheckedChange = onDynamicThemeChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                Slider(
                    value = currentHue,
                    onValueChange = onHueChange,
                    valueRange = 0f..360f,
                    enabled = !useDynamicTheme,
                    colors = SliderDefaults.colors(
                        thumbColor = if (useDynamicTheme) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else hsl(currentHue, 65f, 66f),
                        activeTrackColor = if (useDynamicTheme) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else hsl(currentHue, 65f, 66f)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val presets = listOf(262f, 14f, 178f, 44f, 340f, 104f)
                    presets.forEach { hue ->
                        val color = hsl(hue, 65f, 66f)
                        val isActive = kotlin.math.abs(currentHue - hue) < 1f
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(if (isActive) com.ripple.filemanager.ui.getDynamicCornerShape(21f, cornerRoundness) else com.ripple.filemanager.ui.getDynamicCornerShape(12f, cornerRoundness))
                                .background(if (useDynamicTheme) color.copy(alpha = 0.3f) else color)
                                .border(3.dp, if (isActive && !useDynamicTheme) MaterialTheme.colorScheme.onSurface else Color.Transparent, if (isActive) com.ripple.filemanager.ui.getDynamicCornerShape(21f, cornerRoundness) else com.ripple.filemanager.ui.getDynamicCornerShape(12f, cornerRoundness))
                                .clickable(enabled = !useDynamicTheme) { onHueChange(hue) }
                        )
                    }
                }
            }



            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, com.ripple.filemanager.ui.theme.SkylineColors.Border, com.ripple.filemanager.ui.getDynamicCornerShape(24f, cornerRoundness))
                    .clip(com.ripple.filemanager.ui.getDynamicCornerShape(24f, cornerRoundness))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(stringResource(R.string.font_style_label), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("System", "Skyline Ledger", "Roboto", "Google Sans", "Poppins", "Monospace").forEach { style ->
                        val isActive = fontStyle == style
                        val bgColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        val contentColor = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        
                        Surface(
                            shape = com.ripple.filemanager.ui.getDynamicCornerShape(12f, cornerRoundness),
                            color = bgColor,
                            contentColor = contentColor,
                            border = if (isActive) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            onClick = { onFontStyleChange(style) }
                        ) {
                            Text(
                                text = style,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(stringResource(R.string.text_formatting_label), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val formats = listOf(
                        "Bold" to "B",
                        "Italics" to "I",
                        "Underline" to "U",
                        "Strikethrough" to "S"
                    )
                    formats.forEach { (format, initial) ->
                        val isActive = textDecorations.contains(format)
                        val bgColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        val contentColor = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        
                        Surface(
                            shape = com.ripple.filemanager.ui.getDynamicCornerShape(21f, cornerRoundness),
                            color = bgColor,
                            contentColor = contentColor,
                            border = if (isActive) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            onClick = { onTextDecorationToggle(format) },
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = initial,
                                    fontWeight = if (format == "Bold") FontWeight.Bold else FontWeight.Normal,
                                    fontStyle = if (format == "Italics") androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                                    textDecoration = when(format) {
                                        "Underline" -> androidx.compose.ui.text.style.TextDecoration.Underline
                                        "Strikethrough" -> androidx.compose.ui.text.style.TextDecoration.LineThrough
                                        else -> androidx.compose.ui.text.style.TextDecoration.None
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.corner_roundness_label), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    Text(stringResource(R.string.square_corners), style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = cornerRoundness,
                        onValueChange = onCornerRoundnessChange,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        valueRange = 0f..1f
                    )
                    Text(stringResource(R.string.round_corners), style = MaterialTheme.typography.labelSmall)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.grid_columns_label), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    Text(stringResource(R.string.grid_columns_value, gridColumns), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                    Slider(
                        value = gridColumns.toFloat(),
                        onValueChange = { onGridColumnsChange(it.toInt()) },
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        valueRange = 2f..4f,
                        steps = 0
                    )
                }
            }
        }
    }

@Composable
fun SecuritySettingsContent(
    cornerRoundness: Float,
    errorMessage: String?,
    onUpdatePassword: (String, String) -> Unit,
    onSetBiometric: (Boolean) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("sift_prefs", android.content.Context.MODE_PRIVATE)
    
    // We want the current state to reflect the UI
    var oldPassword by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var biometricEnabled by remember { mutableStateOf(prefs.getBoolean("lock_biometric_enabled", false)) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, com.ripple.filemanager.ui.theme.SkylineColors.Border, com.ripple.filemanager.ui.getDynamicCornerShape(24f, cornerRoundness))
                .clip(com.ripple.filemanager.ui.getDynamicCornerShape(24f, cornerRoundness))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(stringResource(R.string.global_password_label), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.global_password_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            if (errorMessage != null) {
                Text(text = errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            OutlinedTextField(
                value = oldPassword,
                onValueChange = { oldPassword = it },
                label = { Text(stringResource(R.string.old_password_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
            )
            
            OutlinedTextField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                label = { Text(stringResource(R.string.set_new_password_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
            )
            androidx.compose.material3.Button(
                onClick = { 
                    if (currentPassword.isNotEmpty() && oldPassword.isNotEmpty()) {
                        onUpdatePassword(oldPassword, currentPassword)
                        oldPassword = ""
                        currentPassword = ""
                    } else {
                        android.widget.Toast.makeText(context, context.getString(R.string.enter_both_passwords_error), android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                shape = com.ripple.filemanager.ui.getDynamicCornerShape(12f, cornerRoundness),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = com.ripple.filemanager.ui.theme.SkylineColors.Amber)
            ) {
                com.ripple.filemanager.ui.MonoLabel("UPDATE PASSWORD", color = com.ripple.filemanager.ui.theme.SkylineColors.Surface)
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, com.ripple.filemanager.ui.theme.SkylineColors.Border, com.ripple.filemanager.ui.getDynamicCornerShape(24f, cornerRoundness))
                .clip(com.ripple.filemanager.ui.getDynamicCornerShape(24f, cornerRoundness))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(stringResource(R.string.biometrics_label), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { 
                    biometricEnabled = !biometricEnabled 
                    onSetBiometric(biometricEnabled)
                }
            ) {
                androidx.compose.material3.Checkbox(
                    checked = biometricEnabled,
                    onCheckedChange = { 
                        biometricEnabled = it 
                        onSetBiometric(it)
                    }
                )
                Text(stringResource(R.string.enable_fingerprint_access), color = com.ripple.filemanager.ui.theme.SkylineColors.TextPrimary)
            }
        }
    }
}

@Composable
fun ModeButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isActive: Boolean, cornerRoundness: Float, onClick: () -> Unit) {
    val bgColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = if (isActive) com.ripple.filemanager.ui.getDynamicCornerShape(21f, cornerRoundness) else com.ripple.filemanager.ui.getDynamicCornerShape(18f, cornerRoundness)

    Surface(
        modifier = Modifier.height(42.dp),
        color = bgColor,
        contentColor = contentColor,
        shape = shape,
        border = if (isActive) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
        }
    }
}