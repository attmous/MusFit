package com.musfit.benchmark

import androidx.test.uiautomator.StaleObjectException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class UiSnapshotRetryTest {
    @Test
    fun staleAttempts_areDiscardedBeforeReturningFinalSnapshot() {
        var captureCount = 0
        var retryCount = 0
        val finalSnapshot = listOf("final")

        val result = retryStaleUiSnapshot(
            description = "exercise resources",
            maxAttempts = 3,
            onRetry = { retryCount += 1 },
        ) {
            captureCount += 1
            if (captureCount < 3) throw StaleObjectException()
            finalSnapshot
        }

        assertSame(finalSnapshot, result)
        assertEquals(3, captureCount)
        assertEquals(2, retryCount)
    }

    @Test
    fun exhaustedStaleAttempts_reportFinalFailure() {
        var captureCount = 0
        var retryCount = 0
        val finalFailure = StaleObjectException()

        val failure = assertThrows(IllegalStateException::class.java) {
            retryStaleUiSnapshot(
                description = "exercise resources",
                maxAttempts = 3,
                onRetry = { retryCount += 1 },
            ) {
                captureCount += 1
                throw finalFailure
            }
        }

        assertEquals(
            "Could not capture exercise resources after 3 attempts because the UI hierarchy kept changing.",
            failure.message,
        )
        assertSame(finalFailure, failure.cause)
        assertEquals(3, captureCount)
        assertEquals(2, retryCount)
    }

    @Test
    fun nonStaleFailure_isNotRetried() {
        var captureCount = 0
        var retryCount = 0
        val expected = IllegalArgumentException("invalid selector")

        val failure = assertThrows(IllegalArgumentException::class.java) {
            retryStaleUiSnapshot(
                description = "exercise resources",
                maxAttempts = 3,
                onRetry = { retryCount += 1 },
            ) {
                captureCount += 1
                throw expected
            }
        }

        assertSame(expected, failure)
        assertEquals(1, captureCount)
        assertEquals(0, retryCount)
    }
}
