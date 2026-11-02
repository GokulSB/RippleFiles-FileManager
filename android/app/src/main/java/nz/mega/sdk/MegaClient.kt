package nz.mega.sdk

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.ripple.filemanager.FileItem
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MegaClient(
    private val context: Context,
    val accountEmail: String? = null
) {
    private val appKey = "l4cmkI7B"

    companion object {
        private const val TAG = "MegaClient"

        // Set of active listeners strongly retained on the Java heap to prevent
        // premature Garbage Collection while native C++ calls (libmega.so) are in flight.
        // If an anonymous listener is GC-collected before onRequestFinish completes,
        // its C++ director is deleted, causing SIGSEGV inside MegaApiImpl::fireOnRequestFinish.
        private val activeListeners = ConcurrentHashMap.newKeySet<Any>()
        private val listenerTimestamps = ConcurrentHashMap<Any, Long>()
        private const val LISTENER_TIMEOUT_MS = 15 * 60 * 1000L // 15 minutes safety prune

        // Static cache of client instances to ensure MegaApi instances and native C++
        // state are preserved for the process lifetime and isolated per account.
        private val clientInstances = ConcurrentHashMap<String, MegaClient>()

        fun getInstance(context: Context, email: String?): MegaClient {
            val key = email ?: "default"
            return clientInstances.getOrPut(key) {
                MegaClient(context.applicationContext, email)
            }
        }

        private fun retainListener(listener: Any) {
            val now = SystemClock.elapsedRealtime()
            activeListeners.add(listener)
            listenerTimestamps[listener] = now

            // Periodic safety cleanup of stale listeners older than 15 minutes
            if (listenerTimestamps.size > 20) {
                val iterator = listenerTimestamps.entries.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    if (now - entry.value > LISTENER_TIMEOUT_MS) {
                        activeListeners.remove(entry.key)
                        iterator.remove()
                    }
                }
            }
        }

        private fun releaseListener(listener: Any) {
            activeListeners.remove(listener)
            listenerTimestamps.remove(listener)
        }
    }

    internal val megaApi: MegaApi by lazy {
        val accountFolder = if (!accountEmail.isNullOrBlank()) {
            "mega_" + accountEmail.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        } else {
            "mega_default"
        }
        val accountDir = File(context.cacheDir, accountFolder).apply { mkdirs() }
        MegaApi(appKey, accountDir.absolutePath, "RippleFiles/1.0")
    }

    private suspend inline fun <T> executeRequest(
        crossinline action: (MegaRequestListener) -> Unit,
        crossinline onFinish: (cont: CancellableContinuation<T>, api: MegaApi?, request: MegaRequest?, e: MegaError?) -> Unit
    ): T = suspendCancellableCoroutine { cont ->
        val listener = object : MegaRequestListener() {
            override fun onRequestFinish(api: MegaApi?, request: MegaRequest?, e: MegaError?) {
                try {
                    onFinish(cont, api, request, e)
                } catch (t: Throwable) {
                    Log.e(TAG, "Error in onRequestFinish", t)
                    if (cont.isActive) cont.resumeWithException(t)
                } finally {
                    releaseListener(this)
                }
            }
        }
        retainListener(listener)
        try {
            action(listener)
        } catch (t: Throwable) {
            releaseListener(listener)
            if (cont.isActive) cont.resumeWithException(t)
        }
    }

    private suspend inline fun <T> executeTransfer(
        crossinline action: (MegaTransferListener) -> Unit,
        crossinline onFinish: (cont: CancellableContinuation<T>, api: MegaApi?, transfer: MegaTransfer?, error: MegaError?) -> Unit
    ): T = suspendCancellableCoroutine { cont ->
        val listener = object : MegaTransferListener() {
            override fun onTransferFinish(api: MegaApi?, transfer: MegaTransfer?, error: MegaError?) {
                try {
                    onFinish(cont, api, transfer, error)
                } catch (t: Throwable) {
                    Log.e(TAG, "Error in onTransferFinish", t)
                    if (cont.isActive) cont.resumeWithException(t)
                } finally {
                    releaseListener(this)
                }
            }
        }
        retainListener(listener)
        try {
            action(listener)
        } catch (t: Throwable) {
            releaseListener(listener)
            if (cont.isActive) cont.resumeWithException(t)
        }
    }

    suspend fun login(email: String, password: String): Boolean = executeRequest(
        action = { listener -> megaApi.login(email, password, listener) },
        onFinish = { cont, _, _, e ->
            if (!cont.isActive) return@executeRequest
            if (e != null && e.errorCode == MegaError.API_OK) {
                cont.resume(true)
            } else {
                cont.resumeWithException(Exception("MEGA Login failed: ${e?.errorString ?: "Unknown error"}"))
            }
        }
    )

    fun dumpSession(): String? {
        return try {
            megaApi.dumpSession()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fastLogin(session: String): Boolean = executeRequest(
        action = { listener -> megaApi.fastLogin(session, listener) },
        onFinish = { cont, _, _, e ->
            if (!cont.isActive) return@executeRequest
            if (e != null && e.errorCode == MegaError.API_OK) {
                cont.resume(true)
            } else {
                cont.resumeWithException(Exception("MEGA FastLogin failed: ${e?.errorString ?: "Unknown error"}"))
            }
        }
    )

    suspend fun fetchNodes(): Boolean = executeRequest(
        action = { listener -> megaApi.fetchNodes(listener) },
        onFinish = { cont, _, _, e ->
            if (!cont.isActive) return@executeRequest
            if (e != null && e.errorCode == MegaError.API_OK) {
                cont.resume(true)
            } else {
                cont.resumeWithException(Exception("Failed to fetch MEGA nodes: ${e?.errorString ?: "Unknown error"}"))
            }
        }
    )

    fun getChildren(folderId: String?): List<FileItem> {
        return try {
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
                        owner = accountEmail ?: "me",
                        isPinned = false,
                        isEmptyFolder = isFolder && (megaApi.getChildren(child)?.size() ?: 0) == 0,
                        sizeBytes = if (isFolder) 0 else child.size,
                        lastModified = child.modificationTime * 1000
                    )
                )
            }
            result
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    suspend fun deleteNode(handleStr: String): Boolean {
        val node = megaApi.getNodeByHandle(handleStr.toLongOrNull() ?: 0L) ?: return false
        return executeRequest(
            action = { listener -> megaApi.remove(node, listener) },
            onFinish = { cont, _, _, e ->
                if (!cont.isActive) return@executeRequest
                if (e != null && e.errorCode == MegaError.API_OK) {
                    cont.resume(true)
                } else {
                    cont.resumeWithException(Exception("Failed to delete MEGA node: ${e?.errorString ?: "Unknown error"}"))
                }
            }
        )
    }

    suspend fun getStorageQuota(): Pair<Long, Long> = executeRequest(
        action = { listener -> megaApi.getAccountDetails(listener) },
        onFinish = { cont, _, request, e ->
            if (!cont.isActive) return@executeRequest
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
    )

    suspend fun downloadFile(handleStr: String, destPath: String): Boolean {
        val node = megaApi.getNodeByHandle(handleStr.toLongOrNull() ?: 0L) ?: return false
        return executeTransfer(
            action = { listener ->
                megaApi.startDownload(node, destPath, null, null, false, null, 0, 0, false, listener)
            },
            onFinish = { cont, _, _, error ->
                if (!cont.isActive) return@executeTransfer
                if (error != null && error.errorCode == MegaError.API_OK) {
                    cont.resume(true)
                } else {
                    cont.resumeWithException(Exception("Download failed: ${error?.errorString ?: "Unknown error"}"))
                }
            }
        )
    }

    suspend fun uploadFile(localPath: String, parentHandle: String?): Boolean {
        val parentNode = if (parentHandle != null) {
            megaApi.getNodeByHandle(parentHandle.toLongOrNull() ?: 0L)
        } else {
            megaApi.rootNode
        } ?: throw Exception("MEGA upload failed: destination folder not found")

        val fileName = localPath.substringAfterLast('/')
        return executeTransfer(
            action = { listener ->
                megaApi.startUpload(
                    localPath,
                    parentNode,
                    fileName,
                    -1L,
                    null,
                    false,
                    false,
                    null,
                    listener
                )
            },
            onFinish = { cont, _, _, error ->
                if (!cont.isActive) return@executeTransfer
                if (error != null && error.errorCode == MegaError.API_OK) {
                    cont.resume(true)
                } else {
                    cont.resumeWithException(Exception("MEGA upload failed: ${error?.errorString ?: "Unknown error"}"))
                }
            }
        )
    }

    suspend fun createFolder(name: String, parentHandle: String?): Boolean {
        val parentNode = if (parentHandle != null) {
            megaApi.getNodeByHandle(parentHandle.toLongOrNull() ?: 0L)
        } else {
            megaApi.rootNode
        } ?: throw Exception("MEGA create folder failed: destination folder not found")

        return executeRequest(
            action = { listener -> megaApi.createFolder(name, parentNode, listener) },
            onFinish = { cont, _, _, e ->
                if (!cont.isActive) return@executeRequest
                if (e != null && e.errorCode == MegaError.API_OK) {
                    cont.resume(true)
                } else {
                    cont.resumeWithException(Exception("MEGA create folder failed: ${e?.errorString ?: "Unknown error"}"))
                }
            }
        )
    }

    suspend fun localLogout(): Boolean = executeRequest(
        action = { listener -> megaApi.localLogout(listener) },
        onFinish = { cont, _, _, e ->
            if (!cont.isActive) return@executeRequest
            cont.resume(e == null || e.errorCode == MegaError.API_OK)
        }
    )

    private fun formatSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(Locale.getDefault(), "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
