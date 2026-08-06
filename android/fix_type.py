import re

with open('app/src/main/java/com/ripple/filemanager/FileRepository.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_item = '''                    val item = FileItem(
                        id = idCounter++,
                        path = file.absolutePath,
                        name = file.name,
                        type = ext,
                        kind = "file",'''

new_item = '''                    val typeStr = when {
                        ext in setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic") -> "image"
                        ext in setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm") -> "video"
                        ext in setOf("mp3", "wav", "flac", "ogg", "m4a", "aac") -> "audio"
                        ext in setOf("zip", "rar", "7z", "tar", "gz") -> "archive"
                        ext in setOf("pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx", "csv") -> "doc"
                        ext in setOf("apk", "xapk", "aab") -> "apk"
                        else -> "file"
                    }
                    val item = FileItem(
                        id = idCounter++,
                        path = file.absolutePath,
                        name = file.name,
                        type = typeStr,
                        kind = "file",'''

content = content.replace(old_item, new_item)

with open('app/src/main/java/com/ripple/filemanager/FileRepository.kt', 'w', encoding='utf-8') as f:
    f.write(content)
