import re

file_path = "app/src/main/java/com/ripple/filemanager/ui/SiftApp.kt"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Update Signatures
content = content.replace("fun SiftApp(\n    windowWidthSizeClass: WindowWidthSizeClass,\n    viewModel: MainViewModel = viewModel()\n)",
                          "fun SiftApp(\n    state: AppState,\n    onAction: (AppAction) -> Unit,\n    windowWidthSizeClass: WindowWidthSizeClass\n)")

content = content.replace("fun MainContent(viewModel: MainViewModel, modifier: Modifier = Modifier)",
                          "fun MainContent(state: AppState, onAction: (AppAction) -> Unit, modifier: Modifier = Modifier)")

# Remove val state by viewModel.state.collectAsState()
content = re.sub(r'val state by viewModel\.state\.collectAsState\(\)\s*', '', content)

# 2. Replace viewModel calls with onAction calls
replacements = {
    "viewModel.clearSelection()": "onAction(AppAction.ClearSelection)",
    "viewModel.clearClipboard()": "onAction(AppAction.ClearClipboard)",
    "viewModel.togglePastePause()": "onAction(AppAction.TogglePastePause)",
    "viewModel.cancelPaste()": "onAction(AppAction.CancelPaste)",
    "viewModel.pasteClipboard(state.location)": "onAction(AppAction.PasteClipboard(state.location))",
    "viewModel.createFolder(folderName)": "onAction(AppAction.CreateFolder(folderName))",
    "viewModel.createFile(fileName)": "onAction(AppAction.CreateFile(fileName))",
    "viewModel.setTrashScreenVisible(false)": "onAction(AppAction.SetTrashScreenVisible(false))",
    "viewModel.setTrashScreenVisible(true)": "onAction(AppAction.SetTrashScreenVisible(true))",
    "viewModel.setShowSettingsScreen(false)": "onAction(AppAction.SetShowSettingsScreen(false))",
    "viewModel.setShowSettingsScreen(true)": "onAction(AppAction.SetShowSettingsScreen(true))",
    "viewModel.setShowThemeSheet(false)": "onAction(AppAction.SetShowThemeSheet(false))",
    "viewModel.setShowThemeSheet(true)": "onAction(AppAction.SetShowThemeSheet(true))",
    "viewModel.setShowBatchRenameDialog(false)": "onAction(AppAction.SetShowBatchRenameDialog(false))",
    "viewModel.setShowBatchRenameDialog(true)": "onAction(AppAction.SetShowBatchRenameDialog(true))",
    "viewModel.batchRenameFiles(base, ext, pad, start, isPrefix, style)": "onAction(AppAction.BatchRenameFiles(base, ext, pad, start, isPrefix, style))",
    "viewModel.closeFileViewer()": "onAction(AppAction.CloseFileViewer)",
    "viewModel.setGoogleDriveAuthStatus(true, account.email)": "onAction(AppAction.SetGoogleDriveAuthStatus(true, account.email))",
    "viewModel.reload()": "onAction(AppAction.Reload)",
    "viewModel.setErrorMessage": "onAction(AppAction.SetErrorMessage", # needs fixing for the message
    "viewModel.setLocation(parent)": "onAction(AppAction.SetLocation(parent))",
    "viewModel.setLocation(\"home\")": "onAction(AppAction.SetLocation(\"home\"))",
    "viewModel.setLocation(\"drive\")": "onAction(AppAction.SetLocation(\"drive\"))",
    "viewModel.setLocation(rootPath)": "onAction(AppAction.SetLocation(rootPath))",
    "viewModel.setLocation(pathForClick)": "onAction(AppAction.SetLocation(pathForClick))",
    "viewModel.setLocation(file.path, file.name)": "onAction(AppAction.SetLocation(file.path, file.name))",
    "viewModel.setLocation(path)": "onAction(AppAction.SetLocation(path))",
    "viewModel.setQuery(\"\")": "onAction(AppAction.SetQuery(\"\"))",
    "viewModel.setCleanerScreenVisible(true)": "onAction(AppAction.SetCleanerScreenVisible(true))",
    "viewModel.setFilter(\"all\")": "onAction(AppAction.SetFilter(\"all\"))",
    "viewModel.setFilter(id)": "onAction(AppAction.SetFilter(id))",
    "viewModel.autoRequestAccess(state.location)": "onAction(AppAction.AutoRequestAccess(state.location))",
    "viewModel.requestShizukuAccess()": "onAction(AppAction.RequestShizukuAccess)",
    "viewModel.toggleViewMode()": "onAction(AppAction.ToggleViewMode)",
    "viewModel.toggleSelection(file.id)": "onAction(AppAction.ToggleSelection(file.id))",
    "viewModel.openFileViewer(file.id)": "onAction(AppAction.OpenFileViewer(file.id))",
    "viewModel.togglePin(file.path)": "onAction(AppAction.TogglePin(file.path))",
    "viewModel.renameFile(file, newName)": "onAction(AppAction.RenameFile(file.path, newName))",
    "viewModel.selectAll()": "onAction(AppAction.SelectAll)",
    "viewModel.selectNone()": "onAction(AppAction.SelectNone)",
    "viewModel.setClipboard(\"copy\")": "onAction(AppAction.SetClipboard(\"copy\"))",
    "viewModel.setClipboard(\"cut\")": "onAction(AppAction.SetClipboard(\"cut\"))",
    "viewModel.deleteSelectedFiles()": "onAction(AppAction.DeleteSelectedFiles)",
    "viewModel.extractZip(extractTargetFile!!, path)": "onAction(AppAction.ExtractZip(extractTargetFile!!.path, path))",
    "viewModel.toggleExtractPause()": "onAction(AppAction.ToggleExtractPause)",
    "viewModel.cancelExtract()": "onAction(AppAction.CancelExtract)",
    "viewModel.clearExtractResult()": "onAction(AppAction.ClearExtractResult)",
    "TrashScreen(viewModel = viewModel": "TrashScreen(state = state, onAction = onAction",
}

for old, new in replacements.items():
    content = content.replace(old, new)

# Special regex replacements
content = re.sub(r'viewModel\.setErrorMessage\("([^"]+)"\)', r'onAction(AppAction.SetErrorMessage("\1"))', content)
content = re.sub(r'viewModel\.setErrorMessage\(([^)]+)\)', r'onAction(AppAction.SetErrorMessage(\1))', content)
content = re.sub(r'fileDetails = viewModel\.getFileDetails\(([^)]+)\)', r'onAction(AppAction.LoadFileDetails(\1) { fileDetails = it })', content)
content = re.sub(r'viewModel\.snackbarMessage', r'// viewModel.snackbarMessage removed, moved to MainActivity', content)

# Clean up imports
content = content.replace("import com.ripple.filemanager.MainViewModel", "import com.ripple.filemanager.AppAction\nimport com.ripple.filemanager.AppState\nimport com.ripple.filemanager.FileDetails\nimport com.ripple.filemanager.FileItem\nimport com.ripple.filemanager.ThemeMode")

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)
print("Updated SiftApp.kt")
