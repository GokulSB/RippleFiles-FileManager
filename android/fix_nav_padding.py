import os

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_row = '''                      if (!state.isSelectionMode) {
                          Row(
                              modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),'''
new_row = '''                      if (!state.isSelectionMode) {
                          Row(
                              modifier = Modifier.align(Alignment.BottomCenter).androidx.compose.foundation.layout.navigationBarsPadding().padding(bottom = 20.dp),'''
content = content.replace(old_row, new_row)

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'w', encoding='utf-8') as f:
    f.write(content)
