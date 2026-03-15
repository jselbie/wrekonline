package com.selbie.wrek.ui.components

import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selbie.wrek.BuildConfig
import com.selbie.wrek.R
import com.selbie.wrek.data.models.PlaybackState
import com.selbie.wrek.data.models.RadioShow
import com.selbie.wrek.ui.theme.WrekTheme

@Composable
fun MediaFooter(
    playbackState: PlaybackState,
    albumArtUrl: String?,
    onPlayPauseToggle: () -> Unit,
    onStop: () -> Unit,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Extract display values from playback state
    val titleText = when (playbackState) {
        is PlaybackState.Idle -> stringResource(R.string.media_footer_select_show)
        is PlaybackState.Loading -> stringResource(R.string.media_footer_loading)
        is PlaybackState.Playing -> playbackState.songMetadata?.let { m ->
            if (m.artist.isNotEmpty()) "${m.track} - ${m.artist}" else m.track
        } ?: playbackState.show.title
        is PlaybackState.Paused -> playbackState.songMetadata?.let { m ->
            if (m.artist.isNotEmpty()) "${m.track} - ${m.artist}" else m.track
        } ?: playbackState.show.title
        is PlaybackState.Stopped -> stringResource(R.string.media_footer_stopped)
        is PlaybackState.Error -> stringResource(R.string.media_footer_error)
    }

    val isLiveStream = when (playbackState) {
        is PlaybackState.Playing -> playbackState.isLiveStream
        is PlaybackState.Paused -> playbackState.isLiveStream
        else -> true // Treat all other states as "no seekbar"
    }

    val position = when (playbackState) {
        is PlaybackState.Playing -> playbackState.position
        is PlaybackState.Paused -> playbackState.position
        else -> 0L
    }

    val duration = when (playbackState) {
        is PlaybackState.Playing -> playbackState.duration
        is PlaybackState.Paused -> playbackState.duration
        else -> null
    }

    val seekbarEnabled = !isLiveStream && duration != null && duration > 0

    // Button state
    val buttonEnabled = playbackState is PlaybackState.Playing
            || playbackState is PlaybackState.Paused
            || playbackState is PlaybackState.Loading

    val buttonIcon = when {
        playbackState is PlaybackState.Loading -> Icons.Default.Stop
        playbackState is PlaybackState.Playing && playbackState.isLiveStream -> Icons.Default.Stop
        playbackState is PlaybackState.Playing -> Icons.Default.Pause
        playbackState is PlaybackState.Paused -> Icons.Default.PlayArrow
        else -> Icons.Default.PlayArrow
    }

    val buttonContentDescription = when {
        playbackState is PlaybackState.Loading -> stringResource(R.string.cd_stop_button)
        playbackState is PlaybackState.Playing && playbackState.isLiveStream -> stringResource(R.string.cd_stop_button)
        playbackState is PlaybackState.Playing -> stringResource(R.string.cd_pause_button)
        playbackState is PlaybackState.Paused -> stringResource(R.string.cd_play_button)
        else -> stringResource(R.string.cd_play_button)
    }

    val onButtonClick: () -> Unit = when {
        playbackState is PlaybackState.Loading -> onStop
        playbackState is PlaybackState.Playing && playbackState.isLiveStream -> onStop
        else -> onPlayPauseToggle
    }

    // Seekbar drag state
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }

    val isActiveLiveStream = (playbackState is PlaybackState.Playing && playbackState.isLiveStream) ||
            (playbackState is PlaybackState.Paused && playbackState.isLiveStream)
    val albumArtShow = when (playbackState) {
        is PlaybackState.Playing -> playbackState.show
        is PlaybackState.Paused -> playbackState.show
        else -> null
    }
    val liveSongMetadata = when (playbackState) {
        is PlaybackState.Playing -> playbackState.songMetadata
        is PlaybackState.Paused -> playbackState.songMetadata
        else -> null
    }

    Column(modifier = modifier.navigationBarsPadding()) {
        HorizontalDivider()
        Surface(
            color = if (isSystemInDarkTheme()) Color(0xFF2B2D30) else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Non-live content: always present to anchor the footer height.
                // Invisible in live mode but still participates in layout measurement.
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .alpha(if (isActiveLiveStream) 0f else 1f)
                        .then(if (isActiveLiveStream) Modifier.pointerInput(Unit) {} else Modifier)
                ) {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = onButtonClick,
                            enabled = buttonEnabled,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = buttonIcon,
                                contentDescription = buttonContentDescription
                            )
                        }
                        Slider(
                            value = if (isDragging) dragPosition else position.toFloat(),
                            onValueChange = { value ->
                                isDragging = true
                                dragPosition = value
                            },
                            onValueChangeFinished = {
                                isDragging = false
                                onSeekTo(dragPosition.toLong())
                            },
                            enabled = seekbarEnabled,
                            valueRange = 0f..(duration?.toFloat() ?: 1f),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (seekbarEnabled) {
                            val displayPosition = if (isDragging) dragPosition.toLong() else position
                            Text(
                                text = "${formatTime(displayPosition)} / ${formatTime(duration ?: 0L)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        } else {
                            Text(text = "", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Live overlay: three sections filling the full footer height.
                // Rendered on top of the invisible height-anchor Column above.
                if (isActiveLiveStream) {
                    Row(modifier = Modifier.matchParentSize()) {
                        // Left: media control button, vertically centered
                        IconButton(
                            onClick = onButtonClick,
                            enabled = buttonEnabled,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .size(48.dp)
                                .align(Alignment.CenterVertically)
                        ) {
                            Icon(
                                imageVector = buttonIcon,
                                contentDescription = buttonContentDescription
                            )
                        }

                        // Middle: track + artist, vertically centered in remaining space
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = 8.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = liveSongMetadata?.track ?: albumArtShow?.title ?: "",
                                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 20.sp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (!liveSongMetadata?.artist.isNullOrBlank()) {
                                Text(
                                    text = liveSongMetadata?.artist ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Right: album art, full footer height, no padding
                        if (isActiveLiveStream) {
                            // Remembers the last successfully fetched album art URL,
                            // resetting when the show changes so we never show stale art
                            // from a previous show.
                            var lastSuccessUrl by remember(albumArtShow?.id) { mutableStateOf<String?>(null) }
                            LaunchedEffect(albumArtUrl) {
                                if (albumArtUrl != null) lastSuccessUrl = albumArtUrl
                            }
                            val logoFallback = coil.compose.rememberAsyncImagePainter(albumArtShow?.logoUrl)

                            if (BuildConfig.DEBUG) {
                                LaunchedEffect(albumArtUrl, albumArtShow?.logoUrl, lastSuccessUrl) {
                                    Log.d(
                                        "MediaFooter",
                                        "AsyncImage:   albumArtUrl=$albumArtUrl   logoUrl=${albumArtShow?.logoUrl}   lastSuccessUrl=$lastSuccessUrl"
                                    )
                                }
                            }

                            AsyncImage(
                                model = albumArtUrl ?: albumArtShow?.logoUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .aspectRatio(1f),
                                contentScale = ContentScale.Crop,
                                placeholder = coil.compose.rememberAsyncImagePainter(lastSuccessUrl ?: albumArtShow?.logoUrl),
                                error = logoFallback,
                                fallback = logoFallback
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

// --- Previews ---

private val previewShow = RadioShow(
    id = "preview-show",
    title = "The Best of WREK",
    description = "A sample show for preview",
    creationTime = null,
    streams = emptyList(),
    logoUrl = null,
    logoBlurHash = null
)

@Preview(name = "Idle")
@Composable
private fun PreviewIdle() {
    WrekTheme {
        MediaFooter(
            playbackState = PlaybackState.Idle,
            albumArtUrl = null,
            onPlayPauseToggle = {},
            onStop = {},
            onSeekTo = {}
        )
    }
}

@Preview(name = "Loading")
@Composable
private fun PreviewLoading() {
    WrekTheme {
        MediaFooter(
            playbackState = PlaybackState.Loading(previewShow),
            albumArtUrl = null,
            onPlayPauseToggle = {},
            onStop = {},
            onSeekTo = {}
        )
    }
}

@Preview(name = "Playing - Live")
@Composable
private fun PreviewPlayingLive() {
    WrekTheme {
        MediaFooter(
            playbackState = PlaybackState.Playing(
                show = previewShow,
                currentUrl = "https://example.com/stream",
                currentMediaItemIndex = 0,
                position = 42000,
                duration = null,
                isLiveStream = true,
                songMetadata = com.selbie.wrek.data.models.SongMetadata(track = "One", artist = "Metallica")
            ),
            albumArtUrl = null,
            onPlayPauseToggle = {},
            onStop = {},
            onSeekTo = {}
        )
    }
}

@Preview(name = "Playing - Live with Album Art")
@Composable
private fun PreviewPlayingLiveWithAlbumArt() {
    WrekTheme {
        MediaFooter(
            playbackState = PlaybackState.Playing(
                show = previewShow,
                currentUrl = "https://example.com/stream",
                currentMediaItemIndex = 0,
                position = 0,
                duration = null,
                isLiveStream = true,
                songMetadata = com.selbie.wrek.data.models.SongMetadata(track = "Overnight Alternatives", artist = "")
            ),
            albumArtUrl = "https://lastfm.freetls.fastly.net/i/u/300x300/f5a96b8d1bce4ae2ba6e4b4e7b42e5cc.jpg",
            onPlayPauseToggle = {},
            onStop = {},
            onSeekTo = {}
        )
    }
}

@Preview(name = "Playing - Live with Album Art Long")
@Composable
private fun PreviewPlayingLiveWithAlbumArtLong() {
    WrekTheme {
        MediaFooter(
            playbackState = PlaybackState.Playing(
                show = previewShow,
                currentUrl = "https://example.com/stream",
                currentMediaItemIndex = 0,
                position = 0,
                duration = null,
                isLiveStream = true,
                songMetadata = com.selbie.wrek.data.models.SongMetadata(track = "A different kind of truth in another direction", artist = "Charlie Parr, Steve Tibbits, Captain Beefheart")
            ),
            albumArtUrl = "https://lastfm.freetls.fastly.net/i/u/300x300/f5a96b8d1bce4ae2ba6e4b4e7b42e5cc.jpg",
            onPlayPauseToggle = {},
            onStop = {},
            onSeekTo = {}
        )
    }
}

@Preview(name = "Playing - Pre-recorded")
@Composable
private fun PreviewPlayingPreRecorded() {
    WrekTheme {
        MediaFooter(
            playbackState = PlaybackState.Playing(
                show = previewShow,
                currentUrl = "https://example.com/episode.mp3",
                currentMediaItemIndex = 0,
                position = 263000,
                duration = 495000,
                isLiveStream = false
            ),
            albumArtUrl = null,
            onPlayPauseToggle = {},
            onStop = {},
            onSeekTo = {}
        )
    }
}

@Preview(name = "Paused")
@Composable
private fun PreviewPaused() {
    WrekTheme {
        MediaFooter(
            playbackState = PlaybackState.Paused(
                show = previewShow,
                currentUrl = "https://example.com/episode.mp3",
                currentMediaItemIndex = 0,
                position = 263000,
                duration = 495000,
                isLiveStream = false
            ),
            albumArtUrl = null,
            onPlayPauseToggle = {},
            onStop = {},
            onSeekTo = {}
        )
    }
}

@Preview(name = "Error")
@Composable
private fun PreviewError() {
    WrekTheme {
        MediaFooter(
            playbackState = PlaybackState.Error(
                lastShow = previewShow,
                errorMessage = "Network connection failed"
            ),
            albumArtUrl = null,
            onPlayPauseToggle = {},
            onStop = {},
            onSeekTo = {}
        )
    }
}
