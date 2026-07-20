package com.musfit.ui.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musfit.feature.food.R
import com.musfit.ui.icons.filled.Remove
import com.musfit.ui.text.asString
import com.musfit.ui.theme.MusFitTheme
import kotlin.math.roundToInt
import com.musfit.core.designsystem.R as DesignR

// Self-contained tracker panels (water + Health Connect sync) rendered inside
// the Food modal sheets. The sheet container is already colors.surface, so the
// panels draw naked — no card chrome.

@Composable
@Suppress("LongMethod", "LongParameterList")
internal fun WaterTrackerCard(
    state: FoodTrackerUiState,
    onQuickWaterClick: (Double) -> Unit,
    onRemoveWaterClick: (Double) -> Unit,
    onCustomAmountChanged: (String) -> Unit,
    onCustomAddClick: () -> Unit,
    onCustomRemoveClick: () -> Unit,
    onGoalChanged: (String) -> Unit,
    onGoalSaveClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.food_water), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(
                        R.string.food_water_progress,
                        state.waterConsumedMilliliters.roundToInt(),
                        state.waterGoalMilliliters.roundToInt(),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
            Text(
                stringResource(R.string.food_percentage, (state.waterProgress * 100).roundToInt()),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MusFitTheme.colors.brand,
            )
        }

        ProgressBar(progress = state.waterProgress.toFloat().coerceIn(0f, 1f), color = MusFitTheme.colors.water)

        // Each preset is a "− amount +" stepper so a mistaken add can be undone. The
        // "−" is disabled once the day is empty (there is nothing left to remove).
        val canRemoveWater = state.waterConsumedMilliliters > 0.0
        WaterQuickStepperRow(
            amountMilliliters = 250.0,
            addEnabled = !state.isSaving,
            removeEnabled = !state.isSaving && canRemoveWater,
            onRemove = onRemoveWaterClick,
            onAdd = onQuickWaterClick,
        )
        WaterQuickStepperRow(
            amountMilliliters = 500.0,
            addEnabled = !state.isSaving,
            removeEnabled = !state.isSaving && canRemoveWater,
            onRemove = onRemoveWaterClick,
            onAdd = onQuickWaterClick,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = state.waterCustomAmountInput,
                onValueChange = onCustomAmountChanged,
                label = { Text(stringResource(R.string.food_custom_milliliters)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            MusFitOutlinedButton(
                onClick = onCustomRemoveClick,
                enabled = !state.isSaving && canRemoveWater,
            ) {
                Text(stringResource(DesignR.string.common_remove))
            }
            Button(
                onClick = onCustomAddClick,
                enabled = !state.isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
            ) {
                Text(stringResource(DesignR.string.common_add))
            }
        }

        HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = state.waterGoalInput,
                onValueChange = onGoalChanged,
                label = { Text(stringResource(R.string.food_goal_milliliters)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            MusFitOutlinedButton(onClick = onGoalSaveClick, enabled = !state.isSaving) {
                Text(stringResource(DesignR.string.common_save))
            }
        }
    }
}

/**
 * A single water preset rendered as a "− amount +" stepper: the "+" logs the preset,
 * the "−" removes it. Keeps add and remove side by side so undoing an accidental add
 * is obvious.
 */
@Composable
private fun WaterQuickStepperRow(
    amountMilliliters: Double,
    addEnabled: Boolean,
    removeEnabled: Boolean,
    onRemove: (Double) -> Unit,
    onAdd: (Double) -> Unit,
) {
    val label = stringResource(R.string.food_milliliters, amountMilliliters.roundToInt())
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        MusFitOutlinedButton(
            onClick = { onRemove(amountMilliliters) },
            enabled = removeEnabled,
        ) {
            Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.food_remove_amount, label))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = MusFitTheme.colors.onSurface,
            modifier = Modifier.weight(1f),
        )
        MusFitOutlinedButton(
            onClick = { onAdd(amountMilliliters) },
            enabled = addEnabled,
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.food_add_amount, label))
        }
    }
}

@Composable
@Suppress("LongMethod")
internal fun FoodHealthConnectSyncCard(
    state: FoodTrackerUiState,
    onEnabledChanged: (Boolean) -> Unit,
    onRequestPermissionsClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onSyncClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.food_health_connect), style = MaterialTheme.typography.titleMedium)
                Text(
                    state.foodHealthConnectPermissionSummary.asString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
            Switch(
                checked = state.foodHealthConnectSyncEnabled,
                onCheckedChange = onEnabledChanged,
                enabled = !state.isSaving,
            )
        }

        Text(
            text = stringResource(R.string.food_health_connect_write_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MusFitTheme.colors.onSurfaceVariant,
        )

        state.foodHealthConnectLastFailureMessage?.let { failure ->
            Text(
                text = failure,
                style = MaterialTheme.typography.bodySmall,
                color = MusFitTheme.colors.warning,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MusFitOutlinedButton(
                onClick = onRequestPermissionsClick,
                enabled = state.foodHealthConnectCanRequestPermissions && !state.isSaving,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.food_permissions), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            MusFitOutlinedButton(
                onClick = onRefreshClick,
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.food_refresh), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Button(
            onClick = onSyncClick,
            enabled = state.foodHealthConnectSyncEnabled && state.foodHealthConnectCanSync && !state.isSaving,
            colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.food_sync_to_health_connect))
        }
    }
}
