import re

with open('app/src/main/java/com/ripple/filemanager/ui/FileGrid.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Add Android icon import if not present
if 'import androidx.compose.material.icons.outlined.Android' not in content:
    content = content.replace('import androidx.compose.material.icons.outlined.Folder', 'import androidx.compose.material.icons.outlined.Folder\nimport androidx.compose.material.icons.outlined.Android')

# Add apk to getShapeData
old_archive = '"archive" -> ShapeData(finalShape, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Icons.Outlined.Inventory2)'
new_apk = '''"archive" -> ShapeData(finalShape, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Icons.Outlined.Inventory2)
        "apk" -> ShapeData(finalShape, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, Icons.Outlined.Android)'''
if '"apk" -> ShapeData' not in content:
    content = content.replace(old_archive, new_apk)

with open('app/src/main/java/com/ripple/filemanager/ui/FileGrid.kt', 'w', encoding='utf-8') as f:
    f.write(content)

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'r', encoding='utf-8') as f:
    sift_content = f.read()

old_name_logic = 'else -> java.io.File(state.location).name.takeIf { it.isNotEmpty() } ?: "Files"'
new_name_logic = '''else -> {
                                      val rawName = java.io.File(state.location).name.takeIf { it.isNotEmpty() } ?: "Files"
                                      val words = rawName.split(Regex("\\\\s+"))
                                      if (words.size > 3) {
                                          words.take(3).joinToString(" ") + "..."
                                      } else {
                                          rawName
                                      }
                                  }'''
sift_content = sift_content.replace(old_name_logic, new_name_logic)

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'w', encoding='utf-8') as f:
    f.write(sift_content)
