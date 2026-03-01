package com.selbie.wrek.utils

import android.util.Log
import com.selbie.wrek.data.models.RadioShow
import com.selbie.wrek.data.models.Stream

private const val TAG = "ScheduleValidator"

object ScheduleValidator {

    fun validate(shows: List<RadioShow>): List<RadioShow> {
        return shows.mapNotNull { show -> validateShow(show) }
    }

    private fun validateShow(show: RadioShow): RadioShow? {
        if (show.id.isBlank()) {
            Log.e(TAG, "Filtering show: id is missing or blank (title='${show.title}')")
            return null
        }
        if (show.title.isBlank()) {
            Log.e(TAG, "Filtering show: title is missing or blank (id='${show.id}')")
            return null
        }

        val validStreams = show.streams.mapNotNull { stream -> validateStream(stream, show.id) }

        if (validStreams.isEmpty()) {
            Log.e(TAG, "Filtering show '${show.id}': no valid streams remain after validation")
            return null
        }

        return if (validStreams.size == show.streams.size) show
               else show.copy(streams = validStreams)
    }

    private fun validateStream(stream: Stream, showId: String): Stream? {
        if (stream.bitrate <= 0) {
            Log.e(TAG, "Filtering stream for show '$showId': bitrate=${stream.bitrate} is not positive")
            return null
        }
        if (stream.playlist.isEmpty()) {
            Log.e(TAG, "Filtering stream for show '$showId': bitrate=${stream.bitrate}kbps has empty playlist")
            return null
        }
        if (stream.playlistTimes != null && stream.playlistTimes.size != stream.playlist.size) {
            Log.e(TAG, "Filtering stream for show '$showId': bitrate=${stream.bitrate}kbps " +
                "playlistTimes length (${stream.playlistTimes.size}) != playlist length (${stream.playlist.size})")
            return null
        }
        return stream
    }
}
