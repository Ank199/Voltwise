package com.voltwise.util

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.voltwise.data.local.entity.BatterySessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtils {
    suspend fun exportSessionsToCsv(context: Context, uri: Uri, sessions: List<BatterySessionEntity>) {
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write("SessionId,StartTime,EndTime,DurationMin,StartLevel,EndLevel,LevelGained,IsCharging,ChargingSource,AvgTemp,MaxTemp\n")
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    for (s in sessions) {
                        val durationMin = if (s.endTime != null) (s.endTime - s.startTime) / 60000 else 0
                        val startStr = dateFormat.format(Date(s.startTime))
                        val endStr = if (s.endTime != null) dateFormat.format(Date(s.endTime)) else "Active"
                        val endLvl = s.endLevel ?: 0
                        val gained = if (s.endLevel != null) s.endLevel - s.startLevel else 0
                        writer.write("${s.id},$startStr,$endStr,$durationMin,${s.startLevel},$endLvl,$gained,${s.isCharging},${s.chargingSource ?: "Unknown"},${s.avgTemperature ?: 0f},${s.maxTemperature ?: 0f}\n")
                    }
                }
            }
        }
    }

    suspend fun exportSessionsToJson(context: Context, uri: Uri, sessions: List<BatterySessionEntity>) {
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    val gson = Gson()
                    writer.write(gson.toJson(sessions))
                }
            }
        }
    }
}
