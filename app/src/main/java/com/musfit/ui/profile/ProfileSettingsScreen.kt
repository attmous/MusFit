package com.musfit.ui.profile

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.musfit.BuildConfig
import com.musfit.ui.AppDestination
import com.musfit.ui.components.ExpressiveBadge
import com.musfit.ui.components.ExpressiveBadgeShape
import com.musfit.ui.components.InnerScreenHeader
import com.musfit.ui.components.groupedShape
import com.musfit.ui.permissions.LOCAL_NETWORK_PERMISSION
import com.musfit.ui.permissions.hasLocalNetworkPermission
import com.musfit.ui.permissions.requiresLocalNetworkPermission
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import com.musfit.ui.theme.tabAccentFor
import com.musfit.data.repository.AiCoachProviderKind
import com.musfit.data.repository.UserProfile
import java.time.LocalDate
import java.time.Period
import kotlinx.coroutines.launch

/** Inner settings surfaces reached from the 11b hub. */
private enum class SettingsSubPage { AiCoach, HealthConnect }

@Composable
fun ProfileSettingsScreen(
    onBack: () -> Unit,
    viewModel: ProfileSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val accent = tabAccentFor(AppDestination.Profile)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var subPage by rememberSaveable { mutableStateOf<SettingsSubPage?>(null) }
    var showProfileSheet by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { viewModel.refreshStatus() }
    val localNetworkPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.testAiCoachConnection()
        } else {
            viewModel.reportAiCoachLocalNetworkPermissionDenied()
        }
    }
    val onTestAiCoach = {
        if (
            requiresLocalNetworkPermission(state.aiCoach.providerKind) &&
            !hasLocalNetworkPermission(context)
        ) {
            localNetworkPermissionLauncher.launch(LOCAL_NETWORK_PERMISSION)
        } else {
            viewModel.testAiCoachConnection()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshStatus()
        viewModel.syncRecentHealthData()
    }

    // A system/gesture back on an inner settings page returns to the hub instead
    // of popping the whole settings route.
    BackHandler(enabled = subPage != null) { subPage = null }

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
                            googleWebClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
                        )
                    }.onSuccess(viewModel::signInWithProvider)
                        .onFailure { viewModel.reportExternalSignInFailure("Google", it) }
                }
            },
            onGitHubSignIn = viewModel::signInWithGitHub,
            onOpenAiCoach = { subPage = SettingsSubPage.AiCoach },
            onOpenHealthConnect = { subPage = SettingsSubPage.HealthConnect },
            onIncludeBurnedCaloriesChange = viewModel::setIncludeBurnedCalories,
        )
    }

    if (showProfileSheet) {
        ProfileEditSheet(
            initial = state.profile,
            initialWeightKg = state.latestWeightKg,
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
                        Intent(Intent.ACTION_VIEW, Uri.parse(githubDeviceCode.verificationUri)),
                    )
                }.onFailure { viewModel.reportExternalSignInFailure("GitHub", it) }
            },
            onDismiss = viewModel::dismissGitHubDeviceCode,
        )
    }
}

/**
 * The 11b settings hub: local-account hero, then Account / Connections /
 * Preferences / About grouped lists. AI coach and Health Connect are launcher
 * rows into their own inner pages.
 */
@Composable
private fun SettingsHub(
    state: ProfileSettingsUiState,
    accent: TabAccent,
    onBack: () -> Unit,
    onEditAccount: () -> Unit,
    onOpenProfileDetails: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onGitHubSignIn: () -> Unit,
    onOpenAiCoach: () -> Unit,
    onOpenHealthConnect: () -> Unit,
    onIncludeBurnedCaloriesChange: (Boolean) -> Unit,
) {
    val signInActions = providerSignInActions(
        googleConfigured = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank(),
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
            title = "Settings",
            subtitle = "Profile, identity, coach and device sync",
            onBack = onBack,
        )

        LocalAccountHero(account = state.account, accent = accent, onEdit = onEditAccount)

        GroupLabel("Account")
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ProfileHubRow(
                title = "Profile details",
                subtitle = profileDetailsSummary(state.profile),
                shape = groupedShape(0, 3),
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
                title = "Google",
                linked = state.account.providerLabel == "Google",
                action = signInActions.google,
                accent = accent,
                shape = groupedShape(1, 3),
                badgeShape = ExpressiveBadgeShape.Circle,
                badgeIcon = Icons.Outlined.Link,
                linkedSubtitle = state.account.email ?: "Linked to this local account",
                onClick = onGoogleSignIn,
            )
            ProviderRow(
                title = "GitHub",
                linked = state.account.providerLabel == "GitHub",
                action = signInActions.github,
                accent = accent,
                shape = groupedShape(2, 3),
                badgeShape = ExpressiveBadgeShape.Squircle,
                badgeIcon = Icons.Outlined.Code,
                linkedSubtitle = state.account.email ?: "Linked to this local account",
                onClick = onGitHubSignIn,
            )
        }

        GroupLabel("Connections")
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ProfileHubRow(
                title = "AI coach",
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
                title = "Health Connect",
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

        GroupLabel("Preferences")
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ProfileHubRow(
                title = "Add burned calories to budget",
                subtitle = "kcal left = goal − eaten + burned",
                shape = groupedShape(0, 4),
                onClick = null,
                leading = null,
                trailing = {
                    AccentSwitch(
                        checked = state.includeBurnedCalories,
                        onCheckedChange = onIncludeBurnedCaloriesChange,
                        accent = accent,
                    )
                },
            )
            CompactValueRow(
                title = "Units",
                value = "Metric · kg, cm",
                shape = groupedShape(1, 4),
            )
            CompactValueRow(title = "Theme", value = "System", shape = groupedShape(2, 4))
            CompactValueRow(title = "Data", value = "Local first", shape = groupedShape(3, 4))
        }

        GroupLabel("About")
        CompactValueRow(
            title = "MusFit",
            value = BuildConfig.VERSION_NAME,
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
            action.statusLabel == "Setup needed" -> "Not configured in this build"
            action.statusLabel == "In progress" || action.statusLabel == "Waiting" -> action.supportingText
            else -> "Optional linking · no cloud sync"
        },
        shape = shape,
        onClick = if (!linked && action.enabled) onClick else null,
        onClickLabel = action.buttonLabel,
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
                        "Linked",
                        style = MusFitTheme.typography.labelSmall.copy(fontWeight = FontWeight.W800, letterSpacing = 0.sp),
                        color = accent.onContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }

                else -> Text(
                    text = if (action.statusLabel == "In progress") "Waiting…" else "Link",
                    style = MusFitTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.W800),
                    color = if (action.enabled) accent.color else MusFitTheme.colors.onSurfaceFaint,
                )
            }
        },
    )
}

/** "31 · 183 cm · gain 0.3 kg/wk" — or an invitation while fields are missing. */
private fun profileDetailsSummary(profile: UserProfile): String {
    val age = profile.birthDateEpochDay?.let {
        Period.between(LocalDate.ofEpochDay(it), LocalDate.now()).years.toString()
    }
    val height = profile.heightCm?.let { "${it.format1()} cm" }
    val pace = when (profile.goalType) {
        com.musfit.domain.profile.GoalType.Maintain -> "maintain"
        com.musfit.domain.profile.GoalType.Lose -> "lose ${profile.goalPaceKgPerWeek.format1()} kg/wk"
        com.musfit.domain.profile.GoalType.Gain -> "gain ${profile.goalPaceKgPerWeek.format1()} kg/wk"
    }
    val parts = listOfNotNull(age, height, pace)
    return if (age == null && height == null) "Add your age, height, and goal" else parts.joinToString(" · ")
}

private fun coachConnectionSummary(state: ProfileSettingsUiState): String {
    val coach = state.aiCoach
    return when {
        coach.providerKind == AiCoachProviderKind.Disabled -> "Off · bring your own model"
        coach.modelName.isNotBlank() -> "${coach.providerLabel} · ${coach.modelName}"
        else -> coach.providerLabel
    }
}

private fun healthConnectionSummary(state: ProfileSettingsUiState): String =
    when {
        state.requestablePermissionCount > 0 ->
            "${state.grantedPermissionCount} of ${state.requestablePermissionCount} permissions"
        state.availabilityLabel == "Unknown" -> "Checking availability…"
        else -> state.availabilityLabel
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
        title = { Text("GitHub sign-in") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter this code at GitHub:")
                Text(userCode, style = MusFitTheme.typography.headlineSmall)
                Text(verificationUri, style = MusFitTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onOpen) { Text("Open GitHub") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
