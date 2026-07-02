package com.musfit.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.data.repository.AccountRepository
import com.musfit.data.repository.DEFAULT_USER_PROFILE
import com.musfit.data.repository.HealthRepository
import com.musfit.data.repository.ProfileRepository
import com.musfit.data.repository.UserProfile
import com.musfit.domain.health.HealthConnectAvailability
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

data class ProfileSettingsUiState(
    val availabilityLabel: String = "Unknown",
    val grantedPermissionCount: Int = 0,
    val requestablePermissionCount: Int = 0,
    val requestablePermissions: Set<String> = emptySet(),
    val canRequestPermissions: Boolean = false,
    val message: String = "Refresh status to check whether Health Connect is ready.",
    val account: AccountUiState = AccountUiState(),
    val accountEditorOpen: Boolean = false,
    val accountNameInput: String = "",
    val accountEmailInput: String = "",
    val accountErrorMessage: String? = null,
    val profile: UserProfile = DEFAULT_USER_PROFILE,
    val latestWeightKg: Double? = null,
)

private data class AccountEditorState(
    val open: Boolean = false,
    val nameInput: String = "",
    val emailInput: String = "",
    val errorMessage: String? = null,
)

@HiltViewModel
class ProfileSettingsViewModel @Inject constructor(
    private val healthRepository: HealthRepository,
    private val accountRepository: AccountRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {
    // Health Connect fields live in this mutable base; account/profile fields are
    // layered on top of it in the combined [state] below.
    private val mutableState = MutableStateFlow(ProfileSettingsUiState())
    private val accountEditorFlow = MutableStateFlow(AccountEditorState())

    init {
        viewModelScope.launch {
            runCatching { accountRepository.ensureActiveAccount() }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(message = error.message ?: "Could not prepare your local account.")
                    }
                }
        }
    }

    val state: StateFlow<ProfileSettingsUiState> = combine(
        mutableState,
        accountRepository.observeActiveAccount(),
        accountEditorFlow,
        profileRepository.observeProfile(),
        profileRepository.observeLatestWeight(),
    ) { base, account, editor, profile, latestWeight ->
        base.copy(
            account = account.toUiState(),
            accountEditorOpen = editor.open,
            accountNameInput = editor.nameInput,
            accountEmailInput = editor.emailInput,
            accountErrorMessage = editor.errorMessage,
            profile = profile,
            latestWeightKg = latestWeight?.weightKg,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ProfileSettingsUiState())

    fun refreshStatus() {
        viewModelScope.launch {
            runCatching {
                val status = healthRepository.status()
                val requestablePermissions = healthRepository.requestablePermissions()
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
                        message = "Unable to refresh Health Connect status right now. Try again from the Profile tab.",
                    )
                }
            }
        }
    }

    fun importToday() {
        viewModelScope.launch {
            runCatching {
                healthRepository.importDailySummary(LocalDate.now())
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
                healthRepository.exportLatestWorkout()
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

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch { profileRepository.saveProfile(profile) }
    }

    fun logWeight(weightKg: Double) {
        viewModelScope.launch {
            runCatching { profileRepository.logWeight(weightKg) }
                .onFailure { error ->
                    mutableState.update { it.copy(message = error.message ?: "Could not log weight.") }
                }
        }
    }

    fun openAccountEditor() {
        val account = state.value.account
        accountEditorFlow.value = AccountEditorState(
            open = true,
            nameInput = account.displayName,
            emailInput = account.email.orEmpty(),
        )
    }

    fun closeAccountEditor() {
        accountEditorFlow.value = AccountEditorState()
    }

    fun onAccountNameChanged(value: String) {
        accountEditorFlow.update { it.copy(nameInput = value, errorMessage = null) }
    }

    fun onAccountEmailChanged(value: String) {
        accountEditorFlow.update { it.copy(emailInput = value, errorMessage = null) }
    }

    fun saveAccount() {
        val editor = accountEditorFlow.value
        if (editor.nameInput.isBlank()) {
            accountEditorFlow.update { it.copy(errorMessage = "Account name is required.") }
            return
        }
        viewModelScope.launch {
            runCatching {
                accountRepository.updateActiveAccount(
                    displayName = editor.nameInput.trim(),
                    email = editor.emailInput.trim().takeIf { it.isNotBlank() },
                )
            }.onSuccess {
                accountEditorFlow.value = AccountEditorState()
            }.onFailure { error ->
                accountEditorFlow.update {
                    it.copy(errorMessage = error.message ?: "Could not save account.")
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
