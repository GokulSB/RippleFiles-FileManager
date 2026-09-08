package com.ripple.filemanager.data.core

import com.ripple.filemanager.FileItem
import java.io.File

/**
 * Common interface that every storage provider (Local, SMB, FTP, SFTP,
 * WebDAV/Nextcloud, Google Drive, MEGA) implements.
 *
 * This lets any part of the app — especially the future dual-pane screen —
 * work with "a source of files" without caring what kind of connection it is.
 *
 * Design note: cross-source transfers (e.g. SMB -> Google Drive) go through
 * a local temp file: downloadToLocal() on the source, then uploadFromLocal()
 * on the destination. This matches what SMB/FTP/SFTP/WebDAV already do
 * internally, so no provider needs to change its core logic.
 */
interface FileSource {

    /** A short label for UI/logging, e.g. "SMB", "FTP", "Local". */
    val sourceLabel: String

    suspend fun listFiles(path: String): Result<List<FileItem>>

    suspend fun downloadToLocal(path: String, destination: File): Result<File>

    /**
     * destinationPath is always the TARGET FOLDER (not the full file path
     * including a filename) — the adapter is responsible for placing the
     * file inside it, typically using localFile.name. This matches how
     * Drive and MEGA naturally work (they only have folder IDs, not real
     * paths), so every adapter follows the same rule for consistency.
     */
    suspend fun uploadFromLocal(localFile: File, destinationPath: String): Result<Unit>

    /**
     * isDirectory is optional: some providers (SMB) can figure this out
     * themselves; others (FTP) need to be told upfront which delete call to make.
     */
    suspend fun delete(path: String, isDirectory: Boolean = false): Result<Unit>

    suspend fun createFolder(path: String): Result<Unit>
}
