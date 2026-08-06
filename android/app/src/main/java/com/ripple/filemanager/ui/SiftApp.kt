package com.ripple.filemanager.ui

import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.with
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.sp
import com.ripple.filemanager.ui.theme.PoppinsFontFamily
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

data class FlyingDelete(
    val id: String,
    val startRect: androidx.compose.ui.geometry.Rect,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val file: FileItem
)

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

        var trashIconCenter by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
        var trashPulse by remember { mutableStateOf(false) }
        LaunchedEffect(trashPulse) {
            if (trashPulse) {
                kotlinx.coroutines.delay(400)
                trashPulse = false
            }
        }
        val itemBounds = remember { mutableMapOf<Int, androidx.compose.ui.geometry.Rect>() }
        val flyingDeletes = remember { androidx.compose.runtime.mutableStateListOf<FlyingDelete>() }

        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
        if (state.showMegaPopup) {
            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { onAction(AppAction.SetShowMegaPopup(false)) },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.material3.Text("Mega")
                    }
                },
                text = {
                    if (state.isMegaAuthenticated) {
                        Column {
                            androidx.compose.material3.Text("Logged in as:")
                            androidx.compose.material3.Text(state.megaAccountEmail ?: "Unknown", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Column {
                            androidx.compose.material3.OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { androidx.compose.material3.Text("Email") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            androidx.compose.material3.OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { androidx.compose.material3.Text("Password") },
                                singleLine = true,
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (state.megaLoginError != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                androidx.compose.material3.Text(state.megaLoginError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (email.isNotBlank() && password.isNotBlank()) {
                                        onAction(AppAction.SetMegaAuthStatus(true, email, password))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                androidx.compose.material3.Text("Login")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                androidx.compose.material3.TextButton(onClick = { uriHandler.openUri("https://mega.io/register") }) {
                                    androidx.compose.material3.Text("Sign Up")
                                }
                                androidx.compose.material3.TextButton(onClick = { uriHandler.openUri("https://mega.io/recovery") }) {
                                    androidx.compose.material3.Text("Forgot Password?")
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    if (state.isMegaAuthenticated) {
                        androidx.compose.material3.TextButton(onClick = {
                            onAction(AppAction.SetMegaAuthStatus(false, null))
                            onAction(AppAction.SetShowMegaPopup(false))
                        }) {
                            androidx.compose.material3.Text("Logout", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { onAction(AppAction.SetShowMegaPopup(false)) }) {
                        androidx.compose.material3.Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                DrawerContent(
                    state = state,
                    onAction = onAction,
                    cornerRoundness = state.cornerRoundness,
                    onCloseDrawer = { drawerScope.launch { drawerState.close() } },
                    onShowAbout = { showAboutScreen = true }
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
                        onLocationSelected = { 
                            onAction(AppAction.SetLocation(it))
                            if (it == "drive" && !state.isGoogleDriveAuthenticated && !state.isMegaAuthenticated && !state.isDropboxAuthenticated) {
                                onAction(AppAction.SetErrorMessage("Sign in to a cloud server in the menu first."))
                            }
                        },
                        modifier = Modifier.width(280.dp)
                    )
                    MainContent(
                        state = state,
                        onAction = onAction,
                        snackbarHostState = snackbarHostState,
                        onDrawerOpen = { drawerScope.launch { drawerState.open() } },
                        modifier = Modifier.weight(1f),
                        onItemPositioned = { id, rect -> itemBounds[id] = rect },
                        onDeleteFlying = { flyingDeletes.add(it) },
                        trashIconCenter = trashIconCenter,
                        onTrashIconPositioned = { trashIconCenter = it },
                        trashPulse = trashPulse,
                        onTrashPulseFinish = { trashPulse = false },
                        itemBounds = itemBounds
                    )
                    if (state.selectedFiles.isNotEmpty()) {
                        val file = state.files.find { it.id == state.selectedFiles.first() }
                        DetailsPane(
                            file = file,
                            cornerRoundness = state.cornerRoundness,
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
                        modifier = Modifier.fillMaxSize(),
                        onItemPositioned = { id, rect -> itemBounds[id] = rect },
                        onDeleteFlying = { flyingDeletes.add(it) },
                        trashIconCenter = trashIconCenter,
                        onTrashIconPositioned = { trashIconCenter = it },
                        trashPulse = trashPulse,
                        onTrashPulseFinish = { trashPulse = false },
                        itemBounds = itemBounds
                    )
                    var fabMenuExpanded by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
                    var showCreateFolderDialog by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
                    var showCreateFileDialog by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
                    var showCleanerIntroDialog by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

                    androidx.activity.compose.BackHandler(enabled = fabMenuExpanded) {
                        fabMenuExpanded = false
                    }

                    val animEnabled = android.animation.ValueAnimator.getDurationScale() > 0f

                    androidx.compose.animation.AnimatedVisibility(
                        visible = fabMenuExpanded,
                        enter = if (animEnabled) androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(180)) else androidx.compose.animation.fadeIn(androidx.compose.animation.core.snap()),
                        exit = if (animEnabled) androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(150)) else androidx.compose.animation.fadeOut(androidx.compose.animation.core.snap()),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f))
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                ) { fabMenuExpanded = false }
                        )
                    }

                    if (!state.isSelectionMode) {
                          val navBottom = androidx.compose.foundation.layout.WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                          val isThreeButton = navBottom > 24.dp
                          Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = navBottom + 8.dp), contentAlignment = Alignment.Center) {
                            RippleBottomNav(
                                currentLocation = state.location,
                                onLocationSelected = { 
                                    onAction(AppAction.SetLocation(it))
                                    if (it == "drive" && !state.isGoogleDriveAuthenticated && !state.isMegaAuthenticated && !state.isDropboxAuthenticated) {
                                        onAction(AppAction.SetErrorMessage("Sign in to a cloud server in the menu first."))
                                    }
                                },
                                onTabTapped = { onAction(AppAction.NavTabTapped(it)) },
                                tapCounters = state.navTapCounters,
                                cornerRoundness = state.cornerRoundness,
                                modifier = Modifier
                            )
                        }

                        val hasClipboardItems = state.clipboardPaths.isNotEmpty()
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = navBottom + 20.dp)
                        ) {
                                val actions = kotlinx.collections.immutable.persistentListOf(
                                    FabMenuAction("New folder", Icons.Default.CreateNewFolder) { showCreateFolderDialog = true; fabMenuExpanded = false }
                                )
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.align(Alignment.BottomEnd).offset(y = (-64).dp)
                                ) {
                                    actions.reversed().forEachIndexed { index, action ->
                                        val delay = if (fabMenuExpanded && animEnabled) index * 40 else 0
                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = fabMenuExpanded,
                                            enter = if (animEnabled) {
                                                androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(180, delayMillis = delay)) +
                                                androidx.compose.animation.scaleIn(
                                                    initialScale = 0.6f,
                                                    animationSpec = androidx.compose.animation.core.spring(
                                                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                                                    )
                                                ) +
                                                androidx.compose.animation.slideInVertically(
                                                    initialOffsetY = { it / 2 },
                                                    animationSpec = androidx.compose.animation.core.tween(200, delayMillis = delay, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                                )
                                            } else {
                                                androidx.compose.animation.fadeIn(androidx.compose.animation.core.snap())
                                            },
                                            exit = if (animEnabled) {
                                                androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120)) + androidx.compose.animation.scaleOut(targetScale = 0.6f, animationSpec = androidx.compose.animation.core.tween(150))
                                            } else {
                                                androidx.compose.animation.fadeOut(androidx.compose.animation.core.snap())
                                            }
                                        ) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                shape = getDynamicCornerShape(50f, state.cornerRoundness),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                onClick = action.onClick
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                                ) {
                                                    Icon(
                                                        action.icon,
                                                        contentDescription = action.label,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Text(
                                                        text = action.label,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontFamily = PoppinsFontFamily,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
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
                                OffsetFab(
                                    onClick = {
                                        if (hasClipboardItems) {
                                            if (state.pasteProgress == null && !state.isPasteComplete) {
                                                onAction(AppAction.PasteClipboard(state.location))
                                            }
                                        } else {
                                            fabMenuExpanded = !fabMenuExpanded
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.BottomEnd),
                                    isExpanded = fabMenuExpanded,
                                    cornerRoundness = state.cornerRoundness
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        if (state.organiseProgress != null) {
                                            androidx.compose.material3.CircularProgressIndicator(
                                                progress = { state.organiseProgress },
                                                modifier = Modifier.fillMaxSize(0.9f),
                                                color = com.ripple.filemanager.ui.theme.SkylineColors.Background,
                                                trackColor = com.ripple.filemanager.ui.theme.SkylineColors.Background.copy(alpha = 0.3f)
                                            )
                                            Text(
                                                "${(state.organiseProgress * 100).toInt()}%",
                                                fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp),
                                                fontWeight = FontWeight.Bold,
                                                color = com.ripple.filemanager.ui.theme.SkylineColors.Background
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
                                                    2 -> Icon(Icons.Default.Check, contentDescription = "Complete", tint = com.ripple.filemanager.ui.theme.SkylineColors.Background)
                                                    1 -> Text("${((state.pasteProgress ?: 0f) * 100).toInt()}%", fontWeight = FontWeight.Bold, color = com.ripple.filemanager.ui.theme.SkylineColors.Background)
                                                    else -> Icon(Icons.Outlined.ContentPaste, contentDescription = "Paste", tint = com.ripple.filemanager.ui.theme.SkylineColors.Background)
                                                }
                                            }
                                        } else {
                                            val rotation by androidx.compose.animation.core.animateFloatAsState(
                                                targetValue = if (fabMenuExpanded) 45f else 0f,
                                                animationSpec = if (animEnabled) androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium) else androidx.compose.animation.core.snap(),
                                                label = "fabRotation"
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = if (fabMenuExpanded) "Close menu" else "New",
                                                modifier = Modifier.graphicsLayer { rotationZ = rotation },
                                                tint = com.ripple.filemanager.ui.theme.SkylineColors.Background
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
                        } // end if (!state.isSelectionMode)
                    } // end MainContent Box
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
                    TrashScreen(
                        state = state,
                        onAction = onAction,
                        onClose = { onAction(AppAction.SetTrashScreenVisible(false)) },
                        onItemPositioned = { id, rect -> itemBounds[id] = rect },
                        onDeleteFlying = { flyingDeletes.add(it) },
                        itemBounds = itemBounds
                    )
                }
            }

            flyingDeletes.forEach { flying ->
                androidx.compose.runtime.key(flying.id) {
                    val progress = remember { androidx.compose.animation.core.Animatable(0f) }
                    LaunchedEffect(flying.id) {
                        progress.animateTo(
                            targetValue = 1f,
                            animationSpec = androidx.compose.animation.core.tween(250, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                        )
                        flyingDeletes.removeAll { it.id == flying.id }
                        trashPulse = true
                    }

                    val currentX = androidx.compose.ui.util.lerp(flying.startRect.center.x, trashIconCenter.x, progress.value)
                    val currentY = androidx.compose.ui.util.lerp(flying.startRect.center.y, trashIconCenter.y, progress.value)
                    val scale = androidx.compose.ui.util.lerp(1f, 0.15f, progress.value)
                    val alpha = androidx.compose.ui.util.lerp(1f, 0f, (progress.value - 0.7f).coerceIn(0f, 0.3f) / 0.3f)

                    val iconTint = if (flying.file?.type == "folder") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

                    Icon(
                        imageVector = flying.icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = currentX - (size.width / 2)
                                translationY = currentY - (size.height / 2)
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                            }
                            .size(40.dp)
                    )
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
                    if (state.viewingFile!!.type == "image") {
                        val imageFiles = state.files.filter { it.type == "image" }
                        val initialIndex = imageFiles.indexOfFirst { it.id == state.viewingFile!!.id }.coerceAtLeast(0)
                        ImageViewerScreen(
                            files = imageFiles,
                            initialIndex = initialIndex,
                            onAction = onAction,
                            onClose = { onAction(AppAction.CloseFileViewer) },
                            onDeleteClick = { file -> 
                                onAction(AppAction.ClearSelection)
                                onAction(AppAction.ToggleSelection(file.id))
                                onAction(AppAction.DeleteSelectedFiles)
                            },
                            onNavigateToFolder = { path ->
                                onAction(AppAction.SetLocation(path))
                            }
                        )
                    } else {
                        val isDocument = state.viewingFile!!.name.endsWith(".pdf", true) ||
                                         state.viewingFile!!.name.endsWith(".docx", true) ||
                                         state.viewingFile!!.name.endsWith(".txt", true) ||
                                         state.viewingFile!!.name.endsWith(".md", true) ||
                                         state.viewingFile!!.type == "doc"
                        if (isDocument) {
                            DocumentViewerScreen(
                                fileItem = state.viewingFile!!,
                                onClose = { onAction(AppAction.CloseFileViewer) },
                                onAction = onAction
                            )
                        } else {
                            FileViewerScreen(
                                fileItem = state.viewingFile!!,
                                onClose = { onAction(AppAction.CloseFileViewer) }
                            )
                        }
                    }
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

    } // end ModalNavigationDrawer
}

@Composable
fun DrawerContent(
    state: AppState,
    onAction: (AppAction) -> Unit,
    cornerRoundness: Float,
    onCloseDrawer: () -> Unit,
    onShowAbout: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showGDrivePopup by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showDropboxPopup by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    
    val googleSignInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            if (account?.email != null) {
                onAction(AppAction.SetGoogleDriveAuthStatus(true, account.email!!))
            }
        } catch (e: Exception) {
            onAction(AppAction.SetErrorMessage("Google Login Failed: ${e.message}"))
        }
        showGDrivePopup = false
    }
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.width(300.dp)
    ) {
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

            Spacer(modifier = Modifier.height(4.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

            // Menu items
            var isCloudExpanded by remember { mutableStateOf(false) }

            // Link cloud - expandable section
            Surface(
                shape = getDynamicCornerShape(14f, cornerRoundness),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, com.ripple.filemanager.ui.theme.SkylineColors.Border),
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
                            // Google Drive
                            Surface(
                                shape = getDynamicCornerShape(12f, cornerRoundness),
                                color = if (state.isGoogleDriveAuthenticated) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = if (state.isGoogleDriveAuthenticated) null else BorderStroke(1.dp, com.ripple.filemanager.ui.theme.SkylineColors.Border),
                                onClick = { showGDrivePopup = true },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize().padding(4.dp)
                                ) {
                                    Icon(
                                        if (state.isGoogleDriveAuthenticated) Icons.Default.CloudDone else Icons.Default.Cloud,
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
                                color = if (state.isMegaAuthenticated) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = if (state.isMegaAuthenticated) null else BorderStroke(1.dp, com.ripple.filemanager.ui.theme.SkylineColors.Border),
                                onClick = { onAction(AppAction.SetShowMegaPopup(true)) },
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
                                color = if (state.isDropboxAuthenticated) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = if (state.isDropboxAuthenticated) null else BorderStroke(1.dp, com.ripple.filemanager.ui.theme.SkylineColors.Border),
                                onClick = { showDropboxPopup = true },
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
                    }
                }
            }
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
                    onShowAbout()
                }
            )
        }
        
        if (showGDrivePopup) {
            val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(com.google.android.gms.common.api.Scope(com.google.api.services.drive.DriveScopes.DRIVE))
                .build()
            val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)

            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showGDrivePopup = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                title = { androidx.compose.material3.Text("Google Drive", style = MaterialTheme.typography.headlineSmall) },
                text = {
                    if (state.isGoogleDriveAuthenticated) {
                        androidx.compose.material3.Text("Logged in as:\n${state.googleDriveAccountEmail ?: "Unknown"}", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        androidx.compose.material3.Text("Sign in to Google Drive to access your cloud files securely.", style = MaterialTheme.typography.bodyMedium)
                    }
                },
                confirmButton = {
                    if (state.isGoogleDriveAuthenticated) {
                        androidx.compose.material3.TextButton(onClick = {
                            showGDrivePopup = false
                            onCloseDrawer()
                            onAction(AppAction.SetLocation("drive"))
                        }) {
                            androidx.compose.material3.Text("View Files", color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        androidx.compose.material3.TextButton(onClick = {
                            googleSignInClient.signOut().addOnCompleteListener {
                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                            }
                        }) {
                            androidx.compose.material3.Text("Login", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                dismissButton = {
                    if (state.isGoogleDriveAuthenticated) {
                        androidx.compose.material3.TextButton(onClick = {
                            googleSignInClient.signOut().addOnCompleteListener {
                                onAction(AppAction.SetGoogleDriveAuthStatus(false, null))
                            }
                            showGDrivePopup = false
                        }) {
                            androidx.compose.material3.Text("Logout", color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        androidx.compose.material3.TextButton(onClick = { showGDrivePopup = false }) {
                            androidx.compose.material3.Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            )
        }



        if (showDropboxPopup) {
            var email by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
            var password by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showDropboxPopup = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.material3.Text("Dropbox")
                    }
                },
                text = {
                    if (state.isDropboxAuthenticated) {
                        Column {
                            androidx.compose.material3.Text("Logged in as:")
                            androidx.compose.material3.Text(state.dropboxAccountEmail ?: "Unknown", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Column {
                            androidx.compose.material3.OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { androidx.compose.material3.Text("Email") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            androidx.compose.material3.OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { androidx.compose.material3.Text("Password") },
                                singleLine = true,
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (email.isNotBlank() && password.isNotBlank()) {
                                        onAction(AppAction.SetDropboxAuthStatus(true, email))
                                        showDropboxPopup = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                androidx.compose.material3.Text("Login")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                androidx.compose.material3.TextButton(onClick = { /* Handle Sign Up */ }) {
                                    androidx.compose.material3.Text("Sign Up")
                                }
                                androidx.compose.material3.TextButton(onClick = { /* Handle Forgot Password */ }) {
                                    androidx.compose.material3.Text("Forgot Password?")
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    if (state.isDropboxAuthenticated) {
                        androidx.compose.material3.TextButton(onClick = {
                            onAction(AppAction.SetDropboxAuthStatus(false, null))
                            showDropboxPopup = false
                        }) {
                            androidx.compose.material3.Text("Logout", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showDropboxPopup = false }) {
                        androidx.compose.material3.Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
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
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, com.ripple.filemanager.ui.theme.SkylineColors.Border),
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
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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

@androidx.compose.runtime.Immutable
data class FabMenuAction(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
    state: AppState,
    onAction: (AppAction) -> Unit,
    snackbarHostState: SnackbarHostState,
    onDrawerOpen: () -> Unit = {},
    modifier: Modifier = Modifier,
    onItemPositioned: (Int, androidx.compose.ui.geometry.Rect) -> Unit = { _, _ -> },
    onDeleteFlying: (FlyingDelete) -> Unit = {},
    trashIconCenter: androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset.Zero,
    onTrashIconPositioned: (androidx.compose.ui.geometry.Offset) -> Unit = {},
    trashPulse: Boolean = false,
    onTrashPulseFinish: () -> Unit = {},
    itemBounds: Map<Int, androidx.compose.ui.geometry.Rect> = emptyMap()
) {
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
    
    BackHandler(enabled = state.query.isNotEmpty()) {
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
            } else if (state.location == "drive" || state.location.startsWith("drive_id:") ||
                       state.location == "mega" || state.location.startsWith("mega_id:") ||
                       state.location == "dropbox" || state.location.startsWith("dropbox_id:")) {
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
            Box {
                SkylineTopBar(
                    query = state.query,
                    onQueryChange = { onAction(AppAction.SetQuery(it)) },
                    onMenuClick = onDrawerOpen,
                    onTrashClick = {
                        onAction(AppAction.SetTrashScreenVisible(true))
                    },
                    cornerRoundness = state.cornerRoundness
                )
                // Preserve trash pulse animation as an overlay on the trash icon area
                androidx.compose.animation.AnimatedVisibility(
                    visible = trashPulse,
                    enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(150)),
                    exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(400)),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(top = 8.dp, end = 12.dp)
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                    colors = listOf(
                                        com.ripple.filemanager.ui.theme.SkylineColors.Amber.copy(alpha = 0.4f),
                                        androidx.compose.ui.graphics.Color.Transparent
                                    )
                                )
                            )
                            .onGloballyPositioned { onTrashIconPositioned(it.boundsInRoot().center) }
                    )
                }
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



            if (state.location == "drive" && !state.isGoogleDriveAuthenticated && !state.isMegaAuthenticated && !state.isDropboxAuthenticated) {
                var unauthCloudIndex by remember { mutableStateOf(0) }
                LaunchedEffect(Unit) {
                    while (true) {
                        kotlinx.coroutines.delay(2500)
                        unauthCloudIndex = (unauthCloudIndex + 1) % 3
                    }
                }
                
                val cloudNames = listOf("Google Drive", "Mega", "Dropbox")
                val currentCloud = cloudNames[unauthCloudIndex]
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimatedContent(
                        targetState = currentCloud,
                        transitionSpec = {
                            (slideInVertically { height -> height } + fadeIn()) togetherWith
                                (slideOutVertically { height -> -height } + fadeOut())
                        },
                        label = "cloud_anim"
                    ) { targetCloud ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Cloud, contentDescription = targetCloud, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(targetCloud, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
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
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        modifier = Modifier.width(250.dp)
                    ) {
                        Text("Sign in to Google Drive", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onAction(AppAction.SetShowMegaPopup(true)) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        modifier = Modifier.width(250.dp)
                    ) {
                        Text("Sign in to Mega", fontWeight = FontWeight.Bold)
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

                    val targetLocation = state.location
var showSortMenu by remember { mutableStateOf(false) }
Column {
if (state.storageTotalGb > 0f && (targetLocation == "home" || targetLocation == "drive" || targetLocation == "recent" || targetLocation == "starred" || targetLocation.startsWith("drive_id:") || targetLocation.startsWith(android.os.Environment.getExternalStorageDirectory().absolutePath))) {
    val isScrollable = (state.sdCardStorageTotalGb > 0 && state.isGoogleDriveAuthenticated)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .then(if (isScrollable) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val usedText = "%.1f GB".format(state.storageTotalGb - state.storageFreeGb)
        val totalText = "%.1f GB".format(state.storageTotalGb)
        val freeText = "%.1f GB free".format(state.storageFreeGb)
        val progress = if (state.storageTotalGb > 0) ((state.storageTotalGb - state.storageFreeGb) / state.storageTotalGb) else 0f
        
        val cardModifier = if (isScrollable) Modifier.width(180.dp) else Modifier.weight(1f)


        StorageCard(
            modifier = cardModifier.fillMaxHeight(),
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
                modifier = cardModifier.fillMaxHeight(),
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

        data class CloudData(val name: String, val used: String, val total: String, val free: String, val progress: Float, val icon: androidx.compose.ui.graphics.vector.ImageVector)
        val activeClouds = mutableListOf<CloudData>()
        if (state.isGoogleDriveAuthenticated) {
            activeClouds.add(CloudData("GDrive", formatSize(state.driveStorageTotalBytes - state.driveStorageFreeBytes), formatSize(state.driveStorageTotalBytes), "${formatSize(state.driveStorageFreeBytes)} free", if (state.driveStorageTotalBytes > 0L) ((state.driveStorageTotalBytes - state.driveStorageFreeBytes).toFloat() / state.driveStorageTotalBytes.toFloat()) else 0f, Icons.Default.Cloud))
        }
        if (state.isMegaAuthenticated) {
            activeClouds.add(CloudData("Mega", "0 B", "15 GB", "15.0 GB free", 0f, Icons.Default.Cloud))
        }
        if (state.isDropboxAuthenticated) {
            activeClouds.add(CloudData("Dropbox", "0 B", "2 GB", "2.0 GB free", 0f, Icons.Default.Cloud))
        }

        if (activeClouds.isNotEmpty()) {
            val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { activeClouds.size })
            androidx.compose.foundation.pager.VerticalPager(state = pagerState, modifier = cardModifier.fillMaxHeight()) { page ->
                val cloud = activeClouds[page]
                StorageCard(
                    modifier = Modifier.fillMaxSize(),
                    icon = cloud.icon,
                    titleText = cloud.name,
                    usedText = cloud.used,
                    totalText = cloud.total,
                    freeText = cloud.free,
                    progress = cloud.progress,
                    cornerRoundness = state.cornerRoundness,
                    onClick = { /* Navigate to drive */ }
                )
            }
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
        "audio" to Icons.Outlined.MusicNote, 
        "video" to Icons.Outlined.PermMedia, 
        "doc" to Icons.Outlined.InsertDriveFile,
        "apk" to Icons.Outlined.Android
    ).forEach { (id, icon) ->
        val active = state.filter == id || (id == "video" && state.filter == "media") || (id == "audio" && state.filter == "music")
        val toneColor = com.ripple.filemanager.ui.theme.fileTypeTone(id)
        val shape = getDynamicCornerShape(18f, state.cornerRoundness)
        Box(
            modifier = Modifier
                .heightIn(min = 36.dp)
                .border(if (active) 2.dp else 1.dp, if (active) toneColor else MaterialTheme.colorScheme.outlineVariant, shape)
                .clip(shape)
                .background(if (active) toneColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                .clickable { 
                    if (active) onAction(AppAction.SetFilter("all")) else onAction(AppAction.SetFilter(id))
                }
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = id,
                tint = if (active) toneColor else MaterialTheme.colorScheme.onSurfaceVariant,
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
    if (targetLocation.startsWith("/") && targetLocation.length > rootPath.length) {
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
        
        val relativePath = targetLocation.removePrefix(rootPath).removePrefix("/")
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
    } else if (!targetLocation.startsWith("/")) {
        if (targetLocation.startsWith("drive") || targetLocation.startsWith("mega") || targetLocation.startsWith("dropbox")) {
            val currentCloudName = when {
                targetLocation.startsWith("mega") -> "Mega"
                targetLocation.startsWith("dropbox") -> "Dropbox"
                else -> "Google Drive"
            }
            Icon(Icons.Default.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            
            var cloudMenuExpanded by remember { mutableStateOf(false) }
            Box {
                Row(
                    modifier = Modifier
                        .clip(getDynamicCornerShape(12f, state.cornerRoundness))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { cloudMenuExpanded = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentCloudName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Cloud", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
                
                DropdownMenu(
                    expanded = cloudMenuExpanded,
                    onDismissRequest = { cloudMenuExpanded = false }
                ) {
                    if (state.isGoogleDriveAuthenticated) {
                        DropdownMenuItem(
                            text = { Text("Google Drive") },
                            onClick = { onAction(AppAction.SetLocation("drive")); cloudMenuExpanded = false }
                        )
                    }
                    if (state.isMegaAuthenticated) {
                        DropdownMenuItem(
                            text = { Text("Mega") },
                            onClick = { onAction(AppAction.SetLocation("mega")); cloudMenuExpanded = false }
                        )
                    }
                    if (state.isDropboxAuthenticated) {
                        DropdownMenuItem(
                            text = { Text("Dropbox") },
                            onClick = { onAction(AppAction.SetLocation("dropbox")); cloudMenuExpanded = false }
                        )
                    }
                }
            }

            if (targetLocation != "drive" && targetLocation != "mega" && targetLocation != "dropbox") {
                state.driveFolderStack.forEach { (loc, name) ->
                    if (loc != "drive" && loc != "mega" && loc != "dropbox") {
                        Text(" / ", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(getDynamicCornerShape(4f, state.cornerRoundness))
                                .clickable { onAction(AppAction.SetLocation(loc, name)) }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(" / ", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                Text(state.currentFolderName ?: "Folder", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        } else {
            val displayLoc = targetLocation.replaceFirstChar { it.uppercase() }
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
        val shape = getDynamicCornerShape(20f, state.cornerRoundness)
        Box(
            modifier = Modifier
                .clip(shape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                .background(MaterialTheme.colorScheme.surface)
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
    val shape = getDynamicCornerShape(20f, state.cornerRoundness)
    Box(
        modifier = Modifier
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .background(MaterialTheme.colorScheme.surface)
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


                androidx.compose.animation.AnimatedContent(
                    targetState = state,
                    contentKey = { it.location },
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        val duration = if (android.animation.ValueAnimator.getDurationScale() == 0f) 0 else 300
                        val fadeDuration = if (android.animation.ValueAnimator.getDurationScale() == 0f) 0 else 200
                        val easing = androidx.compose.animation.core.FastOutSlowInEasing
                        
                        val isForward = when {
                            initialState.location == "home" && targetState.location != "home" -> true
                            targetState.location == "home" && initialState.location != "home" -> false
                            initialState.location.startsWith("/") && targetState.location.startsWith("/") -> targetState.location.length > initialState.location.length
                            initialState.driveFolderStack.size != targetState.driveFolderStack.size -> targetState.driveFolderStack.size > initialState.driveFolderStack.size
                            else -> true
                        }

                        val slideInOffset = if (isForward) { fullWidth: Int -> fullWidth } else { fullWidth: Int -> -(fullWidth * 0.3f).toInt() }
                        val slideOutOffset = if (isForward) { fullWidth: Int -> -(fullWidth * 0.3f).toInt() } else { fullWidth: Int -> fullWidth }

                        (androidx.compose.animation.slideInHorizontally(
                            animationSpec = androidx.compose.animation.core.tween(duration, easing = easing),
                            initialOffsetX = slideInOffset
                        ) + androidx.compose.animation.fadeIn(
                            animationSpec = androidx.compose.animation.core.tween(fadeDuration)
                        )).togetherWith(
                            androidx.compose.animation.slideOutHorizontally(
                                animationSpec = androidx.compose.animation.core.tween(duration, easing = easing),
                                targetOffsetX = slideOutOffset
                            ) + androidx.compose.animation.scaleOut(
                                targetScale = 0.94f,
                                animationSpec = androidx.compose.animation.core.tween(duration)
                            ) + androidx.compose.animation.fadeOut(
                                targetAlpha = 0.5f,
                                animationSpec = androidx.compose.animation.core.tween(duration)
                            )
                        ).using(androidx.compose.animation.SizeTransform(clip = false))
                    },
                    label = "folder_transition"
                ) { targetStateSnapshot ->
                    val state = targetStateSnapshot
                    val targetLocation = state.location
                    androidx.compose.foundation.layout.Box {
                                        FileGrid(
                                            folderState = if (state.isLoading) FolderListUiState.Loading else FolderListUiState.Loaded(state.files),
                                            pasteLoadingCount = state.pasteLoadingCount,
                                            emptyState = {
                                                if (!(!targetLocation.contains("Android/data") && !targetLocation.contains("Android/obb") || state.hasShizuku)) {
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
                                                                                        selectedFiles = state.selectedFiles,
                                            isListMode = state.isListMode,
                                            iconShape = state.activeIconShape,
                                            searchQuery = state.query,
                                            onFileClick = { file -> 
                                                if (state.query.isNotEmpty()) {
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
                                            onItemPositioned = onItemPositioned,
                                            cornerRoundness = state.cornerRoundness,
                                            gridColumns = state.gridColumns,
                                            modifier = Modifier.fillMaxSize()
                                        )

                        val isOutgoing = targetLocation != state.location
                        if (isOutgoing) {
                            val progress by transition.animateFloat(
                                transitionSpec = { androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing) },
                                label = "scrim"
                            ) { enterExitState ->
                                when (enterExitState) {
                                    androidx.compose.animation.EnterExitState.PreEnter -> 0f
                                    androidx.compose.animation.EnterExitState.Visible -> 0f
                                    androidx.compose.animation.EnterExitState.PostExit -> 1f
                                }
                            }
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = androidx.compose.ui.util.lerp(0f, 0.35f, progress)))
                            )
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
                        val durationScale = android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
                        if (durationScale > 0f) {
                            state.selectedFiles.forEach { id ->
                                val file = state.files.find { it.id == id } ?: state.trashFiles.find { it.id == id }
                                val rect = itemBounds[id]
                                if (file != null && rect != null) {
                                    val icon = if (file.type == "folder") androidx.compose.material.icons.Icons.Default.Folder else androidx.compose.material.icons.Icons.Outlined.InsertDriveFile
                                    onDeleteFlying(FlyingDelete(id = id.toString(), startRect = rect, icon = icon, file = file))
                                }
                            }
                        }
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
    val active = if (id == "drive") {
        currentId == "drive" || currentId.startsWith("drive_id:") ||
        currentId == "mega" || currentId.startsWith("mega_id:") ||
        currentId == "dropbox" || currentId.startsWith("dropbox_id:")
    } else if (id == "home") {
        currentId == "home" || currentId.startsWith(android.os.Environment.getExternalStorageDirectory().absolutePath)
    } else {
        currentId == id
    }
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
fun RippleBottomNav(
    currentLocation: String, 
    onLocationSelected: (String) -> Unit, 
    onTabTapped: (String) -> Unit,
    tapCounters: kotlinx.collections.immutable.ImmutableMap<String, Int>,
    cornerRoundness: Float, 
    modifier: Modifier = Modifier
) {
    val items = listOf(
        "home" to androidx.compose.material.icons.Icons.Default.Home,
        "recent" to androidx.compose.material.icons.Icons.Default.Schedule,
        "starred" to androidx.compose.material.icons.Icons.Default.Star,
        "drive" to androidx.compose.material.icons.Icons.Default.Cloud
    )
    
    val selectedIndex = items.indexOfFirst { (id, _) ->
        if (id == "drive") {
            currentLocation == "drive" || currentLocation.startsWith("drive_id:") ||
            currentLocation == "mega" || currentLocation.startsWith("mega_id:") ||
            currentLocation == "dropbox" || currentLocation.startsWith("dropbox_id:")
        } else if (id == "home") {
            currentLocation == "home" || currentLocation.startsWith(android.os.Environment.getExternalStorageDirectory().absolutePath)
        } else {
            currentLocation == id
        }
    }.coerceAtLeast(0)

    val selectedId = items.getOrNull(selectedIndex)?.first ?: "home"

    BottomNavBar(
        items = items.map { (id, icon) ->
            val label = when(id) {
                "home"    -> "Home"
                "recent"  -> "Recent"
                "starred" -> "Starred"
                "drive"   -> "Cloud"
                else      -> id
            }
            BottomNavItem(id = id, icon = icon, label = label)
        },
        selectedId = selectedId,
        tapCounters = tapCounters,
        cornerRoundness = cornerRoundness,
        onSelect = { 
            onTabTapped(it)
            onLocationSelected(it) 
        },
        modifier = modifier
    )
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
        BlueprintCard(
            modifier = modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            heroEmphasis = false,
            cornerRoundness = cornerRoundness
        ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Top mono label (e.g. LOCAL / GDRIVE) and total size
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                com.ripple.filemanager.ui.MonoLabel(
                    text = titleText.uppercase(),
                    color = com.ripple.filemanager.ui.theme.SkylineColors.AmberDim,
                    fontSize = 11
                )
                com.ripple.filemanager.ui.MonoLabel(
                    text = totalText,
                    color = com.ripple.filemanager.ui.theme.SkylineColors.TextDim,
                    fontSize = 10
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Fraunces headline (Free space), allowed to wrap
            Text(
                text = freeText,
                style = MaterialTheme.typography.headlineMedium,
                color = com.ripple.filemanager.ui.theme.SkylineColors.TextPrimary
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Subline: OF x GB · y GB USED
            com.ripple.filemanager.ui.MonoLabel(
                text = "OF $totalText · $usedText",
                color = com.ripple.filemanager.ui.theme.SkylineColors.TextDim,
                fontSize = 10
            )
        }
    }
}
