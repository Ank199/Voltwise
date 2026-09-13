package com.voltwise
import com.voltwise.service.*
import com.voltwise.data.local.entity.BatteryObservationEntity
import org.junit.Assert.*
import org.junit.Test
class SensorMetricsTest {
 @Test fun signAndUnitConversion() {
  assertEquals(1500f,currentMilliAmps(1500000)!!,0.01f)
  assertEquals(1500f,currentMilliAmps(-1500000)!!,0.01f)
  assertEquals(0.5f,currentMilliAmps(500)!!,0.01f)
 }
 @Test fun verifiedMilliampFirmwareKeepsUnits() {
  assertEquals(184f,currentMilliAmps(-184,true)!!,0.01f)
  assertEquals(0.184f,currentMilliAmps(-184,false)!!,0.001f)
 }
 @Test fun unsupportedCurrentIsNeverInvented() {
  listOf(null,0,Int.MIN_VALUE,Int.MAX_VALUE).forEach { assertNull(currentMilliAmps(it)) }
 }
 @Test fun rejectsBrokenVoltageFromPhysicalPhone() {
  assertNull(batteryVolts(4)); assertNull(batteryVolts(0)); assertNull(batteryVolts(null))
  assertEquals(4.2f,batteryVolts(4200)!!,0.001f)
 }
 @Test fun wattageRequiresBothSensors() {
  val reading=BatteryReading(0,50,"Charging","USB",true,4200,-1500000,320,"Good","Li-ion")
  assertEquals(6.3f,reading.watts!!,0.01f)
  assertNull(reading.copy(rawCurrent=0).watts)
  assertNull(reading.copy(rawVoltage=4).watts)
 }
 @Test fun meanUsesAllSamplesAndStabilityPenalizesVariation() {
  assertEquals(20f,SessionMetrics.mean(listOf(10f,20f,30f))!!,0.01f)
  assertNull(SessionMetrics.mean(emptyList()))
  assertEquals(100f,SessionMetrics.stability(listOf(100f,100f))!!,0.01f)
  assertTrue(SessionMetrics.stability(listOf(1f,199f))!!<10f)
 }
 private fun samples(current: Int? = 1000000) = (0..120).map {
  BatteryObservationEntity(sessionId=1,timestamp=it*5000L,level=20+it/10,temperature=30f,voltage=4200,currentNow=current,timeToFull=null)
 }
 @Test fun qualityRequiresTenMinutesAndContinuousSamples() {
  assertTrue(SessionMetrics.quality(samples().dropLast(1)).startsWith("Connect charger"))
  assertTrue(SessionMetrics.quality(samples().filterIndexed { i,_ -> i !in 10..30 }).startsWith("Connect charger"))
  assertTrue(SessionMetrics.quality(samples()).startsWith("Excellent"))
 }
 @Test fun currentUnsupportedStillAllowsLimitedMeasuredResult() {
  assertTrue(SessionMetrics.quality(samples(null)).contains("limited"))
  assertTrue(SessionMetrics.quality(samples().map { it.copy(temperature=null) }).startsWith("Not supported"))
 }
}
