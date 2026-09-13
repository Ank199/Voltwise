package com.voltwise.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voltwise.data.local.AppDatabase
import com.voltwise.data.local.entity.BatteryObservationEntity
import com.voltwise.data.local.entity.BatterySessionEntity
import com.voltwise.data.repository.BatteryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BatteryRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = BatteryRepository(database.batteryDao())
    }

    val sessions: StateFlow<List<BatterySessionEntity>> = repository.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        
    private val _selectedSessionObservations = MutableStateFlow<List<BatteryObservationEntity>>(emptyList())
    val selectedSessionObservations: StateFlow<List<BatteryObservationEntity>> = _selectedSessionObservations

    fun loadSessionObservations(sessionId: Long) {
        viewModelScope.launch {
            _selectedSessionObservations.value = repository.getObservationsForSession(sessionId).firstOrNull() ?: emptyList()
        }
    }
}
