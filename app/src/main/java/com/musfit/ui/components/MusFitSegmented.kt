package com.musfit.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent

/**
 * The shared single-select segmented control: accent-tinted active segment, neutral
 * inactive. One switcher styling for every tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> MusFitSegmented(
    options: List<T>,
    selected: T,
    accent: TabAccent,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    onSelect: (T) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = accent.container,
                    activeContentColor = accent.onContainer,
                    activeBorderColor = accent.color,
                    inactiveContainerColor = MusFitTheme.colors.surface,
                    inactiveContentColor = MusFitTheme.colors.onSurfaceVariant,
                ),
                icon = {},
                label = {
                    Text(
                        text = label(option),
                        style = MusFitTheme.typography.labelMedium,
                        fontWeight = if (option == selected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                    )
                },
            )
        }
    }
}
