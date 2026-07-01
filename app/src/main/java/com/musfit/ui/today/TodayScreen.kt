package com.musfit.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.musfit.domain.coach.CoachAction
import com.musfit.domain.coach.CoachBriefing
import com.musfit.ui.AppDestination
import com.musfit.ui.components.MusFitScreenScaffold
import com.musfit.ui.components.MusFitSummaryCard
import com.musfit.ui.components.charts.BarDatum
import com.musfit.ui.components.charts.MetricRing
import com.musfit.ui.components.charts.TrendLineChart
import com.musfit.ui.components.charts.WeekBarChart
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import com.musfit.ui.theme.tabAccentFor
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: TodayViewModel = hiltViewModel(),
    onOpenFood: () -> Unit = {},
    onOpenTraining: () -> Unit = {},
    onOpenHealth: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()

    MusFitScreenScaffold(
        title = "Today",
        subtitle = state.dateLabel,
        actions = {
            IconButton(onClick = viewModel::openGoalsEditor) {
                Icon(Icons.Outlined.Tune, contentDescription = "Edit goals", tint = MusFitTheme.colors.onSurfaceVariant)
            }
        },
    ) {

        state.coach?.let { briefing ->
            CoachBriefingCard(briefing) { action ->
                when (action) {
                    CoachAction.OpenFood -> onOpenFood()
                    CoachAction.OpenTraining -> onOpenTraining()
                    CoachAction.OpenHealth -> onOpenHealth()
                    is CoachAction.StartRoutine -> onOpenTraining()
                }
            }
        }

        val accent = tabAccentFor(AppDestination.Today)

        if (state.rings.isNotEmpty()) {
            DailyRingsCard(rings = state.rings, macros = state.macros, onClick = onOpenFood)
        }

        state.weeklyCharts?.let { charts ->
            WeeklyCaloriesCard(
                charts = charts,
                accent = accent,
                selectedIndex = state.selectedCalorieDayIndex ?: charts.defaultSelectedIndex,
                onDaySelected = viewModel::onCalorieDaySelected,
            )
            WeightTrendCard(charts = charts, accent = accent)
            WeekStatsRow(charts = charts)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlimpseTile(
                icon = Icons.Outlined.FitnessCenter,
                value = state.training.title,
                label = state.training.subtitle,
                onClick = onOpenTraining,
            )
        }
    }

    if (state.isGoalsEditorVisible) {
        TodayGoalsEditorSheet(
            state = state,
            onStepGoalChanged = viewModel::onStepGoalInputChanged,
            onSessionTargetChanged = viewModel::onSessionTargetInputChanged,
            onTargetWeightChanged = viewModel::onTargetWeightInputChanged,
            onSave = viewModel::saveUserGoals,
            onDismiss = viewModel::closeGoalsEditor,
        )
    }
}

@Composable
private fun DailyRingsCard(
    rings: List<DailyRingUiState>,
    macros: MacroBreakdownUiState,
    onClick: () -> Unit,
) {
    val accent = tabAccentFor(AppDestination.Today)
    MusFitSummaryCard(accent = accent, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            rings.forEach { ring ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MetricRing(progress = ring.progress, color = ringColor(ring.kind)) {
                        Text(
                            text = "${(ring.progress * 100).roundToInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = accent.onContainer,
                        )
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = ring.kind.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = accent.onContainer,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        MacroBar(macros = macros, labelColor = accent.onContainer)
    }
}

@Composable
private fun MacroBar(macros: MacroBreakdownUiState, labelColor: Color = MusFitTheme.colors.onSurfaceVariant) {
    val macroColors = MusFitTheme.colors.macroColors
    val values = listOf(macros.carbsGrams, macros.proteinGrams, macros.fatGrams)
    val total = values.sum().takeIf { it > 0.0 } ?: 1.0
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        values.forEachIndexed { index, value ->
            Box(
                modifier = Modifier
                    .weight((value / total).toFloat().coerceAtLeast(0.001f))
                    .height(6.dp)
                    .clip(MusFitTheme.shapes.small)
                    .background(macroColors[index]),
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = "C ${macros.carbsGrams.roundToInt()} · P ${macros.proteinGrams.roundToInt()} · F ${macros.fatGrams.roundToInt()}",
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RowScope.GlimpseTile(
    icon: ImageVector,
    value: String,
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.medium,
        modifier = Modifier.weight(1f),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = MusFitTheme.colors.brand)
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MusFitTheme.colors.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeeklyCaloriesCard(
    charts: WeeklyChartsUiState,
    accent: TabAccent,
    selectedIndex: Int?,
    onDaySelected: (Int) -> Unit,
) {
    Surface(color = MusFitTheme.colors.surface, shape = MusFitTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "Calories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MusFitTheme.colors.onSurface,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "this week",
                        style = MaterialTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
                Surface(color = accent.container, shape = MusFitTheme.shapes.small) {
                    Text(
                        text = "${charts.onTargetDays}/${charts.trackedDays} on target",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = accent.onContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            WeekBarChart(
                bars = charts.calorieBars.map { BarDatum(value = it.calories, label = it.label) },
                accent = accent.color,
                onAccent = accent.onColor,
                target = charts.calorieTarget,
                selectedIndex = selectedIndex,
                onBarSelected = onDaySelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
            )
        }
    }
}

@Composable
private fun WeightTrendCard(charts: WeeklyChartsUiState, accent: TabAccent) {
    Surface(color = MusFitTheme.colors.surface, shape = MusFitTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        text = "Weight · 30 days",
                        style = MaterialTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                    Text(
                        text = charts.latestWeightKg?.let { "${it.formatMetric()} kg" } ?: "—",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MusFitTheme.colors.onSurface,
                    )
                }
                charts.weightDeltaKg?.let { d ->
                    val arrow = if (d < -0.05) "↓" else if (d > 0.05) "↑" else "→"
                    Surface(color = MusFitTheme.colors.positiveContainer, shape = MusFitTheme.shapes.small) {
                        Text(
                            text = "$arrow ${abs(d).formatMetric()} kg",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MusFitTheme.colors.positive,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (charts.weightTrend.isEmpty()) {
                Text(
                    text = "Log your weight to see the trend.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            } else {
                TrendLineChart(
                    values = charts.weightTrend,
                    accent = accent.color,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                )
            }
        }
    }
}

@Composable
private fun WeekStatsRow(charts: WeeklyChartsUiState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        WeeklyMiniTracker(Modifier.weight(1f), "Sessions", "${charts.sessionsDone} / ${charts.sessionTarget}")
        WeeklyMiniTracker(Modifier.weight(1f), "Step-goal days", "${charts.stepGoalDays} / ${charts.trackedDays}")
    }
}

@Composable
private fun WeeklyMiniTracker(modifier: Modifier, label: String, value: String) {
    Column(
        modifier = modifier
            .clip(MusFitTheme.shapes.medium)
            .background(MusFitTheme.colors.surfaceVariant)
            .padding(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MusFitTheme.colors.onSurfaceVariant,
            maxLines = 1,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MusFitTheme.colors.onSurface,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodayGoalsEditorSheet(
    state: TodayUiState,
    onStepGoalChanged: (String) -> Unit,
    onSessionTargetChanged: (String) -> Unit,
    onTargetWeightChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MusFitTheme.colors.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Goals",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MusFitTheme.colors.onSurface,
            )
            OutlinedTextField(
                value = state.stepGoalInput,
                onValueChange = onStepGoalChanged,
                label = { Text("Daily step goal") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.sessionTargetInput,
                onValueChange = onSessionTargetChanged,
                label = { Text("Workouts per week") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.targetWeightInput,
                onValueChange = onTargetWeightChanged,
                label = { Text("Target weight (kg)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MusFitTheme.colors.brand,
                    contentColor = MusFitTheme.colors.onBrand,
                ),
            ) {
                Text("Save goals")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoachBriefingCard(briefing: CoachBriefing, onAction: (CoachAction) -> Unit) {
    val cues = briefing.cues
    if (cues.isEmpty()) return
    var index by rememberSaveable { mutableStateOf(0) }
    val safeIndex = index.coerceIn(0, cues.size - 1)
    val cue = cues[safeIndex]

    Surface(
        color = MusFitTheme.colors.brand,
        shape = MusFitTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Coach briefing",
                    style = MaterialTheme.typography.labelMedium,
                    color = MusFitTheme.colors.positiveContainer,
                )
                if (cues.size > 1) {
                    Text(
                        text = "${safeIndex + 1} / ${cues.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MusFitTheme.colors.positiveContainer,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = briefing.greeting,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MusFitTheme.colors.onBrand,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = cue.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onBrand,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                cue.action?.let { action ->
                    Surface(
                        onClick = { onAction(action) },
                        color = MusFitTheme.colors.onBrand,
                        shape = MusFitTheme.shapes.small,
                    ) {
                        Text(
                            text = actionLabel(action),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MusFitTheme.colors.brandInk,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        )
                    }
                }
                if (cues.size > 1) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { index = (safeIndex + 1) % cues.size }) {
                        Text("Next", color = MusFitTheme.colors.positiveContainer)
                    }
                }
            }
        }
    }
}

private fun actionLabel(action: CoachAction): String = when (action) {
    CoachAction.OpenFood -> "Open food"
    CoachAction.OpenTraining -> "Open training"
    CoachAction.OpenHealth -> "Open health"
    is CoachAction.StartRoutine -> "Start workout"
}

@Composable
private fun ringColor(kind: RingKind): Color = when (kind) {
    RingKind.Calories -> MusFitTheme.colors.brand
    RingKind.Protein -> MusFitTheme.colors.macroProtein
    RingKind.Steps -> MusFitTheme.colors.water
}

private val RingKind.label: String
    get() = when (this) {
        RingKind.Calories -> "Calories"
        RingKind.Protein -> "Protein"
        RingKind.Steps -> "Steps"
    }

private fun Double.formatMetric(): String =
    if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", this)
    }
