package com.selbie.wrek.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.selbie.wrek.data.models.RadioShow
import com.selbie.wrek.data.repository.SettingsRepository
import com.selbie.wrek.data.repository.ShowRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MainUiState(
    val shows: List<RadioShow> = emptyList(),
    val selectedShowId: String? = null,
    val isLoading: Boolean = true
)

class MainViewModel(
    private val showRepository: ShowRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            showRepository.shows.collect { shows ->
                _uiState.value = _uiState.value.copy(
                    shows = shows,
                    isLoading = false
                )
            }
        }
    }

    fun selectShow(showId: String) {
        _uiState.value = _uiState.value.copy(selectedShowId = showId)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedShowId = null)
    }

    class Factory(
        private val showRepository: ShowRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(showRepository, settingsRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
