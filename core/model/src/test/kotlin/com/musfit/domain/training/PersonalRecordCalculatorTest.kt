package com.musfit.domain.training

import org.junit.Assert.assertEquals
import org.junit.Test

class PersonalRecordCalculatorTest {
    @Test
    fun recentPersonalRecords_flagsOnlySetsBeatingPriorBestPerExercise() {
        val events = PersonalRecordCalculator.recentPersonalRecords(
            listOf(
                // Squat: 100x5 (e1RM ~116.7) then a lighter day (no PR) then 107.5x5 (~125.4).
                squatSet(day = 1, reps = 5, weightKg = 100.0),
                squatSet(day = 8, reps = 5, weightKg = 90.0),
                squatSet(day = 15, reps = 5, weightKg = 107.5),
                // Deadlift: single PR.
                set(exerciseId = "rdl", name = "Romanian Deadlift", day = 10, reps = 8, weightKg = 90.0),
            ),
        )

        assertEquals(listOf(15L, 10L, 1L), events.map { it.dateEpochDay })
        assertEquals(listOf("Back Squat", "Romanian Deadlift", "Back Squat"), events.map { it.exerciseName })
        assertEquals(107.5, events.first().weightKg, 1e-6)
    }

    @Test
    fun recentPersonalRecords_collapsesSameDayPrsToTheBestSet() {
        val events = PersonalRecordCalculator.recentPersonalRecords(
            listOf(
                squatSet(day = 3, reps = 5, weightKg = 100.0),
                squatSet(day = 3, reps = 5, weightKg = 105.0),
                squatSet(day = 3, reps = 5, weightKg = 110.0),
            ),
        )

        assertEquals(1, events.size)
        assertEquals(110.0, events.single().weightKg, 1e-6)
    }

    @Test
    fun recentPersonalRecords_appliesLimitToMostRecent() {
        val events = PersonalRecordCalculator.recentPersonalRecords(
            (1..6).map { day -> squatSet(day = day.toLong(), reps = 5, weightKg = 100.0 + day * 2.5) },
            limit = 2,
        )

        assertEquals(listOf(6L, 5L), events.map { it.dateEpochDay })
    }

    @Test
    fun recentPersonalRecords_emptyInputYieldsNoEvents() {
        assertEquals(emptyList<PersonalRecordEvent>(), PersonalRecordCalculator.recentPersonalRecords(emptyList()))
    }

    private fun squatSet(day: Long, reps: Int, weightKg: Double) = set(exerciseId = "squat", name = "Back Squat", day = day, reps = reps, weightKg = weightKg)

    private fun set(exerciseId: String, name: String, day: Long, reps: Int, weightKg: Double) = PersonalRecordSetInput(
        exerciseId = exerciseId,
        exerciseName = name,
        dateEpochDay = day,
        reps = reps,
        weightKg = weightKg,
    )
}
