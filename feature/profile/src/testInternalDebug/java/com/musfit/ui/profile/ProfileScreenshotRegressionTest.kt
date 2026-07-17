package com.musfit.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.github.takahirom.roborazzi.captureRoboImage
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
        compose.setContent {
            ScreenshotFrame {
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
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("profile-foldable-light-ltr.png")
    }
}

@Composable
private fun ScreenshotFrame(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, 1f),
        LocalLayoutDirection provides LayoutDirection.Ltr,
    ) {
        MusFitTheme(darkTheme = false) {
            Box(Modifier.fillMaxSize().background(MusFitTheme.colors.background)) { content() }
        }
    }
}
