import re

with open('app/src/main/java/com/ripple/filemanager/FileRepository.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_logic = '''                try {
                    val retriever = android.media.MediaMetadataRetriever()
                    retriever.setDataSource(file.absolutePath)
                    val timeMillis = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()'''

new_logic = '''                try {
                    val retriever = android.media.MediaMetadataRetriever()
                    val fis = java.io.FileInputStream(file)
                    retriever.setDataSource(fis.fd)
                    val timeMillis = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                    fis.close()'''

content = content.replace(old_logic, new_logic)

with open('app/src/main/java/com/ripple/filemanager/FileRepository.kt', 'w', encoding='utf-8') as f:
    f.write(content)
