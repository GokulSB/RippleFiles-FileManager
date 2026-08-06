import os

with open('app/src/main/java/com/ripple/filemanager/FileRepository.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Fix the string interpolation issue caused by python script
content = content.replace('\\count items', '\ items')

# Inject call to listRestrictedFiles
old_get_files = '''        } else {
            val rootDir = when (location) {
                "home" -> Environment.getExternalStorageDirectory()
                else -> File(location)
            }
            rootDir.listFiles() ?: return@withContext emptyList()
        }'''
new_get_files = '''        } else {
            if (location.contains("Android/data") || location.contains("Android/obb")) {
                val restrictedItems = listRestrictedFiles(location, dateFormat)
                if (restrictedItems != null) return@withContext restrictedItems
            }
            val rootDir = when (location) {
                "home" -> Environment.getExternalStorageDirectory()
                else -> File(location)
            }
            rootDir.listFiles() ?: return@withContext emptyList()
        }'''
content = content.replace(old_get_files, new_get_files)

with open('app/src/main/java/com/ripple/filemanager/FileRepository.kt', 'w', encoding='utf-8') as f:
    f.write(content)
