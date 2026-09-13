package com.voltwise.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voltwise.data.local.AppDatabase
import com.voltwise.service.LiveBattery
import com.voltwise.service.SessionMetrics
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class LiveViewModel(app: Application) : AndroidViewModel(app) {
 private val dao = AppDatabase.getDatabase(app).batteryDao()
 val reading = LiveBattery.reading.asStateFlow()
 val monitoring = LiveBattery.monitoring.asStateFlow()
 val test = LiveBattery.test.asStateFlow()
 val sessions = dao.getAllSessions().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 private val clock = flow { while(true) { emit(System.currentTimeMillis()); delay(10000) } }
 @OptIn(ExperimentalCoroutinesApi::class)
 val snapshots = clock.flatMapLatest { dao.getSnapshotsSince(it-30L*86400000L) }.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 @OptIn(ExperimentalCoroutinesApi::class)
 val events = clock.flatMapLatest { dao.events(it-86400000) }.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 @OptIn(ExperimentalCoroutinesApi::class)
 val labResults = dao.getAllSessions().mapLatest { list ->
  list.filter { it.isCharging }.map { s -> (s.customLabel ?: "${s.chargingSource ?: "Unknown"} session #${s.id}") to SessionMetrics.quality(dao.samples(s.id)) }
 }.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 fun startTest() { if(reading.value?.plugged == true) { LiveBattery.requestTest=true; LiveBattery.test.value="Starting charger test…" } }
}
