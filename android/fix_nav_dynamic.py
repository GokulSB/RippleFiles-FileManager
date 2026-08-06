import os

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'r', encoding='utf-8') as f:
    content = f.read()

target = "modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 40.dp),"
replacement = '''val navBottom = androidx.compose.foundation.layout.WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                              val isThreeButton = navBottom > 24.dp
                              val finalBottomPadding = if (isThreeButton) navBottom + 30.dp else 20.dp
                              modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = finalBottomPadding),'''

content = content.replace(target, replacement)

if "val isThreeButton" not in content:
    print("Could not find target string!")
else:
    with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'w', encoding='utf-8') as f:
        f.write(content)
    print("Replaced")
