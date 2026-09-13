package com.voltwise.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voltwise.data.local.AppDatabase
import com.voltwise.data.local.entity.BatteryObservationEntity
import com.voltwise.data.local.entity.BatterySessionEntity
import com.voltwise.data.repository.BatteryRepository
import com.voltwise.service.BatteryMonitorService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardState(
    val isMonitoring: Boolean = false,
    val currentSession: BatterySessionEntity? = null,
    val latestObservation: BatteryObservationEntity? = null,
    val batteryCondition: String = "Unknown"
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BatteryRepository
    private val prefs = application.getSharedPreferences("voltwise_prefs", Context.MODE_PRIVATE)
    
    private val _isMonitoring = MutableStateFlow(prefs.getBoolean("is_monitoring", false))

    init {
        val database = AppDatabase.getDatabase(application)
        repository = BatteryRepository(database.batteryDao())
    }

    val uiState: StateFlow<DashboardState> = combine(
        _isMonitoring,
        repository.getLastSessionFlow(),
        repository.getLatestObservationFlow()
    ) { monitoring, session, observation ->
        DashboardState(
            isMonitoring = monitoring,
            currentSession = session,
            latestObservation = observation,
            batteryCondition = com.voltwise.service.LiveBattery.reading.value?.health ?: "Unknown"
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState(isMonitoring = _isMonitoring.value)
    )

    fun toggleMonitoring() {
        val newState = !_isMonitoring.value
        _isMonitoring.value = newState
        prefs.edit().putBoolean("is_monitoring", newState).apply()

        val context = getApplication<Application>()
        val intent = Intent(context, BatteryMonitorService::class.java)
        
        if (newState) {
            intent.action = BatteryMonitorService.ACTION_START_SERVICE
            context.startForegroundService(intent)
        } else {
            intent.action = BatteryMonitorService.ACTION_STOP_SERVICE
            context.startService(intent) // Will stop it
        }
    }
}
