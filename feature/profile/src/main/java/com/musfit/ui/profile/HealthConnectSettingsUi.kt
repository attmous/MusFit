package com.musfit.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musfit.domain.health.StepSource
import com.musfit.ui.components.InnerScreenHeader
import com.musfit.ui.components.groupedShape
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent

/**
 * The Turn 11 Health Connect page (11d): permissions hero with the segmented
 * indicator (a count is discrete — no wavy bar), tonal Refresh/Sync/Export
 * pills, and the steps-source grouped radio list. Radio taps commit
 * immediately through `selectStepSource`.
 */
@Composable
internal fun HealthConnectSettingsPage(
    state: ProfileSettingsUiState,
    accent: TabAccent,
    onBack: () -> Unit,
    onRequestPermissions: () -> Unit,
    onRefresh: () -> Unit,
    onSync: () -> Unit,
    onExport: () -> Unit,
    onSelectStepSource: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MusFitTheme.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        InnerScreenHeader(
            title = "Health Connect",
            subtitle = "Mirrors steps, weight and heart rate",
            onBack = onBack,
        )

        PermissionsHero(state = state, accent = accent, onRequestPermissions = onRequestPermissions)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TonalActionPill(
                text = "Refresh",
                icon = Icons.Outlined.Refresh,
                accent = accent,
                onClick = onRefresh,
                modifier = Modifier.weight(1f),
            )
            TonalActionPill(
                text = if (state.isHealthConnectSyncing) "Syncing" else "Sync",
                icon = Icons.Outlined.Sync,
                accent = accent,
                onClick = onSync,
                enabled = !state.isHealthConnectSyncing,
                modifier = Modifier.weight(1f),
            )
            TonalActionPill(
                text = "Export",
                icon = Icons.Outlined.Upload,
                accent = accent,
                onClick = onExport,
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            state.message,
            style = MusFitTheme.typography.bodySmall,
            color = MusFitTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        GroupLabel("Steps source")
        Text(
            "The combined total can read higher than a single app because it merges every source. " +
                "Pick one to mirror it exactly.",
            style = MusFitTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = MusFitTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        StepSourceList(
            sources = state.stepSources,
            selectedPackage = state.preferredStepsPackage,
            enabled = state.availabilityLabel == "Available" && !state.isHealthConnectSyncing,
            accent = accent,
            onSelect = onSelectStepSource,
        )
    }
}

@Composable
private fun PermissionsHero(
    state: ProfileSettingsUiState,
    accent: TabAccent,
    onRequestPermissions: () -> Unit,
) {
    val total = state.requestablePermissionCount
    val granted = state.grantedPermissionCount
    val missing = (total - granted).coerceAtLeast(0)
    Surface(color = accent.container, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "DEVICE SYNC",
                    style = MusFitTheme.typography.labelSmall.copy(fontWeight = FontWeight.W800, letterSpacing = 0.8.sp),
                    color = accent.onContainer,
                )
                HeroChip(text = availabilityPillLabel(state.availabilityLabel), accent = accent)
            }
            if (total > 0) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "$granted of $total",
                        style = MusFitTheme.typography.displayMedium.copy(fontSize = 44.sp, lineHeight = 44.sp),
                        color = accent.onContainer,
                        maxLines = 1,
                    )
                    Text(
                        "permissions granted",
                        style = MusFitTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        fontWeight = FontWeight.Medium,
                        color = accent.onContainer,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
                    )
                }
                PermissionSegments(granted = granted, total = total, accent = accent)
            } else {
                Text(
                    "Refresh to check available health permissions.",
                    style = MusFitTheme.typography.bodyMedium,
                    color = accent.onContainerVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = when {
                        total > 0 && missing == 0 -> "All permissions granted"
                        total > 0 -> "$missing more to grant"
                        else -> ""
                    },
                    style = MusFitTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = accent.onContainerVariant,
                    modifier = Modifier.weight(1f),
                )
                if (state.canRequestPermissions) {
                    HeroActionPill(
                        text = if (missing > 0) "Grant $missing more" else "Review",
                        accent = accent,
                        onClick = onRequestPermissions,
                    )
                }
            }
        }
    }
}

@Composable
private fun StepSourceList(
    sources: List<StepSource>,
    selectedPackage: String?,
    enabled: Boolean,
    accent: TabAccent,
    onSelect: (String?) -> Unit,
) {
    val rowCount = 1 + sources.size
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.selectableGroup(),
    ) {
        StepSourceRow(
            label = "All sources (unified)",
            detail = buildAnnotatedString { append("Health Connect combined total") },
            selected = selectedPackage == null,
            enabled = enabled,
            accent = accent,
            shape = groupedShape(0, rowCount),
            onClick = { onSelect(null) },
        )
        sources.forEachIndexed { index, source ->
            StepSourceRow(
                label = source.label,
                detail = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.W800, color = MusFitTheme.colors.onSurface)) {
                        append("%,d".format(source.steps))
                    }
                    append(" steps today")
                },
                selected = selectedPackage == source.packageName,
                enabled = enabled,
                accent = accent,
                shape = groupedShape(index + 1, rowCount),
                onClick = { onSelect(source.packageName) },
            )
        }
        if (sources.isEmpty()) {
            Text(
                "No step sources found for today yet. Sync or walk a little, then come back.",
                style = MusFitTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun StepSourceRow(
    label: String,
    detail: androidx.compose.ui.text.AnnotatedString,
    selected: Boolean,
    enabled: Boolean,
    accent: TabAccent,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
) {
    Surface(
        color = MusFitTheme.colors.surface,
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            RadioCircle(selected = selected, accent = accent)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    label,
                    style = MusFitTheme.typography.titleSmall.copy(fontSize = 15.sp),
                    color = MusFitTheme.colors.onSurface,
                )
                Text(
                    detail,
                    style = MusFitTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
        }
    }
}

private fun availabilityPillLabel(availabilityLabel: String): String = when (availabilityLabel) {
    "Install or update required" -> "Needs app"
    else -> availabilityLabel
}
