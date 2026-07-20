package com.musfit.ui.food

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musfit.feature.food.R
import com.musfit.ui.components.topGroupShape
import com.musfit.ui.icons.filled.AutoAwesome
import com.musfit.ui.icons.filled.Bolt
import com.musfit.ui.icons.filled.Bookmark
import com.musfit.ui.icons.filled.EditNote
import com.musfit.ui.icons.filled.QrCodeScanner
import com.musfit.ui.icons.outlined.AutoAwesome
import com.musfit.ui.icons.outlined.Bolt
import com.musfit.ui.icons.outlined.Bookmark
import com.musfit.ui.icons.outlined.EditNote
import com.musfit.ui.icons.outlined.QrCodeScanner
import com.musfit.ui.theme.BrandCoral
import com.musfit.ui.theme.MusFitMotion
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccentRole
import com.musfit.ui.theme.tabAccentFor

/** Five equal tiles at normal text size; two columns per row at large text size. */
@Composable
internal fun FoodAddModeRow(
    selected: FoodAddMode,
    onSelect: (FoodAddMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modes = FoodAddMode.entries
    if (LocalDensity.current.fontScale >= 1.3f) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            modes.chunked(2).forEach { rowModes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    rowModes.forEach { mode ->
                        FoodAddModeTile(
                            mode = mode,
                            isSelected = mode == selected,
                            shape = topGroupShape(modes.indexOf(mode), modes.size, outer = 22.dp),
                            onSelect = onSelect,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowModes.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = modifier.fillMaxWidth(),
        ) {
            modes.forEachIndexed { index, mode ->
                FoodAddModeTile(
                    mode = mode,
                    isSelected = mode == selected,
                    shape = topGroupShape(index, modes.size, outer = 22.dp),
                    onSelect = onSelect,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun FoodAddModeTile(
    mode: FoodAddMode,
    isSelected: Boolean,
    shape: Shape,
    onSelect: (FoodAddMode) -> Unit,
    modifier: Modifier,
) {
    val accent = tabAccentFor(TabAccentRole.Food)
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
        modifier = modifier.selectable(
            selected = isSelected,
            onClick = { onSelect(mode) },
            role = Role.RadioButton,
        ),
    ) {
        Surface(
            color = container,
            shape = shape,
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

@Composable
private fun addModeLabel(mode: FoodAddMode): String = stringResource(
    when (mode) {
        FoodAddMode.Saved -> R.string.food_add_mode_saved
        FoodAddMode.Manual -> R.string.food_add_mode_manual
        FoodAddMode.Barcode -> R.string.food_add_mode_barcode
        FoodAddMode.Quick -> R.string.food_add_mode_quick
        FoodAddMode.Ai -> R.string.food_add_mode_ai
    },
)

private fun addModeIcon(mode: FoodAddMode, selected: Boolean): ImageVector = when (mode) {
    FoodAddMode.Saved -> if (selected) Icons.Filled.Bookmark else Icons.Outlined.Bookmark
    FoodAddMode.Manual -> if (selected) Icons.Filled.EditNote else Icons.Outlined.EditNote
    FoodAddMode.Barcode -> if (selected) Icons.Filled.QrCodeScanner else Icons.Outlined.QrCodeScanner
    FoodAddMode.Quick -> if (selected) Icons.Filled.Bolt else Icons.Outlined.Bolt
    FoodAddMode.Ai -> if (selected) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome
}
