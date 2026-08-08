package com.ripple.filemanager

enum class AuthReason { OPEN_FILE, LOCK_FILE, UNLOCK_FILE }

sealed class AppAction {
    // Navigation & View State
    data class SetLocation(val path: String, val folderName: String? = null) : AppAction()
    data class NavTabTapped(val id: String) : AppAction()
    object NavigateBackInDrive : AppAction()
    data class SetQuery(val query: String) : AppAction()
    data class SetFilter(val filter: String) : AppAction()
    object ToggleViewMode : AppAction()
    data class SetTrashScreenVisible(val visible: Boolean) : AppAction()
    data class SetCleanerScreenVisible(val visible: Boolean) : AppAction()
    data class SetShowSettingsScreen(val show: Boolean) : AppAction()
    object ClearRecoverableAuthIntent : AppAction()
    data class SetShowThemeSheet(val show: Boolean) : AppAction()
    data class SetShowBatchRenameDialog(val show: Boolean) : AppAction()
    data class SetSortMode(val mode: SortMode) : AppAction()
    
    // File Selection
    object ClearSelection : AppAction()
    data class ToggleSelection(val id: Int) : AppAction()
    object SelectAll : AppAction()
    object SelectNone : AppAction()
    
    // File Operations
    data class OpenFileViewer(val id: Int) : AppAction()
    object CloseFileViewer : AppAction()
    data class CreateFolder(val name: String) : AppAction()
    data class CreateFile(val name: String) : AppAction()
    data class TogglePin(val path: String) : AppAction()
    data class RequestAuth(val reason: AuthReason, val path: String, val fileId: Int? = null, val folderName: String? = null) : AppAction()
    data class AuthSuccess(val reason: AuthReason, val path: String, val fileId: Int? = null, val folderName: String? = null, val password: String? = null) : AppAction()
    object CancelAuth : AppAction()
    data class UpdateGlobalPassword(val oldPassword: String, val newPassword: String) : AppAction()
    data class SetBiometricEnabled(val enabled: Boolean) : AppAction()
    data class RenameFile(val path: String, val newName: String) : AppAction()
    data class BatchRenameFiles(
        val baseName: String, 
        val extension: String, 
        val padding: Int, 
        val startNumber: Int, 
        val isPrefix: Boolean, 
        val style: String
    ) : AppAction()
    object DeleteSelectedFiles : AppAction()
    
    // Clipboard Operations
    data class SetClipboard(val action: String) : AppAction()
    object ClearClipboard : AppAction()
    data class PasteClipboard(val location: String) : AppAction()
    object TogglePastePause : AppAction()
    object CancelPaste : AppAction()
    
    // Extraction Operations
    data class ExtractZip(val sourcePath: String, val destPath: String) : AppAction()
    object ToggleExtractPause : AppAction()
    object CancelExtract : AppAction()
    object ClearExtractResult : AppAction()
    
    // Auth & Permissions
    data class SetGoogleDriveAuthStatus(val isAuthenticated: Boolean, val email: String?) : AppAction()
    data class SetMegaAuthStatus(val isAuthenticated: Boolean, val email: String?, val password: String? = null) : AppAction()
    data class SetShowMegaPopup(val show: Boolean) : AppAction()
    data class SetDropboxAuthStatus(val isAuthenticated: Boolean, val email: String?) : AppAction()
    data class AutoRequestAccess(val path: String) : AppAction()
    object RequestShizukuAccess : AppAction()
    
    // Music Player Actions
    data class PlayAudio(val file: FileItem) : AppAction()
    object ToggleAudioPlayback : AppAction()
    data class SeekAudio(val position: Long) : AppAction()
    object StopAudio : AppAction()
    object PlayNextAudio : AppAction()
    object PlayPreviousAudio : AppAction()
    data class SetShowFullScreenPlayer(val show: Boolean) : AppAction()
    
    // Theme & Customization
    data class SetThemeMode(val mode: ThemeMode) : AppAction()
    data class SetThemeHue(val hue: Float) : AppAction()
    data class SetDynamicSystemTheme(val dynamic: Boolean) : AppAction()
    data class SetIconShape(val shape: com.ripple.filemanager.IconShapeType) : AppAction()
    data class SetFontStyle(val style: String) : AppAction()
    data class ToggleTextDecoration(val decoration: String) : AppAction()
    data class SetMainTextScale(val scale: Float) : AppAction()
    data class SetSubTextScale(val scale: Float) : AppAction()
    data class SetCornerRoundness(val roundness: Float) : AppAction()
    data class SetGridColumns(val columns: Int) : AppAction()

    
    // Cleaner Screen
    data class SetCleanerCategory(val category: String?) : AppAction()
    data class SelectAllCleanerFiles(val ids: List<Int>) : AppAction()
    object ClearCleanerSelection : AppAction()
    object DeleteSelectedCleanerFiles : AppAction()
    data class ToggleCleanerSelection(val id: Int) : AppAction()

    // General
    object Reload : AppAction()
    data class SetErrorMessage(val message: String) : AppAction()
    data class LoadFileDetails(val path: String, val onLoaded: (FileDetails) -> Unit) : AppAction()
    
    // Trash Screen Operations
    object RefreshTrash : AppAction()
    data class RestoreTrashFiles(val files: List<String>) : AppAction()
    data class PermanentlyDeleteTrashFiles(val files: List<String>) : AppAction()
    data class SetRecycleBinSettings(val enabled: Boolean, val retentionValue: Int, val retentionUnit: String) : AppAction()
    
    // File Organiser
    data class SetOrganiserPath(val category: String, val path: String) : AppAction()
    object OrganiseDownloads : AppAction()
    
    // Viewers
    data class SetViewerPreference(val category: String, val preference: String) : AppAction()
}
