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

data class ExerciseProgressSetInput(
    val dateEpochDay: Long,
    val reps: Int,
    val weightKg: Double,
    val completed: Boolean,
)

data class TrainingTrendPoint(
    val dateEpochDay: Long,
    val volumeKg: Double,
    val bestEstimatedOneRepMaxKg: Double,
)

data class ExerciseProgress(
    val exerciseId: String,
    val exerciseName: String,
    val equipment: String?,
    val targetMuscles: String,
    val heaviestWeightKg: Double,
    val maxReps: Int,
    val bestEstimatedOneRepMaxKg: Double,
    val bestWorkoutVolumeKg: Double,
    val trend: List<TrainingTrendPoint>,
)
