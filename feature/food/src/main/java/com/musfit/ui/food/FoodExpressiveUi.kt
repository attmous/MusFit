@file:OptIn(ExperimentalMaterial3Api::class)

package com.musfit.ui.food

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musfit.ui.components.ExpressiveBadge
import com.musfit.ui.components.expressiveBadgeShapeFor
import com.musfit.ui.components.groupedShape
import com.musfit.ui.components.topGroupShape
import com.musfit.ui.theme.BrandCoral
import com.musfit.ui.theme.MusFitMotion
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.NeutralOutline
import com.musfit.ui.theme.NeutralOutlineDark
import com.musfit.ui.theme.TabAccentRole
import com.musfit.ui.theme.tabAccentFor

// Shared Food-package building blocks for the Turn 9 inner-screen restyle:
// sheet chrome, chips, mode row, and the dense grouped list row.

/** Sheet header row with a 40dp close visual inside a 48dp interaction target. */
@Composable
internal fun FoodSheetHeader(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    chip: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        leading?.invoke()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MusFitTheme.typography.headlineSmall.copy(fontSize = 24.sp, lineHeight = 28.sp),
                color = MusFitTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MusFitTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        chip?.invoke()
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = "Close" }
                .clip(CircleShape)
                .clickable(
                    onClickLabel = "Close",
                    role = Role.Button,
                    onClick = onClose,
                ),
        ) {
            Surface(
                shape = CircleShape,
                color = MusFitTheme.colors.surfaceVariant,
                contentColor = MusFitTheme.colors.onSurface,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(19.dp))
                }
            }
        }
    }
}

/** "to Lunch" target chip: green container pill + dropdown of meal titles. */
@Composable
internal fun MealTargetChip(
    label: String,
    meals: List<FoodMealDefinitionUiState>,
    onMealSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = tabAccentFor(TabAccentRole.Food)
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(99.dp),
            color = accent.container,
            contentColor = accent.onContainer,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
            ) {
                Text(
                    text = label,
                    style = MusFitTheme.typography.labelMedium.copy(fontWeight = FontWeight.W800),
                    maxLines = 1,
                )
                Icon(
                    Icons.Outlined.KeyboardArrowDown,
                    contentDescription = "Change meal",
                    modifier = Modifier.size(17.dp),
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            meals.forEach { meal ->
                DropdownMenuItem(
                    text = { Text(meal.title) },
                    onClick = {
                        expanded = false
                        onMealSelected(meal.id)
                    },
                )
            }
        }
    }
}

/** Provenance chip ("Open Food Facts", "Edited by you"). */
@Composable
internal fun FoodTrustChip(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = MusFitTheme.colors.trustChip,
        contentColor = MusFitTheme.colors.onTrustChip,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(14.dp))
            Text(
                text = label,
                style = MusFitTheme.typography.labelSmall.copy(fontSize = 11.5.sp, letterSpacing = 0.sp),
                maxLines = 1,
            )
        }
    }
}

/** One of the 4 KCAL/CARBS/PROTEIN/FAT tiles under a barcode match (9d). */
@Composable
internal fun FoodStatTile(
    label: String,
    labelColor: Color,
    value: String,
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MusFitTheme.colors.surface,
        shape = topGroupShape(index, count, outer = 18.dp),
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
        ) {
            Text(
                text = label,
                style = MusFitTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.W800,
                    letterSpacing = 0.4.sp,
                ),
                color = labelColor,
                maxLines = 1,
            )
            Text(
                text = value,
                style = MusFitTheme.typography.titleLarge,
                color = MusFitTheme.colors.onSurface,
                maxLines = 1,
            )
        }
    }
}

/** "Keep adding" white pill + switch, kept beside the final logging action. */
@Composable
internal fun KeepAddingPill(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MusFitTheme.colors.surface,
        shape = RoundedCornerShape(99.dp),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 16.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Text(
                text = "Keep adding",
                style = MusFitTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W800),
                color = MusFitTheme.colors.onSurface,
                maxLines = 1,
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MusFitTheme.colors.brand,
                    checkedThumbColor = MusFitTheme.colors.surface,
                    uncheckedTrackColor = if (isSystemInDarkTheme()) NeutralOutlineDark else NeutralOutline,
                    uncheckedThumbColor = MusFitTheme.colors.surface,
                    uncheckedBorderColor = Color.Transparent,
                ),
            )
        }
    }
}

private fun addModeLabel(mode: FoodAddMode): String = when (mode) {
    FoodAddMode.Saved -> "Saved"
    FoodAddMode.Manual -> "Manual"
    FoodAddMode.Barcode -> "Barcode"
    FoodAddMode.Quick -> "Quick"
    FoodAddMode.Ai -> "AI"
}

// Filled icon on the selected brand tile, outlined on unselected white tiles.
private fun addModeIcon(mode: FoodAddMode, selected: Boolean): ImageVector = when (mode) {
    FoodAddMode.Saved -> if (selected) Icons.Filled.Bookmark else Icons.Outlined.Bookmark
    FoodAddMode.Manual -> if (selected) Icons.Filled.EditNote else Icons.Outlined.EditNote
    FoodAddMode.Barcode -> if (selected) Icons.Filled.QrCodeScanner else Icons.Outlined.QrCodeScanner
    FoodAddMode.Quick -> if (selected) Icons.Filled.Bolt else Icons.Outlined.Bolt
    FoodAddMode.Ai -> if (selected) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome
}

/**
 * The 5-mode entry row (9b): equal 52dp tiles + 11sp labels. The row's top
 * outer corners are 22dp, every other corner 8dp (positional, per the kit);
 * fills/inks morph with the effects spring on selection.
 */
@Composable
internal fun FoodAddModeRow(
    selected: FoodAddMode,
    onSelect: (FoodAddMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = tabAccentFor(TabAccentRole.Food)
    val modes = FoodAddMode.entries
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        modes.forEachIndexed { index, mode ->
            val isSelected = mode == selected
            val container by animateColorAsState(
                targetValue = if (isSelected) MusFitTheme.colors.brand else MusFitTheme.colors.surface,
                animationSpec = MusFitMotion.effects(),
                label = "modeTileFill",
            )
            val iconTint by animateColorAsState(
                targetValue = when {
                    isSelected -> MusFitTheme.colors.onBrand
                    mode == FoodAddMode.Ai -> BrandCoral
                    else -> MusFitTheme.colors.onSurfaceVariant
                },
                animationSpec = MusFitMotion.effects(),
                label = "modeTileIcon",
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f),
            ) {
                Surface(
                    onClick = { onSelect(mode) },
                    color = container,
                    shape = topGroupShape(index, modes.size, outer = 22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            addModeIcon(mode, isSelected),
                            contentDescription = addModeLabel(mode),
                            tint = iconTint,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Text(
                    text = addModeLabel(mode),
                    style = MusFitTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.W800 else FontWeight.W500,
                        letterSpacing = 0.sp,
                    ),
                    color = if (isSelected) accent.onContainer else MusFitTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Dark-vs-white selectable pill chip (sort, filter, unit, preset chips). */
@Composable
@Suppress("LongParameterList") // Shared chip styling keeps its color/icon overrides in one contract.
internal fun SelectableChip(
    text: String,
    selected: Boolean?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedContainer: Color = MusFitTheme.colors.chipSelected,
    selectedContent: Color = MusFitTheme.colors.onChipSelected,
    unselectedContainer: Color = MusFitTheme.colors.surface,
    unselectedContent: Color = MusFitTheme.colors.onSurfaceVariant,
    leadingIcon: ImageVector? = null,
    leadingTint: Color? = null,
) {
    val isSelected = selected == true
    val container by animateColorAsState(
        targetValue = if (isSelected) selectedContainer else unselectedContainer,
        animationSpec = MusFitMotion.effects(),
        label = "chipFill",
    )
    val content by animateColorAsState(
        targetValue = if (isSelected) selectedContent else unselectedContent,
        animationSpec = MusFitMotion.effects(),
        label = "chipInk",
    )
    val interactionModifier = if (selected != null) {
        Modifier.selectable(
            selected = isSelected,
            role = Role.RadioButton,
            onClick = onClick,
        )
    } else {
        Modifier.clickable(
            role = Role.Button,
            onClick = onClick,
        )
    }
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = container,
        contentColor = content,
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .then(interactionModifier),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp),
        ) {
            if (leadingIcon != null) {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    tint = leadingTint ?: content,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = text,
                style = MusFitTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.W700 else FontWeight.W500,
                ),
                maxLines = 1,
            )
        }
    }
}

/**
 * Dense grouped list row (9a items, 9b recents, 9g draft, 9i programs):
 * expressive badge (or photo thumb), title/subtitle, kcal-over-tag trailing
 * column or a custom trailing control.
 */
@Composable
internal fun FoodListItemRow(
    index: Int,
    count: Int,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    fallbackIcon: ImageVector,
    badgeSize: Dp = 48.dp,
    trailingTop: String? = null,
    trailingSub: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val accent = tabAccentFor(TabAccentRole.Food)
    val shape = groupedShape(index, count)
    val rowContent: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            if (imageUrl != null) {
                FoodThumb(imageUrl = imageUrl, fallback = fallbackIcon, size = badgeSize)
            } else {
                ExpressiveBadge(
                    icon = fallbackIcon,
                    shape = expressiveBadgeShapeFor(index),
                    containerColor = accent.container,
                    contentColor = accent.onContainerVariant,
                    size = badgeSize,
                    iconSize = if (badgeSize >= 48.dp) 22.dp else 20.dp,
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = title,
                    style = MusFitTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.W800,
                        letterSpacing = (-0.2).sp,
                    ),
                    color = MusFitTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MusFitTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (trailingContent != null) {
                trailingContent()
            } else if (trailingTop != null || trailingSub != null) {
                Column(horizontalAlignment = Alignment.End) {
                    if (trailingTop != null) {
                        Text(
                            text = trailingTop,
                            style = MusFitTheme.typography.titleMedium,
                            color = accent.onContainer,
                            maxLines = 1,
                        )
                    }
                    if (trailingSub != null) {
                        Text(
                            text = trailingSub,
                            style = MusFitTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.W500,
                                letterSpacing = 0.sp,
                            ),
                            color = MusFitTheme.colors.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
    if (onClick != null) {
        Surface(onClick = onClick, shape = shape, color = MusFitTheme.colors.surface, modifier = modifier) {
            rowContent()
        }
    } else {
        Surface(shape = shape, color = MusFitTheme.colors.surface, modifier = modifier) {
            rowContent()
        }
    }
}
