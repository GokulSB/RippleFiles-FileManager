package com.ripple.filemanager.data.smb

data class SmbConnection(
    val id: String,          // UUID
    val displayName: String, // user-facing label, e.g. "Bedroom NAS"
    val host: String,
    val port: Int = 445,
    val shareName: String,
    val domain: String = "",
    val username: String,
    // password is NOT stored in this data class — read/written via EncryptedCredentialStore
    val savedAt: Long
)
