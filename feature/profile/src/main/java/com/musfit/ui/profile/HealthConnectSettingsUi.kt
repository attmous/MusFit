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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musfit.domain.health.StepSource
import com.musfit.feature.profile.R
import com.musfit.ui.components.InnerScreenHeader
import com.musfit.ui.components.groupedShape
import com.musfit.ui.icons.outlined.Sync
import com.musfit.ui.icons.outlined.Upload
import com.musfit.ui.text.LocalizedFormatter
import com.musfit.ui.text.UiText
import com.musfit.ui.text.asString
import com.musfit.ui.text.uiText
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
            title = stringResource(R.string.profile_health_connect),
            subtitle = stringResource(R.string.profile_health_connect_subtitle),
            onBack = onBack,
        )

        PermissionsHero(state = state, accent = accent, onRequestPermissions = onRequestPermissions)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TonalActionPill(
                text = stringResource(R.string.profile_refresh),
                icon = Icons.Outlined.Refresh,
                accent = accent,
                onClick = onRefresh,
                modifier = Modifier.weight(1f),
            )
            TonalActionPill(
                text = stringResource(if (state.isHealthConnectSyncing) R.string.profile_syncing else R.string.profile_sync),
                icon = Icons.Outlined.Sync,
                accent = accent,
                onClick = onSync,
                enabled = !state.isHealthConnectSyncing,
                modifier = Modifier.weight(1f),
            )
            TonalActionPill(
                text = stringResource(R.string.profile_export),
                icon = Icons.Outlined.Upload,
                accent = accent,
                onClick = onExport,
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            state.message.asString(),
            style = MusFitTheme.typography.bodySmall,
            color = MusFitTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        GroupLabel(stringResource(R.string.profile_steps_source))
        Text(
            stringResource(R.string.profile_steps_source_explanation),
            style = MusFitTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = MusFitTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        StepSourceList(
            sources = state.stepSources,
            selectedPackage = state.preferredStepsPackage,
            enabled = state.availabilityLabel == uiText(R.string.profile_health_available) && !state.isHealthConnectSyncing,
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
                    stringResource(R.string.profile_device_sync),
                    style = MusFitTheme.typography.labelSmall.copy(fontWeight = FontWeight.W800, letterSpacing = 0.8.sp),
                    color = accent.onContainer,
                )
                HeroChip(text = availabilityPillLabel(state.availabilityLabel).asString(), accent = accent)
            }
            if (total > 0) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        stringResource(
                            R.string.profile_permissions_granted_count,
                            LocalizedFormatter.integer(granted.toLong(), locale = LocalConfiguration.current.locales[0]),
                            LocalizedFormatter.integer(total.toLong(), locale = LocalConfiguration.current.locales[0]),
                        ),
                        style = MusFitTheme.typography.displayMedium.copy(fontSize = 44.sp, lineHeight = 44.sp),
                        color = accent.onContainer,
                        maxLines = 1,
                    )
                    Text(
                        stringResource(R.string.profile_permissions_granted),
                        style = MusFitTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        fontWeight = FontWeight.Medium,
                        color = accent.onContainer,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
                    )
                }
                PermissionSegments(granted = granted, total = total, accent = accent)
            } else {
                Text(
                    stringResource(R.string.profile_refresh_permissions),
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
                        total > 0 && missing == 0 -> stringResource(R.string.profile_all_permissions_granted)

                        total > 0 -> pluralStringResource(
                            R.plurals.profile_permissions_more_to_grant,
                            missing,
                            LocalizedFormatter.integer(missing.toLong(), locale = LocalConfiguration.current.locales[0]),
                        )

                        else -> ""
                    },
                    style = MusFitTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = accent.onContainerVariant,
                    modifier = Modifier.weight(1f),
                )
                if (state.canRequestPermissions) {
                    HeroActionPill(
                        text = if (missing > 0) {
                            pluralStringResource(
                                R.plurals.profile_grant_more,
                                missing,
                                LocalizedFormatter.integer(missing.toLong(), locale = LocalConfiguration.current.locales[0]),
                            )
                        } else {
                            stringResource(R.string.profile_review)
                        },
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
            label = stringResource(R.string.profile_all_step_sources),
            detail = stringResource(R.string.profile_health_combined_total),
            selected = selectedPackage == null,
            enabled = enabled,
            accent = accent,
            shape = groupedShape(0, rowCount),
            onClick = { onSelect(null) },
        )
        sources.forEachIndexed { index, source ->
            StepSourceRow(
                label = source.label,
                detail = stringResource(
                    R.string.profile_steps_today,
                    LocalizedFormatter.integer(source.steps, locale = LocalConfiguration.current.locales[0]),
                ),
                selected = selectedPackage == source.packageName,
                enabled = enabled,
                accent = accent,
                shape = groupedShape(index + 1, rowCount),
                onClick = { onSelect(source.packageName) },
            )
        }
        if (sources.isEmpty()) {
            Text(
                stringResource(R.string.profile_no_step_sources),
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
    detail: String,
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

private fun availabilityPillLabel(availabilityLabel: UiText): UiText = when (availabilityLabel) {
    uiText(R.string.profile_health_install_required) -> uiText(R.string.profile_needs_app)
    else -> availabilityLabel
}
