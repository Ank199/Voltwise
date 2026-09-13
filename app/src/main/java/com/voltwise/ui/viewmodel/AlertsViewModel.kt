package com.voltwise.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voltwise.data.local.AppDatabase
import com.voltwise.data.local.entity.AlertHistoryEntity
import com.voltwise.data.repository.BatteryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class AlertsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BatteryRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = BatteryRepository(database.batteryDao())
    }

    val alerts: StateFlow<List<AlertHistoryEntity>> = repository.getAllAlerts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
