package com.ripple.filemanager.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ripple.filemanager.FileItem
import com.ripple.filemanager.AppState
import com.ripple.filemanager.AppAction
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    state: AppState,
    onAction: (AppAction) -> Unit,
    onClose: () -> Unit,
    onItemPositioned: (Int, androidx.compose.ui.geometry.Rect) -> Unit = { _, _ -> },
    onDeleteFlying: (com.ripple.filemanager.ui.FlyingDelete) -> Unit = {},
    itemBounds: Map<Int, androidx.compose.ui.geometry.Rect> = emptyMap()
) {
    var selectedFiles by remember { mutableStateOf<ImmutableSet<Int>>(persistentSetOf()) }
    var showSettings by remember { mutableStateOf(false) }
    var shatteringFiles by remember { mutableStateOf<ImmutableSet<Int>>(persistentSetOf()) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = true) {
        if (selectedFiles.isNotEmpty()) {
            selectedFiles = persistentSetOf()
        } else {
            onClose()
        }
    }

    LaunchedEffect(Unit) {
        onAction(AppAction.RefreshTrash)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        if (state.isRecycleBinEnabled) {
                            Text("Bin (${state.recycleBinRetentionValue} ${state.recycleBinRetentionUnit.uppercase()})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Files deleted after ${state.recycleBinRetentionValue} ${state.recycleBinRetentionUnit.lowercase()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text("Bin (DISABLED)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Files are deleted permanently", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        if (selectedFiles.size == state.trashFiles.size && state.trashFiles.isNotEmpty()) {
                            selectedFiles = persistentSetOf()
                        } else {
                            selectedFiles = state.trashFiles.map { it.id }.toImmutableSet()
                        }
                    }) {
                        if (selectedFiles.size == state.trashFiles.size && state.trashFiles.isNotEmpty()) {
                            Icon(Icons.Default.Deselect, contentDescription = "Deselect All")
                        } else {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                    }
                    IconButton(onClick = { onAction(AppAction.ToggleViewMode) }) {
                        Icon(if (state.isListMode) Icons.Default.GridView else Icons.Default.ViewList, contentDescription = "Toggle View")
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            androidx.compose.animation.AnimatedVisibility(
                visible = selectedFiles.isNotEmpty(),
                enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
                exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it })
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(onClick = {
                            val filesToRestore = state.trashFiles.filter { it.id in selectedFiles }.mapNotNull { it.encodedTrashName }
                            onAction(AppAction.RestoreTrashFiles(filesToRestore))
                            selectedFiles = persistentSetOf()
                        }) {
                            Icon(Icons.Outlined.Restore, contentDescription = "Restore", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restore")
                        }
                        
                        var showDeleteConfirm by remember { mutableStateOf(false) }
                        
                        Button(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete Permanently", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete")
                        }
                        
                        if (showDeleteConfirm) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirm = false },
                                title = { Text("Delete permanently?") },
                                text = { Text("These files will be deleted forever. This action cannot be undone.") },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            showDeleteConfirm = false
                                            val filesToDelete = state.trashFiles.filter { it.id in selectedFiles }.mapNotNull { it.encodedTrashName }
                                            val deletingIds = selectedFiles
                                            shatteringFiles = shatteringFiles.plus(deletingIds).toImmutableSet()
                                            selectedFiles = persistentSetOf()
                                            
                                            coroutineScope.launch {
                                                kotlinx.coroutines.delay(500)
                                                onAction(AppAction.PermanentlyDeleteTrashFiles(filesToDelete))
                                                shatteringFiles = shatteringFiles.minus(deletingIds).toImmutableSet()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Outlined.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Delete")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteConfirm = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (state.trashIsLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (state.trashFiles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Bin is empty", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        FileGrid(
                            folderState = FolderListUiState.Loaded(state.trashFiles),
                            selectedFiles = selectedFiles,
                            shatteringFiles = shatteringFiles,
                            isListMode = state.isListMode,
                            iconShape = com.ripple.filemanager.IconShapeType.SYSTEM,
                            cornerRoundness = state.cornerRoundness,
                            searchQuery = "",
                            onFileClick = { file ->
                                if (selectedFiles.isNotEmpty()) {
                                    selectedFiles = if (selectedFiles.contains(file.id)) {
                                        selectedFiles.minus(file.id).toImmutableSet()
                                    } else {
                                        selectedFiles.plus(file.id).toImmutableSet()
                                    }
                                } else {
                                    // Handle single tap... maybe view? (Not fully supported for trash)
                                }
                            },
                            onFileLongClick = { file ->
                                selectedFiles = if (selectedFiles.contains(file.id)) {
                                    selectedFiles.minus(file.id).toImmutableSet()
                                } else {
                                    selectedFiles.plus(file.id).toImmutableSet()
                                }
                            },
                            onPinClick = {},
                            onInfoClick = {},
                            onRenameClick = { _, _ -> },
                            onExtractClick = {},
                            onItemPositioned = onItemPositioned
                        )
                    }
                }
            }
        }
    }
    
    if (showSettings) {
        var isEnabled by remember { mutableStateOf(state.isRecycleBinEnabled) }
        var retentionValue by remember { mutableStateOf(state.recycleBinRetentionValue.toString()) }
        var retentionUnit by remember { mutableStateOf(state.recycleBinRetentionUnit) }
        var expanded by remember { mutableStateOf(false) }
        val units = listOf("Seconds", "Minutes", "Hours", "Days", "Weeks", "Months", "Years")

        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Bin Settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enable Bin")
                        Switch(checked = isEnabled, onCheckedChange = { isEnabled = it })
                    }
                    if (isEnabled) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = retentionValue,
                                onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) retentionValue = it },
                                label = { Text("Time") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = retentionUnit,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Unit") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    modifier = Modifier.menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    units.forEach { selectionOption ->
                                        DropdownMenuItem(
                                            text = { Text(selectionOption) },
                                            onClick = {
                                                retentionUnit = selectionOption
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val value = retentionValue.toIntOrNull() ?: 7
                        onAction(AppAction.SetRecycleBinSettings(isEnabled, value, retentionUnit))
                        showSettings = false
                        onAction(AppAction.RefreshTrash)
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettings = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
