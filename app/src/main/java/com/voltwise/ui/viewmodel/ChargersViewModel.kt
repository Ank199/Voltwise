package com.voltwise.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voltwise.data.local.AppDatabase
import com.voltwise.data.local.entity.BatterySessionEntity
import com.voltwise.data.repository.BatteryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

data class ChargerProfile(
    val sourceName: String,
    val sessionCount: Int,
    val avgTemp: Float,
    val rating: String // "Excellent", "Good", "Average", "Poor"
)

class ChargersViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BatteryRepository

    init {
        val dao = AppDatabase.getDatabase(application).batteryDao()
        repository = BatteryRepository(dao)
    }

    val sessions: StateFlow<List<BatterySessionEntity>> = repository.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
