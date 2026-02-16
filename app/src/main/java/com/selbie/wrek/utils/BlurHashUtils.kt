package com.selbie.wrek.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Composable function to decode BlurHash and convert to ImageBitmap
 */
@Composable
fun rememberBlurHashBitmap(
    blurHash: String?,
    width: Int = 32,
    height: Int = 32,
    punch: Float = 1f
): ImageBitmap? {
    return remember(blurHash) {
        BlurHashDecoder.decode(blurHash, width, height, punch)?.asImageBitmap()
    }
}