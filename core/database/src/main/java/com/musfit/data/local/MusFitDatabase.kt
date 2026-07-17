package com.musfit.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.musfit.data.local.dao.AccountDao
import com.musfit.data.local.dao.AccountErasureDao
import com.musfit.data.local.dao.AiCoachChatDao
import com.musfit.data.local.dao.AiCoachDao
import com.musfit.data.local.dao.CoachDao
import com.musfit.data.local.dao.FoodDao
import com.musfit.data.local.dao.HealthDao
import com.musfit.data.local.dao.ProfileDao
import com.musfit.data.local.dao.TrainingDao
import com.musfit.data.local.dao.UserGoalsDao
import com.musfit.data.local.entity.AccountEntity
import com.musfit.data.local.entity.AccountSessionEntity
import com.musfit.data.local.entity.AiCoachChatMessageEntity
import com.musfit.data.local.entity.AiCoachSettingsEntity
import com.musfit.data.local.entity.AiCoachThreadEntity
import com.musfit.data.local.entity.AppSettingsEntity
import com.musfit.data.local.entity.BarcodeProductEntity
import com.musfit.data.local.entity.BodyMetricEntity
import com.musfit.data.local.entity.CoachMessageEntity
import com.musfit.data.local.entity.DailyHealthSummaryEntity
import com.musfit.data.local.entity.DashboardPinEntity
import com.musfit.data.local.entity.ExerciseEntity
import com.musfit.data.local.entity.ExerciseNoteEntity
import com.musfit.data.local.entity.FoodEntity
import com.musfit.data.local.entity.FoodGoalEntity
import com.musfit.data.local.entity.FoodHealthConnectSyncEntity
import com.musfit.data.local.entity.FoodServingEntity
import com.musfit.data.local.entity.HealthConnectExportRecordEntity
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
import com.musfit.data.local.entity.RoutineExerciseSetEntity
import com.musfit.data.local.entity.RoutineFolderEntity
import com.musfit.data.local.entity.ShoppingListItemEntity
import com.musfit.data.local.entity.TrainingSettingsEntity
import com.musfit.data.local.entity.UserGoalsEntity
import com.musfit.data.local.entity.UserProfileEntity
import com.musfit.data.local.entity.WaterEntryEntity
import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity

const val MUSFIT_DATABASE_NAME = "musfit.db"
const val MUSFIT_DATABASE_VERSION = 42

@Database(
    entities = [
        AccountEntity::class,
        AccountSessionEntity::class,
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
        ExerciseNoteEntity::class,
        RoutineEntity::class,
        RoutineFolderEntity::class,
        RoutineExerciseEntity::class,
        RoutineExerciseSetEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSetEntity::class,
        TrainingSettingsEntity::class,
        BodyMetricEntity::class,
        DailyHealthSummaryEntity::class,
        HealthConnectSyncStateEntity::class,
        HealthConnectExportRecordEntity::class,
        UserProfileEntity::class,
        AppSettingsEntity::class,
        UserGoalsEntity::class,
        AiCoachSettingsEntity::class,
        AiCoachThreadEntity::class,
        AiCoachChatMessageEntity::class,
        CoachMessageEntity::class,
        DashboardPinEntity::class,
    ],
    version = 42,
    exportSchema = true,
)
abstract class MusFitDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao

    abstract fun accountErasureDao(): AccountErasureDao

    abstract fun foodDao(): FoodDao

    abstract fun trainingDao(): TrainingDao

    abstract fun healthDao(): HealthDao

    abstract fun profileDao(): ProfileDao

    abstract fun userGoalsDao(): UserGoalsDao

    abstract fun aiCoachDao(): AiCoachDao

    abstract fun aiCoachChatDao(): AiCoachChatDao

    abstract fun coachDao(): CoachDao
}
