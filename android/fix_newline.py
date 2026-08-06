import os

with open('app/src/main/java/com/ripple/filemanager/ui/CleanerScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('{\\n    val context', '{\n    val context')

with open('app/src/main/java/com/ripple/filemanager/ui/CleanerScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
