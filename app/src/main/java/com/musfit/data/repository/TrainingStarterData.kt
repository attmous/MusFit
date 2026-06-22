package com.musfit.data.repository

internal data class StarterExerciseDefinition(
    val id: String,
    val name: String,
    val equipment: String?,
    val targetMuscles: String,
)

internal data class StarterRoutineExerciseDefinition(
    val exerciseId: String,
    val targetSets: Int,
    val targetReps: String,
)

internal data class StarterRoutineDefinition(
    val id: String,
    val name: String,
    val notes: String,
    val exercises: List<StarterRoutineExerciseDefinition>,
)

internal object TrainingStarterData {
    val exercises = listOf(
        StarterExerciseDefinition("starter-ex-bench-press", "Barbell Bench Press", "barbell", "chest,triceps,shoulders"),
        StarterExerciseDefinition("starter-ex-back-squat", "Back Squat", "barbell", "quads,glutes,hamstrings"),
        StarterExerciseDefinition("starter-ex-deadlift", "Deadlift", "barbell", "back,glutes,hamstrings"),
        StarterExerciseDefinition("starter-ex-overhead-press", "Overhead Press", "barbell", "shoulders,triceps"),
        StarterExerciseDefinition("starter-ex-barbell-row", "Barbell Row", "barbell", "back,biceps"),
        StarterExerciseDefinition("starter-ex-lat-pulldown", "Lat Pulldown", "machine", "back,biceps"),
        StarterExerciseDefinition("starter-ex-cable-row", "Seated Cable Row", "cable", "back,biceps"),
        StarterExerciseDefinition("starter-ex-incline-db-press", "Incline Dumbbell Press", "dumbbell", "chest,shoulders,triceps"),
        StarterExerciseDefinition("starter-ex-db-shoulder-press", "Dumbbell Shoulder Press", "dumbbell", "shoulders,triceps"),
        StarterExerciseDefinition("starter-ex-leg-press", "Leg Press", "machine", "quads,glutes"),
        StarterExerciseDefinition("starter-ex-romanian-deadlift", "Romanian Deadlift", "barbell", "hamstrings,glutes,back"),
        StarterExerciseDefinition("starter-ex-leg-curl", "Leg Curl", "machine", "hamstrings"),
        StarterExerciseDefinition("starter-ex-leg-extension", "Leg Extension", "machine", "quads"),
        StarterExerciseDefinition("starter-ex-face-pull", "Face Pull", "cable", "rear delts,upper back"),
        StarterExerciseDefinition("starter-ex-biceps-curl", "Dumbbell Biceps Curl", "dumbbell", "biceps"),
        StarterExerciseDefinition("starter-ex-triceps-pushdown", "Triceps Pushdown", "cable", "triceps"),
    )

    val routines = listOf(
        StarterRoutineDefinition(
            id = "starter-routine-full-body-a",
            name = "Full Body A",
            notes = "Balanced starter strength session.",
            exercises = listOf(
                StarterRoutineExerciseDefinition("starter-ex-back-squat", 3, "5"),
                StarterRoutineExerciseDefinition("starter-ex-bench-press", 3, "5"),
                StarterRoutineExerciseDefinition("starter-ex-barbell-row", 3, "8"),
                StarterRoutineExerciseDefinition("starter-ex-romanian-deadlift", 2, "8"),
                StarterRoutineExerciseDefinition("starter-ex-face-pull", 2, "12"),
            ),
        ),
        StarterRoutineDefinition(
            id = "starter-routine-full-body-b",
            name = "Full Body B",
            notes = "Alternate full-body starter session.",
            exercises = listOf(
                StarterRoutineExerciseDefinition("starter-ex-deadlift", 3, "3"),
                StarterRoutineExerciseDefinition("starter-ex-overhead-press", 3, "5"),
                StarterRoutineExerciseDefinition("starter-ex-lat-pulldown", 3, "8"),
                StarterRoutineExerciseDefinition("starter-ex-leg-press", 3, "10"),
                StarterRoutineExerciseDefinition("starter-ex-triceps-pushdown", 2, "12"),
            ),
        ),
        StarterRoutineDefinition(
            id = "starter-routine-push",
            name = "Push",
            notes = "Chest, shoulders, and triceps.",
            exercises = listOf(
                StarterRoutineExerciseDefinition("starter-ex-bench-press", 3, "5"),
                StarterRoutineExerciseDefinition("starter-ex-incline-db-press", 3, "8"),
                StarterRoutineExerciseDefinition("starter-ex-overhead-press", 3, "6"),
                StarterRoutineExerciseDefinition("starter-ex-db-shoulder-press", 2, "10"),
                StarterRoutineExerciseDefinition("starter-ex-triceps-pushdown", 3, "12"),
            ),
        ),
        StarterRoutineDefinition(
            id = "starter-routine-pull",
            name = "Pull",
            notes = "Back and biceps.",
            exercises = listOf(
                StarterRoutineExerciseDefinition("starter-ex-deadlift", 3, "3"),
                StarterRoutineExerciseDefinition("starter-ex-barbell-row", 3, "8"),
                StarterRoutineExerciseDefinition("starter-ex-lat-pulldown", 3, "10"),
                StarterRoutineExerciseDefinition("starter-ex-cable-row", 2, "10"),
                StarterRoutineExerciseDefinition("starter-ex-biceps-curl", 3, "12"),
            ),
        ),
        StarterRoutineDefinition(
            id = "starter-routine-legs",
            name = "Legs",
            notes = "Squat-focused lower-body session.",
            exercises = listOf(
                StarterRoutineExerciseDefinition("starter-ex-back-squat", 3, "5"),
                StarterRoutineExerciseDefinition("starter-ex-leg-press", 3, "10"),
                StarterRoutineExerciseDefinition("starter-ex-romanian-deadlift", 3, "8"),
                StarterRoutineExerciseDefinition("starter-ex-leg-curl", 3, "12"),
                StarterRoutineExerciseDefinition("starter-ex-leg-extension", 3, "12"),
            ),
        ),
    )
}
