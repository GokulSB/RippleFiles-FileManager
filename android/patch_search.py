import sys
content = open('android/SiftApp_replayed.kt', 'r', encoding='utf-8').read()
content = content.replace('var isExtracting by remember { mutableStateOf(false) }', 'var isExtracting by remember { mutableStateOf(false) }\n    var isSearchExpanded by remember { mutableStateOf(false) }')
open('android/app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'w', encoding='utf-8').write(content)
