package com.musfit.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.musfit.data.repository.DEFAULT_USER_PROFILE
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onSettingsClick: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showEditor by remember { mutableStateOf(false) }
    var showLogWeight by remember { mutableStateOf(false) }
    var showLogMeasurement by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IdentityCard(state = state, onEdit = { showEditor = true })
            GoalCard(state = state, onApply = viewModel::applyTargetsToFood, onComplete = { showEditor = true })
            WeightCard(state = state, onLog = { showLogWeight = true })
            MeasurementsCard(state = state, onLog = { showLogMeasurement = true })
            VitalsCard(state = state, onManage = onSettingsClick)
        }
    }

    if (showEditor) {
        ProfileEditDialog(
            initial = state.profile ?: DEFAULT_USER_PROFILE,
            initialWeightKg = state.latestWeightKg,
            onDismiss = { showEditor = false },
            onSave = { profile, weightKg ->
                viewModel.saveProfile(profile)
                if (weightKg != null) viewModel.logWeight(weightKg)
                showEditor = false
            },
        )
    }
    if (showLogWeight) {
        LogWeightDialog(
            prefillKg = state.latestWeightKg,
            onDismiss = { showLogWeight = false },
            onConfirm = { weightKg ->
                viewModel.logWeight(weightKg)
                showLogWeight = false
            },
        )
    }
    if (showLogMeasurement) {
        LogMeasurementDialog(
            onDismiss = { showLogMeasurement = false },
            onConfirm = { type, value, unit ->
                viewModel.logMeasurement(type, value, unit)
                showLogMeasurement = false
            },
        )
    }
}

@Composable
private fun IdentityCard(state: ProfileUiState, onEdit: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Your profile", style = MaterialTheme.typography.titleMedium)
                    Text(identitySubtitle(state), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("Edit", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MiniMetric("Weight", state.latestWeightKg?.let { "${it.format1()} kg" }, Modifier.weight(1f))
                MiniMetric("BMI", state.bmi?.let { it.format1() }, Modifier.weight(1f))
                MiniMetric("Body fat", state.bodyFatPercent?.let { "${it.format1()} %" }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GoalCard(state: ProfileUiState, onApply: () -> Unit, onComplete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Goal", style = MaterialTheme.typography.titleMedium)
            val profile = state.profile
            if (profile != null) {
                val goalText = buildString {
                    append(profile.goalType.label())
                    if (profile.goalType != com.musfit.domain.profile.GoalType.Maintain) {
                        append(" · ${profile.goalPaceKgPerWeek.format1()} kg/wk")
                    }
                }
                Text(goalText, style = MaterialTheme.typography.bodyMedium)
                if (state.latestWeightKg != null && profile.goalWeightKg != null) {
                    Text(
                        "${state.latestWeightKg.format1()} kg → ${profile.goalWeightKg.format1()} kg",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.goalProgressFraction != null) {
                    ProgressBar(fraction = state.goalProgressFraction.toFloat())
                }
            }
            val targets = state.recommendedTargets
            if (targets != null) {
                Text(
                    "${targets.caloriesKcal.format1()} kcal · recommended",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    "P ${targets.proteinGrams.format1()} g · C ${targets.carbsGrams.format1()} g · F ${targets.fatGrams.format1()} g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onApply, modifier = Modifier.fillMaxWidth()) {
                    Text("Apply to Food goals")
                }
            } else {
                Text(
                    "Complete your profile to see recommended calories and macros.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onComplete, modifier = Modifier.fillMaxWidth()) {
                    Text("Complete your profile")
                }
            }
        }
    }
}

@Composable
private fun WeightCard(state: ProfileUiState, onLog: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Weight", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onLog) { Text("Log") }
            }
            if (state.latestWeightKg != null) {
                Text("${state.latestWeightKg.format1()} kg", style = MaterialTheme.typography.titleLarge)
            } else {
                Text(
                    "No weight logged yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MeasurementsCard(state: ProfileUiState, onLog: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Measurements", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onLog) { Text("Log") }
            }
            state.measurements.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { measurement ->
                        MiniMetric(
                            label = measurement.label,
                            value = measurement.value?.let { "${it.format1()} ${measurement.unit}" },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun VitalsCard(state: ProfileUiState, onManage: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("From Health Connect", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onManage) { Text("Manage") }
            }
            val vitals = state.vitals
            if (vitals != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    MiniMetric("Resting HR", vitals.restingHeartRateBpm?.let { "$it bpm" }, Modifier.weight(1f))
                    MiniMetric("Steps", vitals.steps?.toString(), Modifier.weight(1f))
                    MiniMetric("Active", vitals.activeCaloriesKcal?.let { "${it.format1()} kcal" }, Modifier.weight(1f))
                }
            } else {
                Text(
                    "Connect Health Connect to mirror your steps, resting heart rate, and active calories.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MiniMetric(label: String, value: String?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value ?: "—",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ProgressBar(fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

private fun identitySubtitle(state: ProfileUiState): String {
    val parts = mutableListOf<String>()
    state.ageYears?.let { parts.add("$it yrs") }
    state.profile?.heightCm?.let { parts.add("${it.format1()} cm") }
    state.profile?.activityLevel?.let { parts.add(it.label()) }
    return if (parts.isEmpty()) "Tap to set up your profile" else parts.joinToString(" · ")
}

internal fun Double.format1(): String =
    if (this % 1.0 == 0.0) toInt().toString() else String.format(Locale.US, "%.1f", this)

internal fun com.musfit.domain.profile.ActivityLevel.label(): String = when (this) {
    com.musfit.domain.profile.ActivityLevel.Sedentary -> "Sedentary"
    com.musfit.domain.profile.ActivityLevel.Light -> "Light"
    com.musfit.domain.profile.ActivityLevel.Moderate -> "Moderate"
    com.musfit.domain.profile.ActivityLevel.Active -> "Active"
    com.musfit.domain.profile.ActivityLevel.VeryActive -> "Very active"
}

internal fun com.musfit.domain.profile.GoalType.label(): String = when (this) {
    com.musfit.domain.profile.GoalType.Lose -> "Lose"
    com.musfit.domain.profile.GoalType.Maintain -> "Maintain"
    com.musfit.domain.profile.GoalType.Gain -> "Gain"
}

internal fun com.musfit.domain.profile.Sex.label(): String = when (this) {
    com.musfit.domain.profile.Sex.Male -> "Male"
    com.musfit.domain.profile.Sex.Female -> "Female"
}
