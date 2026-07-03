package com.musfit.core.di

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.musfit.data.local.MusFitDatabase
import com.musfit.data.local.dao.AccountDao
import com.musfit.data.local.dao.AiCoachDao
import com.musfit.data.local.dao.CoachDao
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
    fun provideDatabase(@ApplicationContext context: Context): MusFitDatabase {
        repairLegacyVersion28Database(context, DATABASE_NAME)
        return Room.databaseBuilder(context, MusFitDatabase::class.java, DATABASE_NAME)
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
                MIGRATION_19_20,
                MIGRATION_20_21,
                MIGRATION_21_22,
                MIGRATION_22_23,
                MIGRATION_23_24,
                MIGRATION_24_25,
                MIGRATION_25_26,
                MIGRATION_26_27,
                MIGRATION_27_28,
                MIGRATION_28_29,
            )
            .build()
    }

    @Provides
    fun provideAccountDao(database: MusFitDatabase): AccountDao = database.accountDao()

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

    @Provides
    fun provideAiCoachDao(database: MusFitDatabase): AiCoachDao = database.aiCoachDao()

    @Provides
    fun provideCoachDao(database: MusFitDatabase): CoachDao = database.coachDao()

    internal fun repairLegacyVersion28Database(context: Context, databaseName: String = DATABASE_NAME) {
        val databaseFile = context.getDatabasePath(databaseName)
        if (!databaseFile.exists()) return

        val database = SQLiteDatabase.openDatabase(databaseFile.path, null, SQLiteDatabase.OPEN_READWRITE)
        try {
            if (database.version != 28 || database.readRoomIdentityHash() != LEGACY_HEALTH_SYNC_V28_IDENTITY_HASH) {
                return
            }

            database.beginTransaction()
            try {
                database.addColumnIfMissing(
                    tableName = "accounts",
                    columnName = "authProvider",
                    sql = "ALTER TABLE accounts ADD COLUMN authProvider TEXT NOT NULL DEFAULT 'local'",
                )
                database.addColumnIfMissing(
                    tableName = "accounts",
                    columnName = "avatarUrl",
                    sql = "ALTER TABLE accounts ADD COLUMN avatarUrl TEXT",
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_authProvider ON accounts(authProvider)")
                database.normalizeDailyHealthSummaries()
                database.execSQL(
                    "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES(42, ?)",
                    arrayOf<Any>(CURRENT_V28_IDENTITY_HASH),
                )
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        } finally {
            database.close()
        }
    }

    private fun SQLiteDatabase.readRoomIdentityHash(): String? =
        if (!tableExists("room_master_table")) {
            null
        } else {
            rawQuery("SELECT identity_hash FROM room_master_table WHERE id = 42 LIMIT 1", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }

    private fun SQLiteDatabase.tableExists(tableName: String): Boolean =
        rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ? LIMIT 1",
            arrayOf(tableName),
        ).use { cursor ->
            cursor.moveToFirst()
        }

    private fun SQLiteDatabase.addColumnIfMissing(tableName: String, columnName: String, sql: String) {
        if (tableColumns(tableName).contains(columnName)) return
        execSQL(sql)
    }

    private fun SQLiteDatabase.normalizeDailyHealthSummaries() {
        val columns = tableColumns("daily_health_summaries")
        if (columns == CURRENT_DAILY_HEALTH_SUMMARY_COLUMNS) return

        execSQL("DROP TABLE IF EXISTS daily_health_summaries_room_repair")
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS daily_health_summaries_room_repair (
                dateEpochDay INTEGER NOT NULL PRIMARY KEY,
                steps INTEGER,
                activeCaloriesKcal REAL,
                latestWeightKg REAL,
                restingHeartRateBpm INTEGER,
                updatedAtEpochMillis INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT OR REPLACE INTO daily_health_summaries_room_repair (
                dateEpochDay, steps, activeCaloriesKcal, latestWeightKg,
                restingHeartRateBpm, updatedAtEpochMillis
            )
            SELECT dateEpochDay, steps, activeCaloriesKcal, latestWeightKg,
                   restingHeartRateBpm, updatedAtEpochMillis
            FROM daily_health_summaries
            """.trimIndent(),
        )
        execSQL("DROP TABLE daily_health_summaries")
        execSQL("ALTER TABLE daily_health_summaries_room_repair RENAME TO daily_health_summaries")
    }

    private fun SQLiteDatabase.tableColumns(tableName: String): List<String> =
        rawQuery("PRAGMA table_info(`$tableName`)", null).use { cursor ->
            buildList {
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) {
                    add(cursor.getString(nameIndex))
                }
            }
        }

    private const val DATABASE_NAME = "musfit.db"
    internal const val CURRENT_V28_IDENTITY_HASH = "09b1d1975301639eaff70e11601ed13b"
    private const val LEGACY_HEALTH_SYNC_V28_IDENTITY_HASH = "71b5b71f394a9a0bedf45d1a67317f04"

    private val CURRENT_DAILY_HEALTH_SUMMARY_COLUMNS = listOf(
        "dateEpochDay",
        "steps",
        "activeCaloriesKcal",
        "latestWeightKg",
        "restingHeartRateBpm",
        "updatedAtEpochMillis",
    )

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

    internal val MIGRATION_19_20 =
        object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS accounts (
                        id TEXT NOT NULL PRIMARY KEY,
                        displayName TEXT NOT NULL,
                        email TEXT,
                        remoteUserId TEXT,
                        createdAtEpochMillis INTEGER NOT NULL,
                        updatedAtEpochMillis INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_email ON accounts(email)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_accounts_remoteUserId ON accounts(remoteUserId)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS account_session (
                        `key` TEXT NOT NULL PRIMARY KEY,
                        activeAccountId TEXT NOT NULL,
                        updatedAtEpochMillis INTEGER NOT NULL,
                        FOREIGN KEY(activeAccountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_account_session_activeAccountId ON account_session(activeAccountId)")
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO accounts (
                        id, displayName, email, remoteUserId, createdAtEpochMillis, updatedAtEpochMillis
                    ) VALUES (
                        'local-default', 'You', NULL, NULL, 0, 0
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO account_session (`key`, activeAccountId, updatedAtEpochMillis)
                    VALUES ('active', 'local-default', 0)
                    """.trimIndent(),
                )
            }
        }

    internal val MIGRATION_20_21 =
        object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    INSERT OR REPLACE INTO user_profile (
                        id, sex, birthDateEpochDay, heightCm, activityLevel, goalType,
                        goalPaceKgPerWeek, goalWeightKg, updatedAtEpochMillis
                    )
                    SELECT
                        'local-default', sex, birthDateEpochDay, heightCm, activityLevel, goalType,
                        goalPaceKgPerWeek, goalWeightKg, updatedAtEpochMillis
                    FROM user_profile
                    WHERE id = 'user'
                    """.trimIndent(),
                )
                db.execSQL("DELETE FROM user_profile WHERE id = 'user'")
                db.execSQL(
                    """
                    INSERT OR REPLACE INTO app_settings (
                        id, unitSystem, themeMode, updatedAtEpochMillis
                    )
                    SELECT
                        'local-default', unitSystem, themeMode, updatedAtEpochMillis
                    FROM app_settings
                    WHERE id = 'app'
                    """.trimIndent(),
                )
                db.execSQL("DELETE FROM app_settings WHERE id = 'app'")
                db.execSQL(
                    """
                    INSERT OR REPLACE INTO user_goals (
                        id, stepGoal, weeklySessionTarget, targetWeightKg, updatedAtEpochMillis
                    )
                    SELECT
                        'local-default', stepGoal, weeklySessionTarget, targetWeightKg, updatedAtEpochMillis
                    FROM user_goals
                    WHERE id = 'default'
                    """.trimIndent(),
                )
                db.execSQL("DELETE FROM user_goals WHERE id = 'default'")
            }
        }

    internal val MIGRATION_21_22 =
        object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN primaryMuscles TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE exercises ADD COLUMN secondaryMuscles TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE exercises ADD COLUMN instructions TEXT")
                db.execSQL("ALTER TABLE exercises ADD COLUMN localNotes TEXT")
                db.execSQL("UPDATE exercises SET primaryMuscles = targetMuscles WHERE primaryMuscles = ''")
            }
        }

    internal val MIGRATION_22_23 =
        object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routines ADD COLUMN programName TEXT")
                db.execSQL("ALTER TABLE routines ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
            }
        }

    internal val MIGRATION_23_24 =
        object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS training_settings (
                        id TEXT NOT NULL PRIMARY KEY,
                        defaultRestSeconds INTEGER NOT NULL,
                        barWeightKg REAL NOT NULL,
                        availablePlatesKg TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO training_settings (
                        id, defaultRestSeconds, barWeightKg, availablePlatesKg
                    ) VALUES (
                        'default', 120, 20.0, '25,20,15,10,5,2.5,1.25'
                    )
                    """.trimIndent(),
                )
            }
        }

    internal val MIGRATION_24_25 =
        object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN imageUrl TEXT")
                db.execSQL("ALTER TABLE exercises ADD COLUMN gifUrl TEXT")
            }
        }

    internal val MIGRATION_25_26 =
        object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS coach_messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        dayEpochDay INTEGER NOT NULL,
                        ruleKey TEXT NOT NULL,
                        category TEXT NOT NULL,
                        title TEXT NOT NULL,
                        body TEXT NOT NULL,
                        actionType TEXT,
                        actionData TEXT,
                        firstSeenAtEpochMillis INTEGER NOT NULL,
                        isRead INTEGER NOT NULL,
                        isDismissed INTEGER NOT NULL,
                        source TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_coach_messages_dayEpochDay_ruleKey_source " +
                        "ON coach_messages(dayEpochDay, ruleKey, source)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_coach_messages_dayEpochDay ON coach_messages(dayEpochDay)",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS dashboard_pins (
                        metricId TEXT NOT NULL PRIMARY KEY,
                        position INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                // Seed defaults so existing users see the default carousel after upgrade.
                db.execSQL(
                    "INSERT OR IGNORE INTO dashboard_pins (metricId, position) " +
                        "VALUES ('calories', 0), ('steps', 1), ('protein', 2)",
                )
            }
        }

    internal val MIGRATION_26_27 =
        object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Data-only: user_profile.goalWeightKg becomes the canonical target weight. The runtime
                // read paths (since retired) preferred user_goals.targetWeightKg when > 0, so
                // that value is carried over — recency-aware, and creating the profile row
                // when the user only ever set the target in Today's sheet.
                db.execSQL(
                    """
                    INSERT INTO user_profile (
                        id, sex, birthDateEpochDay, heightCm, activityLevel, goalType,
                        goalPaceKgPerWeek, goalWeightKg, updatedAtEpochMillis
                    )
                    SELECT g.id, NULL, NULL, NULL, 'Moderate', 'Maintain', 0.5,
                           g.targetWeightKg, g.updatedAtEpochMillis
                    FROM user_goals g
                    WHERE g.targetWeightKg > 0
                      AND NOT EXISTS (SELECT 1 FROM user_profile p WHERE p.id = g.id)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    UPDATE user_profile SET goalWeightKg = (
                        SELECT g.targetWeightKg FROM user_goals g WHERE g.id = user_profile.id
                    )
                    WHERE EXISTS (
                        SELECT 1 FROM user_goals g
                        WHERE g.id = user_profile.id AND g.targetWeightKg > 0
                          AND (user_profile.goalWeightKg IS NULL OR user_profile.goalWeightKg <= 0
                               OR g.updatedAtEpochMillis > user_profile.updatedAtEpochMillis)
                    )
                    """.trimIndent(),
                )
            }
        }

    internal val MIGRATION_27_28 =
        object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN authProvider TEXT NOT NULL DEFAULT 'local'")
                db.execSQL("ALTER TABLE accounts ADD COLUMN avatarUrl TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_authProvider ON accounts(authProvider)")
            }
        }

    internal val MIGRATION_28_29 =
        object : Migration(28, 29) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ai_coach_settings (
                        accountId TEXT NOT NULL PRIMARY KEY,
                        providerKind TEXT NOT NULL,
                        baseUrl TEXT NOT NULL,
                        modelName TEXT NOT NULL,
                        localAgentKind TEXT NOT NULL,
                        apiKeyStored INTEGER NOT NULL,
                        updatedAtEpochMillis INTEGER NOT NULL,
                        FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
            }
        }
}
