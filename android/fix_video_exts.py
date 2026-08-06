import re

with open('app/src/main/java/com/ripple/filemanager/FileRepository.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('val isVideo = ext in listOf("mp4", "mkv", "avi", "mov", "webm")', 'val isVideo = ext in listOf("mp4", "mkv", "avi", "mov", "webm", "wmv", "flv")')

with open('app/src/main/java/com/ripple/filemanager/FileRepository.kt', 'w', encoding='utf-8') as f:
    f.write(content)
