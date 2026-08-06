import re

with open('app/src/main/java/com/ripple/filemanager/FileRepository.kt', 'r', encoding='utf-8') as f:
    repo = f.read()

old_scan = '''                    val item = FileItem(
                        id = idCounter++,
                        path = file.absolutePath,
                        name = file.name,
                        type = ext,
                        kind = "file",
                        size = formatSize(size),
                        changed = changedStr,
                        owner = "Me",
                        isPinned = pinned.contains(file.absolutePath),
                        isEmptyFolder = false,
                        sizeBytes = size,
                        lastModified = modified
                    )'''

new_scan = '''                    var durationStr: String? = null
                    val isVideo = ext in listOf("mp4", "mkv", "avi", "mov", "webm")
                    if (isVideo && file.exists()) {
                        try {
                            val retriever = android.media.MediaMetadataRetriever()
                            retriever.setDataSource(file.absolutePath)
                            val timeMillis = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                            if (timeMillis != null) {
                                val secs = (timeMillis / 1000) % 60
                                val mins = (timeMillis / (1000 * 60)) % 60
                                val hours = timeMillis / (1000 * 60 * 60)
                                durationStr = if (hours > 0) String.format("%02d:%02d:%02d", hours, mins, secs)
                                              else String.format("%02d:%02d", mins, secs)
                            }
                            retriever.release()
                        } catch(e: Exception) {}
                    }
                    
                    val item = FileItem(
                        id = idCounter++,
                        path = file.absolutePath,
                        name = file.name,
                        type = ext,
                        kind = "file",
                        size = formatSize(size),
                        changed = changedStr,
                        owner = "Me",
                        isPinned = pinned.contains(file.absolutePath),
                        isEmptyFolder = false,
                        sizeBytes = size,
                        lastModified = modified,
                        duration = durationStr
                    )'''

repo = repo.replace(old_scan, new_scan)

with open('app/src/main/java/com/ripple/filemanager/FileRepository.kt', 'w', encoding='utf-8') as f:
    f.write(repo)
