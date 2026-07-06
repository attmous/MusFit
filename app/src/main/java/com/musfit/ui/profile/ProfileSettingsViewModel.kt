package com.musfit.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.data.repository.AccountAuthProvider
import com.musfit.data.repository.AccountRepository
import com.musfit.data.repository.AiCoachApiKeyUpdate
import com.musfit.data.repository.AiCoachProviderKind
import com.musfit.data.repository.AiCoachRepository
import com.musfit.data.repository.AiCoachSettings
import com.musfit.data.repository.AiCoachSettingsInput
import com.musfit.data.repository.DEFAULT_USER_PROFILE
import com.musfit.data.repository.ExternalAuthRepository
import com.musfit.data.repository.ExternalAccountProfile
import com.musfit.data.repository.GitHubDeviceAuthorization
import com.musfit.data.repository.HealthConnectRefreshResult
import com.musfit.data.repository.HealthRepository
import com.musfit.data.repository.LocalAgentKind
import com.musfit.data.repository.ProfileRepository
import com.musfit.data.repository.UserProfile
import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.domain.health.StepSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

private const val DEFAULT_STATUS_MESSAGE = "Refresh status to check whether Health Connect is ready."
private const val ALL_STEP_SOURCES_LABEL = "All sources (unified)"

private fun stepSourceLabel(preferredStepsPackage: String?, sources: List<StepSource>): String =
    if (preferredStepsPackage == null) {
        ALL_STEP_SOURCES_LABEL
    } else {
        sources.firstOrNull { it.packageName == preferredStepsPackage }?.label ?: preferredStepsPackage
    }

data class ProfileSettingsUiState(
    val availabilityLabel: String = "Unknown",
    val grantedPermissionCount: Int = 0,
    val requestablePermissionCount: Int = 0,
    val requestablePermissions: Set<String> = emptySet(),
    val canRequestPermissions: Boolean = false,
    val isHealthConnectSyncing: Boolean = false,
    val message: String = DEFAULT_STATUS_MESSAGE,
    val preferredStepsPackage: String? = null,
    val stepSourceLabel: String = ALL_STEP_SOURCES_LABEL,
    val stepSources: List<StepSource> = emptyList(),
    val stepSourcePickerOpen: Boolean = false,
    val account: AccountUiState = AccountUiState(),
    val accountEditorOpen: Boolean = false,
    val accountNameInput: String = "",
    val accountEmailInput: String = "",
    val accountErrorMessage: String? = null,
    val githubDeviceCode: GitHubDeviceAuthorization? = null,
    val githubSignInInProgress: Boolean = false,
    val isGitHubSignInConfigured: Boolean = false,
    val profile: UserProfile = DEFAULT_USER_PROFILE,
    val latestWeightKg: Double? = null,
    val aiCoach: AiCoachSettingsUiState = AiCoachSettingsUiState(),
    val aiCoachEditorOpen: Boolean = false,
    val aiCoachProviderInput: AiCoachProviderKind = AiCoachProviderKind.Disabled,
    val aiCoachBaseUrlInput: String = "",
    val aiCoachModelNameInput: String = "",
    val aiCoachLocalAgentInput: LocalAgentKind = LocalAgentKind.Custom,
    val aiCoachApiKeyInput: String = "",
    val aiCoachErrorMessage: String? = null,
)

data class AiCoachSettingsUiState(
    val providerKind: AiCoachProviderKind = AiCoachProviderKind.Disabled,
    val baseUrl: String = "",
    val modelName: String = "",
    val localAgentKind: LocalAgentKind = LocalAgentKind.Custom,
    val hasApiKey: Boolean = false,
    val providerLabel: String = "Off",
    val endpointLabel: String = "Not set",
    val modelLabel: String = "Not set",
    val localAgentLabel: String = "Custom local agent",
    val apiKeyLabel: String = "No API key",
)

internal data class ProviderSignInActionsUiState(
    val google: ProviderSignInActionUiState,
    val github: ProviderSignInActionUiState,
)

internal data class ProviderSignInActionUiState(
    val providerLabel: String,
    val buttonLabel: String,
    val statusLabel: String,
    val supportingText: String,
    val enabled: Boolean,
)

internal fun providerSignInActions(
    googleConfigured: Boolean,
    githubConfigured: Boolean,
    githubBusy: Boolean,
): ProviderSignInActionsUiState =
    ProviderSignInActionsUiState(
        google = ProviderSignInActionUiState(
            providerLabel = "Google",
            buttonLabel = "Connect Google",
            statusLabel = when {
                !googleConfigured -> "Setup needed"
                githubBusy -> "Waiting"
                else -> "Ready"
            },
            supportingText = when {
                !googleConfigured -> "Missing Google OAuth client ID in this build."
                githubBusy -> "Wait for GitHub to finish first."
                else -> "Links Google to this local account. MusFit still keeps your data on this device."
            },
            enabled = googleConfigured && !githubBusy,
        ),
        github = ProviderSignInActionUiState(
            providerLabel = "GitHub",
            buttonLabel = if (githubBusy) "Waiting for GitHub" else "Connect GitHub",
            statusLabel = when {
                githubBusy -> "In progress"
                !githubConfigured -> "Setup needed"
                else -> "Ready"
            },
            supportingText = when {
                githubBusy -> "Enter the code in GitHub to finish linking your local account."
                !githubConfigured -> "Missing GitHub OAuth client ID in this build."
                else -> "Uses GitHub device flow to link your local account."
            },
            enabled = githubConfigured && !githubBusy,
        ),
    )

/**
 * Only the fields the mutable base flow actually owns (Health Connect status plus the
 * shared message line). The account/editor/profile fields of [ProfileSettingsUiState]
 * are derived from other flows in the combine, so keeping them off this type means a
 * future `mutableState.update { it.copy(...) }` can never silently drop them.
 */
private data class HealthConnectState(
    val availabilityLabel: String = "Unknown",
    val grantedPermissionCount: Int = 0,
    val requestablePermissionCount: Int = 0,
    val requestablePermissions: Set<String> = emptySet(),
    val canRequestPermissions: Boolean = false,
    val isHealthConnectSyncing: Boolean = false,
    val message: String = DEFAULT_STATUS_MESSAGE,
    val preferredStepsPackage: String? = null,
    val stepSources: List<StepSource> = emptyList(),
    val stepSourcePickerOpen: Boolean = false,
    val githubDeviceCode: GitHubDeviceAuthorization? = null,
    val githubSignInInProgress: Boolean = false,
    val isGitHubSignInConfigured: Boolean = false,
)

private data class AccountEditorState(
    val open: Boolean = false,
    val nameInput: String = "",
    val emailInput: String = "",
    val errorMessage: String? = null,
)

private data class AiCoachEditorState(
    val open: Boolean = false,
    val providerKind: AiCoachProviderKind = AiCoachProviderKind.Disabled,
    val baseUrlInput: String = "",
    val modelNameInput: String = "",
    val localAgentKind: LocalAgentKind = LocalAgentKind.Custom,
    val apiKeyInput: String = "",
    val errorMessage: String? = null,
)

@HiltViewModel
class ProfileSettingsViewModel @Inject constructor(
    private val healthRepository: HealthRepository,
    private val accountRepository: AccountRepository,
    private val profileRepository: ProfileRepository,
    private val externalAuthRepository: ExternalAuthRepository,
    private val aiCoachRepository: AiCoachRepository,
) : ViewModel() {
    // Health Connect fields live in this mutable base; account/profile fields are
    // layered on top of it in the combined [state] below.
    private val mutableState = MutableStateFlow(
        HealthConnectState(isGitHubSignInConfigured = externalAuthRepository.isGitHubConfigured),
    )
    private val accountEditorFlow = MutableStateFlow(AccountEditorState())
    private val aiCoachEditorFlow = MutableStateFlow(AiCoachEditorState())

    init {
        viewModelScope.launch {
            runCatching { accountRepository.ensureActiveAccount() }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(message = error.message ?: "Could not prepare your local account.")
                    }
                }
        }
        healthRepository.observePreferredStepsPackage()
            .onEach { preferredStepsPackage ->
                mutableState.update { it.copy(preferredStepsPackage = preferredStepsPackage) }
            }
            .launchIn(viewModelScope)
    }

    private val profileState = combine(
        profileRepository.observeProfile(),
        profileRepository.observeLatestWeight(),
    ) { profile, latestWeight -> profile to latestWeight }

    private val aiCoachState = combine(
        aiCoachRepository.observeSettings(),
        aiCoachEditorFlow,
    ) { settings, editor -> settings.toUiState() to editor }

    val state: StateFlow<ProfileSettingsUiState> = combine(
        mutableState,
        accountRepository.observeActiveAccount(),
        accountEditorFlow,
        profileState,
        aiCoachState,
    ) { base, account, editor, profilePair, aiCoachPair ->
        val (profile, latestWeight) = profilePair
        val (aiCoach, aiCoachEditor) = aiCoachPair
        ProfileSettingsUiState(
            availabilityLabel = base.availabilityLabel,
            grantedPermissionCount = base.grantedPermissionCount,
            requestablePermissionCount = base.requestablePermissionCount,
            requestablePermissions = base.requestablePermissions,
            canRequestPermissions = base.canRequestPermissions,
            isHealthConnectSyncing = base.isHealthConnectSyncing,
            message = base.message,
            preferredStepsPackage = base.preferredStepsPackage,
            stepSourceLabel = stepSourceLabel(base.preferredStepsPackage, base.stepSources),
            stepSources = base.stepSources,
            stepSourcePickerOpen = base.stepSourcePickerOpen,
            account = account.toUiState(),
            accountEditorOpen = editor.open,
            accountNameInput = editor.nameInput,
            accountEmailInput = editor.emailInput,
            accountErrorMessage = editor.errorMessage,
            githubDeviceCode = base.githubDeviceCode,
            githubSignInInProgress = base.githubSignInInProgress,
            isGitHubSignInConfigured = base.isGitHubSignInConfigured,
            profile = profile,
            latestWeightKg = latestWeight?.weightKg,
            aiCoach = aiCoach,
            aiCoachEditorOpen = aiCoachEditor.open,
            aiCoachProviderInput = aiCoachEditor.providerKind,
            aiCoachBaseUrlInput = aiCoachEditor.baseUrlInput,
            aiCoachModelNameInput = aiCoachEditor.modelNameInput,
            aiCoachLocalAgentInput = aiCoachEditor.localAgentKind,
            aiCoachApiKeyInput = aiCoachEditor.apiKeyInput,
            aiCoachErrorMessage = aiCoachEditor.errorMessage,
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
                loadStepSources()
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

    fun syncRecentHealthData() {
        viewModelScope.launch {
            mutableState.update { it.copy(isHealthConnectSyncing = true) }
            runCatching {
                healthRepository.refreshRecentData(LocalDate.now())
            }.onSuccess { result ->
                mutableState.update {
                    it.copy(
                        isHealthConnectSyncing = false,
                        message = result.toMessage(),
                    )
                }
            }.onFailure { error ->
                mutableState.update {
                    it.copy(
                        isHealthConnectSyncing = false,
                        message = error.message ?: "Unable to sync Health Connect data.",
                    )
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

    fun openStepSourcePicker() {
        loadStepSources()
        mutableState.update { it.copy(stepSourcePickerOpen = true) }
    }

    fun dismissStepSourcePicker() {
        mutableState.update { it.copy(stepSourcePickerOpen = false) }
    }

    fun selectStepSource(packageName: String?) {
        viewModelScope.launch {
            mutableState.update { it.copy(stepSourcePickerOpen = false, isHealthConnectSyncing = true) }
            runCatching {
                healthRepository.setPreferredStepsPackage(packageName)
                healthRepository.refreshRecentData(LocalDate.now())
            }.onSuccess {
                mutableState.update {
                    it.copy(
                        isHealthConnectSyncing = false,
                        preferredStepsPackage = packageName,
                        message = "Updated steps source. MusFit now shows " +
                            "${stepSourceLabel(packageName, it.stepSources)}.",
                    )
                }
            }.onFailure { error ->
                mutableState.update {
                    it.copy(
                        isHealthConnectSyncing = false,
                        message = error.message ?: "Unable to update steps source.",
                    )
                }
            }
        }
    }

    private fun loadStepSources() {
        viewModelScope.launch {
            val sources = runCatching { healthRepository.readStepSources(LocalDate.now()) }
                .getOrDefault(emptyList())
            mutableState.update { it.copy(stepSources = sources) }
        }
    }

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            runCatching { profileRepository.saveProfile(profile) }
                .onFailure { error ->
                    mutableState.update { it.copy(message = error.message ?: "Could not save profile.") }
                }
        }
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

    fun signInWithProvider(profile: ExternalAccountProfile) {
        viewModelScope.launch {
            linkProviderProfile(profile)
        }
    }

    fun reportExternalSignInFailure(providerName: String, error: Throwable) {
        mutableState.update {
            it.copy(message = error.message ?: "Could not sign in with $providerName.")
        }
    }

    fun signInWithGitHub() {
        if (!externalAuthRepository.isGitHubConfigured) {
            mutableState.update { it.copy(message = "GitHub sign-in is not configured.") }
            return
        }
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    githubSignInInProgress = true,
                    githubDeviceCode = null,
                    message = "Starting GitHub sign-in.",
                )
            }
            runCatching {
                externalAuthRepository.signInWithGitHub { authorization ->
                    mutableState.update {
                        it.copy(
                            githubDeviceCode = authorization,
                            message = "Enter ${authorization.userCode} at GitHub to finish sign-in.",
                        )
                    }
                }
            }.onSuccess { profile ->
                linkProviderProfile(profile)
            }.onFailure { error ->
                mutableState.update {
                    it.copy(
                        githubSignInInProgress = false,
                        message = error.message ?: "Could not sign in with GitHub.",
                    )
                }
            }
        }
    }

    fun dismissGitHubDeviceCode() {
        mutableState.update { it.copy(githubDeviceCode = null) }
    }

    private suspend fun linkProviderProfile(profile: ExternalAccountProfile) {
        runCatching {
            accountRepository.signInWithProvider(profile)
        }.onSuccess {
            mutableState.update {
                it.copy(
                    githubSignInInProgress = false,
                    message = "Signed in with ${profile.provider.messageLabel()}.",
                )
            }
            accountEditorFlow.value = AccountEditorState()
        }.onFailure { error ->
            mutableState.update {
                it.copy(
                    githubSignInInProgress = false,
                    message = error.message ?: "Could not sign in.",
                )
            }
        }
    }

    fun openAiCoachEditor() {
        val settings = state.value.aiCoach
        aiCoachEditorFlow.value = AiCoachEditorState(
            open = true,
            providerKind = settings.providerKind,
            baseUrlInput = settings.baseUrl,
            modelNameInput = settings.modelName,
            localAgentKind = settings.localAgentKind,
        )
    }

    fun closeAiCoachEditor() {
        aiCoachEditorFlow.value = AiCoachEditorState()
    }

    fun onAiCoachProviderChanged(value: AiCoachProviderKind) {
        aiCoachEditorFlow.update { it.copy(providerKind = value, errorMessage = null) }
    }

    fun onAiCoachBaseUrlChanged(value: String) {
        aiCoachEditorFlow.update { it.copy(baseUrlInput = value, errorMessage = null) }
    }

    fun onAiCoachModelNameChanged(value: String) {
        aiCoachEditorFlow.update { it.copy(modelNameInput = value, errorMessage = null) }
    }

    fun onAiCoachLocalAgentKindChanged(value: LocalAgentKind) {
        aiCoachEditorFlow.update { it.copy(localAgentKind = value, errorMessage = null) }
    }

    fun onAiCoachApiKeyChanged(value: String) {
        aiCoachEditorFlow.update { it.copy(apiKeyInput = value, errorMessage = null) }
    }

    fun saveAiCoachSettings() {
        val editor = aiCoachEditorFlow.value
        viewModelScope.launch {
            runCatching {
                aiCoachRepository.saveSettings(
                    AiCoachSettingsInput(
                        providerKind = editor.providerKind,
                        baseUrl = editor.baseUrlInput,
                        modelName = editor.modelNameInput,
                        localAgentKind = editor.localAgentKind,
                        apiKey = if (editor.apiKeyInput.isBlank()) {
                            AiCoachApiKeyUpdate.KeepExisting
                        } else {
                            AiCoachApiKeyUpdate.Replace(editor.apiKeyInput)
                        },
                    ),
                )
            }.onSuccess {
                aiCoachEditorFlow.value = AiCoachEditorState()
                mutableState.update { it.copy(message = "AI coach setup saved.") }
            }.onFailure { error ->
                aiCoachEditorFlow.update {
                    it.copy(errorMessage = error.message ?: "Could not save AI coach setup.")
                }
            }
        }
    }

    fun clearAiCoachApiKey() {
        viewModelScope.launch {
            runCatching { aiCoachRepository.clearApiKey() }
                .onSuccess {
                    mutableState.update { it.copy(message = "AI coach API key cleared.") }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(message = error.message ?: "Could not clear AI coach API key.")
                    }
                }
        }
    }
}

private fun AccountAuthProvider.messageLabel(): String = when (this) {
    AccountAuthProvider.Local -> "local account"
    AccountAuthProvider.Google -> "Google"
    AccountAuthProvider.GitHub -> "GitHub"
}

private fun AiCoachSettings.toUiState(): AiCoachSettingsUiState =
    AiCoachSettingsUiState(
        providerKind = providerKind,
        baseUrl = baseUrl,
        modelName = modelName,
        localAgentKind = localAgentKind,
        hasApiKey = hasApiKey,
        providerLabel = providerKind.displayLabel(localAgentKind),
        endpointLabel = baseUrl.ifBlank { "Not set" },
        modelLabel = modelName.ifBlank { "Not set" },
        localAgentLabel = localAgentKind.displayLabel(),
        apiKeyLabel = if (hasApiKey) "Key saved" else "No API key",
    )

private fun AiCoachProviderKind.displayLabel(localAgentKind: LocalAgentKind): String = when (this) {
    AiCoachProviderKind.Disabled -> "Off"
    AiCoachProviderKind.OpenAiCompatible -> "API-compatible endpoint"
    AiCoachProviderKind.LocalAgent -> localAgentKind.displayLabel()
}

private fun LocalAgentKind.displayLabel(): String = when (this) {
    LocalAgentKind.OpenClaw -> "OpenClaw local agent"
    LocalAgentKind.HermesAgent -> "Hermes agent"
    LocalAgentKind.Custom -> "Custom local agent"
}

private fun com.musfit.domain.health.ImportedDailyHealthSummary.importMessage(): String {
    val stepsText = steps?.let { "$it steps" } ?: "health data"
    val caloriesText = activeCaloriesKcal?.let { "${it.formatMetric()} kcal" }
    val sleepText = sleepMinutes?.let { it.formatDuration() + " sleep" }
    val exerciseText = exerciseMinutes?.let { it.formatDuration() + " exercise" }
    val parts = listOfNotNull(stepsText, caloriesText, sleepText, exerciseText)
    return "Imported ${parts.joinForSentence()} from Health Connect."
}

private fun HealthConnectRefreshResult.toMessage(): String {
    val dayText = if (importedDayCount == 1) "1 day" else "$importedDayCount days"
    val metricText = when (bodyMetricCount) {
        0 -> null
        1 -> "1 body metric"
        else -> "$bodyMetricCount body metrics"
    }
    return "Synced ${listOfNotNull(dayText, metricText).joinForSentence()} from Health Connect."
}

private fun Double.formatMetric(): String =
    if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", this)
    }

private fun Long.formatDuration(): String {
    val hours = this / 60L
    val minutes = this % 60L
    return if (hours > 0L) {
        "${hours}h ${minutes.toString().padStart(2, '0')}m"
    } else {
        "$minutes min"
    }
}

private fun List<String>.joinForSentence(): String = when (size) {
    0 -> "health data"
    1 -> single()
    2 -> "${this[0]} and ${this[1]}"
    else -> dropLast(1).joinToString(", ") + ", and " + last()
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
