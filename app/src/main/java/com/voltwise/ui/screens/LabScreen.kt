package com.voltwise.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.voltwise.ui.viewmodel.LiveViewModel

@Composable
fun LabScreen(navController: NavController? = null, vm: LiveViewModel = viewModel()) {
    val reading by vm.reading.collectAsState()
    val snapshots by vm.snapshots.collectAsState()
    val test by vm.test.collectAsState()
    val results by vm.labResults.collectAsState()
    val collecting = test.startsWith("Collecting") || test.startsWith("Starting")
    val elapsed = Regex("samples: (\\d+)").find(test)?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: 0f
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { PageHeading("Explore & understand", "Battery Lab", "Get to know the way you charge.") }
        item {
            Surface(color = Panel, shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, PanelBorder)) {
                Column(Modifier.background(Brush.verticalGradient(listOf(Lavender.copy(alpha = .10f), Panel))).padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Lavender.copy(alpha = .12f), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Rounded.Science, null, tint = Lavender, modifier = Modifier.padding(14.dp).size(28.dp)) }
                        StatusPill(if(collecting) "TEST RUNNING" else "10 MINUTES", Lavender)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("A closer look at\nyour charger.", fontSize = 27.sp, lineHeight = 34.sp, fontWeight = FontWeight.Medium)
                        Text("One measured session. A clearer picture of charging speed, heat and stability.", color = Muted, fontSize = 13.sp)
                    }
                    if(collecting) {
                        LinearProgressIndicator(progress = (elapsed/600f).coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth().height(6.dp), color = Lavender, trackColor = PanelBorder)
                        Text(test, color = Lavender, fontSize = 12.sp)
                    }
                    Button(onClick = { vm.startTest() }, enabled = reading?.plugged == true && !collecting,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Ink)) {
                        Icon(if(collecting) Icons.Rounded.HourglassTop else Icons.Rounded.PlayArrow, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp)); Text(if(collecting) "Measuring your charge…" else "Start 10-minute charger test", fontSize = 12.sp)
                    }
                    if(!collecting) Text(if(reading?.plugged != true) "Connect charger for at least 10 minutes to test." else test, color = Muted, fontSize = 12.sp)
                }
            }
        }
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SensorTile("Current", sensorNumber(reading?.currentMa, 0), "mA", if(reading?.currentMa == null) "Not supported" else "Live reading", Icons.Rounded.Bolt, Modifier.weight(1f))
            SensorTile("Temperature", sensorNumber(reading?.temperature), "°C", if(reading?.temperature == null) "Not supported" else "Live reading", Icons.Rounded.Thermostat, Modifier.weight(1f), Amber)
        } }
        item { BatteryOptimizationCard(reading, snapshots) }
        item { Text("Your results", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        if(results.isEmpty()) item { EmptyPanel("Your first discovery awaits", "Results appear here after a measured charging session.", Icons.Rounded.Insights) }
        items(results) { (name, result) -> ReadingCard(name) {
            val grade = result.substringBefore(" - ")
            if(grade in listOf("Excellent", "Good", "Average", "Poor")) StatusPill(grade, if(grade == "Poor") Amber else Mint)
            Text(result, color = Muted, fontSize = 12.sp)
        } }
        item { Text("Scores are estimates from saved samples. Phone activity and battery level affect results. This test cannot independently identify a cable fault.", color = Muted, fontSize = 11.sp, lineHeight = 17.sp) }
    }
}

@Composable
private fun BatteryOptimizationCard(
    reading: com.voltwise.service.BatteryReading?,
    snapshots: List<com.voltwise.data.local.entity.BatterySnapshotEntity>
) {
    val level = reading?.level
    val temperature = reading?.temperature
    val recentDrain = snapshots
        .filter { !it.plugged }
        .sortedBy { it.timestamp }
        .zipWithNext()
        .mapNotNull { (a, b) ->
            val minutes = (b.timestamp - a.timestamp) / 60000f
            val drop = a.level - b.level
            if (minutes >= 1f && drop > 0) drop * 60f / minutes else null
        }
        .takeLast(6)
        .takeIf { it.isNotEmpty() }
        ?.average()
        ?.toFloat()

    val tips = buildList {
        when {
            temperature != null && temperature >= 42f -> add(OptimizationTip(Icons.Rounded.DeviceThermostat, "Cool it down", "Pause heavy apps, remove the case, or charge in a cooler place.", Amber))
            reading?.plugged == true && level != null && level >= 80 -> add(OptimizationTip(Icons.Rounded.BatteryChargingFull, "Unplug near 80%", "For daily use, stopping around 80-90% is gentler than staying full.", Mint))
            reading?.plugged == true -> add(OptimizationTip(Icons.Rounded.Speed, "Use a steady charger", "Keep the phone idle during charging tests so speed and heat readings stay accurate.", Lavender))
            recentDrain != null && recentDrain >= 8f -> add(OptimizationTip(Icons.Rounded.BatteryAlert, "High drain detected", "Check screen brightness, hotspot, GPS, and background apps.", Amber))
            level != null && level <= 20 -> add(OptimizationTip(Icons.Rounded.Power, "Low battery mode", "Turn on battery saver and reduce brightness until you can charge.", Amber))
            else -> add(OptimizationTip(Icons.Rounded.HealthAndSafety, "Healthy pattern", "Keep heat low, avoid long 100% sessions, and use stable chargers.", Mint))
        }
        add(OptimizationTip(Icons.Rounded.AutoGraph, "Learn from Activity", "Use the Activity graph to spot which hours drain battery fastest.", Lavender))
    }

    ReadingCard("Optimize battery use") {
        tips.forEachIndexed { index, tip ->
            OptimizationRow(tip)
            if(index != tips.lastIndex) Divider(color = PanelBorder)
        }
    }
}

private data class OptimizationTip(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val detail: String,
    val color: androidx.compose.ui.graphics.Color
)

@Composable
private fun OptimizationRow(tip: OptimizationTip) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Surface(color = tip.color.copy(alpha = .12f), shape = RoundedCornerShape(12.dp)) {
            Icon(tip.icon, null, tint = tip.color, modifier = Modifier.padding(8.dp).size(19.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(tip.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(tip.detail, color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}
