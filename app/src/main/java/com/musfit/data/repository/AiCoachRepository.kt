package com.musfit.data.repository

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.musfit.data.local.dao.AiCoachDao
import com.musfit.data.local.entity.AiCoachSettingsEntity
import com.musfit.data.remote.coach.AiCoachEndpointPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

enum class AiCoachProviderKind {
    Disabled,
    OpenAiCompatible,
    LocalAgent,
}

enum class LocalAgentKind {
    OpenClaw,
    HermesAgent,
    Custom,
}

data class AiCoachSettings(
    val providerKind: AiCoachProviderKind = AiCoachProviderKind.Disabled,
    val baseUrl: String = "",
    val modelName: String = "",
    val localAgentKind: LocalAgentKind = LocalAgentKind.Custom,
    val hasApiKey: Boolean = false,
    val updatedAtEpochMillis: Long = 0L,
)

data class AiCoachSettingsInput(
    val providerKind: AiCoachProviderKind,
    val baseUrl: String,
    val modelName: String,
    val localAgentKind: LocalAgentKind,
    val apiKey: AiCoachApiKeyUpdate,
)

sealed interface AiCoachApiKeyUpdate {
    object KeepExisting : AiCoachApiKeyUpdate
    data class Replace(val value: String) : AiCoachApiKeyUpdate
    object Clear : AiCoachApiKeyUpdate
}

data class AiCoachConnection(
    val providerKind: AiCoachProviderKind,
    val baseUrl: String,
    val modelName: String,
    val localAgentKind: LocalAgentKind,
    val apiKey: String?,
)

data class AiCoachDebugDefaults(
    val hermesBaseUrl: String = "",
    val hermesModelName: String = "",
)

interface AiCoachSecretStore {
    suspend fun saveApiKey(accountId: String, apiKey: String)
    suspend fun getApiKey(accountId: String): String?
    suspend fun clearApiKey(accountId: String)
}

interface AiCoachRepository {
    fun observeSettings(): Flow<AiCoachSettings>
    suspend fun saveSettings(input: AiCoachSettingsInput)
    suspend fun clearApiKey()
    suspend fun activeConnection(): AiCoachConnection?
}

@OptIn(ExperimentalCoroutinesApi::class)
class LocalAiCoachRepository @Inject constructor(
    private val aiCoachDao: AiCoachDao,
    private val accountRepository: AccountRepository,
    private val secretStore: AiCoachSecretStore,
    private val debugDefaults: AiCoachDebugDefaults,
) : AiCoachRepository {
    private var clock: () -> Long = { System.currentTimeMillis() }

    internal constructor(
        aiCoachDao: AiCoachDao,
        accountRepository: AccountRepository,
        secretStore: AiCoachSecretStore,
        debugDefaults: AiCoachDebugDefaults = AiCoachDebugDefaults(),
        clock: () -> Long,
    ) : this(aiCoachDao, accountRepository, secretStore, debugDefaults) {
        this.clock = clock
    }

    override fun observeSettings(): Flow<AiCoachSettings> =
        accountRepository.observeActiveAccount().flatMapLatest { account ->
            aiCoachDao.observeSettings(account.id).map { row ->
                if (row == null) {
                    debugDefaults.toHermesSettings() ?: AiCoachSettings()
                } else {
                    settingsWithRuntimeCredential(account.id, row)
                }
            }
        }

    override suspend fun saveSettings(input: AiCoachSettingsInput) {
        val normalized = input.normalized()
        val account = accountRepository.ensureActiveAccount()

        if (normalized.providerKind == AiCoachProviderKind.Disabled) {
            secretStore.clearApiKey(account.id)
            aiCoachDao.upsertSettings(normalized.toEntity(account.id, apiKeyStored = false, now = clock()))
            return
        }

        val existing = aiCoachDao.getSettings(account.id)
        val existingApiKeyStored = existing?.apiKeyStored ?: false
        val existingRuntimeApiKey = if (input.apiKey == AiCoachApiKeyUpdate.KeepExisting) {
            readUsableRuntimeCredential(account.id, claimedPresent = existingApiKeyStored)
        } else {
            null
        }
        val replacementApiKey = (input.apiKey as? AiCoachApiKeyUpdate.Replace)?.value?.trim()
        val apiKeyStored = when (input.apiKey) {
            AiCoachApiKeyUpdate.KeepExisting -> existingRuntimeApiKey != null
            AiCoachApiKeyUpdate.Clear -> false
            is AiCoachApiKeyUpdate.Replace -> replacementApiKey?.isNotBlank() == true
        }
        if (normalized.requiresApiServerKey() && !apiKeyStored) {
            if (existing != null && existingApiKeyStored && existingRuntimeApiKey == null) {
                reconcileRuntimeCredentialFlag(existing, isPresent = false)
            }
            throw IllegalArgumentException("Hermes agent requires the API_SERVER_KEY bearer token.")
        }

        when (input.apiKey) {
            AiCoachApiKeyUpdate.KeepExisting -> Unit
            AiCoachApiKeyUpdate.Clear -> {
                secretStore.clearApiKey(account.id)
            }
            is AiCoachApiKeyUpdate.Replace -> {
                val trimmed = replacementApiKey.orEmpty()
                if (trimmed.isBlank()) {
                    secretStore.clearApiKey(account.id)
                } else {
                    secretStore.saveApiKey(account.id, trimmed)
                }
            }
        }
        aiCoachDao.upsertSettings(normalized.toEntity(account.id, apiKeyStored, clock()))
    }

    override suspend fun clearApiKey() {
        val account = accountRepository.ensureActiveAccount()
        secretStore.clearApiKey(account.id)
        aiCoachDao.getSettings(account.id)?.let { row ->
            aiCoachDao.upsertSettings(row.copy(apiKeyStored = false, updatedAtEpochMillis = clock()))
        }
    }

    override suspend fun activeConnection(): AiCoachConnection? {
        val account = accountRepository.ensureActiveAccount()
        val row = aiCoachDao.getSettings(account.id) ?: return null
        val settings = row.toSettings()
        if (settings.providerKind == AiCoachProviderKind.Disabled) {
            secretStore.clearApiKey(account.id)
            if (row.apiKeyStored) {
                reconcileRuntimeCredentialFlag(row, isPresent = false)
            }
            return null
        }
        val validatedEndpoint = AiCoachEndpointPolicy.requireAllowed(settings.baseUrl)
        val runtimeApiKey = readUsableRuntimeCredential(account.id, claimedPresent = row.apiKeyStored)
        if (row.apiKeyStored != (runtimeApiKey != null)) {
            reconcileRuntimeCredentialFlag(row, isPresent = runtimeApiKey != null)
        }
        if (settings.requiresApiServerKey() && runtimeApiKey == null) return null
        return AiCoachConnection(
            providerKind = settings.providerKind,
            baseUrl = validatedEndpoint.normalizedBaseUrl,
            modelName = settings.modelName,
            localAgentKind = settings.localAgentKind,
            apiKey = runtimeApiKey,
        )
    }

    private suspend fun settingsWithRuntimeCredential(
        accountId: String,
        row: AiCoachSettingsEntity,
    ): AiCoachSettings {
        val settings = row.toSettings()
        if (settings.providerKind == AiCoachProviderKind.Disabled) {
            secretStore.clearApiKey(accountId)
            return if (row.apiKeyStored) {
                reconcileRuntimeCredentialFlag(row, isPresent = false).toSettings()
            } else {
                settings.copy(hasApiKey = false)
            }
        }

        // Preserve the SEC-002 ordering: do not read a credential until the
        // persisted endpoint passes the active variant's URL policy.
        if (AiCoachEndpointPolicy.normalizedBaseUrlOrNull(settings.baseUrl) == null) {
            return settings.copy(hasApiKey = false)
        }
        val runtimeApiKey = readUsableRuntimeCredential(accountId, claimedPresent = row.apiKeyStored)
        val isPresent = runtimeApiKey != null
        return if (row.apiKeyStored != isPresent) {
            reconcileRuntimeCredentialFlag(row, isPresent).toSettings()
        } else {
            settings.copy(hasApiKey = isPresent)
        }
    }

    private suspend fun readUsableRuntimeCredential(
        accountId: String,
        claimedPresent: Boolean,
    ): String? {
        val stored = secretStore.getApiKey(accountId)
        val usable = stored?.takeUnless(String::isBlank)
        if ((claimedPresent && usable == null) || (stored != null && usable == null)) {
            secretStore.clearApiKey(accountId)
        }
        return usable
    }

    private suspend fun reconcileRuntimeCredentialFlag(
        row: AiCoachSettingsEntity,
        isPresent: Boolean,
    ): AiCoachSettingsEntity {
        val reconciled = row.copy(
            apiKeyStored = isPresent,
            updatedAtEpochMillis = clock(),
        )
        aiCoachDao.upsertSettings(reconciled)
        return reconciled
    }
}

class AndroidKeyStoreAiCoachSecretStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : AiCoachSecretStore {
    private val preferences by lazy {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    override suspend fun saveApiKey(accountId: String, apiKey: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8))
        preferences.edit()
            .putString(accountId.preferenceKey(), "${cipher.iv.base64()}:${encrypted.base64()}")
            .apply()
    }

    override suspend fun getApiKey(accountId: String): String? {
        val preferenceKey = accountId.preferenceKey()
        val encoded = preferences.getString(preferenceKey, null) ?: return null
        val parts = encoded.split(":", limit = 2)
        if (parts.size != 2) {
            preferences.edit().remove(preferenceKey).apply()
            return null
        }
        return try {
            val iv = parts[0].decodeBase64()
            val ciphertext = parts[1].decodeBase64()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8).takeUnless(String::isBlank)
                ?: run {
                    preferences.edit().remove(preferenceKey).apply()
                    null
                }
        } catch (_: GeneralSecurityException) {
            preferences.edit().remove(preferenceKey).apply()
            null
        } catch (_: IllegalArgumentException) {
            preferences.edit().remove(preferenceKey).apply()
            null
        }
    }

    override suspend fun clearApiKey(accountId: String) {
        preferences.edit().remove(accountId.preferenceKey()).apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private fun String.preferenceKey(): String = "api_key_$this"

    private fun ByteArray.base64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    private fun String.decodeBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "musfit_ai_coach_api_key"
        const val PREFERENCES_NAME = "ai_coach_secrets"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
    }
}

private data class NormalizedAiCoachSettings(
    val providerKind: AiCoachProviderKind,
    val baseUrl: String,
    val modelName: String,
    val localAgentKind: LocalAgentKind,
)

private fun AiCoachSettingsInput.normalized(): NormalizedAiCoachSettings =
    when (providerKind) {
        AiCoachProviderKind.Disabled -> NormalizedAiCoachSettings(
            providerKind = AiCoachProviderKind.Disabled,
            baseUrl = "",
            modelName = "",
            localAgentKind = LocalAgentKind.Custom,
        )
        AiCoachProviderKind.OpenAiCompatible -> NormalizedAiCoachSettings(
            providerKind = AiCoachProviderKind.OpenAiCompatible,
            baseUrl = AiCoachEndpointPolicy.requireAllowed(baseUrl).normalizedBaseUrl,
            modelName = modelName.trim().takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("Model name is required for API-compatible providers."),
            localAgentKind = LocalAgentKind.Custom,
        )
        AiCoachProviderKind.LocalAgent -> NormalizedAiCoachSettings(
            providerKind = AiCoachProviderKind.LocalAgent,
            baseUrl = AiCoachEndpointPolicy.requireAllowed(baseUrl).normalizedBaseUrl,
            modelName = modelName.trim(),
            localAgentKind = localAgentKind,
        )
    }

private fun NormalizedAiCoachSettings.toEntity(
    accountId: String,
    apiKeyStored: Boolean,
    now: Long,
): AiCoachSettingsEntity =
    AiCoachSettingsEntity(
        accountId = accountId,
        providerKind = providerKind.name,
        baseUrl = baseUrl,
        modelName = modelName,
        localAgentKind = localAgentKind.name,
        apiKeyStored = apiKeyStored,
        updatedAtEpochMillis = now,
    )

private fun NormalizedAiCoachSettings.requiresApiServerKey(): Boolean =
    providerKind == AiCoachProviderKind.LocalAgent && localAgentKind == LocalAgentKind.HermesAgent

private fun AiCoachSettings.requiresApiServerKey(): Boolean =
    providerKind == AiCoachProviderKind.LocalAgent && localAgentKind == LocalAgentKind.HermesAgent

private fun AiCoachDebugDefaults.toHermesSettings(): AiCoachSettings? {
    val normalizedBaseUrl = AiCoachEndpointPolicy.normalizedBaseUrlOrNull(hermesBaseUrl) ?: return null
    val model = hermesModelName.trim().takeIf { it.isNotBlank() } ?: return null
    return AiCoachSettings(
        providerKind = AiCoachProviderKind.LocalAgent,
        baseUrl = normalizedBaseUrl,
        modelName = model,
        localAgentKind = LocalAgentKind.HermesAgent,
        hasApiKey = false,
    )
}

private fun AiCoachSettingsEntity.toSettings(): AiCoachSettings =
    AiCoachSettings(
        providerKind = runCatching { AiCoachProviderKind.valueOf(providerKind) }
            .getOrDefault(AiCoachProviderKind.Disabled),
        baseUrl = baseUrl,
        modelName = modelName,
        localAgentKind = runCatching { LocalAgentKind.valueOf(localAgentKind) }
            .getOrDefault(LocalAgentKind.Custom),
        hasApiKey = apiKeyStored,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )
