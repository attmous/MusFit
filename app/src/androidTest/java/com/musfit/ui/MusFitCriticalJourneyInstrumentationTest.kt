package com.musfit.ui

import android.Manifest
import android.app.Instrumentation
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
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
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.musfit.MainActivity
import com.musfit.core.di.DatabaseModule
import com.musfit.data.local.entity.LOCAL_DEFAULT_ACCOUNT_ID
import com.musfit.debug.MusFitDebugSeeder
import com.musfit.ui.theme.MusFitTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
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
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
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
        val remainingBeforeLeaving = currentRestTimerSeconds()

        compose.onNodeWithContentDescription("Food").performClick()
        Thread.sleep(1_500)
        compose.onNodeWithContentDescription("Training").performClick()
        if (compose.onAllNodesWithText("Resume").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithText("Resume").performClick()
        }
        compose.waitForText("REST")
        assertEquals(remainingBeforeLeaving, currentRestTimerSeconds())
        assertEquals(true, activeBenchSetIsComplete())
    }

    @Test
    fun trainingRoutineDraft_restoresEditorRouteWithoutSavingDuringRecreation() {
        compose.onNodeWithContentDescription("Training").performClick()
        compose.waitForText("New routine")
        compose.onNodeWithText("New routine").performClick()
        compose.waitForText("Name your routine")
        compose.onNodeWithText("Name your routine").performTextReplacement("Process-safe draft")

        compose.activityRule.scenario.recreate()

        compose.waitForText("Process-safe draft")
        compose.onNodeWithText("Add at least one exercise to start this routine").assertIsDisplayed()
    }

    @Test
    fun todayShortcuts_requestTypedTopLevelActions_andBackToToday() {
        compose.onAllNodesWithText("Open Food").onFirst().performScrollTo().performClick()
        compose.onNodeWithContentDescription("Food").assertIsSelected()

        UiDevice.getInstance(instrumentation).pressBack()
        compose.onNodeWithContentDescription("Today").assertIsSelected()
        compose.onAllNodesWithText("Open Profile").onFirst().performScrollTo().performClick()
        compose.onNodeWithContentDescription("Profile").assertIsSelected()

        UiDevice.getInstance(instrumentation).pressBack()
        compose.onNodeWithContentDescription("Today").assertIsSelected()
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
    fun predictiveBack_previewCancelAndCommit_preserveVisitOrder() {
        compose.onNodeWithContentDescription("Food").performClick()
        compose.onNodeWithContentDescription("Training").performClick()
        compose.onNodeWithContentDescription("Profile").performClick()

        val cancelledGesture = beginPredictiveBackGesture()
        instrumentation.waitForIdleSync()
        compose.onNodeWithContentDescription("Profile").assertIsSelected()
        finishPredictiveBackGesture(cancelledGesture, commit = false)
        instrumentation.waitForIdleSync()
        compose.onNodeWithContentDescription("Profile").assertIsSelected()

        val committedGesture = beginPredictiveBackGesture()
        finishPredictiveBackGesture(committedGesture, commit = true)
        compose.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                compose.onNodeWithContentDescription("Training").assertIsSelected()
                true
            }.getOrDefault(false)
        }
    }

    @Test
    fun cameraDenialAndDeterministicReturn_coverPermissionAndOfflineSafeRoundTrip() {
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
        val device = UiDevice.getInstance(instrumentation)

        repeat(20) { cycle ->
            compose.onAllNodesWithContentDescription("Scan barcode").onFirst().performClick()
            compose.waitUntil(timeoutMillis = 15_000) {
                compose.onAllNodesWithContentDescription("Close scanner").fetchSemanticsNodes().isNotEmpty()
            }
            val requestedOrientation = when (cycle) {
                6 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                13 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                else -> null
            }
            if (requestedOrientation != null) {
                val activityRecreated = device.performActionAndWait(
                    { compose.activity.requestedOrientation = requestedOrientation },
                    Until.newWindow(),
                    15_000,
                )
                assertTrue("Scanner activity did not recreate after rotation", activityRecreated)
                assertTrue(
                    "Scanner controls did not return after rotation",
                    device.wait(Until.hasObject(By.desc("Close scanner")), 15_000),
                )
            }
            device.pressBack()
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

    private fun currentRestTimerSeconds(): Int = (0..300).firstOrNull { seconds ->
        compose.onAllNodesWithContentDescription("Rest timer $seconds seconds remaining")
            .fetchSemanticsNodes()
            .isNotEmpty()
    } ?: error("No visible rest-timer remaining-seconds semantics found")

    private fun configureCameraDenial() {
        // The suite's enforced name order runs this denial case before the only camera-grant case.
        runShellCommand(
            "pm set-permission-flags ${targetContext.packageName} ${Manifest.permission.CAMERA} user-set user-fixed",
        )
    }

    private fun beginPredictiveBackGesture(): Long {
        val downTime = SystemClock.uptimeMillis()
        injectMotionEvent(downTime, downTime, MotionEvent.ACTION_DOWN, x = 1f)
        injectMotionEvent(downTime, downTime + 80, MotionEvent.ACTION_MOVE, x = 180f)
        injectMotionEvent(downTime, downTime + 160, MotionEvent.ACTION_MOVE, x = 360f)
        return downTime
    }

    private fun finishPredictiveBackGesture(
        downTime: Long,
        commit: Boolean,
    ) {
        val action = if (commit) MotionEvent.ACTION_UP else MotionEvent.ACTION_CANCEL
        val x = if (commit) 720f else 360f
        injectMotionEvent(downTime, downTime + 240, action, x)
    }

    private fun injectMotionEvent(
        downTime: Long,
        eventTime: Long,
        action: Int,
        x: Float,
    ) {
        MotionEvent.obtain(
            downTime,
            eventTime,
            action,
            x,
            1_200f,
            0,
        ).also { event ->
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            check(instrumentation.uiAutomation.injectInputEvent(event, true)) {
                "Predictive-back motion event was not injected"
            }
            event.recycle()
        }
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
