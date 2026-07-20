package com.musfit.ui.today

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musfit.domain.today.TodayMetric
import com.musfit.feature.today.R
import com.musfit.ui.components.WavyProgressBar
import com.musfit.ui.components.gridGroupShape
import com.musfit.ui.text.asString
import com.musfit.ui.theme.AmberBright
import com.musfit.ui.theme.AmberContainerDark
import com.musfit.ui.theme.AmberInkDark
import com.musfit.ui.theme.Indigo
import com.musfit.ui.theme.IndigoBright
import com.musfit.ui.theme.IndigoContainer
import com.musfit.ui.theme.IndigoContainerDark
import com.musfit.ui.theme.IndigoInk
import com.musfit.ui.theme.IndigoInkDark
import com.musfit.ui.theme.MacroProtein
import com.musfit.ui.theme.MacroProteinDark
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccentRole
import com.musfit.ui.theme.VitalsAmber
import com.musfit.ui.theme.VitalsAmberContainer
import com.musfit.ui.theme.VitalsAmberDisplay
import com.musfit.ui.theme.VitalsAmberDisplayDark
import com.musfit.ui.theme.VitalsAmberOn
import com.musfit.ui.theme.VitalsIndigoDisplay
import com.musfit.ui.theme.VitalsIndigoDisplayDark
import com.musfit.ui.theme.VitalsRoseContainer
import com.musfit.ui.theme.VitalsRoseContainerDark
import com.musfit.ui.theme.VitalsRoseDisplay
import com.musfit.ui.theme.VitalsRoseDisplayDark
import com.musfit.ui.theme.VitalsRoseOn
import com.musfit.ui.theme.VitalsRoseOnDark
import com.musfit.ui.theme.VitalsWaterDisplay
import com.musfit.ui.theme.VitalsWaterDisplayDark
import com.musfit.ui.theme.VitalsWaterOn
import com.musfit.ui.theme.VitalsWaterOnDark
import com.musfit.ui.theme.Water
import com.musfit.ui.theme.WaterDark
import com.musfit.ui.theme.WaterFill
import com.musfit.ui.theme.WaterFillDark
import com.musfit.ui.theme.tabAccentFor

/**
 * Today's hero (Turn 8 §8a): a color-coded vitals grid — every pinned metric is
 * one tile, two per row, in grouped containment (4dp gaps, 28dp grid-outer /
 * 8dp inner corners). Each tile carries its metric-scoped color family (amber
 * kcal, indigo steps, rose protein, blue water), never the tab accent.
 */
@Composable
fun TodayVitalsGrid(
    vitals: List<MetricCardUiState>,
    onMetricClick: (TodayMetric) -> Unit,
) {
    if (vitals.isEmpty()) return
    val columns = 2
    val rows = vitals.chunked(columns)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEachIndexed { rowIndex, rowTiles ->
            Row(
                modifier = Modifier.height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                rowTiles.forEachIndexed { columnIndex, tile ->
                    VitalsTile(
                        card = tile,
                        shape = gridGroupShape(rowIndex, rows.size, columnIndex, columns, outer = 28.dp),
                        onClick = { onMetricClick(tile.metric) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
                if (rowTiles.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/**
 * One reusable vitals tile: overline (icon + label), display value, "of {goal}
 * · {pct}%" sub, and a mini wavy progress whose remaining track is translucent
 * white rather than a color tint. Goal-less metrics show value + caption only.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VitalsTile(
    card: MetricCardUiState,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val family = vitalsFamilyFor(card.metric)
    val figure = when (val value = card.value) {
        is MetricValueUiState.WithGoal -> value.figure.asString()
        is MetricValueUiState.Plain -> value.figure.asString()
        is MetricValueUiState.NoData -> "—"
    }
    val caption = when (val value = card.value) {
        is MetricValueUiState.WithGoal -> value.caption.asString()
        is MetricValueUiState.Plain -> value.caption.asString()
        is MetricValueUiState.NoData -> value.caption.asString()
    }
    val locale = LocalConfiguration.current.locales[0]
    val label = card.label.asString()
    Surface(
        onClick = onClick,
        color = family.container,
        shape = shape,
        modifier = modifier.semantics {
            role = Role.Button
            contentDescription = "$label: $figure $caption"
        },
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = vitalsTileIcon(card.metric),
                    contentDescription = null,
                    tint = family.onContainer,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = label.uppercase(locale),
                    style = MusFitTheme.typography.labelMedium.copy(fontSize = 11.5.sp, letterSpacing = 0.4.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = family.onContainer,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = figure,
                style = MusFitTheme.typography.headlineMedium.copy(fontSize = 28.sp, letterSpacing = (-0.8).sp),
                fontWeight = FontWeight.ExtraBold,
                color = family.display,
                maxLines = 1,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = caption,
                style = MusFitTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = family.onContainer,
                maxLines = 1,
            )
            (card.value as? MetricValueUiState.WithGoal)?.let { value ->
                Spacer(Modifier.height(8.dp))
                WavyProgressBar(
                    progress = value.progress,
                    color = family.primary,
                    trackColor = family.track,
                    strokeWidth = 4.dp,
                    amplitude = 2.5.dp,
                    wavelength = 16.dp,
                    live = true,
                )
            }
        }
    }
}

/**
 * A vitals tile's metric-scoped color family (Turn 8 Delta 1): container,
 * on-container ink, deeper display ink, and the wave/dot primary. The remaining
 * wave track is translucent white in every family — light neutral, not a tint.
 */
internal data class VitalsTileFamily(
    val container: Color,
    val onContainer: Color,
    val display: Color,
    val primary: Color,
    val track: Color,
)

private enum class VitalsPalette { Amber, Indigo, Rose, Water }

/**
 * Best-fit family per metric. The spec fixes the default four (kcal amber,
 * steps indigo, protein rose, water blue); the rest reuse the nearest family —
 * energy metrics go amber, activity indigo, body metrics rose, recovery blue.
 */
private fun vitalsPaletteFor(metric: TodayMetric): VitalsPalette = when (metric) {
    TodayMetric.Calories, TodayMetric.Carbs, TodayMetric.Fat,
    TodayMetric.ActiveCalories, TodayMetric.CalorieBalance, TodayMetric.LoggingStreak,
    -> VitalsPalette.Amber

    TodayMetric.Steps, TodayMetric.Sessions, TodayMetric.Exercise -> VitalsPalette.Indigo

    TodayMetric.Protein, TodayMetric.Weight, TodayMetric.BodyFat, TodayMetric.RestingHeartRate,
    -> VitalsPalette.Rose

    TodayMetric.Water, TodayMetric.Sleep -> VitalsPalette.Water
}

@Composable
internal fun vitalsFamilyFor(metric: TodayMetric): VitalsTileFamily {
    val dark = isSystemInDarkTheme()
    val track = Color.White.copy(alpha = if (dark) 0.25f else 0.75f)
    return when (vitalsPaletteFor(metric)) {
        VitalsPalette.Amber -> if (dark) {
            VitalsTileFamily(AmberContainerDark, AmberInkDark, VitalsAmberDisplayDark, AmberBright, track)
        } else {
            VitalsTileFamily(VitalsAmberContainer, VitalsAmberOn, VitalsAmberDisplay, VitalsAmber, track)
        }

        VitalsPalette.Indigo -> if (dark) {
            VitalsTileFamily(IndigoContainerDark, IndigoInkDark, VitalsIndigoDisplayDark, IndigoBright, track)
        } else {
            VitalsTileFamily(IndigoContainer, IndigoInk, VitalsIndigoDisplay, Indigo, track)
        }

        VitalsPalette.Rose -> if (dark) {
            VitalsTileFamily(VitalsRoseContainerDark, VitalsRoseOnDark, VitalsRoseDisplayDark, MacroProteinDark, track)
        } else {
            VitalsTileFamily(VitalsRoseContainer, VitalsRoseOn, VitalsRoseDisplay, MacroProtein, track)
        }

        VitalsPalette.Water -> if (dark) {
            VitalsTileFamily(WaterFillDark, VitalsWaterOnDark, VitalsWaterDisplayDark, WaterDark, track)
        } else {
            VitalsTileFamily(WaterFill, VitalsWaterOn, VitalsWaterDisplay, Water, track)
        }
    }
}

/** Tile overline glyphs from the 8a mock (restaurant / steps / exercise / water_drop). */
internal fun vitalsTileIcon(metric: TodayMetric): ImageVector = when (metric) {
    TodayMetric.Calories -> Icons.Filled.Restaurant
    TodayMetric.Steps -> Icons.AutoMirrored.Filled.DirectionsWalk
    TodayMetric.Protein -> Icons.Filled.FitnessCenter
    TodayMetric.Water -> Icons.Filled.WaterDrop
    else -> metricIcon(metric)
}

/** Emphasis glyph for the remaining metric pool (editor rows, non-default tiles). */
internal fun metricIcon(metric: TodayMetric): ImageVector = when (metric) {
    TodayMetric.Calories -> Icons.Filled.LocalFireDepartment
    TodayMetric.Protein -> Icons.Filled.Egg
    TodayMetric.Carbs -> Icons.Filled.BakeryDining
    TodayMetric.Fat -> Icons.Filled.Restaurant
    TodayMetric.Water -> Icons.Filled.WaterDrop
    TodayMetric.Steps -> Icons.AutoMirrored.Filled.DirectionsWalk
    TodayMetric.Weight -> Icons.Filled.MonitorWeight
    TodayMetric.BodyFat -> Icons.Filled.Percent
    TodayMetric.Sessions -> Icons.Filled.FitnessCenter
    TodayMetric.Sleep -> Icons.Filled.Bedtime
    TodayMetric.Exercise -> Icons.AutoMirrored.Filled.DirectionsRun
    TodayMetric.ActiveCalories -> Icons.Filled.Whatshot
    TodayMetric.RestingHeartRate -> Icons.Filled.MonitorHeart
    TodayMetric.CalorieBalance -> Icons.Filled.Balance
    TodayMetric.LoggingStreak -> Icons.Filled.EmojiEvents
}

/** Spec §1 metric-pool deep-link table: each metric's home tab. */
internal fun metricDestination(metric: TodayMetric): TodayNavigationTarget = when (metric) {
    TodayMetric.Calories, TodayMetric.Protein, TodayMetric.Carbs, TodayMetric.Fat,
    TodayMetric.Water, TodayMetric.CalorieBalance, TodayMetric.LoggingStreak,
    -> TodayNavigationTarget.Food

    TodayMetric.Sessions, TodayMetric.Exercise -> TodayNavigationTarget.Training

    TodayMetric.Steps, TodayMetric.Weight, TodayMetric.BodyFat,
    TodayMetric.Sleep, TodayMetric.ActiveCalories, TodayMetric.RestingHeartRate,
    -> TodayNavigationTarget.Profile
}

/** Edit sheet — the vitals-grid tile library: pin/order metrics + the Goals section. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardEditSheet(
    state: TodayUiState,
    onTogglePin: (TodayMetric) -> Unit,
    onMovePin: (TodayMetric, Boolean) -> Unit,
    onStepGoalChanged: (String) -> Unit,
    onSessionTargetChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val accent = tabAccentFor(TabAccentRole.Today)
    val layoutDirection = LocalLayoutDirection.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MusFitTheme.colors.surface,
        dragHandle = {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                BottomSheetDefaults.DragHandle()
            }
        },
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(MusFitTheme.spacing.sm),
            ) {
                Text(
                    text = stringResource(R.string.today_dashboard),
                    style = MusFitTheme.typography.titleLarge,
                    color = MusFitTheme.colors.onSurface,
                )
                Text(
                    text = stringResource(R.string.today_dashboard_explanation),
                    style = MusFitTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )

                // Pinned first (in order, with reorder controls), then the rest of the pool.
                val pinned = state.editPins
                pinned.forEachIndexed { index, metric ->
                    val metricLabel = metric.presentationLabel().asString()
                    // The last remaining pin cannot be unpinned — surface the rule as disabled.
                    val toggleEnabled = pinned.size > 1
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = true,
                                enabled = toggleEnabled,
                                role = Role.Checkbox,
                                onValueChange = { onTogglePin(metric) },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = true, onCheckedChange = null, enabled = toggleEnabled)
                        Text(
                            text = metricLabel,
                            style = MusFitTheme.typography.bodyMedium,
                            color = MusFitTheme.colors.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        val arrowColors =
                            IconButtonDefaults.iconButtonColors(contentColor = MusFitTheme.colors.onSurfaceVariant)
                        IconButton(
                            onClick = { onMovePin(metric, true) },
                            enabled = index > 0,
                            colors = arrowColors,
                            modifier = Modifier.size(48.dp).semantics { role = Role.Button },
                        ) {
                            Icon(
                                Icons.Outlined.ArrowUpward,
                                contentDescription = stringResource(R.string.today_move_metric_up, metricLabel),
                            )
                        }
                        IconButton(
                            onClick = { onMovePin(metric, false) },
                            enabled = index < pinned.lastIndex,
                            colors = arrowColors,
                            modifier = Modifier.size(48.dp).semantics { role = Role.Button },
                        ) {
                            Icon(
                                Icons.Outlined.ArrowDownward,
                                contentDescription = stringResource(R.string.today_move_metric_down, metricLabel),
                            )
                        }
                    }
                }
                TodayMetric.entries.filter { it !in pinned }.forEach { metric ->
                    val metricLabel = metric.presentationLabel().asString()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = false,
                                role = Role.Checkbox,
                                onValueChange = { onTogglePin(metric) },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = false, onCheckedChange = null)
                        Text(
                            text = metricLabel,
                            style = MusFitTheme.typography.bodyMedium,
                            color = MusFitTheme.colors.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(Modifier.height(MusFitTheme.spacing.sm))
                Text(
                    text = stringResource(R.string.today_goals),
                    style = MusFitTheme.typography.titleMedium,
                    color = MusFitTheme.colors.onSurface,
                )
                OutlinedTextField(
                    value = state.stepGoalInput,
                    onValueChange = onStepGoalChanged,
                    label = { Text(stringResource(R.string.today_daily_step_goal)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.sessionTargetInput,
                    onValueChange = onSessionTargetChanged,
                    label = { Text(stringResource(R.string.today_workouts_per_week)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent.color,
                        contentColor = accent.onColor,
                    ),
                ) {
                    Text(stringResource(R.string.today_save_dashboard))
                }
            }
        }
    }
}
