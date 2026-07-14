package com.musfit.ui

import android.Manifest
import android.app.Instrumentation
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import androidx.activity.compose.setContent
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.musfit.MainActivity
import com.musfit.core.di.DatabaseModule
import com.musfit.data.local.entity.LOCAL_DEFAULT_ACCOUNT_ID
import com.musfit.debug.MusFitDebugSeeder
import com.musfit.ui.theme.MusFitTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

/**
 * Small framework-owned journey layer. Lower-level state and callback behavior stays in unit and
 * Robolectric tests; these cases prove the real Activity, Hilt graph, Room, permission dialog, and
 * saved-instance-state boundaries on managed Android devices.
 */
@RunWith(AndroidJUnit4::class)
class MusFitCriticalJourneyInstrumentationTest {

    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
    private val targetContext = instrumentation.targetContext

    private val deterministicSeed = TestRule { base, _ ->
        object : Statement() {
            override fun evaluate() {
                runBlocking {
                    MusFitDebugSeeder.create(targetContext).use { seeder -> seeder.seed(reset = true) }
                }
                base.evaluate()
            }
        }
    }

    val compose = createAndroidComposeRule<MainActivity>()

    private val failureArtifacts = object : TestWatcher() {
        override fun failed(error: Throwable?, description: Description) {
            val output = failureOutputDirectory()
            output.mkdirs()
            val stem = description.methodName.replace(Regex("[^A-Za-z0-9_.-]"), "_")
            runCatching {
                UiDevice.getInstance(instrumentation)
                    .dumpWindowHierarchy(File(output, "$stem-ui-tree.xml"))
            }
            runCatching {
                instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
                    FileOutputStream(File(output, "$stem-screenshot.png")).use { stream ->
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                    }
                }
            }
        }
    }

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(deterministicSeed)
        .around(compose)
        .around(failureArtifacts)

    @Test
    fun foodQuickLog_persistsThroughActivityRecreation() {
        val before = todayDiaryRowCount()

        compose.onNodeWithContentDescription("Food").performClick()
        compose.waitForText("Food")
        compose.onAllNodesWithContentDescription("Add to Breakfast").onFirst().performClick()
        compose.onNodeWithContentDescription("More actions").performClick()
        compose.onNodeWithText("Quick track").performClick()
        compose.onNode(hasText("Protein shake", substring = true)).performScrollTo().performClick()
        compose.onNode(hasText("220 kcal", substring = true)).performClick()

        compose.waitUntil(timeoutMillis = 10_000) { todayDiaryRowCount() == before + 1 }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(timeoutMillis = 10_000) { todayDiaryRowCount() == before + 1 }
        compose.onNodeWithContentDescription("Food").assertIsSelected()
    }

    @Test
    fun foodDatabaseQuery_restoresWithoutWritingDuringRecreation() {
        compose.onNodeWithContentDescription("Food").performClick()
        compose.onNodeWithContentDescription("More actions").performClick()
        compose.onNodeWithText("Food database").performClick()
        compose.waitForText("Food database")
        compose.onNodeWithText("Search foods").performTextReplacement("zzzunknown")

        compose.activityRule.scenario.recreate()

        compose.waitForText("Food database")
        compose.onNodeWithText("zzzunknown").assertIsDisplayed()
    }

    @Test
    fun activeWorkout_setAndRestStateSurviveRecreation_andTickerStopsOffScreen() {
        compose.onNodeWithContentDescription("Training").performClick()
        compose.waitForText("Resume")
        compose.onNodeWithText("Resume").performClick()
        compose.waitForText("Finish")
        compose.onAllNodesWithContentDescription("Mark complete").onFirst()
            .performScrollTo()
            .performClick()
        compose.waitForText("REST")
        compose.waitUntil(timeoutMillis = 10_000) { activeBenchSetIsComplete() }

        compose.activityRule.scenario.recreate()
        compose.waitForText("REST")
        assertEquals(true, activeBenchSetIsComplete())

        compose.onNodeWithContentDescription("Food").performClick()
        Thread.sleep(1_500)
        compose.onNodeWithContentDescription("Training").performClick()
        if (compose.onAllNodesWithText("Resume").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithText("Resume").performClick()
        }
        compose.waitForText("REST")
    }

    @Test
    fun profileSettingAndVisitOrder_persistAcrossRecreationAndBack() {
        compose.onNodeWithContentDescription("Food").performClick()
        compose.onNodeWithContentDescription("Training").performClick()
        compose.onNodeWithContentDescription("Profile").performClick()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.waitForText("Add burned calories to budget")

        val toggle = compose.onAllNodes(isToggleable()).onFirst()
        val initiallyOn = runCatching {
            toggle.assertIsOn()
            true
        }.getOrDefault(false)
        toggle.performScrollTo().performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                val updated = compose.onAllNodes(isToggleable()).onFirst()
                if (initiallyOn) updated.assertIsOff() else updated.assertIsOn()
                true
            }.getOrDefault(false)
        }
        compose.activityRule.scenario.recreate()
        compose.waitForText("Add burned calories to budget")
        if (initiallyOn) {
            compose.onAllNodes(isToggleable()).onFirst().assertIsOff()
        } else {
            compose.onAllNodes(isToggleable()).onFirst().assertIsOn()
        }

        UiDevice.getInstance(instrumentation).pressBack()
        compose.onNodeWithContentDescription("Profile").assertIsSelected()
        UiDevice.getInstance(instrumentation).pressBack()
        compose.onNodeWithContentDescription("Training").assertIsSelected()
        UiDevice.getInstance(instrumentation).pressBack()
        compose.onNodeWithContentDescription("Food").assertIsSelected()
    }

    @Test
    fun profileEditorDraft_restoresSettingsRouteWithoutSavingDuringRecreation() {
        compose.onNodeWithContentDescription("Profile").performClick()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Profile details").performScrollTo().performClick()
        compose.waitForText("Your profile")
        compose.onNodeWithContentDescription("Height, cm").performTextReplacement("181")

        compose.activityRule.scenario.recreate()

        compose.waitForText("Your profile")
        compose.onNodeWithContentDescription("Height, cm").assertTextEquals("181")
    }

    @Test
    fun scannerDenialAndDeterministicReturn_coverPermissionAndOfflineSafeRoundTrip() {
        if (Build.VERSION.SDK_INT > 28) {
            configureCameraDenial()
            compose.onNodeWithContentDescription("Food").performClick()
            compose.onAllNodesWithContentDescription("Add to Breakfast").onFirst().performClick()
            compose.onAllNodesWithContentDescription("Scan barcode").onFirst().performClick()
            compose.waitForText("Camera permission is required to scan barcodes.")
            UiDevice.getInstance(instrumentation).pressBack()
        }
        compose.activity.setContent {
            MusFitTheme {
                AppNavGraph(
                    barcodeScannerContent = { onBarcodeDetected, _ ->
                        Button(onClick = { onBarcodeDetected(TEST_BARCODE) }) {
                            Text("Return deterministic barcode")
                        }
                    },
                )
            }
        }
        compose.onNodeWithContentDescription("Food").performClick()
        compose.onAllNodesWithContentDescription("Add to Breakfast").onFirst().performClick()
        compose.onAllNodesWithContentDescription("Scan barcode").onFirst().performClick()
        val controlsNetwork = Build.VERSION.SDK_INT > 28
        if (controlsNetwork) setAirplaneMode(enabled = true)
        try {
            compose.onNodeWithText("Return deterministic barcode").performClick()
            compose.waitUntil(timeoutMillis = 15_000) {
                compose.onAllNodesWithText("Create").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Create").assertIsDisplayed()
            compose.onNodeWithText("Amount (g)").assertIsDisplayed()
        } finally {
            if (controlsNetwork) setAirplaneMode(enabled = false)
        }
    }

    @Test
    fun cameraScanner_rapidOpenBackAndRotate_twentyCyclesWithoutRetainedSession() {
        runShellCommand("pm grant ${targetContext.packageName} ${Manifest.permission.CAMERA}")
        compose.onNodeWithContentDescription("Food").performClick()
        compose.onAllNodesWithContentDescription("Add to Breakfast").onFirst().performClick()

        repeat(20) { cycle ->
            compose.onAllNodesWithContentDescription("Scan barcode").onFirst().performClick()
            compose.waitUntil(timeoutMillis = 15_000) {
                compose.onAllNodesWithContentDescription("Close scanner").fetchSemanticsNodes().isNotEmpty()
            }
            when (cycle) {
                6 -> compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                13 -> compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            compose.waitForIdle()
            UiDevice.getInstance(instrumentation).pressBack()
            compose.waitUntil(timeoutMillis = 15_000) {
                compose.onAllNodesWithContentDescription("Scan barcode").fetchSemanticsNodes().isNotEmpty()
            }
        }

        compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        compose.waitUntil(timeoutMillis = 15_000) { activeCameraClients().isEmpty() }
        val cameraDump = activeCameraClients()
        assertTrue(
            "Camera service retained an active client after the scanner route closed:\n$cameraDump",
            cameraDump.isEmpty(),
        )
    }

    private fun todayDiaryRowCount(): Int = runBlocking {
        val database = DatabaseModule.provideDatabase(targetContext)
        try {
            database.foodDao().getFoodDiaryEntryRowsForDate(LOCAL_DEFAULT_ACCOUNT_ID, LocalDate.now().toEpochDay()).size
        } finally {
            database.close()
        }
    }

    private fun activeBenchSetIsComplete(): Boolean = runBlocking {
        val database = DatabaseModule.provideDatabase(targetContext)
        try {
            database.trainingDao().getWorkoutSet(LOCAL_DEFAULT_ACCOUNT_ID, "debug-set-active-bench-1")?.completed == true
        } finally {
            database.close()
        }
    }

    private fun configureCameraDenial() {
        // Orchestrator clears package data before every case, so CAMERA is already denied. Mark
        // that denial fixed without revoking a live instrumentation process (which kills API 28).
        runShellCommand(
            "pm set-permission-flags ${targetContext.packageName} ${Manifest.permission.CAMERA} user-set user-fixed",
        )
    }

    private fun setAirplaneMode(enabled: Boolean) {
        val value = if (enabled) "1" else "0"
        runShellCommand(
            "settings put global airplane_mode_on $value",
        )
        runShellCommand(
            "am broadcast -a android.intent.action.AIRPLANE_MODE --ez state $enabled",
        )
    }

    private fun runShellCommand(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(
        instrumentation.uiAutomation.executeShellCommand(command),
    ).use { stream -> stream.readBytes().decodeToString() }

    private fun activeCameraClients(): String = runShellCommand("dumpsys media.camera")
        .substringAfter("Active Camera Clients:", missingDelimiterValue = "unavailable")
        .substringBefore("Allowed user IDs:")
        .trim()
        .takeUnless { it == "[]" }
        .orEmpty()

    private fun failureOutputDirectory(): File {
        val managedOutput = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        return if (managedOutput.isNullOrBlank()) {
            File(targetContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "critical-journey-failures")
        } else {
            File(managedOutput)
        }
    }

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.waitForText(
        text: String,
        substring: Boolean = false,
    ) {
        waitUntil(timeoutMillis = 15_000) {
            onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val TEST_BARCODE = "900000000001"
    }
}
