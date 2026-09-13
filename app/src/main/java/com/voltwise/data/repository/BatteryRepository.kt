package com.voltwise.data.repository

import com.voltwise.data.local.dao.BatteryDao
import com.voltwise.data.local.entity.AlertHistoryEntity
import com.voltwise.data.local.entity.BatteryObservationEntity
import com.voltwise.data.local.entity.BatterySessionEntity
import kotlinx.coroutines.flow.Flow

class BatteryRepository(private val batteryDao: BatteryDao) {

    // Sessions
    suspend fun insertSession(session: BatterySessionEntity): Long = batteryDao.insertSession(session)
    suspend fun updateSession(session: BatterySessionEntity) = batteryDao.updateSession(session)
    suspend fun getSessionById(sessionId: Long): BatterySessionEntity? = batteryDao.getSessionById(sessionId)
    suspend fun getLastSession(): BatterySessionEntity? = batteryDao.getLastSession()
    
    fun getAllSessions(): Flow<List<BatterySessionEntity>> = batteryDao.getAllSessions()
    fun getLastSessionFlow(): Flow<BatterySessionEntity?> = batteryDao.getLastSessionFlow()

    // Observations
    suspend fun insertObservation(observation: BatteryObservationEntity) {
        batteryDao.insertObservation(observation)
        
    }

    fun getObservationsForSession(sessionId: Long): Flow<List<BatteryObservationEntity>> = 
        batteryDao.getObservationsForSession(sessionId)
        
    fun getLatestObservationFlow(): Flow<BatteryObservationEntity?> = batteryDao.getLatestObservationFlow()
    suspend fun getLatestObservation(): BatteryObservationEntity? = batteryDao.getLatestObservation()

    // Alerts
    suspend fun insertAlert(alert: AlertHistoryEntity) = batteryDao.insertAlert(alert)
    fun getAllAlerts(): Flow<List<AlertHistoryEntity>> = batteryDao.getAllAlerts()
    
    // Clear data
    suspend fun deleteAllData() {
        batteryDao.deleteAllSessions()
        batteryDao.deleteAllAlerts()
        batteryDao.deleteEvents()
    }
}
