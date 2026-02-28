package com.selbie.wrek.service

import android.util.Log
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player

/**
 * A ForwardingPlayer that presents a multi-segment archive show as a single logical stream.
 *
 * WREK archive shows are stored as sequential 30-minute MP3 files (segments). ExoPlayer
 * treats each segment as a separate MediaItem. This class intercepts position and duration
 * queries to return absolute values across all segments, and translates single-argument
 * seekTo(absolutePositionMs) into the correct (segmentIndex, offsetWithinSegment) form.
 *
 * Set [playlistTimes] before playback begins. If null, all segments are assumed to be
 * [SEGMENT_DURATION_MS] (30 minutes). If set, it must be the same length as the playlist
 * and contains the actual duration of each segment in milliseconds.
 *
 * Single-segment streams (live streams, single-URL pre-recorded) pass through unchanged.
 */
class SegmentAwarePlayer(player: Player) : ForwardingPlayer(player) {

    companion object {
        // Default assumed duration of each WREK archive segment (30 minutes)
        const val SEGMENT_DURATION_MS = 1_800_000L
        private const val TAG = "SegmentAwarePlayer"
    }

    // Per-segment durations set by PlaybackController when loading a show.
    // If null, all segments are assumed to be SEGMENT_DURATION_MS.
    var playlistTimes: List<Long>? = null

    // Always returns a non-null list of segment durations.
    // Uses playlistTimes if set, otherwise generates a uniform list using SEGMENT_DURATION_MS.
    private val effectiveTimes: List<Long>
        get() = playlistTimes ?: List(mediaItemCount) { SEGMENT_DURATION_MS }

    private fun segmentStartMs(segmentIndex: Int): Long =
        effectiveTimes.take(segmentIndex).sum()

    private fun totalDurationMs(): Long =
        effectiveTimes.sum()

    override fun seekTo(positionMs: Long) {
        // Guard needed: for single-segment streams, we pass through without coercion.
        // Without this, a single-segment seek would call super.seekTo(0, positionMs.coerceAtMost(segDuration))
        // where segDuration is SEGMENT_DURATION_MS. If the actual audio file is longer than
        // SEGMENT_DURATION_MS, the coercion would wrongly clamp the seek position.
        if (mediaItemCount <= 1) {
            super.seekTo(positionMs)
            return
        }
        var remaining = positionMs
        val times = effectiveTimes
        for (i in times.indices) {
            val segDuration = times[i]
            if (remaining < segDuration || i == times.size - 1) {
                Log.d(TAG, "seekTo: ${positionMs}ms → segment $i @ ${remaining}ms")
                super.seekTo(i, remaining.coerceAtMost(segDuration))
                return
            }
            remaining -= segDuration
        }
    }

    override fun getCurrentPosition(): Long {
        // No guard needed: segmentStartMs(0) = effectiveTimes.take(0).sum() = 0,
        // so single-segment result is 0 + super.getCurrentPosition() = super.getCurrentPosition().
        return segmentStartMs(currentMediaItemIndex) + super.getCurrentPosition()
    }

    override fun getDuration(): Long {
        // Guard needed: live streams have no duration — ExoPlayer returns C.TIME_UNSET.
        // Without this, totalDurationMs() would return 1 × SEGMENT_DURATION_MS for a
        // live stream, which would cause PlaybackViewModel to treat it as a 30-minute
        // pre-recorded show (showing a seekbar and timestamps).
        if (mediaItemCount <= 1) return super.getDuration()
        return totalDurationMs()
    }

    override fun getBufferedPosition(): Long {
        // No guard needed: segmentStartMs(0) = 0, so single-segment result is
        // 0 + super.getBufferedPosition() = super.getBufferedPosition().
        return segmentStartMs(currentMediaItemIndex) + super.getBufferedPosition()
    }
}
