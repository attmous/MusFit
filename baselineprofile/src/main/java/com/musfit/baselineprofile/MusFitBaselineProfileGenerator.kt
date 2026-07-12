package com.musfit.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TARGET_PACKAGE = "com.musfit"
private const val UI_TIMEOUT_MILLIS = 15_000L

@LargeTest
@RunWith(AndroidJUnit4::class)
class MusFitBaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generateAppProfile() {
        baselineProfileRule.collect(
            packageName = TARGET_PACKAGE,
            includeInStartupProfile = true,
            filterPredicate = { rule -> rule.contains("com/musfit") },
        ) {
            pressHome()
            startActivityAndWait()
            check(device.wait(Until.hasObject(By.desc("Today")), UI_TIMEOUT_MILLIS)) {
                "MusFit bottom navigation did not become ready."
            }

            listOf("Food", "Training", "Profile").forEach { destination ->
                val item = device.wait(Until.findObject(By.desc(destination)), UI_TIMEOUT_MILLIS)
                    ?: error("Destination '$destination' was not found.")
                item.click()
                device.waitForIdle()
                Thread.sleep(500)
            }
        }
    }
}
