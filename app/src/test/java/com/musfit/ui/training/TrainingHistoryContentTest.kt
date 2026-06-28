package com.musfit.ui.training

import com.musfit.data.repository.ExerciseGrouping
import com.musfit.data.repository.ExerciseSummary
import com.musfit.data.repository.SupersetGroup
import com.musfit.data.repository.WorkoutExerciseBlock
import com.musfit.data.repository.WorkoutHistoryDetail
import com.musfit.data.repository.WorkoutHistorySummary
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

    private fun historyDetail(
        blocks: List<WorkoutExerciseBlock>,
        groupings: List<ExerciseGrouping>,
    ): WorkoutHistoryDetail =
        WorkoutHistoryDetail(
            summary = WorkoutHistorySummary(
                sessionId = "session-1",
                title = "Upper",
                startedAtEpochMillis = 1_000L,
                endedAtEpochMillis = 2_000L,
                completedSetCount = 0,
                totalVolumeKg = 0.0,
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
