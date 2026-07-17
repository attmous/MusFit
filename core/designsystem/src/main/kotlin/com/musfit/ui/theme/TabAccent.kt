package com.musfit.ui.theme

import androidx.compose.ui.graphics.Color

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
