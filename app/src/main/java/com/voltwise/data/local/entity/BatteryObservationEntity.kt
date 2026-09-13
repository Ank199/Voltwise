package com.voltwise.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "battery_observations",
    foreignKeys = [
        ForeignKey(
            entity = BatterySessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class BatteryObservationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val level: Int,
    val temperature: Float?, // in Celsius
    val voltage: Int?, // in millivolts
    val currentNow: Int?, // Android API microamperes, original sign preserved.
    val timeToFull: Long? // estimated time to full if charging, in milliseconds
)
