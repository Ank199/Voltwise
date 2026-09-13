package com.voltwise.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voltwise.data.local.AppDatabase
import com.voltwise.data.repository.BatteryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SettingsState(
    val useCelsius: Boolean = true,
    val languageCode: String = "en",
    val powerSavingMode: Boolean = false,
    val nightChargingMode: Boolean = false,
    val standbyOptimizeMode: Boolean = false,
    val alertChargeTargetEnabled: Boolean = false,
    val alertChargeTargetLevel: Int = 80,
    val alertLowBatteryEnabled: Boolean = false,
    val alertLowBatteryLevel: Int = 20,
    val alertHighTempEnabled: Boolean = false,
    val alertHighTempThreshold: Float = 40f,
    val dailyReportEnabled: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs: SharedPreferences = application.getSharedPreferences("voltwise_prefs", Context.MODE_PRIVATE)
    private val repository: BatteryRepository
    
    private val _uiState = MutableStateFlow(loadSettings())
    val uiState: StateFlow<SettingsState> = _uiState

    init {
        val database = AppDatabase.getDatabase(application)
        repository = BatteryRepository(database.batteryDao())
    }

    private fun loadSettings(): SettingsState {
        return SettingsState(
            useCelsius = prefs.getBoolean("use_celsius", true),
            languageCode = prefs.getString("language_code", "en") ?: "en",
            powerSavingMode = prefs.getBoolean("power_saving_mode", false),
            nightChargingMode = prefs.getBoolean("night_charging_mode", false),
            standbyOptimizeMode = prefs.getBoolean("standby_optimize_mode", false),
            alertChargeTargetEnabled = prefs.getBoolean("alert_charge_target_enabled", false),
            alertChargeTargetLevel = prefs.getInt("alert_charge_target_level", 80),
            alertLowBatteryEnabled = prefs.getBoolean("alert_low_battery_enabled", false),
            alertLowBatteryLevel = prefs.getInt("alert_low_battery_level", 20),
            alertHighTempEnabled = prefs.getBoolean("alert_high_temp_enabled", false),
            alertHighTempThreshold = prefs.getFloat("alert_high_temp_threshold", 40f),
            dailyReportEnabled = prefs.getBoolean("daily_report_enabled", false)
        )
    }

    fun updateUseCelsius(value: Boolean) {
        prefs.edit().putBoolean("use_celsius", value).apply()
        _uiState.value = _uiState.value.copy(useCelsius = value)
    }

    fun updateLanguageCode(value: String) {
        prefs.edit().putString("language_code", value).apply()
        _uiState.value = _uiState.value.copy(languageCode = value)
    }

    fun updatePowerSavingMode(value: Boolean) {
        prefs.edit().putBoolean("power_saving_mode", value).apply()
        _uiState.value = _uiState.value.copy(powerSavingMode = value)
    }

    fun updateNightChargingMode(value: Boolean) {
        prefs.edit().putBoolean("night_charging_mode", value).apply()
        _uiState.value = _uiState.value.copy(nightChargingMode = value)
    }

    fun updateStandbyOptimizeMode(value: Boolean) {
        prefs.edit().putBoolean("standby_optimize_mode", value).apply()
        _uiState.value = _uiState.value.copy(standbyOptimizeMode = value)
    }
    
    fun updateChargeTarget(enabled: Boolean, level: Int) {
        prefs.edit()
            .putBoolean("alert_charge_target_enabled", enabled)
            .putInt("alert_charge_target_level", level)
            .apply()
        _uiState.value = _uiState.value.copy(
            alertChargeTargetEnabled = enabled,
            alertChargeTargetLevel = level
        )
    }

    fun updateLowBatteryAlert(enabled: Boolean, level: Int) {
        prefs.edit()
            .putBoolean("alert_low_battery_enabled", enabled)
            .putInt("alert_low_battery_level", level)
            .apply()
        _uiState.value = _uiState.value.copy(
            alertLowBatteryEnabled = enabled,
            alertLowBatteryLevel = level
        )
    }

    fun updateHighTempAlert(enabled: Boolean, threshold: Float) {
        prefs.edit()
            .putBoolean("alert_high_temp_enabled", enabled)
            .putFloat("alert_high_temp_threshold", threshold)
            .apply()
        _uiState.value = _uiState.value.copy(
            alertHighTempEnabled = enabled,
            alertHighTempThreshold = threshold
        )
    }

    fun updateDailyReport(value: Boolean) {
        prefs.edit().putBoolean("daily_report_enabled", value).apply()
        _uiState.value = _uiState.value.copy(dailyReportEnabled = value)
    }

    fun clearData() {
        viewModelScope.launch {
            repository.deleteAllData()
        }
    }
}
