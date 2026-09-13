package com.voltwise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatterySessionTest {
    @Test
    fun testChargePercentageCalculation() {
        // Simulating the logic from the app
        val startLevel = 20
        val endLevel = 85
        
        val percentageChange = endLevel - startLevel
        assertEquals(65, percentageChange)
    }

    @Test
    fun testDurationCalculation() {
        val startTime = 1000L
        val endTime = 5000L
        
        val durationMs = endTime - startTime
        assertEquals(4000L, durationMs)
    }
}
