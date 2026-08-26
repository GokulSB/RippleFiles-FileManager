package com.ripple.filemanager.data.sftp

/**
 * Connection config for an SFTP server. Supports password auth and private-key auth.
 * Password is NOT stored in this data class — read/written via EncryptedSharedPreferences.
 * privateKeyPem/privateKeyPassphrase are only used when authType == KEY.
 */
enum class SftpAuthType { PASSWORD, KEY }

data class SftpConnection(
    val id: String,
    val displayName: String,
    val host: String,
    val port: Int = 22,
    val username: String,
    val authType: SftpAuthType = SftpAuthType.PASSWORD,
    val privateKeyPem: String = "",        // raw PEM content, if authType == KEY
    val privateKeyPassphrase: String = "",
    val initialPath: String = "/",
    val hostKeyFingerprint: String? = null, // pinned host key (SHA256), null = accept-on-first-use
    val savedAt: Long
)
