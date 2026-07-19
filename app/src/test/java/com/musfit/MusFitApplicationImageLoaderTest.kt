package com.musfit

import androidx.test.core.app.ApplicationProvider
import coil.Coil
import coil.imageLoader
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MusFitApplicationImageLoaderTest {
    @After
    fun tearDown() {
        Coil.reset()
    }

    @Test
    fun applicationProvidesOneProcessImageLoader() {
        Coil.reset()
        val application = ApplicationProvider.getApplicationContext<MusFitApplication>()

        val first = application.imageLoader
        val second = application.imageLoader

        assertSame(first, second)
        assertSame(first, application.newImageLoader())
        assertSame(first, application.newImageLoader())
    }
}
