@file:OptIn(ExperimentalMaterial3Api::class)

package com.musfit.ui.profile

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musfit.ui.theme.BrandCoral
import com.musfit.ui.theme.MusFitMotion
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.RadioOutline
import com.musfit.ui.theme.RadioOutlineDark
import com.musfit.ui.theme.TabAccent

// Shared Turn 11 vocabulary for the Profile tab and every settings surface:
// grouped hub rows, connected segment groups, teal switches/radios, field
// tiles, the permission segment bar, and the naked profile trend chart.

/** Section label on grouped settings surfaces: 13/800 + optional accent action. */
@Composable
internal fun GroupLabel(
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionColor: Color = MusFitTheme.colors.onSurface,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MusFitTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.W800),
            color = MusFitTheme.colors.onSurface,
        )
        if (actionLabel != null && onAction != null) {
            Surface(
                onClick = onAction,
                color = Color.Transparent,
                shape = RoundedCornerShape(99.dp),
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text(
                        text = actionLabel,
                        style = MusFitTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.W800),
                        color = actionColor,
                    )
                }
            }
        }
    }
}

/**
 * A grouped white hub row: leading badge slot, emphasized 15/700 title over a
 * quiet 12sp subline, trailing slot (chevron by default). Corner radii come
 * from the caller via [groupedShape]-style positional shapes.
 */
@Composable
internal fun ProfileHubRow(
    title: String,
    subtitle: String?,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = title,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = { HubRowChevron() },
) {
    Surface(
        color = MusFitTheme.colors.surface,
        shape = shape,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .let { if (onClick != null) it.clickable(onClickLabel = onClickLabel, onClick = onClick) else it },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            leading?.invoke()
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    title,
                    style = MusFitTheme.typography.titleSmall.copy(fontSize = 15.sp),
                    color = MusFitTheme.colors.onSurface,
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MusFitTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
            }
            trailing?.invoke()
        }
    }
}

@Composable
internal fun HubRowChevron() {
    Icon(
        Icons.Outlined.ChevronRight,
        contentDescription = null,
        tint = MusFitTheme.colors.onSurfaceFaint,
        modifier = Modifier.size(20.dp),
    )
}

/** Compact value row (Units / Theme / Provider…): 14.5/700 title, 13/700 value. */
@Composable
internal fun CompactValueRow(
    title: String,
    value: String,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Surface(color = MusFitTheme.colors.surface, shape = shape, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = if (subtitle == null) 14.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    title,
                    style = MusFitTheme.typography.titleSmall.copy(fontSize = 14.5.sp),
                    color = MusFitTheme.colors.onSurface,
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MusFitTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
            }
            Text(
                value,
                style = MusFitTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.W700),
                color = MusFitTheme.colors.onSurfaceVariant,
            )
            trailing?.invoke()
        }
    }
}

/** Filled accent pill for hero actions ("＋ Log weight", "Test", "Grant 2 more"). */
@Composable
internal fun HeroActionPill(
    text: String,
    accent: TabAccent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = if (enabled) accent.color else accent.color.copy(alpha = 0.5f),
        contentColor = accent.onColor,
        shape = RoundedCornerShape(99.dp),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp))
            }
            Text(
                text,
                style = MusFitTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.W800),
                maxLines = 1,
            )
        }
    }
}

/** Tonal accent-container pill with a leading icon (11d Refresh / Sync / Export row). */
@Composable
internal fun TonalActionPill(
    text: String,
    icon: ImageVector,
    accent: TabAccent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = accent.container,
        contentColor = accent.onContainer,
        shape = RoundedCornerShape(99.dp),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp))
            Text(
                text,
                style = MusFitTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.W800),
                maxLines = 1,
            )
        }
    }
}

/** Small white chip on a tonal hero (goal chip, status pill, "Edit"). */
@Composable
internal fun HeroChip(
    text: String,
    accent: TabAccent,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(99.dp)
    Surface(
        color = MusFitTheme.colors.surface,
        contentColor = accent.onContainer,
        shape = shape,
        modifier = modifier.let {
            if (onClick != null) it.clip(shape).clickable(onClickLabel = text, onClick = onClick) else it
        },
    ) {
        Text(
            text,
            style = MusFitTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.W800),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
        )
    }
}

/**
 * The Turn 8 composite coach mark: a filled chat bubble with the auto_awesome
 * sparkle knocked out in the surface behind it. Coral even on Profile surfaces.
 */
@Composable
internal fun CoachChatMark(
    knockoutColor: Color,
    modifier: Modifier = Modifier,
    bubbleColor: Color = BrandCoral,
    size: Dp = 20.dp,
) {
    Box(modifier = modifier.size(size)) {
        Icon(
            Icons.Filled.ChatBubble,
            contentDescription = null,
            tint = bubbleColor,
            modifier = Modifier.size(size),
        )
        Icon(
            Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = knockoutColor,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = size * 0.2f)
                .size(size * 0.45f),
        )
    }
}

/**
 * The M3E connected single-select group (11e): 2dp gaps; outer ends keep 99dp
 * pill corners, inner corners rest at 8dp. The selected segment fills with the
 * accent, morphs to a full pill, grows (flex ≈ 1.35, spatial spring) and shows
 * a leading check — unless the option carries its own [optionIcon], which is
 * shown on every segment instead. [equalWidths] (11f ranges) disables the
 * grow/pill morph and the check, keeping positional corners on selection.
 */
@Composable
internal fun <T> ConnectedSegmentRow(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    accent: TabAccent,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    optionIcon: ((T) -> ImageVector?)? = null,
    equalWidths: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { index, option ->
            val active = option == selected
            val weight by animateFloatAsState(
                targetValue = if (active && !equalWidths) 1.35f else 1f,
                animationSpec = MusFitMotion.spatial(),
                label = "segmentWeight",
            )
            val pill = active && !equalWidths
            val outerStart = if (index == 0) 99.dp else 8.dp
            val outerEnd = if (index == options.size - 1) 99.dp else 8.dp
            val startRadius by animateDpAsState(
                targetValue = if (pill) 99.dp else outerStart,
                animationSpec = MusFitMotion.spatial(),
                label = "segmentStartRadius",
            )
            val endRadius by animateDpAsState(
                targetValue = if (pill) 99.dp else outerEnd,
                animationSpec = MusFitMotion.spatial(),
                label = "segmentEndRadius",
            )
            val fill by animateColorAsState(
                targetValue = if (active) accent.color else MusFitTheme.colors.surface,
                animationSpec = MusFitMotion.effects(),
                label = "segmentFill",
            )
            val content by animateColorAsState(
                targetValue = if (active) accent.onColor else MusFitTheme.colors.onSurface,
                animationSpec = MusFitMotion.effects(),
                label = "segmentContent",
            )
            val shape = RoundedCornerShape(
                topStart = startRadius,
                bottomStart = startRadius,
                topEnd = endRadius,
                bottomEnd = endRadius,
            )
            Surface(
                color = fill,
                contentColor = content,
                shape = shape,
                modifier = Modifier
                    .weight(weight)
                    .heightIn(min = 48.dp)
                    .clip(shape)
                    .selectable(
                        selected = active,
                        role = Role.RadioButton,
                        onClick = { onSelect(option) },
                    ),
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                ) {
                    val icon = optionIcon?.invoke(option)
                    when {
                        icon != null -> {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = if (active) content else MusFitTheme.colors.onSurfaceVariant,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                        }
                        active && !equalWidths -> {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                tint = content,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                        }
                    }
                    Text(
                        text = label(option),
                        style = MusFitTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.W800),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** The Turn 11 switch: accent track, white thumb with a 13dp accent check when on. */
@Composable
internal fun AccentSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accent: TabAccent,
    modifier: Modifier = Modifier,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedTrackColor = accent.color,
            checkedThumbColor = MusFitTheme.colors.surface,
            checkedIconColor = accent.color,
            uncheckedTrackColor = MusFitTheme.colors.track,
            uncheckedThumbColor = MusFitTheme.colors.surface,
            uncheckedBorderColor = Color.Transparent,
        ),
        thumbContent = if (checked) {
            { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(13.dp)) }
        } else {
            null
        },
        modifier = modifier,
    )
}

/** 22dp M3E radio: selected = 2dp accent ring + 10dp dot; unselected = quiet ring. */
@Composable
internal fun RadioCircle(selected: Boolean, accent: TabAccent, modifier: Modifier = Modifier) {
    val restingOutline = if (isSystemInDarkTheme()) RadioOutlineDark else RadioOutline
    Box(
        modifier = modifier
            .size(22.dp)
            .border(2.dp, if (selected) accent.color else restingOutline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(Modifier.size(10.dp).background(accent.color, CircleShape))
        }
    }
}

/**
 * The 11d segmented permission indicator: one 12dp segment per permission with
 * 4dp gaps; outer end caps 99dp, inner corners 4dp; granted segments in the
 * accent, missing ones white @75% on the tonal hero.
 */
@Composable
internal fun PermissionSegments(
    granted: Int,
    total: Int,
    accent: TabAccent,
    modifier: Modifier = Modifier,
) {
    if (total <= 0) return
    val missingColor = MusFitTheme.colors.surface.copy(alpha = 0.75f)
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(total) { index ->
            val start = if (index == 0) 99.dp else 4.dp
            val end = if (index == total - 1) 99.dp else 4.dp
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(12.dp)
                    .background(
                        color = if (index < granted) accent.color else missingColor,
                        shape = RoundedCornerShape(
                            topStart = start,
                            bottomStart = start,
                            topEnd = end,
                            bottomEnd = end,
                        ),
                    ),
            )
        }
    }
}

/**
 * Editable white field tile (11e): 16dp corners, 11/600 label over a 19/800
 * value with a quiet unit suffix at the baseline.
 */
@Composable
internal fun ProfileFieldTile(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    unit: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Decimal,
) {
    Surface(color = MusFitTheme.colors.surface, shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
        ) {
            Text(
                text = label,
                style = MusFitTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.W600,
                    letterSpacing = 0.sp,
                ),
                color = MusFitTheme.colors.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.clearAndSetSemantics { },
            )
            Row(verticalAlignment = Alignment.Bottom) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MusFitTheme.typography.titleLarge.copy(
                        fontSize = 19.sp,
                        fontWeight = FontWeight.W800,
                        letterSpacing = (-0.4).sp,
                        color = MusFitTheme.colors.onSurface,
                    ),
                    cursorBrush = SolidColor(MusFitTheme.colors.brand),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .semantics {
                            contentDescription = if (unit.isBlank()) label else "$label, $unit"
                        },
                )
                Text(
                    unit,
                    style = MusFitTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    fontWeight = FontWeight.Medium,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier
                        .padding(start = 4.dp, bottom = 1.dp)
                        .clearAndSetSemantics { },
                )
            }
        }
    }
}

/**
 * The naked Turn 11 trend line: a Catmull-Rom-smoothed 3.5dp stroke with a 5dp
 * endpoint dot — no area fill — plus an optional dashed goal hairline (11f).
 * The line sweeps in over ~600ms on entry. Goal values participate in the
 * vertical scale so the hairline always fits inside the canvas.
 */
@Composable
internal fun ProfileTrendChart(
    values: List<Double>,
    color: Color,
    modifier: Modifier = Modifier,
    goalValue: Double? = null,
    goalColor: Color = color,
    strokeWidth: Dp = 3.5.dp,
) {
    val sweep = remember(values) { Animatable(0f) }
    LaunchedEffect(values) {
        sweep.snapTo(0f)
        sweep.animateTo(1f, tween(durationMillis = 600))
    }
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val stroke = strokeWidth.toPx()
        val dotRadius = 5.dp.toPx()
        val padX = dotRadius + 1f
        val padY = dotRadius + 1f
        val min = minOf(values.min(), goalValue ?: values.min())
        val max = maxOf(values.max(), goalValue ?: values.max())
        val plotW = size.width - padX * 2f
        val plotH = size.height - padY * 2f
        fun yFor(value: Double): Float =
            if (max == min) {
                size.height / 2f
            } else {
                padY + plotH * (1f - ((value - min) / (max - min)).toFloat())
            }

        if (goalValue != null) {
            drawLine(
                color = goalColor,
                start = Offset(padX, yFor(goalValue)),
                end = Offset(size.width - padX, yFor(goalValue)),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 6.dp.toPx())),
            )
        }

        val points = values.mapIndexed { index, value ->
            Offset(padX + index * (plotW / (values.size - 1)), yFor(value))
        }
        val path = Path()
        path.moveTo(points.first().x, points.first().y)
        for (i in 0 until points.size - 1) {
            val p0 = points[if (i == 0) i else i - 1]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = points[if (i + 2 < points.size) i + 2 else i + 1]
            path.cubicTo(
                p1.x + (p2.x - p0.x) / 6f,
                p1.y + (p2.y - p0.y) / 6f,
                p2.x - (p3.x - p1.x) / 6f,
                p2.y - (p3.y - p1.y) / 6f,
                p2.x,
                p2.y,
            )
        }
        val measure = PathMeasure()
        measure.setPath(path, false)
        val partial = Path()
        measure.getSegment(0f, measure.length * sweep.value, partial, true)
        drawPath(
            partial,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        if (sweep.value >= 1f) {
            drawCircle(color = color, radius = dotRadius, center = points.last())
        }
    }
}
