package com.selbie.wrek.data.repository

import android.util.Log
import com.selbie.wrek.data.models.RadioShow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL

private const val TAG = "ShowRepository"
private const val SCHEDULE_URL = "https://www.selbie.com/wrek/schedule2.json"

@Serializable
private data class ScheduleResponse(val schedule: List<RadioShow>)

sealed interface ScheduleState {
    data object Loading : ScheduleState
    data class Success(val shows: List<RadioShow>) : ScheduleState
    data class Error(val message: String) : ScheduleState
}

class ShowRepository {
    private val json = Json { ignoreUnknownKeys = true }

    private val _state = MutableStateFlow<ScheduleState>(ScheduleState.Loading)
    val state: StateFlow<ScheduleState> = _state.asStateFlow()

    // Backward-compatible shows flow for PlaybackViewModel
    private val _shows = MutableStateFlow<List<RadioShow>>(emptyList())
    val shows: StateFlow<List<RadioShow>> = _shows.asStateFlow()

    suspend fun refresh() {
        _state.value = ScheduleState.Loading
        try {
            val text = withContext(Dispatchers.IO) {
                URL(SCHEDULE_URL).readText()
            }
            val response = json.decodeFromString<ScheduleResponse>(text)
            Log.d(TAG, "Fetched ${response.schedule.size} shows")
            _shows.value = response.schedule
            _state.value = ScheduleState.Success(response.schedule)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch schedule", e)
            _shows.value = emptyList()
            _state.value = ScheduleState.Error("Unable to load schedule. Check your connection.")
        }
    }
}
