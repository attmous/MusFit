package com.musfit.ui.profile

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.musfit.data.repository.DEFAULT_USER_PROFILE
import com.musfit.ui.AppDestination
import com.musfit.ui.components.MusFitScreenHeader
import com.musfit.ui.components.MusFitSummaryCard
import com.musfit.ui.theme.TabAccent
import com.musfit.ui.theme.tabAccentFor
import java.util.Locale
import kotlin.math.abs

@Composable
fun ProfileScreen(
    onSettingsClick: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val accent = tabAccentFor(AppDestination.Profile)
    val snackbarHostState = remember { SnackbarHostState() }
    var showEditor by remember { mutableStateOf(false) }
    var showLogWeight by remember { mutableStateOf(false) }
    var showLogMeasurement by remember { mutableStateOf(false) }
    var showWeightSheet by remember { mutableStateOf(false) }
    var measurementSheetType by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // The app-level Scaffold already applies the system-bar insets; this inner
        // Scaffold (kept only for the snackbar) must not re-add them, or the top inset
        // doubles into a gap above the header.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MusFitScreenHeader(
                title = "Profile",
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
            )
            AccountCard(state = state, onEdit = viewModel::openAccountEditor)
            IdentityCard(state = state, onEdit = { showEditor = true })
            GoalCard(state = state, onApply = viewModel::applyTargetsToFood, onComplete = { showEditor = true })
            WeightCard(state = state, accent = accent, onLog = { showLogWeight = true }, onOpenEntries = { showWeightSheet = true })
            MeasurementsCard(state = state, onLog = { showLogMeasurement = true }, onOpenType = { measurementSheetType = it })
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
    if (showWeightSheet) {
        EntriesSheet(
            title = "Weight history",
            items = state.weightEntries.map {
                EntrySheetItem(it.id, formatEntryDate(it.measuredAtEpochMillis), it.weightKg, "kg")
            },
            onDismiss = { showWeightSheet = false },
            onEdit = viewModel::editEntry,
            onDelete = viewModel::deleteEntry,
        )
    }
    measurementSheetType?.let { type ->
        val rows = state.measurementEntries[type].orEmpty()
        EntriesSheet(
            title = "${MEASUREMENT_SHEET_LABELS[type] ?: type} history",
            items = rows.map { EntrySheetItem(it.id, formatEntryDate(it.measuredAtEpochMillis), it.value, it.unit) },
            onDismiss = { measurementSheetType = null },
            onEdit = viewModel::editEntry,
            onDelete = viewModel::deleteEntry,
        )
    }
    if (state.accountEditorOpen) {
        AccountEditDialog(
            name = state.accountNameInput,
            email = state.accountEmailInput,
            error = state.accountErrorMessage,
            onNameChange = viewModel::onAccountNameChanged,
            onEmailChange = viewModel::onAccountEmailChanged,
            onDismiss = viewModel::closeAccountEditor,
            onSave = viewModel::saveAccount,
        )
    }
}

@Composable
private fun AccountCard(state: ProfileUiState, onEdit: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    state.account.displayName.accountInitial(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(state.account.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    state.account.email ?: "Local account",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.account.isLocalOnly) {
                    AssistChip(onClick = {}, label = { Text("Local only") })
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = "Edit account")
            }
        }
    }
}

@Composable
private fun AccountEditDialog(
    name: String,
    email: String,
    error: String?,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Local account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    isError = error != null,
                    supportingText = { if (error != null) Text(error) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Email (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Stored on this device. Sync and sign-in are not enabled.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun IdentityCard(state: ProfileUiState, onEdit: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit), shape = MaterialTheme.shapes.extraLarge) {
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
    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
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
private fun WeightCard(state: ProfileUiState, accent: TabAccent, onLog: () -> Unit, onOpenEntries: () -> Unit) {
    MusFitSummaryCard(accent = accent, onClick = onOpenEntries) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Weight",
                    style = MaterialTheme.typography.titleMedium,
                    color = accent.onContainer,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onLog) { Text("Log") }
            }
            if (state.latestWeightKg != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${state.latestWeightKg.format1()} kg",
                            style = MaterialTheme.typography.titleLarge,
                            color = accent.onContainer,
                        )
                        state.weeklyWeightDeltaKg?.let { delta ->
                            val arrow = if (delta < 0) "▼" else if (delta > 0) "▲" else "•"
                            Text(
                                "$arrow ${abs(delta).format1()} kg this week",
                                style = MaterialTheme.typography.bodySmall,
                                color = accent.onContainer,
                            )
                        }
                    }
                    if (state.weightTrend.size >= 2) {
                        Sparkline(values = state.weightTrend, color = accent.color, modifier = Modifier.width(120.dp).height(36.dp))
                    }
                }
            } else {
                Text(
                    "No weight logged yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent.onContainer,
                )
            }
        }
    }
}

@Composable
private fun Sparkline(values: List<Double>, color: Color, modifier: Modifier) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val minV = values.min()
        val maxV = values.max()
        val range = (maxV - minV).takeIf { it > 0.0 } ?: 1.0
        val stepX = size.width / (values.size - 1)
        val offsets = values.mapIndexed { i, v ->
            Offset(stepX * i, (size.height - ((v - minV) / range * size.height)).toFloat())
        }
        val path = Path().apply {
            moveTo(offsets.first().x, offsets.first().y)
            offsets.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(path, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun MeasurementsCard(state: ProfileUiState, onLog: () -> Unit, onOpenType: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Measurements", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onLog) { Text("Log") }
            }
            state.measurements.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { measurement ->
                        MeasurementCell(
                            row = measurement,
                            onClick = { onOpenType(measurement.type) },
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
private fun MeasurementCell(row: MeasurementRow, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(row.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                row.value?.let { "${it.format1()} ${row.unit}" } ?: "—",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
            row.deltaFromPrevious?.let { d ->
                if (d != 0.0) {
                    Text(
                        "${if (d < 0) "▼" else "▲"}${abs(d).format1()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun VitalsCard(state: ProfileUiState, onManage: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
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

private val MEASUREMENT_SHEET_LABELS = mapOf(
    "waist" to "Waist", "chest" to "Chest", "arms" to "Arms",
    "thighs" to "Thighs", "hips" to "Hips", "body_fat" to "Body fat",
)

private fun identitySubtitle(state: ProfileUiState): String {
    val parts = mutableListOf<String>()
    state.ageYears?.let { parts.add("$it yrs") }
    state.profile?.heightCm?.let { parts.add("${it.format1()} cm") }
    state.profile?.activityLevel?.let { parts.add(it.label()) }
    return if (parts.isEmpty()) "Tap to set up your profile" else parts.joinToString(" · ")
}

private fun String.accountInitial(): String =
    trim().firstOrNull()?.uppercaseChar()?.toString() ?: "Y"

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
