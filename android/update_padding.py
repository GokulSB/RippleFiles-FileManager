import os

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'r', encoding='utf-8') as f:
    content = f.read()

target = "val finalBottomPadding = if (isThreeButton) navBottom + 30.dp else 20.dp"
replacement = "val finalBottomPadding = if (isThreeButton) navBottom + 25.dp else 23.dp"

content = content.replace(target, replacement)

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'w', encoding='utf-8') as f:
    f.write(content)
