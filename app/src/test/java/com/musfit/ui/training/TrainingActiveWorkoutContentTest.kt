package com.musfit.ui.training

import com.musfit.data.repository.LoggedWorkoutSetDetail
import org.junit.Assert.assertEquals
import org.junit.Test

class TrainingActiveWorkoutContentTest {
    @Test
    fun formatWorkoutSetRowsForDisplay_labelsWarmupsAndNumbersWorkingSets() {
        val rows = formatWorkoutSetRowsForDisplay(
            listOf(
                set(id = "warmup-1", setType = "warmup", previousLabel = null, reps = 15, weightKg = 20.0),
                set(id = "warmup-2", setType = "warmup", previousLabel = "40kg x 5", reps = 5, weightKg = 40.0),
                set(id = "working-1", setType = "working", previousLabel = "70kg x 8", reps = 8, weightKg = 70.0),
                set(id = "working-2", setType = "working", previousLabel = null, reps = null, weightKg = null),
            ),
        )

        assertEquals(listOf("W", "W", "1", "2"), rows.map { it.setLabel })
        assertEquals(listOf("-", "40kg x 5", "70kg x 8", "-"), rows.map { it.previousLabel })
        assertEquals(listOf("20", "40", "70", ""), rows.map { it.weightKg })
        assertEquals(listOf("15", "5", "8", ""), rows.map { it.reps })
        assertEquals(listOf("", "", "", ""), rows.map { it.rpe })
    }

    @Test
    fun formatWorkoutSetRowsForDisplay_keepsCompactDecimalValuesAndRpe() {
        val rows = formatWorkoutSetRowsForDisplay(
            listOf(
                set(
                    id = "working-1",
                    setType = "working",
                    previousLabel = "22.5kg x 12",
                    reps = 12,
                    weightKg = 22.5,
                    rpe = 7.5,
                ),
            ),
        )

        assertEquals("22.5", rows.single().weightKg)
        assertEquals("7.5", rows.single().rpe)
    }

    private fun set(
        id: String,
        setType: String,
        previousLabel: String?,
        reps: Int?,
        weightKg: Double?,
        rpe: Double? = null,
    ): LoggedWorkoutSetDetail =
        LoggedWorkoutSetDetail(
            id = id,
            exerciseId = "exercise-bench",
            setType = setType,
            reps = reps,
            weightKg = weightKg,
            rpe = rpe,
            notes = null,
            completed = false,
            previousLabel = previousLabel,
        )
}
