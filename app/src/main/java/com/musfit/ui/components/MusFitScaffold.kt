package com.musfit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musfit.ui.theme.MusFitTheme

/**
 * The shared tab header: an optional muted eyebrow line (e.g. the date) over a
 * quiet regular-weight title that reads like a sentence, plus trailing actions.
 * One idiom for every tab — same type, height, and alignment.
 */
@Composable
fun MusFitScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MusFitTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
            Text(
                text = title,
                style = MusFitTheme.typography.headlineMedium,
                color = MusFitTheme.colors.onSurface,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/**
 * The shared scrolling screen container: content directly on the pure surface,
 * generous edge padding, a [MusFitScreenHeader] at the top, then a vertically-
 * spaced content slot. Bottom padding keeps the last row clear of the coach FAB.
 * Tabs that need a non-scrolling or lazy container use [MusFitScreenHeader] directly.
 */
@Composable
fun MusFitScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MusFitTheme.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(start = MusFitTheme.spacing.xl, end = MusFitTheme.spacing.xl, top = MusFitTheme.spacing.xl)
            .padding(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(MusFitTheme.spacing.xl),
    ) {
        MusFitScreenHeader(title = title, subtitle = subtitle, actions = actions)
        content()
    }
}
