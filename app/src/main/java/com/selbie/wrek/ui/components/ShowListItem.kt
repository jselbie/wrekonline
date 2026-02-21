package com.selbie.wrek.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.selbie.wrek.data.models.RadioShow
import com.selbie.wrek.utils.rememberBlurHashBitmap
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ShowListItem(
    show: RadioShow,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            show.logoUrl?.let { url ->
                val blurHashBitmap = rememberBlurHashBitmap(show.logoBlurHash, width = 32, height = 32)
                val placeholder = blurHashBitmap?.let { BitmapPainter(it) }

                AsyncImage(
                    model = url,
                    contentDescription = "${show.title} logo",
                    modifier = Modifier
                        .size(width = 96.dp, height = 64.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                    placeholder = placeholder
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = show.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = show.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                show.creationTime?.let { timeStr ->
                    val formatted = remember(timeStr) {
                        formatCreationTime(timeStr)
                    }
                    if (formatted != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            }
                        )
                    }
                }
            }
        }
    }
}

private val isoParser = DateTimeFormatter.ISO_LOCAL_DATE_TIME
private val displayFormatter = DateTimeFormatter.ofPattern("M/d h:mma", Locale.US)

private fun formatCreationTime(isoString: String): String? {
    return try {
        val dt = LocalDateTime.parse(isoString, isoParser)
        dt.format(displayFormatter).lowercase(Locale.US)
    } catch (_: Exception) {
        null
    }
}
