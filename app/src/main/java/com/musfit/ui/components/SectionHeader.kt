package com.musfit.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.musfit.ui.theme.MusFitTheme

/**
 * The shared header for every titled block — a small, quiet `titleMedium`
 * (16/500) label + optional accent-colored trailing text action ("See all",
 * "Edit", …).
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailingActionLabel: String? = null,
    trailingActionColor: Color = MusFitTheme.colors.onSurfaceVariant,
    onTrailingAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MusFitTheme.typography.titleMedium,
            color = MusFitTheme.colors.onSurface,
        )
        if (trailingActionLabel != null && onTrailingAction != null) {
            TextButton(onClick = onTrailingAction) {
                Text(
                    text = trailingActionLabel,
                    style = MusFitTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = trailingActionColor,
                )
            }
        }
    }
}
