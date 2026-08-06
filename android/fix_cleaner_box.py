import re

with open('app/src/main/java/com/ripple/filemanager/ui/CleanerScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_box = '''          items(categoryData.files) { file ->
              val isSelected = selectedFiles.contains(file.id)
              val isMedia = file.type in setOf("jpg", "jpeg", "png", "gif", "webp", "heic", "mp4", "mkv", "avi", "mov")
              
              Box(
                  modifier = Modifier
                      .aspectRatio(1f)
                      .clip(RoundedCornerShape(8.dp))
                      .background(MaterialTheme.colorScheme.surfaceVariant)
                      .clickable { onFileToggle(file.id) }
              ) {
                  if (isMedia) {
                      AsyncImage(
                          model = file.path,
                          imageLoader = imageLoader,
                          contentDescription = null,
                          modifier = Modifier.fillMaxSize(),
                          contentScale = ContentScale.Crop
                      )
                  } else {
                      Column(
                          modifier = Modifier.fillMaxSize().padding(8.dp),
                          horizontalAlignment = Alignment.CenterHorizontally,
                          verticalArrangement = Arrangement.Center
                      ) {
                          Text(file.name, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                          if (!file.isEmptyFolder) {
                              Text(file.size, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                          }
                      }
                  }
                  
                  RadioButton(
                      selected = isSelected,
                      onClick = { onFileToggle(file.id) },
                      modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                  )
              }
          }'''

new_box = '''          items(categoryData.files) { file ->
              val isSelected = selectedFiles.contains(file.id)
              val isMedia = file.type == "image" || file.type == "video"
              
              Box(
                  modifier = Modifier
                      .aspectRatio(1f)
                      .clip(RoundedCornerShape(8.dp))
                      .background(MaterialTheme.colorScheme.surfaceVariant)
                      .clickable { onFileToggle(file.id) }
              ) {
                  if (isMedia) {
                      val requestBuilder = coil.request.ImageRequest.Builder(context)
                          .data(java.io.File(file.path))
                      if (file.type == "video") {
                          requestBuilder.videoFrameMillis(1000)
                      }
                      
                      AsyncImage(
                          model = requestBuilder.build(),
                          imageLoader = imageLoader,
                          contentDescription = null,
                          modifier = Modifier.fillMaxSize(),
                          contentScale = ContentScale.Crop
                      )
                      
                      if (file.type == "video") {
                          androidx.compose.material3.Icon(
                              androidx.compose.material.icons.Icons.Default.PlayArrow,
                              contentDescription = "Play",
                              modifier = Modifier.align(Alignment.Center).size(36.dp),
                              tint = androidx.compose.ui.graphics.Color.White
                          )
                          if (file.duration != null) {
                              Box(
                                  modifier = Modifier
                                      .align(Alignment.BottomEnd)
                                      .padding(bottom = 8.dp, end = 8.dp)
                                      .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                      .padding(horizontal = 4.dp, vertical = 2.dp)
                              ) {
                                  Text(
                                      text = file.duration,
                                      style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                      color = androidx.compose.ui.graphics.Color.White
                                  )
                              }
                          }
                      }
                  } else {
                      Column(
                          modifier = Modifier.fillMaxSize().padding(8.dp),
                          horizontalAlignment = Alignment.CenterHorizontally,
                          verticalArrangement = Arrangement.Center
                      ) {
                          Text(file.name, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                          if (!file.isEmptyFolder) {
                              Text(file.size, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                          }
                      }
                  }
                  
                  androidx.compose.material3.RadioButton(
                      selected = isSelected,
                      onClick = { onFileToggle(file.id) },
                      modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                  )
              }
          }'''

content = content.replace(old_box, new_box)

with open('app/src/main/java/com/ripple/filemanager/ui/CleanerScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
