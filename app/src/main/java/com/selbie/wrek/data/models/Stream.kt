package com.selbie.wrek.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class Stream(
    val bitrate: Int,              // kbps (128 or 320)
    val playlist: List<String>,    // URLs to play sequentially
    val isLiveStream: Boolean      // true for live, false for pre-recorded
) : Parcelable
