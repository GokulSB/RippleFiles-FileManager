import os

with open('app/src/main/java/com/ripple/filemanager/FileRepository.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# find "if (hasShizuku()) {" and remove until "return emptyList()" inside listRestrictedFiles
start_idx = content.find("        // 3. Try Shizuku via shell fallback")
end_idx = content.find("        return emptyList()", start_idx)
if start_idx != -1 and end_idx != -1:
    content = content[:start_idx] + content[end_idx:]

with open('app/src/main/java/com/ripple/filemanager/FileRepository.kt', 'w', encoding='utf-8') as f:
    f.write(content)
