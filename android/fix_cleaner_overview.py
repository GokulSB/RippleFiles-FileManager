import os
import re

with open('app/src/main/java/com/ripple/filemanager/ui/CleanerScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

target = 'fun CleanerOverview(data: CleanerData, onCategoryClick: (String) -> Unit) {'
replacement = 'fun CleanerOverview(data: CleanerData, onCategoryClick: (String) -> Unit) {\\n    val context = androidx.compose.ui.platform.LocalContext.current'
content = content.replace(target, replacement)

with open('app/src/main/java/com/ripple/filemanager/ui/CleanerScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
