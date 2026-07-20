package com.musfit.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musfit.data.repository.ExerciseGrouping
import com.musfit.data.repository.SupersetGroup
import com.musfit.data.repository.WorkoutExerciseBlock
import com.musfit.data.repository.WorkoutHistoryDetail
import com.musfit.data.repository.WorkoutHistorySummary
import com.musfit.data.repository.WorkoutRecapSummary
import com.musfit.domain.training.WorkoutCalculator
import com.musfit.feature.training.R
import com.musfit.ui.components.ExpressiveBadge
import com.musfit.ui.components.ExpressiveBadgeShape
import com.musfit.ui.components.InnerScreenTitleStyle
import com.musfit.ui.components.PillButton
import com.musfit.ui.components.TonalHeaderIconButton
import com.musfit.ui.components.WavyProgressBar
import com.musfit.ui.components.groupedShape
import com.musfit.ui.text.LocalizedFormatter
import com.musfit.ui.text.UiText
import com.musfit.ui.text.asString
import com.musfit.ui.text.pluralUiText
import com.musfit.ui.text.uiText
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Workout summary (Turn 10 §10b) — shown right after finishing a workout and
 * when a history row is opened: duration hero with a full wavy line and stat
 * chips, personal-record rows, a coach note, the per-set breakdown, and a Done
 * pill. The mock's edit square is omitted — no session editor exists yet.
 */
@Composable
@Suppress("LongMethod")
fun WorkoutCompleteContent(
    detail: WorkoutHistoryDetail,
    accent: TabAccent,
    onClose: () -> Unit,
    onOpenCoach: () -> Unit,
    showCloseAction: Boolean = true,
) {
    val recap = detail.effectiveRecap()
    val locale = LocalConfiguration.current.locales[0]
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.training_workout_complete),
                    style = InnerScreenTitleStyle,
                    color = MusFitTheme.colors.onSurface,
                )
                Text(
                    text = workoutCompleteSubtitle(detail.summary, locale),
                    style = MaterialTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            if (showCloseAction) {
                TonalHeaderIconButton(
                    icon = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.training_close_workout_summary),
                    onClick = onClose,
                )
            }
        }

        WorkoutDurationHero(recap = recap, accent = accent)

        val prs = workoutPrDisplays(detail, locale)
        if (prs.isNotEmpty()) {
            Column {
                Text(
                    text = stringResource(R.string.training_personal_records),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = MusFitTheme.colors.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    prs.forEachIndexed { index, pr ->
                        Surface(
                            color = MusFitTheme.colors.surface,
                            shape = groupedShape(index, prs.size),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                ExpressiveBadge(
                                    icon = Icons.Filled.EmojiEvents,
                                    shape = if (index % 2 == 0) ExpressiveBadgeShape.Sunny else ExpressiveBadgeShape.Circle,
                                    containerColor = MusFitTheme.colors.warningContainer,
                                    contentColor = MusFitTheme.colors.warning,
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pr.exerciseName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MusFitTheme.colors.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = pr.meta.asString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MusFitTheme.colors.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 1.dp),
                                    )
                                }
                                Text(
                                    text = pr.deltaLabel.asString(),
                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MusFitTheme.colors.positive,
                                )
                            }
                        }
                    }
                }
            }
        }

        WorkoutCoachNoteCard(note = workoutCompleteCoachNote(recap), onOpenCoach = onOpenCoach)

        historyDetailGroupingsForDisplay(detail).forEach { grouping ->
            when (grouping) {
                is ExerciseGrouping.Single -> HistoryExerciseBlockCard(grouping.block, accent)
                is ExerciseGrouping.Superset -> HistorySupersetGroupCard(grouping.group, accent)
            }
        }

        PillButton(
            text = stringResource(R.string.training_done),
            onClick = onClose,
            containerColor = accent.color,
            contentColor = accent.onColor,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Duration hero (10b): 54/800 minutes, a 100% wavy line, and quiet stat chips. */
@Composable
private fun WorkoutDurationHero(recap: WorkoutRecapSummary, accent: TabAccent) {
    val chipColor = MusFitTheme.colors.surface.copy(alpha = 0.75f)
    Surface(
        color = accent.container,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = workoutDurationMinutes(recap.durationSeconds).toString(),
                    style = MaterialTheme.typography.displayLarge,
                    color = accent.onContainer,
                )
                Text(
                    text = stringResource(R.string.training_minutes_suffix),
                    style = MaterialTheme.typography.titleLarge,
                    color = accent.onContainer.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            WavyProgressBar(
                progress = 1f,
                color = accent.color,
                trackColor = chipColor,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WorkoutStatChip(
                    figure = recap.completedSetCount.toString(),
                    caption = pluralStringResource(R.plurals.training_set_word, recap.completedSetCount),
                    chipColor = chipColor,
                    contentColor = accent.onContainer,
                )
                WorkoutStatChip(
                    figure = workoutVolumeChipFigure(recap.totalVolumeKg),
                    caption = stringResource(R.string.training_kg_volume),
                    chipColor = chipColor,
                    contentColor = accent.onContainer,
                )
                WorkoutStatChip(
                    figure = recap.personalRecordCount.toString(),
                    caption = stringResource(
                        if (recap.personalRecordCount == 1) R.string.training_pr else R.string.training_prs,
                    ),
                    chipColor = chipColor,
                    contentColor = accent.onContainer,
                )
            }
        }
    }
}

/** "13 sets" pill: emphasized figure + quiet caption on a white@75% fill. */
@Composable
private fun WorkoutStatChip(
    figure: String,
    caption: String,
    chipColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    Surface(color = chipColor, shape = RoundedCornerShape(99.dp)) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append(figure) }
                append(" $caption")
            },
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
            fontWeight = FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}

/** Coach note (10b): 7dp coral dot, one deterministic sentence, "Ask Coach". */
@Composable
private fun WorkoutCoachNoteCard(note: UiText, onOpenCoach: () -> Unit) {
    Surface(
        color = MusFitTheme.colors.surface,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(7.dp)
                    .background(MusFitTheme.colors.accent, CircleShape),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = note.asString(),
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = MusFitTheme.colors.onSurface,
                )
                TextButton(onClick = onOpenCoach) {
                    Text(
                        text = stringResource(R.string.training_ask_coach),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = MusFitTheme.colors.accent,
                    )
                }
            }
        }
    }
}

/**
 * History list (Turn 10 §10g): the month hero, an optional calendar (behind the
 * header's calendar toggle), and week sections of grouped session rows.
 */
@Composable
@Suppress("LongMethod", "LongParameterList")
fun TrainingHistoryContent(
    history: List<WorkoutHistorySummary>,
    overview: TrainingHistoryOverview,
    accent: TabAccent,
    calendarOpen: Boolean,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val sections = historyWeekSections(history, LocalDate.now(), locale)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp),
    ) {
        item(key = "history-month-hero", contentType = "history-hero") {
            Box(modifier = Modifier.padding(bottom = 18.dp)) {
                HistoryMonthHero(overview = overview, accent = accent)
            }
        }
        if (calendarOpen) {
            item(key = "history-calendar", contentType = "history-calendar") {
                Surface(
                    color = MusFitTheme.colors.surface,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
                ) {
                    Box(modifier = Modifier.padding(14.dp)) {
                        HistoryCalendarGrid(overview = overview, accent = accent)
                    }
                }
            }
        }
        if (history.isEmpty()) {
            item(key = "history-empty", contentType = "history-empty") {
                Text(
                    stringResource(R.string.training_finish_to_build_history),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
        }
        sections.forEach { section ->
            item(
                key = "history-section-${section.workouts.firstOrNull()?.sessionId ?: section.title}",
                contentType = "history-section",
            ) {
                Text(
                    text = section.title.asString(),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = MusFitTheme.colors.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
                )
            }
            itemsIndexed(
                items = section.workouts,
                key = { _, workout -> "history-session-${workout.sessionId}" },
                contentType = { _, _ -> "history-session" },
            ) { index, workout ->
                Box(
                    modifier = Modifier.padding(
                        bottom = if (index == section.workouts.lastIndex) 18.dp else 4.dp,
                    ),
                ) {
                    HistorySessionRow(
                        workout = workout,
                        accent = accent,
                        shape = groupedShape(index, section.workouts.size),
                        badgeShape = if (index % 2 == 0) ExpressiveBadgeShape.Sunny else ExpressiveBadgeShape.Circle,
                        onClick = { onOpenDetail(workout.sessionId) },
                    )
                }
            }
        }
    }
}

/** Month hero (10g): "JULY" overline, 44/800 session count, volume line. */
@Composable
private fun HistoryMonthHero(overview: TrainingHistoryOverview, accent: TabAccent) {
    val (sessions, volumeKg) = historyMonthStats(overview)
    val locale = LocalConfiguration.current.locales[0]
    val monthLabel = overview.month
        ?.atDay(1)
        ?.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
        ?: stringResource(R.string.training_this_month)
    val sessionWord = pluralStringResource(R.plurals.training_session_word, sessions)
    Surface(
        color = accent.container,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = monthLabel.uppercase(locale),
                style = MaterialTheme.typography.labelSmall,
                color = accent.onContainer,
            )
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 2.dp)) {
                Text(
                    text = sessions.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    color = accent.onContainer,
                )
                Text(
                    text = " $sessionWord · ${trainingWeekVolumeFigure(volumeKg, locale)} ${stringResource(R.string.training_volume)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = accent.onContainer.copy(alpha = 0.85f),
                    modifier = Modifier.padding(bottom = 7.dp),
                )
            }
        }
    }
}

/** One session row (10g): 46dp badge, title, one subline, trailing chevron. */
@Composable
private fun HistorySessionRow(
    workout: WorkoutHistorySummary,
    accent: TabAccent,
    shape: RoundedCornerShape,
    badgeShape: ExpressiveBadgeShape,
    onClick: () -> Unit,
) {
    val meta = historyRowMeta(workout, LocalConfiguration.current.locales[0])
    Surface(
        onClick = onClick,
        color = MusFitTheme.colors.surface,
        shape = shape,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ExpressiveBadge(
                icon = Icons.Filled.FitnessCenter,
                shape = badgeShape,
                containerColor = accent.container,
                contentColor = accent.onContainer,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workout.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MusFitTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildAnnotatedString {
                        append(meta.dateLabel)
                        append(" · ")
                        withStyle(
                            SpanStyle(fontWeight = FontWeight.ExtraBold, color = MusFitTheme.colors.onSurface),
                        ) {
                            append(meta.volumeLabel)
                        }
                        meta.durationLabel?.let { append(" · $it") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MusFitTheme.colors.onSurfaceFaint,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun HistoryCalendarGrid(
    overview: TrainingHistoryOverview,
    accent: TabAccent,
) {
    val locale = LocalConfiguration.current.locales[0]
    val weekdayFormatter = DateTimeFormatter.ofPattern("EEEEE", locale)
    val weekdayLabels = (0L..6L).map { offset ->
        LocalDate.of(2024, 1, 1).plusDays(offset).format(weekdayFormatter)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        overview.calendarWeeks.forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    HistoryCalendarDayCell(day = day, accent = accent, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HistoryCalendarDayCell(
    day: TrainingHistoryCalendarDay?,
    accent: TabAccent,
    modifier: Modifier = Modifier,
) {
    if (day == null) {
        Box(modifier = modifier.height(34.dp))
        return
    }
    val hasWorkout = day.workoutCount > 0
    Surface(
        color = if (hasWorkout) accent.container else MusFitTheme.colors.surfaceVariant,
        shape = MusFitTheme.shapes.extraSmall,
        modifier = modifier.height(34.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (hasWorkout) FontWeight.Bold else FontWeight.Normal,
                color = if (hasWorkout) accent.onContainer else MusFitTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HistorySupersetGroupCard(
    group: SupersetGroup,
    accent: TabAccent,
) {
    Surface(
        color = MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(color = accent.container, shape = RoundedCornerShape(99.dp)) {
                    Text(
                        text = stringResource(R.string.training_superset),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = accent.onContainer,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                    )
                }
                Text(
                    text = pluralStringResource(
                        R.plurals.training_exercise_count,
                        group.exerciseBlocks.size,
                        group.exerciseBlocks.size,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
            group.exerciseBlocks.forEachIndexed { index, block ->
                HistoryExerciseBlockSection(block, accent)
                if (index < group.exerciseBlocks.lastIndex) {
                    HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
                }
            }
        }
    }
}

@Composable
private fun HistoryExerciseBlockCard(block: WorkoutExerciseBlock, accent: TabAccent) {
    Surface(
        color = MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        HistoryExerciseBlockSection(
            block = block,
            accent = accent,
            modifier = Modifier.padding(14.dp),
        )
    }
}

@Composable
private fun HistoryExerciseBlockSection(
    block: WorkoutExerciseBlock,
    accent: TabAccent,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            block.supersetLabel?.let { label ->
                Surface(color = accent.container, shape = RoundedCornerShape(99.dp)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = accent.onContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            Text(block.exercise.name, style = MaterialTheme.typography.titleMedium)
        }
        block.sets.forEachIndexed { index, set ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = historySetLabel(index, set.setType),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(0.35f),
                )
                Text(
                    text = set.setType.replaceFirstChar { it.uppercase(locale) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    modifier = Modifier.weight(0.9f),
                )
                Text(
                    text = stringResource(
                        R.string.training_kilograms,
                        set.weightKg?.formatKg(locale) ?: "-",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(0.9f),
                )
                Text(
                    text = stringResource(R.string.training_reps_value, set.reps?.toString() ?: "-"),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(0.9f),
                )
                Text(
                    text = set.rpe?.let {
                        stringResource(R.string.training_rpe_value, it.formatKg(locale))
                    }.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    modifier = Modifier.weight(0.9f),
                )
            }
            if (index < block.sets.lastIndex) {
                HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
            }
        }
    }
}

// --- Display helpers (pure, unit-tested) ---

internal fun historyDetailGroupingsForDisplay(detail: WorkoutHistoryDetail): List<ExerciseGrouping> = detail.exerciseGroupings.ifEmpty {
    detail.exerciseBlocks.map { ExerciseGrouping.Single(it) }
}

/** "Thu 2 Jul · Full Body A" under the Workout complete title. */
internal fun workoutCompleteSubtitle(
    summary: WorkoutHistorySummary,
    locale: Locale = Locale.getDefault(),
): String {
    val date = Instant.ofEpochMilli(summary.startedAtEpochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("EEE d MMM", locale))
    return "$date · ${summary.title}"
}

/** Whole minutes for the duration hero; a sub-minute session still reads "1". */
internal fun workoutDurationMinutes(durationSeconds: Int): Int = if (durationSeconds <= 0) 0 else (durationSeconds / 60).coerceAtLeast(1)

/** "4,120" — grouped kilogram figure for the volume stat chip. */
internal fun workoutVolumeChipFigure(
    volumeKg: Double,
    locale: Locale = Locale.getDefault(),
): String = LocalizedFormatter.integer(Math.round(volumeKg), locale = locale)

internal data class WorkoutPrDisplay(
    val exerciseName: String,
    /** "107.5 kg × 5 · e1RM 128 kg" */
    val meta: UiText,
    /** "+9 kg" e1RM improvement over the prior best. */
    val deltaLabel: UiText,
)

/**
 * The session's PR rows: for each exercise whose best completed working set
 * beats the pre-session best e1RM, the winning set and the improvement.
 * Mirrors the recap's [personalRecordCount] rule (warm-ups and drops excluded).
 */
internal fun workoutPrDisplays(
    detail: WorkoutHistoryDetail,
    locale: Locale = Locale.getDefault(),
): List<WorkoutPrDisplay> = detail.exerciseBlocks.mapNotNull { block ->
    val best = block.sets
        .filter { set ->
            val type = set.setType.lowercase(Locale.ROOT)
            set.completed &&
                type != "warmup" && type != "warm-up" && type != "drop" &&
                set.reps != null && set.weightKg != null
        }
        .maxByOrNull { set -> WorkoutCalculator.estimatedOneRepMax(set.weightKg!!, set.reps!!) }
        ?: return@mapNotNull null
    val bestE1rm = WorkoutCalculator.estimatedOneRepMax(best.weightKg!!, best.reps!!)
    val bestWeightKg = best.weightKg ?: return@mapNotNull null
    val prior = block.priorBestEstimatedOneRepMaxKg
    if (bestE1rm <= prior + 1e-6) return@mapNotNull null
    WorkoutPrDisplay(
        exerciseName = block.exercise.name,
        meta = uiText(
            R.string.training_pr_meta,
            UiText.Argument.Text(bestWeightKg.formatKg(locale)),
            UiText.Argument.Integer(requireNotNull(best.reps)),
            UiText.Argument.Text(bestE1rm.formatKg(locale)),
        ),
        // A first-ever lift has no prior best — "+128 kg" would be noise.
        deltaLabel = if (prior > 0.0) {
            uiText(
                R.string.training_pr_delta,
                UiText.Argument.Text((bestE1rm - prior).formatKg(locale)),
            )
        } else {
            uiText(R.string.training_new_pr)
        },
    )
}

/** One deterministic coach sentence for the finished session — no prose engine. */
internal fun workoutCompleteCoachNote(recap: WorkoutRecapSummary): UiText {
    val minutes = workoutDurationMinutes(recap.durationSeconds)
    return when {
        recap.personalRecordCount > 0 -> uiText(
            if (recap.personalRecordCount == 1) {
                R.string.training_coach_strong_session
            } else {
                R.string.training_coach_strong_session_plural
            },
            UiText.Argument.Integer(recap.personalRecordCount),
        )

        recap.completedSetCount > 0 -> uiText(
            R.string.training_coach_solid_work,
            UiText.Argument.Integer(recap.completedSetCount),
            UiText.Argument.Integer(minutes),
        )

        else -> uiText(R.string.training_coach_session_logged)
    }
}

/** Month hero stats: sessions and volume summed over the overview's calendar month. */
internal fun historyMonthStats(overview: TrainingHistoryOverview): Pair<Int, Double> {
    val days = overview.calendarWeeks.flatten().filterNotNull()
    return days.sumOf { it.workoutCount } to days.sumOf { it.totalVolumeKg }
}

internal data class HistoryWeekSection(
    val title: UiText,
    val workouts: List<WorkoutHistorySummary>,
)

/** Sessions grouped into "This week" / "Last week" / "Week of d MMM" sections. */
internal fun historyWeekSections(
    history: List<WorkoutHistorySummary>,
    today: LocalDate,
    locale: Locale = Locale.getDefault(),
): List<HistoryWeekSection> {
    val thisWeekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
    return history
        .sortedByDescending { it.startedAtEpochMillis }
        .groupBy { workout ->
            val date = Instant.ofEpochMilli(workout.startedAtEpochMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            date.minusDays((date.dayOfWeek.value - 1).toLong())
        }
        .toList()
        .sortedByDescending { (weekStart, _) -> weekStart }
        .map { (weekStart, workouts) ->
            val title = when (weekStart) {
                thisWeekStart -> uiText(R.string.training_this_week)

                thisWeekStart.minusWeeks(1) -> uiText(R.string.training_last_week)

                else -> uiText(
                    R.string.training_week_of,
                    UiText.Argument.Text(weekStart.format(DateTimeFormatter.ofPattern("d MMM", locale))),
                )
            }
            HistoryWeekSection(title = title, workouts = workouts)
        }
}

internal data class HistoryRowMeta(
    val dateLabel: String,
    val volumeLabel: String,
    val durationLabel: String?,
)

/** Row subline parts (10g): "Thu 2 Jul" · "1.9 t" (emphasized) · "41 min". */
internal fun historyRowMeta(
    summary: WorkoutHistorySummary,
    locale: Locale = Locale.getDefault(),
): HistoryRowMeta {
    val dateLabel = Instant.ofEpochMilli(summary.startedAtEpochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("EEE d MMM", locale))
    val durationSeconds = summary.endedAtEpochMillis
        ?.let { ((it - summary.startedAtEpochMillis) / 1000L).toInt() }
        ?.coerceAtLeast(0)
    val durationLabel = durationSeconds
        ?.takeIf { it > 0 }
        ?.let { LocalizedFormatter.integer(workoutDurationMinutes(it).toLong(), locale = locale) + " min" }
    return HistoryRowMeta(
        dateLabel = dateLabel,
        volumeLabel = trainingWeekVolumeFigure(summary.totalVolumeKg, locale),
        durationLabel = durationLabel,
    )
}

internal fun WorkoutHistoryDetail.effectiveRecap(): WorkoutRecapSummary = if (recap.hasVisibleData()) {
    recap
} else {
    WorkoutRecapSummary(
        durationSeconds = summary.durationSeconds(),
        exerciseCount = exerciseBlocks.size,
        completedSetCount = summary.completedSetCount,
        totalVolumeKg = summary.totalVolumeKg,
        personalRecordCount = 0,
        notes = null,
    )
}

private fun WorkoutRecapSummary.hasVisibleData(): Boolean = durationSeconds > 0 ||
    exerciseCount > 0 ||
    completedSetCount > 0 ||
    totalVolumeKg > 0.0 ||
    personalRecordCount > 0 ||
    !notes.isNullOrBlank()

private fun WorkoutHistorySummary.durationSeconds(): Int {
    val endedAt = endedAtEpochMillis ?: startedAtEpochMillis
    return ((endedAt - startedAtEpochMillis).coerceAtLeast(0L) / 1000L).toInt()
}

private fun historySetLabel(index: Int, setType: String): String = when (setType.lowercase(Locale.ROOT)) {
    "warmup" -> "W"
    "drop" -> "D"
    "failure" -> "F"
    else -> "${index + 1}"
}

private fun Double.formatKg(locale: Locale = Locale.getDefault()): String = LocalizedFormatter.number(this, maximumFractionDigits = 1, locale = locale)
