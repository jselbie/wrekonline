package com.selbie.wrek.data.models

/**
 * Represents the current state of media playback.
 */
sealed class PlaybackState {
    /**
     * Initial state - no show selected, no playback attempted.
     */
    object Idle : PlaybackState()

    /**
     * Buffering stream, preparing to play.
     * @param show The show being loaded
     */
    data class Loading(
        val show: RadioShow
    ) : PlaybackState()

    /**
     * Audio actively playing.
     * @param show Current show
     * @param currentUrl The URL currently playing from the playlist
     * @param currentMediaItemIndex Index in the playlist (0-based)
     * @param position Current playback position in milliseconds
     * @param duration Total duration in milliseconds (null for live streams)
     * @param isLiveStream True if this is a live stream (no seeking)
     */
    data class Playing(
        val show: RadioShow,
        val currentUrl: String,
        val currentMediaItemIndex: Int,
        val position: Long,
        val duration: Long?,
        val isLiveStream: Boolean
    ) : PlaybackState()

    /**
     * Playback paused (only applicable to pre-recorded shows).
     * @param show Current show
     * @param currentUrl The URL paused from the playlist
     * @param currentMediaItemIndex Index in the playlist (0-based)
     * @param position Paused position in milliseconds
     * @param duration Total duration in milliseconds
     * @param isLiveStream False (live streams cannot be paused)
     */
    data class Paused(
        val show: RadioShow,
        val currentUrl: String,
        val currentMediaItemIndex: Int,
        val position: Long,
        val duration: Long?,
        val isLiveStream: Boolean
    ) : PlaybackState()

    /**
     * Playback stopped - user stopped or playlist ended.
     * @param lastShow The last show that was playing (null if never played)
     */
    data class Stopped(
        val lastShow: RadioShow?
    ) : PlaybackState()

    /**
     * Error occurred during playback.
     * @param lastShow The show that encountered an error (null if initial play failed)
     * @param errorMessage User-friendly error description
     */
    data class Error(
        val lastShow: RadioShow?,
        val errorMessage: String
    ) : PlaybackState()
}
