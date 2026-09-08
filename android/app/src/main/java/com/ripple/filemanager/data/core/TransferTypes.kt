package com.ripple.filemanager.data.core

enum class TransferMode { COPY, MOVE }

sealed class ConflictResolution {
    /** Upload as normal. For path-based providers (SMB/FTP/SFTP/WebDAV/Local)
     * this naturally replaces the existing file. For Drive and MEGA, which
     * have no true "replace" operation, the ViewModel's transfer engine
     * finds and deletes the existing same-named item first so the result
     * still ends up looking like a genuine overwrite. */
    object Overwrite : ConflictResolution()

    /** Upload under a different name instead of the original. */
    data class Rename(val newName: String) : ConflictResolution()
}
