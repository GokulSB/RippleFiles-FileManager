import os

filepath = 'app/src/main/java/com/ripple/filemanager/FileRepository.kt'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('\"\"/* \"\"/.*', '\\"\\\\"/* \\"\\\\"/.*')

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)
print("Fixed Shizuku location interpolation")
