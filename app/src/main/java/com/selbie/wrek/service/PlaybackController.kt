package com.selbie.wrek.service

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.selbie.wrek.R
import com.selbie.wrek.data.models.RadioShow
import com.selbie.wrek.data.models.Stream

/**
 * Manages ExoPlayer instance and playback operations.
 * Media3 automatically handles notifications based on player state and MediaMetadata.
 */
class PlaybackController(
    private val context: Context
) {
    private val tag = "PlaybackController"

    // Current show and stream being played
    var currentShow: RadioShow? = null
        private set
    var currentStream: Stream? = null
        private set

    // ExoPlayer instance with audio focus handling
    private val audioAttributes = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val stateName = when (playbackState) {
                Player.STATE_IDLE -> "IDLE"
                Player.STATE_BUFFERING -> "BUFFERING"
                Player.STATE_READY -> "READY"
                Player.STATE_ENDED -> "ENDED"
                else -> "UNKNOWN"
            }
            Log.d(tag, "Player state: $stateName, isPlaying=${exoPlayer.isPlaying}")
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Log.d(tag, "isPlaying: $isPlaying")
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            super.onMediaMetadataChanged(mediaMetadata)
            Log.d(tag, "Metadata change: ${mediaMetadata?.title}")
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            Log.d(tag, "Media item transition: ${mediaItem?.mediaMetadata?.title}")
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(tag, "Player error: ${error.message}", error)
            currentShow = null
            currentStream = null
        }
    }

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
        .build()
        .apply {
            addListener(playerListener)
        }

    val player: SegmentAwarePlayer = SegmentAwarePlayer(exoPlayer)

    /**
     * Loads a show's stream and starts playback.
     * Clears existing playlist, adds all URLs from the stream, prepares, and plays.
     *
     * @param show The show to play
     * @param stream The selected stream (bitrate already resolved)
     */
    fun loadAndPlay(show: RadioShow, stream: Stream) {
        Log.d(tag, "loadAndPlay: ${show.title}, ${stream.bitrate}kbps, ${stream.playlist.size} URLs")

        currentShow = show
        currentStream = stream

        // Clear existing playlist
        exoPlayer.clearMediaItems()
        player.playlistTimes = stream.playlistTimes

        // Add all URLs from the stream's playlist with metadata
        stream.playlist.forEachIndexed { index, url ->
            val metadata = MediaMetadata.Builder()
                .setArtist(context.getString(R.string.playback_artist))
                .apply {
                    // Set title only for pre-recorded content.
                    // For live streams, leave title unset so ICY metadata can populate it.
                    if (!stream.isLiveStream) {
                        setTitle(show.title)
                        setDisplayTitle(show.title)
                    }
                    // Add artwork if available
                    show.logoUrl?.let { logoUrl ->
                        setArtworkUri(Uri.parse(logoUrl))
                    }
                }
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .setMediaId("${show.id}_$index")
                .setMediaMetadata(metadata)
                .build()

            exoPlayer.addMediaItem(mediaItem)
        }

        // Prepare and play - Media3 will automatically show notification
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        Log.d(tag, "Player prepared and playing - Media3 will show notification")
    }

    /**
     * Pauses playback (only for pre-recorded content).
     * Live streams should use stop() instead.
     */
    fun pause() {
        Log.d(tag, "pause")
        if (player.isPlaying) {
            player.pause()
        }
    }

    /**
     * Resumes playback from paused state.
     */
    fun resume() {
        Log.d(tag, "resume")
        if (!player.isPlaying && player.playbackState != Player.STATE_IDLE) {
            player.play()
        }
    }

    /**
     * Stops playback completely and clears the playlist.
     */
    fun stop() {
        Log.d(tag, "stop")
        player.stop()
        player.clearMediaItems()
        player.playlistTimes = null
        currentShow = null
        currentStream = null
    }

    /**
     * Releases the player instance. Call when service is destroyed.
     */
    fun release() {
        Log.d(tag, "release")
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }

}
