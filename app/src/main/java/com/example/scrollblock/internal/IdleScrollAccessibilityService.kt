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
    
    // Detection parameters
    private var windowMs = 300_000L 
    private var scrollThreshold = 80 
    private var idleResetMs = 15_000L 
    private var minEventGapMs = 300L 
    
    private val scrollTimestamps = LinkedList<Long>()
    private var lastSeenEventTime = 0L
    private var lastProcessedEventTime = 0L
    private var lastOverlayTime = 0L
    private val OVERLAY_COOLDOWN_MS = 60_000L 

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when (key) {
            "detector_enabled" -> {
                isEnabled = sharedPreferences.getBoolean(key, false)
                Log.d(TAG, "Detector enabled state changed: $isEnabled")
                if (!isEnabled) {
                    resetDetectionState()
                }
            }
            "pref_window_ms", "pref_threshold", "pref_idle_reset_ms", "pref_debounce_ms" -> {
                loadPreferences()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("scroll_block_prefs", Context.MODE_PRIVATE)
        loadPreferences()
    }

    private fun loadPreferences() {
        isEnabled = prefs.getBoolean("detector_enabled", false)
        windowMs = prefs.getLong("pref_window_ms", 300_000L)
        scrollThreshold = prefs.getInt("pref_threshold", 80)
        idleResetMs = prefs.getLong("pref_idle_reset_ms", 15_000L)
        minEventGapMs = prefs.getLong("pref_debounce_ms", 300L)
        Log.d(TAG, "Preferences loaded: threshold=$scrollThreshold, window=${windowMs}ms, idleReset=${idleResetMs}ms, debounce=${minEventGapMs}ms")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        loadPreferences()
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        loadPreferences()
        return START_STICKY
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!isEnabled) return

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            handleScrollEvent(event)
        }
    }

    private fun handleScrollEvent(event: AccessibilityEvent) {
        val currentTime = System.currentTimeMillis()
        val packageName = event.packageName?.toString() ?: "unknown"
        
        // 1. Idle Reset: Check gap since the LAST SEEN event
        if (lastSeenEventTime != 0L) {
            val idleGap = currentTime - lastSeenEventTime
            if (idleGap > idleResetMs) {
                Log.d(TAG, "Idle reset: Gap of ${idleGap/1000}s. Resetting state.")
                resetDetectionState()
            }
        }
        lastSeenEventTime = currentTime

        // 2. Debounce Check: Prevent point-spamming from finger holds
        val timeSinceLastProcessed = currentTime - lastProcessedEventTime
        if (timeSinceLastProcessed < minEventGapMs) {
            return 
        }

        // 3. Movement Filtering
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val deltaY = event.scrollDeltaY
            val deltaX = event.scrollDeltaX
            
            // Ignore horizontal swipes
            if (Math.abs(deltaX) > Math.abs(deltaY) && deltaX != 0) return

            // Ignore significant upward scrolls
            if (deltaY < -1) return
        }

        // 4. Uniform Point Allocation
        lastProcessedEventTime = currentTime
        scrollTimestamps.add(currentTime)
        
        Log.d(TAG, "App: $packageName | Point added! Window: ${scrollTimestamps.size}")
        
        // 5. Sliding Window Cleanup
        while (scrollTimestamps.isNotEmpty() && (currentTime - (scrollTimestamps.peekFirst() ?: 0L)) > windowMs) {
            scrollTimestamps.removeFirst()
        }

        // 6. Threshold Check
        if (scrollTimestamps.size >= scrollThreshold) {
            if (currentTime - lastOverlayTime > OVERLAY_COOLDOWN_MS) {
                Log.w(TAG, "!!! TRIGGER !!! ${scrollTimestamps.size} scrolls in ${windowMs/1000}s")
                showWarningOverlay()
                lastOverlayTime = currentTime
                resetDetectionState()
            }
        }
    }

    private fun resetDetectionState() {
        scrollTimestamps.clear()
    }

    private fun showWarningOverlay() {
        OverlayManager(this).show()
    }

    override fun onInterrupt() {}
}
