package com.musfit.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import com.musfit.data.local.dao.AccountDao
import com.musfit.data.local.dao.FoodDao
import com.musfit.data.local.dao.HealthDao
import com.musfit.data.local.dao.ProfileDao
import com.musfit.data.local.dao.TrainingDao
import com.musfit.data.local.entity.ACTIVE_ACCOUNT_SESSION_KEY
import com.musfit.data.local.entity.AccountEntity
import com.musfit.data.local.entity.AccountSessionEntity
import com.musfit.data.local.entity.AppSettingsEntity
import com.musfit.data.local.entity.BarcodeProductEntity
import com.musfit.data.local.entity.BodyMetricEntity
import com.musfit.data.local.entity.ExerciseEntity
import com.musfit.data.local.entity.FoodEntity
import com.musfit.data.local.entity.FoodServingEntity
import com.musfit.data.local.entity.RoutineEntity
import com.musfit.data.local.entity.RoutineExerciseEntity
import com.musfit.data.local.entity.UserGoalsEntity
import com.musfit.data.local.entity.UserProfileEntity
import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MusFitDatabaseTest {
    private lateinit var database: MusFitDatabase
    private lateinit var accountDao: AccountDao
    private lateinit var foodDao: FoodDao
    private lateinit var trainingDao: TrainingDao
    private lateinit var healthDao: HealthDao
    private lateinit var profileDao: ProfileDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room.inMemoryDatabaseBuilder(context, MusFitDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        accountDao = database.accountDao()
        foodDao = database.foodDao()
        trainingDao = database.trainingDao()
        healthDao = database.healthDao()
        profileDao = database.profileDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun database_exposesExpectedDaosAndGeneratedImplementation() {
        assertTrue(RoomDatabase::class.java.isAssignableFrom(MusFitDatabase::class.java))
        assertEquals(AccountDao::class.java, MusFitDatabase::class.java.getMethod("accountDao").returnType)
        assertEquals(FoodDao::class.java, MusFitDatabase::class.java.getMethod("foodDao").returnType)
        assertEquals(TrainingDao::class.java, MusFitDatabase::class.java.getMethod("trainingDao").returnType)
        assertEquals(HealthDao::class.java, MusFitDatabase::class.java.getMethod("healthDao").returnType)
        assertTrue(
            RoomDatabase::class.java.isAssignableFrom(
                Class.forName("com.musfit.data.local.MusFitDatabase_Impl"),
            ),
        )
    }

    @Test
    fun accountDao_roundTripFromDatabaseSmokeTest() = runTest {
        val account = AccountEntity(
            id = "account-smoke",
            displayName = "Smoke",
            email = null,
            remoteUserId = null,
            authProvider = "local",
            avatarUrl = null,
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 2L,
        )
        val session = AccountSessionEntity(
            key = ACTIVE_ACCOUNT_SESSION_KEY,
            activeAccountId = account.id,
            updatedAtEpochMillis = 3L,
        )

        accountDao.upsertAccount(account)
        accountDao.upsertSession(session)

        assertEquals(account, accountDao.observeActiveAccount().first())
    }

    @Test
    fun daoRoundTrip_readsBackTask3PersistenceSurface() = runTest {
        val food =
            FoodEntity(
                id = "food-1",
                name = "Oats",
                brand = "MusFit",
                defaultServingGrams = 40.0,
                caloriesPer100g = 389.0,
                proteinPer100g = 16.9,
                carbsPer100g = 66.3,
                fatPer100g = 6.9,
                createdAtEpochMillis = 1_000L,
                updatedAtEpochMillis = 2_000L,
            )
        val serving =
            FoodServingEntity(
                id = "serving-1",
                foodId = food.id,
                label = "Bowl",
                grams = 40.0,
            )
        val barcodeProduct =
            BarcodeProductEntity(
                id = "barcode-1",
                barcode = "1234567890123",
                provider = "openfoodfacts",
                providerProductName = "Rolled Oats",
                providerBrand = "MusFit",
                rawJson = """{"name":"Rolled Oats"}""",
                quality = "verified",
                linkedFoodId = food.id,
                fetchedAtEpochMillis = 3_000L,
            )
        val exercise =
            ExerciseEntity(
                id = "exercise-1",
                name = "Back Squat",
                category = "strength",
                equipment = "barbell",
                targetMuscles = "quads,glutes",
                isCustom = false,
            )
        val routine =
            RoutineEntity(
                id = "routine-1",
                name = "Lower Body",
                notes = null,
                createdAtEpochMillis = 4_000L,
                updatedAtEpochMillis = 4_000L,
                isStarter = false,
            )
        val routineExercise =
            RoutineExerciseEntity(
                id = "routine-exercise-1",
                routineId = routine.id,
                exerciseId = exercise.id,
                sortOrder = 1,
                targetSets = 4,
                targetReps = "5",
            )
        val bodyMetric =
            BodyMetricEntity(
                id = "metric-1",
                type = "weight",
                value = 80.5,
                unit = "kg",
                measuredAtEpochMillis = 5_000L,
                source = "manual",
                externalId = null,
            )

        foodDao.upsertFood(food)
        foodDao.upsertServing(serving)
        foodDao.upsertBarcodeProduct(barcodeProduct)
        trainingDao.upsertExercise(exercise)
        trainingDao.upsertRoutine(routine)
        trainingDao.upsertRoutineExercise(routineExercise)
        healthDao.upsertBodyMetric(bodyMetric)

        assertEquals(listOf(serving), foodDao.observeServings(food.id).first())
        assertEquals(barcodeProduct, foodDao.getBarcodeProduct(barcodeProduct.barcode))
        assertEquals(listOf(barcodeProduct), foodDao.observeBarcodeProducts(food.id).first())
        assertEquals(listOf(routineExercise), trainingDao.observeRoutineExercises(routine.id).first())
        assertEquals(
            listOf(bodyMetric),
            healthDao.observeBodyMetrics(type = bodyMetric.type, fromEpochMillis = 4_000L).first(),
        )
    }

    @Test
    fun deletingLinkedFood_setsBarcodeProductLinkToNull() = runTest {
        val food =
            FoodEntity(
                id = "food-2",
                name = "Yogurt",
                brand = null,
                defaultServingGrams = 150.0,
                caloriesPer100g = 61.0,
                proteinPer100g = 3.5,
                carbsPer100g = 4.7,
                fatPer100g = 3.3,
                createdAtEpochMillis = 10_000L,
                updatedAtEpochMillis = 11_000L,
            )
        val barcodeProduct =
            BarcodeProductEntity(
                id = "barcode-2",
                barcode = "9876543210987",
                provider = "manual",
                providerProductName = "Greek Yogurt",
                providerBrand = null,
                rawJson = """{"name":"Greek Yogurt"}""",
                quality = "draft",
                linkedFoodId = food.id,
                fetchedAtEpochMillis = 12_000L,
            )

        foodDao.upsertFood(food)
        foodDao.upsertBarcodeProduct(barcodeProduct)

        foodDao.deleteFood(food)

        assertNull(foodDao.getBarcodeProduct(barcodeProduct.barcode)?.linkedFoodId)
    }

    @Test
    fun profileDao_roundTripsProfileAndSettings() = runTest {
        val profile = UserProfileEntity(
            id = "user",
            sex = "Male",
            birthDateEpochDay = 9_000L,
            heightCm = 182.0,
            activityLevel = "Moderate",
            goalType = "Lose",
            goalPaceKgPerWeek = 0.5,
            goalWeightKg = 78.0,
            updatedAtEpochMillis = 1_000L,
        )
        val settings = AppSettingsEntity(
            id = "app",
            unitSystem = "metric",
            themeMode = "system",
            updatedAtEpochMillis = 1_000L,
        )

        profileDao.upsertProfile(profile)
        profileDao.upsertSettings(settings)

        assertEquals(profile, profileDao.observeProfile("user").first())
        assertEquals(settings, profileDao.observeSettings("app").first())
        assertEquals(profile, profileDao.getProfile("user"))
        assertNull(profileDao.observeProfile("missing").first())
    }

    @Test
    fun workoutSet_supersetGroupId_roundTrips() = runTest {
        val exercise = ExerciseEntity(
            id = "ex-ss",
            name = "Bench Press",
            category = "strength",
            equipment = "barbell",
            targetMuscles = "chest",
            isCustom = false,
        )
        val session = WorkoutSessionEntity(
            id = "sess-ss",
            routineId = null,
            title = "Workout",
            status = "active",
            startedAtEpochMillis = 1_000L,
            endedAtEpochMillis = null,
            notes = null,
            healthConnectRecordId = null,
            healthConnectLastExportedAtEpochMillis = null,
        )
        trainingDao.upsertExercise(exercise)
        trainingDao.upsertWorkoutSession(session)
        trainingDao.upsertWorkoutSet(
            WorkoutSetEntity(
                id = "set-grouped",
                sessionId = session.id,
                exerciseId = exercise.id,
                sortOrder = 0,
                setType = "working",
                reps = 5,
                weightKg = 100.0,
                durationSeconds = null,
                distanceMeters = null,
                rpe = null,
                notes = null,
                completed = false,
                supersetGroupId = "grp-1",
            ),
        )
        trainingDao.upsertWorkoutSet(
            WorkoutSetEntity(
                id = "set-standalone",
                sessionId = session.id,
                exerciseId = exercise.id,
                sortOrder = 1,
                setType = "working",
                reps = 8,
                weightKg = 80.0,
                durationSeconds = null,
                distanceMeters = null,
                rpe = null,
                notes = null,
                completed = false,
            ),
        )

        assertEquals("grp-1", trainingDao.getWorkoutSet("set-grouped")?.supersetGroupId)
        assertNull(trainingDao.getWorkoutSet("set-standalone")?.supersetGroupId)
    }

    @Test
    fun supersetGroupWriters_tagAndClearAllSetsOfExercise() = runTest {
        val exercise = ExerciseEntity(
            id = "ex-w",
            name = "Barbell Row",
            category = "strength",
            equipment = "barbell",
            targetMuscles = "back",
            isCustom = false,
        )
        val session = WorkoutSessionEntity(
            id = "sess-w",
            routineId = null,
            title = "Workout",
            status = "active",
            startedAtEpochMillis = 1_000L,
            endedAtEpochMillis = null,
            notes = null,
            healthConnectRecordId = null,
            healthConnectLastExportedAtEpochMillis = null,
        )
        trainingDao.upsertExercise(exercise)
        trainingDao.upsertWorkoutSession(session)
        repeat(2) { i ->
            trainingDao.upsertWorkoutSet(
                WorkoutSetEntity(
                    id = "w$i",
                    sessionId = session.id,
                    exerciseId = exercise.id,
                    sortOrder = i,
                    setType = "working",
                    reps = 8,
                    weightKg = 60.0,
                    durationSeconds = null,
                    distanceMeters = null,
                    rpe = null,
                    notes = null,
                    completed = false,
                ),
            )
        }

        trainingDao.setExerciseSupersetGroup(session.id, exercise.id, "grp-X")
        val tagged = trainingDao.observeWorkoutSetDetailRows(session.id).first()
        assertEquals(listOf("grp-X", "grp-X"), tagged.map { it.supersetGroupId })

        trainingDao.clearSupersetGroup(session.id, "grp-X")
        val cleared = trainingDao.observeWorkoutSetDetailRows(session.id).first()
        assertTrue(cleared.all { it.supersetGroupId == null })
    }

    @Test
    fun userGoals_roundTrip() = runTest {
        val dao = database.userGoalsDao()
        dao.upsertUserGoals(UserGoalsEntity("default", 8_000L, 5, 78.0, 1L))

        val loaded = dao.observeUserGoals("default").first()

        assertEquals(8_000L, loaded?.stepGoal)
        assertEquals(5, loaded?.weeklySessionTarget)
        assertEquals(78.0, loaded?.targetWeightKg ?: 0.0, 0.001)
    }
}
