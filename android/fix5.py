import re

with open('app/src/main/java/com/ripple/filemanager/ui/FileGrid.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Modify FileShapeIcon signature
old_sig = 'fun FileShapeIcon(type: String, name: String = "", size: Int, path: String = "", iconShape: com.ripple.filemanager.IconShapeType = com.ripple.filemanager.IconShapeType.SYSTEM)'
new_sig = 'fun FileShapeIcon(type: String, name: String = "", size: Int, path: String = "", iconShape: com.ripple.filemanager.IconShapeType = com.ripple.filemanager.IconShapeType.SYSTEM, duration: String? = null)'
content = content.replace(old_sig, new_sig)

# Modify usage of FileShapeIcon
content = content.replace('FileShapeIcon(file.type, name = file.name, size = 64, path = file.path, iconShape = iconShape)', 'FileShapeIcon(file.type, name = file.name, size = 64, path = file.path, iconShape = iconShape, duration = file.duration)')
content = content.replace('FileShapeIcon(file.type, name = file.name, size = 48, path = file.path, iconShape = iconShape)', 'FileShapeIcon(file.type, name = file.name, size = 48, path = file.path, iconShape = iconShape, duration = file.duration)')
content = content.replace('FileShapeIcon(file.type, name = file.name, size = 42, path = file.path, iconShape = iconShape)', 'FileShapeIcon(file.type, name = file.name, size = 42, path = file.path, iconShape = iconShape, duration = file.duration)')

old_block = '''        if (type == "image" && path.isNotEmpty()) {
            AsyncImage(
                model = File(path),
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(shapeData.icon, contentDescription = type, tint = shapeData.iconColor, modifier = Modifier.size((size * 0.4).dp))
        }'''

new_block = '''        if ((type == "image" || type == "video") && path.isNotEmpty()) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val imageLoader = androidx.compose.runtime.remember {
                coil.ImageLoader.Builder(context)
                    .components {
                        add(coil.decode.VideoFrameDecoder.Factory())
                    }
                    .build()
            }
            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(context)
                        .data(java.io.File(path))
                        .videoFrameMillis(1000)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (type == "video") {
                    if (duration != null) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = duration,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = Color.White
                            )
                        }
                    } else {
                        Icon(
                            androidx.compose.material.icons.Icons.Outlined.PlayCircleOutline,
                            contentDescription = "Play",
                            modifier = Modifier.align(Alignment.Center).size((size * 0.4).dp),
                            tint = Color.White
                        )
                    }
                }
            }
        } else {
            Icon(shapeData.icon, contentDescription = type, tint = shapeData.iconColor, modifier = Modifier.size((size * 0.4).dp))
        }'''

content = content.replace(old_block, new_block)

# Add VideoFrameDecoder import if not there
if 'coil.decode.VideoFrameDecoder' not in content:
    content = content.replace('import androidx.compose.material.icons.outlined.Folder', 'import androidx.compose.material.icons.outlined.Folder\\nimport androidx.compose.material.icons.outlined.PlayCircleOutline\\nimport coil.decode.VideoFrameDecoder')

with open('app/src/main/java/com/ripple/filemanager/ui/FileGrid.kt', 'w', encoding='utf-8') as f:
    f.write(content)

with open('app/src/main/java/com/ripple/filemanager/FileRepository.kt', 'r', encoding='utf-8') as f:
    repo = f.read()

repo = repo.replace('val encodedTrashName: String? = null', 'val encodedTrashName: String? = null,\\n    val duration: String? = null')

old_file_item = '''                val item = FileItem(
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
                )'''

new_file_item = '''                var durationStr: String? = null
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
                
                val item = FileItem(
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
                )'''

repo = repo.replace(old_file_item, new_file_item)

with open('app/src/main/java/com/ripple/filemanager/FileRepository.kt', 'w', encoding='utf-8') as f:
    f.write(repo)
