import re

with open('app/src/main/java/com/ripple/filemanager/FileRepository.kt', 'r', encoding='utf-8') as f:
    content = f.read()

recent_old = '''                    val displayAction = action.replace("Explored", "Visited")
                    items.add(
                        FileItem(
                            id = idCounter++,
                            path = path,
                            name = name,
                            type = type,
                            kind = kind,
                            size = displayAction,
                            changed = timeStr,
                            owner = "Me",
                            isPinned = pinned.contains(path),
                            isEmptyFolder = false,
                            sizeBytes = 0,
                            lastModified = timestamp
                        )
                    )'''

recent_new = '''                    val displayAction = action.replace("Explored", "Visited")
                    
                    var durationStr: String? = null
                    if (type == "video" && file.exists()) {
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
                    
                    items.add(
                        FileItem(
                            id = idCounter++,
                            path = path,
                            name = name,
                            type = type,
                            kind = kind,
                            size = displayAction,
                            changed = timeStr,
                            owner = "Me",
                            isPinned = pinned.contains(path),
                            isEmptyFolder = false,
                            sizeBytes = 0,
                            lastModified = timestamp,
                            duration = durationStr
                        )
                    )'''

content = content.replace(recent_old, recent_new)

files_old = '''            val changedStr = dateFormat.format(Date(file.lastModified()))

            items.add(
                FileItem(
                    id = idCounter++,
                    path = file.absolutePath,
                    name = file.name,
                    type = type,
                    kind = kind,
                    size = sizeStr,
                    changed = changedStr,
                    owner = "Me",
                    isPinned = pinned.contains(file.absolutePath),
                    isEmptyFolder = isFolder && count == 0,
                    sizeBytes = file.length(),
                    lastModified = file.lastModified()
                )
            )'''

files_new = '''            val changedStr = dateFormat.format(Date(file.lastModified()))
            
            var durationStr: String? = null
            if (type == "video" && file.exists()) {
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

            items.add(
                FileItem(
                    id = idCounter++,
                    path = file.absolutePath,
                    name = file.name,
                    type = type,
                    kind = kind,
                    size = sizeStr,
                    changed = changedStr,
                    owner = "Me",
                    isPinned = pinned.contains(file.absolutePath),
                    isEmptyFolder = isFolder && count == 0,
                    sizeBytes = file.length(),
                    lastModified = file.lastModified(),
                    duration = durationStr
                )
            )'''

content = content.replace(files_old, files_new)

with open('app/src/main/java/com/ripple/filemanager/FileRepository.kt', 'w', encoding='utf-8') as f:
    f.write(content)
