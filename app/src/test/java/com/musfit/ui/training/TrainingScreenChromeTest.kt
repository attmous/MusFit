package com.musfit.ui.training

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import org.junit.Assert.assertFalse
import org.junit.Test

class TrainingScreenChromeTest {
    @Test
    fun trainingTopChrome_doesNotRenderWeeklySummaryCoachPrompt() {
        val source = trainingScreenSource()

        // The Routines tab shows a clean weekly *stats* card (workouts / volume / streak with a
        // goal bar). What stays banned is the old "coach hint" prose — empty-state nudges that read
        // like coaching rather than data.
        assertFalse(source.contains("No workouts yet"))
        assertFalse(source.contains("whenever you're ready"))
    }

    private fun trainingScreenSource(): String {
        val candidates = listOf(
            Path.of("src/main/java/com/musfit/ui/training/TrainingScreen.kt"),
            Path.of("app/src/main/java/com/musfit/ui/training/TrainingScreen.kt"),
        )
        return candidates.firstOrNull(Files::exists)?.readText()
            ?: error("Could not locate TrainingScreen.kt from ${Path.of("").toAbsolutePath()}")
    }
}
