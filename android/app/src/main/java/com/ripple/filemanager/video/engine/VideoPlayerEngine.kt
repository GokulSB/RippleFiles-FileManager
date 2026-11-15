package com.ripple.filemanager.video.engine

import android.net.Uri
import kotlinx.coroutines.flow.StateFlow
import org.videolan.libvlc.util.VLCVideoLayout

/**
 * Pluggable engine interface for video playback.
 * Allows Ripple Files to support libVLC as well as future alternative engines.
 */
interface VideoPlayerEngine {
    val playbackState: StateFlow<PlaybackState>
    val currentPosition: StateFlow<Long>
    val duration: StateFlow<Long>
    val audioTracks: StateFlow<List<VideoTrack>>
    val subtitleTracks: StateFlow<List<VideoTrack>>
    val selectedAudioTrackId: StateFlow<Int>
    val selectedSubtitleTrackId: StateFlow<Int>
    val speed: StateFlow<Float>
    val aspectRatio: StateFlow<VideoAspectRatio>
    val isSeekable: StateFlow<Boolean>
    val videoWidth: StateFlow<Int>
    val videoHeight: StateFlow<Int>
    val isPlaying: Boolean

    fun attachVideoLayout(layout: VLCVideoLayout)
    fun detachVideoLayout()

    fun open(source: VideoSource, options: VideoPlayerOptions = VideoPlayerOptions())
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMs: Long)

    fun setSpeed(speed: Float)
    fun setAspectRatio(ratio: VideoAspectRatio)

    fun setAudioTrack(trackId: Int): Boolean
    fun setAudioDelay(delayMs: Long): Boolean
    fun getAudioDelay(): Long
    fun addExternalAudio(uri: Uri): Boolean

    fun setSubtitleTrack(trackId: Int): Boolean
    fun setSubtitleDelay(delayMs: Long): Boolean
    fun getSubtitleDelay(): Long
    fun addSubtitle(uri: Uri, select: Boolean = true): Boolean

    fun setHardwareAcceleration(mode: HwAccelerationMode)
    fun setSubtitleEncoding(encoding: String)

    fun release()
}
