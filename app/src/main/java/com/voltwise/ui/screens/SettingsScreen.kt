package com.voltwise.ui.screens

import androidx.compose.foundation.BorderStroke
import android.content.Intent
import android.content.Context
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.voltwise.ui.viewmodel.SettingsViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { PageHeading("Make it yours", "Settings", "Your preferences. Your battery data.") }
        
        item { ReadingCard("Display") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.Thermostat, null, tint = Mint)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Celsius", fontSize = 14.sp)
                    Text("Dashboard temperature units", color = Muted, fontSize = 11.sp)
                }
                Switch(checked = state.useCelsius, onCheckedChange = viewModel::updateUseCelsius)
            }
        } }

        item { ReadingCard("Power saving & system state") {
            val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
            val isSystemSaverActive = powerManager.isPowerSaveMode

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.BatterySaver, null, tint = if (isSystemSaverActive) Amber else Mint)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Android System Battery Saver", fontSize = 14.sp)
                    Text(if (isSystemSaverActive) "Status: ACTIVE (System power saving is ON)" else "Status: OFF (System running normally)", color = if (isSystemSaverActive) Amber else Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
            OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)) }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, PanelBorder)) {
                Icon(Icons.Rounded.OpenInNew, null, Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("Open actual Android Battery Saver settings", fontSize = 12.sp)
            }
            Divider(color = PanelBorder)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.SettingsPower, null, tint = Mint)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Battery optimization exemption", fontSize = 14.sp)
                    Text("Ensure Voltwise is not restricted by OS background limits.", color = Muted, fontSize = 11.sp)
                }
            }
            OutlinedButton(onClick = { 
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply { data = Uri.parse("package:${context.packageName}") })
                }.onFailure {
                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, PanelBorder)) {
                Icon(Icons.Rounded.Shield, null, Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("Open battery optimization settings", fontSize = 12.sp)
            }
            Divider(color = PanelBorder)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.BatteryChargingFull, null, tint = Mint)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Voltwise power saving", fontSize = 14.sp)
                    Text("Use fewer background samples whenever you want lower internal app battery use.", color = Muted, fontSize = 11.sp)
                }
                Switch(checked = state.powerSavingMode, onCheckedChange = viewModel::updatePowerSavingMode)
            }
            Text(if(state.powerSavingMode) "Sampling every 30 seconds." else "Sampling every 5 seconds for richer analytics.", color = Muted, fontSize = 11.sp)
            Divider(color = PanelBorder)
            PremiumSwitchRow(Icons.Rounded.Bedtime, "Night charging protection", "Overnight alert near 80-90% to reduce battery aging.", state.nightChargingMode, viewModel::updateNightChargingMode)
            Divider(color = PanelBorder)
            PremiumSwitchRow(Icons.Rounded.NightsStay, "Optimize standby mode", "When unplugged, Voltwise samples less often to reduce standby drain.", state.standbyOptimizeMode, viewModel::updateStandbyOptimizeMode)
        } }

        item { ReadingCard("Monitoring & privacy") {
            InsightLine(Icons.Rounded.Sensors, "Foreground monitoring", "Voltwise keeps a small ongoing service so charge, discharge, temperature and events can be recorded reliably.")
            Divider(color = PanelBorder)
            InsightLine(Icons.Rounded.Lock, "Local history (100% On-Device)", "Battery samples stay entirely on this device. Voltwise does not require an account, cloud sync, or remote servers.")
            Divider(color = PanelBorder)
            InsightLine(Icons.Rounded.Info, "Authentic metrics", "Hardware sensors are read directly from the OS. Unsupported sensors are accurately marked rather than faked.")
        } }

        item { ReadingCard("Alerts") {
            AlertSliderRow(Icons.Rounded.NotificationsActive, "Charge target alert", "Only alert when charging reaches your selected ${state.alertChargeTargetLevel}%.", state.alertChargeTargetEnabled, state.alertChargeTargetLevel.toFloat(), 50f..100f, 49, "${state.alertChargeTargetLevel}%", { viewModel.updateChargeTarget(it, state.alertChargeTargetLevel) }, { viewModel.updateChargeTarget(true, it.roundToInt().coerceIn(50, 100)) })
            Divider(color = PanelBorder)
            AlertSliderRow(Icons.Rounded.BatteryAlert, "Low battery", "Alert when unplugged battery drops to ${state.alertLowBatteryLevel}%", state.alertLowBatteryEnabled, state.alertLowBatteryLevel.toFloat(), 5f..50f, 44, "${state.alertLowBatteryLevel}%", { viewModel.updateLowBatteryAlert(it, state.alertLowBatteryLevel) }, { viewModel.updateLowBatteryAlert(true, it.roundToInt().coerceIn(5, 50)) })
            Divider(color = PanelBorder)
            AlertSliderRow(Icons.Rounded.DeviceThermostat, "High temperature", "Alert when battery reaches ${state.alertHighTempThreshold.roundToInt()}°C", state.alertHighTempEnabled, state.alertHighTempThreshold, 35f..55f, 19, "${state.alertHighTempThreshold.roundToInt()}°C", { viewModel.updateHighTempAlert(it, state.alertHighTempThreshold) }, { viewModel.updateHighTempAlert(true, it.roundToInt().coerceIn(35, 55).toFloat()) })
            Divider(color = PanelBorder)
            PremiumSwitchRow(Icons.Rounded.Summarize, "Daily battery report", "Optional night report. Keep this off if you only want the selected percentage alert.", state.dailyReportEnabled, viewModel::updateDailyReport)
            Text("The ongoing Voltwise monitor notification is quiet and required by Android while live monitoring runs. Loud alerts follow the switches above.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
        } }

        item { ReadingCard("System battery settings") {
            InsightLine(Icons.Rounded.BatteryUnknown, "Android system battery usage", "View system-reported app battery consumption in Android settings.")
            OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_POWER_USAGE_SUMMARY)) }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, PanelBorder)) {
                Icon(Icons.Rounded.OpenInNew, null, Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("Open system battery usage", fontSize = 12.sp)
            }
            Divider(color = PanelBorder)
            InsightLine(Icons.Rounded.SettingsApplications, "Voltwise app details", "Open system app settings to manage permissions, notifications, or storage.")
            OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.parse("package:${context.packageName}") }) }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, PanelBorder)) {
                Icon(Icons.Rounded.Settings, null, Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("Open Voltwise app settings", fontSize = 12.sp)
            }
        } }

        item { ReadingCard("Play Store & Legal") {
            InsightLine(Icons.Rounded.Security, "Privacy Policy", "Review how Voltwise handles your data locally with zero cloud collection.")
            OutlinedButton(onClick = { showPrivacyDialog = true }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, PanelBorder)) {
                Icon(Icons.Rounded.Description, null, Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("View In-App Privacy Policy", fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { 
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com")))
                }
            }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, PanelBorder)) {
                Icon(Icons.Rounded.OpenInNew, null, Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("Open Online Privacy Policy URL", fontSize = 12.sp)
            }
            Divider(color = PanelBorder)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Voltwise Version", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("v1.0 (Build 1) • Production Ready", color = Muted, fontSize = 11.sp)
                }
                Surface(color = Mint.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text("PLAY READY", color = Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        } }

        item { ReadingCard("On your device") {
            Icon(Icons.Rounded.Shield, null, tint = Mint, modifier = Modifier.size(28.dp))
            Text("Your charging history stays local.", fontSize = 14.sp)
            Text("Remove recorded sessions, samples and events whenever you choose.", color = Muted, fontSize = 12.sp)
            TextButton(onClick = { confirmClear = true }) { Text("Clear recorded history", color = Amber, fontSize = 12.sp) }
        } }

        item { Text("VOLTWISE X  /  BATTERY INTELLIGENCE", color = Muted, fontSize = 9.sp, letterSpacing = 1.5.sp, modifier = Modifier.padding(vertical = 8.dp)) }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear your history?") },
            text = { Text("This removes saved sessions, samples and events. It cannot be undone.") },
            confirmButton = { TextButton(onClick = { viewModel.clearData(); confirmClear = false }) { Text("Clear history", color = Amber) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Keep my data") } }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Voltwise Privacy Policy") },
            text = { 
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. Data Collection: Voltwise collects battery metrics (charge level, temperature, voltage) exclusively on your device.")
                    Text("2. No Cloud Storage: All historical data and logs remain in a local encrypted database. No data is sent to external servers.")
                    Text("3. Permissions: Foreground service and notifications are used solely for real-time battery monitoring and alerts.")
                    Text("4. Play Store Compliance: This app complies with Google Play developer policies.")
                }
            },
            confirmButton = { TextButton(onClick = { showPrivacyDialog = false }) { Text("Close") } }
        )
    }
}

@Composable
private fun InsightLine(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, detail: String) {
    val text = localizedUiText()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Surface(color = Mint.copy(alpha = .10f), shape = RoundedCornerShape(12.dp)) {
            Icon(icon, null, tint = Mint, modifier = Modifier.padding(8.dp).size(18.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text(title), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(text(detail), color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun AlertSliderRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    detail: String,
    checked: Boolean,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueText: String,
    onCheckedChange: (Boolean) -> Unit,
    onValueChange: (Float) -> Unit
) {
    val text = localizedUiText()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = Lavender)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text(title), fontSize = 14.sp)
            Text(text(detail), color = Muted, fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            modifier = Modifier.weight(1f)
        )
        Surface(color = Mint.copy(alpha = .10f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Mint.copy(alpha = .45f))) {
            Text(valueText, color = Mint, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
        }
    }
}

@Composable
private fun PremiumSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    detail: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val text = localizedUiText()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = Mint)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text(title), fontSize = 14.sp)
            Text(text(detail), color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
