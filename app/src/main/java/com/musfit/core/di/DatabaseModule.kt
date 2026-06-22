package com.musfit.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.musfit.data.local.MusFitDatabase
import com.musfit.data.local.dao.FoodDao
import com.musfit.data.local.dao.HealthDao
import com.musfit.data.local.dao.ProfileDao
import com.musfit.data.local.dao.TrainingDao
import com.musfit.data.local.dao.UserGoalsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MusFitDatabase =
        Room.databaseBuilder(context, MusFitDatabase::class.java, "musfit.db")
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
                MIGRATION_10_11,
                MIGRATION_11_12,
                MIGRATION_12_13,
                MIGRATION_13_14,
                MIGRATION_14_15,
                MIGRATION_15_16,
                MIGRATION_16_17,
                MIGRATION_17_18,
                MIGRATION_18_19,
            )
            .build()

    @Provides
    fun provideFoodDao(database: MusFitDatabase): FoodDao = database.foodDao()

    @Provides
    fun provideTrainingDao(database: MusFitDatabase): TrainingDao = database.trainingDao()

    @Provides
    fun provideHealthDao(database: MusFitDatabase): HealthDao = database.healthDao()

    @Provides
    fun provideProfileDao(database: MusFitDatabase): ProfileDao = database.profileDao()

    @Provides
    fun provideUserGoalsDao(database: MusFitDatabase): UserGoalsDao = database.userGoalsDao()

    private val MIGRATION_1_2 =
        object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE foods ADD COLUMN servingName TEXT")
                db.execSQL("ALTER TABLE foods ADD COLUMN barcode TEXT")
                db.execSQL("ALTER TABLE foods ADD COLUMN category TEXT")
                db.execSQL("ALTER TABLE foods ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE foods ADD COLUMN fiberPer100g REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE foods ADD COLUMN sugarPer100g REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE foods ADD COLUMN saturatedFatPer100g REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE foods ADD COLUMN sodiumMgPer100g REAL NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_foods_barcode ON foods(barcode)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS food_goals (
                        id TEXT NOT NULL PRIMARY KEY,
                        dailyCaloriesKcal REAL NOT NULL,
                        proteinGrams REAL NOT NULL,
                        carbsGrams REAL NOT NULL,
                        fatGrams REAL NOT NULL,
                        fiberGrams REAL NOT NULL,
                        sugarGrams REAL NOT NULL,
                        saturatedFatGrams REAL NOT NULL,
                        sodiumMilligrams REAL NOT NULL,
                        mode TEXT NOT NULL,
                        includeTrainingCalories INTEGER NOT NULL,
                        updatedAtEpochMillis INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS meal_templates (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        mealType TEXT NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL,
                        updatedAtEpochMillis INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS meal_template_items (
                        id TEXT NOT NULL PRIMARY KEY,
                        templateId TEXT NOT NULL,
                        foodId TEXT NOT NULL,
                        quantityGrams REAL NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        FOREIGN KEY(templateId) REFERENCES meal_templates(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(foodId) REFERENCES foods(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_meal_template_items_templateId ON meal_template_items(templateId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_meal_template_items_foodId ON meal_template_items(foodId)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS recipes (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        category TEXT,
                        servingName TEXT NOT NULL,
                        servingGrams REAL NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL,
                        updatedAtEpochMillis INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS recipe_ingredients (
                        id TEXT NOT NULL PRIMARY KEY,
                        recipeId TEXT NOT NULL,
                        foodId TEXT NOT NULL,
                        quantityGrams REAL NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        FOREIGN KEY(recipeId) REFERENCES recipes(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(foodId) REFERENCES foods(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_ingredients_recipeId ON recipe_ingredients(recipeId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_ingredients_foodId ON recipe_ingredients(foodId)")
            }
        }

    private val MIGRATION_2_3 =
        object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE meal_templates ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            }
        }

    private val MIGRATION_3_4 =
        object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recipes ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            }
        }

    private val MIGRATION_4_5 =
        object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS quick_calorie_presets (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        caloriesKcal REAL NOT NULL,
                        proteinGrams REAL NOT NULL,
                        carbsGrams REAL NOT NULL,
                        fatGrams REAL NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL,
                        updatedAtEpochMillis INTEGER NOT NULL,
                        isFavorite INTEGER NOT NULL DEFAULT 1
                    )
                    """.trimIndent(),
                )
            }
        }

    private val MIGRATION_5_6 =
        object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS meal_definitions (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        timeMinutes INTEGER,
                        sortOrder INTEGER NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL,
                        updatedAtEpochMillis INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_meal_definitions_sortOrder ON meal_definitions(sortOrder)")
            }
        }

    private val MIGRATION_6_7 =
        object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE food_goals ADD COLUMN useNetCarbs INTEGER NOT NULL DEFAULT 0")
            }
        }

    private val MIGRATION_7_8 =
        object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE foods ADD COLUMN potassiumMgPer100g REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE foods ADD COLUMN calciumMgPer100g REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE foods ADD COLUMN ironMgPer100g REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE foods ADD COLUMN vitaminDMcgPer100g REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE foods ADD COLUMN vitaminCMgPer100g REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE foods ADD COLUMN magnesiumMgPer100g REAL NOT NULL DEFAULT 0")
            }
        }

    private val MIGRATION_8_9 =
        object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recipes ADD COLUMN servings REAL NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE recipes ADD COLUMN cookedYieldGrams REAL NOT NULL DEFAULT 0")
                db.execSQL("UPDATE recipes SET cookedYieldGrams = servingGrams WHERE cookedYieldGrams = 0")
                db.execSQL("ALTER TABLE recipe_ingredients ADD COLUMN unitLabel TEXT NOT NULL DEFAULT 'g'")
                db.execSQL("ALTER TABLE recipe_ingredients ADD COLUMN unitGrams REAL NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE recipe_ingredients ADD COLUMN unitQuantity REAL NOT NULL DEFAULT 0")
                db.execSQL("UPDATE recipe_ingredients SET unitQuantity = quantityGrams WHERE unitQuantity = 0")
            }
        }

    private val MIGRATION_9_10 =
        object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE meal_items ADD COLUMN status TEXT NOT NULL DEFAULT 'logged'")
            }
        }

    private val MIGRATION_10_11 =
        object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS shopping_list_items (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        category TEXT NOT NULL,
                        quantityGrams REAL NOT NULL,
                        isChecked INTEGER NOT NULL,
                        isManual INTEGER NOT NULL,
                        sourceKey TEXT,
                        sortOrder INTEGER NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL,
                        updatedAtEpochMillis INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_list_items_category ON shopping_list_items(category)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_shopping_list_items_sourceKey ON shopping_list_items(sourceKey)")
            }
        }

    private val MIGRATION_11_12 =
        object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE food_goals ADD COLUMN waterGoalMilliliters REAL NOT NULL DEFAULT 2000")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS water_entries (
                        id TEXT NOT NULL PRIMARY KEY,
                        dateEpochDay INTEGER NOT NULL,
                        amountMilliliters REAL NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_water_entries_dateEpochDay ON water_entries(dateEpochDay)")
            }
        }

    private val MIGRATION_12_13 =
        object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS food_health_connect_sync (
                        `key` TEXT NOT NULL PRIMARY KEY,
                        isEnabled INTEGER NOT NULL,
                        lastSyncAtEpochMillis INTEGER,
                        lastFailureMessage TEXT,
                        updatedAtEpochMillis INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

    private val MIGRATION_13_14 =
        object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_foods_name ON foods(name)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_foods_brand ON foods(brand)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_foods_category ON foods(category)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_foods_isFavorite ON foods(isFavorite)")
            }
        }

    private val MIGRATION_14_15 =
        object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routines ADD COLUMN updatedAtEpochMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE routines SET updatedAtEpochMillis = createdAtEpochMillis WHERE updatedAtEpochMillis = 0")
                db.execSQL("ALTER TABLE routines ADD COLUMN isStarter INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN title TEXT")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN status TEXT NOT NULL DEFAULT 'completed'")
                db.execSQL("ALTER TABLE workout_sets ADD COLUMN setType TEXT NOT NULL DEFAULT 'working'")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sessions_status ON workout_sessions(status)")
            }
        }

    private val MIGRATION_15_16 =
        object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE foods ADD COLUMN imageUrl TEXT")
            }
        }

    private val MIGRATION_16_17 =
        object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_goals (
                        id TEXT NOT NULL PRIMARY KEY,
                        stepGoal INTEGER NOT NULL,
                        weeklySessionTarget INTEGER NOT NULL,
                        targetWeightKg REAL NOT NULL,
                        updatedAtEpochMillis INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

    private val MIGRATION_17_18 =
        object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_profile (
                        id TEXT NOT NULL PRIMARY KEY,
                        sex TEXT,
                        birthDateEpochDay INTEGER,
                        heightCm REAL,
                        activityLevel TEXT NOT NULL,
                        goalType TEXT NOT NULL,
                        goalPaceKgPerWeek REAL NOT NULL,
                        goalWeightKg REAL,
                        updatedAtEpochMillis INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS app_settings (
                        id TEXT NOT NULL PRIMARY KEY,
                        unitSystem TEXT NOT NULL,
                        themeMode TEXT NOT NULL,
                        updatedAtEpochMillis INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

    private val MIGRATION_18_19 =
        object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_sets ADD COLUMN supersetGroupId TEXT")
            }
        }
}
