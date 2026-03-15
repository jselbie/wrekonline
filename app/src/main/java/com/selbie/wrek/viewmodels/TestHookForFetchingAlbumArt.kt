package com.selbie.wrek.viewmodels

import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Debug-only test hook that simulates ICY metadata changes by cycling through
 * a hardcoded list of song titles on a fixed interval. Used to exercise the
 * album art fetching pipeline without needing a live radio stream.
 */
class TestHookForFetchingAlbumArt {
    private val tag = "TestHookForFetchingAlbumArt"
    private val testSongs = arrayOf(
        "'Hit the Lights' by Metallica",
        "'Paranoid' by Black Sabbath",
        "'Hit the Lights' by Black Tide",
        )


    private var songIndex = 0
    private var isStarted = false
    private val handler = Handler(Looper.getMainLooper())
    private var cb : (String)->Unit = {}
    private val runnable : Runnable = Runnable {
        Log.d(tag, "Next song = ${testSongs[songIndex]}")
        cb(testSongs[songIndex])
        songIndex = (songIndex + 1) % testSongs.size
        handler.postDelayed(this.runnable,8000)
    }

    /**
     * Begins cycling through test songs every 8 seconds, invoking [callback] with
     * each ICY-style metadata string. No-op if already started.
     *
     * @param callback receives the metadata string for each simulated song change
     */
    fun startTestHook(callback:(String)->Unit) {
        if (isStarted) {
            return
        }
        Log.d(tag, "starting test hook")
        songIndex = 0
        handler.removeCallbacks(runnable)
        cb = callback
        handler.post(runnable)
        isStarted = true
    }

    /**
     * Stops the simulated metadata cycle and clears the callback.
     */
    fun stopTestHook() {
        Log.d(tag, "stopping test hook")
        handler.removeCallbacks(runnable)
        cb = {}
        isStarted = false
    }
}