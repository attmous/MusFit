package com.musfit.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

internal const val TARGET_PACKAGE = "com.musfit"
internal const val UI_TIMEOUT_MILLIS = 15_000L

internal fun MacrobenchmarkScope.launchToToday() {
    repeat(2) { attempt ->
        pressHome()
        startActivityAndWaitWithSetupRetry()
        val today = device.wait(Until.findObject(By.desc("Today")), UI_TIMEOUT_MILLIS)
        if (today != null) {
            today.clickAction()
            device.waitForIdle()
            Thread.sleep(500)
            return
        }
        if (attempt == 0) {
            device.executeShellCommand("am force-stop $TARGET_PACKAGE")
            Thread.sleep(1_000)
        }
    }
    error("MusFit bottom navigation did not become ready after one setup retry.")
}

private fun MacrobenchmarkScope.startActivityAndWaitWithSetupRetry() {
    repeat(2) { attempt ->
        try {
            startActivityAndWait()
            return
        } catch (failure: IllegalStateException) {
            if (attempt == 1) throw failure
            pressHome()
            Thread.sleep(1_000)
        }
    }
}

internal fun MacrobenchmarkScope.visitDestination(contentDescription: String) {
    val destination = device.wait(Until.findObject(By.desc(contentDescription)), UI_TIMEOUT_MILLIS)
        ?: error("Destination '$contentDescription' was not found.")
    destination.clickAction()
    device.waitForIdle()
    Thread.sleep(500)
}

internal fun UiObject2.clickAction() {
    generateSequence(this) { node -> node.parent }
        .firstOrNull { node -> node.isClickable }
        ?.click()
        ?: click()
}

internal fun MacrobenchmarkScope.scrollCurrentDestination() {
    val width = device.displayWidth
    val height = device.displayHeight
    device.swipe(width / 2, height * 3 / 4, width / 2, height / 3, 20)
    device.waitForIdle()
}
