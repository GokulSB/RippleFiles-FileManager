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
import androidx.compose.runtime.DisposableEffect


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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
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
import com.ripple.filemanager.ui.expressive.*
import com.ripple.filemanager.ui.theme.SiftTheme
import com.ripple.filemanager.ui.theme.JetBrainsMonoFamily
import com.ripple.filemanager.ui.theme.ManropeFontFamily
import com.ripple.filemanager.ui.theme.OutfitFontFamily
import com.ripple.filemanager.ui.theme.LocalAppFont
import com.ripple.filemanager.ui.theme.SkylineColors
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.res.stringResource
import com.ripple.filemanager.R

@Composable
fun getDynamicCornerShape(defaultRadius: Float, cornerRoundness: Float): RoundedCornerShape {
    return RoundedCornerShape((defaultRadius * (cornerRoundness * 2)).coerceIn(0f, 100f).dp)
}

@androidx.compose.runtime.Immutable
data class FlyingDelete(
    val id: String,
    val startRect: androidx.compose.ui.geometry.Rect,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val file: FileItem
)

@Composable
fun SiftApp(
    state: AppState,
    onActionOrig: (AppAction) -> Unit,
    snackbarHostState: androidx.compose.material3.SnackbarHostState,
    windowWidthSizeClass: androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
) {
    val isWideScreen = windowWidthSizeClass != WindowWidthSizeClass.Compact

    val haptics = com.ripple.filemanager.haptics.rememberHapticsController { state.haptics }
    androidx.compose.runtime.CompositionLocalProvider(com.ripple.filemanager.haptics.LocalHaptics provides haptics) {
        val onAction: (AppAction) -> Unit = { action ->
            when (action) {
                is AppAction.DeleteSelectedFiles,
                is AppAction.PermanentlyDeleteTrashFiles,
                is AppAction.DeleteSelectedCleanerFiles -> haptics.delete()
                is AppAction.SetClipboard,
                is AppAction.PasteClipboard -> haptics.copyPaste()
                is AppAction.OpenFileViewer,
                is AppAction.PlayAudio -> haptics.fileOpen()
                is AppAction.SetShowSettingsScreen,
                is AppAction.ToggleHapticsMaster,
                is AppAction.ToggleHapticsOption,
                is AppAction.SetRecycleBinSettings -> haptics.settingsToggle()
                is AppAction.SetCleanerScreenVisible,
                is AppAction.SetCleanerCategory,
                is AppAction.SelectAllCleanerFiles,
                is AppAction.ClearCleanerSelection -> haptics.cleaner()
                is AppAction.CreateFolder,
                is AppAction.CreateFile -> haptics.fab()
                is AppAction.Reload,
                is AppAction.LoadFileDetails,
                is AppAction.AuthSuccess,
                is AppAction.SetGoogleDriveAuthStatus,
                is AppAction.SetMegaAuthStatus,
                is AppAction.SetDropboxAuthStatus,
                is AppAction.AutoRequestAccess -> {}
                else -> haptics.tap()
            }
            onActionOrig(action)
        }

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
        lightnessOffset = state.themeLightnessOffset,
        fontStyle = state.fontStyle,
        textDecorations = state.textDecorations,
        mainTextScale = state.mainTextScale,
        subTextScale = state.subTextScale,
        invertText = state.invertText,
        cornerRoundness = state.cornerRoundness
    ) {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val drawerScope = rememberCoroutineScope()
        var showAboutScreen by remember { mutableStateOf(false) }
        var returnToCleanerOnCategoryBack by remember { mutableStateOf(false) }

        var trashIconCenter by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
        var trashPulse by remember { mutableStateOf(false) }
        if (state.showMegaPopup) {
            if (state.isMegaAuthenticated) {
                ExpressiveConfirmationSheet(
                    title = "Log out of Mega?",
                    subtitle = state.megaAccountEmail ?: "Connected account",
                    confirmLabel = stringResource(R.string.logout_action),
                    cancelLabel = stringResource(R.string.cancel),
                    onConfirm = {
                        onAction(AppAction.SetMegaAuthStatus(false, null, null))
                        onAction(AppAction.SetShowMegaPopup(false))
                        if (state.location.startsWith("mega")) {
                            onAction(AppAction.SetLocation("cloud"))
                        }
                    },
                    onDismissRequest = {
                        onAction(AppAction.SetShowMegaPopup(false))
                    }
                )
            } else {
                CloudLoginBottomSheet(
                    providerName = "Mega",
                    providerIcon = Icons.Outlined.Cloud,
                    providerCategory = "archives",
                    errorMessage = state.megaLoginError,
                    registerUrl = "https://mega.io/register",
                    recoveryUrl = "https://mega.io/recovery",
                    onLogin = { email, password ->
                        onAction(AppAction.SetMegaAuthStatus(true, email, password))
                    },
                    onDismissRequest = {
                        onAction(AppAction.SetShowMegaPopup(false))
                    }
                )
            }
        }

        if (state.authRequestReason != null && state.authRequestPath != null) {
            AuthDialog(
                errorMessage = state.authErrorMessage,
                onDismiss = { onAction(AppAction.CancelAuth) },
                onConfirm = { password -> 
                    onAction(AppAction.AuthSuccess(
                        reason = state.authRequestReason,
                        path = state.authRequestPath,
                        fileId = state.authRequestFileId,
                        folderName = state.authRequestFolderName,
                        password = password
                    ))
                },
                onShowToast = { onAction(AppAction.ShowToast(it)) }
            )
        }

        val isAppReducedMotion = com.ripple.filemanager.ui.expressive.ExpressiveMotion.isReducedMotion()
        val modalEnter = if (isAppReducedMotion) {
            fadeIn(androidx.compose.animation.core.snap())
        } else {
            slideInVertically(animationSpec = androidx.compose.animation.core.tween(220, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { (it * 0.08f).toInt() } +
            fadeIn(animationSpec = androidx.compose.animation.core.tween(200, easing = androidx.compose.animation.core.FastOutSlowInEasing))
        }
        val modalExit = if (isAppReducedMotion) {
            fadeOut(androidx.compose.animation.core.snap())
        } else {
            slideOutVertically(animationSpec = androidx.compose.animation.core.tween(180, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { (it * 0.08f).toInt() } +
            fadeOut(animationSpec = androidx.compose.animation.core.tween(150, easing = androidx.compose.animation.core.LinearEasing))
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
            color = com.ripple.filemanager.ui.expressive.ExpressiveTheme.colors.bg,
            contentColor = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxSize()
        ) {
            // Hoisted stable background layer drawn once behind navigation (never recomposed or animated during screen transitions)
            com.ripple.filemanager.ui.expressive.RippleBackground(modifier = Modifier.fillMaxSize())

            val currentScreen = when {
                showAboutScreen -> "about"
                state.showSettingsScreen -> "settings"
                state.showTrashScreen -> "trash"
                state.showCleanerScreen -> "cleaner"
                else -> state.location
            }
            val isModalScreen = currentScreen in setOf("cleaner", "trash", "settings", "about")

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
                        returnToCleanerOnCategoryBack = returnToCleanerOnCategoryBack,
                        onResetReturnToCleaner = { returnToCleanerOnCategoryBack = false },
                        showAboutScreen = showAboutScreen,
                        onCloseAbout = { showAboutScreen = false },
                        onSetReturnToCleaner = { returnToCleanerOnCategoryBack = it },
                        modifier = Modifier.weight(1f)
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
                        returnToCleanerOnCategoryBack = returnToCleanerOnCategoryBack,
                        onResetReturnToCleaner = { returnToCleanerOnCategoryBack = false },
                        showAboutScreen = showAboutScreen,
                        onCloseAbout = { showAboutScreen = false },
                        onSetReturnToCleaner = { returnToCleanerOnCategoryBack = it },
                        modifier = Modifier.fillMaxSize()
                    )
                    var fabMenuExpanded by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
                    var showCreateFolderDialog by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
                    var showCreateFileDialog by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
                    var showCleanerIntroDialog by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

                    androidx.activity.compose.BackHandler(enabled = fabMenuExpanded) {
                        fabMenuExpanded = false
                    }

                    val animEnabled = android.animation.ValueAnimator.getDurationScale() > 0f

                    val scrimAlpha by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (fabMenuExpanded && !isModalScreen) 0.35f else 0f,
                        animationSpec = if (animEnabled) androidx.compose.animation.core.tween(if (fabMenuExpanded) 180 else 150) else androidx.compose.animation.core.snap(),
                        label = "scrim_alpha"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = scrimAlpha }
                            .background(androidx.compose.ui.graphics.Color.Black)
                            .then(
                                if (fabMenuExpanded && !isModalScreen) {
                                    Modifier.clickable(
                                        indication = null,
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                    ) { fabMenuExpanded = false }
                                } else {
                                    Modifier
                                }
                            )
                    )

                    if (!state.isSelectionMode && !isModalScreen) {
                          val navBottom = androidx.compose.foundation.layout.WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                          val hasClipboardItems = state.clipboardPaths.isNotEmpty()
                          
                          // Menu and Small FABs (Absolute Positioned above the Bottom Row)
                          Box(
                              modifier = Modifier
                                  .align(Alignment.BottomEnd)
                                  .padding(end = 24.dp, bottom = navBottom + 24.dp + 68.dp + 16.dp)
                          ) {
                              val actions = kotlinx.collections.immutable.persistentListOf(
                                  FabMenuAction("New folder", Icons.Default.CreateNewFolder) { showCreateFolderDialog = true; fabMenuExpanded = false }
                              )
                              Column(
                                  horizontalAlignment = Alignment.End,
                                  verticalArrangement = Arrangement.spacedBy(12.dp),
                                  modifier = Modifier.align(Alignment.BottomEnd)
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
                          }
                          

                            
                            if (showCreateFolderDialog) {
                                var folderName by remember { mutableStateOf("") }
                                com.ripple.filemanager.ui.GradientAlertDialog(
                                    onDismissRequest = { showCreateFolderDialog = false },
                                    title = { com.ripple.filemanager.ui.MonoLabel("NEW FOLDER", color = com.ripple.filemanager.ui.theme.SkylineColors.Amber, fontSize = 14) },
                                    text = {
                                        OutlinedTextField(
                                            value = folderName,
                                            onValueChange = { folderName = it },
                                            label = { Text(stringResource(R.string.folder_name_label)) },
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
                                            Text(stringResource(R.string.save_action), color = com.ripple.filemanager.ui.theme.SkylineColors.Amber)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { haptics.tap(); showCreateFolderDialog = false }) {
                                            Text(stringResource(R.string.cancel), color = com.ripple.filemanager.ui.theme.SkylineColors.TextDim)
                                        }
                                    },
                                    containerColor = com.ripple.filemanager.ui.theme.SkylineColors.Surface,
                                    shape = getDynamicCornerShape(12f, state.cornerRoundness)
                                )
                            }
                            
                            if (showCreateFileDialog) {
                                var fileName by remember { mutableStateOf("") }
                                com.ripple.filemanager.ui.GradientAlertDialog(
                                    onDismissRequest = { showCreateFileDialog = false },
                                    title = { com.ripple.filemanager.ui.MonoLabel("NEW FILE", color = com.ripple.filemanager.ui.theme.SkylineColors.Amber, fontSize = 14) },
                                    text = {
                                        OutlinedTextField(
                                            value = fileName,
                                            onValueChange = { fileName = it },
                                            label = { Text(stringResource(R.string.file_name_label)) },
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
                                            Text(stringResource(R.string.save_action), color = com.ripple.filemanager.ui.theme.SkylineColors.Amber)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { haptics.tap(); showCreateFileDialog = false }) {
                                            Text(stringResource(R.string.cancel), color = com.ripple.filemanager.ui.theme.SkylineColors.TextDim)
                                        }
                                    },
                                    containerColor = com.ripple.filemanager.ui.theme.SkylineColors.Surface,
                                    shape = getDynamicCornerShape(12f, state.cornerRoundness)
                                )
                            }
                        } // end if (!state.isSelectionMode)
                    } // end MainContent Box
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
                enter = modalEnter,
                exit = modalExit
            ) {
                if (state.viewingFile != null) {
                    if (state.viewingFile!!.type == "image") {
                        val currentViewing = state.viewingFile!!
                        val baseImages = state.files.filter { it.type == "image" }
                        val imageFiles = if (baseImages.none { it.id == currentViewing.id }) listOf(currentViewing) + baseImages else baseImages
                        val initialIndex = imageFiles.indexOfFirst { it.id == currentViewing.id }.coerceAtLeast(0)
                        ImageViewerScreen(
                            files = imageFiles,
                            initialIndex = initialIndex,
                            cornerRoundness = state.cornerRoundness,
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
                                         state.viewingFile!!.name.endsWith(".json", true) ||
                                         state.viewingFile!!.name.endsWith(".md", true) ||
                                         state.viewingFile!!.type == "doc"
                        if (isDocument) {
                            DocumentViewerScreen(
                                fileItem = state.viewingFile!!,
                                onClose = { onAction(AppAction.CloseFileViewer) },
                                onAction = onAction,
                                cornerRoundness = state.cornerRoundness
                            )
                        } else {
                            FileViewerScreen(
                                fileItem = state.viewingFile!!,
                                onClose = { onAction(AppAction.CloseFileViewer) },
                                cornerRoundness = state.cornerRoundness
                            )
                        }
                    }
                }
            }

        }
    }

    } // end ModalNavigationDrawer
}

    } // end CompositionLocalProvider

@Composable
fun DrawerContent(
    state: AppState,
    onAction: (AppAction) -> Unit,
    cornerRoundness: Float,
    onCloseDrawer: () -> Unit,
    onShowAbout: () -> Unit = {}
) {
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var showGDrivePopup by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showDropboxPopup by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showSmbConnections by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showFtpConnections by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showSftpConnections by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showWebDavConnections by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showNextcloudConnections by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    
    val launchDrivePicker = rememberDrivePickerLauncher { success, pickedIds ->
        showGDrivePopup = false
        if (success) {
            pickedIds?.let { onAction(AppAction.SetDrivePickedIds(it)) }
            onCloseDrawer()
            onAction(AppAction.SetLocation("drive"))
        }
    }

    val googleSignInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            if (account?.email != null) {
                onAction(AppAction.SetGoogleDriveAuthStatus(true, account.email!!))
                launchDrivePicker()
            }
        } catch (e: Exception) {
            onAction(AppAction.SetErrorMessage("Google Login Failed: ${e.message}"))
            showGDrivePopup = false
        }
    }
    ExpressiveDrawerContent(
        state = state,
        onAction = onAction,
        onCloseDrawer = onCloseDrawer,
        onShowAbout = onShowAbout,
        onConnectGoogleDrive = {
            val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(com.google.android.gms.common.api.Scope(com.google.api.services.drive.DriveScopes.DRIVE_FILE))
                .build()
            val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
            googleSignInLauncher.launch(client.signInIntent)
        },
        onConnectMega = { onAction(AppAction.SetShowMegaPopup(true)) },
        onConnectDropbox = { showDropboxPopup = true },
        onConnectNextcloud = { showNextcloudConnections = true },
        onConnectWebDav = { showWebDavConnections = true },
        onConnectSftp = { showSftpConnections = true },
        onConnectFtp = { showFtpConnections = true },
        onConnectSmb = { showSmbConnections = true }
    )
    if (false) {
    ModalDrawerSheet(
        drawerContainerColor = androidx.compose.ui.graphics.Color.Transparent,
        drawerContentColor = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.width(300.dp).appGradientBackground()
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
                    IconButton(onClick = { haptics.tap(); onCloseDrawer() }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
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
                onClick = { haptics.tap(); isCloudExpanded = !isCloudExpanded },
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
                                    contentDescription = stringResource(R.string.connections_content_desc),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "Connections",
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
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp).horizontalScroll(rememberScrollState())
                        ) {
                            // Google Drive
                            Surface(
                                shape = getDynamicCornerShape(12f, cornerRoundness),
                                color = if (state.isGoogleDriveAuthenticated) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = if (state.isGoogleDriveAuthenticated) null else BorderStroke(1.dp, com.ripple.filemanager.ui.theme.SkylineColors.Border),
                                onClick = { haptics.tap(); showGDrivePopup = true },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize().padding(4.dp)
                                ) {
                                    Icon(
                                        if (state.isGoogleDriveAuthenticated) Icons.Default.CloudDone else Icons.Default.Cloud,
                                        contentDescription = stringResource(R.string.google_drive),
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
                                onClick = { haptics.tap(); onAction(AppAction.SetShowMegaPopup(true)) },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize().padding(4.dp)
                                ) {
                                    Icon(
                                        if (state.isMegaAuthenticated) Icons.Default.CloudDone else Icons.Default.Cloud,
                                        contentDescription = stringResource(R.string.mega),
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
                                onClick = { haptics.tap(); showDropboxPopup = true },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize().padding(4.dp)
                                ) {
                                    Icon(
                                        if (state.isDropboxAuthenticated) Icons.Default.CloudDone else Icons.Default.Cloud,
                                        contentDescription = stringResource(R.string.dropbox),
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

                            // LAN / SMB
                            val smbIsActive = state.smbState.activeConnectionId != null
                            Surface(
                                shape = getDynamicCornerShape(12f, cornerRoundness),
                                color = if (smbIsActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = if (smbIsActive) null else BorderStroke(1.dp, com.ripple.filemanager.ui.theme.SkylineColors.Border),
                                onClick = { haptics.tap(); showSmbConnections = true },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize().padding(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Dns,
                                        contentDescription = stringResource(R.string.lan_smb),
                                        tint = if (smbIsActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        "LAN/SMB",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (smbIsActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // FTP
                            val ftpIsActive = state.ftpState.activeConnectionId != null
                            Surface(
                                shape = getDynamicCornerShape(12f, cornerRoundness),
                                color = if (ftpIsActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = if (ftpIsActive) null else BorderStroke(1.dp, com.ripple.filemanager.ui.theme.SkylineColors.Border),
                                onClick = { haptics.tap(); showFtpConnections = true },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize().padding(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Storage,
                                        contentDescription = stringResource(R.string.ftp),
                                        tint = if (ftpIsActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        "FTP",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (ftpIsActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // SFTP
                            val sftpIsActive = state.sftpState.activeConnectionId != null
                            Surface(
                                shape = getDynamicCornerShape(12f, cornerRoundness),
                                color = if (sftpIsActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = if (sftpIsActive) null else BorderStroke(1.dp, com.ripple.filemanager.ui.theme.SkylineColors.Border),
                                onClick = { haptics.tap(); showSftpConnections = true },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize().padding(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Security,
                                        contentDescription = stringResource(R.string.sftp),
                                        tint = if (sftpIsActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        "SFTP",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (sftpIsActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Nextcloud
                            val nextcloudIsActive = state.webDavState.savedConnections.any { it.isNextcloud && it.id == state.webDavState.activeConnectionId }
                            Surface(
                                shape = getDynamicCornerShape(12f, cornerRoundness),
                                color = if (nextcloudIsActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = if (nextcloudIsActive) null else BorderStroke(1.dp, com.ripple.filemanager.ui.theme.SkylineColors.Border),
                                onClick = { haptics.tap(); showNextcloudConnections = true },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize().padding(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CloudSync,
                                        contentDescription = "Nextcloud",
                                        tint = if (nextcloudIsActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        "Nextcloud",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (nextcloudIsActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // WebDAV
                            val webdavIsActive = state.webDavState.savedConnections.any { !it.isNextcloud && it.id == state.webDavState.activeConnectionId }
                            Surface(
                                shape = getDynamicCornerShape(12f, cornerRoundness),
                                color = if (webdavIsActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = if (webdavIsActive) null else BorderStroke(1.dp, com.ripple.filemanager.ui.theme.SkylineColors.Border),
                                onClick = { haptics.tap(); showWebDavConnections = true },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize().padding(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.FolderShared,
                                        contentDescription = "WebDAV",
                                        tint = if (webdavIsActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        "WebDAV",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (webdavIsActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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
                label = stringResource(R.string.storage_cleaner_nav),
                cornerRoundness = cornerRoundness,
                onClick = {
                    onCloseDrawer()
                    onAction(AppAction.SetCleanerScreenVisible(true))
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            DrawerMenuItem(
                icon = Icons.Outlined.Settings,
                label = stringResource(R.string.settings_nav),
                cornerRoundness = cornerRoundness,
                onClick = {
                    onCloseDrawer()
                    onAction(AppAction.SetShowSettingsScreen(true))
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            DrawerMenuItem(
                icon = Icons.Outlined.Info,
                label = stringResource(R.string.about_nav),
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
                .requestScopes(com.google.android.gms.common.api.Scope(com.google.api.services.drive.DriveScopes.DRIVE_FILE))
                .build()
            val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)

            if (state.isGoogleDriveAuthenticated) {
                ExpressiveConfirmationSheet(
                    title = "Log out of Google Drive?",
                    subtitle = state.googleDriveAccountEmail ?: "Connected account",
                    confirmLabel = stringResource(R.string.logout_action),
                    cancelLabel = stringResource(R.string.cancel),
                    onConfirm = {
                        googleSignInClient.signOut().addOnCompleteListener {
                            onAction(AppAction.SetGoogleDriveAuthStatus(false, null))
                        }
                        showGDrivePopup = false
                        if (state.location.startsWith("drive")) {
                            onAction(AppAction.SetLocation("cloud"))
                        }
                    },
                    onDismissRequest = {
                        showGDrivePopup = false
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    googleSignInClient.signOut().addOnCompleteListener {
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    }
                    showGDrivePopup = false
                }
            }
        }
    } // end if (false)



        if (showDropboxPopup) {
            if (state.isDropboxAuthenticated) {
                ExpressiveConfirmationSheet(
                    title = "Log out of Dropbox?",
                    subtitle = state.dropboxAccountEmail ?: "Connected account",
                    confirmLabel = stringResource(R.string.logout_action),
                    cancelLabel = stringResource(R.string.cancel),
                    onConfirm = {
                        onAction(AppAction.SetDropboxAuthStatus(false, null))
                        showDropboxPopup = false
                        if (state.location.startsWith("dropbox")) {
                            onAction(AppAction.SetLocation("cloud"))
                        }
                    },
                    onDismissRequest = {
                        showDropboxPopup = false
                    }
                )
            } else {
                CloudLoginBottomSheet(
                    providerName = "Dropbox",
                    providerIcon = Icons.Outlined.Cloud,
                    providerCategory = "videos",
                    registerUrl = "https://dropbox.com/register",
                    recoveryUrl = "https://www.dropbox.com/forgot",
                    onLogin = { email, password ->
                        onAction(AppAction.SetDropboxAuthStatus(true, email))
                        showDropboxPopup = false
                    },
                    onDismissRequest = {
                        showDropboxPopup = false
                    }
                )
            }
        }

        if (showSmbConnections) {
            SmbConnectionsDialog(
                state = state.smbState,
                onAction = onAction,
                onDismiss = { showSmbConnections = false }
            )
        }

        if (showFtpConnections) {
            FtpConnectionsDialog(
                state = state.ftpState,
                onAction = onAction,
                onDismiss = { showFtpConnections = false }
            )
        }

        if (showSftpConnections) {
            SftpConnectionsDialog(
                state = state.sftpState,
                onAction = onAction,
                onDismiss = { showSftpConnections = false }
            )
        }

        if (showWebDavConnections) {
            WebDavConnectionsDialog(
                state = state.webDavState,
                onAction = onAction,
                onDismiss = { showWebDavConnections = false }
            )
        }

        if (showNextcloudConnections) {
            NextcloudConnectionsDialog(
                state = state.webDavState,
                onAction = onAction,
                onDismiss = { showNextcloudConnections = false }
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
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    Surface(
        shape = getDynamicCornerShape(14f, cornerRoundness),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, com.ripple.filemanager.ui.theme.SkylineColors.Border),
        onClick = { haptics.tap(); onClick() },
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
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
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
                IconButton(onClick = { haptics.tap(); onClose() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
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
                    "Version ${com.ripple.filemanager.BuildConfig.VERSION_NAME}",
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
                                    contentDescription = stringResource(R.string.email_label),
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

fun handleFileOpen(
    file: FileItem,
    context: android.content.Context,
    state: AppState,
    focusManager: androidx.compose.ui.focus.FocusManager,
    onAction: (AppAction) -> Unit
) {
    if (file.path.startsWith("/")) {
        onAction(AppAction.LogRecentAction(file.path, "Opened"))
    }
    if (file.name.endsWith(".pdf", ignoreCase = true) || file.type == "image" || file.type == "doc" || listOf(".txt", ".json", ".md", ".csv", ".xml", ".log", ".kt", ".java", ".py", ".html").any { file.name.endsWith(it, ignoreCase = true) }) {
        if (file.type == "image" && state.viewerImage == "Device default") {
            if (state.query.isNotEmpty()) { focusManager.clearFocus(); onAction(AppAction.SetQuery("")) }
            openFileInExternalApp(context, file, "image/*")
        } else if (file.type != "image" && state.viewerTextPdf == "Device default") {
            val defaultMime = if (file.name.endsWith(".pdf", ignoreCase = true)) "application/pdf" else "text/plain"
            if (state.query.isNotEmpty()) { focusManager.clearFocus(); onAction(AppAction.SetQuery("")) }
            openFileInExternalApp(context, file, defaultMime)
        } else {
            focusManager.clearFocus()
            onAction(AppAction.ViewFile(file))
            if (state.query.isNotEmpty()) { onAction(AppAction.SetQuery("")) }
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
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", java.io.File(file.path))
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
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", java.io.File(file.path))
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            onAction(AppAction.ShowToast(context.getString(R.string.error_opening_apk, e.message ?: "")))
        }
    } else {
        openFileInExternalApp(context, file, "*/*")
    }
}

@androidx.compose.runtime.Immutable
data class FabMenuAction(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val onClick: () -> Unit)

/**
 * Same decision chain as the single-pane onFileClick block below
 * (lock check -> archive check -> handleFileOpen), pulled out so the
 * dual-pane screen's onOpenFile callback can reuse it without duplicating
 * the whole if/else chain. Selection-mode and folder handling aren't
 * included here since dual-pane doesn't support multi-select yet and
 * folders are already routed to onNavigate before this is called.
 */
fun handleFileTapForPane(
    file: FileItem,
    state: AppState,
    context: android.content.Context,
    focusManager: androidx.compose.ui.focus.FocusManager,
    onAction: (AppAction) -> Unit,
    onShowArchiveOptions: (FileItem) -> Unit
) {
    if (file.isLocked) {
        onAction(AppAction.RequestAuth(com.ripple.filemanager.AuthReason.OPEN_FILE, file.path, fileId = file.id, folderName = file.name))
    } else if (file.name.lowercase().endsWith(".zip") || file.name.lowercase().endsWith(".rar") || file.name.lowercase().endsWith(".tar") || file.name.lowercase().endsWith(".gz") || file.name.lowercase().endsWith(".bz2") || file.name.lowercase().endsWith(".xz")) {
        onShowArchiveOptions(file)
    } else {
        handleFileOpen(file, context, state, focusManager, onAction)
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun MainContent(
    state: AppState,
    onAction: (AppAction) -> Unit,
    snackbarHostState: SnackbarHostState,
    onDrawerOpen: () -> Unit = {},
    returnToCleanerOnCategoryBack: Boolean = false,
    onResetReturnToCleaner: () -> Unit = {},
    showAboutScreen: Boolean = false,
    onCloseAbout: () -> Unit = {},
    onSetReturnToCleaner: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    val context = LocalContext.current
    val isDark = when (state.themeMode) {
        com.ripple.filemanager.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
        com.ripple.filemanager.ThemeMode.DARK -> true
        com.ripple.filemanager.ThemeMode.LIGHT -> false
    }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var extractTargetFile by remember { mutableStateOf<FileItem?>(null) }
    var viewingArchive by remember { mutableStateOf<FileItem?>(null) }
    var showArchiveOptionsFor by remember { mutableStateOf<FileItem?>(null) }
    var fileForSafExtraction by remember { mutableStateOf<FileItem?>(null) }
    
    val archiveViewModel: com.ripple.filemanager.archive.ArchiveViewModel = viewModel()
    val archiveProgress by archiveViewModel.archiveProgress.collectAsState()
    
    val safExtractLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null && fileForSafExtraction != null) {
            val file = fileForSafExtraction!!
            val sourceUri = if (file.path.startsWith("content://")) android.net.Uri.parse(file.path) else android.net.Uri.fromFile(File(file.path))
            archiveViewModel.extract(sourceUri, uri)
            showArchiveOptionsFor = null
            fileForSafExtraction = null
        }
    }
    
    var isExtracting by remember { mutableStateOf(false) }
    // Search is always visible in the new header design
    var infoDialogFile by remember { mutableStateOf<FileItem?>(null) }
    var fileToDelete by remember { mutableStateOf<FileItem?>(null) }
    var showSmbConnections by remember { mutableStateOf(false) }
    var showFtpConnections by remember { mutableStateOf(false) }
    var showSftpConnections by remember { mutableStateOf(false) }
    var showWebDavConnections by remember { mutableStateOf(false) }
    var showNextcloudConnections by remember { mutableStateOf(false) }
    var showDropboxPopup by remember { mutableStateOf(false) }
    var fileDetails by remember { mutableStateOf<FileDetails?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var backProgress by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }

    var capturedTargetFolderName by remember { mutableStateOf<String?>(null) }
    var pillNavExpanded by remember { mutableStateOf(false) }
    var showNewCloudConnectionSheet by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showCreateFileDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.unlockedFileToOpen) {
        state.unlockedFileToOpen?.let { file ->
            handleFileOpen(file, context, state, focusManager, onAction)
            onAction(AppAction.ClearUnlockedFileToOpen)
        }
    }
    
    LaunchedEffect(state.isPasteComplete) {
        if (state.isPasteComplete) {
            val fileCount = state.clipboardPaths.size
            snackbarHostState.showSnackbar("Pasted $fileCount item(s) into ${capturedTargetFolderName ?: "folder"}")
            capturedTargetFolderName = null
        }
    }

    val launchDrivePicker = rememberDrivePickerLauncher { success, pickedIds ->
        if (success) {
            pickedIds?.let { onAction(AppAction.SetDrivePickedIds(it)) }
            onAction(AppAction.SetLocation("drive"))
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            if (account?.email != null) {
                onAction(AppAction.SetGoogleDriveAuthStatus(true, account.email!!))
                launchDrivePicker()
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
    
    val isImeVisible = androidx.compose.foundation.layout.WindowInsets.Companion.isImeVisible
    BackHandler(enabled = state.query.isNotEmpty() && !isImeVisible) {
        focusManager.clearFocus()
        onAction(AppAction.SetQuery(""))
    }

    BackHandler(enabled = state.selectedFiles.isNotEmpty()) {
        onAction(AppAction.ClearSelection)
    }

    val canDualPaneGoBack = if (state.dualPaneMode != com.ripple.filemanager.DualPaneMode.OFF) {
        if (state.activePaneSide == com.ripple.filemanager.PaneSide.LEFT) {
            state.driveFolderStack.isNotEmpty() || state.location != "home"
        } else {
            state.secondPaneState.folderStack.isNotEmpty() || state.secondPaneState.location != "home"
        }
    } else false

    val targetLocation = when {
        showAboutScreen -> "about"
        state.showSettingsScreen -> "settings"
        state.showTrashScreen -> "trash"
        state.showCleanerScreen -> "cleaner"
        else -> state.location
    }
    val isModalScreen = targetLocation in setOf("cleaner", "trash", "settings", "about")

    androidx.activity.compose.PredictiveBackHandler(
        enabled = !isModalScreen && (if (state.dualPaneMode != com.ripple.filemanager.DualPaneMode.OFF) canDualPaneGoBack else (state.selectedFiles.isEmpty() && state.query.isEmpty() && state.location != "home"))
    ) { progress ->
        try {
            progress.collect { backEvent ->
                backProgress = backEvent.progress
            }
            if (state.dualPaneMode != com.ripple.filemanager.DualPaneMode.OFF) {
                onAction(AppAction.NavigateBackInPane(state.activePaneSide))
            } else if (state.location.startsWith("category/")) {
                if (returnToCleanerOnCategoryBack) {
                    onResetReturnToCleaner()
                    onAction(AppAction.SetLocation("home"))
                    onAction(AppAction.SetCleanerScreenVisible(true))
                } else {
                    onAction(AppAction.SetLocation("home"))
                }
            } else if (state.location.startsWith("/")) {
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

    

    val exprBg = com.ripple.filemanager.ui.expressive.ExpressiveTheme.colors.bg
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        snackbarHost = {
            val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val isThreeButton = navBottom > 24.dp
            val finalBottomPadding = if (isThreeButton) navBottom + 25.dp else 23.dp
            val toastBottomPadding = if (state.isSelectionMode) {
                finalBottomPadding + 52.dp + 14.dp
            } else {
                finalBottomPadding + 56.dp + 14.dp
            }
            
            ExpressiveToastHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .imePadding()
                    .padding(bottom = toastBottomPadding)
            )
        },
        topBar = {}
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = { focusManager.clearFocus() })
                    }
                    .graphicsLayer {
                        val scale = 1f - (backProgress * 0.1f)
                        scaleX = scale
                        scaleY = scale
                        translationX = backProgress * 150f
                        alpha = 1f - (backProgress * 0.3f)
                    }
            ) {



            if (state.errorMessage != null) {
                val colors = ExpressiveTheme.colors
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    ExpressiveScreenHeader(
                        title = "Error",
                        subtitle = "Something went wrong",
                        leftIcon = Icons.Default.Menu,
                        onLeftClick = onDrawerOpen,
                        showSearch = false
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 110.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CookieIcon(
                            icon = Icons.Outlined.ErrorOutline,
                            size = 72.dp,
                            lobes = 8,
                            bgColor = colors.card,
                            iconTint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.something_went_wrong),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.text
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.errorMessage ?: "",
                            fontSize = 14.sp,
                            color = colors.muted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        if (state.recoverableAuthIntent != null) {
                            Button(
                                onClick = { authRecoverLauncher.launch(state.recoverableAuthIntent) },
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.ink),
                                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)
                            ) {
                                Text(stringResource(R.string.grant_permission), fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Button(
                                onClick = { onAction(AppAction.Reload) },
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.ink),
                                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)
                            ) {
                                Text(stringResource(R.string.retry), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {

                    val onFileClickCommon: (com.ripple.filemanager.FileItem) -> Unit = { file ->
                        if (file.path.startsWith("/")) {
                            onAction(com.ripple.filemanager.AppAction.LogRecentAction(file.path, if (file.type == "folder") "Visited" else "Opened"))
                        }
                        if (file.isLocked) {
                            onAction(com.ripple.filemanager.AppAction.RequestAuth(com.ripple.filemanager.AuthReason.OPEN_FILE, file.path, fileId = file.id, folderName = file.name))
                        } else if (state.isSelectionMode) {
                            onAction(com.ripple.filemanager.AppAction.ToggleSelection(file.id))
                        } else if (file.type == "folder") {
                            if (state.query.isNotEmpty()) { focusManager.clearFocus(); onAction(com.ripple.filemanager.AppAction.SetQuery("")) }
                            onAction(com.ripple.filemanager.AppAction.SetLocation(file.path, file.name))
                        } else if (file.name.lowercase().endsWith(".zip") || file.name.lowercase().endsWith(".rar") || file.name.lowercase().endsWith(".tar") || file.name.lowercase().endsWith(".gz") || file.name.lowercase().endsWith(".bz2") || file.name.lowercase().endsWith(".xz")) {
                            showArchiveOptionsFor = file
                        } else {
                            handleFileOpen(file, context, state, focusManager, onAction)
                        }
                    }

                    val isReducedMotion = com.ripple.filemanager.ui.expressive.ExpressiveMotion.isReducedMotion()
                    fun getNavTabGroup(loc: String): String = when {
                        loc == "home" -> "home"
                        loc == "send" -> "send"
                        loc == "cloud" || loc.startsWith("drive") || loc.startsWith("mega") ||
                        loc.startsWith("dropbox") || loc.startsWith("smb_") || loc.startsWith("ftp_") ||
                        loc.startsWith("sftp_") || loc.startsWith("webdav_") || loc.startsWith("nextcloud_") -> "cloud"
                        loc.startsWith("category/") -> "category"
                        loc.startsWith("/") -> "browse"
                        loc == "cleaner" || loc == "trash" || loc == "settings" || loc == "about" -> "modal"
                        else -> ""
                    }
                    val tabOrder = mapOf("home" to 0, "browse" to 1, "cloud" to 2, "send" to 3)
                    val modalOrder = mapOf("cleaner" to 0, "trash" to 1, "settings" to 2, "about" to 3)

                    AnimatedContent(
                        targetState = targetLocation,
                        transitionSpec = {
                            if (isReducedMotion) {
                                (fadeIn(animationSpec = androidx.compose.animation.core.snap()))
                                    .togetherWith(
                                        fadeOut(animationSpec = androidx.compose.animation.core.snap())
                                    )
                            } else {
                                val initialGroup = getNavTabGroup(initialState)
                                val targetGroup = getNavTabGroup(targetState)
                                val initialTab = tabOrder[initialGroup]
                                val targetTab = tabOrder[targetGroup]

                                val isTabSwitch = initialTab != null && targetTab != null && initialTab != targetTab
                                com.ripple.filemanager.ui.TabSwitchLatencyTracker.onTransitionStart(initialState, targetState)

                                val isForward = when {
                                    // 1. Between distinct main tabs: follow horizontal tab order (Home < Browse < Cloud < Send)
                                    isTabSwitch -> {
                                        targetTab!! > initialTab!!
                                    }
                                    // 2. From modal to modal (e.g. Cleaner -> Trash)
                                    initialGroup == "modal" && targetGroup == "modal" -> {
                                        val initM = modalOrder[initialState] ?: 0
                                        val targetM = modalOrder[targetState] ?: 0
                                        targetM >= initM
                                    }
                                    // 3. Modal screens: modal is forward relative to main tabs, backward when leaving modal to main tab
                                    targetGroup == "modal" && initialGroup != "modal" && initialGroup != "category" -> true
                                    initialGroup == "modal" && targetGroup != "modal" && targetGroup != "category" -> false
                                    // 4. Modal (Cleaner) <-> Category
                                    initialGroup == "modal" && targetGroup == "category" -> true
                                    initialGroup == "category" && targetGroup == "modal" -> false
                                    // 5. From category to another main tab (browse, cloud, send): forward
                                    initialGroup == "category" && targetTab != null && targetTab > 0 -> true
                                    // 6. Home/Main <-> Category
                                    initialGroup != "category" && targetGroup == "category" -> true
                                    initialGroup == "category" && targetGroup != "category" -> false
                                    // 7. Within file paths (folder open vs back navigation)
                                    initialState.startsWith("/") && targetState.startsWith("/") -> {
                                        if (targetState.startsWith(initialState) && targetState != initialState) true
                                        else if (initialState.startsWith(targetState) && initialState != targetState) false
                                        else targetState.length > initialState.length
                                    }
                                    // 8. Default: forward if target path is longer than initial
                                    targetState.length > initialState.length -> true
                                    else -> false
                                }

                                if (isTabSwitch) {
                                    // Snappy 160ms tab switch with subtle parallax
                                    if (isForward) {
                                        (slideInHorizontally(
                                            animationSpec = androidx.compose.animation.core.tween(160, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                        ) { fullWidth -> fullWidth / 6 } +
                                         fadeIn(
                                            animationSpec = androidx.compose.animation.core.tween(140, easing = androidx.compose.animation.core.LinearEasing)
                                        )).togetherWith(
                                            slideOutHorizontally(
                                                animationSpec = androidx.compose.animation.core.tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                            ) { fullWidth -> -fullWidth / 6 } +
                                            fadeOut(
                                                animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearEasing)
                                            )
                                        ).apply {
                                            targetContentZIndex = 1f
                                        }
                                    } else {
                                        (slideInHorizontally(
                                            animationSpec = androidx.compose.animation.core.tween(160, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                        ) { fullWidth -> -fullWidth / 6 } +
                                         fadeIn(
                                            animationSpec = androidx.compose.animation.core.tween(140, easing = androidx.compose.animation.core.LinearEasing)
                                        )).togetherWith(
                                            slideOutHorizontally(
                                                animationSpec = androidx.compose.animation.core.tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                            ) { fullWidth -> fullWidth / 6 } +
                                            fadeOut(
                                                animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearEasing)
                                            )
                                        ).apply {
                                            targetContentZIndex = 0f
                                        }
                                    }
                                } else if (isForward) {
                                    (slideInHorizontally(
                                        animationSpec = androidx.compose.animation.core.tween(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                    ) { fullWidth -> fullWidth } +
                                     fadeIn(
                                        animationSpec = androidx.compose.animation.core.tween(200, easing = androidx.compose.animation.core.LinearEasing)
                                    )).togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = androidx.compose.animation.core.tween(220, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                        ) { fullWidth -> -fullWidth } +
                                        fadeOut(
                                            animationSpec = androidx.compose.animation.core.tween(180, easing = androidx.compose.animation.core.LinearEasing)
                                        )
                                    ).apply {
                                        targetContentZIndex = 1f
                                    }
                                } else {
                                    (slideInHorizontally(
                                        animationSpec = androidx.compose.animation.core.tween(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                    ) { fullWidth -> -fullWidth } +
                                     fadeIn(
                                        animationSpec = androidx.compose.animation.core.tween(200, easing = androidx.compose.animation.core.LinearEasing)
                                    )).togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = androidx.compose.animation.core.tween(220, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                        ) { fullWidth -> fullWidth } +
                                        fadeOut(
                                            animationSpec = androidx.compose.animation.core.tween(180, easing = androidx.compose.animation.core.LinearEasing)
                                        )
                                    ).apply {
                                        targetContentZIndex = 0f
                                    }
                                }
                            }
                        },
                        label = "screen_content",
                        modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds()
                    ) { currentTargetLocation ->
                        DisposableEffect(currentTargetLocation) {
                            ScreenCompositionTracker.onScreenEnter(currentTargetLocation)
                            TabSwitchLatencyTracker.onTransitionSettled(currentTargetLocation)
                            onDispose {
                                ScreenCompositionTracker.onScreenExit(currentTargetLocation)
                            }
                        }

                        if (currentTargetLocation == "cleaner") {
                            BackHandler(enabled = true) {
                                if (state.currentCleanerCategory != null) {
                                    onAction(AppAction.SetCleanerCategory(null))
                                } else {
                                    onAction(AppAction.SetCleanerScreenVisible(false))
                                }
                            }
                            CleanerScreen(
                                state = state,
                                onAction = onAction,
                                snackbarHostState = snackbarHostState,
                                onNavigateToCategory = { filterKey ->
                                    onSetReturnToCleaner(true)
                                    onAction(AppAction.SetCleanerScreenVisible(false))
                                    onAction(AppAction.SetLocation("category/$filterKey"))
                                }
                            )
                        } else if (currentTargetLocation == "trash") {
                            BackHandler(enabled = true) {
                                onAction(AppAction.SetTrashScreenVisible(false))
                            }
                            TrashScreen(
                                state = state,
                                onAction = onAction,
                                onClose = { onAction(AppAction.SetTrashScreenVisible(false)) }
                            )
                        } else if (currentTargetLocation == "settings") {
                            BackHandler(enabled = true) {
                                onAction(AppAction.SetShowSettingsScreen(false))
                            }
                            SettingsScreen(state = state, onAction = onAction)
                        } else if (currentTargetLocation == "about") {
                            BackHandler(enabled = true) {
                                onCloseAbout()
                            }
                            ExpressiveAboutScreen(
                                onClose = onCloseAbout
                            )
                        } else if (currentTargetLocation == "home") {
                            ExpressiveHomeScreen(
                                state = state,
                                onAction = onAction,
                                onMenuClick = onDrawerOpen,
                                onTrashClick = { onAction(com.ripple.filemanager.AppAction.SetTrashScreenVisible(true)) },
                                onOrganiseClick = { onAction(com.ripple.filemanager.AppAction.OrganiseDownloads) },
                                onFileClick = onFileClickCommon,
                                onFileMenuClick = { file -> infoDialogFile = file },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (currentTargetLocation == "send") {
                            ExpressiveSendScreen(
                                state = state,
                                onAction = onAction,
                                onMenuClick = onDrawerOpen,
                                onTrashClick = { onAction(com.ripple.filemanager.AppAction.SetTrashScreenVisible(true)) },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (currentTargetLocation == "cloud" ||
                            currentTargetLocation.startsWith("drive") ||
                            currentTargetLocation.startsWith("mega") ||
                            currentTargetLocation.startsWith("dropbox") ||
                            currentTargetLocation.startsWith("smb_") ||
                            currentTargetLocation.startsWith("ftp_") ||
                            currentTargetLocation.startsWith("sftp_") ||
                            currentTargetLocation.startsWith("webdav_") ||
                            currentTargetLocation.startsWith("nextcloud_")) {
                            ExpressiveCloudScreen(
                                state = state,
                                onAction = onAction,
                                onConnectGoogleDrive = {
                                    if (state.isGoogleDriveAuthenticated) {
                                        launchDrivePicker()
                                    } else {
                                        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                                            .requestEmail()
                                            .requestScopes(com.google.android.gms.common.api.Scope(com.google.api.services.drive.DriveScopes.DRIVE_FILE))
                                            .build()
                                        val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                                        client.signOut().addOnCompleteListener {
                                            googleSignInLauncher.launch(client.signInIntent)
                                        }
                                    }
                                },
                                onConnectMega = { onAction(com.ripple.filemanager.AppAction.SetShowMegaPopup(true)) },
                                onConnectDropbox = { showDropboxPopup = true },
                                onConnectNextcloud = { showNextcloudConnections = true },
                                onConnectWebDav = { showWebDavConnections = true },
                                onConnectSftp = { showSftpConnections = true },
                                onConnectFtp = { showFtpConnections = true },
                                onConnectSmb = { showSmbConnections = true },
                                onLogoutGoogleDrive = {
                                    val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestEmail()
                                        .requestScopes(com.google.android.gms.common.api.Scope(com.google.api.services.drive.DriveScopes.DRIVE_FILE))
                                        .build()
                                    val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                                    client.signOut().addOnCompleteListener {
                                        onAction(AppAction.SetGoogleDriveAuthStatus(false, null))
                                    }
                                },
                                onFileClick = onFileClickCommon,
                                onFileMenuClick = { file -> infoDialogFile = file },
                                onMenuClick = onDrawerOpen,
                                onTrashClick = { onAction(com.ripple.filemanager.AppAction.SetTrashScreenVisible(true)) },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (currentTargetLocation.startsWith("category/")) {
                            val categoryKey = currentTargetLocation.removePrefix("category/")
                            ExpressiveCategoryDetailScreen(
                                categoryKey = categoryKey,
                                state = state,
                                onAction = onAction,
                                onBack = {
                                    if (returnToCleanerOnCategoryBack) {
                                        onResetReturnToCleaner()
                                        onAction(AppAction.SetLocation("home"))
                                        onAction(AppAction.SetCleanerScreenVisible(true))
                                    } else {
                                        onAction(com.ripple.filemanager.AppAction.SetLocation("home"))
                                    }
                                },
                                onTrashClick = { onAction(com.ripple.filemanager.AppAction.SetTrashScreenVisible(true)) },
                                onFileClick = onFileClickCommon,
                                onFileMenuClick = { file -> infoDialogFile = file },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (state.dualPaneMode != com.ripple.filemanager.DualPaneMode.OFF) {
                            com.ripple.filemanager.ui.dualpane.DualPaneFileManagerScreen(
                                state = state,
                                mode = state.dualPaneMode,
                                onNavigate = { side, path, name ->
                                    if (side == com.ripple.filemanager.PaneSide.LEFT) {
                                        onAction(com.ripple.filemanager.AppAction.SetLocation(path, name))
                                    } else {
                                        onAction(com.ripple.filemanager.AppAction.SetLocationForPane(path, name))
                                    }
                                },
                                onBack = { side -> onAction(com.ripple.filemanager.AppAction.NavigateBackInPane(side)) },
                                onSetActivePane = { side -> onAction(com.ripple.filemanager.AppAction.SetActivePane(side)) },
                                onOpenFile = { _, file ->
                                    handleFileTapForPane(
                                        file = file,
                                        state = state,
                                        context = context,
                                        focusManager = focusManager,
                                        onAction = onAction,
                                        onShowArchiveOptions = { showArchiveOptionsFor = it }
                                    )
                                },
                                onDropFile = { sourceSide, file, destSide, mode, conflictResolution ->
                                    onAction(com.ripple.filemanager.AppAction.TransferFileBetweenPanes(sourceSide, file, destSide, mode, conflictResolution))
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            ExpressiveBrowseScreen(
                                state = state,
                                onAction = onAction,
                                onFileClick = onFileClickCommon,
                                onFileMenuClick = { file -> infoDialogFile = file },
                                onMenuClick = onDrawerOpen,
                                onTrashClick = { onAction(com.ripple.filemanager.AppAction.SetTrashScreenVisible(true)) },
                                onOrganiseClick = { onAction(com.ripple.filemanager.AppAction.OrganiseDownloads) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        } 
        val isSheetMode = state.isSelectionMode || state.clipboardPaths.isNotEmpty()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isSheetMode) Modifier
                    else Modifier.navigationBarsPadding().padding(bottom = 14.dp)
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = state.currentAudioFile != null && !state.showFullScreenPlayer && !state.isSelectionMode,
                    enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut(),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    MiniMusicPlayer(
                        state = state,
                        onAction = onAction
                    )
                }

                val selectedFiles = state.files.filter { it.id in state.selectedFiles }
                val selectedApks = selectedFiles.filter { it.name.endsWith(".apk", ignoreCase = true) || it.name.endsWith(".apks", ignoreCase = true) }
                val showInstallFab = state.isSelectionMode && selectedApks.isNotEmpty() && selectedApks.size == selectedFiles.size

                var showSingleApkInstallPopup by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                var showBatchApkInstallPopup by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                var showDowngradeWarningSingle by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                var showDowngradeWarningBatch by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                var batchSheetMode by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.ripple.filemanager.ui.InstallMode?>(null) }
                var parsedBatchApks by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<List<com.ripple.filemanager.ui.ParsedApkItem>?>(null) }

                androidx.compose.runtime.LaunchedEffect(selectedApks, showBatchApkInstallPopup) {
                    if (selectedApks.size > 1 && showBatchApkInstallPopup) {
                        parsedBatchApks = null
                        val results = mutableListOf<com.ripple.filemanager.ui.ParsedApkItem>()
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            val pm = context.packageManager
                            for (apk in selectedApks) {
                                if (apk.path.startsWith("/")) {
                                    val pi = pm.getPackageArchiveInfo(apk.path, android.content.pm.PackageManager.GET_ACTIVITIES)
                                    if (pi != null) {
                                        pi.applicationInfo?.sourceDir = apk.path
                                        pi.applicationInfo?.publicSourceDir = apk.path
                                        val label = pi.applicationInfo?.loadLabel(pm)?.toString() ?: apk.name
                                        val vName = pi.versionName ?: "Unknown"
                                        
                                        var isDowngrade = false
                                        var installedVName: String? = null
                                        var installedVCode: Long? = null
                                        try {
                                            val installed = pm.getPackageInfo(pi.packageName, 0)
                                            installedVName = installed.versionName
                                            installedVCode = installed.longVersionCode
                                            if (pi.longVersionCode < installed.longVersionCode) {
                                                isDowngrade = true
                                            }
                                        } catch (e: Exception) {}
                                        
                                        results.add(com.ripple.filemanager.ui.ParsedApkItem(apk, label, vName, pi.longVersionCode, isDowngrade, installedVName, installedVCode))
                                    } else {
                                        results.add(com.ripple.filemanager.ui.ParsedApkItem(apk, apk.name, "Unknown", 0L, false, null, null))
                                    }
                                } else {
                                    results.add(com.ripple.filemanager.ui.ParsedApkItem(apk, apk.name, "Unknown", 0L, false, null, null))
                                }
                            }
                        }
                        parsedBatchApks = results
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showSingleApkInstallPopup && showInstallFab && selectedApks.size == 1,
                    enter = androidx.compose.animation.scaleIn(
                        initialScale = 0.9f, 
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f), 
                        animationSpec = androidx.compose.animation.core.tween(120)
                    ) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(120)),
                    exit = androidx.compose.animation.scaleOut(
                        targetScale = 0.9f, 
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f), 
                        animationSpec = androidx.compose.animation.core.tween(120)
                    ) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120))
                ) {
                    val singleApk = selectedApks.firstOrNull() ?: return@AnimatedVisibility
                    var apkInfo by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<android.content.pm.PackageInfo?>(null) }
                    var installedInfo by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<android.content.pm.PackageInfo?>(null) }
                    var hasLoaded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                    
                    androidx.compose.runtime.LaunchedEffect(singleApk) {
                        hasLoaded = false
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            if (singleApk.path.startsWith("/")) {
                                val pm = context.packageManager
                                val pi = pm.getPackageArchiveInfo(singleApk.path, android.content.pm.PackageManager.GET_ACTIVITIES)
                                if (pi != null) {
                                    pi.applicationInfo?.sourceDir = singleApk.path
                                    pi.applicationInfo?.publicSourceDir = singleApk.path
                                    apkInfo = pi
                                    try {
                                        installedInfo = pm.getPackageInfo(pi.packageName, 0)
                                    } catch (e: Exception) {
                                        installedInfo = null
                                    }
                                }
                            }
                        }
                        hasLoaded = true
                    }
                    
                    if (!hasLoaded) {
                        Box(modifier = Modifier.padding(bottom = 8.dp).size(56.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = com.ripple.filemanager.ui.theme.SkylineColors.Amber)
                        }
                    } else {
                        val pm = context.packageManager
                        val appLabel = apkInfo?.applicationInfo?.loadLabel(pm)?.toString() ?: singleApk.name
                        val modes = com.ripple.filemanager.ui.evaluateInstallModes(
                            selectedApk = if (apkInfo != null) com.ripple.filemanager.ui.ApkVersionInfo(apkInfo!!.packageName, apkInfo!!.longVersionCode, apkInfo!!.versionName ?: "Unknown") else com.ripple.filemanager.ui.ApkVersionInfo("", 0, "Unknown"),
                            installedApp = if (installedInfo != null) com.ripple.filemanager.ui.ApkVersionInfo(installedInfo!!.packageName, installedInfo!!.longVersionCode, installedInfo!!.versionName ?: "Unknown") else null,
                            shizukuConnected = state.hasShizuku
                        )
                        
                        val vText = if (apkInfo != null) {
                            if (installedInfo != null) "${installedInfo!!.versionName ?: "Unknown"} (${installedInfo!!.longVersionCode}) -> ${apkInfo!!.versionName ?: "Unknown"} (${apkInfo!!.longVersionCode})" else "${apkInfo!!.versionName ?: "Unknown"} (${apkInfo!!.longVersionCode})"
                        } else {
                            "Unknown Version"
                        }
                        
                        com.ripple.filemanager.ui.SingleApkInstallPopup(
                            appName = appLabel,
                            versionText = vText,
                            modes = modes,
                            onNormalInstall = { 
                                showSingleApkInstallPopup = false
                                handleFileOpen(singleApk, context, state, focusManager, onAction)
                            },
                            onDowngradeInstall = {
                                showSingleApkInstallPopup = false
                                showDowngradeWarningSingle = true
                            },
                            onSilentInstall = {
                                showSingleApkInstallPopup = false
                                onAction(com.ripple.filemanager.AppAction.SilentInstallApk(singleApk.path, downgrade = false))
                            },
                            modifier = Modifier.padding(bottom = 8.dp, end = 16.dp),
                            cornerRoundness = state.cornerRoundness
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showBatchApkInstallPopup && showInstallFab && selectedApks.size > 1,
                    enter = androidx.compose.animation.scaleIn(
                        initialScale = 0.9f, 
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f), 
                        animationSpec = androidx.compose.animation.core.tween(120)
                    ) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(120)),
                    exit = androidx.compose.animation.scaleOut(
                        targetScale = 0.9f, 
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f), 
                        animationSpec = androidx.compose.animation.core.tween(120)
                    ) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120))
                ) {
                    val parsed = parsedBatchApks
                    if (parsed == null) {
                        Box(modifier = Modifier.padding(bottom = 8.dp, end = 16.dp).size(56.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = com.ripple.filemanager.ui.theme.SkylineColors.Amber)
                        }
                    } else {
                        val normalCount = parsed.count { !it.isDowngrade }
                        val downgradeCount = parsed.count { it.isDowngrade }
                        
                        com.ripple.filemanager.ui.BatchApkInstallPopup(
                            selectedCount = parsed.size,
                            normalPillText = "$normalCount apps",
                            normalPillIsCoral = normalCount < parsed.size,
                            downgradePillText = "$downgradeCount apps",
                            downgradePillIsCoral = downgradeCount == 0,
                            silentPillText = "$normalCount apps",
                            silentPillIsCoral = normalCount < parsed.size,
                            isShizukuConnected = state.hasShizuku,
                            onNormalClick = {
                                showBatchApkInstallPopup = false
                                batchSheetMode = com.ripple.filemanager.ui.InstallMode.NORMAL
                            },
                            onDowngradeClick = {
                                showBatchApkInstallPopup = false
                                batchSheetMode = com.ripple.filemanager.ui.InstallMode.DOWNGRADE
                            },
                            onSilentClick = {
                                showBatchApkInstallPopup = false
                                batchSheetMode = com.ripple.filemanager.ui.InstallMode.SILENT
                            },
                            modifier = Modifier.padding(bottom = 8.dp, end = 16.dp),
                            cornerRoundness = state.cornerRoundness
                        )
                    }
                }


                if (showDowngradeWarningSingle) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showDowngradeWarningSingle = false },
                        shape = getDynamicCornerShape(24f, state.cornerRoundness),
                        title = { androidx.compose.material3.Text("Downgrade Warning", color = com.ripple.filemanager.ui.theme.SkylineColors.Rust) },
                        text = { androidx.compose.material3.Text("Android OS strictly forbids downgrading most apps to prevent security vulnerabilities.\n\nTo install this older version, the currently installed app MUST be completely uninstalled first, WHICH WILL PERMANENTLY DELETE ALL ITS DATA.\n\nWould you like to proceed with a clean downgrade (wipes data)?") },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                showDowngradeWarningSingle = false
                                onAction(com.ripple.filemanager.AppAction.SilentInstallApk(selectedApks.first().path, downgrade = true, forceUninstall = true))
                            }) {
                                androidx.compose.material3.Text("Wipe Data & Downgrade", color = com.ripple.filemanager.ui.theme.SkylineColors.Rust)
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                showDowngradeWarningSingle = false
                                // Try keeping data anyway
                                onAction(com.ripple.filemanager.AppAction.SilentInstallApk(selectedApks.first().path, downgrade = true, forceUninstall = false))
                            }) {
                                androidx.compose.material3.Text("Try Keeping Data (May Fail)")
                            }
                        },
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                if (showDowngradeWarningBatch) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showDowngradeWarningBatch = false },
                        shape = getDynamicCornerShape(24f, state.cornerRoundness),
                        title = { androidx.compose.material3.Text("Batch Downgrade Warning", color = com.ripple.filemanager.ui.theme.SkylineColors.Rust) },
                        text = { androidx.compose.material3.Text("Android OS strictly forbids downgrading most apps. To install older versions, the currently installed apps MUST be completely uninstalled first, WHICH WILL PERMANENTLY DELETE ALL THEIR DATA.\n\nWould you like to proceed with a clean downgrade for all selected apps (wipes data)?") },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                showDowngradeWarningBatch = false
                                val filteredApks = parsedBatchApks?.filter { it.isDowngrade } ?: emptyList()
                                onAction(com.ripple.filemanager.AppAction.BatchInstallApks(filteredApks.map { it.file.path }, downgrade = true, silent = true, forceUninstall = true))
                            }) {
                                androidx.compose.material3.Text("Wipe Data & Downgrade", color = com.ripple.filemanager.ui.theme.SkylineColors.Rust)
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                showDowngradeWarningBatch = false
                                val filteredApks = parsedBatchApks?.filter { it.isDowngrade } ?: emptyList()
                                onAction(com.ripple.filemanager.AppAction.BatchInstallApks(filteredApks.map { it.file.path }, downgrade = true, silent = true, forceUninstall = false))
                            }) {
                                androidx.compose.material3.Text("Try Keeping Data (May Fail)")
                            }
                        },
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                if (batchSheetMode != null) {
                    val filteredApks = parsedBatchApks?.filter {
                        when (batchSheetMode) {
                            com.ripple.filemanager.ui.InstallMode.NORMAL -> !it.isDowngrade
                            com.ripple.filemanager.ui.InstallMode.SILENT -> !it.isDowngrade
                            com.ripple.filemanager.ui.InstallMode.DOWNGRADE -> it.isDowngrade
                            else -> true
                        }
                    } ?: emptyList()
                    
                    val batchApks = filteredApks.map { parsedApk ->
                        val vTransition = if (parsedApk.installedVersionName != null) "${parsedApk.installedVersionName} (${parsedApk.installedVersionCode}) -> ${parsedApk.versionName} (${parsedApk.versionCode})" else "${parsedApk.versionName} (${parsedApk.versionCode})"
                        com.ripple.filemanager.ui.BatchApkItem(
                            name = parsedApk.appName,
                            initial = parsedApk.appName.take(1).uppercase(),
                            versionTransition = vTransition,
                            note = null,
                            isNoteWarning = false,
                            overrideMode = null
                        )
                    }
                    com.ripple.filemanager.ui.BatchInstallSheet(
                        fileCount = batchApks.size,
                        selectedMode = batchSheetMode!!,
                        apks = batchApks,
                        isShizukuConnected = state.hasShizuku,
                        cornerRoundness = state.cornerRoundness,
                        onRemove = { /* implement remove later */ },
                        onCancel = { batchSheetMode = null },
                        onInstall = {
                            val mode = batchSheetMode
                            batchSheetMode = null
                            if (mode == com.ripple.filemanager.ui.InstallMode.DOWNGRADE) {
                                showDowngradeWarningBatch = true
                            } else {
                                val isSilent = mode == com.ripple.filemanager.ui.InstallMode.SILENT
                                onAction(com.ripple.filemanager.AppAction.BatchInstallApks(filteredApks.map { it.file.path }, downgrade = false, silent = isSilent))
                            }
                        }
                    )
                }

                if ((!isModalScreen && !targetLocation.startsWith("category/")) || state.isSelectionMode || state.clipboardPaths.isNotEmpty()) {
                    UnifiedBottomPill(
                        state = state,
                        archiveProgress = archiveProgress,
                        onCancelExtract = { archiveViewModel.cancelExtraction() },
                        onAction = onAction,
                        capturedTargetFolderName = capturedTargetFolderName,
                        onCaptureTargetFolder = { capturedTargetFolderName = it },
                        modifier = Modifier.fillMaxWidth(),
                        selectionModeContent = {
                            ExpressiveSelectionBar(
                                selectedCount = state.selectedFiles.size,
                                onClearSelection = {
                                    haptics.tap()
                                    onAction(AppAction.ClearSelection)
                                },
                                onSelectAll = {
                                    haptics.tap()
                                    onAction(AppAction.SelectAll())
                                },
                                onSelectNone = {
                                    haptics.tap()
                                    onAction(AppAction.SelectNone)
                                },
                                onBatchRename = {
                                    haptics.tap()
                                    onAction(AppAction.SetShowBatchRenameDialog(true))
                                },
                                onShare = {
                                    haptics.tap()
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
                                },
                                onCopy = {
                                    haptics.tap()
                                    onAction(AppAction.SetClipboard("copy"))
                                },
                                onCut = {
                                    haptics.tap()
                                    onAction(AppAction.SetClipboard("cut"))
                                },
                                onPaste = if (state.clipboardPaths.isNotEmpty()) {
                                    {
                                        haptics.tap()
                                        capturedTargetFolderName = state.currentFolderName ?: "Current Folder"
                                        onAction(AppAction.PasteClipboard(state.location))
                                        onAction(AppAction.ClearSelection)
                                    }
                                } else null,
                                onDelete = {
                                    haptics.tap()
                                    showDeleteConfirm = true
                                },
                                onInstallApks = if (showInstallFab) {
                                    {
                                        haptics.tap()
                                        if (selectedApks.size == 1) {
                                            showSingleApkInstallPopup = !showSingleApkInstallPopup
                                            showBatchApkInstallPopup = false
                                        } else {
                                            showBatchApkInstallPopup = !showBatchApkInstallPopup
                                            showSingleApkInstallPopup = false
                                        }
                                    }
                                } else null,
                                isSingleApk = selectedApks.size == 1
                            )
                        },
                        rippleNavContent = {
                            val activeTab = when {
                                state.location == "send" -> ExpressiveTab.SEND
                                state.location == "cloud" || state.location.startsWith("drive") || state.location.startsWith("mega") || state.location.startsWith("dropbox") || state.location.startsWith("smb_") || state.location.startsWith("ftp_") || state.location.startsWith("sftp_") || state.location.startsWith("webdav_") || state.location.startsWith("nextcloud_") -> ExpressiveTab.CLOUD
                                state.location.startsWith("/") && state.location != "home" -> ExpressiveTab.BROWSE
                                else -> ExpressiveTab.HOME
                            }
                            ExpressiveNavShell(
                                activeTab = activeTab,
                                onTabSelected = { tab ->
                                    haptics.tap()
                                    when (tab) {
                                        ExpressiveTab.HOME -> {
                                            com.ripple.filemanager.ui.TabSwitchLatencyTracker.onActionDispatched("home")
                                            onAction(AppAction.SelectNavTab(com.ripple.filemanager.NavTab.HOME))
                                        }
                                        ExpressiveTab.BROWSE -> {
                                            val browseTarget = state.lastBrowseLocation.ifEmpty { android.os.Environment.getExternalStorageDirectory().absolutePath }
                                            com.ripple.filemanager.ui.TabSwitchLatencyTracker.onActionDispatched(browseTarget)
                                            onAction(AppAction.SetLocation(browseTarget))
                                        }
                                        ExpressiveTab.CLOUD -> {
                                            com.ripple.filemanager.ui.TabSwitchLatencyTracker.onActionDispatched("cloud")
                                            onAction(AppAction.SelectNavTab(com.ripple.filemanager.NavTab.CLOUD))
                                        }
                                        ExpressiveTab.SEND -> {
                                            com.ripple.filemanager.ui.TabSwitchLatencyTracker.onActionDispatched("send")
                                            if (state.isSelectionMode && state.selectedFiles.isNotEmpty()) {
                                                val selectedItems = state.selectedFiles.mapNotNull { id -> state.files.find { it.id == id } }
                                                onAction(AppAction.NearbyShareAction.StageFiles(selectedItems))
                                                onAction(AppAction.ClearSelection)
                                            }
                                            onAction(AppAction.SetLocation("send"))
                                        }
                                    }
                                },
                                onFabClick = {
                                    haptics.tap()
                                    if (activeTab == ExpressiveTab.CLOUD) {
                                        showNewCloudConnectionSheet = true
                                    } else {
                                        pillNavExpanded = true
                                    }
                                },
                                isSelectionMode = state.isSelectionMode,
                                selectedCount = state.selectedFiles.size,
                                onClearSelection = { onAction(AppAction.ClearSelection) },
                                onCopy = { onAction(AppAction.SetClipboard("copy")) },
                                onMove = { onAction(AppAction.SetClipboard("cut")) },
                                onShare = {
                                    haptics.tap()
                                    val uris = state.selectedFiles.mapNotNull { id ->
                                        state.files.find { it.id == id }?.path?.let { p ->
                                            androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", java.io.File(p))
                                        }
                                    }
                                    if (uris.isNotEmpty()) {
                                        val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND_MULTIPLE).apply {
                                            type = "*/*"
                                            putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, ArrayList(uris))
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(sendIntent, "Share files"))
                                    }
                                },
                                onDelete = {
                                    haptics.tap()
                                    showDeleteConfirm = true
                                },
                                onSelectAll = { onAction(AppAction.SelectAll()) },
                                isFabExpanded = pillNavExpanded || showNewCloudConnectionSheet
                            )

                            if (pillNavExpanded) {
                                CreateBottomSheet(
                                    onDismissRequest = { pillNavExpanded = false },
                                    onNewFolder = { showCreateFolderDialog = true },
                                    onNewFile = { showCreateFileDialog = true },
                                    onAddCloud = {
                                        pillNavExpanded = false
                                        showNewCloudConnectionSheet = true
                                    },
                                    onReceive = { onAction(AppAction.SetLocation("send")) }
                                )
                            }

                            if (showNewCloudConnectionSheet) {
                                NewCloudConnectionSheet(
                                    onDismissRequest = { showNewCloudConnectionSheet = false },
                                    onConnectGoogleDrive = {
                                        if (state.isGoogleDriveAuthenticated) {
                                            launchDrivePicker()
                                        } else {
                                            val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                                                .requestEmail()
                                                .requestScopes(com.google.android.gms.common.api.Scope(com.google.api.services.drive.DriveScopes.DRIVE_FILE))
                                                .build()
                                            val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                                            client.signOut().addOnCompleteListener {
                                                googleSignInLauncher.launch(client.signInIntent)
                                            }
                                        }
                                    },
                                    onConnectMega = { onAction(com.ripple.filemanager.AppAction.SetShowMegaPopup(true)) },
                                    onConnectDropbox = { showDropboxPopup = true },
                                    onConnectNextcloud = { showNextcloudConnections = true },
                                    onConnectWebDav = { showWebDavConnections = true },
                                    onConnectSftp = { showSftpConnections = true },
                                    onConnectFtp = { showFtpConnections = true },
                                    onConnectSmb = { showSmbConnections = true }
                                )
                            }
                        }
                    )
                }

                if (showCreateFolderDialog) {
                    var folderName by remember { mutableStateOf("") }
                    com.ripple.filemanager.ui.GradientAlertDialog(
                        onDismissRequest = { showCreateFolderDialog = false },
                        title = { MonoLabel("NEW FOLDER", color = SkylineColors.TextPrimary, fontSize = 14) },
                        text = {
                            OutlinedTextField(
                                value = folderName,
                                onValueChange = { folderName = it },
                                label = { Text(stringResource(R.string.folder_name_label)) },
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
                                Text(stringResource(R.string.save_action), color = SkylineColors.TextPrimary)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { haptics.tap(); showCreateFolderDialog = false }) {
                                Text(stringResource(R.string.cancel), color = SkylineColors.TextDim)
                            }
                        },
                        containerColor = SkylineColors.Surface,
                        shape = getDynamicCornerShape(12f, state.cornerRoundness)
                    )
                }

                if (showCreateFileDialog) {
                    var fileName by remember { mutableStateOf("") }
                    com.ripple.filemanager.ui.GradientAlertDialog(
                        onDismissRequest = { showCreateFileDialog = false },
                        title = { MonoLabel("NEW FILE", color = SkylineColors.TextPrimary, fontSize = 14) },
                        text = {
                            OutlinedTextField(
                                value = fileName,
                                onValueChange = { fileName = it },
                                label = { Text(stringResource(R.string.file_name_label)) },
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
                                Text(stringResource(R.string.save_action), color = SkylineColors.TextPrimary)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { haptics.tap(); showCreateFileDialog = false }) {
                                Text(stringResource(R.string.cancel), color = SkylineColors.TextDim)
                            }
                        },
                        containerColor = SkylineColors.Surface,
                        shape = getDynamicCornerShape(12f, state.cornerRoundness)
                    )
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
            val targetFile = fileToDelete
            
            val allAvailableFiles = remember(state.files, state.cleanerData) {
                (state.files + (state.cleanerData?.let { cd ->
                    cd.documents.files + cd.images.files + cd.videos.files + 
                    cd.audio.files + cd.apps.files + cd.archives.files + 
                    cd.downloads.files + cd.large.files + cd.duplicates.files
                } ?: emptyList())).distinctBy { it.id }
            }
            
            val selectedFilesList = remember(state.selectedFiles, allAvailableFiles, targetFile) {
                if (targetFile != null) {
                    listOf(targetFile)
                } else {
                    state.selectedFiles.mapNotNull { id -> allAvailableFiles.find { it.id == id } }
                }
            }
            
            val titleText = when {
                targetFile != null -> "Delete \"${targetFile.name}\"?"
                selectedFilesList.size == 1 -> "Delete \"${selectedFilesList.first().name}\"?"
                state.selectedFiles.size == 1 -> "Delete selected item?"
                state.selectedFiles.size > 1 -> "Delete ${state.selectedFiles.size} items?"
                selectedFilesList.size > 1 -> "Delete ${selectedFilesList.size} items?"
                else -> "Delete selected items?"
            }
            
            val warningText = if (isDrive) {
                "Deleting is irreversible proceed"
            } else if (state.isRecycleBinEnabled) {
                "Files will be moved to Trash."
            } else {
                "This action cannot be undone."
            }

            ExpressiveConfirmationSheet(
                title = titleText,
                subtitle = warningText,
                confirmLabel = stringResource(R.string.delete_action),
                cancelLabel = stringResource(R.string.cancel),
                onConfirm = {
                    haptics.delete()
                    if (targetFile != null) {
                        onAction(AppAction.ClearSelection)
                        onAction(AppAction.ToggleSelection(targetFile.id))
                        onAction(AppAction.DeleteSelectedFiles)
                        fileToDelete = null
                    } else {
                        onAction(AppAction.DeleteSelectedFiles)
                    }
                    showDeleteConfirm = false
                },
                onDismissRequest = { 
                    haptics.tap()
                    showDeleteConfirm = false 
                    fileToDelete = null
                }
            )
        }

        if (showDropboxPopup) {
            if (state.isDropboxAuthenticated) {
                ExpressiveConfirmationSheet(
                    title = "Log out of Dropbox?",
                    subtitle = state.dropboxAccountEmail ?: "Connected account",
                    confirmLabel = stringResource(R.string.logout_action),
                    cancelLabel = stringResource(R.string.cancel),
                    onConfirm = {
                        onAction(AppAction.SetDropboxAuthStatus(false, null))
                        showDropboxPopup = false
                        if (state.location.startsWith("dropbox")) {
                            onAction(AppAction.SetLocation("cloud"))
                        }
                    },
                    onDismissRequest = {
                        showDropboxPopup = false
                    }
                )
            } else {
                CloudLoginBottomSheet(
                    providerName = "Dropbox",
                    providerIcon = Icons.Outlined.Cloud,
                    providerCategory = "videos",
                    registerUrl = "https://dropbox.com/register",
                    recoveryUrl = "https://www.dropbox.com/forgot",
                    onLogin = { email, password ->
                        onAction(AppAction.SetDropboxAuthStatus(true, email))
                        showDropboxPopup = false
                    },
                    onDismissRequest = {
                        showDropboxPopup = false
                    }
                )
            }
        }

        if (showSmbConnections) {
            SmbConnectionsDialog(
                state = state.smbState,
                onAction = onAction,
                onDismiss = { showSmbConnections = false }
            )
        }

        if (showFtpConnections) {
            FtpConnectionsDialog(
                state = state.ftpState,
                onAction = onAction,
                onDismiss = { showFtpConnections = false }
            )
        }

        if (showSftpConnections) {
            SftpConnectionsDialog(
                state = state.sftpState,
                onAction = onAction,
                onDismiss = { showSftpConnections = false }
            )
        }

        if (showWebDavConnections) {
            WebDavConnectionsDialog(
                state = state.webDavState,
                onAction = onAction,
                onDismiss = { showWebDavConnections = false }
            )
        }

        if (showNextcloudConnections) {
            NextcloudConnectionsDialog(
                state = state.webDavState,
                onAction = onAction,
                onDismiss = { showNextcloudConnections = false }
            )
        }

        if (extractTargetFile != null) {
            if (!isExtracting) {
                FolderPickerDialog(
                    onDismiss = { extractTargetFile = null },
                    onFolderSelected = { path ->
                        isExtracting = true
                        onAction(AppAction.ExtractZip(extractTargetFile!!.path, path))
                    },
                    cornerRoundness = state.cornerRoundness
                )
            } else {
                if (state.extractProgress == null) {
                    isExtracting = false
                    extractTargetFile = null
                } else {
                    com.ripple.filemanager.ui.GradientAlertDialog(
                        onDismissRequest = {},
                        title = { com.ripple.filemanager.ui.MonoLabel("EXTRACTING ZIP", color = com.ripple.filemanager.ui.theme.SkylineColors.Amber, fontSize = 14) },
                        text = {
                            Column {
                                Text(stringResource(R.string.extracting_file, extractTargetFile!!.name), color = com.ripple.filemanager.ui.theme.SkylineColors.TextPrimary)
                                Spacer(modifier = Modifier.height(16.dp))
                                LinearProgressIndicator(
                                    progress = { state.extractProgress ?: 0f },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = com.ripple.filemanager.ui.theme.SkylineColors.Amber
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(stringResource(R.string.extraction_progress_percent, ((state.extractProgress ?: 0f) * 100).toInt()), color = com.ripple.filemanager.ui.theme.SkylineColors.TextPrimary)
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { onAction(AppAction.ToggleExtractPause) }) {
                                Text(if (state.isExtractPaused) "RESUME" else "PAUSE", color = com.ripple.filemanager.ui.theme.SkylineColors.Amber)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { 
                                onAction(AppAction.CancelExtract)
                            }) {
                                Text(stringResource(R.string.cancel), color = com.ripple.filemanager.ui.theme.SkylineColors.TextDim)
                            }
                        },
                        containerColor = com.ripple.filemanager.ui.theme.SkylineColors.Surface,
                        shape = getDynamicCornerShape(12f, state.cornerRoundness)
                    )
                }
            }
        }

        if (viewingArchive != null) {
            com.ripple.filemanager.ui.ArchiveViewerDialog(
                archiveFile = viewingArchive!!,
                onDismiss = { viewingArchive = null },
                onExtractRequest = {
                    fileForSafExtraction = viewingArchive
                    safExtractLauncher.launch(null)
                    viewingArchive = null
                }
            )
        }
        
        if (showArchiveOptionsFor != null) {
            val file = showArchiveOptionsFor!!
            // Detect format via magic bytes; fall back to extension while async detection runs
            var isRar by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(file.name.lowercase().endsWith(".rar")) }
            androidx.compose.runtime.LaunchedEffect(file.path) {
                val sourceUri = if (file.path.startsWith("content://")) android.net.Uri.parse(file.path) else android.net.Uri.fromFile(File(file.path))
                isRar = archiveViewModel.detectIsRar(sourceUri)
            }
            com.ripple.filemanager.ui.ArchiveExtractDialog(
                cornerRoundness = state.cornerRoundness,
                archiveName = file.name,
                itemCount = file.size,
                isRar = isRar,
                onExtractHere = {
                    val sourceUri = if (file.path.startsWith("content://")) android.net.Uri.parse(file.path) else android.net.Uri.fromFile(File(file.path))
                    if (file.path.startsWith("content://")) {
                        fileForSafExtraction = file
                        safExtractLauncher.launch(null)
                    } else {
                        val destUri = android.net.Uri.fromFile(File(file.path).parentFile)
                        archiveViewModel.extract(sourceUri, destUri)
                    }
                    showArchiveOptionsFor = null
                },
                onExtractTo = {
                    fileForSafExtraction = file
                    safExtractLauncher.launch(null)
                },
                onViewContents = {
                    viewingArchive = file
                    showArchiveOptionsFor = null
                },
                onDelete = {
                    onAction(AppAction.ClearSelection)
                    onAction(AppAction.ToggleSelection(file.id))
                    onAction(AppAction.DeleteSelectedFiles)
                    showArchiveOptionsFor = null
                },
                onDismiss = { showArchiveOptionsFor = null }
            )
        }

        val progressState = archiveProgress
        if (progressState is com.ripple.filemanager.archive.ArchiveProgress.Complete) {
            androidx.compose.runtime.LaunchedEffect(progressState) {
                kotlinx.coroutines.delay(1500)
                archiveViewModel.resetState()
                onAction(AppAction.Reload)
            }
        } else if (progressState is com.ripple.filemanager.archive.ArchiveProgress.NeedsPassword) {
            com.ripple.filemanager.ui.ArchivePasswordDialog(
                cornerRoundness = state.cornerRoundness,
                archiveName = "Encrypted Archive",
                attemptFailed = progressState.attemptFailed,
                onSubmit = { password -> archiveViewModel.submitPassword(password) },
                onDismiss = { archiveViewModel.cancelExtraction() }
            )
        } else if (progressState is com.ripple.filemanager.archive.ArchiveProgress.Failed) {
            com.ripple.filemanager.ui.ArchiveFailedDialog(
                cornerRoundness = state.cornerRoundness,
                archiveName = "Extraction Error",
                reason = progressState.reason,
                onDismiss = { archiveViewModel.resetState() }
            )
        }
        
        if (state.extractResultPath != null) {
            com.ripple.filemanager.ui.GradientAlertDialog(
                onDismissRequest = { onAction(AppAction.ClearExtractResult) },
                title = { com.ripple.filemanager.ui.MonoLabel("EXTRACTION COMPLETE", color = com.ripple.filemanager.ui.theme.SkylineColors.Amber, fontSize = 14) },
                text = { Text(stringResource(R.string.extract_open_prompt)) },
                confirmButton = {
                    TextButton(onClick = { 
                        val path = state.extractResultPath
                        onAction(AppAction.ClearExtractResult)
                        if (path != null) {
                            onAction(AppAction.SetLocation(path))
                        }
                    }) {
                        Text(stringResource(R.string.open_action), color = com.ripple.filemanager.ui.theme.SkylineColors.Amber)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onAction(AppAction.ClearExtractResult) }) {
                        Text(stringResource(R.string.cancel), color = com.ripple.filemanager.ui.theme.SkylineColors.TextDim)
                    }
                },
                containerColor = com.ripple.filemanager.ui.theme.SkylineColors.Surface,
                shape = getDynamicCornerShape(12f, state.cornerRoundness)
            )
        }
        
        if (infoDialogFile != null) {
            val targetFile = infoDialogFile!!
            var showRenameForThisFile by remember { mutableStateOf(false) }
            var renameText by remember { mutableStateOf(targetFile.name) }

            if (showRenameForThisFile) {
                com.ripple.filemanager.ui.GradientAlertDialog(
                    onDismissRequest = { showRenameForThisFile = false },
                    title = { com.ripple.filemanager.ui.MonoLabel("RENAME", color = com.ripple.filemanager.ui.expressive.ExpressiveTheme.colors.text, fontSize = 14) },
                    text = {
                        OutlinedTextField(
                            value = renameText,
                            onValueChange = { renameText = it },
                            label = { Text("New name") },
                            singleLine = true,
                            shape = getDynamicCornerShape(12f, state.cornerRoundness)
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (renameText.isNotBlank()) {
                                onAction(AppAction.RenameFile(targetFile.path, renameText.trim()))
                            }
                            showRenameForThisFile = false
                            infoDialogFile = null
                        }) {
                            Text("SAVE", color = com.ripple.filemanager.ui.expressive.ExpressiveTheme.colors.accent)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRenameForThisFile = false }) {
                            Text("CANCEL", color = com.ripple.filemanager.ui.expressive.ExpressiveTheme.colors.muted)
                        }
                    },
                    containerColor = com.ripple.filemanager.ui.expressive.ExpressiveTheme.colors.container,
                    shape = getDynamicCornerShape(12f, state.cornerRoundness)
                )
            }

            FileInfoBottomSheet(
                file = targetFile,
                fileDetails = fileDetails,
                onDismissRequest = { infoDialogFile = null },
                onShare = {
                    haptics.tap()
                    infoDialogFile = null
                    val uri = if (targetFile.path.startsWith("content://")) {
                        android.net.Uri.parse(targetFile.path)
                    } else {
                        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(targetFile.path))
                    }
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "*/*"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share ${targetFile.name}"))
                },
                onRename = {
                    haptics.tap()
                    renameText = targetFile.name
                    showRenameForThisFile = true
                },
                onDelete = {
                    haptics.tap()
                    fileToDelete = targetFile
                    infoDialogFile = null
                    showDeleteConfirm = true
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
                        Text(stringResource(R.string.downloading_file), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        if (state.incomingNearbyRequest != null) {
            val req = state.incomingNearbyRequest
            com.ripple.filemanager.ui.expressive.IncomingTransferBottomSheet(
                request = req,
                onAccept = {
                    onAction(com.ripple.filemanager.AppAction.NearbyShareAction.AcceptIncoming(req.sessionId))
                },
                onDecline = {
                    onAction(com.ripple.filemanager.AppAction.NearbyShareAction.DeclineIncoming(req.sessionId))
                }
            )
        }

        if (state.receivedFilesPrompt != null) {
            val prompt = state.receivedFilesPrompt
            com.ripple.filemanager.ui.expressive.ReceivedFilesBottomSheet(
                prompt = prompt,
                onOpen = { fileItem ->
                    onAction(com.ripple.filemanager.AppAction.NearbyShareAction.DismissReceivedFilesPrompt)
                    handleFileOpen(fileItem, context, state, focusManager, onAction)
                },
                onOpenFolder = { folderPath ->
                    onAction(com.ripple.filemanager.AppAction.NearbyShareAction.DismissReceivedFilesPrompt)
                    onAction(com.ripple.filemanager.AppAction.NavTabTapped("browse"))
                    onAction(com.ripple.filemanager.AppAction.SetLocation(folderPath, "RippleReceived"))
                },
                onDismiss = {
                    onAction(com.ripple.filemanager.AppAction.NearbyShareAction.DismissReceivedFilesPrompt)
                }
            )
        }
    }
}

@Composable
fun Sidebar(currentLocation: String, onLocationSelected: (String) -> Unit, cornerRoundness: Float, modifier: Modifier = Modifier) {
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
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
                Text(stringResource(R.string.app_name_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text(stringResource(R.string.app_subtitle), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Text(stringResource(R.string.create_action), fontWeight = FontWeight.ExtraBold)
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            NavButton("Home", "home", Icons.Default.Home, currentLocation, cornerRoundness, onLocationSelected)
            NavButton("Recents", "recent", Icons.Default.Schedule, currentLocation, cornerRoundness, onLocationSelected)
            NavButton("Pinned", "pinned", Icons.Default.PushPin, currentLocation, cornerRoundness, onLocationSelected)
            NavButton("Drive", "drive", Icons.Default.Cloud, currentLocation, cornerRoundness, onLocationSelected)
            NavButton("Trash", "trash", Icons.Default.Delete, currentLocation, cornerRoundness, onLocationSelected)
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh, getDynamicCornerShape(22f, cornerRoundness)).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.device_storage_label), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.storage_percentage, 64), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(progress = { 0.64f }, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(10.dp).clip(getDynamicCornerShape(5f, cornerRoundness)))
            Text(stringResource(R.string.storage_usage_summary, "164 GB", "256 GB", "Photos and videos use the most space."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun NavButton(label: String, id: String, icon: androidx.compose.ui.graphics.vector.ImageVector, currentId: String, cornerRoundness: Float, onSelect: (String) -> Unit) {
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
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
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    val items = listOf(
        Triple("home", androidx.compose.material.icons.Icons.Filled.Home, androidx.compose.material.icons.Icons.Outlined.Home),
        Triple("recent", androidx.compose.material.icons.Icons.Filled.Schedule, androidx.compose.material.icons.Icons.Outlined.Schedule),
        Triple("pinned", androidx.compose.material.icons.Icons.Filled.PushPin, androidx.compose.material.icons.Icons.Outlined.PushPin),
        Triple("drive", androidx.compose.material.icons.Icons.Filled.Cloud, androidx.compose.material.icons.Icons.Outlined.Cloud)
    )
    
    val selectedIndex = items.indexOfFirst { (id, _, _) ->
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
        items = items.map { (id, activeIcon, inactiveIcon) ->
            val label = when(id) {
                "home"    -> "Home"
                "recent"  -> "Recent"
                "pinned" -> "Pinned"
                "drive"   -> "Cloud"
                else      -> id
            }
            BottomNavItem(id = id, activeIcon = activeIcon, inactiveIcon = inactiveIcon, label = label)
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
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
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
            color = com.ripple.filemanager.ui.theme.SkylineColors.Surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                com.ripple.filemanager.ui.MonoLabel(
                    text = "BATCH RENAME",
                    color = com.ripple.filemanager.ui.theme.SkylineColors.Amber,
                    fontSize = 16
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
                        label = { Text(stringResource(R.string.base_name_label)) },
                        modifier = Modifier.weight(2f),
                        singleLine = true,
                        shape = getDynamicCornerShape(12f, cornerRoundness)
                    )
                    OutlinedTextField(
                        value = extension,
                        onValueChange = { extension = it },
                        label = { Text(stringResource(R.string.file_type_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = getDynamicCornerShape(12f, cornerRoundness)
                    )
                }

                Text(stringResource(R.string.numbering_style_label), style = MaterialTheme.typography.labelMedium)
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
                                selectedContainerColor = com.ripple.filemanager.ui.theme.SkylineColors.Amber,
                                selectedLabelColor = Color(0xFF161009)
                            )
                        )
                    }
                }

                if (numberingStyle != "None") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startNumberStr,
                            onValueChange = { startNumberStr = it },
                            label = { Text(stringResource(R.string.starts_from_label)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = getDynamicCornerShape(12f, cornerRoundness)
                        )
                        OutlinedTextField(
                            value = paddingStr,
                            onValueChange = { paddingStr = it },
                            label = { Text(stringResource(R.string.leading_zeros_label)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = getDynamicCornerShape(12f, cornerRoundness)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { haptics.tap(); isPrefix = true }) {
                            RadioButton(
                                selected = isPrefix,
                                onClick = { isPrefix = true },
                                colors = RadioButtonDefaults.colors(selectedColor = com.ripple.filemanager.ui.theme.SkylineColors.Amber)
                            )
                            Text(stringResource(R.string.prefix_label))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { haptics.tap(); isPrefix = false }) {
                            RadioButton(
                                selected = !isPrefix,
                                onClick = { isPrefix = false },
                                colors = RadioButtonDefaults.colors(selectedColor = com.ripple.filemanager.ui.theme.SkylineColors.Amber)
                            )
                            Text(stringResource(R.string.suffix_label))
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
                            Text(stringResource(R.string.cancel), color = com.ripple.filemanager.ui.theme.SkylineColors.TextDim)
                        }
                        Button(
                            onClick = {
                                val pad = paddingStr.toIntOrNull() ?: 0
                                val start = startNumberStr.toIntOrNull() ?: 1
                                onRename(baseName, extension, pad, start, isPrefix, numberingStyle)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = com.ripple.filemanager.ui.theme.SkylineColors.Amber, contentColor = Color(0xFF161009))
                        ) {
                            Text(stringResource(R.string.rename_action))
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
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
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
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    var expanded by remember { mutableStateOf(false) }
    Surface(
        shape = getDynamicCornerShape(16f, cornerRoundness),
        color = com.ripple.filemanager.ui.theme.SkylineColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, com.ripple.filemanager.ui.theme.SkylineColors.Border),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = com.ripple.filemanager.ui.theme.SkylineColors.TextPrimary)
            androidx.compose.foundation.layout.Box {
                Surface(
                    shape = getDynamicCornerShape(12f, cornerRoundness),
                    color = com.ripple.filemanager.ui.theme.SkylineColors.Surface2,
                    border = androidx.compose.foundation.BorderStroke(1.dp, com.ripple.filemanager.ui.theme.SkylineColors.Border),
                    onClick = { expanded = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(currentValue, style = MaterialTheme.typography.bodyMedium, color = com.ripple.filemanager.ui.theme.SkylineColors.TextPrimary)
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.select_content_desc), tint = com.ripple.filemanager.ui.theme.SkylineColors.TextDim, modifier = Modifier.size(18.dp))
                    }
                }
                androidx.compose.material3.DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(stringResource(R.string.in_app_viewer)) },
                        onClick = { onValueChange("In-app"); expanded = false }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(stringResource(R.string.device_default_viewer)) },
                        onClick = { onValueChange("Device default"); expanded = false }
                    )
                }
            }
        }
    }
}

fun openFileInExternalApp(context: android.content.Context, file: com.ripple.filemanager.FileItem, defaultMime: String, onShowToast: ((String) -> Unit)? = null) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", java.io.File(file.path))
        val mimeType = when {
            file.name.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            file.name.endsWith(".html", ignoreCase = true) || file.name.endsWith(".htm", ignoreCase = true) -> "text/html"
            file.name.endsWith(".csv", ignoreCase = true) -> "text/csv"
            file.name.endsWith(".json", ignoreCase = true) -> "application/json"
            file.name.endsWith(".xml", ignoreCase = true) -> "text/xml"
            file.name.endsWith(".txt", ignoreCase = true) || file.name.endsWith(".json", ignoreCase = true) || file.name.endsWith(".md", ignoreCase = true) || file.name.endsWith(".log", ignoreCase = true) || file.name.endsWith(".kt", ignoreCase = true) || file.name.endsWith(".java", ignoreCase = true) || file.name.endsWith(".py", ignoreCase = true) -> "text/plain"
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
        val msg = context.getString(R.string.no_app_found)
        if (onShowToast != null) {
            onShowToast(msg)
        } else {
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun GradientProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainerHigh
) {
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
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
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
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
                    color = com.ripple.filemanager.ui.theme.SkylineColors.TextDim,
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

@Composable
fun AuthDialog(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (password: String?) -> Unit,
    onShowToast: ((String) -> Unit)? = null
) {
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    var password by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("sift_prefs", android.content.Context.MODE_PRIVATE)
    val biometricEnabled = prefs.getBoolean("lock_biometric_enabled", false)
    var biometricFailures by remember { mutableStateOf(0) }
    var showPasswordField by remember { mutableStateOf(!biometricEnabled) }
    
    // Auto trigger biometric if enabled
    LaunchedEffect(Unit) {
        if (biometricEnabled && context is androidx.fragment.app.FragmentActivity) {
            var biometricPrompt: androidx.biometric.BiometricPrompt? = null
            val executor = androidx.core.content.ContextCompat.getMainExecutor(context)
            val callback = object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // Auth directly without password
                    onConfirm(null)
                }
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    biometricFailures++
                    if (biometricFailures >= 3) {
                        biometricPrompt?.cancelAuthentication()
                        val msg = context.getString(R.string.fingerprint_failed_fallback)
                        if (onShowToast != null) {
                            onShowToast(msg)
                        } else {
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                        }
                        showPasswordField = true
                    }
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    showPasswordField = true
                }
            }
            biometricPrompt = androidx.biometric.BiometricPrompt(context, executor, callback)
            val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                .setTitle("Authenticate")
                .setSubtitle("Use your device credential to verify access")
                .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()
            biometricPrompt.authenticate(promptInfo)
        }
    }

    if (showPasswordField) {
        com.ripple.filemanager.ui.GradientAlertDialog(
            onDismissRequest = onDismiss,
            containerColor = com.ripple.filemanager.ui.theme.SkylineColors.Surface,
            shape = com.ripple.filemanager.ui.getDynamicCornerShape(12f, 0.5f),
            title = {
                com.ripple.filemanager.ui.MonoLabel(
                    text = "AUTHENTICATE",
                    color = com.ripple.filemanager.ui.theme.SkylineColors.Amber,
                    fontSize = 14
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.enter_global_password)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        shape = com.ripple.filemanager.ui.getDynamicCornerShape(12f, 0.5f)
                    )
                    if (errorMessage != null) {
                        Text(text = errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = { 
                        if (password.isNotEmpty()) {
                            onConfirm(password)
                        }
                    }
                ) {
                    Text(stringResource(R.string.verify_action), color = com.ripple.filemanager.ui.theme.SkylineColors.Amber)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel), color = com.ripple.filemanager.ui.theme.SkylineColors.TextDim)
                }
            }
        )
    }
}

