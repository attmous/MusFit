package com.musfit.ui.profile

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.musfit.data.repository.MEASUREMENT_TYPES
import com.musfit.data.repository.UserProfile
import com.musfit.ui.AppDestination
import com.musfit.ui.components.charts.TrendLineChart
import com.musfit.ui.theme.tabAccentFor
import com.musfit.domain.profile.ActivityLevel
import com.musfit.domain.profile.GoalType
import com.musfit.domain.profile.Sex
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val MEASUREMENT_LABELS = mapOf(
    "waist" to "Waist", "chest" to "Chest", "arms" to "Arms",
    "thighs" to "Thighs", "hips" to "Hips", "body_fat" to "Body fat",
)
private val PACE_OPTIONS = listOf(0.25, 0.5, 0.75)

@Composable
fun ProfileEditDialog(
    initial: UserProfile,
    initialWeightKg: Double?,
    onDismiss: () -> Unit,
    onSave: (UserProfile, Double?) -> Unit,
) {
    var sex by remember { mutableStateOf(initial.sex) }
    var activity by remember { mutableStateOf(initial.activityLevel) }
    var goalType by remember { mutableStateOf(initial.goalType) }
    var pace by remember { mutableStateOf(initial.goalPaceKgPerWeek) }
    var ageText by remember {
        mutableStateOf(
            initial.birthDateEpochDay?.let {
                Period.between(LocalDate.ofEpochDay(it), LocalDate.now()).years.toString()
            } ?: "",
        )
    }
    var heightText by remember { mutableStateOf(initial.heightCm?.let { it.format1() } ?: "") }
    var goalWeightText by remember { mutableStateOf(initial.goalWeightKg?.let { it.format1() } ?: "") }
    val initialWeightText = remember { initialWeightKg?.let { it.format1() } ?: "" }
    var currentWeightText by remember { mutableStateOf(initialWeightText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your profile") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Sex")
                ChipGroup(
                    options = listOf(Sex.Male, Sex.Female),
                    isSelected = { it == sex },
                    onSelect = { sex = it },
                    label = { it.label() },
                )
                NumberField(value = ageText, onValueChange = { ageText = it }, label = "Age (years)")
                NumberField(value = heightText, onValueChange = { heightText = it }, label = "Height (cm)")

                Text("Activity level")
                ChipGroup(
                    options = ActivityLevel.entries,
                    isSelected = { it == activity },
                    onSelect = { activity = it },
                    label = { it.label() },
                )

                Text("Goal")
                ChipGroup(
                    options = GoalType.entries,
                    isSelected = { it == goalType },
                    onSelect = { goalType = it },
                    label = { it.label() },
                )
                if (goalType != GoalType.Maintain) {
                    Text("Pace (kg/week)")
                    ChipGroup(
                        options = PACE_OPTIONS,
                        isSelected = { it == pace },
                        onSelect = { pace = it },
                        label = { it.format1() },
                    )
                }
                NumberField(value = goalWeightText, onValueChange = { goalWeightText = it }, label = "Goal weight (kg)")
                NumberField(value = currentWeightText, onValueChange = { currentWeightText = it }, label = "Current weight (kg)")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val ageYears = ageText.trim().toIntOrNull()
                val profile = UserProfile(
                    sex = sex,
                    birthDateEpochDay = ageYears?.let { LocalDate.now().minusYears(it.toLong()).toEpochDay() },
                    heightCm = heightText.toPositiveDoubleOrNull(),
                    activityLevel = activity,
                    goalType = goalType,
                    goalPaceKgPerWeek = if (goalType == GoalType.Maintain) 0.0 else pace,
                    goalWeightKg = goalWeightText.toPositiveDoubleOrNull(),
                )
                // Only pass a weight the user actually changed — round-tripping the
                // prefill would log a duplicate entry (and bias the weekly-average
                // delta) on every profile save. Compare parsed values, not text:
                // "80" vs "80.0" is not a change.
                val editedWeightKg = currentWeightText.toPositiveDoubleOrNull()
                    ?.takeIf { it != initialWeightText.toPositiveDoubleOrNull() }
                onSave(profile, editedWeightKg)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun LogWeightDialog(
    prefillKg: Double?,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    var text by remember { mutableStateOf(prefillKg?.let { it.format1() } ?: "") }
    val parsed = text.toPositiveDoubleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log weight") },
        text = { NumberField(value = text, onValueChange = { text = it }, label = "Weight (kg)") },
        confirmButton = {
            TextButton(enabled = parsed != null, onClick = { parsed?.let(onConfirm) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun LogMeasurementDialog(
    initialType: String = MEASUREMENT_TYPES.first(),
    onDismiss: () -> Unit,
    onConfirm: (type: String, value: Double, unit: String) -> Unit,
) {
    // Keyed so a still-composed dialog re-seeds when the caller retargets it to another type.
    var type by remember(initialType) { mutableStateOf(initialType) }
    var text by remember { mutableStateOf("") }
    val parsed = text.toPositiveDoubleOrNull()
    val unit = if (type == "body_fat") "%" else "cm"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log measurement") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ChipGroup(
                    options = MEASUREMENT_TYPES,
                    isSelected = { it == type },
                    onSelect = { type = it },
                    label = { MEASUREMENT_LABELS[it] ?: it },
                )
                NumberField(value = text, onValueChange = { text = it }, label = "Value ($unit)")
            }
        },
        confirmButton = {
            TextButton(enabled = parsed != null, onClick = { parsed?.let { onConfirm(type, it, unit) } }) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun <T> ChipGroup(
    options: List<T>,
    isSelected: (T) -> Boolean,
    onSelect: (T) -> Unit,
    label: (T) -> String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = isSelected(option),
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
            )
        }
    }
}

@Composable
private fun NumberField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun String.toPositiveDoubleOrNull(): Double? =
    trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }

data class EntrySheetItem(val id: String, val dateLabel: String, val value: Double, val unit: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntriesSheet(
    title: String,
    items: List<EntrySheetItem>,
    onDismiss: () -> Unit,
    onEdit: (id: String, value: Double) -> Unit,
    onDelete: (id: String) -> Unit,
    chartSeries: List<Double> = emptyList(),
) {
    var editing by remember { mutableStateOf<EntrySheetItem?>(null) }
    var deleting by remember { mutableStateOf<EntrySheetItem?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (chartSeries.size >= 2) {
                TrendLineChart(
                    values = chartSeries,
                    accent = tabAccentFor(AppDestination.Profile).color,
                    modifier = Modifier.fillMaxWidth().height(96.dp).padding(vertical = 8.dp),
                )
            }
            if (items.isEmpty()) {
                Text(
                    "No entries yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${item.value.format1()} ${item.unit}", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            item.dateLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { editing = item }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { deleting = item }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }

    editing?.let { item ->
        var text by remember(item.id) { mutableStateOf(item.value.format1()) }
        val parsed = text.toPositiveDoubleOrNull()
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Edit value") },
            text = { NumberField(value = text, onValueChange = { text = it }, label = "Value (${item.unit})") },
            confirmButton = {
                TextButton(enabled = parsed != null, onClick = {
                    parsed?.let { onEdit(item.id, it) }
                    editing = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }

    deleting?.let { item ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete entry?") },
            text = { Text("${item.value.format1()} ${item.unit} · ${item.dateLabel}") },
            confirmButton = {
                TextButton(onClick = { onDelete(item.id); deleting = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

internal fun formatEntryDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        .format(DateTimeFormatter.ofPattern("d MMM yyyy"))
