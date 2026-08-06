import os

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'r', encoding='utf-8') as f:
    content = f.read()

if "import androidx.compose.material.icons.filled.Lock" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.Menu", "import androidx.compose.material.icons.filled.Menu\nimport androidx.compose.material.icons.filled.Lock")

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'w', encoding='utf-8') as f:
    f.write(content)
