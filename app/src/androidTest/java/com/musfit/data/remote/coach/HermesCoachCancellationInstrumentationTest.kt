package com.musfit.data.remote.coach

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.musfit.data.repository.AiCoachConnection
import com.musfit.data.repository.AiCoachProviderKind
import com.musfit.data.repository.LocalAgentKind
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

@RunWith(AndroidJUnit4::class)
class HermesCoachCancellationInstrumentationTest {
    @Test
    fun cancelDelayedRequest_cancelsUnderlyingCallWithinOneSecond() = runBlocking {
        val requestStarted = CountDownLatch(1)
        val callCanceled = CountDownLatch(1)
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                requestStarted.countDown()
                val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
                while (!chain.call().isCanceled() && System.nanoTime() < deadline) {
                    Thread.sleep(10)
                }
                if (chain.call().isCanceled()) callCanceled.countDown()
                throw IOException("Delayed instrumentation request stopped.")
            }
            .build()
        val client = HermesCoachClient(
            okHttpClient = okHttpClient,
            moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build(),
            context = ApplicationProvider.getApplicationContext<Context>(),
            localNetworkProvider = { null },
        )
        val request = async(Dispatchers.IO) {
            client.testConnection(
                AiCoachConnection(
                    providerKind = AiCoachProviderKind.OpenAiCompatible,
                    baseUrl = "https://api.example.com/v1/",
                    modelName = "test-model",
                    localAgentKind = LocalAgentKind.Custom,
                    apiKey = "instrumentation-test-key",
                ),
            )
        }
        assertTrue("Delayed request did not start.", requestStarted.await(1, TimeUnit.SECONDS))

        val cancellationMillis = measureTimeMillis { request.cancelAndJoin() }

        assertTrue("Underlying OkHttp call was not canceled.", callCanceled.await(1, TimeUnit.SECONDS))
        assertTrue("Cancellation took ${cancellationMillis}ms.", cancellationMillis < 1_000L)
    }
}
