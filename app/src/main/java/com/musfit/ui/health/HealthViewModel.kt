package com.musfit.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.data.repository.HealthRepository
import com.musfit.domain.health.HealthConnectAvailability
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

data class HealthUiState(
    val availabilityLabel: String = "Unknown",
    val grantedPermissionCount: Int = 0,
    val requestablePermissionCount: Int = 0,
    val requestablePermissions: Set<String> = emptySet(),
    val canRequestPermissions: Boolean = false,
    val message: String = "Refresh status to check whether Health Connect is ready.",
)

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val repository: HealthRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(HealthUiState())
    val state: StateFlow<HealthUiState> = mutableState.asStateFlow()

    fun refreshStatus() {
        viewModelScope.launch {
            runCatching {
                val status = repository.status()
                val requestablePermissions = repository.requestablePermissions()
                val launchablePermissions = if (status.availability == HealthConnectAvailability.Available) {
                    requestablePermissions
                } else {
                    emptySet()
                }
                mutableState.update {
                    it.copy(
                        availabilityLabel = status.availability.label(),
                        grantedPermissionCount = status.grantedPermissions.size,
                        requestablePermissionCount = requestablePermissions.size,
                        requestablePermissions = launchablePermissions,
                        canRequestPermissions = status.availability == HealthConnectAvailability.Available &&
                            launchablePermissions.isNotEmpty(),
                        message = status.toMessage(requestablePermissions.size),
                    )
                }
            }.onFailure {
                mutableState.update {
                    it.copy(
                        availabilityLabel = "Unknown",
                        grantedPermissionCount = 0,
                        requestablePermissionCount = 0,
                        requestablePermissions = emptySet(),
                        canRequestPermissions = false,
                        message = "Unable to refresh Health Connect status right now. Try again from the Health tab.",
                    )
                }
            }
        }
    }

    fun importToday() {
        viewModelScope.launch {
            runCatching {
                repository.importDailySummary(LocalDate.now())
            }.onSuccess { summary ->
                mutableState.update {
                    it.copy(message = summary.importMessage())
                }
            }.onFailure { error ->
                mutableState.update {
                    it.copy(message = error.message ?: "Unable to import from Health Connect.")
                }
            }
        }
    }

    fun exportLatestWorkout() {
        viewModelScope.launch {
            runCatching {
                repository.exportLatestWorkout()
            }.onSuccess { recordId ->
                mutableState.update {
                    it.copy(
                        message = if (recordId != null) {
                            "Exported latest workout to Health Connect."
                        } else {
                            "No workout was exported. Check permissions and log a workout first."
                        },
                    )
                }
            }.onFailure { error ->
                mutableState.update {
                    it.copy(message = error.message ?: "Unable to export workout to Health Connect.")
                }
            }
        }
    }
}

private fun com.musfit.domain.health.ImportedDailyHealthSummary.importMessage(): String {
    val stepsText = steps?.let { "$it steps" } ?: "health data"
    val caloriesText = activeCaloriesKcal?.let { "${it.formatMetric()} kcal" }
    return if (caloriesText != null) {
        "Imported $stepsText and $caloriesText from Health Connect."
    } else {
        "Imported $stepsText from Health Connect."
    }
}

private fun Double.formatMetric(): String =
    if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", this)
    }

private fun HealthConnectAvailability.label(): String = when (this) {
    HealthConnectAvailability.Available -> "Available"
    HealthConnectAvailability.NotInstalled -> "Install or update required"
    HealthConnectAvailability.NotSupported -> "Not supported"
}

private fun com.musfit.domain.health.HealthConnectStatus.toMessage(
    requestablePermissionCount: Int,
): String = when (availability) {
    HealthConnectAvailability.NotInstalled ->
        "Install or update Health Connect to sync health data with MusFit."

    HealthConnectAvailability.NotSupported ->
        "Health Connect is not supported on this device."

    HealthConnectAvailability.Available -> when {
        grantedPermissions.isEmpty() ->
            "No Health Connect permissions are granted. Tap Enable Health Connect sync to choose what MusFit can access."

        grantedPermissions.size < requestablePermissionCount ->
            "Some Health Connect permissions are granted. Tap Enable Health Connect sync to review or add access."

        else ->
            "Health Connect is ready. MusFit can read and write the enabled data types."
    }
}
