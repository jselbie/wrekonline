package com.selbie.wrek.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class RadioShow(
    val id: String = "",           // Generated from title; default empty so null JSON value doesn't crash parse
    val title: String = "",        // Default empty so null JSON value doesn't crash parse
    val description: String? = null,
    val creationTime: String?,     // null for live streams
    val streams: List<Stream>,
    val logoUrl: String?,
    val logoBlurHash: String?,      // For progressive loading
    val albumCoverUrl: String? = null
) : Parcelable
