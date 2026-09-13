package com.voltwise.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alert_history")
data class AlertHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val alertType: String, // e.g., "CHARGE_TARGET", "LOW_BATTERY", "HIGH_TEMP", "UNPLUGGED"
    val title: String,
    val message: String,
    val isDelivered: Boolean = true // Did we successfully show the notification?
)
