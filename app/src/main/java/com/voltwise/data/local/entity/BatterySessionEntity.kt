package com.voltwise.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battery_sessions")
data class BatterySessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @androidx.room.ColumnInfo(defaultValue = "0") val sensorVersion: Int = 2,
    val startTime: Long,
    val endTime: Long? = null,
    val startLevel: Int,
    val endLevel: Int? = null,
    val isCharging: Boolean,
    val chargingSource: String? = null,
    val avgTemperature: Float? = null,
    val maxTemperature: Float? = null,
    val avgVoltage: Float? = null,
    val avgCurrent: Float? = null,
    val avgWattage: Float? = null,
    val overchargeDuration: Long = 0,
    val stabilityScore: Float = -1f,
    val customLabel: String? = null,
    val isFullyObserved: Boolean = true
)

