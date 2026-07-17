package com.musfit.domain.training

import com.musfit.domain.model.ExerciseBestSetSummary
import com.musfit.domain.model.ExercisePrTimelineEntry
import com.musfit.domain.model.ExerciseProgress
import com.musfit.domain.model.ExerciseProgressHistoryEntry
import com.musfit.domain.model.ExerciseProgressSetInput
import com.musfit.domain.model.PersonalRecords
import com.musfit.domain.model.TrainingTrendPoint
import com.musfit.domain.model.WorkoutSetInput
import java.util.Locale

object WorkoutCalculator {
    fun totalVolume(sets: List<WorkoutSetInput>): Double = sets.filter { it.completed && it.reps > 0 && it.weightKg > 0.0 }
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
                    heaviestWeightKg = daySets.maxOfOrNull { it.weightKg } ?: 0.0,
                )
            }
        val bestSetByDay = completed.bestSetByDay()
        val history = completed
            .groupBy { it.dateEpochDay }
            .toSortedMap()
            .map { (dateEpochDay, daySets) ->
                val bestSet = daySets.bestEstimatedOneRepMaxSet()
                ExerciseProgressHistoryEntry(
                    dateEpochDay = dateEpochDay,
                    completedSetCount = daySets.size,
                    volumeKg = daySets.sumOf { it.reps * it.weightKg },
                    bestEstimatedOneRepMaxKg = estimatedOneRepMax(bestSet.weightKg, bestSet.reps).round2(),
                    bestSetLabel = "${bestSet.weightKg.formatCompactKg()} kg x ${bestSet.reps}",
                )
            }
        val prTimeline = mutableListOf<ExercisePrTimelineEntry>()
        var runningBest = 0.0
        completed.sortedWith(compareBy<ExerciseProgressSetInput> { it.dateEpochDay }.thenBy { it.weightKg }).forEach { set ->
            val estimated = estimatedOneRepMax(set.weightKg, set.reps)
            if (estimated > runningBest) {
                runningBest = estimated
                prTimeline += ExercisePrTimelineEntry(
                    dateEpochDay = set.dateEpochDay,
                    reps = set.reps,
                    weightKg = set.weightKg,
                    estimatedOneRepMaxKg = estimated.round2(),
                )
            }
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
            history = history,
            bestSets = bestSetByDay.sortedByDescending { it.estimatedOneRepMaxKg },
            prTimeline = prTimeline,
        )
    }

    private fun List<ExerciseProgressSetInput>.bestSetByDay(): List<ExerciseBestSetSummary> = groupBy { it.dateEpochDay }
        .toSortedMap()
        .map { (dateEpochDay, daySets) ->
            val bestSet = daySets.bestEstimatedOneRepMaxSet()
            ExerciseBestSetSummary(
                dateEpochDay = dateEpochDay,
                reps = bestSet.reps,
                weightKg = bestSet.weightKg,
                estimatedOneRepMaxKg = estimatedOneRepMax(bestSet.weightKg, bestSet.reps).round2(),
            )
        }

    private fun List<ExerciseProgressSetInput>.bestEstimatedOneRepMaxSet(): ExerciseProgressSetInput = maxBy { estimatedOneRepMax(it.weightKg, it.reps) }

    private fun Double.round2(): Double = kotlin.math.round(this * 100.0) / 100.0

    private fun Double.formatCompactKg(): String = if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", this)
    }
}
