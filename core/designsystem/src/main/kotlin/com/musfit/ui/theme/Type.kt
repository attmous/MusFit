package com.musfit.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.musfit.core.designsystem.R

/** Roboto Flex (bundled static instances of the variable font, 400–800). */
private val RobotoFlex = FontFamily(
    Font(R.font.roboto_flex_regular, FontWeight.Normal),
    Font(R.font.roboto_flex_medium, FontWeight.Medium),
    Font(R.font.roboto_flex_semibold, FontWeight.SemiBold),
    Font(R.font.roboto_flex_bold, FontWeight.Bold),
    Font(R.font.roboto_flex_extrabold, FontWeight.ExtraBold),
)

private fun TextStyle.flex(weight: FontWeight) = copy(fontFamily = RobotoFlex, fontWeight = weight)

/**
 * Material 3 Expressive type scale. The signature move is weight contrast:
 * w800 display/headline/title numerals and labels against w400 body of the same
 * family. Displays are tight (negative tracking, line-height ≈ 1); body stays
 * relaxed and readable.
 */
val MusFitTypography: Typography = Typography().let { base ->
    base.copy(
        // Hero displays: 54 (Today kcal / Profile weight), 44 (Food gauge).
        displayLarge = base.displayLarge.flex(FontWeight.ExtraBold)
            .copy(fontSize = 54.sp, lineHeight = 56.sp, letterSpacing = (-1.5).sp),
        displayMedium = base.displayMedium.flex(FontWeight.ExtraBold)
            .copy(fontSize = 44.sp, lineHeight = 46.sp, letterSpacing = (-1.2).sp),
        // Stat-cell display (Training "2 workouts" row, measurement values).
        displaySmall = base.displaySmall.flex(FontWeight.ExtraBold)
            .copy(fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.4).sp),
        // Page titles ("Today", "Food") — 34/w800, tight.
        headlineLarge = base.headlineLarge.flex(FontWeight.ExtraBold)
            .copy(fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.5).sp),
        headlineMedium = base.headlineMedium.flex(FontWeight.ExtraBold)
            .copy(fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.5).sp),
        // Hero titles inside tonal containers ("Full Body A").
        headlineSmall = base.headlineSmall.flex(FontWeight.ExtraBold)
            .copy(fontSize = 21.sp, lineHeight = 26.sp, letterSpacing = (-0.3).sp),
        // Card headlines (17/800), section labels (16/800), row titles (15.5/700).
        titleLarge = base.titleLarge.flex(FontWeight.ExtraBold)
            .copy(fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = 0.sp),
        titleMedium = base.titleMedium.flex(FontWeight.ExtraBold)
            .copy(fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.sp),
        titleSmall = base.titleSmall.flex(FontWeight.Bold)
            .copy(fontSize = 15.5.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
        // Body stays w400 — the quiet half of the weight contrast.
        bodyLarge = base.bodyLarge.flex(FontWeight.Normal).copy(fontSize = 15.sp, lineHeight = 21.sp),
        bodyMedium = base.bodyMedium.flex(FontWeight.Normal).copy(fontSize = 13.5.sp, lineHeight = 19.sp),
        bodySmall = base.bodySmall.flex(FontWeight.Normal).copy(fontSize = 12.5.sp, lineHeight = 17.sp),
        // Buttons 14/700, chips 12.5/700, overlines 11/700 (+0.8 tracking).
        labelLarge = base.labelLarge.flex(FontWeight.Bold).copy(fontSize = 14.sp, lineHeight = 20.sp),
        labelMedium = base.labelMedium.flex(FontWeight.Bold).copy(fontSize = 12.5.sp, lineHeight = 16.sp),
        labelSmall = base.labelSmall.flex(FontWeight.Bold)
            .copy(fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.8.sp),
    )
}
