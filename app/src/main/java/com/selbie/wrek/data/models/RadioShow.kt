package com.selbie.wrek.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RadioShow(
    val id: String,                // Generated from title
    val title: String,
    val description: String,
    val creationTime: String?,     // null for live streams
    val streams: List<Stream>,
    val logoUrl: String?,
    val logoBlurHash: String?      // For progressive loading
) : Parcelable

// Extension function for ID generation
fun String.toShowId(): String {
    return this.lowercase()
        .replace(Regex("[^a-z0-9\\s-]"), "")
        .replace(Regex("\\s+"), "-")
        .trim('-')
}
