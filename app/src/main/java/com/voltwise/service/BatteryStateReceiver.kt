package com.voltwise.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BatteryStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // We can trigger the service to update immediately upon receiving these intents
        // if the service is already running.
        val prefs = context.getSharedPreferences("voltwise_prefs", Context.MODE_PRIVATE)
        val isMonitoring = prefs.getBoolean("is_monitoring", false)
        
        if (isMonitoring) {
            val action = intent.action
            if (action == Intent.ACTION_POWER_CONNECTED || action == Intent.ACTION_POWER_DISCONNECTED) {
                // To avoid starting the service in the background implicitly on newer OS versions,
                // we'll rely on the foreground service's 60s polling which covers this accurately enough,
                // or we could send a specific command to the existing foreground service to trigger an immediate read.
            }
        }
    }
}
