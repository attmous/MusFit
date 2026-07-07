package com.musfit.ui.training

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingRoutineStaticThumbnailTest {
    @Test
    fun routineDetailExerciseRowsDisableAnimatedThumbnails() {
        val routineDetailRow = trainingRoutineContentSource()
            .functionBody("private fun RoutineDetailExerciseRow", "private fun RoutineProgramTag")

        assertTrue(
            "Routine detail rows should render exercise thumbnails as static images.",
            routineDetailRow.contains("animateGif = false"),
        )
    }

    @Test
    fun exerciseDetailPageKeepsAnimatedDemoGif() {
        val exerciseDetailPage = trainingScreenSource()
            .functionBody("private fun ExerciseDetailPage", "private fun MuscleChipRow")

        assertTrue(
            "Exercise detail page should keep the animated demo component.",
            exerciseDetailPage.contains("ExerciseGif("),
        )
    }

    private fun trainingRoutineContentSource(): String = sourceText(
        "src/main/java/com/musfit/ui/training/TrainingRoutineContent.kt",
        "app/src/main/java/com/musfit/ui/training/TrainingRoutineContent.kt",
    )

    private fun trainingScreenSource(): String = sourceText(
        "src/main/java/com/musfit/ui/training/TrainingScreen.kt",
        "app/src/main/java/com/musfit/ui/training/TrainingScreen.kt",
    )

    private fun sourceText(vararg candidates: String): String =
        candidates
            .map(Path::of)
            .firstOrNull(Files::exists)
            ?.readText()
            ?: error("Could not locate source from ${Path.of("").toAbsolutePath()}")

    private fun String.functionBody(startMarker: String, endMarker: String): String {
        val start = indexOf(startMarker)
        require(start >= 0) { "Missing marker: $startMarker" }
        val end = indexOf(endMarker, start)
        require(end >= 0) { "Missing marker: $endMarker" }
        return substring(start, end)
    }
}
