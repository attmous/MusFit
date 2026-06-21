package com.musfit.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.musfit.data.local.MusFitDatabase
import com.musfit.data.local.dao.FoodDao
import com.musfit.data.local.dao.HealthDao
import com.musfit.data.local.dao.TrainingDao
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
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideFoodDao(database: MusFitDatabase): FoodDao = database.foodDao()

    @Provides
    fun provideTrainingDao(database: MusFitDatabase): TrainingDao = database.trainingDao()

    @Provides
    fun provideHealthDao(database: MusFitDatabase): HealthDao = database.healthDao()

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
}
