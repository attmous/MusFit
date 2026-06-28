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
    val heaviestWeightKg: Double = 0.0,
)

data class ExerciseProgressHistoryEntry(
    val dateEpochDay: Long,
    val completedSetCount: Int,
    val volumeKg: Double,
    val bestEstimatedOneRepMaxKg: Double,
    val bestSetLabel: String,
)

data class ExerciseBestSetSummary(
    val dateEpochDay: Long,
    val reps: Int,
    val weightKg: Double,
    val estimatedOneRepMaxKg: Double,
)

data class ExercisePrTimelineEntry(
    val dateEpochDay: Long,
    val reps: Int,
    val weightKg: Double,
    val estimatedOneRepMaxKg: Double,
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
    val history: List<ExerciseProgressHistoryEntry> = emptyList(),
    val bestSets: List<ExerciseBestSetSummary> = emptyList(),
    val prTimeline: List<ExercisePrTimelineEntry> = emptyList(),
)
