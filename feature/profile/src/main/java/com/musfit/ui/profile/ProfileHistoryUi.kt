package com.musfit.ui.profile

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musfit.feature.profile.R
import com.musfit.ui.components.InnerScreenTitleStyle
import com.musfit.ui.components.TonalHeaderIconButton
import com.musfit.ui.components.TonalIconSquare
import com.musfit.ui.components.groupedShape
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/** One logged value on a history surface (weight or a body measurement). */
data class HistoryEntry(
    val id: String,
    val measuredAtEpochMillis: Long,
    val value: Double,
    val unit: String,
)

internal enum class HistoryRange(
    @StringRes val chipLabelResource: Int,
    @StringRes val titleResource: Int,
    val days: Long?,
) {
    Week(R.string.profile_range_7_days_short, R.string.profile_range_7_days, 7L),
    Month(R.string.profile_range_30_days_short, R.string.profile_range_30_days, 30L),
    Quarter(R.string.profile_range_90_days_short, R.string.profile_range_90_days, 90L),
    All(R.string.profile_range_all, R.string.profile_range_all_time, null),
}

private fun formatEntryTimestamp(epochMillis: Long, locale: Locale): String = Instant.ofEpochMilli(epochMillis)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", locale))

private fun formatAxisDate(epochMillis: Long, locale: Locale): String = Instant.ofEpochMilli(epochMillis)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("d MMM", locale))

internal fun historyRangeStartEpochMillis(
    range: HistoryRange,
    today: LocalDate,
    zone: ZoneId,
): Long? = range.days?.let { days ->
    today.minusDays(days - 1L).atStartOfDay(zone).toInstant().toEpochMilli()
}

internal fun historyEntriesInRange(
    entries: List<HistoryEntry>,
    range: HistoryRange,
    today: LocalDate,
    zone: ZoneId,
): List<HistoryEntry> {
    val fromMillis = historyRangeStartEpochMillis(range, today, zone) ?: return entries
    return entries.filter { it.measuredAtEpochMillis >= fromMillis }
}

/**
 * The Turn 11 history surface (11f): back header with a filled add-squircle,
 * connected range selector, naked trend chart with an optional dashed goal
 * hairline, and value-first grouped entry rows with an Edit/Delete menu. One
 * pattern for weight and every body measurement.
 */
@Composable
fun MeasurementHistoryScreen(
    title: String,
    entries: List<HistoryEntry>, // newest-first
    accent: TabAccent,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (id: String, value: Double) -> Unit,
    onDelete: (id: String) -> Unit,
    modifier: Modifier = Modifier,
    goalValue: Double? = null,
) {
    var range by rememberSaveable { mutableStateOf(HistoryRange.Month) }
    var editing by remember { mutableStateOf<HistoryEntry?>(null) }
    var deleting by remember { mutableStateOf<HistoryEntry?>(null) }

    val zone = ZoneId.systemDefault()
    val inRange = remember(entries, range, zone) {
        historyEntriesInRange(entries, range, LocalDate.now(zone), zone)
    }
    val unit = entries.firstOrNull()?.unit.orEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MusFitTheme.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        HistoryHeader(
            title = title,
            entries = entries,
            inRange = inRange,
            range = range,
            unit = unit,
            accent = accent,
            onBack = onBack,
            onAdd = onAdd,
        )

        ConnectedSegmentRow(
            options = HistoryRange.entries,
            selected = range,
            label = { stringResource(it.chipLabelResource) },
            accent = accent,
            onSelect = { range = it },
            equalWidths = true,
        )

        HistoryChartCard(inRange = inRange, range = range, unit = unit, accent = accent, goalValue = goalValue)

        GroupLabel(stringResource(R.string.profile_entries))
        if (entries.isEmpty()) {
            Text(
                stringResource(R.string.profile_no_entries_yet),
                style = MusFitTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        } else if (inRange.isEmpty()) {
            Text(
                stringResource(R.string.profile_no_entries_in_range),
                style = MusFitTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                inRange.forEachIndexed { index, entry ->
                    // Delta against the chronologically previous entry, looked up in
                    // the FULL series so the top of a windowed range still has one.
                    val fullIndex = entries.indexOfFirst { it.id == entry.id }
                    val previous = entries.getOrNull(fullIndex + 1)
                    HistoryEntryRow(
                        entry = entry,
                        previousValue = previous?.value,
                        goalValue = goalValue,
                        accent = accent,
                        shape = groupedShape(index, inRange.size),
                        onEdit = { editing = entry },
                        onDelete = { deleting = entry },
                    )
                }
            }
        }
    }

    editing?.let { entry ->
        EditHistoryValueDialog(
            entry = entry,
            onDismiss = { editing = null },
            onConfirm = { value ->
                onEdit(entry.id, value)
                editing = null
            },
        )
    }
    deleting?.let { entry ->
        val locale = LocalConfiguration.current.locales[0]
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.profile_delete_entry_question)) },
            text = {
                Text(
                    stringResource(
                        R.string.profile_history_entry_summary,
                        entry.value.format1(locale),
                        entry.unit,
                        formatEntryTimestamp(entry.measuredAtEpochMillis, locale),
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(entry.id)
                    deleting = null
                }) { Text(stringResource(R.string.profile_delete)) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.profile_cancel)) } },
        )
    }
}

/**
 * Back circle · title with the "80.9 kg · +0.5 kg · 30 days" subline (bold
 * latest value, range delta, range label) · filled accent add-squircle.
 */
@Composable
private fun HistoryHeader(
    title: String,
    entries: List<HistoryEntry>,
    inRange: List<HistoryEntry>,
    range: HistoryRange,
    unit: String,
    accent: TabAccent,
    onBack: () -> Unit,
    onAdd: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val separator = stringResource(R.string.profile_separator_middle_dot)
    val rangeTitle = stringResource(range.titleResource)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        TonalHeaderIconButton(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = stringResource(R.string.profile_back),
            onClick = onBack,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = InnerScreenTitleStyle, color = MusFitTheme.colors.onSurface)
            val latest = entries.firstOrNull()
            if (latest != null) {
                val rangeDelta = if (inRange.size >= 2) inRange.first().value - inRange.last().value else null
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.W800, color = MusFitTheme.colors.onSurface)) {
                            append("${latest.value.format1(locale)} $unit")
                        }
                        rangeDelta?.let { append("$separator${it.signedFormat1(locale)} $unit") }
                        append(separator)
                        append(rangeTitle)
                    },
                    style = MusFitTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
        }
        TonalIconSquare(
            icon = Icons.Outlined.Add,
            contentDescription = stringResource(R.string.profile_log_entry),
            onClick = onAdd,
            size = 44.dp,
            cornerRadius = 16.dp,
            containerColor = accent.color,
            contentColor = accent.onColor,
        )
    }
}

@Composable
private fun HistoryChartCard(
    inRange: List<HistoryEntry>,
    range: HistoryRange,
    unit: String,
    accent: TabAccent,
    goalValue: Double?,
) {
    val locale = LocalConfiguration.current.locales[0]
    Surface(color = MusFitTheme.colors.surface, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    stringResource(range.titleResource),
                    style = MusFitTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.W800),
                )
                if (inRange.size >= 2) {
                    val min = inRange.minOf { it.value }
                    val max = inRange.maxOf { it.value }
                    Text(
                        stringResource(
                            R.string.profile_history_value_range,
                            min.format1(locale),
                            max.format1(locale),
                            unit,
                        ),
                        style = MusFitTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
            }
            if (inRange.size >= 2) {
                ProfileTrendChart(
                    values = inRange.map { it.value }.asReversed(),
                    color = accent.color,
                    goalValue = goalValue,
                    goalColor = accent.track,
                    modifier = Modifier.fillMaxWidth().height(92.dp),
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        formatAxisDate(inRange.last().measuredAtEpochMillis, locale),
                        style = MusFitTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal, letterSpacing = 0.sp),
                        color = MusFitTheme.colors.onSurfaceFaint,
                    )
                    if (goalValue != null) {
                        Text(
                            stringResource(R.string.profile_goal_value_with_unit, goalValue.format1(locale), unit),
                            style = MusFitTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal, letterSpacing = 0.sp),
                            color = MusFitTheme.colors.onSurfaceFaint,
                        )
                    }
                    Text(
                        formatAxisDate(inRange.first().measuredAtEpochMillis, locale),
                        style = MusFitTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal, letterSpacing = 0.sp),
                        color = MusFitTheme.colors.onSurfaceFaint,
                    )
                }
            } else {
                Text(
                    stringResource(R.string.profile_log_two_entries_for_trend),
                    style = MusFitTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Value-first entry row. The trailing delta compares against the previous
 * entry: with a goal it reads accent-teal when the change moves toward the
 * goal and quiet gray when it moves away or holds; without a goal any real
 * change is teal (matching the 11a tile trend rule).
 */
@Composable
private fun HistoryEntryRow(
    entry: HistoryEntry,
    previousValue: Double?,
    goalValue: Double?,
    accent: TabAccent,
    shape: RoundedCornerShape,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val entryOptions = stringResource(R.string.profile_entry_options)
    Surface(color = MusFitTheme.colors.surface, shape = shape, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 18.dp, end = 10.dp, top = 13.dp, bottom = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        entry.value.format1(locale),
                        style = MusFitTheme.typography.titleSmall.copy(fontSize = 15.5.sp, fontWeight = FontWeight.W800),
                    )
                    Text(
                        entry.unit,
                        style = MusFitTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        fontWeight = FontWeight.Medium,
                        color = MusFitTheme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(start = 3.dp, bottom = 1.dp),
                    )
                }
                Text(
                    formatEntryTimestamp(entry.measuredAtEpochMillis, locale),
                    style = MusFitTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
            if (previousValue != null) {
                val delta = entry.value - previousValue
                Text(
                    delta.signedFormat1(locale),
                    style = MusFitTheme.typography.labelMedium.copy(fontSize = 12.5.sp, fontWeight = FontWeight.W800),
                    color = deltaColor(delta, previousValue, goalValue, accent),
                )
            }
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = entryOptions }
                        .clip(RoundedCornerShape(99.dp))
                        .clickable(
                            onClickLabel = entryOptions,
                            role = Role.Button,
                            onClick = { menuOpen = true },
                        ),
                ) {
                    Surface(
                        color = MusFitTheme.colors.surface,
                        contentColor = MusFitTheme.colors.onSurfaceFaint,
                        shape = RoundedCornerShape(99.dp),
                        modifier = Modifier.size(32.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.MoreVert,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.profile_edit_value)) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.profile_delete)) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun deltaColor(
    delta: Double,
    previousValue: Double,
    goalValue: Double?,
    accent: TabAccent,
): androidx.compose.ui.graphics.Color {
    // Below display resolution (one decimal) reads as flat.
    val flat = abs(delta) < 0.05
    return when {
        flat -> MusFitTheme.colors.onSurfaceFaint
        goalValue == null -> accent.color
        (goalValue - previousValue) * delta > 0 -> accent.color
        else -> MusFitTheme.colors.onSurfaceFaint
    }
}

@Composable
private fun EditHistoryValueDialog(
    entry: HistoryEntry,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    var text by remember(entry.id, locale) { mutableStateOf(entry.value.format1(locale)) }
    val parsed = text.toPositiveDoubleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_edit_value)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.profile_value_with_unit, entry.unit)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(enabled = parsed != null, onClick = { parsed?.let(onConfirm) }) { Text(stringResource(R.string.profile_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.profile_cancel)) } },
    )
}

internal fun Double.signedFormat1(locale: Locale = Locale.getDefault()): String = when {
    this > 0 -> "+${format1(locale)}"
    this < 0 -> "−${abs(this).format1(locale)}"
    else -> format1(locale)
}
