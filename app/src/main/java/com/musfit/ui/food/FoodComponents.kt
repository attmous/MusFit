package com.musfit.ui.food

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.DinnerDining
import androidx.compose.material.icons.outlined.LunchDining
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.musfit.ui.theme.MusFitTheme
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(MusFitTheme.shapes.small)
            .background(color.copy(alpha = 0.18f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(8.dp)
                .clip(MusFitTheme.shapes.small)
                .background(color),
        )
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MusFitTheme.colors.onSurfaceVariant,
    )
}

internal fun mealTypeIcon(id: String, title: String): ImageVector {
    val key = "$id $title".lowercase()
    return when {
        "breakfast" in key -> Icons.Outlined.BakeryDining
        "lunch" in key -> Icons.Outlined.LunchDining
        "dinner" in key -> Icons.Outlined.DinnerDining
        "snack" in key -> Icons.Outlined.Cookie
        else -> Icons.Outlined.Restaurant
    }
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
            text = text.firstOrNull()?.uppercase().orEmpty(),
            style = MaterialTheme.typography.titleMedium,
            color = MusFitTheme.colors.brandInk,
            fontWeight = FontWeight.Bold,
        )
    }
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

internal fun Double.formatNutritionDisplay(): String {
    val tenths = (this * 10.0).roundToInt()
    val whole = tenths / 10
    val decimal = tenths % 10
    return if (decimal == 0) {
        whole.toString()
    } else {
        "$whole.$decimal"
    }
}

internal fun Double.formatMicronutrientDisplay(): String =
    if (this < 10.0 && this != roundToInt().toDouble()) {
        formatNutritionDisplay()
    } else {
        roundToInt().toString()
    }
