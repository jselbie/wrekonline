package com.selbie.wrek.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.selbie.wrek.MainActivity
import com.selbie.wrek.data.models.RadioShow
import com.selbie.wrek.data.models.Stream

/**
 * MediaSessionService for audio playback.
 * Uses Media3's automatic notification handling.
 */
class MediaPlaybackService : MediaSessionService() {
    private val tag = "MediaPlaybackService"

    private var mediaSession: MediaSession? = null
    private lateinit var playbackController: PlaybackController

    companion object {
        // Custom session commands
        const val COMMAND_LOAD_AND_PLAY = "com.selbie.wrek.LOAD_AND_PLAY"
        const val COMMAND_GET_CURRENT_STATE = "com.selbie.wrek.GET_CURRENT_STATE"
        const val EXTRA_SHOW = "extra_show"
        const val EXTRA_STREAM = "extra_stream"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "onCreate - Media3 will handle notifications automatically")

        // Initialize PlaybackController (no callback needed - Media3 handles notifications)
        playbackController = PlaybackController(this)

        // Create a PendingIntent to launch MainActivity when notification is tapped
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create MediaSession - Media3 will automatically show notification when playing
        mediaSession = MediaSession.Builder(this, playbackController.player)
            .setSessionActivity(pendingIntent)
            .setCallback(sessionCallback)
            .build()

        Log.d(tag, "MediaSession created - ready for playback")
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        Log.d(tag, "onDestroy")
        playbackController.release()
        mediaSession?.run {
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    /**
     * MediaSession callback handling playback commands.
     */
    private val sessionCallback = object : MediaSession.Callback {
        override fun onPlayerCommandRequest(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            playerCommand: Int
        ): Int {
            // For live streams, intercept COMMAND_PLAY_PAUSE when the player is paused
            // (i.e. the user is pressing play to resume). Rather than resuming from the
            // stale buffer position — which could be minutes behind the live edge —
            // reconnect fresh. This way the notification persists naturally when paused
            // (standard behavior) and resuming always returns to the current broadcast.
            if (playerCommand == Player.COMMAND_PLAY_PAUSE &&
                playbackController.currentStream?.isLiveStream == true &&
                !playbackController.player.isPlaying
            ) {
                val show = playbackController.currentShow
                val stream = playbackController.currentStream
                if (show != null && stream != null) {
                    Log.d(tag, "Intercepting PLAY on paused live stream — reconnecting to live edge")
                    playbackController.loadAndPlay(show, stream)
                }
                return SessionResult.RESULT_ERROR_NOT_SUPPORTED
            }
            return super.onPlayerCommandRequest(session, controller, playerCommand)
        }

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val availableSessionCommands = connectionResult.availableSessionCommands.buildUpon()
                .add(SessionCommand(COMMAND_LOAD_AND_PLAY, Bundle.EMPTY))
                .add(SessionCommand(COMMAND_GET_CURRENT_STATE, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.accept(
                availableSessionCommands,
                connectionResult.availablePlayerCommands
            )
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                COMMAND_LOAD_AND_PLAY -> {
                    val show = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        args.getParcelable(EXTRA_SHOW, RadioShow::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        args.getParcelable<RadioShow>(EXTRA_SHOW)
                    }
                    val stream = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        args.getParcelable(EXTRA_STREAM, Stream::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        args.getParcelable<Stream>(EXTRA_STREAM)
                    }

                    if (show != null && stream != null) {
                        Log.d(tag, "LOAD_AND_PLAY: ${show.title}")
                        playbackController.loadAndPlay(show, stream)
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    } else {
                        Log.e(tag, "LOAD_AND_PLAY missing show or stream")
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE))
                    }
                }

                COMMAND_GET_CURRENT_STATE -> {
                    val show = playbackController.currentShow
                    val stream = playbackController.currentStream
                    if (show != null && stream != null) {
                        Log.d(tag, "GET_CURRENT_STATE: ${show.title}")
                        val resultBundle = Bundle().apply {
                            putParcelable(EXTRA_SHOW, show)
                            putParcelable(EXTRA_STREAM, stream)
                        }
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, resultBundle))
                    } else {
                        Log.d(tag, "GET_CURRENT_STATE: no active show")
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_UNKNOWN))
                    }
                }
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }
}
