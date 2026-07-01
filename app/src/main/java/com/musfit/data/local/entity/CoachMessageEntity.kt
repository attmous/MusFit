package com.musfit.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One persisted coach feed message. Deduped per (dayEpochDay, ruleKey, source);
 * dismissed rows are soft-delete tombstones the sync upsert respects.
 */
@Entity(
    tableName = "coach_messages",
    indices = [
        Index(value = ["dayEpochDay", "ruleKey", "source"], unique = true),
        Index(value = ["dayEpochDay"]),
    ],
)
data class CoachMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dayEpochDay: Long,
    val ruleKey: String,
    val category: String,
    val title: String,
    val body: String,
    val actionType: String?,
    val actionData: String?,
    val firstSeenAtEpochMillis: Long,
    val isRead: Boolean,
    val isDismissed: Boolean,
    val source: String,
)
