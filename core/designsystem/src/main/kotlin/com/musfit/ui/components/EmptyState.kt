package com.musfit.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent

/**
 * Design-language §4: calm empty state — muted icon, one-line sentence-case body,
 * optional accent-tonal action button.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    accent: TabAccent? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MusFitTheme.spacing.xl, horizontal = MusFitTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MusFitTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.height(MusFitTheme.spacing.md))
        Text(
            text = title,
            style = MusFitTheme.typography.titleMedium,
            color = MusFitTheme.colors.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(MusFitTheme.spacing.xs))
        Text(
            text = body,
            style = MusFitTheme.typography.bodyMedium,
            color = MusFitTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (accent != null && actionLabel != null && onAction != null) {
            Spacer(Modifier.height(MusFitTheme.spacing.lg))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent.container,
                    contentColor = accent.onContainer,
                ),
            ) {
                Text(actionLabel)
            }
        }
    }
}
