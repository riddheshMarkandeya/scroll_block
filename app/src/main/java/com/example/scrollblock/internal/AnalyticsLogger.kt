package com.example.scrollblock.internal

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Simple analytics logger that writes events to a local file.
 * Can be replaced with a real analytics SDK later.
 */
class AnalyticsLogger(context: Context) {

    companion object {
        private const val LOG_FILE_PATH = "scroll_analytics.log"
        private const val MAX_LOG_SIZE_MB = 10
    }

    private val file = context.filesDir.resolve(LOG_FILE_PATH)
    private val maxBytes = MAX_LOG_SIZE_MB.toLong() * 1024L

    /**
     * Log an analytics event.
     *
     * @param eventType Type of event (e.g., "idle_scroll_detected", "idle_scroll_hidden")
     */
    fun logAnalytics(eventType: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)
        val timestamp = sdf.format(Date())
        val logLine = "${timestamp}|${eventType}|${System.currentTimeMillis()}\n"

        // Append to log file
        file.appendText(logLine)

        // Optional: Rotate log if too large
        if (file.length() > maxBytes) {
            rotateLog()
        }
    }

    private fun rotateLog() {
        val rotatedFile = file.parentFile.resolve("${LOG_FILE_PATH}.backup")
        file.renameTo(rotatedFile)
    }
}
