package com.voltwise.service

import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.abs

data class BatteryReading(
    val timestamp: Long,
    val level: Int?,
    val status: String,
    val source: String,
    val plugged: Boolean,
    val rawVoltage: Int?,
    val rawCurrent: Int?,
    val rawTemperature: Int?,
    val health: String,
    val technology: String,
    val currentInMilliAmps: Boolean = false
) {
    val volts get() = batteryVolts(rawVoltage)
    val currentMa get() = currentMilliAmps(rawCurrent, currentInMilliAmps)
    val temperature get() = rawTemperature?.div(10f)
    val watts get() = volts?.let { v -> currentMa?.let { mA -> v * (mA / 1000f) } }
}

fun batteryVolts(raw: Int?): Float? {
    if (raw == null || raw <= 0) return null
    return when {
        raw > 100_000 -> raw / 1_000_000f // microvolts
        raw in 1000..20000 -> raw / 1000f // millivolts (e.g. 4180 -> 4.18V)
        raw in 20000..500_000 -> raw / 100_000f // microvolts variant
        raw < 20 -> raw.toFloat() // already in volts
        else -> raw / 1000f
    }
}

fun currentMilliAmps(raw: Int?, alreadyMilliAmps: Boolean = false): Float? = raw
    ?.takeIf { it != Int.MIN_VALUE && it != Int.MAX_VALUE && it != 0 }
    ?.let { abs(it.toDouble()).toFloat() / (if (alreadyMilliAmps) 1f else 1000f) }

object LiveBattery {
    val reading = MutableStateFlow<BatteryReading?>(null)
    val monitoring = MutableStateFlow(false)
    val test = MutableStateFlow("Connect charger for at least 10 minutes to test.")
    @Volatile var requestTest = false

    fun read(intent: Intent, manager: BatteryManager): BatteryReading {
        fun extra(key: String) = if (intent.hasExtra(key)) intent.getIntExtra(key, -1) else null
        val level = extra(BatteryManager.EXTRA_LEVEL)
        val scale = extra(BatteryManager.EXTRA_SCALE)
        val plug = extra(BatteryManager.EXTRA_PLUGGED)
        return BatteryReading(
            System.currentTimeMillis(),
            if (level != null && scale != null && scale > 0 && level in 0..scale) level * 100 / scale else null,
            when(extra(BatteryManager.EXTRA_STATUS)) { 2 -> "Charging"; 3 -> "Discharging"; 4 -> "Not charging"; 5 -> "Full"; else -> "Unknown" },
            when(plug) { 0 -> "Unplugged"; 1 -> "AC"; 2 -> "USB"; 4 -> "Wireless"; 8 -> "Dock"; else -> "Unknown" },
            plug != null && plug > 0,
            extra(BatteryManager.EXTRA_VOLTAGE),
            runCatching { manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) }.getOrNull(),
            extra(BatteryManager.EXTRA_TEMPERATURE),
            when(extra(BatteryManager.EXTRA_HEALTH)) { 2 -> "Good"; 3 -> "Overheat"; 4 -> "Dead"; 5 -> "Over voltage"; 6 -> "Failure"; 7 -> "Cold"; else -> "Unknown" },
            intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown",
            Build.MANUFACTURER.equals("realme", ignoreCase = true) && Build.MODEL == "RMX2151"
        )
    }
}
