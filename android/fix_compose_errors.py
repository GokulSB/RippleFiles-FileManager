import os

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace fully qualified drawIntoCanvas
content = content.replace("androidx.compose.ui.graphics.drawscope.drawIntoCanvas { canvas ->", "drawIntoCanvas { canvas ->")

# Replace wildcard imports just to be absolutely sure
content = content.replace("import androidx.compose.animation.core.animateFloat", "import androidx.compose.animation.core.*\nimport androidx.compose.animation.core.animateFloat")

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'w', encoding='utf-8') as f:
    f.write(content)
