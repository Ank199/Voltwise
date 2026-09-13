package com.voltwise.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battery_snapshots")
data class BatterySnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val level: Int,
    val plugged: Boolean,
    val status: String,
    val source: String,
    val temperature: Float?,
    val voltage: Int?,
    val currentNow: Int?
)
