package com.musfit.ui.training

import com.musfit.data.repository.ExerciseSummary
import com.musfit.data.repository.LoggedWorkoutSetDetail
import com.musfit.data.repository.WorkoutExerciseBlock
import org.junit.Assert.assertEquals
import org.junit.Test

class TrainingActiveWorkoutContentTest {
    @Test
    fun compactExerciseSuggestions_collapsedDoesNotShowLibraryItems() {
        val suggestions = compactExerciseSuggestions(
            exercises = listOf(
                exercise(id = "squat", name = "Back Squat"),
                exercise(id = "bench", name = "Barbell Bench Press"),
            ),
            query = "",
            expanded = false,
        )

        assertEquals(emptyList<ExerciseSummary>(), suggestions)
    }

    @Test
    fun compactExerciseSuggestions_expandedCapsBlankAndFilteredSuggestions() {
        val exercises = listOf(
            exercise(id = "squat", name = "Back Squat"),
            exercise(id = "bench", name = "Barbell Bench Press"),
            exercise(id = "row", name = "Barbell Row"),
            exercise(id = "deadlift", name = "Deadlift"),
            exercise(id = "curl", name = "Dumbbell Biceps Curl"),
            exercise(id = "press", name = "Dumbbell Shoulder Press"),
            exercise(id = "face-pull", name = "Face Pull"),
        )

        val blankSuggestions = compactExerciseSuggestions(
            exercises = exercises,
            query = "",
            expanded = true,
        )
        val filteredSuggestions = compactExerciseSuggestions(
            exercises = exercises,
            query = "bell",
            expanded = true,
        )

        assertEquals(listOf("Back Squat", "Barbell Bench Press", "Barbell Row"), blankSuggestions.map { it.name })
        assertEquals(
            listOf(
                "Barbell Bench Press",
                "Barbell Row",
                "Dumbbell Biceps Curl",
                "Dumbbell Shoulder Press",
            ),
            filteredSuggestions.map { it.name },
        )
    }

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
    fun formatWorkoutSetRowsForDisplay_labelsDropAndFailureSets() {
        val rows = formatWorkoutSetRowsForDisplay(
            listOf(
                set(id = "warmup", setType = "warmup", previousLabel = null, reps = 10, weightKg = 40.0),
                set(id = "working", setType = "working", previousLabel = null, reps = 6, weightKg = 80.0),
                set(id = "drop", setType = "drop", previousLabel = null, reps = 10, weightKg = 60.0),
                set(id = "failure", setType = "failure", previousLabel = null, reps = 12, weightKg = 50.0),
            ),
        )

        assertEquals(listOf("W", "1", "D", "F"), rows.map { it.setLabel })
    }

    @Test
    fun formatWorkoutSetRowsForDisplay_flagsPrOnlyForCompletedWorkingSetsBeatingPriorBest() {
        val rows = formatWorkoutSetRowsForDisplay(
            listOf(
                // est-1RM = 100 * (1 + 5/30) ≈ 116.7 > 110 prior best, completed → PR.
                set(id = "pr", setType = "working", previousLabel = null, reps = 5, weightKg = 100.0, completed = true),
                // Same lift but not completed → no PR.
                set(id = "not-done", setType = "working", previousLabel = null, reps = 5, weightKg = 100.0, completed = false),
                // Lighter completed set below prior best → no PR.
                set(id = "light", setType = "working", previousLabel = null, reps = 5, weightKg = 80.0, completed = true),
                // Heavy completed warmup is never a PR.
                set(id = "warmup", setType = "warmup", previousLabel = null, reps = 5, weightKg = 100.0, completed = true),
            ),
            priorBestEstimatedOneRepMaxKg = 110.0,
        )

        assertEquals(listOf(true, false, false, false), rows.map { it.isPr })
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

    @Test
    fun restTimerDisplayText_formatsOffRunningAndPausedStates() {
        assertEquals("Rest Timer: OFF", restTimerDisplayText(RestTimerState()))
        assertEquals(
            "Rest Timer: 2min 0s",
            restTimerDisplayText(
                RestTimerState(
                    isVisible = true,
                    sourceSetId = "set-1",
                    durationSeconds = 120,
                    remainingSeconds = 120,
                    isRunning = true,
                ),
            ),
        )
        assertEquals(
            "Rest Timer: Paused at 1min 5s",
            restTimerDisplayText(
                RestTimerState(
                    isVisible = true,
                    sourceSetId = "set-1",
                    durationSeconds = 120,
                    remainingSeconds = 65,
                    isRunning = false,
                ),
            ),
        )
    }

    @Test
    fun restTimerSettingsSummaryText_focusesOnCompletedSetTrigger() {
        assertEquals("90 sec after each completed set", restTimerSettingsSummaryText("90"))
        assertEquals("Set rest after each completed set", restTimerSettingsSummaryText(""))
    }

    @Test
    fun activeWorkoutStatLine_mergesDurationVolumeAndSetProgress() {
        assertEquals(
            "1:12 · 537.5 kg · set 3 of 13",
            activeWorkoutStatLine(
                elapsedSeconds = 72,
                totalVolumeKg = 537.5,
                completedSetCount = 2,
                totalSetCount = 13,
            ),
        )
        // All sets done: clamps to the last set instead of overshooting.
        assertEquals(
            "45:00 · 5000 kg · set 13 of 13",
            activeWorkoutStatLine(
                elapsedSeconds = 2700,
                totalVolumeKg = 5000.0,
                completedSetCount = 13,
                totalSetCount = 13,
            ),
        )
        // No sets yet: skip the set part entirely.
        assertEquals(
            "0:05 · 0 kg",
            activeWorkoutStatLine(
                elapsedSeconds = 5,
                totalVolumeKg = 0.0,
                completedSetCount = 0,
                totalSetCount = 0,
            ),
        )
    }

    @Test
    fun defaultFocusedExerciseId_picksFirstUnfinishedElseLast() {
        val doneBlock = block(
            exerciseId = "squat",
            sets = listOf(set(id = "s1", setType = "working", previousLabel = null, reps = 5, weightKg = 100.0, completed = true)),
        )
        val pendingBlock = block(
            exerciseId = "bench",
            sets = listOf(set(id = "b1", setType = "working", previousLabel = null, reps = null, weightKg = null)),
        )

        assertEquals("bench", defaultFocusedExerciseId(listOf(doneBlock, pendingBlock)))
        assertEquals("squat", defaultFocusedExerciseId(listOf(doneBlock)))
        assertEquals(null, defaultFocusedExerciseId(emptyList()))
    }

    @Test
    fun upNextTarget_summarizesPlannedSets() {
        val withReps = block(
            exerciseId = "bench",
            targetReps = "8",
            sets = List(3) { set(id = "s$it", setType = "working", previousLabel = null, reps = null, weightKg = null) },
        )
        val withoutReps = block(
            exerciseId = "row",
            targetReps = null,
            sets = List(2) { set(id = "r$it", setType = "working", previousLabel = null, reps = null, weightKg = null) },
        )

        assertEquals("3 × 8", upNextTarget(withReps))
        assertEquals("2 sets", upNextTarget(withoutReps))
        assertEquals("no sets", upNextTarget(block(exerciseId = "plank", sets = emptyList())))
    }

    @Test
    fun lastTimeLabel_prefersPreviousLabelThenTargetReps() {
        val withPrevious = block(
            exerciseId = "squat",
            targetReps = "5",
            sets = listOf(set(id = "s1", setType = "working", previousLabel = "105 kg x 5", reps = null, weightKg = null)),
        )
        val withoutPrevious = block(
            exerciseId = "bench",
            targetReps = "8",
            sets = listOf(set(id = "b1", setType = "working", previousLabel = null, reps = null, weightKg = null)),
        )
        val bare = block(exerciseId = "row", targetReps = null, sets = emptyList())

        assertEquals("last time 105 kg x 5", lastTimeLabel(withPrevious))
        assertEquals("target 8 reps", lastTimeLabel(withoutPrevious))
        assertEquals(null, lastTimeLabel(bare))
    }

    @Test
    fun plateLineText_usesConfiguredBarAndPlates() {
        assertEquals(
            "Plates · 20 + 2.5 / side",
            plateLineText(
                weightKg = 60.0,
                barWeightKg = 15.0,
                availablePlatesKg = listOf(20.0, 10.0, 5.0, 2.5),
            ),
        )
        assertEquals(
            null,
            plateLineText(
                weightKg = 20.0,
                barWeightKg = 20.0,
                availablePlatesKg = listOf(20.0, 10.0, 5.0, 2.5),
            ),
        )
    }

    private fun set(
        id: String,
        setType: String,
        previousLabel: String?,
        reps: Int?,
        weightKg: Double?,
        rpe: Double? = null,
        completed: Boolean = false,
    ): LoggedWorkoutSetDetail =
        LoggedWorkoutSetDetail(
            id = id,
            exerciseId = "exercise-bench",
            setType = setType,
            reps = reps,
            weightKg = weightKg,
            rpe = rpe,
            notes = null,
            completed = completed,
            previousLabel = previousLabel,
        )

    private fun exercise(id: String, name: String): ExerciseSummary =
        ExerciseSummary(
            id = id,
            name = name,
            category = "Strength",
            equipment = null,
            targetMuscles = "Full body",
            isCustom = false,
        )

    private fun block(
        exerciseId: String,
        targetReps: String? = null,
        sets: List<LoggedWorkoutSetDetail> = emptyList(),
    ): WorkoutExerciseBlock =
        WorkoutExerciseBlock(
            exercise = exercise(id = exerciseId, name = exerciseId.replaceFirstChar { it.uppercase() }),
            targetReps = targetReps,
            sets = sets,
        )
}
