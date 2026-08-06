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
                    .clip(com.ripple.filemanager.ui.getDynamicCornerShape(24f, cornerRoundness))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Theme mode", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeButton("System", Icons.Default.Computer, currentMode == ThemeMode.SYSTEM, cornerRoundness) { onModeChange(ThemeMode.SYSTEM) }
                    ModeButton("Light", Icons.Default.LightMode, currentMode == ThemeMode.LIGHT, cornerRoundness) { onModeChange(ThemeMode.LIGHT) }
                    ModeButton("Dark", Icons.Default.DarkMode, currentMode == ThemeMode.DARK, cornerRoundness) { onModeChange(ThemeMode.DARK) }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(com.ripple.filemanager.ui.getDynamicCornerShape(24f, cornerRoundness))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Dynamic color", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Use system theme color", style = MaterialTheme.typography.bodyMedium)
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
                        thumbColor = if (useDynamicTheme) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary,
                        activeTrackColor = if (useDynamicTheme) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val presets = listOf(262f, 14f, 178f, 44f, 340f, 104f)
                    presets.forEach { hue ->
                        val color = hsl(hue, 64f, 58f)
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
                    .clip(com.ripple.filemanager.ui.getDynamicCornerShape(24f, cornerRoundness))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Icon shape", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconShapeType.values().forEach { shapeType ->
                        val isActive = currentIconShape == shapeType
                        val bgColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        val shapeObj = getCustomShape(shapeType)
                        
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(bgColor, shapeObj)
                                .border(1.dp, if (isActive) Color.Transparent else MaterialTheme.colorScheme.outline, shapeObj)
                                .clip(shapeObj)
                                .clickable { onIconShapeChange(shapeType) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (shapeType == IconShapeType.SYSTEM) {
                                Text("Auto", style = MaterialTheme.typography.labelSmall, color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(com.ripple.filemanager.ui.getDynamicCornerShape(24f, cornerRoundness))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Font style", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
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
                Text("Text formatting", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
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
                Text("Corner Roundness", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    Text("Square", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = cornerRoundness,
                        onValueChange = onCornerRoundnessChange,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        valueRange = 0f..1f
                    )
                    Text("Round", style = MaterialTheme.typography.labelSmall)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Grid Columns", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    Text("$gridColumns", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
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