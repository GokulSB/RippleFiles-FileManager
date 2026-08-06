import os

with open('app/src/main/java/com/ripple/filemanager/MainActivity.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_saf = '''        safLauncher.launch(intent)
    }'''
new_saf = '''        try {
            safLauncher.launch(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }'''

content = content.replace(old_saf, new_saf)

with open('app/src/main/java/com/ripple/filemanager/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(content)
