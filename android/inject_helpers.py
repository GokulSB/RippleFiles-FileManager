import os

with open('app/src/main/java/com/ripple/filemanager/FileRepository.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Add imports
imports = '''import android.os.Environment
import android.provider.DocumentsContract
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.io.SuFile
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader'''
content = content.replace("import android.os.Environment", imports)

# We need to add saf, root, shizuku helper functions at the top of FileRepository class
helpers = '''
    fun hasSafPermission(path: String): Boolean {
        val uriStr = if (path.contains("Android/obb")) 
            "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fobb"
        else 
            "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata"
        return context.contentResolver.persistedUriPermissions.any { it.uri.toString().startsWith(uriStr) }
    }

    fun hasRoot(): Boolean {
        return Shell.isAppGrantedRoot() == true
    }

    fun hasShizuku(): Boolean {
        return try {
            Shizuku.pingBinder() && Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    private fun getDocumentFile(path: String): DocumentFile? {
        val isObb = path.contains("Android/obb")
        val baseUriStr = if (isObb) 
            "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fobb/document/primary%3AAndroid%2Fobb"
        else 
            "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata/document/primary%3AAndroid%2Fdata"
        
        val basePath = if (isObb) "/Android/obb" else "/Android/data"
        
        val relativePath = path.removePrefix(basePath).trim('/')
        var currentDoc = DocumentFile.fromTreeUri(context, Uri.parse(baseUriStr))
        
        if (relativePath.isNotEmpty()) {
            val parts = relativePath.split("/")
            for (part in parts) {
                currentDoc = currentDoc?.findFile(part)
                if (currentDoc == null) return null
            }
        }
        return currentDoc
    }
'''

content = content.replace("class FileRepository(private val context: Context) {", "class FileRepository(private val context: Context) {" + helpers)

with open('app/src/main/java/com/ripple/filemanager/FileRepository.kt', 'w', encoding='utf-8') as f:
    f.write(content)
