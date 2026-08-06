import re

with open('app/src/main/java/com/ripple/filemanager/ui/FileGrid.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_async = '''                AsyncImage(
                    model = coil.request.ImageRequest.Builder(context)
                        .data(java.io.File(path))
                        .videoFrameMillis(1000)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )'''

new_async = '''                val requestBuilder = coil.request.ImageRequest.Builder(context)
                    .data(java.io.File(path))
                if (type == "video") {
                    requestBuilder.videoFrameMillis(1000)
                }
                
                AsyncImage(
                    model = requestBuilder.build(),
                    imageLoader = imageLoader,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )'''

content = content.replace(old_async, new_async)

with open('app/src/main/java/com/ripple/filemanager/ui/FileGrid.kt', 'w', encoding='utf-8') as f:
    f.write(content)
