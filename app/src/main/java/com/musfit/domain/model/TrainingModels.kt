package com.musfit.domain.model

data class WorkoutSetInput(
    val exerciseId: String,
    val reps: Int,
    val weightKg: Double,
    val completed: Boolean,
)

data class PersonalRecords(
    val heaviestWeightKg: Double,
    val maxReps: Int,
    val totalVolumeKg: Double,
    val bestEstimatedOneRepMaxKg: Double,
)
