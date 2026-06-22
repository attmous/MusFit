package com.musfit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val sex: String?,
    val birthDateEpochDay: Long?,
    val heightCm: Double?,
    val activityLevel: String,
    val goalType: String,
    val goalPaceKgPerWeek: Double,
    val goalWeightKg: Double?,
    val updatedAtEpochMillis: Long,
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: String,
    val unitSystem: String,
    val themeMode: String,
    val updatedAtEpochMillis: Long,
)
