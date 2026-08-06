

# Operation 0: multi_replace_file_content
Instruction: Remove the extra closing brace at line 173 and the three extra closing braces at lines 348-350 to fix the brace mismatch that causes compilation errors.
## Chunk 0 Replacement Content:
`kotlin
            }
                                if (state.pasteProgress != null) {
`
## Chunk 1 Replacement Content:
`kotlin
            }

`


# Operation 1: multi_replace_file_content
Instruction: Remove the extra closing brace at line 277 and line 346 to fix the remaining brace imbalance.
## Chunk 0 Replacement Content:
`kotlin
                                }
                            

`
## Chunk 1 Replacement Content:
`kotlin


`


# Operation 2: replace_file_content
Instruction: Remove the extra closing brace at line 345 that causes the brace depth to go negative.
## Replacement Content:
`kotlin
                             }\r\n\r\n             AnimatedVisibility(
`


# Operation 3: multi_replace_file_content
Instruction: Add variable declarations for showFabMenu, hasClipboardItems, showCreateFolderDialog, showCreateFileDialog before the FAB code, and wrap the block in Box(Modifier.fillMaxSize()) to provide BoxScope for .align(). Also close the Box after the dialogs.
## Chunk 0 Replacement Content:
`kotlin
            }

            var showFabMenu by remember { mutableStateOf(false) }
            var showCreateFolderDialog by remember { mutableStateOf(false) }
            var showCreateFileDialog by remember { mutableStateOf(false) }
            val hasClipboardItems = state.clipboardPaths.isNotEmpty()

            Box(modifier = Modifier.fillMaxSize()) {
                                if (state.pasteProgress != null) {
`
## Chunk 1 Replacement Content:
`kotlin
                             }
            }

             AnimatedVisibility(
`


# Operation 4: replace_file_content
Instruction: Replace the corrupted dismissButton section (lines 345-519) with the correct dismissButton content plus proper closing of the create file dialog and Box wrapper.
## Replacement Content:
`kotlin
                                    dismissButton = {\r\n                                        TextButton(onClick = { showCreateFileDialog = false }) {\r\n                                            Text(\"Cancel\")\r\n                                        }\r\n                                    }\r\n                                )\r\n                             }\r\n            }\r\n\r\n             AnimatedVisibility(
`


# Operation 5: replace_file_content
Instruction: Add ViewerPreferenceItem composable and openFileInExternalApp helper after FabMenuItem in SiftApp.kt
## Replacement Content:
`kotlin
    }
}

@Composable
fun ViewerPreferenceItem(
    label: String,
    currentValue: String,
    cornerRoundness: Float,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        shape = getDynamicCornerShape(16f, cornerRoundness),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            androidx.compose.foundation.layout.Box {
                Surface(
                    shape = getDynamicCornerShape(12f, cornerRoundness),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    onClick = { expanded = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(currentValue, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Select", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
                androidx.compose.material3.DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("In-app") },
                        onClick = { onValueChange("In-app"); expanded = false }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Device default") },
                        onClick = { onValueChange("Device default"); expanded = false }
                    )
                }
            }
        }
    }
}

fun openFileInExternalApp(context: android.content.Context, file: com.ripple.filemanager.FileItem, defaultMime: String) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", java.io.File(file.path))
        val mimeType = when {
            file.name.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            file.name.endsWith(".html", ignoreCase = true) || file.name.endsWith(".htm", ignoreCase = true) -> "text/html"
            file.name.endsWith(".csv", ignoreCase = true) -> "text/csv"
            file.name.endsWith(".json", ignoreCase = true) -> "application/json"
            file.name.endsWith(".xml", ignoreCase = true) -> "text/xml"
            file.name.endsWith(".txt", ignoreCase = true) || file.name.endsWith(".md", ignoreCase = true) || file.name.endsWith(".log", ignoreCase = true) || file.name.endsWith(".kt", ignoreCase = true) || file.name.endsWith(".java", ignoreCase = true) || file.name.endsWith(".py", ignoreCase = true) -> "text/plain"
            else -> defaultMime
        }
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = android.content.Intent.createChooser(intent, "Open with").apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(context, "No app found to open this file", android.widget.Toast.LENGTH_LONG).show()
    }
}

@Composable
fun GradientProgressIndicator(
`


# Operation 6: replace_file_content
Instruction: Insert Viewers collapsible section before File Organiser in Settings screen of SiftApp.kt
## Replacement Content:
`kotlin
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        var isViewersExpanded by remember { mutableStateOf(false) }
                        
                        Surface(
                            shape = getDynamicCornerShape(16f, state.cornerRoundness),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isViewersExpanded = !isViewersExpanded }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Viewers", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Icon(
                                    imageVector = if (isViewersExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Expand",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        androidx.compose.animation.AnimatedVisibility(visible = isViewersExpanded) {
                            Column(
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                ViewerPreferenceItem(
                                    label = "Text / PDF",
                                    currentValue = state.viewerTextPdf,
                                    cornerRoundness = state.cornerRoundness,
                                    onValueChange = { onAction(AppAction.SetViewerPreference("Text/PDF", it)) }
                                )
                                ViewerPreferenceItem(
                                    label = "Music",
                                    currentValue = state.viewerMusic,
                                    cornerRoundness = state.cornerRoundness,
                                    onValueChange = { onAction(AppAction.SetViewerPreference("Music", it)) }
                                )
                                ViewerPreferenceItem(
                                    label = "Image",
                                    currentValue = state.viewerImage,
                                    cornerRoundness = state.cornerRoundness,
                                    onValueChange = { onAction(AppAction.SetViewerPreference("Image", it)) }
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        var isOrganiserExpanded by remember { mutableStateOf(false) }
`


# Operation 7: replace_file_content
Instruction: Update file click handling in SiftApp.kt for images, text/pdf, and audio to check viewerImage, viewerTextPdf, and viewerMusic settings
## Replacement Content:
`kotlin
                            } else if (file.name.endsWith(".pdf", ignoreCase = true) || file.type == "image" || file.type == "doc" || listOf(".txt", ".json", ".md", ".csv", ".xml", ".log", ".kt", ".java", ".py", ".html").any { file.name.endsWith(it, ignoreCase = true) }) {
                                if (file.type == "image" && state.viewerImage == "Device default") {
                                    openFileInExternalApp(context, file, "image/*")
                                } else if (file.type != "image" && state.viewerTextPdf == "Device default") {
                                    val defaultMime = if (file.name.endsWith(".pdf", ignoreCase = true)) "application/pdf" else "text/plain"
                                    openFileInExternalApp(context, file, defaultMime)
                                } else {
                                    onAction(AppAction.OpenFileViewer(file.id))
                                }
                            } else if (file.path.startsWith("drive_id:")) {
                                val id = file.path.removePrefix("drive_id:")
                                val url = "https://drive.google.com/file/d/$id/view"
                                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            } else if (file.type == "audio") {
                                if (state.viewerMusic == "Device default") {
                                    openFileInExternalApp(context, file, "audio/*")
                                } else {
                                    onAction(AppAction.PlayAudio(file))
                                }
                            } else if (file.type == "video") {
`


# Operation 8: multi_replace_file_content
Instruction: 1. Add drawer-related imports after line 70. 2. Wrap the SiftApp Surface content with ModalNavigationDrawer and pass onDrawerOpen to MainContent. 3. Add DrawerContent composable before the MainContent function. 4. Update MainContent signature to accept onDrawerOpen. 5. Replace the topBar with new header design. 6. Remove isSearchExpanded state and BackHandler.
## Chunk 0 Replacement Content:
`kotlin
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.launch
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource

`
## Chunk 1 Replacement Content:
`kotlin
    ) {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val drawerScope = rememberCoroutineScope()

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                DrawerContent(
                    state = state,
                    onAction = onAction,
                    cornerRoundness = state.cornerRoundness,
                    onCloseDrawer = { drawerScope.launch { drawerState.close() } }
                )
            }
        ) {
        Surface(
            color = MaterialTheme.colorScheme.surface, 
            modifier = Modifier.fillMaxSize()
        ) {
`
## Chunk 2 Replacement Content:
`kotlin
                    MainContent(
                        state = state,
                        onAction = onAction,
                        snackbarHostState = snackbarHostState,
                        onDrawerOpen = { drawerScope.launch { drawerState.open() } },
                        modifier = Modifier.weight(1f)
                    )
`
## Chunk 3 Replacement Content:
`kotlin
                    MainContent(
                        state = state,
                        onAction = onAction,
                        snackbarHostState = snackbarHostState,
                        onDrawerOpen = { drawerScope.launch { drawerState.open() } },
                        modifier = Modifier.fillMaxSize()
                    )
`
## Chunk 4 Replacement Content:
`kotlin
        }
    }
    } // end ModalNavigationDrawer
}

@Composable
fun DrawerContent(
    state: AppState,
    onAction: (AppAction) -> Unit,
    cornerRoundness: Float,
    onCloseDrawer: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Close button
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onCloseDrawer) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Branding card
            Surface(
                shape = getDynamicCornerShape(16f, cornerRoundness),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Surface(
                        shape = getDynamicCornerShape(12f, cornerRoundness),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Ripple Files",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Every file, in flow.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Menu items
            DrawerMenuItem(
                icon = Icons.Outlined.Cloud,
                label = "Link cloud",
                cornerRoundness = cornerRoundness,
                onClick = {
                    onCloseDrawer()
                    onAction(AppAction.SetLocation("drive"))
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            DrawerMenuItem(
                icon = Icons.Outlined.CleaningServices,
                label = "Storage cleaner",
                cornerRoundness = cornerRoundness,
                onClick = {
                    onCloseDrawer()
                    onAction(AppAction.SetCleanerScreenVisible(true))
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            DrawerMenuItem(
                icon = Icons.Outlined.Settings,
                label = "Settings",
                cornerRoundness = cornerRoundness,
                onClick = {
                    onCloseDrawer()
                    onAction(AppAction.SetShowSettingsScreen(true))
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            DrawerMenuItem(
                icon = Icons.Outlined.Info,
                label = "About",
                cornerRoundness = cornerRoundness,
                onClick = {
                    onCloseDrawer()
                    // TODO: Show about dialog
                }
            )
        }
    }
}

@Composable
fun DrawerMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    cornerRoundness: Float,
    onClick: () -> Unit
) {
    Surface(
        shape = getDynamicCornerShape(14f, cornerRoundness),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Surface(
                shape = getDynamicCornerShape(12f, cornerRoundness),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
`
## Chunk 5 Replacement Content:
`kotlin
fun MainContent(state: AppState, onAction: (AppAction) -> Unit, snackbarHostState: SnackbarHostState, onDrawerOpen: () -> Unit = {}, modifier: Modifier = Modifier) {
`
## Chunk 6 Replacement Content:
`kotlin
    // Search is always visible in the new header design
`
## Chunk 7 Replacement Content:
`kotlin
    BackHandler(enabled = state.query.isNotEmpty()) {
        onAction(AppAction.SetQuery(""))
    }
`
## Chunk 8 Replacement Content:
`kotlin
    androidx.activity.compose.PredictiveBackHandler(enabled = state.selectedFiles.isEmpty() && state.query.isEmpty() && state.location != "home") { progress ->
`
## Chunk 9 Replacement Content:
`kotlin
        topBar = {
            Row(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hamburger menu icon
                IconButton(onClick = onDrawerOpen) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Menu",
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Full-width search bar
                TextField(
                    value = state.query,
                    onValueChange = { onAction(AppAction.SetQuery(it)) },
                    placeholder = {
                        Text(
                            "Search files & folders",
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { onAction(AppAction.SetQuery("")) }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = getDynamicCornerShape(24f, state.cornerRoundness),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Trash icon
                IconButton(onClick = { onAction(AppAction.SetTrashScreenVisible(true)) }) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Trash",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
`


# Operation 9: replace_file_content
Instruction: Replace the isSearchExpanded check with clearing the search query if it's not empty
## Replacement Content:
`kotlin
                    onFileClick = { file -> 
                        if (state.query.isNotEmpty()) {
                            onAction(AppAction.SetQuery(""))
                        }
`


# Operation 10: multi_replace_file_content
Instruction: Apply all UI tweaks: pill containers on header icons, more gap, drawer positioning, close button container, expandable Link cloud section with GDrive/Mega/Dropbox
## Chunk 0 Replacement Content:
`kotlin
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Close button with container
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Surface(
                    shape = getDynamicCornerShape(12f, cornerRoundness),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(40.dp)
                ) {
                    IconButton(onClick = onCloseDrawer) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
`
## Chunk 1 Replacement Content:
`kotlin
            Spacer(modifier = Modifier.height(4.dp))
`
## Chunk 2 Replacement Content:
`kotlin
            Spacer(modifier = Modifier.height(16.dp))

            // Menu items
            var isCloudExpanded by remember { mutableStateOf(false) }

            // Link cloud - expandable section
            Surface(
                shape = getDynamicCornerShape(14f, cornerRoundness),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                onClick = { isCloudExpanded = !isCloudExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Surface(
                            shape = getDynamicCornerShape(12f, cornerRoundness),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.Cloud,
                                    contentDescription = "Link cloud",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "Link cloud",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            if (isCloudExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AnimatedVisibility(visible = isCloudExpanded) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
                        ) {
                            // Google Drive - active
                            Surface(
                                shape = getDynamicCornerShape(12f, cornerRoundness),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                onClick = {
                                    onCloseDrawer()
                                    onAction(AppAction.SetLocation("drive"))
                                },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize().padding(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CloudDone,
                                        contentDescription = "Google Drive",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        "GDrive",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Mega - greyed out
                            Surface(
                                shape = getDynamicCornerShape(12f, cornerRoundness),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize().padding(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Cloud,
                                        contentDescription = "Mega",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        "Mega",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                        maxLines = 1
                                    )
                                }
                            }

                            // Dropbox - greyed out
                            Surface(
                                shape = getDynamicCornerShape(12f, cornerRoundness),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize().padding(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Cloud,
                                        contentDescription = "Dropbox",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        "Dropbox",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
`
## Chunk 3 Replacement Content:
`kotlin
        topBar = {
            Row(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 12.dp)
                    .padding(top = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hamburger menu icon in pill container
                Surface(
                    shape = getDynamicCornerShape(14f, state.cornerRoundness),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(onClick = onDrawerOpen) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Menu",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Full-width search bar
                TextField(
                    value = state.query,
                    onValueChange = { onAction(AppAction.SetQuery(it)) },
                    placeholder = {
                        Text(
                            "Search files & folders",
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { onAction(AppAction.SetQuery("")) }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = getDynamicCornerShape(24f, state.cornerRoundness),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Trash icon in pill container
                Surface(
                    shape = getDynamicCornerShape(14f, state.cornerRoundness),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(onClick = { onAction(AppAction.SetTrashScreenVisible(true)) }) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Trash",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
`


# Operation 11: multi_replace_file_content
Instruction: 1. Add showAboutScreen local state variable in SiftApp. 2. Pass it to DrawerContent. 3. Add AnimatedVisibility overlay for About screen before the closing braces. 4. Update DrawerContent to accept and use showAboutScreen. 5. Wire up About drawer item.
## Chunk 0 Replacement Content:
`kotlin
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val drawerScope = rememberCoroutineScope()
        var showAboutScreen by remember { mutableStateOf(false) }
`
## Chunk 1 Replacement Content:
`kotlin
                DrawerContent(
                    state = state,
                    onAction = onAction,
                    cornerRoundness = state.cornerRoundness,
                    onCloseDrawer = { drawerScope.launch { drawerState.close() } },
                    onShowAbout = { showAboutScreen = true }
                )
`
## Chunk 2 Replacement Content:
`kotlin
        }
    }

            // About Screen Overlay
            AnimatedVisibility(
                visible = showAboutScreen,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                BackHandler(enabled = showAboutScreen) {
                    showAboutScreen = false
                }
                AboutScreen(
                    cornerRoundness = state.cornerRoundness,
                    onClose = { showAboutScreen = false }
                )
            }

    } // end ModalNavigationDrawer
}
`
## Chunk 3 Replacement Content:
`kotlin
fun DrawerContent(
    state: AppState,
    onAction: (AppAction) -> Unit,
    cornerRoundness: Float,
    onCloseDrawer: () -> Unit,
    onShowAbout: () -> Unit = {}
) {
`
## Chunk 4 Replacement Content:
`kotlin
            DrawerMenuItem(
                icon = Icons.Outlined.Info,
                label = "About",
                cornerRoundness = cornerRoundness,
                onClick = {
                    onCloseDrawer()
                    onShowAbout()
                }
            )
`


# Operation 12: replace_file_content
Instruction: Add AboutScreen composable after DrawerMenuItem and before MainContent
## Replacement Content:
`kotlin
        }
    }
}

@Composable
fun AboutScreen(
    cornerRoundness: Float,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top bar with back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    "About",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // App icon / branding
                Surface(
                    shape = getDynamicCornerShape(20f, cornerRoundness),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.size(100.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Ripple Files",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Every file, in flow.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Text(
                    "Version 1.0.12",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // App description card
                Surface(
                    shape = getDynamicCornerShape(16f, cornerRoundness),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "About Ripple Files",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Ripple Files is a modern, powerful, and beautifully designed file manager built for Android. " +
                            "It gives you complete control over your files with an intuitive interface that makes browsing, organizing, " +
                            "and managing files effortless.\n\n" +
                            "Key features include smart file categorization, built-in media viewers for images, music, and documents, " +
                            "Google Drive cloud integration, a storage cleaner to free up space, a secure recycle bin with auto-delete, " +
                            "batch file renaming, zip extraction, and extensive theming options with dynamic colors.\n\n" +
                            "Designed with Material You design language, Ripple Files adapts to your device's theme and offers " +
                            "a premium, fluid experience — making file management not just functional, but enjoyable.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            lineHeight = androidx.compose.ui.unit.TextUnit(22f, androidx.compose.ui.unit.TextUnitType.Sp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Developer card
                Surface(
                    shape = getDynamicCornerShape(16f, cornerRoundness),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Developer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Dev name
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = getDynamicCornerShape(12f, cornerRoundness),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    "GokuCruz",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Lead Developer",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Email
                        Surface(
                            shape = getDynamicCornerShape(12f, cornerRoundness),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = android.net.Uri.parse("mailto:gokulsb009@gmail.com")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // No email app found
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Email,
                                    contentDescription = "Email",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "gokulsb009@gmail.com",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Made with love footer
                Surface(
                    shape = getDynamicCornerShape(16f, cornerRoundness),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            "Made with ❤\uFE0F in India",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "© 2025 GokuCruz. All rights reserved.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
`


# Operation 13: replace_file_content
Instruction: Replace Icons.AutoMirrored.Filled.ArrowBack with Icons.Default.ArrowBack
## Replacement Content:
`kotlin
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
`


# Operation 14: replace_file_content
Instruction: Replace the BottomBar implementation with the new animated version using Animatable.
## Replacement Content:
`kotlin
fun BottomBar(currentLocation: String, onLocationSelected: (String) -> Unit, cornerRoundness: Float, modifier: Modifier = Modifier) {
    val items = listOf(
        "home" to Icons.Default.Home,
        "recent" to Icons.Default.Schedule,
        "starred" to Icons.Default.Star,
        "drive" to Icons.Default.Cloud
    )
    
    val selectedIndex = items.indexOfFirst { (id, _) -> 
        currentLocation == id || (id == "home" && currentLocation == android.os.Environment.getExternalStorageDirectory().absolutePath)
    }.coerceAtLeast(0)
    
    val targetLeft = (selectedIndex * 56 - 2).dp
    val blobLeft = remember { androidx.compose.animation.core.Animatable(targetLeft, androidx.compose.ui.unit.Dp.VectorConverter) }
    val blobWidth = remember { androidx.compose.animation.core.Animatable(52.dp, androidx.compose.ui.unit.Dp.VectorConverter) }
    
    LaunchedEffect(selectedIndex) {
        val oldLeft = blobLeft.value
        val newLeft = targetLeft
        if (oldLeft != newLeft) {
            val stretchWidth = kotlin.math.abs(newLeft.value - oldLeft.value).dp + 52.dp
            val stretchLeft = androidx.compose.ui.unit.min(oldLeft, newLeft)
            
            // Phase 1: Stretch
            launch {
                blobLeft.animateTo(
                    stretchLeft,
                    animationSpec = androidx.compose.animation.core.tween(170, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                )
            }
            blobWidth.animateTo(
                stretchWidth,
                animationSpec = androidx.compose.animation.core.tween(170, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            )
            
            // Phase 2: Settle
            launch {
                blobLeft.animateTo(
                    newLeft,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                    )
                )
            }
            blobWidth.animateTo(
                52.dp,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                )
            )
        }
    }

    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(getDynamicCornerShape(24f, cornerRoundness))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Blob (rendered behind icons)
        Box(
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(blobLeft.value.roundToPx(), 0) }
                .width(blobWidth.value)
                .height(32.dp)
                .clip(getDynamicCornerShape(16f, cornerRoundness))
                .background(MaterialTheme.colorScheme.secondaryContainer)
        )
        
        // Icons row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, (id, icon) ->
                val selected = index == selectedIndex
                val iconColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .width(48.dp)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { onLocationSelected(id) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = id, tint = iconColor, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
`


# Operation 15: multi_replace_file_content
Instruction: Update BottomBar to take `hasCloudAuth: Boolean` and populate items conditionally.
## Chunk 0 Replacement Content:
`kotlin
fun BottomBar(currentLocation: String, hasCloudAuth: Boolean, onLocationSelected: (String) -> Unit, cornerRoundness: Float, modifier: Modifier = Modifier) {
    val items = mutableListOf(
        "home" to Icons.Default.Home,
        "recent" to Icons.Default.Schedule,
        "starred" to Icons.Default.Star
    )
    if (hasCloudAuth) {
        items.add("drive" to Icons.Default.Cloud)
    }
`


# Operation 16: multi_replace_file_content
Instruction: Add onCloudAuthClick callback to DrawerContent and update GDrive, Mega, Dropbox buttons to trigger it.
## Chunk 0 Replacement Content:
`kotlin
fun DrawerContent(
    state: AppState,
    onAction: (AppAction) -> Unit,
    cornerRoundness: Float,
    onCloseDrawer: () -> Unit,
    onShowAbout: () -> Unit = {},
    onCloudAuthClick: (String) -> Unit = {}
) {
`
## Chunk 1 Replacement Content:
`kotlin
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
                        ) {
                            // Google Drive
                            Surface(
                                shape = getDynamicCornerShape(12f, cornerRoundness),
                                color = if (state.isGoogleDriveAuthenticated) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                                onClick = {
                                    onCloseDrawer()
                                    onCloudAuthClick("drive")
                                },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize().padding(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CloudDone,
                                        contentDescription = "Google Drive",
                                        tint = if (state.isGoogleDriveAuthenticated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        "GDrive",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (state.isGoogleDriveAuthenticated) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        maxLines = 1
                                    )
                                }
                            }

                            // Mega
                            Surface(
                                shape = getDynamicCornerShape(12f, cornerRoundness),
                                color = if (state.isMegaAuthenticated) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                                onClick = {
                                    onCloseDrawer()
                                    onCloudAuthClick("mega")
                                },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize().padding(4.dp)
                                ) {
                                    Icon(
                                        if (state.isMegaAuthenticated) Icons.Default.CloudDone else Icons.Default.Cloud,
                                        contentDescription = "Mega",
                                        tint = if (state.isMegaAuthenticated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        "Mega",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (state.isMegaAuthenticated) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        maxLines = 1
                                    )
                                }
                            }

                            // Dropbox
                            Surface(
                                shape = getDynamicCornerShape(12f, cornerRoundness),
                                color = if (state.isDropboxAuthenticated) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                                onClick = {
                                    onCloseDrawer()
                                    onCloudAuthClick("dropbox")
                                },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize().padding(4.dp)
                                ) {
                                    Icon(
                                        if (state.isDropboxAuthenticated) Icons.Default.CloudDone else Icons.Default.Cloud,
                                        contentDescription = "Dropbox",
                                        tint = if (state.isDropboxAuthenticated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        "Dropbox",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (state.isDropboxAuthenticated) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
`


# Operation 17: multi_replace_file_content
Instruction: Add showCloudAuthDialog state and pass onCloudAuthClick to DrawerContent. Add the popup dialog component.
## Chunk 0 Replacement Content:
`kotlin
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val drawerScope = rememberCoroutineScope()
        var showAboutScreen by remember { mutableStateOf(false) }
        var showCloudAuthDialog by remember { mutableStateOf<String?>(null) }
`
## Chunk 1 Replacement Content:
`kotlin
                DrawerContent(
                    state = state,
                    onAction = onAction,
                    cornerRoundness = state.cornerRoundness,
                    onCloseDrawer = { drawerScope.launch { drawerState.close() } },
                    onShowAbout = { showAboutScreen = true },
                    onCloudAuthClick = { showCloudAuthDialog = it }
                )
`
## Chunk 2 Replacement Content:
`kotlin
        }
    }

            // About Screen Overlay
            AnimatedVisibility(
                visible = showAboutScreen,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                BackHandler(enabled = showAboutScreen) {
                    showAboutScreen = false
                }
                AboutScreen(
                    cornerRoundness = state.cornerRoundness,
                    onClose = { showAboutScreen = false }
                )
            }

            if (showCloudAuthDialog != null) {
                val cloudType = showCloudAuthDialog!!
                val isAuth = when (cloudType) {
                    "drive" -> state.isGoogleDriveAuthenticated
                    "mega" -> state.isMegaAuthenticated
                    "dropbox" -> state.isDropboxAuthenticated
                    else -> false
                }
                val cloudName = when (cloudType) {
                    "drive" -> "Google Drive"
                    "mega" -> "Mega"
                    "dropbox" -> "Dropbox"
                    else -> "Cloud"
                }

                androidx.compose.ui.window.Dialog(onDismissRequest = { showCloudAuthDialog = null }) {
                    Surface(
                        shape = getDynamicCornerShape(24f, state.cornerRoundness),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                if (isAuth) Icons.Default.CloudDone else Icons.Default.Cloud,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "$cloudName",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (isAuth) {
                                val email = when (cloudType) {
                                    "drive" -> state.googleDriveAccountEmail
                                    "mega" -> state.megaAccountEmail
                                    "dropbox" -> state.dropboxAccountEmail
                                    else -> null
                                }
                                Text("Signed in as $email", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = {
                                        when (cloudType) {
                                            "drive" -> onAction(AppAction.SetGoogleDriveAuthStatus(false, null))
                                            "mega" -> onAction(AppAction.SetMegaAuthStatus(false, null))
                                            "dropbox" -> onAction(AppAction.SetDropboxAuthStatus(false, null))
                                        }
                                        showCloudAuthDialog = null
                                    },
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Logout")
                                }
                            } else {
                                Text("Not signed in", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = {
                                        when (cloudType) {
                                            "drive" -> {
                                                // Assuming we handle google auth in MainContent, we can trigger it or just set it mock true for now
                                                // Since UI only, we'll mock auth for Mega and Dropbox. Google Drive uses standard intent.
                                                // But actually, we need an action to trigger the intent if possible.
                                                // We'll mock it for Mega/Dropbox.
                                            }
                                            "mega" -> onAction(AppAction.SetMegaAuthStatus(true, "user@mega.nz"))
                                            "dropbox" -> onAction(AppAction.SetDropboxAuthStatus(true, "user@dropbox.com"))
                                        }
                                        showCloudAuthDialog = null
                                    }
                                ) {
                                    Text("Login")
                                }
                            }
                        }
                    }
                }
            }

    } // end ModalNavigationDrawer
}
`


# Operation 18: multi_replace_file_content
Instruction: Update BottomBar call to pass hasCloudAuth. In MainContent, update topBar to show dropdown instead of search if location is drive.
## Chunk 0 Replacement Content:
`kotlin
                            BottomBar(
                                currentLocation = state.location,
                                hasCloudAuth = state.isGoogleDriveAuthenticated || state.isMegaAuthenticated || state.isDropboxAuthenticated,
                                cornerRoundness = state.cornerRoundness,
                                onLocationSelected = { onAction(AppAction.SetLocation(it)) },
                                modifier = Modifier
                            )
`
## Chunk 1 Replacement Content:
`kotlin
                if (state.location == "drive") {
                    var expanded by remember { mutableStateOf(false) }
                    val availableClouds = mutableListOf<String>()
                    if (state.isGoogleDriveAuthenticated) availableClouds.add("Google Drive")
                    if (state.isMegaAuthenticated) availableClouds.add("Mega")
                    if (state.isDropboxAuthenticated) availableClouds.add("Dropbox")
                    
                    var selectedCloud by remember { mutableStateOf(availableClouds.firstOrNull() ?: "Google Drive") }

                    Box(modifier = Modifier.weight(1f).height(50.dp)) {
                        Surface(
                            shape = getDynamicCornerShape(24f, state.cornerRoundness),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                val icon = when(selectedCloud) {
                                    "Google Drive" -> Icons.Default.CloudDone
                                    "Mega" -> Icons.Default.Cloud
                                    "Dropbox" -> Icons.Default.CloudQueue
                                    else -> Icons.Default.Cloud
                                }
                                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(selectedCloud, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            availableClouds.forEach { cloud ->
                                DropdownMenuItem(
                                    text = { Text(cloud) },
                                    onClick = { 
                                        selectedCloud = cloud
                                        expanded = false 
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Full-width search bar
                    TextField(
                        value = state.query,
                        onValueChange = { onAction(AppAction.SetQuery(it)) },
                        placeholder = {
                            Text(
                                "Search files & folders",
                                maxLines = 1,
                                softWrap = false,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        },
                        trailingIcon = {
                            if (state.query.isNotEmpty()) {
                                IconButton(onClick = { onAction(AppAction.SetQuery("")) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = getDynamicCornerShape(24f, state.cornerRoundness),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge
                    )
                }


`


# Operation 19: multi_replace_file_content
Instruction: Make UI changes as requested by user.
## Chunk 0 Replacement Content:
`kotlin
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { showCloudAuthDialog = null },
                    properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(
                        shape = getDynamicCornerShape(24f, state.cornerRoundness),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth(0.9f),
                        tonalElevation = 8.dp
                    ) {
`
## Chunk 1 Replacement Content:
`kotlin
                // Full-width search bar
                TextField(
                    value = state.query,
                    onValueChange = { onAction(AppAction.SetQuery(it)) },
                    placeholder = {
                        Text(
                            "Search files & folders",
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { onAction(AppAction.SetQuery("")) }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = getDynamicCornerShape(24f, state.cornerRoundness),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge
                )
`
## Chunk 2 Replacement Content:
`kotlin
                        if (state.location.startsWith("drive")) {
                            var expanded by remember { mutableStateOf(false) }
                            val availableClouds = mutableListOf<String>()
                            if (state.isGoogleDriveAuthenticated) availableClouds.add("Google Drive")
                            if (state.isMegaAuthenticated) availableClouds.add("Mega")
                            if (state.isDropboxAuthenticated) availableClouds.add("Dropbox")
                            var selectedCloud by remember { mutableStateOf(availableClouds.firstOrNull() ?: "Google Drive") }

                            Box {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(getDynamicCornerShape(12f, state.cornerRoundness))
                                        .clickable { expanded = true }
                                        .padding(horizontal = 4.dp, vertical = 4.dp)
                                ) {
                                    val icon = when(selectedCloud) {
                                        "Google Drive" -> Icons.Default.CloudDone
                                        "Mega" -> Icons.Default.Cloud
                                        "Dropbox" -> Icons.Default.CloudQueue
                                        else -> Icons.Default.Cloud
                                    }
                                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = selectedCloud, 
                                        style = MaterialTheme.typography.labelLarge, 
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                }
                                androidx.compose.material3.DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    availableClouds.forEach { cloud ->
                                        androidx.compose.material3.DropdownMenuItem(
                                            text = { Text(cloud) },
                                            onClick = { 
                                                selectedCloud = cloud
                                                expanded = false 
                                            }
                                        )
                                    }
                                }
                            }
                            if (state.location != "drive") {
                                Text(" / ", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                Text("Folder", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
`


# Operation 20: multi_replace_file_content
Instruction: Add custom login UI for Mega in the cloud auth dialog.
## Chunk 0 Replacement Content:
`kotlin
                            if (isAuth) {
                                val email = when (cloudType) {
                                    "drive" -> state.googleDriveAccountEmail
                                    "mega" -> state.megaAccountEmail
                                    "dropbox" -> state.dropboxAccountEmail
                                    else -> null
                                }
                                Text("Signed in as $email", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = {
                                        when (cloudType) {
                                            "drive" -> onAction(AppAction.SetGoogleDriveAuthStatus(false, null))
                                            "mega" -> onAction(AppAction.SetMegaAuthStatus(false, null))
                                            "dropbox" -> onAction(AppAction.SetDropboxAuthStatus(false, null))
                                        }
                                        showCloudAuthDialog = null
                                    },
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Logout")
                                }
                            } else {
                                if (cloudType == "mega") {
                                    var megaEmail by remember { mutableStateOf("") }
                                    var megaPassword by remember { mutableStateOf("") }

                                    TextField(
                                        value = megaEmail,
                                        onValueChange = { megaEmail = it },
                                        placeholder = { Text("Email ID") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextField(
                                        value = megaPassword,
                                        onValueChange = { megaPassword = it },
                                        placeholder = { Text("Password") },
                                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            onAction(AppAction.SetMegaAuthStatus(true, megaEmail.ifEmpty { "user@mega.nz" }))
                                            showCloudAuthDialog = null
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Sign In")
                                    }
                                    androidx.compose.material3.TextButton(onClick = { /* Forgot Password */ }) {
                                        Text("Forgot password?")
                                    }
                                } else {
                                    Text("Not signed in", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick = {
                                            when (cloudType) {
                                                "drive" -> { }
                                                "dropbox" -> onAction(AppAction.SetDropboxAuthStatus(true, "user@dropbox.com"))
                                            }
                                            showCloudAuthDialog = null
                                        }
                                    ) {
                                        Text("Login")
                                    }
                                }
                            }
                        }
`


# Operation 21: replace_file_content
Instruction: Replace NavigateBackInDrive with NavigateBackInCloud
## Replacement Content:
`kotlin
            } else if (state.location.startsWith("drive") || state.location.startsWith("mega") || state.location.startsWith("dropbox")) {
                onAction(AppAction.NavigateBackInCloud)
            } else if (state.location != "home") {
`


# Operation 22: multi_replace_file_content
Instruction: When clicking a folder, check if it's a cloud folder and use AppAction.RemoteNavigate.
## Chunk 0 Replacement Content:
`kotlin
                            if (file.type == "folder") {
                                if (file.path.startsWith("drive_id:")) {
                                    onAction(AppAction.RemoteNavigate(CloudProvider.DRIVE, file.path.removePrefix("drive_id:"), file.name))
                                } else if (file.path.startsWith("mega_id:")) {
                                    onAction(AppAction.RemoteNavigate(CloudProvider.MEGA, file.path.removePrefix("mega_id:"), file.name))
                                } else if (file.path.startsWith("dropbox_id:")) {
                                    onAction(AppAction.RemoteNavigate(CloudProvider.DROPBOX, file.path.removePrefix("dropbox_id:"), file.name))
                                } else {
                                    onAction(AppAction.SetLocation(file.path, file.name))
                                }
`


# Operation 23: replace_file_content
Instruction: Add CloudProvider import to SiftApp
## Replacement Content:
`kotlin
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.ripple.filemanager.CloudProvider
`


# Operation 24: replace_file_content
Instruction: Add CloudProvider import to SiftApp
## Replacement Content:
`kotlin
import com.ripple.filemanager.FileItem
import com.ripple.filemanager.FileDetails
import com.ripple.filemanager.CloudProvider
`


# Operation 25: multi_replace_file_content
Instruction: Replace Starred with Pinned, use PushPin icon. Update corners for hamburger and trash. Add gap to Linked Clouds. Add dropdown arrow and menu for search bar.
## Chunk 0 Replacement Content:
`kotlin
                    AnimatedVisibility(visible = isCloudExpanded) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
                        ) {
`
## Chunk 1 Replacement Content:
`kotlin
                // Hamburger menu icon in pill container
                Surface(
                    shape = getDynamicCornerShape(24f, state.cornerRoundness),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
`
## Chunk 2 Replacement Content:
`kotlin
                // Full-width search bar
                var searchExpanded by remember { mutableStateOf(false) }
                val recentSearches = remember { listOf("Downloads", "Pictures/Screenshots", "Documents/PDFs") }
                
                Box(modifier = Modifier.weight(1f)) {
                    TextField(
                        value = state.query,
                        onValueChange = { onAction(AppAction.SetQuery(it)) },
                        placeholder = {
                            Text(
                                "Search files & folders",
                                maxLines = 1,
                                softWrap = false,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (state.query.isNotEmpty()) {
                                    IconButton(onClick = { onAction(AppAction.SetQuery("")) }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                                IconButton(onClick = { searchExpanded = !searchExpanded }) {
                                    Icon(
                                        if (searchExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Recent Searches"
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = getDynamicCornerShape(24f, state.cornerRoundness),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge
                    )
                    
                    androidx.compose.material3.DropdownMenu(
                        expanded = searchExpanded,
                        onDismissRequest = { searchExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.6f)
                    ) {
                        Text(
                            text = "Recent Searches",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        recentSearches.forEach { recent ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(recent) },
                                onClick = {
                                    onAction(AppAction.SetQuery(recent))
                                    searchExpanded = false
                                },
                                leadingIcon = { Icon(Icons.Default.History, contentDescription = null) }
                            )
                        }
                    }
                }
`
## Chunk 3 Replacement Content:
`kotlin
                // Trash icon in pill container
                Surface(
                    shape = getDynamicCornerShape(24f, state.cornerRoundness),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
`
## Chunk 4 Replacement Content:
`kotlin
                        NavButton("Pinned", "starred", Icons.Default.PushPin, currentLocation, cornerRoundness, onLocationSelected)
`
## Chunk 5 Replacement Content:
`kotlin
                            "starred" to Icons.Default.PushPin,
`


# Operation 26: replace_file_content
Instruction: Replace Icons.Default.Star with Icons.Default.PushPin
## Replacement Content:
`kotlin
                            "starred" to Icons.Default.PushPin,
`


# Operation 27: multi_replace_file_content
Instruction: Revert PushPin to Search in TextField leading icon, and change Star to PushPin in NavButton mapping
## Chunk 0 Replacement Content:
`kotlin
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        },
`
## Chunk 1 Replacement Content:
`kotlin
                            "starred" to Icons.Default.PushPin,
`


# Operation 28: replace_file_content
Instruction: Change Star to PushPin in Nav mapping
## Replacement Content:
`kotlin
                            "starred" to Icons.Default.PushPin,
`


# Operation 29: replace_file_content
Instruction: Update starred mapping
## Replacement Content:
`kotlin
        "starred" to Icons.Default.PushPin
`


# Operation 30: multi_replace_file_content
Instruction: Add clear focus on outside tap and back gesture. Also, wire the dropdown to state.recentSearches.
## Chunk 0 Replacement Content:
`kotlin
    val focusManager = LocalFocusManager.current
    var isSearchFocused by remember { mutableStateOf(false) }

    BackHandler(enabled = isSearchFocused) {
        focusManager.clearFocus()
    }

    Scaffold(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
`
## Chunk 1 Replacement Content:
`kotlin
                // Full-width search bar
                var searchExpanded by remember { mutableStateOf(false) }
`
## Chunk 2 Replacement Content:
`kotlin
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .onFocusChanged { isSearchFocused = it.isFocused },
                        shape = getDynamicCornerShape(24f, state.cornerRoundness),
`
## Chunk 3 Replacement Content:
`kotlin
                        if (state.recentSearches.isEmpty()) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("No recent searches", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                onClick = { searchExpanded = false }
                            )
                        } else {
                            state.recentSearches.forEach { recent ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(recent.name) },
                                    onClick = {
                                        if (recent.isDirectory) {
                                            onAction(AppAction.SetLocation(recent.path, recent.name))
                                        } else {
                                            onAction(AppAction.OpenFileViewerByPath(recent.path))
                                        }
                                        searchExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (recent.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                        }
`


# Operation 31: replace_file_content
Instruction: Remove leftover lines from previous DropdownMenuItem
## Replacement Content:
`kotlin
                        }
                    }
                }
            }
`


# Operation 32: multi_replace_file_content
Instruction: Add LocalFocusManager, pointerInput, detectTapGestures, and onFocusChanged imports
## Chunk 0 Replacement Content:
`kotlin
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
`
## Chunk 1 Replacement Content:
`kotlin
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.viewinterop.AndroidView
`


# Operation 33: replace_file_content
Instruction: Update onClick to use LoginToMega
## Replacement Content:
`kotlin
                                    Button(
                                        onClick = {
                                            onAction(AppAction.LoginToMega(megaEmail, megaPassword))
                                            showCloudAuthDialog = null
                                        },
                                        modifier = Modifier.fillMaxWidth()
`


# Operation 34: replace_file_content
Instruction: Replace Google Drive card with VerticalPager for both providers
## Replacement Content:
`kotlin
                        if (state.isGoogleDriveAuthenticated || state.isMegaAuthenticated) {
                            val cloudProviders = mutableListOf<String>()
                            if (state.isGoogleDriveAuthenticated) cloudProviders.add("drive")
                            if (state.isMegaAuthenticated) cloudProviders.add("mega")

                            val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { cloudProviders.size })
                            androidx.compose.foundation.pager.VerticalPager(
                                state = pagerState,
                                modifier = cardModifier.height(110.dp)
                            ) { page ->
                                val provider = cloudProviders[page]
                                if (provider == "drive") {
                                    val driveUsedText = formatSize(state.driveStorageTotalBytes - state.driveStorageFreeBytes)
                                    val driveTotalText = formatSize(state.driveStorageTotalBytes)
                                    val driveFreeText = "${formatSize(state.driveStorageFreeBytes)} free"
                                    val driveProgress = if (state.driveStorageTotalBytes > 0L) ((state.driveStorageTotalBytes - state.driveStorageFreeBytes).toFloat() / state.driveStorageTotalBytes.toFloat()) else 0f
                                    
                                    StorageCard(
                                        modifier = Modifier.fillMaxSize(),
                                        icon = Icons.Default.Cloud,
                                        titleText = "GDrive",
                                        usedText = driveUsedText,
                                        totalText = driveTotalText,
                                        freeText = driveFreeText,
                                        progress = driveProgress,
                                        cornerRoundness = state.cornerRoundness,
                                        onClick = { /* Navigate to drive */ }
                                    )
                                } else if (provider == "mega") {
                                    val megaUsedText = formatSize(state.megaStorageTotalBytes - state.megaStorageFreeBytes)
                                    val megaTotalText = formatSize(state.megaStorageTotalBytes)
                                    val megaFreeText = "${formatSize(state.megaStorageFreeBytes)} free"
                                    val megaProgress = if (state.megaStorageTotalBytes > 0L) ((state.megaStorageTotalBytes - state.megaStorageFreeBytes).toFloat() / state.megaStorageTotalBytes.toFloat()) else 0f
                                    
                                    StorageCard(
                                        modifier = Modifier.fillMaxSize(),
                                        icon = Icons.Default.Cloud,
                                        titleText = "MEGA",
                                        usedText = megaUsedText,
                                        totalText = megaTotalText,
                                        freeText = megaFreeText,
                                        progress = megaProgress,
                                        cornerRoundness = state.cornerRoundness,
                                        onClick = { /* Navigate to mega */ }
                                    )
                                }
                            }
                        }
`


# Operation 35: multi_replace_file_content
Instruction: Update BottomBar and TopBar cloud dropdown to properly navigate and display the correct cloud provider
## Chunk 0 Replacement Content:
`kotlin
                        if (state.location.startsWith("drive") || state.location.startsWith("mega") || state.location.startsWith("dropbox")) {
                            var expanded by remember { mutableStateOf(false) }
                            val availableClouds = mutableListOf<String>()
                            if (state.isGoogleDriveAuthenticated) availableClouds.add("Google Drive")
                            if (state.isMegaAuthenticated) availableClouds.add("Mega")
                            if (state.isDropboxAuthenticated) availableClouds.add("Dropbox")
                            
                            val currentCloudName = when {
                                state.location.startsWith("mega") -> "Mega"
                                state.location.startsWith("dropbox") -> "Dropbox"
                                else -> "Google Drive"
                            }

                            Box {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(getDynamicCornerShape(12f, state.cornerRoundness))
                                        .clickable { expanded = true }
                                        .padding(horizontal = 4.dp, vertical = 4.dp)
                                ) {
                                    val icon = when(currentCloudName) {
                                        "Google Drive" -> Icons.Default.CloudDone
                                        "Mega" -> Icons.Default.Cloud
                                        "Dropbox" -> Icons.Default.CloudQueue
                                        else -> Icons.Default.Cloud
                                    }
                                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = currentCloudName, 
                                        style = MaterialTheme.typography.labelLarge, 
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                }
                                androidx.compose.material3.DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    availableClouds.forEach { cloud ->
                                        androidx.compose.material3.DropdownMenuItem(
                                            text = { Text(cloud) },
                                            onClick = { 
                                                expanded = false 
                                                val newLoc = when(cloud) {
                                                    "Mega" -> "mega"
                                                    "Dropbox" -> "dropbox"
                                                    else -> "drive"
                                                }
                                                if (!state.location.startsWith(newLoc)) {
                                                    onAction(AppAction.SetLocation(newLoc))
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            if (state.location != "drive" && state.location != "mega" && state.location != "dropbox") {
                                Text(" / ", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                Text(state.location.split("/").lastOrNull() ?: "Folder", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        } else {
`
## Chunk 1 Replacement Content:
`kotlin
                            val firstCloud = if (state.isGoogleDriveAuthenticated) "drive" else if (state.isMegaAuthenticated) "mega" else if (state.isDropboxAuthenticated) "dropbox" else "drive"
                            BottomBar(
                                currentLocation = state.location,
                                hasCloudAuth = state.isGoogleDriveAuthenticated || state.isMegaAuthenticated || state.isDropboxAuthenticated,
                                firstCloudId = firstCloud,
                                cornerRoundness = state.cornerRoundness,
                                onLocationSelected = { onAction(AppAction.SetLocation(it)) },
                                modifier = Modifier
                            )
`
## Chunk 2 Replacement Content:
`kotlin
fun BottomBar(currentLocation: String, hasCloudAuth: Boolean, firstCloudId: String, onLocationSelected: (String) -> Unit, cornerRoundness: Float, modifier: Modifier = Modifier) {
    val items = mutableListOf(
        "home" to Icons.Default.Home,
        "recent" to Icons.Default.Schedule,
        "starred" to Icons.Default.PushPin
    )
    if (hasCloudAuth) {
        items.add("cloud" to Icons.Default.Cloud)
    }
    
    val isCloudLoc = currentLocation.startsWith("drive") || currentLocation.startsWith("mega") || currentLocation.startsWith("dropbox")
    
    val selectedIndex = items.indexOfFirst { (id, _) -> 
        currentLocation == id || 
        (id == "home" && currentLocation == android.os.Environment.getExternalStorageDirectory().absolutePath) || 
        (id == "cloud" && isCloudLoc)
    }.coerceAtLeast(0)
`
## Chunk 3 Replacement Content:
`kotlin
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { onLocationSelected(if (id == "cloud") firstCloudId else id) },
`


# Operation 36: replace_file_content
Instruction: Remove duplicate clickable
## Replacement Content:
`kotlin
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { onLocationSelected(if (id == "cloud") firstCloudId else id) },
`


# Operation 37: multi_replace_file_content
Instruction: Give all StorageCards, including the VerticalPager, a uniform fixed height of 100dp so they match the local storage card height
## Chunk 0 Replacement Content:
`kotlin
                        val cardModifier = if (isScrollable) Modifier.width(180.dp).height(100.dp) else Modifier.weight(1f).height(100.dp)


`
## Chunk 1 Replacement Content:
`kotlin
                            androidx.compose.foundation.pager.VerticalPager(
                                state = pagerState,
                                modifier = cardModifier
                            ) { page ->
`
