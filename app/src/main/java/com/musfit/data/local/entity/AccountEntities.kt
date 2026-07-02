package com.musfit.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

const val ACTIVE_ACCOUNT_SESSION_KEY = "active"
const val LOCAL_DEFAULT_ACCOUNT_ID = "local-default"

@Entity(
    tableName = "accounts",
    indices = [
        Index("email"),
        Index(value = ["remoteUserId"], unique = true),
        Index("authProvider"),
    ],
)
data class AccountEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val email: String?,
    val remoteUserId: String?,
    val authProvider: String,
    val avatarUrl: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "account_session",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["activeAccountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("activeAccountId")],
)
data class AccountSessionEntity(
    @PrimaryKey val key: String,
    val activeAccountId: String,
    val updatedAtEpochMillis: Long,
)
