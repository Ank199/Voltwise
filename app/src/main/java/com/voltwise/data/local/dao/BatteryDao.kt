package com.voltwise.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.voltwise.data.local.entity.AlertHistoryEntity
import com.voltwise.data.local.entity.BatteryObservationEntity
import com.voltwise.data.local.entity.BatterySessionEntity
import com.voltwise.data.local.entity.BatterySnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryDao {
 @Insert suspend fun insertEvent(event: com.voltwise.data.local.entity.TimelineEvent)
 @Query("SELECT * FROM timeline_events WHERE timestamp >= :since ORDER BY timestamp DESC")
 fun events(since: Long): Flow<List<com.voltwise.data.local.entity.TimelineEvent>>
 @Query("SELECT * FROM battery_observations WHERE sessionId = :id ORDER BY timestamp")
 suspend fun samples(id: Long): List<BatteryObservationEntity>
 @Query("DELETE FROM timeline_events") suspend fun deleteEvents()

    
    // --- Sessions ---
    @Insert
    suspend fun insertSession(session: BatterySessionEntity): Long

    @Update
    suspend fun updateSession(session: BatterySessionEntity)

    @Query("SELECT * FROM battery_sessions WHERE sensorVersion = 2 ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<BatterySessionEntity>>
    
    @Query("SELECT * FROM battery_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): BatterySessionEntity?

    @Query("SELECT * FROM battery_sessions WHERE sensorVersion = 2 ORDER BY startTime DESC LIMIT 1")
    suspend fun getLastSession(): BatterySessionEntity?
    
    @Query("SELECT * FROM battery_sessions WHERE sensorVersion = 2 ORDER BY startTime DESC LIMIT 1")
    fun getLastSessionFlow(): Flow<BatterySessionEntity?>

    // --- Observations ---
    @Insert
    suspend fun insertObservation(observation: BatteryObservationEntity)

    @Query("SELECT * FROM battery_observations WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getObservationsForSession(sessionId: Long): Flow<List<BatteryObservationEntity>>

    @Query("SELECT * FROM battery_observations ORDER BY timestamp DESC LIMIT 1")
    fun getLatestObservationFlow(): Flow<BatteryObservationEntity?>
    
    @Query("SELECT * FROM battery_observations ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestObservation(): BatteryObservationEntity?

    // --- Battery snapshots ---
    @Insert
    suspend fun insertSnapshot(snapshot: BatterySnapshotEntity)

    @Query("SELECT * FROM battery_snapshots WHERE timestamp >= :since ORDER BY timestamp ASC")
    fun getSnapshotsSince(since: Long): Flow<List<BatterySnapshotEntity>>

    @Query("SELECT * FROM battery_snapshots WHERE timestamp >= :since ORDER BY timestamp ASC")
    suspend fun snapshotsSince(since: Long): List<BatterySnapshotEntity>

    // --- Alerts ---
    @Insert
    suspend fun insertAlert(alert: AlertHistoryEntity)

    @Query("SELECT * FROM alert_history ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<AlertHistoryEntity>>
    
    // --- Delete ---
    @Query("DELETE FROM battery_sessions")
    suspend fun deleteAllSessions()
    
    @Query("DELETE FROM alert_history")
    suspend fun deleteAllAlerts()
}

