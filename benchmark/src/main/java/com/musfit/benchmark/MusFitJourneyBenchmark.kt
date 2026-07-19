package com.musfit.benchmark

import android.os.Build
import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMetricApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class MusFitJourneyBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun foodJourney() = measureDestination("Food")

    @Test
    fun trainingJourney() = measureDestination("Training")

    @Test
    fun trainingExerciseImageBrowse100Items() {
        lateinit var imageBrowsePlan: ExerciseImageBrowsePlan
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(
                FrameTimingMetric(),
                MemoryUsageMetric(MemoryUsageMetric.Mode.Max),
                TrainingExerciseImagePssMetric(),
            ),
            compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
            startupMode = null,
            // API 37 is the approved regression target. Keep API 28 in the production-shaped
            // execution matrix without adding five more long image traversals to the serial lane.
            iterations = if (Build.VERSION.SDK_INT >= 37) 5 else 1,
            setupBlock = {
                stabilizeExerciseImageBenchmarkDevice()
                launchToToday()
                openTrainingExerciseImageBrowse()
                imageBrowsePlan = warmExerciseImageBrowse()
            },
            measureBlock = {
                browseWarmedExerciseImages(imageBrowsePlan)
                traceTargetPss()
            },
        )
    }

    @Test
    fun profileJourney() = measureDestination("Profile")

    private fun measureDestination(destination: String) {
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(
                FrameTimingMetric(),
                MemoryUsageMetric(MemoryUsageMetric.Mode.Max),
            ),
            compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
            startupMode = StartupMode.WARM,
            iterations = 5,
            setupBlock = { launchToToday() },
            measureBlock = {
                visitDestination(destination)
                scrollCurrentDestination()
            },
        )
    }
}
