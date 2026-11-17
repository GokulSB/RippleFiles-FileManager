package com.ripple.filemanager.video.engine

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.libvlc.util.VLCVideoLayout
import java.io.File

/**
 * libVLC implementation of VideoPlayerEngine.
 * Handles VLC lifecycle, SurfaceView binding via VLCVideoLayout,
 * audio/subtitle track management, delays, aspect ratios and hardware acceleration.
 */
class LibVlcPlayerEngine(
    private val context: Context,
    subtitleStyle: SubtitleStyleConfig = SubtitleStyleConfig()
) : VideoPlayerEngine {

    companion object {
        private const val TAG = "LibVlcPlayerEngine"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var libVLC: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentMedia: Media? = null
    private var pfd: ParcelFileDescriptor? = null
    private var attachedLayout: VLCVideoLayout? = null
    private var areViewsAttached = false
    private var pendingSeekPositionMs: Long = 0L

    private var currentSource: VideoSource? = null
    private var currentOptions: VideoPlayerOptions = VideoPlayerOptions()
    private var currentSubtitleStyle: SubtitleStyleConfig = subtitleStyle

    // State flows
    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    override val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    override val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<VideoTrack>>(emptyList())
    override val audioTracks: StateFlow<List<VideoTrack>> = _audioTracks.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<VideoTrack>>(emptyList())
    override val subtitleTracks: StateFlow<List<VideoTrack>> = _subtitleTracks.asStateFlow()

    private val _selectedAudioTrackId = MutableStateFlow(-1)
    override val selectedAudioTrackId: StateFlow<Int> = _selectedAudioTrackId.asStateFlow()

    private val _selectedSubtitleTrackId = MutableStateFlow(-1)
    override val selectedSubtitleTrackId: StateFlow<Int> = _selectedSubtitleTrackId.asStateFlow()

    private val _speed = MutableStateFlow(1.0f)
    override val speed: StateFlow<Float> = _speed.asStateFlow()

    private val _aspectRatio = MutableStateFlow(VideoAspectRatio.FIT)
    override val aspectRatio: StateFlow<VideoAspectRatio> = _aspectRatio.asStateFlow()

    private val _isSeekable = MutableStateFlow(true)
    override val isSeekable: StateFlow<Boolean> = _isSeekable.asStateFlow()

    private val _videoWidth = MutableStateFlow(0)
    override val videoWidth: StateFlow<Int> = _videoWidth.asStateFlow()

    private val _videoHeight = MutableStateFlow(0)
    override val videoHeight: StateFlow<Int> = _videoHeight.asStateFlow()

    override val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying == true || _playbackState.value is PlaybackState.Playing

    private val externalSubtitles = mutableListOf<VideoTrack>()
    private val externalAudioTracks = mutableListOf<VideoTrack>()

    init {
        initLibVlc()
    }

    private fun buildVlcOptions(
        hw: HwAccelerationMode = currentOptions.hwAcceleration,
        encoding: String = currentOptions.subtitleEncoding,
        style: SubtitleStyleConfig = currentSubtitleStyle
    ): ArrayList<String> {
        val options = ArrayList<String>()
        options.add("--audio-time-stretch")
        options.add("--file-caching=2000")
        options.add("--network-caching=3000")
        options.add("--drop-late-frames")
        options.add("--skip-frames")
        options.add("--no-stats")

        when (hw) {
            HwAccelerationMode.AUTO -> {
                // Let VLC probe and choose MediaCodec
            }
            HwAccelerationMode.ON -> {
                options.add("--avcodec-hw=any")
            }
            HwAccelerationMode.OFF -> {
                options.add("--avcodec-hw=none")
            }
        }

        if (encoding.isNotBlank()) {
            options.add("--subsdec-encoding=$encoding")
        }

        // Subtitle rendering style
        options.add("--freetype-fontsize=${style.size.ptSize}")
        options.add("--freetype-color=${style.color.colorHex}")
        options.add("--freetype-outline-thickness=${style.outline.thickness}")
        options.add("--freetype-background-opacity=${style.background.opacity}")
        if (style.bold) {
            options.add("--freetype-bold")
        } else {
            options.add("--no-freetype-bold")
        }
        if (style.verticalMarginPx > 0) {
            options.add("--sub-margin=${style.verticalMarginPx}")
        }

        return options
    }

    private fun initLibVlc(
        hw: HwAccelerationMode = currentOptions.hwAcceleration,
        encoding: String = currentOptions.subtitleEncoding,
        style: SubtitleStyleConfig = currentSubtitleStyle
    ) {
        try {
            val options = buildVlcOptions(hw, encoding, style)
            val oldVlc = libVLC
            libVLC = null
            if (oldVlc != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        oldVlc.release()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error releasing old LibVLC in background", e)
                    }
                }
            }
            libVLC = LibVLC(context.applicationContext, options)
            createMediaPlayer()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing LibVLC", e)
            _playbackState.value = PlaybackState.Error("Failed to initialize video engine: ${e.message}")
        }
    }

    private fun createMediaPlayer() {
        val vlc = libVLC ?: return
        val oldMp = mediaPlayer
        mediaPlayer = null

        oldMp?.let { mp ->
            try {
                mp.setEventListener(null)
                if (areViewsAttached) {
                    mp.detachViews()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error detaching previous player", e)
            }
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    mp.stop()
                    mp.release()
                } catch (e: Exception) {
                    Log.w(TAG, "Error releasing previous player in background", e)
                }
            }
        }

        areViewsAttached = false
        mediaPlayer = MediaPlayer(vlc).apply {
            setEventListener { event ->
                handleMediaPlayerEvent(event)
            }
        }

        attachedLayout?.let { layout ->
            try {
                mediaPlayer?.attachViews(layout, null, true, true)
                areViewsAttached = true
                applyScaleType(_aspectRatio.value)
            } catch (e: Exception) {
                Log.w(TAG, "Error attaching views in createMediaPlayer", e)
            }
        }
    }

    private fun handleMediaPlayerEvent(event: MediaPlayer.Event) {
        // Extract values synchronously on the Main Looper before EventRunnable releases the event!
        val eventType = event.type
        val eventTime = try { event.timeChanged } catch (_: Exception) { -1L }
        val eventLength = try { event.lengthChanged } catch (_: Exception) { -1L }
        val eventBuffering = try { event.buffering } catch (_: Exception) { 0f }
        val eventSeekable = try { event.seekable } catch (_: Exception) { true }

        when (eventType) {
            MediaPlayer.Event.Buffering -> {
                if (_playbackState.value !is PlaybackState.Playing) {
                    _playbackState.value = PlaybackState.Buffering(eventBuffering)
                }
            }
            MediaPlayer.Event.Playing -> {
                _playbackState.value = PlaybackState.Playing
                if (pendingSeekPositionMs > 0L) {
                    val target = pendingSeekPositionMs
                    pendingSeekPositionMs = 0L
                    try {
                        mediaPlayer?.time = target
                        _currentPosition.value = target
                    } catch (_: Exception) {}
                }
                updateTracks()
                val len = try { mediaPlayer?.length ?: 0L } catch (_: Exception) { 0L }
                if (len > 0L) _duration.value = len
                val currTime = try { mediaPlayer?.time ?: 0L } catch (_: Exception) { 0L }
                if (currTime >= 0L) _currentPosition.value = currTime
            }
            MediaPlayer.Event.Paused -> {
                _playbackState.value = PlaybackState.Paused
            }
            MediaPlayer.Event.Stopped -> {
                if (_playbackState.value !is PlaybackState.Ended && _playbackState.value !is PlaybackState.Error) {
                    _playbackState.value = PlaybackState.Idle
                }
            }
            MediaPlayer.Event.EndReached -> {
                _playbackState.value = PlaybackState.Ended
                if (_duration.value > 0L) {
                    _currentPosition.value = _duration.value
                }
            }
            MediaPlayer.Event.EncounteredError -> {
                Log.e(TAG, "libVLC encountered error during playback")
                _playbackState.value = PlaybackState.Error(
                    "Cannot play this media file. It may be corrupt, unsupported, or DRM-protected.",
                    isDrm = true
                )
            }
            MediaPlayer.Event.TimeChanged -> {
                if (eventTime >= 0L) {
                    _currentPosition.value = eventTime
                }
            }
            MediaPlayer.Event.LengthChanged -> {
                if (eventLength > 0L) {
                    _duration.value = eventLength
                }
            }
            MediaPlayer.Event.PositionChanged -> {
                // Position fraction updated
            }
            MediaPlayer.Event.SeekableChanged -> {
                _isSeekable.value = eventSeekable
            }
            MediaPlayer.Event.Vout -> {
                // Update video dimensions safely
                try {
                    mediaPlayer?.currentVideoTrack?.let { vt ->
                        if (vt.width > 0 && vt.height > 0) {
                            _videoWidth.value = vt.width
                            _videoHeight.value = vt.height
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error getting video dimensions on Vout", e)
                }
            }
            MediaPlayer.Event.ESAdded,
            MediaPlayer.Event.ESDeleted,
            MediaPlayer.Event.ESSelected -> {
                updateTracks()
            }
        }
    }

    private fun updateTracks() {
        val mp = mediaPlayer ?: return
        if (mp.isReleased) return
        try {
            // Audio tracks
            val aTracks = mutableListOf<VideoTrack>()
            val selectedAudio = try { mp.audioTrack } catch (_: Exception) { -1 }
            _selectedAudioTrackId.value = selectedAudio
            try {
                mp.audioTracks?.forEach { td ->
                    if (td != null) {
                        aTracks.add(
                            VideoTrack(
                                id = td.id,
                                name = td.name ?: "Track ${td.id}",
                                type = VideoTrackType.AUDIO,
                                isSelected = (td.id == selectedAudio)
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error reading audio tracks", e)
            }
            aTracks.addAll(externalAudioTracks)
            _audioTracks.value = aTracks

            // Subtitle tracks
            val sTracks = mutableListOf<VideoTrack>()
            val selectedSpu = try { mp.spuTrack } catch (_: Exception) { -1 }
            _selectedSubtitleTrackId.value = selectedSpu
            try {
                mp.spuTracks?.forEach { td ->
                    if (td != null) {
                        sTracks.add(
                            VideoTrack(
                                id = td.id,
                                name = if (td.id == -1) "Off" else (td.name ?: "Track ${td.id}"),
                                type = VideoTrackType.SUBTITLE,
                                isSelected = (td.id == selectedSpu)
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error reading subtitle tracks", e)
            }
            sTracks.addAll(externalSubtitles)
            _subtitleTracks.value = sTracks
        } catch (e: Exception) {
            Log.w(TAG, "Error updating tracks", e)
        }
    }

    override fun attachVideoLayout(layout: VLCVideoLayout) {
        if (attachedLayout == layout && areViewsAttached) {
            return
        }
        attachedLayout = layout
        mediaPlayer?.let { mp ->
            if (mp.isReleased) return
            try {
                if (areViewsAttached) {
                    mp.detachViews()
                    areViewsAttached = false
                }
                mp.attachViews(layout, null, true, true)
                areViewsAttached = true
                applyScaleType(_aspectRatio.value)
            } catch (e: Exception) {
                Log.e(TAG, "Error attaching video layout", e)
            }
        }
    }

    override fun detachVideoLayout() {
        attachedLayout = null
        areViewsAttached = false
        try {
            mediaPlayer?.let { mp ->
                if (!mp.isReleased) {
                    mp.detachViews()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error detaching views", e)
        }
    }

    override fun open(source: VideoSource, options: VideoPlayerOptions) {
        currentSource = source
        currentOptions = options
        externalSubtitles.clear()
        externalAudioTracks.clear()
        _duration.value = source.durationMs
        _currentPosition.value = options.startPositionMs

        val vlc = libVLC ?: run {
            initLibVlc(options.hwAcceleration, options.subtitleEncoding)
            libVLC ?: return
        }

        val oldMedia = currentMedia
        val oldPfd = pfd
        currentMedia = null
        pfd = null
        if (oldMedia != null || oldPfd != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try { oldMedia?.release() } catch (_: Exception) {}
                try { oldPfd?.close() } catch (_: Exception) {}
            }
        }

        try {

            val media: Media = when {
                source.path != null && File(source.path).exists() -> {
                    Media(vlc, source.path)
                }
                source.uri.scheme == "file" && source.uri.path != null && File(source.uri.path!!).exists() -> {
                    Media(vlc, source.uri.path!!)
                }
                source.uri.scheme == "content" -> {
                    try {
                        val descriptor = context.contentResolver.openFileDescriptor(source.uri, "r")
                        if (descriptor != null) {
                            pfd = descriptor
                            Media(vlc, descriptor.fileDescriptor)
                        } else {
                            Media(vlc, source.uri)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not open FileDescriptor for ${source.uri}, using URI directly", e)
                        Media(vlc, source.uri)
                    }
                }
                else -> {
                    Media(vlc, source.uri)
                }
            }

            // Configure HW decoding
            when (options.hwAcceleration) {
                HwAccelerationMode.AUTO -> media.setHWDecoderEnabled(true, false)
                HwAccelerationMode.ON -> media.setHWDecoderEnabled(true, true)
                HwAccelerationMode.OFF -> media.setHWDecoderEnabled(false, false)
            }

            media.parseAsync(IMedia.Parse.ParseLocal)
            currentMedia = media

            val mp = mediaPlayer ?: run {
                createMediaPlayer()
                mediaPlayer ?: return
            }

            mp.media = media

            if (options.speed != 1.0f) {
                mp.rate = options.speed
                _speed.value = options.speed
            }

            if (options.audioDelayMs != 0L) {
                mp.setAudioDelay(options.audioDelayMs * 1000L)
            }
            if (options.subtitleDelayMs != 0L) {
                mp.setSpuDelay(options.subtitleDelayMs * 1000L)
            }

            if (!areViewsAttached) {
                attachedLayout?.let { layout ->
                    try {
                        mp.attachViews(layout, null, true, true)
                        areViewsAttached = true
                        applyScaleType(_aspectRatio.value)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error ensuring views attached in open", e)
                    }
                }
            } else {
                applyScaleType(_aspectRatio.value)
            }

            if (options.startPositionMs > 0L) {
                val startSec = options.startPositionMs / 1000.0
                media.addOption(String.format(java.util.Locale.US, ":start-time=%.3f", startSec))
                pendingSeekPositionMs = options.startPositionMs
            } else {
                pendingSeekPositionMs = 0L
            }

            _playbackState.value = PlaybackState.Playing
            mp.play()
            if (options.startPositionMs > 0L) {
                try {
                    mp.time = options.startPositionMs
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening media source: ${source.uri}", e)
            _playbackState.value = PlaybackState.Error("Error opening video: ${e.message}")
        }
    }

    override fun play() {
        val mp = mediaPlayer ?: return
        if (mp.isReleased) return
        try {
            if (_playbackState.value is PlaybackState.Ended) {
                val src = currentSource
                val maxDur = _duration.value.coerceAtLeast(0L)
                val target = if (maxDur > 0L && _currentPosition.value >= maxDur - 300L) 0L else _currentPosition.value
                if (src != null) {
                    open(src, currentOptions.copy(startPositionMs = target))
                } else {
                    pendingSeekPositionMs = target
                    _playbackState.value = PlaybackState.Playing
                    mp.stop()
                    mp.play()
                }
            } else {
                _playbackState.value = PlaybackState.Playing
                mp.play()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing", e)
        }
    }

    override fun pause() {
        val mp = mediaPlayer ?: return
        if (mp.isReleased) return
        try {
            _playbackState.value = PlaybackState.Paused
            mp.pause()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing", e)
        }
    }

    override fun stop() {
        val mp = mediaPlayer ?: return
        if (mp.isReleased) return
        _playbackState.value = PlaybackState.Idle
        CoroutineScope(Dispatchers.IO).launch {
            try {
                mp.stop()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping in background", e)
            }
        }
    }

    override fun seekTo(positionMs: Long) {
        val mp = mediaPlayer ?: return
        if (mp.isReleased) return
        try {
            val maxDur = _duration.value.coerceAtLeast(0L)
            val clamped = if (maxDur > 0L) positionMs.coerceIn(0L, maxDur) else positionMs.coerceAtLeast(0L)
            _currentPosition.value = clamped

            val isEnded = _playbackState.value is PlaybackState.Ended ||
                    try { mp.playerState == MediaPlayer.Event.EndReached } catch (_: Exception) { false }

            if (isEnded) {
                // Replay smoothly from scrubbed position if playback previously ended
                val src = currentSource
                if (src != null) {
                    open(src, currentOptions.copy(startPositionMs = clamped))
                } else {
                    pendingSeekPositionMs = clamped
                    _playbackState.value = PlaybackState.Playing
                    mp.stop()
                    mp.play()
                }
            } else {
                // Standard precision seek prevents MediaCodec hardware buffer corruption / crashes
                mp.time = clamped
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking to $positionMs", e)
        }
    }

    override fun setSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 4.0f)
        try {
            mediaPlayer?.rate = clamped
            _speed.value = clamped
        } catch (e: Exception) {
            Log.e(TAG, "Error setting speed", e)
        }
    }

    override fun setAspectRatio(ratio: VideoAspectRatio) {
        _aspectRatio.value = ratio
        applyScaleType(ratio)
    }

    private fun applyScaleType(ratio: VideoAspectRatio) {
        val mp = mediaPlayer ?: return
        try {
            when (ratio) {
                VideoAspectRatio.FIT -> {
                    mp.aspectRatio = null
                    mp.videoScale = MediaPlayer.ScaleType.SURFACE_BEST_FIT
                }
                VideoAspectRatio.FILL -> {
                    mp.aspectRatio = null
                    mp.videoScale = MediaPlayer.ScaleType.SURFACE_FILL
                }
                VideoAspectRatio.ZOOM -> {
                    mp.aspectRatio = null
                    mp.videoScale = MediaPlayer.ScaleType.SURFACE_FIT_SCREEN
                }
                VideoAspectRatio.RATIO_16_9 -> {
                    mp.videoScale = MediaPlayer.ScaleType.SURFACE_16_9
                    mp.aspectRatio = "16:9"
                }
                VideoAspectRatio.RATIO_4_3 -> {
                    mp.videoScale = MediaPlayer.ScaleType.SURFACE_4_3
                    mp.aspectRatio = "4:3"
                }
                VideoAspectRatio.ORIGINAL -> {
                    mp.aspectRatio = null
                    mp.videoScale = MediaPlayer.ScaleType.SURFACE_ORIGINAL
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying scale type $ratio", e)
        }
    }

    override fun setAudioTrack(trackId: Int): Boolean {
        val mp = mediaPlayer ?: return false
        return try {
            val success = mp.setAudioTrack(trackId)
            if (success) {
                _selectedAudioTrackId.value = trackId
                updateTracks()
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error setting audio track $trackId", e)
            false
        }
    }

    override fun setAudioDelay(delayMs: Long): Boolean {
        val mp = mediaPlayer ?: return false
        return try {
            mp.setAudioDelay(delayMs * 1000L)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting audio delay", e)
            false
        }
    }

    override fun getAudioDelay(): Long {
        val mp = mediaPlayer ?: return 0L
        return try {
            mp.audioDelay / 1000L
        } catch (e: Exception) {
            0L
        }
    }

    override fun addExternalAudio(uri: Uri): Boolean {
        val mp = mediaPlayer ?: return false
        return try {
            val uriStr = if (uri.scheme == "file") uri.path ?: uri.toString() else uri.toString()
            val added = mp.addSlave(IMedia.Slave.Type.Audio, uri, true)
            if (added) {
                val displayName = uri.lastPathSegment ?: "External Audio"
                externalAudioTracks.add(
                    VideoTrack(
                        id = 9000 + externalAudioTracks.size,
                        name = displayName,
                        type = VideoTrackType.AUDIO,
                        isExternal = true,
                        uri = uri,
                        isSelected = true
                    )
                )
                updateTracks()
            }
            added
        } catch (e: Exception) {
            Log.e(TAG, "Error adding external audio: $uri", e)
            false
        }
    }

    override fun setSubtitleTrack(trackId: Int): Boolean {
        val mp = mediaPlayer ?: return false
        return try {
            val success = mp.setSpuTrack(trackId)
            if (success) {
                _selectedSubtitleTrackId.value = trackId
                updateTracks()
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error setting subtitle track $trackId", e)
            false
        }
    }

    override fun setSubtitleDelay(delayMs: Long): Boolean {
        val mp = mediaPlayer ?: return false
        return try {
            mp.setSpuDelay(delayMs * 1000L)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting subtitle delay", e)
            false
        }
    }

    override fun getSubtitleDelay(): Long {
        val mp = mediaPlayer ?: return 0L
        return try {
            mp.spuDelay / 1000L
        } catch (e: Exception) {
            0L
        }
    }

    override fun addSubtitle(uri: Uri, select: Boolean): Boolean {
        val mp = mediaPlayer ?: return false
        return try {
            val uriStr = if (uri.scheme == "file") uri.path ?: uri.toString() else uri.toString()
            val added = mp.addSlave(IMedia.Slave.Type.Subtitle, uri, select)
            if (added) {
                val displayName = uri.lastPathSegment ?: "External Subtitle"
                externalSubtitles.add(
                    VideoTrack(
                        id = 8000 + externalSubtitles.size,
                        name = displayName,
                        type = VideoTrackType.SUBTITLE,
                        isExternal = true,
                        uri = uri,
                        isSelected = select
                    )
                )
                updateTracks()
            }
            added
        } catch (e: Exception) {
            Log.e(TAG, "Error adding subtitle: $uri", e)
            false
        }
    }

    override fun setHardwareAcceleration(mode: HwAccelerationMode) {
        if (currentOptions.hwAcceleration == mode) return
        currentOptions = currentOptions.copy(hwAcceleration = mode)
        val pos = _currentPosition.value
        val src = currentSource
        if (src != null) {
            initLibVlc(mode, currentOptions.subtitleEncoding, currentSubtitleStyle)
            open(src, currentOptions.copy(startPositionMs = pos))
        }
    }

    override fun setSubtitleEncoding(encoding: String) {
        if (currentOptions.subtitleEncoding == encoding) return
        currentOptions = currentOptions.copy(subtitleEncoding = encoding)
        val pos = _currentPosition.value
        val src = currentSource
        if (src != null) {
            initLibVlc(currentOptions.hwAcceleration, encoding, currentSubtitleStyle)
            open(src, currentOptions.copy(startPositionMs = pos))
        }
    }

    fun updateSubtitleStyle(style: SubtitleStyleConfig) {
        currentSubtitleStyle = style
        val pos = _currentPosition.value
        val src = currentSource
        if (src != null) {
            initLibVlc(currentOptions.hwAcceleration, currentOptions.subtitleEncoding, style)
            open(src, currentOptions.copy(startPositionMs = pos))
        }
    }

    override fun release() {
        scope.cancel()
        val layout = attachedLayout
        val mp = mediaPlayer
        val media = currentMedia
        val vlc = libVLC
        val fd = pfd

        attachedLayout = null
        areViewsAttached = false
        mediaPlayer = null
        currentMedia = null
        libVLC = null
        pfd = null

        try {
            mp?.setEventListener(null)
            if (layout != null) {
                mp?.detachViews()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error detaching views during release", e)
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                mp?.stop()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping mediaPlayer in background", e)
            }
            try {
                mp?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing mediaPlayer in background", e)
            }
            try {
                media?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing media in background", e)
            }
            try {
                fd?.close()
            } catch (_: Exception) {}
            try {
                vlc?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing LibVLC in background", e)
            }
        }
    }
}

data class SubtitleStyleConfig(
    val size: SubtitleSize = SubtitleSize.NORMAL,
    val color: SubtitleColor = SubtitleColor.WHITE,
    val background: SubtitleBackground = SubtitleBackground.NONE,
    val outline: SubtitleOutline = SubtitleOutline.NORMAL,
    val bold: Boolean = true,
    val verticalMarginPx: Int = 20
)
