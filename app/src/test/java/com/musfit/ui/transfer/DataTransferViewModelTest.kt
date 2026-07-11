package com.musfit.ui.transfer

import android.net.Uri
import com.musfit.data.transfer.DataTransferReport
import com.musfit.data.transfer.DataTransferRepository
import com.musfit.data.transfer.DataTransferReceipt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DataTransferViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun successfulExportReportsCountsAndClearsTemporarySecret() = runTest {
        val repository = FakeDataTransferRepository()
        val viewModel = DataTransferViewModel(repository)

        viewModel.export(Uri.parse("content://backup/export"), "long migration passphrase")
        advanceUntilIdle()

        assertEquals("Encrypted backup saved: 6 rows · abcdef012345", viewModel.state.value.message)
        assertFalse(viewModel.state.value.busy)
        assertTrue(repository.receivedSecret?.all { it == '\u0000' } == true)
    }

    @Test
    fun shortPassphraseIsRejectedBeforeRepositoryCall() {
        val repository = FakeDataTransferRepository()
        val viewModel = DataTransferViewModel(repository)

        viewModel.export(Uri.EMPTY, "too short")

        assertEquals("Passphrase must be at least 12 characters.", viewModel.state.value.message)
        assertEquals(0, repository.exportCalls)
    }

    @Test
    fun importFailureDoesNotRevealAuthenticationDetail() = runTest {
        val repository = FakeDataTransferRepository(failImport = true)
        val viewModel = DataTransferViewModel(repository)

        viewModel.stageImport(Uri.parse("content://backup/import"), "long migration passphrase")
        advanceUntilIdle()

        assertEquals(
            "Transfer failed. Check the backup, passphrase, and available storage.",
            viewModel.state.value.message,
        )
        assertFalse(viewModel.state.value.busy)
        assertTrue(repository.receivedSecret?.all { it == '\u0000' } == true)
    }

    private class FakeDataTransferRepository(
        private val failImport: Boolean = false,
    ) : DataTransferRepository {
        var exportCalls = 0
        var receivedSecret: CharArray? = null
        private val report = DataTransferReport(
            databaseSha256 = "abcdef012345" + "0".repeat(52),
            tableRowCounts = mapOf("foods" to 4, "workouts" to 2),
            databaseBytes = 1_024,
            archiveBytes = 1_200,
        )

        override suspend fun exportTo(uri: Uri, passphrase: CharArray): DataTransferReport {
            exportCalls += 1
            receivedSecret = passphrase
            return report
        }

        override suspend fun stageImportFrom(uri: Uri, passphrase: CharArray): DataTransferReport {
            receivedSecret = passphrase
            if (failImport) error("wrong password versus corrupt file must not escape")
            return report
        }

        override fun lastRestoreReceipt(): DataTransferReceipt? = null
        override fun lastRestoreFailure(): String? = null
    }
}
