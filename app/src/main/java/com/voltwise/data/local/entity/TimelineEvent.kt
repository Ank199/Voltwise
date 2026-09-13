package com.voltwise.data.local.entity
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "timeline_events")
data class TimelineEvent(@PrimaryKey(autoGenerate = true) val id: Long = 0, val timestamp: Long, val type: String, val detail: String)
