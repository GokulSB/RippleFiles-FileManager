import os

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Fix context in Toast
content = content.replace(
    'android.widget.Toast.makeText(context, "Upload feature coming soon"',
    'android.widget.Toast.makeText(androidx.compose.ui.platform.LocalContext.current, "Upload feature coming soon"'
)
content = content.replace(
    'android.widget.Toast.makeText(context, "Scan feature coming soon"',
    'android.widget.Toast.makeText(androidx.compose.ui.platform.LocalContext.current, "Scan feature coming soon"'
)

# Add FabMenuItem at the end if not present
if "fun FabMenuItem(" not in content:
    fab_menu_item = '''
@Composable
fun FabMenuItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    androidx.compose.material3.Surface(
        shape = androidx.compose.foundation.shape.CircleShape,
        color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh,
        onClick = onClick
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = androidx.compose.foundation.layout.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
        ) {
            androidx.compose.material3.Text(
                text = label,
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                modifier = androidx.compose.foundation.layout.padding(end = 12.dp)
            )
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.foundation.layout.size(40.dp)
                    .androidx.compose.foundation.background(
                        androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
                        androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = androidx.compose.foundation.layout.size(20.dp)
                )
            }
        }
    }
}
'''
    content = content + "\n" + fab_menu_item

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'w', encoding='utf-8') as f:
    f.write(content)
