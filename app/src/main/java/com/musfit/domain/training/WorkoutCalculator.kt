package com.musfit.domain.training

import com.musfit.domain.model.PersonalRecords
import com.musfit.domain.model.ExerciseProgress
import com.musfit.domain.model.ExerciseProgressSetInput
import com.musfit.domain.model.TrainingTrendPoint
import com.musfit.domain.model.WorkoutSetInput

object WorkoutCalculator {
    fun totalVolume(sets: List<WorkoutSetInput>): Double =
        sets.filter { it.completed && it.reps > 0 && it.weightKg > 0.0 }
            .sumOf { it.reps * it.weightKg }

    fun estimatedOneRepMax(weightKg: Double, reps: Int): Double {
        if (weightKg <= 0.0 || reps <= 0) return 0.0
        return weightKg * (1.0 + reps / 30.0)
    }

    fun personalRecords(sets: List<WorkoutSetInput>): PersonalRecords {
        val completed = sets.filter { it.completed }
        return PersonalRecords(
            heaviestWeightKg = completed.maxOfOrNull { it.weightKg } ?: 0.0,
            maxReps = completed.maxOfOrNull { it.reps } ?: 0,
            totalVolumeKg = totalVolume(completed),
            bestEstimatedOneRepMaxKg = completed.maxOfOrNull {
                estimatedOneRepMax(weightKg = it.weightKg, reps = it.reps)
            } ?: 0.0,
        )
    }

    fun exerciseProgress(
        exerciseId: String,
        exerciseName: String,
        equipment: String?,
        targetMuscles: String,
        sets: List<ExerciseProgressSetInput>,
    ): ExerciseProgress {
        val completed = sets.filter { it.completed && it.reps > 0 && it.weightKg > 0.0 }
        val trend = completed
            .groupBy { it.dateEpochDay }
            .toSortedMap()
            .map { (dateEpochDay, daySets) ->
                TrainingTrendPoint(
                    dateEpochDay = dateEpochDay,
                    volumeKg = daySets.sumOf { it.reps * it.weightKg },
                    bestEstimatedOneRepMaxKg = daySets.maxOfOrNull {
                        estimatedOneRepMax(it.weightKg, it.reps)
                    } ?: 0.0,
                )
            }
        return ExerciseProgress(
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            equipment = equipment,
            targetMuscles = targetMuscles,
            heaviestWeightKg = completed.maxOfOrNull { it.weightKg } ?: 0.0,
            maxReps = completed.maxOfOrNull { it.reps } ?: 0,
            bestEstimatedOneRepMaxKg = completed.maxOfOrNull {
                estimatedOneRepMax(it.weightKg, it.reps)
            } ?: 0.0,
            bestWorkoutVolumeKg = trend.maxOfOrNull { it.volumeKg } ?: 0.0,
            trend = trend,
        )
    }
}
