package com.musfit.data.remote.coach

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.musfit.data.repository.AiCoachConnection
import com.musfit.data.repository.AiCoachProviderKind
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.net.InetAddress
import java.net.URI
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
    private val client = okHttpClient.newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val requestAdapter = moshi.adapter(OpenAiChatCompletionRequest::class.java)
    private val responseAdapter = moshi.adapter(OpenAiChatCompletionResponse::class.java)
    private val errorAdapter = moshi.adapter(OpenAiErrorResponse::class.java)

    override suspend fun testConnection(connection: AiCoachConnection) {
        executeJsonRequest(connection, "models")
    }

    override suspend fun chat(request: HermesChatRequest): HermesChatResponse {
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
        relativePath: String,
        method: String = "GET",
        bodyJson: String? = null,
        idempotencyKey: String? = null,
        remoteSessionId: String? = null,
    ): RawHermesResponse = withContext(Dispatchers.IO) {
        val builder = Request.Builder()
            .url(connection.baseUrl.normalizedEndpoint(relativePath))
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

        clientFor(connection).newCall(builder.build()).execute().use { response ->
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

    private fun clientFor(connection: AiCoachConnection): OkHttpClient {
        if (!connection.shouldPreferLocalNetwork()) return client
        val localNetwork = connectivityManager.allNetworks.firstOrNull { network ->
            connectivityManager.getNetworkCapabilities(network)?.isLocalTransport == true
        } ?: return client

        return client.newBuilder()
            .socketFactory(localNetwork.socketFactory)
            .dns(object : Dns {
                override fun lookup(hostname: String): List<InetAddress> =
                    localNetwork.getAllByName(hostname).toList()
            })
            .build()
    }

    private fun String.normalizedEndpoint(relativePath: String): String =
        trimEnd('/') + "/" + relativePath.trimStart('/')

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

internal fun AiCoachConnection.shouldPreferLocalNetwork(): Boolean {
    if (providerKind != AiCoachProviderKind.LocalAgent) return false
    val host = runCatching { URI(baseUrl).host }.getOrNull()?.trim()?.trim('[', ']') ?: return false
    return host.isLocalNetworkHost()
}

private val NetworkCapabilities.isLocalTransport: Boolean
    get() = hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
        hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)

private fun String.isLocalNetworkHost(): Boolean {
    val host = lowercase()
    if (host == "localhost" || host == "127.0.0.1" || host == "::1") return false
    if (host.endsWith(".local") || host.endsWith(".lan") || host.endsWith(".home") || host.endsWith(".fritz.box")) {
        return true
    }
    if (!host.contains('.')) return true
    if (host.contains(':')) return host.startsWith("fc") || host.startsWith("fd") || host.startsWith("fe80")

    val octets = host.split('.').map { it.toIntOrNull() ?: return false }
    if (octets.size != 4 || octets.any { it !in 0..255 }) return false
    return octets[0] == 10 ||
        (octets[0] == 172 && octets[1] in 16..31) ||
        (octets[0] == 192 && octets[1] == 168) ||
        (octets[0] == 169 && octets[1] == 254)
}

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
