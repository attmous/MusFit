package com.musfit.data.repository

import androidx.room.withTransaction
import com.musfit.data.local.MusFitDatabase
import com.musfit.data.local.dao.FoodDao
import com.musfit.data.local.dao.FoodDiaryEntryRow
import com.musfit.data.local.dao.MealNutritionRow
import com.musfit.data.local.dao.MealTemplateItemRow
import com.musfit.data.local.dao.RecipeIngredientRow
import com.musfit.data.local.dao.WaterTotalRow
import com.musfit.data.local.entity.BarcodeProductEntity
import com.musfit.data.local.entity.FoodEntity
import com.musfit.data.local.entity.FoodGoalEntity
import com.musfit.data.local.entity.FoodHealthConnectSyncEntity
import com.musfit.data.local.entity.FoodServingEntity
import com.musfit.data.local.entity.MealDefinitionEntity
import com.musfit.data.local.entity.MealEntity
import com.musfit.data.local.entity.MealItemEntity
import com.musfit.data.local.entity.MealTemplateEntity
import com.musfit.data.local.entity.MealTemplateItemEntity
import com.musfit.data.local.entity.QuickCaloriePresetEntity
import com.musfit.data.local.entity.RecipeEntity
import com.musfit.data.local.entity.RecipeIngredientEntity
import com.musfit.data.local.entity.ShoppingListItemEntity
import com.musfit.data.local.entity.WaterEntryEntity
import com.musfit.data.remote.food.ProductDataQuality
import com.musfit.data.remote.food.ProductLookupResult
import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.domain.health.HealthConnectStatus
import com.musfit.domain.health.ImportedDailyHealthSummary
import com.musfit.domain.model.FoodNutrition
import com.musfit.domain.model.MealItemInput
import com.musfit.domain.model.NutritionTotals
import com.musfit.domain.nutrition.NutritionCalculator
import com.musfit.integrations.healthconnect.HealthConnectFoodExportPayload
import com.musfit.integrations.healthconnect.HealthConnectFoodExportResult
import com.musfit.integrations.healthconnect.HealthConnectFoodMealExport
import com.musfit.integrations.healthconnect.HealthConnectGateway
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

const val DEFAULT_WATER_GOAL_MILLILITERS = 2000.0

data class NutritionDetails(
    val fiberGrams: Double = 0.0,
    val sugarGrams: Double = 0.0,
    val saturatedFatGrams: Double = 0.0,
    val sodiumMilligrams: Double = 0.0,
    val potassiumMilligrams: Double = 0.0,
    val calciumMilligrams: Double = 0.0,
    val ironMilligrams: Double = 0.0,
    val vitaminDMicrograms: Double = 0.0,
    val vitaminCMilligrams: Double = 0.0,
    val magnesiumMilligrams: Double = 0.0,
)

data class FoodServingInput(
    val label: String,
    val grams: Double,
)

data class FoodServingOption(
    val id: String,
    val label: String,
    val grams: Double,
)

data class FoodLogInput(
    val lookupResult: ProductLookupResult.Found?,
    val barcode: String?,
    val name: String,
    val brand: String?,
    val nutritionPer100g: FoodNutrition,
    val nutritionDetailsPer100g: NutritionDetails = NutritionDetails(),
    val servingGrams: Double?,
    val mealType: String,
    val quantityGrams: Double,
    val date: LocalDate,
)

data class SavedFoodLogInput(
    val foodId: String,
    val mealType: String,
    val quantityGrams: Double,
    val date: LocalDate,
)

data class QuickCalorieLogInput(
    val mealType: String,
    val caloriesKcal: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val date: LocalDate,
)

enum class FoodDiaryEntryStatus {
    Logged,
    Planned,
}

data class QuickCaloriePresetInput(
    val name: String,
    val caloriesKcal: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val isFavorite: Boolean = true,
)

data class QuickCaloriePreset(
    val id: String,
    val name: String,
    val caloriesKcal: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val isFavorite: Boolean,
)

data class DiaryEntryUpdateInput(
    val mealItemId: String,
    val mealType: String,
    val quantityGrams: Double,
    val date: LocalDate,
)

data class SavedFoodUpsertInput(
    val foodId: String?,
    val name: String,
    val brand: String?,
    val defaultServingGrams: Double,
    val nutritionPer100g: FoodNutrition,
    val nutritionDetailsPer100g: NutritionDetails = NutritionDetails(),
    val servingName: String? = null,
    val barcode: String? = null,
    val category: String? = null,
    val isFavorite: Boolean = false,
    val servings: List<FoodServingInput> = emptyList(),
    val imageUrl: String? = null,
)

data class SavedFoodItem(
    val id: String,
    val name: String,
    val brand: String?,
    val defaultServingGrams: Double,
    val nutritionPer100g: FoodNutrition,
    val nutritionDetailsPer100g: NutritionDetails = NutritionDetails(),
    val servingName: String? = null,
    val barcode: String? = null,
    val category: String? = null,
    val isFavorite: Boolean = false,
    val servings: List<FoodServingOption> = emptyList(),
    val imageUrl: String? = null,
)

data class FoodDiaryEntry(
    val id: String,
    val foodId: String,
    val name: String,
    val brand: String?,
    val quantityGrams: Double,
    val caloriesKcal: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val nutritionDetails: NutritionDetails = NutritionDetails(),
    val status: FoodDiaryEntryStatus = FoodDiaryEntryStatus.Logged,
    val imageUrl: String? = null,
)

data class FoodDiaryMeal(
    val type: String,
    val entries: List<FoodDiaryEntry>,
    val totals: NutritionTotals,
    val detailTotals: NutritionDetails = NutritionDetails(),
    val plannedTotals: NutritionTotals = NutritionTotals(0.0, 0.0, 0.0, 0.0),
    val plannedDetailTotals: NutritionDetails = NutritionDetails(),
)

data class FoodDiary(
    val totals: NutritionTotals,
    val meals: List<FoodDiaryMeal>,
    val detailTotals: NutritionDetails = NutritionDetails(),
    val plannedTotals: NutritionTotals = NutritionTotals(0.0, 0.0, 0.0, 0.0),
    val plannedDetailTotals: NutritionDetails = NutritionDetails(),
)

data class FoodPlanDay(
    val date: LocalDate,
    val loggedTotals: NutritionTotals,
    val plannedTotals: NutritionTotals,
    val loggedEntryCount: Int,
    val plannedEntryCount: Int,
)

data class ShoppingListItem(
    val id: String,
    val name: String,
    val category: String,
    val quantityGrams: Double,
    val isChecked: Boolean,
    val isManual: Boolean,
)

data class ShoppingListGroup(
    val category: String,
    val items: List<ShoppingListItem>,
)

data class ManualShoppingListItemInput(
    val name: String,
    val category: String?,
    val quantityGrams: Double,
)

data class WaterLogInput(
    val date: LocalDate,
    val amountMilliliters: Double,
)

data class FoodWaterSummary(
    val date: LocalDate,
    val consumedMilliliters: Double,
    val goalMilliliters: Double,
)

data class FoodWeeklyDaySummary(
    val date: LocalDate,
    val diary: FoodDiary,
    val water: FoodWaterSummary,
)

data class FoodWeeklySummary(
    val startDate: LocalDate,
    val days: List<FoodWeeklyDaySummary>,
    val goal: FoodGoal,
)

data class FoodProgressSummary(
    val startDate: LocalDate,
    val dayCount: Int,
    val days: List<FoodWeeklyDaySummary>,
    val goal: FoodGoal,
)

data class FoodHealthConnectSyncState(
    val isEnabled: Boolean = false,
    val availability: HealthConnectAvailability = HealthConnectAvailability.NotSupported,
    val grantedPermissionCount: Int = 0,
    val requestablePermissionCount: Int = 0,
    val requestablePermissions: Set<String> = emptySet(),
    val canRequestPermissions: Boolean = false,
    val canSync: Boolean = false,
    val lastSyncAtEpochMillis: Long? = null,
    val lastFailureMessage: String? = null,
)

data class FoodHealthConnectSyncResult(
    val nutritionRecordCount: Int,
    val hydrationRecordCount: Int,
)

enum class FoodGoalMode {
    Balanced,
    HighProtein,
    KetoLowCarb,
    MuscleGain,
    WeightLoss,
    MediterraneanStyle,
    CleanEating,
    Custom,
}

data class FoodGoal(
    val dailyCaloriesKcal: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val fiberGrams: Double,
    val sugarGrams: Double,
    val saturatedFatGrams: Double,
    val sodiumMilligrams: Double,
    val mode: FoodGoalMode,
    val includeTrainingCalories: Boolean,
    val useNetCarbs: Boolean = false,
    val waterGoalMilliliters: Double = DEFAULT_WATER_GOAL_MILLILITERS,
)

data class MealTemplateItem(
    val foodId: String,
    val foodName: String,
    val brand: String?,
    val quantityGrams: Double,
)

data class MealTemplateItemInput(
    val foodId: String,
    val quantityGrams: Double,
)

data class MealTemplateUpdateInput(
    val templateId: String,
    val name: String,
    val mealType: String,
    val items: List<MealTemplateItemInput>,
)

data class MealTemplate(
    val id: String,
    val name: String,
    val mealType: String,
    val isFavorite: Boolean = false,
    val items: List<MealTemplateItem>,
)

data class FoodMealDefinitionInput(
    val mealId: String?,
    val name: String,
    val timeMinutes: Int?,
    val sortOrder: Int,
)

data class FoodMealDefinition(
    val id: String,
    val name: String,
    val timeMinutes: Int?,
    val sortOrder: Int,
)

data class RecipeIngredientInput(
    val foodId: String,
    val quantityGrams: Double,
    val unitLabel: String = "g",
    val unitGrams: Double = 1.0,
    val unitQuantity: Double = quantityGrams,
)

data class RecipeUpsertInput(
    val recipeId: String?,
    val name: String,
    val category: String?,
    val servingName: String,
    val servingGrams: Double,
    val servings: Double = 1.0,
    val cookedYieldGrams: Double = servingGrams,
    val ingredients: List<RecipeIngredientInput>,
)

data class RecipeIngredient(
    val foodId: String,
    val foodName: String,
    val brand: String?,
    val quantityGrams: Double,
    val unitLabel: String = "g",
    val unitGrams: Double = 1.0,
    val unitQuantity: Double = quantityGrams,
)

data class Recipe(
    val id: String,
    val name: String,
    val category: String?,
    val servingName: String,
    val servingGrams: Double,
    val servings: Double = 1.0,
    val cookedYieldGrams: Double = servingGrams,
    val isFavorite: Boolean = false,
    val ingredients: List<RecipeIngredient>,
    val nutritionPerServing: FoodNutrition,
    val detailNutritionPerServing: NutritionDetails,
)

interface FoodRepository {
    suspend fun saveConfirmedProduct(
        result: ProductLookupResult.Found,
        editedName: String,
        editedBrand: String?,
        editedNutrition: FoodNutrition,
    ): String

    suspend fun logFood(input: FoodLogInput): String

    fun observeDailyNutrition(date: LocalDate): Flow<NutritionTotals>

    fun observeFoodDiary(date: LocalDate): Flow<FoodDiary>

    fun observeFoodPlan(startDate: LocalDate): Flow<List<FoodPlanDay>> = flowOf(emptyList())

    /** Distinct epoch-days (newest first) with at least one logged diary entry, for streaks. */
    fun observeLoggedDayEpochDays(fromEpochDay: Long): Flow<List<Long>> = flowOf(emptyList())

    fun observeShoppingList(): Flow<List<ShoppingListGroup>> = flowOf(emptyList())

    fun observeSavedFoods(): Flow<List<SavedFoodItem>>

    fun observeRecentFoods(limit: Int = 20): Flow<List<SavedFoodItem>>

    fun observeSameAsYesterday(mealType: String, date: LocalDate): Flow<List<SavedFoodItem>>

    suspend fun getFoodDetail(foodId: String): SavedFoodItem? = null

    suspend fun logSavedFood(input: SavedFoodLogInput): String

    suspend fun planSavedFood(input: SavedFoodLogInput): String = ""

    suspend fun quickLog(input: QuickCalorieLogInput): String

    fun observeQuickCaloriePresets(): Flow<List<QuickCaloriePreset>> = flowOf(emptyList())

    suspend fun saveFavoriteQuickLog(input: QuickCaloriePresetInput): String = ""

    suspend fun toggleFavoriteQuickLog(presetId: String, isFavorite: Boolean) = Unit

    suspend fun logFavoriteQuickLog(presetId: String, mealType: String, date: LocalDate): String = ""

    suspend fun updateDiaryEntry(input: DiaryEntryUpdateInput)

    suspend fun deleteDiaryEntry(mealItemId: String)

    suspend fun upsertSavedFood(input: SavedFoodUpsertInput): String

    suspend fun deleteSavedFood(foodId: String)

    suspend fun toggleFavoriteFood(foodId: String, isFavorite: Boolean) = Unit

    suspend fun mergeDuplicateFoods(primaryFoodId: String, duplicateFoodIds: List<String>) = Unit

    fun observeFoodGoal(): Flow<FoodGoal> = flowOf(DEFAULT_REPOSITORY_FOOD_GOAL)

    suspend fun updateFoodGoal(goal: FoodGoal) = Unit

    fun observeMealTemplates(): Flow<List<MealTemplate>> = flowOf(emptyList())

    fun observeCustomMealDefinitions(): Flow<List<FoodMealDefinition>> = flowOf(emptyList())

    suspend fun upsertCustomMealDefinition(input: FoodMealDefinitionInput): String = ""

    suspend fun saveMealAsTemplate(date: LocalDate, mealType: String, name: String): String = ""

    suspend fun logMealTemplate(templateId: String, mealType: String, date: LocalDate): List<String> = emptyList()

    suspend fun copyMeal(
        fromDate: LocalDate,
        toDate: LocalDate,
        mealType: String,
        status: FoodDiaryEntryStatus = FoodDiaryEntryStatus.Logged,
    ): List<String> = emptyList()

    suspend fun copyDay(
        fromDate: LocalDate,
        toDate: LocalDate,
        status: FoodDiaryEntryStatus = FoodDiaryEntryStatus.Logged,
    ): List<String> = emptyList()

    suspend fun renameMealTemplate(templateId: String, name: String, mealType: String) = Unit

    suspend fun updateMealTemplate(input: MealTemplateUpdateInput) = Unit

    suspend fun duplicateMealTemplate(templateId: String, name: String): String = ""

    suspend fun deleteMealTemplate(templateId: String) = Unit

    suspend fun toggleFavoriteMealTemplate(templateId: String, isFavorite: Boolean) = Unit

    suspend fun copyDiaryEntry(mealItemId: String, mealType: String, date: LocalDate): String = ""

    suspend fun markDiaryEntryLogged(mealItemId: String) = Unit

    suspend fun generateShoppingList(startDate: LocalDate, endDate: LocalDate): List<ShoppingListGroup> = emptyList()

    suspend fun addManualShoppingListItem(input: ManualShoppingListItemInput): String = ""

    suspend fun toggleShoppingListItem(itemId: String, isChecked: Boolean) = Unit

    fun observeWaterSummary(date: LocalDate): Flow<FoodWaterSummary> =
        flowOf(FoodWaterSummary(date, 0.0, DEFAULT_WATER_GOAL_MILLILITERS))

    /**
     * Active calories burned on [date], sourced from the imported daily Health
     * Connect summary. Zero when nothing is synced. Informational only — it does
     * not change the calorie budget/remaining math.
     */
    fun observeBurnedCalories(date: LocalDate): Flow<Double> = flowOf(0.0)

    fun observeWeeklyFoodSummary(startDate: LocalDate): Flow<FoodWeeklySummary> =
        flowOf(FoodWeeklySummary(startDate = startDate, days = emptyList(), goal = DEFAULT_REPOSITORY_FOOD_GOAL))

    fun observeFoodProgressSummary(startDate: LocalDate, dayCount: Int): Flow<FoodProgressSummary> =
        flowOf(FoodProgressSummary(startDate = startDate, dayCount = dayCount, days = emptyList(), goal = DEFAULT_REPOSITORY_FOOD_GOAL))

    suspend fun logWater(input: WaterLogInput): String = ""

    suspend fun updateWaterGoal(goalMilliliters: Double) = Unit

    fun observeFoodHealthConnectSyncState(): Flow<FoodHealthConnectSyncState> =
        flowOf(FoodHealthConnectSyncState())

    suspend fun refreshFoodHealthConnectSyncState(): FoodHealthConnectSyncState =
        FoodHealthConnectSyncState()

    suspend fun setFoodHealthConnectSyncEnabled(isEnabled: Boolean) = Unit

    suspend fun syncFoodToHealthConnect(date: LocalDate): FoodHealthConnectSyncResult =
        FoodHealthConnectSyncResult(nutritionRecordCount = 0, hydrationRecordCount = 0)

    fun observeRecipes(): Flow<List<Recipe>> = flowOf(emptyList())

    suspend fun upsertRecipe(input: RecipeUpsertInput): String = ""

    suspend fun logRecipe(recipeId: String, mealType: String, servings: Double, date: LocalDate): String = ""

    suspend fun planRecipe(recipeId: String, mealType: String, servings: Double, date: LocalDate): String = ""

    suspend fun duplicateRecipe(recipeId: String, name: String): String = ""

    suspend fun deleteRecipe(recipeId: String) = Unit

    suspend fun toggleFavoriteRecipe(recipeId: String, isFavorite: Boolean) = Unit

    suspend fun seedStarterFoods() = Unit
}

val DEFAULT_REPOSITORY_FOOD_GOAL =
    FoodGoal(
        dailyCaloriesKcal = 2083.0,
        proteinGrams = 104.0,
        carbsGrams = 260.0,
        fatGrams = 69.0,
        fiberGrams = 30.0,
        sugarGrams = 50.0,
        saturatedFatGrams = 20.0,
        sodiumMilligrams = 2300.0,
        mode = FoodGoalMode.Balanced,
        includeTrainingCalories = false,
        useNetCarbs = false,
        waterGoalMilliliters = DEFAULT_WATER_GOAL_MILLILITERS,
    )

class LocalFoodRepository @Inject constructor(
    private val database: MusFitDatabase,
    private val foodDao: FoodDao,
    private val healthConnectGateway: HealthConnectGateway = NoopHealthConnectGateway,
) : FoodRepository {
    private val foodHealthConnectStatusFlow = MutableStateFlow(DEFAULT_HEALTH_CONNECT_STATUS)
    private val foodHealthConnectPermissionsFlow = MutableStateFlow(emptySet<String>())

    override suspend fun saveConfirmedProduct(
        result: ProductLookupResult.Found,
        editedName: String,
        editedBrand: String?,
        editedNutrition: FoodNutrition,
    ): String =
        database.withTransaction {
            upsertConfirmedFood(
                result = result,
                editedName = editedName,
                editedBrand = editedBrand,
                editedNutrition = editedNutrition,
                editedNutritionDetails = result.nutritionDetailsPer100g,
                servingGrams = result.servingQuantityGrams ?: 100.0,
                category = result.category,
                now = System.currentTimeMillis(),
            )
        }

    override suspend fun logFood(input: FoodLogInput): String {
        input.requireValid()
        return database.withTransaction {
            val now = System.currentTimeMillis()
            val foodId = input.lookupResult?.let { result ->
                upsertConfirmedFood(
                    result = result,
                    editedName = input.name,
                    editedBrand = input.brand,
                    editedNutrition = input.nutritionPer100g,
                    editedNutritionDetails = result.nutritionDetailsPer100g,
                    servingGrams = input.servingGrams ?: result.servingQuantityGrams ?: 100.0,
                    category = result.category,
                    now = now,
                )
            } ?: upsertManualFood(input, now)

            insertMealItem(
                foodId = foodId,
                mealType = input.mealType,
                quantityGrams = input.quantityGrams,
                date = input.date,
                now = now,
            )
        }
    }

    override fun observeDailyNutrition(date: LocalDate): Flow<NutritionTotals> =
        foodDao.observeMealNutritionRowsForDate(date.toEpochDay()).map { rows ->
            NutritionCalculator.calculateMealTotals(rows.map { row -> row.toMealItemInput() })
        }

    override fun observeFoodDiary(date: LocalDate): Flow<FoodDiary> =
        foodDao.observeFoodDiaryEntryRowsForDate(date.toEpochDay()).map { rows ->
            rows.toFoodDiary()
        }

    override fun observeFoodPlan(startDate: LocalDate): Flow<List<FoodPlanDay>> {
        val endDate = startDate.plusDays(6)
        return foodDao.observeFoodDiaryEntryRowsForDateRange(
            startEpochDay = startDate.toEpochDay(),
            endEpochDay = endDate.toEpochDay(),
        ).map { rows ->
            rows.toFoodPlanDays(startDate)
        }
    }

    override fun observeLoggedDayEpochDays(fromEpochDay: Long): Flow<List<Long>> =
        foodDao.observeLoggedDayEpochDays(fromEpochDay)

    override fun observeWeeklyFoodSummary(startDate: LocalDate): Flow<FoodWeeklySummary> {
        val endDate = startDate.plusDays(6)
        return combine(
            foodDao.observeFoodDiaryEntryRowsForDateRange(
                startEpochDay = startDate.toEpochDay(),
                endEpochDay = endDate.toEpochDay(),
            ),
            foodDao.observeWaterTotalsForDateRange(
                startEpochDay = startDate.toEpochDay(),
                endEpochDay = endDate.toEpochDay(),
            ),
            observeFoodGoal(),
        ) { rows, waterRows, goal ->
            rows.toFoodWeeklySummary(
                startDate = startDate,
                waterRows = waterRows,
                goal = goal,
            )
        }
    }

    override fun observeFoodProgressSummary(startDate: LocalDate, dayCount: Int): Flow<FoodProgressSummary> {
        val safeDayCount = dayCount.coerceAtLeast(1)
        val endDate = startDate.plusDays((safeDayCount - 1).toLong())
        return combine(
            foodDao.observeFoodDiaryEntryRowsForDateRange(
                startEpochDay = startDate.toEpochDay(),
                endEpochDay = endDate.toEpochDay(),
            ),
            foodDao.observeWaterTotalsForDateRange(
                startEpochDay = startDate.toEpochDay(),
                endEpochDay = endDate.toEpochDay(),
            ),
            observeFoodGoal(),
        ) { rows, waterRows, goal ->
            rows.toFoodProgressSummary(
                startDate = startDate,
                dayCount = safeDayCount,
                waterRows = waterRows,
                goal = goal,
            )
        }
    }

    override fun observeSavedFoods(): Flow<List<SavedFoodItem>> =
        foodDao.observeFoods().map { foods ->
            foods
                .filterNot { food -> food.name == QUICK_CALORIES_NAME && food.brand == null }
                .map { food -> food.toSavedFoodItem(foodDao.getServings(food.id)) }
        }

    override fun observeRecentFoods(limit: Int): Flow<List<SavedFoodItem>> =
        foodDao.observeRecentFoods(limit).map { foods ->
            foods
                .filterNot { food -> food.name == QUICK_CALORIES_NAME && food.brand == null }
                .map { food -> food.toSavedFoodItem(emptyList()) }
        }

    override fun observeSameAsYesterday(mealType: String, date: LocalDate): Flow<List<SavedFoodItem>> =
        foodDao.observeSameAsYesterday(date.minusDays(1).toEpochDay(), mealType).map { foods ->
            foods
                .filterNot { food -> food.name == QUICK_CALORIES_NAME && food.brand == null }
                .map { food -> food.toSavedFoodItem(emptyList()) }
        }

    override suspend fun getFoodDetail(foodId: String): SavedFoodItem? =
        foodDao.getFood(foodId)?.toSavedFoodItem(foodDao.getServings(foodId))

    override suspend fun logSavedFood(input: SavedFoodLogInput): String {
        input.requireValid()
        return database.withTransaction {
            foodDao.getFood(input.foodId) ?: error("Saved food not found")
            insertMealItem(
                foodId = input.foodId,
                mealType = input.mealType,
                quantityGrams = input.quantityGrams,
                date = input.date,
                now = System.currentTimeMillis(),
            )
        }
    }

    override suspend fun planSavedFood(input: SavedFoodLogInput): String {
        input.requireValid()
        return database.withTransaction {
            foodDao.getFood(input.foodId) ?: error("Saved food not found")
            insertMealItem(
                foodId = input.foodId,
                mealType = input.mealType,
                quantityGrams = input.quantityGrams,
                date = input.date,
                now = System.currentTimeMillis(),
                status = FoodDiaryEntryStatus.Planned,
            )
        }
    }

    override suspend fun quickLog(input: QuickCalorieLogInput): String {
        input.requireValid()
        return logFood(
            FoodLogInput(
                lookupResult = null,
                barcode = null,
                name = QUICK_CALORIES_NAME,
                brand = null,
                nutritionPer100g = FoodNutrition(
                    caloriesKcal = input.caloriesKcal,
                    proteinGrams = input.proteinGrams,
                    carbsGrams = input.carbsGrams,
                    fatGrams = input.fatGrams,
                ),
                servingGrams = 100.0,
                mealType = input.mealType,
                quantityGrams = 100.0,
                date = input.date,
            ),
        )
    }

    override fun observeQuickCaloriePresets(): Flow<List<QuickCaloriePreset>> =
        foodDao.observeQuickCaloriePresets().map { presets ->
            presets.map { it.toQuickCaloriePreset() }
        }

    override suspend fun saveFavoriteQuickLog(input: QuickCaloriePresetInput): String {
        input.requireValid()
        return database.withTransaction {
            val now = System.currentTimeMillis()
            val presetId = UUID.randomUUID().toString()
            foodDao.upsertQuickCaloriePreset(
                QuickCaloriePresetEntity(
                    id = presetId,
                    name = input.name.trim(),
                    caloriesKcal = input.caloriesKcal,
                    proteinGrams = input.proteinGrams,
                    carbsGrams = input.carbsGrams,
                    fatGrams = input.fatGrams,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                    isFavorite = input.isFavorite,
                ),
            )
            presetId
        }
    }

    override suspend fun toggleFavoriteQuickLog(presetId: String, isFavorite: Boolean) {
        require(presetId.isNotBlank()) { "Quick log id is required" }
        database.withTransaction {
            val updatedCount = foodDao.updateQuickCaloriePresetFavorite(
                presetId = presetId,
                isFavorite = isFavorite,
                updatedAtEpochMillis = System.currentTimeMillis(),
            )
            check(updatedCount > 0) { "Quick log not found" }
        }
    }

    override suspend fun logFavoriteQuickLog(presetId: String, mealType: String, date: LocalDate): String {
        require(presetId.isNotBlank()) { "Quick log id is required" }
        val preset = foodDao.getQuickCaloriePreset(presetId) ?: error("Quick log not found")
        return quickLog(
            QuickCalorieLogInput(
                mealType = mealType,
                caloriesKcal = preset.caloriesKcal,
                proteinGrams = preset.proteinGrams,
                carbsGrams = preset.carbsGrams,
                fatGrams = preset.fatGrams,
                date = date,
            ),
        )
    }

    override suspend fun updateDiaryEntry(input: DiaryEntryUpdateInput) {
        input.requireValid()
        database.withTransaction {
            val existingItem = foodDao.getMealItem(input.mealItemId) ?: error("Diary item not found")
            val existingMeal = foodDao.getMeal(existingItem.mealId)
            val normalizedMealType = input.mealType.trim().ifBlank { DEFAULT_MEAL_TYPE }
            val dateEpochDay = input.date.toEpochDay()
            val targetMealId =
                if (existingMeal?.dateEpochDay == dateEpochDay && existingMeal.type == normalizedMealType) {
                    existingMeal.id
                } else {
                    val now = System.currentTimeMillis()
                    UUID.randomUUID().toString().also { newMealId ->
                        foodDao.upsertMeal(
                            MealEntity(
                                id = newMealId,
                                dateEpochDay = dateEpochDay,
                                type = normalizedMealType,
                                notes = null,
                                createdAtEpochMillis = now,
                                updatedAtEpochMillis = now,
                            ),
                        )
                    }
                }

            foodDao.upsertMealItem(existingItem.copy(mealId = targetMealId, quantityGrams = input.quantityGrams))
        }
    }

    override suspend fun deleteDiaryEntry(mealItemId: String) {
        require(mealItemId.isNotBlank()) { "Diary item id is required" }
        database.withTransaction {
            val deletedCount = foodDao.deleteMealItemById(mealItemId)
            check(deletedCount > 0) { "Diary item not found" }
        }
    }

    override suspend fun upsertSavedFood(input: SavedFoodUpsertInput): String {
        input.requireValid()
        return database.withTransaction {
            val now = System.currentTimeMillis()
            val normalizedBarcode = input.barcode?.trim()?.takeIf { it.isNotEmpty() }
            val resolvedBrand = input.brand?.trim()?.takeIf { it.isNotEmpty() }
            val existingByBarcode = normalizedBarcode?.let { foodDao.getFoodByBarcode(it) }
            val existingByName = foodDao.getFoodByNameAndBrand(input.name.trim(), resolvedBrand)
            val foodId =
                input.foodId?.trim()?.takeIf { it.isNotEmpty() }
                    ?: existingByBarcode?.id
                    ?: existingByName?.id
                    ?: UUID.randomUUID().toString()
            val existingFood = foodDao.getFood(foodId)

            foodDao.upsertFood(
                FoodEntity(
                    id = foodId,
                    name = input.name.trim(),
                    brand = resolvedBrand,
                    defaultServingGrams = input.defaultServingGrams,
                    caloriesPer100g = input.nutritionPer100g.caloriesKcal,
                    proteinPer100g = input.nutritionPer100g.proteinGrams,
                    carbsPer100g = input.nutritionPer100g.carbsGrams,
                    fatPer100g = input.nutritionPer100g.fatGrams,
                    createdAtEpochMillis = existingFood?.createdAtEpochMillis ?: now,
                    updatedAtEpochMillis = now,
                    servingName = input.servingName?.trim()?.takeIf { it.isNotEmpty() },
                    barcode = normalizedBarcode,
                    category = input.category?.trim()?.takeIf { it.isNotEmpty() },
                    isFavorite = input.isFavorite,
                    imageUrl = input.imageUrl ?: existingFood?.imageUrl,
                    fiberPer100g = input.nutritionDetailsPer100g.fiberGrams,
                    sugarPer100g = input.nutritionDetailsPer100g.sugarGrams,
                    saturatedFatPer100g = input.nutritionDetailsPer100g.saturatedFatGrams,
                    sodiumMgPer100g = input.nutritionDetailsPer100g.sodiumMilligrams,
                    potassiumMgPer100g = input.nutritionDetailsPer100g.potassiumMilligrams,
                    calciumMgPer100g = input.nutritionDetailsPer100g.calciumMilligrams,
                    ironMgPer100g = input.nutritionDetailsPer100g.ironMilligrams,
                    vitaminDMcgPer100g = input.nutritionDetailsPer100g.vitaminDMicrograms,
                    vitaminCMgPer100g = input.nutritionDetailsPer100g.vitaminCMilligrams,
                    magnesiumMgPer100g = input.nutritionDetailsPer100g.magnesiumMilligrams,
                ),
            )
            replaceServings(foodId, input.resolvedServings())

            foodId
        }
    }

    override suspend fun deleteSavedFood(foodId: String) {
        val trimmedFoodId = foodId.trim()
        require(trimmedFoodId.isNotBlank()) { "Food id is required" }
        database.withTransaction {
            val food = foodDao.getFood(trimmedFoodId) ?: error("Saved food not found")
            if (food.name == QUICK_CALORIES_NAME && food.brand == null) {
                error("Quick calories cannot be deleted from database")
            }
            if (foodDao.countMealItemsForFood(trimmedFoodId) > 0) {
                error("Food is used in diary entries")
            }

            foodDao.deleteFood(food)
        }
    }

    override suspend fun toggleFavoriteFood(foodId: String, isFavorite: Boolean) {
        require(foodId.isNotBlank()) { "Food id is required" }
        database.withTransaction {
            val food = foodDao.getFood(foodId) ?: error("Saved food not found")
            foodDao.upsertFood(food.copy(isFavorite = isFavorite, updatedAtEpochMillis = System.currentTimeMillis()))
        }
    }

    override suspend fun mergeDuplicateFoods(primaryFoodId: String, duplicateFoodIds: List<String>) {
        val trimmedPrimaryId = primaryFoodId.trim()
        val trimmedDuplicateIds = duplicateFoodIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        require(trimmedPrimaryId.isNotBlank()) { "Primary food id is required" }
        require(trimmedDuplicateIds.isNotEmpty()) { "Choose duplicate foods to merge" }
        require(trimmedPrimaryId !in trimmedDuplicateIds) { "Primary food cannot be merged into itself" }

        database.withTransaction {
            val primaryFood = foodDao.getFood(trimmedPrimaryId) ?: error("Primary food not found")
            val duplicates = trimmedDuplicateIds.map { duplicateId ->
                foodDao.getFood(duplicateId) ?: error("Duplicate food not found")
            }
            if (duplicates.isEmpty()) {
                return@withTransaction
            }

            foodDao.reassignMealItemsToFood(primaryFood.id, trimmedDuplicateIds)
            foodDao.reassignMealTemplateItemsToFood(primaryFood.id, trimmedDuplicateIds)
            foodDao.reassignRecipeIngredientsToFood(primaryFood.id, trimmedDuplicateIds)
            foodDao.reassignBarcodeProductsToFood(primaryFood.id, trimmedDuplicateIds)
            duplicates.forEach { duplicateFood ->
                foodDao.deleteFood(duplicateFood)
            }
        }
    }

    override fun observeFoodGoal(): Flow<FoodGoal> =
        foodDao.observeFoodGoal(DEFAULT_GOAL_ID).map { entity ->
            entity?.toFoodGoal() ?: DEFAULT_FOOD_GOAL
        }

    override suspend fun updateFoodGoal(goal: FoodGoal) {
        goal.requireValid()
        foodDao.upsertFoodGoal(goal.toEntity(System.currentTimeMillis()))
    }

    override fun observeWaterSummary(date: LocalDate): Flow<FoodWaterSummary> =
        combine(
            foodDao.observeWaterTotalForDate(date.toEpochDay()),
            observeFoodGoal(),
        ) { consumedMilliliters, goal ->
            FoodWaterSummary(
                date = date,
                consumedMilliliters = consumedMilliliters,
                goalMilliliters = goal.waterGoalMilliliters,
            )
        }

    override fun observeBurnedCalories(date: LocalDate): Flow<Double> =
        database.healthDao().observeDailySummary(date.toEpochDay())
            .map { summary -> summary?.activeCaloriesKcal ?: 0.0 }

    override suspend fun logWater(input: WaterLogInput): String {
        input.requireValid()
        return database.withTransaction {
            val entryId = UUID.randomUUID().toString()
            foodDao.insertWaterEntry(
                WaterEntryEntity(
                    id = entryId,
                    dateEpochDay = input.date.toEpochDay(),
                    amountMilliliters = input.amountMilliliters,
                    createdAtEpochMillis = System.currentTimeMillis(),
                ),
            )
            entryId
        }
    }

    override suspend fun updateWaterGoal(goalMilliliters: Double) {
        require(goalMilliliters.isFinite() && goalMilliliters > 0.0) { "Water goal must be positive" }
        database.withTransaction {
            val currentGoal = foodDao.getFoodGoal(DEFAULT_GOAL_ID)?.toFoodGoal() ?: DEFAULT_FOOD_GOAL
            foodDao.upsertFoodGoal(
                currentGoal
                    .copy(waterGoalMilliliters = goalMilliliters)
                    .toEntity(System.currentTimeMillis()),
            )
        }
    }

    override fun observeFoodHealthConnectSyncState(): Flow<FoodHealthConnectSyncState> =
        combine(
            foodDao.observeFoodHealthConnectSyncState(FOOD_HEALTH_CONNECT_SYNC_KEY),
            foodHealthConnectStatusFlow,
            foodHealthConnectPermissionsFlow,
        ) { entity, status, permissions ->
            entity.toFoodHealthConnectSyncState(status, permissions)
        }

    override suspend fun refreshFoodHealthConnectSyncState(): FoodHealthConnectSyncState {
        val status = runCatching { healthConnectGateway.status() }.getOrDefault(DEFAULT_HEALTH_CONNECT_STATUS)
        val permissions = if (status.availability == HealthConnectAvailability.Available) {
            runCatching { healthConnectGateway.foodRequestablePermissions() }.getOrDefault(emptySet())
        } else {
            emptySet()
        }
        foodHealthConnectStatusFlow.value = status
        foodHealthConnectPermissionsFlow.value = permissions
        return foodDao
            .getFoodHealthConnectSyncState(FOOD_HEALTH_CONNECT_SYNC_KEY)
            .toFoodHealthConnectSyncState(status, permissions)
    }

    override suspend fun setFoodHealthConnectSyncEnabled(isEnabled: Boolean) {
        val now = System.currentTimeMillis()
        val current = foodDao.getFoodHealthConnectSyncState(FOOD_HEALTH_CONNECT_SYNC_KEY)
        foodDao.upsertFoodHealthConnectSyncState(
            FoodHealthConnectSyncEntity(
                key = FOOD_HEALTH_CONNECT_SYNC_KEY,
                isEnabled = isEnabled,
                lastSyncAtEpochMillis = current?.lastSyncAtEpochMillis,
                lastFailureMessage = current?.lastFailureMessage,
                updatedAtEpochMillis = now,
            ),
        )
    }

    override suspend fun syncFoodToHealthConnect(date: LocalDate): FoodHealthConnectSyncResult {
        val state = refreshFoodHealthConnectSyncState()
        if (!state.isEnabled) {
            recordFoodHealthConnectSyncFailure("Enable Health Connect sync first")
            error("Enable Health Connect sync first")
        }
        if (state.availability != HealthConnectAvailability.Available) {
            recordFoodHealthConnectSyncFailure("Health Connect is not available")
            error("Health Connect is not available")
        }
        if (!state.canSync) {
            recordFoodHealthConnectSyncFailure("Check Health Connect nutrition and hydration permissions")
            error("Check Health Connect nutrition and hydration permissions")
        }

        return try {
            val diary = foodDao.getFoodDiaryEntryRowsForDate(date.toEpochDay()).toFoodDiary()
            val waterSummary = observeWaterSummary(date).first()
            val payload = HealthConnectFoodExportPayload(
                date = date,
                meals = diary.meals.mapNotNull { meal -> meal.toHealthConnectFoodMealExport() },
                hydrationMilliliters = waterSummary.consumedMilliliters,
            )
            val exportResult =
                healthConnectGateway.exportFood(payload)
                    ?: error("Check Health Connect nutrition and hydration permissions")
            val result = FoodHealthConnectSyncResult(
                nutritionRecordCount = exportResult.nutritionRecordCount,
                hydrationRecordCount = exportResult.hydrationRecordCount,
            )
            recordFoodHealthConnectSyncSuccess()
            result
        } catch (error: Exception) {
            recordFoodHealthConnectSyncFailure(
                error.message ?: "Failed to sync Food to Health Connect",
            )
            throw error
        }
    }

    private suspend fun recordFoodHealthConnectSyncSuccess() {
        val now = System.currentTimeMillis()
        val current = foodDao.getFoodHealthConnectSyncState(FOOD_HEALTH_CONNECT_SYNC_KEY)
        foodDao.upsertFoodHealthConnectSyncState(
            FoodHealthConnectSyncEntity(
                key = FOOD_HEALTH_CONNECT_SYNC_KEY,
                isEnabled = current?.isEnabled ?: false,
                lastSyncAtEpochMillis = now,
                lastFailureMessage = null,
                updatedAtEpochMillis = now,
            ),
        )
    }

    private suspend fun recordFoodHealthConnectSyncFailure(message: String) {
        val now = System.currentTimeMillis()
        val current = foodDao.getFoodHealthConnectSyncState(FOOD_HEALTH_CONNECT_SYNC_KEY)
        foodDao.upsertFoodHealthConnectSyncState(
            FoodHealthConnectSyncEntity(
                key = FOOD_HEALTH_CONNECT_SYNC_KEY,
                isEnabled = current?.isEnabled ?: false,
                lastSyncAtEpochMillis = current?.lastSyncAtEpochMillis,
                lastFailureMessage = message,
                updatedAtEpochMillis = now,
            ),
        )
    }

    override fun observeCustomMealDefinitions(): Flow<List<FoodMealDefinition>> =
        foodDao.observeMealDefinitions().map { definitions ->
            definitions.map { it.toFoodMealDefinition() }
        }

    override suspend fun upsertCustomMealDefinition(input: FoodMealDefinitionInput): String {
        input.requireValid()
        return database.withTransaction {
            val now = System.currentTimeMillis()
            val mealId =
                input.mealId
                    ?.normalizedMealDefinitionId()
                    ?.takeIf { it.isNotBlank() }
                    ?: input.name.generatedMealDefinitionId()
            val existing = foodDao.getMealDefinition(mealId)

            foodDao.upsertMealDefinition(
                MealDefinitionEntity(
                    id = mealId,
                    name = input.name.trim(),
                    timeMinutes = input.timeMinutes,
                    sortOrder = input.sortOrder,
                    createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
                    updatedAtEpochMillis = now,
                ),
            )
            mealId
        }
    }

    override fun observeMealTemplates(): Flow<List<MealTemplate>> =
        foodDao.observeMealTemplateRows().map { rows ->
            rows.toMealTemplates()
        }

    override suspend fun saveMealAsTemplate(date: LocalDate, mealType: String, name: String): String {
        require(name.isNotBlank()) { "Template name is required" }
        val normalizedMealType = mealType.trim().ifBlank { DEFAULT_MEAL_TYPE }
        return database.withTransaction {
            val rows = foodDao.getFoodDiaryEntryRowsForDateAndMeal(date.toEpochDay(), normalizedMealType)
            require(rows.isNotEmpty()) { "Meal has no food to save" }
            val now = System.currentTimeMillis()
            val templateId = UUID.randomUUID().toString()
            foodDao.upsertMealTemplate(
                MealTemplateEntity(
                    id = templateId,
                    name = name.trim(),
                    mealType = normalizedMealType,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                ),
            )
            rows.sortedWith(compareBy<FoodDiaryEntryRow> { it.createdAtEpochMillis }.thenBy { it.foodName })
                .forEachIndexed { index, row ->
                foodDao.upsertMealTemplateItem(
                    MealTemplateItemEntity(
                        id = UUID.randomUUID().toString(),
                        templateId = templateId,
                        foodId = row.foodId,
                        quantityGrams = row.quantityGrams,
                        sortOrder = index,
                    ),
                )
            }
            templateId
        }
    }

    override suspend fun logMealTemplate(templateId: String, mealType: String, date: LocalDate): List<String> {
        require(templateId.isNotBlank()) { "Template id is required" }
        val normalizedMealType = mealType.trim().ifBlank { DEFAULT_MEAL_TYPE }
        return database.withTransaction {
            val rows = foodDao.getMealTemplateRows(templateId)
            require(rows.isNotEmpty()) { "Template has no food" }
            val now = System.currentTimeMillis()
            rows.mapIndexed { index, row ->
                insertMealItem(
                    foodId = row.foodId,
                    mealType = normalizedMealType,
                    quantityGrams = row.quantityGrams,
                    date = date,
                    now = now + index,
                )
            }
        }
    }

    override suspend fun copyMeal(
        fromDate: LocalDate,
        toDate: LocalDate,
        mealType: String,
        status: FoodDiaryEntryStatus,
    ): List<String> {
        val normalizedMealType = mealType.trim().ifBlank { DEFAULT_MEAL_TYPE }
        return database.withTransaction {
            val rows = foodDao.getFoodDiaryEntryRowsForDateAndMeal(fromDate.toEpochDay(), normalizedMealType)
            require(rows.isNotEmpty()) { "Source meal has no food" }
            val now = System.currentTimeMillis()
            rows.sortedWith(compareBy<FoodDiaryEntryRow> { it.createdAtEpochMillis }.thenBy { it.foodName })
                .mapIndexed { index, row ->
                insertMealItem(
                    foodId = row.foodId,
                    mealType = normalizedMealType,
                    quantityGrams = row.quantityGrams,
                    date = toDate,
                    now = now + index,
                    status = status,
                )
            }
        }
    }

    override suspend fun copyDay(fromDate: LocalDate, toDate: LocalDate, status: FoodDiaryEntryStatus): List<String> =
        database.withTransaction {
            val rows = foodDao.getFoodDiaryEntryRowsForDate(fromDate.toEpochDay())
            require(rows.isNotEmpty()) { "Source day has no food" }
            val now = System.currentTimeMillis()
            rows.sortedWith(compareBy<FoodDiaryEntryRow> { it.createdAtEpochMillis }.thenBy { it.foodName })
                .mapIndexed { index, row ->
                    insertMealItem(
                        foodId = row.foodId,
                        mealType = row.mealType,
                        quantityGrams = row.quantityGrams,
                        date = toDate,
                        now = now + index,
                        status = status,
                    )
                }
        }

    override suspend fun renameMealTemplate(templateId: String, name: String, mealType: String) {
        require(templateId.isNotBlank()) { "Template id is required" }
        require(name.isNotBlank()) { "Template name is required" }
        database.withTransaction {
            val updatedCount = foodDao.updateMealTemplateMetadata(
                templateId = templateId,
                name = name.trim(),
                mealType = mealType.trim().ifBlank { DEFAULT_MEAL_TYPE },
                updatedAtEpochMillis = System.currentTimeMillis(),
            )
            check(updatedCount > 0) { "Template not found" }
        }
    }

    override suspend fun updateMealTemplate(input: MealTemplateUpdateInput) {
        input.requireValid()
        database.withTransaction {
            foodDao.getMealTemplate(input.templateId) ?: error("Template not found")
            val now = System.currentTimeMillis()
            val updatedCount = foodDao.updateMealTemplateMetadata(
                templateId = input.templateId,
                name = input.name.trim(),
                mealType = input.mealType.trim().ifBlank { DEFAULT_MEAL_TYPE },
                updatedAtEpochMillis = now,
            )
            check(updatedCount > 0) { "Template not found" }
            foodDao.deleteMealTemplateItems(input.templateId)
            input.items.forEachIndexed { index, item ->
                foodDao.getFood(item.foodId) ?: error("Template item food not found")
                foodDao.upsertMealTemplateItem(
                    MealTemplateItemEntity(
                        id = UUID.randomUUID().toString(),
                        templateId = input.templateId,
                        foodId = item.foodId,
                        quantityGrams = item.quantityGrams,
                        sortOrder = index,
                    ),
                )
            }
        }
    }

    override suspend fun duplicateMealTemplate(templateId: String, name: String): String {
        require(templateId.isNotBlank()) { "Template id is required" }
        require(name.isNotBlank()) { "Template name is required" }
        return database.withTransaction {
            val source = foodDao.getMealTemplate(templateId) ?: error("Template not found")
            val rows = foodDao.getMealTemplateRows(templateId)
            require(rows.isNotEmpty()) { "Template has no food" }
            val now = System.currentTimeMillis()
            val duplicateId = UUID.randomUUID().toString()
            foodDao.upsertMealTemplate(
                MealTemplateEntity(
                    id = duplicateId,
                    name = name.trim(),
                    mealType = source.mealType,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                    isFavorite = source.isFavorite,
                ),
            )
            rows.sortedBy { it.sortOrder }.forEachIndexed { index, row ->
                foodDao.upsertMealTemplateItem(
                    MealTemplateItemEntity(
                        id = UUID.randomUUID().toString(),
                        templateId = duplicateId,
                        foodId = row.foodId,
                        quantityGrams = row.quantityGrams,
                        sortOrder = index,
                    ),
                )
            }
            duplicateId
        }
    }

    override suspend fun deleteMealTemplate(templateId: String) {
        require(templateId.isNotBlank()) { "Template id is required" }
        database.withTransaction {
            val deletedCount = foodDao.deleteMealTemplateById(templateId)
            check(deletedCount > 0) { "Template not found" }
        }
    }

    override suspend fun toggleFavoriteMealTemplate(templateId: String, isFavorite: Boolean) {
        require(templateId.isNotBlank()) { "Template id is required" }
        database.withTransaction {
            val updatedCount = foodDao.updateMealTemplateFavorite(
                templateId = templateId,
                isFavorite = isFavorite,
                updatedAtEpochMillis = System.currentTimeMillis(),
            )
            check(updatedCount > 0) { "Template not found" }
        }
    }

    override suspend fun copyDiaryEntry(mealItemId: String, mealType: String, date: LocalDate): String {
        require(mealItemId.isNotBlank()) { "Diary item id is required" }
        return database.withTransaction {
            val item = foodDao.getMealItem(mealItemId) ?: error("Diary item not found")
            insertMealItem(
                foodId = item.foodId,
                mealType = mealType,
                quantityGrams = item.quantityGrams,
                date = date,
                now = System.currentTimeMillis(),
            )
        }
    }

    override suspend fun markDiaryEntryLogged(mealItemId: String) {
        require(mealItemId.isNotBlank()) { "Diary item id is required" }
        database.withTransaction {
            val updatedCount = foodDao.updateMealItemStatus(mealItemId, FoodDiaryEntryStatus.Logged.asStorageValue())
            check(updatedCount > 0) { "Diary item not found" }
        }
    }

    override fun observeShoppingList(): Flow<List<ShoppingListGroup>> =
        foodDao.observeShoppingListItems().map { items -> items.toShoppingListGroups() }

    override suspend fun generateShoppingList(startDate: LocalDate, endDate: LocalDate): List<ShoppingListGroup> {
        require(!endDate.isBefore(startDate)) { "End date must be on or after start date" }
        return database.withTransaction {
            val plannedRows =
                foodDao.getFoodDiaryEntryRowsForDateRange(startDate.toEpochDay(), endDate.toEpochDay())
                    .filter { row -> row.status.toFoodDiaryEntryStatus() == FoodDiaryEntryStatus.Planned }
            val generatedRows = plannedRows.toShoppingListGeneratedRows()
            val generatedByKey = generatedRows.associateBy { row -> row.sourceKey }
            val existingGenerated = foodDao.getGeneratedShoppingListItems().associateBy { item -> item.sourceKey }
            val now = System.currentTimeMillis()

            generatedRows
                .sortedWith(compareBy<ShoppingListGeneratedRow> { it.category.lowercase(Locale.US) }.thenBy { it.name.lowercase(Locale.US) })
                .forEachIndexed { index, row ->
                    val existing = existingGenerated[row.sourceKey]
                    foodDao.upsertShoppingListItem(
                        ShoppingListItemEntity(
                            id = existing?.id ?: UUID.randomUUID().toString(),
                            name = row.name,
                            category = row.category,
                            quantityGrams = row.quantityGrams,
                            isChecked = existing?.isChecked ?: false,
                            isManual = false,
                            sourceKey = row.sourceKey,
                            sortOrder = index,
                            createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
                            updatedAtEpochMillis = now,
                        ),
                    )
                }

            existingGenerated
                .filterKeys { sourceKey -> sourceKey !in generatedByKey.keys }
                .values
                .forEach { staleItem -> foodDao.deleteShoppingListItemById(staleItem.id) }

            foodDao.getShoppingListItems().toShoppingListGroups()
        }
    }

    override suspend fun addManualShoppingListItem(input: ManualShoppingListItemInput): String {
        input.requireValid()
        return database.withTransaction {
            val now = System.currentTimeMillis()
            val itemId = UUID.randomUUID().toString()
            foodDao.upsertShoppingListItem(
                ShoppingListItemEntity(
                    id = itemId,
                    name = input.name.trim(),
                    category = input.category.normalizedShoppingCategory(),
                    quantityGrams = input.quantityGrams,
                    isChecked = false,
                    isManual = true,
                    sourceKey = null,
                    sortOrder = Int.MAX_VALUE,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                ),
            )
            itemId
        }
    }

    override suspend fun toggleShoppingListItem(itemId: String, isChecked: Boolean) {
        require(itemId.isNotBlank()) { "Shopping item id is required" }
        database.withTransaction {
            val updatedCount = foodDao.updateShoppingListItemChecked(itemId, isChecked, System.currentTimeMillis())
            check(updatedCount > 0) { "Shopping item not found" }
        }
    }

    override fun observeRecipes(): Flow<List<Recipe>> =
        foodDao.observeRecipeRows().map { rows ->
            rows.toRecipes()
        }

    override suspend fun upsertRecipe(input: RecipeUpsertInput): String {
        input.requireValid()
        return database.withTransaction {
            val now = System.currentTimeMillis()
            val recipeId = input.recipeId?.trim()?.takeIf { it.isNotEmpty() } ?: UUID.randomUUID().toString()
            val existing = foodDao.getRecipe(recipeId)
            foodDao.upsertRecipe(
                RecipeEntity(
                    id = recipeId,
                    name = input.name.trim(),
                    category = input.category?.trim()?.takeIf { it.isNotEmpty() },
                    servingName = input.servingName.trim().ifBlank { "Serving" },
                    servingGrams = input.servingGrams,
                    servings = input.servings,
                    cookedYieldGrams = input.cookedYieldGrams,
                    createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
                    updatedAtEpochMillis = now,
                    isFavorite = existing?.isFavorite ?: false,
                ),
            )
            foodDao.deleteRecipeIngredients(recipeId)
            input.ingredients.forEachIndexed { index, ingredient ->
                foodDao.getFood(ingredient.foodId) ?: error("Ingredient food not found")
                foodDao.upsertRecipeIngredient(
                    RecipeIngredientEntity(
                        id = UUID.randomUUID().toString(),
                        recipeId = recipeId,
                        foodId = ingredient.foodId,
                        quantityGrams = ingredient.quantityGrams,
                        unitLabel = ingredient.unitLabel.trim().ifBlank { "g" },
                        unitGrams = ingredient.unitGrams,
                        unitQuantity = ingredient.unitQuantity,
                        sortOrder = index,
                    ),
                )
            }
            recipeId
        }
    }

    override suspend fun logRecipe(recipeId: String, mealType: String, servings: Double, date: LocalDate): String =
        insertRecipeMealItem(
            recipeId = recipeId,
            mealType = mealType,
            servings = servings,
            date = date,
            status = FoodDiaryEntryStatus.Logged,
        )

    override suspend fun planRecipe(recipeId: String, mealType: String, servings: Double, date: LocalDate): String =
        insertRecipeMealItem(
            recipeId = recipeId,
            mealType = mealType,
            servings = servings,
            date = date,
            status = FoodDiaryEntryStatus.Planned,
        )

    private suspend fun insertRecipeMealItem(
        recipeId: String,
        mealType: String,
        servings: Double,
        date: LocalDate,
        status: FoodDiaryEntryStatus,
    ): String {
        require(recipeId.isNotBlank()) { "Recipe id is required" }
        require(servings.isFinite() && servings > 0.0) { "Servings must be positive" }
        return database.withTransaction {
            val recipe = foodDao.getRecipe(recipeId) ?: error("Recipe not found")
            val rows = foodDao.getRecipeRows(recipeId)
            require(rows.isNotEmpty()) { "Recipe has no ingredients" }
            val fullRecipeGrams = recipe.resolvedCookedYieldGrams(rows)
            val per100gNutrition = rows.calculateRecipeNutritionPer100g(fullRecipeGrams)
            val per100gDetails = rows.calculateRecipeDetailsPer100g(fullRecipeGrams)
            val now = System.currentTimeMillis()
            val foodId = UUID.randomUUID().toString()
            foodDao.upsertFood(
                FoodEntity(
                    id = foodId,
                    name = recipe.name,
                    brand = RECIPE_BRAND,
                    defaultServingGrams = recipe.servingGrams,
                    caloriesPer100g = per100gNutrition.caloriesKcal,
                    proteinPer100g = per100gNutrition.proteinGrams,
                    carbsPer100g = per100gNutrition.carbsGrams,
                    fatPer100g = per100gNutrition.fatGrams,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                    servingName = recipe.servingName,
                    category = recipe.category,
                    fiberPer100g = per100gDetails.fiberGrams,
                    sugarPer100g = per100gDetails.sugarGrams,
                    saturatedFatPer100g = per100gDetails.saturatedFatGrams,
                    sodiumMgPer100g = per100gDetails.sodiumMilligrams,
                    potassiumMgPer100g = per100gDetails.potassiumMilligrams,
                    calciumMgPer100g = per100gDetails.calciumMilligrams,
                    ironMgPer100g = per100gDetails.ironMilligrams,
                    vitaminDMcgPer100g = per100gDetails.vitaminDMicrograms,
                    vitaminCMgPer100g = per100gDetails.vitaminCMilligrams,
                    magnesiumMgPer100g = per100gDetails.magnesiumMilligrams,
                ),
            )
            replaceServings(foodId, listOf(FoodServingInput(recipe.servingName, recipe.servingGrams)))
            insertMealItem(
                foodId = foodId,
                mealType = mealType,
                quantityGrams = recipe.servingGrams * servings,
                date = date,
                now = now,
                status = status,
            )
        }
    }

    override suspend fun duplicateRecipe(recipeId: String, name: String): String {
        require(recipeId.isNotBlank()) { "Recipe id is required" }
        require(name.isNotBlank()) { "Recipe name is required" }
        return database.withTransaction {
            val source = foodDao.getRecipe(recipeId) ?: error("Recipe not found")
            val rows = foodDao.getRecipeRows(recipeId)
            require(rows.isNotEmpty()) { "Recipe has no ingredients" }
            val now = System.currentTimeMillis()
            val duplicateId = UUID.randomUUID().toString()
            foodDao.upsertRecipe(
                RecipeEntity(
                    id = duplicateId,
                    name = name.trim(),
                    category = source.category,
                    servingName = source.servingName,
                    servingGrams = source.servingGrams,
                    servings = source.servings,
                    cookedYieldGrams = source.cookedYieldGrams,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                    isFavorite = source.isFavorite,
                ),
            )
            rows.sortedBy { it.sortOrder }.forEachIndexed { index, row ->
                foodDao.upsertRecipeIngredient(
                    RecipeIngredientEntity(
                        id = UUID.randomUUID().toString(),
                        recipeId = duplicateId,
                        foodId = row.foodId,
                        quantityGrams = row.quantityGrams,
                        unitLabel = row.unitLabel,
                        unitGrams = row.unitGrams,
                        unitQuantity = row.unitQuantity,
                        sortOrder = index,
                    ),
                )
            }
            duplicateId
        }
    }

    override suspend fun deleteRecipe(recipeId: String) {
        require(recipeId.isNotBlank()) { "Recipe id is required" }
        database.withTransaction {
            val deletedCount = foodDao.deleteRecipeById(recipeId)
            check(deletedCount > 0) { "Recipe not found" }
        }
    }

    override suspend fun toggleFavoriteRecipe(recipeId: String, isFavorite: Boolean) {
        require(recipeId.isNotBlank()) { "Recipe id is required" }
        database.withTransaction {
            val updatedCount = foodDao.updateRecipeFavorite(
                recipeId = recipeId,
                isFavorite = isFavorite,
                updatedAtEpochMillis = System.currentTimeMillis(),
            )
            check(updatedCount > 0) { "Recipe not found" }
        }
    }

    override suspend fun seedStarterFoods() {
        database.withTransaction {
            STARTER_FOODS.forEach { input ->
                upsertSavedFood(input)
            }
        }
    }

    private suspend fun upsertManualFood(input: FoodLogInput, now: Long): String {
        val foodId = UUID.randomUUID().toString()
        val servingGrams = input.servingGrams ?: input.quantityGrams
        val resolvedBrand = input.brand?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedBarcode = input.barcode?.trim()?.takeIf { it.isNotEmpty() }

        foodDao.upsertFood(
            FoodEntity(
                id = foodId,
                name = input.name.trim(),
                brand = resolvedBrand,
                defaultServingGrams = servingGrams,
                caloriesPer100g = input.nutritionPer100g.caloriesKcal,
                proteinPer100g = input.nutritionPer100g.proteinGrams,
                carbsPer100g = input.nutritionPer100g.carbsGrams,
                fatPer100g = input.nutritionPer100g.fatGrams,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
                servingName = servingLabel(servingGrams),
                barcode = normalizedBarcode,
                imageUrl = input.lookupResult?.imageUrl?.trim()?.takeIf { it.isNotEmpty() },
                fiberPer100g = input.nutritionDetailsPer100g.fiberGrams,
                sugarPer100g = input.nutritionDetailsPer100g.sugarGrams,
                saturatedFatPer100g = input.nutritionDetailsPer100g.saturatedFatGrams,
                sodiumMgPer100g = input.nutritionDetailsPer100g.sodiumMilligrams,
                potassiumMgPer100g = input.nutritionDetailsPer100g.potassiumMilligrams,
                calciumMgPer100g = input.nutritionDetailsPer100g.calciumMilligrams,
                ironMgPer100g = input.nutritionDetailsPer100g.ironMilligrams,
                vitaminDMcgPer100g = input.nutritionDetailsPer100g.vitaminDMicrograms,
                vitaminCMgPer100g = input.nutritionDetailsPer100g.vitaminCMilligrams,
                magnesiumMgPer100g = input.nutritionDetailsPer100g.magnesiumMilligrams,
            ),
        )
        replaceServings(foodId, listOf(FoodServingInput(servingLabel(servingGrams), servingGrams)))
        return foodId
    }

    private suspend fun insertMealItem(
        foodId: String,
        mealType: String,
        quantityGrams: Double,
        date: LocalDate,
        now: Long,
        status: FoodDiaryEntryStatus = FoodDiaryEntryStatus.Logged,
    ): String {
        val mealId = UUID.randomUUID().toString()
        val mealItemId = UUID.randomUUID().toString()

        foodDao.upsertMeal(
            MealEntity(
                id = mealId,
                dateEpochDay = date.toEpochDay(),
                type = mealType.trim().ifBlank { DEFAULT_MEAL_TYPE },
                notes = null,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
            ),
        )
        foodDao.upsertMealItem(
            MealItemEntity(
                id = mealItemId,
                mealId = mealId,
                foodId = foodId,
                quantityGrams = quantityGrams,
                status = status.asStorageValue(),
            ),
        )

        return mealItemId
    }

    private suspend fun List<FoodDiaryEntryRow>.toShoppingListGeneratedRows(): List<ShoppingListGeneratedRow> {
        val generatedBySourceKey = linkedMapOf<String, ShoppingListGeneratedRow>()

        suspend fun addFood(foodId: String, name: String, category: String?, quantityGrams: Double) {
            val sourceKey = "food:$foodId"
            val normalizedCategory = category.normalizedShoppingCategory()
            val existing = generatedBySourceKey[sourceKey]
            generatedBySourceKey[sourceKey] =
                if (existing == null) {
                    ShoppingListGeneratedRow(
                        sourceKey = sourceKey,
                        name = name,
                        category = normalizedCategory,
                        quantityGrams = quantityGrams,
                    )
                } else {
                    existing.copy(quantityGrams = existing.quantityGrams + quantityGrams)
                }
        }

        forEach { row ->
            val recipe = if (row.brand == RECIPE_BRAND) foodDao.getRecipeByName(row.foodName) else null
            val recipeRows = recipe?.let { foodDao.getRecipeRows(it.id) }.orEmpty()
            if (recipe != null && recipeRows.isNotEmpty()) {
                val scale = row.quantityGrams / recipe.resolvedCookedYieldGrams(recipeRows)
                recipeRows.forEach { ingredient ->
                    addFood(
                        foodId = ingredient.foodId,
                        name = ingredient.foodName,
                        category = ingredient.foodCategory,
                        quantityGrams = ingredient.quantityGrams * scale,
                    )
                }
            } else {
                addFood(
                    foodId = row.foodId,
                    name = row.foodName,
                    category = row.foodCategory,
                    quantityGrams = row.quantityGrams,
                )
            }
        }

        return generatedBySourceKey.values.toList()
    }

    private suspend fun upsertConfirmedFood(
        result: ProductLookupResult.Found,
        editedName: String,
        editedBrand: String?,
        editedNutrition: FoodNutrition,
        editedNutritionDetails: NutritionDetails,
        servingGrams: Double,
        category: String?,
        now: Long,
    ): String {
        val existingBarcodeProduct = foodDao.getBarcodeProduct(result.barcode)
        val existingFood = existingBarcodeProduct?.linkedFoodId?.let { linkedFoodId ->
            foodDao.getFood(linkedFoodId)
        }
        val resolvedName = editedName.ifBlank { result.name }
        val resolvedBrand = editedBrand?.trim()?.takeIf { it.isNotEmpty() }
        val shouldReuseExistingFood =
            existingFood?.matchesLocalSnapshot(
                name = resolvedName,
                brand = resolvedBrand,
                servingGrams = servingGrams,
                nutrition = editedNutrition,
            ) == true
        val foodId = if (shouldReuseExistingFood) existingFood.id else UUID.randomUUID().toString()

        foodDao.upsertFood(
            FoodEntity(
                id = foodId,
                name = resolvedName,
                brand = resolvedBrand,
                defaultServingGrams = servingGrams,
                caloriesPer100g = editedNutrition.caloriesKcal,
                proteinPer100g = editedNutrition.proteinGrams,
                carbsPer100g = editedNutrition.carbsGrams,
                fatPer100g = editedNutrition.fatGrams,
                createdAtEpochMillis = existingFood?.takeIf { shouldReuseExistingFood }?.createdAtEpochMillis ?: now,
                updatedAtEpochMillis = now,
                servingName = servingLabel(servingGrams),
                barcode = result.barcode,
                imageUrl = result.imageUrl,
                category = category?.trim()?.takeIf { it.isNotEmpty() },
                fiberPer100g = editedNutritionDetails.fiberGrams,
                sugarPer100g = editedNutritionDetails.sugarGrams,
                saturatedFatPer100g = editedNutritionDetails.saturatedFatGrams,
                sodiumMgPer100g = editedNutritionDetails.sodiumMilligrams,
                potassiumMgPer100g = editedNutritionDetails.potassiumMilligrams,
                calciumMgPer100g = editedNutritionDetails.calciumMilligrams,
                ironMgPer100g = editedNutritionDetails.ironMilligrams,
                vitaminDMcgPer100g = editedNutritionDetails.vitaminDMicrograms,
                vitaminCMgPer100g = editedNutritionDetails.vitaminCMilligrams,
                magnesiumMgPer100g = editedNutritionDetails.magnesiumMilligrams,
            ),
        )
        replaceServings(foodId, listOf(FoodServingInput(servingLabel(servingGrams), servingGrams)))
        foodDao.upsertBarcodeProduct(
            BarcodeProductEntity(
                id = existingBarcodeProduct?.id ?: UUID.randomUUID().toString(),
                barcode = result.barcode,
                provider = OPEN_FOOD_FACTS_PROVIDER,
                providerProductName = result.name,
                providerBrand = result.brand,
                rawJson = result.rawJson,
                quality = result.quality.asStorageValue(),
                linkedFoodId = foodId,
                fetchedAtEpochMillis = now,
            ),
        )

        return foodId
    }

    private suspend fun replaceServings(foodId: String, servings: List<FoodServingInput>) {
        foodDao.deleteServingsForFood(foodId)
        servings.forEachIndexed { index, serving ->
            foodDao.upsertServing(
                FoodServingEntity(
                    id = "$foodId:serving:$index",
                    foodId = foodId,
                    label = serving.label.trim().ifBlank { servingLabel(serving.grams) },
                    grams = serving.grams,
                ),
            )
        }
    }

    private fun servingLabel(servingGrams: Double): String {
        val rounded = servingGrams.toLong()
        return if (servingGrams == rounded.toDouble()) {
            "$rounded g"
        } else {
            "${String.format(Locale.US, "%.1f", servingGrams)} g"
        }
    }

    private fun ProductDataQuality.asStorageValue(): String =
        when (this) {
            ProductDataQuality.Complete -> "complete"
            ProductDataQuality.Incomplete -> "incomplete"
        }

    private fun FoodEntity.matchesLocalSnapshot(
        name: String,
        brand: String?,
        servingGrams: Double,
        nutrition: FoodNutrition,
    ): Boolean =
        this.name == name &&
            this.brand == brand &&
            defaultServingGrams == servingGrams &&
            caloriesPer100g == nutrition.caloriesKcal &&
            proteinPer100g == nutrition.proteinGrams &&
            carbsPer100g == nutrition.carbsGrams &&
            fatPer100g == nutrition.fatGrams

    private companion object {
        const val OPEN_FOOD_FACTS_PROVIDER = "open_food_facts"
        const val DEFAULT_MEAL_TYPE = "meal"
        const val QUICK_CALORIES_NAME = "Quick calories"
        const val DEFAULT_GOAL_ID = "default"
        const val FOOD_HEALTH_CONNECT_SYNC_KEY = "food"
        const val RECIPE_BRAND = "Recipe"
        val DEFAULT_HEALTH_CONNECT_STATUS =
            HealthConnectStatus(
                availability = HealthConnectAvailability.NotSupported,
                grantedPermissions = emptySet(),
            )

        val DEFAULT_FOOD_GOAL =
            FoodGoal(
                dailyCaloriesKcal = 2083.0,
                proteinGrams = 104.0,
                carbsGrams = 260.0,
                fatGrams = 69.0,
                fiberGrams = 30.0,
                sugarGrams = 50.0,
                saturatedFatGrams = 20.0,
                sodiumMilligrams = 2300.0,
                mode = FoodGoalMode.Balanced,
                includeTrainingCalories = false,
                useNetCarbs = false,
                waterGoalMilliliters = DEFAULT_WATER_GOAL_MILLILITERS,
            )

        val STARTER_FOODS =
            listOf(
                SavedFoodUpsertInput(
                    foodId = null,
                    name = "Banana",
                    brand = null,
                    defaultServingGrams = 118.0,
                    nutritionPer100g = FoodNutrition(89.0, 1.1, 23.0, 0.3),
                    nutritionDetailsPer100g = NutritionDetails(fiberGrams = 2.6, sugarGrams = 12.2),
                    servingName = "Medium",
                    category = "Fruit",
                    servings = listOf(FoodServingInput("Medium", 118.0), FoodServingInput("Small", 101.0)),
                ),
                SavedFoodUpsertInput(
                    foodId = null,
                    name = "Rolled oats",
                    brand = null,
                    defaultServingGrams = 40.0,
                    nutritionPer100g = FoodNutrition(389.0, 16.9, 66.3, 6.9),
                    nutritionDetailsPer100g = NutritionDetails(fiberGrams = 10.6, sugarGrams = 0.9),
                    servingName = "Bowl",
                    category = "Grains",
                    servings = listOf(FoodServingInput("Bowl", 40.0), FoodServingInput("100 g", 100.0)),
                ),
                SavedFoodUpsertInput(
                    foodId = null,
                    name = "Chicken breast",
                    brand = null,
                    defaultServingGrams = 150.0,
                    nutritionPer100g = FoodNutrition(165.0, 31.0, 0.0, 3.6),
                    nutritionDetailsPer100g = NutritionDetails(saturatedFatGrams = 1.0, sodiumMilligrams = 74.0),
                    servingName = "Fillet",
                    category = "Protein",
                    servings = listOf(FoodServingInput("Fillet", 150.0), FoodServingInput("100 g", 100.0)),
                ),
                SavedFoodUpsertInput(
                    foodId = null,
                    name = "Cooked white rice",
                    brand = null,
                    defaultServingGrams = 150.0,
                    nutritionPer100g = FoodNutrition(130.0, 2.7, 28.0, 0.3),
                    nutritionDetailsPer100g = NutritionDetails(fiberGrams = 0.4, sugarGrams = 0.1),
                    servingName = "Cup",
                    category = "Grains",
                    servings = listOf(FoodServingInput("Cup", 150.0), FoodServingInput("100 g", 100.0)),
                ),
            )
    }
}

private fun FoodHealthConnectSyncEntity?.toFoodHealthConnectSyncState(
    status: HealthConnectStatus,
    requestablePermissions: Set<String>,
): FoodHealthConnectSyncState {
    val grantedPermissionCount = requestablePermissions.count { permission ->
        permission in status.grantedPermissions
    }
    val isEnabled = this?.isEnabled ?: false
    return FoodHealthConnectSyncState(
        isEnabled = isEnabled,
        availability = status.availability,
        grantedPermissionCount = grantedPermissionCount,
        requestablePermissionCount = requestablePermissions.size,
        requestablePermissions = requestablePermissions,
        canRequestPermissions = status.availability == HealthConnectAvailability.Available &&
            requestablePermissions.isNotEmpty() &&
            grantedPermissionCount < requestablePermissions.size,
        canSync = isEnabled &&
            status.availability == HealthConnectAvailability.Available &&
            requestablePermissions.isNotEmpty() &&
            grantedPermissionCount == requestablePermissions.size,
        lastSyncAtEpochMillis = this?.lastSyncAtEpochMillis,
        lastFailureMessage = this?.lastFailureMessage,
    )
}

private fun FoodDiaryMeal.toHealthConnectFoodMealExport(): HealthConnectFoodMealExport? {
    val hasLoggedNutrition =
        entries.any { entry -> entry.status == FoodDiaryEntryStatus.Logged } &&
            (totals.caloriesKcal > 0.0 || totals.proteinGrams > 0.0 || totals.carbsGrams > 0.0 || totals.fatGrams > 0.0)
    if (!hasLoggedNutrition) {
        return null
    }
    return HealthConnectFoodMealExport(
        mealType = type,
        name = type.toHealthConnectMealName(),
        caloriesKcal = totals.caloriesKcal,
        proteinGrams = totals.proteinGrams,
        carbsGrams = totals.carbsGrams,
        fatGrams = totals.fatGrams,
        fiberGrams = detailTotals.fiberGrams,
        sugarGrams = detailTotals.sugarGrams,
        saturatedFatGrams = detailTotals.saturatedFatGrams,
        sodiumMilligrams = detailTotals.sodiumMilligrams,
        potassiumMilligrams = detailTotals.potassiumMilligrams,
        calciumMilligrams = detailTotals.calciumMilligrams,
        ironMilligrams = detailTotals.ironMilligrams,
        vitaminDMicrograms = detailTotals.vitaminDMicrograms,
        vitaminCMilligrams = detailTotals.vitaminCMilligrams,
        magnesiumMilligrams = detailTotals.magnesiumMilligrams,
    )
}

private fun String.toHealthConnectMealName(): String =
    split('-', '_', ' ')
        .filter { part -> part.isNotBlank() }
        .joinToString(" ") { part ->
            part.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
            }
        }
        .ifBlank { "Meal" }

private object NoopHealthConnectGateway : HealthConnectGateway {
    override suspend fun status(): HealthConnectStatus = HealthConnectStatus(
        availability = HealthConnectAvailability.NotSupported,
        grantedPermissions = emptySet(),
    )

    override suspend fun requestablePermissions(): Set<String> = emptySet()

    override suspend fun foodRequestablePermissions(): Set<String> = emptySet()

    override suspend fun readDailySummary(date: LocalDate): ImportedDailyHealthSummary =
        ImportedDailyHealthSummary()

    override suspend fun exportWorkout(
        session: com.musfit.data.local.entity.WorkoutSessionEntity,
        sets: List<com.musfit.data.local.entity.WorkoutSetEntity>,
    ): String? = null

    override suspend fun exportFood(payload: HealthConnectFoodExportPayload): HealthConnectFoodExportResult? = null
}

private fun FoodLogInput.requireValid() {
    require(name.isNotBlank()) { "Food name is required" }
    require(mealType.isNotBlank()) { "Meal type is required" }
    require(quantityGrams.isFinite() && quantityGrams > 0.0) { "Quantity must be positive" }
    servingGrams?.let {
        require(it.isFinite() && it > 0.0) { "Serving size must be positive" }
    }
    nutritionPer100g.requireValid()
}

private fun SavedFoodLogInput.requireValid() {
    require(foodId.isNotBlank()) { "Food id is required" }
    require(mealType.isNotBlank()) { "Meal type is required" }
    require(quantityGrams.isFinite() && quantityGrams > 0.0) { "Quantity must be positive" }
}

private fun QuickCalorieLogInput.requireValid() {
    require(mealType.isNotBlank()) { "Meal type is required" }
    require(caloriesKcal.isNonNegativeFinite())
    require(proteinGrams.isNonNegativeFinite())
    require(carbsGrams.isNonNegativeFinite())
    require(fatGrams.isNonNegativeFinite())
}

private fun QuickCaloriePresetInput.requireValid() {
    require(name.isNotBlank()) { "Quick log name is required" }
    require(caloriesKcal.isFinite() && caloriesKcal > 0.0) { "Quick calories must be positive" }
    require(proteinGrams.isNonNegativeFinite())
    require(carbsGrams.isNonNegativeFinite())
    require(fatGrams.isNonNegativeFinite())
}

private fun FoodMealDefinitionInput.requireValid() {
    require(name.isNotBlank()) { "Meal name is required" }
    timeMinutes?.let { minutes ->
        require(minutes in 0..1439) { "Meal time must be within the day" }
    }
    require(sortOrder >= 0) { "Meal order must be zero or greater" }
}

private fun DiaryEntryUpdateInput.requireValid() {
    require(mealItemId.isNotBlank()) { "Diary item id is required" }
    require(mealType.isNotBlank()) { "Meal type is required" }
    require(quantityGrams.isFinite() && quantityGrams > 0.0) { "Quantity must be positive" }
}

private fun SavedFoodUpsertInput.requireValid() {
    require(name.isNotBlank()) { "Food name is required" }
    require(defaultServingGrams.isFinite() && defaultServingGrams > 0.0) { "Serving size must be positive" }
    nutritionPer100g.requireValid()
    nutritionDetailsPer100g.requireValid()
    servings.forEach { serving ->
        require(serving.label.isNotBlank()) { "Serving label is required" }
        require(serving.grams.isFinite() && serving.grams > 0.0) { "Serving grams must be positive" }
    }
}

private fun FoodGoal.requireValid() {
    require(dailyCaloriesKcal.isFinite() && dailyCaloriesKcal > 0.0) { "Calories goal must be positive" }
    require(proteinGrams.isNonNegativeFinite())
    require(carbsGrams.isNonNegativeFinite())
    require(fatGrams.isNonNegativeFinite())
    require(fiberGrams.isNonNegativeFinite())
    require(sugarGrams.isNonNegativeFinite())
    require(saturatedFatGrams.isNonNegativeFinite())
    require(sodiumMilligrams.isNonNegativeFinite())
    require(waterGoalMilliliters.isFinite() && waterGoalMilliliters > 0.0) { "Water goal must be positive" }
}

private fun MealTemplateUpdateInput.requireValid() {
    require(templateId.isNotBlank()) { "Template id is required" }
    require(name.isNotBlank()) { "Template name is required" }
    require(items.isNotEmpty()) { "Template needs at least one food" }
    items.forEach { item ->
        require(item.foodId.isNotBlank()) { "Template item food is required" }
        require(item.quantityGrams.isFinite() && item.quantityGrams > 0.0) { "Template item quantity must be positive" }
    }
}

private fun ManualShoppingListItemInput.requireValid() {
    require(name.isNotBlank()) { "Shopping item name is required" }
    require(quantityGrams.isFinite() && quantityGrams > 0.0) { "Shopping item amount must be positive" }
}

private fun WaterLogInput.requireValid() {
    require(amountMilliliters.isFinite() && amountMilliliters > 0.0) { "Water amount must be positive" }
}

private fun RecipeUpsertInput.requireValid() {
    require(name.isNotBlank()) { "Recipe name is required" }
    require(servingGrams.isFinite() && servingGrams > 0.0) { "Recipe serving size must be positive" }
    require(servings.isFinite() && servings > 0.0) { "Recipe servings must be positive" }
    require(cookedYieldGrams.isFinite() && cookedYieldGrams > 0.0) { "Cooked yield must be positive" }
    require(ingredients.isNotEmpty()) { "Recipe needs at least one ingredient" }
    ingredients.forEach {
        require(it.foodId.isNotBlank()) { "Ingredient food is required" }
        require(it.quantityGrams.isFinite() && it.quantityGrams > 0.0) { "Ingredient quantity must be positive" }
        require(it.unitLabel.isNotBlank()) { "Ingredient unit is required" }
        require(it.unitGrams.isFinite() && it.unitGrams > 0.0) { "Ingredient unit grams must be positive" }
        require(it.unitQuantity.isFinite() && it.unitQuantity > 0.0) { "Ingredient unit quantity must be positive" }
    }
}

private fun FoodNutrition.requireValid() {
    require(caloriesKcal.isNonNegativeFinite())
    require(proteinGrams.isNonNegativeFinite())
    require(carbsGrams.isNonNegativeFinite())
    require(fatGrams.isNonNegativeFinite())
}

private fun NutritionDetails.requireValid() {
    require(fiberGrams.isNonNegativeFinite())
    require(sugarGrams.isNonNegativeFinite())
    require(saturatedFatGrams.isNonNegativeFinite())
    require(sodiumMilligrams.isNonNegativeFinite())
    require(potassiumMilligrams.isNonNegativeFinite())
    require(calciumMilligrams.isNonNegativeFinite())
    require(ironMilligrams.isNonNegativeFinite())
    require(vitaminDMicrograms.isNonNegativeFinite())
    require(vitaminCMilligrams.isNonNegativeFinite())
    require(magnesiumMilligrams.isNonNegativeFinite())
}

private fun SavedFoodUpsertInput.resolvedServings(): List<FoodServingInput> =
    servings.ifEmpty {
        listOf(FoodServingInput(servingName?.takeIf { it.isNotBlank() } ?: "${defaultServingGrams.formatFoodNumber()} g", defaultServingGrams))
    }

private fun Double.isNonNegativeFinite(): Boolean = isFinite() && this >= 0.0

private fun QuickCaloriePresetEntity.toQuickCaloriePreset(): QuickCaloriePreset =
    QuickCaloriePreset(
        id = id,
        name = name,
        caloriesKcal = caloriesKcal,
        proteinGrams = proteinGrams,
        carbsGrams = carbsGrams,
        fatGrams = fatGrams,
        isFavorite = isFavorite,
    )

private fun MealDefinitionEntity.toFoodMealDefinition(): FoodMealDefinition =
    FoodMealDefinition(
        id = id,
        name = name,
        timeMinutes = timeMinutes,
        sortOrder = sortOrder,
    )

private fun MealNutritionRow.toMealItemInput(): MealItemInput =
    MealItemInput(
        foodId = "",
        quantityGrams = quantityGrams,
        nutritionPer100g = FoodNutrition(
            caloriesKcal = caloriesPer100g,
            proteinGrams = proteinPer100g,
            carbsGrams = carbsPer100g,
            fatGrams = fatPer100g,
        ),
    )

private fun List<FoodDiaryEntryRow>.toFoodDiary(): FoodDiary {
    val entriesByMeal = groupBy { it.mealType }
    val meals = entriesByMeal.map { (mealType, mealRows) ->
        val entries = mealRows.map { row -> row.toDiaryEntry() }
        val loggedEntries = entries.filter { entry -> entry.status == FoodDiaryEntryStatus.Logged }
        val plannedEntries = entries.filter { entry -> entry.status == FoodDiaryEntryStatus.Planned }
        FoodDiaryMeal(
            type = mealType,
            entries = entries,
            totals = loggedEntries.calculateTotals(),
            detailTotals = loggedEntries.calculateDetailTotals(),
            plannedTotals = plannedEntries.calculateTotals(),
            plannedDetailTotals = plannedEntries.calculateDetailTotals(),
        )
    }

    val allEntries = meals.flatMap { it.entries }
    val loggedEntries = allEntries.filter { entry -> entry.status == FoodDiaryEntryStatus.Logged }
    val plannedEntries = allEntries.filter { entry -> entry.status == FoodDiaryEntryStatus.Planned }
    return FoodDiary(
        totals = loggedEntries.calculateTotals(),
        meals = meals,
        detailTotals = loggedEntries.calculateDetailTotals(),
        plannedTotals = plannedEntries.calculateTotals(),
        plannedDetailTotals = plannedEntries.calculateDetailTotals(),
    )
}

private fun FoodDiaryEntryRow.toDiaryEntry(): FoodDiaryEntry {
    val multiplier = quantityGrams / 100.0
    return FoodDiaryEntry(
        id = mealItemId,
        foodId = foodId,
        imageUrl = imageUrl,
        name = foodName,
        brand = brand,
        quantityGrams = quantityGrams,
        caloriesKcal = caloriesPer100g * multiplier,
        proteinGrams = proteinPer100g * multiplier,
        carbsGrams = carbsPer100g * multiplier,
        fatGrams = fatPer100g * multiplier,
        nutritionDetails = NutritionDetails(
            fiberGrams = fiberPer100g * multiplier,
            sugarGrams = sugarPer100g * multiplier,
            saturatedFatGrams = saturatedFatPer100g * multiplier,
            sodiumMilligrams = sodiumMgPer100g * multiplier,
            potassiumMilligrams = potassiumMgPer100g * multiplier,
            calciumMilligrams = calciumMgPer100g * multiplier,
            ironMilligrams = ironMgPer100g * multiplier,
            vitaminDMicrograms = vitaminDMcgPer100g * multiplier,
            vitaminCMilligrams = vitaminCMgPer100g * multiplier,
            magnesiumMilligrams = magnesiumMgPer100g * multiplier,
        ),
        status = status.toFoodDiaryEntryStatus(),
    )
}

private fun List<FoodDiaryEntryRow>.toFoodPlanDays(startDate: LocalDate): List<FoodPlanDay> {
    val rowsByDate = groupBy { row -> row.dateEpochDay }
    return (0L..6L).map { offset ->
        val date = startDate.plusDays(offset)
        val entries = rowsByDate[date.toEpochDay()].orEmpty().map { row -> row.toDiaryEntry() }
        val loggedEntries = entries.filter { entry -> entry.status == FoodDiaryEntryStatus.Logged }
        val plannedEntries = entries.filter { entry -> entry.status == FoodDiaryEntryStatus.Planned }
        FoodPlanDay(
            date = date,
            loggedTotals = loggedEntries.calculateTotals(),
            plannedTotals = plannedEntries.calculateTotals(),
            loggedEntryCount = loggedEntries.size,
            plannedEntryCount = plannedEntries.size,
        )
    }
}

private fun List<FoodDiaryEntryRow>.toFoodWeeklySummary(
    startDate: LocalDate,
    waterRows: List<WaterTotalRow>,
    goal: FoodGoal,
): FoodWeeklySummary =
    FoodWeeklySummary(
        startDate = startDate,
        days = toFoodRangeDaySummaries(startDate = startDate, dayCount = 7, waterRows = waterRows, goal = goal),
        goal = goal,
    )

private fun List<FoodDiaryEntryRow>.toFoodProgressSummary(
    startDate: LocalDate,
    dayCount: Int,
    waterRows: List<WaterTotalRow>,
    goal: FoodGoal,
): FoodProgressSummary =
    FoodProgressSummary(
        startDate = startDate,
        dayCount = dayCount,
        days = toFoodRangeDaySummaries(startDate = startDate, dayCount = dayCount, waterRows = waterRows, goal = goal),
        goal = goal,
    )

private fun List<FoodDiaryEntryRow>.toFoodRangeDaySummaries(
    startDate: LocalDate,
    dayCount: Int,
    waterRows: List<WaterTotalRow>,
    goal: FoodGoal,
): List<FoodWeeklyDaySummary> {
    val rowsByDate = groupBy { row -> row.dateEpochDay }
    val waterByDate = waterRows.associateBy { row -> row.dateEpochDay }
    return (0 until dayCount).map { offset ->
        val date = startDate.plusDays(offset.toLong())
        val epochDay = date.toEpochDay()
        FoodWeeklyDaySummary(
            date = date,
            diary = rowsByDate[epochDay].orEmpty().toFoodDiary(),
            water = FoodWaterSummary(
                date = date,
                consumedMilliliters = waterByDate[epochDay]?.consumedMilliliters ?: 0.0,
                goalMilliliters = goal.waterGoalMilliliters,
            ),
        )
    }
}

private fun List<FoodDiaryEntry>.calculateTotals(): NutritionTotals =
    NutritionTotals(
        caloriesKcal = sumOf { it.caloriesKcal },
        proteinGrams = sumOf { it.proteinGrams },
        carbsGrams = sumOf { it.carbsGrams },
        fatGrams = sumOf { it.fatGrams },
    )

private fun List<FoodDiaryEntry>.calculateDetailTotals(): NutritionDetails =
    NutritionDetails(
        fiberGrams = sumOf { it.nutritionDetails.fiberGrams },
        sugarGrams = sumOf { it.nutritionDetails.sugarGrams },
        saturatedFatGrams = sumOf { it.nutritionDetails.saturatedFatGrams },
        sodiumMilligrams = sumOf { it.nutritionDetails.sodiumMilligrams },
        potassiumMilligrams = sumOf { it.nutritionDetails.potassiumMilligrams },
        calciumMilligrams = sumOf { it.nutritionDetails.calciumMilligrams },
        ironMilligrams = sumOf { it.nutritionDetails.ironMilligrams },
        vitaminDMicrograms = sumOf { it.nutritionDetails.vitaminDMicrograms },
        vitaminCMilligrams = sumOf { it.nutritionDetails.vitaminCMilligrams },
        magnesiumMilligrams = sumOf { it.nutritionDetails.magnesiumMilligrams },
    )

private data class ShoppingListGeneratedRow(
    val sourceKey: String,
    val name: String,
    val category: String,
    val quantityGrams: Double,
)

private fun List<ShoppingListItemEntity>.toShoppingListGroups(): List<ShoppingListGroup> =
    groupBy { item -> item.category.normalizedShoppingCategory() }
        .map { (category, items) ->
            ShoppingListGroup(
                category = category,
                items = items
                    .sortedWith(compareBy<ShoppingListItemEntity> { it.isChecked }.thenBy { it.name.lowercase(Locale.US) })
                    .map { it.toShoppingListItem() },
            )
        }
        .sortedBy { group -> group.category.lowercase(Locale.US) }

private fun ShoppingListItemEntity.toShoppingListItem(): ShoppingListItem =
    ShoppingListItem(
        id = id,
        name = name,
        category = category.normalizedShoppingCategory(),
        quantityGrams = quantityGrams,
        isChecked = isChecked,
        isManual = isManual,
    )

private fun String?.normalizedShoppingCategory(): String =
    this?.trim()?.takeIf { it.isNotEmpty() } ?: "Other"

private fun FoodDiaryEntryStatus.asStorageValue(): String =
    when (this) {
        FoodDiaryEntryStatus.Logged -> "logged"
        FoodDiaryEntryStatus.Planned -> "planned"
    }

private fun String.toFoodDiaryEntryStatus(): FoodDiaryEntryStatus =
    when (lowercase(Locale.US)) {
        "planned" -> FoodDiaryEntryStatus.Planned
        else -> FoodDiaryEntryStatus.Logged
    }

private fun FoodEntity.toSavedFoodItem(servings: List<FoodServingEntity>): SavedFoodItem =
    SavedFoodItem(
        id = id,
        imageUrl = imageUrl,
        name = name,
        brand = brand,
        defaultServingGrams = defaultServingGrams,
        nutritionPer100g = FoodNutrition(
            caloriesKcal = caloriesPer100g,
            proteinGrams = proteinPer100g,
            carbsGrams = carbsPer100g,
            fatGrams = fatPer100g,
        ),
        nutritionDetailsPer100g = NutritionDetails(
            fiberGrams = fiberPer100g,
            sugarGrams = sugarPer100g,
            saturatedFatGrams = saturatedFatPer100g,
            sodiumMilligrams = sodiumMgPer100g,
            potassiumMilligrams = potassiumMgPer100g,
            calciumMilligrams = calciumMgPer100g,
            ironMilligrams = ironMgPer100g,
            vitaminDMicrograms = vitaminDMcgPer100g,
            vitaminCMilligrams = vitaminCMgPer100g,
            magnesiumMilligrams = magnesiumMgPer100g,
        ),
        servingName = servingName,
        barcode = barcode,
        category = category,
        isFavorite = isFavorite,
        servings = servings.map { serving -> FoodServingOption(serving.id, serving.label, serving.grams) },
    )

private fun FoodGoal.toEntity(now: Long): FoodGoalEntity =
    FoodGoalEntity(
        id = "default",
        dailyCaloriesKcal = dailyCaloriesKcal,
        proteinGrams = proteinGrams,
        carbsGrams = carbsGrams,
        fatGrams = fatGrams,
        fiberGrams = fiberGrams,
        sugarGrams = sugarGrams,
        saturatedFatGrams = saturatedFatGrams,
        sodiumMilligrams = sodiumMilligrams,
        mode = mode.name,
        includeTrainingCalories = includeTrainingCalories,
        useNetCarbs = useNetCarbs,
        waterGoalMilliliters = waterGoalMilliliters,
        updatedAtEpochMillis = now,
    )

private fun FoodGoalEntity.toFoodGoal(): FoodGoal =
    FoodGoal(
        dailyCaloriesKcal = dailyCaloriesKcal,
        proteinGrams = proteinGrams,
        carbsGrams = carbsGrams,
        fatGrams = fatGrams,
        fiberGrams = fiberGrams,
        sugarGrams = sugarGrams,
        saturatedFatGrams = saturatedFatGrams,
        sodiumMilligrams = sodiumMilligrams,
        mode = mode.toFoodGoalMode(),
        includeTrainingCalories = includeTrainingCalories,
        useNetCarbs = useNetCarbs,
        waterGoalMilliliters = waterGoalMilliliters,
    )

private fun String.toFoodGoalMode(): FoodGoalMode =
    when (this) {
        "Maintain" -> FoodGoalMode.Balanced
        "LoseWeight" -> FoodGoalMode.WeightLoss
        else -> runCatching { FoodGoalMode.valueOf(this) }.getOrDefault(FoodGoalMode.Balanced)
    }

private fun List<MealTemplateItemRow>.toMealTemplates(): List<MealTemplate> =
    groupBy { it.templateId }.map { (_, rows) ->
        val first = rows.first()
        MealTemplate(
            id = first.templateId,
            name = first.templateName,
            mealType = first.templateMealType,
            isFavorite = first.templateIsFavorite,
            items = rows.sortedBy { it.sortOrder }.map { row ->
                MealTemplateItem(
                    foodId = row.foodId,
                    foodName = row.foodName,
                    brand = row.brand,
                    quantityGrams = row.quantityGrams,
                )
            },
        )
    }

private fun List<RecipeIngredientRow>.toRecipes(): List<Recipe> =
    groupBy { it.recipeId }.map { (_, rows) ->
        val first = rows.first()
        val fullRecipeGrams = first.resolvedCookedYieldGrams(rows)
        val per100gNutrition = rows.calculateRecipeNutritionPer100g(fullRecipeGrams)
        val per100gDetails = rows.calculateRecipeDetailsPer100g(fullRecipeGrams)
        val servingMultiplier = first.recipeServingGrams / 100.0
        Recipe(
            id = first.recipeId,
            name = first.recipeName,
            category = first.recipeCategory,
            servingName = first.recipeServingName,
            servingGrams = first.recipeServingGrams,
            servings = first.recipeServings,
            cookedYieldGrams = fullRecipeGrams,
            isFavorite = first.recipeIsFavorite,
            ingredients = rows.sortedBy { it.sortOrder }.map { row ->
                RecipeIngredient(
                    foodId = row.foodId,
                    foodName = row.foodName,
                    brand = row.brand,
                    quantityGrams = row.quantityGrams,
                    unitLabel = row.unitLabel,
                    unitGrams = row.unitGrams,
                    unitQuantity = row.unitQuantity.takeIf { it > 0.0 } ?: row.quantityGrams,
                )
            },
            nutritionPerServing = per100gNutrition * servingMultiplier,
            detailNutritionPerServing = per100gDetails * servingMultiplier,
        )
    }

private fun RecipeEntity.resolvedCookedYieldGrams(rows: List<RecipeIngredientRow>): Double =
    cookedYieldGrams.takeIf { it.isFinite() && it > 0.0 }
        ?: rows.sumOf { it.quantityGrams }.takeIf { it > 0.0 }
        ?: servingGrams

private fun RecipeIngredientRow.resolvedCookedYieldGrams(rows: List<RecipeIngredientRow>): Double =
    recipeCookedYieldGrams.takeIf { it.isFinite() && it > 0.0 }
        ?: rows.sumOf { it.quantityGrams }.takeIf { it > 0.0 }
        ?: recipeServingGrams

private fun List<RecipeIngredientRow>.calculateRecipeNutritionPer100g(totalRecipeGrams: Double): FoodNutrition {
    val totalCalories = sumOf { it.caloriesPer100g * it.quantityGrams / 100.0 }
    val totalProtein = sumOf { it.proteinPer100g * it.quantityGrams / 100.0 }
    val totalCarbs = sumOf { it.carbsPer100g * it.quantityGrams / 100.0 }
    val totalFat = sumOf { it.fatPer100g * it.quantityGrams / 100.0 }
    val multiplier = 100.0 / totalRecipeGrams
    return FoodNutrition(totalCalories * multiplier, totalProtein * multiplier, totalCarbs * multiplier, totalFat * multiplier)
}

private fun List<RecipeIngredientRow>.calculateRecipeDetailsPer100g(totalRecipeGrams: Double): NutritionDetails {
    val totalFiber = sumOf { it.fiberPer100g * it.quantityGrams / 100.0 }
    val totalSugar = sumOf { it.sugarPer100g * it.quantityGrams / 100.0 }
    val totalSaturatedFat = sumOf { it.saturatedFatPer100g * it.quantityGrams / 100.0 }
    val totalSodium = sumOf { it.sodiumMgPer100g * it.quantityGrams / 100.0 }
    val totalPotassium = sumOf { it.potassiumMgPer100g * it.quantityGrams / 100.0 }
    val totalCalcium = sumOf { it.calciumMgPer100g * it.quantityGrams / 100.0 }
    val totalIron = sumOf { it.ironMgPer100g * it.quantityGrams / 100.0 }
    val totalVitaminD = sumOf { it.vitaminDMcgPer100g * it.quantityGrams / 100.0 }
    val totalVitaminC = sumOf { it.vitaminCMgPer100g * it.quantityGrams / 100.0 }
    val totalMagnesium = sumOf { it.magnesiumMgPer100g * it.quantityGrams / 100.0 }
    val multiplier = 100.0 / totalRecipeGrams
    return NutritionDetails(
        fiberGrams = totalFiber * multiplier,
        sugarGrams = totalSugar * multiplier,
        saturatedFatGrams = totalSaturatedFat * multiplier,
        sodiumMilligrams = totalSodium * multiplier,
        potassiumMilligrams = totalPotassium * multiplier,
        calciumMilligrams = totalCalcium * multiplier,
        ironMilligrams = totalIron * multiplier,
        vitaminDMicrograms = totalVitaminD * multiplier,
        vitaminCMilligrams = totalVitaminC * multiplier,
        magnesiumMilligrams = totalMagnesium * multiplier,
    )
}

private operator fun FoodNutrition.times(multiplier: Double): FoodNutrition =
    FoodNutrition(
        caloriesKcal = caloriesKcal * multiplier,
        proteinGrams = proteinGrams * multiplier,
        carbsGrams = carbsGrams * multiplier,
        fatGrams = fatGrams * multiplier,
    )

private operator fun NutritionDetails.times(multiplier: Double): NutritionDetails =
    NutritionDetails(
        fiberGrams = fiberGrams * multiplier,
        sugarGrams = sugarGrams * multiplier,
        saturatedFatGrams = saturatedFatGrams * multiplier,
        sodiumMilligrams = sodiumMilligrams * multiplier,
        potassiumMilligrams = potassiumMilligrams * multiplier,
        calciumMilligrams = calciumMilligrams * multiplier,
        ironMilligrams = ironMilligrams * multiplier,
        vitaminDMicrograms = vitaminDMicrograms * multiplier,
        vitaminCMilligrams = vitaminCMilligrams * multiplier,
        magnesiumMilligrams = magnesiumMilligrams * multiplier,
    )

private fun String.normalizedMealDefinitionId(): String {
    val normalized = trim().lowercase(Locale.US)
    return when (normalized) {
        "snack" -> "snacks"
        else -> normalized
    }
}

private fun String.generatedMealDefinitionId(): String =
    trim()
        .lowercase(Locale.US)
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .ifBlank { "meal_${UUID.randomUUID()}" }

private fun Double.formatFoodNumber(): String {
    val longValue = toLong()
    return if (this == longValue.toDouble()) longValue.toString() else String.format(Locale.US, "%.1f", this)
}
