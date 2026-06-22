package com.musfit.ui.training

import com.musfit.data.repository.ExerciseSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class TrainingHomeContentTest {
    @Test
    fun nextQuickLogExpanded_togglesPanelState() {
        assertEquals(true, nextQuickLogExpanded(current = false))
        assertEquals(false, nextQuickLogExpanded(current = true))
    }

    @Test
    fun routineCardActions_keepsStarterRoutineReadOnlyButStartable() {
        val actions = routineCardActions(isStarter = true)

        assertEquals(listOf("Start", "Duplicate"), actions)
    }

    @Test
    fun routineCardActions_includesEditAndDeleteForCustomRoutine() {
        val actions = routineCardActions(isStarter = false)

        assertEquals(listOf("Start", "Edit", "Duplicate", "Delete"), actions)
    }

    @Test
    fun routineExercisePickerSuggestions_collapsedShowsNoSuggestions() {
        val suggestions = routineExercisePickerSuggestions(
            exercises = listOf(exercise(id = "bench", name = "Barbell Bench Press")),
            selectedExerciseIds = emptySet(),
            query = "",
            expanded = false,
        )

        assertEquals(emptyList<ExerciseSummary>(), suggestions)
    }

    @Test
    fun routineExercisePickerSuggestions_expandedBlankCapsAndExcludesSelected() {
        val suggestions = routineExercisePickerSuggestions(
            exercises = listOf(
                exercise(id = "bench", name = "Barbell Bench Press"),
                exercise(id = "row", name = "Seated Cable Row"),
                exercise(id = "squat", name = "Back Squat"),
                exercise(id = "deadlift", name = "Deadlift"),
            ),
            selectedExerciseIds = setOf("bench"),
            query = "",
            expanded = true,
        )

        assertEquals(listOf("Seated Cable Row", "Back Squat", "Deadlift"), suggestions.map { it.name })
    }

    @Test
    fun routineExercisePickerSuggestions_filtersByNameEquipmentAndMuscle() {
        val suggestions = routineExercisePickerSuggestions(
            exercises = listOf(
                exercise(id = "bench", name = "Barbell Bench Press", equipment = "barbell", targetMuscles = "chest"),
                exercise(id = "row", name = "Seated Cable Row", equipment = "cable", targetMuscles = "back"),
                exercise(id = "raise", name = "Lateral Raise", equipment = "dumbbell", targetMuscles = "shoulders"),
            ),
            selectedExerciseIds = emptySet(),
            query = "cable",
            expanded = true,
        )

        assertEquals(listOf("Seated Cable Row"), suggestions.map { it.name })
    }

    private fun exercise(
        id: String,
        name: String,
        equipment: String? = null,
        targetMuscles: String = "full body",
    ): ExerciseSummary =
        ExerciseSummary(
            id = id,
            name = name,
            category = "strength",
            equipment = equipment,
            targetMuscles = targetMuscles,
            isCustom = false,
        )
}
