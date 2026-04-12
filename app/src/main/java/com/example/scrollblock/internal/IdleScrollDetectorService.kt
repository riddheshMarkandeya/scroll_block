package com.example.scrollblock.internal

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import androidx.core.app.NotificationCompat
import android.app.Notification
import android.app.Service
import android.content.pm.ServiceInfo

/**
 * Foreground service that monitors scroll events from other apps.
 *
 * Uses WindowManager to receive MotionEvents and detect idle scrolling.
 */
class IdleScrollDetectorService : Service() {

    companion object {
        private const val CHANNEL_ID = "idle-scroll-detector"
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_TAG = "IdleScrollDetector"

        private const val SERVICE_NOTIFICATION_ID = 1001
    }

    private lateinit var velocityDetector: ScrollVelocityDetector

    private val handler = Handler(Looper.getMainLooper())
    private var isScrollingDetected = false

    override fun onCreate() {
        super.onCreate()
        velocityDetector = ScrollVelocityDetector { velocity, timestamp ->
            // Update UI on main thread
            handler.post {
                updateVelocityUI(velocity, timestamp)
            }
        }

        // Setup WindowManager to receive scroll events from other apps
        setupWindowManager()

        // Create notification channel for Android O and above
        createNotificationChannel()

        // Register as foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                SERVICE_NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // On API 29-33, we can still provide a type, but SPECIAL_USE is API 34+
            // If you had a different type like 'dataSync', you'd use it here.
            // For now, we'll use the default startForeground for < 34 if no other type applies.
            startForeground(SERVICE_NOTIFICATION_ID, buildNotification())
        } else {
            startForeground(SERVICE_NOTIFICATION_ID, buildNotification())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // Service will restart if killed
    }

    /**
     * Receive MotionEvents from WindowManager.
     *
     * @param event Raw motion event from system
     */
    private fun handleMotionEvent(event: MotionEvent) {
        // Only process scroll actions (deltaY > 0)
        if (event.action == MotionEvent.ACTION_SCROLL && event.getY() > 0) {
            isScrollingDetected = false // Reset when new scroll starts
        }

        // Check for idle scroll
        val isIdleScroll = velocityDetector.processScrollEvent(event)
        if (isIdleScroll) {
            showWarningOverlay()
        }
    }

    private fun updateVelocityUI(velocity: Double, timestamp: Long) {
        // Could add a small status indicator in the corner
        // e.g., "Velocity: ${velocity.toInt()}px/s"
    }

    /**
     * Show the idle scroll warning overlay.
     */
    private fun showWarningOverlay() {
        isScrollingDetected = true

        // Show overlay directly
        OverlayManager(this).show()

        // Log the detection for analytics
        logAnalytics("idle_scroll_detected")
    }

    /**
     * Hide the warning overlay after cooldown period.
     */
    private fun hideWarningOverlay() {
        val intent = Intent(this, ScrollWarningReceiver::class.java).apply {
            putExtra("hide_overlay", "true")
        }
        sendBroadcast(intent)

        logAnalytics("idle_scroll_hidden")
    }

    /**
     * Initialize WindowManager to receive scroll events from other apps.
     */
    private fun setupWindowManager() {
        // WindowManager.GlobalCallback is not a public API. 
        // This is a placeholder for actual implementation which usually requires AccessibilityService.
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Idle Scroll Detector")
            .setContentText("Monitoring scroll activity...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Idle Scroll Detector Service"
            val descriptionText = "Notifications for the idle scroll detector service"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private val analyticsLogger by lazy { AnalyticsLogger(applicationContext) }

    private fun logAnalytics(eventType: String) {
        // Log to file for analytics
        analyticsLogger.logAnalytics(eventType)
    }

    /**
     * Stop the foreground service.
     */
    override fun onDestroy() {
        // Cleanup
        velocityDetector.reset()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): android.os.IBinder? = null
}
