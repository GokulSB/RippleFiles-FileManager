Created At: 2026-07-26T07:43:07+05:30
Completed At: 2026-07-26T07:43:07+05:30
File Path: `file:///d:/File%20Manager/android/app/src/main/java/com/ripple/filemanager/MainViewModel.kt`
Total Lines: 1231
Total Bytes: 53817
Showing lines 105 to 130
The following code has been modified to include a line number before every line, in the format: <line_number>: <original_line>. Please note that any changes targeting the original code should remove the line number, colon, and leading space.
105:     val recycleBinRetentionUnit: String = "Days",
106:     
107:     // File Organiser
108:     val orgDestDocs: String = "",
109:     val orgDestImages: String = "",
110:     val orgDestApks: String = "",
111:     val orgDestMusic: String = "",
112:     val orgDestVideos: String = "",
113:     val organiseProgress: Float? = null,
114: 
115:     val extractProgress: Float? = null,
116:     val isExtractPaused: Boolean = false,
117:     val extractResultPath: String? = null
118: )
119: 
120: class MainViewModel(application: Application) : AndroidViewModel(application) {
121:     private val _state = MutableStateFlow(AppState())
122:     val state: StateFlow<AppState> = _state.asStateFlow()
123: 
124:     private val _snackbarMessage = MutableSharedFlow<String>()
125:     val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()
126: 
127:     private val repository = FileRepository(application)
128:     private var rawFiles: List<FileItem> = emptyList()
129:     private var loadJob: kotlinx.coroutines.Job? = null
130: 
The above content does NOT show the entire file contents. If you need to view any lines of the file which were not shown to complete your task, call this tool again to view those lines.
