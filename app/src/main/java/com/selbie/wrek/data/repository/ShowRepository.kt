package com.selbie.wrek.data.repository

import android.content.Context
import android.util.Log
import com.selbie.wrek.R
import com.selbie.wrek.data.models.RadioShow
import com.selbie.wrek.utils.ScheduleValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "ShowRepository"
private const val SCHEDULE_URL = "https://www.selbie.com/wrek/schedule3.json"

@Serializable
private data class ScheduleResponse(val schedule: List<RadioShow>)

sealed interface ScheduleState {
    data object Loading : ScheduleState
    data class Success(val shows: List<RadioShow>) : ScheduleState
    data class Error(val message: String) : ScheduleState
}

class ShowRepository(private val context: Context) {
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
                val connection = (URL(SCHEDULE_URL).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 30000 // 30 seconds
                    readTimeout    = 30000 // 30 seconds
                }
                connection.inputStream.bufferedReader().use { it.readText() }
            }
            val response = json.decodeFromString<ScheduleResponse>(text)
            val validShows = ScheduleValidator.validate(response.schedule)
            Log.d(TAG, "Fetched ${response.schedule.size} shows, ${validShows.size} passed validation")
            if (validShows.isEmpty()) {
                Log.e(TAG, "No valid shows after validation — schedule may be malformed")
                _shows.value = emptyList()
                _state.value = ScheduleState.Error(context.getString(R.string.error_schedule_no_valid_shows))
                return
            }
            _shows.value = validShows
            _state.value = ScheduleState.Success(validShows)
        } catch (e: SerializationException) {
            Log.e(TAG, "Schedule data is malformed", e)
            _shows.value = emptyList()
            _state.value = ScheduleState.Error(context.getString(R.string.error_schedule_invalid_data))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch schedule", e)
            _shows.value = emptyList()
            _state.value = ScheduleState.Error(context.getString(R.string.error_load_schedule))
        }
    }
}
