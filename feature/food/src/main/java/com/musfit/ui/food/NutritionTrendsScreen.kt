package com.musfit.ui.food

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musfit.ui.theme.MusFitTheme

@Composable
fun NutritionTrendsScreen(
    onBack: () -> Unit,
    viewModel: NutritionTrendsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
            NutritionTrendsHeader(onBack = onBack)
            WeeklyMusFitScoreCard(state.weeklyScore)
            FoodProgressStatsCard(state.progressStats)
        }
    }
}

@Composable
private fun NutritionTrendsHeader(onBack: () -> Unit) {
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
                "Nutrition trends",
                style = MusFitTheme.typography.headlineMedium,
                color = MusFitTheme.colors.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Weekly MusFit score and 7/28-day progress.",
                style = MusFitTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeeklyMusFitScoreCard(score: FoodWeeklyScoreUiState) {
    val accent = score.tone.ratingColor()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = accent.copy(alpha = 0.10f).compositeOver(MusFitTheme.colors.surface),
        shape = MusFitTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = score.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MusFitTheme.colors.brandInk,
                    )
                    Text(
                        text = score.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = score.score.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        maxLines = 1,
                    )
                }
            }
            ProgressBar(progress = score.score / 100f, color = accent)
            Text(
                text = score.suggestion,
                style = MaterialTheme.typography.bodySmall,
                color = accent,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (score.factors.isNotEmpty()) {
                HorizontalDivider(color = MusFitTheme.colors.outline)
                RatingFactorColumn(score.factors)
            }
        }
    }
}

@Composable
private fun FoodProgressStatsCard(stats: FoodProgressStatsUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Progress stats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MusFitTheme.colors.brandInk,
            )
            FoodProgressPeriodRow(stats.weekly)
            HorizontalDivider(color = MusFitTheme.colors.outline)
            FoodProgressPeriodRow(stats.monthly)
        }
    }
}

@Composable
private fun FoodProgressPeriodRow(period: FoodProgressPeriodUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(period.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                period.trackedDaysLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MusFitTheme.colors.brand,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // Scannable caption/value grid instead of a run-on sentence of metrics.
        period.metrics.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                pair.forEach { metric ->
                    FoodProgressMetricCell(metric = metric, modifier = Modifier.weight(1f))
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        Text(
            text = period.trendLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MusFitTheme.colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FoodProgressMetricCell(
    metric: FoodProgressMetricUiState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            text = metric.caption,
            style = MaterialTheme.typography.labelSmall,
            color = MusFitTheme.colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = metric.value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MusFitTheme.colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
