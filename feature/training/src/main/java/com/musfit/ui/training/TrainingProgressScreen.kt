package com.musfit.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musfit.data.repository.ExerciseSummary
import com.musfit.ui.components.InnerScreenHeader
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import com.musfit.ui.theme.TabAccentRole
import com.musfit.ui.theme.tabAccentFor

/**
 * Progress page (mock 5b): back + "Progress" + a plain-text period dropdown, then the
 * e1RM chart / weekly volume / recent PRs content. "All exercises" re-anchors the page.
 */
@Composable
fun TrainingProgressScreen(
    onBack: () -> Unit,
    viewModel: TrainingProgressViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val accent = tabAccentFor(TabAccentRole.Training)

    Scaffold(
        containerColor = MusFitTheme.colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MusFitTheme.colors.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            TrainingProgressHeader(
                period = state.period,
                onBack = onBack,
                onSelectPeriod = viewModel::selectPeriod,
            )
            TrainingProgressContent(
                progress = state.selectedExerciseProgress,
                period = state.period,
                weeklyVolume = state.progressAnalytics.weeklyVolume,
                recentPrs = state.recentPrs,
                accent = accent,
                onOpenAllExercises = viewModel::openExercisePicker,
            )
        }
    }

    if (state.exercisePickerOpen) {
        ProgressExercisePickerSheet(
            exercises = state.exercises,
            loggedExerciseIds = state.loggedExerciseIds,
            selectedExerciseId = state.selectedProgressExerciseId,
            accent = accent,
            onSelect = viewModel::selectProgressExercise,
            onDismiss = viewModel::closeExercisePicker,
        )
    }
}

/** 10f header: back circle, 27/800 title, and the tonal period pill dropdown. */
@Composable
private fun TrainingProgressHeader(
    period: TrainingProgressPeriod,
    onBack: () -> Unit,
    onSelectPeriod: (TrainingProgressPeriod) -> Unit,
) {
    InnerScreenHeader(title = "Progress", onBack = onBack) {
        Box {
            var menuOpen by remember { mutableStateOf(false) }
            Surface(
                onClick = { menuOpen = true },
                color = MusFitTheme.colors.surfaceVariant,
                contentColor = MusFitTheme.colors.onSurface,
                shape = RoundedCornerShape(99.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Period, ${period.label}"
                        role = Role.Button
                    },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(start = 14.dp, end = 10.dp, top = 9.dp, bottom = 9.dp),
                ) {
                    Text(
                        text = period.label,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                        fontWeight = FontWeight.Bold,
                    )
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                TrainingProgressPeriod.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            menuOpen = false
                            onSelectPeriod(option)
                        },
                    )
                }
            }
        }
    }
}

/** "All exercises": a searchable list to re-anchor the page — trained exercises first. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressExercisePickerSheet(
    exercises: List<ExerciseSummary>,
    loggedExerciseIds: Set<String>,
    selectedExerciseId: String?,
    accent: TabAccent,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, exercises, loggedExerciseIds) {
        val q = query.trim()
        exercises
            .filter { q.isBlank() || it.name.contains(q, ignoreCase = true) }
            .sortedByDescending { it.id in loggedExerciseIds }
            .take(50)
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MusFitTheme.colors.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "All exercises",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Normal,
                color = MusFitTheme.colors.onSurface,
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search exercises") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                shape = MusFitTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                filtered.forEachIndexed { index, exercise ->
                    ListItem(
                        headlineContent = { Text(exercise.name) },
                        supportingContent = {
                            if (exercise.id in loggedExerciseIds) Text("Trained before")
                        },
                        trailingContent = {
                            if (exercise.id == selectedExerciseId) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = accent.color,
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.selectable(
                            selected = exercise.id == selectedExerciseId,
                            role = Role.RadioButton,
                            onClick = { onSelect(exercise.id) },
                        ),
                    )
                    if (index < filtered.lastIndex) {
                        HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
                    }
                }
            }
        }
    }
}
