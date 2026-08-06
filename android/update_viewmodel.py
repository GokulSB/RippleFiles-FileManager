import re
with open('app/src/main/java/com/ripple/filemanager/MainViewModel.kt', 'r', encoding='utf-8') as f:
    c = f.read()

c = c.replace('fun extractZip(sourceFile: FileItem, destDirPath: String) {', 'fun extractZip(sourcePath: String, destDirPath: String) {')
c = c.replace('val zipFile = java.util.zip.ZipFile(sourceFile.path)', 'val zipFile = java.util.zip.ZipFile(sourcePath)')
c = c.replace('val folderName = sourceFile.name.substringBeforeLast(".")', 'val folderName = java.io.File(sourcePath).name.substringBeforeLast(".")')

with open('app/src/main/java/com/ripple/filemanager/MainViewModel.kt', 'w', encoding='utf-8') as f:
    f.write(c)
