package com.musfit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One pinned Today-carousel metric; position 0 is the hero slot. */
@Entity(tableName = "dashboard_pins")
data class DashboardPinEntity(
    @PrimaryKey val metricId: String,
    val position: Int,
)
