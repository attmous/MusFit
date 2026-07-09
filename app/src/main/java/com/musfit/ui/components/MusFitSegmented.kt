package com.musfit.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musfit.ui.theme.MusFitMotion
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent

/**
 * The shared single-select switcher as a Material 3 Expressive connected button
 * group: equal-width segments with 2dp gaps; the selected segment springs to a
 * full accent-filled pill while unselected segments relax to quiet 12dp-radius
 * tonal cells.
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
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { option ->
            val active = option == selected
            val fillColor by animateColorAsState(
                targetValue = if (active) accent.color else MusFitTheme.colors.track,
                animationSpec = MusFitMotion.effects(),
                label = "segmentFillColor",
            )
            val textColor by animateColorAsState(
                targetValue = if (active) accent.onColor else MusFitTheme.colors.onSurface,
                animationSpec = MusFitMotion.effects(),
                label = "segmentTextColor",
            )
            val cornerRadius by animateDpAsState(
                targetValue = if (active) 99.dp else 12.dp,
                animationSpec = MusFitMotion.spatial(),
                label = "segmentCornerRadius",
            )
            Surface(
                onClick = { onSelect(option) },
                color = fillColor,
                shape = RoundedCornerShape(cornerRadius),
                modifier = Modifier.weight(1f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label(option),
                        style = MusFitTheme.typography.labelMedium,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 11.dp),
                    )
                }
            }
        }
    }
}
