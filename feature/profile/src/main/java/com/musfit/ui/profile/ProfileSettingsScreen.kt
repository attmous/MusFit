package com.musfit.ui.profile

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.health.connect.client.PermissionController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musfit.data.repository.AccountAuthProvider
import com.musfit.data.repository.AccountErasureScope
import com.musfit.data.repository.AiCoachProviderKind
import com.musfit.data.repository.UserProfile
import com.musfit.feature.profile.R
import com.musfit.ui.components.ExpressiveBadge
import com.musfit.ui.components.ExpressiveBadgeShape
import com.musfit.ui.components.InnerScreenHeader
import com.musfit.ui.components.groupedShape
import com.musfit.ui.icons.outlined.Code
import com.musfit.ui.icons.outlined.DeleteForever
import com.musfit.ui.icons.outlined.Link
import com.musfit.ui.icons.outlined.SwapHoriz
import com.musfit.ui.text.LocalizedFormatter
import com.musfit.ui.text.asString
import com.musfit.ui.text.uiText
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import com.musfit.ui.theme.TabAccentRole
import com.musfit.ui.theme.tabAccentFor
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period

/** Inner settings surfaces reached from the 11b hub. */
private enum class SettingsSubPage { AiCoach, HealthConnect, DataPrivacy }

data class ProfileSettingsEntryConfig(
    val googleWebClientId: String,
    val versionName: String,
    val localNetworkPermission: String,
    val localNetworkPermissionDeniedMessage: String,
    val requiresLocalNetworkPermission: (String) -> Boolean,
    val hasLocalNetworkPermission: (Context) -> Boolean,
)

@Composable
fun ProfileSettingsScreen(
    onBack: () -> Unit,
    onOpenDataTransfer: () -> Unit,
    entryConfig: ProfileSettingsEntryConfig,
    viewModel: ProfileSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val accent = tabAccentFor(TabAccentRole.Profile)
    val context = LocalContext.current
    val googleLabel = stringResource(R.string.profile_google)
    val githubLabel = stringResource(R.string.profile_github)
    val coroutineScope = rememberCoroutineScope()
    var subPage by rememberSaveable { mutableStateOf<SettingsSubPage?>(null) }
    var showProfileSheet by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { viewModel.refreshStatus() }
    val localNetworkPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.testAiCoachConnection()
        } else {
            viewModel.reportAiCoachLocalNetworkPermissionDenied(entryConfig.localNetworkPermissionDeniedMessage)
        }
    }
    val onTestAiCoach = aiCoachTestAction(
        requiresPermission = entryConfig.requiresLocalNetworkPermission(state.aiCoach.baseUrl),
        permissionGranted = entryConfig.hasLocalNetworkPermission(context),
        requestPermission = { localNetworkPermissionLauncher.launch(entryConfig.localNetworkPermission) },
        testConnection = viewModel::testAiCoachConnection,
    )

    LaunchedEffect(Unit) {
        viewModel.refreshStatus()
        viewModel.syncRecentHealthData()
    }

    // A system/gesture back on an inner settings page returns to the hub instead
    // of popping the whole settings route.
    SettingsBackHandler(subPage) { subPage = null }

    when (subPage) {
        SettingsSubPage.AiCoach -> AiCoachSettingsPage(
            state = state,
            accent = accent,
            onBack = { subPage = null },
            onEdit = viewModel::openAiCoachEditor,
            onClearApiKey = viewModel::clearAiCoachApiKey,
            onTestConnection = onTestAiCoach,
        )

        SettingsSubPage.HealthConnect -> HealthConnectSettingsPage(
            state = state,
            accent = accent,
            onBack = { subPage = null },
            onRequestPermissions = {
                if (state.canRequestPermissions) permissionLauncher.launch(state.requestablePermissions)
            },
            onRefresh = viewModel::refreshStatus,
            onSync = viewModel::syncRecentHealthData,
            onExport = viewModel::exportLatestWorkout,
            onSelectStepSource = viewModel::selectStepSource,
        )

        SettingsSubPage.DataPrivacy -> DataPrivacySettingsPage(
            accent = accent,
            onBack = { subPage = null },
            onOpenDataTransfer = onOpenDataTransfer,
            onDeleteAccount = { viewModel.openAccountErasure(AccountErasureScope.ActiveAccount) },
            onDeleteAllData = { viewModel.openAccountErasure(AccountErasureScope.AllAccounts) },
        )

        null -> SettingsHub(
            state = state,
            accent = accent,
            onBack = onBack,
            onEditAccount = viewModel::openAccountEditor,
            onOpenProfileDetails = { showProfileSheet = true },
            onGoogleSignIn = {
                coroutineScope.launch {
                    runCatching {
                        signInWithGoogle(
                            context = context,
                            googleWebClientId = entryConfig.googleWebClientId,
                        )
                    }.onSuccess(viewModel::signInWithProvider)
                        .onFailure { viewModel.reportExternalSignInFailure(googleLabel, it) }
                }
            },
            onGitHubSignIn = viewModel::signInWithGitHub,
            onOpenAiCoach = { subPage = SettingsSubPage.AiCoach },
            onOpenHealthConnect = { subPage = SettingsSubPage.HealthConnect },
            onOpenDataTransfer = {
                subPage = SettingsSubPage.DataPrivacy
            },
            onIncludeBurnedCaloriesChange = viewModel::setIncludeBurnedCalories,
            googleSignInConfigured = entryConfig.googleWebClientId.isNotBlank(),
            versionName = entryConfig.versionName,
        )
    }

    if (showProfileSheet) {
        ProfileEditSheet(
            initial = state.profile,
            initialWeightKg = state.latestWeightKg,
            targetApplyState = state.targetApplyState,
            targetApplyTargets = state.targetApplyTargets,
            onDismiss = { showProfileSheet = false },
            onSave = { profile, weightKg ->
                viewModel.saveProfile(profile)
                weightKg?.let(viewModel::logWeight)
                showProfileSheet = false
            },
            onApplyTargets = viewModel::applyRecommendedTargetsToFood,
        )
    }
    if (state.accountEditorOpen) {
        AccountEditSheet(
            name = state.accountNameInput,
            email = state.accountEmailInput,
            error = state.accountErrorMessage,
            accent = accent,
            onNameChange = viewModel::onAccountNameChanged,
            onEmailChange = viewModel::onAccountEmailChanged,
            onDismiss = viewModel::closeAccountEditor,
            onSave = viewModel::saveAccount,
        )
    }
    state.accountErasureScope?.let { scope ->
        AccountErasureDialog(
            state = state,
            onDeleteAuthoredHealthRecordsChange = viewModel::setDeleteAuthoredHealthRecords,
            onDismiss = viewModel::closeAccountErasure,
            onConfirm = viewModel::confirmAccountErasure,
        )
    }
    if (state.aiCoachEditorOpen) {
        AiCoachEditorSheet(
            provider = state.aiCoachProviderInput,
            baseUrl = state.aiCoachBaseUrlInput,
            modelName = state.aiCoachModelNameInput,
            localAgentKind = state.aiCoachLocalAgentInput,
            apiKey = state.aiCoachApiKeyInput,
            hasSavedApiKey = state.aiCoach.hasApiKey,
            error = state.aiCoachErrorMessage,
            accent = accent,
            onProviderChange = viewModel::onAiCoachProviderChanged,
            onBaseUrlChange = viewModel::onAiCoachBaseUrlChanged,
            onModelNameChange = viewModel::onAiCoachModelNameChanged,
            onLocalAgentKindChange = viewModel::onAiCoachLocalAgentKindChanged,
            onApiKeyChange = viewModel::onAiCoachApiKeyChanged,
            onDismiss = viewModel::closeAiCoachEditor,
            onSave = viewModel::saveAiCoachSettings,
        )
    }
    val githubDeviceCode = state.githubDeviceCode
    if (githubDeviceCode != null && state.githubSignInInProgress) {
        GitHubDeviceCodeDialog(
            userCode = githubDeviceCode.userCode,
            verificationUri = githubDeviceCode.verificationUri,
            onOpen = {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, githubDeviceCode.verificationUri.toUri()),
                    )
                }.onFailure { viewModel.reportExternalSignInFailure(githubLabel, it) }
            },
            onDismiss = viewModel::dismissGitHubDeviceCode,
        )
    }
}

@Composable
private fun SettingsBackHandler(subPage: SettingsSubPage?, onBack: () -> Unit) {
    BackHandler(enabled = subPage != null, onBack = onBack)
}

private fun aiCoachTestAction(
    requiresPermission: Boolean,
    permissionGranted: Boolean,
    requestPermission: () -> Unit,
    testConnection: () -> Unit,
): () -> Unit = {
    if (requiresPermission && !permissionGranted) requestPermission() else testConnection()
}

/**
 * The 11b settings hub: local-account hero, then Account / Connections /
 * Preferences / About grouped lists. AI coach and Health Connect are launcher
 * rows into their own inner pages.
 */
@Composable
internal fun SettingsHub(
    state: ProfileSettingsUiState,
    accent: TabAccent,
    onBack: () -> Unit,
    onEditAccount: () -> Unit,
    onOpenProfileDetails: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onGitHubSignIn: () -> Unit,
    onOpenAiCoach: () -> Unit,
    onOpenHealthConnect: () -> Unit,
    onOpenDataTransfer: () -> Unit,
    onIncludeBurnedCaloriesChange: (Boolean) -> Unit,
    googleSignInConfigured: Boolean,
    versionName: String,
) {
    val addBurnedDescription = stringResource(R.string.profile_add_burned_calories)
    val signInActions = providerSignInActions(
        googleConfigured = googleSignInConfigured,
        githubConfigured = state.isGitHubSignInConfigured,
        githubBusy = state.githubSignInInProgress,
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MusFitTheme.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        InnerScreenHeader(
            title = stringResource(R.string.profile_settings_title),
            subtitle = stringResource(R.string.profile_settings_subtitle),
            onBack = onBack,
        )

        LocalAccountHero(account = state.account, accent = accent, onEdit = onEditAccount)

        GroupLabel(stringResource(R.string.profile_account))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ProfileHubRow(
                title = stringResource(R.string.profile_details),
                subtitle = profileDetailsSummary(state.profile),
                shape = groupedShape(0, 4),
                onClick = onOpenProfileDetails,
                leading = {
                    ExpressiveBadge(
                        icon = Icons.Outlined.Person,
                        shape = ExpressiveBadgeShape.Sunny,
                        containerColor = accent.container,
                        contentColor = accent.onContainer,
                        size = 40.dp,
                        iconSize = 19.dp,
                    )
                },
            )
            ProviderRow(
                title = stringResource(R.string.profile_google),
                linked = state.account.provider == AccountAuthProvider.Google,
                action = signInActions.google,
                accent = accent,
                shape = groupedShape(1, 4),
                badgeShape = ExpressiveBadgeShape.Circle,
                badgeIcon = Icons.Outlined.Link,
                linkedSubtitle = state.account.email ?: stringResource(R.string.profile_linked_local_account),
                onClick = onGoogleSignIn,
            )
            ProviderRow(
                title = stringResource(R.string.profile_github),
                linked = state.account.provider == AccountAuthProvider.GitHub,
                action = signInActions.github,
                accent = accent,
                shape = groupedShape(2, 4),
                badgeShape = ExpressiveBadgeShape.Squircle,
                badgeIcon = Icons.Outlined.Code,
                linkedSubtitle = state.account.email ?: stringResource(R.string.profile_linked_local_account),
                onClick = onGitHubSignIn,
            )
            ProfileHubRow(
                title = stringResource(R.string.profile_data_privacy),
                subtitle = stringResource(R.string.profile_data_privacy_summary),
                shape = groupedShape(3, 4),
                onClick = onOpenDataTransfer,
                leading = null,
            )
        }

        GroupLabel(stringResource(R.string.profile_connections))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ProfileHubRow(
                title = stringResource(R.string.profile_ai_title),
                subtitle = coachConnectionSummary(state),
                shape = groupedShape(0, 2),
                onClick = onOpenAiCoach,
                leading = {
                    // The coach keeps its coral mark even on Profile surfaces.
                    Surface(
                        color = MusFitTheme.colors.accentContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CoachChatMark(
                                knockoutColor = MusFitTheme.colors.accentContainer,
                                bubbleColor = MusFitTheme.colors.accent,
                                size = 20.dp,
                            )
                        }
                    }
                },
            )
            ProfileHubRow(
                title = stringResource(R.string.profile_health_connect),
                subtitle = healthConnectionSummary(state),
                shape = groupedShape(1, 2),
                onClick = onOpenHealthConnect,
                leading = {
                    ExpressiveBadge(
                        icon = Icons.Filled.Favorite,
                        shape = ExpressiveBadgeShape.Squircle,
                        containerColor = accent.container,
                        contentColor = accent.onContainer,
                        size = 40.dp,
                        iconSize = 19.dp,
                    )
                },
            )
        }

        GroupLabel(stringResource(R.string.profile_preferences))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ProfileHubRow(
                title = stringResource(R.string.profile_add_burned_calories),
                subtitle = stringResource(R.string.profile_burned_calorie_formula),
                shape = groupedShape(0, 5),
                onClick = null,
                leading = null,
                trailing = {
                    AccentSwitch(
                        checked = state.includeBurnedCalories,
                        onCheckedChange = onIncludeBurnedCaloriesChange,
                        accent = accent,
                        modifier = Modifier.semantics {
                            contentDescription = addBurnedDescription
                        },
                    )
                },
            )
            CompactValueRow(
                title = stringResource(R.string.profile_units),
                value = stringResource(R.string.profile_metric_units),
                shape = groupedShape(1, 5),
            )
            CompactValueRow(
                title = stringResource(R.string.profile_theme),
                value = stringResource(R.string.profile_system),
                shape = groupedShape(2, 5),
            )
            ProfileHubRow(
                title = stringResource(R.string.profile_data_transfer),
                subtitle = stringResource(R.string.profile_data_transfer_summary),
                shape = groupedShape(3, 5),
                onClick = onOpenDataTransfer,
                leading = {
                    ExpressiveBadge(
                        icon = Icons.Outlined.SwapHoriz,
                        shape = ExpressiveBadgeShape.Squircle,
                        containerColor = accent.container,
                        contentColor = accent.onContainer,
                        size = 40.dp,
                        iconSize = 19.dp,
                    )
                },
            )
            CompactValueRow(
                title = stringResource(R.string.profile_data),
                value = stringResource(R.string.profile_local_first),
                shape = groupedShape(4, 5),
            )
        }

        GroupLabel(stringResource(R.string.profile_about))
        CompactValueRow(
            title = stringResource(R.string.profile_app_name),
            value = versionName,
            shape = RoundedCornerShape(24.dp),
        )
    }
}

/** Identity row: badge · title/sub · trailing "Linked" pill or a Link action. */
@Composable
private fun ProviderRow(
    title: String,
    linked: Boolean,
    action: ProviderSignInActionUiState,
    accent: TabAccent,
    shape: RoundedCornerShape,
    badgeShape: ExpressiveBadgeShape,
    badgeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    linkedSubtitle: String,
    onClick: () -> Unit,
) {
    ProfileHubRow(
        title = title,
        subtitle = when {
            linked -> linkedSubtitle

            action.statusLabel == uiText(R.string.profile_setup_needed) -> stringResource(R.string.profile_not_configured_build)

            action.statusLabel == uiText(R.string.profile_in_progress) ||
                action.statusLabel == uiText(R.string.profile_waiting) -> action.supportingText.asString()

            else -> stringResource(R.string.profile_optional_linking)
        },
        shape = shape,
        onClick = if (!linked && action.enabled) onClick else null,
        onClickLabel = action.buttonLabel.asString(),
        leading = {
            ExpressiveBadge(
                icon = badgeIcon,
                shape = badgeShape,
                containerColor = accent.container,
                contentColor = accent.onContainer,
                size = 40.dp,
                iconSize = 19.dp,
            )
        },
        trailing = {
            when {
                linked -> Surface(color = accent.container, shape = RoundedCornerShape(99.dp)) {
                    Text(
                        stringResource(R.string.profile_linked),
                        style = MusFitTheme.typography.labelSmall.copy(fontWeight = FontWeight.W800, letterSpacing = 0.sp),
                        color = accent.onContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }

                else -> Text(
                    text = stringResource(
                        if (action.statusLabel == uiText(R.string.profile_in_progress)) {
                            R.string.profile_waiting_ellipsis
                        } else {
                            R.string.profile_link
                        },
                    ),
                    style = MusFitTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.W800),
                    color = if (action.enabled) accent.color else MusFitTheme.colors.onSurfaceFaint,
                )
            }
        },
    )
}

/** "31 · 183 cm · gain 0.3 kg/wk" — or an invitation while fields are missing. */
@Composable
private fun profileDetailsSummary(profile: UserProfile): String {
    val locale = LocalConfiguration.current.locales[0]
    val age = profile.birthDateEpochDay?.let {
        LocalizedFormatter.integer(Period.between(LocalDate.ofEpochDay(it), LocalDate.now()).years.toLong(), locale = locale)
    }
    val height = profile.heightCm?.let {
        stringResource(R.string.profile_details_height, it.format1(locale))
    }
    val pace = when (profile.goalType) {
        com.musfit.domain.profile.GoalType.Maintain -> stringResource(R.string.profile_pace_maintain)

        com.musfit.domain.profile.GoalType.Lose -> stringResource(
            R.string.profile_details_pace_lose,
            profile.goalPaceKgPerWeek.format1(locale),
        )

        com.musfit.domain.profile.GoalType.Gain -> stringResource(
            R.string.profile_details_pace_gain,
            profile.goalPaceKgPerWeek.format1(locale),
        )
    }
    val parts = listOfNotNull(age, height, pace)
    return if (age == null && height == null) {
        stringResource(R.string.profile_details_invitation)
    } else {
        when (parts.size) {
            1 -> parts.single()

            2 -> stringResource(R.string.profile_join_middle_dot, parts[0], parts[1])

            else -> {
                val firstTwo = stringResource(R.string.profile_join_middle_dot, parts[0], parts[1])
                stringResource(R.string.profile_join_middle_dot, firstTwo, parts[2])
            }
        }
    }
}

@Composable
private fun coachConnectionSummary(state: ProfileSettingsUiState): String {
    val coach = state.aiCoach
    return when {
        coach.providerKind == AiCoachProviderKind.Disabled -> stringResource(R.string.profile_ai_off_summary)

        coach.modelName.isNotBlank() -> stringResource(
            R.string.profile_ai_provider_model,
            coach.providerLabel.asString(),
            coach.modelName,
        )

        else -> coach.providerLabel.asString()
    }
}

@Composable
private fun healthConnectionSummary(state: ProfileSettingsUiState): String = when {
    state.requestablePermissionCount > 0 ->
        stringResource(
            R.string.profile_permissions_summary,
            LocalizedFormatter.integer(state.grantedPermissionCount.toLong(), locale = LocalConfiguration.current.locales[0]),
            LocalizedFormatter.integer(state.requestablePermissionCount.toLong(), locale = LocalConfiguration.current.locales[0]),
        )

    state.availabilityLabel == uiText(R.string.profile_unknown) -> stringResource(R.string.profile_checking_availability)

    else -> state.availabilityLabel.asString()
}

@Composable
private fun GitHubDeviceCodeDialog(
    userCode: String,
    verificationUri: String,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_github_sign_in)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.profile_github_enter_code_prompt))
                Text(userCode, style = MusFitTheme.typography.headlineSmall)
                Text(verificationUri, style = MusFitTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onOpen) { Text(stringResource(R.string.profile_open_github)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.profile_close)) } },
    )
}
