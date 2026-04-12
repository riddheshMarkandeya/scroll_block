package com.example.scrollblock.internal

import android.view.MotionEvent
import java.util.concurrent.TimeUnit

/**
 * Detects scroll velocity from MotionEvent events.
 *
 * Uses a sliding window approach to track consecutive scroll movements
 * and calculate average velocity over time.
 */
class ScrollVelocityDetector(private val onVelocityChanged: (Double, Long) -> Unit) {

    companion object {
        /** Minimum scroll delta to consider as a scroll event (pixels) */
        private val MIN_SCROLL_DELTA = 15f

        /** Velocity threshold to flag as "idle scrolling" (pixels per second) */
        private val IDLE_SCROLL_VELOCITY_THRESHOLD = 180f

        /** Window size for calculating rolling velocity (milliseconds) */
        private val VELOCITY_WINDOW_MS = 200L

        /** Minimum consecutive scroll cycles to confirm idle scrolling */
        private val MIN_CONSECUTIVE_CYCLES = 3

        /** Time between each velocity sample (milliseconds) */
        private val SAMPLE_INTERVAL_MS = 50L
    }

    private var currentVelocity: Double = 0.0
    private var velocityTimestamp: Long = 0
    private var isScrollingActive = false
    private var lastScrollTime: Long = 0

    /**
     * Process a scroll event from MotionEvent.
     *
     * @param event The scroll event (deltaY > 0 indicates scrolling down)
     * @return true if idle scroll was detected
     */
    fun processScrollEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_SCROLL) {
            return false
        }

        // Get vertical scroll delta in pixels
        val deltaY = event.getY()
        if (Math.abs(deltaY) < MIN_SCROLL_DELTA) {
            return false
        }

        // Calculate time since last sample
        val timeSinceLast = System.currentTimeMillis() - velocityTimestamp
        if (timeSinceLast > SAMPLE_INTERVAL_MS) {
            // Update current velocity
            currentVelocity = calculateVelocity(deltaY, timeSinceLast)
            velocityTimestamp = System.currentTimeMillis()

            onVelocityChanged(currentVelocity, System.currentTimeMillis())

            // Check if velocity indicates idle scrolling
            val isHighVelocity = currentVelocity > IDLE_SCROLL_VELOCITY_THRESHOLD

            if (isHighVelocity) {
                // Start tracking consecutive high-velocity scrolls
                isScrollingActive = true
                lastScrollTime = timeSinceLast
            } else {
                // Reset if low velocity detected
                isScrollingActive = false
            }

            // Only return true if we've detected multiple consecutive high-velocity cycles
            if (isScrollingActive && isHighVelocity) {
                val timeSinceLastCycle = System.currentTimeMillis() - lastScrollTime
                val cycleDurationMs = timeSinceLastCycle / MIN_CONSECUTIVE_CYCLES

                if (cycleDurationMs >= VELOCITY_WINDOW_MS) {
                    return true
                }
            }
        }

        return false
    }

    private fun calculateVelocity(deltaY: Float, timeMs: Long): Double {
        val distance = Math.abs(deltaY)
        val velocityPxPerSec = (distance * 1000.0) / timeMs
        return velocityPxPerSec
    }

    /**
     * Reset the detector state.
     */
    fun reset() {
        currentVelocity = 0.0
        velocityTimestamp = 0
        isScrollingActive = false
        lastScrollTime = 0
    }
}
