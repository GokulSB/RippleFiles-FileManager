import os

with open('app/src/main/java/com/ripple/filemanager/MainViewModel.kt', 'r', encoding='utf-8') as f:
    content = f.read()

target = '''            val pathsToTrash = filesToDelete.map { it.path }
            if (pathsToTrash.isNotEmpty()) {
                repository.moveToTrash(pathsToTrash)
            }'''

replacement = '''            val pathsToTrash = filesToDelete.map { it.path }
            if (pathsToTrash.isNotEmpty()) {
                if (currentCategory == "Empty folders") {
                    pathsToTrash.forEach { path ->
                        try {
                            val f = java.io.File(path)
                            if (f.exists() && f.isDirectory) {
                                f.deleteRecursively()
                            }
                        } catch (e: Exception) {}
                    }
                } else {
                    repository.moveToTrash(pathsToTrash)
                }
            }'''

content = content.replace(target, replacement)

with open('app/src/main/java/com/ripple/filemanager/MainViewModel.kt', 'w', encoding='utf-8') as f:
    f.write(content)
