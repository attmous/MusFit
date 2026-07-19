package com.musfit.benchmark

import androidx.test.uiautomator.StaleObjectException

private const val UI_SNAPSHOT_MAX_ATTEMPTS = 5
internal const val UI_SNAPSHOT_RETRY_DELAY_MILLIS = 25L

internal fun <T> retryStaleUiSnapshot(
    description: String,
    maxAttempts: Int = UI_SNAPSHOT_MAX_ATTEMPTS,
    onRetry: () -> Unit = { Thread.sleep(UI_SNAPSHOT_RETRY_DELAY_MILLIS) },
    capture: () -> T,
): T {
    require(maxAttempts > 0) { "maxAttempts must be positive." }
    var lastFailure: StaleObjectException? = null
    repeat(maxAttempts) { attempt ->
        try {
            return capture()
        } catch (failure: StaleObjectException) {
            lastFailure = failure
            if (attempt < maxAttempts - 1) {
                onRetry()
            }
        }
    }
    throw IllegalStateException(
        "Could not capture $description after $maxAttempts attempts because the UI hierarchy kept changing.",
        lastFailure,
    )
}
