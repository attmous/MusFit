package com.musfit.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musfit.ui.theme.MusFitMotion
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent

/**
 * The shared single-select switcher, restyled to the health-grade clean language:
 * a plain row of text pills — the active option is a dark filled pill (onSurface
 * ground, surface text), inactive options are quiet secondary text. No borders.
 * [accent] is kept in the signature so call sites stay tab-aware, but the pill is
 * deliberately monochrome — one styling for every tab.
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
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { option ->
            val active = option == selected
            val pillColor by animateColorAsState(
                targetValue = if (active) MusFitTheme.colors.onSurface else Color.Transparent,
                animationSpec = MusFitMotion.effects(),
                label = "segmentPillColor",
            )
            val textColor by animateColorAsState(
                targetValue = if (active) MusFitTheme.colors.surface else MusFitTheme.colors.onSurfaceVariant,
                animationSpec = MusFitMotion.effects(),
                label = "segmentTextColor",
            )
            Surface(
                onClick = { onSelect(option) },
                color = pillColor,
                shape = RoundedCornerShape(999.dp),
            ) {
                Text(
                    text = label(option),
                    style = MusFitTheme.typography.labelLarge,
                    fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                    color = textColor,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }
}
