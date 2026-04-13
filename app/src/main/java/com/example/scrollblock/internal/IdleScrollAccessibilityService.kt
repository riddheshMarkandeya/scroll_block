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
                resetDetectionState()
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
    private val WINDOW_MS = 300_000L // 5 minute sliding window
    private val SCROLL_COUNT_THRESHOLD = 120 // Total scrolls needed in 5 mins
    private val IDLE_RESET_MS = 15_000L // Reset if user stops for 15 seconds
    private val MIN_EVENT_GAP_MS = 300L // Debounce: 300ms sweet spot to ignore holds
    
    private val scrollTimestamps = LinkedList<Long>()
    private var lastSeenEventTime = 0L
    private var lastProcessedEventTime = 0L
    private var lastOverlayTime = 0L
    private val OVERLAY_COOLDOWN_MS = 60_000L 

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isEnabled = prefs.getBoolean("detector_enabled", false)
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
            if (idleGap > IDLE_RESET_MS) {
                Log.d(TAG, "Idle reset: Gap of ${idleGap/1000}s. Resetting state.")
                resetDetectionState()
            }
        }
        lastSeenEventTime = currentTime

        // 2. Debounce Check: Prevent point-spamming from finger holds
        val timeSinceLastProcessed = currentTime - lastProcessedEventTime
        if (timeSinceLastProcessed < MIN_EVENT_GAP_MS) {
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
        while (scrollTimestamps.isNotEmpty() && (currentTime - (scrollTimestamps.peekFirst() ?: 0L)) > WINDOW_MS) {
            scrollTimestamps.removeFirst()
        }

        // 6. Threshold Check
        if (scrollTimestamps.size >= SCROLL_COUNT_THRESHOLD) {
            if (currentTime - lastOverlayTime > OVERLAY_COOLDOWN_MS) {
                Log.w(TAG, "!!! TRIGGER !!! ${scrollTimestamps.size} scrolls in 5 mins")
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
