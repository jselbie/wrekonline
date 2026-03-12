package com.selbie.wrek.utils

import android.util.Log
import com.selbie.wrek.BuildConfig
import com.selbie.wrek.data.models.SongMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val TAG = "AlbumArtRepository"

// Last.fm returns this hash as a placeholder when no image exists
private const val LAST_FM_PLACEHOLDER_HASH = "2a96cbd8b46e442fc41c2b86b821562f"

@Serializable
private data class LastFmResponse(
    val track: LastFmTrack? = null,
    val error: Int? = null
)

@Serializable
private data class LastFmTrack(
    val album: LastFmAlbum? = null
)

@Serializable
private data class LastFmAlbum(
    val image: List<LastFmImage> = emptyList()
)

@Serializable
private data class LastFmImage(
    @SerialName("#text") val url: String = "",
    val size: String = ""
)

/**
 * Fetches album art URLs from Last.fm for parsed song metadata.
 * Caches up to 10 results (LRU) to avoid redundant API calls.
 */
class AlbumArtRepository {

    private val json = Json { ignoreUnknownKeys = true }

    // LRU cache: evicts least-recently-accessed entry when size exceeds 10
    private val cache = object : LinkedHashMap<String, String?>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, String?>): Boolean = size > 10
    }

    /**
     * Returns album art URL for the given song metadata, or null if unavailable.
     * Caches both hits and misses (null) to avoid repeat fetches.
     *
     * Must be called from the main thread (or a single coroutine context) — no internal
     * synchronization is needed as long as callers cancel previous jobs before launching new ones.
     */
    suspend fun getAlbumArtUrl(song: SongMetadata): String? {
        val key = "${song.track}\t${song.artist}"

        if (cache.containsKey(key)) {
            Log.d(TAG, "Cache hit for: $key")
            return cache[key]
        }

        if (song.artist.isBlank()) {
            Log.d(TAG, "Skipping Last.fm lookup — no artist for track '${song.track}'")
            cache[key] = null
            return null
        }

        Log.d(TAG, "Fetching album art: track='${song.track}' artist='${song.artist}'")

        val result = fetchFromLastFm(song.track, song.artist)
        cache[key] = result
        return result
    }

    private suspend fun fetchFromLastFm(track: String, artist: String): String? {
        if (BuildConfig.LASTFM_API_KEY.isEmpty()) return null
        return withContext(Dispatchers.IO) {
            try {
                val encodedTrack = URLEncoder.encode(track, "UTF-8")
                val encodedArtist = URLEncoder.encode(artist, "UTF-8")
                val urlString = "https://ws.audioscrobbler.com/2.0/?method=track.getInfo" +
                        "&api_key=${BuildConfig.LASTFM_API_KEY}" +
                        "&artist=$encodedArtist" +
                        "&track=$encodedTrack" +
                        "&autocorrect=1" +
                        "&format=json"

                val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }

                val text = connection.inputStream.bufferedReader().use { it.readText() }
                val response = json.decodeFromString<LastFmResponse>(text)

                if (response.error != null) {
                    Log.d(TAG, "Last.fm API error ${response.error} for '$track' by '$artist'")
                    return@withContext null
                }

                // Pick the largest non-placeholder image
                val imageUrl = response.track?.album?.image
                    ?.filter { it.url.isNotEmpty() && !it.url.contains(LAST_FM_PLACEHOLDER_HASH) }
                    ?.maxByOrNull { sizeRank(it.size) }
                    ?.url

                Log.d(TAG, "Last.fm art for '$track': $imageUrl")
                imageUrl
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch from Last.fm: ${e.message}")
                null
            }
        }
    }

    private fun sizeRank(size: String): Int = when (size) {
        "small" -> 1
        "medium" -> 2
        "large" -> 3
        "extralarge" -> 4
        "mega" -> 5
        else -> 0
    }
}
