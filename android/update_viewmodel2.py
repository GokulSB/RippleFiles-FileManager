import os

filepath = 'app/src/main/java/com/ripple/filemanager/MainViewModel.kt'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace("if (f.exists() && f.isDirectory) {\\n                                repository.deleteRestrictedPath(path)\\n                            }", "repository.deleteRestrictedPath(path)")
content = content.replace("if (f.exists()) repository.deleteRestrictedPath(path)", "repository.deleteRestrictedPath(path)")

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)
print("Removed f.exists checks in MainViewModel deletes")
