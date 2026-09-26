package com.ripple.filemanager.ui

import android.util.Log
import com.ripple.filemanager.BuildConfig
import java.util.concurrent.ConcurrentHashMap

/**
 * Diagnostic tracker that asserts the active composed screen count never exceeds 2
 * during screen transitions (at most 1 outgoing + 1 incoming in flight, exactly 1 when settled).
 */
object ScreenCompositionTracker {
    private const val TAG = "ScreenComposition"
    private val activeScreens = ConcurrentHashMap.newKeySet<String>()

    fun onScreenEnter(screen: String) {
        activeScreens.add(screen)
        val count = activeScreens.size
        Log.d(TAG, "Screen ENTER: $screen (Active composed screens: $count -> $activeScreens)")
        if (count > 2) {
            val message = "VIOLATION: More than 2 screens composed simultaneously! Count=$count, screens=$activeScreens"
            Log.e(TAG, message)
            if (BuildConfig.DEBUG) {
                error(message)
            }
        }
    }

    fun onScreenExit(screen: String) {
        activeScreens.remove(screen)
        val count = activeScreens.size
        Log.d(TAG, "Screen EXIT: $screen (Active composed screens: $count -> $activeScreens)")
    }

    val activeCount: Int
        get() = activeScreens.size

    val currentActiveScreens: Set<String>
        get() = activeScreens.toSet()
}
