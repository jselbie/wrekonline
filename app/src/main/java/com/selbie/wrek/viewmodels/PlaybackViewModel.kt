package com.selbie.wrek.viewmodels

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.selbie.wrek.R
import com.selbie.wrek.data.models.PlaybackState
import com.selbie.wrek.data.models.RadioShow
import com.selbie.wrek.data.repository.SettingsRepository
import com.selbie.wrek.data.repository.ShowRepository
import com.selbie.wrek.service.MediaPlaybackService
import com.selbie.wrek.utils.NetworkMonitor
import com.selbie.wrek.utils.StreamSelector
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel for managing media playback.
 * Binds to MediaPlaybackService via MediaController and exposes playback state to UI.
 */
class PlaybackViewModel(
    application: Application,
    private val showRepository: ShowRepository,
    private val settingsRepository: SettingsRepository,
    private val networkMonitor: NetworkMonitor
) : AndroidViewModel(application) {

    private val tag = "PlaybackViewModel"
    private fun str(resId: Int) = getApplication<Application>().getString(resId)

    // Playback state exposed to UI
    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    // MediaController for communicating with MediaPlaybackService
    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    // Track current show for state updates
    private var currentShow: RadioShow? = null
    private var currentStreamUrls: List<String> = emptyList()
    private var isLiveStream: Boolean = false

    // Dynamic song title from ICY metadata (live streams)
    private var currentSongTitle: String? = null

    // Position polling for smooth seekbar updates
    private var positionUpdateJob: Job? = null

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            while (isActive) {
                delay(250)
                updatePlaybackStateFromPlayer()
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    init {
        // Bind to MediaPlaybackService
        bindToService()
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            updatePlaybackStateFromPlayer()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlaybackStateFromPlayer()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updatePlaybackStateFromPlayer()
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            val title = mediaMetadata.title?.toString()
            Log.d(tag, "Media metadata changed: title=$title")
            currentSongTitle = title
            updatePlaybackStateFromPlayer()
        }

        override fun onPlayerError(error: PlaybackException) {
            handlePlayerError(error)
        }
    }

    /**
     * Binds to MediaPlaybackService and initializes MediaController.
     */
    private fun bindToService() {
        val sessionToken = SessionToken(
            getApplication(),
            ComponentName(getApplication(), MediaPlaybackService::class.java)
        )

        controllerFuture = MediaController.Builder(getApplication(), sessionToken).buildAsync()

        controllerFuture?.addListener(
            {
                try {
                    mediaController = controllerFuture?.get()?.apply {
                        // Add listener to observe player state changes
                        addListener(playerListener)
                    }
                    Log.d(tag, "MediaController connected")

                    // Start the service to ensure it's running
                    val intent = Intent(getApplication(), MediaPlaybackService::class.java)
                    getApplication<Application>().startService(intent)

                    // Recover current show/stream from the service if it's already playing
                    recoverCurrentState()
                } catch (e: Exception) {
                    Log.e(tag, "Failed to connect MediaController", e)
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    /**
     * Queries the service for the currently playing show/stream.
     * Restores local state so updatePlaybackStateFromPlayer() works after Activity recreation.
     */
    private fun recoverCurrentState() {
        val controller = mediaController ?: return

        val command = SessionCommand(MediaPlaybackService.COMMAND_GET_CURRENT_STATE, Bundle.EMPTY)
        val resultFuture = controller.sendCustomCommand(command, Bundle.EMPTY)

        resultFuture.addListener(
            {
                try {
                    val result = resultFuture.get()
                    if (result.resultCode == androidx.media3.session.SessionResult.RESULT_SUCCESS) {
                        val extras = result.extras
                        val show = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            extras.getParcelable(MediaPlaybackService.EXTRA_SHOW, RadioShow::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            extras.getParcelable<RadioShow>(MediaPlaybackService.EXTRA_SHOW)
                        }
                        val stream = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            extras.getParcelable(MediaPlaybackService.EXTRA_STREAM, com.selbie.wrek.data.models.Stream::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            extras.getParcelable<com.selbie.wrek.data.models.Stream>(MediaPlaybackService.EXTRA_STREAM)
                        }

                        if (show != null && stream != null) {
                            Log.d(tag, "Recovered current state: ${show.title}")
                            currentShow = show
                            currentStreamUrls = stream.playlist
                            isLiveStream = stream.isLiveStream
                            updatePlaybackStateFromPlayer()
                        }
                    } else {
                        Log.d(tag, "No active show to recover")
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Failed to recover current state", e)
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    /**
     * Updates playback state based on current player state.
     */
    private fun updatePlaybackStateFromPlayer() {
        val controller = mediaController ?: return
        val show = currentShow ?: return

        val playerState = controller.playbackState
        val isPlaying = controller.isPlaying
        val currentIndex = controller.currentMediaItemIndex
        val currentUrl = if (currentIndex >= 0 && currentIndex < currentStreamUrls.size) {
            currentStreamUrls[currentIndex]
        } else {
            currentStreamUrls.firstOrNull() ?: ""
        }
        val position = controller.currentPosition
        val duration = if (controller.duration != C.TIME_UNSET) controller.duration else null

        when (playerState) {
            Player.STATE_IDLE -> {
                _playbackState.value = PlaybackState.Idle
            }

            Player.STATE_BUFFERING -> {
                _playbackState.value = PlaybackState.Loading(show)
            }

            Player.STATE_READY -> {
                if (isPlaying) {
                    _playbackState.value = PlaybackState.Playing(
                        show = show,
                        currentUrl = currentUrl,
                        currentMediaItemIndex = currentIndex,
                        position = position,
                        duration = duration,
                        isLiveStream = isLiveStream,
                        songTitle = currentSongTitle
                    )
                    startPositionUpdates()
                } else {
                    _playbackState.value = PlaybackState.Paused(
                        show = show,
                        currentUrl = currentUrl,
                        currentMediaItemIndex = currentIndex,
                        position = position,
                        duration = duration,
                        isLiveStream = isLiveStream,
                        songTitle = currentSongTitle
                    )
                    stopPositionUpdates()
                }
            }

            Player.STATE_ENDED -> {
                _playbackState.value = PlaybackState.Stopped(show)
                currentShow = null
                currentStreamUrls = emptyList()
                stopPositionUpdates()
            }
        }
    }

    /**
     * Handles player errors and maps to Error state.
     */
    private fun handlePlayerError(error: PlaybackException) {
        val errorMessage = when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> str(R.string.error_network_connection)

            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> str(R.string.error_stream_unavailable)

            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED -> str(R.string.error_playback)

            else -> error.message ?: str(R.string.error_unknown)
        }

        Log.e(tag, "Player error: $errorMessage (code=${error.errorCode})")

        _playbackState.value = PlaybackState.Error(
            lastShow = currentShow,
            errorMessage = errorMessage
        )

        stopPositionUpdates()
        currentShow = null
        currentStreamUrls = emptyList()
    }

    /**
     * Plays a show by ID.
     * Resolves the stream based on bitrate preference and network type.
     *
     * @param showId The ID of the show to play
     */
    fun play(showId: String) {
        viewModelScope.launch {
            Log.d(tag, "play: showId=$showId")

            // Find the show
            val show = showRepository.shows.value.find { it.id == showId }
            if (show == null) {
                Log.e(tag, "Show not found: $showId")
                _playbackState.value = PlaybackState.Error(null, str(R.string.error_show_not_found))
                return@launch
            }

            // Get bitrate preference
            val bitratePreference = settingsRepository.settings.value.bitratePreference

            // Determine if on WiFi for AUTO mode
            val isOnWifi = networkMonitor.isOnWifi()

            Log.d(tag, "isOnWifi == $isOnWifi")

            // Resolve preferred bitrate
            val preferredBitrate = StreamSelector.resolveBitratePreference(bitratePreference, isOnWifi)

            // Select stream
            val stream = StreamSelector.selectStream(show.streams, preferredBitrate)
            if (stream == null) {
                Log.e(tag, "No suitable stream found for bitrate: $preferredBitrate")
                _playbackState.value = PlaybackState.Error(show, str(R.string.error_no_stream))
                return@launch
            }

            Log.d(tag, "Selected stream: bitrate=${stream.bitrate}kbps, urls=${stream.playlist.size}")

            // Update current show tracking
            currentShow = show
            currentStreamUrls = stream.playlist
            isLiveStream = stream.isLiveStream
            currentSongTitle = null

            // Send custom command to service
            val controller = mediaController
            if (controller == null) {
                Log.e(tag, "MediaController not connected")
                _playbackState.value = PlaybackState.Error(show, str(R.string.error_service_unavailable))
                return@launch
            }

            // Update state to Loading
            _playbackState.value = PlaybackState.Loading(show)

            // Send load and play command
            val args = Bundle().apply {
                putParcelable(MediaPlaybackService.EXTRA_SHOW, show)
                putParcelable(MediaPlaybackService.EXTRA_STREAM, stream)
            }

            val command = SessionCommand(MediaPlaybackService.COMMAND_LOAD_AND_PLAY, Bundle.EMPTY)
            val resultFuture = controller.sendCustomCommand(command, args)

            resultFuture.addListener(
                {
                    try {
                        val result = resultFuture.get()
                        if (result.resultCode != androidx.media3.session.SessionResult.RESULT_SUCCESS) {
                            Log.e(tag, "Load and play command failed")
                            _playbackState.value = PlaybackState.Error(show, str(R.string.error_start_playback))
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Error sending load and play command", e)
                        _playbackState.value = PlaybackState.Error(show, str(R.string.error_start_playback))
                    }
                },
                MoreExecutors.directExecutor()
            )
        }
    }

    /**
     * Pauses playback (only for pre-recorded content).
     */
    fun pause() {
        viewModelScope.launch {
            Log.d(tag, "pause")
            mediaController?.pause()
        }
    }

    /**
     * Resumes playback from paused state.
     */
    fun resume() {
        viewModelScope.launch {
            Log.d(tag, "resume")
            mediaController?.play()
        }
    }

    /**
     * Stops playback completely.
     */
    fun stop() {
        stopPositionUpdates()
        viewModelScope.launch {
            Log.d(tag, "stop")
            mediaController?.stop()
        }
    }

    /**
     * Seeks to a specific position (only for pre-recorded content).
     * @param positionMs Position in milliseconds
     */
    fun seekTo(positionMs: Long) {
        viewModelScope.launch {
            Log.d(tag, "seekTo: ${positionMs}ms")
            mediaController?.seekTo(positionMs)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(tag, "onCleared")

        stopPositionUpdates()
        // Remove listener and release MediaController
        mediaController?.removeListener(playerListener)
        mediaController?.release()
        controllerFuture?.let { future ->
            MediaController.releaseFuture(future)
        }
    }

    /**
     * Factory for creating PlaybackViewModel with dependencies.
     */
    class Factory(
        private val application: Application,
        private val showRepository: ShowRepository,
        private val settingsRepository: SettingsRepository,
        private val networkMonitor: NetworkMonitor
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PlaybackViewModel::class.java)) {
                return PlaybackViewModel(
                    application,
                    showRepository,
                    settingsRepository,
                    networkMonitor
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
