package com.selbie.wrek.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.selbie.wrek.data.models.BitratePreference
import com.selbie.wrek.data.repository.SettingsRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel for managing settings screen state
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val bitratePreference = settingsRepository.settings

    /**
     * Update the bitrate preference
     */
    fun setBitratePreference(preference: BitratePreference) {
        settingsRepository.saveBitratePreference(preference)
    }

    fun setAutoStop(value: Boolean) {
        settingsRepository.saveAutoStop(value)
    }

    /**
     * Factory for creating SettingsViewModel with dependencies
     */
    class Factory(
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(settingsRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
