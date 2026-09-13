package com.voltwise.service
import com.voltwise.data.local.entity.BatteryObservationEntity
import kotlin.math.sqrt
object SessionMetrics {
 fun mean(v: List<Float>): Float? = if (v.isEmpty()) null else v.average().toFloat()
 fun stability(v: List<Float>): Float? {
  if (v.size < 2) return null
  val avg = v.average(); if (avg <= 0) return null
  return (100 - sqrt(v.sumOf { (it - avg) * (it - avg) } / v.size) / avg * 100).toFloat().coerceIn(0f,100f)
 }
 fun sessionStability(samples: List<BatteryObservationEntity>): Float? = mean(listOfNotNull(
  stability(samples.mapNotNull { currentMilliAmps(it.currentNow) }),
  stability(samples.filter { (it.voltage ?: 0) > 0 }.map { it.voltage!!.toFloat() }),
  stability(samples.mapNotNull { s -> currentMilliAmps(s.currentNow)?.takeIf { (s.voltage ?: 0) > 0 }?.let { it * s.voltage!! / 1000000f } })))
 fun quality(s: List<BatteryObservationEntity>): String {
  if (s.size < 2 || s.last().timestamp - s.first().timestamp < 600000 || s.zipWithNext().any { (a,b) -> b.timestamp-a.timestamp > 30000 }) return "Connect charger for at least 10 minutes to test."
  val temps = s.mapNotNull { it.temperature }.filter { it.isFinite() }
  if (temps.isEmpty() || sessionStability(s) == null) return "Not supported: insufficient temperature or electrical sensor data for a quality result."
  val speed = (s.last().level-s.first().level).coerceAtLeast(0) * 3600000f / (s.last().timestamp-s.first().timestamp)
  val rise = if (temps.isEmpty()) 0f else (temps.max()-temps.first()).coerceAtLeast(0f)
  val heatEvents = temps.zipWithNext().count { (a,b) -> a < 45 && b >= 45 } + if (temps.firstOrNull()?.let { it >= 45 } == true) 1 else 0
  val score = (sessionStability(s)!!*0.5f + speed.coerceAtMost(50f) - rise*2 - heatEvents*10).coerceIn(0f,100f)
  val grade = when { score >= 85 -> "Excellent"; score >= 65 -> "Good"; score >= 40 -> "Average"; else -> "Poor" }
  return "$grade - Estimated score ${score.toInt()}/100; %.1f %%/h; temperature rise %.1f C; overheat events $heatEvents".format(speed,rise) + if (s.none { currentMilliAmps(it.currentNow) != null }) " (limited: current/wattage not supported)" else if (s.none { batteryVolts(it.voltage) != null }) " (limited: voltage/wattage not supported)" else ""
 }
}
