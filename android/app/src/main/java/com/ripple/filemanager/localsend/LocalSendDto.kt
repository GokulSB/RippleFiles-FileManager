package com.ripple.filemanager.localsend

import kotlinx.coroutines.CompletableDeferred

/**
 * LocalSend Protocol v2 DTOs and Transfer Models.
 * Matches https://github.com/localsend/protocol
 */

data class DeviceDto(
    val alias: String,
    val version: String = "2.0",
    val deviceModel: String? = null,
    val deviceType: String? = "mobile", // mobile | desktop | web | headless | server
    val fingerprint: String,
    val port: Int = 53317,
    val protocol: String = "https", // https | http
    val download: Boolean = false,
    val announce: Boolean? = null
)

data class PrepareUploadRequestDto(
    val info: DeviceDto,
    val files: Map<String, FileDto>
)

data class FileDto(
    val id: String,
    val fileName: String,
    val size: Long,
    val fileType: String,
    val sha256: String? = null,
    val preview: String? = null,
    val metadata: FileMetadataDto? = null
)

data class FileMetadataDto(
    val modified: String? = null,
    val accessed: String? = null
)

data class PrepareUploadResponseDto(
    val sessionId: String,
    val files: Map<String, String> // fileId to token
)

data class NearbyPeer(
    val ip: String,
    val port: Int,
    val alias: String,
    val deviceModel: String?,
    val deviceType: String,
    val protocol: String,
    val fingerprint: String,
    val lastSeenMs: Long
) {
    val id: String get() = "$ip:$port"
    val baseUrl: String get() = "$protocol://$ip:$port"
}

enum class TransferStatus {
    INITIALIZING,
    WAITING_ACCEPT,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    REJECTED,
    FAILED
}

data class TransferFileItem(
    val fileId: String,
    val token: String? = null,
    val fileName: String,
    val size: Long,
    val fileType: String,
    val localPath: String? = null,
    val bytesTransferred: Long = 0L,
    val isFinished: Boolean = false
)

data class TransferSession(
    val sessionId: String,
    val peer: NearbyPeer,
    val files: List<TransferFileItem>,
    val isIncoming: Boolean,
    val status: TransferStatus,
    val currentFileIndex: Int = 0,
    val bytesTransferred: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val errorMessage: String? = null
) {
    val progress: Float
        get() = if (totalBytes > 0) (bytesTransferred.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

data class IncomingTransferRequest(
    val sessionId: String,
    val sender: DeviceDto,
    val senderIp: String,
    val files: List<TransferFileItem>,
    val totalBytes: Long,
    val decisionChannel: CompletableDeferred<Boolean>
)
