package com.ripple.filemanager.video

import android.content.Context
import com.ripple.filemanager.video.engine.HwAccelerationMode

class VideoPlayerPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("ripple_video_player_prefs", Context.MODE_PRIVATE)

    var hwAcceleration: HwAccelerationMode
        get() {
            val name = prefs.getString("hw_acceleration", HwAccelerationMode.AUTO.name)
            return try { HwAccelerationMode.valueOf(name!!) } catch (_: Exception) { HwAccelerationMode.AUTO }
        }
        set(value) = prefs.edit().putString("hw_acceleration", value.name).apply()

    var resumePlayback: Boolean
        get() = prefs.getBoolean("resume_playback", true)
        set(value) = prefs.edit().putBoolean("resume_playback", value).apply()

    var defaultSpeed: Float
        get() = prefs.getFloat("default_speed", 1.0f)
        set(value) = prefs.edit().putFloat("default_speed", value).apply()

    var defaultOrientation: String
        get() = prefs.getString("default_orientation", "sensor") ?: "sensor"
        set(value) = prefs.edit().putString("default_orientation", value).apply()

    var gestureDoubleTapSeek: Boolean
        get() = prefs.getBoolean("gesture_double_tap_seek", true)
        set(value) = prefs.edit().putBoolean("gesture_double_tap_seek", value).apply()

    var gestureSwipeSeek: Boolean
        get() = prefs.getBoolean("gesture_swipe_seek", true)
        set(value) = prefs.edit().putBoolean("gesture_swipe_seek", value).apply()

    var gestureSwipeBrightnessVolume: Boolean
        get() = prefs.getBoolean("gesture_swipe_bv", true)
        set(value) = prefs.edit().putBoolean("gesture_swipe_bv", value).apply()

    var gesturePinchZoom: Boolean
        get() = prefs.getBoolean("gesture_pinch_zoom", true)
        set(value) = prefs.edit().putBoolean("gesture_pinch_zoom", value).apply()

    var gestureLongPressSpeed: Boolean
        get() = prefs.getBoolean("gesture_long_press_speed", true)
        set(value) = prefs.edit().putBoolean("gesture_long_press_speed", value).apply()

    var backgroundPlay: Boolean
        get() = prefs.getBoolean("background_play", false)
        set(value) = prefs.edit().putBoolean("background_play", value).apply()

    var pictureInPicture: Boolean
        get() = prefs.getBoolean("pip_enabled", true)
        set(value) = prefs.edit().putBoolean("pip_enabled", value).apply()

    fun savePosition(fileKey: String, positionMs: Long, durationMs: Long) {
        if (!resumePlayback) return
        val editor = prefs.edit()
        val key = "resume_${fileKey.hashCode()}"
        if (positionMs > 3000L && (durationMs <= 0L || positionMs < durationMs - 5000L)) {
            editor.putLong(key, positionMs)
        } else {
            editor.remove(key)
        }
        editor.apply()
    }

    fun getSavedPosition(fileKey: String): Long {
        if (!resumePlayback) return 0L
        val key = "resume_${fileKey.hashCode()}"
        return prefs.getLong(key, 0L)
    }

    fun clearSavedPosition(fileKey: String) {
        val key = "resume_${fileKey.hashCode()}"
        prefs.edit().remove(key).apply()
    }
}
