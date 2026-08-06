import os

with open('app/src/main/java/com/ripple/filemanager/MainViewModel.kt', 'r', encoding='utf-8') as f:
    content = f.read()

helpers = '''
    private val _safRequestEvent = kotlinx.coroutines.flow.MutableSharedFlow<String>()
    val safRequestEvent = kotlinx.coroutines.flow.asSharedFlow(_safRequestEvent)

    fun requestSafAccess(path: String) {
        viewModelScope.launch {
            _safRequestEvent.emit(path)
        }
    }

    fun requestRootAccess() {
        viewModelScope.launch(Dispatchers.IO) {
            com.topjohnwu.superuser.Shell.getShell()
            reload()
        }
    }

    fun requestShizukuAccess() {
        try {
            if (rikka.shizuku.Shizuku.pingBinder()) {
                if (rikka.shizuku.Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    rikka.shizuku.Shizuku.requestPermission(0)
                } else {
                    reload()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
'''

content = content.replace("    fun hasRestrictedAccess", helpers + "\n    fun hasRestrictedAccess")

with open('app/src/main/java/com/ripple/filemanager/MainViewModel.kt', 'w', encoding='utf-8') as f:
    f.write(content)
