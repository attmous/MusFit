package com.musfit.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent

/**
 * The reusable "headline moment" card: a contained (inset), soft accent-tinted
 * card each tab fills with its key stat. Background = [TabAccent.container]; place
 * text with [TabAccent.onContainer] and figure strokes with [TabAccent.color].
 *
 * [onClickLabel] renames the announced click action for accessibility services;
 * it only applies when [onClick] is set.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusFitSummaryCard(
    accent: TabAccent,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (onClick != null) {
        Surface(
            onClick = onClick,
            color = accent.container,
            shape = MusFitTheme.shapes.large,
            modifier = modifier
                .fillMaxWidth()
                .then(
                    if (onClickLabel != null) {
                        // Outer semantics merge label-first with the Surface's own clickable,
                        // so this overrides the announced label while keeping the real action.
                        Modifier.semantics { onClick(label = onClickLabel, action = null) }
                    } else {
                        Modifier
                    },
                ),
        ) {
            Column(modifier = Modifier.padding(MusFitTheme.spacing.lg), content = content)
        }
    } else {
        Surface(
            color = accent.container,
            shape = MusFitTheme.shapes.large,
            modifier = modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(MusFitTheme.spacing.lg), content = content)
        }
    }
}
