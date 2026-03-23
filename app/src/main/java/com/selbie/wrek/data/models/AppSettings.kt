package com.selbie.wrek.data.models

/**
 * Represents user's bitrate preference for streaming
 */
enum class BitratePreference {
    MODEST,          // Always use 128 kbps
    BEST,            // Always use 320 kbps
    AUTO             // WiFi = 320 kbps, Mobile = 128 kbps
}

/**
 * Application settings
 */
data class AppSettings(
    val bitratePreference: BitratePreference = BitratePreference.AUTO,
    val autoStop: Boolean = true
)
