import re

with open('app/src/main/java/com/ripple/filemanager/ui/FileGrid.kt', 'r', encoding='utf-8') as f:
    content = f.read()

if 'import coil.request.videoFrameMillis' not in content:
    content = content.replace('import coil.decode.VideoFrameDecoder', 'import coil.decode.VideoFrameDecoder\\nimport coil.request.videoFrameMillis\\nimport androidx.compose.ui.unit.sp')

with open('app/src/main/java/com/ripple/filemanager/ui/FileGrid.kt', 'w', encoding='utf-8') as f:
    f.write(content)

with open('app/src/main/java/com/ripple/filemanager/FileRepository.kt', 'r', encoding='utf-8') as f:
    repo = f.read()

repo = repo.replace('val encodedTrashName: String? = null\\n)', 'val encodedTrashName: String? = null,\\n    val duration: String? = null\\n)')

with open('app/src/main/java/com/ripple/filemanager/FileRepository.kt', 'w', encoding='utf-8') as f:
    f.write(repo)
