package com.musfit.ui.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musfit.ui.theme.MusFitTheme
import kotlin.math.roundToInt

// Self-contained "More details" tracker cards (water + Health Connect sync),
// extracted from FoodScreen.kt with no behavior change.

@Composable
internal fun WaterTrackerCard(
    state: FoodUiState,
    onQuickWaterClick: (Double) -> Unit,
    onCustomAmountChanged: (String) -> Unit,
    onCustomAddClick: () -> Unit,
    onGoalChanged: (String) -> Unit,
    onGoalSaveClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MusFitTheme.colors.surface),
        shape = MusFitTheme.shapes.small,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Water", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${state.waterConsumedMilliliters.roundToInt()} / ${state.waterGoalMilliliters.roundToInt()} ml",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
                Text(
                    "${(state.waterProgress * 100).roundToInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MusFitTheme.colors.brand,
                )
            }

            ProgressBar(progress = state.waterProgress.toFloat().coerceIn(0f, 1f), color = MusFitTheme.colors.water)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MusFitOutlinedButton(
                    onClick = { onQuickWaterClick(250.0) },
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("250 ml")
                }
                MusFitOutlinedButton(
                    onClick = { onQuickWaterClick(500.0) },
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("500 ml")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.waterCustomAmountInput,
                    onValueChange = onCustomAmountChanged,
                    label = { Text("Custom ml") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = onCustomAddClick,
                    enabled = !state.isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
                ) {
                    Text("Add")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.waterGoalInput,
                    onValueChange = onGoalChanged,
                    label = { Text("Goal ml") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                MusFitOutlinedButton(onClick = onGoalSaveClick, enabled = !state.isSaving) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
internal fun FoodHealthConnectSyncCard(
    state: FoodUiState,
    onEnabledChanged: (Boolean) -> Unit,
    onRequestPermissionsClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onSyncClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MusFitTheme.colors.surface),
        shape = MusFitTheme.shapes.small,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Health Connect", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        state.foodHealthConnectPermissionSummary,
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
                text = "Writes logged meals and water from Food.",
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
                    Text("Permissions", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                MusFitOutlinedButton(
                    onClick = onRefreshClick,
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Refresh", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Button(
                onClick = onSyncClick,
                enabled = state.foodHealthConnectSyncEnabled && state.foodHealthConnectCanSync && !state.isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Sync Food to Health Connect")
            }
        }
    }
}
