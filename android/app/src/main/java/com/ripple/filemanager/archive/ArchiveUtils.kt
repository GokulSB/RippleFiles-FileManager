package com.ripple.filemanager.archive

import com.ripple.filemanager.FileItem
import java.util.Locale

object ArchiveUtils {
    val ARCHIVE_EXTENSIONS = setOf(
        "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "zst", "zstd",
        "tgz", "tbz", "tbz2", "txz", "tzst", "tlz4", "tlz",
        "iso", "img", "wim", "swm", "esd",
        "cab", "arj", "lzh", "lha", "lz4", "lz5", "lizard", "lzma", "lzo", "z", "taz",
        "cpio", "rpm", "deb", "dmg", "hfs", "xar", "chm", "squashfs", "cramfs",
        "vhd", "vhdx", "vdi", "vmdk"
    )

    fun isArchiveExtension(ext: String): Boolean {
        return ext.lowercase(Locale.getDefault()).trim().trimStart('.') in ARCHIVE_EXTENSIONS
    }

    fun isArchive(nameOrPath: String?): Boolean {
        if (nameOrPath.isNullOrBlank()) return false
        val lower = nameOrPath.lowercase(Locale.getDefault()).trim()
        val ext = lower.substringAfterLast('.', "")
        if (ext in ARCHIVE_EXTENSIONS) return true
        if (Regex(""".*\.(tar\.[a-z0-9]+|001|z01|r00|r01)$""").matches(lower)) return true
        return false
    }

    fun isArchiveFile(file: FileItem?): Boolean {
        if (file == null) return false
        if (file.type == "archive") return true
        return isArchive(file.name) || isArchive(file.path)
    }
}
