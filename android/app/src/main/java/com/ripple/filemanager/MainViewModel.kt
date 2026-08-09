package com.ripple.filemanager

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import android.os.Environment
import android.os.StatFs
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.collections.immutable.ImmutableMap
import com.ripple.filemanager.data.smb.SmbConnection

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class IconShapeType { 
    SYSTEM, 
    CIRCLE, 
    SQUARE, 
    SQUIRCLE, 
    DIAMOND, 
    HEXAGON, 
    ARCH,
    COOKIE,
    STAR,
    FLOWER
}

enum class ConnectionStatus { Idle, Connecting, Connected, Error }

@androidx.compose.runtime.Immutable
data class SmbState(
    val savedConnections: ImmutableList<SmbConnection> = persistentListOf(),
    val activeConnectionId: String? = null,
    val currentPath: String = "",
    val connectionStatus: ConnectionStatus = ConnectionStatus.Idle,
    val error: SmbError? = null
)

enum class NavTab { HOME, RECENT, PINNED, CLOUD }

@androidx.compose.runtime.Immutable
data class NavBarState(val selected: NavTab = NavTab.HOME)

@androidx.compose.runtime.Immutable
data class AppState(
    val navBarState: NavBarState = NavBarState(),
    val hasShizuku: Boolean = false,
    val smbState: SmbState = SmbState(),
    val location: String = "home",
    val currentFolderName: String? = null,
    val driveFolderStack: List<Pair<String, String>> = emptyList(),
    val filter: String = "all",
    val query: String = "",
    val isListMode: Boolean = false,
    val isSelectionMode: Boolean = false,
    val selectedFiles: ImmutableSet<Int> = persistentSetOf(),
    val sortMode: SortMode = SortMode.ALPHABETICAL,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themeHue: Float = 262f,
    val showThemeSheet: Boolean = false,
    val showSettingsScreen: Boolean = false,
    val useDynamicSystemTheme: Boolean = true,
    val isGoogleDriveAuthenticated: Boolean = false,
    val googleDriveAccountEmail: String? = null,
    val isMegaAuthenticated: Boolean = false,
    val megaAccountEmail: String? = null,
    val megaLoginError: String? = null,
    val showMegaPopup: Boolean = false,
    val isDropboxAuthenticated: Boolean = false,
    val dropboxAccountEmail: String? = null,
    val files: ImmutableList<FileItem> = persistentListOf(),
    val isLoading: Boolean = false,
    val viewingFile: FileItem? = null,
    val storageFreeGb: Float = 0f,
    val storageTotalGb: Float = 0f,
    val sdCardStorageFreeGb: Float = 0f,
    val sdCardStorageTotalGb: Float = 0f,
    val driveStorageFreeBytes: Long = 0L,
    val driveStorageTotalBytes: Long = 0L,
    val clipboardPaths: ImmutableList<String> = persistentListOf(),
    val clipboardAction: String? = null,
    val pasteProgress: Float? = null,
    val isPasteComplete: Boolean = false,
    val isPastePaused: Boolean = false,
    val errorMessage: String? = null,
    val recoverableAuthIntent: android.content.Intent? = null,
    val isDownloading: Boolean = false,
    val showBatchRenameDialog: Boolean = false,
    val iconShapeSetting: IconShapeType = IconShapeType.SYSTEM,
    val activeIconShape: IconShapeType = IconShapeType.SYSTEM,
    val fontStyle: String = "System",
    val textDecorations: ImmutableSet<String> = persistentSetOf(),
    val mainTextScale: Float = 1.0f,
    val subTextScale: Float = 1.0f,
    val gridColumns: Int = 2,
    val cornerRoundness: Float = 0.5f,
    val showCleanerScreen: Boolean = false,
    val authRequestReason: AuthReason? = null,
    val authRequestPath: String? = null,
    val authRequestFileId: Int? = null,
    val authRequestFolderName: String? = null,
    val authErrorMessage: String? = null,
    val securitySettingsErrorMessage: String? = null,
    val showTrashScreen: Boolean = false,
    val trashFiles: ImmutableList<FileItem> = persistentListOf(),
    val trashIsLoading: Boolean = false,
    val cleanerLoading: Boolean = false,
    val cleanerData: CleanerData? = null,
    val currentCleanerCategory: String? = null,
    val cleanerSelectedFiles: ImmutableSet<Int> = persistentSetOf(),
    
    val navTapCounters: kotlinx.collections.immutable.ImmutableMap<String, Int> = kotlinx.collections.immutable.persistentMapOf(),
    
    // Music Player State
    val currentAudioFile: FileItem? = null,
    val isAudioPlaying: Boolean = false,
    val audioPlaybackPosition: Long = 0L,
    val audioDuration: Long = 0L,
    val showFullScreenPlayer: Boolean = false,
    val audioTitle: String = "",
    val audioArtist: String = "",
    val audioArtworkData: ByteArray? = null,

    val isRecycleBinEnabled: Boolean = true,
    val recycleBinRetentionValue: Int = 7,
    val recycleBinRetentionUnit: String = "Days",
    
    // File Organiser
    val orgDestDocs: String = "",
    val orgDestImages: String = "",
    val orgDestApks: String = "",
    val orgDestMusic: String = "",
    val orgDestVideos: String = "",
    val organiseProgress: Float? = null,

    val extractProgress: Float? = null,
    val isExtractPaused: Boolean = false,
    val extractResultPath: String? = null,
    
    // Viewers
    val viewerTextPdf: String = "In-app",
    val viewerMusic: String = "In-app",
    val viewerImage: String = "In-app",
    val pasteLoadingCount: Int? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private val repository = FileRepository(application)
    private var rawFiles: List<FileItem> = emptyList()
    private var loadJob: kotlinx.coroutines.Job? = null

    private val prefs = application.getSharedPreferences("sift_prefs", android.content.Context.MODE_PRIVATE)

    private val megaClient by lazy { nz.mega.sdk.MegaClient(application) }

    private val smbStore by lazy { com.ripple.filemanager.data.smb.SmbStore(application) }
    private val smbProvider = com.ripple.filemanager.data.smb.SmbStorageProvider()

    init {
        val savedTheme = prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM"
        val savedHue = prefs.getFloat("theme_hue", 262f)
        val savedDynamic = prefs.getBoolean("theme_dynamic", true)
        val savedIconShape = prefs.getString("icon_shape", "SYSTEM") ?: "SYSTEM"
        val savedFontStyle = prefs.getString("font_style", "System") ?: "System"
        val savedDecorations = prefs.getStringSet("text_decorations", emptySet()) ?: emptySet()
        val savedMainScale = prefs.getFloat("main_text_scale", 1.0f)
        val savedSubScale = prefs.getFloat("sub_text_scale", 1.0f)
        val savedGridColumns = prefs.getInt("grid_columns", 2)
        val savedCornerRoundness = prefs.getFloat("corner_roundness", 0.5f)
        
        val downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
        val defaultDocs = "$downloadPath/Docs"
        val defaultImages = "$downloadPath/Images"
        val defaultApks = "$downloadPath/Apk"
        val defaultMusic = "$downloadPath/Music"
        val defaultVideos = "$downloadPath/Videos"
        
        val savedDocs = prefs.getString("org_dest_docs", defaultDocs) ?: defaultDocs
        val savedImages = prefs.getString("org_dest_images", defaultImages) ?: defaultImages
        val savedApks = prefs.getString("org_dest_apks", defaultApks) ?: defaultApks
        val savedMusic = prefs.getString("org_dest_music", defaultMusic) ?: defaultMusic
        val savedVideos = prefs.getString("org_dest_videos", defaultVideos) ?: defaultVideos

        _state.update { it.copy(
            hasShizuku = repository.hasShizuku(), 
            isLoading = true,
            themeMode = try { ThemeMode.valueOf(savedTheme) } catch(e: Exception) { ThemeMode.SYSTEM },
            themeHue = savedHue,
            useDynamicSystemTheme = savedDynamic,
            iconShapeSetting = try { IconShapeType.valueOf(savedIconShape) } catch(e: Exception) { IconShapeType.SYSTEM },
            activeIconShape = try { IconShapeType.valueOf(savedIconShape) } catch(e: Exception) { IconShapeType.SYSTEM },
            fontStyle = savedFontStyle,
            textDecorations = savedDecorations.toImmutableSet(),
            mainTextScale = savedMainScale,
            subTextScale = savedSubScale,
            gridColumns = savedGridColumns,
            cornerRoundness = savedCornerRoundness,
            isRecycleBinEnabled = prefs.getBoolean("recycle_bin_enabled", true),
            recycleBinRetentionValue = prefs.getInt("recycle_bin_retention_value", 7),
            recycleBinRetentionUnit = prefs.getString("recycle_bin_retention_unit", "Days") ?: "Days",
            orgDestDocs = savedDocs,
            orgDestImages = savedImages,
            orgDestApks = savedApks,
            orgDestMusic = savedMusic,
            orgDestVideos = savedVideos,
            viewerTextPdf = prefs.getString("viewer_text_pdf", "In-app") ?: "In-app",
            viewerMusic = prefs.getString("viewer_music", "In-app") ?: "In-app",
            viewerImage = prefs.getString("viewer_image", "In-app") ?: "In-app"
        ) }
        
        val savedSmbConnectionId = prefs.getString("active_smb_connection", null)
        val savedConnectionsList = smbStore.getConnections()
        val validSmbConnection = if (savedSmbConnectionId != null && savedConnectionsList.any { it.id == savedSmbConnectionId }) savedSmbConnectionId else null
        
        _state.update { it.copy(smbState = it.smbState.copy(
            savedConnections = savedConnectionsList,
            activeConnectionId = validSmbConnection,
            connectionStatus = if (validSmbConnection != null) ConnectionStatus.Connected else ConnectionStatus.Idle
        )) }

        viewModelScope.launch(Dispatchers.IO) {
            val email = repository.currentGoogleAccountEmail
            if (email != null) {
                _state.update { it.copy(hasShizuku = repository.hasShizuku(), isGoogleDriveAuthenticated = true, googleDriveAccountEmail = email) }
                fetchDriveStorageQuota()
            }
            
            val megaSession = prefs.getString("mega_session", null)
            val megaEmail = prefs.getString("mega_email", null)
            if (megaSession != null && megaEmail != null) {
                try {
                    val success = megaClient.fastLogin(megaSession)
                    if (success) {
                        megaClient.fetchNodes()
                        _state.update { it.copy(hasShizuku = repository.hasShizuku(), isMegaAuthenticated = true, megaAccountEmail = megaEmail) }
                    }
                } catch (e: Exception) {
                    // silently fail session login
                }
            }
            
            updateStorageStats()
        }
        loadFiles("home")
    }

    private fun updateStorageStats() {
        try {
            val stat = StatFs(Environment.getExternalStorageDirectory().path)
            val bytesTotal = stat.totalBytes
            val bytesFree = stat.availableBytes
            val gbTotal = bytesTotal.toFloat() / (1024 * 1024 * 1024)
            val gbFree = bytesFree.toFloat() / (1024 * 1024 * 1024)
            
            var sdGbTotal = 0f
            var sdGbFree = 0f
            val externalDirs = androidx.core.content.ContextCompat.getExternalFilesDirs(getApplication(), null)
            if (externalDirs.size > 1 && externalDirs[1] != null) {
                try {
                    val sdStat = StatFs(externalDirs[1].path)
                    sdGbTotal = sdStat.totalBytes.toFloat() / (1024 * 1024 * 1024)
                    sdGbFree = sdStat.availableBytes.toFloat() / (1024 * 1024 * 1024)
                } catch(e: Exception) {}
            }
            
            _state.update { it.copy(
                hasShizuku = repository.hasShizuku(), 
                storageTotalGb = gbTotal, 
                storageFreeGb = gbFree,
                sdCardStorageTotalGb = sdGbTotal,
                sdCardStorageFreeGb = sdGbFree
            ) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun reload() {
        loadFiles(_state.value.location)
    }

    fun togglePin(path: String) {
        repository.togglePin(path)
        reload()
    }

    suspend fun getFileDetails(path: String): FileDetails {
        return repository.getFileDetails(path)
    }

    private fun loadFiles(location: String) {
        loadJob?.cancel()
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), isLoading = true, location = location, errorMessage = null) }
        loadJob = viewModelScope.launch {
            try {
                val fetched = if (location == "mega") {
                    megaClient.getChildren(null)
                } else if (location.startsWith("mega_id:")) {
                    megaClient.getChildren(location.removePrefix("mega_id:"))
                } else if (location.startsWith("smb_")) {
                    val connectionId = location.substringAfter("smb_").substringBefore(":")
                    var res = smbProvider.listFiles(location, connectionId)
                    if (res.isFailure) {
                        val connection = smbStore.getConnections().find { it.id == connectionId } ?: throw Exception("Connection not found")
                        val password = smbStore.getPassword(connectionId) ?: throw Exception("Password not found")
                        smbProvider.connect(connection, password).getOrThrow()
                        res = smbProvider.listFiles(location, connectionId)
                    }
                    res.getOrThrow()
                } else {
                    repository.getFiles(location)
                }
                rawFiles = fetched
                
                val st = _state.value
                val filtered = applyFiltersAndSort(fetched, st)
                
                _state.update { it.copy(hasShizuku = repository.hasShizuku(), isLoading = false, files = filtered.toImmutableList()) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException) {
                e.printStackTrace()
                rawFiles = emptyList()
                _state.update { it.copy(hasShizuku = repository.hasShizuku(), files = persistentListOf(), isLoading = false, recoverableAuthIntent = e.intent, errorMessage = "Authentication required. Please grant permission.") }
            } catch (e: Exception) {
                e.printStackTrace()
                rawFiles = emptyList()
                _state.update { it.copy(hasShizuku = repository.hasShizuku(), files = persistentListOf(), isLoading = false, errorMessage = e.message ?: e.toString()) }
            }
        }
    }

    fun clearRecoverableAuthIntent() {
        _state.update { it.copy(recoverableAuthIntent = null) }
    }

    fun navTabTapped(id: String) {
        val tapCount = _state.value.navTapCounters[id] ?: 0
        val map = _state.value.navTapCounters as kotlinx.collections.immutable.PersistentMap
        _state.update { it.copy(navTapCounters = map.put(id, tapCount + 1)) }
    }
    
    fun selectNavTab(tab: NavTab) {
        _state.update { it.copy(navBarState = NavBarState(selected = tab)) }
        if (tab == NavTab.CLOUD) {
            val st = _state.value
            if (st.smbState.activeConnectionId != null) {
                setLocation("smb_${st.smbState.activeConnectionId}:/")
            } else if (st.isGoogleDriveAuthenticated) {
                setLocation("drive")
            } else if (st.isMegaAuthenticated) {
                setLocation("mega")
            } else if (st.isDropboxAuthenticated) {
                setLocation("dropbox")
            } else {
                setLocation("cloud")
            }
        } else {
            val targetLocation = when (tab) {
                NavTab.HOME -> "home"
                NavTab.RECENT -> "recent"
                NavTab.PINNED -> "pinned"
                else -> "home"
            }
            setLocation(targetLocation)
        }
    }

    fun setLocation(location: String, folderName: String? = null) {
        val current = _state.value
        val enteringCloudSubfolder = location.startsWith("drive_id:") || location.startsWith("mega_id:") || location.startsWith("dropbox_id:") || location.startsWith("smb_")
        val wasInCloud = current.location == "drive" || current.location == "mega" || current.location == "dropbox" || current.location.startsWith("drive_id:") || current.location.startsWith("mega_id:") || current.location.startsWith("dropbox_id:") || current.location.startsWith("smb_")

        val newStack = when {
            enteringCloudSubfolder && wasInCloud ->
                current.driveFolderStack + (current.location to (current.currentFolderName ?: "Cloud"))
            location == "drive" || location == "mega" || location == "dropbox" || (location.startsWith("smb_") && location.endsWith(":/")) -> emptyList()
            else -> current.driveFolderStack
        }

        _state.update { it.copy(hasShizuku = repository.hasShizuku(), location = location, currentFolderName = folderName, driveFolderStack = newStack, isLoading = true, files = kotlinx.collections.immutable.persistentListOf()) }
        repository.logRecentAction(location, "Explored")
        loadFiles(location)
    }

    fun navigateBackInDrive() {
        val stack = _state.value.driveFolderStack
        if (stack.isNotEmpty()) {
            val (prevLocation, prevName) = stack.last()
            _state.update { it.copy(location = prevLocation, currentFolderName = prevName, driveFolderStack = stack.dropLast(1), isLoading = true, files = kotlinx.collections.immutable.persistentListOf()) }
            loadFiles(prevLocation)
        } else {
            setLocation("home")
        }
    }

    fun setFilter(filter: String) {
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), filter = filter, selectedFiles = persistentSetOf()) }
        updateFilteredFiles()
    }

    fun setSortMode(mode: SortMode) {
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), sortMode = mode) }
        updateFilteredFiles()
    }

    fun setQuery(query: String) {
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), query = query) }
        updateFilteredFiles()
    }

    fun toggleViewMode() {
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), isListMode = !it.isListMode) }
    }

    fun setListMode(isList: Boolean) {
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), isListMode = isList) }
    }


    private fun updateFilteredFiles() {
        viewModelScope.launch(Dispatchers.Default) {
            val st = _state.value
            val filtered = applyFiltersAndSort(rawFiles, st)
            _state.update { it.copy(hasShizuku = repository.hasShizuku(), files = filtered.toImmutableList()) }
        }
    }

    private fun applyFiltersAndSort(raw: List<FileItem>, st: AppState): List<FileItem> {
        return raw.filter {
            val filterMatch = st.filter == "all" || 
                (st.filter == "music" && it.type == "audio") ||
                (st.filter == "apk" && it.type == "apk") ||
                (st.filter == "media" && (it.type == "image" || it.type == "video" || it.type == "audio")) ||
                (st.filter == "doc" && it.kind == "doc")
            val searchMatch = it.name.contains(st.query, ignoreCase = true)
            filterMatch && searchMatch
        }.sortedWith(
            when (st.sortMode) {
                SortMode.ALPHABETICAL -> compareBy<FileItem> { it.type != "folder" }.thenBy { it.name.lowercase() }
                SortMode.DATE -> compareBy<FileItem> { it.type != "folder" }.thenByDescending { it.lastModified }
                SortMode.SIZE -> compareBy<FileItem> { it.type != "folder" }.thenByDescending { it.sizeBytes }
            }
        )
    }

    fun toggleSelection(fileId: Int) {
        val current = _state.value.selectedFiles
        val newSelection = if (current.contains(fileId)) current.minus(fileId).toImmutableSet() else current.plus(fileId).toImmutableSet()
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), selectedFiles = newSelection, isSelectionMode = newSelection.isNotEmpty()) }
    }

    // Trash methods
    fun setTrashScreenVisible(visible: Boolean) {
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), showTrashScreen = visible) }
    }

    // Cleaner methods
    fun setCleanerScreenVisible(visible: Boolean) {
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), showCleanerScreen = visible) }
        if (visible && _state.value.cleanerData == null) {
            scanDeviceForCleaner()
        }
    }

    fun scanDeviceForCleaner() {
        viewModelScope.launch {
            _state.update { it.copy(hasShizuku = repository.hasShizuku(), cleanerLoading = true) }
            val data = repository.scanDeviceForCleaner()
            _state.update { it.copy(hasShizuku = repository.hasShizuku(), cleanerData = data, cleanerLoading = false) }
        }
    }

    fun setCleanerCategory(category: String?) {
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), currentCleanerCategory = category, cleanerSelectedFiles = persistentSetOf()) }
    }

    fun selectAllCleanerFiles(fileIds: List<Int>) {
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), cleanerSelectedFiles = fileIds.toImmutableSet()) }
    }

    fun clearCleanerSelection() {
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), cleanerSelectedFiles = persistentSetOf()) }
    }

    fun toggleCleanerSelection(fileId: Int) {
        val current = _state.value.cleanerSelectedFiles
        val newSelection = if (current.contains(fileId)) current.minus(fileId).toImmutableSet() else current.plus(fileId).toImmutableSet()
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), cleanerSelectedFiles = newSelection) }
    }



    private val _safRequestEvent = kotlinx.coroutines.flow.MutableSharedFlow<String>()
    val safRequestEvent: kotlinx.coroutines.flow.SharedFlow<String> = _safRequestEvent

    fun requestSafAccess(path: String) {
        viewModelScope.launch {
            _safRequestEvent.emit(path)
        }
    }

    fun requestRootAccess() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            com.topjohnwu.superuser.Shell.getShell()
            reload()
        }
    }

    
    fun autoRequestAccess(path: String) {
        try {
            if (rikka.shizuku.Shizuku.pingBinder()) {
                if (rikka.shizuku.Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    rikka.shizuku.Shizuku.requestPermission(0)
                    return
                }
            }
        } catch (e: Exception) {}
    }

    fun requestShizukuAccess() {
        try {
            if (rikka.shizuku.Shizuku.pingBinder()) {
                if (rikka.shizuku.Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    rikka.shizuku.Shizuku.requestPermission(0)
                } else {
                    reload()
                }
            } else {
                throw IllegalStateException("Shizuku binder hasn't been received. Please make sure the service is running.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    fun hasRestrictedAccess(path: String): Boolean {
        if (!path.contains("Android/data") && !path.contains("Android/obb")) return true
        return repository.hasShizuku()
    }

    fun deleteSelectedCleanerFiles() {
        viewModelScope.launch {
            val selected = _state.value.cleanerSelectedFiles
            val currentCategory = _state.value.currentCleanerCategory
            val data = _state.value.cleanerData ?: return@launch
            
            val filesToDelete = when(currentCategory) {
                "Documents" -> data.documents.files.filter { it.id in selected }
                "Images" -> data.images.files.filter { it.id in selected }
                "Videos" -> data.videos.files.filter { it.id in selected }
                "Audio" -> data.audio.files.filter { it.id in selected }
                "Apps" -> data.apps.files.filter { it.id in selected }
                "Empty folders" -> data.emptyFolders.files.filter { it.id in selected }
                "Duplicates" -> data.duplicates.files.filter { it.id in selected }
                else -> emptyList()
            }
            
            val pathsToTrash = filesToDelete.map { it.path }
            if (pathsToTrash.isNotEmpty()) {
                if (currentCategory == "Empty folders") {
                    pathsToTrash.forEach { path ->
                        try {
                            val f = java.io.File(path)
                            if (f.exists() && f.isDirectory) {
                                repository.deleteRestrictedPath(path)
                            }
                        } catch (e: Exception) {}
                    }
                } else {
                    repository.moveToTrash(pathsToTrash)
                }
            }
            
            // Rescan
            scanDeviceForCleaner()
            _state.update { it.copy(hasShizuku = repository.hasShizuku(), cleanerSelectedFiles = persistentSetOf()) }
        }
    }

    fun clearSelection() {
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), selectedFiles = persistentSetOf(), isSelectionMode = false) }
    }

    fun selectAll() {
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), selectedFiles = it.files.map { f -> f.id }.toImmutableSet()) }
    }

    fun selectNone() {
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), selectedFiles = persistentSetOf()) }
    }

    fun deleteSelectedFiles() {
        viewModelScope.launch {
            val selected = _state.value.selectedFiles
            val allFiles = _state.value.files
            val pathsToTrash = mutableListOf<String>()
            val pathsToDeletePermanently = mutableListOf<String>()
            val isTrashEnabled = _state.value.isRecycleBinEnabled
            
            var driveError: String? = null
            
            selected.forEach { id ->
                val fileItem = allFiles.find { it.id == id }
                if (fileItem != null) {
                    if (fileItem.path.startsWith("drive_id:") || _state.value.location == "drive") {
                        val fileId = if (fileItem.path.startsWith("drive_id:")) fileItem.path.removePrefix("drive_id:") else fileItem.id.toString()
                        val err = repository.deleteDriveFile(fileId)
                        if (err != null) {
                            driveError = err
                        }
                    } else {
                        if (isTrashEnabled) {
                            pathsToTrash.add(fileItem.path)
                        } else {
                            pathsToDeletePermanently.add(fileItem.path)
                        }
                    }
                }
            }
            
            if (pathsToTrash.isNotEmpty()) {
                repository.moveToTrash(pathsToTrash)
                val retentionValue = _state.value.recycleBinRetentionValue
                val retentionUnit = _state.value.recycleBinRetentionUnit
                _snackbarMessage.emit("Moved to recycle bin and will delete within $retentionValue $retentionUnit")
            }
            if (pathsToDeletePermanently.isNotEmpty()) {
                // Not in trash, permanent delete
                pathsToDeletePermanently.forEach { path ->
                    val f = java.io.File(path)
                    repository.deleteRestrictedPath(path)
                }
            }
            if (driveError != null) {
                _snackbarMessage.emit("Failed to delete some Google Drive files: $driveError")
            }
            
            _state.update { it.copy(hasShizuku = repository.hasShizuku(), selectedFiles = persistentSetOf(), isSelectionMode = false) }
            loadFiles(_state.value.location)
            updateStorageStats()
        }
    }

    fun setRecycleBinSettings(enabled: Boolean, retentionValue: Int, retentionUnit: String) {
        prefs.edit().apply {
            putBoolean("recycle_bin_enabled", enabled)
            putInt("recycle_bin_retention_value", retentionValue)
            putString("recycle_bin_retention_unit", retentionUnit)
            apply()
        }
        _state.update { it.copy(isRecycleBinEnabled = enabled, recycleBinRetentionValue = retentionValue, recycleBinRetentionUnit = retentionUnit) }
    }

    fun setOrganiserPath(category: String, path: String) {
        val key = when(category) {
            "Docs" -> "org_dest_docs"
            "Images" -> "org_dest_images"
            "Apks" -> "org_dest_apks"
            "Music" -> "org_dest_music"
            "Videos" -> "org_dest_videos"
            else -> return
        }
        prefs.edit().putString(key, path).apply()
        _state.update {
            when(category) {
                "Docs" -> it.copy(orgDestDocs = path)
                "Images" -> it.copy(orgDestImages = path)
                "Apks" -> it.copy(orgDestApks = path)
                "Music" -> it.copy(orgDestMusic = path)
                "Videos" -> it.copy(orgDestVideos = path)
                else -> it
            }
        }
    }

    fun setViewerPreference(category: String, preference: String) {
        val key = when (category) {
            "Text/PDF" -> "viewer_text_pdf"
            "Music" -> "viewer_music"
            "Image" -> "viewer_image"
            else -> return
        }
        prefs.edit().putString(key, preference).apply()
        _state.update {
            when (category) {
                "Text/PDF" -> it.copy(viewerTextPdf = preference)
                "Music" -> it.copy(viewerMusic = preference)
                "Image" -> it.copy(viewerImage = preference)
                else -> it
            }
        }
    }

    fun organiseDownloads() {
        val currentPath = _state.value.location
        if (!currentPath.lowercase(java.util.Locale.getDefault()).endsWith("download")) return

        val filesToOrganise = _state.value.files.filter { !it.isEmptyFolder && it.type != "folder" }
        if (filesToOrganise.isEmpty()) return

        _state.update { it.copy(organiseProgress = 0f) }

        viewModelScope.launch(Dispatchers.IO) {
            val docsDest = _state.value.orgDestDocs
            val imagesDest = _state.value.orgDestImages
            val apksDest = _state.value.orgDestApks
            val musicDest = _state.value.orgDestMusic
            val videosDest = _state.value.orgDestVideos

            val docsFiles = mutableListOf<String>()
            val imagesFiles = mutableListOf<String>()
            val apksFiles = mutableListOf<String>()
            val musicFiles = mutableListOf<String>()
            val videosFiles = mutableListOf<String>()

            for (file in filesToOrganise) {
                val ext = file.name.substringAfterLast('.', "").lowercase(java.util.Locale.getDefault())
                when (ext) {
                    "pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx", "html", "htm" -> docsFiles.add(file.path)
                    "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif", "svg" -> imagesFiles.add(file.path)
                    "apk", "apks", "apkm", "xapk" -> apksFiles.add(file.path)
                    "mp3", "wav", "ogg", "flac", "m4a", "opus", "aac" -> musicFiles.add(file.path)
                    "mp4", "mkv", "avi", "mov", "webm", "wmv", "flv" -> videosFiles.add(file.path)
                }
            }

            val totalOps = docsFiles.size + imagesFiles.size + apksFiles.size + musicFiles.size + videosFiles.size
            if (totalOps == 0) {
                _state.update { it.copy(organiseProgress = null) }
                _snackbarMessage.emit("No matching files found to organise")
                return@launch
            }

            var completedOps = 0

            suspend fun moveGroup(paths: List<String>, dest: String) {
                if (paths.isEmpty()) return
                java.io.File(dest).mkdirs()
                repository.moveFiles(
                    sourcePaths = paths,
                    destDir = dest,
                    onProgress = { /* We can ignore individual progress for simplicity and just increment completedOps */ },
                    checkPause = {}
                )
                completedOps += paths.size
                _state.update { it.copy(organiseProgress = completedOps.toFloat() / totalOps.toFloat()) }
            }

            try {
                moveGroup(docsFiles, docsDest)
                moveGroup(imagesFiles, imagesDest)
                moveGroup(apksFiles, apksDest)
                moveGroup(musicFiles, musicDest)
                moveGroup(videosFiles, videosDest)
                
                _snackbarMessage.emit("Files successfully organised")
                _state.update { it.copy(organiseProgress = null) }
                loadFiles(currentPath)
            } catch (e: Exception) {
                e.printStackTrace()
                _snackbarMessage.emit("Error organising files: ${e.message}")
                _state.update { it.copy(organiseProgress = null) }
                loadFiles(currentPath)
            }
        }
    }

    fun loadTrashFiles() {
        viewModelScope.launch {
            _state.update { it.copy(hasShizuku = repository.hasShizuku(), trashIsLoading = true) }
            val value = _state.value.recycleBinRetentionValue
            val unit = _state.value.recycleBinRetentionUnit
            val multiplier = when (unit.lowercase()) {
                "seconds", "second" -> 1000L
                "minutes", "minute" -> 60L * 1000L
                "hours", "hour" -> 60L * 60L * 1000L
                "days", "day" -> 24L * 60L * 60L * 1000L
                "weeks", "week" -> 7L * 24L * 60L * 60L * 1000L
                "months", "month" -> 30L * 24L * 60L * 60L * 1000L
                "years", "year" -> 365L * 24L * 60L * 60L * 1000L
                else -> 7L * 24L * 60L * 60L * 1000L
            }
            val expiryMs = value * multiplier
            val files = repository.getTrashFiles(expiryMs)
            _state.update { it.copy(hasShizuku = repository.hasShizuku(), trashFiles = files.toImmutableList(), trashIsLoading = false) }
        }
    }

    fun restoreTrashFiles(encodedNames: List<String>) {
        viewModelScope.launch {
            repository.restoreFromTrash(encodedNames)
            loadTrashFiles()
        }
    }

    fun permanentlyDeleteTrashFiles(encodedNames: List<String>) {
        viewModelScope.launch {
            repository.permanentlyDeleteTrash(encodedNames)
            loadTrashFiles()
        }
    }

    fun renameFile(path: String, newName: String) {
        viewModelScope.launch {
            val success = repository.renameFile(path, newName)
            if (success) {
                val oldName = java.io.File(path).name
                val newPath = java.io.File(path).parentFile?.absolutePath + "/" + newName
                repository.logRecentAction(newPath, "Renamed from $oldName")
                setLocation(_state.value.location) // Refresh the list
            } else {
                setErrorMessage("Failed to rename file")
            }
        }
    }

    fun setShowBatchRenameDialog(show: Boolean) {
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), showBatchRenameDialog = show) }
    }

    fun setIconShape(shape: IconShapeType) {
        prefs.edit().putString("icon_shape", shape.name).apply()
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), iconShapeSetting = shape, activeIconShape = shape) }
    }

    fun setFontStyle(style: String) {
        prefs.edit().putString("font_style", style).apply()
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), fontStyle = style) }
    }

    fun toggleTextDecoration(decoration: String) {
        _state.update { state ->
            val newDecorations = if (state.textDecorations.contains(decoration)) {
                state.textDecorations.minus(decoration).toImmutableSet()
            } else {
                state.textDecorations.plus(decoration).toImmutableSet()
            }
            prefs.edit().putStringSet("text_decorations", newDecorations).apply()
            state.copy(textDecorations = newDecorations)
        }
    }

    fun setMainTextScale(scale: Float) {
        prefs.edit().putFloat("main_text_scale", scale).apply()
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), mainTextScale = scale) }
    }

    fun setSubTextScale(scale: Float) {
        prefs.edit().putFloat("sub_text_scale", scale).apply()
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), subTextScale = scale) }
    }

    fun setGridColumns(columns: Int) {
        prefs.edit().putInt("grid_columns", columns).apply()
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), gridColumns = columns) }
    }

    fun setCornerRoundness(value: Float) {
        prefs.edit().putFloat("corner_roundness", value).apply()
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), cornerRoundness = value) }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val currentPath = _state.value.location
            val success = repository.createFolder(currentPath, name)
            if (success) {
                loadFiles(currentPath)
            }
        }
    }

    fun createFile(name: String) {
        viewModelScope.launch {
            val currentPath = _state.value.location
            val success = repository.createFile(currentPath, name)
            if (success) {
                loadFiles(currentPath)
            }
        }
    }

    fun batchRenameFiles(baseName: String, extension: String, padding: Int, startNumber: Int, isPrefix: Boolean, numberingStyle: String) {
        viewModelScope.launch {
            val selectedItems = _state.value.files.filter { _state.value.selectedFiles.contains(it.id) }.sortedBy { it.name }
            var currentNumber = startNumber
            var anyFailed = false

            for (file in selectedItems) {
                val numString = currentNumber.toString()
                val paddedNumStr = if (padding > numString.length) {
                    "0".repeat(padding - numString.length) + numString
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
                
                val success = repository.renameFile(file.path, newNameBuilder.toString().trim())
                if (success) {
                    val newPath = java.io.File(file.path).parentFile?.absolutePath + "/" + newNameBuilder.toString().trim()
                    repository.logRecentAction(newPath, "Renamed from ${file.name}")
                } else {
                    anyFailed = true
                }
                currentNumber++
            }
            
            clearSelection()
            setShowBatchRenameDialog(false)
            setLocation(_state.value.location)
            if (anyFailed) {
                setErrorMessage("Failed to rename some files")
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("themeMode", mode.name).apply()
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), themeMode = mode) }
    }

    fun setDynamicSystemTheme(useDynamic: Boolean) {
        prefs.edit().putBoolean("useDynamicSystemTheme", useDynamic).apply()
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), useDynamicSystemTheme = useDynamic) }
    }

    fun setThemeHue(hue: Float) {
        prefs.edit().putFloat("themeHue", hue).apply()
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), themeHue = hue, useDynamicSystemTheme = false) }
    }

    fun setGoogleDriveAuthStatus(isAuthenticated: Boolean, email: String?) {
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), isGoogleDriveAuthenticated = isAuthenticated, googleDriveAccountEmail = email, errorMessage = null) }
    }

    fun setMegaAuthStatus(isAuthenticated: Boolean, email: String?, password: String? = null) {
        if (isAuthenticated && email != null && password != null) {
            _state.update { it.copy(megaLoginError = null) }
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val success = megaClient.login(email, password)
                    if (success) {
                        val session = megaClient.dumpSession()
                        if (session != null) {
                            prefs.edit().putString("mega_session", session).putString("mega_email", email).apply()
                        }
                        megaClient.fetchNodes()
                        _state.update { it.copy(hasShizuku = repository.hasShizuku(), isMegaAuthenticated = true, megaAccountEmail = email, megaLoginError = null, showMegaPopup = false) }
                        if (_state.value.location == "mega") {
                            loadFiles("mega")
                        }
                    } else {
                        _state.update { it.copy(megaLoginError = "Invalid email or password.") }
                    }
                } catch (e: Exception) {
                    _state.update { it.copy(megaLoginError = "Mega Login Failed: ${e.message}") }
                }
            }
        } else {
            prefs.edit().remove("mega_session").remove("mega_email").apply()
            _state.update { it.copy(hasShizuku = repository.hasShizuku(), isMegaAuthenticated = false, megaAccountEmail = null, megaLoginError = null, showMegaPopup = false) }
        }
    }

    fun setShowMegaPopup(show: Boolean) {
        _state.update { it.copy(showMegaPopup = show, megaLoginError = null) }
    }

    fun setDropboxAuthStatus(isAuthenticated: Boolean, email: String?) {
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), isDropboxAuthenticated = isAuthenticated, dropboxAccountEmail = email, errorMessage = null) }
    }

    fun updateGoogleDriveAuthStatus(isAuthenticated: Boolean, email: String?) {
        if (isAuthenticated) {
            repository.setGoogleAccount(email)
        } else {
            repository.setGoogleAccount(null)
        }
        setGoogleDriveAuthStatus(isAuthenticated, email)
        
        if (isAuthenticated) {
            fetchDriveStorageQuota()
        }
    }

    private fun fetchDriveStorageQuota() {
        viewModelScope.launch {
            val quota = repository.getDriveStorageQuota()
            if (quota != null) {
                val freeBytes = quota.second - quota.first
                _state.update { it.copy(hasShizuku = repository.hasShizuku(), driveStorageTotalBytes = quota.second, driveStorageFreeBytes = freeBytes) }
            }
        }
    }
    
    fun setErrorMessage(msg: String?) {
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), errorMessage = msg) }
    }


      fun setShowSettingsScreen(show: Boolean) {
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), showSettingsScreen = show) }
    }
    fun setShowThemeSheet(show: Boolean) {
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), showThemeSheet = show) }
    }

    fun openFileViewer(id: Int) {
        val file = _state.value.files.find { it.id == id }
        if (file != null) {
            repository.logRecentAction(file.path, "Opened")
            if (file.path.startsWith("drive_id:")) {
                // Download and then view
                _state.update { it.copy(hasShizuku = repository.hasShizuku(), isDownloading = true) }
                viewModelScope.launch {
                    val driveId = file.path.removePrefix("drive_id:")
                    val localFile = repository.downloadDriveFile(driveId, file.name)
                    if (localFile != null) {
                        _state.update { 
                            it.copy(
                                isDownloading = false,
                                viewingFile = file.copy(path = localFile.absolutePath)
                            )
                        }
                    } else {
                        _state.update { it.copy(hasShizuku = repository.hasShizuku(), isDownloading = false, errorMessage = "Failed to download file from Google Drive.") }
                    }
                }
            } else {
                _state.update { it.copy(hasShizuku = repository.hasShizuku(), viewingFile = file) }
            }
        }
    }

    fun closeFileViewer() {
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), viewingFile = null) }
    }

    fun setClipboard(action: String) {
        val selectedIds = _state.value.selectedFiles
        val paths = _state.value.files.filter { it.id in selectedIds }.map { it.path }
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), 
            clipboardPaths = paths.toImmutableList(), 
            clipboardAction = action, 
            selectedFiles = persistentSetOf(),
            isSelectionMode = false
        ) }
    }

    fun clearClipboard() {
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), clipboardPaths = persistentListOf(), clipboardAction = null) }
    }

    private var pasteJob: kotlinx.coroutines.Job? = null
    private var pasteCancelled = false

    fun pasteClipboard(destLocation: String) {
        val paths = _state.value.clipboardPaths
        val action = _state.value.clipboardAction
        
        if (paths.isNotEmpty() && action != null) {
            pasteJob?.cancel()
            pasteCancelled = false
            
            pasteJob = viewModelScope.launch {
                _state.update { it.copy(hasShizuku = repository.hasShizuku(), isLoading = true, pasteProgress = 0f, isPasteComplete = false, isPastePaused = false) }
                
                val onProgress = { progress: Float ->
                    _state.update { it.copy(hasShizuku = repository.hasShizuku(), pasteProgress = progress) }
                }
                
                val checkPause = suspend {
                    while (_state.value.isPastePaused && !pasteCancelled) {
                        kotlinx.coroutines.delay(100)
                    }
                    if (pasteCancelled) {
                        throw kotlinx.coroutines.CancellationException("Paste cancelled")
                    }
                }

                try {
                    if (action == "copy") {
                        repository.copyFiles(paths, destLocation, onProgress, checkPause)
                    } else if (action == "cut") {
                        repository.moveFiles(paths, destLocation, onProgress, checkPause)
                    }
                    
                    // Force the progress to 100% and hold it briefly so the UI animation has time to play
                    _state.update { it.copy(hasShizuku = repository.hasShizuku(), pasteProgress = 1f) }
                    kotlinx.coroutines.delay(600)
                    
                    _state.update { it.copy(hasShizuku = repository.hasShizuku(), isLoading = false, isPasteComplete = true, pasteProgress = null, pasteLoadingCount = paths.size) }
                    
                    kotlinx.coroutines.delay(2000)
                    
                    clearClipboard()
                    _state.update { it.copy(hasShizuku = repository.hasShizuku(), isPasteComplete = false, pasteLoadingCount = null) }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    _state.update { it.copy(hasShizuku = repository.hasShizuku(), isLoading = false, isPasteComplete = false, pasteProgress = null, isPastePaused = false) }
                } catch (e: Exception) {
                    _state.update { it.copy(hasShizuku = repository.hasShizuku(), isLoading = false, isPasteComplete = false, pasteProgress = null, isPastePaused = false, errorMessage = "Paste failed: ${e.message}") }
                } finally {
                    reload()
                }
            }
        }
    }

    fun togglePastePause() {
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), isPastePaused = !it.isPastePaused) }
    }

    fun cancelPaste() {
        pasteCancelled = true
    }


    private var extractJob: kotlinx.coroutines.Job? = null
    private var extractCancelled = false

    fun extractZip(sourcePath: String, destDirPath: String) {
        extractJob?.cancel()
        extractCancelled = false
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), extractProgress = 0f, isExtractPaused = false) }

        extractJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val zipFile = java.util.zip.ZipFile(sourcePath)
                val entries = zipFile.entries().toList()
                val totalBytes = entries.sumOf { it.size }
                var extractedBytes = 0L

                val folderName = java.io.File(sourcePath).name.substringBeforeLast(".")
                val targetDir = java.io.File(destDirPath, folderName)
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }

                for (entry in entries) {
                    while (_state.value.isExtractPaused && !extractCancelled) {
                        kotlinx.coroutines.delay(100)
                    }
                    if (extractCancelled) break

                    val entryFile = java.io.File(targetDir, entry.name)
                    if (entry.isDirectory) {
                        entryFile.mkdirs()
                    } else {
                        entryFile.parentFile?.mkdirs()
                        zipFile.getInputStream(entry).use { input ->
                            entryFile.outputStream().use { output ->
                                val buffer = ByteArray(8192)
                                var read: Int
                                while (input.read(buffer).also { read = it } != -1) {
                                    while (_state.value.isExtractPaused && !extractCancelled) {
                                        kotlinx.coroutines.delay(100)
                                    }
                                    if (extractCancelled) break
                                    
                                    output.write(buffer, 0, read)
                                    extractedBytes += read
                                    if (totalBytes > 0) {
                                        _state.update { it.copy(hasShizuku = repository.hasShizuku(), extractProgress = extractedBytes.toFloat() / totalBytes) }
                                    }
                                }
                            }
                        }
                    }
                }
                zipFile.close()

                if (extractCancelled) {
                    targetDir.deleteRecursively()
                } else {
                    _state.update { it.copy(hasShizuku = repository.hasShizuku(), extractResultPath = targetDir.absolutePath) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(hasShizuku = repository.hasShizuku(), errorMessage = "Extraction failed: ${e.message}") }
            } finally {
                _state.update { it.copy(hasShizuku = repository.hasShizuku(), extractProgress = null, isExtractPaused = false) }
                if (!extractCancelled) {
                    reload()
                }
                extractJob = null
            }
        }
    }

    fun clearExtractResult() {
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), extractResultPath = null) }
    }

    fun toggleExtractPause() {
        _state.update { it.copy(hasShizuku = repository.hasShizuku(), isExtractPaused = !it.isExtractPaused) }
    }

    fun cancelExtract() {
        extractCancelled = true
    }
    // Music Player Implementation
    private var exoPlayer: androidx.media3.exoplayer.ExoPlayer? = null
    private var musicJob: kotlinx.coroutines.Job? = null
    private var closePlayerJob: kotlinx.coroutines.Job? = null
    
    fun playAudio(file: FileItem) {
        if (exoPlayer == null) {
            exoPlayer = androidx.media3.exoplayer.ExoPlayer.Builder(getApplication()).build().apply {
                addListener(object : androidx.media3.common.Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _state.update { it.copy(isAudioPlaying = isPlaying, hasShizuku = repository.hasShizuku()) }
                        if (isPlaying) {
                            closePlayerJob?.cancel()
                        } else {
                            closePlayerJob?.cancel()
                            closePlayerJob = viewModelScope.launch {
                                kotlinx.coroutines.delay(5000)
                                stopAudio()
                            }
                        }
                    }
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == androidx.media3.common.Player.STATE_READY) {
                            _state.update { it.copy(audioDuration = duration.coerceAtLeast(0L), hasShizuku = repository.hasShizuku()) }
                        } else if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                            _state.update { it.copy(isAudioPlaying = false, audioPlaybackPosition = 0L, hasShizuku = repository.hasShizuku()) }
                            seekTo(0)
                        }
                    }
                    override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
                        _state.update {
                            it.copy(
                                audioTitle = mediaMetadata.title?.toString() ?: it.audioTitle,
                                audioArtist = mediaMetadata.artist?.toString() ?: it.audioArtist,
                                audioArtworkData = mediaMetadata.artworkData ?: it.audioArtworkData,
                                hasShizuku = repository.hasShizuku()
                            )
                        }
                    }
                })
            }
            musicJob = viewModelScope.launch {
                while(true) {
                    val p = exoPlayer
                    if (p != null && p.isPlaying) {
                        _state.update { it.copy(audioPlaybackPosition = p.currentPosition, hasShizuku = repository.hasShizuku()) }
                    }
                    kotlinx.coroutines.delay(500)
                }
            }
        }
        
        val uri = if (file.path.startsWith("drive_id:")) {
            android.net.Uri.parse(file.path)
        } else if (file.path.startsWith("content://")) {
            android.net.Uri.parse(file.path)
        } else {
            android.net.Uri.fromFile(java.io.File(file.path))
        }

        val mediaItem = androidx.media3.common.MediaItem.fromUri(uri)
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()
        exoPlayer?.play()
        
        _state.update { 
            it.copy(
                currentAudioFile = file,
                audioTitle = file.name,
                audioArtist = "Unknown Artist",
                audioArtworkData = null,
                hasShizuku = repository.hasShizuku()
            ) 
        }
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val retriever = android.media.MediaMetadataRetriever()
                if (file.path.startsWith("content://")) {
                    retriever.setDataSource(getApplication(), uri)
                } else if (!file.path.startsWith("drive_id:")) {
                    retriever.setDataSource(file.path)
                } else {
                    return@launch
                }
                val title = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)
                val artist = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val art = retriever.embeddedPicture
                retriever.release()
                
                _state.update {
                    it.copy(
                        audioTitle = title ?: it.audioTitle,
                        audioArtist = artist ?: it.audioArtist,
                        audioArtworkData = art ?: it.audioArtworkData
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun toggleAudioPlayback() {
        exoPlayer?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }
    
    fun seekAudio(position: Long) {
        exoPlayer?.seekTo(position)
        _state.update { it.copy(audioPlaybackPosition = position, hasShizuku = repository.hasShizuku()) }
    }
    
    fun stopAudio() {
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        _state.update { 
            it.copy(
                currentAudioFile = null,
                isAudioPlaying = false,
                audioPlaybackPosition = 0L,
                audioDuration = 0L,
                showFullScreenPlayer = false,
                audioArtworkData = null,
                hasShizuku = repository.hasShizuku()
            )
        }
    }
    
    fun playNextAudio() {
        val current = _state.value.currentAudioFile ?: return
        val audioFiles = _state.value.files.filter { 
            val ext = it.name.substringAfterLast('.', "").lowercase()
            ext in listOf("mp3", "opus", "wav", "ogg", "flac", "m4a", "aac")
        }
        val idx = audioFiles.indexOfFirst { it.id == current.id }
        if (idx != -1 && idx < audioFiles.size - 1) {
            playAudio(audioFiles[idx + 1])
        } else if (audioFiles.isNotEmpty()) {
            playAudio(audioFiles[0])
        }
    }

    fun playPreviousAudio() {
        val current = _state.value.currentAudioFile ?: return
        val audioFiles = _state.value.files.filter { 
            val ext = it.name.substringAfterLast('.', "").lowercase()
            ext in listOf("mp3", "opus", "wav", "ogg", "flac", "m4a", "aac")
        }
        val idx = audioFiles.indexOfFirst { it.id == current.id }
        if (idx > 0) {
            playAudio(audioFiles[idx - 1])
        } else if (audioFiles.isNotEmpty()) {
            playAudio(audioFiles[audioFiles.size - 1])
        }
    }
    
    fun setShowFullScreenPlayer(show: Boolean) {
        _state.update { it.copy(showFullScreenPlayer = show, hasShizuku = repository.hasShizuku()) }
    }
    
    fun requestAuth(reason: AuthReason, path: String, fileId: Int?, folderName: String?) {
        _state.update { 
            it.copy(
                authRequestReason = reason, 
                authRequestPath = path, 
                authRequestFileId = fileId, 
                authRequestFolderName = folderName 
            ) 
        }
    }

    fun cancelAuth() {
        _state.update { 
            it.copy(
                authRequestReason = null, 
                authRequestPath = null, 
                authRequestFileId = null, 
                authRequestFolderName = null,
                authErrorMessage = null
            ) 
        }
    }

    fun authSuccess(reason: AuthReason, path: String, fileId: Int?, folderName: String?, password: String?) {
        viewModelScope.launch {
            if (password != null) {
                val currentHash = repository.getGlobalPasswordHash()
                if (currentHash != repository.hashPassword(password)) {
                    _state.value = _state.value.copy(authErrorMessage = "Wrong password, try again or reset from settings")
                    return@launch
                }
            }
            
            when (reason) {
                AuthReason.LOCK_FILE -> {
                    repository.setLocked(path, true)
                    loadFiles(_state.value.location)
                    _snackbarMessage.emit("Item locked successfully")
                }
                AuthReason.UNLOCK_FILE -> {
                    repository.setLocked(path, false)
                    loadFiles(_state.value.location)
                    _snackbarMessage.emit("Item unlocked successfully")
                }
                AuthReason.OPEN_FILE -> {
                    if (fileId != null) {
                        openFileViewer(fileId)
                    } else if (folderName != null) {
                        setLocation(path, folderName)
                    }
                }
            }
            cancelAuth()
        }
    }

    fun updateGlobalPassword(oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            val currentHash = repository.getGlobalPasswordHash()
            if (currentHash == repository.hashPassword(oldPassword)) {
                repository.setGlobalPasswordHash(repository.hashPassword(newPassword))
                _state.value = _state.value.copy(securitySettingsErrorMessage = null)
                _snackbarMessage.emit("Password updated successfully")
            } else {
                _state.value = _state.value.copy(securitySettingsErrorMessage = "Old password is wrong")
            }
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        repository.setBiometricEnabled(enabled)
    }

    fun handleSmbAction(action: AppAction.SmbAction) {
        when (action) {
            is AppAction.SmbAction.AddConnection -> {
                smbStore.saveConnection(action.connection, action.password)
                _state.update { it.copy(smbState = it.smbState.copy(savedConnections = smbStore.getConnections())) }
            }
            is AppAction.SmbAction.DeleteConnection -> {
                smbStore.deleteConnection(action.connectionId)
                _state.update { it.copy(smbState = it.smbState.copy(savedConnections = smbStore.getConnections())) }
                if (_state.value.smbState.activeConnectionId == action.connectionId) {
                    handleSmbAction(AppAction.SmbAction.Disconnect(action.connectionId))
                }
            }
            is AppAction.SmbAction.Connect -> {
                val connection = smbStore.getConnections().find { it.id == action.connectionId } ?: return
                val password = smbStore.getPassword(action.connectionId) ?: return
                
                _state.update { it.copy(smbState = it.smbState.copy(connectionStatus = ConnectionStatus.Connecting, error = null)) }
                
                viewModelScope.launch {
                    val result = smbProvider.connect(connection, password)
                    if (result.isSuccess) {
                        _state.update { it.copy(
                            smbState = it.smbState.copy(
                                activeConnectionId = action.connectionId,
                                connectionStatus = ConnectionStatus.Connected,
                                error = null
                            )
                        )}
                        prefs.edit().putString("active_smb_connection", action.connectionId).apply()
                        setLocation("smb_${action.connectionId}:/")
                    } else {
                        val e = result.exceptionOrNull()
                        val smbError = when (e) {
                            is java.net.UnknownHostException, is java.net.ConnectException -> SmbError.HOST_UNREACHABLE
                            is java.net.SocketTimeoutException, is java.util.concurrent.TimeoutException -> SmbError.TIMEOUT
                            else -> {
                                val msg = e?.message ?: ""
                                if (msg.contains("LOGON_FAILURE") || msg.contains("Access denied") || msg.contains("Authentication")) SmbError.AUTH_FAILED
                                else if (msg.contains("BAD_NETWORK_NAME") || msg.contains("not found")) SmbError.SHARE_NOT_FOUND
                                else SmbError.UNKNOWN
                            }
                        }
                        _state.update { it.copy(smbState = it.smbState.copy(connectionStatus = ConnectionStatus.Error, error = smbError)) }
                        _snackbarMessage.emit("SMB Error: $smbError")
                    }
                }
            }

            is AppAction.SmbAction.Disconnect -> {
                viewModelScope.launch {
                    smbProvider.disconnect()
                    _state.update { it.copy(smbState = it.smbState.copy(
                        activeConnectionId = null,
                        connectionStatus = ConnectionStatus.Idle,
                        error = null,
                        currentPath = ""
                    ))}
                    prefs.edit().remove("active_smb_connection").apply()
                    
                    val st = _state.value
                    if (st.isGoogleDriveAuthenticated) {
                        setLocation("drive")
                    } else if (st.isMegaAuthenticated) {
                        setLocation("mega")
                    } else if (st.isDropboxAuthenticated) {
                        setLocation("dropbox")
                    } else {
                        setLocation("cloud")
                    }
                }
            }
            is AppAction.SmbAction.NavigateTo -> {
                setLocation(action.path)
            }
            is AppAction.SmbAction.ConnectSucceeded -> {}
            is AppAction.SmbAction.ConnectFailed -> {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        musicJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
    }
}
