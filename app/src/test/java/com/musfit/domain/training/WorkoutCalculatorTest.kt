package com.musfit.domain.training

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
}
