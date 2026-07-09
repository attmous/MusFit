package com.musfit.domain.training

/** One completed working set, flattened for PR detection. */
data class PersonalRecordSetInput(
    val exerciseId: String,
    val exerciseName: String,
    val dateEpochDay: Long,
    val reps: Int,
    val weightKg: Double,
)

/** A set that beat the exercise's prior best estimated 1RM. */
data class PersonalRecordEvent(
    val exerciseId: String,
    val exerciseName: String,
    val dateEpochDay: Long,
    val reps: Int,
    val weightKg: Double,
    val estimatedOneRepMaxKg: Double,
)

/**
 * Cross-exercise PR history for the Progress page's "Recent PRs" list. A PR is any
 * completed set whose estimated 1RM beats everything the exercise logged before it;
 * multiple PRs on the same day collapse to that day's best so one big session
 * doesn't flood the list.
 */
object PersonalRecordCalculator {
    fun recentPersonalRecords(
        sets: List<PersonalRecordSetInput>,
        limit: Int = 10,
    ): List<PersonalRecordEvent> {
        val events = mutableListOf<PersonalRecordEvent>()
        sets.groupBy { it.exerciseId }.values.forEach { exerciseSets ->
            var bestSoFar = 0.0
            val prsByDay = linkedMapOf<Long, PersonalRecordEvent>()
            exerciseSets.sortedBy { it.dateEpochDay }.forEach { set ->
                val estimated = WorkoutCalculator.estimatedOneRepMax(set.weightKg, set.reps)
                if (estimated > bestSoFar + EPSILON) {
                    bestSoFar = estimated
                    prsByDay[set.dateEpochDay] = PersonalRecordEvent(
                        exerciseId = set.exerciseId,
                        exerciseName = set.exerciseName,
                        dateEpochDay = set.dateEpochDay,
                        reps = set.reps,
                        weightKg = set.weightKg,
                        estimatedOneRepMaxKg = estimated,
                    )
                }
            }
            events += prsByDay.values
        }
        return events
            .sortedWith(compareByDescending<PersonalRecordEvent> { it.dateEpochDay }.thenByDescending { it.estimatedOneRepMaxKg })
            .take(limit)
    }

    private const val EPSILON = 1e-6
}
