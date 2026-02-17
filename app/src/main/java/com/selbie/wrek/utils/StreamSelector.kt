package com.selbie.wrek.utils

import com.selbie.wrek.data.models.BitratePreference
import com.selbie.wrek.data.models.Stream

object StreamSelector {
    /**
     * Given a list of streams and a preferredBitrate, pick the stream with the
     * highest bitrate that does not exceed preferredBitrate. If no stream qualifies, return
     * the stream with the smallest bitrate.
     */
    fun selectStream(streams: List<Stream>, preferredBitrate: Int): Stream? {
        if (streams.size < 1) {
            return null
        }
        val streamsSorted = streams.sortedBy { it.bitrate }
        val streamsEligible = streams.filter { it.bitrate <= preferredBitrate }
        val streamsInelligible = streams.filter { it.bitrate > preferredBitrate }
        val bestStream = streamsEligible.lastOrNull() ?: streamsInelligible.firstOrNull()
        return bestStream
    }

    /**
     * Convert BitratePreference to kbps value
     * Phase 2: AUTO assumes WiFi (320), Phase 3 will add network detection
     */
    fun resolveBitratePreference(
        preference: BitratePreference,
        isOnWifi: Boolean = true
    ): Int {
        return when (preference) {
            BitratePreference.MODEST -> 128
            BitratePreference.BEST -> 320
            BitratePreference.AUTO -> if (isOnWifi) 320 else 128
        }
    }
}
