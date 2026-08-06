package com.ripple.filemanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ripple.filemanager.FileItem

@Composable
fun DetailsPane(
    file: FileItem?,
    cornerRoundness: Float,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Selected item", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onClose, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(16.dp))) {
                    Icon(Icons.Default.Close, contentDescription = "Close details")
                }
            }

            if (file != null) {
                val shapeDynamic = com.ripple.filemanager.ui.getDynamicCornerShape(16f, cornerRoundness)
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapeDynamic)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, com.ripple.filemanager.ui.theme.SkylineColors.Border, shapeDynamic)
                        .padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FileShapeIcon(file.type, size = 78)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(file.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = com.ripple.filemanager.ui.theme.SkylineColors.TextPrimary)
                        Text("${file.size} - ${file.changed}", style = MaterialTheme.typography.bodySmall, color = com.ripple.filemanager.ui.theme.SkylineColors.TextDim)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapeDynamic)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, com.ripple.filemanager.ui.theme.SkylineColors.Border, shapeDynamic)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    InfoRow("Type", file.type)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                    InfoRow("Owner", file.owner)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                    InfoRow("Location", file.path)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                    InfoRow("Modified", file.changed)
                }

                Text("ACTIVITY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                
                ActivityItem("Shared", "with Design Team yesterday.")
                ActivityItem("Moved", "from Downloads last week.")
                ActivityItem("Synced", "to Drive 4 minutes ago.")
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun ActivityItem(action: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(modifier = Modifier.size(10.dp).background(MaterialTheme.colorScheme.tertiary, CircleShape))
        Text(action, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
