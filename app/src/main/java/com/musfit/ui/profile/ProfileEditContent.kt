@file:OptIn(ExperimentalMaterial3Api::class)

package com.musfit.ui.profile

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musfit.data.repository.MEASUREMENT_TYPES
import com.musfit.data.repository.UserProfile
import com.musfit.domain.profile.ActivityLevel
import com.musfit.domain.profile.EnergyCalculator
import com.musfit.domain.profile.GoalType
import com.musfit.domain.profile.RecommendedTargets
import com.musfit.domain.profile.Sex
import com.musfit.ui.AppDestination
import com.musfit.ui.components.PillButton
import com.musfit.ui.components.SheetDragHandle
import com.musfit.ui.theme.LavenderBody
import com.musfit.ui.theme.LavenderBodyDark
import com.musfit.ui.theme.LavenderContainer
import com.musfit.ui.theme.LavenderContainerDark
import com.musfit.ui.theme.LavenderInk
import com.musfit.ui.theme.LavenderInkDark
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import com.musfit.ui.theme.tabAccentFor
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import java.time.LocalDate
import java.time.Period
import java.util.Locale

private val MEASUREMENT_LABELS = mapOf(
    "waist" to "Waist", "chest" to "Chest", "arms" to "Arms",
    "thighs" to "Thighs", "hips" to "Hips", "body_fat" to "Body fat",
)

private const val PACE_STEP = 0.1
private const val PACE_MIN = 0.1
private const val PACE_MAX = 1.0

/**
 * The Turn 11 "Your profile" bottom sheet (11e), replacing the old
 * `ProfileEditDialog`: connected segment groups for sex/activity/goal, white
 * field tiles, the tonal pace mini-hero with steppers, a live lilac
 * "New targets" banner, and a filled Save pill. Fields map 1:1 to the old
 * dialog; Save persists the profile plus an optional changed weight.
 */
@Composable
fun ProfileEditSheet(
    initial: UserProfile,
    initialWeightKg: Double?,
    targetApplyState: TargetApplyState = TargetApplyState.Idle,
    targetApplyTargets: RecommendedTargets? = null,
    onDismiss: () -> Unit,
    onSave: (UserProfile, Double?) -> Unit,
    onApplyTargets: ((RecommendedTargets) -> Unit)? = null,
) {
    val accent = tabAccentFor(AppDestination.Profile)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var sex by remember { mutableStateOf(initial.sex) }
    var activity by remember { mutableStateOf(initial.activityLevel) }
    var goalType by remember { mutableStateOf(initial.goalType) }
    var pace by remember {
        mutableStateOf(initial.goalPaceKgPerWeek.takeIf { it > 0.0 } ?: 0.3)
    }
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

    val liveTargets = liveRecommendedTargets(
        sex = sex,
        ageText = ageText,
        heightText = heightText,
        currentWeightText = currentWeightText,
        activity = activity,
        goalType = goalType,
        pace = pace,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MusFitTheme.colors.background,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)) { SheetDragHandle() }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Your profile",
                        style = MusFitTheme.typography.headlineMedium.copy(fontSize = 22.sp, lineHeight = 25.sp),
                    )
                    Text(
                        "Sets your calorie and macro targets",
                        style = MusFitTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Surface(
                    onClick = onDismiss,
                    color = MusFitTheme.colors.surfaceVariant,
                    contentColor = MusFitTheme.colors.onSurface,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }
            }

            SheetFieldLabel("Sex")
            ConnectedSegmentRow(
                options = listOf(Sex.Male, Sex.Female),
                selected = sex,
                label = { it.label() },
                accent = accent,
                onSelect = { sex = it },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileFieldTile(
                    label = "Age",
                    value = ageText,
                    onValueChange = { ageText = it },
                    unit = "years",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
                ProfileFieldTile(
                    label = "Height",
                    value = heightText,
                    onValueChange = { heightText = it },
                    unit = "cm",
                    modifier = Modifier.weight(1f),
                )
            }

            SheetFieldLabel("Activity level")
            // Five levels don't fit one connected row; split into two rows that
            // share the selection (a documented deviation from the 3-segment mock).
            ConnectedSegmentRow(
                options = listOf(ActivityLevel.Sedentary, ActivityLevel.Light, ActivityLevel.Moderate),
                selected = activity,
                label = { it.label() },
                accent = accent,
                onSelect = { activity = it },
            )
            ConnectedSegmentRow(
                options = listOf(ActivityLevel.Active, ActivityLevel.VeryActive),
                selected = activity,
                label = { it.label() },
                accent = accent,
                onSelect = { activity = it },
            )

            SheetFieldLabel("Goal")
            ConnectedSegmentRow(
                options = GoalType.entries,
                selected = goalType,
                label = { it.sheetLabel() },
                accent = accent,
                onSelect = { goalType = it },
                optionIcon = { it.trendIcon() },
            )

            if (goalType != GoalType.Maintain) {
                PaceMiniHero(
                    pace = pace,
                    accent = accent,
                    onDecrease = { pace = (pace - PACE_STEP).coerceAtLeast(PACE_MIN).roundToPaceStep() },
                    onIncrease = { pace = (pace + PACE_STEP).coerceAtMost(PACE_MAX).roundToPaceStep() },
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileFieldTile(
                    label = "Goal weight",
                    value = goalWeightText,
                    onValueChange = { goalWeightText = it },
                    unit = "kg",
                    modifier = Modifier.weight(1f),
                )
                ProfileFieldTile(
                    label = "Current weight",
                    value = currentWeightText,
                    onValueChange = { currentWeightText = it },
                    unit = "kg",
                    modifier = Modifier.weight(1f),
                )
            }

            if (liveTargets != null && onApplyTargets != null) {
                ApplyTargetsBanner(
                    targets = liveTargets,
                    applyState = targetApplyState,
                    requestedTargets = targetApplyTargets,
                    onApply = onApplyTargets,
                )
            }

            PillButton(
                text = "Save",
                onClick = {
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
                },
                containerColor = accent.color,
                contentColor = accent.onColor,
                height = 50.dp,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
            Text(
                "Cancel",
                style = MusFitTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.W800),
                color = MusFitTheme.colors.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(99.dp))
                    .clickable(onClickLabel = "Cancel", onClick = onDismiss)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun SheetFieldLabel(text: String) {
    Text(
        text,
        style = MusFitTheme.typography.labelMedium.copy(fontSize = 12.5.sp, fontWeight = FontWeight.W700),
        color = MusFitTheme.colors.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

/** The tonal PACE mini-hero (r20): overline, 24/800 value, −/＋ stepper circles. */
@Composable
private fun PaceMiniHero(
    pace: Double,
    accent: TabAccent,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Surface(color = accent.container, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "PACE",
                    style = MusFitTheme.typography.labelSmall.copy(fontWeight = FontWeight.W800, letterSpacing = 0.8.sp),
                    color = accent.onContainer,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        pace.format1(),
                        style = MusFitTheme.typography.displaySmall.copy(fontSize = 24.sp, lineHeight = 24.sp),
                        color = accent.onContainer,
                    )
                    Text(
                        "kg/week",
                        style = MusFitTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        fontWeight = FontWeight.Medium,
                        color = accent.onContainerVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 1.dp),
                    )
                }
            }
            PaceStepperCircle(
                icon = Icons.Outlined.Remove,
                contentDescription = "Decrease pace",
                container = MusFitTheme.colors.surface,
                content = accent.onContainer,
                onClick = onDecrease,
            )
            PaceStepperCircle(
                icon = Icons.Outlined.Add,
                contentDescription = "Increase pace",
                container = accent.color,
                content = accent.onColor,
                onClick = onIncrease,
            )
        }
    }
}

@Composable
private fun PaceStepperCircle(
    icon: ImageVector,
    contentDescription: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = container,
        contentColor = content,
        shape = CircleShape,
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * The lilac apply banner: live-recomputed targets with an "Apply to Food"
 * action. Only repository-confirmed success is acknowledged as applied.
 */
@Composable
private fun ApplyTargetsBanner(
    targets: RecommendedTargets,
    applyState: TargetApplyState,
    requestedTargets: RecommendedTargets?,
    onApply: (RecommendedTargets) -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val container = if (dark) LavenderContainerDark else LavenderContainer
    val body = if (dark) LavenderBodyDark else LavenderBody
    val action = if (dark) LavenderInkDark else LavenderInk
    val stateForTargets = targetApplyStateForTargets(applyState, requestedTargets, targets)
    val actionLabel = when (stateForTargets) {
        TargetApplyState.Idle -> "Apply to Food"
        TargetApplyState.Applying -> "Applying…"
        TargetApplyState.Success -> "Applied"
        TargetApplyState.Failure -> "Retry"
    }
    val actionEnabled = applyState != TargetApplyState.Applying && stateForTargets != TargetApplyState.Success
    Surface(color = container, shape = RoundedCornerShape(99.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                buildAnnotatedString {
                    append("New targets: ")
                    withStyle(SpanStyle(fontWeight = FontWeight.W800)) {
                        append(String.format(Locale.US, "%,d kcal", targets.caloriesKcal.toInt()))
                    }
                    append(" · ${targets.proteinGrams.toInt()} g protein")
                    if (stateForTargets == TargetApplyState.Failure) {
                        append("\nCouldn't apply targets. Try again.")
                    }
                },
                style = MusFitTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = body,
                modifier = Modifier.weight(1f),
            )
            Surface(
                onClick = { onApply(targets) },
                enabled = actionEnabled,
                color = androidx.compose.ui.graphics.Color.Transparent,
                shape = RoundedCornerShape(99.dp),
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text(
                        actionLabel,
                        style = MusFitTheme.typography.labelMedium.copy(
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.W800,
                        ),
                        color = action,
                    )
                }
            }
        }
    }
}

internal fun targetApplyStateForTargets(
    applyState: TargetApplyState,
    requestedTargets: RecommendedTargets?,
    currentTargets: RecommendedTargets,
): TargetApplyState = when {
    applyState == TargetApplyState.Applying -> TargetApplyState.Applying
    requestedTargets == currentTargets -> applyState
    else -> TargetApplyState.Idle
}

/** Live targets from the draft fields — null until every input parses. */
private fun liveRecommendedTargets(
    sex: Sex?,
    ageText: String,
    heightText: String,
    currentWeightText: String,
    activity: ActivityLevel,
    goalType: GoalType,
    pace: Double,
): RecommendedTargets? {
    val ageYears = ageText.trim().toIntOrNull()?.takeIf { it in 10..120 } ?: return null
    val heightCm = heightText.toPositiveDoubleOrNull() ?: return null
    val weightKg = currentWeightText.toPositiveDoubleOrNull() ?: return null
    if (sex == null) return null
    return EnergyCalculator.recommendedTargets(
        sex = sex,
        weightKg = weightKg,
        heightCm = heightCm,
        ageYears = ageYears,
        activityLevel = activity,
        goalType = goalType,
        goalPaceKgPerWeek = if (goalType == GoalType.Maintain) 0.0 else pace,
    )
}

private fun Double.roundToPaceStep(): Double = Math.round(this * 10.0) / 10.0

/** The sheet renames Maintain to the mock's "Keep"; storage is unchanged. */
private fun GoalType.sheetLabel(): String = when (this) {
    GoalType.Lose -> "Lose"
    GoalType.Maintain -> "Keep"
    GoalType.Gain -> "Gain"
}

private fun GoalType.trendIcon(): ImageVector = when (this) {
    GoalType.Lose -> Icons.AutoMirrored.Outlined.TrendingDown
    GoalType.Maintain -> Icons.AutoMirrored.Outlined.TrendingFlat
    GoalType.Gain -> Icons.AutoMirrored.Outlined.TrendingUp
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

internal fun String.toPositiveDoubleOrNull(): Double? =
    trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
