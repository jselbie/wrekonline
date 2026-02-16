package com.selbie.wrek.utils

import com.selbie.wrek.data.models.BitratePreference
import com.selbie.wrek.data.models.Stream

object StreamSelector {
    /**
     * "Price is Right" algorithm: pick highest bitrate that doesn't exceed preference
     */
    fun selectStream(streams: List<Stream>, preferredBitrate: Int): Stream? {
        if (streams.isEmpty()) return null

        val eligibleStreams = streams.filter { it.bitrate <= preferredBitrate }

        return if (eligibleStreams.isNotEmpty()) {
            eligibleStreams.maxByOrNull { it.bitrate }
        } else {
            streams.minByOrNull { it.bitrate }
        }
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
