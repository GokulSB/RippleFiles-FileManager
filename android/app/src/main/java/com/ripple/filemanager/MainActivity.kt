package com.ripple.filemanager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.compose.ui.unit.sp
import android.provider.Settings
import android.provider.DocumentsContract
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.content.ContextCompat
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import android.widget.Toast
import com.ripple.filemanager.AppAction
import com.ripple.filemanager.data.core.TransferMode
import com.ripple.filemanager.ui.SiftApp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private var hasUserDismissedPermissionDialog = false
    private var permissionDialog: android.app.AlertDialog? = null
    private var lastHadStorageAccess: Boolean? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        val granted = hasStorageAccess()
        viewModel.updateStorageAccessState(granted)
        if (granted) {
            viewModel.scanDeviceForCleaner(showLoading = false)
        }
        viewModel.reload()
    }

    private var isFirstResume = true

    override fun attachBaseContext(newBase: android.content.Context) {
        val prefs = newBase.getSharedPreferences("sift_prefs", android.content.Context.MODE_PRIVATE)
        val langTag = prefs.getString("app_language", "device_default") ?: "device_default"
        val context = if (langTag != "device_default" && langTag.isNotBlank()) {
            try {
                val locale = java.util.Locale.forLanguageTag(langTag)
                java.util.Locale.setDefault(locale)
                val config = android.content.res.Configuration(newBase.resources.configuration)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    config.setLocales(android.os.LocaleList(locale))
                } else {
                    config.setLocale(locale)
                }
                newBase.createConfigurationContext(config)
            } catch (e: Exception) {
                newBase
            }
        } else {
            newBase
        }
        super.attachBaseContext(context)
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val crashLogPath = intent.getStringExtra("CRASH_LOG")
        if (crashLogPath != null) {
            super.onCreate(savedInstanceState)
            setContent {
                val text = try { java.io.File(crashLogPath).readText() } catch (e: Exception) { "Error reading crash log: ${e.message}" }
                androidx.compose.material3.MaterialTheme {
                    androidx.compose.material3.Surface(
                        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.background
                    ) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = {},
                            title = { androidx.compose.material3.Text("App Crashed!") },
                            text = { 
                                androidx.compose.foundation.lazy.LazyColumn { 
                                    item { androidx.compose.material3.Text(text, style = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)) } 
                                } 
                            },
                            confirmButton = {
                                androidx.compose.material3.TextButton(onClick = {
                                    val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                    clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("Crash Log", text))
                                    android.widget.Toast.makeText(this@MainActivity, "Crash log copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                                }) {
                                    androidx.compose.material3.Text("Copy Log")
                                }
                            },
                            dismissButton = {
                                androidx.compose.material3.TextButton(onClick = { finish() }) {
                                    androidx.compose.material3.Text("Close")
                                }
                            }
                        )
                    }
                }
            }
            return
        }

        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            try {
                val trace = android.util.Log.getStackTraceString(throwable)
                android.util.Log.e("RippleCrash", "Fatal exception in Ripple Files", throwable)
                val crashLog = java.io.File(filesDir, "crash_log.txt")
                crashLog.writeText(trace)
                try {
                    val extLog = java.io.File(getExternalFilesDir(null), "crash_log.txt")
                    extLog.writeText(trace)
                } catch (e: Exception) {}
                try {
                    val dlLog = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "ripple_crash_log.txt")
                    dlLog.writeText(trace)
                } catch (e: Exception) {}

                val intent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra("CRASH_LOG", crashLog.absolutePath)
                }
                startActivity(intent)
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(10)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        try {
            registerReceiver(screenOffReceiver, android.content.IntentFilter(android.content.Intent.ACTION_SCREEN_OFF))
        } catch (_: Exception) {}
        if (savedInstanceState == null) {
            viewModel.onAppLaunch()
        }
        enableEdgeToEdge()
        val hasAccess = hasStorageAccess()
        lastHadStorageAccess = hasAccess
        viewModel.updateStorageAccessState(hasAccess)
        if (hasAccess) {
            viewModel.scanDeviceForCleaner(showLoading = false)
        }

        val rootFile = Environment.getExternalStorageDirectory()
        val rootListingCount = try {
            rootFile.listFiles()?.size ?: -1
        } catch (e: Exception) {
            -2
        }
        android.util.Log.i(
            "RippleStorage",
            "Startup check: Android SDK=${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE}), Model=${Build.MANUFACTURER} ${Build.MODEL}, hasStorageAccess=$hasAccess, rootPath=${rootFile.absolutePath}, rootListFilesCount=$rootListingCount"
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        val splashStart = android.os.SystemClock.elapsedRealtime()
        splashScreen.setKeepOnScreenCondition {
            viewModel.state.value.isLoading && (android.os.SystemClock.elapsedRealtime() - splashStart < 1200L)
        }
        
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(applicationContext)
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "PDFBoxResourceLoader init failed", e)
            }
        }

        try {
            coil.Coil.setImageLoader {
                coil.ImageLoader.Builder(applicationContext)
                    .components {
                        add(coil.decode.VideoFrameDecoder.Factory())
                        add(com.ripple.filemanager.ui.DocumentThumbnailFetcher.Factory())
                        add(com.ripple.filemanager.ui.DocumentThumbnailFetcher.UriFactory())
                    }
                    .crossfade(true)
                    .build()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Coil image loader init failed", e)
        }
        
        try {
            rikka.shizuku.Shizuku.addRequestPermissionResultListener { requestCode: Int, grantResult: Int ->
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    viewModel.reload()
                }
            }
            rikka.shizuku.Shizuku.addBinderReceivedListenerSticky {
                viewModel.refreshShizukuStatus()
            }
            rikka.shizuku.Shizuku.addBinderDeadListener {
                viewModel.refreshShizukuStatus()
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
                        is AppAction.SwitchToStorage -> viewModel.switchToStorage(action.storageId, action.defaultLocation)
                        is AppAction.NavTabTapped -> viewModel.navTabTapped(action.id)
                        is AppAction.NavigateBackInDrive -> viewModel.navigateBackInDrive()
                        is AppAction.SetQuery -> viewModel.setQuery(action.query)
                        is AppAction.SetFilter -> viewModel.setFilter(action.filter)
                        is AppAction.ToggleViewMode -> viewModel.toggleViewMode()
                        is AppAction.SetSortMode -> viewModel.setSortMode(action.mode)
                        is AppAction.SetTrashScreenVisible -> viewModel.setTrashScreenVisible(action.visible)
                        is AppAction.SetCleanerScreenVisible -> viewModel.setCleanerScreenVisible(action.visible)
                        is AppAction.SetShowSettingsScreen -> viewModel.setShowSettingsScreen(action.show)
                        is AppAction.SetShowVaultScreen -> viewModel.setShowVaultScreen(action.show)
                        is AppAction.SetVaultAlwaysShown -> viewModel.setVaultAlwaysShown(action.alwaysShown)
                        is AppAction.SetVaultTemporarilyUnhidden -> viewModel.setVaultTemporarilyUnhidden(action.unhidden)
                        is AppAction.ClearRecoverableAuthIntent -> viewModel.clearRecoverableAuthIntent()
                        is AppAction.SetShowThemeSheet -> viewModel.setShowThemeSheet(action.show)
                        is AppAction.SetShowBatchRenameDialog -> viewModel.setShowBatchRenameDialog(action.show)
                        is AppAction.ClearSelection -> viewModel.clearSelection()
                        is AppAction.ToggleSelection -> viewModel.toggleSelection(action.id)
                        is AppAction.SelectAll -> viewModel.selectAll(action.ids)
                        is AppAction.SelectNone -> viewModel.selectNone()
                        is AppAction.OpenFileViewer -> viewModel.openFileViewer(action.id)
                        is AppAction.ViewFile -> viewModel.viewFile(action.file)
                        is AppAction.CloseFileViewer -> viewModel.closeFileViewer()
                        is AppAction.ClearUnlockedFileToOpen -> viewModel.clearUnlockedFileToOpen()
                        is AppAction.CreateFolder -> viewModel.createFolder(action.name)
                        is AppAction.CreateFile -> viewModel.createFile(action.name)
                        is AppAction.TogglePin -> viewModel.togglePin(action.path)
                        is AppAction.RenameFile -> viewModel.renameFile(action.path, action.newName)
                        is AppAction.BatchRenameFiles -> viewModel.batchRenameFiles(action.baseName, action.extension, action.padding, action.startNumber, action.isPrefix, action.style)
                        is AppAction.DeleteSelectedFiles -> viewModel.deleteSelectedFiles()
                        is AppAction.LogRecentAction -> viewModel.logRecentAction(action.path, action.action)
                        is AppAction.SetClipboard -> viewModel.setClipboard(action.action)
                        is AppAction.ClearClipboard -> viewModel.clearClipboard()
                        is AppAction.PasteClipboard -> viewModel.pasteClipboard(action.location)
                        is AppAction.TogglePastePause -> viewModel.togglePastePause()
                        is AppAction.CancelPaste -> viewModel.cancelPaste()
                        is AppAction.ExtractZip -> viewModel.extractZip(action.sourcePath, action.destPath)
                        is AppAction.ToggleExtractPause -> viewModel.toggleExtractPause()
                        is AppAction.CancelExtract -> viewModel.cancelExtract()
                        is AppAction.ClearExtractResult -> viewModel.clearExtractResult()
                        is AppAction.SetGoogleDriveAuthStatus -> viewModel.updateGoogleDriveAuthStatus(action.isAuthenticated, action.email)
                        is AppAction.SetDrivePickedIds -> viewModel.setDrivePickedIds(action.ids)
                        is AppAction.SetMegaAuthStatus -> viewModel.setMegaAuthStatus(action.isAuthenticated, action.email, action.password)
                        is AppAction.SetShowMegaPopup -> viewModel.setShowMegaPopup(action.show)
                        is AppAction.SetDropboxAuthStatus -> viewModel.setDropboxAuthStatus(action.isAuthenticated, action.email)
                        is AppAction.RequestStorageAccess -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                openStorageSettings()
                            } else {
                                requestPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_EXTERNAL_STORAGE,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    )
                                )
                            }
                        }
                        is AppAction.AutoRequestAccess -> viewModel.autoRequestAccess(action.path)
                        is AppAction.RequestShizukuAccess -> {
                            try {
                                if (rikka.shizuku.Shizuku.pingBinder()) {
                                    if (rikka.shizuku.Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                        rikka.shizuku.Shizuku.requestPermission(100)
                                    } else {
                                        viewModel.reload()
                                    }
                                } else {
                                    val launchIntent = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                                    if (launchIntent != null) {
                                        startActivity(launchIntent)
                                    } else {
                                        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=moe.shizuku.privileged.api")).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        try {
                                            startActivity(marketIntent)
                                        } catch (e2: Exception) {
                                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api")).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            })
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                viewModel.showToast("Shizuku not detected or not running")
                            }
                        }
                        is AppAction.ShowToast -> viewModel.showToast(action.message)
                        is AppAction.SetShowHiddenFiles -> viewModel.setShowHiddenFiles(action.show)
                        is AppAction.SetHasSeenHideWarning -> viewModel.setHasSeenHideWarning(action.seen)
                        is AppAction.ToggleHideFiles -> viewModel.toggleHideFiles(action.files)
                        is AppAction.ResolveHideConflict -> viewModel.resolveHideConflict(action.resolution)
                        is AppAction.DismissHideConflict -> viewModel.dismissHideConflict()
                        is AppAction.NearbyShareAction -> viewModel.handleNearbyShareAction(action)
                        is AppAction.SmbAction -> viewModel.handleSmbAction(action)
                        is AppAction.FtpAction -> viewModel.handleFtpAction(action)
                        is AppAction.SftpAction -> viewModel.handleSftpAction(action)
                        is AppAction.WebDavAction -> viewModel.handleWebDavAction(action)
                        is AppAction.Reload -> viewModel.reload()
                        is AppAction.SelectNavTab -> viewModel.selectNavTab(action.tab)
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
                        is AppAction.SetDarkSurfaceStyle -> viewModel.setDarkSurfaceStyle(action.style)
                        is AppAction.SetLanguage -> {
                            viewModel.setLanguage(action.tag)
                            recreate()
                        }
                        is AppAction.SetThemeHue -> viewModel.setThemeHue(action.hue)
                        is AppAction.SetThemeLightnessOffset -> viewModel.setThemeLightnessOffset(action.offset)
                        is AppAction.SetDynamicSystemTheme -> viewModel.setDynamicSystemTheme(action.dynamic)
                        is AppAction.SetIconShape -> viewModel.setIconShape(action.shape)
                        is AppAction.SetFontStyle -> viewModel.setFontStyle(action.style)
                        is AppAction.ToggleTextDecoration -> viewModel.toggleTextDecoration(action.decoration)
                        is AppAction.SetMainTextScale -> viewModel.setMainTextScale(action.scale)
                        is AppAction.SetSubTextScale -> viewModel.setSubTextScale(action.scale)
                        is AppAction.SetInvertText -> viewModel.setInvertText(action.invert)
                        is AppAction.SetCornerRoundness -> viewModel.setCornerRoundness(action.roundness)
                        is AppAction.SetGridColumns -> viewModel.setGridColumns(action.columns)
                        is AppAction.SetListMode -> viewModel.setListMode(action.isList)
                        is AppAction.SetCustomAppName -> viewModel.setCustomAppName(action.name)
                        is AppAction.SetLauncherPreset -> viewModel.setLauncherPreset(action.preset)
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
                        is AppAction.SetMiniPlayerAutoCloseSeconds -> viewModel.setMiniPlayerAutoCloseSeconds(action.seconds)
                        is AppAction.SilentInstallApk -> viewModel.silentInstallApk(action.path, action.downgrade, action.forceUninstall)
            is AppAction.BatchInstallApks -> viewModel.batchInstallApks(action.paths, action.downgrade, action.silent, action.forceUninstall)
            is AppAction.SetOrganiserPath -> viewModel.setOrganiserPath(action.category, action.path)
                        is AppAction.OrganiseDownloads -> viewModel.organiseDownloads()
                        is AppAction.ConfirmOrganiseDownloads -> viewModel.confirmOrganiseDownloads()
                        is AppAction.CancelOrganiseDownloads -> viewModel.cancelOrganiseDownloads()
                        is AppAction.SetViewerPreference -> viewModel.setViewerPreference(action.category, action.preference)
                        is AppAction.RequestAuth -> viewModel.requestAuth(action.reason, action.path, action.fileId, action.folderName)
                        is AppAction.CancelAuth -> viewModel.cancelAuth()
                        is AppAction.AuthSuccess -> viewModel.authSuccess(action.reason, action.path, action.fileId, action.folderName, action.password)
                        is AppAction.UpdateGlobalPassword -> viewModel.updateGlobalPassword(action.oldPassword, action.newPassword)
                        is AppAction.SetBiometricEnabled -> viewModel.setBiometricEnabled(action.enabled)
                        is AppAction.ToggleHapticsMaster -> viewModel.toggleHapticsMaster(action.enabled)
                        is AppAction.ToggleHapticsOption -> viewModel.toggleHapticsOption(action.key, action.enabled)
                        is AppAction.CycleDualPaneMode -> viewModel.cycleDualPaneMode()
                        is AppAction.SetActivePane -> viewModel.setActivePane(action.side)
                        is AppAction.SetLocationForPane -> viewModel.setLocationForPane(action.path, action.folderName)
                        is AppAction.NavigateBackInPane -> viewModel.navigateBackInPane(action.side)
                        is AppAction.TransferFileBetweenPanes -> {
                            val current = viewModel.state.value
                            val sourceLocation = if (action.sourceSide == PaneSide.LEFT) current.location else current.secondPaneState.location
                            val destLocation = if (action.destinationSide == PaneSide.LEFT) current.location else current.secondPaneState.location
                            viewModel.transferFileBetweenPanes(action.file, sourceLocation, destLocation, action.mode, action.conflictResolution) { outcome ->
                                val message = when (outcome) {
                                    is MainViewModel.TransferOutcome.Success -> "${if (action.mode == TransferMode.MOVE) "Moved" else "Copied"} ${action.file.name}"
                                    is MainViewModel.TransferOutcome.Failed -> "Transfer failed: ${outcome.message}"
                                    is MainViewModel.TransferOutcome.UnsupportedFolderTransfer -> "Copying folders between different storage types isn't supported yet"
                                }
                                viewModel.showToast(message)
                            }
                        }
                        is AppAction.SaveIncomingShare -> viewModel.saveIncomingShare(action.destinationPath)
                        is AppAction.DismissIncomingShare -> viewModel.dismissIncomingShare()
                        is AppAction.UpdateIncomingShareDestination -> viewModel.updateIncomingShareDestination(action.destinationPath)
                    }
                }
            }

            androidx.compose.runtime.CompositionLocalProvider(
                com.ripple.filemanager.ui.LocalAudioPosition provides viewModel.audioPosition
            ) {
                SiftApp(
                    state = state,
                    onActionOrig = onAction,
                    snackbarHostState = snackbarHostState,
                    windowWidthSizeClass = windowSizeClass.widthSizeClass
                )
            }
        }

        if (savedInstanceState == null) {
            handleIncomingIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return
        when (action) {
            Intent.ACTION_SEND -> {
                val streamUri = androidx.core.content.IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                    ?: (if (intent.clipData != null && intent.clipData!!.itemCount > 0) intent.clipData!!.getItemAt(0).uri else null)
                    ?: intent.data
                if (streamUri != null) {
                    viewModel.handleIncomingShare(listOf(streamUri), intent.type)
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = androidx.core.content.IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                    ?: run {
                        val clip = intent.clipData
                        if (clip != null && clip.itemCount > 0) {
                            val list = ArrayList<Uri>()
                            for (i in 0 until clip.itemCount) {
                                clip.getItemAt(i).uri?.let { list.add(it) }
                            }
                            list
                        } else null
                    }
                if (!uris.isNullOrEmpty()) {
                    viewModel.handleIncomingShare(uris, intent.type)
                }
            }
            Intent.ACTION_VIEW -> {
                val uri = intent.data
                if (uri != null) {
                    viewModel.handleOpenFileFromIntent(uri, intent.type)
                }
            }
        }
    }

    fun hasStorageAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onResume() {
        super.onResume()
        val hasAccessNow = hasStorageAccess()
        viewModel.updateStorageAccessState(hasAccessNow)
        if (hasAccessNow && lastHadStorageAccess == false) {
            viewModel.scanDeviceForCleaner(showLoading = false)
            viewModel.reload()
        } else if (!isFirstResume) {
            viewModel.reload()
        }
        lastHadStorageAccess = hasAccessNow
        isFirstResume = false
        checkPermissions()
    }

    private fun checkPermissions() {
        val accessGranted = hasStorageAccess()
        viewModel.updateStorageAccessState(accessGranted)
        if (accessGranted) {
            permissionDialog?.dismiss()
            if (lastHadStorageAccess == false) {
                viewModel.scanDeviceForCleaner(showLoading = false)
                viewModel.reload()
            }
            lastHadStorageAccess = true
        } else {
            lastHadStorageAccess = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!hasUserDismissedPermissionDialog && (permissionDialog == null || !permissionDialog!!.isShowing)) {
                    showPermissionDialog()
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

    private fun showPermissionDialog() {
        if (isFinishing || isDestroyed) return
        if (permissionDialog?.isShowing == true) return

        val builder = android.app.AlertDialog.Builder(this)
            .setTitle(R.string.permission_dialog_title)
            .setMessage(R.string.permission_dialog_message)
            .setPositiveButton(R.string.permission_open_settings) { dialog, _ ->
                dialog.dismiss()
                openStorageSettings()
            }
            .setNegativeButton(R.string.permission_not_now) { dialog, _ ->
                hasUserDismissedPermissionDialog = true
                dialog.dismiss()
            }
            .setOnCancelListener {
                hasUserDismissedPermissionDialog = true
            }

        permissionDialog = builder.create().apply {
            setCanceledOnTouchOutside(true)
            show()
        }
    }

    private fun openStorageSettings() {
        // a) Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName"))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
                return
            } catch (e: Exception) {
                android.util.Log.w("RippleStorage", "Failed to open ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION", e)
            }

            // b) Intent(ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            try {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
                return
            } catch (e: Exception) {
                android.util.Log.w("RippleStorage", "Failed to open ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION", e)
            }
        }

        // c) Intent(ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        try {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return
        } catch (e: Exception) {
            android.util.Log.w("RippleStorage", "Failed to open ACTION_APPLICATION_DETAILS_SETTINGS", e)
        }

        // d) If all fail, Toast: "Open Settings > Apps > Ripple Files > Permissions and allow Files access"
        Toast.makeText(
            this,
            R.string.permission_settings_fallback_toast,
            Toast.LENGTH_LONG
        ).show()
    }

    private val screenOffReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            if (intent?.action == android.content.Intent.ACTION_SCREEN_OFF) {
                viewModel.onPhoneLocked()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        permissionDialog?.dismiss()
        permissionDialog = null
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (_: Exception) {}
    }
}
