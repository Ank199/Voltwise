package com.voltwise.service

import android.Manifest
import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.voltwise.data.local.entity.AlertHistoryEntity
import com.voltwise.data.repository.BatteryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class AlertManager(
    private val context: Context,
    private val repository: BatteryRepository
) {

    private val prefs: SharedPreferences = context.getSharedPreferences("voltwise_prefs", Context.MODE_PRIVATE)
    private var textToSpeech: TextToSpeech? = null
    
    companion object {
        const val CHANNEL_ALERTS = "voltwise_alerts_channel"
        private const val ALERT_COOLDOWN_MS = 30 * 60 * 1000L // 30 minutes
    }
    
    init {
        createNotificationChannel()
        initTts()
    }

    fun close() { textToSpeech?.shutdown() }

    private fun initTts() {
        try {
            textToSpeech = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech?.language = Locale.getDefault()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Battery Alerts"
            val descriptionText = "Notifications for battery charge level and temperature alerts"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ALERTS, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun checkAlerts(level: Int, temperature: Float?, isCharging: Boolean) {
        val targetChargeEnabled = prefs.getBoolean("alert_charge_target_enabled", false)
        val targetChargeLevel = prefs.getInt("alert_charge_target_level", 80)
        
        val lowBatteryEnabled = prefs.getBoolean("alert_low_battery_enabled", false)
        val lowBatteryLevel = prefs.getInt("alert_low_battery_level", 20)
        
        val highTempEnabled = prefs.getBoolean("alert_high_temp_enabled", false)
        val highTempThreshold = prefs.getFloat("alert_high_temp_threshold", 40f)

        if (!isCharging || level < targetChargeLevel) {
            prefs.edit().putBoolean("alert_charge_target_armed", true).apply()
        }

        val targetChargeArmed = prefs.getBoolean("alert_charge_target_armed", true)
        if (targetChargeEnabled && isCharging && level >= targetChargeLevel && targetChargeArmed) {
            prefs.edit().putBoolean("alert_charge_target_armed", false).apply()
            triggerAlert(
                "CHARGE_TARGET",
                "Target Charge Reached",
                "Battery reached your selected target: $level%. You can unplug now."
            )
        }

        if (lowBatteryEnabled && !isCharging && level <= lowBatteryLevel) {
            triggerAlert(
                "LOW_BATTERY",
                "Low Battery Alert",
                "Battery has dropped to $level%. Please connect your charger."
            )
        }

        if (highTempEnabled && temperature != null && temperature >= highTempThreshold) {
            triggerAlert(
                "HIGH_TEMP",
                "High Battery Temperature",
                "Battery temperature is ${temperature}°C. Consider stopping charging to cool down."
            )
        }
    }

    private fun triggerAlert(type: String, title: String, message: String) {
        val lastAlertTime = prefs.getLong("last_alert_time_$type", 0)
        val now = System.currentTimeMillis()
        
        if (now - lastAlertTime < ALERT_COOLDOWN_MS) {
            return // Cooldown active
        }

        prefs.edit().putLong("last_alert_time_$type", now).apply()

        // Play sound and vibrate
        playAlertSoundAndVibrate()

        // Speak out voice alert
        speakAlert(message)

        var isDelivered = false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            
            val builder = NotificationCompat.Builder(context, CHANNEL_ALERTS)
                .setSmallIcon(R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            try {
                with(NotificationManagerCompat.from(context)) {
                    notify(type.hashCode(), builder.build())
                }
                isDelivered = true
            } catch (e: SecurityException) {
                isDelivered = false
            }
        }

        // Save alert to history
        CoroutineScope(Dispatchers.IO).launch {
            repository.insertAlert(
                AlertHistoryEntity(
                    timestamp = now,
                    alertType = type,
                    title = title,
                    message = message,
                    isDelivered = isDelivered
                )
            )
        }
    }

    private fun playAlertSoundAndVibrate() {
        val soundEnabled = prefs.getBoolean("alert_sound_enabled", true)
        if (soundEnabled) {
            try {
                val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val r = RingtoneManager.getRingtone(context, notificationUri)
                r.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(800, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(800)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun speakAlert(message: String) {
        val voiceEnabled = prefs.getBoolean("alert_voice_enabled", true)
        if (!voiceEnabled) return

        try {
            textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
