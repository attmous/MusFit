package com.musfit.ui.profile

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.musfit.data.repository.MEASUREMENT_TYPES
import com.musfit.data.repository.UserProfile
import com.musfit.domain.profile.ActivityLevel
import com.musfit.domain.profile.GoalType
import com.musfit.domain.profile.Sex
import java.time.LocalDate
import java.time.Period

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
    var currentWeightText by remember { mutableStateOf(initialWeightKg?.let { it.format1() } ?: "") }

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
                onSave(profile, currentWeightText.toPositiveDoubleOrNull())
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
    onDismiss: () -> Unit,
    onConfirm: (type: String, value: Double, unit: String) -> Unit,
) {
    var type by remember { mutableStateOf(MEASUREMENT_TYPES.first()) }
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
