import os

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Define FabMenuItem
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
if "fun FabMenuItem(" not in content:
    content = content.replace("@Composable\nfun FileGrid(", fab_menu_item + "\n@Composable\nfun FileGrid(")

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'w', encoding='utf-8') as f:
    f.write(content)
