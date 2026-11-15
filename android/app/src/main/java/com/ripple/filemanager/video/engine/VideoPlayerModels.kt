package com.ripple.filemanager.video.engine

import android.net.Uri

sealed class PlaybackState {
    object Idle : PlaybackState()
    data class Buffering(val percent: Float = 0f) : PlaybackState()
    object Playing : PlaybackState()
    object Paused : PlaybackState()
    object Ended : PlaybackState()
    data class Error(val message: String, val isDrm: Boolean = false) : PlaybackState()
}

enum class VideoTrackType {
    AUDIO,
    SUBTITLE,
    VIDEO
}

data class VideoTrack(
    val id: Int,
    val name: String,
    val type: VideoTrackType,
    val language: String? = null,
    val isSelected: Boolean = false,
    val isExternal: Boolean = false,
    val uri: Uri? = null
)

enum class VideoAspectRatio(val label: String) {
    FIT("Fit"),
    FILL("Fill"),
    ZOOM("Zoom"),
    RATIO_16_9("16:9"),
    RATIO_4_3("4:3"),
    ORIGINAL("Original")
}

enum class HwAccelerationMode(val label: String) {
    AUTO("Auto"),
    ON("On"),
    OFF("Off")
}

enum class SubtitleColor(val label: String, val colorHex: Long) {
    WHITE("White", 0x00FFFFFF),
    YELLOW("Yellow", 0x00FFFF00),
    CYAN("Cyan", 0x0000FFFF),
    GREEN("Green", 0x0000FF00)
}

enum class SubtitleBackground(val label: String, val opacity: Int) {
    NONE("None", 0),
    SEMI("Semi-transparent", 128),
    SOLID("Solid Black", 255)
}

enum class SubtitleOutline(val label: String, val thickness: Int) {
    NONE("None", 0),
    THIN("Thin", 1),
    NORMAL("Normal", 2),
    THICK("Thick", 4)
}

enum class SubtitleSize(val label: String, val scale: Float, val ptSize: Int) {
    SMALL("Small", 0.75f, 18),
    NORMAL("Normal", 1.0f, 24),
    LARGE("Large", 1.25f, 32),
    HUGE("Huge", 1.5f, 40)
}

data class SubtitleEncoding(val displayName: String, val charsetName: String)

val SUPPORTED_SUBTITLE_ENCODINGS = listOf(
    SubtitleEncoding("Auto-detect", ""),
    SubtitleEncoding("UTF-8", "UTF-8"),
    SubtitleEncoding("UTF-16", "UTF-16"),
    SubtitleEncoding("Western European (Windows-1252)", "WINDOWS-1252"),
    SubtitleEncoding("Cyrillic (Windows-1251)", "WINDOWS-1251"),
    SubtitleEncoding("Central European (Windows-1250)", "WINDOWS-1250"),
    SubtitleEncoding("Arabic (Windows-1256)", "WINDOWS-1256"),
    SubtitleEncoding("Simplified Chinese (GBK)", "GBK"),
    SubtitleEncoding("Traditional Chinese (Big5)", "BIG5"),
    SubtitleEncoding("Japanese (Shift-JIS)", "SHIFT-JIS"),
    SubtitleEncoding("Korean (EUC-KR)", "EUC-KR")
)

data class VideoSource(
    val uri: Uri,
    val title: String,
    val path: String? = null,
    val mimeType: String? = null,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L
)

data class VideoPlayerOptions(
    val hwAcceleration: HwAccelerationMode = HwAccelerationMode.AUTO,
    val startPositionMs: Long = 0L,
    val speed: Float = 1.0f,
    val subtitleEncoding: String = "",
    val subtitleDelayMs: Long = 0L,
    val audioDelayMs: Long = 0L
)
