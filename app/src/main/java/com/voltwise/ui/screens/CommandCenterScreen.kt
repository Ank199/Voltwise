package com.voltwise.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.voltwise.data.local.entity.BatterySessionEntity
import com.voltwise.data.local.entity.BatterySnapshotEntity
import com.voltwise.service.BatteryReading
import com.voltwise.service.batteryVolts
import com.voltwise.service.currentMilliAmps
import com.voltwise.ui.viewmodel.LiveViewModel
import com.voltwise.ui.viewmodel.SettingsViewModel
import java.text.DateFormat
import java.util.Date
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun CommandCenterScreen(navController: NavController, viewModel: LiveViewModel = viewModel()) {
    val copy = remember { commandCopy("en") }
    LiveDashboard(navController, viewModel) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { navController.navigate("forecast") }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, PanelBorder)) {
                Icon(Icons.Rounded.AutoGraph, null, Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Text(copy.forecast, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun LiveDashboard(navController: NavController? = null, vm: LiveViewModel = viewModel(), navigation: @Composable () -> Unit = {}) {
    val settings: SettingsViewModel = viewModel()
    val preferences by settings.uiState.collectAsState()
    val copy = remember { commandCopy("en") }
    val text = remember { { value: String -> appText("en", value) } }
    val reading by vm.reading.collectAsState()
    val active by vm.monitoring.collectAsState()
    val sessions by vm.sessions.collectAsState()
    val snapshots by vm.snapshots.collectAsState()
    var debug by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Ink)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(Modifier.size(34.dp).background(Mint, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Bolt, null, tint = Ink, modifier = Modifier.size(22.dp))
                }
                Text("voltwise", fontWeight = FontWeight.Bold, fontSize = 23.sp, letterSpacing = (-.8).sp)
                Text("X", color = Mint, fontWeight = FontWeight.Light, fontSize = 20.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(if(active) copy.live else copy.paused, if(active) Mint else Amber)
                IconButton(onClick = { navController?.navigate("settings") }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = Muted, modifier = Modifier.size(20.dp))
                }
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TabPill(copy.overview, true) { }
                TabPill("Lab", false) { navController?.navigate("lab") }
                TabPill(copy.timeline, false) { navController?.navigate("timeline") }
                TabPill(copy.forecast, false) { navController?.navigate("forecast") }
                TabPill(copy.analytics, false) { navController?.navigate("insights") }
                TabPill("Doctor", false) { navController?.navigate("doctor") }
                TabPill("Missions", false) { navController?.navigate("missions") }
            }
        }
        item { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(copy.headline, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
            Text(copy.subtitle, color = Muted, fontSize = 12.sp)
        } }
        val b = reading
        if(b == null) item { ReadingCard(copy.connecting) { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Mint); Text(copy.waiting, color = Muted) } }
        if(b != null) {
            item { PremiumAlwaysOnCard(b, sessions, copy) }
            item { ChargingCoachCard(b, sessions) }
            if(snapshots.size < 6 && sessions.isEmpty()) item { GettingStartedCard() }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(copy.powerMonitor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(copy.deviceSensors, color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
                }
            }
            item {
                val ma = b.currentMa
                val currentIndicator = when {
                    !b.plugged -> copy.discharging
                    ma == null -> copy.notSupported
                    ma < 300f -> copy.extremeSlow
                    ma < 800f -> copy.slow
                    ma < 1500f -> copy.general
                    ma < 3000f -> copy.fast
                    else -> copy.superFast
                }
                val indicatorColor = when {
                    !b.plugged -> Muted
                    ma == null -> Muted
                    ma < 300f -> Color(0xFFF44336) // Red
                    ma < 800f -> Color(0xFFFF5252) // Light Red
                    ma < 1500f -> Amber // Medium Amber
                    ma < 3000f -> Color(0xFF8BC34A) // Green
                    else -> Mint // Neon Green
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SensorTile(copy.batteryCurrent, sensorNumber(b.currentMa, 0), "mA", currentIndicator, Icons.Rounded.ElectricBolt, Modifier.weight(1f), accent = indicatorColor, indicatorColor = indicatorColor)
                        SensorTile(copy.temperature, sensorNumber(b.temperature?.let { if(preferences.useCelsius) it else it * 9 / 5 + 32 }), if(preferences.useCelsius) "°C" else "°F", if(b.temperature == null) copy.notSupported else copy.realBattery, Icons.Rounded.Thermostat, Modifier.weight(1f), Amber)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SensorTile(copy.batteryVoltage, sensorNumber(b.volts, 2), "V", if(b.volts == null) copy.notSupported else copy.realBattery, Icons.Rounded.Speed, Modifier.weight(1f), Lavender)
                        SensorTile(copy.batteryPower, sensorNumber(b.watts), "W", if(b.watts == null) copy.notSupported else copy.estimatedPower, Icons.Rounded.Bolt, Modifier.weight(1f), Color(0xFFA9D5F4))
                    }
                }
            }
            item { LivePowerGraphs(b, snapshots, preferences.useCelsius) }
            item { ChargeHealthCard(b, sessions) }
            if(b.currentMa == null || b.watts == null) item {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Icon(Icons.Rounded.Info, null, tint = Muted, modifier = Modifier.size(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if(b.currentMa == null) Text(text("Current not supported on this device"), color = Muted, fontSize = 11.sp)
                        if(b.watts == null) Text(text("Wattage not supported on this device"), color = Muted, fontSize = 11.sp)
                    }
                }
            }
            item {
                val session = sessions.firstOrNull { it.endTime == null && it.isCharging }
                ReadingCard("This charging session") {
                    if(session == null) Text(text("Connect a charger to start your next session."), color = Muted, fontSize = 13.sp)
                    else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column { Text("${((b.timestamp-session.startTime)/60000).coerceAtLeast(0)} ${text("min")}", fontSize = 25.sp, fontWeight = FontWeight.Medium); Text(text("Connected"), color = Muted, fontSize = 11.sp) }
                            Column(horizontalAlignment = Alignment.End) { Text("${(session.endLevel ?: session.startLevel)-session.startLevel}%", fontSize = 25.sp, fontWeight = FontWeight.Medium, color = Mint); Text(text("Battery gained"), color = Muted, fontSize = 11.sp) }
                        }
                        Divider(color = PanelBorder)
                        Text("${session.startLevel}% → ${session.endLevel ?: session.startLevel}%   ·   ${session.chargingSource}", color = Muted, fontSize = 12.sp)
                        Text("${text("Connected at 100%")}: ${session.overchargeDuration/60000} ${text("min")}", color = Muted, fontSize = 11.sp)
                    }
                }
            }
            item { navigation() }
            item { ReadingCard("Battery details") {
                DetailRow("Health", b.health); DetailRow("Technology", b.technology)
                Text(text("Power reflects the battery, not the charger's rated output."), color = Muted, fontSize = 11.sp)
            } }
            item { TextButton(onClick = { debug = !debug }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Code, null, Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text(text(if(debug) "Hide sensor diagnostics" else "Sensor diagnostics"), fontSize = 12.sp)
            } }
            if(debug) item { ReadingCard("Raw sensor values") {
                DetailRow("EXTRA_VOLTAGE", "${b.rawVoltage}")
                DetailRow("CURRENT_NOW", "${b.rawCurrent}")
                DetailRow("Converted current", measured(b.currentMa, "mA"))
                Text(text(if(b.currentInMilliAmps) "Current units: mA · RMX2151 firmware correction" else "Current units: µA ÷ 1000"), color = Muted, fontSize = 11.sp)
                DetailRow("Status / source", "${b.status} / ${b.source}")
                DetailRow("Raw temperature", "${b.rawTemperature}")
                DetailRow("Battery level", "${b.level}%")
                Text("${text("Updated")} ${DateFormat.getTimeInstance().format(Date(b.timestamp))}", color = Muted, fontSize = 11.sp)
            } }
        }
        }
    }
}

private data class ElectricalPoint(
    val timestamp: Long,
    val currentMa: Float?,
    val volts: Float?,
    val watts: Float?,
    val temperature: Float?
)

private data class CommandCopy(
    val live: String,
    val paused: String,
    val overview: String,
    val timeline: String,
    val forecast: String,
    val analytics: String,
    val headline: String,
    val subtitle: String,
    val language: String,
    val languageDetail: String,
    val connecting: String,
    val waiting: String,
    val chargingStatus: String,
    val premiumAod: String,
    val charging: String,
    val ready: String,
    val learningSpeed: String,
    val minutesToFull: String,
    val powerMonitor: String,
    val deviceSensors: String,
    val discharging: String,
    val notSupported: String,
    val extremeSlow: String,
    val slow: String,
    val general: String,
    val fast: String,
    val superFast: String,
    val batteryCurrent: String,
    val temperature: String,
    val batteryVoltage: String,
    val batteryPower: String,
    val realBattery: String,
    val estimatedPower: String
)

private fun commandCopy(@Suppress("UNUSED_PARAMETER") code: String): CommandCopy = CommandCopy(
    live = "LIVE",
    paused = "PAUSED",
    overview = "Overview",
    timeline = "Activity",
    forecast = "Forecast",
    analytics = "Analytics",
    headline = "Your battery, made simple.",
    subtitle = "Live speed, heat, power and health in plain English.",
    language = "Language",
    languageDetail = "English only",
    connecting = "Connecting to your battery",
    waiting = "Waiting for the first device reading...",
    chargingStatus = "Charging status",
    premiumAod = "Premium always-on display",
    charging = "CHARGING",
    ready = "READY",
    learningSpeed = "Learning speed",
    minutesToFull = "~%d min to full",
    powerMonitor = "Live power monitor",
    deviceSensors = "EASY READINGS",
    discharging = "Discharging",
    notSupported = "Not supported",
    extremeSlow = "Very slow",
    slow = "Slow",
    general = "Normal",
    fast = "Fast",
    superFast = "Super fast",
    batteryCurrent = "Charging speed",
    temperature = "Battery heat",
    batteryVoltage = "Voltage",
    batteryPower = "Power",
    realBattery = "Live battery sensor",
    estimatedPower = "Estimated from voltage × current"
)
@Composable
private fun PremiumAlwaysOnCard(reading: BatteryReading, sessions: List<BatterySessionEntity>, copy: CommandCopy) {
    var showCurrent by rememberSaveable { mutableStateOf(true) }
    var showWatts by rememberSaveable { mutableStateOf(true) }
    var showTemp by rememberSaveable { mutableStateOf(true) }
    var compact by rememberSaveable { mutableStateOf(false) }
    val session = sessions.firstOrNull { it.endTime == null && it.isCharging }
    val speed = activeSessionSpeed(reading, session) ?: recentAverageChargeSpeed(sessions)
    val minutesToFull = chargeMinutesRemaining(reading.level, speed)
    val levelFraction by animateFloatAsState((reading.level ?: 0) / 100f, tween(900), label = "premiumAodLevel")
    val infiniteTransition = rememberInfiniteTransition(label = "premiumAodMotion")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = if (reading.plugged) .35f else .18f,
        targetValue = if (reading.plugged) .95f else .34f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "premiumGlowAlpha"
    )
    val boltScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (reading.plugged) 1.18f else 1f,
        animationSpec = infiniteRepeatable(tween(780), RepeatMode.Reverse),
        label = "premiumBoltScale"
    )
    val sweep by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4200), RepeatMode.Restart),
        label = "premiumSweep"
    )

    Surface(shape = RoundedCornerShape(30.dp), color = Panel, border = BorderStroke(1.dp, Mint.copy(alpha = .35f))) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF0F221D),
                            Color(0xFF141C27),
                            Color(0xFF20203A),
                            Color(0xFF111821)
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(copy.chargingStatus, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                    Text(copy.premiumAod, color = Muted, fontSize = 11.sp)
                }
                StatusPill(if (reading.plugged) copy.charging else copy.ready, if (reading.plugged) Mint else Lavender)
            }
            Surface(shape = RoundedCornerShape(26.dp), color = Ink.copy(alpha = .62f), border = BorderStroke(1.dp, Color.White.copy(alpha = .08f))) {
                Column(Modifier.fillMaxWidth().padding(if (compact) 14.dp else 18.dp), verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(if (compact) 168.dp else 214.dp)
                            .semantics { contentDescription = "Premium charging status ${reading.level ?: "unknown"} percent" },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            val inset = 16.dp.toPx()
                            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                            val stroke = if (compact) 9.dp.toPx() else 12.dp.toPx()
                            drawCircle(Mint.copy(alpha = .05f + glowAlpha * .08f), radius = size.minDimension * .42f, center = center)
                            drawArc(
                                Color.White.copy(alpha = .07f),
                                130f,
                                280f,
                                false,
                                Offset(inset, inset),
                                arcSize,
                                style = Stroke(stroke, cap = StrokeCap.Round)
                            )
                            drawArc(
                                Brush.sweepGradient(
                                    listOf(
                                        Mint.copy(alpha = .25f),
                                        Mint,
                                        Lavender,
                                        Color(0xFFA9D5F4),
                                        Mint.copy(alpha = .25f)
                                    )
                                ),
                                130f + if (reading.plugged) sweep / 18f else 0f,
                                280f * levelFraction.coerceIn(0f, 1f),
                                false,
                                Offset(inset, inset),
                                arcSize,
                                style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                            if (reading.plugged) {
                                drawArc(
                                    Mint.copy(alpha = .12f * glowAlpha),
                                    sweep,
                                    42f,
                                    false,
                                    Offset(inset, inset),
                                    arcSize,
                                    style = Stroke((stroke * 1.75f), cap = StrokeCap.Round)
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Box(
                                Modifier
                                    .size(if (compact) 34.dp else 42.dp)
                                    .background(Mint.copy(alpha = .12f + glowAlpha * .12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (reading.plugged) Icons.Rounded.Bolt else Icons.Rounded.BatteryStd,
                                    null,
                                    tint = Mint,
                                    modifier = Modifier
                                        .size(if (compact) 20.dp else 25.dp)
                                        .graphicsLayer(scaleX = boltScale, scaleY = boltScale)
                                )
                            }
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(reading.level?.toString() ?: "—", fontSize = if (compact) 48.sp else 64.sp, fontWeight = FontWeight.Light, letterSpacing = (-2).sp, color = Color(0xFFF3FFF7))
                                Text("%", fontSize = if (compact) 18.sp else 24.sp, color = Mint, modifier = Modifier.padding(bottom = if (compact) 8.dp else 12.dp, start = 3.dp))
                            }
                            Text(reading.status.uppercase(), color = Mint.copy(alpha = .9f), fontSize = 11.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (showCurrent) MiniAodMetric("Current", sensorNumber(reading.currentMa, 0), "mA", Mint, Modifier.weight(1f))
                        if (showWatts) MiniAodMetric("Power", sensorNumber(reading.watts), "W", Lavender, Modifier.weight(1f))
                        if (showTemp) MiniAodMetric("Temp", sensorNumber(reading.temperature), "C", Amber, Modifier.weight(1f))
                    }
                    Surface(shape = CircleShape, color = Mint.copy(alpha = .08f), border = BorderStroke(1.dp, Mint.copy(alpha = .18f))) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                Icon(Icons.Rounded.Power, null, tint = Mint, modifier = Modifier.size(16.dp))
                                Text(reading.source, fontSize = 12.sp, color = Color(0xFFEAFBF1), fontWeight = FontWeight.SemiBold)
                            }
                            Text(minutesToFull?.let { copy.minutesToFull.format(it) } ?: copy.learningSpeed, color = Muted, fontSize = 11.sp)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToggleChip("Current", showCurrent) { showCurrent = !showCurrent }
                ToggleChip("Wattage", showWatts) { showWatts = !showWatts }
                ToggleChip("Temp", showTemp) { showTemp = !showTemp }
                ToggleChip("Compact", compact) { compact = !compact }
            }
        }
    }
}

@Composable
private fun MiniAodMetric(label: String, value: String, unit: String, color: Color, modifier: Modifier) {
    val text = localizedUiText()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text(label), color = Muted, fontSize = 10.sp)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(" $unit", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(bottom = 2.dp))
        }
    }
}

@Composable
private fun ToggleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val text = localizedUiText()
    Surface(
        shape = CircleShape,
        color = if (selected) Mint.copy(alpha = .16f) else Color.Transparent,
        border = BorderStroke(1.dp, if (selected) Mint.copy(alpha = .55f) else PanelBorder),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (selected) Icon(Icons.Rounded.Check, null, tint = Mint, modifier = Modifier.size(14.dp))
            Text(text(label), color = if (selected) Mint else Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LivePowerGraphs(reading: BatteryReading, snapshots: List<BatterySnapshotEntity>, useCelsius: Boolean) {
    val text = localizedUiText()
    var rangeHours by rememberSaveable { mutableStateOf(6) }
    val newest = reading.timestamp
    val points = remember(reading, snapshots, rangeHours) {
        val saved = snapshots.map {
            val current = currentMilliAmps(it.currentNow)
            val volts = batteryVolts(it.voltage)
            ElectricalPoint(it.timestamp, current, volts, current?.let { ma -> volts?.let { v -> v * ma / 1000f } }, it.temperature)
        }
        val live = ElectricalPoint(reading.timestamp, reading.currentMa, reading.volts, reading.watts, reading.temperature)
        (saved + live)
            .distinctBy { it.timestamp }
            .filter { newest - it.timestamp <= rangeHours * 60L * 60L * 1000L }
            .sortedBy { it.timestamp }
            .takeLast(96)
    }
    ReadingCard("Easy live graphs") {
        Text(text("Simple lines: higher means more of that reading. The dot at the end is your latest value."), color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 6, 24).forEach { hours -> TabPill("${hours}H", rangeHours == hours) { rangeHours = hours } }
        }
        MetricLineChart(
            title = "Charging strength",
            helper = "Current shows charging speed. Voltage shows battery pressure.",
            primaryLabel = "Current",
            primaryUnit = "mA",
            secondaryLabel = "Voltage",
            secondaryUnit = "V",
            points = points,
            primary = { it.currentMa },
            secondary = { it.volts },
            primaryColor = Mint,
            secondaryColor = Lavender,
            contentDescription = "Current and voltage live graph"
        )
        MetricLineChart(
            title = "Power going into battery",
            helper = "More watts usually means faster charging.",
            primaryLabel = "Power",
            primaryUnit = "W",
            secondaryLabel = null,
            secondaryUnit = null,
            points = points,
            primary = { it.watts },
            secondary = { null },
            primaryColor = Color(0xFFA9D5F4),
            secondaryColor = Muted,
            contentDescription = "Wattage live graph"
        )
        MetricLineChart(
            title = "Battery temperature",
            helper = "Lower and steady is better for battery health.",
            primaryLabel = "Temp",
            primaryUnit = if (useCelsius) "C" else "F",
            secondaryLabel = null,
            secondaryUnit = null,
            points = points.map { if (useCelsius) it else it.copy(temperature = it.temperature?.let { c -> c * 9 / 5 + 32 }) },
            primary = { it.temperature },
            secondary = { null },
            primaryColor = Amber,
            secondaryColor = Muted,
            contentDescription = "Temperature live graph"
        )
    }
}

@Composable
private fun MetricLineChart(
    title: String,
    helper: String,
    primaryLabel: String,
    primaryUnit: String,
    secondaryLabel: String?,
    secondaryUnit: String?,
    points: List<ElectricalPoint>,
    primary: (ElectricalPoint) -> Float?,
    secondary: (ElectricalPoint) -> Float?,
    primaryColor: Color,
    secondaryColor: Color,
    contentDescription: String
) {
    val text = localizedUiText()
    val primaryValues = points.mapNotNull(primary)
    val secondaryValues = points.mapNotNull(secondary)
    val latestPrimary = points.asReversed().firstNotNullOfOrNull(primary)
    val latestSecondary = points.asReversed().firstNotNullOfOrNull(secondary)
    var animateLines by remember(points.size, title) { mutableStateOf(false) }
    LaunchedEffect(points.size, title) { animateLines = true }
    val lineProgress by animateFloatAsState(if (animateLines) 1f else 0f, tween(850), label = "${title}GraphReveal")

    Surface(shape = RoundedCornerShape(22.dp), color = Ink.copy(alpha = .36f), border = BorderStroke(1.dp, PanelBorder.copy(alpha = .8f))) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(text(title), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(text(helper), color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    LegendDot(primaryLabel, primaryColor)
                    secondaryLabel?.let { LegendDot(it, secondaryColor) }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GraphStat("Now", latestPrimary?.let { graphNumber(it, primaryUnit) } ?: text("Learning"), primaryColor, Modifier.weight(1f))
                GraphStat("High", primaryValues.maxOrNull()?.let { graphNumber(it, primaryUnit) } ?: "--", Color(0xFFF0F4F8), Modifier.weight(1f))
                GraphStat("Low", primaryValues.minOrNull()?.let { graphNumber(it, primaryUnit) } ?: "--", Color(0xFFF0F4F8), Modifier.weight(1f))
            }
            if (secondaryUnit != null && latestSecondary != null) {
                Text("${text(secondaryLabel ?: "")} ${text("now")} ${graphNumber(latestSecondary, secondaryUnit)}", color = secondaryColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(124.dp)
                    .semantics { this.contentDescription = contentDescription }
            ) {
                val left = 8.dp.toPx()
                val right = size.width - 8.dp.toPx()
                val top = 14.dp.toPx()
                val bottom = size.height - 18.dp.toPx()
                listOf(.33f, .66f).forEach { fraction ->
                    val y = top + (bottom - top) * fraction
                    drawLine(PanelBorder.copy(alpha = .35f), Offset(left, y), Offset(right, y), strokeWidth = 1.dp.toPx())
                }
                if (points.size < 2 || primaryValues.size < 2) {
                    drawLine(Muted.copy(alpha = .42f), Offset(left, bottom), Offset(right, bottom), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                    return@Canvas
                }
                fun drawSeries(values: (ElectricalPoint) -> Float?, color: Color, strokeWidth: Float) {
                    val series = points.mapNotNull { point -> values(point)?.takeIf { it.isFinite() }?.let { point.timestamp to it } }
                    if (series.size < 2) return
                    val visibleCount = (2 + ((series.size - 2) * lineProgress)).roundToInt().coerceIn(2, series.size)
                    val visible = series.take(visibleCount)
                    val minTime = points.first().timestamp
                    val maxTime = points.last().timestamp
                    val timeRange = (maxTime - minTime).coerceAtLeast(1L)
                    val minValue = series.minOf { it.second }
                    val maxValue = series.maxOf { it.second }
                    val range = (maxValue - minValue).takeIf { it > 0f } ?: 1f
                    val path = Path()
                    visible.forEachIndexed { index, (timestamp, value) ->
                        val x = left + ((timestamp - minTime).toFloat() / timeRange) * (right - left)
                        val y = bottom - ((value - minValue) / range) * (bottom - top)
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    visible.lastOrNull()?.let { (timestamp, value) ->
                        val x = left + ((timestamp - minTime).toFloat() / timeRange) * (right - left)
                        val y = bottom - ((value - minValue) / range) * (bottom - top)
                        drawCircle(color.copy(alpha = .16f), radius = 11.dp.toPx(), center = Offset(x, y))
                        drawCircle(color, radius = 4.8.dp.toPx(), center = Offset(x, y))
                    }
                }
                drawSeries(primary, primaryColor, 4.dp.toPx())
                if (secondaryValues.size >= 2) drawSeries(secondary, secondaryColor.copy(alpha = .82f), 2.5.dp.toPx())
            }
            Text(if (primaryValues.size < 2) text("Collecting more readings to draw this clearly.") else "${primaryValues.size} ${text("readings in this view")}", color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    val text = localizedUiText()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(7.dp).background(color, CircleShape))
        Text(text(label), color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun GraphStat(label: String, value: String, color: Color, modifier: Modifier) {
    val text = localizedUiText()
    Surface(modifier, shape = RoundedCornerShape(14.dp), color = Panel.copy(alpha = .62f), border = BorderStroke(1.dp, PanelBorder.copy(alpha = .65f))) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text(label), color = Muted, fontSize = 10.sp)
            Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun ChargingCoachCard(reading: BatteryReading, sessions: List<BatterySessionEntity>) {
    val activeSession = sessions.firstOrNull { it.endTime == null && it.isCharging }
    val activeSpeed = activeSessionSpeed(reading, activeSession)
    val averageSpeed = recentAverageChargeSpeed(sessions)
    val bestSpeed = sessions.mapNotNull { it.percentPerHour() }.maxOrNull()
    val minutesToFull = chargeMinutesRemaining(reading.level, activeSpeed ?: averageSpeed)
    val currentMa = reading.currentMa
    val heat = reading.temperature
    val score = careScore(reading.health, sessions)
    val speedLabel = when {
        !reading.plugged -> "Not charging"
        currentMa == null -> "Learning"
        currentMa < 800f -> "Slow"
        currentMa < 1500f -> "Normal"
        currentMa < 3000f -> "Fast"
        else -> "Excellent"
    }
    val heatLabel = when {
        heat == null -> "Unknown heat"
        heat < 35f -> "Cool"
        heat < 42f -> "Warm"
        else -> "Hot"
    }
    val verdict = when {
        !reading.plugged -> "Connect your charger to start a live charging score."
        heat != null && heat >= 42f -> "Charging is hot. Remove thick covers and keep the phone in open air."
        currentMa != null && currentMa < 800f -> "Charging is slow. Try a stronger adapter or another cable."
        score >= 85 -> "Great charging condition. Speed and battery care look healthy."
        else -> "Good condition. VoltWise will improve estimates as it learns more sessions."
    }

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Panel,
        border = BorderStroke(1.dp, Mint.copy(alpha = .22f))
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF121F25),
                            Color(0xFF111821),
                            Color(0xFF1B2131)
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Charging coach", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("Simple answer from the live readings", color = Muted, fontSize = 11.sp)
                }
                StatusPill("${score}/100", if (score >= 85) Mint else if (score >= 70) Amber else Color(0xFFFF8A80))
            }
            Text(verdict, color = Color(0xFFEAF7F3), fontSize = 13.sp, lineHeight = 19.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CoachMiniStat("Speed", speedLabel, Icons.Rounded.Bolt, if (speedLabel == "Excellent" || speedLabel == "Fast") Mint else Amber, Modifier.weight(1f))
                CoachMiniStat("Heat", heatLabel, Icons.Rounded.Thermostat, if (heatLabel == "Hot") Color(0xFFFF8A80) else Mint, Modifier.weight(1f))
                CoachMiniStat("Full in", minutesToFull?.let { "${it}m" } ?: "--", Icons.Rounded.Schedule, Lavender, Modifier.weight(1f))
            }
            Divider(color = PanelBorder.copy(alpha = .7f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Average ${averageSpeed?.let { "%.1f%%/hr".format(it) } ?: "learning"}", color = Muted, fontSize = 11.sp)
                Text("Best ${bestSpeed?.let { "%.1f%%/hr".format(it) } ?: "learning"}", color = Muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun CoachMiniStat(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(18.dp), color = Ink.copy(alpha = .46f), border = BorderStroke(1.dp, Color.White.copy(alpha = .07f))) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Text(value, color = Color(0xFFF3F7FB), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(label, color = Muted, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
private fun ChargeHealthCard(reading: BatteryReading, sessions: List<BatterySessionEntity>) {
    val activeSession = sessions.firstOrNull { it.endTime == null && it.isCharging }
    val activeSpeed = activeSessionSpeed(reading, activeSession)
    val averageSpeed = recentAverageChargeSpeed(sessions)
    val bestSpeed = sessions.mapNotNull { it.percentPerHour() }.maxOrNull()
    val speed = activeSpeed ?: averageSpeed
    val minutesToFull = chargeMinutesRemaining(reading.level, speed)
    val careScore = careScore(reading.health, sessions)

    ReadingCard("Charge time and health") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SensorTile("Time to full", minutesToFull?.toString() ?: "--", "min", if (reading.plugged) "Live estimate" else "Connect charger", Icons.Rounded.Schedule, Modifier.weight(1f), Mint)
            SensorTile("Health score", careScore.toString(), "/100", reading.health, Icons.Rounded.HealthAndSafety, Modifier.weight(1f), if (careScore >= 85) Mint else if (careScore >= 70) Amber else Color(0xFFFF8A80))
        }
        Divider(color = PanelBorder)
        DetailRow("Charging average speed", averageSpeed?.let { "%.1f %%/hr".format(it) } ?: "Learning")
        DetailRow("Current session speed", activeSpeed?.let { "%.1f %%/hr".format(it) } ?: "Learning")
        DetailRow("Best recorded speed", bestSpeed?.let { "%.1f %%/hr".format(it) } ?: "Learning")
        DetailRow("Battery health", reading.health)
    }
}

@Composable
private fun GettingStartedCard() {
    val text = localizedUiText()
    ReadingCard("Getting started") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.Route, null, tint = Mint, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text("Use your phone normally for a few hours."), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(text("Voltwise learns from plugged and unplugged samples. Activity, Insights and Forecast become more useful as history builds."), color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
        Divider(color = PanelBorder)
        Text(text("Tip: connect a charger and leave monitoring active for at least 10 minutes to unlock better charger speed estimates."), color = Muted, fontSize = 11.sp)
    }
}

private fun BatterySessionEntity.percentPerHour(): Float? {
    val end = endTime ?: return null
    val gained = ((endLevel ?: startLevel) - startLevel).coerceAtLeast(0)
    val hours = (end - startTime) / 3600000f
    return if (gained > 0 && hours > 0f) gained / hours else null
}

private fun activeSessionSpeed(reading: BatteryReading, session: BatterySessionEntity?): Float? {
    val active = session ?: return null
    val level = reading.level ?: return null
    val gained = (level - active.startLevel).coerceAtLeast(0)
    val hours = (reading.timestamp - active.startTime) / 3600000f
    return if (gained > 0 && hours > 0f) gained / hours else null
}

private fun recentAverageChargeSpeed(sessions: List<BatterySessionEntity>): Float? {
    val speeds = sessions.mapNotNull { it.percentPerHour() }.take(8)
    return if (speeds.isEmpty()) null else speeds.average().toFloat()
}

private fun chargeMinutesRemaining(level: Int?, speedPercentPerHour: Float?): Int? {
    val currentLevel = level ?: return null
    val speed = speedPercentPerHour?.takeIf { it > 0f } ?: return null
    if (currentLevel >= 100) return 0
    return ceil(((100 - currentLevel) / speed) * 60f).roundToInt().coerceAtLeast(1)
}

private fun careScore(health: String, sessions: List<BatterySessionEntity>): Int {
    val complete = sessions.filter { it.endTime != null }
    val hotSessions = complete.count { (it.maxTemperature ?: 0f) >= 42f }
    val longFullCharges = complete.count { it.overchargeDuration >= 30 * 60 * 1000L }
    val unstable = complete.count { it.stabilityScore in 0f..55f }
    val healthPenalty = if (health.equals("Good", true)) 0 else 12
    return (100 - hotSessions * 5 - longFullCharges * 4 - unstable * 3 - healthPenalty).coerceIn(50, 100)
}

private fun graphNumber(value: Float, unit: String): String {
    val decimals = when (unit) {
        "mA" -> 0
        "V" -> 2
        else -> 1
    }
    return ("%." + decimals + "f %s").format(value, unit)
}

@Composable
fun DetailRow(label: String, value: String) {
    val text = localizedUiText()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text(label), modifier = Modifier.weight(1f), color = Muted, fontSize = 12.sp)
        Text(text(value), modifier = Modifier.weight(1f), fontSize = 12.sp, textAlign = TextAlign.End)
    }
}
