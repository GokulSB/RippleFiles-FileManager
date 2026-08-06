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

            } else {

                Box(modifier = Modifier.fillMaxSize()) {

                    MainContent(

                        state = state,

                        onAction = onAction,

                        snackbarHostState = snackbarHostState,

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

        } else {

            onAction(AppAction.ClearRecoverableAuthIntent)

        }

    }



    LaunchedEffect(infoDialogFile) {

        if (infoDialogFile != null) {

            onAction(AppAction.LoadFileDetails(infoDialogFile!!.path) { fileDetails = it })

        } else {

            fileDetails = null

        }

    }

    

    BackHandler(enabled = isSearchExpanded) {

        isSearchExpanded = false

        onAction(AppAction.SetQuery(""))

    }



    BackHandler(enabled = state.selectedFiles.isNotEmpty()) {

        onAction(AppAction.ClearSelection)

    }



    androidx.activity.compose.PredictiveBackHandler(enabled = state.selectedFiles.isEmpty() && state.query.isEmpty() && state.location != "home") { progress ->

        try {

            progress.collect { backEvent ->

                backProgress = backEvent.progress

            }

            if (state.location.startsWith("/")) {

                val parent = File(state.location).parent

                val rootPath = Environment.getExternalStorageDirectory().absolutePath

                if (parent != null && parent.length >= rootPath.length) {

                    onAction(AppAction.SetLocation(parent))

                } else {

                    onAction(AppAction.SetLocation("home"))

                }

            } else if (state.location == "drive" || state.location.startsWith("drive_id:")) {

                onAction(AppAction.NavigateBackInDrive)

            } else {

                onAction(AppAction.SetLocation("home"))

            }

        } catch (e: java.util.concurrent.CancellationException) {

            // Cancelled

        } finally {

            backProgress = 0f

        }

    }

    

    val files = state.files



    



    Scaffold(

        modifier = modifier,

        snackbarHost = {

            val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

            val isThreeButton = navBottom > 24.dp

            val finalBottomPadding = if (isThreeButton) navBottom + 25.dp else 23.dp

            val snackbarBottomPadding = finalBottomPadding + 56.dp // Height of BottomBar + offset

            

            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.imePadding().padding(bottom = snackbarBottomPadding)) { data ->

                Snackbar(

                    snackbarData = data,

                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,

                    contentColor = MaterialTheme.colorScheme.onSurface,

                    shape = getDynamicCornerShape(12f, state.cornerRoundness),

                    actionColor = MaterialTheme.colorScheme.primary

                )

            }

        },

        topBar = {

                Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 24.dp).padding(top = 0.dp, bottom = 4.dp)) {

                Row(verticalAlignment = Alignment.CenterVertically) {

                    Box(modifier = Modifier.weight(1f)) {

                        Text(

                            text = when {

                                    state.location == "home" -> "Home"

                                    state.location == "recent" -> "Recent"

                                    state.location == "starred" -> "Starred"

                                    state.location == "trash" -> "Trash"

                                    else -> "MyFiles"

                                },

                            style = MaterialTheme.typography.displayMedium,

                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,

                            fontWeight = FontWeight.Bold,

                            maxLines = 1,

                            overflow = TextOverflow.Ellipsis

                        )

                    }

                    

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(

                        modifier = Modifier

                            .clip(getDynamicCornerShape(12f, state.cornerRoundness))

                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),

                        verticalAlignment = Alignment.CenterVertically

                    ) {

                        if (state.location.lowercase(java.util.Locale.getDefault()).endsWith("download")) {

                            Box(

                                modifier = Modifier

                                    .clickable { onAction(AppAction.OrganiseDownloads) }

                                    .padding(12.dp),

                                contentAlignment = Alignment.Center

                            ) {

                                Icon(Icons.Default.AutoAwesome, contentDescription = "Organise", modifier = Modifier.size(24.dp))

                            }

                        }

                        Box {

                            Box(

                                modifier = Modifier

                                    .clickable { isSearchExpanded = true }

                                    .padding(12.dp),

                                contentAlignment = Alignment.Center

                            ) {

                                Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(24.dp))

                            }

                            

                            androidx.compose.animation.AnimatedVisibility(

                                visible = isSearchExpanded,

                                enter = androidx.compose.animation.expandHorizontally(expandFrom = androidx.compose.ui.Alignment.End) + androidx.compose.animation.fadeIn(),

                                exit = androidx.compose.animation.shrinkHorizontally(shrinkTowards = androidx.compose.ui.Alignment.End) + androidx.compose.animation.fadeOut()

                            ) {

                                TextField(

                                    value = state.query,

                                    onValueChange = { onAction(AppAction.SetQuery(it)) },

                                    placeholder = { Text("Search", maxLines = 1, softWrap = false) },

                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },

                                    trailingIcon = { 

                                        IconButton(onClick = { 

                                            isSearchExpanded = false

                                            onAction(AppAction.SetQuery(""))

                                        }) {

                                            Icon(Icons.Default.Close, contentDescription = "Close")

                                        }

                                    },

                                    modifier = Modifier.width(250.dp).padding(start = 8.dp),

                                    shape = getDynamicCornerShape(24f, state.cornerRoundness),

                                    colors = TextFieldDefaults.colors(

                                        focusedIndicatorColor = Color.Transparent,

                                        unfocusedIndicatorColor = Color.Transparent,

                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,

                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest

                                    ),

                                    singleLine = true

                                )

                            }

                        }

                        

                        Box(

                            modifier = Modifier

                                .clickable { onAction(AppAction.SetTrashScreenVisible(true)) }

                                .padding(12.dp),

                            contentAlignment = Alignment.Center

                        ) {

                            Icon(Icons.Outlined.Delete, contentDescription = "Trash", modifier = Modifier.size(24.dp))

                        }

                        

                        Box(

                            modifier = Modifier

                                .clickable { onAction(AppAction.SetShowSettingsScreen(true)) }

                                .padding(12.dp),

                            contentAlignment = Alignment.Center

                        ) {

                            Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(24.dp))

                        }

                    }

                }

                

                Spacer(modifier = Modifier.height(4.dp))

                



            }

        }

    ) { paddingValues ->

        Column(

            modifier = Modifier

                .padding(paddingValues)

                .padding(horizontal = 24.dp)

                .graphicsLayer {

                    val scale = 1f - (backProgress * 0.1f)

                    scaleX = scale

                    scaleY = scale

                    translationX = backProgress * 150f

                    alpha = 1f - (backProgress * 0.3f)

                }

        ) {







            if (state.location == "drive" && !state.isGoogleDriveAuthenticated) {

                Column(

                    modifier = Modifier.weight(1f).fillMaxWidth(),

                    horizontalAlignment = Alignment.CenterHorizontally,

                    verticalArrangement = Arrangement.Center

                ) {

                    Icon(Icons.Default.Cloud, contentDescription = "Drive", modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Google Drive", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(4.dp))

                    Text("Sign in to access your cloud files securely.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(

                        onClick = {

                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)

                                .requestEmail()

                                .requestScopes(Scope(DriveScopes.DRIVE))

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

                    if (state.recoverableAuthIntent != null) {

                        Button(onClick = { authRecoverLauncher.launch(state.recoverableAuthIntent) }) {

                            Text("Grant Permission")

                        }

                    } else {

                        Button(onClick = { onAction(AppAction.Reload) }) {

                            Text("Retry")

                        }

                    }

                }

            } else {

                FileGrid(

                    isLoading = state.isLoading,

                    emptyState = {

                        if (!(!state.location.contains("Android/data") && !state.location.contains("Android/obb") || state.hasShizuku)) {

                              val context = LocalContext.current

                              

                              var isShizukuRunning by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

                              androidx.compose.runtime.LaunchedEffect(state.location) {

                                  while (true) {

                                      isShizukuRunning = try { rikka.shizuku.Shizuku.pingBinder() } catch (e: Exception) { false }

                                      if (isShizukuRunning) {

                                          onAction(AppAction.AutoRequestAccess(state.location))

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

                                      if (isShizukuRunning) {

                                          onAction(AppAction.RequestShizukuAccess)

                                      } else {

                                          try {

                                              onAction(AppAction.RequestShizukuAccess) // Try anyway to trigger alternatives

                                          } catch (e: Exception) {

                                              android.widget.Toast.makeText(context, "Shizuku service not detected. Ensure it's running.", android.widget.Toast.LENGTH_LONG).show()

                                          }

                                      }

                                  }, modifier = Modifier.fillMaxWidth()) {

                                      Text("Grant Access")

                                  }

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

                if (state.storageTotalGb > 0f && !state.location.startsWith("drive")) {

                    val isScrollable = (state.sdCardStorageTotalGb > 0 && state.isGoogleDriveAuthenticated)

                    Row(

                        modifier = Modifier

                            .fillMaxWidth()

                            .then(if (isScrollable) Modifier.horizontalScroll(rememberScrollState()) else Modifier),

                        horizontalArrangement = Arrangement.spacedBy(12.dp)

                    ) {

                        val usedText = "%.1f GB".format(state.storageTotalGb - state.storageFreeGb)

                        val totalText = "%.1f GB".format(state.storageTotalGb)

                        val freeText = "%.1f GB free".format(state.storageFreeGb)

                        val progress = if (state.storageTotalGb > 0) ((state.storageTotalGb - state.storageFreeGb) / state.storageTotalGb) else 0f

                        

                        val cardModifier = if (isScrollable) Modifier.width(180.dp).height(100.dp) else Modifier.weight(1f).height(100.dp)





                        StorageCard(

                            modifier = cardModifier,

                            icon = Icons.Default.Storage,

                            titleText = "Local",

                            usedText = usedText,

                            totalText = totalText,

                            freeText = freeText,

                            progress = progress,

                            cornerRoundness = state.cornerRoundness,

                            onClick = { onAction(AppAction.SetCleanerScreenVisible(true)) }

                        )

                        

                        if (state.sdCardStorageTotalGb > 0) {

                            val sdUsedText = "%.1f GB".format(state.sdCardStorageTotalGb - state.sdCardStorageFreeGb)

                            val sdTotalText = "%.1f GB".format(state.sdCardStorageTotalGb)

                            val sdFreeText = "%.1f GB free".format(state.sdCardStorageFreeGb)

                            val sdProgress = if (state.sdCardStorageTotalGb > 0) ((state.sdCardStorageTotalGb - state.sdCardStorageFreeGb) / state.sdCardStorageTotalGb) else 0f

                            StorageCard(

                                modifier = cardModifier,

                                icon = Icons.Default.SdStorage,

                                titleText = "SD Card",

                                usedText = sdUsedText,

                                totalText = sdTotalText,

                                freeText = sdFreeText,

                                progress = sdProgress,

                                cornerRoundness = state.cornerRoundness,

                                onClick = { /* Navigate to SD card if possible */ }

                            )

                        }



                        if (state.isGoogleDriveAuthenticated) {

                            val driveUsedText = formatSize(state.driveStorageTotalBytes - state.driveStorageFreeBytes)

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

                                leadingIcon = { if (state.sortMode == com.ripple.filemanager.SortMode.ALPHABETICAL) Icon(Icons.Default.Check, null) }

                            )

                            androidx.compose.material3.DropdownMenuItem(

                                text = { Text("Date") },

                                onClick = { onAction(AppAction.SetSortMode(com.ripple.filemanager.SortMode.DATE)); showSortMenu = false },

                                leadingIcon = { if (state.sortMode == com.ripple.filemanager.SortMode.DATE) Icon(Icons.Default.Check, null) }

                            )

                            androidx.compose.material3.DropdownMenuItem(

                                text = { Text("Size") },

                                onClick = { onAction(AppAction.SetSortMode(com.ripple.filemanager.SortMode.SIZE)); showSortMenu = false },

                                leadingIcon = { if (state.sortMode == com.ripple.filemanager.SortMode.SIZE) Icon(Icons.Default.Check, null) }

                            )

                        }

                    }



                    // View toggle button

                    Box(

                        modifier = Modifier

                            .clip(getDynamicCornerShape(20f, state.cornerRoundness))

                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)

                            .clickable { onAction(AppAction.ToggleViewMode) }

                            .padding(8.dp)

                    ) {

                        Icon(

                            imageVector = if (state.isListMode) Icons.Default.GridView else Icons.Default.FilterList, 

                            contentDescription = "Toggle View", 

                            tint = MaterialTheme.colorScheme.onSurfaceVariant, 

                            modifier = Modifier.size(20.dp)

                        )

                    }

                }

            }

                }





                      },

                    files = files,

                    selectedFiles = state.selectedFiles,

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

                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(file.path))

                                val intent = Intent(Intent.ACTION_VIEW).apply {

                                    setDataAndType(uri, "video/*")

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

                        onAction(AppAction.ToggleSelection(file.id))

                    },

                    onPinClick = { file -> onAction(AppAction.TogglePin(file.path)) },

                    onInfoClick = { file -> infoDialogFile = file },

                    onRenameClick = { file, newName -> onAction(AppAction.RenameFile(file.path, newName)) },

                    onExtractClick = { file -> extractTargetFile = file },

                    cornerRoundness = state.cornerRoundness,

                    gridColumns = state.gridColumns,

                    modifier = Modifier.weight(1f)

                )

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

fun Sidebar(currentLocation: String, onLocationSelected: (String) -> Unit, cornerRoundness: Float, modifier: Modifier = Modifier) {

    Column(

        modifier = modifier.fillMaxHeight().background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f)).windowInsetsPadding(WindowInsets.statusBars).padding(16.dp),

        verticalArrangement = Arrangement.spacedBy(16.dp)

    ) {

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {

            Box(modifier = Modifier.size(46.dp).background(MaterialTheme.colorScheme.primary, getDynamicCornerShape(16f, cornerRoundness)), contentAlignment = Alignment.Center) {

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

            shape = getDynamicCornerShape(27f, cornerRoundness),

            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)

        ) {

            Icon(Icons.Default.Add, contentDescription = null)

            Spacer(modifier = Modifier.width(8.dp))

            Text("Create", fontWeight = FontWeight.ExtraBold)

        }



        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {

            NavButton("Home", "home", Icons.Default.Home, currentLocation, cornerRoundness, onLocationSelected)

            NavButton("Recents", "recent", Icons.Default.Schedule, currentLocation, cornerRoundness, onLocationSelected)

            NavButton("Starred", "starred", Icons.Default.Star, currentLocation, cornerRoundness, onLocationSelected)

            NavButton("Drive", "drive", Icons.Default.Cloud, currentLocation, cornerRoundness, onLocationSelected)

            NavButton("Trash", "trash", Icons.Default.Delete, currentLocation, cornerRoundness, onLocationSelected)

        }

        

        Spacer(modifier = Modifier.weight(1f))

        

        Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh, getDynamicCornerShape(22f, cornerRoundness)).padding(16.dp)) {

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {

                Text("Device storage", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                Text("64%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

            }

            LinearProgressIndicator(progress = { 0.64f }, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(10.dp).clip(getDynamicCornerShape(5f, cornerRoundness)))

            Text("164 GB used of 256 GB\nPhotos and videos use the most space.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        }

    }

}



@Composable

fun NavButton(label: String, id: String, icon: androidx.compose.ui.graphics.vector.ImageVector, currentId: String, cornerRoundness: Float, onSelect: (String) -> Unit) {

    val active = currentId == id

    val bg = if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent

    val fg = if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    val shape = if (active) getDynamicCornerShape(24f, cornerRoundness) else getDynamicCornerShape(18f, cornerRoundness)

    

    Row(

        modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp).clip(shape).background(bg).clickable { onSelect(id) }.padding(horizontal = 12.dp, vertical = 8.dp),

        verticalAlignment = Alignment.CenterVertically

    ) {

        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(24.dp))

        Spacer(modifier = Modifier.width(12.dp))

        Text(label, color = fg, fontWeight = FontWeight.Bold)

    }

}



@Composable

fun BottomBar(currentLocation: String, onLocationSelected: (String) -> Unit, cornerRoundness: Float, modifier: Modifier = Modifier) {

    Row(

        modifier = modifier

            .heightIn(min = 48.dp)

            .clip(getDynamicCornerShape(24f, cornerRoundness))

            .background(MaterialTheme.colorScheme.surfaceContainer)

            .padding(horizontal = 12.dp, vertical = 8.dp),

        horizontalArrangement = Arrangement.spacedBy(8.dp),

        verticalAlignment = Alignment.CenterVertically

    ) {

        val items = listOf(

            "home" to Icons.Default.Home,

            "recent" to Icons.Default.Schedule,

            "starred" to Icons.Default.PushPin,

            "drive" to Icons.Default.Cloud

        )

        

        items.forEach { (id, icon) ->

            val selected = currentLocation == id || (id == "home" && currentLocation == android.os.Environment.getExternalStorageDirectory().absolutePath)

            val iconColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

            val bgColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent

            

            Box(

                modifier = Modifier

                    .height(32.dp)

                    .width(48.dp)

                    .clip(getDynamicCornerShape(16f, cornerRoundness))

                    .background(bgColor)

                    .clickable { onLocationSelected(id) },

                contentAlignment = Alignment.Center

            ) {

                Icon(icon, contentDescription = id, tint = iconColor, modifier = Modifier.size(20.dp))

            }

        }

    }

}



@OptIn(ExperimentalMaterial3Api::class)

@Composable

fun BatchRenameDialog(

    initialBaseName: String,

    initialExtension: String,

    selectedFileNames: List<String>,

    cornerRoundness: Float,

    onDismiss: () -> Unit,

    onRename: (String, String, Int, Int, Boolean, String) -> Unit

) {

    var baseName by remember { mutableStateOf(initialBaseName) }

    var extension by remember { mutableStateOf(initialExtension) }

    var startNumberStr by remember { mutableStateOf("1") }

    var paddingStr by remember { mutableStateOf("0") }

    var isPrefix by remember { mutableStateOf(false) }

    var numberingStyle by remember { mutableStateOf("None") }



    androidx.compose.ui.window.Dialog(

        onDismissRequest = onDismiss,

        properties = androidx.compose.ui.window.DialogProperties(

            usePlatformDefaultWidth = false

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

                            style = MaterialTheme.typography.labelMedium,

                            color = MaterialTheme.colorScheme.primary,

                            modifier = Modifier.padding(bottom = 8.dp)

                        )

                        

                        val pad = paddingStr.toIntOrNull() ?: 0

                        var currentStart = startNumberStr.toIntOrNull() ?: 1

                        

                        selectedFileNames.take(5).forEach { originalName ->

                            val numString = currentStart.toString()

                            val paddedNumStr = if (pad > numString.length) {

                                "0".repeat(pad - numString.length) + numString

                            } else {

                                numString

                            }

                            

                            val numberStr = when (numberingStyle) {

                                "None" -> ""

                                "(1)" -> "($paddedNumStr)"

                                "0001" -> paddedNumStr

                                else -> paddedNumStr

                            }

                            

                            val newNameBuilder = StringBuilder()

                            if (numberingStyle != "None") {

                                if (isPrefix) {

                                    newNameBuilder.append(numberStr)

                                    if (numberingStyle == "0001") newNameBuilder.append("_") else newNameBuilder.append(" ")

                                }

                            }

                            newNameBuilder.append(baseName)

                            if (numberingStyle != "None") {

                                if (!isPrefix) {

                                    if (numberingStyle == "0001") newNameBuilder.append("_") else newNameBuilder.append(" ")

                                    newNameBuilder.append(numberStr)

                                }

                            }

                            if (extension.isNotEmpty()) {

                                newNameBuilder.append(".").append(extension.removePrefix("."))

                            }

                            

                            Text(

                                text = newNameBuilder.toString().trim(),

                                style = MaterialTheme.typography.bodyMedium,

                                color = MaterialTheme.colorScheme.onSurfaceVariant

                            )

                            currentStart++

                        }

                        if (selectedFileNames.size > 5) {

                            Text(

                                text = "And ${selectedFileNames.size - 5} more...",

                                style = MaterialTheme.typography.labelSmall,

                                color = MaterialTheme.colorScheme.onSurfaceVariant,

                                modifier = Modifier.padding(top = 4.dp)

                            )

                        }

                    }

                }

                

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                    OutlinedTextField(

                        value = baseName,

                        onValueChange = { baseName = it },

                        label = { Text("Base Name") },

                        modifier = Modifier.weight(2f),

                        singleLine = true,

                        shape = RoundedCornerShape(12.dp)

                    )

                    OutlinedTextField(

                        value = extension,

                        onValueChange = { extension = it },

                        label = { Text("File Type") },

                        modifier = Modifier.weight(1f),

                        singleLine = true,

                        shape = RoundedCornerShape(12.dp)

                    )

                }



                Text("Numbering Style", style = MaterialTheme.typography.labelMedium)

                Row(

                    horizontalArrangement = Arrangement.spacedBy(8.dp),

                    modifier = Modifier.horizontalScroll(rememberScrollState())

                ) {

                    listOf("None", "(1)", "0001").forEach { style ->

                        FilterChip(

                            selected = numberingStyle == style,

                            onClick = { numberingStyle = style },

                            label = { Text(style) },

                            colors = FilterChipDefaults.filterChipColors(

                                selectedContainerColor = Color(0xFF6B4C41),

                                selectedLabelColor = Color.White

                            )

                        )

                    }

                }



                if (numberingStyle != "None") {

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                        OutlinedTextField(

                            value = startNumberStr,

                            onValueChange = { startNumberStr = it },

                            label = { Text("Starts From") },

                            modifier = Modifier.weight(1f),

                            singleLine = true,

                            shape = RoundedCornerShape(12.dp)

                        )

                        OutlinedTextField(

                            value = paddingStr,

                            onValueChange = { paddingStr = it },

                            label = { Text("Leading Zeros") },

                            modifier = Modifier.weight(1f),

                            singleLine = true,

                            shape = RoundedCornerShape(12.dp)

                        )

                    }



                    Row(

                        verticalAlignment = Alignment.CenterVertically,

                        horizontalArrangement = Arrangement.spacedBy(16.dp)

                    ) {

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isPrefix = true }) {

                            RadioButton(

                                selected = isPrefix,

                                onClick = { isPrefix = true },

                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF6B4C41))

                            )

                            Text("Prefix")

                        }

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isPrefix = false }) {

                            RadioButton(

                                selected = !isPrefix,

                                onClick = { isPrefix = false },

                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF6B4C41))

                            )

                            Text("Suffix")

                        }

                    }

                }



                Row(

                    modifier = Modifier.fillMaxWidth(),

                    verticalAlignment = Alignment.CenterVertically,

                    horizontalArrangement = Arrangement.SpaceBetween

                ) {

                    Text(

                        text = "${selectedFileNames.size} files selected",

                        style = MaterialTheme.typography.labelMedium,

                        color = MaterialTheme.colorScheme.onSurfaceVariant

                    )

                    

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                        TextButton(onClick = onDismiss) {

                            Text("Cancel", color = Color(0xFF6B4C41))

                        }

                        Button(

                            onClick = {

                                val pad = paddingStr.toIntOrNull() ?: 0

                                val start = startNumberStr.toIntOrNull() ?: 1

                                onRename(baseName, extension, pad, start, isPrefix, numberingStyle)

                            },

                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B4C41), contentColor = Color.White)

                        ) {

                            Text("Rename")

                        }

                    }

                }

            }

        }

    }

}





@Composable

fun FabMenuItem(

    label: String,

    icon: androidx.compose.ui.graphics.vector.ImageVector,

    cornerRoundness: Float,

    onClick: () -> Unit

) {

    androidx.compose.material3.Surface(

        shape = getDynamicCornerShape(24f, cornerRoundness),

        color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh,

        onClick = onClick

    ) {

        androidx.compose.foundation.layout.Row(

            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,

            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)

        ) {

            androidx.compose.material3.Text(

                text = label,

                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,

                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,

                modifier = Modifier.padding(end = 12.dp)

            )

            androidx.compose.foundation.layout.Box(

                modifier = Modifier.size(40.dp)

                    .background(

                        androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,

                        getDynamicCornerShape(20f, cornerRoundness)

                    ),

                contentAlignment = androidx.compose.ui.Alignment.Center

            ) {

                androidx.compose.material3.Icon(

                    imageVector = icon,

                    contentDescription = label,

                    tint = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,

                    modifier = Modifier.size(20.dp)

                )

            }

        }

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

    progress: Float,

    modifier: Modifier = Modifier,

    trackColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainerHigh

) {

    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "gradient")

    val offset by infiniteTransition.animateFloat(

        initialValue = 0f,

        targetValue = 1000f,

        animationSpec = androidx.compose.animation.core.infiniteRepeatable(

            animation = androidx.compose.animation.core.tween(2000, easing = androidx.compose.animation.core.LinearEasing),

            repeatMode = androidx.compose.animation.core.RepeatMode.Restart

        ),

        label = "offset"

    )



    val brush = androidx.compose.ui.graphics.Brush.linearGradient(

        colors = listOf(

            MaterialTheme.colorScheme.primary,

            MaterialTheme.colorScheme.tertiary,

            MaterialTheme.colorScheme.secondary,

            MaterialTheme.colorScheme.primary

        ),

        start = androidx.compose.ui.geometry.Offset(offset, 0f),

        end = androidx.compose.ui.geometry.Offset(offset + 500f, 0f),

        tileMode = androidx.compose.ui.graphics.TileMode.Mirror

    )



    androidx.compose.foundation.Canvas(modifier = modifier) {

        val width = size.width

        val height = size.height

        val progressWidth = width * progress

        val strokeW = height



        // Draw track

        drawLine(

            color = trackColor,

            start = androidx.compose.ui.geometry.Offset(0f, height / 2),

            end = androidx.compose.ui.geometry.Offset(width, height / 2),

            strokeWidth = strokeW,

            cap = androidx.compose.ui.graphics.StrokeCap.Round

        )



        // Draw progress with gradient

        if (progressWidth > 0) {

            drawLine(

                brush = brush,

                start = androidx.compose.ui.geometry.Offset(0f, height / 2),

                end = androidx.compose.ui.geometry.Offset(progressWidth, height / 2),

                strokeWidth = strokeW,

                cap = androidx.compose.ui.graphics.StrokeCap.Round

            )

        }

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

                    )

                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(

                    text = usedText,

                    style = MaterialTheme.typography.labelSmall,

                    color = MaterialTheme.colorScheme.onSurfaceVariant,

                    maxLines = 1,

                    overflow = TextOverflow.Ellipsis

                )

            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(verticalArrangement = Arrangement.Center) {

                Text(

                    text = titleText,

                    style = MaterialTheme.typography.labelLarge,

                    fontWeight = FontWeight.Bold,

                    color = MaterialTheme.colorScheme.onSurface,

                    maxLines = 1,

                    overflow = TextOverflow.Ellipsis

                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(

                    text = totalText,

                    style = MaterialTheme.typography.labelSmall,

                    color = MaterialTheme.colorScheme.onSurfaceVariant,

                    maxLines = 1,

                    overflow = TextOverflow.Ellipsis

                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(

                    text = freeText,

                    style = MaterialTheme.typography.labelSmall,

                    color = MaterialTheme.colorScheme.primary,

                    maxLines = 1,

                    overflow = TextOverflow.Ellipsis

                )

            }

        }

    }

}


