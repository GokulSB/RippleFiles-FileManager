import re

with open('app/src/main/java/com/ripple/filemanager/MainActivity.kt', 'r', encoding='utf-8') as f:
    c = f.read()

# Add necessary imports
imports = """import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import com.ripple.filemanager.AppAction
"""
c = c.replace('import com.ripple.filemanager.ui.SiftApp', imports + 'import com.ripple.filemanager.ui.SiftApp')

# Replace the setContent block
old_set_content = """        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            SiftApp(
                windowWidthSizeClass = windowSizeClass.widthSizeClass,
                viewModel = viewModel
            )
        }"""

new_set_content = """        setContent {
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
                        is AppAction.SetQuery -> viewModel.setQuery(action.query)
                        is AppAction.SetFilter -> viewModel.setFilter(action.filter)
                        is AppAction.ToggleViewMode -> viewModel.toggleViewMode()
                        is AppAction.SetTrashScreenVisible -> viewModel.setTrashScreenVisible(action.visible)
                        is AppAction.SetCleanerScreenVisible -> viewModel.setCleanerScreenVisible(action.visible)
                        is AppAction.SetShowSettingsScreen -> viewModel.setShowSettingsScreen(action.show)
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
                        is AppAction.AutoRequestAccess -> viewModel.autoRequestAccess(action.path)
                        is AppAction.RequestShizukuAccess -> { /* Shizuku access handled internally by rikka */ }
                        is AppAction.Reload -> viewModel.reload()
                        is AppAction.SetErrorMessage -> viewModel.setErrorMessage(action.message)
                        is AppAction.LoadFileDetails -> {
                            val details = viewModel.getFileDetails(action.path)
                            action.onLoaded(details)
                        }
                        is AppAction.RefreshTrash -> viewModel.refreshTrash()
                        is AppAction.RestoreTrashFiles -> viewModel.restoreTrashFiles(action.files)
                        is AppAction.PermanentlyDeleteTrashFiles -> viewModel.permanentlyDeleteTrashFiles(action.files)
                        is AppAction.SetRecycleBinSettings -> viewModel.setRecycleBinSettings(action.enabled, action.retentionValue, action.retentionUnit)
                        is AppAction.SetThemeMode -> viewModel.setThemeMode(action.mode)
                        is AppAction.SetThemeHue -> viewModel.setThemeHue(action.hue)
                        is AppAction.SetDynamicSystemTheme -> viewModel.setDynamicSystemTheme(action.dynamic)
                        is AppAction.SetIconShape -> viewModel.setIconShape(action.shape)
                        is AppAction.SetFontStyle -> viewModel.setFontStyle(action.style)
                        is AppAction.ToggleTextDecoration -> viewModel.toggleTextDecoration()
                        is AppAction.SetMainTextScale -> viewModel.setMainTextScale(action.scale)
                        is AppAction.SetSubTextScale -> viewModel.setSubTextScale(action.scale)
                        is AppAction.SetCornerRoundness -> viewModel.setCornerRoundness(action.roundness)
                        is AppAction.SetCleanerCategory -> viewModel.setCleanerCategory(action.category)
                        is AppAction.SelectAllCleanerFiles -> viewModel.selectAllCleanerFiles(action.ids)
                        is AppAction.ClearCleanerSelection -> viewModel.clearCleanerSelection()
                        is AppAction.DeleteSelectedCleanerFiles -> viewModel.deleteSelectedCleanerFiles()
                        is AppAction.ToggleCleanerSelection -> viewModel.toggleCleanerSelection(action.id)
                    }
                }
            }

            SiftApp(
                state = state,
                onAction = onAction,
                snackbarHostState = snackbarHostState,
                windowWidthSizeClass = windowSizeClass.widthSizeClass
            )
        }"""

c = c.replace(old_set_content, new_set_content)

with open('app/src/main/java/com/ripple/filemanager/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(c)
