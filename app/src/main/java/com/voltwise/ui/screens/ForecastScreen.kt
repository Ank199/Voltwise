package com.voltwise.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.voltwise.data.local.entity.BatterySessionEntity
import com.voltwise.data.local.entity.BatterySnapshotEntity
import com.voltwise.ui.viewmodel.LiveViewModel
import kotlin.math.ceil

@Composable
fun ForecastScreen(vm: LiveViewModel = viewModel()) {
    val reading by vm.reading.collectAsState()
    val sessions by vm.sessions.collectAsState()
    val snapshots by vm.snapshots.collectAsState()
    val level = reading?.level
    val chargeSpeed = activeChargeSpeed(reading?.timestamp, sessions, level)
        ?: sessions.mapNotNull { it.percentPerHour() }.filter { it in 1f..90f }.averageOrNull()
    val drainSpeed = snapshots.dischargePercentPerHour()
    val chargerForecasts = chargerForecasts(level, sessions, reading?.source)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { PageHeading("What happens next", "Forecast", "Predictions from your saved charge and discharge samples.") }
        item {
            ReadingCard(if (reading?.plugged == true) "Charge forecast" else "Usage forecast") {
                if (level == null) {
                    Text("Waiting for a live battery reading.", color = Muted, fontSize = 13.sp)
                } else if (reading?.plugged == true) {
                    ChargeForecast(level, chargeSpeed)
                } else {
                    DischargeForecast(level, drainSpeed)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SensorTile("Charge speed", chargeSpeed?.let { "%.1f".format(it) } ?: "--", "%/hr", "Saved sessions", Icons.Rounded.BatteryChargingFull, Modifier.weight(1f), Mint)
                SensorTile("Drain speed", drainSpeed?.let { "%.1f".format(it) } ?: "--", "%/hr", "Saved samples", Icons.Rounded.BatteryStd, Modifier.weight(1f), Amber)
            }
        }
        if (chargerForecasts.isNotEmpty()) item {
            ReadingCard("Charger comparison") {
                chargerForecasts.forEach { forecast ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(forecast.label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("${"%.1f".format(forecast.speed)}%/hr from ${forecast.sessions} session${if (forecast.sessions == 1) "" else "s"}", color = Muted, fontSize = 11.sp)
                        }
                        Text(forecast.minutes?.let { "~$it min" } ?: "Full", color = if (forecast.isCurrent) Mint else Lavender, fontWeight = FontWeight.Medium)
                    }
                    if (forecast != chargerForecasts.last()) Divider(color = PanelBorder)
                }
            }
        }
        item {
            ReadingCard("Forecast confidence") {
                val confidence = when {
                    snapshots.size >= 40 && sessions.size >= 3 -> "High"
                    snapshots.size >= 12 || sessions.size >= 1 -> "Medium"
                    else -> "Learning"
                }
                Text(confidence, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = if (confidence == "High") Mint else Lavender)
                Divider(color = PanelBorder)
                Text("Predictions improve as Voltwise records more plugged and unplugged samples. Battery level, temperature, and phone activity can change the real result.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}

private data class ChargerForecast(
    val label: String,
    val speed: Float,
    val sessions: Int,
    val minutes: Int?,
    val isCurrent: Boolean
)

@Composable
private fun ChargeForecast(level: Int, speed: Float?) {
    if (level >= 100) {
        Text("Battery full", fontSize = 24.sp, fontWeight = FontWeight.Medium, color = Mint)
        Text("Unplug when convenient to reduce time at 100%.", color = Muted, fontSize = 12.sp)
        return
    }
    if (speed == null) {
        Text("Learning charge time", fontSize = 22.sp, fontWeight = FontWeight.Medium, color = Lavender)
        Text("Charge for a few minutes with monitoring active to estimate time remaining.", color = Muted, fontSize = 12.sp)
        return
    }
    val target = if (level < 80) 80 else 100
    val minutes = ceil(((target - level).coerceAtLeast(1) / speed) * 60f).toInt().coerceAtLeast(1)
    Text("$target% in about $minutes min", fontSize = 24.sp, fontWeight = FontWeight.Medium, color = Mint)
    Text("Based on %.1f%%/hr from this charger and recent sessions.".format(speed), color = Muted, fontSize = 12.sp)
}

@Composable
private fun DischargeForecast(level: Int, speed: Float?) {
    if (speed == null) {
        Text("Learning discharge time", fontSize = 22.sp, fontWeight = FontWeight.Medium, color = Lavender)
        Text("Use the phone unplugged while monitoring is active to estimate remaining time.", color = Muted, fontSize = 12.sp)
        return
    }
    val minutesTo20 = ceil(((level - 20).coerceAtLeast(1) / speed) * 60f).toInt().coerceAtLeast(1)
    Text("20% in about $minutesTo20 min", fontSize = 24.sp, fontWeight = FontWeight.Medium, color = Amber)
    Text("Based on %.1f%%/hr from saved discharge samples.".format(speed), color = Muted, fontSize = 12.sp)
}

private fun activeChargeSpeed(timestamp: Long?, sessions: List<BatterySessionEntity>, level: Int?): Float? {
    val now = timestamp ?: return null
    val active = sessions.firstOrNull { it.endTime == null && it.isCharging } ?: return null
    val currentLevel = level ?: return null
    val gained = currentLevel - active.startLevel
    val hours = (now - active.startTime) / 3600000f
    return if (gained > 0 && hours > 0.05f) gained / hours else null
}

private fun BatterySessionEntity.percentPerHour(): Float? {
    val end = endTime ?: return null
    val gained = ((endLevel ?: return null) - startLevel).coerceAtLeast(0)
    val hours = (end - startTime) / 3600000f
    return if (gained > 0 && hours > 0f) gained / hours else null
}

private fun chargerForecasts(level: Int?, sessions: List<BatterySessionEntity>, currentSource: String?): List<ChargerForecast> {
    val currentLevel = level ?: return emptyList()
    if (currentLevel >= 100) return emptyList()
    val target = if (currentLevel < 80) 80 else 100
    return sessions
        .filter { it.endTime != null && it.isCharging }
        .groupBy { it.customLabel?.takeIf { label -> label.isNotBlank() } ?: it.chargingSource ?: "Unknown charger" }
        .mapNotNull { (label, list) ->
            val speed = list.mapNotNull { it.percentPerHour() }.filter { it in 1f..90f }.averageOrNull() ?: return@mapNotNull null
            val minutes = ceil(((target - currentLevel).coerceAtLeast(1) / speed) * 60f).toInt().coerceAtLeast(1)
            ChargerForecast(label, speed, list.size, minutes, label == currentSource)
        }
        .sortedWith(compareByDescending<ChargerForecast> { it.isCurrent }.thenBy { it.minutes ?: Int.MAX_VALUE })
        .take(3)
}

private fun List<Float>.averageOrNull(): Float? = takeIf { it.isNotEmpty() }?.average()?.toFloat()

private fun List<BatterySnapshotEntity>.dischargePercentPerHour(): Float? {
    val drops = filter { !it.plugged }.sortedBy { it.timestamp }.zipWithNext().mapNotNull { (a, b) ->
        val minutes = (b.timestamp - a.timestamp) / 60000f
        val drop = a.level - b.level
        if (minutes >= 1f && drop > 0) drop * 60f / minutes else null
    }
    return drops.averageOrNull()
}
