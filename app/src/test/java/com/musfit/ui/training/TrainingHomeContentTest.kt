package com.musfit.ui.training

import com.musfit.data.repository.ExerciseSummary
import com.musfit.data.repository.RoutineExerciseInput
import com.musfit.data.repository.RoutineSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingHomeContentTest {
    @Test
    fun validateTargetSets_enforces1To20() {
        assertEquals(TargetFieldResult.Valid, validateTargetSets("3"))
        assertEquals(TargetFieldResult.Valid, validateTargetSets("1"))
        assertEquals(TargetFieldResult.Valid, validateTargetSets("20"))
        assertTrue(validateTargetSets("0") is TargetFieldResult.Invalid)
        assertTrue(validateTargetSets("21") is TargetFieldResult.Invalid)
        assertTrue(validateTargetSets("") is TargetFieldResult.Invalid)
        assertTrue(validateTargetSets("abc") is TargetFieldResult.Invalid)
        assertTrue(validateTargetSets("-1") is TargetFieldResult.Invalid)
    }

    @Test
    fun validateTargetReps_acceptsBlankNumberOrRange() {
        assertEquals(TargetFieldResult.Valid, validateTargetReps(""))
        assertEquals(TargetFieldResult.Valid, validateTargetReps("8"))
        assertEquals(TargetFieldResult.Valid, validateTargetReps("8-12"))
        assertEquals(TargetFieldResult.Valid, validateTargetReps("8 - 12"))
        assertTrue(validateTargetReps("12-8") is TargetFieldResult.Invalid)
        assertTrue(validateTargetReps("0") is TargetFieldResult.Invalid)
        assertTrue(validateTargetReps("101") is TargetFieldResult.Invalid)
        assertTrue(validateTargetReps("8-") is TargetFieldResult.Invalid)
        assertTrue(validateTargetReps("x") is TargetFieldResult.Invalid)
    }

    @Test
    fun routineEditorCanSave_requiresNameExercisesAndValidTargets() {
        val validRow = RoutineExerciseInput(exerciseId = "e1", targetSets = 3, targetReps = "8-12")
        assertFalse(routineEditorCanSave("", listOf(validRow)))
        assertFalse(routineEditorCanSave("Push", emptyList()))
        assertFalse(routineEditorCanSave("Push", listOf(RoutineExerciseInput("e1", 0, "8"))))
        assertTrue(routineEditorCanSave("Push", listOf(validRow)))
    }

    @Test
    fun nextQuickLogExpanded_togglesPanelState() {
        assertEquals(true, nextQuickLogExpanded(current = false))
        assertEquals(false, nextQuickLogExpanded(current = true))
    }

    @Test
    fun routineCardActions_allowsStarterRoutinesToBeEditedButNotDeleted() {
        val actions = routineCardActions(isStarter = true)

        assertEquals(listOf("Start", "Edit", "Duplicate"), actions)
    }

    @Test
    fun routineCardActions_includesEditAndDeleteForCustomRoutine() {
        val actions = routineCardActions(isStarter = false)

        assertEquals(listOf("Start", "Edit", "Duplicate", "Delete"), actions)
    }

    @Test
    fun groupRoutineSummariesByProgram_groupsSavedRoutinesWithFallback() {
        val groups = groupRoutineSummariesByProgram(
            listOf(
                routine(id = "upper-a", name = "Upper A", programName = "Upper Lower"),
                routine(id = "full-body-a", name = "Full Body A", programName = "Full Body"),
                routine(id = "custom", name = "Garage Day", programName = null),
                routine(id = "upper-b", name = "Upper B", programName = "Upper Lower"),
            ),
        )

        assertEquals(listOf("Upper Lower", "Full Body", "My routines"), groups.map { it.title })
        assertEquals(listOf("Upper A", "Upper B"), groups.first().routines.map { it.name })
        assertEquals(listOf("Garage Day"), groups.last().routines.map { it.name })
    }

    @Test
    fun routineExercisePickerOptions_derivesEquipmentAndMuscleChips() {
        val options = routineExercisePickerOptions(
            listOf(
                exercise(id = "bench", name = "Barbell Bench Press", equipment = "barbell", targetMuscles = "chest,triceps"),
                exercise(id = "row", name = "Seated Cable Row", equipment = "cable", targetMuscles = "back, biceps"),
                exercise(id = "raise", name = "Lateral Raise", equipment = "dumbbell", targetMuscles = "shoulders"),
            ),
        )

        assertEquals(listOf("barbell", "cable", "dumbbell"), options.equipment)
        assertEquals(listOf("back", "biceps", "chest", "shoulders", "triceps"), options.muscles)
    }

    @Test
    fun routineExercisePickerSuggestions_blankShowsSavedExercisesAndExcludesSelected() {
        val suggestions = routineExercisePickerSuggestions(
            exercises = listOf(
                exercise(id = "bench", name = "Barbell Bench Press"),
                exercise(id = "row", name = "Seated Cable Row"),
                exercise(id = "squat", name = "Back Squat"),
                exercise(id = "deadlift", name = "Deadlift"),
            ),
            selectedExerciseIds = setOf("bench"),
            query = "",
        )

        assertEquals(listOf("Seated Cable Row", "Back Squat", "Deadlift"), suggestions.map { it.name })
    }

    @Test
    fun routineExercisePickerSuggestions_filtersBySearchEquipmentAndMuscle() {
        val suggestions = routineExercisePickerSuggestions(
            exercises = listOf(
                exercise(id = "bench", name = "Barbell Bench Press", equipment = "barbell", targetMuscles = "chest"),
                exercise(id = "row", name = "Seated Cable Row", equipment = "cable", targetMuscles = "back"),
                exercise(id = "raise", name = "Lateral Raise", equipment = "dumbbell", targetMuscles = "shoulders"),
            ),
            selectedExerciseIds = emptySet(),
            query = "row",
            equipmentFilter = "cable",
            muscleFilter = "back",
        )

        assertEquals(listOf("Seated Cable Row"), suggestions.map { it.name })
    }

    private fun routine(
        id: String,
        name: String,
        programName: String?,
    ): RoutineSummary =
        RoutineSummary(
            id = id,
            name = name,
            notes = null,
            exerciseCount = 4,
            targetSetCount = 12,
            isStarter = programName != null,
            programName = programName,
        )

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
