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
import com.musfit.domain.today.MetricValue
import com.musfit.domain.today.TodayMetric
import com.musfit.ui.theme.MusFitTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

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
                                "EATEN",
                                MetricValue.WithGoal("1,443", "of 2,450 kcal · 59%", 0.59f),
                            ),
                            MetricCardUiState(
                                TodayMetric.Steps,
                                "STEPS",
                                MetricValue.WithGoal("7,800", "of 10,000 · 78%", 0.78f),
                            ),
                            MetricCardUiState(
                                TodayMetric.Protein,
                                "PROTEIN",
                                MetricValue.WithGoal("118 g", "of 180 g · 66%", 0.66f),
                            ),
                            MetricCardUiState(
                                TodayMetric.Water,
                                "WATER",
                                MetricValue.WithGoal("1.8 L", "of 2.6 L · 67%", 0.67f),
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
private fun ScreenshotFrame(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, 1f),
        LocalLayoutDirection provides LayoutDirection.Ltr,
    ) {
        MusFitTheme(darkTheme = true) {
            Box(Modifier.fillMaxSize().background(MusFitTheme.colors.background)) { content() }
        }
    }
}
