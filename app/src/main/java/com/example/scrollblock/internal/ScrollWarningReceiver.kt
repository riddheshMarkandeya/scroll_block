package com.example.scrollblock.internal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Broadcast receiver to handle overlay show/hide commands.
 */
class ScrollWarningReceiver : BroadcastReceiver() {

    companion object {
        private const val EXTRA_SHOW_EXTRA_TYPE = "show_overlay"
        private const val EXTRA_HIDE_EXTRA_TYPE = "hide_overlay"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when {
            intent.extras?.getString(EXTRA_SHOW_EXTRA_TYPE) == "true" -> {
                OverlayManager(context).show()
            }
            intent.extras?.getString(EXTRA_HIDE_EXTRA_TYPE) == "true" -> {
                OverlayManager(context).hide()
            }
        }
    }
}
