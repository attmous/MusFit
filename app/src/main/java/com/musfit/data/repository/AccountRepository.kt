package com.musfit.data.repository

import com.musfit.data.local.dao.AccountDao
import com.musfit.data.local.entity.ACTIVE_ACCOUNT_SESSION_KEY
import com.musfit.data.local.entity.AccountEntity
import com.musfit.data.local.entity.AccountSessionEntity
import com.musfit.data.local.entity.LOCAL_DEFAULT_ACCOUNT_ID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

data class Account(
    val id: String,
    val displayName: String,
    val email: String?,
    val remoteUserId: String?,
    val authProvider: AccountAuthProvider = AccountAuthProvider.Local,
    val avatarUrl: String? = null,
)

enum class AccountAuthProvider(
    val storageValue: String,
    val displayName: String,
) {
    Local("local", "Local account"),
    Google("google", "Google"),
    GitHub("github", "GitHub"),
    ;

    companion object {
        fun fromStorage(value: String?): AccountAuthProvider =
            entries.firstOrNull { it.storageValue == value } ?: Local
    }
}

data class ExternalAccountProfile(
    val provider: AccountAuthProvider,
    val providerUserId: String,
    val displayName: String,
    val email: String?,
    val avatarUrl: String?,
)

val DEFAULT_LOCAL_ACCOUNT = Account(
    id = LOCAL_DEFAULT_ACCOUNT_ID,
    displayName = "You",
    email = null,
    remoteUserId = null,
    authProvider = AccountAuthProvider.Local,
    avatarUrl = null,
)

interface AccountRepository {
    fun observeActiveAccount(): Flow<Account>
    fun observeAccounts(): Flow<List<Account>>
    suspend fun ensureActiveAccount(): Account
    suspend fun createAccount(displayName: String, email: String? = null): String
    suspend fun updateActiveAccount(displayName: String, email: String?)
    suspend fun switchAccount(accountId: String)
    suspend fun signInWithProvider(profile: ExternalAccountProfile): Account
}

class LocalAccountRepository @Inject constructor(
    private val accountDao: AccountDao,
) : AccountRepository {
    private var clock: () -> Long = { System.currentTimeMillis() }

    internal constructor(accountDao: AccountDao, clock: () -> Long) : this(accountDao) {
        this.clock = clock
    }

    override fun observeActiveAccount(): Flow<Account> =
        accountDao.observeActiveAccount().map { entity -> entity?.toAccount() ?: DEFAULT_LOCAL_ACCOUNT }

    override fun observeAccounts(): Flow<List<Account>> =
        accountDao.observeAccounts().map { rows -> rows.map { it.toAccount() } }

    override suspend fun ensureActiveAccount(): Account {
        accountDao.getActiveAccount()?.let { return it.toAccount() }

        val existing = accountDao.getMostRecentlyUpdatedAccount()
        if (existing != null) {
            accountDao.upsertSession(
                AccountSessionEntity(
                    key = ACTIVE_ACCOUNT_SESSION_KEY,
                    activeAccountId = existing.id,
                    updatedAtEpochMillis = clock(),
                ),
            )
            return existing.toAccount()
        }

        val now = clock()
        val default = AccountEntity(
            id = LOCAL_DEFAULT_ACCOUNT_ID,
            displayName = "You",
            email = null,
            remoteUserId = null,
            authProvider = AccountAuthProvider.Local.storageValue,
            avatarUrl = null,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        accountDao.upsertAccount(default)
        accountDao.upsertSession(
            AccountSessionEntity(
                key = ACTIVE_ACCOUNT_SESSION_KEY,
                activeAccountId = default.id,
                updatedAtEpochMillis = now,
            ),
        )
        return default.toAccount()
    }

    override suspend fun createAccount(displayName: String, email: String?): String {
        val normalizedName = displayName.normalizedDisplayName()
        val now = clock()
        val id = UUID.randomUUID().toString()
        accountDao.upsertAccount(
            AccountEntity(
                id = id,
                displayName = normalizedName,
                email = email.normalizedEmail(),
                remoteUserId = null,
                authProvider = AccountAuthProvider.Local.storageValue,
                avatarUrl = null,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
            ),
        )
        return id
    }

    override suspend fun updateActiveAccount(displayName: String, email: String?) {
        val active = ensureActiveAccount()
        val existing = accountDao.getAccount(active.id) ?: error("Active account is missing.")
        accountDao.upsertAccount(
            existing.copy(
                displayName = displayName.normalizedDisplayName(),
                email = email.normalizedEmail(),
                updatedAtEpochMillis = clock(),
            ),
        )
    }

    override suspend fun switchAccount(accountId: String) {
        val account = accountDao.getAccount(accountId)
            ?: throw IllegalArgumentException("Account not found.")
        accountDao.upsertSession(
            AccountSessionEntity(
                key = ACTIVE_ACCOUNT_SESSION_KEY,
                activeAccountId = account.id,
                updatedAtEpochMillis = clock(),
            ),
        )
    }

    override suspend fun signInWithProvider(profile: ExternalAccountProfile): Account {
        val remoteUserId = profile.scopedRemoteUserId()
        val now = clock()
        val existingLinked = accountDao.getAccountByRemoteUserId(remoteUserId)
        val linkedAccount = if (existingLinked != null) {
            existingLinked.copy(
                displayName = profile.displayName.normalizedDisplayName(),
                email = profile.email.normalizedEmail(),
                authProvider = profile.provider.storageValue,
                avatarUrl = profile.avatarUrl.normalizedOptionalText(),
                updatedAtEpochMillis = now,
            )
        } else {
            val active = ensureActiveAccount()
            val activeEntity = accountDao.getAccount(active.id) ?: error("Active account is missing.")
            val canClaimActiveAccount = activeEntity.remoteUserId == null ||
                activeEntity.remoteUserId == remoteUserId
            if (canClaimActiveAccount) {
                activeEntity.copy(
                    displayName = profile.displayName.normalizedDisplayName(),
                    email = profile.email.normalizedEmail(),
                    remoteUserId = remoteUserId,
                    authProvider = profile.provider.storageValue,
                    avatarUrl = profile.avatarUrl.normalizedOptionalText(),
                    updatedAtEpochMillis = now,
                )
            } else {
                AccountEntity(
                    id = UUID.randomUUID().toString(),
                    displayName = profile.displayName.normalizedDisplayName(),
                    email = profile.email.normalizedEmail(),
                    remoteUserId = remoteUserId,
                    authProvider = profile.provider.storageValue,
                    avatarUrl = profile.avatarUrl.normalizedOptionalText(),
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                )
            }
        }
        accountDao.upsertAccount(linkedAccount)
        accountDao.upsertSession(
            AccountSessionEntity(
                key = ACTIVE_ACCOUNT_SESSION_KEY,
                activeAccountId = linkedAccount.id,
                updatedAtEpochMillis = now,
            ),
        )
        return linkedAccount.toAccount()
    }
}

private fun AccountEntity.toAccount(): Account =
    Account(
        id = id,
        displayName = displayName,
        email = email,
        remoteUserId = remoteUserId,
        authProvider = AccountAuthProvider.fromStorage(authProvider),
        avatarUrl = avatarUrl,
    )

private fun String.normalizedDisplayName(): String =
    trim().takeIf { it.isNotBlank() } ?: throw IllegalArgumentException("Account name is required.")

private fun String?.normalizedEmail(): String? =
    this?.trim()?.takeIf { it.isNotBlank() }

private fun String?.normalizedOptionalText(): String? =
    this?.trim()?.takeIf { it.isNotBlank() }

private fun ExternalAccountProfile.scopedRemoteUserId(): String {
    val normalizedProviderUserId = this.providerUserId.normalizedOptionalText()
        ?: throw IllegalArgumentException("Provider user id is required.")
    return "${provider.storageValue}:$normalizedProviderUserId"
}
