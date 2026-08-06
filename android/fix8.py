import re

with open('app/src/main/java/com/ripple/filemanager/ui/FileGrid.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Add imports
if 'import coil.decode.VideoFrameDecoder' not in content:
    content = content.replace('import androidx.compose.foundation.background', 'import androidx.compose.foundation.background\\nimport coil.decode.VideoFrameDecoder\\nimport coil.request.videoFrameMillis\\nimport androidx.compose.ui.unit.sp')

# Change PlayCircleOutline to PlayArrow
content = content.replace('androidx.compose.material.icons.Icons.Outlined.PlayCircleOutline', 'androidx.compose.material.icons.Icons.Default.PlayArrow')

with open('app/src/main/java/com/ripple/filemanager/ui/FileGrid.kt', 'w', encoding='utf-8') as f:
    f.write(content)
