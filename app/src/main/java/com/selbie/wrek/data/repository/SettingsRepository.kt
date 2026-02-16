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
class SettingsRepository(context: Context) {

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

        return AppSettings(bitratePreference = bitratePreference)
    }

    /**
     * Save bitrate preference to SharedPreferences
     */
    fun saveBitratePreference(preference: BitratePreference) {
        prefs.edit()
            .putString(KEY_BITRATE_PREFERENCE, preference.name)
            .apply()

        _settings.value = AppSettings(bitratePreference = preference)
    }

    companion object {
        private const val PREFS_NAME = "wrek_settings"
        private const val KEY_BITRATE_PREFERENCE = "bitrate_preference"
    }
}
