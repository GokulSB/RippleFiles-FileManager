import os

with open('app/src/main/java/com/ripple/filemanager/MainViewModel.kt', 'r', encoding='utf-8') as f:
    content = f.read()

helpers = '''
    fun hasRestrictedAccess(path: String): Boolean {
        if (!path.contains("Android/data") && !path.contains("Android/obb")) return true
        return repository.hasSafPermission(path) || repository.hasRoot() || repository.hasShizuku()
    }
'''

content = content.replace("    fun deleteSelectedCleanerFiles() {", helpers + "\n    fun deleteSelectedCleanerFiles() {")

with open('app/src/main/java/com/ripple/filemanager/MainViewModel.kt', 'w', encoding='utf-8') as f:
    f.write(content)
