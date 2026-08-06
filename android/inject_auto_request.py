import os

with open('app/src/main/java/com/ripple/filemanager/MainViewModel.kt', 'r', encoding='utf-8') as f:
    content = f.read()

auto_request = '''
    fun autoRequestAccess(path: String) {
        try {
            if (rikka.shizuku.Shizuku.pingBinder()) {
                if (rikka.shizuku.Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    rikka.shizuku.Shizuku.requestPermission(0)
                    return
                }
            }
        } catch (e: Exception) {}
        
        try {
            if (com.topjohnwu.superuser.Shell.isAppGrantedRoot() == true || com.topjohnwu.superuser.Shell.rootAccess()) {
                requestRootAccess()
                return
            }
        } catch (e: Exception) {}
    }
'''

if "fun autoRequestAccess(" not in content:
    content = content.replace("fun requestShizukuAccess() {", auto_request + "\n    fun requestShizukuAccess() {")

with open('app/src/main/java/com/ripple/filemanager/MainViewModel.kt', 'w', encoding='utf-8') as f:
    f.write(content)
