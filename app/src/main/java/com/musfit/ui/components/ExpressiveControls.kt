@file:OptIn(ExperimentalMaterial3Api::class)

package com.musfit.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musfit.ui.theme.MusFitMotion
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.NeutralOutline
import com.musfit.ui.theme.NeutralOutlineDark
import kotlinx.coroutines.delay

// Shared M3E control vocabulary for inner screens (Turn 9). All colors via
// MusFitTheme; motion via MusFitMotion.

/** Inner-page titles: 26–28/800 (tab roots keep the 34/800 headlineMedium). */
val InnerScreenTitleStyle: TextStyle
    @Composable get() = MusFitTheme.typography.headlineMedium.copy(fontSize = 27.sp, lineHeight = 30.sp)

/** Hero numerals on inner screens (meal-detail kcal, goal calorie target). */
val HeroNumberMediumStyle: TextStyle
    @Composable get() = MusFitTheme.typography.displayMedium.copy(fontSize = 40.sp, lineHeight = 42.sp)

/** Filled primary pill (h56, radius 99) with press scale-down spring. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = MusFitTheme.colors.brand,
    contentColor: Color = MusFitTheme.colors.onBrand,
    height: Dp = 56.dp,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = MusFitMotion.spatialFast(),
        label = "pillPressScale",
    )
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(99.dp),
        color = if (enabled) containerColor else containerColor.copy(alpha = 0.5f),
        contentColor = contentColor,
        interactionSource = interactionSource,
        modifier = modifier
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 22.dp),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MusFitTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.W800),
                maxLines = 1,
            )
        }
    }
}

/** 56dp tonal icon square (radius 20) — bottom-bar secondary actions. */
@Composable
fun TonalIconSquare(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    cornerRadius: Dp = 20.dp,
    containerColor: Color = MusFitTheme.colors.surfaceVariant,
    contentColor: Color = MusFitTheme.colors.onSurface,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(22.dp))
        }
    }
}

/** Inner-screen header: tonal back circle + 27/800 title + trailing slot. */
@Composable
fun InnerScreenHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        TonalHeaderIconButton(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Back",
            onClick = onBack,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = InnerScreenTitleStyle, color = MusFitTheme.colors.onSurface)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MusFitTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
        }
        trailing()
    }
}

/** Section overline ("PROGRAMS") — caller provides uppercase text. */
@Composable
fun SectionOverline(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MusFitTheme.typography.labelSmall.copy(fontWeight = FontWeight.W800, letterSpacing = 0.8.sp),
        color = MusFitTheme.colors.onSurfaceFaint,
        modifier = modifier.padding(horizontal = 4.dp),
    )
}

/**
 * Circular stepper (+/−) with hold-to-repeat: after 400ms of press, [onClick]
 * repeats every 120ms accelerating to 40ms. [filled] = brand fill (the "+").
 */
@Composable
fun StepperCircleButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    filled: Boolean = false,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val currentOnClick by rememberUpdatedState(onClick)
    // When the hold-repeat fires, swallow the click Surface emits on release.
    var consumedByRepeat by remember { mutableStateOf(false) }
    LaunchedEffect(pressed, enabled) {
        if (pressed && enabled) {
            // Fresh gesture: clear any stale flag left by a cancelled press so
            // the next discrete tap is not swallowed.
            consumedByRepeat = false
            delay(400)
            var interval = 120L
            while (true) {
                consumedByRepeat = true
                currentOnClick()
                delay(interval)
                interval = (interval - 10L).coerceAtLeast(40L)
            }
        }
    }
    Surface(
        onClick = {
            if (consumedByRepeat) consumedByRepeat = false else onClick()
        },
        enabled = enabled,
        shape = CircleShape,
        color = if (filled) MusFitTheme.colors.brand else MusFitTheme.colors.surfaceVariant,
        contentColor = if (filled) MusFitTheme.colors.onBrand else MusFitTheme.colors.onSurface,
        interactionSource = interactionSource,
        modifier = modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(22.dp))
        }
    }
}

/** 36×4 sheet drag handle on the cream sheet ground. */
@Composable
fun SheetDragHandle(modifier: Modifier = Modifier) {
    val color = if (isSystemInDarkTheme()) NeutralOutlineDark else NeutralOutline
    Box(
        modifier = modifier
            .size(width = 36.dp, height = 4.dp)
            .background(color = color, shape = RoundedCornerShape(99.dp)),
    )
}

/**
 * Horizontal group where only the TOP outer corners get the large radius
 * (9b mode row, 9d stat tiles): bottoms stay [inner] on every cell.
 */
fun topGroupShape(index: Int, count: Int, outer: Dp = 18.dp, inner: Dp = 8.dp): RoundedCornerShape =
    RoundedCornerShape(
        topStart = if (index == 0) outer else inner,
        topEnd = if (index == count - 1) outer else inner,
        bottomStart = inner,
        bottomEnd = inner,
    )

/** Dense 13sp label/value row with a bottom hairline (nutrition detail lists). */
@Composable
fun HairlineDetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 11.dp),
        ) {
            Text(
                text = label,
                style = MusFitTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = MusFitTheme.colors.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = value,
                style = MusFitTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.W700),
                color = MusFitTheme.colors.onSurface,
            )
        }
        if (showDivider) {
            HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
        }
    }
}
