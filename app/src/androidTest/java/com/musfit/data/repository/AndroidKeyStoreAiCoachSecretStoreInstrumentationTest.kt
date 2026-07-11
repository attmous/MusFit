package com.musfit.data.repository

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.musfit.MainActivity
import com.musfit.data.local.MusFitDatabase
import com.musfit.data.remote.coach.HermesCoachClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidKeyStoreAiCoachSecretStoreInstrumentationTest {
    @Test
    fun freshInstallHasNoImplicitHermesCredential() = runBlocking {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val store = AndroidKeyStoreAiCoachSecretStore(targetContext)

        assertNull(store.getApiKey(DEFAULT_ACCOUNT_ID))
        assertFalse(
            Class.forName("com.musfit.BuildConfig").declaredFields.any { field ->
                field.name == "DEBUG_HERMES_API_KEY"
            },
        )
    }

    @Test
    fun runtimeCredentialSaveReadOverwriteIsolationAndDeleteUseCiphertext() = runBlocking {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val store = AndroidKeyStoreAiCoachSecretStore(targetContext)
        val suffix = UUID.randomUUID().toString()
        val firstAccount = "sec003-first-$suffix"
        val secondAccount = "sec003-second-$suffix"
        val firstCredential = "synthetic-runtime-first-$suffix"
        val replacementCredential = "synthetic-runtime-replacement-$suffix"
        val secondCredential = "synthetic-runtime-second-$suffix"

        try {
            assertNull(store.getApiKey(firstAccount))
            assertNull(store.getApiKey(secondAccount))

            store.saveApiKey(firstAccount, firstCredential)
            store.saveApiKey(secondAccount, secondCredential)
            assertEquals(firstCredential, store.getApiKey(firstAccount))
            assertEquals(secondCredential, store.getApiKey(secondAccount))
            assertCiphertextOnly(targetContext, firstAccount, firstCredential)
            assertCiphertextOnly(targetContext, secondAccount, secondCredential)

            store.saveApiKey(firstAccount, replacementCredential)
            assertEquals(replacementCredential, store.getApiKey(firstAccount))
            assertEquals(secondCredential, store.getApiKey(secondAccount))
            assertCiphertextOnly(targetContext, firstAccount, replacementCredential)

            store.clearApiKey(firstAccount)
            assertNull(store.getApiKey(firstAccount))
            assertEquals(secondCredential, store.getApiKey(secondAccount))

            store.clearApiKey(secondAccount)
            assertNull(store.getApiKey(secondAccount))
        } finally {
            store.clearApiKey(firstAccount)
            store.clearApiKey(secondAccount)
        }
    }

    @Test
    fun corruptOrBlankEncryptedPreferenceIsRemovedAndUnreadable() = runBlocking {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val store = AndroidKeyStoreAiCoachSecretStore(targetContext)
        val accountId = "sec003-corrupt-${UUID.randomUUID()}"
        val preferences = targetContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val preferenceKey = "api_key_$accountId"

        try {
            preferences.edit().putString(preferenceKey, "not-valid-ciphertext").commit()
            assertNull(store.getApiKey(accountId))
            assertFalse(preferences.contains(preferenceKey))

            store.saveApiKey(accountId, "   ")
            assertNull(store.getApiKey(accountId))
            assertFalse(preferences.contains(preferenceKey))
        } finally {
            store.clearApiKey(accountId)
        }
    }

    @Test
    fun runtimeEnteredCredentialIsUsedOnceThenRemovalDisablesHermes() = runBlocking {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(targetContext, MusFitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val secretStore = AndroidKeyStoreAiCoachSecretStore(targetContext)
        val repository = localRepository(database, secretStore)
        val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        val requestCount = AtomicInteger(0)
        val authorizationPresent = AtomicBoolean(false)
        val listenerFailure = AtomicReference<Throwable?>(null)
        val listener = startSingleRequestListener(
            server = server,
            requestCount = requestCount,
            authorizationPresent = authorizationPresent,
            failure = listenerFailure,
        )

        try {
            repository.saveSettings(
                AiCoachSettingsInput(
                    providerKind = AiCoachProviderKind.LocalAgent,
                    baseUrl = "http://127.0.0.1:${server.localPort}/v1/",
                    modelName = "hermes-agent",
                    localAgentKind = LocalAgentKind.HermesAgent,
                    apiKey = AiCoachApiKeyUpdate.Replace("synthetic-device-runtime-key"),
                ),
            )
            val connection = repository.activeConnection()
            assertNotNull(connection)

            HermesCoachClient(
                okHttpClient = OkHttpClient(),
                moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build(),
                context = targetContext,
            ).testConnection(connection!!)

            listener.join(5_000)
            assertFalse("Controlled listener did not finish", listener.isAlive)
            listenerFailure.get()?.let { throw AssertionError("Controlled listener failed", it) }
            assertEquals(1, requestCount.get())
            assertTrue(authorizationPresent.get())

            repository.clearApiKey()
            assertNull(repository.activeConnection())
            assertNull(secretStore.getApiKey(DEFAULT_ACCOUNT_ID))
        } finally {
            server.close()
            listener.join(1_000)
            secretStore.clearApiKey(DEFAULT_ACCOUNT_ID)
            database.close()
        }
    }

    @Test
    fun appLaunchWithConfiguredRuntimeCredentialDispatchesZeroStartupRequests() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val targetContext = instrumentation.targetContext
        val secretStore = AndroidKeyStoreAiCoachSecretStore(targetContext)
        val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        val database = com.musfit.core.di.DatabaseModule.provideDatabase(targetContext.applicationContext)
        val repository = localRepository(database, secretStore)
        var activity: Activity? = null
        val monitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)

        try {
            repository.saveSettings(
                AiCoachSettingsInput(
                    providerKind = AiCoachProviderKind.LocalAgent,
                    baseUrl = "http://127.0.0.1:${server.localPort}/v1/",
                    modelName = "hermes-agent",
                    localAgentKind = LocalAgentKind.HermesAgent,
                    apiKey = AiCoachApiKeyUpdate.Replace("synthetic-startup-runtime-key"),
                ),
            )

            targetContext.startActivity(
                Intent(targetContext, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            )
            activity = instrumentation.waitForMonitorWithTimeout(monitor, 5_000)
            assertNotNull("MainActivity did not launch", activity)
            instrumentation.waitForIdleSync()
            SystemClock.sleep(2_000)

            server.soTimeout = 750
            val unexpectedRequestCount = try {
                server.accept().use { 1 }
            } catch (_: SocketTimeoutException) {
                0
            }
            assertEquals("App startup must not contact the configured coach endpoint", 0, unexpectedRequestCount)
        } finally {
            activity?.let { launched -> instrumentation.runOnMainSync { launched.finish() } }
            instrumentation.removeMonitor(monitor)
            server.close()
            try {
                repository.saveSettings(
                    AiCoachSettingsInput(
                        providerKind = AiCoachProviderKind.Disabled,
                        baseUrl = "",
                        modelName = "",
                        localAgentKind = LocalAgentKind.Custom,
                        apiKey = AiCoachApiKeyUpdate.Clear,
                    ),
                )
            } finally {
                database.close()
                secretStore.clearApiKey(DEFAULT_ACCOUNT_ID)
            }
        }
    }

    private fun localRepository(
        database: MusFitDatabase,
        secretStore: AiCoachSecretStore,
    ): LocalAiCoachRepository {
        var clock = 1_000L
        return LocalAiCoachRepository(
            aiCoachDao = database.aiCoachDao(),
            accountRepository = LocalAccountRepository(
                accountDao = database.accountDao(),
                clock = { clock += 1_000L; clock },
            ),
            secretStore = secretStore,
            clock = { clock += 1_000L; clock },
        )
    }

    private fun startSingleRequestListener(
        server: ServerSocket,
        requestCount: AtomicInteger,
        authorizationPresent: AtomicBoolean,
        failure: AtomicReference<Throwable?>,
    ): Thread = Thread {
        try {
            server.accept().use { socket ->
                requestCount.incrementAndGet()
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.US_ASCII))
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isEmpty()) break
                    if (line.startsWith("Authorization:", ignoreCase = true)) {
                        authorizationPresent.set(true)
                    }
                }
                val response = "{}"
                socket.getOutputStream().bufferedWriter(Charsets.US_ASCII).use { writer ->
                    writer.write("HTTP/1.1 200 OK\r\n")
                    writer.write("Content-Type: application/json\r\n")
                    writer.write("Content-Length: ${response.length}\r\n")
                    writer.write("Connection: close\r\n\r\n")
                    writer.write(response)
                    writer.flush()
                }
            }
        } catch (error: Throwable) {
            failure.set(error)
        }
    }.apply {
        name = "sec003-controlled-listener"
        isDaemon = true
        start()
    }

    private fun assertCiphertextOnly(
        context: Context,
        accountId: String,
        plaintext: String,
    ) {
        val encoded = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString("api_key_$accountId", null)
        assertNotNull(encoded)
        assertFalse(encoded!!.contains(plaintext))
    }

    private companion object {
        const val DEFAULT_ACCOUNT_ID = "local-default"
        const val PREFERENCES_NAME = "ai_coach_secrets"
    }
}
