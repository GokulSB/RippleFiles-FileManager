package nz.mega.sdk

import android.content.Context
import com.ripple.filemanager.FileItem
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MegaClient(private val context: Context) {
    private val appKey = "dummy_app_key"
    
    internal val megaApi: MegaApi by lazy {
        val cacheDir = context.cacheDir.absolutePath
        MegaApi(appKey, cacheDir, "RippleFiles/1.0")
    }

    suspend fun login(email: String, password: String): Boolean = suspendCancellableCoroutine { cont ->
        val listener = object : MegaRequestListener() {
            override fun onRequestFinish(api: MegaApi?, request: MegaRequest?, e: MegaError?) {
                if (e != null && e.errorCode == MegaError.API_OK) {
                    cont.resume(true)
                } else {
                    cont.resumeWithException(Exception("MEGA Login failed: ${e?.errorString}"))
                }
            }
        }
        megaApi.login(email, password, listener)
    }

    fun dumpSession(): String? {
        return megaApi.dumpSession()
    }

    suspend fun fastLogin(session: String): Boolean = suspendCancellableCoroutine { cont ->
        val listener = object : MegaRequestListener() {
            override fun onRequestFinish(api: MegaApi?, request: MegaRequest?, e: MegaError?) {
                if (e != null && e.errorCode == MegaError.API_OK) {
                    cont.resume(true)
                } else {
                    cont.resumeWithException(Exception("MEGA FastLogin failed: ${e?.errorString}"))
                }
            }
        }
        megaApi.fastLogin(session, listener)
    }

    suspend fun fetchNodes(): Boolean = suspendCancellableCoroutine { cont ->
        val listener = object : MegaRequestListener() {
            override fun onRequestFinish(api: MegaApi?, request: MegaRequest?, e: MegaError?) {
                if (e != null && e.errorCode == MegaError.API_OK) {
                    cont.resume(true)
                } else {
                    cont.resumeWithException(Exception("Failed to fetch MEGA nodes: ${e?.errorString}"))
                }
            }
        }
        megaApi.fetchNodes(listener)
    }

    fun getChildren(folderId: String?): List<FileItem> {
        val parentNode = if (folderId != null) {
            megaApi.getNodeByHandle(folderId.toLongOrNull() ?: 0L)
        } else {
            megaApi.rootNode
        }

        if (parentNode == null) return emptyList()

        val children = megaApi.getChildren(parentNode) ?: return emptyList()
        val result = mutableListOf<FileItem>()
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        for (i in 0 until children.size()) {
            val child = children.get(i) ?: continue
            val isFolder = child.isFolder
            val sizeStr = if (isFolder) "Folder" else formatSize(child.size)
            val dateStr = sdf.format(Date(child.modificationTime * 1000))
            
            result.add(
                FileItem(
                    id = child.handle.hashCode(),
                    path = "mega_id:${child.handle}",
                    name = child.name ?: "Unknown",
                    type = if (isFolder) "folder" else child.name?.substringAfterLast(".", "") ?: "",
                    kind = if (isFolder) "Folder" else "File",
                    size = sizeStr,
                    changed = dateStr,
                    owner = "me",
                    isPinned = false,
                    isEmptyFolder = isFolder && (megaApi.getChildren(child)?.size() ?: 0) == 0,
                    sizeBytes = if (isFolder) 0 else child.size,
                    lastModified = child.modificationTime * 1000
                )
            )
        }
        return result
    }
    
    suspend fun deleteNode(handleStr: String): Boolean = suspendCancellableCoroutine { cont ->
        val node = megaApi.getNodeByHandle(handleStr.toLongOrNull() ?: 0L)
        if (node == null) {
            cont.resume(false)
            return@suspendCancellableCoroutine
        }
        
        val listener = object : MegaRequestListener() {
            override fun onRequestFinish(api: MegaApi?, request: MegaRequest?, e: MegaError?) {
                if (e != null && e.errorCode == MegaError.API_OK) {
                    cont.resume(true)
                } else {
                    cont.resumeWithException(Exception("Failed to delete MEGA node: ${e?.errorString}"))
                }
            }
        }
        megaApi.remove(node, listener)
    }

    suspend fun getStorageQuota(): Pair<Long, Long> = suspendCancellableCoroutine { cont ->
        val listener = object : MegaRequestListener() {
            override fun onRequestFinish(api: MegaApi?, request: MegaRequest?, e: MegaError?) {
                if (e != null && e.errorCode == MegaError.API_OK && request != null) {
                    val details = request.megaAccountDetails
                    if (details != null) {
                        cont.resume(Pair(details.storageUsed, details.storageMax))
                    } else {
                        cont.resume(Pair(0L, 0L))
                    }
                } else {
                    cont.resume(Pair(0L, 0L))
                }
            }
        }
        megaApi.getAccountDetails(listener)
    }

    suspend fun downloadFile(handleStr: String, destPath: String): Boolean = suspendCancellableCoroutine { cont ->
        val node = megaApi.getNodeByHandle(handleStr.toLongOrNull() ?: 0L)
        if (node == null) {
            cont.resume(false)
            return@suspendCancellableCoroutine
        }
        
        val listener = object : nz.mega.sdk.MegaTransferListener() {
            override fun onTransferFinish(api: MegaApi?, transfer: nz.mega.sdk.MegaTransfer?, error: MegaError?) {
                if (error != null && error.errorCode == MegaError.API_OK) {
                    cont.resume(true)
                } else {
                    cont.resumeWithException(Exception("Download failed: ${error?.errorString}"))
                }
            }
        }
        megaApi.startDownload(node, destPath, null, null, false, null, 0, 0, false, listener)
    }

    private fun formatSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(Locale.getDefault(), "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
