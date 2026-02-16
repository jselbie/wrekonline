package com.selbie.wrek.data.repository

import com.selbie.wrek.data.models.RadioShow
import com.selbie.wrek.data.models.Stream
import com.selbie.wrek.data.models.toShowId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ShowRepository {
    private val _shows = MutableStateFlow(getHardcodedShows())
    val shows: StateFlow<List<RadioShow>> = _shows.asStateFlow()

    private fun getHardcodedShows(): List<RadioShow> {
        val baseShows = listOf(
            RadioShow(
                id = "Live Air Stream".toShowId(),
                title = "Live Air Stream",
                description = "WREK's main 91.1 FM broadcast - eclectic college radio",
                creationTime = null,
                streams = listOf(
                    Stream(128, listOf("https://streaming.wrek.org/main/128kb.mp3"), true),
                    Stream(320, listOf("https://streaming.wrek.org/main/320kb.mp3"), true)
                ),
                logoUrl = "https://www.selbie.com/wrek/radio2.png",
                logoBlurHash = "LKO2?V%2Tw=w]~RBVZRi};RPxuwH"
            ),
            RadioShow(
                id = "HD2 Subchannel".toShowId(),
                title = "HD2 Subchannel",
                description = "WREK's HD Radio subchannel - alternative programming",
                creationTime = null,
                streams = listOf(
                    Stream(128, listOf("https://streaming.wrek.org/hd2/128kb.mp3"), true),
                    Stream(320, listOf("https://streaming.wrek.org/hd2/320kb.mp3"), true)
                ),
                logoUrl = "https://www.selbie.com/wrek/hd.png",
                logoBlurHash = "LEHV6nWB2yk8pyo0adR*.7kCMdnj"
            ),
            RadioShow(
                id = "Random Stuff".toShowId(),
                title = "Random Stuff",
                description = "Some random songs",
                creationTime = "2026-02-10T20:00:00Z",
                streams = listOf(
                    Stream(
                        128,
                        listOf(
                            "https://www.selbie.com/shows/Maria.mp3",
                            "https://www.selbie.com/shows/NightPeople.mp3"
                        ),
                        false
                    ),
                    Stream(
                        320,
                        listOf(
                            "https://www.selbie.com/shows/Maria.mp3",
                            "https://www.selbie.com/shows/NightPeople.mp3"
                        ),
                        false
                    )
                ),
                logoUrl = "https://www.selbie.com/shows/DioDreamEvil.jpg",
                logoBlurHash = "L9Aw]~?w00?v~q-;_3-;00Rj%MRj"
            )
        )

        // Generate 20 shows by repeating the base 3 shows
        return buildList {
            repeat(20) { index ->
                val baseShow = baseShows[index % baseShows.size]
                add(baseShow.copy(
                    id = "${baseShow.id}-${index + 1}",
                    title = "${baseShow.title} ${index + 1}"
                ))
            }
        }
    }
}
