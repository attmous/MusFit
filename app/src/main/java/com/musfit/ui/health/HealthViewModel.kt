package com.musfit.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.integrations.healthconnect.HealthConnectGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HealthUiState(
    val availabilityLabel: String = "Unknown",
    val grantedPermissionCount: Int = 0,
    val requestablePermissionCount: Int = 0,
    val requestablePermissions: Set<String> = emptySet(),
    val message: String = "Refresh status to check whether Health Connect is ready.",
)

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val gateway: HealthConnectGateway,
) : ViewModel() {
    private val mutableState = MutableStateFlow(HealthUiState())
    val state: StateFlow<HealthUiState> = mutableState.asStateFlow()

    fun refreshStatus() {
        viewModelScope.launch {
            runCatching {
                val status = gateway.status()
                val requestablePermissions = gateway.requestablePermissions()
                mutableState.update {
                    it.copy(
                        availabilityLabel = status.availability.label(),
                        grantedPermissionCount = status.grantedPermissions.size,
                        requestablePermissionCount = requestablePermissions.size,
                        requestablePermissions = requestablePermissions,
                        message = status.toMessage(requestablePermissions.size),
                    )
                }
            }.onFailure {
                mutableState.update {
                    it.copy(
                        message = "Unable to refresh Health Connect status right now. Try again from the Health tab.",
                    )
                }
            }
        }
    }
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
