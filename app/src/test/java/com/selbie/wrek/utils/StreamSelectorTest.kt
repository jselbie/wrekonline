package com.selbie.wrek.utils

import com.selbie.wrek.data.models.Stream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreamSelectorTest {

    private fun stream(bitrate: Int, live: Boolean = false) = Stream(
        bitrate = bitrate,
        playlist = listOf("https://example.com/$bitrate.mp3"),
        isLiveStream = live,
        playlistTimes = null
    )

    // --- Empty / single-element lists ---

    @Test
    fun emptyList_returnsNull() {
        assertNull(StreamSelector.selectStream(emptyList(), 320))
    }

    @Test
    fun singleStream_returned_regardless_of_preference() {
        val s = stream(128)
        assertEquals(s, StreamSelector.selectStream(listOf(s), 320))
        assertEquals(s, StreamSelector.selectStream(listOf(s), 128))
        assertEquals(s, StreamSelector.selectStream(listOf(s), 64))
    }

    // --- Two-stream (128 / 320) — matches production data ---

    @Test
    fun twoStreams_preferBest_returns320() {
        val s128 = stream(128)
        val s320 = stream(320)
        assertEquals(s320, StreamSelector.selectStream(listOf(s128, s320), 320))
    }

    @Test
    fun twoStreams_preferModest_returns128() {
        val s128 = stream(128)
        val s320 = stream(320)
        assertEquals(s128, StreamSelector.selectStream(listOf(s128, s320), 128))
    }

    @Test
    fun twoStreams_preferenceBelow_all_returnsSmallest() {
        val s128 = stream(128)
        val s320 = stream(320)
        assertEquals(s128, StreamSelector.selectStream(listOf(s128, s320), 64))
    }

    @Test
    fun twoStreams_preferenceBetween_returnsHighestEligible() {
        val s128 = stream(128)
        val s320 = stream(320)
        assertEquals(s128, StreamSelector.selectStream(listOf(s128, s320), 256))
    }

    @Test
    fun twoStreams_reverseOrder_stillCorrect() {
        val s128 = stream(128)
        val s320 = stream(320)
        assertEquals(s320, StreamSelector.selectStream(listOf(s320, s128), 320))
        assertEquals(s128, StreamSelector.selectStream(listOf(s320, s128), 128))
    }

    // --- Three or more streams ---

    @Test
    fun threeStreams_picksHighestNotExceedingPreference() {
        val s64 = stream(64)
        val s128 = stream(128)
        val s320 = stream(320)
        val streams = listOf(s64, s128, s320)

        assertEquals(s128, StreamSelector.selectStream(streams, 200))
        assertEquals(s320, StreamSelector.selectStream(streams, 320))
        assertEquals(s64, StreamSelector.selectStream(streams, 64))
    }

    @Test
    fun threeStreams_preferenceAboveAll_returnsHighest() {
        val s64 = stream(64)
        val s128 = stream(128)
        val s320 = stream(320)
        assertEquals(s320, StreamSelector.selectStream(listOf(s64, s128, s320), 512))
    }

    @Test
    fun threeStreams_preferenceBelowAll_returnsSmallest() {
        val s64 = stream(64)
        val s128 = stream(128)
        val s320 = stream(320)
        assertEquals(s64, StreamSelector.selectStream(listOf(s64, s128, s320), 32))
    }

    // --- Exact match on boundary ---

    @Test
    fun exactMatch_returnsExactStream() {
        val s128 = stream(128)
        val s256 = stream(256)
        val s320 = stream(320)
        assertEquals(s256, StreamSelector.selectStream(listOf(s128, s256, s320), 256))
    }

    // --- Duplicate bitrates ---

    @Test
    fun duplicateBitrates_returnsOne() {
        val s128a = stream(128)
        val s128b = Stream(128, listOf("https://example.com/alt.mp3"), false, null)
        val result = StreamSelector.selectStream(listOf(s128a, s128b), 128)
        assertEquals(128, result?.bitrate)
    }

    // --- Live stream flag is irrelevant to selection ---

    @Test
    fun liveStreamFlag_doesNotAffectSelection() {
        val sLive = stream(128, live = true)
        val sArchive = stream(320, live = false)
        assertEquals(sArchive, StreamSelector.selectStream(listOf(sLive, sArchive), 320))
        assertEquals(sLive, StreamSelector.selectStream(listOf(sLive, sArchive), 128))
    }
}
