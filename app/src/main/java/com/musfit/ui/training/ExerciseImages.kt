package com.musfit.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent

/**
 * Square exercise thumbnail. Demos in the dataset sit on white, so the image fills a white tile;
 * when no media is available it falls back to a tinted dumbbell placeholder.
 */
@Composable
fun ExerciseThumb(
    imageUrl: String?,
    contentDescription: String?,
    accent: TabAccent,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shape: Shape = RoundedCornerShape(10.dp),
) {
    if (imageUrl.isNullOrBlank()) {
        Box(
            modifier = modifier.size(size).clip(shape).background(accent.container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.FitnessCenter,
                contentDescription = contentDescription,
                tint = accent.onContainer,
                modifier = Modifier.size(size / 2),
            )
        }
        return
    }
    AsyncImage(
        model = imageUrl,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier.size(size).clip(shape).background(Color.White),
    )
}

/**
 * Animated GIF demonstration, decoded via Coil's [ImageDecoderDecoder] (minSdk 28). Fetched from
 * the CDN and cached; falls back to nothing while loading.
 */
@Composable
fun ExerciseGif(
    gifUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
    shape: Shape = RoundedCornerShape(16.dp),
) {
    val context = LocalContext.current
    val gifLoader = remember(context) {
        ImageLoader.Builder(context)
            .components { add(ImageDecoderDecoder.Factory()) }
            .build()
    }
    AsyncImage(
        model = ImageRequest.Builder(context).data(gifUrl).crossfade(true).build(),
        imageLoader = gifLoader,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(Color.White),
    )
}
