package com.voltwise.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voltwise.data.local.AppDatabase
import com.voltwise.data.local.entity.BatterySessionEntity
import com.voltwise.data.repository.BatteryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class InsightsState(
    val avgChargingDurationMs: Long = 0,
    val maxTempObserved: Float = 0f,
    val totalSessions: Int = 0
)

class InsightsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BatteryRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = BatteryRepository(database.batteryDao())
    }

    val uiState: StateFlow<InsightsState> = repository.getAllSessions().map { sessions ->
        val chargingSessions = sessions.filter { it.isCharging && it.endTime != null }
        
        val avgDuration = if (chargingSessions.isNotEmpty()) {
            chargingSessions.map { it.endTime!! - it.startTime }.average().toLong()
        } else 0L

        val maxTemp = sessions.mapNotNull { it.maxTemperature }.maxOrNull() ?: 0f

        InsightsState(
            avgChargingDurationMs = avgDuration,
            maxTempObserved = maxTemp,
            totalSessions = sessions.size
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, InsightsState())
}
