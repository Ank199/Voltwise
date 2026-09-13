package com.voltwise.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.voltwise.data.local.entity.BatterySessionEntity
import com.voltwise.ui.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel()) {
    val sessions by viewModel.sessions.collectAsState()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageHeading("Your charging journal", "History", "Real sessions, saved on your device.") }
        if(sessions.isEmpty()) item { EmptyPanel("No sessions yet", "Connect your charger to begin your charging journal.", Icons.Rounded.History) }
        items(sessions, key = { it.id }) { SessionCard(it) }
    }
}

@Composable
fun SessionCard(session: BatterySessionEntity) {
    var expanded by remember { mutableStateOf(false) }
    ReadingCard(session.customLabel ?: "${session.chargingSource ?: "Unknown"} charging") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(SimpleDateFormat("MMM d · HH:mm", Locale.getDefault()).format(Date(session.startTime)), color = Muted, fontSize = 11.sp)
            StatusPill(if(session.endTime == null) "ACTIVE" else "SAVED")
        }
        Text("${session.startLevel}% → ${session.endLevel ?: session.startLevel}%", fontSize = 28.sp, fontWeight = FontWeight.Light)
        val duration = session.endTime?.let { "${(it-session.startTime)/60000} min" } ?: "Ongoing"
        Text("$duration   ·   ${(session.endLevel ?: session.startLevel)-session.startLevel}% gained", color = Mint, fontSize = 12.sp)
        TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) { Text(if(expanded) "Less detail" else "View session details", fontSize = 12.sp) }
        if(expanded) {
            Divider(color = PanelBorder)
            DetailRow("Average temperature", measured(session.avgTemperature, "°C"))
            DetailRow("Peak temperature", measured(session.maxTemperature, "°C"))
            DetailRow("Average current", measured(session.avgCurrent, "mA"))
            DetailRow("Average voltage", measured(session.avgVoltage?.div(1000f), "V"))
            DetailRow("Power · estimated", measured(session.avgWattage, "W"))
            DetailRow("Stability · estimated", session.stabilityScore.takeIf { it >= 0 }?.let { "%.0f / 100".format(it) } ?: "Not supported")
            DetailRow("Connected at 100%", "${session.overchargeDuration/60000} min")
            if(!session.isFullyObserved) Text("Partial session · monitoring was interrupted", color = Amber, fontSize = 11.sp)
        }
    }
}
