package com.ripple.filemanager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.provider.DocumentsContract
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.content.ContextCompat
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import com.ripple.filemanager.AppAction
import com.ripple.filemanager.ui.SiftApp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.reload()
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        splashScreen.setKeepOnScreenCondition {
            viewModel.state.value.isLoading
        }
        
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(applicationContext)
        
        try {
            rikka.shizuku.Shizuku.addRequestPermissionResultListener { requestCode: Int, grantResult: Int ->
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    viewModel.reload()
                }
            }
        } catch (e: Exception) {}

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val state by viewModel.state.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            
            LaunchedEffect(viewModel) {
                viewModel.snackbarMessage.collect { message ->
                    snackbarHostState.showSnackbar(message = message, withDismissAction = true)
                }
            }
            
            val onAction: (AppAction) -> Unit = remember {
                { action ->
                    when (action) {
                        is AppAction.SetLocation -> viewModel.setLocation(action.path)
                        is AppAction.NavTabTapped -> viewModel.navTabTapped(action.id)
                        is AppAction.NavigateBackInDrive -> viewModel.navigateBackInDrive()
                        is AppAction.SetQuery -> viewModel.setQuery(action.query)
                        is AppAction.SetFilter -> viewModel.setFilter(action.filter)
                        is AppAction.ToggleViewMode -> viewModel.toggleViewMode()
                        is AppAction.SetSortMode -> viewModel.setSortMode(action.mode)
                        is AppAction.SetTrashScreenVisible -> viewModel.setTrashScreenVisible(action.visible)
                        is AppAction.SetCleanerScreenVisible -> viewModel.setCleanerScreenVisible(action.visible)
                        is AppAction.SetShowSettingsScreen -> viewModel.setShowSettingsScreen(action.show)
                        is AppAction.ClearRecoverableAuthIntent -> viewModel.clearRecoverableAuthIntent()
                        is AppAction.SetShowThemeSheet -> viewModel.setShowThemeSheet(action.show)
                        is AppAction.SetShowBatchRenameDialog -> viewModel.setShowBatchRenameDialog(action.show)
                        is AppAction.ClearSelection -> viewModel.clearSelection()
                        is AppAction.ToggleSelection -> viewModel.toggleSelection(action.id)
                        is AppAction.SelectAll -> viewModel.selectAll()
                        is AppAction.SelectNone -> viewModel.selectNone()
                        is AppAction.OpenFileViewer -> viewModel.openFileViewer(action.id)
                        is AppAction.CloseFileViewer -> viewModel.closeFileViewer()
                        is AppAction.CreateFolder -> viewModel.createFolder(action.name)
                        is AppAction.CreateFile -> viewModel.createFile(action.name)
                        is AppAction.TogglePin -> viewModel.togglePin(action.path)
                        is AppAction.RenameFile -> viewModel.renameFile(action.path, action.newName)
                        is AppAction.BatchRenameFiles -> viewModel.batchRenameFiles(action.baseName, action.extension, action.padding, action.startNumber, action.isPrefix, action.style)
                        is AppAction.DeleteSelectedFiles -> viewModel.deleteSelectedFiles()
                        is AppAction.SetClipboard -> viewModel.setClipboard(action.action)
                        is AppAction.ClearClipboard -> viewModel.clearClipboard()
                        is AppAction.PasteClipboard -> viewModel.pasteClipboard(action.location)
                        is AppAction.TogglePastePause -> viewModel.togglePastePause()
                        is AppAction.CancelPaste -> viewModel.cancelPaste()
                        is AppAction.ExtractZip -> viewModel.extractZip(action.sourcePath, action.destPath)
                        is AppAction.ToggleExtractPause -> viewModel.toggleExtractPause()
                        is AppAction.CancelExtract -> viewModel.cancelExtract()
                        is AppAction.ClearExtractResult -> viewModel.clearExtractResult()
                        is AppAction.SetGoogleDriveAuthStatus -> viewModel.setGoogleDriveAuthStatus(action.isAuthenticated, action.email)
                        is AppAction.SetMegaAuthStatus -> viewModel.setMegaAuthStatus(action.isAuthenticated, action.email, action.password)
                        is AppAction.SetShowMegaPopup -> viewModel.setShowMegaPopup(action.show)
                        is AppAction.SetDropboxAuthStatus -> viewModel.setDropboxAuthStatus(action.isAuthenticated, action.email)
                        is AppAction.AutoRequestAccess -> viewModel.autoRequestAccess(action.path)
                        is AppAction.RequestShizukuAccess -> { /* Shizuku access handled internally by rikka */ }
                        is AppAction.Reload -> viewModel.reload()
                        is AppAction.SetErrorMessage -> viewModel.setErrorMessage(action.message)
                        is AppAction.LoadFileDetails -> {
                            lifecycleScope.launch {
                                val details = viewModel.getFileDetails(action.path)
                                action.onLoaded(details)
                            }
                        }
                        is AppAction.RefreshTrash -> viewModel.loadTrashFiles()
                        is AppAction.RestoreTrashFiles -> viewModel.restoreTrashFiles(action.files)
                        is AppAction.PermanentlyDeleteTrashFiles -> viewModel.permanentlyDeleteTrashFiles(action.files)
                        is AppAction.SetRecycleBinSettings -> viewModel.setRecycleBinSettings(action.enabled, action.retentionValue, action.retentionUnit)
                        is AppAction.SetThemeMode -> viewModel.setThemeMode(action.mode)
                        is AppAction.SetThemeHue -> viewModel.setThemeHue(action.hue)
                        is AppAction.SetDynamicSystemTheme -> viewModel.setDynamicSystemTheme(action.dynamic)
                        is AppAction.SetIconShape -> viewModel.setIconShape(action.shape)
                        is AppAction.SetFontStyle -> viewModel.setFontStyle(action.style)
                        is AppAction.ToggleTextDecoration -> viewModel.toggleTextDecoration(action.decoration)
                        is AppAction.SetMainTextScale -> viewModel.setMainTextScale(action.scale)
                        is AppAction.SetSubTextScale -> viewModel.setSubTextScale(action.scale)
                        is AppAction.SetCornerRoundness -> viewModel.setCornerRoundness(action.roundness)
                        is AppAction.SetGridColumns -> viewModel.setGridColumns(action.columns)
                        is AppAction.SetCleanerCategory -> viewModel.setCleanerCategory(action.category)
                        is AppAction.SelectAllCleanerFiles -> viewModel.selectAllCleanerFiles(action.ids)
                        is AppAction.ClearCleanerSelection -> viewModel.clearCleanerSelection()
                        is AppAction.DeleteSelectedCleanerFiles -> viewModel.deleteSelectedCleanerFiles()
                        is AppAction.ToggleCleanerSelection -> viewModel.toggleCleanerSelection(action.id)
                        is AppAction.PlayAudio -> viewModel.playAudio(action.file)
                        is AppAction.ToggleAudioPlayback -> viewModel.toggleAudioPlayback()
                        is AppAction.SeekAudio -> viewModel.seekAudio(action.position)
                        is AppAction.StopAudio -> viewModel.stopAudio()
                        is AppAction.PlayNextAudio -> viewModel.playNextAudio()
                        is AppAction.PlayPreviousAudio -> viewModel.playPreviousAudio()
                        is AppAction.SetShowFullScreenPlayer -> viewModel.setShowFullScreenPlayer(action.show)
                        is AppAction.SetOrganiserPath -> viewModel.setOrganiserPath(action.category, action.path)
                        is AppAction.OrganiseDownloads -> viewModel.organiseDownloads()
                        is AppAction.SetViewerPreference -> viewModel.setViewerPreference(action.category, action.preference)
                        is AppAction.RequestAuth -> viewModel.requestAuth(action.reason, action.path, action.fileId, action.folderName)
                        is AppAction.CancelAuth -> viewModel.cancelAuth()
                        is AppAction.AuthSuccess -> viewModel.authSuccess(action.reason, action.path, action.fileId, action.folderName, action.password)
                        is AppAction.UpdateGlobalPassword -> viewModel.updateGlobalPassword(action.oldPassword, action.newPassword)
                        is AppAction.SetBiometricEnabled -> viewModel.setBiometricEnabled(action.enabled)
                    }
                }
            }

            SiftApp(
                state = state,
                onAction = onAction,
                snackbarHostState = snackbarHostState,
                windowWidthSizeClass = windowSizeClass.widthSizeClass
            )
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
        viewModel.reload()
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        } else {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val needed = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (needed.isNotEmpty()) {
                requestPermissionLauncher.launch(needed.toTypedArray())
            }
        }
    }
}
