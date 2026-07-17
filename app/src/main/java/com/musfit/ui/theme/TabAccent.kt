package com.musfit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.musfit.ui.AppDestination

@Composable
fun tabAccentFor(destination: AppDestination): TabAccent = if (isSystemInDarkTheme()) tabAccentForDark(destination) else tabAccentForLight(destination)

// The M3 Expressive quad: coral (Today) → green (Food) → indigo (Training) →
// teal (Profile). Amber stays reserved for semantic warning tones.
internal fun tabAccentForLight(destination: AppDestination): TabAccent = when (destination) {
    AppDestination.Today -> TabAccent(
        color = Coral,
        onColor = CardWhite,
        container = CoralContainer,
        onContainer = CoralInkStrong,
        onContainerVariant = CoralInk,
        track = CoralTrack,
        badge = CoralBadge,
        chip = CoralChip,
    )

    AppDestination.Food -> TabAccent(
        color = Green,
        onColor = CardWhite,
        container = GreenContainer,
        onContainer = GreenInk,
        onContainerVariant = GreenBody,
        track = GreenTrack,
    )

    AppDestination.Training -> TabAccent(
        color = Indigo,
        onColor = CardWhite,
        container = IndigoContainer,
        onContainer = IndigoInk,
        track = IndigoTrack,
    )

    AppDestination.Profile -> TabAccent(
        color = Teal,
        onColor = CardWhite,
        container = TealContainer,
        onContainer = TealInk,
        onContainerVariant = TealBody,
        track = TealTrack,
    )
}

internal fun tabAccentForDark(destination: AppDestination): TabAccent = when (destination) {
    AppDestination.Today -> TabAccent(
        color = CoralBright,
        onColor = CoralOnDark,
        container = CoralContainerDark,
        onContainer = CoralInkDark,
        onContainerVariant = CoralBright,
        track = CoralTrackDark,
        badge = CoralBadgeDark,
        chip = CoralChipDark,
    )

    AppDestination.Food -> TabAccent(
        color = GreenBright,
        onColor = GreenOnDark,
        container = GreenContainerDark,
        onContainer = GreenInkDark,
        onContainerVariant = GreenBright,
        track = GreenTrackDark,
    )

    AppDestination.Training -> TabAccent(
        color = IndigoBright,
        onColor = IndigoOnDark,
        container = IndigoContainerDark,
        onContainer = IndigoInkDark,
        onContainerVariant = IndigoBright,
        track = IndigoTrackDark,
    )

    AppDestination.Profile -> TabAccent(
        color = TealBright,
        onColor = TealOnDark,
        container = TealContainerDark,
        onContainer = TealInkDark,
        onContainerVariant = TealBright,
        track = TealTrackDark,
    )
}
