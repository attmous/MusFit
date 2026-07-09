package com.musfit.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * M3 Expressive radii: 8 group-inner · 12 unselected segment · 16 squircle
 * badge/icon-button · 24 group-outer card · 28 tonal hero. Pills/chips/buttons
 * stay fully rounded.
 */
val MusFitShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
