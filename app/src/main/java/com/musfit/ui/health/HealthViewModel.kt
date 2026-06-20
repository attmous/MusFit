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
            val status = gateway.status()
            val requestablePermissions = gateway.requestablePermissions()
            mutableState.update {
                it.copy(
                    availabilityLabel = status.availability.label(),
                    grantedPermissionCount = status.grantedPermissions.size,
                    requestablePermissionCount = requestablePermissions.size,
                    message = status.toMessage(requestablePermissions.size),
                )
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
            "Permissions are not granted. Use the Health tab actions when you want to enable sync."

        grantedPermissions.size < requestablePermissionCount ->
            "Some permissions are granted. MusFit will use only the data types you allow."

        else ->
            "Health Connect is ready. MusFit can read and write the enabled data types."
    }
}
