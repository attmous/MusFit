package com.musfit.integrations.healthconnect

import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.grams
import androidx.health.connect.client.units.kilocalories
import androidx.health.connect.client.units.micrograms
import androidx.health.connect.client.units.milligrams
import androidx.health.connect.client.units.milliliters
import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

object HealthConnectRecordMapper {
    fun toExerciseSessionRecord(
        session: WorkoutSessionEntity,
        sets: List<WorkoutSetEntity>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        identity: HealthConnectRecordIdentity = HealthConnectRecordIdentity.forWorkout(
            accountId = session.accountId,
            sessionId = session.id,
            version = 1,
        ),
    ): ExerciseSessionRecord {
        val startInstant = Instant.ofEpochMilli(session.startedAtEpochMillis)
        val endInstant = Instant.ofEpochMilli(
            session.endedAtEpochMillis ?: (session.startedAtEpochMillis + 1),
        )
        val completedSetCount = sets.count { it.completed }
        return ExerciseSessionRecord(
            startTime = startInstant,
            startZoneOffset = zoneId.rules.getOffset(startInstant),
            endTime = endInstant,
            endZoneOffset = zoneId.rules.getOffset(endInstant),
            metadata = Metadata.manualEntry(identity.clientRecordId, identity.clientRecordVersion),
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
            title = "MusFit workout: $completedSetCount completed sets",
            notes = session.notes,
        )
    }

    fun toNutritionRecord(
        date: LocalDate,
        meal: HealthConnectFoodMealExport,
        zoneId: ZoneId = ZoneId.systemDefault(),
        identity: HealthConnectRecordIdentity = HealthConnectRecordIdentity.forNutrition(
            accountId = meal.accountId,
            mealId = meal.localMealId,
            version = 1,
        ),
    ): NutritionRecord {
        val startInstant = date
            .atTime(meal.mealStartTime())
            .atZone(zoneId)
            .toInstant()
        val endInstant = startInstant.plusSeconds(15 * 60)
        return NutritionRecord(
            startTime = startInstant,
            startZoneOffset = zoneId.rules.getOffset(startInstant),
            endTime = endInstant,
            endZoneOffset = zoneId.rules.getOffset(endInstant),
            metadata = Metadata.manualEntry(
                identity.clientRecordId,
                identity.clientRecordVersion,
            ),
            energy = meal.caloriesKcal.coerceAtLeast(0.0).kilocalories,
            protein = meal.proteinGrams.positiveOrNull()?.grams,
            totalCarbohydrate = meal.carbsGrams.positiveOrNull()?.grams,
            totalFat = meal.fatGrams.positiveOrNull()?.grams,
            dietaryFiber = meal.fiberGrams.positiveOrNull()?.grams,
            sugar = meal.sugarGrams.positiveOrNull()?.grams,
            saturatedFat = meal.saturatedFatGrams.positiveOrNull()?.grams,
            sodium = meal.sodiumMilligrams.positiveOrNull()?.milligrams,
            potassium = meal.potassiumMilligrams.positiveOrNull()?.milligrams,
            calcium = meal.calciumMilligrams.positiveOrNull()?.milligrams,
            iron = meal.ironMilligrams.positiveOrNull()?.milligrams,
            vitaminD = meal.vitaminDMicrograms.positiveOrNull()?.micrograms,
            vitaminC = meal.vitaminCMilligrams.positiveOrNull()?.milligrams,
            magnesium = meal.magnesiumMilligrams.positiveOrNull()?.milligrams,
            name = meal.name,
            mealType = meal.mealType.toHealthConnectMealType(),
        )
    }

    fun toHydrationRecord(
        date: LocalDate,
        milliliters: Double,
        zoneId: ZoneId = ZoneId.systemDefault(),
        accountId: String = "local-default",
        identity: HealthConnectRecordIdentity = HealthConnectRecordIdentity.forHydration(accountId, date, version = 1),
    ): HydrationRecord {
        val startInstant = date.atTime(LocalTime.NOON).atZone(zoneId).toInstant()
        val endInstant = startInstant.plusSeconds(60)
        return HydrationRecord(
            startTime = startInstant,
            startZoneOffset = zoneId.rules.getOffset(startInstant),
            endTime = endInstant,
            endZoneOffset = zoneId.rules.getOffset(endInstant),
            volume = milliliters.coerceAtLeast(0.0).milliliters,
            metadata = Metadata.manualEntry(
                identity.clientRecordId,
                identity.clientRecordVersion,
            ),
        )
    }
}

private fun HealthConnectFoodMealExport.mealStartTime(): LocalTime = when (mealType.lowercase()) {
    "breakfast" -> LocalTime.of(7, 30)
    "lunch" -> LocalTime.of(12, 30)
    "dinner" -> LocalTime.of(18, 30)
    "snacks", "snack" -> LocalTime.of(15, 30)
    else -> LocalTime.NOON
}

private fun String.toHealthConnectMealType(): Int = when (lowercase()) {
    "breakfast" -> MealType.MEAL_TYPE_BREAKFAST
    "lunch" -> MealType.MEAL_TYPE_LUNCH
    "dinner" -> MealType.MEAL_TYPE_DINNER
    "snacks", "snack" -> MealType.MEAL_TYPE_SNACK
    else -> MealType.MEAL_TYPE_UNKNOWN
}

private fun Double.positiveOrNull(): Double? = takeIf { value -> value.isFinite() && value > 0.0 }
