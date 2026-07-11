package com.musfit.data.remote.coach

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.musfit.BuildConfig
import com.musfit.data.repository.AiCoachConnection
import com.musfit.data.repository.AiCoachProviderKind
import com.musfit.data.repository.LocalAgentKind
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.sun.net.httpserver.HttpServer
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HermesCoachClientTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @Test
    fun invalidPublicHttpChatIsRejectedBeforeSensitiveRequestDispatch() = runTest {
        val dispatches = AtomicInteger()
        val client = clientWithResponse(dispatches, OPEN_AI_RESPONSE)
        val completionClient = HermesCoachClient(client, moshi, context, localNetworkProvider = { null })
        val request = HermesChatRequest(
            connection = connection(
                baseUrl = "http://api.example.com/v1/",
                apiKey = "dummy-bearer-never-dispatch",
            ),
            systemPrompt = "dummy-system-never-dispatch",
            messages = listOf(HermesChatMessage("user", "dummy-chat-never-dispatch")),
            idempotencyKey = "dummy-idempotency-never-dispatch",
            remoteSessionId = "dummy-session-never-dispatch",
        )

        try {
            completionClient.chat(request)
            fail("Expected public HTTP to be rejected")
        } catch (_: IllegalArgumentException) {
            assertEquals(0, dispatches.get())
        }
    }

    @Test
    fun allowedHttpsBuildsAndDispatchesTheBearerRequest() = runTest {
        val dispatches = AtomicInteger()
        var observedAuthorization: String? = null
        var observedBody: String? = null
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                dispatches.incrementAndGet()
                observedAuthorization = chain.request().header("Authorization")
                observedBody = chain.request().body?.let { body ->
                    val buffer = okio.Buffer()
                    body.writeTo(buffer)
                    buffer.readUtf8()
                }
                syntheticResponse(chain, OPEN_AI_RESPONSE)
            }
            .build()
        val completionClient = HermesCoachClient(client, moshi, context, localNetworkProvider = { null })

        val response = completionClient.chat(
            HermesChatRequest(
                connection = connection("https://api.example.com/v1/", apiKey = "dummy-allowed-key"),
                systemPrompt = "dummy-allowed-system",
                messages = listOf(HermesChatMessage("user", "dummy-allowed-chat")),
                idempotencyKey = "dummy-allowed-id",
                remoteSessionId = null,
            ),
        )

        assertEquals(1, dispatches.get())
        assertEquals("Bearer dummy-allowed-key", observedAuthorization)
        assertTrue(observedBody.orEmpty().contains("dummy-allowed-chat"))
        assertEquals("Safe response", response.content)
    }

    @Test
    fun privateLanRequestFailsClosedWhenNoWifiOrEthernetExists() = runTest {
        assumeTrue(isInternal)
        val dispatches = AtomicInteger()
        val completionClient = HermesCoachClient(
            clientWithResponse(dispatches, "{}"),
            moshi,
            context,
            localNetworkProvider = { null },
        )

        try {
            completionClient.testConnection(
                connection(
                    baseUrl = "http://192.168.1.8:8080/v1/",
                    providerKind = AiCoachProviderKind.OpenAiCompatible,
                ),
            )
            fail("Expected private LAN dispatch without a local transport to fail closed")
        } catch (expected: IOException) {
            assertTrue(expected.message.orEmpty().contains("Wi-Fi or Ethernet"))
            assertEquals(0, dispatches.get())
        }
    }

    @Test
    fun loopbackRequestRemainsUnboundAndCanDispatchInternally() = runTest {
        assumeTrue(isInternal)
        val dispatches = AtomicInteger()
        val completionClient = HermesCoachClient(
            clientWithResponse(dispatches, "{}"),
            moshi,
            context,
            localNetworkProvider = { null },
        )

        completionClient.testConnection(connection("http://127.0.0.1:8080/v1/"))

        assertEquals(1, dispatches.get())
    }

    @Test
    fun requestClientDisablesHttpAndTlsRedirectFollowing() {
        val secured = hermesRequestClient(OkHttpClient())

        assertFalse(secured.followRedirects)
        assertFalse(secured.followSslRedirects)
    }

    @Test
    fun redirectResponseCannotCarryBearerOrChatBodyToASecondEndpoint() = runTest {
        assumeTrue(isInternal)
        val loopback = InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1))
        val secondHopCount = AtomicInteger()
        val secondHopAuthorization = AtomicReference<String?>()
        val secondHopBody = AtomicReference<String?>()
        val secondHop = HttpServer.create(InetSocketAddress(loopback, 0), 0).apply {
            createContext("/") { exchange ->
                secondHopCount.incrementAndGet()
                secondHopAuthorization.set(exchange.requestHeaders.getFirst("Authorization"))
                secondHopBody.set(exchange.requestBody.bufferedReader().use { it.readText() })
                val response = OPEN_AI_RESPONSE.toByteArray()
                exchange.sendResponseHeaders(200, response.size.toLong())
                exchange.responseBody.use { it.write(response) }
            }
            start()
        }
        val firstHopCount = AtomicInteger()
        val redirect = HttpServer.create(InetSocketAddress(loopback, 0), 0).apply {
            createContext("/") { exchange ->
                firstHopCount.incrementAndGet()
                exchange.requestBody.close()
                exchange.responseHeaders.add(
                    "Location",
                    "http://127.0.0.1:${secondHop.address.port}${exchange.requestURI}",
                )
                exchange.sendResponseHeaders(307, -1)
                exchange.close()
            }
            start()
        }
        try {
            val completionClient = HermesCoachClient(
                OkHttpClient(),
                moshi,
                context,
                localNetworkProvider = { null },
            )

            try {
                completionClient.chat(
                    HermesChatRequest(
                        connection = connection(
                            "http://127.0.0.1:${redirect.address.port}/v1/",
                            apiKey = "dummy-redirect-key",
                        ),
                        systemPrompt = "dummy-redirect-system",
                        messages = listOf(HermesChatMessage("user", "dummy-redirect-chat")),
                        idempotencyKey = "dummy-redirect-id",
                        remoteSessionId = null,
                    ),
                )
                fail("Expected the non-followed redirect to surface as an HTTP error")
            } catch (_: IOException) {
                assertEquals(1, firstHopCount.get())
                assertEquals(0, secondHopCount.get())
                assertEquals(null, secondHopAuthorization.get())
                assertEquals(null, secondHopBody.get())
            }
        } finally {
            redirect.stop(0)
            secondHop.stop(0)
        }
    }

    @Test
    fun privateRoutingClassificationIgnoresProviderLabelsAndKeepsLoopbackUnbound() {
        val privateConnection = connection(
            baseUrl = "https://192.168.1.8:8443/v1/",
            providerKind = AiCoachProviderKind.OpenAiCompatible,
        )
        val loopbackConnection = connection("https://127.0.0.1:8443/v1/")

        assertEquals(isInternal, privateConnection.shouldPreferLocalNetwork())
        assertFalse(loopbackConnection.shouldPreferLocalNetwork())
    }

    private fun clientWithResponse(dispatches: AtomicInteger, body: String): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                dispatches.incrementAndGet()
                syntheticResponse(chain, body)
            }
            .build()

    private fun syntheticResponse(chain: Interceptor.Chain, body: String): Response =
        Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(body.toResponseBody())
            .build()

    private fun connection(
        baseUrl: String,
        providerKind: AiCoachProviderKind = AiCoachProviderKind.LocalAgent,
        apiKey: String? = "dummy-key",
    ): AiCoachConnection =
        AiCoachConnection(
            providerKind = providerKind,
            baseUrl = baseUrl,
            modelName = "hermes-agent",
            localAgentKind = LocalAgentKind.HermesAgent,
            apiKey = apiKey,
        )

    private companion object {
        const val OPEN_AI_RESPONSE =
            "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Safe response\"}}]}"
        val isInternal: Boolean = BuildConfig.APPLICATION_ID == "com.musfit.internal"
    }
}
