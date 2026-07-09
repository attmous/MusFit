package com.musfit.data.remote.coach

import com.musfit.data.repository.AiCoachConnection
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
) : CoachCompletionClient {
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

        client.newCall(builder.build()).execute().use { response ->
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
