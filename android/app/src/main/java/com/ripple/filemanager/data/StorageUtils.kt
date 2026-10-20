package com.ripple.filemanager.data

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import androidx.core.content.ContextCompat
import java.io.File

object StorageUtils {

    /**
     * Resolves the mounted root path of the primary secondary/removable storage (SD Card / OTG).
     * Returns an accessible path like "/storage/1234-5678", or null if no SD Card is present.
     */
    fun getSdCardPath(context: Context): String? {
        // 1. Try StorageManager.storageVolumes (API 24+)
        try {
            val sm = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
            if (sm != null) {
                for (volume in sm.storageVolumes) {
                    if (!volume.isPrimary) {
                        val state = volume.state
                        if (state != Environment.MEDIA_MOUNTED && state != Environment.MEDIA_MOUNTED_READ_ONLY) {
                            continue
                        }

                        // On Android 11+ (API 30+), volume.directory provides the File
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val dir = volume.directory
                            if (dir != null) {
                                var p = dir.absolutePath
                                if (p.startsWith("/mnt/media_rw/")) {
                                    p = p.replace("/mnt/media_rw/", "/storage/")
                                }
                                val f = File(p)
                                if (f.exists() || f.canRead()) return p
                            }
                        }

                        // Try volume.uuid
                        val uuid = volume.uuid
                        if (!uuid.isNullOrEmpty()) {
                            val candidate = File("/storage/$uuid")
                            if (candidate.exists() || candidate.canRead()) return candidate.absolutePath
                            return candidate.absolutePath
                        }

                        // Reflection fallback for getPath()
                        try {
                            val getPathMethod = volume.javaClass.getMethod("getPath")
                            val rawPath = getPathMethod.invoke(volume) as? String
                            if (!rawPath.isNullOrEmpty()) {
                                var clean = rawPath
                                if (clean.startsWith("/mnt/media_rw/")) {
                                    clean = clean.replace("/mnt/media_rw/", "/storage/")
                                }
                                return clean
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Try ContextCompat.getExternalFilesDirs
        try {
            val externalDirs = ContextCompat.getExternalFilesDirs(context, null)
            val primaryRoot = Environment.getExternalStorageDirectory().absolutePath
            for (dir in externalDirs) {
                if (dir != null) {
                    var p = dir.absolutePath
                    if (p.startsWith("/mnt/media_rw/")) {
                        p = p.replace("/mnt/media_rw/", "/storage/")
                    }
                    if (!p.startsWith(primaryRoot) && !p.contains("emulated")) {
                        val idx = p.indexOf("/Android", ignoreCase = true)
                        val sdRoot = if (idx != -1) p.substring(0, idx) else p
                        return sdRoot
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Check /proc/mounts
        try {
            val mountsFile = File("/proc/mounts")
            if (mountsFile.exists() && mountsFile.canRead()) {
                val found = mountsFile.useLines { lines ->
                    lines.map { it.split("\\s+".toRegex()) }
                        .filter { it.size >= 2 }
                        .map { it[1] }
                        .firstOrNull { mp ->
                            mp.startsWith("/storage/") &&
                            !mp.startsWith("/storage/emulated") &&
                            !mp.startsWith("/storage/self") &&
                            mp != "/storage"
                        }
                }
                if (found != null) return found
            }
        } catch (_: Exception) {}

        // 4. Directly scan /storage mount directory
        try {
            val storageDir = File("/storage")
            if (storageDir.exists() && storageDir.isDirectory) {
                val subFiles = storageDir.listFiles()
                if (subFiles != null) {
                    for (file in subFiles) {
                        val name = file.name
                        if (file.isDirectory &&
                            name != "emulated" &&
                            name != "self" &&
                            name != "knox" &&
                            !name.startsWith(".")
                        ) {
                            return file.absolutePath
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return null
    }

    /**
     * Calculates storage stats (Total GB, Free GB) for the given SD card path or context.
     */
    fun getSdCardStats(context: Context, path: String?): Pair<Float, Float> {
        val resolvedPath = path ?: getSdCardPath(context)
        if (resolvedPath != null) {
            try {
                val stat = StatFs(resolvedPath)
                val totalGb = stat.totalBytes.toFloat() / (1024f * 1024f * 1024f)
                val freeGb = stat.availableBytes.toFloat() / (1024f * 1024f * 1024f)
                if (totalGb > 0f) return Pair(totalGb, freeGb)
            } catch (_: Exception) {}
        }

        // Secondary fallback: check ContextCompat.getExternalFilesDirs
        try {
            val dirs = ContextCompat.getExternalFilesDirs(context, null)
            val primaryRoot = Environment.getExternalStorageDirectory().absolutePath
            for (dir in dirs) {
                if (dir != null && !dir.path.contains("emulated") && !dir.path.startsWith(primaryRoot)) {
                    try {
                        val stat = StatFs(dir.path)
                        val totalGb = stat.totalBytes.toFloat() / (1024f * 1024f * 1024f)
                        val freeGb = stat.availableBytes.toFloat() / (1024f * 1024f * 1024f)
                        if (totalGb > 0f) return Pair(totalGb, freeGb)
                    } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}

        return Pair(0f, 0f)
    }

    /**
     * Determines whether the given location represents the SD Card.
     */
    fun isSdCardLocation(location: String, sdCardPath: String?): Boolean {
        if (location == "sd" || location.startsWith("sd:") || location.startsWith("sd/")) return true
        if (sdCardPath != null && (location == sdCardPath || location.startsWith("$sdCardPath/"))) return true
        if (location.startsWith("/storage/") && !location.startsWith("/storage/emulated") && !location.startsWith("/storage/self")) return true
        if (location.startsWith("/mnt/media_rw/")) return true
        return false
    }
}
