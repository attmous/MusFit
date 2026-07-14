package com.musfit.integrations.healthconnect

import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.LocalDate

internal fun workoutExportFingerprint(
    session: WorkoutSessionEntity,
    sets: List<WorkoutSetEntity>,
): String = fingerprint {
    component(session.id)
    component(session.title)
    component(session.startedAtEpochMillis)
    component(session.endedAtEpochMillis)
    component(session.notes)
    sets.sortedWith(compareBy<WorkoutSetEntity> { it.sortOrder }.thenBy { it.id }).forEach { set ->
        component(set.id)
        component(set.exerciseId)
        component(set.sortOrder)
        component(set.setType)
        component(set.reps)
        component(set.weightKg?.toBits())
        component(set.durationSeconds)
        component(set.distanceMeters?.toBits())
        component(set.rpe?.toBits())
        component(set.notes)
        component(set.completed)
        component(set.supersetGroupId)
        component(set.restSeconds)
    }
}

internal fun nutritionExportFingerprint(date: LocalDate, meal: HealthConnectFoodMealExport): String = fingerprint {
    component(date)
    component(meal.localMealId)
    component(meal.mealType)
    component(meal.name)
    component(meal.caloriesKcal.toBits())
    component(meal.proteinGrams.toBits())
    component(meal.carbsGrams.toBits())
    component(meal.fatGrams.toBits())
    component(meal.fiberGrams.toBits())
    component(meal.sugarGrams.toBits())
    component(meal.saturatedFatGrams.toBits())
    component(meal.sodiumMilligrams.toBits())
    component(meal.potassiumMilligrams.toBits())
    component(meal.calciumMilligrams.toBits())
    component(meal.ironMilligrams.toBits())
    component(meal.vitaminDMicrograms.toBits())
    component(meal.vitaminCMilligrams.toBits())
    component(meal.magnesiumMilligrams.toBits())
}

internal fun hydrationExportFingerprint(date: LocalDate, milliliters: Double): String = fingerprint {
    component(date)
    component(milliliters.toBits())
}

private inline fun fingerprint(block: FingerprintBuilder.() -> Unit): String = FingerprintBuilder().apply(block).digest()

private class FingerprintBuilder {
    private val canonical = StringBuilder()

    fun component(value: Any?) {
        val encoded = value?.toString() ?: "<null>"
        canonical.append(encoded.length).append(':').append(encoded)
    }

    fun digest(): String = MessageDigest.getInstance("SHA-256")
        .digest(canonical.toString().toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
}
