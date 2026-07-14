package com.musfit.integrations.healthconnect

import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class HealthConnectRecordMapperTest {
    @Test
    fun toExerciseSessionRecord_mapsStrengthWorkoutTimeRangeWithLocalOffsets() {
        val zoneId = ZoneId.of("Europe/Berlin")
        val session = workoutSession(
            startedAtEpochMillis = 1_700_000_000_000,
            endedAtEpochMillis = 1_700_003_600_000,
        )

        val record = HealthConnectRecordMapper.toExerciseSessionRecord(session, completedSets(), zoneId)

        assertEquals(session.startedAtEpochMillis, record.startTime.toEpochMilli())
        assertEquals(session.endedAtEpochMillis, record.endTime.toEpochMilli())
        assertEquals(ZoneOffset.ofHours(1), record.startZoneOffset)
        assertEquals(ZoneOffset.ofHours(1), record.endZoneOffset)
        assertEquals("MusFit workout: 1 completed sets", record.title)
    }

    @Test
    fun records_useStableAccountScopedClientIdentityAndExplicitVersion() {
        val session = workoutSession(
            startedAtEpochMillis = 1_700_000_000_000,
            endedAtEpochMillis = 1_700_003_600_000,
        )
        val identity = HealthConnectRecordIdentity.forWorkout(
            accountId = "account-a",
            sessionId = session.id,
            version = 7,
        )

        val first = HealthConnectRecordMapper.toExerciseSessionRecord(session, completedSets(), identity = identity)
        val retry = HealthConnectRecordMapper.toExerciseSessionRecord(session, completedSets(), identity = identity)
        val otherAccount = HealthConnectRecordMapper.toExerciseSessionRecord(
            session.copy(accountId = "account-b"),
            completedSets().map { it.copy(accountId = "account-b") },
            identity = HealthConnectRecordIdentity.forWorkout("account-b", session.id, version = 1),
        )

        assertEquals(first.metadata.clientRecordId, retry.metadata.clientRecordId)
        assertEquals(7, first.metadata.clientRecordVersion)
        assertNotEquals(first.metadata.clientRecordId, otherAccount.metadata.clientRecordId)
    }

    @Test
    fun nutritionIdentity_usesImmutableMealId_notLossyDisplayLabel() {
        val date = LocalDate.of(2026, 7, 14)
        val firstMeal = foodMeal(localMealId = "meal/a", mealType = "Post Run")
        val collidingLabelMeal = foodMeal(localMealId = "meal:b", mealType = "post-run")

        val first = HealthConnectRecordMapper.toNutritionRecord(
            date = date,
            meal = firstMeal,
            identity = HealthConnectRecordIdentity.forNutrition("account-a", firstMeal.localMealId, version = 3),
        )
        val second = HealthConnectRecordMapper.toNutritionRecord(
            date = date,
            meal = collidingLabelMeal,
            identity = HealthConnectRecordIdentity.forNutrition("account-a", collidingLabelMeal.localMealId, version = 1),
        )
        val otherAccount = HealthConnectRecordMapper.toNutritionRecord(
            date = date,
            meal = firstMeal,
            identity = HealthConnectRecordIdentity.forNutrition("account-b", firstMeal.localMealId, version = 1),
        )

        assertNotEquals(first.metadata.clientRecordId, second.metadata.clientRecordId)
        assertNotEquals(first.metadata.clientRecordId, otherAccount.metadata.clientRecordId)
        assertEquals(3, first.metadata.clientRecordVersion)
    }

    @Test
    fun toExerciseSessionRecord_usesMinimalPositiveDurationWhenEndTimeMissing_forSdkConstraint() {
        val zoneId = ZoneId.of("Europe/Berlin")
        val session = workoutSession(
            startedAtEpochMillis = 1_700_000_000_000,
            endedAtEpochMillis = null,
        )

        val record = HealthConnectRecordMapper.toExerciseSessionRecord(session, completedSets(), zoneId)

        assertEquals(session.startedAtEpochMillis, record.startTime.toEpochMilli())
        assertEquals(session.startedAtEpochMillis + 1, record.endTime.toEpochMilli())
        assertEquals(ZoneOffset.ofHours(1), record.startZoneOffset)
        assertEquals(ZoneOffset.ofHours(1), record.endZoneOffset)
    }

    private fun completedSets(): List<WorkoutSetEntity> = listOf(
        WorkoutSetEntity(
            accountId = "local-default",
            id = "set-1",
            sessionId = "session-1",
            exerciseId = "bench",
            sortOrder = 0,
            reps = 5,
            weightKg = 100.0,
            durationSeconds = null,
            distanceMeters = null,
            rpe = 8.0,
            notes = null,
            completed = true,
        ),
    )

    private fun foodMeal(localMealId: String, mealType: String) = HealthConnectFoodMealExport(
        localMealId = localMealId,
        mealType = mealType,
        name = mealType,
        caloriesKcal = 500.0,
        proteinGrams = 30.0,
        carbsGrams = 50.0,
        fatGrams = 20.0,
    )

    private fun workoutSession(
        startedAtEpochMillis: Long,
        endedAtEpochMillis: Long?,
    ) = WorkoutSessionEntity(
        accountId = "local-default",
        id = "session-1",
        routineId = null,
        startedAtEpochMillis = startedAtEpochMillis,
        endedAtEpochMillis = endedAtEpochMillis,
        notes = "Push day",
        healthConnectRecordId = null,
        healthConnectLastExportedAtEpochMillis = null,
    )
}
