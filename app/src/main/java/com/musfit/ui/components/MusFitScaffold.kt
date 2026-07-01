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
import androidx.compose.ui.text.font.FontWeight
import com.musfit.ui.theme.MusFitTheme

/**
 * The shared tab header: title (left) + optional muted subtitle + trailing icon
 * actions (right). One idiom for every tab — same type, height, and alignment.
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
            Text(
                text = title,
                style = MusFitTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
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
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/**
 * The shared scrolling screen container: cream background, standard edge padding,
 * a [MusFitScreenHeader] at the top, then a vertically-spaced content slot. Tabs
 * that need a non-scrolling or lazy container use [MusFitScreenHeader] directly.
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
            .padding(MusFitTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(MusFitTheme.spacing.lg),
    ) {
        MusFitScreenHeader(title = title, subtitle = subtitle, actions = actions)
        content()
    }
}
