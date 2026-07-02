package com.musfit.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.musfit.data.local.entity.ACTIVE_ACCOUNT_SESSION_KEY
import com.musfit.data.local.entity.AccountEntity
import com.musfit.data.local.entity.AccountSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY updatedAtEpochMillis DESC")
    fun observeAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccount(id: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE remoteUserId = :remoteUserId LIMIT 1")
    suspend fun getAccountByRemoteUserId(remoteUserId: String): AccountEntity?

    @Query("SELECT * FROM accounts ORDER BY updatedAtEpochMillis DESC LIMIT 1")
    suspend fun getMostRecentlyUpdatedAccount(): AccountEntity?

    @Query(
        """
        SELECT accounts.* FROM account_session
        INNER JOIN accounts ON accounts.id = account_session.activeAccountId
        WHERE account_session.`key` = :key
        LIMIT 1
        """,
    )
    fun observeActiveAccount(key: String = ACTIVE_ACCOUNT_SESSION_KEY): Flow<AccountEntity?>

    @Query(
        """
        SELECT accounts.* FROM account_session
        INNER JOIN accounts ON accounts.id = account_session.activeAccountId
        WHERE account_session.`key` = :key
        LIMIT 1
        """,
    )
    suspend fun getActiveAccount(key: String = ACTIVE_ACCOUNT_SESSION_KEY): AccountEntity?

    @Query("SELECT * FROM account_session WHERE `key` = :key LIMIT 1")
    suspend fun getSession(key: String = ACTIVE_ACCOUNT_SESSION_KEY): AccountSessionEntity?

    @Upsert
    suspend fun upsertAccount(account: AccountEntity)

    @Upsert
    suspend fun upsertSession(session: AccountSessionEntity)
}
