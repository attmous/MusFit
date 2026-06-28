package com.musfit.ui.training

import com.musfit.data.repository.ExerciseGrouping
import com.musfit.data.repository.ExerciseSummary
import com.musfit.data.repository.SupersetGroup
import com.musfit.data.repository.WorkoutExerciseBlock
import com.musfit.data.repository.WorkoutHistoryDetail
import com.musfit.data.repository.WorkoutHistorySummary
import com.musfit.data.repository.WorkoutRecapSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun workoutRecapMetricsForDisplay_formatsCoreRecapStats() {
        val metrics = workoutRecapMetricsForDisplay(
            WorkoutRecapSummary(
                durationSeconds = 2_700,
                exerciseCount = 4,
                completedSetCount = 12,
                totalVolumeKg = 8_425.5,
                personalRecordCount = 2,
                notes = "Strong finish.",
            ),
        )

        assertEquals(
            listOf(
                RecapMetric("Duration", "45m"),
                RecapMetric("Sets", "12"),
                RecapMetric("Volume", "8425.50 kg"),
                RecapMetric("Exercises", "4"),
                RecapMetric("PRs", "2"),
            ),
            metrics,
        )
    }

    @Test
    fun workoutRecapMetricsForDisplay_fallsBackToSummaryWhenRecapIsEmpty() {
        val detail = historyDetail(
            blocks = listOf(block("bench"), block("row")),
            groupings = emptyList(),
            completedSetCount = 4,
            totalVolumeKg = 500.0,
            startedAtEpochMillis = 1_000L,
            endedAtEpochMillis = 91_000L,
        )

        val metrics = workoutRecapMetricsForDisplay(detail)

        assertEquals(
            listOf(
                RecapMetric("Duration", "1m"),
                RecapMetric("Sets", "4"),
                RecapMetric("Volume", "500 kg"),
                RecapMetric("Exercises", "2"),
                RecapMetric("PRs", "0"),
            ),
            metrics,
        )
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
            sets = emptyList(),
            supersetGroupId = groupId,
            supersetLabel = label,
        )
}
