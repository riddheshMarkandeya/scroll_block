package com.example.scrollblock.internal

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import java.util.LinkedList

class IdleScrollAccessibilityService : AccessibilityService() {

    private var isEnabled = true
    private val TAG = "ScrollBlockDetector"
    private lateinit var prefs: SharedPreferences
    
    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("scroll_block_prefs", Context.MODE_PRIVATE)
        isEnabled = prefs.getBoolean("detector_enabled", false)
    }

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == "detector_enabled") {
            isEnabled = sharedPreferences.getBoolean(key, false)
            Log.d(TAG, "Detector enabled state changed: $isEnabled")
            if (!isEnabled) {
                scrollTimestamps.clear()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
    }

    // Detection parameters
    private val WINDOW_MS = 30_000L // 30 seconds sliding window
    private val SCROLL_COUNT_THRESHOLD = 20 // ~20 scrolls in 30 seconds
    private val IDLE_RESET_MS = 5_000L // Reset if user stops for 5 seconds
    private val MIN_TIME_BETWEEN_COUNTS_MS = 150L // Ignore duplicate events from the same scroll
    
    private val scrollTimestamps = LinkedList<Long>()
    private var lastSeenEventTime = 0L
    private var lastCountedEventTime = 0L
    private var lastOverlayTime = 0L
    private val OVERLAY_COOLDOWN_MS = 60_000L // Don't show overlay more than once per minute

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Refresh state in case it was changed while service was running
        isEnabled = prefs.getBoolean("detector_enabled", false)
        return START_STICKY
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!isEnabled) return

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            handleScrollEvent()
        }
    }

    private fun handleScrollEvent() {
        val currentTime = System.currentTimeMillis()
        
        // 1. Idle Reset: Check gap since the LAST SEEN event (any event)
        if (lastSeenEventTime != 0L) {
            val idleGap = currentTime - lastSeenEventTime
            if (idleGap > IDLE_RESET_MS) {
                Log.d(TAG, "Idle reset: Gap of ${idleGap/1000}s detected. Resetting points.")
                scrollTimestamps.clear()
            }
        }
        lastSeenEventTime = currentTime
        
        // 2. Debounce: Only count a point if it's been 150ms since the LAST COUNTED event
        // This ensures continuous scrolling actually increments the counter.
        val timeSinceLastCount = currentTime - lastCountedEventTime
        if (timeSinceLastCount < MIN_TIME_BETWEEN_COUNTS_MS) {
            return
        }
        lastCountedEventTime = currentTime
        
        // 3. Add current scroll timestamp
        scrollTimestamps.add(currentTime)
        
        // 4. Sliding Window: Remove timestamps older than 30s
        while (scrollTimestamps.isNotEmpty() && (currentTime - (scrollTimestamps.peekFirst() ?: 0L)) > WINDOW_MS) {
            scrollTimestamps.removeFirst()
        }

        // LOG FOR MONITORING
        Log.d(TAG, "Points: ${scrollTimestamps.size} / $SCROLL_COUNT_THRESHOLD | Added point (gap: ${timeSinceLastCount}ms)")

        // 5. Classification
        if (scrollTimestamps.size >= SCROLL_COUNT_THRESHOLD) {
            if (currentTime - lastOverlayTime > OVERLAY_COOLDOWN_MS) {
                Log.w(TAG, "!!! IDLE SCROLL DETECTED !!! Showing Overlay.")
                showWarningOverlay()
                lastOverlayTime = currentTime
                scrollTimestamps.clear()
            }
        }
    }

    private fun showWarningOverlay() {
        OverlayManager(this).show()
    }

    override fun onInterrupt() {}
}
