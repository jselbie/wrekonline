package com.selbie.wrek.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.selbie.wrek.MainActivity
import com.selbie.wrek.R
import com.selbie.wrek.data.models.PlaybackState
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
        const val EXTRA_SHOW = "extra_show"
        const val EXTRA_STREAM = "extra_stream"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "onCreate - Media3 will handle notifications automatically")

        // Initialize PlaybackController (no callback needed - Media3 handles notifications)
        playbackController = PlaybackController(this)

        // Create MediaSession - Media3 will automatically show notification when playing
        mediaSession = MediaSession.Builder(this, playbackController.player)
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
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val availableSessionCommands = connectionResult.availableSessionCommands.buildUpon()
                .add(SessionCommand(COMMAND_LOAD_AND_PLAY, Bundle.EMPTY))
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
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }
}
