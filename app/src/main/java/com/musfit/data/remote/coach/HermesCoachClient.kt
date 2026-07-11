package com.musfit.data.remote.coach

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.musfit.data.repository.AiCoachConnection
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class HermesChatMessage(
    val role: String,
    val content: String,
)

data class HermesChatRequest(
    val connection: AiCoachConnection,
    val systemPrompt: String,
    val messages: List<HermesChatMessage>,
    val idempotencyKey: String,
    val remoteSessionId: String?,
)

data class HermesChatResponse(
    val content: String,
    val remoteSessionId: String?,
)

interface CoachCompletionClient {
    suspend fun testConnection(connection: AiCoachConnection)
    suspend fun chat(request: HermesChatRequest): HermesChatResponse
}

class HermesCoachClient @Inject constructor(
    okHttpClient: OkHttpClient,
    moshi: Moshi,
    @ApplicationContext context: Context,
) : CoachCompletionClient {
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    private var localNetworkProvider: () -> Network? = {
        connectivityManager.allNetworks.firstOrNull { network ->
            connectivityManager.getNetworkCapabilities(network)?.isLocalTransport == true
        }
    }
    private val client = hermesRequestClient(okHttpClient)
    private val requestAdapter = moshi.adapter(OpenAiChatCompletionRequest::class.java)
    private val responseAdapter = moshi.adapter(OpenAiChatCompletionResponse::class.java)
    private val errorAdapter = moshi.adapter(OpenAiErrorResponse::class.java)

    internal constructor(
        okHttpClient: OkHttpClient,
        moshi: Moshi,
        context: Context,
        localNetworkProvider: () -> Network?,
    ) : this(okHttpClient, moshi, context) {
        this.localNetworkProvider = localNetworkProvider
    }

    override suspend fun testConnection(connection: AiCoachConnection) {
        val endpoint = AiCoachEndpointPolicy.requireAllowed(connection.baseUrl)
        executeJsonRequest(connection, endpoint, "models")
    }

    override suspend fun chat(request: HermesChatRequest): HermesChatResponse {
        val endpoint = AiCoachEndpointPolicy.requireAllowed(request.connection.baseUrl)
        val body = OpenAiChatCompletionRequest(
            model = request.connection.modelName.ifBlank { DEFAULT_HERMES_MODEL },
            messages = buildList {
                add(OpenAiChatMessage(role = "system", content = request.systemPrompt))
                addAll(request.messages.map { OpenAiChatMessage(it.role, it.content) })
            },
            stream = false,
        )
        val rawJson = requestAdapter.toJson(body)
        val response = executeJsonRequest(
            connection = request.connection,
            endpoint = endpoint,
            relativePath = "chat/completions",
            method = "POST",
            bodyJson = rawJson,
            idempotencyKey = request.idempotencyKey,
            remoteSessionId = request.remoteSessionId?.takeIf { request.connection.apiKey != null },
        )
        val parsed = responseAdapter.fromJson(response.body)
            ?: throw IOException("Hermes returned an empty response.")
        val content = parsed.choices.firstOrNull()?.message?.content.orEmpty().trim()
        if (content.isBlank()) throw IOException("Hermes returned no coach reply.")
        return HermesChatResponse(
            content = content,
            remoteSessionId = response.remoteSessionId ?: request.remoteSessionId,
        )
    }

    private suspend fun executeJsonRequest(
        connection: AiCoachConnection,
        endpoint: ValidatedAiCoachEndpoint,
        relativePath: String,
        method: String = "GET",
        bodyJson: String? = null,
        idempotencyKey: String? = null,
        remoteSessionId: String? = null,
    ): RawHermesResponse = withContext(Dispatchers.IO) {
        val builder = Request.Builder()
            .url(endpoint.resolve(relativePath))
            .header("Accept", "application/json")
        connection.apiKey?.takeIf { it.isNotBlank() }?.let { builder.header("Authorization", "Bearer $it") }
        idempotencyKey?.let { builder.header("Idempotency-Key", it) }
        remoteSessionId?.let { builder.header("X-Hermes-Session-Id", it) }

        if (bodyJson == null) {
            builder.get()
        } else {
            builder.method(method, bodyJson.toRequestBody(JSON_MEDIA_TYPE))
                .header("Content-Type", JSON_MEDIA_TYPE.toString())
        }

        clientFor(endpoint).newCall(builder.build()).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = errorAdapter.fromJsonOrNull(body)?.error?.message
                    ?: body.take(180).ifBlank { "HTTP ${response.code}" }
                throw IOException(message)
            }
            RawHermesResponse(
                body = body,
                remoteSessionId = response.header("X-Hermes-Session-Id")?.takeIf { it.isNotBlank() },
            )
        }
    }

    private fun clientFor(endpoint: ValidatedAiCoachEndpoint): OkHttpClient {
        if (
            !AI_COACH_PRIVATE_ENDPOINT_ROUTING_ENABLED ||
            endpoint.route != AiCoachEndpointRoute.PrivateLan
        ) {
            return client
        }
        val localNetwork = localNetworkProvider()
            ?: throw IOException("A private AI coach endpoint requires an active Wi-Fi or Ethernet network.")

        return client.newBuilder()
            .socketFactory(localNetwork.socketFactory)
            .dns(object : Dns {
                override fun lookup(hostname: String): List<InetAddress> =
                    localNetwork.getAllByName(hostname).toList()
            })
            .build()
    }

    private fun <T> com.squareup.moshi.JsonAdapter<T>.fromJsonOrNull(raw: String): T? =
        runCatching { fromJson(raw) }.getOrNull()

    private data class RawHermesResponse(
        val body: String,
        val remoteSessionId: String?,
    )

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        const val DEFAULT_HERMES_MODEL = "hermes-agent"
    }
}

internal fun AiCoachConnection.shouldPreferLocalNetwork(): Boolean =
    AiCoachEndpointPolicy.requiresPrivateLanRouting(baseUrl)

internal fun hermesRequestClient(okHttpClient: OkHttpClient): OkHttpClient =
    okHttpClient.newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

private val NetworkCapabilities.isLocalTransport: Boolean
    get() = hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
        hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)

private data class OpenAiChatCompletionRequest(
    val model: String,
    val messages: List<OpenAiChatMessage>,
    val stream: Boolean,
)

private data class OpenAiChatMessage(
    val role: String,
    val content: String,
)

private data class OpenAiChatCompletionResponse(
    val choices: List<OpenAiChoice> = emptyList(),
)

private data class OpenAiChoice(
    val message: OpenAiChatMessage? = null,
)

private data class OpenAiErrorResponse(
    val error: OpenAiError? = null,
)

private data class OpenAiError(
    @param:Json(name = "message") val message: String? = null,
)
