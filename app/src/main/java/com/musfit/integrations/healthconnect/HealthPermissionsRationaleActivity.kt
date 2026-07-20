package com.musfit.integrations.healthconnect

import android.content.res.Resources
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.musfit.R
import com.musfit.configureMusFitEdgeToEdge
import com.musfit.ui.theme.MusFitTheme

class HealthPermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureMusFitEdgeToEdge()
        setContent {
            MusFitTheme {
                HealthPermissionsRationaleScreen()
            }
        }
    }
}

@Composable
internal fun HealthPermissionsRationaleScreen(
    items: List<HealthPermissionRationaleItem> = HealthPermissionInventory.rationaleItems,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MusFitTheme.colors.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.health_rationale_title),
            style = MusFitTheme.typography.headlineSmall,
            color = MusFitTheme.colors.onSurface,
        )
        Text(
            text = stringResource(R.string.health_rationale_intro),
            style = MusFitTheme.typography.bodyMedium,
            color = MusFitTheme.colors.onSurfaceVariant,
        )
        HealthPermissionRationaleSection(
            title = stringResource(R.string.health_rationale_read),
            items = items.filter { it.access == HealthPermissionAccess.Read },
        )
        HealthPermissionRationaleSection(
            title = stringResource(R.string.health_rationale_write),
            items = items.filter { it.access == HealthPermissionAccess.Write },
        )
    }
}

@Composable
private fun HealthPermissionRationaleSection(
    title: String,
    items: List<HealthPermissionRationaleItem>,
) {
    val resources = LocalResources.current
    Text(
        text = title,
        style = MusFitTheme.typography.titleMedium,
        color = MusFitTheme.colors.onSurface,
    )
    items.forEach { item ->
        val rationale = item.resolveRationale(resources)
        Text(
            text = stringResource(R.string.health_rationale_item, rationale.label, rationale.purpose),
            style = MusFitTheme.typography.bodyMedium,
            color = MusFitTheme.colors.onSurfaceVariant,
        )
    }
}

internal data class HealthPermissionRationaleText(
    val label: String,
    val purpose: String,
)

internal fun HealthPermissionRationaleItem.resolveRationale(resources: Resources): HealthPermissionRationaleText {
    val resourceIds = rationale.resourceIds()
    return HealthPermissionRationaleText(
        label = resources.getString(resourceIds.label),
        purpose = resources.getString(resourceIds.purpose),
    )
}

private data class HealthPermissionRationaleResourceIds(
    @param:StringRes val label: Int,
    @param:StringRes val purpose: Int,
)

private fun HealthPermissionRationale.resourceIds(): HealthPermissionRationaleResourceIds = when (this) {
    HealthPermissionRationale.Steps -> rationaleResources(R.string.health_steps_label, R.string.health_steps_purpose)

    HealthPermissionRationale.ActiveCalories ->
        rationaleResources(R.string.health_active_calories_label, R.string.health_active_calories_purpose)

    HealthPermissionRationale.TotalCalories ->
        rationaleResources(R.string.health_total_calories_label, R.string.health_total_calories_purpose)

    HealthPermissionRationale.Distance -> rationaleResources(R.string.health_distance_label, R.string.health_distance_purpose)

    HealthPermissionRationale.Sleep -> rationaleResources(R.string.health_sleep_label, R.string.health_sleep_purpose)

    HealthPermissionRationale.ExerciseSessions ->
        rationaleResources(R.string.health_exercise_sessions_label, R.string.health_exercise_sessions_purpose)

    HealthPermissionRationale.Weight -> rationaleResources(R.string.health_weight_label, R.string.health_weight_purpose)

    HealthPermissionRationale.BodyFat -> rationaleResources(R.string.health_body_fat_label, R.string.health_body_fat_purpose)

    HealthPermissionRationale.RestingHeartRate ->
        rationaleResources(R.string.health_resting_heart_rate_label, R.string.health_resting_heart_rate_purpose)

    HealthPermissionRationale.HeartRateVariability -> rationaleResources(R.string.health_hrv_label, R.string.health_hrv_purpose)

    HealthPermissionRationale.Workouts -> rationaleResources(R.string.health_workouts_label, R.string.health_workouts_purpose)

    HealthPermissionRationale.MealsAndNutrition ->
        rationaleResources(R.string.health_meals_nutrition_label, R.string.health_meals_nutrition_purpose)

    HealthPermissionRationale.Water -> rationaleResources(R.string.health_water_label, R.string.health_water_purpose)
}

private fun rationaleResources(
    @StringRes label: Int,
    @StringRes purpose: Int,
) = HealthPermissionRationaleResourceIds(label = label, purpose = purpose)
