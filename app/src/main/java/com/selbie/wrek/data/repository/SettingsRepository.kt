package com.selbie.wrek.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.selbie.wrek.data.models.AppSettings
import com.selbie.wrek.data.models.BitratePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for managing application settings persistence
 */
class SettingsRepository private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    /**
     * Load settings from SharedPreferences
     */
    private fun loadSettings(): AppSettings {
        val bitrateString = prefs.getString(KEY_BITRATE_PREFERENCE, null)
        val bitratePreference = when (bitrateString) {
            "MODEST" -> BitratePreference.MODEST
            "BEST" -> BitratePreference.BEST
            "AUTO" -> BitratePreference.AUTO
            else -> BitratePreference.AUTO // Default
        }

        val autoStop = prefs.getBoolean(KEY_AUTO_STOP, true)

        return AppSettings(bitratePreference = bitratePreference, autoStop = autoStop)
    }

    /**
     * Save bitrate preference to SharedPreferences
     */
    fun saveBitratePreference(preference: BitratePreference) {
        prefs.edit()
            .putString(KEY_BITRATE_PREFERENCE, preference.name)
            .apply()

        _settings.value = _settings.value.copy(bitratePreference = preference)
    }

    /**
     * Save auto-stop preference to SharedPreferences
     */
    fun saveAutoStop(value: Boolean) {
        prefs.edit()
            .putBoolean(KEY_AUTO_STOP, value)
            .apply()

        _settings.value = _settings.value.copy(autoStop = value)
    }

    companion object {
        private const val PREFS_NAME = "wrek_settings"
        private const val KEY_BITRATE_PREFERENCE = "bitrate_preference"
        private const val KEY_AUTO_STOP = "auto_stop"

        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
