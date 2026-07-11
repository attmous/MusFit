package com.musfit.ui.training

import com.musfit.data.repository.ExerciseGrouping
import com.musfit.data.repository.ExerciseSummary
import com.musfit.data.repository.LoggedWorkoutSetDetail
import com.musfit.data.repository.SupersetGroup
import com.musfit.data.repository.WorkoutExerciseBlock
import com.musfit.data.repository.WorkoutHistoryDetail
import com.musfit.data.repository.WorkoutHistorySummary
import com.musfit.data.repository.WorkoutRecapSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TrainingHistoryContentTest {
    @Test
    fun historyDetailGroupingsForDisplay_usesPersistedSupersetGrouping() {
        val blockA = block("bench", label = "A", groupId = "grp-1")
        val blockB = block("row", label = "B", groupId = "grp-1")
        val detail = historyDetail(
            blocks = listOf(blockA, blockB),
            groupings = listOf(ExerciseGrouping.Superset(SupersetGroup("grp-1", listOf(blockA, blockB)))),
        )

        val display = historyDetailGroupingsForDisplay(detail)

        val superset = display.filterIsInstance<ExerciseGrouping.Superset>().single()
        assertEquals("grp-1", superset.group.supersetGroupId)
        assertEquals(listOf("A", "B"), superset.group.exerciseBlocks.map { it.supersetLabel })
    }

    @Test
    fun historyDetailGroupingsForDisplay_fallsBackToFlatBlocks() {
        val detail = historyDetail(
            blocks = listOf(block("bench"), block("row")),
            groupings = emptyList(),
        )

        val display = historyDetailGroupingsForDisplay(detail)

        assertEquals(2, display.size)
        assertTrue(display.all { it is ExerciseGrouping.Single })
    }

    @Test
    fun effectiveRecap_fallsBackToSummaryWhenRecapIsEmpty() {
        val detail = historyDetail(
            blocks = listOf(block("bench"), block("row")),
            groupings = emptyList(),
            completedSetCount = 4,
            totalVolumeKg = 500.0,
            startedAtEpochMillis = 1_000L,
            endedAtEpochMillis = 91_000L,
        )

        val recap = detail.effectiveRecap()

        assertEquals(90, recap.durationSeconds)
        assertEquals(4, recap.completedSetCount)
        assertEquals(500.0, recap.totalVolumeKg, 1e-6)
        assertEquals(2, recap.exerciseCount)
        assertEquals(0, recap.personalRecordCount)
    }

    @Test
    fun workoutDurationMinutes_floorsButNeverHidesAShortSession() {
        assertEquals(41, workoutDurationMinutes(41 * 60 + 20))
        assertEquals(1, workoutDurationMinutes(45))
        assertEquals(0, workoutDurationMinutes(0))
    }

    @Test
    fun workoutVolumeChipFigure_groupsThousands() {
        assertEquals("4,120", workoutVolumeChipFigure(4_120.4))
        assertEquals("980", workoutVolumeChipFigure(980.0))
    }

    @Test
    fun workoutCompleteSubtitle_mergesDateAndTitle() {
        val startedAt = LocalDate.of(2026, 7, 2)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val subtitle = workoutCompleteSubtitle(
            WorkoutHistorySummary(
                sessionId = "s",
                title = "Full Body A",
                startedAtEpochMillis = startedAt,
                endedAtEpochMillis = null,
                completedSetCount = 0,
                totalVolumeKg = 0.0,
            ),
        )

        assertEquals("Thu 2 Jul · Full Body A", subtitle)
    }

    @Test
    fun workoutPrDisplays_reportsBestBeatingSetPerExerciseWithDelta() {
        // 100 kg × 5 → e1RM ≈ 116.7 beats the 110 prior best (~+6.7 kg).
        val prBlock = block(
            id = "squat",
            priorBest = 110.0,
            sets = listOf(
                completedSet("warm", setType = "warmup", reps = 5, weightKg = 100.0),
                completedSet("s1", reps = 5, weightKg = 100.0),
                completedSet("s2", reps = 3, weightKg = 90.0),
            ),
        )
        val noPrBlock = block(
            id = "bench",
            priorBest = 130.0,
            sets = listOf(completedSet("b1", reps = 5, weightKg = 100.0)),
        )

        val displays = workoutPrDisplays(
            historyDetail(blocks = listOf(prBlock, noPrBlock), groupings = emptyList()),
        )

        val pr = displays.single()
        assertEquals("squat", pr.exerciseName)
        assertEquals("100 kg × 5 · e1RM 116.7 kg", pr.meta)
        assertEquals("+6.7 kg", pr.deltaLabel)
    }

    @Test
    fun workoutPrDisplays_marksFirstEverLiftAsNewPrInsteadOfFullDelta() {
        val firstEver = block(
            id = "squat",
            priorBest = 0.0,
            sets = listOf(completedSet("s1", reps = 5, weightKg = 100.0)),
        )

        val displays = workoutPrDisplays(
            historyDetail(blocks = listOf(firstEver), groupings = emptyList()),
        )

        assertEquals("New PR", displays.single().deltaLabel)
    }

    @Test
    fun workoutCompleteCoachNote_prefersPrsThenSetsThenHonestFallback() {
        assertTrue(
            workoutCompleteCoachNote(WorkoutRecapSummary(personalRecordCount = 2))
                .contains("2 personal records"),
        )
        assertTrue(
            workoutCompleteCoachNote(WorkoutRecapSummary(completedSetCount = 8, durationSeconds = 600))
                .contains("8 sets in 10 min"),
        )
        assertTrue(
            workoutCompleteCoachNote(WorkoutRecapSummary()).contains("Session logged"),
        )
    }

    @Test
    fun historyMonthStats_sumsTheCalendarMonth() {
        val overview = TrainingHistoryOverview(
            monthLabel = "July",
            calendarWeeks = listOf(
                listOf(
                    null,
                    TrainingHistoryCalendarDay(LocalDate.of(2026, 7, 1), workoutCount = 1, completedSetCount = 10, totalVolumeKg = 1_500.0),
                    TrainingHistoryCalendarDay(LocalDate.of(2026, 7, 2), workoutCount = 2, completedSetCount = 20, totalVolumeKg = 2_500.0),
                ),
            ),
        )

        val (sessions, volumeKg) = historyMonthStats(overview)

        assertEquals(3, sessions)
        assertEquals(4_000.0, volumeKg, 1e-6)
    }

    @Test
    fun historyWeekSections_labelsThisAndLastWeekThenWeekOf() {
        val today = LocalDate.of(2026, 7, 11) // Saturday; week starts Mon 6 Jul.
        val zone = ZoneId.systemDefault()
        fun at(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val history = listOf(
            summary("this", at(LocalDate.of(2026, 7, 9))),
            summary("last", at(LocalDate.of(2026, 7, 1))),
            summary("older", at(LocalDate.of(2026, 6, 16))),
        )

        val sections = historyWeekSections(history, today)

        assertEquals(listOf("This week", "Last week", "Week of 15 Jun"), sections.map { it.title })
        assertEquals(listOf("this", "last", "older"), sections.flatMap { it.workouts }.map { it.sessionId })
    }

    @Test
    fun historyRowMeta_formatsDateVolumeAndDuration() {
        val startedAt = LocalDate.of(2026, 7, 2)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val meta = historyRowMeta(
            summary(
                sessionId = "s",
                startedAtEpochMillis = startedAt,
                endedAtEpochMillis = startedAt + 41 * 60 * 1000L,
                totalVolumeKg = 1_900.0,
            ),
        )

        assertEquals("Thu 2 Jul", meta.dateLabel)
        assertEquals("1.9 t", meta.volumeLabel)
        assertEquals("41 min", meta.durationLabel)

        // Sessions without an end time drop the duration part instead of showing 0.
        val openEnded = historyRowMeta(summary(sessionId = "s2", startedAtEpochMillis = startedAt))
        assertEquals(null, openEnded.durationLabel)
    }

    private fun historyDetail(
        blocks: List<WorkoutExerciseBlock>,
        groupings: List<ExerciseGrouping>,
        completedSetCount: Int = 0,
        totalVolumeKg: Double = 0.0,
        startedAtEpochMillis: Long = 1_000L,
        endedAtEpochMillis: Long? = 2_000L,
    ): WorkoutHistoryDetail =
        WorkoutHistoryDetail(
            summary = WorkoutHistorySummary(
                sessionId = "session-1",
                title = "Upper",
                startedAtEpochMillis = startedAtEpochMillis,
                endedAtEpochMillis = endedAtEpochMillis,
                completedSetCount = completedSetCount,
                totalVolumeKg = totalVolumeKg,
            ),
            exerciseBlocks = blocks,
            exerciseGroupings = groupings,
        )

    private fun block(
        id: String,
        label: String? = null,
        groupId: String? = null,
        priorBest: Double = 0.0,
        sets: List<LoggedWorkoutSetDetail> = emptyList(),
    ): WorkoutExerciseBlock =
        WorkoutExerciseBlock(
            exercise = ExerciseSummary(
                id = id,
                name = id,
                category = "strength",
                equipment = null,
                targetMuscles = "x",
                isCustom = false,
            ),
            targetReps = null,
            priorBestEstimatedOneRepMaxKg = priorBest,
            sets = sets,
            supersetGroupId = groupId,
            supersetLabel = label,
        )

    private fun completedSet(
        id: String,
        setType: String = "working",
        reps: Int,
        weightKg: Double,
    ): LoggedWorkoutSetDetail =
        LoggedWorkoutSetDetail(
            id = id,
            exerciseId = "exercise",
            setType = setType,
            reps = reps,
            weightKg = weightKg,
            rpe = null,
            notes = null,
            completed = true,
            previousLabel = null,
        )

    private fun summary(
        sessionId: String,
        startedAtEpochMillis: Long,
        endedAtEpochMillis: Long? = null,
        totalVolumeKg: Double = 0.0,
    ): WorkoutHistorySummary =
        WorkoutHistorySummary(
            sessionId = sessionId,
            title = "Workout",
            startedAtEpochMillis = startedAtEpochMillis,
            endedAtEpochMillis = endedAtEpochMillis,
            completedSetCount = 0,
            totalVolumeKg = totalVolumeKg,
        )
}
