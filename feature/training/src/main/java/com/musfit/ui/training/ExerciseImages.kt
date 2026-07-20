package com.musfit.ui.training

import android.content.Context
import android.util.Base64
import android.util.Log
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.decode.BitmapFactoryDecoder
import coil.decode.DataSource
import coil.decode.Decoder
import coil.decode.ImageDecoderDecoder
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.musfit.feature.training.BuildConfig
import com.musfit.feature.training.R
import com.musfit.ui.theme.TabAccent

internal const val EXERCISE_MEDIA_MODE_PARAMETER = "exercise-media-mode"
internal const val EXERCISE_MEDIA_MODE_STATIC = "static"
internal const val EXERCISE_MEDIA_MODE_ANIMATED = "animated"

internal enum class ExerciseDetailMediaMode { Animated, Static, Placeholder }

internal fun exerciseDetailMediaMode(
    gifUrl: String?,
    imageUrl: String?,
    gifUnavailable: Boolean,
): ExerciseDetailMediaMode = when {
    !gifUrl.isNullOrBlank() && !gifUnavailable -> ExerciseDetailMediaMode.Animated
    !imageUrl.isNullOrBlank() -> ExerciseDetailMediaMode.Static
    else -> ExerciseDetailMediaMode.Placeholder
}

private val staticExerciseDecoderFactory = BitmapFactoryDecoder.Factory()
private val animatedExerciseDecoderFactory = ImageDecoderDecoder.Factory()
private val benchmarkThumbnailData: ByteArray? by lazy {
    BuildConfig.BENCHMARK_THUMBNAIL_BASE64
        .takeIf(String::isNotEmpty)
        ?.let { Base64.decode(it, Base64.DEFAULT) }
}

/**
 * Square, static exercise thumbnail. Mirrored GIF media is intentionally decoded to its first
 * frame; animation is reserved for [ExerciseGif] on the visible detail screen. Demos in the
 * dataset sit on white, so the image fills a white tile; when no media is available it falls back
 * to a tinted dumbbell placeholder.
 */
@Composable
@Suppress("LongParameterList")
fun ExerciseThumb(
    imageUrl: String?,
    contentDescription: String?,
    accent: TabAccent,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shape: Shape = RoundedCornerShape(10.dp),
    useBenchmarkFixture: Boolean = false,
    onDataSourceChanged: ((DataSource) -> Unit)? = null,
    onLoading: (() -> Unit)? = null,
    onLoadError: ((Throwable) -> Unit)? = null,
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
    val request = remember(context, imageUrl, useBenchmarkFixture) {
        exerciseThumbnailRequest(
            context = context,
            imageUrl = imageUrl,
            benchmarkData = if (useBenchmarkFixture) benchmarkThumbnailData else null,
        )
    }
    SubcomposeAsyncImage(
        model = request,
        imageLoader = context.imageLoader,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier.size(size).clip(shape).background(Color.White),
        onLoading = { onLoading?.invoke() },
        onSuccess = { onDataSourceChanged?.invoke(it.result.dataSource) },
        onError = { state ->
            onLoadError?.invoke(state.result.throwable)
            if (useBenchmarkFixture) {
                Log.e("MusFitBenchmarkImage", "Deterministic thumbnail failed", state.result.throwable)
            }
        },
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
 * Animated detail demonstration, decoded via Coil's [ImageDecoderDecoder] (minSdk 28). It shares
 * the process image loader and encoded disk entry with thumbnails while keeping a distinct memory
 * cache entry. A failed animation reports once so its detail owner can show the static fallback.
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
    var reportedError by remember(gifUrl) { mutableStateOf(false) }
    SubcomposeAsyncImage(
        model = exerciseAnimatedMediaRequest(context, gifUrl),
        imageLoader = context.imageLoader,
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
                label = stringResource(R.string.training_loading_demo),
            )
        },
        error = {
            ExerciseMediaPlaceholder(
                contentDescription = contentDescription,
                accent = accent,
                modifier = Modifier.fillMaxWidth().height(height),
                shape = shape,
                iconSize = 32.dp,
                label = stringResource(R.string.training_demo_unavailable),
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

internal fun exerciseThumbnailRequest(
    context: Context,
    imageUrl: String,
    benchmarkData: ByteArray? = null,
): ImageRequest {
    if (benchmarkData != null) {
        return ImageRequest.Builder(context)
            .data(benchmarkData)
            .memoryCacheKey("benchmark-picker-static:$imageUrl")
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .networkCachePolicy(CachePolicy.DISABLED)
            .crossfade(false)
            .build()
    }
    return exerciseMediaRequest(
        context = context,
        mediaUrl = imageUrl,
        mode = EXERCISE_MEDIA_MODE_STATIC,
        decoderFactory = staticExerciseDecoderFactory,
        crossfade = false,
    )
}

internal fun exerciseAnimatedMediaRequest(context: Context, gifUrl: String): ImageRequest = exerciseMediaRequest(
    context = context,
    mediaUrl = gifUrl,
    mode = EXERCISE_MEDIA_MODE_ANIMATED,
    decoderFactory = animatedExerciseDecoderFactory,
    crossfade = true,
)

private fun exerciseMediaRequest(
    context: Context,
    mediaUrl: String,
    mode: String,
    decoderFactory: Decoder.Factory,
    crossfade: Boolean,
): ImageRequest = ImageRequest.Builder(context)
    .data(mediaUrl)
    .decoderFactory(decoderFactory)
    // Coil's computed memory key does not include decoderFactory; the mode prevents a static first
    // frame from satisfying the animated detail request while the encoded disk entry stays shared.
    .setParameter(EXERCISE_MEDIA_MODE_PARAMETER, mode, memoryCacheKey = mode)
    .memoryCachePolicy(CachePolicy.ENABLED)
    .diskCachePolicy(CachePolicy.ENABLED)
    .networkCachePolicy(CachePolicy.ENABLED)
    .diskCacheKey(mediaUrl)
    .crossfade(crossfade)
    .build()

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
