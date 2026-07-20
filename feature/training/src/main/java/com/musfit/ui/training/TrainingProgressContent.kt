package com.musfit.ui.training

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musfit.data.repository.TrainingPrRecord
import com.musfit.data.repository.WeeklyTrainingVolume
import com.musfit.domain.model.ExerciseProgress
import com.musfit.domain.model.TrainingTrendPoint
import com.musfit.feature.training.R
import com.musfit.ui.components.ExpressiveBadge
import com.musfit.ui.components.ExpressiveBadgeShape
import com.musfit.ui.components.groupedShape
import com.musfit.ui.text.LocalizedFormatter
import com.musfit.ui.text.UiText
import com.musfit.ui.text.asString
import com.musfit.ui.text.pluralUiText
import com.musfit.ui.text.uiText
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.roundToInt

data class TrainingProgressContentActions(
    val onOpenAllExercises: () -> Unit,
)

data class TrainingProgressContentData(
    val progress: ExerciseProgress?,
    val period: TrainingProgressPeriod,
    val weeklyVolume: List<WeeklyTrainingVolume>,
    val recentPrs: List<TrainingPrRecord>,
    val today: LocalDate = LocalDate.now(),
)

private data class VisibleProgressContent(
    val trend: List<TrainingTrendPoint>,
    val weeks: List<WeeklyTrainingVolume>,
)

private fun TrainingProgressContentData.visibleContent(): VisibleProgressContent = VisibleProgressContent(
    trend = progress?.let { filterTrendByPeriod(it.trend, period, today) }.orEmpty(),
    weeks = weeklyVolume.sortedBy { it.weekStartEpochDay }.takeLast(VOLUME_WEEK_COUNT),
)

/**
 * Progress page body (Turn 10 §10f): the anchored exercise's e1RM trend as the
 * tonal hero, the weekly volume card, and Recent PR grouped rows with the
 * "All exercises" re-anchor link.
 */
@Composable
fun TrainingProgressContent(
    data: TrainingProgressContentData,
    accent: TabAccent,
    actions: TrainingProgressContentActions,
) {
    val (visibleTrend, visibleWeeks) = data.visibleContent()
    var selectedTrendDateEpochDay by rememberSaveable(data.progress?.exerciseId) { mutableStateOf<Long?>(null) }
    var selectedWeekStartEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    val effectiveTrendDate = visibleTrend
        .firstOrNull { it.dateEpochDay == selectedTrendDateEpochDay }
        ?.dateEpochDay
        ?: visibleTrend.lastOrNull()?.dateEpochDay
    val effectiveWeekStart = visibleWeeks
        .firstOrNull { it.weekStartEpochDay == selectedWeekStartEpochDay }
        ?.weekStartEpochDay
        ?: visibleWeeks.lastOrNull()?.weekStartEpochDay
    SideEffect {
        if (selectedTrendDateEpochDay != effectiveTrendDate) selectedTrendDateEpochDay = effectiveTrendDate
        if (selectedWeekStartEpochDay != effectiveWeekStart) selectedWeekStartEpochDay = effectiveWeekStart
    }
    var openDataTab by rememberSaveable { mutableStateOf<ProgressDataTab?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        EstimatedOneRepMaxHero(
            progress = data.progress,
            trend = visibleTrend,
            accent = accent,
            selectedDateEpochDay = effectiveTrendDate,
            onSelectDate = { selectedTrendDateEpochDay = it },
            onOpenDataTable = { openDataTab = ProgressDataTab.EstimatedOneRepMax },
        )
        WeeklyVolumeCard(
            weeks = visibleWeeks,
            accent = accent,
            selectedWeekStartEpochDay = effectiveWeekStart,
            onSelectWeek = { selectedWeekStartEpochDay = it },
            onOpenProgressData = {
                openDataTab = if (visibleTrend.isNotEmpty() || visibleWeeks.isEmpty()) {
                    ProgressDataTab.EstimatedOneRepMax
                } else {
                    ProgressDataTab.WeeklyVolume
                }
            },
            onOpenDataTable = { openDataTab = ProgressDataTab.WeeklyVolume },
        )
        RecentPrsSection(
            recentPrs = data.recentPrs,
            accent = accent,
            onOpenAllExercises = actions.onOpenAllExercises,
        )
    }

    openDataTab?.let { initialTab ->
        ProgressDataSheet(
            trend = visibleTrend,
            weeks = visibleWeeks,
            initialTab = initialTab,
            selectedTrendDateEpochDay = effectiveTrendDate,
            selectedWeekStartEpochDay = effectiveWeekStart,
            onSelectTrendDate = { selectedTrendDateEpochDay = it },
            onSelectWeek = { selectedWeekStartEpochDay = it },
            onDismiss = { openDataTab = null },
        )
    }
}

/** e1RM hero (10f): overline, 44/800 figure, trend delta, and the line chart. */
@Composable
@Suppress("LongMethod", "LongParameterList")
private fun EstimatedOneRepMaxHero(
    progress: ExerciseProgress?,
    trend: List<TrainingTrendPoint>,
    accent: TabAccent,
    selectedDateEpochDay: Long?,
    onSelectDate: (Long) -> Unit,
    onOpenDataTable: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val estimatedOneRepMaxLabel = stringResource(R.string.training_estimated_one_rep_max)
    Surface(
        color = accent.container,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = progressHeroOverline(progress?.exerciseName, estimatedOneRepMaxLabel, locale).asString(),
                style = MaterialTheme.typography.labelSmall,
                color = accent.onContainer,
            )
            if (progress == null || trend.isEmpty()) {
                Text(
                    text = stringResource(R.string.training_complete_for_e1rm),
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent.onContainer.copy(alpha = 0.8f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .chartInteraction(
                            dates = emptyList(),
                            selectedIndex = -1,
                            summary = e1rmChartSummary(progress?.exerciseName, emptyList()).asString(),
                            selectedState = stringResource(R.string.training_no_data),
                            onSelectIndex = {},
                            onOpenDataTable = onOpenDataTable,
                        )
                        .padding(vertical = 12.dp),
                )
            } else {
                val selectedIndex = trend.indexOfFirst { it.dateEpochDay == selectedDateEpochDay }
                    .takeIf { it >= 0 }
                    ?: trend.lastIndex
                val selectedPoint = trend[selectedIndex]
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clearAndSetSemantics {
                            testTag = e1rmVisualSelectionTag(selectedPoint)
                        },
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = selectedPoint.bestEstimatedOneRepMaxKg.formatE1rm(locale),
                                style = MaterialTheme.typography.displayMedium,
                                color = accent.onContainer,
                                maxLines = 1,
                                softWrap = false,
                            )
                            Text(
                                text = stringResource(R.string.training_kilograms, "").trimEnd(),
                                style = MaterialTheme.typography.titleLarge,
                                color = accent.onContainer.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 6.dp),
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    val delta = e1rmDeltaKg(trend.take(selectedIndex + 1))
                    if (delta != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(bottom = 8.dp),
                        ) {
                            Icon(
                                imageVector = if (delta >= 0) {
                                    Icons.AutoMirrored.Outlined.TrendingUp
                                } else {
                                    Icons.AutoMirrored.Outlined.TrendingDown
                                },
                                contentDescription = null,
                                tint = accent.onContainer,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = deltaLabel(delta, locale).asString(),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 13.sp,
                                    textDirection = TextDirection.Ltr,
                                ),
                                fontWeight = FontWeight.ExtraBold,
                                color = accent.onContainer,
                            )
                        }
                    }
                }
                Text(
                    text = accessibleDate(selectedPoint.dateEpochDay),
                    style = MaterialTheme.typography.labelMedium,
                    color = accent.onContainer.copy(alpha = 0.8f),
                    modifier = Modifier.clearAndSetSemantics { },
                )
                ProgressLineChart(
                    data = ProgressLineChartData(
                        exerciseName = progress.exerciseName,
                        trend = trend,
                    ),
                    accent = accent,
                    selectedDateEpochDay = selectedDateEpochDay,
                    actions = ProgressLineChartActions(
                        onSelectDate = onSelectDate,
                        onOpenDataTable = onOpenDataTable,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val labels = monthLabelsFor(trend, LocalConfiguration.current.locales[0])
                    labels.forEachIndexed { index, label ->
                        val isCurrent = index == labels.lastIndex
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp, letterSpacing = 0.sp),
                            fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isCurrent) accent.onContainer else accent.onContainer.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
    }
}

/** 2.5dp line + endpoint dot over an on-container hairline baseline (10f hero). */
private data class ProgressLineChartActions(
    val onSelectDate: (Long) -> Unit,
    val onOpenDataTable: () -> Unit,
)

private data class ProgressLineChartData(
    val exerciseName: String,
    val trend: List<TrainingTrendPoint>,
)

@Composable
private fun ProgressLineChart(
    data: ProgressLineChartData,
    accent: TabAccent,
    selectedDateEpochDay: Long?,
    actions: ProgressLineChartActions,
    modifier: Modifier = Modifier,
) {
    val trend = data.trend
    val dates = trend.map { it.dateEpochDay }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val selectedIndex = dates.indexOf(selectedDateEpochDay).takeIf { it >= 0 } ?: trend.lastIndex
    val selectIndex: (Int) -> Unit = { index ->
        dates.getOrNull(index)?.let(actions.onSelectDate)
    }
    val lineColor = accent.color
    val baselineColor = accent.onContainer.copy(alpha = 0.18f)
    val chartModifier = modifier.chartInteraction(
        dates = dates,
        selectedIndex = selectedIndex,
        summary = e1rmChartSummary(data.exerciseName, trend).asString(),
        selectedState = e1rmChartSelectionDescription(trend, selectedIndex).asString(),
        onSelectIndex = selectIndex,
        onOpenDataTable = actions.onOpenDataTable,
    )
    Canvas(modifier = chartModifier) {
        drawProgressLineChart(
            values = trend.map { it.bestEstimatedOneRepMaxKg },
            selectedIndex = selectedIndex,
            lineColor = lineColor,
            baselineColor = baselineColor,
            isRtl = isRtl,
        )
    }
}

private fun DrawScope.drawProgressLineChart(
    values: List<Double>,
    selectedIndex: Int,
    lineColor: Color,
    baselineColor: Color,
    isRtl: Boolean,
) {
    if (values.isEmpty()) return
    val minValue = values.min()
    val maxValue = values.max()
    val range = (maxValue - minValue).takeIf { it > 0 } ?: 1.0
    val padTop = 8.dp.toPx()
    val padBottom = 10.dp.toPx()
    val usableHeight = size.height - padTop - padBottom
    val points = values.mapIndexed { index, value ->
        val fraction = ((value - minValue) / range).toFloat()
        Offset(
            x = chartPointX(
                index = index,
                itemCount = values.size,
                width = size.width,
                isRtl = isRtl,
            ),
            y = padTop + usableHeight * (1f - fraction),
        )
    }
    drawLine(
        color = baselineColor,
        start = Offset(0f, size.height - 1.dp.toPx()),
        end = Offset(size.width, size.height - 1.dp.toPx()),
        strokeWidth = 1.dp.toPx(),
    )
    if (points.size > 1) {
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
    drawCircle(
        color = lineColor,
        radius = 4.5.dp.toPx(),
        center = points[selectedIndex.coerceIn(0, points.lastIndex)],
    )
}

/** Weekly volume (10f): white card, 6 rounded bars — tonal past, filled current. */
@Composable
@Suppress("LongMethod", "LongParameterList")
private fun WeeklyVolumeCard(
    weeks: List<WeeklyTrainingVolume>,
    accent: TabAccent,
    selectedWeekStartEpochDay: Long?,
    onSelectWeek: (Long) -> Unit,
    onOpenProgressData: () -> Unit,
    onOpenDataTable: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val selectedIndex = weeks.indexOfFirst { it.weekStartEpochDay == selectedWeekStartEpochDay }
        .takeIf { it >= 0 }
        ?: weeks.lastIndex
    val selectedWeek = weeks.getOrNull(selectedIndex)
    Surface(
        color = MusFitTheme.colors.surface,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.training_weekly_volume),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = MusFitTheme.colors.onSurface,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    selectedWeek?.let { current ->
                        val weekOfLabel = stringResource(
                            R.string.training_week_of_lower,
                            accessibleDate(current.weekStartEpochDay, locale),
                        )
                        Text(
                            text = buildAnnotatedString {
                                withStyle(
                                    SpanStyle(fontWeight = FontWeight.ExtraBold, color = MusFitTheme.colors.onSurface),
                                ) {
                                    append(volumeTonsLabel(current.totalVolumeKg).asString())
                                }
                                append(" · $weekOfLabel")
                            },
                            style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr),
                            color = MusFitTheme.colors.onSurfaceVariant,
                            modifier = Modifier.clearAndSetSemantics {
                                testTag = weeklyVolumeVisualSelectionTag(current)
                            },
                        )
                    }
                    IconButton(
                        onClick = onOpenProgressData,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.TableChart,
                            contentDescription = stringResource(R.string.training_view_progress_data),
                            tint = accent.color,
                        )
                    }
                }
            }
            if (weeks.isEmpty()) {
                Text(
                    text = stringResource(R.string.training_complete_for_weekly_volume),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .chartInteraction(
                            dates = emptyList(),
                            selectedIndex = -1,
                            summary = weeklyVolumeChartSummary(emptyList()).asString(),
                            selectedState = stringResource(R.string.training_no_data),
                            onSelectIndex = {},
                            onOpenDataTable = onOpenDataTable,
                        )
                        .padding(vertical = 12.dp),
                )
            } else {
                val maxVolume = weeks.maxOf { it.totalVolumeKg }.takeIf { it > 0 } ?: 1.0
                val dates = weeks.map { it.weekStartEpochDay }
                val selectIndex: (Int) -> Unit = { index ->
                    dates.getOrNull(index)?.let(onSelectWeek)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .chartInteraction(
                            dates = dates,
                            selectedIndex = selectedIndex,
                            summary = weeklyVolumeChartSummary(weeks).asString(),
                            selectedState = weeklyVolumeChartSelectionDescription(weeks, selectedIndex).asString(),
                            onSelectIndex = selectIndex,
                            onOpenDataTable = onOpenDataTable,
                            tapSelectionMode = ChartTapSelectionMode.Slots,
                        ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        weeks.forEachIndexed { index, week ->
                            val fraction = (week.totalVolumeKg / maxVolume).toFloat().coerceIn(0.04f, 1f)
                            val isSelected = index == selectedIndex
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Bottom,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height((78 * fraction).dp)
                                        .background(
                                            color = if (isSelected) accent.color else accent.container,
                                            shape = RoundedCornerShape(6.dp),
                                        ),
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        weeks.forEachIndexed { index, week ->
                            val isSelected = index == selectedIndex
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Text(
                                    text = weekLabel(week.weekStartEpochDay).asString(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp, letterSpacing = 0.sp),
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                    color = if (isSelected) {
                                        MusFitTheme.colors.onSurface
                                    } else {
                                        MusFitTheme.colors.onSurfaceFaint
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Suppress("LongMethod", "LongParameterList")
@Composable
private fun Modifier.chartInteraction(
    dates: List<Long>,
    selectedIndex: Int,
    summary: String,
    selectedState: String,
    onSelectIndex: (Int) -> Unit,
    onOpenDataTable: () -> Unit,
    tapSelectionMode: ChartTapSelectionMode = ChartTapSelectionMode.Points,
): Modifier {
    var isFocused by remember { mutableStateOf(false) }
    val focusColor = MaterialTheme.colorScheme.primary
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val previousDataPoint = stringResource(R.string.training_previous_data_point)
    val nextDataPoint = stringResource(R.string.training_next_data_point)
    val showDataTable = stringResource(R.string.training_show_data_table)
    return this
        .drawBehind {
            if (isFocused) {
                val strokeWidth = 2.dp.toPx()
                drawRoundRect(
                    color = focusColor,
                    cornerRadius = CornerRadius(8.dp.toPx()),
                    style = Stroke(width = strokeWidth),
                )
            }
        }
        .pointerInput(dates, onSelectIndex, tapSelectionMode, isRtl) {
            detectTapGestures { offset ->
                if (dates.isNotEmpty() && size.width > 0) {
                    onSelectIndex(
                        chartIndexForTap(
                            x = offset.x,
                            width = size.width.toFloat(),
                            itemCount = dates.size,
                            mode = tapSelectionMode,
                            isRtl = isRtl,
                        ),
                    )
                }
            }
        }
        .onKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) {
                false
            } else {
                chartIndexForKey(
                    key = event.key,
                    selectedIndex = selectedIndex,
                    lastIndex = dates.lastIndex,
                    isRtl = isRtl,
                )?.let { nextIndex ->
                    onSelectIndex(nextIndex)
                    true
                } ?: false
            }
        }
        .onFocusChanged { isFocused = it.isFocused }
        .focusable()
        .clearAndSetSemantics {
            role = Role.Image
            contentDescription = summary
            stateDescription = selectedState
            if (isFocused) testTag = FOCUSED_PROGRESS_CHART_TEST_TAG
            customActions = buildList {
                if (selectedIndex > 0) {
                    add(
                        CustomAccessibilityAction(previousDataPoint) {
                            onSelectIndex(selectedIndex - 1)
                            true
                        },
                    )
                }
                if (selectedIndex in 0 until dates.lastIndex) {
                    add(
                        CustomAccessibilityAction(nextDataPoint) {
                            onSelectIndex(selectedIndex + 1)
                            true
                        },
                    )
                }
                add(
                    CustomAccessibilityAction(showDataTable) {
                        onOpenDataTable()
                        true
                    },
                )
            }
        }
}

internal const val FOCUSED_PROGRESS_CHART_TEST_TAG = "training-progress-chart-focused"

internal enum class ChartTapSelectionMode { Points, Slots }

internal fun chartIndexForTap(
    x: Float,
    width: Float,
    itemCount: Int,
    mode: ChartTapSelectionMode,
    isRtl: Boolean = false,
): Int {
    if (itemCount <= 1 || width <= 0f) return 0
    val visualFraction = (x / width).coerceIn(0f, 1f)
    val fraction = if (isRtl) 1f - visualFraction else visualFraction
    return when (mode) {
        ChartTapSelectionMode.Points -> (fraction * (itemCount - 1)).roundToInt()
        ChartTapSelectionMode.Slots -> (fraction * itemCount).toInt().coerceAtMost(itemCount - 1)
    }
}

/** Maps chronological point order onto the same mirrored horizontal axis used by label rows. */
internal fun chartPointX(
    index: Int,
    itemCount: Int,
    width: Float,
    isRtl: Boolean,
): Float {
    if (width <= 0f) return 0f
    val logicalFraction = if (itemCount <= 1) {
        1f
    } else {
        index.coerceIn(0, itemCount - 1).toFloat() / (itemCount - 1)
    }
    val visualFraction = if (isRtl) 1f - logicalFraction else logicalFraction
    return width * visualFraction
}

internal fun chartIndexForKey(
    key: Key,
    selectedIndex: Int,
    lastIndex: Int,
    isRtl: Boolean = false,
): Int? = when (key) {
    Key.DirectionLeft -> if (isRtl) {
        (selectedIndex + 1).takeIf { it <= lastIndex }
    } else {
        (selectedIndex - 1).takeIf { it >= 0 }
    }

    Key.DirectionRight -> if (isRtl) {
        (selectedIndex - 1).takeIf { it >= 0 }
    } else {
        (selectedIndex + 1).takeIf { it <= lastIndex }
    }

    Key.DirectionUp -> (selectedIndex - 1).takeIf { it >= 0 }

    Key.DirectionDown -> (selectedIndex + 1).takeIf { it <= lastIndex }

    Key.MoveHome -> 0.takeIf { selectedIndex > 0 && lastIndex >= 0 }

    Key.MoveEnd -> lastIndex.takeIf { selectedIndex in 0 until lastIndex }

    else -> null
}

private enum class ProgressDataTab(val labelResource: Int) {
    EstimatedOneRepMax(R.string.training_e1rm),
    WeeklyVolume(R.string.training_weekly_volume),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongMethod", "LongParameterList")
private fun ProgressDataSheet(
    trend: List<TrainingTrendPoint>,
    weeks: List<WeeklyTrainingVolume>,
    initialTab: ProgressDataTab,
    selectedTrendDateEpochDay: Long?,
    selectedWeekStartEpochDay: Long?,
    onSelectTrendDate: (Long) -> Unit,
    onSelectWeek: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedTab by rememberSaveable(initialTab) { mutableStateOf(initialTab) }
    val tabLabels = mapOf(
        ProgressDataTab.EstimatedOneRepMax to stringResource(ProgressDataTab.EstimatedOneRepMax.labelResource),
        ProgressDataTab.WeeklyVolume to stringResource(ProgressDataTab.WeeklyVolume.labelResource),
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MusFitTheme.colors.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.training_progress_data),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Normal,
                    color = MusFitTheme.colors.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.training_close_progress_data),
                    )
                }
            }
            PrimaryTabRow(selectedTabIndex = selectedTab.ordinal) {
                ProgressDataTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tabLabels.getValue(tab)) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
            when (selectedTab) {
                ProgressDataTab.EstimatedOneRepMax -> EstimatedOneRepMaxDataRows(
                    trend = trend,
                    selectedDateEpochDay = selectedTrendDateEpochDay,
                    onSelectDate = onSelectTrendDate,
                )

                ProgressDataTab.WeeklyVolume -> WeeklyVolumeDataRows(
                    weeks = weeks,
                    selectedWeekStartEpochDay = selectedWeekStartEpochDay,
                    onSelectWeek = onSelectWeek,
                )
            }
        }
    }
}

@Composable
private fun EstimatedOneRepMaxDataRows(
    trend: List<TrainingTrendPoint>,
    selectedDateEpochDay: Long?,
    onSelectDate: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp, max = 520.dp),
    ) {
        if (trend.isEmpty()) {
            item(key = "empty-e1rm") {
                EmptyProgressDataMessage(stringResource(R.string.training_no_e1rm_data))
            }
        } else {
            items(
                // The chart defaults to the latest point; keep that selected row immediately
                // discoverable instead of placing it hundreds of off-screen rows away.
                items = trend.asReversed(),
                key = { point -> point.dateEpochDay },
            ) { point ->
                ProgressDataRow(
                    primary = accessibleDate(point.dateEpochDay, LocalConfiguration.current.locales[0]),
                    value = stringResource(
                        R.string.training_kg_e1rm,
                        point.bestEstimatedOneRepMaxKg.formatE1rm(LocalConfiguration.current.locales[0]),
                    ),
                    supporting = stringResource(
                        R.string.training_workout_volume,
                        volumeTonsLabel(point.volumeKg, LocalConfiguration.current.locales[0]).asString(),
                    ),
                    association = e1rmDataRowDescription(point).asString(),
                    isSelected = point.dateEpochDay == selectedDateEpochDay,
                    onSelect = { onSelectDate(point.dateEpochDay) },
                )
            }
        }
    }
}

@Composable
private fun WeeklyVolumeDataRows(
    weeks: List<WeeklyTrainingVolume>,
    selectedWeekStartEpochDay: Long?,
    onSelectWeek: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp, max = 520.dp),
    ) {
        if (weeks.isEmpty()) {
            item(key = "empty-weekly-volume") {
                EmptyProgressDataMessage(stringResource(R.string.training_no_weekly_volume_data))
            }
        } else {
            items(
                items = weeks.asReversed(),
                key = { week -> week.weekStartEpochDay },
            ) { week ->
                ProgressDataRow(
                    primary = stringResource(
                        R.string.training_week_of,
                        accessibleDate(week.weekStartEpochDay, LocalConfiguration.current.locales[0]),
                    ),
                    value = volumeTonsLabel(week.totalVolumeKg, LocalConfiguration.current.locales[0]).asString(),
                    supporting = stringResource(
                        R.string.training_workout_set_summary,
                        pluralStringResource(R.plurals.training_workout_count, week.workoutCount, week.workoutCount),
                        pluralStringResource(
                            R.plurals.training_completed_set_count,
                            week.completedSetCount,
                            week.completedSetCount,
                        ),
                    ),
                    association = weeklyVolumeDataRowDescription(week).asString(),
                    isSelected = week.weekStartEpochDay == selectedWeekStartEpochDay,
                    onSelect = { onSelectWeek(week.weekStartEpochDay) },
                )
            }
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun ProgressDataRow(
    primary: String,
    value: String,
    supporting: String,
    association: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    val selectedState = stringResource(if (isSelected) R.string.training_selected else R.string.training_not_selected)
    val selectDataPoint = stringResource(R.string.training_select_data_point)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(if (isSelected) MusFitTheme.colors.surfaceVariant else MusFitTheme.colors.surface)
            .clickable(role = Role.RadioButton, onClick = onSelect)
            .clearAndSetSemantics {
                contentDescription = association
                role = Role.RadioButton
                selected = isSelected
                stateDescription = selectedState
                onClick(label = selectDataPoint) {
                    onSelect()
                    true
                }
            }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = primary,
                style = MaterialTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurface,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MusFitTheme.colors.onSurface,
            )
        }
        Text(
            text = supporting,
            style = MaterialTheme.typography.bodySmall,
            color = MusFitTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyProgressDataMessage(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MusFitTheme.colors.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
    )
}

/** Recent PRs (10f): grouped rows with amber trophy badges + teal e1RM figures. */
@Composable
private fun RecentPrsSection(
    recentPrs: List<TrainingPrRecord>,
    accent: TabAccent,
    onOpenAllExercises: () -> Unit,
) {
    val prs = recentPrs.take(RECENT_PR_COUNT)
    val locale = LocalConfiguration.current.locales[0]
    val todayLabel = stringResource(R.string.training_today_lower)
    val yesterdayLabel = stringResource(R.string.training_yesterday)
    // PR rows plus the trailing "All exercises" link share one grouped list.
    val groupCount = prs.size + 1
    Column {
        Text(
            text = stringResource(R.string.training_recent_prs),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
            fontWeight = FontWeight.ExtraBold,
            color = MusFitTheme.colors.onSurface,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
        )
        if (prs.isEmpty()) {
            Text(
                text = stringResource(R.string.training_beat_best_for_pr),
                style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr),
                color = MusFitTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp, start = 4.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            prs.forEachIndexed { index, pr ->
                Surface(
                    color = MusFitTheme.colors.surface,
                    shape = groupedShape(index, groupCount),
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
                                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                                color = MusFitTheme.colors.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = prMetaLabel(
                                    pr = pr,
                                    today = LocalDate.now(),
                                    locale = locale,
                                    todayLabel = todayLabel,
                                    yesterdayLabel = yesterdayLabel,
                                ).asString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MusFitTheme.colors.onSurfaceVariant,
                                modifier = Modifier.padding(top = 1.dp),
                            )
                        }
                        Text(
                            text = stringResource(
                                R.string.training_kg_e1rm,
                                pr.estimatedOneRepMaxKg.formatE1rm(LocalConfiguration.current.locales[0]),
                            ),
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = MusFitTheme.colors.positive,
                        )
                    }
                }
            }
            Surface(
                onClick = onOpenAllExercises,
                color = MusFitTheme.colors.surface,
                shape = groupedShape(groupCount - 1, groupCount),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                ) {
                    Text(
                        text = stringResource(R.string.training_all_exercises),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent.color,
                    )
                }
            }
        }
    }
}

// --- Display helpers (pure, unit-tested) ---

/** "BACK SQUAT · ESTIMATED 1RM" — the e1RM hero overline. */
internal fun progressHeroOverline(
    exerciseName: String?,
    estimatedOneRepMaxLabel: String,
    locale: Locale = Locale.getDefault(),
): UiText {
    val label = UiText.Verbatim(estimatedOneRepMaxLabel.uppercase(locale))
    val exercise = exerciseName?.trim()?.takeIf(String::isNotBlank)?.uppercase(locale)
    return if (exercise == null) {
        label
    } else {
        listOf(UiText.Verbatim(exercise), label).joinedWithMiddleDot()
    }
}

internal fun filterTrendByPeriod(
    trend: List<TrainingTrendPoint>,
    period: TrainingProgressPeriod,
    today: LocalDate,
): List<TrainingTrendPoint> {
    val cutoff = today.minusDays(period.days).toEpochDay()
    return trend.filter { it.dateEpochDay >= cutoff }.sortedBy { it.dateEpochDay }
}

/** e1RM change across the visible window; null with fewer than two points. */
internal fun e1rmDeltaKg(trend: List<TrainingTrendPoint>): Double? {
    if (trend.size < 2) return null
    return trend.last().bestEstimatedOneRepMaxKg - trend.first().bestEstimatedOneRepMaxKg
}

internal fun deltaLabel(delta: Double, locale: Locale = Locale.getDefault()): UiText {
    val magnitude = kotlin.math.abs(delta).formatE1rm(locale)
    return uiText(
        if (delta >= 0) R.string.training_delta_positive else R.string.training_delta_negative,
        UiText.Argument.Text(magnitude),
    )
}

/** Up to four month labels spread across the visible trend window. */
internal fun monthLabelsFor(
    trend: List<TrainingTrendPoint>,
    locale: Locale = Locale.getDefault(),
): List<String> {
    if (trend.isEmpty()) return emptyList()
    val months = trend
        .map { YearMonth.from(LocalDate.ofEpochDay(it.dateEpochDay)) }
        .distinct()
        .sorted()
    val picked = if (months.size <= 4) {
        months
    } else {
        listOf(
            months.first(),
            months[months.size / 3],
            months[months.size * 2 / 3],
            months.last(),
        ).distinct()
    }
    return picked.map { it.atDay(1).format(DateTimeFormatter.ofPattern("MMM", locale)) }
}

/** "W27" ISO week label for a volume bar. */
internal fun weekLabel(weekStartEpochDay: Long): UiText {
    val week = LocalDate.ofEpochDay(weekStartEpochDay).get(WeekFields.ISO.weekOfWeekBasedYear())
    return uiText(R.string.training_week_number, UiText.Argument.Integer(week))
}

internal fun e1rmChartSummary(
    exerciseName: String?,
    trend: List<TrainingTrendPoint>,
): UiText {
    val title = exerciseName
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?.let { name -> uiText(R.string.training_e1rm_chart_named, UiText.Argument.Text(name)) }
        ?: uiText(R.string.training_e1rm_chart)
    if (trend.isEmpty()) {
        return uiText(
            R.string.training_sentence_pair,
            UiText.Argument.Nested(title),
            UiText.Argument.Resource(R.string.training_no_data_sentence),
        )
    }
    return pluralUiText(
        R.plurals.training_e1rm_chart_point_summary,
        trend.size,
        UiText.Argument.Nested(title),
        UiText.Argument.Integer(trend.size),
        UiText.Argument.Nested(dateRangeDescription(trend.map { it.dateEpochDay }, isWeekly = false)),
    )
}

internal fun e1rmChartSelectionDescription(
    trend: List<TrainingTrendPoint>,
    selectedIndex: Int,
): UiText {
    val point = trend.getOrNull(selectedIndex) ?: return uiText(R.string.training_no_data_selected)
    return uiText(
        R.string.training_selected_e1rm,
        UiText.Argument.Text(accessibleDate(point.dateEpochDay)),
        UiText.Argument.Nested(
            uiText(
                R.string.training_kilograms,
                UiText.Argument.Text(point.bestEstimatedOneRepMaxKg.formatE1rm()),
            ),
        ),
        UiText.Argument.Integer(selectedIndex + 1),
        UiText.Argument.Integer(trend.size),
    )
}

internal fun e1rmVisualSelectionTag(point: TrainingTrendPoint): String = "training-e1rm-selection-${point.dateEpochDay}-${point.bestEstimatedOneRepMaxKg.formatE1rm(Locale.ROOT)}"

internal fun weeklyVolumeChartSummary(weeks: List<WeeklyTrainingVolume>): UiText {
    val title = uiText(R.string.training_weekly_volume_chart)
    if (weeks.isEmpty()) {
        return uiText(
            R.string.training_sentence_pair,
            UiText.Argument.Nested(title),
            UiText.Argument.Resource(R.string.training_no_data_sentence),
        )
    }
    return pluralUiText(
        R.plurals.training_weekly_chart_summary,
        weeks.size,
        UiText.Argument.Nested(title),
        UiText.Argument.Integer(weeks.size),
        UiText.Argument.Nested(dateRangeDescription(weeks.map { it.weekStartEpochDay }, isWeekly = true)),
    )
}

internal fun weeklyVolumeChartSelectionDescription(
    weeks: List<WeeklyTrainingVolume>,
    selectedIndex: Int,
): UiText {
    val week = weeks.getOrNull(selectedIndex) ?: return uiText(R.string.training_no_data_selected)
    return uiText(
        R.string.training_selected_weekly_volume,
        UiText.Argument.Text(accessibleDate(week.weekStartEpochDay)),
        UiText.Argument.Nested(volumeTonsLabel(week.totalVolumeKg)),
        UiText.Argument.Nested(
            pluralUiText(
                R.plurals.training_workout_count,
                week.workoutCount,
                UiText.Argument.Integer(week.workoutCount),
            ),
        ),
        UiText.Argument.Nested(
            pluralUiText(
                R.plurals.training_completed_set_count,
                week.completedSetCount,
                UiText.Argument.Integer(week.completedSetCount),
            ),
        ),
        UiText.Argument.Integer(selectedIndex + 1),
        UiText.Argument.Integer(weeks.size),
    )
}

internal fun weeklyVolumeVisualSelectionTag(week: WeeklyTrainingVolume): String = "training-weekly-volume-selection-${week.weekStartEpochDay}-${volumeTonsMachineLabel(week.totalVolumeKg)}"

internal fun e1rmDataRowDescription(point: TrainingTrendPoint): UiText = uiText(
    R.string.training_e1rm_data_row,
    UiText.Argument.Text(accessibleDate(point.dateEpochDay)),
    UiText.Argument.Nested(
        uiText(
            R.string.training_kilograms,
            UiText.Argument.Text(point.bestEstimatedOneRepMaxKg.formatE1rm()),
        ),
    ),
    UiText.Argument.Nested(volumeTonsLabel(point.volumeKg)),
)

internal fun weeklyVolumeDataRowDescription(week: WeeklyTrainingVolume): UiText = uiText(
    R.string.training_weekly_volume_data_row,
    UiText.Argument.Text(accessibleDate(week.weekStartEpochDay)),
    UiText.Argument.Nested(volumeTonsLabel(week.totalVolumeKg)),
    UiText.Argument.Nested(
        pluralUiText(
            R.plurals.training_workout_count,
            week.workoutCount,
            UiText.Argument.Integer(week.workoutCount),
        ),
    ),
    UiText.Argument.Nested(
        pluralUiText(
            R.plurals.training_completed_set_count,
            week.completedSetCount,
            UiText.Argument.Integer(week.completedSetCount),
        ),
    ),
)

private fun dateRangeDescription(dates: List<Long>, isWeekly: Boolean): UiText = when (dates.size) {
    0 -> UiText.Verbatim(".")

    1 -> uiText(
        if (isWeekly) R.string.training_range_on_week else R.string.training_range_on_date,
        UiText.Argument.Text(accessibleDate(dates.single())),
    )

    else -> uiText(
        if (isWeekly) R.string.training_range_from_week_to_week else R.string.training_range_from_to,
        UiText.Argument.Text(accessibleDate(dates.first())),
        UiText.Argument.Text(accessibleDate(dates.last())),
    )
}

private fun accessibleDate(epochDay: Long, locale: Locale = Locale.getDefault()): String = LocalizedFormatter.date(
    value = LocalDate.ofEpochDay(epochDay),
    style = java.time.format.FormatStyle.LONG,
    locale = locale,
)

/** "11.2 t" above a tonne, otherwise "840 kg". */
internal fun volumeTonsLabel(
    volumeKg: Double,
    locale: Locale = Locale.getDefault(),
): UiText = if (volumeKg >= 1000.0) {
    val tons = volumeKg / 1000.0
    uiText(
        R.string.training_tonnes,
        UiText.Argument.Text(LocalizedFormatter.number(tons, maximumFractionDigits = 1, locale = locale)),
    )
} else {
    uiText(
        R.string.training_kilograms,
        UiText.Argument.Text(LocalizedFormatter.integer(Math.round(volumeKg), locale = locale)),
    )
}

private fun volumeTonsMachineLabel(volumeKg: Double): String = if (volumeKg >= 1000.0) {
    "${LocalizedFormatter.number(volumeKg / 1000.0, maximumFractionDigits = 1, locale = Locale.ROOT)} t"
} else {
    "${LocalizedFormatter.integer(Math.round(volumeKg), locale = Locale.ROOT)} kg"
}

/** PR row meta (mock 5b): "107.5 kg × 5 · Wed". */
internal fun prMetaLabel(
    pr: TrainingPrRecord,
    today: LocalDate,
    todayLabel: String,
    yesterdayLabel: String,
    locale: Locale = Locale.getDefault(),
): UiText {
    val date = LocalDate.ofEpochDay(pr.dateEpochDay)
    val dateLabel = when {
        date == today -> todayLabel
        date == today.minusDays(1) -> yesterdayLabel
        !date.isBefore(today.minusDays(6)) -> date.format(DateTimeFormatter.ofPattern("EEE", locale))
        else -> LocalizedFormatter.date(date, java.time.format.FormatStyle.MEDIUM, locale)
    }
    return uiText(
        R.string.training_pr_row_meta,
        UiText.Argument.Text(pr.weightKg.formatE1rm(locale)),
        UiText.Argument.Integer(pr.reps),
        UiText.Argument.Text(dateLabel),
    )
}

internal fun Double.formatE1rm(locale: Locale = Locale.getDefault()): String = LocalizedFormatter.number(this, maximumFractionDigits = 1, locale = locale)

private const val VOLUME_WEEK_COUNT = 6
private const val RECENT_PR_COUNT = 5
