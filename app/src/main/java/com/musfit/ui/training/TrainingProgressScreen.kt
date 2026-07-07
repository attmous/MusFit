package com.musfit.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.musfit.ui.AppDestination
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.tabAccentFor

@Composable
fun TrainingProgressScreen(
    onBack: () -> Unit,
    viewModel: TrainingProgressViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val accent = tabAccentFor(AppDestination.Training)

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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TrainingProgressHeader(onBack = onBack)
            TrainingProgressContent(
                exercises = state.exercises,
                selectedExerciseId = state.selectedProgressExerciseId,
                progress = state.selectedExerciseProgress,
                analytics = state.progressAnalytics,
                accent = accent,
                onSelectExercise = viewModel::selectProgressExercise,
            )
        }
    }
}

@Composable
private fun TrainingProgressHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Training progress",
                style = MusFitTheme.typography.headlineMedium,
                color = MusFitTheme.colors.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "PRs, trends, and volume analytics per exercise.",
                style = MusFitTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        }
    }
}
