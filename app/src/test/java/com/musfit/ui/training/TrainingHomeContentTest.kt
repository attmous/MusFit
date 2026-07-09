package com.musfit.ui.training

import com.musfit.data.repository.ExerciseSummary
import com.musfit.data.repository.RoutineExerciseInput
import com.musfit.data.repository.RoutineFolder
import com.musfit.data.repository.RoutineSummary
import com.musfit.data.repository.RoutineSetInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
        val validRow = RoutineExerciseInput(
            exerciseId = "e1",
            targetSets = 3,
            targetReps = "8-12",
            restSeconds = 180,
            setPlans = listOf(
                RoutineSetInput(setType = "warmup", targetReps = "12", targetWeightKg = 40.0),
                RoutineSetInput(setType = "working", targetReps = "8", targetWeightKg = 80.0),
            ),
        )
        assertFalse(routineEditorCanSave("", listOf(validRow)))
        assertFalse(routineEditorCanSave("Push", emptyList()))
        assertFalse(routineEditorCanSave("Push", listOf(RoutineExerciseInput("e1", 0, "8"))))
        assertFalse(
            routineEditorCanSave(
                "Push",
                listOf(
                    validRow.copy(
                        restSeconds = 0,
                    ),
                ),
            ),
        )
        assertFalse(
            routineEditorCanSave(
                "Push",
                listOf(
                    validRow.copy(
                        setPlans = listOf(RoutineSetInput(setType = "unknown", targetReps = "8")),
                    ),
                ),
            ),
        )
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
    fun routineDescription_prefersNotesAndFallsBackToStarterLabel() {
        assertEquals(
            "Heavy compound upper-body session.",
            routineDescription(
                routine(
                    id = "upper",
                    name = "Upper A",
                    folderName = null,
                    notes = " Heavy compound upper-body session. ",
                ),
            ),
        )
        assertEquals(
            "Pre-saved routine",
            routineDescription(routine(id = "starter", name = "Full Body A", folderName = "Starter Pack")),
        )
    }

    @Test
    fun groupRoutineSummariesByFolder_groupsSavedRoutinesWithFallback() {
        val groups = groupRoutineSummariesByFolder(
            listOf(
                routine(id = "push-a", name = "Push A", folderName = "PPL System"),
                routine(id = "full-body-a", name = "Full Body A", folderName = "Starter Pack"),
                routine(id = "custom", name = "Garage Day", folderName = null),
                routine(id = "push-b", name = "Push B", folderName = "PPL System"),
            ),
        )

        assertEquals(listOf("PPL System", "Starter Pack", "My routines"), groups.map { it.title })
        assertEquals(listOf("Push A", "Push B"), groups.first().routines.map { it.name })
        assertEquals(listOf("Garage Day"), groups.last().routines.map { it.name })
    }

    @Test
    fun groupRoutineSummariesByFolder_includesEmptyConfiguredFolders() {
        val groups = groupRoutineSummariesByFolder(
            routines = listOf(
                routine(id = "push-a", name = "Push A", folderName = "PPL System"),
                routine(id = "custom", name = "Garage Day", folderName = null),
            ),
            folders = listOf(
                RoutineFolder(id = "folder-ppl", name = "PPL System", sortOrder = 0),
                RoutineFolder(id = "folder-new", name = "Powerbuilding", sortOrder = 1),
            ),
        )

        assertEquals(listOf("PPL System", "Powerbuilding", "My routines"), groups.map { it.title })
        assertEquals(listOf("Push A"), groups[0].routines.map { it.name })
        assertEquals(emptyList<String>(), groups[1].routines.map { it.name })
        assertEquals(listOf("Garage Day"), groups[2].routines.map { it.name })

        // User folders carry their id + are flagged so the section can host drops and an edit control.
        assertEquals(listOf("folder-ppl", "folder-new"), groups.take(2).map { it.folderId })
        assertTrue(groups[0].isUserFolder)
        assertTrue(groups[1].isUserFolder)
        // The "My routines" bucket is droppable-by-title but is not an editable user folder.
        assertEquals(null, groups[2].folderId)
        assertFalse(groups[2].isUserFolder)
    }

    @Test
    fun routineFolderDropTargetAt_resolvesFolderAndUnassignedTargets() {
        val targetBounds = mapOf(
            null to Rect(left = 0f, top = 0f, right = 96f, bottom = 40f),
            "folder-ppl" to Rect(left = 104f, top = 0f, right = 240f, bottom = 40f),
        )

        assertEquals(RoutineFolderDropTarget(null), routineFolderDropTargetAt(Offset(24f, 20f), targetBounds))
        assertEquals(RoutineFolderDropTarget("folder-ppl"), routineFolderDropTargetAt(Offset(160f, 20f), targetBounds))
        assertEquals(null, routineFolderDropTargetAt(Offset(260f, 20f), targetBounds))
    }

    @Test
    fun routineFolderMoveTargets_includesUnassignedThenConfiguredFolders() {
        val targets = routineFolderMoveTargets(
            listOf(
                RoutineFolder(id = "folder-full-body", name = "Full Body", sortOrder = 0),
                RoutineFolder(id = "folder-ppl", name = "Push Pull Legs", sortOrder = 1),
            ),
        )

        assertEquals(
            listOf(
                RoutineFolderMoveTarget(folderId = null, label = "My routines"),
                RoutineFolderMoveTarget(folderId = "folder-full-body", label = "Full Body"),
                RoutineFolderMoveTarget(folderId = "folder-ppl", label = "Push Pull Legs"),
            ),
            targets,
        )
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
    fun routineExercisePickerSuggestions_blankShowsAllAvailableExercisesWithoutDefaultCap() {
        val exercises = (1..120).map { index ->
            exercise(id = "exercise-$index", name = "Exercise $index")
        }

        val suggestions = routineExercisePickerSuggestions(
            exercises = exercises,
            selectedExerciseIds = setOf("exercise-2", "exercise-99"),
            query = "",
        )

        assertEquals(118, suggestions.size)
        assertEquals("Exercise 1", suggestions.first().name)
        assertEquals("Exercise 120", suggestions.last().name)
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
            filters = TrainingPickerFilters(equipment = setOf("cable"), muscles = setOf("back")),
        )

        assertEquals(listOf("Seated Cable Row"), suggestions.map { it.name })
    }

    @Test
    fun routineExercisePickerSuggestions_onlyDoneKeepsLoggedExercises() {
        val suggestions = routineExercisePickerSuggestions(
            exercises = listOf(
                exercise(id = "bench", name = "Barbell Bench Press"),
                exercise(id = "row", name = "Seated Cable Row"),
            ),
            selectedExerciseIds = emptySet(),
            query = "",
            filters = TrainingPickerFilters(onlyDone = true),
            loggedExerciseIds = setOf("row"),
        )

        assertEquals(listOf("Seated Cable Row"), suggestions.map { it.name })
    }

    @Test
    fun pickerFilterSummary_titleCasesActiveFilters() {
        assertEquals(
            "Barbell · Quads",
            pickerFilterSummary(TrainingPickerFilters(equipment = setOf("barbell"), muscles = setOf("quads"))),
        )
        assertEquals(
            "Dumbbell · Done before",
            pickerFilterSummary(TrainingPickerFilters(equipment = setOf("dumbbell"), onlyDone = true)),
        )
    }

    @Test
    fun trainingPickerFilters_countsActiveSelections() {
        assertEquals(0, TrainingPickerFilters().activeCount)
        assertEquals(
            3,
            TrainingPickerFilters(
                equipment = setOf("barbell"),
                muscles = setOf("quads"),
                onlyDone = true,
            ).activeCount,
        )
    }

    @Test
    fun topPickerMuscles_ranksByCatalogFrequency() {
        val muscles = topPickerMuscles(
            listOf(
                exercise(id = "squat", name = "Back Squat", targetMuscles = "quads, glutes"),
                exercise(id = "lunge", name = "Lunge", targetMuscles = "quads, glutes"),
                exercise(id = "leg-ext", name = "Leg Extension", targetMuscles = "quads"),
                exercise(id = "sit-up", name = "Sit-up", targetMuscles = "abs"),
            ),
            limit = 2,
        )

        assertEquals(listOf("quads", "glutes"), muscles)
    }

    @Test
    fun topPickerEquipment_ranksByCatalogFrequency() {
        val equipment = topPickerEquipment(
            listOf(
                exercise(id = "squat", name = "Back Squat", equipment = "barbell"),
                exercise(id = "bench", name = "Bench Press", equipment = "barbell"),
                exercise(id = "curl", name = "Curl", equipment = "dumbbell"),
                exercise(id = "band", name = "Band Pull", equipment = "band"),
            ),
            limit = 2,
        )

        assertEquals(listOf("barbell", "band"), equipment)
    }

    @Test
    fun pickerConfirmLabel_countsSelection() {
        assertEquals("Add exercises", pickerConfirmLabel(0))
        assertEquals("Add 1 exercise", pickerConfirmLabel(1))
        assertEquals("Add 2 exercises", pickerConfirmLabel(2))
    }

    @Test
    fun routineExerciseSubline_showsTargetAndRest() {
        assertEquals(
            "3 × 8 · 150s rest",
            routineExerciseSubline(
                RoutineExerciseInput(exerciseId = "bench", targetSets = 3, targetReps = "8", restSeconds = 150),
            ),
        )
        assertEquals(
            "2 sets",
            routineExerciseSubline(
                RoutineExerciseInput(exerciseId = "plank", targetSets = 2, targetReps = null),
            ),
        )
    }

    @Test
    fun setPlanSummaryLabel_describesSpecialSets() {
        assertEquals("Straight sets", setPlanSummaryLabel(listOf(RoutineSetInput(setType = "working"))))
        assertEquals(
            "First set to failure",
            setPlanSummaryLabel(
                listOf(RoutineSetInput(setType = "failure"), RoutineSetInput(setType = "working")),
            ),
        )
        assertEquals(
            "1 warm-up set · 1 drop set",
            setPlanSummaryLabel(
                listOf(
                    RoutineSetInput(setType = "warmup"),
                    RoutineSetInput(setType = "working"),
                    RoutineSetInput(setType = "drop"),
                ),
            ),
        )
    }

    @Test
    fun routineEditorMetaLine_summarizesBlockSizeAndDuration() {
        val editor = RoutineEditorState(
            routineId = "full-body-a",
            name = "Full Body A",
            folderName = "Strength block",
            exercises = listOf(
                RoutineExerciseInput(exerciseId = "squat", targetSets = 3, targetReps = "5"),
                RoutineExerciseInput(exerciseId = "bench", targetSets = 3, targetReps = "8"),
            ),
        )

        assertEquals("Strength block · 2 exercises · ~20 min", routineEditorMetaLine(editor))
        assertEquals(
            "Routine · 0 exercises",
            routineEditorMetaLine(RoutineEditorState(name = "New")),
        )
    }

    private fun routine(
        id: String,
        name: String,
        folderName: String?,
        notes: String? = null,
    ): RoutineSummary =
        RoutineSummary(
            id = id,
            name = name,
            notes = notes,
            exerciseCount = 4,
            targetSetCount = 12,
            isStarter = folderName != null,
            folderName = folderName,
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
