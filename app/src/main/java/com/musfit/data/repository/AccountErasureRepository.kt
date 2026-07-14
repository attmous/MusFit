package com.musfit.data.repository

import androidx.room.withTransaction
import com.musfit.data.local.MusFitDatabase
import com.musfit.data.local.dao.AccountErasureDao
import com.musfit.data.local.entity.ACTIVE_ACCOUNT_SESSION_KEY
import com.musfit.data.local.entity.AccountEntity
import com.musfit.data.local.entity.AccountSessionEntity
import com.musfit.data.local.entity.LOCAL_DEFAULT_ACCOUNT_ID
import com.musfit.integrations.healthconnect.HealthConnectDeleteResult
import javax.inject.Inject
import javax.inject.Singleton

data class AccountErasureRequest(
    val scope: AccountErasureScope,
    val deleteAuthoredHealthRecords: Boolean,
)

enum class AccountErasureScope { ActiveAccount, AllAccounts }

sealed interface AccountErasureResult {
    data class Complete(val fallbackAccountId: String) : AccountErasureResult
    data class HealthCleanupFailed(val message: String) : AccountErasureResult
}

interface AccountErasureRepository {
    suspend fun erase(request: AccountErasureRequest): AccountErasureResult
}

@Singleton
class LocalAccountErasureRepository @Inject constructor(
    private val database: MusFitDatabase,
    private val accountErasureDao: AccountErasureDao,
    private val healthRepository: HealthRepository,
    private val secretStore: AiCoachSecretStore,
) : AccountErasureRepository {
    private var clock: () -> Long = { System.currentTimeMillis() }

    internal constructor(
        database: MusFitDatabase,
        accountErasureDao: AccountErasureDao,
        healthRepository: HealthRepository,
        secretStore: AiCoachSecretStore,
        clock: () -> Long,
    ) : this(database, accountErasureDao, healthRepository, secretStore) {
        this.clock = clock
    }

    override suspend fun erase(request: AccountErasureRequest): AccountErasureResult {
        val accounts = accountErasureDao.getAccounts()
        val activeAccount = accountErasureDao.getActiveAccount() ?: accounts.firstOrNull()
        return if (activeAccount == null) {
            eraseEmptyDatabase(request.scope)
        } else {
            val targets = when (request.scope) {
                AccountErasureScope.ActiveAccount -> listOf(activeAccount)
                AccountErasureScope.AllAccounts -> accounts
            }
            cleanupAuthoredHealth(request, targets)
                ?: eraseAfterExternalCleanup(request.scope, activeAccount, targets)
        }
    }

    private suspend fun eraseEmptyDatabase(scope: AccountErasureScope): AccountErasureResult {
        if (scope == AccountErasureScope.AllAccounts) secretStore.clearAll(emptyList())
        return AccountErasureResult.Complete(ensureFallbackAccount())
    }

    private suspend fun cleanupAuthoredHealth(
        request: AccountErasureRequest,
        targets: List<AccountEntity>,
    ): AccountErasureResult.HealthCleanupFailed? {
        if (!request.deleteAuthoredHealthRecords) return null
        var failure: AccountErasureResult.HealthCleanupFailed? = null
        for (account in targets) {
            failure = healthDeleteFailure(healthRepository.deleteAuthoredRecords(account.id))
            if (failure != null) break
        }
        return failure
    }

    private fun healthDeleteFailure(result: HealthConnectDeleteResult): AccountErasureResult.HealthCleanupFailed? = when (result) {
        is HealthConnectDeleteResult.Complete -> null

        is HealthConnectDeleteResult.Partial -> AccountErasureResult.HealthCleanupFailed(
            "Health Connect deleted some MusFit records, but ${result.failures.size} failed. Retry before erasing local data.",
        )

        is HealthConnectDeleteResult.Unavailable -> AccountErasureResult.HealthCleanupFailed(result.message)

        is HealthConnectDeleteResult.Failure -> AccountErasureResult.HealthCleanupFailed(result.message)
    }

    private suspend fun eraseAfterExternalCleanup(
        scope: AccountErasureScope,
        activeAccount: AccountEntity,
        targets: List<AccountEntity>,
    ): AccountErasureResult {
        if (scope == AccountErasureScope.AllAccounts) {
            secretStore.clearAll(targets.map { it.id })
        } else {
            targets.forEach { secretStore.clearApiKey(it.id) }
        }
        val fallbackId = database.withTransaction {
            when (scope) {
                AccountErasureScope.ActiveAccount -> eraseActiveAccount(activeAccount.id)
                AccountErasureScope.AllAccounts -> eraseAllAccounts()
            }
        }
        return AccountErasureResult.Complete(fallbackId)
    }

    private suspend fun eraseActiveAccount(accountId: String): String {
        accountErasureDao.deleteSession()
        check(accountErasureDao.deleteAccount(accountId) == 1) { "Account no longer exists." }
        val fallback = accountErasureDao.getMostRecentlyUpdatedAccount() ?: newFallbackAccount()
        accountErasureDao.upsertAccount(fallback)
        accountErasureDao.upsertSession(fallback.toSession(clock()))
        return fallback.id
    }

    private suspend fun eraseAllAccounts(): String {
        accountErasureDao.deleteSession()
        accountErasureDao.deleteAllAccounts()
        val fallback = newFallbackAccount()
        accountErasureDao.upsertAccount(fallback)
        accountErasureDao.upsertSession(fallback.toSession(clock()))
        return fallback.id
    }

    private suspend fun ensureFallbackAccount(): String = database.withTransaction {
        val existing = accountErasureDao.getMostRecentlyUpdatedAccount()
        if (existing != null) {
            accountErasureDao.upsertSession(existing.toSession(clock()))
            existing.id
        } else {
            val fallback = newFallbackAccount()
            accountErasureDao.upsertAccount(fallback)
            accountErasureDao.upsertSession(fallback.toSession(clock()))
            fallback.id
        }
    }

    private fun newFallbackAccount(): AccountEntity {
        val now = clock()
        return AccountEntity(
            id = LOCAL_DEFAULT_ACCOUNT_ID,
            displayName = "You",
            email = null,
            remoteUserId = null,
            authProvider = AccountAuthProvider.Local.storageValue,
            avatarUrl = null,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
    }

    private fun AccountEntity.toSession(now: Long) = AccountSessionEntity(
        key = ACTIVE_ACCOUNT_SESSION_KEY,
        activeAccountId = id,
        updatedAtEpochMillis = now,
    )
}
