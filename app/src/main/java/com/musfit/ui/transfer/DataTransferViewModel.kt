package com.musfit.ui.transfer

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.R
import com.musfit.data.transfer.DataTransferArchiveCodec
import com.musfit.data.transfer.DataTransferReceipt
import com.musfit.data.transfer.DataTransferReport
import com.musfit.data.transfer.DataTransferRepository
import com.musfit.ui.text.UiText
import com.musfit.ui.text.pluralUiText
import com.musfit.ui.text.uiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DataTransferUiState(
    val busy: Boolean = false,
    val message: UiText? = null,
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
                message = pluralUiText(
                    R.plurals.data_transfer_export_success,
                    report.totalRows.toQuantity(),
                    UiText.Argument.LongInteger(report.totalRows),
                    UiText.Argument.Text(report.checksumLabel),
                ),
            )
        }
    }

    fun stageImport(uri: Uri, passphrase: String) = runTransfer(passphrase) { secret ->
        val report = repository.stageImportFrom(uri, secret)
        _state.update {
            it.copy(
                busy = false,
                report = report,
                message = pluralUiText(
                    R.plurals.data_transfer_import_success,
                    report.totalRows.toQuantity(),
                    UiText.Argument.LongInteger(report.totalRows),
                ),
            )
        }
    }

    private fun runTransfer(passphrase: String, operation: suspend (CharArray) -> Unit) {
        if (passphrase.length < DataTransferArchiveCodec.MINIMUM_PASSPHRASE_LENGTH) {
            _state.update { it.copy(message = uiText(R.string.data_transfer_short_passphrase)) }
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
                        message = uiText(R.string.data_transfer_failed),
                    )
                }
            } finally {
                secret.fill('\u0000')
            }
        }
    }
}

private fun Long.toQuantity(): Int = coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
