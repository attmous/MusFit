package com.musfit.ui.training

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
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
        ExerciseMediaPlaceholder(
            contentDescription = contentDescription,
            accent = accent,
            modifier = modifier.size(size),
            shape = shape,
            iconSize = size / 2,
        )
        return
    }
    val context = LocalContext.current
    val imageLoader = rememberExerciseMediaImageLoader(context)
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context).data(imageUrl).crossfade(true).build(),
        imageLoader = imageLoader,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier.size(size).clip(shape).background(Color.White),
        loading = {
            ExerciseMediaPlaceholder(
                contentDescription = contentDescription,
                accent = accent,
                modifier = Modifier.size(size),
                shape = shape,
                iconSize = size / 2,
                loading = true,
            )
        },
        error = {
            ExerciseMediaPlaceholder(
                contentDescription = contentDescription,
                accent = accent,
                modifier = Modifier.size(size),
                shape = shape,
                iconSize = size / 2,
            )
        },
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
    accent: TabAccent,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    onError: () -> Unit = {},
) {
    val context = LocalContext.current
    val gifLoader = rememberExerciseMediaImageLoader(context)
    var reportedError by remember(gifUrl) { mutableStateOf(false) }
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context).data(gifUrl).crossfade(true).build(),
        imageLoader = gifLoader,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(Color.White),
        loading = {
            ExerciseMediaPlaceholder(
                contentDescription = contentDescription,
                accent = accent,
                modifier = Modifier.fillMaxWidth().height(height),
                shape = shape,
                iconSize = 32.dp,
                loading = true,
                label = "Loading demo",
            )
        },
        error = {
            ExerciseMediaPlaceholder(
                contentDescription = contentDescription,
                accent = accent,
                modifier = Modifier.fillMaxWidth().height(height),
                shape = shape,
                iconSize = 32.dp,
                label = "Demo unavailable",
            )
        },
        onError = {
            if (!reportedError) {
                reportedError = true
                onError()
            }
        },
    )
}

@Composable
private fun rememberExerciseMediaImageLoader(context: Context): ImageLoader =
    remember(context) {
        ImageLoader.Builder(context)
            .components { add(ImageDecoderDecoder.Factory()) }
            .build()
    }

@Composable
private fun ExerciseMediaPlaceholder(
    contentDescription: String?,
    accent: TabAccent,
    modifier: Modifier,
    shape: Shape,
    iconSize: Dp,
    loading: Boolean = false,
    label: String? = null,
) {
    Box(
        modifier = modifier.clip(shape).background(accent.container),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    color = accent.color,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(iconSize),
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.FitnessCenter,
                    contentDescription = contentDescription,
                    tint = accent.onContainer,
                    modifier = Modifier.size(iconSize),
                )
            }
            label?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = accent.onContainer,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
