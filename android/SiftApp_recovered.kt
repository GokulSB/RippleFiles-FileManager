package com.ripple.filemanager.ui

import com.ripple.filemanager.FileItem
import com.ripple.filemanager.FileDetails


import androidx.compose.animation.core.*
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue


import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
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
import com.ripple.filemanager.AppAction
import com.ripple.filemanager.AppState
import com.ripple.filemanager.ThemeMode
import com.ripple.filemanager.SortMode
import com.ripple.filemanager.ui.theme.SiftTheme
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
import kotlinx.coroutines.launch
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource

@Composable
fun getDynamicCornerShape(defaultRadius: Float, cornerRoundness: Float): RoundedCornerShape {
    return RoundedCornerShape((defaultRadius * (cornerRoundness * 2)).coerceIn(0f, 100f).dp)
}

@Composable
fun SiftApp(
    state: AppState,
    onAction: (AppAction) -> Unit,
    snackbarHostState: androidx.compose.material3.SnackbarHostState,
    windowWidthSizeClass: androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
) {
    val isWideScreen = windowWidthSizeClass != WindowWidthSizeClass.Compact

    BackHandler(enabled = state.isSelectionMode) {
        onAction(AppAction.ClearSelection)
    }

    val isDark = when (state.themeMode) {
        com.ripple.filemanager.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
        com.ripple.filemanager.ThemeMode.DARK -> true
        com.ripple.filemanager.ThemeMode.LIGHT -> false
    }

    SiftTheme(
        darkTheme = isDark,
        dynamicColor = state.useDynamicSystemTheme,
        customHue = state.themeHue,
        fontStyle = state.fontStyle,
        textDecorations = state.textDecorations,
        mainTextScale = state.mainTextScale,
        subTextScale = state.subTextScale
    ) {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val drawerScope = rememberCoroutineScope()
        var showAboutScreen by remember { mutableStateOf(false) }
        var showCloudAuthDialog by remember { mutableStateOf<String?>(null) }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                DrawerContent(
                    state = state,
                    onAction = onAction,
                    cornerRoundness = state.cornerRoundness,
                    onCloseDrawer = { drawerScope.launch { drawerState.close() } },
                    onShowAbout = { showAboutScreen = true },
                    onCloudAuthClick = { showCloudAuthDialog = it }
                )
            }
        ) {
        Surface(
            color = MaterialTheme.colorScheme.surface, 
            modifier = Modifier.fillMaxSize()
        ) {
            if (isWideScreen) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Sidebar(
                        currentLocation = state.location,
                        cornerRoundness = state.cornerRoundness,
                        onLocationSelected = { onAction(AppAction.SetLocation(it)) },
                        modifier = Modifier.width(280.dp)
                    )
                    MainContent(
                        state = state,
                        onAction = onAction,
                        snackbarHostState = snackbarHostState,
                        onDrawerOpen = { drawerScope.launch { drawerState.open() } },
                        modifier = Modifier.weight(1f)
                    )
                    if (state.selectedFiles.isNotEmpty()) {
                        val file = state.files.find { it.id == state.selectedFiles.first() }
                        DetailsPane(
                            file = file,
                            onClose = { onAction(AppAction.ClearSelection) },
                            modifier = Modifier.width(320.dp).background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    MainContent(
                        state = state,
                        onAction = onAction,
                        snackbarHostState = snackbarHostState,
                        onDrawerOpen = { drawerScope.launch { drawerState.open() } },
                        modifier = Modifier.fillMaxSize()
                    )
                    if (!state.isSelectionMode) {
                          val navBottom = androidx.compose.foundation.layout.WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                          val isThreeButton = navBottom > 24.dp
                          val finalBottomPadding = if (isThreeButton) navBottom + 25.dp else 23.dp
                          Row(
                              modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = finalBottomPadding),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BottomBar(
                                currentLocation = state.location,
                                hasCloudAuth = state.isGoogleDriveAuthenticated || state.isMegaAuthenticated || state.isDropboxAuthenticated,
                                cornerRoundness = state.cornerRoundness,
                                onLocationSelected = { onAction(AppAction.SetLocation(it)) },
                                modifier = Modifier
                            )

                            var showFabMenu by remember { mutableStateOf(false) }
                            var showCreateFolderDialog by remember { mutableStateOf(false) }
                            var showCreateFileDialog by remember { mutableStateOf(false) }
                            var showCleanerIntroDialog by remember { mutableStateOf(false) }
                            val hasClipboardItems = state.clipboardPaths.isNotEmpty()
                            Box {
                                if (hasClipboardItems && state.pasteProgress == null && !state.isPasteComplete) {
                                    Box(modifier = Modifier.align(Alignment.TopCenter).offset(y = (-56).dp)) {
                                        SmallFloatingActionButton(
                                            onClick = { onAction(AppAction.ClearClipboard) },
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            shape = getDynamicCornerShape(12f, state.cornerRoundness)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                                        }
                                    }
                                }
                                if (state.pasteProgress != null) {
                                    Row(modifier = Modifier.align(Alignment.TopCenter).offset(y = (-56).dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SmallFloatingActionButton(
                                            onClick = { onAction(AppAction.TogglePastePause) },
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            shape = getDynamicCornerShape(12f, state.cornerRoundness)
                                        ) {
                                            Icon(if (state.isPastePaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = "Pause/Resume")
                                        }
                                        SmallFloatingActionButton(
                                            onClick = { onAction(AppAction.CancelPaste) },
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                            shape = getDynamicCornerShape(12f, state.cornerRoundness)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Cancel Paste")
                                        }
                                    }
                                }
                                Surface(
                                    shape = getDynamicCornerShape(28f, state.cornerRoundness),
                                    color = if (state.isPasteComplete) androidx.compose.ui.graphics.Color(0xFF4CAF50) else MaterialTheme.colorScheme.primaryContainer,
                                    onClick = { 
                                        if (hasClipboardItems) {
                                            if (state.pasteProgress == null && !state.isPasteComplete) {
                                                onAction(AppAction.PasteClipboard(state.location))
                                            }
                                        } else {
                                            showFabMenu = !showFabMenu 
                                        }
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (state.organiseProgress != null) {
                                            androidx.compose.material3.CircularProgressIndicator(
                                                progress = { state.organiseProgress },
                                                modifier = Modifier.fillMaxSize(0.9f),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                                            )
                                            Text(
                                                "${(state.organiseProgress * 100).toInt()}%",
                                                fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        } else if (hasClipboardItems) {
                                            androidx.compose.animation.AnimatedContent(
                                                targetState = when {
                                                    state.isPasteComplete -> 2
                                                    state.pasteProgress != null -> 1
                                                    else -> 0
                                                },
                                                label = "paste_fab_anim"
                                            ) { target ->
                                                when (target) {
                                                    2 -> Icon(Icons.Default.Check, contentDescription = "Complete", tint = androidx.compose.ui.graphics.Color.White)
                                                    1 -> Text("${((state.pasteProgress ?: 0f) * 100).toInt()}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                                    else -> Icon(Icons.Outlined.ContentPaste, contentDescription = "Paste", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                                }
                                            }
                                        } else {
                                            androidx.compose.animation.Crossfade(
                                                targetState = showFabMenu,
                                                label = "FabIcon"
                                            ) { isExpanded ->
                                                Icon(
                                                    imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Add,
                                                    contentDescription = if (isExpanded) "Close" else "Add",
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }
                                }
                                if (showFabMenu) {
                                    val context = androidx.compose.ui.platform.LocalContext.current
                                    val density = androidx.compose.ui.platform.LocalDensity.current
                                    val yOffset = with(density) { (-52).dp.roundToPx() }
                                    androidx.compose.ui.window.Popup(
                                        alignment = Alignment.BottomCenter,
                                        offset = androidx.compose.ui.unit.IntOffset(with(density) { (-24).dp.roundToPx() }, yOffset),
                                        onDismissRequest = { showFabMenu = false },
                                        properties = androidx.compose.ui.window.PopupProperties(focusable = true)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {

                                            FabMenuItem(
                                                label = "New folder",
                                                icon = Icons.Default.Folder,
                                                cornerRoundness = state.cornerRoundness,
                                                onClick = {
                                                    showFabMenu = false
                                                    showCreateFolderDialog = true
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            
                            if (showCreateFolderDialog) {
                                var folderName by remember { mutableStateOf("") }
                                AlertDialog(
                                    onDismissRequest = { showCreateFolderDialog = false },
                                    title = { Text("New folder") },
                                    text = {
                                        OutlinedTextField(
                                            value = folderName,
                                            onValueChange = { folderName = it },
                                            label = { Text("Folder name") },
                                            singleLine = true,
                                            shape = getDynamicCornerShape(12f, state.cornerRoundness)
                                        )
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                if (folderName.isNotBlank()) {
                                                    onAction(AppAction.CreateFolder(folderName))
                                                }
                                                showCreateFolderDialog = false
                                            }
                                        ) {
                                            Text("Save")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showCreateFolderDialog = false }) {
                                            Text("Cancel")
                                        }
                                    }
                                )
                            }
                            
                            if (showCreateFileDialog) {
                                var fileName by remember { mutableStateOf("") }
                                AlertDialog(
                                    onDismissRequest = { showCreateFileDialog = false },
                                    title = { Text("New file") },
                                    text = {
                                        OutlinedTextField(
                                            value = fileName,
                                            onValueChange = { fileName = it },
                                            label = { Text("File name") },
                                            singleLine = true,
                                            shape = getDynamicCornerShape(12f, state.cornerRoundness)
                                        )
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                if (fileName.isNotBlank()) {
                                                    onAction(AppAction.CreateFile(fileName))
                                                }
                                                showCreateFileDialog = false
                                            }
                                        ) {
                                            Text("Save")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showCreateFileDialog = false }) {
                                            Text("Cancel")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = state.showCleanerScreen,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                CleanerScreen(state = state, onAction = onAction, snackbarHostState = snackbarHostState)
            }

            AnimatedVisibility(
                visible = state.showTrashScreen,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                if (state.showTrashScreen) {
                    TrashScreen(state = state, onAction = onAction, onClose = { onAction(AppAction.SetTrashScreenVisible(false)) })
                }
            }

    AnimatedVisibility(
        visible = state.showSettingsScreen,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier
    ) {
        BackHandler(enabled = state.showSettingsScreen) {
            onAction(AppAction.SetShowSettingsScreen(false))
        }
        val currentPickingCategory = remember { mutableStateOf<String?>(null) }
        val context = androidx.compose.ui.platform.LocalContext.current
        val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            if (uri != null && currentPickingCategory.value != null) {
                val path = uri.path
                if (path != null && path.startsWith("/tree/primary:")) {
                    val relPath = path.removePrefix("/tree/primary:")
                    val absolutePath = "/storage/emulated/0/$relPath"
                    onAction(AppAction.SetOrganiserPath(currentPickingCategory.value!!, absolutePath))
                } else if (path != null && path == "/tree/primary") {
                    val absolutePath = "/storage/emulated/0"
                    onAction(AppAction.SetOrganiserPath(currentPickingCategory.value!!, absolutePath))
                } else {
                    android.widget.Toast.makeText(context, "Please select a folder on internal storage.", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            currentPickingCategory.value = null
        }

        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onAction(AppAction.SetShowSettingsScreen(false)) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Settings", style = MaterialTheme.typography.titleLarge)
                    }
                    Column(modifier = Modifier.weight(1f).padding(16.dp).verticalScroll(rememberScrollState())) {
                        var isThemeExpanded by remember { mutableStateOf(false) }
                        
                        Surface(
                            shape = getDynamicCornerShape(16f, state.cornerRoundness),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isThemeExpanded = !isThemeExpanded }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Theme (Appearance)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Icon(
                                    imageVector = if (isThemeExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Expand",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        androidx.compose.animation.AnimatedVisibility(visible = isThemeExpanded) {
                            ThemeSettingsContent(
                                currentMode = state.themeMode,
                                currentHue = state.themeHue,
                                useDynamicTheme = state.useDynamicSystemTheme,
                                onModeChange = { onAction(AppAction.SetThemeMode(it)) },
                                onHueChange = { onAction(AppAction.SetThemeHue(it)) },
                                onDynamicThemeChange = { onAction(AppAction.SetDynamicSystemTheme(it)) },
                                currentIconShape = state.iconShapeSetting,
                                onIconShapeChange = { onAction(AppAction.SetIconShape(it)) },
                                fontStyle = state.fontStyle,
                                textDecorations = state.textDecorations,
                                mainTextScale = state.mainTextScale,
                                subTextScale = state.subTextScale,
                                onFontStyleChange = { onAction(AppAction.SetFontStyle(it)) },
                                onTextDecorationToggle = { onAction(AppAction.ToggleTextDecoration(it)) },
                                onMainTextScaleChange = { onAction(AppAction.SetMainTextScale(it)) },
                                onSubTextScaleChange = { onAction(AppAction.SetSubTextScale(it)) },
                                cornerRoundness = state.cornerRoundness,
                                onCornerRoundnessChange = { onAction(AppAction.SetCornerRoundness(it)) },
                                gridColumns = state.gridColumns,
                                onGridColumnsChange = { onAction(AppAction.SetGridColumns(it)) }
                            )
                        }
                        
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
                        
                        Surface(
                            shape = getDynamicCornerShape(16f, state.cornerRoundness),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isOrganiserExpanded = !isOrganiserExpanded }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("File Organiser", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Icon(
                                    imageVector = if (isOrganiserExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Expand",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        androidx.compose.animation.AnimatedVisibility(visible = isOrganiserExpanded) {
                            Column {
                                Spacer(modifier = Modifier.height(16.dp))
                                val paths = listOf(
                                    "Docs" to ("Documents Path" to state.orgDestDocs),
                                    "Images" to ("Images Path" to state.orgDestImages),
                                    "Apks" to ("APKs Path" to state.orgDestApks),
                                    "Music" to ("Music Path" to state.orgDestMusic),
                                    "Videos" to ("Videos Path" to state.orgDestVideos)
                                )
        
                                paths.forEach { (cat, info) ->
                                    val (label, pathValue) = info
                                    Surface(
                                        shape = getDynamicCornerShape(16f, state.cornerRoundness),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                androidx.compose.foundation.text.BasicTextField(
                                                    value = pathValue,
                                                    onValueChange = { onAction(AppAction.SetOrganiserPath(cat, it)) },
                                                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
                                                )
                                            }
                                            IconButton(onClick = { currentPickingCategory.value = cat; launcher.launch(null) }) {
                                                Icon(Icons.Default.MoreVert, contentDescription = "Pick Directory", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }



            if (state.showBatchRenameDialog) {
                val selectedFilesList = state.files.filter { state.selectedFiles.contains(it.id) }.sortedBy { it.name }.map { it.name }
                val firstFile = state.files.find { it.id == state.selectedFiles.firstOrNull() }
                val initialName = firstFile?.name?.substringBeforeLast(".") ?: ""
                val initialExt = if (firstFile?.name?.contains(".") == true) firstFile.name.substringAfterLast(".") else ""
                BatchRenameDialog(
                    initialBaseName = initialName,
                    initialExtension = initialExt,
                    selectedFileNames = selectedFilesList,
                    cornerRoundness = state.cornerRoundness,
                    onDismiss = { onAction(AppAction.SetShowBatchRenameDialog(false)) },
                    onRename = { base, ext, pad, start, isPrefix, style ->
                        onAction(AppAction.BatchRenameFiles(base, ext, pad, start, isPrefix, style))
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
                        onClose = { onAction(AppAction.CloseFileViewer) }
                    )
                }
            }

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
                    }
                }
            }

    } // end ModalNavigationDrawer
}
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
            Spacer(modifier = Modifier.height(8.dp))
            DrawerMenuItem(
                icon = Icons.Outlined.CleaningServices,
                label = "Storage cleaner",
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
                            val driveTotalText = formatSize(state.driveStorageTotalBytes)
                            val driveFreeText = "${formatSize(state.driveStorageFreeBytes)} free"
                            val driveProgress = if (state.driveStorageTotalBytes > 0L) ((state.driveStorageTotalBytes - state.driveStorageFreeBytes).toFloat() / state.driveStorageTotalBytes.toFloat()) else 0f
                            
                            StorageCard(
                                modifier = cardModifier,
                                icon = Icons.Default.Cloud,
                                titleText = "GDrive",
                                usedText = driveUsedText,
                                totalText = driveTotalText,
                                freeText = driveFreeText,
                                progress = driveProgress,
                                cornerRoundness = state.cornerRoundness,
                                onClick = { /* Navigate to drive */ }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "music" to Icons.Default.MusicNote, 
                        "media" to Icons.Default.PermMedia, 
                        "doc" to Icons.Default.InsertDriveFile,
                        "apk" to Icons.Default.Android
                    ).forEach { (id, icon) ->
                        val active = state.filter == id
                        Box(
                            modifier = Modifier
                                .heightIn(min = 36.dp)
                                .clip(getDynamicCornerShape(18f, state.cornerRoundness))
                                .background(if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { 
                                    if (active) onAction(AppAction.SetFilter("all")) else onAction(AppAction.SetFilter(id))
                                }
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = id,
                                tint = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                val rootPath = Environment.getExternalStorageDirectory().absolutePath
                
                Row(
                    modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.location.startsWith("/") && state.location.length > rootPath.length) {
                        Icon(Icons.Default.SdStorage, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Internal storage", 
                            style = MaterialTheme.typography.labelLarge, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(getDynamicCornerShape(4f, state.cornerRoundness))
                                .clickable { onAction(AppAction.SetLocation(rootPath)) }
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
                                        .clip(getDynamicCornerShape(4f, state.cornerRoundness))
                                        .clickable { onAction(AppAction.SetLocation(pathForClick)) }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    } else if (!state.location.startsWith("/")) {
                        if (state.location.startsWith("drive")) {
                            Icon(Icons.Default.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Google Drive", 
                                style = MaterialTheme.typography.labelLarge, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clip(getDynamicCornerShape(4f, state.cornerRoundness))
                                    .clickable { onAction(AppAction.SetLocation("drive")) }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            if (state.location != "drive") {
                                Text(" / ", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                Text("Folder", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            val displayLoc = state.location.replaceFirstChar { it.uppercase() }
                            Text(displayLoc, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        // Just root path
                        Icon(Icons.Default.SdStorage, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Internal storage", 
                            style = MaterialTheme.typography.labelLarge, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Sort button with DropdownMenu
                    Box {
                        Box(
                            modifier = Modifier
                                .clip(getDynamicCornerShape(20f, state.cornerRoundness))
                    isListMode = state.isListMode,
                    iconShape = state.activeIconShape,
                    searchQuery = state.query,
                    onFileClick = { file -> 
                        if (isSearchExpanded) {
                            isSearchExpanded = false
                            onAction(AppAction.SetQuery(""))
                        }
                        if (state.isSelectionMode) {
                            onAction(AppAction.ToggleSelection(file.id))
                        } else {
                            if (file.type == "folder") {
                                onAction(AppAction.SetLocation(file.path, file.name))
                            } else if (file.name.endsWith(".pdf", ignoreCase = true) || file.type == "image" || file.type == "doc" || listOf(".txt", ".json", ".md", ".csv", ".xml", ".log", ".kt", ".java", ".py", ".html").any { file.name.endsWith(it, ignoreCase = true) }) {
                                onAction(AppAction.OpenFileViewer(file.id))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(state: AppState, onAction: (AppAction) -> Unit, snackbarHostState: SnackbarHostState, onDrawerOpen: () -> Unit = {}, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var extractTargetFile by remember { mutableStateOf<FileItem?>(null) }
    var isExtracting by remember { mutableStateOf(false) }
    // Search is always visible in the new header design
    var infoDialogFile by remember { mutableStateOf<FileItem?>(null) }
    var fileDetails by remember { mutableStateOf<FileDetails?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var backProgress by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            if (account?.email != null) {
                onAction(AppAction.SetGoogleDriveAuthStatus(true, account.email!!))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onAction(AppAction.SetErrorMessage("Google Login Failed: ${e.message}"))
        }
    }

    val authRecoverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            onAction(AppAction.ClearRecoverableAuthIntent)
            onAction(AppAction.Reload)
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
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    android.widget.Toast.makeText(context, "Error opening APK: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    onFileLongClick = { file ->
                        onAction(AppAction.ToggleSelection(file.id))
                    },
                    onPinClick = { file -> onAction(AppAction.TogglePin(file.path)) },
                    onInfoClick = { file -> infoDialogFile = file },
                    onRenameClick = { file, newName -> onAction(AppAction.RenameFile(file.path, newName)) },
                    onExtractClick = { file -> extractTargetFile = file },
                    cornerRoundness = displayState.cornerRoundness,
                    gridColumns = displayState.gridColumns,
                    modifier = Modifier.fillMaxSize()
                )

                        if (isBehind) {
                            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = scrimAlpha)))
                        }
                    }
                }
            }
        }
        
        Box(modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(bottom = 70.dp), contentAlignment = Alignment.BottomCenter) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = state.currentAudioFile != null && !state.showFullScreenPlayer && !state.isSelectionMode,
                    enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut()
                ) {
                    MiniMusicPlayer(
                        state = state,
                        onAction = onAction
                    )
                }

                AnimatedVisibility(
                    visible = state.isSelectionMode,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .clip(getDynamicCornerShape(16f, state.cornerRoundness))
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
                            IconButton(onClick = { onAction(AppAction.SetShowBatchRenameDialog(true)) }, modifier = Modifier.size(42.dp)) { Icon(Icons.Outlined.Edit, contentDescription = "Batch Rename", tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                        }
                        IconButton(onClick = { onAction(AppAction.SelectAll) }, modifier = Modifier.size(42.dp)) { Icon(Icons.Default.SelectAll, contentDescription = "Select All", tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                            IconButton(onClick = { onAction(AppAction.SelectNone) }, modifier = Modifier.size(42.dp)) { Icon(Icons.Default.Deselect, contentDescription = "Select None", tint = MaterialTheme.colorScheme.onPrimaryContainer) }
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
                            IconButton(onClick = { onAction(AppAction.SetClipboard("copy")) }, modifier = Modifier.size(42.dp)) { Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                            IconButton(onClick = { onAction(AppAction.SetClipboard("cut")) }, modifier = Modifier.size(42.dp)) { Icon(Icons.Outlined.ContentCut, contentDescription = "Cut", tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                            IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(42.dp)) { Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                    }
                }
            }
        }
        
        if (state.showFullScreenPlayer) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { onAction(AppAction.SetShowFullScreenPlayer(false)) },
                properties = androidx.compose.ui.window.DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                FullScreenMusicPlayer(state = state, onAction = onAction)
            }
        }
        
        if (showDeleteConfirm) {
            val isDrive = state.location == "drive"
            val warningText = if (isDrive) "Deleting is irreversible proceed" else if (state.isRecycleBinEnabled) "Files will be moved to Trash." else "This action cannot be undone."
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete selected item(s)?") },
                text = { Text(warningText) },
                confirmButton = {
                    TextButton(onClick = {
                        onAction(AppAction.DeleteSelectedFiles)
                        showDeleteConfirm = false
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
                }
            )
        }

        if (extractTargetFile != null) {
            if (!isExtracting) {
                FolderPickerDialog(
                    onDismiss = { extractTargetFile = null },
                    onFolderSelected = { path ->
                        isExtracting = true
                        onAction(AppAction.ExtractZip(extractTargetFile!!.path, path))
                    }
                )
            } else {
                if (state.extractProgress == null) {
                    isExtracting = false
                    extractTargetFile = null
                } else {
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text("Extracting Zip") },
                        text = {
                            Column {
                                Text("Extracting ${extractTargetFile!!.name}...")
                                Spacer(modifier = Modifier.height(16.dp))
                                LinearProgressIndicator(
                                    progress = { state.extractProgress ?: 0f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("${((state.extractProgress ?: 0f) * 100).toInt()}%")
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { onAction(AppAction.ToggleExtractPause) }) {
                                Text(if (state.isExtractPaused) "Resume" else "Pause")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { 
                                onAction(AppAction.CancelExtract)
                            }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
        
        if (state.extractResultPath != null) {
            AlertDialog(
                onDismissRequest = { onAction(AppAction.ClearExtractResult) },
                title = { Text("Extraction Complete") },
                text = { Text("Folder extracted successfully. Do you want to open it?") },
                confirmButton = {
                    TextButton(onClick = { 
                        val path = state.extractResultPath
                        onAction(AppAction.ClearExtractResult)
                        if (path != null) {
                            onAction(AppAction.SetLocation(path))
                        }
                    }) {
                        Text("Open")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onAction(AppAction.ClearExtractResult) }) {
                        Text("Cancel")
                    }
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
                            .height(50.dp)
                            .onFocusChanged { isSearchFocused = it.isFocused },
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
                        if (state.recentSearches.isEmpty()) {
                        val stack = state.remoteFolderStack[com.ripple.filemanager.CloudProvider.DROPBOX]
                        if (stack != null && stack.size > 1) "dropbox:" else "dropbox"
                    } else "home"
                    
                    val prevState = stateMap[prevLocation] ?: state.copy(location = prevLocation) // Might not have files, but better than nothing
                    
                    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                        FileGrid(
                            files = prevState.files,
                            selectedFiles = prevState.selectedFiles,
                            isListMode = prevState.isListMode,
                            iconShape = prevState.activeIconShape,
                            cornerRoundness = prevState.cornerRoundness,
                            onFileClick = {},
                            onFileLongClick = {},
                            onPinClick = {},
                                    leadingIcon = {
                                        Icon(
                                            if (recent.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                        }
                    }
                }


                Spacer(modifier = Modifier.width(8.dp))

                // Trash icon in pill container
                Surface(
                    shape = getDynamicCornerShape(12f, state.cornerRoundness),
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                                androidx.compose.animation.EnterExitState.PreEnter -> 0.35f
                                androidx.compose.animation.EnterExitState.Visible -> 0f
                                androidx.compose.animation.EnterExitState.PostExit -> 0.35f
                            }
                        } else {
                            0f
                        }
                    }

                    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 12.dp)
                    .padding(top = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hamburger menu icon in pill container
                Surface(
                    shape = getDynamicCornerShape(24f, displayState.cornerRoundness),
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
                var searchExpanded by remember { mutableStateOf(false) }
                
                Box(modifier = Modifier.weight(1f)) {
                    TextField(
                        value = displayState.query,
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
                                if (displayState.query.isNotEmpty()) {
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
                            .height(50.dp)
                            .onFocusChanged { isSearchFocused = it.isFocused },
                        shape = getDynamicCornerShape(24f, displayState.cornerRoundness),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        singleLine = true,
                            Icons.Default.Menu,
                            contentDescription = "Menu",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    shape = getDynamicCornerShape(24f, displayState.cornerRoundness),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(onClick = onDrawerOpen) {
                    } else if (state.location.startsWith("dropbox:")) {
                        val stack = state.remoteFolderStack[com.ripple.filemanager.CloudProvider.DROPBOX]
                        if (stack != null && stack.size > 1) "dropbox:" else "dropbox"
                    } else "home"
                    
                    val prevState = stateMap[prevLocation] ?: state.copy(location = prevLocation) // Might not have files, but better than nothing
                    
                    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                        FileGrid(
                        } else {
                            displayState.recentSearches.forEach { recent ->
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
                    }
                }


                Spacer(modifier = Modifier.width(8.dp))

                // Trash icon in pill container
                Surface(
                    shape = getDynamicCornerShape(12f, displayState.cornerRoundness),
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

                        if (animScale == 0f) {
                            androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
                        } else {
                            val isForward = targetState.second > initialState.second
                            val isBackward = targetState.second < initialState.second
                            
                            if (isForward) {
                                (androidx.compose.animation.slideInHorizontally(
                                    animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) { fullWidth -> fullWidth } + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200)))
                                .togetherWith(
                                    androidx.compose.animation.slideOutHorizontally(
                                        animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                    ) { fullWidth -> -(fullWidth * 0.3f).toInt() } + 
                                    androidx.compose.animation.scaleOut(targetScale = 0.94f, animationSpec = androidx.compose.animation.core.tween(300)) + 
                                    androidx.compose.animation.fadeOut(targetAlpha = 0.5f, animationSpec = androidx.compose.animation.core.tween(300))
                                )
                                .using(androidx.compose.animation.SizeTransform(clip = false))
                                .apply { targetContentZIndex = 1f }
                            } else if (isBackward) {
                                (androidx.compose.animation.slideInHorizontally(
                                    animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) { fullWidth -> -(fullWidth * 0.3f).toInt() } + 
                                androidx.compose.animation.scaleIn(initialScale = 0.94f, animationSpec = androidx.compose.animation.core.tween(300)) + 
                                androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200)))
                                .togetherWith(
                                    androidx.compose.animation.slideOutHorizontally(
                                        animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                    ) { fullWidth -> fullWidth } + 
                                    androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300))
                                )
                                .using(androidx.compose.animation.SizeTransform(clip = false))
                                .apply { targetContentZIndex = -1f }
                            } else {
                                androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200)) togetherWith androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200))
                            }
                        }
                    },
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
                FileGrid(
                    isLoading = displayState.isLoading,
                    emptyState = {
                        if (!(!displayState.location.contains("Android/data") && !displayState.location.contains("Android/obb") || displayState.hasShizuku)) {
                              val context = LocalContext.current
                              
                              var isShizukuRunning by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                              androidx.compose.runtime.LaunchedEffect(displayState.location) {
                                  while (true) {
                        if (isBehind) {
                            when (exitState) {
                                androidx.compose.animation.EnterExitState.PreEnter -> 0.35f
                                androidx.compose.animation.EnterExitState.Visible -> 0f
                                androidx.compose.animation.EnterExitState.PostExit -> 0.35f
                            }
                        } else {
                            0f
                        }
                    }

                    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                FileGrid(
                    isLoading = displayState.isLoading,
                    emptyState = {
                        if (!(!displayState.location.contains("Android/data") && !displayState.location.contains("Android/obb") || displayState.hasShizuku)) {
                              val context = LocalContext.current
                              
                              var isShizukuRunning by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                              androidx.compose.runtime.LaunchedEffect(displayState.location) {
                                  while (true) {
                                      isShizukuRunning = try { rikka.shizuku.Shizuku.pingBinder() } catch (e: Exception) { false }
                                      if (isShizukuRunning) {
                                          onAction(AppAction.AutoRequestAccess(displayState.location))
                                          break
                                      }
                                      kotlinx.coroutines.delay(1000)
                                  }
                              }
                              
                              Column(
                                  modifier = Modifier.fillMaxWidth().padding(32.dp).padding(top = 80.dp),
                                  horizontalAlignment = Alignment.CenterHorizontally,
                                  verticalArrangement = Arrangement.Center
                              ) {
                                  Icon(Icons.Default.Lock, contentDescription = "Restricted", modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.error)
                                  Spacer(modifier = Modifier.height(16.dp))
                                  Text("Access Restricted", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                  Spacer(modifier = Modifier.height(8.dp))
                                  Text("Android 11+ restricts access to this folder. Please grant access using Shizuku or a compatible alternative.", textAlign = TextAlign.Center)
                                  Spacer(modifier = Modifier.height(24.dp))
                                  
                                  Button(onClick = { 
                                      isShizukuRunning = try { rikka.shizuku.Shizuku.pingBinder() } catch (e: Exception) { false }
                              }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(top = 100.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.FolderOff, contentDescription = "Empty", modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No files found", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                      header = {
                var showSortMenu by remember { mutableStateOf(false) }
                Column {
                if (displayState.storageTotalGb > 0f) {
                    val isScrollable = (displayState.sdCardStorageTotalGb > 0 && displayState.isGoogleDriveAuthenticated)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (isScrollable) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val usedText = "%.1f GB".format(displayState.storageTotalGb - displayState.storageFreeGb)
                        val totalText = "%.1f GB".format(displayState.storageTotalGb)
                        val freeText = "%.1f GB free".format(displayState.storageFreeGb)
                        val progress = if (displayState.storageTotalGb > 0) ((displayState.storageTotalGb - displayState.storageFreeGb) / displayState.storageTotalGb) else 0f
                        
                        val cardModifier = if (isScrollable) Modifier.width(180.dp).height(100.dp) else Modifier.weight(1f).height(100.dp)

                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (isScrollable) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val usedText = "%.1f GB".format(displayState.storageTotalGb - displayState.storageFreeGb)
                        val totalText = "%.1f GB".format(displayState.storageTotalGb)
                        val freeText = "%.1f GB free".format(displayState.storageFreeGb)
                        val progress = if (displayState.storageTotalGb > 0) ((displayState.storageTotalGb - displayState.storageFreeGb) / displayState.storageTotalGb) else 0f
                        
                        val cardModifier = if (isScrollable) Modifier.width(180.dp).height(100.dp) else Modifier.weight(1f).height(100.dp)


                        StorageCard(
                            modifier = cardModifier,
                            icon = Icons.Default.Storage,
                            titleText = "Local",
                            usedText = usedText,
                            totalText = totalText,
                            freeText = freeText,
                            progress = progress,
                            cornerRoundness = displayState.cornerRoundness,
                            onClick = { onAction(AppAction.SetCleanerScreenVisible(true)) }
                        )
                        
                        if (displayState.sdCardStorageTotalGb > 0) {
                            val sdUsedText = "%.1f GB".format(displayState.sdCardStorageTotalGb - displayState.sdCardStorageFreeGb)
                            val sdTotalText = "%.1f GB".format(displayState.sdCardStorageTotalGb)
                            val sdFreeText = "%.1f GB free".format(displayState.sdCardStorageFreeGb)
                            val sdProgress = if (displayState.sdCardStorageTotalGb > 0) ((displayState.sdCardStorageTotalGb - displayState.sdCardStorageFreeGb) / displayState.sdCardStorageTotalGb) else 0f
                            StorageCard(
                                modifier = cardModifier,
                                icon = Icons.Default.SdStorage,
                                titleText = "SD Card",
                                usedText = sdUsedText,
                                        "Dropbox" -> Icons.Default.CloudQueue
                                        else -> Icons.Default.Cloud
                                    }
                                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = selectedCloud, 
                                        style = MaterialTheme.typography.labelLarge, 
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                            if (displayState.isGoogleDriveAuthenticated) cloudProviders.add("drive")
                            if (displayState.isMegaAuthenticated) cloudProviders.add("mega")

                            val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { cloudProviders.size })
                            androidx.compose.foundation.pager.VerticalPager(
                                state = pagerState,
                                modifier = cardModifier
                            ) { page ->
                                val provider = cloudProviders[page]
                                if (provider == "drive") {
                                    val driveUsedText = formatSize(displayState.driveStorageTotalBytes - displayState.driveStorageFreeBytes)
                                    val driveTotalText = formatSize(displayState.driveStorageTotalBytes)
                                    val driveFreeText = "${formatSize(displayState.driveStorageFreeBytes)} free"
                                    val driveProgress = if (displayState.driveStorageTotalBytes > 0L) ((displayState.driveStorageTotalBytes - displayState.driveStorageFreeBytes).toFloat() / displayState.driveStorageTotalBytes.toFloat()) else 0f
                                    
                                    StorageCard(
                                    StorageCard(
                                        modifier = Modifier.fillMaxSize(),
                                        icon = Icons.Default.Cloud,
                                        titleText = "GDrive",
                                        usedText = driveUsedText,
                                        totalText = driveTotalText,
                                        freeText = driveFreeText,
                                        progress = driveProgress,
                                        cornerRoundness = displayState.cornerRoundness,
                                        onClick = { /* Navigate to drive */ }
                                    )
                                } else if (provider == "mega") {
                                    val megaUsedText = formatSize(displayState.megaStorageTotalBytes - displayState.megaStorageFreeBytes)
                                    val megaTotalText = formatSize(displayState.megaStorageTotalBytes)
                                    val megaFreeText = "${formatSize(displayState.megaStorageFreeBytes)} free"
                                    val megaProgress = if (displayState.megaStorageTotalBytes > 0L) ((displayState.megaStorageTotalBytes - displayState.megaStorageFreeBytes).toFloat() / displayState.megaStorageTotalBytes.toFloat()) else 0f
                                    
                                    StorageCard(
                                        modifier = Modifier.fillMaxSize(),
                                        icon = Icons.Default.Cloud,
                                        titleText = "MEGA",
                                        usedText = megaUsedText,
                                        totalText = megaTotalText,
                                        freeText = megaFreeText,
                                        progress = megaProgress,
                        }
                    } else {
                        // Just root path
                        Icon(Icons.Default.SdStorage, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Internal storage", 
                            style = MaterialTheme.typography.labelLarge, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Sort button with DropdownMenu
                    Box {
                        Box(
                            modifier = Modifier
                                .clip(getDynamicCornerShape(20f, state.cornerRoundness))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { showSortMenu = true }
                                .padding(8.dp)
                        ) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = showSortMenu,
        // Draw progress with gradient
        if (progressWidth > 0) {
            drawLine(
                brush = brush,
                start = androidx.compose.ui.geometry.Offset(0f, height / 2),
                end = androidx.compose.ui.geometry.Offset(progressWidth, height / 2),
                strokeWidth = strokeW,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                val rootPath = Environment.getExternalStorageDirectory().absolutePath
                
                Row(
                    modifier = Modifier.fillMaxSize().horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (displayState.location.startsWith("/") && displayState.location.length > rootPath.length) {
                        Icon(Icons.Default.SdStorage, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Internal storage", 
                            style = MaterialTheme.typography.labelLarge, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(getDynamicCornerShape(4f, displayState.cornerRoundness))
                                .clickable { onAction(AppAction.SetLocation(rootPath)) }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                        
                        val relativePath = displayState.location.removePrefix(rootPath).removePrefix("/")
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
                                        .clip(getDynamicCornerShape(4f, displayState.cornerRoundness))
                                        .clickable { onAction(AppAction.SetLocation(pathForClick)) }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    } else if (!displayState.location.startsWith("/")) {
                        if (displayState.location.startsWith("drive") || displayState.location.startsWith("mega") || displayState.location.startsWith("dropbox")) {
                            var expanded by remember { mutableStateOf(false) }
                            val availableClouds = mutableListOf<String>()
                            if (displayState.isGoogleDriveAuthenticated) availableClouds.add("Google Drive")
                            if (displayState.isMegaAuthenticated) availableClouds.add("Mega")
                            if (displayState.isDropboxAuthenticated) availableClouds.add("Dropbox")
                            
                            val currentCloudName = when {
                                displayState.location.startsWith("mega") -> "Mega"
                                displayState.location.startsWith("dropbox") -> "Dropbox"
                                else -> "Google Drive"
                            }

                            Box {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(getDynamicCornerShape(12f, displayState.cornerRoundness))
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
                                                if (!displayState.location.startsWith(newLoc)) {
                                                    onAction(AppAction.SetLocation(newLoc))
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            if (displayState.location != "drive" && displayState.location != "mega" && displayState.location != "dropbox") {
                                Text(" / ", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                Text(displayState.location.split("/").lastOrNull() ?: "Folder", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            val displayLoc = displayState.location.replaceFirstChar { it.uppercase() }
                            Text(displayLoc, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        // Just root path
                        Icon(Icons.Default.SdStorage, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Internal storage", 
                            style = MaterialTheme.typography.labelLarge, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Sort button with DropdownMenu
                    Box {
                        Box(
                            modifier = Modifier
                                .clip(getDynamicCornerShape(20f, displayState.cornerRoundness))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { showSortMenu = true }
                                .padding(8.dp)
                        ) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Alphabetical") },
                                onClick = { onAction(AppAction.SetSortMode(com.ripple.filemanager.SortMode.ALPHABETICAL)); showSortMenu = false },
                                leadingIcon = { if (displayState.sortMode == com.ripple.filemanager.SortMode.ALPHABETICAL) Icon(Icons.Default.Check, null) }
                            )
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Date") },
                                onClick = { onAction(AppAction.SetSortMode(com.ripple.filemanager.SortMode.DATE)); showSortMenu = false },
                                leadingIcon = { if (displayState.sortMode == com.ripple.filemanager.SortMode.DATE) Icon(Icons.Default.Check, null) }
                            )
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Size") },
                                onClick = { onAction(AppAction.SetSortMode(com.ripple.filemanager.SortMode.SIZE)); showSortMenu = false },
                                leadingIcon = { if (displayState.sortMode == com.ripple.filemanager.SortMode.SIZE) Icon(Icons.Default.Check, null) }
                            )
                        }
                    }

                    // View toggle button
                    Box(
                        modifier = Modifier
                            .clip(getDynamicCornerShape(20f, displayState.cornerRoundness))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { onAction(AppAction.ToggleViewMode) }
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = if (displayState.isListMode) Icons.Default.GridView else Icons.Default.FilterList, 
                            contentDescription = "Toggle View", 
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, 
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
                }


        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight(),
            shape = getDynamicCornerShape(28f, cornerRoundness),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Batch Rename",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                // Live Preview Section
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Live Preview",



















































































































































































    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = fg, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BottomBar(currentLocation: String, hasCloudAuth: Boolean, onLocationSelected: (String) -> Unit, cornerRoundness: Float, modifier: Modifier = Modifier) {
    val items = mutableListOf(
        "home" to Icons.Default.Home,
        "recent" to Icons.Default.Schedule,
        "starred" to Icons.Default.PushPin
    )
    if (hasCloudAuth) {
        items.add("drive" to Icons.Default.Cloud)
    }
    
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






















































































































































































































































































































































































fun StorageCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    titleText: String,
    usedText: String,
    totalText: String,
    freeText: String,
    progress: Float,
    cornerRoundness: Float,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(getDynamicCornerShape(16f, cornerRoundness))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = getDynamicCornerShape(16f, cornerRoundness)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
























}

@Composable
fun StorageCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    titleText: String,
    usedText: String,
    totalText: String,
    freeText: String,
    progress: Float,
    cornerRoundness: Float,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(getDynamicCornerShape(16f, cornerRoundness))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = getDynamicCornerShape(16f, cornerRoundness)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(
                        progress = progress,
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        strokeWidth = 5.dp,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = usedText,
                    style = MaterialTheme.typography.labelSmall,













































































































    }
}

@Composable
fun StorageCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    titleText: String,
    usedText: String,
    totalText: String,
    freeText: String,
    progress: Float,
    cornerRoundness: Float,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(getDynamicCornerShape(16f, cornerRoundness))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = getDynamicCornerShape(16f, cornerRoundness)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(
                        progress = progress,
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        strokeWidth = 5.dp,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)















































































































































































































































































































































































































