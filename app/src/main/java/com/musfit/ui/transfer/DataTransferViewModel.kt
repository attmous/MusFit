package com.musfit.ui.transfer

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.data.transfer.DataTransferArchiveCodec
import com.musfit.data.transfer.DataTransferReport
import com.musfit.data.transfer.DataTransferRepository
import com.musfit.data.transfer.DataTransferReceipt
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

data class DataTransferUiState(
    val busy: Boolean = false,
    val message: String? = null,
    val report: DataTransferReport? = null,
    val lastReceipt: DataTransferReceipt? = null,
    val lastRestoreFailure: String? = null,
)

@HiltViewModel
class DataTransferViewModel @Inject constructor(
    private val repository: DataTransferRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(
        DataTransferUiState(
            lastReceipt = repository.lastRestoreReceipt(),
            lastRestoreFailure = repository.lastRestoreFailure(),
        ),
    )
    val state: StateFlow<DataTransferUiState> = _state.asStateFlow()

    fun export(uri: Uri, passphrase: String) = runTransfer(passphrase) { secret ->
        val report = repository.exportTo(uri, secret)
        _state.update {
            it.copy(
                busy = false,
                report = report,
                message = "Encrypted backup saved: ${report.totalRows} rows · ${report.checksumLabel}",
            )
        }
    }

    fun stageImport(uri: Uri, passphrase: String) = runTransfer(passphrase) { secret ->
        val report = repository.stageImportFrom(uri, secret)
        _state.update {
            it.copy(
                busy = false,
                report = report,
                message = "Backup verified and staged. Fully close and reopen MusFit to finish restoring ${report.totalRows} rows.",
            )
        }
    }

    private fun runTransfer(passphrase: String, operation: suspend (CharArray) -> Unit) {
        if (passphrase.length < DataTransferArchiveCodec.MINIMUM_PASSPHRASE_LENGTH) {
            _state.update { it.copy(message = "Passphrase must be at least 12 characters.") }
            return
        }
        _state.update { it.copy(busy = true, message = null, report = null) }
        viewModelScope.launch {
            val secret = passphrase.toCharArray()
            try {
                operation(secret)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                _state.update {
                    it.copy(
                        busy = false,
                        message = "Transfer failed. Check the backup, passphrase, and available storage.",
                    )
                }
            } finally {
                secret.fill('\u0000')
            }
        }
    }
}
