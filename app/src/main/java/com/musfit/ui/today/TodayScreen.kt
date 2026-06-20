package com.musfit.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import java.util.Locale

@Composable
fun TodayScreen(viewModel: TodayViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Today",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "Calories: ${state.caloriesKcal.formatMetric()} kcal",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = "Protein: ${state.proteinGrams.formatMetric()} g",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Carbs: ${state.carbsGrams.formatMetric()} g",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Fat: ${state.fatGrams.formatMetric()} g",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = state.trainingSummary,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Steps: ${state.steps?.toString() ?: "Not synced"}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Active calories: ${state.activeCaloriesKcal?.let { "${it.formatMetric()} kcal" } ?: "Not synced"}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Body weight: ${state.bodyWeightKg?.let { "${it.formatMetric()} kg" } ?: "Not synced"}",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun Double.formatMetric(): String =
    if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", this)
    }
