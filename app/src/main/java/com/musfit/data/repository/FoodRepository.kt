package com.musfit.data.repository

import androidx.room.withTransaction
import com.musfit.data.local.MusFitDatabase
import com.musfit.data.local.dao.FoodDao
import com.musfit.data.local.dao.FoodDiaryEntryRow
import com.musfit.data.local.dao.MealNutritionRow
import com.musfit.data.local.dao.MealTemplateItemRow
import com.musfit.data.local.dao.RecipeIngredientRow
import com.musfit.data.local.entity.BarcodeProductEntity
import com.musfit.data.local.entity.FoodEntity
import com.musfit.data.local.entity.FoodGoalEntity
import com.musfit.data.local.entity.FoodServingEntity
import com.musfit.data.local.entity.MealEntity
import com.musfit.data.local.entity.MealItemEntity
import com.musfit.data.local.entity.MealTemplateEntity
import com.musfit.data.local.entity.MealTemplateItemEntity
import com.musfit.data.local.entity.RecipeEntity
import com.musfit.data.local.entity.RecipeIngredientEntity
import com.musfit.data.remote.food.ProductDataQuality
import com.musfit.data.remote.food.ProductLookupResult
import com.musfit.domain.model.FoodNutrition
import com.musfit.domain.model.MealItemInput
import com.musfit.domain.model.NutritionTotals
import com.musfit.domain.nutrition.NutritionCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class NutritionDetails(
    val fiberGrams: Double = 0.0,
    val sugarGrams: Double = 0.0,
    val saturatedFatGrams: Double = 0.0,
    val sodiumMilligrams: Double = 0.0,
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
)

data class FoodDiaryMeal(
    val type: String,
    val entries: List<FoodDiaryEntry>,
    val totals: NutritionTotals,
    val detailTotals: NutritionDetails = NutritionDetails(),
)

data class FoodDiary(
    val totals: NutritionTotals,
    val meals: List<FoodDiaryMeal>,
    val detailTotals: NutritionDetails = NutritionDetails(),
)

enum class FoodGoalMode {
    LoseWeight,
    Maintain,
    MuscleGain,
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
)

data class MealTemplateItem(
    val foodId: String,
    val foodName: String,
    val brand: String?,
    val quantityGrams: Double,
)

data class MealTemplate(
    val id: String,
    val name: String,
    val mealType: String,
    val items: List<MealTemplateItem>,
)

data class RecipeIngredientInput(
    val foodId: String,
    val quantityGrams: Double,
)

data class RecipeUpsertInput(
    val recipeId: String?,
    val name: String,
    val category: String?,
    val servingName: String,
    val servingGrams: Double,
    val ingredients: List<RecipeIngredientInput>,
)

data class RecipeIngredient(
    val foodId: String,
    val foodName: String,
    val brand: String?,
    val quantityGrams: Double,
)

data class Recipe(
    val id: String,
    val name: String,
    val category: String?,
    val servingName: String,
    val servingGrams: Double,
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

    fun observeSavedFoods(): Flow<List<SavedFoodItem>>

    suspend fun getFoodDetail(foodId: String): SavedFoodItem? = null

    suspend fun logSavedFood(input: SavedFoodLogInput): String

    suspend fun quickLog(input: QuickCalorieLogInput): String

    suspend fun updateDiaryEntry(input: DiaryEntryUpdateInput)

    suspend fun deleteDiaryEntry(mealItemId: String)

    suspend fun upsertSavedFood(input: SavedFoodUpsertInput): String

    suspend fun deleteSavedFood(foodId: String)

    suspend fun toggleFavoriteFood(foodId: String, isFavorite: Boolean) = Unit

    fun observeFoodGoal(): Flow<FoodGoal> = flowOf(DEFAULT_REPOSITORY_FOOD_GOAL)

    suspend fun updateFoodGoal(goal: FoodGoal) = Unit

    fun observeMealTemplates(): Flow<List<MealTemplate>> = flowOf(emptyList())

    suspend fun saveMealAsTemplate(date: LocalDate, mealType: String, name: String): String = ""

    suspend fun logMealTemplate(templateId: String, mealType: String, date: LocalDate): List<String> = emptyList()

    suspend fun copyMeal(fromDate: LocalDate, toDate: LocalDate, mealType: String): List<String> = emptyList()

    suspend fun renameMealTemplate(templateId: String, name: String, mealType: String) = Unit

    suspend fun duplicateMealTemplate(templateId: String, name: String): String = ""

    suspend fun deleteMealTemplate(templateId: String) = Unit

    suspend fun copyDiaryEntry(mealItemId: String, mealType: String, date: LocalDate): String = ""

    fun observeRecipes(): Flow<List<Recipe>> = flowOf(emptyList())

    suspend fun upsertRecipe(input: RecipeUpsertInput): String = ""

    suspend fun logRecipe(recipeId: String, mealType: String, servings: Double, date: LocalDate): String = ""

    suspend fun deleteRecipe(recipeId: String) = Unit

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
        mode = FoodGoalMode.Maintain,
        includeTrainingCalories = false,
    )

class LocalFoodRepository @Inject constructor(
    private val database: MusFitDatabase,
    private val foodDao: FoodDao,
) : FoodRepository {
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

    override fun observeSavedFoods(): Flow<List<SavedFoodItem>> =
        foodDao.observeFoods().map { foods ->
            foods
                .filterNot { food -> food.name == QUICK_CALORIES_NAME && food.brand == null }
                .map { food -> food.toSavedFoodItem(foodDao.getServings(food.id)) }
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
                    fiberPer100g = input.nutritionDetailsPer100g.fiberGrams,
                    sugarPer100g = input.nutritionDetailsPer100g.sugarGrams,
                    saturatedFatPer100g = input.nutritionDetailsPer100g.saturatedFatGrams,
                    sodiumMgPer100g = input.nutritionDetailsPer100g.sodiumMilligrams,
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

    override fun observeFoodGoal(): Flow<FoodGoal> =
        foodDao.observeFoodGoal(DEFAULT_GOAL_ID).map { entity ->
            entity?.toFoodGoal() ?: DEFAULT_FOOD_GOAL
        }

    override suspend fun updateFoodGoal(goal: FoodGoal) {
        goal.requireValid()
        foodDao.upsertFoodGoal(goal.toEntity(System.currentTimeMillis()))
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

    override suspend fun copyMeal(fromDate: LocalDate, toDate: LocalDate, mealType: String): List<String> {
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
                )
            }
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
                    createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
                    updatedAtEpochMillis = now,
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
                        sortOrder = index,
                    ),
                )
            }
            recipeId
        }
    }

    override suspend fun logRecipe(recipeId: String, mealType: String, servings: Double, date: LocalDate): String {
        require(recipeId.isNotBlank()) { "Recipe id is required" }
        require(servings.isFinite() && servings > 0.0) { "Servings must be positive" }
        return database.withTransaction {
            val recipe = foodDao.getRecipe(recipeId) ?: error("Recipe not found")
            val rows = foodDao.getRecipeRows(recipeId)
            require(rows.isNotEmpty()) { "Recipe has no ingredients" }
            val fullRecipeGrams = rows.sumOf { it.quantityGrams }.takeIf { it > 0.0 } ?: recipe.servingGrams
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
                ),
            )
            replaceServings(foodId, listOf(FoodServingInput(recipe.servingName, recipe.servingGrams)))
            insertMealItem(
                foodId = foodId,
                mealType = mealType,
                quantityGrams = recipe.servingGrams * servings,
                date = date,
                now = now,
            )
        }
    }

    override suspend fun deleteRecipe(recipeId: String) {
        require(recipeId.isNotBlank()) { "Recipe id is required" }
        database.withTransaction {
            val deletedCount = foodDao.deleteRecipeById(recipeId)
            check(deletedCount > 0) { "Recipe not found" }
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
                fiberPer100g = input.nutritionDetailsPer100g.fiberGrams,
                sugarPer100g = input.nutritionDetailsPer100g.sugarGrams,
                saturatedFatPer100g = input.nutritionDetailsPer100g.saturatedFatGrams,
                sodiumMgPer100g = input.nutritionDetailsPer100g.sodiumMilligrams,
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
            ),
        )

        return mealItemId
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
                category = category?.trim()?.takeIf { it.isNotEmpty() },
                fiberPer100g = editedNutritionDetails.fiberGrams,
                sugarPer100g = editedNutritionDetails.sugarGrams,
                saturatedFatPer100g = editedNutritionDetails.saturatedFatGrams,
                sodiumMgPer100g = editedNutritionDetails.sodiumMilligrams,
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
        const val RECIPE_BRAND = "Recipe"

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
                mode = FoodGoalMode.Maintain,
                includeTrainingCalories = false,
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
}

private fun RecipeUpsertInput.requireValid() {
    require(name.isNotBlank()) { "Recipe name is required" }
    require(servingGrams.isFinite() && servingGrams > 0.0) { "Recipe serving size must be positive" }
    require(ingredients.isNotEmpty()) { "Recipe needs at least one ingredient" }
    ingredients.forEach {
        require(it.foodId.isNotBlank()) { "Ingredient food is required" }
        require(it.quantityGrams.isFinite() && it.quantityGrams > 0.0) { "Ingredient quantity must be positive" }
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
}

private fun SavedFoodUpsertInput.resolvedServings(): List<FoodServingInput> =
    servings.ifEmpty {
        listOf(FoodServingInput(servingName?.takeIf { it.isNotBlank() } ?: "${defaultServingGrams.formatFoodNumber()} g", defaultServingGrams))
    }

private fun Double.isNonNegativeFinite(): Boolean = isFinite() && this >= 0.0

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
        FoodDiaryMeal(
            type = mealType,
            entries = entries,
            totals = entries.calculateTotals(),
            detailTotals = entries.calculateDetailTotals(),
        )
    }

    val allEntries = meals.flatMap { it.entries }
    return FoodDiary(
        totals = allEntries.calculateTotals(),
        meals = meals,
        detailTotals = allEntries.calculateDetailTotals(),
    )
}

private fun FoodDiaryEntryRow.toDiaryEntry(): FoodDiaryEntry {
    val multiplier = quantityGrams / 100.0
    return FoodDiaryEntry(
        id = mealItemId,
        foodId = foodId,
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
        ),
    )
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
    )

private fun FoodEntity.toSavedFoodItem(servings: List<FoodServingEntity>): SavedFoodItem =
    SavedFoodItem(
        id = id,
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
        mode = runCatching { FoodGoalMode.valueOf(mode) }.getOrDefault(FoodGoalMode.Maintain),
        includeTrainingCalories = includeTrainingCalories,
    )

private fun List<MealTemplateItemRow>.toMealTemplates(): List<MealTemplate> =
    groupBy { it.templateId }.map { (_, rows) ->
        val first = rows.first()
        MealTemplate(
            id = first.templateId,
            name = first.templateName,
            mealType = first.templateMealType,
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
        val fullRecipeGrams = rows.sumOf { it.quantityGrams }.takeIf { it > 0.0 } ?: first.recipeServingGrams
        val per100gNutrition = rows.calculateRecipeNutritionPer100g(fullRecipeGrams)
        val per100gDetails = rows.calculateRecipeDetailsPer100g(fullRecipeGrams)
        val servingMultiplier = first.recipeServingGrams / 100.0
        Recipe(
            id = first.recipeId,
            name = first.recipeName,
            category = first.recipeCategory,
            servingName = first.recipeServingName,
            servingGrams = first.recipeServingGrams,
            ingredients = rows.sortedBy { it.sortOrder }.map { row ->
                RecipeIngredient(
                    foodId = row.foodId,
                    foodName = row.foodName,
                    brand = row.brand,
                    quantityGrams = row.quantityGrams,
                )
            },
            nutritionPerServing = per100gNutrition * servingMultiplier,
            detailNutritionPerServing = per100gDetails * servingMultiplier,
        )
    }

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
    val multiplier = 100.0 / totalRecipeGrams
    return NutritionDetails(totalFiber * multiplier, totalSugar * multiplier, totalSaturatedFat * multiplier, totalSodium * multiplier)
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
    )

private fun Double.formatFoodNumber(): String {
    val longValue = toLong()
    return if (this == longValue.toDouble()) longValue.toString() else String.format(Locale.US, "%.1f", this)
}
