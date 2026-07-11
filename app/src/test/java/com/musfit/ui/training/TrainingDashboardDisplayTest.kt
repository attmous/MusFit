package com.musfit.ui.training

import com.musfit.data.repository.RoutineSummary
import com.musfit.data.repository.WorkoutHistorySummary
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class TrainingDashboardDisplayTest {
    // Wednesday 8 Jul 2026.
    private val today: LocalDate = LocalDate.of(2026, 7, 8)

    @Test
    fun trainingWeekDays_marksTrainedDaysAndToday() {
        val days = trainingWeekDays(
            history = listOf(
                workout(title = "Full Body A", date = LocalDate.of(2026, 7, 6)), // Mon
                workout(title = "Full Body B", date = LocalDate.of(2026, 7, 7)), // Tue
            ),
            today = today,
        )

        assertEquals(listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"), days.map { it.label })
        assertEquals(listOf(true, true, false, false, false, false, false), days.map { it.isDone })
        assertEquals(listOf(false, false, true, false, false, false, false), days.map { it.isToday })
        // No weekly target passed → nothing is planned.
        assertEquals(emptyList<String>(), days.filter { it.isPlanned }.map { it.label })
    }

    @Test
    fun trainingWeekDays_plansOneRestDayAfterTheLatestSession() {
        // The 8c test data: Mon done, Wed done, today Thu, target 3 → Fri planned.
        val days = trainingWeekDays(
            history = listOf(
                workout(title = "Full Body A", date = LocalDate.of(2026, 7, 6)), // Mon
                workout(title = "Full Body B", date = LocalDate.of(2026, 7, 8)), // Wed (= today)
            ),
            today = LocalDate.of(2026, 7, 9), // Thu
            weeklyTarget = 3,
        )

        assertEquals(listOf("Fri"), days.filter { it.isPlanned }.map { it.label })
    }

    @Test
    fun trainingWeekDays_planFallsForwardToTodayAndNeverIntoThePast() {
        // Only Monday trained, today Friday → Mon+2 (Wed) is in the past, plan today.
        val days = trainingWeekDays(
            history = listOf(workout(title = "Full Body A", date = LocalDate.of(2026, 7, 6))),
            today = LocalDate.of(2026, 7, 10), // Fri
            weeklyTarget = 2,
        )

        assertEquals(listOf("Fri"), days.filter { it.isPlanned }.map { it.label })
    }

    @Test
    fun trainingWeekDays_noPlanWhenTargetMetOrCandidateLeavesTheWeek() {
        val target = 2
        // Target already met → no dashed day.
        val met = trainingWeekDays(
            history = listOf(
                workout(title = "A", date = LocalDate.of(2026, 7, 6)),
                workout(title = "B", date = LocalDate.of(2026, 7, 7)),
            ),
            today = today,
            weeklyTarget = target,
        )
        assertEquals(emptyList<String>(), met.filter { it.isPlanned }.map { it.label })

        // Trained Saturday, today Sunday → Sat+2 lands next week → no dashed day.
        val overflow = trainingWeekDays(
            history = listOf(workout(title = "A", date = LocalDate.of(2026, 7, 11))),
            today = LocalDate.of(2026, 7, 12), // Sun
            weeklyTarget = target,
        )
        assertEquals(emptyList<String>(), overflow.filter { it.isPlanned }.map { it.label })
    }

    @Test
    fun trainingWeekDays_emptyWeekPlansToday() {
        val days = trainingWeekDays(history = emptyList(), today = today, weeklyTarget = 3)

        assertEquals(listOf("Wed"), days.filter { it.isPlanned }.map { it.label })
    }

    @Test
    fun trainingWeekVolumeFigure_switchesToTonnesAtOneThousandKg() {
        assertEquals("3.8 t", trainingWeekVolumeFigure(3800.0))
        assertEquals("950 kg", trainingWeekVolumeFigure(950.0))
    }

    @Test
    fun trainingHeroOverline_usesLeadMuscleGroupWhenPresent() {
        assertEquals("TODAY · QUADS DAY", trainingHeroOverline(routine(muscleGroups = listOf("quads", "glutes"))))
        assertEquals("TODAY", trainingHeroOverline(routine(muscleGroups = emptyList())))
    }

    @Test
    fun trainingHeroMeta_summarizesSizeAndDuration() {
        assertEquals("5 exercises · ~40 min", trainingHeroMeta(routine(exerciseCount = 5, targetSetCount = 13)))
    }

    @Test
    fun routineLastPerformedMeta_prefersRecencyOverSizeSummary() {
        val routine = routine(name = "Full Body A", exerciseCount = 5, targetSetCount = 13)

        assertEquals(
            "last: today",
            routineLastPerformedMeta(routine, listOf(workout("Full Body A", today)), today),
        )
        assertEquals(
            "last: yesterday",
            routineLastPerformedMeta(routine, listOf(workout("Full Body A", today.minusDays(1))), today),
        )
        assertEquals(
            "last: Fri",
            routineLastPerformedMeta(routine, listOf(workout("full body a", LocalDate.of(2026, 7, 3))), today),
        )
        assertEquals(
            "last: 26 Jun",
            routineLastPerformedMeta(routine, listOf(workout("Full Body A", LocalDate.of(2026, 6, 26))), today),
        )
        assertEquals(
            "5 exercises · ~40 min",
            routineLastPerformedMeta(routine, listOf(workout("Other Routine", today)), today),
        )
    }

    @Test
    fun trainingCoachCue_reflectsWeeklyState() {
        assertEquals(
            "No sessions yet this week — Full Body A would be a good start.",
            trainingCoachCue(TrainingHistoryOverview(currentWeekWorkoutCount = 0), "Full Body A"),
        )
        assertEquals(
            "2 of 3 sessions this week — Full Body B is up next.",
            trainingCoachCue(TrainingHistoryOverview(currentWeekWorkoutCount = 2), "Full Body B"),
        )
        assertEquals(
            "Weekly goal done — 3 of 3 sessions. Recovery counts too.",
            trainingCoachCue(TrainingHistoryOverview(currentWeekWorkoutCount = 3), "Full Body A"),
        )
        assertEquals(
            "1 of 3 sessions this week — one more keeps the plan on track.",
            trainingCoachCue(TrainingHistoryOverview(currentWeekWorkoutCount = 1), null),
        )
        // The cue honors the user's weekly session target from the Today editor.
        assertEquals(
            "2 of 4 sessions this week — Full Body B is up next.",
            trainingCoachCue(TrainingHistoryOverview(currentWeekWorkoutCount = 2), "Full Body B", weeklyTarget = 4),
        )
    }

    private fun routine(
        name: String = "Full Body A",
        exerciseCount: Int = 5,
        targetSetCount: Int = 13,
        muscleGroups: List<String> = emptyList(),
    ): RoutineSummary =
        RoutineSummary(
            id = name.lowercase().replace(' ', '-'),
            name = name,
            notes = null,
            exerciseCount = exerciseCount,
            targetSetCount = targetSetCount,
            isStarter = false,
            muscleGroups = muscleGroups,
        )

    private fun workout(title: String, date: LocalDate): WorkoutHistorySummary =
        WorkoutHistorySummary(
            sessionId = "$title-$date",
            title = title,
            startedAtEpochMillis = date.atStartOfDay(ZoneId.systemDefault()).plusHours(10).toInstant().toEpochMilli(),
            endedAtEpochMillis = null,
            completedSetCount = 10,
            totalVolumeKg = 1000.0,
        )
}
