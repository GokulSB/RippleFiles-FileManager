import os

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Fix LocalContext in onClick
content = content.replace(
    'android.widget.Toast.makeText(androidx.compose.ui.platform.LocalContext.current, "Upload feature coming soon"',
    'val ctx = context\n                                                        android.widget.Toast.makeText(ctx, "Upload feature coming soon"'
)
content = content.replace(
    'android.widget.Toast.makeText(androidx.compose.ui.platform.LocalContext.current, "Scan feature coming soon"',
    'val ctx = context\n                                                    android.widget.Toast.makeText(ctx, "Scan feature coming soon"'
)

# FabMenuItem fixes
old_fab = '''        androidx.compose.foundation.layout.Row(
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
        }'''

new_fab = '''        androidx.compose.foundation.layout.Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = androidx.compose.ui.Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
        ) {
            androidx.compose.material3.Text(
                text = label,
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                modifier = androidx.compose.ui.Modifier.padding(end = 12.dp)
            )
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier.size(40.dp)
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
                    modifier = androidx.compose.ui.Modifier.size(20.dp)
                )
            }
        }'''

content = content.replace(old_fab, new_fab)

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'w', encoding='utf-8') as f:
    f.write(content)
