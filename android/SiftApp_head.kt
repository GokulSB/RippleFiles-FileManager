package com.sift.filemanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sift.filemanager.FileItem
import com.sift.filemanager.FileDetails
import com.sift.filemanager.MainViewModel
import com.sift.filemanager.SortMode
import com.sift.filemanager.ui.theme.SiftTheme
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import android.os.Environment
import java.io.File
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.AnnotatedString

@Composable
fun SiftApp(
    windowWidthSizeClass: WindowWidthSizeClass,
    viewModel: MainViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    
    val isWideScreen = windowWidthSizeClass != WindowWidthSizeClass.Compact

    BackHandler(enabled = state.isSelectionMode) {
        viewModel.clearSelection()
    }

    SiftTheme(
        darkTheme = when (state.themeMode) {
            com.sift.filemanager.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            com.sift.filemanager.ThemeMode.DARK -> true
            com.sift.filemanager.ThemeMode.LIGHT -> false
        },
        dynamicColor = state.useDynamicSystemTheme,
        customHue = state.themeHue
    ) {
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
            if (isWideScreen) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Sidebar(
                        currentLocation = state.location,
                        onLocationSelected = viewModel::setLocation,
                        modifier = Modifier.width(280.dp)
                    )
                    MainContent(
                        viewModel = viewModel,
                        modifier = Modifier.weight(1f)
                    )
                    if (state.selectedFiles.isNotEmpty()) {
                        val file = state.files.find { it.id == state.selectedFiles.first() }
                        DetailsPane(
                            file = file,
                            onClose = viewModel::clearSelection,
                            modifier = Modifier.width(320.dp).background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    MainContent(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (!state.isSelectionMode) {
                        BottomBar(
                            currentLocation = state.location,
                            onLocationSelected = viewModel::setLocation,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }

            if (state.showThemeSheet) {
                ThemeBottomSheet(
                    currentMode = state.themeMode,
                    currentHue = state.themeHue,
                    useDynamicTheme = state.useDynamicSystemTheme,
                    onModeChange = viewModel::setThemeMode,
                    onHueChange = viewModel::setThemeHue,
                    onDynamicThemeChange = viewModel::setDynamicSystemTheme,
                    currentIconShape = state.iconShapeSetting,
                    onIconShapeChange = viewModel::setIconShape,
                    onDismiss = { viewModel.setShowThemeSheet(false) }
                )
            }

            if (state.showBatchRenameDialog) {
                val firstFile = state.files.find { it.id == state.selectedFiles.firstOrNull() }
                val initialName = firstFile?.name?.substringBeforeLast(".") ?: ""
                val initialExt = if (firstFile?.name?.contains(".") == true) firstFile.name.substringAfterLast(".") else ""
                BatchRenameDialog(
                    initialBaseName = initialName,
                    initialExtension = initialExt,
                    onDismiss = { viewModel.setShowBatchRenameDialog(false) },
                    onRename = { base, ext, pad, start, isPrefix ->
                        viewModel.batchRenameFiles(base, ext, pad, start, isPrefix)
                    }
                )
            }

            AnimatedVisibility(
                visible = state.viewingFile != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                if (state.viewingFile != null) {
                    FileViewerScreen(
                        fileItem = state.viewingFile!!,
                        onClose = { viewModel.closeFileViewer() }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visible = state.clipboardPaths.isNotEmpty(),
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = if (isWideScreen) 24.dp else 100.dp, end = 24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        SmallFloatingActionButton(
                            onClick = { viewModel.clearClipboard() },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        FloatingActionButton(
                            onClick = { viewModel.pasteClipboard(state.location) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(Icons.Outlined.ContentPaste, contentDescription = "Paste")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var infoDialogFile by remember { mutableStateOf<FileItem?>(null) }
    var fileDetails by remember { mutableStateOf<FileDetails?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            if (account != null) {
                val hasReadScope = account.grantedScopes.any { it.scopeUri == com.google.api.services.drive.DriveScopes.DRIVE_READONLY }
                if (hasReadScope) {
                    viewModel.setGoogleDriveAuthStatus(true, account.email)
                    viewModel.reload()
                } else {
                    viewModel.setErrorMessage("Permission Denied: You MUST check the box to allow Sift to view your Google Drive files. Please try signing in again.")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(infoDialogFile) {
        if (infoDialogFile != null) {
            fileDetails = viewModel.getFileDetails(infoDialogFile!!.path)
        } else {
            fileDetails = null
        }
    }
    
    BackHandler(enabled = state.selectedFiles.isNotEmpty()) {
        viewModel.clearSelection()
    }

    BackHandler(enabled = state.selectedFiles.isEmpty() && state.location != "home" && state.location != "recent" && state.location != "drive" && state.location != "starred" && state.location != "trash" && state.location.startsWith("/")) {
        val parent = File(state.location).parent
        val rootPath = Environment.getExternalStorageDirectory().absolutePath
        if (parent != null && parent.length >= rootPath.length) {
            viewModel.setLocation(parent)
        } else {
            viewModel.setLocation("home")
        }
    }
    
    val files = state.files.filter { 
        val filterMatch = state.filter == "all" || it.kind == state.filter || (state.filter == "media" && it.kind == "media")
        val searchMatch = it.name.contains(state.query, ignoreCase = true)
        filterMatch && searchMatch
    }.sortedWith(
        when (state.sortMode) {
            SortMode.ALPHABETICAL -> compareBy<FileItem> { it.type != "folder" }.thenBy { it.name.lowercase() }
            SortMode.DATE -> compareBy<FileItem> { it.type != "folder" }.thenByDescending { it.lastModified }
            SortMode.SIZE -> compareBy<FileItem> { it.type != "folder" }.thenByDescending { it.sizeBytes }
        }
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 24.dp, vertical = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Good evening", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            when(state.location) {
                                "home" -> "Home files"
                                "recent" -> "Recent files"
                                "starred" -> "Starred files"
                                "drive" -> "Drive files"
                                "trash" -> "Trash"
                                else -> "Files"
                            },
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box {
                            IconButton(onClick = { showSortMenu = true }, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)) {
                                Icon(Icons.Default.Sort, contentDescription = "Sort")
                            }
                            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Date") },
                                    onClick = { viewModel.setSortMode(SortMode.DATE); showSortMenu = false },
                                    leadingIcon = { if (state.sortMode == SortMode.DATE) Icon(Icons.Default.Check, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Alphabetical") },
                                    onClick = { viewModel.setSortMode(SortMode.ALPHABETICAL); showSortMenu = false },
                                    leadingIcon = { if (state.sortMode == SortMode.ALPHABETICAL) Icon(Icons.Default.Check, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Size") },
                                    onClick = { viewModel.setSortMode(SortMode.SIZE); showSortMenu = false },
                                    leadingIcon = { if (state.sortMode == SortMode.SIZE) Icon(Icons.Default.Check, null) }
                                )
                            }
                        }
                        IconButton(onClick = { viewModel.toggleViewMode() }, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)) {
                            Icon(if (state.isListMode) Icons.Default.GridView else Icons.Default.ViewList, contentDescription = "Toggle View")
                        }
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            tooltip = { PlainTooltip { Text("Appearance") } },
                            state = rememberTooltipState()
                        ) {
                            IconButton(onClick = { viewModel.setShowThemeSheet(true) }, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)) {
                                Icon(Icons.Default.Palette, contentDescription = "Theme")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isSearchExpanded) Arrangement.spacedBy(16.dp) else Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSearchExpanded) {
                        TextField(
                            value = state.query,
                            onValueChange = viewModel::setQuery,
                            placeholder = { Text("Search", maxLines = 1, softWrap = false) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = { 
                                IconButton(onClick = { 
                                    isSearchExpanded = false
                                    viewModel.setQuery("")
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = CircleShape,
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        )
                    } else {
                        IconButton(onClick = { isSearchExpanded = true }, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                                .padding(4.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("all" to "All", "folder" to "Folders", "media" to "Media", "doc" to "Docs").forEach { (id, label) ->
                                val active = state.filter == id
                                Box(
                                    modifier = Modifier
                                        .height(32.dp)
                                        .clip(CircleShape)
                                        .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { viewModel.setFilter(id) }
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(horizontal = 24.dp)) {
            if (state.storageTotalGb > 0f && !state.location.startsWith("drive")) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SdStorage, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Internal storage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.weight(1f))
                            val freeStr = "%.1f".format(state.storageFreeGb)
                            val totalStr = "%.1f".format(state.storageTotalGb)
                            Text("$freeStr GB free of $totalStr GB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        val progress = if (state.storageTotalGb > 0) ((state.storageTotalGb - state.storageFreeGb) / state.storageTotalGb) else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    }
                }
            }

            val rootPath = Environment.getExternalStorageDirectory().absolutePath
            if (state.location.startsWith("/") && state.location.length > rootPath.length) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.SdStorage, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Internal storage", 
                        style = MaterialTheme.typography.labelLarge, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { viewModel.setLocation(rootPath) }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    
                    val relativePath = state.location.removePrefix(rootPath).removePrefix("/")
                    if (relativePath.isNotEmpty()) {
                        val segments = relativePath.split("/")
                        var currentPath = rootPath
                        segments.forEachIndexed { index, segment ->
                            Text(" / ", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            currentPath = "$currentPath/$segment"
                            val pathForClick = currentPath
                            Text(
                                text = segment, 
                                style = MaterialTheme.typography.labelLarge, 
                                fontWeight = if (index == segments.size - 1) FontWeight.Bold else null,
                                color = if (index == segments.size - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { viewModel.setLocation(pathForClick) }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            } else if (!state.location.startsWith("/")) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.location.startsWith("drive")) {
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Google Drive", 
                            style = MaterialTheme.typography.labelLarge, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { viewModel.setLocation("drive") }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                        if (state.location != "drive") {
                            Text(" / ", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Text("Folder", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Icon(Icons.Default.SdStorage, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Internal storage", 
                            style = MaterialTheme.typography.labelLarge, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(" / ", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        val displayLoc = state.location.replaceFirstChar { it.uppercase() }
                        Text(displayLoc, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (state.location == "drive" && !state.isGoogleDriveAuthenticated) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Cloud, contentDescription = "Drive", modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Google Drive", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Sign in to access your cloud files securely.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestEmail()
                                .requestScopes(Scope(DriveScopes.DRIVE_FILE), Scope(DriveScopes.DRIVE_READONLY))
                                .build()
                            val googleSignInClient = GoogleSignIn.getClient(context, gso)
                            googleSignInClient.signOut().addOnCompleteListener {
                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text("Sign in to Google Drive", fontWeight = FontWeight.Bold)
                    }
                }
            } else if (state.errorMessage != null) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = "Error", modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Oops! Something went wrong", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(state.errorMessage ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(onClick = { viewModel.reload() }) {
                        Text("Retry")
                    }
                }
            } else if (files.isEmpty() && !state.isLoading) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.FolderOff, contentDescription = "Empty", modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No files found", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                FileGrid(
                    files = files,
                    selectedFiles = state.selectedFiles,
                    isListMode = state.isListMode,
                    iconShape = state.activeIconShape,
                    onFileClick = { file -> 
                        if (state.isSelectionMode) {
                            viewModel.toggleSelection(file.id)
                        } else {
                            if (file.type == "folder") {
                                viewModel.setLocation(file.path)
                            } else if (file.name.endsWith(".pdf", ignoreCase = true) || file.type == "image" || file.type == "doc" || listOf(".txt", ".json", ".md", ".csv", ".xml", ".log", ".kt", ".java", ".py", ".html").any { file.name.endsWith(it, ignoreCase = true) }) {
                                viewModel.openFileViewer(file.id)
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
                            } else if (file.type == "video" || file.type == "audio") {
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(file.path))
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, if (file.type == "video") "video/*" else "audio/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            } else if (file.name.endsWith(".apk", ignoreCase = true)) {
                                try {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(file.path))
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "application/vnd.android.package-archive")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    android.widget.Toast.makeText(context, "Error opening APK: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    onFileLongClick = { file ->
                        viewModel.toggleSelection(file.id)
                    },
                    onPinClick = { file -> viewModel.togglePin(file.path) },
                    onInfoClick = { file -> infoDialogFile = file },
                    onRenameClick = { file, newName -> viewModel.renameFile(file, newName) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Box(modifier = Modifier.fillMaxSize().padding(bottom = 24.dp), contentAlignment = Alignment.BottomCenter) {
            AnimatedVisibility(
                visible = state.isSelectionMode,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BadgedBox(
                        badge = {
                            if (state.selectedFiles.isNotEmpty()) {
                                Badge { Text(state.selectedFiles.size.toString()) }
                            }
                        }
                    ) {
                        IconButton(onClick = { viewModel.setShowBatchRenameDialog(true) }, modifier = Modifier.size(42.dp)) { Icon(Icons.Outlined.Edit, contentDescription = "Batch Rename", tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                    }
                    IconButton(onClick = { viewModel.selectAll() }, modifier = Modifier.size(42.dp)) { Icon(Icons.Default.SelectAll, contentDescription = "Select All", tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                        IconButton(onClick = { viewModel.selectNone() }, modifier = Modifier.size(42.dp)) { Icon(Icons.Default.Deselect, contentDescription = "Select None", tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                        IconButton(onClick = {
                            val uris = state.selectedFiles.mapNotNull { id ->
                                state.files.find { it.id == id }?.path?.let { path ->
                                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(path))
                                }
                            }
                            if (uris.isNotEmpty()) {
                                val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                    type = "*/*"
                                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share files"))
                            }
                        }, modifier = Modifier.size(42.dp)) { Icon(Icons.Outlined.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                        IconButton(onClick = { viewModel.setClipboard("copy") }, modifier = Modifier.size(42.dp)) { Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                        IconButton(onClick = { viewModel.setClipboard("cut") }, modifier = Modifier.size(42.dp)) { Icon(Icons.Outlined.ContentCut, contentDescription = "Cut", tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                        IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(42.dp)) { Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                }
            }
        }
        
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete selected item(s)?") },
                text = { Text("This action cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteSelectedFiles()
                        showDeleteConfirm = false
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
                }
            )
        }
        
        if (infoDialogFile != null) {
            AlertDialog(
                onDismissRequest = { infoDialogFile = null },
                title = { Text(infoDialogFile?.name ?: "Info") },
                text = {
                    if (fileDetails == null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Calculating size...", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Type: ${if (fileDetails!!.isFolder) "Folder" else "File"}")
                            Text("Size: ${fileDetails!!.size}")
                            if (fileDetails!!.itemCount != null) Text("Items: ${fileDetails!!.itemCount}")
                            Text("Modified: ${fileDetails!!.changed}")
                            Text("Owner: ${fileDetails!!.owner}")
                            if (fileDetails!!.format != null) Text("Format: ${fileDetails!!.format}")
                            if (fileDetails!!.resolution != null) Text("Resolution: ${fileDetails!!.resolution}")
                            if (fileDetails!!.duration != null) Text("Duration: ${fileDetails!!.duration}")
                            Text("Path: ${fileDetails!!.path}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { infoDialogFile = null }) { Text("Close") }
                }
            )
        }

        if (state.isDownloading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Downloading file...", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun Sidebar(currentLocation: String, onLocationSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxHeight().background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f)).windowInsetsPadding(WindowInsets.statusBars).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Box(modifier = Modifier.size(46.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("MFile Manager", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text("Expressive file manager", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create", fontWeight = FontWeight.ExtraBold)
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            NavButton("Home", "home", Icons.Default.Home, currentLocation, onLocationSelected)
            NavButton("Recents", "recent", Icons.Default.Schedule, currentLocation, onLocationSelected)
            NavButton("Starred", "starred", Icons.Default.Star, currentLocation, onLocationSelected)
            NavButton("Drive", "drive", Icons.Default.Cloud, currentLocation, onLocationSelected)
            NavButton("Trash", "trash", Icons.Default.Delete, currentLocation, onLocationSelected)
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(22.dp)).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Device storage", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("64%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(progress = { 0.64f }, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(10.dp).clip(CircleShape))
            Text("164 GB used of 256 GB\nPhotos and videos use the most space.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun NavButton(label: String, id: String, icon: androidx.compose.ui.graphics.vector.ImageVector, currentId: String, onSelect: (String) -> Unit) {
    val active = currentId == id
    val bg = if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val fg = if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = if (active) CircleShape else RoundedCornerShape(18.dp)
    
    Row(
        modifier = Modifier.fillMaxWidth().height(46.dp).clip(shape).background(bg).clickable { onSelect(id) }.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = fg, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BottomBar(currentLocation: String, onLocationSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(bottom = 20.dp)
            .height(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val items = listOf(
            "home" to Icons.Default.Home,
            "recent" to Icons.Default.Schedule,
            "starred" to Icons.Default.Star,
            "drive" to Icons.Default.Cloud
        )
        
        items.forEach { (id, icon) ->
            val selected = currentLocation == id
            val iconColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            val bgColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
            
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(bgColor)
                    .clickable { onLocationSelected(id) },
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun BatchRenameDialog(
    initialBaseName: String,
    initialExtension: String,
    onDismiss: () -> Unit,
    onRename: (String, String, Int, Int, Boolean) -> Unit
) {
    var baseName by remember { mutableStateOf(initialBaseName) }
    var extension by remember { mutableStateOf(initialExtension) }
    var startNumberStr by remember { mutableStateOf("1") }
    var paddingStr by remember { mutableStateOf("0") }
    var isPrefix by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Batch Rename") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val pad = paddingStr.toIntOrNull() ?: 0
                val start = startNumberStr.toIntOrNull() ?: 1
                val numberStr = "0".repeat(pad) + start.toString()
                val prefix = if (isPrefix) "${numberStr}_" else ""
                val suffix = if (!isPrefix) "_${numberStr}" else ""

                OutlinedTextField(
                    value = baseName,
                    onValueChange = { baseName = it },
                    label = { Text("Base Name") },
                    visualTransformation = object : VisualTransformation {
                        override fun filter(text: AnnotatedString): TransformedText {
                            val transformedText = AnnotatedString(prefix + text.text + suffix)
                            val offsetMapping = object : OffsetMapping {
                                override fun originalToTransformed(offset: Int): Int {
                                    return offset + prefix.length
                                }
                                override fun transformedToOriginal(offset: Int): Int {
                                    if (offset < prefix.length) return 0
                                    if (offset > prefix.length + text.length) return text.length
                                    return offset - prefix.length
                                }
                            }
                            return TransformedText(transformedText, offsetMapping)
                        }
                    }
                )
                OutlinedTextField(
                    value = extension,
                    onValueChange = { extension = it },
                    label = { Text("Extension") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = paddingStr,
                        onValueChange = { paddingStr = it },
                        label = { Text("Padding zeroes") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = startNumberStr,
                        onValueChange = { startNumberStr = it },
                        label = { Text("Start Number") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = isPrefix, onClick = { isPrefix = true })
                    Text("Prefix")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = !isPrefix, onClick = { isPrefix = false })
                    Text("Suffix")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val pad = paddingStr.toIntOrNull() ?: 0
                val start = startNumberStr.toIntOrNull() ?: 1
                onRename(baseName, extension, pad, start, isPrefix)
            }) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
