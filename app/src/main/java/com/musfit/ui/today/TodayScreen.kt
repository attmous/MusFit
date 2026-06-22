package com.musfit.ui.today

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.musfit.ui.theme.MusFitTheme
import java.util.Locale
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MusFitTheme.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text(
                text = "Today",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MusFitTheme.colors.onSurface,
            )
            Text(
                text = state.dateLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        }

        if (state.rings.isNotEmpty()) {
            DailyRingsCard(rings = state.rings, macros = state.macros, onClick = onOpenFood)
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
            GlimpseTile(
                icon = Icons.Outlined.MonitorWeight,
                value = state.weightKg?.let { "${it.formatMetric()} kg" } ?: "—",
                label = "Body weight",
                onClick = onOpenHealth,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyRingsCard(
    rings: List<DailyRingUiState>,
    macros: MacroBreakdownUiState,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                rings.forEach { ring ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GoalRing(progress = ring.progress, color = ringColor(ring.kind), centerLabel = ring.centerLabel)
                        Spacer(Modifier.height(7.dp))
                        Text(
                            text = ring.kind.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MusFitTheme.colors.onSurface,
                        )
                        Text(
                            text = ring.goalLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MusFitTheme.colors.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            MacroBar(macros)
        }
    }
}

@Composable
private fun GoalRing(
    progress: Float,
    color: Color,
    centerLabel: String,
) {
    val track = MusFitTheme.colors.track
    Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 7.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = progress.coerceIn(0f, 1f) * 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            text = centerLabel,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MusFitTheme.colors.onSurface,
        )
    }
}

@Composable
private fun MacroBar(macros: MacroBreakdownUiState) {
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
            color = MusFitTheme.colors.onSurfaceVariant,
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
