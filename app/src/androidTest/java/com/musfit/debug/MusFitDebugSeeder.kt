package com.musfit.debug

import android.content.Context
import androidx.room.withTransaction
import com.musfit.core.di.DatabaseModule
import com.musfit.data.local.MusFitDatabase
import com.musfit.data.local.entity.ACTIVE_ACCOUNT_SESSION_KEY
import com.musfit.data.local.entity.AccountEntity
import com.musfit.data.local.entity.AccountSessionEntity
import com.musfit.data.local.entity.AppSettingsEntity
import com.musfit.data.local.entity.BodyMetricEntity
import com.musfit.data.local.entity.DailyHealthSummaryEntity
import com.musfit.data.local.entity.FoodEntity
import com.musfit.data.local.entity.FoodGoalEntity
import com.musfit.data.local.entity.FoodHealthConnectSyncEntity
import com.musfit.data.local.entity.FoodServingEntity
import com.musfit.data.local.entity.HealthConnectSyncStateEntity
import com.musfit.data.local.entity.LOCAL_DEFAULT_ACCOUNT_ID
import com.musfit.data.local.entity.MealDefinitionEntity
import com.musfit.data.local.entity.MealEntity
import com.musfit.data.local.entity.MealItemEntity
import com.musfit.data.local.entity.MealTemplateEntity
import com.musfit.data.local.entity.MealTemplateItemEntity
import com.musfit.data.local.entity.QuickCaloriePresetEntity
import com.musfit.data.local.entity.RecipeEntity
import com.musfit.data.local.entity.RecipeIngredientEntity
import com.musfit.data.local.entity.ShoppingListItemEntity
import com.musfit.data.local.entity.TrainingSettingsEntity
import com.musfit.data.local.entity.UserGoalsEntity
import com.musfit.data.local.entity.UserProfileEntity
import com.musfit.data.local.entity.WaterEntryEntity
import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import com.musfit.data.repository.AccountAuthProvider
import com.musfit.data.repository.AssetExerciseDatasetProvider
import com.musfit.data.repository.FoodGoalMode
import com.musfit.data.repository.LocalTrainingRepository
import com.musfit.data.repository.TrainingRepository
import java.time.LocalDate
import java.time.ZoneId

/**
 * Deterministic seed implementation compiled only into the instrumentation APK.
 *
 * The installable MusFit APK contains neither this implementation nor an IPC
 * component that can reach it. Android's instrumentation entry point is guarded
 * by the platform and is started by the developer helper through adb shell.
 */
internal class MusFitDebugSeeder private constructor(
    private val database: MusFitDatabase,
    private val trainingRepository: TrainingRepository,
) : AutoCloseable {
    suspend fun seed(reset: Boolean): String {
        if (reset) {
            database.clearAllTables()
        }

        trainingRepository.seedStarterTrainingData()

        val today = LocalDate.now()
        val now = System.currentTimeMillis()
        val foodDao = database.foodDao()
        val trainingDao = database.trainingDao()

        database.withTransaction {
            database.accountDao().upsertAccount(
                AccountEntity(
                    id = LOCAL_DEFAULT_ACCOUNT_ID,
                    displayName = "MusFit Tester",
                    email = "tester@musfit.local",
                    remoteUserId = null,
                    authProvider = AccountAuthProvider.Local.storageValue,
                    avatarUrl = null,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                ),
            )
            database.accountDao().upsertSession(
                AccountSessionEntity(
                    key = ACTIVE_ACCOUNT_SESSION_KEY,
                    activeAccountId = LOCAL_DEFAULT_ACCOUNT_ID,
                    updatedAtEpochMillis = now,
                ),
            )
            database.profileDao().upsertProfile(
                UserProfileEntity(
                    id = LOCAL_DEFAULT_ACCOUNT_ID,
                    sex = "Male",
                    birthDateEpochDay = LocalDate.of(1992, 4, 18).toEpochDay(),
                    heightCm = 180.0,
                    activityLevel = "Moderate",
                    goalType = "Gain",
                    goalPaceKgPerWeek = 0.25,
                    goalWeightKg = 82.0,
                    updatedAtEpochMillis = now,
                ),
            )
            database.profileDao().upsertSettings(
                AppSettingsEntity(
                    id = LOCAL_DEFAULT_ACCOUNT_ID,
                    unitSystem = "metric",
                    themeMode = "system",
                    updatedAtEpochMillis = now,
                ),
            )
            database.userGoalsDao().upsertUserGoals(
                UserGoalsEntity(
                    id = LOCAL_DEFAULT_ACCOUNT_ID,
                    stepGoal = 10_000L,
                    weeklySessionTarget = 4,
                    targetWeightKg = 82.0,
                    updatedAtEpochMillis = now,
                ),
            )

            seedFoodLibrary(today, now)
            seedDiary(today)
            seedFoodExtras(today, now)
            seedHealth(today, now)
            seedTrainingHistory(today)
            trainingDao.upsertTrainingSettings(
                TrainingSettingsEntity(
                    id = "default",
                    defaultRestSeconds = 150,
                    barWeightKg = 20.0,
                    availablePlatesKg = "25,20,15,10,5,2.5,1.25",
                ),
            )
        }

        return "MusFit debug data seeded (reset=$reset): foods=${DEBUG_FOODS.size}, diaryDays=4, recipes=1, templates=2, workouts=3"
    }

    private suspend fun seedFoodLibrary(today: LocalDate, now: Long) {
        val dao = database.foodDao()
        DEBUG_FOODS.forEach { food ->
            dao.upsertFood(
                FoodEntity(
                    accountId = LOCAL_DEFAULT_ACCOUNT_ID,
                    id = food.id,
                    name = food.name,
                    brand = food.brand,
                    defaultServingGrams = food.defaultServingGrams,
                    caloriesPer100g = food.caloriesPer100g,
                    proteinPer100g = food.proteinPer100g,
                    carbsPer100g = food.carbsPer100g,
                    fatPer100g = food.fatPer100g,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                    servingName = food.servingName,
                    barcode = food.barcode,
                    category = food.category,
                    isFavorite = food.isFavorite,
                    fiberPer100g = food.fiberPer100g,
                    sugarPer100g = food.sugarPer100g,
                    saturatedFatPer100g = food.saturatedFatPer100g,
                    sodiumMgPer100g = food.sodiumMgPer100g,
                    potassiumMgPer100g = food.potassiumMgPer100g,
                    calciumMgPer100g = food.calciumMgPer100g,
                    ironMgPer100g = food.ironMgPer100g,
                    vitaminDMcgPer100g = food.vitaminDMcgPer100g,
                    vitaminCMgPer100g = food.vitaminCMgPer100g,
                    magnesiumMgPer100g = food.magnesiumMgPer100g,
                    imageUrl = food.imageUrl,
                ),
            )
            food.servings.forEach { serving ->
                dao.upsertServing(
                    FoodServingEntity(
                        accountId = LOCAL_DEFAULT_ACCOUNT_ID,
                        id = "${food.id}-serving-${serving.label.lowercase().replace(" ", "-")}",
                        foodId = food.id,
                        label = serving.label,
                        grams = serving.grams,
                    ),
                )
            }
        }

        dao.upsertFoodGoal(
            FoodGoalEntity(
                accountId = LOCAL_DEFAULT_ACCOUNT_ID,
                id = "default",
                dailyCaloriesKcal = 2_450.0,
                proteinGrams = 180.0,
                carbsGrams = 255.0,
                fatGrams = 80.0,
                fiberGrams = 34.0,
                sugarGrams = 70.0,
                saturatedFatGrams = 24.0,
                sodiumMilligrams = 2_300.0,
                mode = FoodGoalMode.HighProtein.name,
                includeTrainingCalories = true,
                useNetCarbs = false,
                waterGoalMilliliters = 2_600.0,
                updatedAtEpochMillis = now,
            ),
        )
        dao.upsertFoodHealthConnectSyncState(
            FoodHealthConnectSyncEntity(
                accountId = LOCAL_DEFAULT_ACCOUNT_ID,
                key = "food",
                isEnabled = false,
                lastSyncAtEpochMillis = today.minusDays(1).atHourMillis(20),
                lastFailureMessage = null,
                updatedAtEpochMillis = now,
            ),
        )
    }

    private suspend fun seedDiary(today: LocalDate) {
        val rows = listOf(
            MealPlan(today.minusDays(2), "breakfast", "logged", "debug-food-eggs", 110.0),
            MealPlan(today.minusDays(2), "breakfast", "logged", "debug-food-oats", 55.0),
            MealPlan(today.minusDays(2), "lunch", "logged", "debug-food-turkey", 150.0),
            MealPlan(today.minusDays(2), "lunch", "logged", "debug-food-rice", 170.0),
            MealPlan(today.minusDays(2), "dinner", "logged", "debug-food-salmon", 160.0),
            MealPlan(today.minusDays(2), "dinner", "logged", "debug-food-sweet-potato", 220.0),
            MealPlan(today.minusDays(1), "breakfast", "logged", "debug-food-yogurt", 220.0),
            MealPlan(today.minusDays(1), "breakfast", "logged", "debug-food-blueberries", 90.0),
            MealPlan(today.minusDays(1), "breakfast", "logged", "debug-food-whey", 30.0),
            MealPlan(today.minusDays(1), "lunch", "logged", "debug-food-chicken", 160.0),
            MealPlan(today.minusDays(1), "lunch", "logged", "debug-food-rice", 180.0),
            MealPlan(today.minusDays(1), "lunch", "logged", "debug-food-spinach", 60.0),
            MealPlan(today.minusDays(1), "dinner", "logged", "debug-food-turkey", 170.0),
            MealPlan(today.minusDays(1), "dinner", "logged", "debug-food-avocado", 70.0),
            MealPlan(today, "breakfast", "logged", "debug-food-oats", 60.0),
            MealPlan(today, "breakfast", "logged", "debug-food-yogurt", 200.0),
            MealPlan(today, "breakfast", "logged", "debug-food-blueberries", 80.0),
            MealPlan(today, "breakfast", "logged", "debug-food-whey", 30.0),
            MealPlan(today, "lunch", "logged", "debug-food-chicken", 155.0),
            MealPlan(today, "lunch", "logged", "debug-food-rice", 190.0),
            MealPlan(today, "lunch", "logged", "debug-food-avocado", 65.0),
            MealPlan(today, "lunch", "logged", "debug-food-spinach", 55.0),
            MealPlan(today, "snacks", "logged", "debug-food-banana", 118.0),
            MealPlan(today, "snacks", "logged", "debug-food-almonds", 30.0),
            MealPlan(today, "dinner", "planned", "debug-food-salmon", 170.0),
            MealPlan(today, "dinner", "planned", "debug-food-sweet-potato", 250.0),
            MealPlan(today, "dinner", "planned", "debug-food-spinach", 70.0),
            MealPlan(today.plusDays(1), "breakfast", "planned", "debug-food-eggs", 120.0),
            MealPlan(today.plusDays(1), "breakfast", "planned", "debug-food-oats", 50.0),
            MealPlan(today.plusDays(1), "lunch", "planned", "debug-food-turkey", 160.0),
            MealPlan(today.plusDays(1), "lunch", "planned", "debug-food-rice", 180.0),
            MealPlan(today.plusDays(1), "dinner", "planned", "debug-food-salmon", 170.0),
            MealPlan(today.plusDays(1), "dinner", "planned", "debug-food-spinach", 70.0),
        )

        val dao = database.foodDao()
        rows
            .groupBy { it.date to it.mealType }
            .forEach { (key, mealPlans) ->
                val (date, mealType) = key
                val mealId = "debug-meal-${date.toEpochDay()}-$mealType"
                dao.upsertMeal(
                    MealEntity(
                        accountId = LOCAL_DEFAULT_ACCOUNT_ID,
                        id = mealId,
                        dateEpochDay = date.toEpochDay(),
                        type = mealType,
                        notes = null,
                        createdAtEpochMillis = date.atMealMillis(mealType),
                        updatedAtEpochMillis = date.atMealMillis(mealType),
                    ),
                )
                mealPlans.forEach { item ->
                    dao.upsertMealItem(
                        MealItemEntity(
                            accountId = LOCAL_DEFAULT_ACCOUNT_ID,
                            id = "debug-item-${date.toEpochDay()}-$mealType-${item.foodId}",
                            mealId = mealId,
                            foodId = item.foodId,
                            quantityGrams = item.quantityGrams,
                            status = item.status,
                        ),
                    )
                }
            }
    }

    private suspend fun seedFoodExtras(today: LocalDate, now: Long) {
        val dao = database.foodDao()
        val definitions = listOf(
            MealDefinitionEntity(LOCAL_DEFAULT_ACCOUNT_ID, "breakfast", "Breakfast", 8 * 60, 0, now, now),
            MealDefinitionEntity(LOCAL_DEFAULT_ACCOUNT_ID, "lunch", "Lunch", 12 * 60 + 30, 10, now, now),
            MealDefinitionEntity(LOCAL_DEFAULT_ACCOUNT_ID, "dinner", "Dinner", 19 * 60, 20, now, now),
            MealDefinitionEntity(LOCAL_DEFAULT_ACCOUNT_ID, "snacks", "Snacks", 16 * 60, 30, now, now),
            MealDefinitionEntity(LOCAL_DEFAULT_ACCOUNT_ID, "post-workout", "Post-workout", 18 * 60, 35, now, now),
        )
        definitions.forEach { dao.upsertMealDefinition(it) }

        listOf(
            QuickCaloriePresetEntity(LOCAL_DEFAULT_ACCOUNT_ID, "debug-quick-protein-shake", "Protein shake", 220.0, 32.0, 14.0, 4.0, now, now, true),
            QuickCaloriePresetEntity(LOCAL_DEFAULT_ACCOUNT_ID, "debug-quick-small-snack", "Small snack", 180.0, 10.0, 20.0, 6.0, now, now, true),
            QuickCaloriePresetEntity(LOCAL_DEFAULT_ACCOUNT_ID, "debug-quick-coffee-milk", "Coffee with milk", 70.0, 4.0, 8.0, 2.0, now, now, false),
        ).forEach { dao.upsertQuickCaloriePreset(it) }

        listOf(
            MealTemplateEntity(LOCAL_DEFAULT_ACCOUNT_ID, "debug-template-protein-breakfast", "Protein breakfast", "breakfast", now, now, true),
            MealTemplateEntity(LOCAL_DEFAULT_ACCOUNT_ID, "debug-template-chicken-prep", "Chicken prep lunch", "lunch", now, now, true),
        ).forEach { dao.upsertMealTemplate(it) }
        listOf(
            MealTemplateItemEntity(LOCAL_DEFAULT_ACCOUNT_ID, "debug-template-protein-breakfast-oats", "debug-template-protein-breakfast", "debug-food-oats", 60.0, 0),
            MealTemplateItemEntity(LOCAL_DEFAULT_ACCOUNT_ID, "debug-template-protein-breakfast-yogurt", "debug-template-protein-breakfast", "debug-food-yogurt", 200.0, 1),
            MealTemplateItemEntity(LOCAL_DEFAULT_ACCOUNT_ID, "debug-template-protein-breakfast-whey", "debug-template-protein-breakfast", "debug-food-whey", 30.0, 2),
            MealTemplateItemEntity(LOCAL_DEFAULT_ACCOUNT_ID, "debug-template-chicken-prep-chicken", "debug-template-chicken-prep", "debug-food-chicken", 160.0, 0),
            MealTemplateItemEntity(LOCAL_DEFAULT_ACCOUNT_ID, "debug-template-chicken-prep-rice", "debug-template-chicken-prep", "debug-food-rice", 190.0, 1),
            MealTemplateItemEntity(LOCAL_DEFAULT_ACCOUNT_ID, "debug-template-chicken-prep-spinach", "debug-template-chicken-prep", "debug-food-spinach", 60.0, 2),
        ).forEach { dao.upsertMealTemplateItem(it) }

        dao.upsertRecipe(
            RecipeEntity(
                accountId = LOCAL_DEFAULT_ACCOUNT_ID,
                id = "debug-recipe-salmon-power-bowl",
                name = "Salmon power bowl",
                category = "Dinner",
                servingName = "bowl",
                servingGrams = 520.0,
                servings = 2.0,
                cookedYieldGrams = 1040.0,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
                isFavorite = true,
            ),
        )
        listOf(
            RecipeIngredientEntity(LOCAL_DEFAULT_ACCOUNT_ID, "debug-recipe-salmon-power-bowl-salmon", "debug-recipe-salmon-power-bowl", "debug-food-salmon", 340.0, "fillet", 170.0, 2.0, 0),
            RecipeIngredientEntity(LOCAL_DEFAULT_ACCOUNT_ID, "debug-recipe-salmon-power-bowl-sweet-potato", "debug-recipe-salmon-power-bowl", "debug-food-sweet-potato", 400.0, "medium", 200.0, 2.0, 1),
            RecipeIngredientEntity(LOCAL_DEFAULT_ACCOUNT_ID, "debug-recipe-salmon-power-bowl-spinach", "debug-recipe-salmon-power-bowl", "debug-food-spinach", 140.0, "cup", 30.0, 4.7, 2),
            RecipeIngredientEntity(LOCAL_DEFAULT_ACCOUNT_ID, "debug-recipe-salmon-power-bowl-avocado", "debug-recipe-salmon-power-bowl", "debug-food-avocado", 120.0, "half", 70.0, 1.7, 3),
        ).forEach { dao.upsertRecipeIngredient(it) }

        listOf(
            ShoppingListItemEntity(LOCAL_DEFAULT_ACCOUNT_ID, "debug-shop-salmon", "Salmon fillets", "Protein", 500.0, false, false, "debug-seed-salmon", 0, now, now),
            ShoppingListItemEntity(LOCAL_DEFAULT_ACCOUNT_ID, "debug-shop-yogurt", "Greek yogurt", "Dairy", 500.0, false, false, "debug-seed-yogurt", 1, now, now),
            ShoppingListItemEntity(LOCAL_DEFAULT_ACCOUNT_ID, "debug-shop-spinach", "Baby spinach", "Produce", 200.0, true, false, "debug-seed-spinach", 2, now, now),
            ShoppingListItemEntity(LOCAL_DEFAULT_ACCOUNT_ID, "debug-shop-coffee", "Coffee filters", "Pantry", 1.0, false, true, null, 3, now, now),
        ).forEach { dao.upsertShoppingListItem(it) }

        listOf(
            WaterEntryEntity(LOCAL_DEFAULT_ACCOUNT_ID, "debug-water-${today.toEpochDay()}-morning", today.toEpochDay(), 500.0, today.atHourMillis(8)),
            WaterEntryEntity(LOCAL_DEFAULT_ACCOUNT_ID, "debug-water-${today.toEpochDay()}-lunch", today.toEpochDay(), 750.0, today.atHourMillis(12)),
            WaterEntryEntity(LOCAL_DEFAULT_ACCOUNT_ID, "debug-water-${today.toEpochDay()}-afternoon", today.toEpochDay(), 500.0, today.atHourMillis(16)),
            WaterEntryEntity(LOCAL_DEFAULT_ACCOUNT_ID, "debug-water-${today.minusDays(1).toEpochDay()}-total", today.minusDays(1).toEpochDay(), 2_400.0, today.minusDays(1).atHourMillis(20)),
            WaterEntryEntity(LOCAL_DEFAULT_ACCOUNT_ID, "debug-water-${today.minusDays(2).toEpochDay()}-total", today.minusDays(2).toEpochDay(), 2_150.0, today.minusDays(2).atHourMillis(20)),
        ).forEach { dao.insertWaterEntry(it) }
    }

    private suspend fun seedHealth(today: LocalDate, now: Long) {
        val dao = database.healthDao()
        listOf(
            BodyMetricEntity("debug-weight-${today.minusDays(14).toEpochDay()}", "weight", 79.8, "kg", today.minusDays(14).atHourMillis(7), "manual", null),
            BodyMetricEntity("debug-weight-${today.minusDays(7).toEpochDay()}", "weight", 80.3, "kg", today.minusDays(7).atHourMillis(7), "manual", null),
            BodyMetricEntity("debug-weight-${today.toEpochDay()}", "weight", 80.9, "kg", today.atHourMillis(7), "manual", null),
            BodyMetricEntity("debug-waist-${today.toEpochDay()}", "waist", 82.0, "cm", today.atHourMillis(7), "manual", null),
            BodyMetricEntity("debug-body-fat-${today.toEpochDay()}", "body_fat", 14.8, "%", today.atHourMillis(7), "manual", null),
        ).forEach { dao.upsertBodyMetric(it) }

        (0L..6L).forEach { offset ->
            val date = today.minusDays(offset)
            dao.upsertDailySummary(
                DailyHealthSummaryEntity(
                    dateEpochDay = date.toEpochDay(),
                    steps = 7_800L + (offset * 450L),
                    activeCaloriesKcal = 360.0 + (offset * 18.0),
                    totalCaloriesKcal = 2_150.0 + (offset * 22.0),
                    distanceMeters = 5_200.0 + (offset * 240.0),
                    sleepMinutes = 430L + (offset * 5L),
                    exerciseMinutes = 35L + (offset * 4L),
                    exerciseSessionCount = if (offset % 2L == 0L) 1 else 0,
                    latestWeightKg = 80.9 - (offset * 0.08),
                    latestBodyFatPercent = 14.8,
                    restingHeartRateBpm = 58L + (offset % 3),
                    hrvRmssdMillis = 66.0 - (offset * 1.5),
                    updatedAtEpochMillis = now,
                ),
            )
        }
        dao.upsertHealthConnectSyncState(
            HealthConnectSyncStateEntity(
                key = "health_connect",
                isAvailable = false,
                grantedPermissionsCsv = "",
                lastImportAtEpochMillis = today.minusDays(1).atHourMillis(22),
                lastExportAtEpochMillis = null,
                lastFailureMessage = null,
            ),
        )
    }

    private suspend fun seedTrainingHistory(today: LocalDate) {
        val dao = database.trainingDao()
        listOf(
            WorkoutSessionEntity(
                id = "debug-workout-upper-a",
                routineId = "starter-routine-upper-a",
                title = "Upper A",
                status = "completed",
                startedAtEpochMillis = today.minusDays(3).atHourMillis(18),
                endedAtEpochMillis = today.minusDays(3).atHourMillis(19) + 18 * 60 * 1000L,
                notes = "Debug seed upper day.",
                healthConnectRecordId = null,
                healthConnectLastExportedAtEpochMillis = null,
            ),
            WorkoutSessionEntity(
                id = "debug-workout-legs",
                routineId = "starter-routine-legs",
                title = "Legs",
                status = "completed",
                startedAtEpochMillis = today.minusDays(1).atHourMillis(17),
                endedAtEpochMillis = today.minusDays(1).atHourMillis(18) + 25 * 60 * 1000L,
                notes = "Debug seed leg day.",
                healthConnectRecordId = null,
                healthConnectLastExportedAtEpochMillis = null,
            ),
            WorkoutSessionEntity(
                id = "debug-workout-active",
                routineId = "starter-routine-full-body-a",
                title = "Full Body A",
                status = "active",
                startedAtEpochMillis = today.atHourMillis(18),
                endedAtEpochMillis = null,
                notes = "Debug active workout for logger testing.",
                healthConnectRecordId = null,
                healthConnectLastExportedAtEpochMillis = null,
            ),
        ).forEach { dao.upsertWorkoutSession(it) }

        listOf(
            WorkoutSetEntity("debug-set-upper-bench-1", "debug-workout-upper-a", "starter-ex-bench-press", 0, "working", 5, 82.5, null, null, 8.0, null, true),
            WorkoutSetEntity("debug-set-upper-bench-2", "debug-workout-upper-a", "starter-ex-bench-press", 1, "working", 5, 82.5, null, null, 8.5, null, true),
            WorkoutSetEntity("debug-set-upper-row-1", "debug-workout-upper-a", "starter-ex-barbell-row", 2, "working", 8, 70.0, null, null, 8.0, null, true),
            WorkoutSetEntity("debug-set-upper-row-2", "debug-workout-upper-a", "starter-ex-barbell-row", 3, "working", 8, 70.0, null, null, 8.0, null, true),
            WorkoutSetEntity("debug-set-legs-squat-1", "debug-workout-legs", "starter-ex-back-squat", 0, "working", 5, 105.0, null, null, 8.0, null, true),
            WorkoutSetEntity("debug-set-legs-squat-2", "debug-workout-legs", "starter-ex-back-squat", 1, "working", 5, 105.0, null, null, 8.5, null, true),
            WorkoutSetEntity("debug-set-legs-rdl-1", "debug-workout-legs", "starter-ex-romanian-deadlift", 2, "working", 8, 95.0, null, null, 8.0, null, true),
            WorkoutSetEntity("debug-set-active-squat-1", "debug-workout-active", "starter-ex-back-squat", 0, "working", 5, 107.5, null, null, 7.5, null, true),
            WorkoutSetEntity("debug-set-active-bench-1", "debug-workout-active", "starter-ex-bench-press", 1, "working", 5, 85.0, null, null, null, null, false),
            WorkoutSetEntity("debug-set-active-row-1", "debug-workout-active", "starter-ex-barbell-row", 2, "working", 8, 72.5, null, null, null, null, false),
        ).forEach { dao.upsertWorkoutSet(it) }
    }

    private data class DebugFood(
        val id: String,
        val name: String,
        val brand: String?,
        val defaultServingGrams: Double,
        val servingName: String?,
        val barcode: String?,
        val category: String?,
        val isFavorite: Boolean,
        val caloriesPer100g: Double,
        val proteinPer100g: Double,
        val carbsPer100g: Double,
        val fatPer100g: Double,
        val fiberPer100g: Double,
        val sugarPer100g: Double,
        val saturatedFatPer100g: Double,
        val sodiumMgPer100g: Double,
        val potassiumMgPer100g: Double,
        val calciumMgPer100g: Double,
        val ironMgPer100g: Double,
        val vitaminDMcgPer100g: Double,
        val vitaminCMgPer100g: Double,
        val magnesiumMgPer100g: Double,
        val imageUrl: String? = null,
        val servings: List<DebugServing> = emptyList(),
    )

    private data class DebugServing(val label: String, val grams: Double)

    private data class MealPlan(
        val date: LocalDate,
        val mealType: String,
        val status: String,
        val foodId: String,
        val quantityGrams: Double,
    )

    companion object {
        fun create(context: Context): MusFitDebugSeeder {
            val appContext = context.applicationContext
            val database = DatabaseModule.provideDatabase(appContext)
            val trainingRepository = LocalTrainingRepository(
                database = database,
                trainingDao = database.trainingDao(),
                exerciseDataset = AssetExerciseDatasetProvider(appContext),
            )
            return MusFitDebugSeeder(database, trainingRepository)
        }

        private val DEBUG_FOODS = listOf(
            DebugFood("debug-food-yogurt", "Greek yogurt 2%", "MusFit Test Kitchen", 200.0, "cup", "900000000001", "Dairy", true, 73.0, 10.0, 3.9, 2.0, 0.0, 3.2, 1.3, 36.0, 141.0, 110.0, 0.1, 0.0, 0.0, 11.0, servings = listOf(DebugServing("cup", 200.0))),
            DebugFood("debug-food-oats", "Rolled oats", "MusFit Test Kitchen", 60.0, "serving", "900000000002", "Grains", true, 389.0, 16.9, 66.3, 6.9, 10.6, 0.9, 1.2, 2.0, 429.0, 54.0, 4.7, 0.0, 0.0, 177.0, servings = listOf(DebugServing("half cup dry", 40.0), DebugServing("training bowl", 60.0))),
            DebugFood("debug-food-blueberries", "Blueberries", null, 80.0, "handful", "900000000003", "Fruit", true, 57.0, 0.7, 14.5, 0.3, 2.4, 10.0, 0.0, 1.0, 77.0, 6.0, 0.3, 0.0, 9.7, 6.0, servings = listOf(DebugServing("handful", 80.0), DebugServing("cup", 148.0))),
            DebugFood("debug-food-whey", "Whey protein vanilla", "MusFit Test Kitchen", 30.0, "scoop", "900000000004", "Supplements", true, 400.0, 80.0, 8.0, 5.0, 1.0, 4.0, 2.0, 180.0, 200.0, 450.0, 1.0, 0.0, 0.0, 50.0, servings = listOf(DebugServing("scoop", 30.0))),
            DebugFood("debug-food-chicken", "Chicken breast cooked", null, 150.0, "fillet", "900000000005", "Protein", true, 165.0, 31.0, 0.0, 3.6, 0.0, 0.0, 1.0, 74.0, 256.0, 15.0, 1.0, 0.1, 0.0, 29.0, servings = listOf(DebugServing("fillet", 150.0))),
            DebugFood("debug-food-rice", "Jasmine rice cooked", null, 180.0, "cup", "900000000006", "Grains", true, 130.0, 2.7, 28.0, 0.3, 0.4, 0.1, 0.1, 1.0, 35.0, 10.0, 1.2, 0.0, 0.0, 12.0, servings = listOf(DebugServing("cup cooked", 158.0))),
            DebugFood("debug-food-avocado", "Avocado", null, 70.0, "half", "900000000007", "Fats", true, 160.0, 2.0, 8.5, 14.7, 6.7, 0.7, 2.1, 7.0, 485.0, 12.0, 0.6, 0.0, 10.0, 29.0, servings = listOf(DebugServing("half", 70.0))),
            DebugFood("debug-food-spinach", "Baby spinach", null, 50.0, "bowl", "900000000008", "Vegetables", false, 23.0, 2.9, 3.6, 0.4, 2.2, 0.4, 0.1, 79.0, 558.0, 99.0, 2.7, 0.0, 28.1, 79.0, servings = listOf(DebugServing("cup", 30.0), DebugServing("bowl", 50.0))),
            DebugFood("debug-food-banana", "Banana", null, 118.0, "medium", "900000000009", "Fruit", false, 89.0, 1.1, 22.8, 0.3, 2.6, 12.2, 0.1, 1.0, 358.0, 5.0, 0.3, 0.0, 8.7, 27.0, servings = listOf(DebugServing("medium", 118.0))),
            DebugFood("debug-food-almonds", "Almonds", null, 30.0, "small handful", "900000000010", "Fats", true, 579.0, 21.2, 21.6, 49.9, 12.5, 4.4, 3.8, 1.0, 733.0, 269.0, 3.7, 0.0, 0.0, 270.0, servings = listOf(DebugServing("small handful", 30.0))),
            DebugFood("debug-food-salmon", "Salmon fillet cooked", null, 170.0, "fillet", "900000000011", "Protein", true, 208.0, 20.4, 0.0, 13.4, 0.0, 0.0, 3.1, 59.0, 363.0, 9.0, 0.3, 10.9, 0.0, 27.0, servings = listOf(DebugServing("fillet", 170.0))),
            DebugFood("debug-food-sweet-potato", "Sweet potato roasted", null, 200.0, "medium", "900000000012", "Vegetables", false, 90.0, 2.0, 20.7, 0.2, 3.3, 6.5, 0.0, 36.0, 475.0, 38.0, 0.7, 0.0, 19.6, 27.0, servings = listOf(DebugServing("medium", 200.0))),
            DebugFood("debug-food-eggs", "Eggs", null, 100.0, "2 large", "900000000013", "Protein", false, 143.0, 12.6, 0.7, 9.5, 0.0, 0.4, 3.1, 142.0, 138.0, 56.0, 1.8, 2.0, 0.0, 12.0, servings = listOf(DebugServing("large egg", 50.0), DebugServing("2 large", 100.0))),
            DebugFood("debug-food-turkey", "Turkey mince 5%", null, 150.0, "portion", "900000000014", "Protein", false, 155.0, 27.0, 0.0, 5.0, 0.0, 0.0, 1.4, 70.0, 250.0, 15.0, 1.1, 0.2, 0.0, 28.0, servings = listOf(DebugServing("portion", 150.0))),
        )
    }

    override fun close() {
        database.close()
    }
}

private fun LocalDate.atMealMillis(mealType: String): Long = atHourMillis(
    when (mealType) {
        "breakfast" -> 8
        "lunch" -> 13
        "dinner" -> 19
        "snacks" -> 16
        else -> 12
    },
)

private fun LocalDate.atHourMillis(hour: Int): Long = atStartOfDay(ZoneId.systemDefault())
    .plusHours(hour.toLong())
    .toInstant()
    .toEpochMilli()
