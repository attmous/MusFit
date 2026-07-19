package com.musfit.ui.training

import android.content.Context
import android.graphics.drawable.Animatable
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import coil.decode.DataSource
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.SuccessResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.util.Base64
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 35])
class ExerciseImageDecodingTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun localAnimatedGif_staticRequestUsesFirstFrameAndThenMemoryCache() {
        val mediaFile = createAnimatedGif()
        val imageLoader = ImageLoader.Builder(context).diskCache(null).build()
        try {
            val mediaUrl = mediaFile.toURI().toString()
            val firstResult = executeOffMain(imageLoader, exerciseThumbnailRequest(context, mediaUrl))
            val secondResult = executeOffMain(imageLoader, exerciseThumbnailRequest(context, mediaUrl))

            assertTrue(firstResult is SuccessResult)
            assertFalse(requireNotNull(firstResult.drawable) is Animatable)
            assertTrue(secondResult is SuccessResult)
            assertEquals(DataSource.MEMORY_CACHE, (secondResult as SuccessResult).dataSource)
        } finally {
            imageLoader.shutdown()
            mediaFile.delete()
        }
    }

    @Test
    fun benchmarkByteArray_staticRequestsUseDistinctKeysAndThenMemoryCache() {
        val imageLoader = ImageLoader.Builder(context).diskCache(null).build()
        try {
            val benchmarkData = Base64.getDecoder().decode(BENCHMARK_PNG_BASE64)
            val firstRequest = exerciseThumbnailRequest(context, "benchmark-exercise-1", benchmarkData)
            val secondRequest = exerciseThumbnailRequest(context, "benchmark-exercise-2", benchmarkData)

            assertTrue(executeOffMain(imageLoader, firstRequest) is SuccessResult)
            assertTrue(executeOffMain(imageLoader, secondRequest) is SuccessResult)
            val firstCachedResult = executeOffMain(imageLoader, firstRequest)
            val secondCachedResult = executeOffMain(imageLoader, secondRequest)

            assertTrue(firstCachedResult is SuccessResult)
            assertEquals(DataSource.MEMORY_CACHE, (firstCachedResult as SuccessResult).dataSource)
            assertTrue(secondCachedResult is SuccessResult)
            assertEquals(DataSource.MEMORY_CACHE, (secondCachedResult as SuccessResult).dataSource)
            assertFalse(firstRequest.memoryCacheKey == secondRequest.memoryCacheKey)
            assertEquals(CachePolicy.ENABLED, firstRequest.memoryCachePolicy)
            assertEquals(CachePolicy.DISABLED, firstRequest.diskCachePolicy)
            assertEquals(CachePolicy.DISABLED, firstRequest.networkCachePolicy)
        } finally {
            imageLoader.shutdown()
        }
    }

    private fun executeOffMain(imageLoader: ImageLoader, request: ImageRequest): ImageResult {
        val executor = Executors.newSingleThreadExecutor()
        val result = executor.submit<ImageResult> {
            runBlocking { imageLoader.execute(request) }
        }
        return try {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
            while (!result.isDone && System.nanoTime() < deadline) {
                shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(1)
            }
            check(result.isDone) { "Timed out waiting for Coil image request" }
            result.get()
        } finally {
            executor.shutdownNow()
        }
    }

    private fun createAnimatedGif(): File = File(context.cacheDir, "exercise-media-${System.nanoTime()}.gif").apply {
        writeBytes(Base64.getDecoder().decode(ANIMATED_GIF_BASE64))
    }

    private companion object {
        // Valid 1x1, two-frame GIF89a with a looping application extension.
        const val ANIMATED_GIF_BASE64 =
            "R0lGODlhAQABAIAAAAAAAP///yH/C05FVFNDQVBFMi4wAwEAAAAh+QQACgAA" +
                "ACwAAAAAAQABAAACAkQBACH5BAAKAAAALAAAAAABAAEAAAICTAEAOw=="
        const val BENCHMARK_PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
    }
}
