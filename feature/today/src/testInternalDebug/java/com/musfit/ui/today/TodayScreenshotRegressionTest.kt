package com.musfit.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.musfit.domain.today.MetricSnapshot
import com.musfit.domain.today.TodayMetric
import com.musfit.ui.text.UiText
import com.musfit.ui.theme.MusFitTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w900dp-h700dp-mdpi")
class TodayScreenshotRegressionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun today_tablet_dark_ltr() {
        compose.setContent {
            ScreenshotFrame {
                Box(Modifier.fillMaxSize().padding(32.dp)) {
                    TodayVitalsGrid(
                        vitals = listOf(
                            MetricCardUiState(
                                TodayMetric.Calories,
                                UiText.Verbatim("EATEN"),
                                metricWithGoal("1,443", "of 2,450 kcal · 59%", 0.59f),
                            ),
                            MetricCardUiState(
                                TodayMetric.Steps,
                                UiText.Verbatim("STEPS"),
                                metricWithGoal("7,800", "of 10,000 · 78%", 0.78f),
                            ),
                            MetricCardUiState(
                                TodayMetric.Protein,
                                UiText.Verbatim("PROTEIN"),
                                metricWithGoal("118 g", "of 180 g · 66%", 0.66f),
                            ),
                            MetricCardUiState(
                                TodayMetric.Water,
                                UiText.Verbatim("WATER"),
                                metricWithGoal("1.8 L", "of 2.6 L · 67%", 0.67f),
                            ),
                        ),
                        onMetricClick = {},
                    )
                }
            }
        }
        compose.waitForIdle()
        assertTouchTargets()
        compose.onRoot().captureRoboImage("today-tablet-dark-ltr.png")
    }

    @Test
    @Config(qualifiers = "en-rXA-w400dp-h800dp-mdpi")
    fun todayDashboard_phone_pseudo_largeFont() {
        // Off-viewport rows have clipped semantics bounds; visible control sizing is covered by TodayComposeSemanticsTest.
        capture("today-dashboard-phone-pseudo-font-150.png", fontScale = 1.5f, verifyTouchTargets = false) {
            DashboardFixture()
        }
    }

    @Test
    @Config(qualifiers = "ar-rXB-w400dp-h800dp-mdpi")
    fun todayDashboard_phone_pseudoRtl() {
        capture("today-dashboard-phone-pseudo-rtl.png", rtl = true, verifyTouchTargets = false) {
            DashboardFixture()
        }
    }

    @Test
    @Config(qualifiers = "de-rDE-w610dp-h900dp-mdpi")
    fun todayVitals_foldable_germanNumbers() {
        capture("today-vitals-foldable-german.png") {
            Box(Modifier.fillMaxSize().padding(32.dp)) {
                TodayVitalsGrid(vitals = localizedVitals(Locale.GERMANY), onMetricClick = {})
            }
        }
    }

    private fun capture(
        fileName: String,
        rtl: Boolean = false,
        fontScale: Float = 1f,
        verifyTouchTargets: Boolean = true,
        content: @Composable () -> Unit,
    ) {
        compose.setContent {
            ScreenshotFrame(rtl = rtl, fontScale = fontScale, content = content)
        }
        compose.waitForIdle()
        if (verifyTouchTargets) assertTouchTargets()
        compose.onRoot().captureRoboImage(fileName)
    }

    private fun assertTouchTargets() {
        val minimum = with(compose.density) { 48.dp.toPx() }
        compose.onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach { node ->
            if (node.boundsInRoot.width == 0f && node.boundsInRoot.height == 0f) return@forEach
            val label = node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()
                ?: node.config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }
                ?: "<unlabelled>"
            assertTrue(
                "Touch target '$label' was ${node.boundsInRoot.width}x${node.boundsInRoot.height}",
                node.boundsInRoot.width >= minimum && node.boundsInRoot.height >= minimum,
            )
        }
    }
}

@Composable
private fun DashboardFixture() {
    DashboardEditSheet(
        state = TodayUiState(
            editPins = listOf(TodayMetric.Calories, TodayMetric.Steps, TodayMetric.Water),
            stepGoalInput = "10000",
            sessionTargetInput = "3",
        ),
        onTogglePin = {},
        onMovePin = { _, _ -> },
        onStepGoalChanged = {},
        onSessionTargetChanged = {},
        onSave = {},
        onDismiss = {},
    )
}

private fun localizedVitals(locale: Locale): List<MetricCardUiState> {
    val snapshot = MetricSnapshot(
        caloriesKcal = 1_443.0,
        calorieGoalKcal = 2_450.0,
        proteinGrams = 118.0,
        proteinGoalGrams = 180.0,
        carbsGrams = 210.0,
        carbsGoalGrams = 300.0,
        fatGrams = 65.0,
        fatGoalGrams = 80.0,
        waterMl = 1_800.0,
        waterGoalMl = 2_600.0,
        steps = 7_800L,
        stepGoal = 10_000L,
        latestWeightKg = 80.5,
        weightDeltaKg = -0.3,
        bodyFatPercent = 21.4,
        bodyFatDelta = -0.2,
        sessionsDone = 2,
        sessionTarget = 3,
        activeCaloriesKcal = 520.0,
        sleepMinutes = 455L,
        exerciseMinutes = 48L,
        exerciseSessionCount = 1,
        restingHeartRateBpm = 54L,
        loggingStreakDays = 8,
    )
    return listOf(TodayMetric.Calories, TodayMetric.Water, TodayMetric.Weight, TodayMetric.BodyFat).map { metric ->
        MetricCardUiState(
            metric = metric,
            label = metric.presentationLabel(),
            value = resolveMetricPresentation(metric, snapshot, locale),
        )
    }
}

private fun metricWithGoal(figure: String, caption: String, progress: Float) = MetricValueUiState.WithGoal(
    figure = UiText.Verbatim(figure),
    caption = UiText.Verbatim(caption),
    progress = progress,
)

@Composable
private fun ScreenshotFrame(
    rtl: Boolean = false,
    fontScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, fontScale),
        LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
        MusFitTheme(darkTheme = true) {
            Box(Modifier.fillMaxSize().background(MusFitTheme.colors.background)) { content() }
        }
    }
}
