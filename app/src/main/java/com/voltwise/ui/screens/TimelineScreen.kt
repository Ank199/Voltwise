package com.voltwise.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.voltwise.ui.viewmodel.LiveViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TimelineScreen(navController: NavController, vm: LiveViewModel = viewModel()) {
    val events by vm.events.collectAsState()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageHeading("The story of your battery", "Activity", "Every connection. Every charging milestone.") }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            StatusPill("LAST 24 HOURS"); Text("${events.size} events", color = Muted, fontSize = 12.sp)
        } }
        item { LiveBatteryTrendCard(vm) }
        if(events.isEmpty()) item { EmptyPanel("A fresh start", "Battery events will appear here as you connect, charge and go.", Icons.Rounded.Timeline) }
        items(events, key = { it.id }) { event ->
            val warning = event.type.contains("temperature", true) || event.type.contains("low", true)
            val color = if(warning) Amber else Mint
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(color = color.copy(alpha = .10f), shape = CircleShape) {
                        Icon(when { warning -> Icons.Rounded.PriorityHigh; event.type.contains("disconnected", true) -> Icons.Rounded.PowerOff; event.type.contains("full", true) -> Icons.Rounded.Check; else -> Icons.Rounded.Bolt }, null, tint = color, modifier = Modifier.padding(12.dp).size(19.dp))
                    }
                    Box(Modifier.padding(top = 8.dp).width(1.dp).height(34.dp).background(PanelBorder))
                }
                Surface(Modifier.weight(1f), color = Panel, shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(SimpleDateFormat("MMM d · HH:mm", Locale.getDefault()).format(Date(event.timestamp)), color = Muted, fontSize = 10.sp)
                        Text(event.type, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        if(event.detail != event.type) Text(event.detail, color = Muted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
