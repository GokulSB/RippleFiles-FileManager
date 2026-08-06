import os

with open('app/src/main/java/com/ripple/filemanager/MainActivity.kt', 'r', encoding='utf-8') as f:
    content = f.read()

imports = '''import android.provider.DocumentsContract
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch'''

content = content.replace("import android.provider.Settings", "import android.provider.Settings\n" + imports)

launcher = '''
    private val safLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                viewModel.reload()
            }
        }
    }

    private fun requestSafAccessInternal(path: String) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val uriStr = if (path.contains("Android/obb")) 
                    "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fobb/document/primary%3AAndroid%2Fobb"
                else 
                    "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata/document/primary%3AAndroid%2Fdata"
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse(uriStr))
            }
        }
        safLauncher.launch(intent)
    }
'''

content = content.replace("    private val viewModel: MainViewModel by viewModels()", "    private val viewModel: MainViewModel by viewModels()\n" + launcher)

on_create = '''
        lifecycleScope.launch {
            viewModel.safRequestEvent.collect { path ->
                requestSafAccessInternal(path)
            }
        }
        
        try {
            rikka.shizuku.Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    viewModel.reload()
                }
            }
        } catch (e: Exception) {}
'''

content = content.replace("enableEdgeToEdge()", "enableEdgeToEdge()\n" + on_create)

with open('app/src/main/java/com/ripple/filemanager/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(content)
