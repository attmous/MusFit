package com.musfit.ui.food

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.musfit.ui.icons.filled.BakeryDining
import com.musfit.ui.icons.filled.Cookie
import com.musfit.ui.icons.filled.DinnerDining
import com.musfit.ui.icons.filled.LunchDining
import com.musfit.ui.icons.filled.Restaurant
import com.musfit.ui.text.LocalizedFormatter
import com.musfit.ui.text.UiText
import com.musfit.ui.theme.MusFitTheme
import java.util.Locale
import kotlin.math.roundToInt

// Shared leaf composables and small formatting helpers used across the Food
// screen and its panels. Kept `internal` so any file in the com.musfit.ui.food
// package can reuse them. Extracted from FoodScreen.kt (no behavior change).

@Composable
internal fun ProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    // Health-grade clean bar: thin, fully rounded, neutral track + one accent fill.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(MusFitTheme.colors.track),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(6.dp)
                .clip(CircleShape)
                .background(color),
        )
    }
}

@Composable
internal fun SectionTitle(text: String) {
    // Quiet 16/500 sentence-case section header — no ALL-CAPS shouting.
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MusFitTheme.colors.onSurface,
    )
}

// Filled glyphs per the Turn 8 (8b) diary badges: bakery / lunch / dinner dining.
internal fun mealTypeIcon(id: String, title: String): ImageVector {
    val key = "$id $title".lowercase(Locale.ROOT)
    return when {
        "breakfast" in key -> Icons.Filled.BakeryDining
        "lunch" in key -> Icons.Filled.LunchDining
        "dinner" in key -> Icons.Filled.DinnerDining
        "snack" in key -> Icons.Filled.Cookie
        else -> Icons.Filled.Restaurant
    }
}

/**
 * The meal the diary FAB defaults to, chosen by time of day. Matches the current
 * time bucket's keyword against each section's id+title (mirroring [mealTypeIcon]),
 * falling back to the first section, or null when there are no meals.
 */
internal fun defaultAddMealId(
    sections: List<FoodMealSectionUiState>,
    hour: Int,
): String? {
    if (sections.isEmpty()) return null
    val keyword = when (hour) {
        in 4..10 -> "breakfast"
        in 11..15 -> "lunch"
        in 16..21 -> "dinner"
        else -> "snack"
    }
    val match = sections.firstOrNull { keyword in "${it.id} ${it.title}".lowercase(Locale.ROOT) }
    return (match ?: sections.first()).id
}

@Composable
internal fun MealInitial(title: String) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(MusFitTheme.colors.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title.first().toString(),
            style = MaterialTheme.typography.titleLarge,
            color = MusFitTheme.colors.brand,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
internal fun FoodThumb(
    imageUrl: String?,
    fallback: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(MusFitTheme.shapes.medium)
            .background(MusFitTheme.colors.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl.isNullOrBlank()) {
            Icon(imageVector = fallback, contentDescription = null, tint = MusFitTheme.colors.brand)
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
internal fun FoodAvatar(
    text: String,
    color: Color = MusFitTheme.colors.surfaceVariant,
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.firstOrNull()?.uppercase(LocalConfiguration.current.locales[0]).orEmpty(),
            style = MaterialTheme.typography.titleMedium,
            color = MusFitTheme.colors.brandInk,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * The shared Food "secondary" button: an [OutlinedButton] with an emerald (brand)
 * border and brand content, so secondary actions pair with the brand-filled primary
 * buttons instead of reading as neutral grey. Use for all non-destructive secondary
 * actions across the Food surfaces; keep a plain [OutlinedButton] with error colors
 * for destructive (delete/remove) actions.
 */
@Composable
internal fun MusFitOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    val brand = MusFitTheme.colors.brand
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = brand),
        border = BorderStroke(1.dp, brand.copy(alpha = if (enabled) 0.45f else 0.15f)),
        contentPadding = contentPadding,
        content = content,
    )
}

@Composable
internal fun SmallNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

internal fun Double.formatNutritionDisplay(): String = LocalizedFormatter.number(
    value = this,
    maximumFractionDigits = 1,
    grouping = false,
)

internal fun Double.formatMicronutrientDisplay(): String = if (this < 10.0 && this != roundToInt().toDouble()) {
    formatNutritionDisplay()
} else {
    roundToInt().toString()
}

/**
 * The summarized diary row's sub line (Turn 8 8b), split so the UI can render
 * the kcal segment in emphasized green ink: "3 items · **545 kcal** · great".
 */
internal data class MealDiarySummary(
    val loggedCount: Int,
    val caloriesKcal: Int?,
    val qualifier: MealDiaryQualifier,
    val ratingLabel: UiText? = null,
)

internal enum class MealDiaryQualifier {
    None,
    SoFar,
    Planned,
    Empty,
    Rating,
}

internal fun FoodMealSectionUiState.mealDiarySummary(): MealDiarySummary {
    val loggedCount = entries.count { !it.isPlanned }
    val loggedCalories = caloriesKcal.roundToInt()
    val plannedCalories = plannedCaloriesKcal.roundToInt()
    return when {
        loggedCount > 0 -> MealDiarySummary(
            loggedCount = loggedCount,
            caloriesKcal = loggedCalories,
            qualifier = when {
                // Pending planned items outrank the rating: the meal is still open.
                plannedCalories > 0 -> MealDiaryQualifier.SoFar

                rating != null -> MealDiaryQualifier.Rating

                else -> MealDiaryQualifier.None
            },
            ratingLabel = rating?.label,
        )

        plannedCalories > 0 -> MealDiarySummary(
            loggedCount = 0,
            caloriesKcal = plannedCalories,
            qualifier = MealDiaryQualifier.Planned,
        )

        else -> MealDiarySummary(
            loggedCount = 0,
            caloriesKcal = null,
            qualifier = MealDiaryQualifier.Empty,
        )
    }
}
