import os

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'r', encoding='utf-8') as f:
    content = f.read()

target = "modifier = Modifier.align(Alignment.BottomCenter).androidx.compose.foundation.layout.navigationBarsPadding().padding(bottom = 40.dp),"
replacement = "modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 40.dp),"
content = content.replace(target, replacement)

if "import androidx.compose.foundation.layout.navigationBarsPadding" not in content:
    content = content.replace("import androidx.compose.foundation.layout.padding", "import androidx.compose.foundation.layout.padding\nimport androidx.compose.foundation.layout.navigationBarsPadding")

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'w', encoding='utf-8') as f:
    f.write(content)
