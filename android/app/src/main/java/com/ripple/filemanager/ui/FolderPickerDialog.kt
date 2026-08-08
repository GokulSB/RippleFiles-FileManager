package com.ripple.filemanager.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ripple.filemanager.FileItem
import com.ripple.filemanager.FileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderPickerDialog(
    initialPath: String = android.os.Environment.getExternalStorageDirectory().path,
    onDismiss: () -> Unit,
    onFolderSelected: (String) -> Unit,
    cornerRoundness: Float = 0.5f
) {
    val context = LocalContext.current
    var currentPath by remember { mutableStateOf(initialPath) }
    var folders by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { FileRepository(context) }

    fun loadFolders(path: String) {
        isLoading = true
        coroutineScope.launch {
            val files = repository.getFiles(path)
            folders = files.filter { it.type == "folder" }.sortedBy { it.name.lowercase() }
            currentPath = path
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadFolders(currentPath)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.8f).fillMaxWidth(0.9f),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (currentPath != android.os.Environment.getExternalStorageDirectory().path) {
                    IconButton(onClick = {
                        val parent = File(currentPath).parent
                        if (parent != null) {
                            loadFolders(parent)
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = com.ripple.filemanager.ui.theme.SkylineColors.Amber)
                    }
                }
                com.ripple.filemanager.ui.MonoLabel("SELECT LOCATION", color = com.ripple.filemanager.ui.theme.SkylineColors.Amber, fontSize = 16)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(currentPath, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (folders.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No folders here")
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(folders) { folder ->
                            ListItem(
                                headlineContent = { Text(folder.name) },
                                leadingContent = { Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                modifier = Modifier.clickable {
                                    loadFolders(folder.path)
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onFolderSelected(currentPath) }) {
                Text("EXTRACT", color = com.ripple.filemanager.ui.theme.SkylineColors.Amber)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = com.ripple.filemanager.ui.theme.SkylineColors.TextDim)
            }
        },
        containerColor = com.ripple.filemanager.ui.theme.SkylineColors.Surface,
        shape = getDynamicCornerShape(12f, cornerRoundness)
    )
}
