package com.musfit.domain.training

import com.musfit.domain.model.ExerciseProgressSetInput
import com.musfit.domain.model.WorkoutSetInput
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutCalculatorTest {
    @Test
    fun totalVolume_sumsCompletedWeightedSets() {
        val sets = listOf(
            WorkoutSetInput(exerciseId = "bench", reps = 5, weightKg = 100.0, completed = true),
            WorkoutSetInput(exerciseId = "bench", reps = 5, weightKg = 102.5, completed = true),
            WorkoutSetInput(exerciseId = "bench", reps = 5, weightKg = 105.0, completed = false),
        )

        assertEquals(1012.5, WorkoutCalculator.totalVolume(sets), 0.01)
    }

    @Test
    fun estimatedOneRepMax_usesEpleyFormula() {
        assertEquals(120.0, WorkoutCalculator.estimatedOneRepMax(weightKg = 100.0, reps = 6), 0.01)
    }

    @Test
    fun personalRecords_returnsBestWeightRepsAndVolume() {
        val sets = listOf(
            WorkoutSetInput("squat", reps = 8, weightKg = 120.0, completed = true),
            WorkoutSetInput("squat", reps = 3, weightKg = 150.0, completed = true),
            WorkoutSetInput("deadlift", reps = 5, weightKg = 180.0, completed = true),
        )

        val records = WorkoutCalculator.personalRecords(sets)

        assertEquals(180.0, records.heaviestWeightKg, 0.01)
        assertEquals(8, records.maxReps)
        assertEquals(2310.0, records.totalVolumeKg, 0.01)
        assertEquals(210.0, records.bestEstimatedOneRepMaxKg, 0.01)
    }

    @Test
    fun exerciseProgress_excludesIncompleteSetsAndBuildsTrendByDay() {
        val sets = listOf(
            ExerciseProgressSetInput(dateEpochDay = 20_000L, reps = 5, weightKg = 100.0, completed = true),
            ExerciseProgressSetInput(dateEpochDay = 20_000L, reps = 5, weightKg = 102.5, completed = true),
            ExerciseProgressSetInput(dateEpochDay = 20_001L, reps = 3, weightKg = 110.0, completed = true),
            ExerciseProgressSetInput(dateEpochDay = 20_001L, reps = 1, weightKg = 120.0, completed = false),
        )

        val progress = WorkoutCalculator.exerciseProgress(
            exerciseId = "bench",
            exerciseName = "Barbell Bench Press",
            equipment = "barbell",
            targetMuscles = "chest",
            sets = sets,
        )

        assertEquals("Barbell Bench Press", progress.exerciseName)
        assertEquals(110.0, progress.heaviestWeightKg, 0.01)
        assertEquals(5, progress.maxReps)
        assertEquals(121.0, progress.bestEstimatedOneRepMaxKg, 0.01)
        assertEquals(1012.5, progress.bestWorkoutVolumeKg, 0.01)
        assertEquals(2, progress.trend.size)
        assertEquals(1012.5, progress.trend.first().volumeKg, 0.01)
        assertEquals(121.0, progress.trend.last().bestEstimatedOneRepMaxKg, 0.01)
    }
}
