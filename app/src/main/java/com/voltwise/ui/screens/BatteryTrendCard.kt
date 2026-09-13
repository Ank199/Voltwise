package com.voltwise.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltwise.ui.viewmodel.LiveViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.abs

private data class BatteryTrendPoint(
    val timestamp: Long,
    val level: Int,
    val plugged: Boolean
)

private data class TrendRange(val label: String, val millis: Long)

@Composable
fun LiveBatteryTrendCard(vm: LiveViewModel) {
    val reading by vm.reading.collectAsState()
    val snapshots by vm.snapshots.collectAsState()
    var selectedRange by remember { mutableStateOf(TrendRange("6H", 6 * 60 * 60 * 1000L)) }
    val newestTimestamp = reading?.timestamp ?: System.currentTimeMillis()
    val saved = snapshots.map { BatteryTrendPoint(it.timestamp, it.level, it.plugged) }
    val live = reading?.level?.let { level -> BatteryTrendPoint(reading!!.timestamp, level, reading!!.plugged) }
    val trend = (saved + listOfNotNull(live))
        .distinctBy { it.timestamp }
        .filter { newestTimestamp - it.timestamp <= selectedRange.millis }
        .thinTo(120)

    BatteryTrendCard(trend, selectedRange) { selectedRange = it }
}

@Composable
private fun BatteryTrendCard(points: List<BatteryTrendPoint>, selectedRange: TrendRange, onRangeSelected: (TrendRange) -> Unit) {
    val text = localizedUiText()
    val ordered = points.sortedBy { it.timestamp }
    val first = ordered.firstOrNull()
    val last = ordered.lastOrNull()
    val delta = if (first != null && last != null) last.level - first.level else 0
    val elapsedMillis = if (first != null && last != null) (last.timestamp - first.timestamp).coerceAtLeast(0) else 0
    val percentPerHour = if (elapsedMillis > 0) delta * 3600000f / elapsedMillis else 0f
    val trendColor = when {
        delta > 0 -> Mint
        delta < 0 -> Amber
        else -> Muted
    }
    val title = if (ordered.size < 2) "Collecting battery activity" else "Battery percentage over time"
    val detail = when {
        ordered.size < 2 -> text("Keep monitoring active to build a simple percentage vs time graph.")
        elapsedMillis < 60000 -> text("Less than 1 min tracked")
        else -> "${elapsedMillis / 60000} min tracked - ${signedPercent(percentPerHour)}/hour"
    }
    var selectedPoint by remember(ordered) { mutableStateOf<BatteryTrendPoint?>(last) }
    var reveal by remember(ordered, selectedRange.label) { mutableStateOf(false) }
    LaunchedEffect(ordered, selectedRange.label) { reveal = true }
    val progress by animateFloatAsState(if (reveal) 1f else 0f, tween(950), label = "activityBatteryGraphReveal")

    Surface(shape = RoundedCornerShape(28.dp), color = Panel, border = BorderStroke(1.dp, trendColor.copy(alpha = .34f))) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF111B24), Color(0xFF19251F), Color(0xFF1B2033))))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text(title), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(text("Easy charging graph: higher line means more battery. Touch or drag to inspect any time."), color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
            }
            StatusPill(if (last?.plugged == true) "CHARGING" else "ACTIVITY", trendColor)
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                TrendRange("1H", 60 * 60 * 1000L),
                TrendRange("6H", 6 * 60 * 60 * 1000L),
                TrendRange("24H", 24 * 60 * 60 * 1000L),
                TrendRange("7D", 7L * 24 * 60 * 60 * 1000L),
                TrendRange("30D", 30L * 24 * 60 * 60 * 1000L)
            ).forEach { range ->
                TabPill(range.label, selectedRange.label == range.label) { onRangeSelected(range) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActivityGraphStat(text("Start"), first?.let { "${it.level}%" } ?: "--", Color(0xFFF0F4F8), Modifier.weight(1f))
            ActivityGraphStat(text("Now"), last?.let { "${it.level}%" } ?: "--", trendColor, Modifier.weight(1f))
            ActivityGraphStat(text("Change"), signedDelta(delta), if (delta >= 0) Mint else Amber, Modifier.weight(1f))
        }
        BatteryTrendChart(ordered, trendColor, selectedPoint, progress) { selectedPoint = it }
        selectedPoint?.let { point ->
            Surface(shape = RoundedCornerShape(18.dp), color = Ink.copy(alpha = .52f), border = BorderStroke(1.dp, Color.White.copy(alpha = .07f))) {
                Row(Modifier.fillMaxWidth().padding(13.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("${point.level}% ${text("battery")}", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFF3FFF7))
                        Text(fullTimeLabel(point.timestamp), color = Muted, fontSize = 11.sp)
                    }
                    StatusPill(if (point.plugged) "CHARGING" else "UNPLUGGED", if (point.plugged) Mint else Amber)
                }
            }
        }
        Surface(shape = CircleShape, color = trendColor.copy(alpha = .08f), border = BorderStroke(1.dp, trendColor.copy(alpha = .18f))) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Icon(Icons.Rounded.TouchApp, null, tint = trendColor, modifier = Modifier.size(16.dp))
                Text(detail, color = Muted, fontSize = 11.sp)
            }
        }
        }
    }
}

@Composable
private fun BatteryTrendChart(
    points: List<BatteryTrendPoint>,
    trendColor: Color,
    selectedPoint: BatteryTrendPoint?,
    progress: Float,
    onPointSelected: (BatteryTrendPoint?) -> Unit
) {
    val chartPoints = points.takeLast(120)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(190.dp)
            .pointerInput(chartPoints) {
                fun selectAt(position: Offset) {
                    if (chartPoints.isEmpty()) return
                    val left = 8.dp.toPx()
                    val right = size.width - 8.dp.toPx()
                    val minTime = chartPoints.first().timestamp
                    val maxTime = chartPoints.last().timestamp
                    val timeRange = (maxTime - minTime).coerceAtLeast(1L)
                    val targetTime = minTime + (((position.x - left) / (right - left)).coerceIn(0f, 1f) * timeRange).toLong()
                    onPointSelected(chartPoints.minByOrNull { abs(it.timestamp - targetTime) })
                }
                detectTapGestures(onPress = { offset ->
                    selectAt(offset)
                    tryAwaitRelease()
                })
            }
            .pointerInput(chartPoints) {
                fun selectAt(position: Offset) {
                    if (chartPoints.isEmpty()) return
                    val left = 8.dp.toPx()
                    val right = size.width - 8.dp.toPx()
                    val minTime = chartPoints.first().timestamp
                    val maxTime = chartPoints.last().timestamp
                    val timeRange = (maxTime - minTime).coerceAtLeast(1L)
                    val targetTime = minTime + (((position.x - left) / (right - left)).coerceIn(0f, 1f) * timeRange).toLong()
                    onPointSelected(chartPoints.minByOrNull { abs(it.timestamp - targetTime) })
                }
                detectDragGestures(
                    onDragStart = { selectAt(it) },
                    onDrag = { change, _ -> selectAt(change.position) }
                )
            }
            .semantics { contentDescription = "Battery percentage over time graph" }
    ) {
        val left = 14.dp.toPx()
        val right = size.width - 14.dp.toPx()
        val top = 16.dp.toPx()
        val bottom = size.height - 24.dp.toPx()

        listOf(25, 50, 75, 100).forEach { level ->
            val y = bottom - (level / 100f) * (bottom - top)
            drawLine(PanelBorder.copy(alpha = if (level == 50) .45f else .28f), Offset(left, y), Offset(right, y), strokeWidth = 1.dp.toPx())
        }

        if (chartPoints.size < 2) {
            drawLine(Muted.copy(alpha = .45f), Offset(left, bottom), Offset(right, bottom), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            return@Canvas
        }

        val minTime = chartPoints.first().timestamp
        val maxTime = chartPoints.last().timestamp
        val timeRange = (maxTime - minTime).coerceAtLeast(1L)
        val visibleCount = (2 + ((chartPoints.size - 2) * progress)).roundToInt().coerceIn(2, chartPoints.size)
        val visible = chartPoints.take(visibleCount)
        val path = Path()

        visible.forEachIndexed { index, point ->
            val x = left + ((point.timestamp - minTime).toFloat() / timeRange) * (right - left)
            val y = bottom - (point.level / 100f) * (bottom - top)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(path, trendColor.copy(alpha = .20f), style = Stroke(11.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path, trendColor.copy(alpha = .98f), style = Stroke(4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        visible.forEach { point ->
            val x = left + ((point.timestamp - minTime).toFloat() / timeRange) * (right - left)
            val y = bottom - (point.level / 100f) * (bottom - top)
            drawCircle(if (point.plugged) Mint else Amber, radius = 3.2.dp.toPx(), center = Offset(x, y))
        }
        val selected = selectedPoint?.takeIf { selected -> chartPoints.any { it.timestamp == selected.timestamp } } ?: visible.lastOrNull()
        selected?.let { point ->
            val x = left + ((point.timestamp - minTime).toFloat() / timeRange) * (right - left)
            val y = bottom - (point.level / 100f) * (bottom - top)
            drawLine(Color.White.copy(alpha = .28f), Offset(x, top), Offset(x, bottom), strokeWidth = 1.dp.toPx())
            drawCircle(Color.White.copy(alpha = .18f), radius = 15.dp.toPx(), center = Offset(x, y))
            drawCircle(Color.White, radius = 8.dp.toPx(), center = Offset(x, y), style = Stroke(2.dp.toPx()))
            drawCircle(if (point.plugged) Mint else Amber, radius = 5.dp.toPx(), center = Offset(x, y))
        }
    }
}

@Composable
private fun ActivityGraphStat(label: String, value: String, color: Color, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(14.dp), color = Ink.copy(alpha = .46f), border = BorderStroke(1.dp, PanelBorder.copy(alpha = .7f))) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = Muted, fontSize = 10.sp)
            Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

private fun signedPercent(value: Float): String {
    val rounded = (value * 10f).roundToInt() / 10f
    return if (rounded > 0f) "+%.1f%%".format(rounded) else "%.1f%%".format(rounded)
}

private fun timeLabel(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

private fun fullTimeLabel(timestamp: Long): String =
    SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(timestamp))

private fun signedDelta(value: Int): String =
    if (value > 0) "+$value%" else "$value%"

private fun List<BatteryTrendPoint>.thinTo(maxPoints: Int): List<BatteryTrendPoint> {
    if (size <= maxPoints) return this
    val step = size.toFloat() / maxPoints
    return List(maxPoints) { index -> this[(index * step).toInt().coerceIn(indices)] }.distinctBy { it.timestamp }
}
