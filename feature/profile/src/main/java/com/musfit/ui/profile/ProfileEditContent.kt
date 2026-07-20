@file:OptIn(ExperimentalMaterial3Api::class)

package com.musfit.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.musfit.feature.profile.R
import com.musfit.ui.components.PillButton
import com.musfit.ui.components.SheetDragHandle
import com.musfit.ui.icons.automirrored.outlined.TrendingDown
import com.musfit.ui.icons.automirrored.outlined.TrendingFlat
import com.musfit.ui.icons.automirrored.outlined.TrendingUp
import com.musfit.ui.icons.outlined.Remove
import com.musfit.ui.text.LocalizedFormatter
import com.musfit.ui.theme.LavenderBody
import com.musfit.ui.theme.LavenderBodyDark
import com.musfit.ui.theme.LavenderContainer
import com.musfit.ui.theme.LavenderContainerDark
import com.musfit.ui.theme.LavenderInk
import com.musfit.ui.theme.LavenderInkDark
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import com.musfit.ui.theme.TabAccentRole
import com.musfit.ui.theme.tabAccentFor
import java.time.LocalDate
import java.time.Period

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
    val accent = tabAccentFor(TabAccentRole.Profile)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var sexName by rememberSaveable { mutableStateOf(initial.sex?.name) }
    var activityName by rememberSaveable { mutableStateOf(initial.activityLevel.name) }
    var goalTypeName by rememberSaveable { mutableStateOf(initial.goalType.name) }
    val sex = sexName?.let(Sex::valueOf)
    val activity = ActivityLevel.valueOf(activityName)
    val goalType = GoalType.valueOf(goalTypeName)
    var pace by rememberSaveable {
        mutableDoubleStateOf(initial.goalPaceKgPerWeek.takeIf { it > 0.0 } ?: 0.3)
    }
    var ageText by rememberSaveable {
        mutableStateOf(
            initial.birthDateEpochDay?.let {
                Period.between(LocalDate.ofEpochDay(it), LocalDate.now()).years.toString()
            } ?: "",
        )
    }
    var heightText by rememberSaveable { mutableStateOf(initial.heightCm?.let { it.format1() } ?: "") }
    var goalWeightText by rememberSaveable { mutableStateOf(initial.goalWeightKg?.let { it.format1() } ?: "") }
    val initialWeightText = rememberSaveable { initialWeightKg?.let { it.format1() } ?: "" }
    var currentWeightText by rememberSaveable { mutableStateOf(initialWeightText) }

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
                        stringResource(R.string.profile_your_profile),
                        style = MusFitTheme.typography.headlineMedium.copy(fontSize = 22.sp, lineHeight = 25.sp),
                    )
                    Text(
                        stringResource(R.string.profile_targets_explanation),
                        style = MusFitTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                ProfileCircleIconButton(
                    icon = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.profile_close),
                    container = MusFitTheme.colors.surfaceVariant,
                    content = MusFitTheme.colors.onSurface,
                    onClick = onDismiss,
                )
            }

            SheetFieldLabel(stringResource(R.string.profile_sex))
            ConnectedSegmentRow(
                options = listOf(Sex.Male, Sex.Female),
                selected = sex,
                label = { stringResource(it.labelResource()) },
                accent = accent,
                onSelect = { sexName = it.name },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileFieldTile(
                    label = stringResource(R.string.profile_age),
                    value = ageText,
                    onValueChange = { ageText = it },
                    unit = stringResource(R.string.profile_unit_years),
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
                ProfileFieldTile(
                    label = stringResource(R.string.profile_height),
                    value = heightText,
                    onValueChange = { heightText = it },
                    unit = stringResource(R.string.profile_unit_cm),
                    modifier = Modifier.weight(1f),
                )
            }

            SheetFieldLabel(stringResource(R.string.profile_activity_level))
            // Five levels don't fit one connected row; split into two rows that
            // share the selection (a documented deviation from the 3-segment mock).
            ConnectedSegmentRow(
                options = listOf(ActivityLevel.Sedentary, ActivityLevel.Light, ActivityLevel.Moderate),
                selected = activity,
                label = { stringResource(it.labelResource()) },
                accent = accent,
                onSelect = { activityName = it.name },
            )
            ConnectedSegmentRow(
                options = listOf(ActivityLevel.Active, ActivityLevel.VeryActive),
                selected = activity,
                label = { stringResource(it.labelResource()) },
                accent = accent,
                onSelect = { activityName = it.name },
            )

            SheetFieldLabel(stringResource(R.string.profile_goal))
            ConnectedSegmentRow(
                options = GoalType.entries,
                selected = goalType,
                label = { it.sheetLabel() },
                accent = accent,
                onSelect = { goalTypeName = it.name },
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
                    label = stringResource(R.string.profile_goal_weight_label),
                    value = goalWeightText,
                    onValueChange = { goalWeightText = it },
                    unit = stringResource(R.string.profile_unit_kg),
                    modifier = Modifier.weight(1f),
                )
                ProfileFieldTile(
                    label = stringResource(R.string.profile_current_weight),
                    value = currentWeightText,
                    onValueChange = { currentWeightText = it },
                    unit = stringResource(R.string.profile_unit_kg),
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
                text = stringResource(R.string.profile_save),
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
            ProfileCancelAction(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterHorizontally),
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
                    stringResource(R.string.profile_pace),
                    style = MusFitTheme.typography.labelSmall.copy(fontWeight = FontWeight.W800, letterSpacing = 0.8.sp),
                    color = accent.onContainer,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        pace.format1(LocalConfiguration.current.locales[0]),
                        style = MusFitTheme.typography.displaySmall.copy(fontSize = 24.sp, lineHeight = 24.sp),
                        color = accent.onContainer,
                    )
                    Text(
                        stringResource(R.string.profile_unit_kg_per_week),
                        style = MusFitTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        fontWeight = FontWeight.Medium,
                        color = accent.onContainerVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 1.dp),
                    )
                }
            }
            PaceStepperCircle(
                icon = Icons.Outlined.Remove,
                contentDescription = stringResource(R.string.profile_decrease_pace),
                container = MusFitTheme.colors.surface,
                content = accent.onContainer,
                onClick = onDecrease,
            )
            PaceStepperCircle(
                icon = Icons.Outlined.Add,
                contentDescription = stringResource(R.string.profile_increase_pace),
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
    ProfileCircleIconButton(
        icon = icon,
        contentDescription = contentDescription,
        container = container,
        content = content,
        onClick = onClick,
    )
}

@Composable
private fun ProfileCircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .semantics { this.contentDescription = contentDescription }
            .clip(CircleShape)
            .clickable(
                onClickLabel = contentDescription,
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        Surface(
            color = container,
            contentColor = content,
            shape = CircleShape,
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            }
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
    val actionEnabled = applyState != TargetApplyState.Applying && stateForTargets != TargetApplyState.Success
    Surface(color = container, shape = RoundedCornerShape(99.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ApplyTargetsSummary(
                targets = targets,
                state = stateForTargets,
                color = body,
                modifier = Modifier.weight(1f),
            )
            ApplyTargetsAction(
                state = stateForTargets,
                enabled = actionEnabled,
                color = action,
                onClick = { onApply(targets) },
            )
        }
    }
}

@Composable
private fun ApplyTargetsSummary(
    targets: RecommendedTargets,
    state: TargetApplyState,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val summary = buildAnnotatedString {
        append(stringResource(R.string.profile_new_targets_prefix))
        withStyle(SpanStyle(fontWeight = FontWeight.W800)) {
            append(
                stringResource(
                    R.string.profile_value_kcal,
                    LocalizedFormatter.integer(targets.caloriesKcal.toLong(), locale = locale),
                ),
            )
        }
        append(stringResource(R.string.profile_separator_middle_dot))
        append(
            stringResource(
                R.string.profile_value_protein_grams,
                LocalizedFormatter.integer(targets.proteinGrams.toLong(), locale = locale),
            ),
        )
        if (state == TargetApplyState.Failure) {
            append('\n')
            append(stringResource(R.string.profile_apply_targets_failed))
        }
    }
    Text(
        summary,
        style = MusFitTheme.typography.bodySmall.copy(fontSize = 12.sp),
        color = color,
        modifier = modifier,
    )
}

@Composable
private fun ApplyTargetsAction(
    state: TargetApplyState,
    enabled: Boolean,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    val label = when (state) {
        TargetApplyState.Idle -> stringResource(R.string.profile_apply_to_food)
        TargetApplyState.Applying -> stringResource(R.string.profile_applying)
        TargetApplyState.Success -> stringResource(R.string.profile_applied)
        TargetApplyState.Failure -> stringResource(R.string.profile_retry)
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = androidx.compose.ui.graphics.Color.Transparent,
        shape = RoundedCornerShape(99.dp),
        modifier = Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .semantics { role = Role.Button },
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                label,
                style = MusFitTheme.typography.labelMedium.copy(fontSize = 12.5.sp, fontWeight = FontWeight.W800),
                color = color,
            )
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
@Composable
private fun GoalType.sheetLabel(): String = when (this) {
    GoalType.Lose -> stringResource(R.string.profile_goal_lose)
    GoalType.Maintain -> stringResource(R.string.profile_goal_keep)
    GoalType.Gain -> stringResource(R.string.profile_goal_gain)
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
        title = { Text(stringResource(R.string.profile_log_weight)) },
        text = { NumberField(value = text, onValueChange = { text = it }, label = stringResource(R.string.profile_weight_kg)) },
        confirmButton = {
            TextButton(enabled = parsed != null, onClick = { parsed?.let(onConfirm) }) { Text(stringResource(R.string.profile_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.profile_cancel)) } },
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
        title = { Text(stringResource(R.string.profile_log_measurement)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ChipGroup(
                    options = MEASUREMENT_TYPES,
                    isSelected = { it == type },
                    onSelect = { type = it },
                    label = { typeKey ->
                        MEASUREMENT_LABEL_RESOURCES[typeKey]?.let { stringResource(it) } ?: typeKey
                    },
                )
                NumberField(
                    value = text,
                    onValueChange = { text = it },
                    label = stringResource(R.string.profile_value_with_unit, unit),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = parsed != null, onClick = { parsed?.let { onConfirm(type, it, unit) } }) {
                Text(stringResource(R.string.profile_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.profile_cancel)) } },
    )
}

@Composable
private fun <T> ChipGroup(
    options: List<T>,
    isSelected: (T) -> Boolean,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
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

internal fun String.toPositiveDoubleOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
