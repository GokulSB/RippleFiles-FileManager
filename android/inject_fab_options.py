import os

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Add imports
if "import androidx.compose.material.icons.filled.Upload" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.Close", "import androidx.compose.material.icons.filled.Close\nimport androidx.compose.material.icons.filled.Upload\nimport androidx.compose.material.icons.filled.DocumentScanner")

# Replace Column in popup
old_col = '''                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            SmallFloatingActionButton(
                                                onClick = { 
                                                    showFabMenu = false
                                                    showCreateFileDialog = true
                                                },
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                contentColor = MaterialTheme.colorScheme.onSurface
                                            ) {
                                                Icon(Icons.Default.InsertDriveFile, contentDescription = "New file")
                                            }
                                            SmallFloatingActionButton(
                                                onClick = { 
                                                    showFabMenu = false
                                                    showCreateFolderDialog = true
                                                },
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                contentColor = MaterialTheme.colorScheme.onSurface
                                            ) {
                                                Icon(Icons.Default.Folder, contentDescription = "New folder")
                                            }
                                        }'''

new_col = '''                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            FabMenuItem(
                                                label = "New file",
                                                icon = Icons.Default.InsertDriveFile,
                                                onClick = {
                                                    showFabMenu = false
                                                    showCreateFileDialog = true
                                                }
                                            )
                                            FabMenuItem(
                                                label = "New folder",
                                                icon = Icons.Default.Folder,
                                                onClick = {
                                                    showFabMenu = false
                                                    showCreateFolderDialog = true
                                                }
                                            )
                                            if (state.location == "drive" || state.location.startsWith("drive_id:")) {
                                                FabMenuItem(
                                                    label = "Upload",
                                                    icon = Icons.Default.Upload,
                                                    onClick = {
                                                        showFabMenu = false
                                                        android.widget.Toast.makeText(context, "Upload feature coming soon", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            }
                                            FabMenuItem(
                                                label = "Scan document",
                                                icon = Icons.Default.DocumentScanner,
                                                onClick = {
                                                    showFabMenu = false
                                                    android.widget.Toast.makeText(context, "Scan feature coming soon", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }'''

content = content.replace(old_col, new_col)

with open('app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt', 'w', encoding='utf-8') as f:
    f.write(content)
