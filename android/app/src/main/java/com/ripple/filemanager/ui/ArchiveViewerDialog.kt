package com.ripple.filemanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ripple.filemanager.ArchiveService
import com.ripple.filemanager.FileItem
import com.ripple.filemanager.R
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveViewerDialog(
    archiveFile: FileItem,
    onDismiss: () -> Unit,
    onExtractRequest: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var fileList by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isExtracting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(archiveFile) {
        isLoading = true
        errorMessage = null
        try {
            val list = ArchiveService.listContents(File(archiveFile.path))
            fileList = list
        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to read archive"
        } finally {
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isExtracting) onDismiss() },
        title = {
            Text(text = archiveFile.name)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isLoading || isExtracting) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    if (isExtracting) {
                        Text("Extracting...", modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp))
                    }
                } else if (errorMessage != null) {
                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                } else {
                    Text("Contents:", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(fileList) { fileName ->
                            Text(
                                text = fileName,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onExtractRequest,
                enabled = !isLoading && errorMessage == null
            ) {
                Text("Extract")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isExtracting
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
