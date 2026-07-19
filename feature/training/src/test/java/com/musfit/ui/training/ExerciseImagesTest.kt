package com.musfit.ui.training

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import coil.Coil
import coil.ImageLoader
import coil.decode.BitmapFactoryDecoder
import coil.decode.ImageDecoderDecoder
import coil.intercept.Interceptor
import coil.request.CachePolicy
import coil.request.ErrorResult
import com.musfit.data.repository.exerciseMediaUrl
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExerciseImagesTest {
    @get:Rule
    val compose = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val accent = TabAccent(
        color = androidx.compose.ui.graphics.Color.Blue,
        onColor = androidx.compose.ui.graphics.Color.White,
        container = androidx.compose.ui.graphics.Color.LightGray,
        onContainer = androidx.compose.ui.graphics.Color.Black,
    )

    @Test
    fun staticAndAnimatedRequests_shareDiskEntryButSeparateMemoryEntries() {
        val imageUrl = exerciseMediaUrl("images/0001-bench-press.jpg")
        val gifUrl = exerciseMediaUrl("videos/0001-bench-press.gif")

        assertEquals(imageUrl, gifUrl)
        val staticRequest = exerciseThumbnailRequest(context, requireNotNull(imageUrl))
        val animatedRequest = exerciseAnimatedMediaRequest(context, requireNotNull(gifUrl))

        assertEquals(staticRequest.diskCacheKey, animatedRequest.diskCacheKey)
        assertEquals(imageUrl, staticRequest.diskCacheKey)
        assertEquals(
            EXERCISE_MEDIA_MODE_STATIC,
            staticRequest.parameters.value<String>(EXERCISE_MEDIA_MODE_PARAMETER),
        )
        assertEquals(
            EXERCISE_MEDIA_MODE_ANIMATED,
            animatedRequest.parameters.value<String>(EXERCISE_MEDIA_MODE_PARAMETER),
        )
        assertEquals(
            EXERCISE_MEDIA_MODE_STATIC,
            staticRequest.parameters.memoryCacheKey(EXERCISE_MEDIA_MODE_PARAMETER),
        )
        assertEquals(
            EXERCISE_MEDIA_MODE_ANIMATED,
            animatedRequest.parameters.memoryCacheKey(EXERCISE_MEDIA_MODE_PARAMETER),
        )
        assertNotEquals(
            staticRequest.parameters.memoryCacheKeys(),
            animatedRequest.parameters.memoryCacheKeys(),
        )
    }

    @Test
    fun exerciseRequests_useSharedModeSpecificDecodersAndEnableAllCaches() {
        val url = "https://example.com/exercise.gif"
        val firstStatic = exerciseThumbnailRequest(context, url)
        val secondStatic = exerciseThumbnailRequest(context, url)
        val firstAnimated = exerciseAnimatedMediaRequest(context, url)
        val secondAnimated = exerciseAnimatedMediaRequest(context, url)

        assertTrue(firstStatic.decoderFactory is BitmapFactoryDecoder.Factory)
        assertSame(firstStatic.decoderFactory, secondStatic.decoderFactory)
        assertTrue(firstAnimated.decoderFactory is ImageDecoderDecoder.Factory)
        assertSame(firstAnimated.decoderFactory, secondAnimated.decoderFactory)
        listOf(firstStatic, firstAnimated).forEach { request ->
            assertEquals(CachePolicy.ENABLED, request.memoryCachePolicy)
            assertEquals(CachePolicy.ENABLED, request.diskCachePolicy)
            assertEquals(CachePolicy.ENABLED, request.networkCachePolicy)
        }
    }

    @Test
    fun detailMediaMode_fallsBackFromFailedGifToStaticThenPlaceholder() {
        assertEquals(
            ExerciseDetailMediaMode.Animated,
            exerciseDetailMediaMode("demo.gif", "thumb.jpg", gifUnavailable = false),
        )
        assertEquals(
            ExerciseDetailMediaMode.Static,
            exerciseDetailMediaMode("demo.gif", "thumb.jpg", gifUnavailable = true),
        )
        assertEquals(
            ExerciseDetailMediaMode.Placeholder,
            exerciseDetailMediaMode("demo.gif", null, gifUnavailable = true),
        )
        assertEquals(
            ExerciseDetailMediaMode.Static,
            exerciseDetailMediaMode(null, "thumb.jpg", gifUnavailable = false),
        )
    }

    @Test
    fun failedNonblankStaticThumbnail_reportsErrorAndShowsFallback() {
        val expectedFailure = IOException("static thumbnail failed")
        val failingImageLoader = ImageLoader.Builder(context)
            .components {
                add(
                    Interceptor { chain ->
                        ErrorResult(
                            drawable = null,
                            request = chain.request,
                            throwable = expectedFailure,
                        )
                    },
                )
            }
            .build()
        Coil.setImageLoader(failingImageLoader)
        var staticErrors = 0
        var reportedFailure: Throwable? = null
        try {
            compose.setContent {
                MusFitTheme {
                    ExerciseThumb(
                        imageUrl = "https://example.com/nonblank-thumbnail.jpg",
                        contentDescription = "Static exercise fallback",
                        accent = accent,
                        onLoadError = { failure ->
                            staticErrors += 1
                            reportedFailure = failure
                        },
                    )
                }
            }

            compose.waitUntil(timeoutMillis = 5_000) { staticErrors == 1 }
            compose.onNodeWithContentDescription("Static exercise fallback").assertExists()
            compose.waitForIdle()
            assertSame(expectedFailure, reportedFailure)
            assertEquals(1, staticErrors)
        } finally {
            Coil.reset()
            failingImageLoader.shutdown()
        }
    }

    @Test
    fun mediaFallbacks_areVisibleAndAnimatedFailureIsReportedOnce() {
        var animatedErrors = 0
        compose.setContent {
            MusFitTheme {
                ExerciseThumb(
                    imageUrl = "   ",
                    contentDescription = "Static exercise fallback",
                    accent = accent,
                )
                ExerciseGif(
                    gifUrl = "file:///does-not-exist.gif",
                    contentDescription = "Animated exercise fallback",
                    accent = accent,
                    onError = { animatedErrors += 1 },
                )
            }
        }

        compose.onNodeWithContentDescription("Static exercise fallback").assertExists()
        compose.waitUntil(timeoutMillis = 5_000) { animatedErrors == 1 }
        compose.onNodeWithText("Demo unavailable").assertExists()
        compose.waitForIdle()
        assertEquals(1, animatedErrors)
    }
}
