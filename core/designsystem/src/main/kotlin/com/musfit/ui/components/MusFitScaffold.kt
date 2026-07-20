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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.musfit.ui.theme.MusFitTheme

/**
 * The shared tab header: an emphasized 34/w800 page title with an optional
 * quiet subtitle underneath (e.g. the date), plus trailing actions. One idiom
 * for every tab — same type, height, and alignment.
 */
@Composable
fun MusFitScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val useStackedLayout = LocalDensity.current.fontScale >= SCREEN_HEADER_STACKED_FONT_SCALE
    if (useStackedLayout) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MusFitTheme.spacing.sm),
        ) {
            ScreenHeaderTitle(title = title, subtitle = subtitle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScreenHeaderTitle(
                title = title,
                subtitle = subtitle,
                modifier = Modifier.weight(1f),
            )
            Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        }
    }
}

@Composable
private fun ScreenHeaderTitle(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MusFitTheme.typography.headlineMedium,
            color = MusFitTheme.colors.onSurface,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MusFitTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        }
    }
}

private const val SCREEN_HEADER_STACKED_FONT_SCALE = 1.3f

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
