package com.musfit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class TabAccentRole {
    Today,
    Food,
    Training,
    Profile,
}

@Composable
fun tabAccentFor(role: TabAccentRole): TabAccent = if (isSystemInDarkTheme()) tabAccentForDark(role) else tabAccentForLight(role)

internal fun tabAccentForLight(role: TabAccentRole): TabAccent = when (role) {
    TabAccentRole.Today -> TabAccent(
        color = Coral,
        onColor = CardWhite,
        container = CoralContainer,
        onContainer = CoralInkStrong,
        onContainerVariant = CoralInk,
        track = CoralTrack,
        badge = CoralBadge,
        chip = CoralChip,
    )

    TabAccentRole.Food -> TabAccent(
        color = Green,
        onColor = CardWhite,
        container = GreenContainer,
        onContainer = GreenInk,
        onContainerVariant = GreenBody,
        track = GreenTrack,
    )

    TabAccentRole.Training -> TabAccent(
        color = Indigo,
        onColor = CardWhite,
        container = IndigoContainer,
        onContainer = IndigoInk,
        track = IndigoTrack,
    )

    TabAccentRole.Profile -> TabAccent(
        color = Teal,
        onColor = CardWhite,
        container = TealContainer,
        onContainer = TealInk,
        onContainerVariant = TealBody,
        track = TealTrack,
    )
}

internal fun tabAccentForDark(role: TabAccentRole): TabAccent = when (role) {
    TabAccentRole.Today -> TabAccent(
        color = CoralBright,
        onColor = CoralOnDark,
        container = CoralContainerDark,
        onContainer = CoralInkDark,
        onContainerVariant = CoralBright,
        track = CoralTrackDark,
        badge = CoralBadgeDark,
        chip = CoralChipDark,
    )

    TabAccentRole.Food -> TabAccent(
        color = GreenBright,
        onColor = GreenOnDark,
        container = GreenContainerDark,
        onContainer = GreenInkDark,
        onContainerVariant = GreenBright,
        track = GreenTrackDark,
    )

    TabAccentRole.Training -> TabAccent(
        color = IndigoBright,
        onColor = IndigoOnDark,
        container = IndigoContainerDark,
        onContainer = IndigoInkDark,
        onContainerVariant = IndigoBright,
        track = IndigoTrackDark,
    )

    TabAccentRole.Profile -> TabAccent(
        color = TealBright,
        onColor = TealOnDark,
        container = TealContainerDark,
        onContainer = TealInkDark,
        onContainerVariant = TealBright,
        track = TealTrackDark,
    )
}

/**
 * A miniapp's signature accent, layered on the shared warm neutrals. Every role
 * the M3 Expressive tab layouts need:
 *  - [color]/[onColor]: filled buttons, progress waves, gauge arcs
 *  - [container]/[onContainer]: the tonal hero card and its strong display ink
 *  - [onContainerVariant]: quieter body/sub text on the container
 *  - [track]: the un-filled remainder of wavy/linear progress in this accent
 *  - [badge]: leading icon-badge tint (sunny/circle/squircle shapes)
 *  - [chip]: the very light stat-chip fill used inside hero cards
 */
data class TabAccent(
    val color: Color,
    val onColor: Color,
    val container: Color,
    val onContainer: Color,
    val onContainerVariant: Color = onContainer,
    val track: Color = container,
    val badge: Color = container,
    val chip: Color = container,
)
