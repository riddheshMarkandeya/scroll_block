package com.example.scrollblock.internal

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.example.scrollblock.R

/**
 * Manages the idle scroll warning overlay.
 */
class OverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val overlayView: View = LayoutInflater.from(context).inflate(R.layout.overlay_warning, null)
    
    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.CENTER
    }

    private var isVisible = false
    private val handler = Handler(Looper.getMainLooper())
    private var canDismiss = false

    /**
     * Show the overlay.
     */
    fun show() {
        if (isVisible) return

        canDismiss = false
        val dismissButton = overlayView.findViewById<Button>(R.id.dismissButton)
        val timerText = overlayView.findViewById<TextView>(R.id.timerText)

        // Initial state: Button is transparent or disabled-looking
        dismissButton?.alpha = 0.5f
        timerText?.visibility = View.VISIBLE
        
        var secondsLeft = 3
        
        val timerRunnable = object : Runnable {
            override fun run() {
                if (secondsLeft > 0) {
                    timerText?.text = "Wait ${secondsLeft}s to dismiss"
                    secondsLeft--
                    handler.postDelayed(this, 1000)
                } else {
                    canDismiss = true
                    dismissButton?.alpha = 1.0f
                    timerText?.text = "Ready to continue"
                    // Optional: hide timer text or change color
                }
            }
        }
        
        handler.post(timerRunnable)

        dismissButton?.setOnClickListener {
            if (canDismiss) {
                hide()
            }
        }

        try {
            windowManager.addView(overlayView, params)
            isVisible = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Hide the overlay.
     */
    fun hide() {
        if (!isVisible) return
        try {
            windowManager.removeView(overlayView)
            isVisible = false
            handler.removeCallbacksAndMessages(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
