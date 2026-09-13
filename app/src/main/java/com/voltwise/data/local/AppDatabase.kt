package com.voltwise.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.voltwise.data.local.dao.BatteryDao
import com.voltwise.data.local.entity.AlertHistoryEntity
import com.voltwise.data.local.entity.BatteryObservationEntity
import com.voltwise.data.local.entity.BatterySessionEntity
import com.voltwise.data.local.entity.BatterySnapshotEntity

@Database(
    entities = [
        BatterySessionEntity::class, 
        BatteryObservationEntity::class,
        BatterySnapshotEntity::class,
        AlertHistoryEntity::class, com.voltwise.data.local.entity.TimelineEvent::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun batteryDao(): BatteryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "voltwise_database"
                )
                .addMigrations(object : androidx.room.migration.Migration(1, 2) {
 override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
  db.execSQL("ALTER TABLE battery_sessions ADD COLUMN avgVoltage REAL")
  db.execSQL("ALTER TABLE battery_sessions ADD COLUMN avgCurrent REAL")
  db.execSQL("ALTER TABLE battery_sessions ADD COLUMN avgWattage REAL")
  db.execSQL("ALTER TABLE battery_sessions ADD COLUMN overchargeDuration INTEGER NOT NULL DEFAULT 0")
  db.execSQL("ALTER TABLE battery_sessions ADD COLUMN stabilityScore REAL NOT NULL DEFAULT -1")
 }
 }, object : androidx.room.migration.Migration(2, 3) {
 override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
 db.execSQL("ALTER TABLE battery_sessions ADD COLUMN sensorVersion INTEGER NOT NULL DEFAULT 0")
 db.execSQL("CREATE TABLE battery_observations_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sessionId INTEGER NOT NULL, timestamp INTEGER NOT NULL, level INTEGER NOT NULL, temperature REAL, voltage INTEGER, currentNow INTEGER, timeToFull INTEGER, FOREIGN KEY(sessionId) REFERENCES battery_sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                        db.execSQL("INSERT INTO battery_observations_new SELECT id, sessionId, timestamp, level, temperature, NULLIF(voltage, 0), currentNow, timeToFull FROM battery_observations")
                        db.execSQL("DROP TABLE battery_observations")
                        db.execSQL("ALTER TABLE battery_observations_new RENAME TO battery_observations")
                        db.execSQL("CREATE INDEX index_battery_observations_sessionId ON battery_observations(sessionId)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS timeline_events (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, timestamp INTEGER NOT NULL, type TEXT NOT NULL, detail TEXT NOT NULL)")
 }
 }, object : androidx.room.migration.Migration(3, 4) {
 override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
  db.execSQL("CREATE TABLE IF NOT EXISTS battery_snapshots (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, timestamp INTEGER NOT NULL, level INTEGER NOT NULL, plugged INTEGER NOT NULL, status TEXT NOT NULL, source TEXT NOT NULL, temperature REAL, voltage INTEGER, currentNow INTEGER)")
 }
 })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
