package com.musfit.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.musfit.data.local.dao.FoodDao
import com.musfit.data.local.dao.HealthDao
import com.musfit.data.local.dao.ProfileDao
import com.musfit.data.local.dao.TrainingDao
import com.musfit.data.local.entity.AppSettingsEntity
import com.musfit.data.local.entity.BarcodeProductEntity
import com.musfit.data.local.entity.BodyMetricEntity
import com.musfit.data.local.entity.DailyHealthSummaryEntity
import com.musfit.data.local.entity.ExerciseEntity
import com.musfit.data.local.entity.FoodEntity
import com.musfit.data.local.entity.FoodGoalEntity
import com.musfit.data.local.entity.FoodHealthConnectSyncEntity
import com.musfit.data.local.entity.FoodServingEntity
import com.musfit.data.local.entity.HealthConnectSyncStateEntity
import com.musfit.data.local.entity.MealDefinitionEntity
import com.musfit.data.local.entity.MealEntity
import com.musfit.data.local.entity.MealItemEntity
import com.musfit.data.local.entity.MealTemplateEntity
import com.musfit.data.local.entity.MealTemplateItemEntity
import com.musfit.data.local.entity.QuickCaloriePresetEntity
import com.musfit.data.local.entity.RecipeEntity
import com.musfit.data.local.entity.RecipeIngredientEntity
import com.musfit.data.local.entity.RoutineEntity
import com.musfit.data.local.entity.RoutineExerciseEntity
import com.musfit.data.local.entity.ShoppingListItemEntity
import com.musfit.data.local.entity.UserProfileEntity
import com.musfit.data.local.entity.WaterEntryEntity
import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity

@Database(
    entities = [
        FoodEntity::class,
        FoodServingEntity::class,
        MealEntity::class,
        MealDefinitionEntity::class,
        MealItemEntity::class,
        BarcodeProductEntity::class,
        FoodGoalEntity::class,
        QuickCaloriePresetEntity::class,
        MealTemplateEntity::class,
        MealTemplateItemEntity::class,
        RecipeEntity::class,
        RecipeIngredientEntity::class,
        ShoppingListItemEntity::class,
        WaterEntryEntity::class,
        FoodHealthConnectSyncEntity::class,
        ExerciseEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSetEntity::class,
        BodyMetricEntity::class,
        DailyHealthSummaryEntity::class,
        HealthConnectSyncStateEntity::class,
        UserProfileEntity::class,
        AppSettingsEntity::class,
    ],
    version = 17,
    exportSchema = true,
)
abstract class MusFitDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao

    abstract fun trainingDao(): TrainingDao

    abstract fun healthDao(): HealthDao

    abstract fun profileDao(): ProfileDao
}
