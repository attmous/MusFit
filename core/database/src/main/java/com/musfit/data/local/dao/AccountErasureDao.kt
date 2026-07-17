package com.musfit.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.musfit.data.local.entity.ACTIVE_ACCOUNT_SESSION_KEY
import com.musfit.data.local.entity.AccountEntity
import com.musfit.data.local.entity.AccountSessionEntity

@Dao
interface AccountErasureDao {
    @Query("SELECT * FROM accounts ORDER BY updatedAtEpochMillis DESC")
    suspend fun getAccounts(): List<AccountEntity>

    @Query(
        "SELECT accounts.* FROM account_session " +
            "INNER JOIN accounts ON accounts.id = account_session.activeAccountId " +
            "WHERE account_session.`key` = :key LIMIT 1",
    )
    suspend fun getActiveAccount(key: String = ACTIVE_ACCOUNT_SESSION_KEY): AccountEntity?

    @Query("SELECT * FROM accounts ORDER BY updatedAtEpochMillis DESC LIMIT 1")
    suspend fun getMostRecentlyUpdatedAccount(): AccountEntity?

    @Upsert
    suspend fun upsertAccount(account: AccountEntity)

    @Upsert
    suspend fun upsertSession(session: AccountSessionEntity)

    @Query("DELETE FROM account_session WHERE `key` = :key")
    suspend fun deleteSession(key: String = ACTIVE_ACCOUNT_SESSION_KEY)

    @Query("DELETE FROM accounts WHERE id = :accountId")
    suspend fun deleteAccount(accountId: String): Int

    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts(): Int
}
