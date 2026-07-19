package com.musfit.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.data.repository.AccountAuthProvider
import com.musfit.data.repository.AccountErasureRepository
import com.musfit.data.repository.AccountErasureRequest
import com.musfit.data.repository.AccountErasureResult
import com.musfit.data.repository.AccountErasureScope
import com.musfit.data.repository.AccountRepository
import com.musfit.data.repository.AiCoachApiKeyUpdate
import com.musfit.data.repository.AiCoachChatRepository
import com.musfit.data.repository.AiCoachProviderKind
import com.musfit.data.repository.AiCoachRepository
import com.musfit.data.repository.AiCoachSettings
import com.musfit.data.repository.AiCoachSettingsInput
import com.musfit.data.repository.DEFAULT_USER_PROFILE
import com.musfit.data.repository.ExternalAccountProfile
import com.musfit.data.repository.ExternalAuthRepository
import com.musfit.data.repository.FoodRepository
import com.musfit.data.repository.GitHubDeviceAuthorization
import com.musfit.data.repository.HealthConnectImportResult
import com.musfit.data.repository.HealthConnectRefreshResult
import com.musfit.data.repository.HealthRepository
import com.musfit.data.repository.LocalAgentKind
import com.musfit.data.repository.ProfileRepository
import com.musfit.data.repository.UserProfile
import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.domain.health.StepSource
import com.musfit.domain.profile.RecommendedTargets
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

private const val DEFAULT_STATUS_MESSAGE = "Refresh status to check whether Health Connect is ready."
private const val ALL_STEP_SOURCES_LABEL = "All sources (unified)"
internal const val HERMES_DEFAULT_MODEL_NAME = "hermes-agent"
private const val ACCOUNT_EDITOR_OPEN_KEY = "profile.accountEditor.open"
private const val ACCOUNT_EDITOR_NAME_KEY = "profile.accountEditor.name"
private const val ACCOUNT_EDITOR_EMAIL_KEY = "profile.accountEditor.email"
private const val AI_COACH_EDITOR_OPEN_KEY = "profile.aiCoachEditor.open"
private const val AI_COACH_EDITOR_PROVIDER_KEY = "profile.aiCoachEditor.provider"
private const val AI_COACH_EDITOR_BASE_URL_KEY = "profile.aiCoachEditor.baseUrl"
private const val AI_COACH_EDITOR_MODEL_NAME_KEY = "profile.aiCoachEditor.modelName"
private const val AI_COACH_EDITOR_LOCAL_AGENT_KEY = "profile.aiCoachEditor.localAgent"

private fun stepSourceLabel(preferredStepsPackage: String?, sources: List<StepSource>): String = if (preferredStepsPackage == null) {
    ALL_STEP_SOURCES_LABEL
} else {
    sources.firstOrNull { it.packageName == preferredStepsPackage }?.label ?: preferredStepsPackage
}

/** Lifecycle of the coach connection test — drives the 11c hero status line. */
enum class AiCoachTestState { Idle, Testing, Success, Failure }

enum class TargetApplyState { Idle, Applying, Success, Failure }

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
    val account: AccountUiState = AccountUiState(),
    val accountEditorOpen: Boolean = false,
    val accountNameInput: String = "",
    val accountEmailInput: String = "",
    val accountErrorMessage: String? = null,
    val accountErasureScope: AccountErasureScope? = null,
    val deleteAuthoredHealthRecords: Boolean = false,
    val isErasingAccountData: Boolean = false,
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
    val aiCoachMessage: String? = null,
    val isAiCoachTesting: Boolean = false,
    val aiCoachTestState: AiCoachTestState = AiCoachTestState.Idle,
    val includeBurnedCalories: Boolean = false,
    val targetApplyState: TargetApplyState = TargetApplyState.Idle,
    val targetApplyTargets: RecommendedTargets? = null,
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
): ProviderSignInActionsUiState = ProviderSignInActionsUiState(
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
    val stepSources: List<StepSource> = emptyList(),
    val githubDeviceCode: GitHubDeviceAuthorization? = null,
    val githubSignInInProgress: Boolean = false,
    val isGitHubSignInConfigured: Boolean = false,
    val aiCoachMessage: String? = null,
    val isAiCoachTesting: Boolean = false,
    val aiCoachTestState: AiCoachTestState = AiCoachTestState.Idle,
    val targetApplyState: TargetApplyState = TargetApplyState.Idle,
    val targetApplyTargets: RecommendedTargets? = null,
)

private data class ObservedProfileSettingsUiState(
    val preferredStepsPackage: String?,
    val includeBurnedCalories: Boolean,
)

private data class AccountEditorState(
    val open: Boolean = false,
    val nameInput: String = "",
    val emailInput: String = "",
    val errorMessage: String? = null,
)

private data class AccountErasureState(
    val scope: AccountErasureScope? = null,
    val deleteAuthoredHealthRecords: Boolean = false,
    val inProgress: Boolean = false,
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

private fun SavedStateHandle.restoreAccountEditor(): AccountEditorState = AccountEditorState(
    open = get<Boolean>(ACCOUNT_EDITOR_OPEN_KEY) ?: false,
    nameInput = get<String>(ACCOUNT_EDITOR_NAME_KEY).orEmpty(),
    emailInput = get<String>(ACCOUNT_EDITOR_EMAIL_KEY).orEmpty(),
)

private fun SavedStateHandle.saveAccountEditor(editor: AccountEditorState) {
    if (!editor.open) {
        remove<Boolean>(ACCOUNT_EDITOR_OPEN_KEY)
        remove<String>(ACCOUNT_EDITOR_NAME_KEY)
        remove<String>(ACCOUNT_EDITOR_EMAIL_KEY)
        return
    }
    set(ACCOUNT_EDITOR_OPEN_KEY, true)
    set(ACCOUNT_EDITOR_NAME_KEY, editor.nameInput)
    set(ACCOUNT_EDITOR_EMAIL_KEY, editor.emailInput)
}

private fun SavedStateHandle.restoreAiCoachEditor(): AiCoachEditorState {
    if (get<Boolean>(AI_COACH_EDITOR_OPEN_KEY) != true) return AiCoachEditorState()
    return AiCoachEditorState(
        open = true,
        providerKind = get<String>(AI_COACH_EDITOR_PROVIDER_KEY)
            ?.let { runCatching { AiCoachProviderKind.valueOf(it) }.getOrNull() }
            ?: AiCoachProviderKind.Disabled,
        baseUrlInput = get<String>(AI_COACH_EDITOR_BASE_URL_KEY).orEmpty(),
        modelNameInput = get<String>(AI_COACH_EDITOR_MODEL_NAME_KEY).orEmpty(),
        localAgentKind = get<String>(AI_COACH_EDITOR_LOCAL_AGENT_KEY)
            ?.let { runCatching { LocalAgentKind.valueOf(it) }.getOrNull() }
            ?: LocalAgentKind.Custom,
    )
}

private fun SavedStateHandle.saveAiCoachEditor(editor: AiCoachEditorState) {
    if (!editor.open) {
        remove<Boolean>(AI_COACH_EDITOR_OPEN_KEY)
        remove<String>(AI_COACH_EDITOR_PROVIDER_KEY)
        remove<String>(AI_COACH_EDITOR_BASE_URL_KEY)
        remove<String>(AI_COACH_EDITOR_MODEL_NAME_KEY)
        remove<String>(AI_COACH_EDITOR_LOCAL_AGENT_KEY)
        return
    }
    set(AI_COACH_EDITOR_OPEN_KEY, true)
    set(AI_COACH_EDITOR_PROVIDER_KEY, editor.providerKind.name)
    set(AI_COACH_EDITOR_BASE_URL_KEY, editor.baseUrlInput)
    set(AI_COACH_EDITOR_MODEL_NAME_KEY, editor.modelNameInput)
    set(AI_COACH_EDITOR_LOCAL_AGENT_KEY, editor.localAgentKind.name)
}

private class ProfileSettingsEditorDraftState(
    private val savedStateHandle: SavedStateHandle,
) {
    val accountEditorFlow = MutableStateFlow(savedStateHandle.restoreAccountEditor())
    val aiCoachEditorFlow = MutableStateFlow(savedStateHandle.restoreAiCoachEditor())

    fun setAccount(editor: AccountEditorState) {
        accountEditorFlow.value = editor
        savedStateHandle.saveAccountEditor(editor)
    }

    fun updateAccount(transform: (AccountEditorState) -> AccountEditorState) = setAccount(transform(accountEditorFlow.value))

    fun setAiCoach(editor: AiCoachEditorState) {
        aiCoachEditorFlow.value = editor
        savedStateHandle.saveAiCoachEditor(editor)
    }

    fun updateAiCoach(transform: (AiCoachEditorState) -> AiCoachEditorState) = setAiCoach(transform(aiCoachEditorFlow.value))
}

class AccountSettingsRepositories @Inject constructor(
    val accounts: AccountRepository,
    val erasure: AccountErasureRepository,
)

class ProfileSettingsRepositories @Inject constructor(
    val profile: ProfileRepository,
    val aiCoach: AiCoachRepository,
    val aiCoachChat: AiCoachChatRepository,
    val food: FoodRepository,
)

@HiltViewModel
class ProfileSettingsViewModel @Inject constructor(
    private val healthRepository: HealthRepository,
    accountRepositories: AccountSettingsRepositories,
    repositories: ProfileSettingsRepositories,
    private val externalAuthRepository: ExternalAuthRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val accountRepository = accountRepositories.accounts
    private val accountErasureRepository = accountRepositories.erasure
    private val profileRepository = repositories.profile
    private val aiCoachRepository = repositories.aiCoach
    private val aiCoachChatRepository = repositories.aiCoachChat
    private val foodRepository = repositories.food

    // Health Connect fields live in this mutable base; account/profile fields are
    // layered on top of it in the combined [state] below.
    private val mutableState = MutableStateFlow(
        HealthConnectState(isGitHubSignInConfigured = externalAuthRepository.isGitHubConfigured),
    )
    private val accountErasureFlow = MutableStateFlow(AccountErasureState())
    private val editorDraftState = ProfileSettingsEditorDraftState(savedStateHandle)

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

    private val profileState = combine(
        profileRepository.observeProfile(),
        profileRepository.observeLatestWeight(),
    ) { profile, latestWeight -> profile to latestWeight }

    private val aiCoachState = combine(
        aiCoachRepository.observeSettings(),
        editorDraftState.aiCoachEditorFlow,
    ) { settings, editor -> settings.toUiState() to editor }

    private val accountState = combine(
        accountRepository.observeActiveAccount(),
        editorDraftState.accountEditorFlow,
        accountErasureFlow,
    ) { account, editor, erasure -> Triple(account, editor, erasure) }

    private val observedUiState = combine(
        healthRepository.observePreferredStepsPackage(),
        foodRepository.observeFoodGoal(),
    ) { preferredStepsPackage, foodGoal ->
        ObservedProfileSettingsUiState(
            preferredStepsPackage = preferredStepsPackage,
            includeBurnedCalories = foodGoal.includeTrainingCalories,
        )
    }

    val state: StateFlow<ProfileSettingsUiState> = combine(
        mutableState,
        accountState,
        profileState,
        aiCoachState,
        observedUiState,
    ) { base, accountTriple, profilePair, aiCoachPair, observed ->
        val (account, editor, erasure) = accountTriple
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
            preferredStepsPackage = observed.preferredStepsPackage,
            stepSourceLabel = stepSourceLabel(observed.preferredStepsPackage, base.stepSources),
            stepSources = base.stepSources,
            account = account.toUiState(),
            accountEditorOpen = editor.open,
            accountNameInput = editor.nameInput,
            accountEmailInput = editor.emailInput,
            accountErrorMessage = editor.errorMessage,
            accountErasureScope = erasure.scope,
            deleteAuthoredHealthRecords = erasure.deleteAuthoredHealthRecords,
            isErasingAccountData = erasure.inProgress,
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
            aiCoachMessage = base.aiCoachMessage,
            isAiCoachTesting = base.isAiCoachTesting,
            aiCoachTestState = base.aiCoachTestState,
            includeBurnedCalories = observed.includeBurnedCalories,
            targetApplyState = base.targetApplyState,
            targetApplyTargets = base.targetApplyTargets,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = ProfileSettingsUiState(),
    )

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

    fun selectStepSource(packageName: String?) {
        if (mutableState.value.isHealthConnectSyncing) return
        mutableState.update { it.copy(isHealthConnectSyncing = true) }
        viewModelScope.launch {
            runCatching {
                healthRepository.setPreferredStepsPackage(packageName)
                healthRepository.refreshRecentData(LocalDate.now())
            }.onSuccess {
                mutableState.update {
                    it.copy(
                        isHealthConnectSyncing = false,
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

    /**
     * Master switch for the burned-calorie budget adjustment. It writes the food goal's
     * `includeTrainingCalories` flag — the same value the Food goal editor toggles — so both
     * stay in sync. When off, Health Connect burned calories are never added to "kcal left".
     */
    fun setIncludeBurnedCalories(enabled: Boolean) {
        viewModelScope.launch {
            runCatching {
                val current = foodRepository.observeFoodGoal().first()
                foodRepository.updateFoodGoal(current.copy(includeTrainingCalories = enabled))
            }.onFailure { error ->
                mutableState.update {
                    it.copy(message = error.message ?: "Could not update the burned-calorie setting.")
                }
            }
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
        editorDraftState.setAccount(
            AccountEditorState(
                open = true,
                nameInput = account.displayName,
                emailInput = account.email.orEmpty(),
            ),
        )
    }

    fun closeAccountEditor() {
        editorDraftState.setAccount(AccountEditorState())
    }

    fun openAccountErasure(scope: AccountErasureScope) {
        accountErasureFlow.value = AccountErasureState(scope = scope)
    }

    fun closeAccountErasure() {
        if (!accountErasureFlow.value.inProgress) {
            accountErasureFlow.value = AccountErasureState()
        }
    }

    fun setDeleteAuthoredHealthRecords(enabled: Boolean) {
        accountErasureFlow.update { it.copy(deleteAuthoredHealthRecords = enabled) }
    }

    fun confirmAccountErasure() {
        val pending = accountErasureFlow.value
        val scope = pending.scope ?: return
        if (pending.inProgress) return
        accountErasureFlow.update { it.copy(inProgress = true) }
        viewModelScope.launch {
            val result = runCatching {
                accountErasureRepository.erase(
                    AccountErasureRequest(scope, pending.deleteAuthoredHealthRecords),
                )
            }.getOrElse { error ->
                accountErasureFlow.update { it.copy(inProgress = false) }
                mutableState.update { it.copy(message = error.message ?: "Data erasure failed.") }
                return@launch
            }
            when (result) {
                is AccountErasureResult.Complete -> {
                    accountErasureFlow.value = AccountErasureState()
                    mutableState.update {
                        it.copy(
                            message = if (scope == AccountErasureScope.AllAccounts) {
                                "All local MusFit data was erased. A fresh local account is ready."
                            } else {
                                "The account and its local MusFit data were erased."
                            },
                        )
                    }
                }

                is AccountErasureResult.HealthCleanupFailed -> {
                    accountErasureFlow.update { it.copy(inProgress = false) }
                    mutableState.update { it.copy(message = result.message) }
                }
            }
        }
    }

    fun onAccountNameChanged(value: String) {
        editorDraftState.updateAccount { it.copy(nameInput = value, errorMessage = null) }
    }

    fun onAccountEmailChanged(value: String) {
        editorDraftState.updateAccount { it.copy(emailInput = value, errorMessage = null) }
    }

    fun saveAccount() {
        val editor = editorDraftState.accountEditorFlow.value
        if (editor.nameInput.isBlank()) {
            editorDraftState.updateAccount { it.copy(errorMessage = "Account name is required.") }
            return
        }
        viewModelScope.launch {
            runCatching {
                accountRepository.updateActiveAccount(
                    displayName = editor.nameInput.trim(),
                    email = editor.emailInput.trim().takeIf { it.isNotBlank() },
                )
            }.onSuccess {
                editorDraftState.setAccount(AccountEditorState())
            }.onFailure { error ->
                editorDraftState.updateAccount {
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
            }.rethrowCancellation().onSuccess { profile ->
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
        }.rethrowCancellation().onSuccess {
            mutableState.update {
                it.copy(
                    githubSignInInProgress = false,
                    message = "Signed in with ${profile.provider.messageLabel()}.",
                )
            }
            editorDraftState.setAccount(AccountEditorState())
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
        editorDraftState.setAiCoach(
            AiCoachEditorState(
                open = true,
                providerKind = settings.providerKind,
                baseUrlInput = settings.baseUrl,
                modelNameInput = settings.modelName,
                localAgentKind = settings.localAgentKind,
            ),
        )
    }

    fun closeAiCoachEditor() {
        editorDraftState.setAiCoach(AiCoachEditorState())
    }

    fun onAiCoachProviderChanged(value: AiCoachProviderKind) {
        editorDraftState.updateAiCoach { editor ->
            val isHermesLocal = value == AiCoachProviderKind.LocalAgent &&
                editor.localAgentKind == LocalAgentKind.HermesAgent
            editor.copy(
                providerKind = value,
                errorMessage = null,
                baseUrlInput = if (isHermesLocal && editor.baseUrlInput.isBlank()) {
                    HERMES_DEFAULT_BASE_URL
                } else {
                    editor.baseUrlInput
                },
                modelNameInput = if (isHermesLocal && editor.modelNameInput.isBlank()) {
                    HERMES_DEFAULT_MODEL_NAME
                } else {
                    editor.modelNameInput
                },
            )
        }
    }

    fun onAiCoachBaseUrlChanged(value: String) {
        editorDraftState.updateAiCoach { it.copy(baseUrlInput = value, errorMessage = null) }
    }

    fun onAiCoachModelNameChanged(value: String) {
        editorDraftState.updateAiCoach { it.copy(modelNameInput = value, errorMessage = null) }
    }

    fun onAiCoachLocalAgentKindChanged(value: LocalAgentKind) {
        editorDraftState.updateAiCoach { editor ->
            editor.copy(
                localAgentKind = value,
                errorMessage = null,
                baseUrlInput = if (value == LocalAgentKind.HermesAgent && editor.baseUrlInput.isBlank()) {
                    HERMES_DEFAULT_BASE_URL
                } else {
                    editor.baseUrlInput
                },
                modelNameInput = if (value == LocalAgentKind.HermesAgent && editor.modelNameInput.isBlank()) {
                    HERMES_DEFAULT_MODEL_NAME
                } else {
                    editor.modelNameInput
                },
            )
        }
    }

    fun onAiCoachApiKeyChanged(value: String) {
        editorDraftState.updateAiCoach { it.copy(apiKeyInput = value, errorMessage = null) }
    }

    fun saveAiCoachSettings() {
        val editor = editorDraftState.aiCoachEditorFlow.value
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
                editorDraftState.setAiCoach(AiCoachEditorState())
                // A previous test result no longer describes the new settings.
                mutableState.update {
                    it.copy(aiCoachMessage = "AI coach setup saved.", aiCoachTestState = AiCoachTestState.Idle)
                }
            }.onFailure { error ->
                editorDraftState.updateAiCoach {
                    it.copy(errorMessage = error.message ?: "Could not save AI coach setup.")
                }
            }
        }
    }

    fun clearAiCoachApiKey() {
        viewModelScope.launch {
            runCatching { aiCoachRepository.clearApiKey() }
                .onSuccess {
                    mutableState.update {
                        it.copy(aiCoachMessage = "AI coach API key cleared.", aiCoachTestState = AiCoachTestState.Idle)
                    }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(aiCoachMessage = error.message ?: "Could not clear AI coach API key.")
                    }
                }
        }
    }

    fun reportAiCoachLocalNetworkPermissionDenied(message: String) {
        mutableState.update {
            it.copy(aiCoachMessage = message)
        }
    }

    fun testAiCoachConnection() {
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    isAiCoachTesting = true,
                    aiCoachTestState = AiCoachTestState.Testing,
                    aiCoachMessage = "Testing AI coach connection...",
                )
            }
            runCatching { aiCoachChatRepository.testConnection() }
                .onSuccess {
                    mutableState.update {
                        it.copy(
                            isAiCoachTesting = false,
                            aiCoachTestState = AiCoachTestState.Success,
                            aiCoachMessage = "AI coach connection is reachable.",
                        )
                    }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(
                            isAiCoachTesting = false,
                            aiCoachTestState = AiCoachTestState.Failure,
                            aiCoachMessage = error.message ?: "AI coach connection is not reachable.",
                        )
                    }
                }
        }
    }

    /**
     * Applies the 11e sheet's live-draft targets to the Food goal, preserving the
     * goal's other fields — the settings-hosted counterpart of the old Profile-tab
     * "Apply to Food goals" action.
     */
    fun applyRecommendedTargetsToFood(targets: RecommendedTargets) {
        if (mutableState.value.targetApplyState == TargetApplyState.Applying) return
        mutableState.update {
            it.copy(
                targetApplyState = TargetApplyState.Applying,
                targetApplyTargets = targets,
            )
        }
        viewModelScope.launch {
            runCatching {
                val current = foodRepository.observeFoodGoal().first()
                foodRepository.updateFoodGoal(
                    current.copy(
                        dailyCaloriesKcal = targets.caloriesKcal,
                        proteinGrams = targets.proteinGrams,
                        carbsGrams = targets.carbsGrams,
                        fatGrams = targets.fatGrams,
                    ),
                )
            }.onSuccess {
                mutableState.update {
                    it.copy(
                        message = "Applied your targets to Food goals.",
                        targetApplyState = TargetApplyState.Success,
                        targetApplyTargets = targets,
                    )
                }
            }.onFailure { error ->
                mutableState.update {
                    it.copy(
                        message = error.message ?: "Could not apply targets to Food.",
                        targetApplyState = TargetApplyState.Failure,
                        targetApplyTargets = targets,
                    )
                }
            }
        }
    }
}

private fun <T> Result<T>.rethrowCancellation(): Result<T> = onFailure { error ->
    if (error is CancellationException) throw error
}

private fun AccountAuthProvider.messageLabel(): String = when (this) {
    AccountAuthProvider.Local -> "local account"
    AccountAuthProvider.Google -> "Google"
    AccountAuthProvider.GitHub -> "GitHub"
}

private fun AiCoachSettings.toUiState(): AiCoachSettingsUiState = AiCoachSettingsUiState(
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

private fun HealthConnectImportResult.importMessage(): String = when (this) {
    is HealthConnectImportResult.Complete -> summary.importedSummaryMessage()

    is HealthConnectImportResult.Partial ->
        "Imported available Health Connect data, but ${failures.size} metric reads failed."

    is HealthConnectImportResult.Empty -> "No Health Connect data was found for today."

    is HealthConnectImportResult.Cleared -> "Health Connect no longer has data for today; cached values were cleared."

    is HealthConnectImportResult.Unavailable -> message

    is HealthConnectImportResult.Failure -> message
}

private fun com.musfit.domain.health.ImportedDailyHealthSummary.importedSummaryMessage(): String {
    val stepsText = steps?.let { "$it steps" } ?: "health data"
    val caloriesText = activeCaloriesKcal?.let { "${it.formatMetric()} kcal" }
    val sleepText = sleepMinutes?.let { it.formatDuration() + " sleep" }
    val exerciseText = exerciseMinutes?.let { it.formatDuration() + " exercise" }
    val parts = listOfNotNull(stepsText, caloriesText, sleepText, exerciseText)
    return "Imported ${parts.joinForSentence()} from Health Connect."
}

private fun HealthConnectRefreshResult.toMessage(): String {
    if (importedDayCount == 0 && failedDayCount > 0) {
        return "Health Connect sync failed for $failedDayCount ${if (failedDayCount == 1) "day" else "days"}."
    }
    val dayText = if (importedDayCount == 1) "1 day" else "$importedDayCount days"
    val metricText = when (bodyMetricCount) {
        0 -> null
        1 -> "1 body metric"
        else -> "$bodyMetricCount body metrics"
    }
    val warningText = when {
        failedDayCount > 0 -> "$failedDayCount failed"
        partialDayCount > 0 -> "$partialDayCount partial"
        else -> null
    }
    return "Synced ${listOfNotNull(dayText, metricText, warningText).joinForSentence()} from Health Connect."
}

private fun Double.formatMetric(): String = if (this % 1.0 == 0.0) {
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
