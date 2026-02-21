package com.selbie.wrek.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.selbie.wrek.R
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
    val isDark = isSystemInDarkTheme()
    val baseAlpha = if (isDark) 0.65f else 0.75f
    val scrimAlpha = remember { Animatable(baseAlpha) }

    var skipNextAnimation by remember { mutableStateOf(isSelected) }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            if (skipNextAnimation) {
                skipNextAnimation = false
                scrimAlpha.snapTo(baseAlpha)
            } else {
                scrimAlpha.snapTo((baseAlpha - 0.30f).coerceAtLeast(0f))
                scrimAlpha.animateTo(baseAlpha, animationSpec = tween(3000))
            }
        } else {
            scrimAlpha.snapTo(baseAlpha)
        }
    }

    val scrimColor = if (isDark) Color.Black.copy(alpha = scrimAlpha.value) else Color.White.copy(alpha = scrimAlpha.value)
    val textColor = if (isDark) Color.White else Color.Black

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, textColor)
        } else {
            null
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            // Background image layer
            show.logoUrl?.let { url ->
                val blurHashBitmap = rememberBlurHashBitmap(show.logoBlurHash, width = 32, height = 32)
                val placeholder = blurHashBitmap?.let { BitmapPainter(it) }

                AsyncImage(
                    model = url,
                    contentDescription = stringResource(R.string.cd_show_logo, show.title),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = placeholder
                )
            }

            // Dark scrim overlay
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(scrimColor)
            )

            // Text content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = show.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = show.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
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
                            color = textColor
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
