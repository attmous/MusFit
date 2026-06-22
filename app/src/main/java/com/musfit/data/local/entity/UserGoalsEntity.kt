package com.musfit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single-row (`id = "default"`) store for cross-cutting Today goals not held in `food_goals`. */
@Entity(tableName = "user_goals")
data class UserGoalsEntity(
    @PrimaryKey val id: String,
    val stepGoal: Long,
    val weeklySessionTarget: Int,
    val targetWeightKg: Double,
    val updatedAtEpochMillis: Long,
)
