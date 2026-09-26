package com.ripple.filemanager.ui

import android.os.SystemClock
import android.util.Log

/**
 * Diagnostic latency tracker for measuring tab-switch responsiveness:
 * 1. Input-to-start (tap to setLocation / animation start)
 * 2. Mid-animation (transition duration)
 * 3. Data-wait (fetch time and time until UI shows files)
 */
object TabSwitchLatencyTracker {
    private const val TAG = "TabSwitchLatency"

    private var tapTimestamp = 0L
    private var lastTappedTab: String = ""

    fun onTabTap(tabName: String) {
        tapTimestamp = SystemClock.elapsedRealtime()
        lastTappedTab = tabName
        Log.i(TAG, "═══ [INPUT] Tab tap registered: $tabName at t=0ms ═══")
    }

    fun onActionDispatched(location: String) {
        val now = SystemClock.elapsedRealtime()
        val elapsed = if (tapTimestamp > 0) now - tapTimestamp else 0
        Log.i(TAG, "[ACTION] SetLocation action dispatched: $location (+${elapsed}ms from tap)")
    }

    fun onViewModelSetLocation(location: String, hasCache: Boolean, cachedCount: Int) {
        val now = SystemClock.elapsedRealtime()
        val elapsed = if (tapTimestamp > 0) now - tapTimestamp else 0
        Log.i(TAG, "[VIEWMODEL] setLocation($location) processed. Cache hit=$hasCache (items=$cachedCount) (+${elapsed}ms from tap)")
    }

    fun onDataFetchStart(location: String) {
        val now = SystemClock.elapsedRealtime()
        val elapsed = if (tapTimestamp > 0) now - tapTimestamp else 0
        Log.i(TAG, "[DATA] Background file fetch STARTED for $location (+${elapsed}ms from tap)")
    }

    fun onDataFetchEnd(location: String, itemCount: Int, durationMs: Long) {
        val now = SystemClock.elapsedRealtime()
        val elapsed = if (tapTimestamp > 0) now - tapTimestamp else 0
        Log.i(TAG, "[DATA] Background file fetch FINISHED for $location: $itemCount items in ${durationMs}ms (+${elapsed}ms from tap)")
    }

    fun onBrowseCompose(location: String, fileCount: Int, isLoading: Boolean) {
        val now = SystemClock.elapsedRealtime()
        val elapsed = if (tapTimestamp > 0) now - tapTimestamp else 0
        Log.i(TAG, "[COMPOSE] BrowseScreen composing: location=$location, fileCount=$fileCount, isLoading=$isLoading (+${elapsed}ms from tap)")
    }

    fun onHomeCompose(recentCount: Int) {
        val now = SystemClock.elapsedRealtime()
        val elapsed = if (tapTimestamp > 0) now - tapTimestamp else 0
        Log.i(TAG, "[COMPOSE] HomeScreen composing: recentCount=$recentCount (+${elapsed}ms from tap)")
    }

    fun onTransitionStart(initialState: String, targetState: String) {
        val now = SystemClock.elapsedRealtime()
        val elapsed = if (tapTimestamp > 0) now - tapTimestamp else 0
        Log.i(TAG, "[ANIM] Switch animation STARTED: $initialState -> $targetState (+${elapsed}ms from tap)")
    }

    fun onTransitionSettled(targetState: String) {
        val now = SystemClock.elapsedRealtime()
        val elapsed = if (tapTimestamp > 0) now - tapTimestamp else 0
        Log.i(TAG, "[ANIM] Switch animation SETTLED at $targetState (Total latency: ${elapsed}ms from tap)")
    }
}
