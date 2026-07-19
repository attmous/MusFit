package com.musfit.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

private const val EXERCISE_BROWSE_SCROLL_STEPS = 24
private const val EXERCISE_BROWSE_IDLE_TIMEOUT_MILLIS = 100L
private const val TRAINING_HOME_STEP_TIMEOUT_MILLIS = 2_000L
private const val TRAINING_HOME_MAX_BACK_COUNT = 4

internal fun MacrobenchmarkScope.returnToTrainingHome(): UiObject2 {
    repeat(TRAINING_HOME_MAX_BACK_COUNT) {
        device.wait(Until.findObject(By.text("New routine")), TRAINING_HOME_STEP_TIMEOUT_MILLIS)
            ?.let { return it }
        device.pressBack()
        device.waitForIdle(EXERCISE_BROWSE_IDLE_TIMEOUT_MILLIS)
        Thread.sleep(250)
    }
    return device.wait(Until.findObject(By.text("New routine")), UI_TIMEOUT_MILLIS)
        ?: error("Training home did not expose 'New routine'.")
}

internal fun MacrobenchmarkScope.clickText(text: String) {
    val node = device.wait(Until.findObject(By.text(text)), UI_TIMEOUT_MILLIS)
        ?: error("Text '$text' was not found.")
    node.clickAction()
    device.waitForIdle()
}

internal fun MacrobenchmarkScope.waitForText(text: String) {
    checkNotNull(device.wait(Until.findObject(By.text(text)), UI_TIMEOUT_MILLIS)) {
        "Text '$text' did not appear."
    }
}

internal fun MacrobenchmarkScope.waitForDescription(description: String) {
    checkNotNull(device.wait(Until.findObject(By.desc(description)), UI_TIMEOUT_MILLIS)) {
        "Content description '$description' did not appear."
    }
}

internal fun MacrobenchmarkScope.findExerciseBrowseList(): UiObject2 = device.wait(Until.findObject(By.scrollable(true)), UI_TIMEOUT_MILLIS)
    ?: error("The exercise browse list did not become scrollable.")

internal fun MacrobenchmarkScope.waitForExerciseCatalog() {
    check(device.wait(Until.hasObject(By.res(exerciseCatalogReadyResource)), UI_TIMEOUT_MILLIS)) {
        "The exercise catalog did not finish importing its ds-0001 gate record; " +
            "nodes=${visibleExerciseNodeSnapshot()}."
    }
}

internal fun MacrobenchmarkScope.rewindExerciseImageBrowse(
    swipeCount: Int,
    firstPageAnchorExerciseId: String,
) {
    repeat(swipeCount + 1) {
        swipeExerciseBrowse(down = false)
    }
    device.waitForIdle(EXERCISE_BROWSE_IDLE_TIMEOUT_MILLIS)
    Thread.sleep(500)
    val visibleExerciseIds = visibleExerciseRowIds()
    check(firstPageAnchorExerciseId in visibleExerciseIds) {
        "Exercise browse did not return to its first-page anchor $firstPageAnchorExerciseId."
    }
}

internal fun MacrobenchmarkScope.swipeExerciseBrowse(down: Boolean) {
    val bounds = findExerciseBrowseList().visibleBounds
    val horizontalCenter = bounds.centerX()
    val upperY = bounds.top + bounds.height() / 5
    val lowerY = bounds.bottom - bounds.height() / 5
    val startY = if (down) lowerY else upperY
    val endY = if (down) upperY else lowerY
    check(device.swipe(horizontalCenter, startY, horizontalCenter, endY, EXERCISE_BROWSE_SCROLL_STEPS)) {
        "Could not ${if (down) "advance" else "rewind"} the exercise browse list."
    }
    device.waitForIdle(EXERCISE_BROWSE_IDLE_TIMEOUT_MILLIS)
}
