package com.voltwise.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.voltwise.data.local.entity.BatterySessionEntity
import com.voltwise.data.local.entity.BatterySnapshotEntity
import com.voltwise.ui.viewmodel.LiveViewModel
import com.voltwise.ui.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InsightsScreen(vm: LiveViewModel = viewModel()) {
    val settings: SettingsViewModel = viewModel()
    val preferences by settings.uiState.collectAsState()
    val reading by vm.reading.collectAsState()
    val sessions by vm.sessions.collectAsState()
    val snapshots by vm.snapshots.collectAsState()
    val complete = sessions.filter { it.isCharging && it.endTime != null }
    val healthScore = batteryCareScore(reading?.health, complete)
    val careReasons = batteryCareReasons(reading?.health, complete)
    val chargeSpeed = complete.mapNotNull { it.percentPerHour() }.filter { it > 0f }.averageOrNull()
    val dischargeSpeed = snapshots.dischargePercentPerHour()
    val today = snapshots.todayReport()
    
    val dateFormat = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())
    val groupedByDate = complete.groupBy { dateFormat.format(Date(it.startTime)) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { PageHeading("See the bigger picture", "Analytics & Health", "Calendar usage, battery capacity & screen time.") }
        item { SmartBatteryInsightsCard(reading, sessions, preferences.useCelsius) }
        item { DailyBatteryReportCard(today) }
        
        item {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Panel,
                border = BorderStroke(1.dp, PanelBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color(0xFF1B2D2B), Panel)))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("BATTERY CARE", fontSize = 10.sp, letterSpacing = 1.5.sp, color = Muted)
                            Spacer(Modifier.height(4.dp))
                            Text("$healthScore%", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = careColor(healthScore))
                        }
                        Surface(shape = RoundedCornerShape(50), color = careColor(healthScore).copy(alpha = 0.15f)) {
                            Text(careLabel(healthScore), color = careColor(healthScore), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                    Divider(color = PanelBorder)
                    Text(careReasons, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                    Divider(color = PanelBorder)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Battery Health Sensor", color = Muted, fontSize = 11.sp)
                            Text(reading?.health ?: "Learning", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Avg Charge Speed", color = Muted, fontSize = 11.sp)
                            Text(chargeSpeed?.let { "%.1f%%/hr".format(it) } ?: "Learning", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Lavender)
                        }
                    }
                    Divider(color = PanelBorder)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Samples Saved", color = Muted, fontSize = 11.sp)
                            Text("${snapshots.size}", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Discharge Rate", color = Muted, fontSize = 11.sp)
                            Text(dischargeSpeed?.let { "%.1f%%/hr".format(it) } ?: "Learning", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Amber)
                        }
                    }
                }
            }
        }

        if(complete.isEmpty()) {
            item { EmptyPanel("Your story is just starting", "Finish a charging session to unlock your personal calendar usage and insights.", Icons.Rounded.BarChart) }
        } else {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SensorTile("Average session", "${complete.map { it.endTime!! - it.startTime }.average().toLong()/60000}", "min", "Recorded duration", Icons.Rounded.Schedule, Modifier.weight(1f))
                    SensorTile("Charge gained", "${complete.sumOf { (it.endLevel ?: it.startLevel) - it.startLevel }}", "%", "Across all sessions", Icons.Rounded.BatteryChargingFull, Modifier.weight(1f), Lavender)
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SensorTile("Peak temperature", sensorNumber(complete.mapNotNull { it.maxTemperature }.maxOrNull()), "°C", "Highest recorded", Icons.Rounded.Thermostat, Modifier.weight(1f), Amber)
                    SensorTile("Time at full", "${complete.sumOf { it.overchargeDuration }/60000}", "min", "Connected at 100%", Icons.Rounded.HourglassTop, Modifier.weight(1f))
                }
            }

            item { Text("CALENDAR-WISE USAGE HISTORY", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp) }

            items(groupedByDate.entries.toList()) { (dateStr, daySessions) ->
                ReadingCard(dateStr) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Sessions: ${daySessions.size}", color = Muted, fontSize = 12.sp)
                        Surface(shape = RoundedCornerShape(50), color = Mint.copy(alpha = 0.15f)) {
                            Text("+${daySessions.sumOf { (it.endLevel ?: it.startLevel) - it.startLevel }}% Total", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                    Divider(color = PanelBorder)
                    daySessions.forEach { s ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(6.dp).clip(CircleShape).background(Mint))
                                Text("Source: ${s.chargingSource ?: "Unknown"}", fontSize = 12.sp, color = Color(0xFFF0F4F8))
                            }
                            Text("+${(s.endLevel ?: s.startLevel) - s.startLevel}%  (${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(s.startTime))})", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Muted)
                        }
                    }
                }
            }

            item { ReadingCard("A note on your data") { Text("These insights use completed sessions only. Missing sensors stay unavailable, and interrupted sessions may contain partial observations.", color = Muted, fontSize = 12.sp) } }
        }
    }
}

private data class DailyBatteryReport(
    val gained: Int,
    val lost: Int,
    val bestCharge: Float?,
    val worstDrain: Float?,
    val sampleCount: Int
)

@Composable
private fun DailyBatteryReportCard(report: DailyBatteryReport) {
    ReadingCard("Today's battery report") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SensorTile("Charged today", "+${report.gained}", "%", "From saved samples", Icons.Rounded.BatteryChargingFull, Modifier.weight(1f), Mint)
            SensorTile("Used today", "-${report.lost}", "%", "While unplugged", Icons.Rounded.BatteryStd, Modifier.weight(1f), Amber)
        }
        Divider(color = PanelBorder)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Best charging window", color = Muted, fontSize = 11.sp)
                Text(report.bestCharge?.let { "%.1f%%/hr".format(it) } ?: "Learning", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Mint)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Worst drain window", color = Muted, fontSize = 11.sp)
                Text(report.worstDrain?.let { "%.1f%%/hr".format(it) } ?: "Learning", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Amber)
            }
        }
        Text("${report.sampleCount} samples saved today", color = Muted, fontSize = 11.sp)
    }
}

private fun batteryCareScore(health: String?, sessions: List<BatterySessionEntity>): Int {
    val hotSessions = sessions.count { (it.maxTemperature ?: 0f) >= 42f }
    val longFullCharges = sessions.count { it.overchargeDuration >= 30 * 60 * 1000L }
    val unstableSessions = sessions.count { it.stabilityScore in 0f..59f }
    val sensorPenalty = when (health) {
        "Good", null -> 0
        "Unknown" -> 4
        else -> 18
    }
    return (100 - hotSessions * 6 - longFullCharges * 4 - unstableSessions * 3 - sensorPenalty).coerceIn(55, 100)
}

private fun batteryCareReasons(health: String?, sessions: List<BatterySessionEntity>): String {
    val hotSessions = sessions.count { (it.maxTemperature ?: 0f) >= 42f }
    val longFullCharges = sessions.count { it.overchargeDuration >= 30 * 60 * 1000L }
    val unstableSessions = sessions.count { it.stabilityScore in 0f..59f }
    val parts = mutableListOf<String>()
    if (health != null && health != "Good" && health != "Unknown") parts += "Health sensor reports $health."
    if (hotSessions > 0) parts += "$hotSessions session${if (hotSessions == 1) "" else "s"} ran hot."
    if (longFullCharges > 0) parts += "$longFullCharges session${if (longFullCharges == 1) "" else "s"} stayed at 100% too long."
    if (unstableSessions > 0) parts += "$unstableSessions session${if (unstableSessions == 1) "" else "s"} had unstable charging."
    return if (parts.isEmpty()) "Score is high because recent charging looks cool, steady, and not left full for long." else parts.joinToString(" ")
}

private fun careLabel(score: Int): String = when {
    score >= 90 -> "Optimal State"
    score >= 75 -> "Good State"
    score >= 60 -> "Needs Care"
    else -> "Review Habits"
}

private fun careColor(score: Int): Color = when {
    score >= 85 -> Mint
    score >= 65 -> Amber
    else -> Color(0xFFFF8A80)
}

private fun List<Float>.averageOrNull(): Float? = takeIf { it.isNotEmpty() }?.average()?.toFloat()

private fun BatterySessionEntity.percentPerHour(): Float? {
    val end = endTime ?: return null
    val gained = ((endLevel ?: return null) - startLevel).coerceAtLeast(0)
    val hours = (end - startTime) / 3600000f
    return if (gained > 0 && hours > 0f) gained / hours else null
}

private fun List<BatterySnapshotEntity>.dischargePercentPerHour(): Float? {
    val points = filter { !it.plugged }.sortedBy { it.timestamp }
    val drops = points.zipWithNext().mapNotNull { (a, b) ->
        val minutes = (b.timestamp - a.timestamp) / 60000f
        val drop = a.level - b.level
        if (minutes >= 1f && drop > 0) drop * 60f / minutes else null
    }
    return drops.averageOrNull()
}

private fun List<BatterySnapshotEntity>.todayReport(): DailyBatteryReport {
    val start = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
    val points = filter { it.timestamp >= start }.sortedBy { it.timestamp }
    var gained = 0
    var lost = 0
    val chargeRates = mutableListOf<Float>()
    val drainRates = mutableListOf<Float>()
    points.zipWithNext().forEach { (a, b) ->
        val minutes = (b.timestamp - a.timestamp) / 60000f
        if (minutes <= 0f) return@forEach
        val delta = b.level - a.level
        if (delta > 0 && b.plugged) {
            gained += delta
            chargeRates += delta * 60f / minutes
        } else if (delta < 0 && !b.plugged) {
            val drop = -delta
            lost += drop
            drainRates += drop * 60f / minutes
        }
    }
    return DailyBatteryReport(
        gained = gained,
        lost = lost,
        bestCharge = chargeRates.maxOrNull(),
        worstDrain = drainRates.maxOrNull(),
        sampleCount = points.size
    )
}
