package com.musfit.ui.training

import android.graphics.drawable.Animatable
import android.net.Uri
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import coil.imageLoader
import coil.request.SuccessResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 28)
class ExerciseAnimatedMediaInstrumentationTest {
    @Test
    fun animatedRequest_decodesTwoFrameGifAsAnimatableDrawable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mediaFile = File(context.cacheDir, "exercise-media-${System.nanoTime()}.gif").apply {
            writeBytes(Base64.decode(ANIMATED_GIF_BASE64, Base64.DEFAULT))
        }
        val imageLoader = context.imageLoader

        try {
            assertSame(imageLoader, context.applicationContext.imageLoader)
            val request = exerciseAnimatedMediaRequest(context, Uri.fromFile(mediaFile).toString())
            val result = runBlocking { imageLoader.execute(request) }

            val success = result as? SuccessResult
                ?: error("Expected a successful GIF decode but received $result")
            val drawable = success.drawable
            assertTrue(
                "Expected an Animatable GIF drawable but received ${drawable.javaClass.name}",
                drawable is Animatable,
            )
        } finally {
            mediaFile.delete()
        }
    }

    private companion object {
        // Valid 1x1, two-frame GIF89a with a looping application extension.
        const val ANIMATED_GIF_BASE64 =
            "R0lGODlhAQABAIAAAAAAAP///yH/C05FVFNDQVBFMi4wAwEAAAAh+QQACgAA" +
                "ACwAAAAAAQABAAACAkQBACH5BAAKAAAALAAAAAABAAEAAAICTAEAOw=="
    }
}
