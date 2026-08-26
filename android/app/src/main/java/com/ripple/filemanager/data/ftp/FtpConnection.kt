package com.ripple.filemanager.data.ftp

/**
 * Connection config for an FTP/FTPS server.
 * Mirrors the shape of SmbConnection so it can be persisted and edited the same way.
 * Password is NOT stored in this data class — read/written via EncryptedSharedPreferences.
 */
data class FtpConnection(
    val id: String,
    val displayName: String,
    val host: String,
    val port: Int = 21,
    val username: String,
    val useFtps: Boolean = false,       // FTP over explicit TLS (AUTH TLS)
    val useImplicitFtps: Boolean = false, // legacy implicit TLS on port 990
    val initialPath: String = "/",
    val passiveMode: Boolean = true,
    val savedAt: Long
)
