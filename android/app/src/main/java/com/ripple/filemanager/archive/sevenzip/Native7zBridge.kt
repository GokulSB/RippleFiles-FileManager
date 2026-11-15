package com.ripple.filemanager.archive.sevenzip

import android.content.Context
import android.util.Log
import java.io.File

data class NativeFormatInfo(
    val name: String,
    val extension: String,
    val canUpdate: Boolean,
    val sigSize: Int,
    val sigOffset: Int
)

data class NativeArchiveEntry(
    val index: Int,
    val path: String,
    val size: Long,
    val packSize: Long,
    val isDir: Boolean,
    val crc: Long,
    val mtime: Long,
    val isEncrypted: Boolean,
    val method: String
)

interface NativeArchiveCallback {
    fun isCancelled(): Boolean = false
    fun onProgress(completedBytes: Long, totalBytes: Long, currentItem: String?) {}
    fun onPasswordRequired(): String? = null
    fun onFileConflict(relativePath: String): Int = 0 // 0=Overwrite, 1=Skip, 2=OverwriteAll, 3=SkipAll
}

object Native7zBridge {
    private const val TAG = "Native7zBridge"
    private var isLoaded = false
    private var isInitialized = false

    fun init(context: Context? = null): Boolean {
        if (isInitialized) return true

        synchronized(this) {
            if (isInitialized) return true

            try {
                if (!isLoaded) {
                    try {
                        System.loadLibrary("c++_shared")
                    } catch (t: Throwable) {
                        Log.w(TAG, "c++_shared: ${t.message}")
                    }
                    try {
                        System.loadLibrary("7z")
                    } catch (t: Throwable) {
                        Log.w(TAG, "7z: ${t.message}")
                    }
                    System.loadLibrary("7z_bridge")
                    isLoaded = true
                }

                // Determine path to lib7z.so
                var lib7zPath: String? = null
                if (context != null) {
                    val nativeDir = context.applicationInfo.nativeLibraryDir
                    val candidate = File(nativeDir, "lib7z.so")
                    if (candidate.exists()) {
                        lib7zPath = candidate.absolutePath
                    }
                }

                if (lib7zPath == null) {
                    // Fallback to library name if system loader can find it
                    lib7zPath = "lib7z.so"
                }

                isInitialized = nativeInit(lib7zPath)
                if (!isInitialized && lib7zPath != "lib7z.so") {
                    // Retry with relative name
                    isInitialized = nativeInit("lib7z.so")
                }

                if (isInitialized) {
                    Log.i(TAG, "Native 7z bridge successfully initialized.")
                } else {
                    Log.e(TAG, "Failed to initialize native 7z bridge with path: $lib7zPath")
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Error loading 7z native libraries", t)
                isInitialized = false
            }
            return isInitialized
        }
    }

    @JvmStatic
    external fun nativeInit(lib7zPath: String): Boolean

    @JvmStatic
    external fun nativeSupportedFormats(): Array<NativeFormatInfo>?

    @JvmStatic
    external fun nativeOpen(path: String, password: String?, volumeHint: String?): Long

    @JvmStatic
    external fun nativeList(handle: Long): Array<NativeArchiveEntry>?

    @JvmStatic
    external fun nativeExtract(
        handle: Long,
        indices: IntArray?,
        destDir: String,
        password: String?,
        callback: NativeArchiveCallback?
    ): Boolean

    @JvmStatic
    external fun nativeTest(
        handle: Long,
        indices: IntArray?,
        password: String?,
        callback: NativeArchiveCallback?
    ): Boolean

    @JvmStatic
    external fun nativeCreate(
        inputPaths: Array<String>,
        outPath: String,
        format: String,
        level: Int,
        password: String?,
        encryptNames: Boolean,
        splitSize: Long,
        callback: NativeArchiveCallback?
    ): Boolean

    @JvmStatic
    external fun nativeClose(handle: Long)
}
