package com.musfit.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** One pinned Today-carousel metric; position 0 is the hero slot. */
@Entity(
    tableName = "dashboard_pins",
    primaryKeys = ["accountId", "metricId"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["accountId", "position"])],
)
data class DashboardPinEntity(
    val accountId: String,
    val metricId: String,
    val position: Int,
)
