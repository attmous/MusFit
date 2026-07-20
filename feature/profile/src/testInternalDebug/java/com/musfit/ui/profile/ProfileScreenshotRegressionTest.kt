package com.musfit.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.musfit.data.repository.DEFAULT_USER_PROFILE
import com.musfit.domain.profile.GoalType
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccentRole
import com.musfit.ui.theme.tabAccentFor
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w610dp-h900dp-mdpi")
class ProfileScreenshotRegressionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun profile_foldable_light_ltr() {
        capture("profile-foldable-light-ltr.png") {
            SettingsFixture()
        }
    }

    @Test
    @Config(qualifiers = "en-rXA-w400dp-h800dp-mdpi")
    fun profileEditor_phone_pseudo_largeFont() {
        capture("profile-editor-phone-pseudo-font-150.png", fontScale = 1.5f) {
            ProfileEditSheet(
                initial = DEFAULT_USER_PROFILE,
                initialWeightKg = 80.0,
                onDismiss = {},
                onSave = { _, _ -> },
            )
        }
    }

    @Test
    @Config(qualifiers = "en-rXA-w400dp-h800dp-mdpi")
    fun profileWeightHero_phone_pseudo_largeFont() {
        capture("profile-weight-hero-phone-pseudo-font-150.png", fontScale = 1.5f) {
            Box(Modifier.padding(16.dp)) {
                WeightHeroCardPreview(
                    state = ProfileUiState(
                        profile = DEFAULT_USER_PROFILE.copy(goalType = GoalType.Gain, goalPaceKgPerWeek = 0.2),
                        hero = WeightHeroState(
                            latestWeightKg = 80.5,
                            deltaKg = 0.5,
                            goalWeightKg = 82.0,
                            goalProgressFraction = 0.5,
                            bmi = 25.0,
                            chartSeries = listOf(79.5, 80.0, 80.5),
                            hasAnyEntry = true,
                        ),
                    ),
                    accent = tabAccentFor(TabAccentRole.Profile),
                    onOpenEntries = {},
                    onLogWeight = {},
                )
            }
        }
    }

    @Test
    @Config(qualifiers = "ar-rXB-w400dp-h800dp-mdpi")
    fun profileSettings_phone_pseudoRtl() {
        capture("profile-settings-phone-pseudo-rtl.png", rtl = true) {
            SettingsFixture()
        }
    }

    @Test
    @Config(qualifiers = "de-rDE-w610dp-h900dp-mdpi")
    fun profileEditor_foldable_german() {
        capture("profile-editor-foldable-german.png") {
            ProfileEditSheet(
                initial = DEFAULT_USER_PROFILE.copy(goalType = GoalType.Gain, goalPaceKgPerWeek = 0.3),
                initialWeightKg = 80.5,
                onDismiss = {},
                onSave = { _, _ -> },
            )
        }
    }

    private fun capture(
        fileName: String,
        rtl: Boolean = false,
        fontScale: Float = 1f,
        content: @Composable () -> Unit,
    ) {
        compose.setContent {
            ScreenshotFrame(rtl = rtl, fontScale = fontScale, content = content)
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage(fileName)
    }
}

@Composable
private fun SettingsFixture() {
    SettingsHub(
        state = ProfileSettingsUiState(),
        accent = tabAccentFor(TabAccentRole.Profile),
        onBack = {},
        onEditAccount = {},
        onOpenProfileDetails = {},
        onGoogleSignIn = {},
        onGitHubSignIn = {},
        onOpenAiCoach = {},
        onOpenHealthConnect = {},
        onOpenDataTransfer = {},
        onIncludeBurnedCaloriesChange = {},
        googleSignInConfigured = false,
        versionName = "test",
    )
}

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
        MusFitTheme(darkTheme = false) {
            Box(Modifier.fillMaxSize().background(MusFitTheme.colors.background)) { content() }
        }
    }
}
