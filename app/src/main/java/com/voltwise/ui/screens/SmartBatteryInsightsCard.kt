package com.voltwise.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltwise.data.local.entity.BatterySessionEntity
import com.voltwise.service.BatteryReading
import kotlin.math.ceil

private data class ChargeEstimate(val target: Int, val minutes: Int)

private data class ChargerInsight(
    val label: String,
    val speedPercentPerHour: Float,
    val maxTemperature: Float?,
    val sessions: Int
)

private data class CareInsight(
    val title: String,
    val detail: String,
    val color: Color
)

@Composable
fun SmartBatteryInsightsCard(
    reading: BatteryReading?,
    sessions: List<BatterySessionEntity>,
    useCelsius: Boolean
) {
    val complete = sessions.filter { it.isCharging && it.endTime != null && (it.endLevel ?: it.startLevel) > it.startLevel }
    val estimate = reading?.let { chargeEstimate(it, complete) }
    val charger = bestCharger(complete)
    val care = reading?.let { careInsight(it, complete) }
        ?: CareInsight("Care score learning", "Open Voltwise while charging to make this advice smarter.", Lavender)

    ReadingCard("Smart battery insights") {
        InsightRow(
            Icons.Rounded.Schedule,
            estimate?.let { "${it.target}% in ~${it.minutes} min" } ?: "Learning charge time",
            estimate?.let { "Based on your recent charging sessions." } ?: "Charge once with monitoring active to unlock prediction.",
            Mint
        )
        Divider(color = PanelBorder)
        InsightRow(Icons.Rounded.HealthAndSafety, care.title, care.detail, care.color)
        Divider(color = PanelBorder)
        InsightRow(
            Icons.Rounded.EmojiEvents,
            charger?.let { it.label } ?: "No best charger yet",
            charger?.let {
                val temp = it.maxTemperature?.let { value -> " · max ${temperatureText(value, useCelsius)}" } ?: ""
                "%.1f%%/h across ${it.sessions} session%s%s".format(
                    it.speedPercentPerHour,
                    if (it.sessions == 1) "" else "s",
                    temp
                )
            } ?: "Complete a few charging sessions to compare chargers.",
            Lavender
        )
    }
}

@Composable
private fun InsightRow(icon: ImageVector, title: String, detail: String, color: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Surface(color = color.copy(alpha = .12f), shape = RoundedCornerShape(12.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.padding(8.dp).size(19.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(detail, color = Muted, fontSize = 11.sp)
        }
    }
}

private fun chargeEstimate(reading: BatteryReading, sessions: List<BatterySessionEntity>): ChargeEstimate? {
    val level = reading.level ?: return null
    if (!reading.plugged || level >= 100) return null

    val recentSpeed = sessions
        .take(8)
        .mapNotNull { it.percentPerHour() }
        .filter { it in 1f..90f }
        .takeIf { it.isNotEmpty() }
        ?.average()
        ?.toFloat()
        ?: return null

    val target = if (level < 80) 80 else 100
    val minutes = ceil(((target - level).coerceAtLeast(1) / recentSpeed) * 60f).toInt()
    return ChargeEstimate(target, minutes.coerceAtLeast(1))
}

private fun bestCharger(sessions: List<BatterySessionEntity>): ChargerInsight? {
    return sessions
        .groupBy { it.customLabel?.takeIf { label -> label.isNotBlank() } ?: it.chargingSource ?: "Unknown charger" }
        .mapNotNull { (label, chargerSessions) ->
            val speeds = chargerSessions.mapNotNull { it.percentPerHour() }.filter { it > 0f }
            if (speeds.isEmpty()) null else ChargerInsight(
                label = label,
                speedPercentPerHour = speeds.average().toFloat(),
                maxTemperature = chargerSessions.mapNotNull { it.maxTemperature }.maxOrNull(),
                sessions = chargerSessions.size
            )
        }
        .maxByOrNull { it.speedPercentPerHour - ((it.maxTemperature ?: 35f) - 35f).coerceAtLeast(0f) }
}

private fun careInsight(reading: BatteryReading, sessions: List<BatterySessionEntity>): CareInsight {
    val hotNow = reading.temperature?.let { it >= 42f } == true
    val longFullCharges = sessions.count { it.overchargeDuration >= 30 * 60 * 1000L }
    val hotSessions = sessions.count { (it.maxTemperature ?: 0f) >= 42f }

    return when {
        hotNow -> CareInsight("Battery is warm", "Unplug or pause heavy use until it cools.", Color(0xFFFF8A80))
        longFullCharges >= 3 -> CareInsight("Often left full", "You had $longFullCharges long 100% charging sessions recently.", Amber)
        hotSessions >= 3 -> CareInsight("Charging runs hot", "Try a cooler place or a slower charger.", Amber)
        sessions.size >= 3 -> CareInsight("Care looks good", "Recent sessions look steady and safe.", Mint)
        else -> CareInsight("Care score learning", "A few sessions will make this advice smarter.", Lavender)
    }
}

private fun BatterySessionEntity.percentPerHour(): Float? {
    val end = endTime ?: return null
    val gained = ((endLevel ?: return null) - startLevel).coerceAtLeast(0)
    val hours = (end - startTime) / 3600000f
    return if (gained > 0 && hours > 0f) gained / hours else null
}

private fun temperatureText(celsius: Float, useCelsius: Boolean): String {
    val value = if (useCelsius) celsius else celsius * 9 / 5 + 32
    val unit = if (useCelsius) "C" else "F"
    return "%.1f %s".format(value, unit)
}
