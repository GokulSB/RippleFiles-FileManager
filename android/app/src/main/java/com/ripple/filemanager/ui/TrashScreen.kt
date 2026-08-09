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
import androidx.compose.ui.res.stringResource
import com.ripple.filemanager.R
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
    onClose: () -> Unit
) {
    var selectedFiles by remember { mutableStateOf<ImmutableSet<Int>>(persistentSetOf()) }
    var showSettings by remember { mutableStateOf(false) }
    var deletingIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
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
                            Text(stringResource(R.string.bin_retention_title, state.recycleBinRetentionValue, state.recycleBinRetentionUnit.uppercase()), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.bin_retention_desc, state.recycleBinRetentionValue, state.recycleBinRetentionUnit.lowercase()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text(stringResource(R.string.bin_disabled), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.files_deleted_permanently), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
                            Icon(Icons.Default.Deselect, contentDescription = stringResource(R.string.deselect_all))
                        } else {
                            Icon(Icons.Default.SelectAll, contentDescription = stringResource(R.string.select_all))
                        }
                    }
                    IconButton(onClick = { onAction(AppAction.ToggleViewMode) }) {
                        Icon(if (state.isListMode) Icons.Default.GridView else Icons.Default.ViewList, contentDescription = stringResource(R.string.toggle_view_content_desc))
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
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
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                val filesToRestore = state.trashFiles.filter { it.id in selectedFiles }.mapNotNull { it.encodedTrashName }
                                onAction(AppAction.RestoreTrashFiles(filesToRestore))
                                selectedFiles = kotlinx.collections.immutable.persistentSetOf()
                            },
                            shape = com.ripple.filemanager.ui.getDynamicCornerShape(0f, state.cornerRoundness),
                            colors = ButtonDefaults.buttonColors(containerColor = com.ripple.filemanager.ui.theme.SkylineColors.Amber, contentColor = androidx.compose.ui.graphics.Color(0xFF161009))
                        ) {
                            Icon(androidx.compose.material.icons.Icons.Outlined.Restore, contentDescription = stringResource(R.string.restore_action), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            com.ripple.filemanager.ui.MonoLabel("RESTORE", color = androidx.compose.ui.graphics.Color(0xFF161009), fontSize = 12)
                        }
                        
                        var showDeleteConfirm by remember { mutableStateOf(false) }
                        
                        Button(
                            onClick = { showDeleteConfirm = true },
                            shape = com.ripple.filemanager.ui.getDynamicCornerShape(0f, state.cornerRoundness),
                            colors = ButtonDefaults.buttonColors(containerColor = com.ripple.filemanager.ui.theme.SkylineColors.Rust, contentColor = androidx.compose.ui.graphics.Color(0xFF161009))
                        ) {
                            Icon(androidx.compose.material.icons.Icons.Outlined.Delete, contentDescription = stringResource(R.string.delete_permanently_action), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            com.ripple.filemanager.ui.MonoLabel("DELETE", color = androidx.compose.ui.graphics.Color(0xFF161009), fontSize = 12)
                        }
                        
                        if (showDeleteConfirm) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirm = false },
                                title = { com.ripple.filemanager.ui.MonoLabel("DELETE PERMANENTLY?", color = com.ripple.filemanager.ui.theme.SkylineColors.Amber, fontSize = 14) },
                                text = { Text(stringResource(R.string.delete_forever_warning)) },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            showDeleteConfirm = false
                                            val filesToDelete = state.trashFiles.filter { it.id in selectedFiles }.mapNotNull { it.encodedTrashName }
                                            val idsToRemove = selectedFiles
                                            deletingIds = deletingIds + idsToRemove
                                            selectedFiles = persistentSetOf()
                                            
                                            coroutineScope.launch {
                                                kotlinx.coroutines.delay(220)
                                                onAction(AppAction.PermanentlyDeleteTrashFiles(filesToDelete))
                                                deletingIds = deletingIds - idsToRemove
                                            }
                                        },
                                        shape = com.ripple.filemanager.ui.getDynamicCornerShape(0f, state.cornerRoundness),
                                        colors = ButtonDefaults.buttonColors(containerColor = com.ripple.filemanager.ui.theme.SkylineColors.Rust, contentColor = androidx.compose.ui.graphics.Color(0xFF161009))
                                    ) {
                                        Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.delete_label), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        com.ripple.filemanager.ui.MonoLabel("DELETE", color = androidx.compose.ui.graphics.Color(0xFF161009), fontSize = 12)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteConfirm = false }) {
                                        Text("CANCEL", color = com.ripple.filemanager.ui.theme.SkylineColors.TextDim)
                                    }
                                },
                                containerColor = com.ripple.filemanager.ui.theme.SkylineColors.Surface,
                                shape = com.ripple.filemanager.ui.getDynamicCornerShape(12f, state.cornerRoundness)
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
                        Text(stringResource(R.string.bin_is_empty), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        FileGrid(
                            folderState = FolderListUiState.Loaded(state.trashFiles),
                            selectedFiles = selectedFiles,
                            deletingIds = deletingIds,
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
                            onExtractClick = {}
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
            title = { com.ripple.filemanager.ui.MonoLabel("BIN SETTINGS", color = com.ripple.filemanager.ui.theme.SkylineColors.Amber, fontSize = 14) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.enable_bin))
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
                                label = { Text(stringResource(R.string.time_label)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = com.ripple.filemanager.ui.getDynamicCornerShape(12f, state.cornerRoundness)
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
                                    label = { Text(stringResource(R.string.unit_label)) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    modifier = Modifier.menuAnchor(),
                                    shape = com.ripple.filemanager.ui.getDynamicCornerShape(12f, state.cornerRoundness)
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
                    Text("SAVE", color = com.ripple.filemanager.ui.theme.SkylineColors.Amber)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettings = false }) {
                    Text("CANCEL", color = com.ripple.filemanager.ui.theme.SkylineColors.TextDim)
                }
            },
            containerColor = com.ripple.filemanager.ui.theme.SkylineColors.Surface,
            shape = com.ripple.filemanager.ui.getDynamicCornerShape(12f, state.cornerRoundness)
        )
    }
}
