package com.selbie.wrek.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.selbie.wrek.R
import com.selbie.wrek.data.models.PlaybackState
import com.selbie.wrek.data.models.RadioShow
import com.selbie.wrek.ui.theme.WrekTheme

@Composable
fun MediaFooter(
    playbackState: PlaybackState,
    onPlayPauseToggle: () -> Unit,
    onStop: () -> Unit,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Extract display values from playback state
    val titleText = when (playbackState) {
        is PlaybackState.Idle -> stringResource(R.string.media_footer_select_show)
        is PlaybackState.Loading -> stringResource(R.string.media_footer_loading)
        is PlaybackState.Playing -> playbackState.songTitle ?: playbackState.show.title
        is PlaybackState.Paused -> playbackState.songTitle ?: playbackState.show.title
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

    Column(modifier = modifier.navigationBarsPadding()) {
        HorizontalDivider()
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Title row
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                // Button + seekbar row
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

                // Timestamps row (always present to keep footer height stable)
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
                        // Invisible placeholder to reserve the same vertical space
                        Text(
                            text = "",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
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
                isLiveStream = true
            ),
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
            onPlayPauseToggle = {},
            onStop = {},
            onSeekTo = {}
        )
    }
}
