import re

with open('app/src/main/java/com/ripple/filemanager/ui/FileGrid.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_box = '''                if (type == "video") {
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
                            androidx.compose.material.icons.Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            modifier = Modifier.align(Alignment.Center).size((size * 0.4).dp),
                            tint = Color.White
                        )
                    }
                }'''

new_box = '''                if (type == "video") {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        modifier = Modifier.align(Alignment.Center).size((size * 0.4).dp),
                        tint = Color.White
                    )
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
                    }
                }'''

content = content.replace(old_box, new_box)

with open('app/src/main/java/com/ripple/filemanager/ui/FileGrid.kt', 'w', encoding='utf-8') as f:
    f.write(content)
