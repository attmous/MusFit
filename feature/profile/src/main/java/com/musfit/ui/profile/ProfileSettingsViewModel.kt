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
import com.musfit.feature.profile.R
import com.musfit.ui.text.LocalizedFormatter
import com.musfit.ui.text.UiText
import com.musfit.ui.text.pluralUiText
import com.musfit.ui.text.uiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
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

internal const val HERMES_DEFAULT_MODEL_NAME = "hermes-agent"
private const val ACCOUNT_EDITOR_OPEN_KEY = "profile.accountEditor.open"
private const val ACCOUNT_EDITOR_NAME_KEY = "profile.accountEditor.name"
private const val ACCOUNT_EDITOR_EMAIL_KEY = "profile.accountEditor.email"
private const val AI_COACH_EDITOR_OPEN_KEY = "profile.aiCoachEditor.open"
private const val AI_COACH_EDITOR_PROVIDER_KEY = "profile.aiCoachEditor.provider"
private const val AI_COACH_EDITOR_BASE_URL_KEY = "profile.aiCoachEditor.baseUrl"
private const val AI_COACH_EDITOR_MODEL_NAME_KEY = "profile.aiCoachEditor.modelName"
private const val AI_COACH_EDITOR_LOCAL_AGENT_KEY = "profile.aiCoachEditor.localAgent"

private fun stepSourceLabel(preferredStepsPackage: String?, sources: List<StepSource>): UiText = if (preferredStepsPackage == null) {
    uiText(R.string.profile_all_step_sources)
} else {
    UiText.Verbatim(sources.firstOrNull { it.packageName == preferredStepsPackage }?.label ?: preferredStepsPackage)
}

/** Lifecycle of the coach connection test — drives the 11c hero status line. */
enum class AiCoachTestState { Idle, Testing, Success, Failure }

enum class TargetApplyState { Idle, Applying, Success, Failure }

data class ProfileSettingsUiState(
    val availabilityLabel: UiText = uiText(R.string.profile_unknown),
    val grantedPermissionCount: Int = 0,
    val requestablePermissionCount: Int = 0,
    val requestablePermissions: Set<String> = emptySet(),
    val canRequestPermissions: Boolean = false,
    val isHealthConnectSyncing: Boolean = false,
    val message: UiText = uiText(R.string.profile_health_status_refresh),
    val preferredStepsPackage: String? = null,
    val stepSourceLabel: UiText = uiText(R.string.profile_all_step_sources),
    val stepSources: List<StepSource> = emptyList(),
    val account: AccountUiState = AccountUiState(),
    val accountEditorOpen: Boolean = false,
    val accountNameInput: String = "",
    val accountEmailInput: String = "",
    val accountErrorMessage: UiText? = null,
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
    val aiCoachErrorMessage: UiText? = null,
    val aiCoachMessage: UiText? = null,
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
    val providerLabel: UiText = uiText(R.string.profile_off),
    val endpointLabel: UiText = uiText(R.string.profile_not_set),
    val modelLabel: UiText = uiText(R.string.profile_not_set),
    val localAgentLabel: UiText = uiText(R.string.profile_custom_local_agent),
    val apiKeyLabel: UiText = uiText(R.string.profile_no_api_key),
)

internal data class ProviderSignInActionsUiState(
    val google: ProviderSignInActionUiState,
    val github: ProviderSignInActionUiState,
)

internal data class ProviderSignInActionUiState(
    val providerLabel: UiText,
    val buttonLabel: UiText,
    val statusLabel: UiText,
    val supportingText: UiText,
    val enabled: Boolean,
)

internal fun providerSignInActions(
    googleConfigured: Boolean,
    githubConfigured: Boolean,
    githubBusy: Boolean,
): ProviderSignInActionsUiState = ProviderSignInActionsUiState(
    google = ProviderSignInActionUiState(
        providerLabel = uiText(R.string.profile_google),
        buttonLabel = uiText(R.string.profile_connect_google),
        statusLabel = when {
            !googleConfigured -> uiText(R.string.profile_setup_needed)
            githubBusy -> uiText(R.string.profile_waiting)
            else -> uiText(R.string.profile_ready)
        },
        supportingText = when {
            !googleConfigured -> uiText(R.string.profile_google_missing_client)
            githubBusy -> uiText(R.string.profile_wait_github_finish)
            else -> uiText(R.string.profile_google_local_link_explanation)
        },
        enabled = googleConfigured && !githubBusy,
    ),
    github = ProviderSignInActionUiState(
        providerLabel = uiText(R.string.profile_github),
        buttonLabel = uiText(if (githubBusy) R.string.profile_waiting_github else R.string.profile_connect_github),
        statusLabel = when {
            githubBusy -> uiText(R.string.profile_in_progress)
            !githubConfigured -> uiText(R.string.profile_setup_needed)
            else -> uiText(R.string.profile_ready)
        },
        supportingText = when {
            githubBusy -> uiText(R.string.profile_github_enter_code_supporting)
            !githubConfigured -> uiText(R.string.profile_github_missing_client)
            else -> uiText(R.string.profile_github_device_flow_explanation)
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
    val availabilityLabel: UiText = uiText(R.string.profile_unknown),
    val grantedPermissionCount: Int = 0,
    val requestablePermissionCount: Int = 0,
    val requestablePermissions: Set<String> = emptySet(),
    val canRequestPermissions: Boolean = false,
    val isHealthConnectSyncing: Boolean = false,
    val message: UiText = uiText(R.string.profile_health_status_refresh),
    val stepSources: List<StepSource> = emptyList(),
    val githubDeviceCode: GitHubDeviceAuthorization? = null,
    val githubSignInInProgress: Boolean = false,
    val isGitHubSignInConfigured: Boolean = false,
    val aiCoachMessage: UiText? = null,
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
    val errorMessage: UiText? = null,
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
    val errorMessage: UiText? = null,
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
    private val healthActions = ProfileHealthSettingsActions(
        healthRepository = healthRepository,
        foodRepository = foodRepository,
        mutableState = mutableState,
        scope = viewModelScope,
    )

    init {
        viewModelScope.launch {
            runCatching { accountRepository.ensureActiveAccount() }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(message = error.messageOr(R.string.profile_error_prepare_account))
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

    fun refreshStatus() = healthActions.refreshStatus()

    fun importToday() = healthActions.importToday()

    fun syncRecentHealthData() = healthActions.syncRecentHealthData()

    fun exportLatestWorkout() = healthActions.exportLatestWorkout()

    fun selectStepSource(packageName: String?) = healthActions.selectStepSource(packageName)

    /**
     * Master switch for the burned-calorie budget adjustment. It writes the food goal's
     * `includeTrainingCalories` flag — the same value the Food goal editor toggles — so both
     * stay in sync. When off, Health Connect burned calories are never added to "kcal left".
     */
    fun setIncludeBurnedCalories(enabled: Boolean) = healthActions.setIncludeBurnedCalories(enabled)

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            runCatching { profileRepository.saveProfile(profile) }
                .onFailure { error ->
                    mutableState.update { it.copy(message = error.messageOr(R.string.profile_error_save_profile)) }
                }
        }
    }

    fun logWeight(weightKg: Double) {
        viewModelScope.launch {
            runCatching { profileRepository.logWeight(weightKg) }
                .onFailure { error ->
                    mutableState.update { it.copy(message = error.messageOr(R.string.profile_error_log_weight)) }
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
                mutableState.update { it.copy(message = error.messageOr(R.string.profile_error_data_erasure)) }
                return@launch
            }
            when (result) {
                is AccountErasureResult.Complete -> {
                    accountErasureFlow.value = AccountErasureState()
                    mutableState.update {
                        it.copy(
                            message = uiText(
                                if (scope == AccountErasureScope.AllAccounts) {
                                    R.string.profile_erased_all_data
                                } else {
                                    R.string.profile_erased_account_data
                                },
                            ),
                        )
                    }
                }

                is AccountErasureResult.HealthCleanupFailed -> {
                    accountErasureFlow.update { it.copy(inProgress = false) }
                    mutableState.update { it.copy(message = UiText.Verbatim(result.message)) }
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
            editorDraftState.updateAccount { it.copy(errorMessage = uiText(R.string.profile_account_name_required)) }
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
                    it.copy(errorMessage = error.messageOr(R.string.profile_error_save_account))
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
            it.copy(
                message = error.message?.let(UiText::Verbatim) ?: uiText(
                    R.string.profile_error_sign_in_provider,
                    UiText.Argument.Text(providerName),
                ),
            )
        }
    }

    fun signInWithGitHub() {
        if (!externalAuthRepository.isGitHubConfigured) {
            mutableState.update { it.copy(message = uiText(R.string.profile_github_not_configured)) }
            return
        }
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    githubSignInInProgress = true,
                    githubDeviceCode = null,
                    message = uiText(R.string.profile_github_starting),
                )
            }
            runCatching {
                externalAuthRepository.signInWithGitHub { authorization ->
                    mutableState.update {
                        it.copy(
                            githubDeviceCode = authorization,
                            message = uiText(
                                R.string.profile_github_enter_code,
                                UiText.Argument.Text(authorization.userCode),
                            ),
                        )
                    }
                }
            }.rethrowCancellation().onSuccess { profile ->
                linkProviderProfile(profile)
            }.onFailure { error ->
                mutableState.update {
                    it.copy(
                        githubSignInInProgress = false,
                        message = error.messageOr(R.string.profile_error_sign_in_github),
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
                    message = uiText(
                        R.string.profile_signed_in_provider,
                        UiText.Argument.Nested(profile.provider.messageLabel()),
                    ),
                )
            }
            editorDraftState.setAccount(AccountEditorState())
        }.onFailure { error ->
            mutableState.update {
                it.copy(
                    githubSignInInProgress = false,
                    message = error.messageOr(R.string.profile_error_sign_in),
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
                    it.copy(aiCoachMessage = uiText(R.string.profile_ai_saved), aiCoachTestState = AiCoachTestState.Idle)
                }
            }.onFailure { error ->
                editorDraftState.updateAiCoach {
                    it.copy(errorMessage = error.messageOr(R.string.profile_error_save_ai))
                }
            }
        }
    }

    fun clearAiCoachApiKey() {
        viewModelScope.launch {
            runCatching { aiCoachRepository.clearApiKey() }
                .onSuccess {
                    mutableState.update {
                        it.copy(aiCoachMessage = uiText(R.string.profile_ai_key_cleared), aiCoachTestState = AiCoachTestState.Idle)
                    }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(aiCoachMessage = error.messageOr(R.string.profile_error_clear_ai_key))
                    }
                }
        }
    }

    fun reportAiCoachLocalNetworkPermissionDenied(message: String) {
        mutableState.update {
            it.copy(aiCoachMessage = UiText.Verbatim(message))
        }
    }

    fun testAiCoachConnection() {
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    isAiCoachTesting = true,
                    aiCoachTestState = AiCoachTestState.Testing,
                    aiCoachMessage = uiText(R.string.profile_ai_testing),
                )
            }
            runCatching { aiCoachChatRepository.testConnection() }
                .onSuccess {
                    mutableState.update {
                        it.copy(
                            isAiCoachTesting = false,
                            aiCoachTestState = AiCoachTestState.Success,
                            aiCoachMessage = uiText(R.string.profile_ai_reachable),
                        )
                    }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(
                            isAiCoachTesting = false,
                            aiCoachTestState = AiCoachTestState.Failure,
                            aiCoachMessage = error.messageOr(R.string.profile_ai_not_reachable),
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
                        message = uiText(R.string.profile_targets_applied),
                        targetApplyState = TargetApplyState.Success,
                        targetApplyTargets = targets,
                    )
                }
            }.onFailure { error ->
                mutableState.update {
                    it.copy(
                        message = error.messageOr(R.string.profile_error_apply_targets),
                        targetApplyState = TargetApplyState.Failure,
                        targetApplyTargets = targets,
                    )
                }
            }
        }
    }
}

private class ProfileHealthSettingsActions(
    private val healthRepository: HealthRepository,
    private val foodRepository: FoodRepository,
    private val mutableState: MutableStateFlow<HealthConnectState>,
    private val scope: CoroutineScope,
) {
    fun refreshStatus() {
        scope.launch {
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
                        availabilityLabel = uiText(R.string.profile_unknown),
                        grantedPermissionCount = 0,
                        requestablePermissionCount = 0,
                        requestablePermissions = emptySet(),
                        canRequestPermissions = false,
                        message = uiText(R.string.profile_error_refresh_health_status),
                    )
                }
            }
        }
    }

    fun importToday() {
        scope.launch {
            runCatching { healthRepository.importDailySummary(LocalDate.now()) }
                .onSuccess { summary -> mutableState.update { it.copy(message = summary.importMessage()) } }
                .onFailure { error ->
                    mutableState.update { it.copy(message = error.messageOr(R.string.profile_error_import_health)) }
                }
        }
    }

    fun syncRecentHealthData() {
        scope.launch {
            mutableState.update { it.copy(isHealthConnectSyncing = true) }
            runCatching { healthRepository.refreshRecentData(LocalDate.now()) }
                .onSuccess { result ->
                    mutableState.update { it.copy(isHealthConnectSyncing = false, message = result.toMessage()) }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(
                            isHealthConnectSyncing = false,
                            message = error.messageOr(R.string.profile_error_sync_health),
                        )
                    }
                }
        }
    }

    fun exportLatestWorkout() {
        scope.launch {
            runCatching { healthRepository.exportLatestWorkout() }
                .onSuccess { recordId ->
                    val message = if (recordId != null) {
                        uiText(R.string.profile_exported_latest_workout)
                    } else {
                        uiText(R.string.profile_no_workout_exported)
                    }
                    mutableState.update { it.copy(message = message) }
                }
                .onFailure { error ->
                    mutableState.update { it.copy(message = error.messageOr(R.string.profile_error_export_workout)) }
                }
        }
    }

    fun selectStepSource(packageName: String?) {
        if (mutableState.value.isHealthConnectSyncing) return
        mutableState.update { it.copy(isHealthConnectSyncing = true) }
        scope.launch {
            runCatching {
                healthRepository.setPreferredStepsPackage(packageName)
                healthRepository.refreshRecentData(LocalDate.now())
            }.onSuccess {
                mutableState.update {
                    it.copy(
                        isHealthConnectSyncing = false,
                        message = uiText(
                            R.string.profile_steps_source_updated,
                            UiText.Argument.Nested(stepSourceLabel(packageName, it.stepSources)),
                        ),
                    )
                }
            }.onFailure { error ->
                mutableState.update {
                    it.copy(
                        isHealthConnectSyncing = false,
                        message = error.messageOr(R.string.profile_error_update_steps_source),
                    )
                }
            }
        }
    }

    fun setIncludeBurnedCalories(enabled: Boolean) {
        scope.launch {
            runCatching {
                val current = foodRepository.observeFoodGoal().first()
                foodRepository.updateFoodGoal(current.copy(includeTrainingCalories = enabled))
            }.onFailure { error ->
                mutableState.update {
                    it.copy(message = error.messageOr(R.string.profile_error_update_burned_calories))
                }
            }
        }
    }

    private fun loadStepSources() {
        scope.launch {
            val sources = runCatching { healthRepository.readStepSources(LocalDate.now()) }
                .getOrDefault(emptyList())
            mutableState.update { it.copy(stepSources = sources) }
        }
    }
}

private fun <T> Result<T>.rethrowCancellation(): Result<T> = onFailure { error ->
    if (error is CancellationException) throw error
}

private fun AccountAuthProvider.messageLabel(): UiText = when (this) {
    AccountAuthProvider.Local -> uiText(R.string.profile_local_account)
    AccountAuthProvider.Google -> uiText(R.string.profile_google)
    AccountAuthProvider.GitHub -> uiText(R.string.profile_github)
}

private fun AiCoachSettings.toUiState(): AiCoachSettingsUiState = AiCoachSettingsUiState(
    providerKind = providerKind,
    baseUrl = baseUrl,
    modelName = modelName,
    localAgentKind = localAgentKind,
    hasApiKey = hasApiKey,
    providerLabel = providerKind.displayLabel(localAgentKind),
    endpointLabel = baseUrl.takeIf(String::isNotBlank)?.let(UiText::Verbatim) ?: uiText(R.string.profile_not_set),
    modelLabel = modelName.takeIf(String::isNotBlank)?.let(UiText::Verbatim) ?: uiText(R.string.profile_not_set),
    localAgentLabel = localAgentKind.displayLabel(),
    apiKeyLabel = uiText(if (hasApiKey) R.string.profile_key_saved else R.string.profile_no_api_key),
)

private fun AiCoachProviderKind.displayLabel(localAgentKind: LocalAgentKind): UiText = when (this) {
    AiCoachProviderKind.Disabled -> uiText(R.string.profile_off)
    AiCoachProviderKind.OpenAiCompatible -> uiText(R.string.profile_api_compatible_endpoint)
    AiCoachProviderKind.LocalAgent -> localAgentKind.displayLabel()
}

private fun LocalAgentKind.displayLabel(): UiText = when (this) {
    LocalAgentKind.OpenClaw -> uiText(R.string.profile_openclaw_agent)
    LocalAgentKind.HermesAgent -> uiText(R.string.profile_hermes_agent)
    LocalAgentKind.Custom -> uiText(R.string.profile_custom_local_agent)
}

private fun HealthConnectImportResult.importMessage(): UiText = when (this) {
    is HealthConnectImportResult.Complete -> summary.importedSummaryMessage()

    is HealthConnectImportResult.Partial -> uiText(
        R.string.profile_health_import_partial,
        UiText.Argument.Text(LocalizedFormatter.integer(failures.size.toLong())),
    )

    is HealthConnectImportResult.Empty -> uiText(R.string.profile_health_import_empty)

    is HealthConnectImportResult.Cleared -> uiText(R.string.profile_health_import_cleared)

    is HealthConnectImportResult.Unavailable -> UiText.Verbatim(message)

    is HealthConnectImportResult.Failure -> UiText.Verbatim(message)
}

private fun com.musfit.domain.health.ImportedDailyHealthSummary.importedSummaryMessage(): UiText {
    val stepsText = steps?.let {
        uiText(
            R.string.profile_health_steps,
            UiText.Argument.Text(LocalizedFormatter.integer(it)),
        )
    } ?: uiText(R.string.profile_health_data)
    val caloriesText = activeCaloriesKcal?.let {
        uiText(R.string.profile_health_calories, UiText.Argument.Text(it.formatMetric()))
    }
    val sleepText = sleepMinutes?.let {
        uiText(R.string.profile_health_sleep, UiText.Argument.Nested(it.formatDuration()))
    }
    val exerciseText = exerciseMinutes?.let {
        uiText(R.string.profile_health_exercise, UiText.Argument.Nested(it.formatDuration()))
    }
    val parts = listOfNotNull(stepsText, caloriesText, sleepText, exerciseText)
    return uiText(R.string.profile_health_imported, UiText.Argument.Nested(parts.joinForSentence()))
}

private fun HealthConnectRefreshResult.toMessage(): UiText {
    if (importedDayCount == 0 && failedDayCount > 0) {
        return uiText(
            R.string.profile_health_sync_failed,
            UiText.Argument.Nested(failedDayCount.toDayCountText()),
        )
    }
    val dayText = importedDayCount.toDayCountText()
    val metricText = when (bodyMetricCount) {
        0 -> null

        else -> pluralUiText(
            R.plurals.profile_health_body_metrics,
            bodyMetricCount,
            UiText.Argument.Text(LocalizedFormatter.integer(bodyMetricCount.toLong())),
        )
    }
    val warningText = when {
        failedDayCount > 0 -> uiText(
            R.string.profile_health_warning_failed,
            UiText.Argument.Text(LocalizedFormatter.integer(failedDayCount.toLong())),
        )

        partialDayCount > 0 -> uiText(
            R.string.profile_health_warning_partial,
            UiText.Argument.Text(LocalizedFormatter.integer(partialDayCount.toLong())),
        )

        else -> null
    }
    return uiText(
        R.string.profile_health_synced,
        UiText.Argument.Nested(listOfNotNull(dayText, metricText, warningText).joinForSentence()),
    )
}

private fun Int.toDayCountText(): UiText = pluralUiText(
    R.plurals.profile_health_days,
    this,
    UiText.Argument.Text(LocalizedFormatter.integer(toLong())),
)

private fun Double.formatMetric(locale: Locale = Locale.getDefault()): String = LocalizedFormatter.number(this, maximumFractionDigits = 1, locale = locale)

private fun Long.formatDuration(): UiText {
    val hours = this / 60L
    val minutes = this % 60L
    return if (hours > 0L) {
        uiText(
            R.string.profile_duration_hours_minutes,
            UiText.Argument.Text(LocalizedFormatter.integer(hours)),
            UiText.Argument.Text(LocalizedFormatter.integer(minutes)),
        )
    } else {
        pluralUiText(
            R.plurals.profile_duration_minutes,
            minutes.toInt(),
            UiText.Argument.Text(LocalizedFormatter.integer(minutes)),
        )
    }
}

private fun List<UiText>.joinForSentence(): UiText = when (size) {
    0 -> uiText(R.string.profile_health_data)

    1 -> single()

    2 -> uiText(
        R.string.profile_join_and,
        UiText.Argument.Nested(this[0]),
        UiText.Argument.Nested(this[1]),
    )

    else -> uiText(
        R.string.profile_join_comma_and,
        UiText.Argument.Nested(
            dropLast(1).reduce { accumulated, next ->
                uiText(
                    R.string.profile_join_comma,
                    UiText.Argument.Nested(accumulated),
                    UiText.Argument.Nested(next),
                )
            },
        ),
        UiText.Argument.Nested(last()),
    )
}

private fun HealthConnectAvailability.label(): UiText = when (this) {
    HealthConnectAvailability.Available -> uiText(R.string.profile_health_available)
    HealthConnectAvailability.NotInstalled -> uiText(R.string.profile_health_install_required)
    HealthConnectAvailability.NotSupported -> uiText(R.string.profile_health_not_supported)
}

private fun com.musfit.domain.health.HealthConnectStatus.toMessage(
    requestablePermissionCount: Int,
): UiText = when (availability) {
    HealthConnectAvailability.NotInstalled ->
        uiText(R.string.profile_health_install_message)

    HealthConnectAvailability.NotSupported ->
        uiText(R.string.profile_health_not_supported_message)

    HealthConnectAvailability.Available -> when {
        grantedPermissions.isEmpty() ->
            uiText(R.string.profile_health_no_permissions)

        grantedPermissions.size < requestablePermissionCount ->
            uiText(R.string.profile_health_some_permissions)

        else ->
            uiText(R.string.profile_health_ready_message)
    }
}
