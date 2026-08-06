import os

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'r', encoding='utf-8') as f:
    content = f.read()

target = "modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),"
replacement = "modifier = Modifier.align(Alignment.BottomCenter).androidx.compose.foundation.layout.navigationBarsPadding().padding(bottom = 40.dp),"

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'w', encoding='utf-8') as f:
        f.write(content)
    print("Replaced")
else:
    print("Not found")
