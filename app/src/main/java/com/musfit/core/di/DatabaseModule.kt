package com.musfit.core.di

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.musfit.data.local.MUSFIT_DATABASE_NAME
import com.musfit.data.local.MusFitDatabase
import com.musfit.data.local.dao.AccountDao
import com.musfit.data.local.dao.AiCoachChatDao
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
        repairLegacyVersion28Database(context, MUSFIT_DATABASE_NAME)
        return Room.databaseBuilder(context, MusFitDatabase::class.java, MUSFIT_DATABASE_NAME)
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
                MIGRATION_29_30,
                MIGRATION_30_31,
                MIGRATION_31_32,
                MIGRATION_32_33,
                MIGRATION_33_34,
                MIGRATION_34_35,
                MIGRATION_35_36,
                MIGRATION_36_37,
                MIGRATION_37_38,
                MIGRATION_38_39,
                MIGRATION_39_40,
                MIGRATION_40_41,
                MIGRATION_41_42,
            )
            .addCallback(FOOD_PERFORMANCE_INDEX_CALLBACK)
            .addCallback(TRAINING_PERFORMANCE_INDEX_CALLBACK)
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
    fun provideAiCoachChatDao(database: MusFitDatabase): AiCoachChatDao = database.aiCoachChatDao()

    @Provides
    fun provideCoachDao(database: MusFitDatabase): CoachDao = database.coachDao()

    internal fun repairLegacyVersion28Database(context: Context, databaseName: String = MUSFIT_DATABASE_NAME) {
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

    private fun SQLiteDatabase.readRoomIdentityHash(): String? = if (!tableExists("room_master_table")) {
        null
    } else {
        rawQuery("SELECT identity_hash FROM room_master_table WHERE id = 42 LIMIT 1", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    private fun SQLiteDatabase.tableExists(tableName: String): Boolean = rawQuery(
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

    private fun SQLiteDatabase.tableColumns(tableName: String): List<String> = rawQuery("PRAGMA table_info(`$tableName`)", null).use { cursor ->
        buildList {
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                add(cursor.getString(nameIndex))
            }
        }
    }

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

    internal val MIGRATION_29_30 =
        object : Migration(29, 30) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_health_summaries ADD COLUMN totalCaloriesKcal REAL")
                db.execSQL("ALTER TABLE daily_health_summaries ADD COLUMN distanceMeters REAL")
                db.execSQL("ALTER TABLE daily_health_summaries ADD COLUMN sleepMinutes INTEGER")
                db.execSQL("ALTER TABLE daily_health_summaries ADD COLUMN exerciseMinutes INTEGER")
                db.execSQL("ALTER TABLE daily_health_summaries ADD COLUMN exerciseSessionCount INTEGER")
                db.execSQL("ALTER TABLE daily_health_summaries ADD COLUMN latestBodyFatPercent REAL")
            }
        }

    internal val MIGRATION_30_31 =
        object : Migration(30, 31) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS routine_folders (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL,
                        updatedAtEpochMillis INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("ALTER TABLE routines ADD COLUMN folderId TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_routines_folderId ON routines(folderId)")
                db.execSQL("ALTER TABLE routine_exercises ADD COLUMN restSeconds INTEGER")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS routine_exercise_sets (
                        id TEXT NOT NULL PRIMARY KEY,
                        routineExerciseId TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        setType TEXT NOT NULL,
                        targetReps TEXT,
                        targetWeightKg REAL,
                        FOREIGN KEY(routineExerciseId) REFERENCES routine_exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_routine_exercise_sets_routineExerciseId " +
                        "ON routine_exercise_sets(routineExerciseId)",
                )
                db.execSQL("ALTER TABLE workout_sets ADD COLUMN restSeconds INTEGER")

                db.execSQL(
                    """
                    INSERT OR IGNORE INTO routine_folders (
                        id, name, sortOrder, createdAtEpochMillis, updatedAtEpochMillis
                    )
                    SELECT
                        'folder-' ||
                            REPLACE(
                                REPLACE(
                                    REPLACE(LOWER(TRIM(programName)), ' ', '-'),
                                    '/',
                                    '-'
                                ),
                                '&',
                                'and'
                            ) AS id,
                        TRIM(programName) AS name,
                        0 AS sortOrder,
                        COALESCE(MIN(createdAtEpochMillis), 0) AS createdAtEpochMillis,
                        COALESCE(MAX(updatedAtEpochMillis), 0) AS updatedAtEpochMillis
                    FROM routines
                    WHERE programName IS NOT NULL AND TRIM(programName) != ''
                    GROUP BY TRIM(programName)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    UPDATE routines
                    SET folderId =
                        'folder-' ||
                        REPLACE(
                            REPLACE(
                                REPLACE(LOWER(TRIM(programName)), ' ', '-'),
                                '/',
                                '-'
                            ),
                            '&',
                            'and'
                        )
                    WHERE programName IS NOT NULL AND TRIM(programName) != ''
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    WITH RECURSIVE set_numbers(routineExerciseId, targetReps, n, maxSets) AS (
                        SELECT
                            id,
                            targetReps,
                            0,
                            CASE WHEN targetSets < 1 THEN 1 ELSE targetSets END
                        FROM routine_exercises
                        UNION ALL
                        SELECT routineExerciseId, targetReps, n + 1, maxSets
                        FROM set_numbers
                        WHERE n + 1 < maxSets
                    )
                    INSERT OR IGNORE INTO routine_exercise_sets (
                        id, routineExerciseId, sortOrder, setType, targetReps, targetWeightKg
                    )
                    SELECT
                        routineExerciseId || '-set-' || n,
                        routineExerciseId,
                        n,
                        'working',
                        targetReps,
                        NULL
                    FROM set_numbers
                    """.trimIndent(),
                )
            }
        }

    internal val MIGRATION_31_32 =
        object : Migration(31, 32) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE meal_definitions ADD COLUMN isHidden INTEGER NOT NULL DEFAULT 0")
            }
        }

    internal val MIGRATION_32_33 =
        object : Migration(32, 33) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE health_connect_sync_state ADD COLUMN preferredStepsPackage TEXT",
                )
            }
        }

    internal val MIGRATION_33_34 =
        object : Migration(33, 34) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_health_summaries ADD COLUMN hrvRmssdMillis REAL")
            }
        }

    internal val MIGRATION_34_35 =
        object : Migration(34, 35) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ai_coach_threads (
                        id TEXT NOT NULL PRIMARY KEY,
                        accountId TEXT NOT NULL,
                        providerKind TEXT NOT NULL,
                        localAgentKind TEXT NOT NULL,
                        remoteSessionId TEXT,
                        createdAtEpochMillis INTEGER NOT NULL,
                        updatedAtEpochMillis INTEGER NOT NULL,
                        FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_ai_coach_threads_accountId_providerKind_localAgentKind
                    ON ai_coach_threads(accountId, providerKind, localAgentKind)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ai_coach_chat_messages (
                        id TEXT NOT NULL PRIMARY KEY,
                        threadId TEXT NOT NULL,
                        role TEXT NOT NULL,
                        content TEXT NOT NULL,
                        status TEXT NOT NULL,
                        errorMessage TEXT,
                        createdAtEpochMillis INTEGER NOT NULL,
                        FOREIGN KEY(threadId) REFERENCES ai_coach_threads(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_ai_coach_chat_messages_threadId_createdAtEpochMillis
                    ON ai_coach_chat_messages(threadId, createdAtEpochMillis)
                    """.trimIndent(),
                )
            }
        }

    // Data-only: the Turn 8 vitals grid defaults to four tiles. Appends the water
    // pin only for users still on the untouched 25→26 three-pin default — any
    // customized pin set (different metrics, order, or count) is left alone.
    internal val MIGRATION_35_36 =
        object : Migration(35, 36) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO dashboard_pins (metricId, position)
                    SELECT 'water', 3
                    WHERE (SELECT COUNT(*) FROM dashboard_pins) = 3
                        AND EXISTS(SELECT 1 FROM dashboard_pins WHERE metricId = 'calories' AND position = 0)
                        AND EXISTS(SELECT 1 FROM dashboard_pins WHERE metricId = 'steps' AND position = 1)
                        AND EXISTS(SELECT 1 FROM dashboard_pins WHERE metricId = 'protein' AND position = 2)
                    """.trimIndent(),
                )
            }
        }

    internal val MIGRATION_36_37 =
        object : Migration(36, 37) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO accounts (
                        id, displayName, email, remoteUserId, authProvider, avatarUrl,
                        createdAtEpochMillis, updatedAtEpochMillis
                    ) VALUES ('local-default', 'You', NULL, NULL, 'local', NULL, 0, 0)
                    """.trimIndent(),
                )

                val tables =
                    listOf(
                        "food_servings",
                        "meal_items",
                        "meal_template_items",
                        "recipe_ingredients",
                        "barcode_products",
                        "foods",
                        "meals",
                        "meal_definitions",
                        "shopping_list_items",
                        "water_entries",
                        "food_health_connect_sync",
                        "food_goals",
                        "quick_calorie_presets",
                        "meal_templates",
                        "recipes",
                    )
                tables.forEach { table -> db.execSQL("ALTER TABLE `$table` RENAME TO `${table}_legacy`") }

                db.execSQL(
                    """
                    CREATE TABLE foods (
                        accountId TEXT NOT NULL, id TEXT NOT NULL, name TEXT NOT NULL, brand TEXT,
                        defaultServingGrams REAL NOT NULL, caloriesPer100g REAL NOT NULL,
                        proteinPer100g REAL NOT NULL, carbsPer100g REAL NOT NULL, fatPer100g REAL NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL,
                        servingName TEXT, barcode TEXT, category TEXT, isFavorite INTEGER NOT NULL DEFAULT 0,
                        fiberPer100g REAL NOT NULL DEFAULT 0, sugarPer100g REAL NOT NULL DEFAULT 0,
                        saturatedFatPer100g REAL NOT NULL DEFAULT 0, sodiumMgPer100g REAL NOT NULL DEFAULT 0,
                        potassiumMgPer100g REAL NOT NULL DEFAULT 0, calciumMgPer100g REAL NOT NULL DEFAULT 0,
                        ironMgPer100g REAL NOT NULL DEFAULT 0, vitaminDMcgPer100g REAL NOT NULL DEFAULT 0,
                        vitaminCMgPer100g REAL NOT NULL DEFAULT 0, magnesiumMgPer100g REAL NOT NULL DEFAULT 0,
                        imageUrl TEXT, PRIMARY KEY(accountId, id),
                        FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE meals (
                        accountId TEXT NOT NULL, id TEXT NOT NULL, dateEpochDay INTEGER NOT NULL,
                        type TEXT NOT NULL, notes TEXT, createdAtEpochMillis INTEGER NOT NULL,
                        updatedAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(accountId, id),
                        FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE meal_definitions (
                        accountId TEXT NOT NULL, id TEXT NOT NULL, name TEXT NOT NULL, timeMinutes INTEGER,
                        sortOrder INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL,
                        updatedAtEpochMillis INTEGER NOT NULL, isHidden INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(accountId, id),
                        FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE shopping_list_items (
                        accountId TEXT NOT NULL, id TEXT NOT NULL, name TEXT NOT NULL, category TEXT NOT NULL,
                        quantityGrams REAL NOT NULL, isChecked INTEGER NOT NULL, isManual INTEGER NOT NULL,
                        sourceKey TEXT, sortOrder INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL,
                        updatedAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(accountId, id),
                        FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE water_entries (
                        accountId TEXT NOT NULL, id TEXT NOT NULL, dateEpochDay INTEGER NOT NULL,
                        amountMilliliters REAL NOT NULL, createdAtEpochMillis INTEGER NOT NULL,
                        PRIMARY KEY(accountId, id),
                        FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE food_health_connect_sync (
                        accountId TEXT NOT NULL, `key` TEXT NOT NULL, isEnabled INTEGER NOT NULL,
                        lastSyncAtEpochMillis INTEGER, lastFailureMessage TEXT,
                        updatedAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(accountId, `key`),
                        FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE food_goals (
                        accountId TEXT NOT NULL, id TEXT NOT NULL, dailyCaloriesKcal REAL NOT NULL,
                        proteinGrams REAL NOT NULL, carbsGrams REAL NOT NULL, fatGrams REAL NOT NULL,
                        fiberGrams REAL NOT NULL, sugarGrams REAL NOT NULL, saturatedFatGrams REAL NOT NULL,
                        sodiumMilligrams REAL NOT NULL, mode TEXT NOT NULL, includeTrainingCalories INTEGER NOT NULL,
                        useNetCarbs INTEGER NOT NULL DEFAULT 0, waterGoalMilliliters REAL NOT NULL DEFAULT 2000,
                        updatedAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(accountId, id),
                        FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE quick_calorie_presets (
                        accountId TEXT NOT NULL, id TEXT NOT NULL, name TEXT NOT NULL, caloriesKcal REAL NOT NULL,
                        proteinGrams REAL NOT NULL, carbsGrams REAL NOT NULL, fatGrams REAL NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL,
                        isFavorite INTEGER NOT NULL DEFAULT 1, PRIMARY KEY(accountId, id),
                        FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE meal_templates (
                        accountId TEXT NOT NULL, id TEXT NOT NULL, name TEXT NOT NULL, mealType TEXT NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL,
                        isFavorite INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(accountId, id),
                        FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE recipes (
                        accountId TEXT NOT NULL, id TEXT NOT NULL, name TEXT NOT NULL, category TEXT,
                        servingName TEXT NOT NULL, servingGrams REAL NOT NULL, servings REAL NOT NULL DEFAULT 1,
                        cookedYieldGrams REAL NOT NULL DEFAULT 0, createdAtEpochMillis INTEGER NOT NULL,
                        updatedAtEpochMillis INTEGER NOT NULL, isFavorite INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(accountId, id),
                        FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE food_servings (
                        accountId TEXT NOT NULL, id TEXT NOT NULL, foodId TEXT NOT NULL, label TEXT NOT NULL,
                        grams REAL NOT NULL, PRIMARY KEY(accountId, id),
                        FOREIGN KEY(accountId, foodId) REFERENCES foods(accountId, id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE meal_items (
                        accountId TEXT NOT NULL, id TEXT NOT NULL, mealId TEXT NOT NULL, foodId TEXT NOT NULL,
                        quantityGrams REAL NOT NULL, status TEXT NOT NULL DEFAULT 'logged',
                        PRIMARY KEY(accountId, id),
                        FOREIGN KEY(accountId, mealId) REFERENCES meals(accountId, id)
                            ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(accountId, foodId) REFERENCES foods(accountId, id)
                            ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE meal_template_items (
                        accountId TEXT NOT NULL, id TEXT NOT NULL, templateId TEXT NOT NULL, foodId TEXT NOT NULL,
                        quantityGrams REAL NOT NULL, sortOrder INTEGER NOT NULL, PRIMARY KEY(accountId, id),
                        FOREIGN KEY(accountId, templateId) REFERENCES meal_templates(accountId, id)
                            ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(accountId, foodId) REFERENCES foods(accountId, id)
                            ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE recipe_ingredients (
                        accountId TEXT NOT NULL, id TEXT NOT NULL, recipeId TEXT NOT NULL, foodId TEXT NOT NULL,
                        quantityGrams REAL NOT NULL, unitLabel TEXT NOT NULL DEFAULT 'g',
                        unitGrams REAL NOT NULL DEFAULT 1, unitQuantity REAL NOT NULL DEFAULT 0,
                        sortOrder INTEGER NOT NULL, PRIMARY KEY(accountId, id),
                        FOREIGN KEY(accountId, recipeId) REFERENCES recipes(accountId, id)
                            ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(accountId, foodId) REFERENCES foods(accountId, id)
                            ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE barcode_products (
                        accountId TEXT NOT NULL, id TEXT NOT NULL, barcode TEXT NOT NULL, provider TEXT NOT NULL,
                        providerProductName TEXT, providerBrand TEXT, rawJson TEXT NOT NULL, quality TEXT NOT NULL,
                        linkedFoodAccountId TEXT, linkedFoodId TEXT, fetchedAtEpochMillis INTEGER NOT NULL,
                        PRIMARY KEY(accountId, id),
                        FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(linkedFoodAccountId, linkedFoodId) REFERENCES foods(accountId, id)
                            ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent(),
                )

                db.execSQL(
                    """
                    INSERT INTO foods SELECT 'local-default', id, name, brand, defaultServingGrams,
                        caloriesPer100g, proteinPer100g, carbsPer100g, fatPer100g,
                        createdAtEpochMillis, updatedAtEpochMillis, servingName, barcode, category,
                        isFavorite, fiberPer100g, sugarPer100g, saturatedFatPer100g, sodiumMgPer100g,
                        potassiumMgPer100g, calciumMgPer100g, ironMgPer100g, vitaminDMcgPer100g,
                        vitaminCMgPer100g, magnesiumMgPer100g, imageUrl FROM foods_legacy
                    """.trimIndent(),
                )
                db.execSQL("INSERT INTO meals SELECT 'local-default', * FROM meals_legacy")
                db.execSQL("INSERT INTO meal_definitions SELECT 'local-default', * FROM meal_definitions_legacy")
                db.execSQL("INSERT INTO shopping_list_items SELECT 'local-default', * FROM shopping_list_items_legacy")
                db.execSQL("INSERT INTO water_entries SELECT 'local-default', * FROM water_entries_legacy")
                db.execSQL("INSERT INTO food_health_connect_sync SELECT 'local-default', * FROM food_health_connect_sync_legacy")
                db.execSQL("INSERT INTO food_goals SELECT 'local-default', * FROM food_goals_legacy")
                db.execSQL("INSERT INTO quick_calorie_presets SELECT 'local-default', * FROM quick_calorie_presets_legacy")
                db.execSQL("INSERT INTO meal_templates SELECT 'local-default', * FROM meal_templates_legacy")
                db.execSQL("INSERT INTO recipes SELECT 'local-default', * FROM recipes_legacy")
                db.execSQL("INSERT INTO food_servings SELECT 'local-default', * FROM food_servings_legacy")
                db.execSQL("INSERT INTO meal_items SELECT 'local-default', * FROM meal_items_legacy")
                db.execSQL("INSERT INTO meal_template_items SELECT 'local-default', * FROM meal_template_items_legacy")
                db.execSQL("INSERT INTO recipe_ingredients SELECT 'local-default', * FROM recipe_ingredients_legacy")
                db.execSQL(
                    """
                    INSERT INTO barcode_products (
                        accountId, id, barcode, provider, providerProductName, providerBrand, rawJson,
                        quality, linkedFoodAccountId, linkedFoodId, fetchedAtEpochMillis
                    ) SELECT 'local-default', id, barcode, provider, providerProductName, providerBrand,
                        rawJson, quality,
                        CASE WHEN linkedFoodId IS NULL THEN NULL ELSE 'local-default' END,
                        linkedFoodId, fetchedAtEpochMillis FROM barcode_products_legacy
                    """.trimIndent(),
                )

                listOf(
                    "food_servings",
                    "meal_items",
                    "meal_template_items",
                    "recipe_ingredients",
                    "barcode_products",
                    "foods",
                    "meals",
                    "meal_definitions",
                    "shopping_list_items",
                    "water_entries",
                    "food_health_connect_sync",
                    "food_goals",
                    "quick_calorie_presets",
                    "meal_templates",
                    "recipes",
                ).forEach { table -> db.execSQL("DROP TABLE `${table}_legacy`") }

                val indexes =
                    listOf(
                        "CREATE INDEX index_foods_accountId_barcode ON foods(accountId, barcode)",
                        "CREATE INDEX index_foods_accountId_name ON foods(accountId, name)",
                        "CREATE INDEX index_foods_accountId_brand ON foods(accountId, brand)",
                        "CREATE INDEX index_foods_accountId_category ON foods(accountId, category)",
                        "CREATE INDEX index_foods_accountId_isFavorite ON foods(accountId, isFavorite)",
                        "CREATE INDEX index_food_servings_accountId_foodId_label ON food_servings(accountId, foodId, label)",
                        "CREATE INDEX index_meals_accountId_dateEpochDay_createdAtEpochMillis ON meals(accountId, dateEpochDay, createdAtEpochMillis)",
                        "CREATE INDEX index_meals_accountId_dateEpochDay_type_createdAtEpochMillis ON meals(accountId, dateEpochDay, type, createdAtEpochMillis)",
                        "CREATE INDEX index_meal_definitions_accountId_sortOrder_name ON meal_definitions(accountId, sortOrder, name)",
                        "CREATE INDEX index_meal_items_accountId_mealId_status ON meal_items(accountId, mealId, status)",
                        "CREATE INDEX index_meal_items_accountId_foodId ON meal_items(accountId, foodId)",
                        "CREATE INDEX index_shopping_list_items_accountId_category ON shopping_list_items(accountId, category)",
                        "CREATE UNIQUE INDEX index_shopping_list_items_accountId_sourceKey ON shopping_list_items(accountId, sourceKey)",
                        "CREATE INDEX index_shopping_list_items_accountId_isManual ON shopping_list_items(accountId, isManual)",
                        "CREATE INDEX index_water_entries_accountId_dateEpochDay ON water_entries(accountId, dateEpochDay)",
                        "CREATE UNIQUE INDEX index_barcode_products_accountId_barcode ON barcode_products(accountId, barcode)",
                        "CREATE INDEX index_barcode_products_accountId_linkedFoodId ON barcode_products(accountId, linkedFoodId)",
                        "CREATE INDEX index_barcode_products_linkedFoodAccountId_linkedFoodId ON barcode_products(linkedFoodAccountId, linkedFoodId)",
                        "CREATE INDEX index_quick_calorie_presets_accountId_isFavorite_updatedAtEpochMillis_name ON quick_calorie_presets(accountId, isFavorite, updatedAtEpochMillis, name)",
                        "CREATE INDEX index_meal_templates_accountId_updatedAtEpochMillis ON meal_templates(accountId, updatedAtEpochMillis)",
                        "CREATE INDEX index_meal_template_items_accountId_templateId_sortOrder ON meal_template_items(accountId, templateId, sortOrder)",
                        "CREATE INDEX index_meal_template_items_accountId_foodId ON meal_template_items(accountId, foodId)",
                        "CREATE INDEX index_recipes_accountId_updatedAtEpochMillis ON recipes(accountId, updatedAtEpochMillis)",
                        "CREATE INDEX index_recipes_accountId_name ON recipes(accountId, name)",
                        "CREATE INDEX index_recipe_ingredients_accountId_recipeId_sortOrder ON recipe_ingredients(accountId, recipeId, sortOrder)",
                        "CREATE INDEX index_recipe_ingredients_accountId_foodId ON recipe_ingredients(accountId, foodId)",
                    )
                indexes.forEach(db::execSQL)
            }
        }

    internal val MIGRATION_37_38 =
        object : Migration(37, 38) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO accounts (
                        id, displayName, email, remoteUserId, authProvider, avatarUrl,
                        createdAtEpochMillis, updatedAtEpochMillis
                    ) VALUES ('local-default', 'You', NULL, NULL, 'local', NULL, 0, 0)
                    """.trimIndent(),
                )

                listOf(
                    "routine_exercise_sets",
                    "routine_exercises",
                    "workout_sets",
                    "workout_sessions",
                    "routines",
                    "routine_folders",
                    "training_settings",
                    "exercises",
                ).forEach { table -> db.execSQL("ALTER TABLE `$table` RENAME TO `${table}_legacy`") }

                db.execSQL(
                    """
                    CREATE TABLE exercises (
                        id TEXT NOT NULL, accountId TEXT, name TEXT NOT NULL, category TEXT NOT NULL,
                        equipment TEXT, targetMuscles TEXT NOT NULL, isCustom INTEGER NOT NULL,
                        primaryMuscles TEXT NOT NULL, secondaryMuscles TEXT NOT NULL, instructions TEXT,
                        imageUrl TEXT, gifUrl TEXT, PRIMARY KEY(id),
                        FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE exercise_notes (
                        accountId TEXT NOT NULL, exerciseId TEXT NOT NULL, notes TEXT NOT NULL,
                        PRIMARY KEY(accountId, exerciseId),
                        FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE routine_folders (
                        accountId TEXT NOT NULL, id TEXT NOT NULL, name TEXT NOT NULL, sortOrder INTEGER NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL,
                        PRIMARY KEY(accountId, id),
                        FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE routines (
                        accountId TEXT NOT NULL, id TEXT NOT NULL, name TEXT NOT NULL, notes TEXT,
                        createdAtEpochMillis INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL,
                        isStarter INTEGER NOT NULL, programName TEXT, tags TEXT NOT NULL, folderId TEXT,
                        PRIMARY KEY(accountId, id),
                        FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(accountId, folderId) REFERENCES routine_folders(accountId, id)
                            ON UPDATE NO ACTION ON DELETE NO ACTION
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE routine_exercises (
                        accountId TEXT NOT NULL, id TEXT NOT NULL, routineId TEXT NOT NULL,
                        exerciseId TEXT NOT NULL, sortOrder INTEGER NOT NULL, targetSets INTEGER NOT NULL,
                        targetReps TEXT, restSeconds INTEGER, PRIMARY KEY(accountId, id),
                        FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(accountId, routineId) REFERENCES routines(accountId, id)
                            ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE routine_exercise_sets (
                        accountId TEXT NOT NULL, id TEXT NOT NULL, routineExerciseId TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL, setType TEXT NOT NULL, targetReps TEXT,
                        targetWeightKg REAL, PRIMARY KEY(accountId, id),
                        FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(accountId, routineExerciseId) REFERENCES routine_exercises(accountId, id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE workout_sessions (
                        accountId TEXT NOT NULL, id TEXT NOT NULL, routineId TEXT, title TEXT,
                        status TEXT NOT NULL, startedAtEpochMillis INTEGER NOT NULL,
                        endedAtEpochMillis INTEGER, notes TEXT, healthConnectRecordId TEXT,
                        healthConnectLastExportedAtEpochMillis INTEGER, PRIMARY KEY(accountId, id),
                        FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(accountId, routineId) REFERENCES routines(accountId, id)
                            ON UPDATE NO ACTION ON DELETE NO ACTION
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE workout_sets (
                        accountId TEXT NOT NULL, id TEXT NOT NULL, sessionId TEXT NOT NULL,
                        exerciseId TEXT NOT NULL, sortOrder INTEGER NOT NULL, setType TEXT NOT NULL,
                        reps INTEGER, weightKg REAL, durationSeconds INTEGER, distanceMeters REAL,
                        rpe REAL, notes TEXT, completed INTEGER NOT NULL, supersetGroupId TEXT,
                        restSeconds INTEGER, PRIMARY KEY(accountId, id),
                        FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(accountId, sessionId) REFERENCES workout_sessions(accountId, id)
                            ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE training_settings (
                        accountId TEXT NOT NULL, id TEXT NOT NULL, defaultRestSeconds INTEGER NOT NULL,
                        barWeightKg REAL NOT NULL, availablePlatesKg TEXT NOT NULL,
                        PRIMARY KEY(accountId, id),
                        FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )

                db.execSQL(
                    """
                    INSERT INTO exercises (
                        id, accountId, name, category, equipment, targetMuscles, isCustom,
                        primaryMuscles, secondaryMuscles, instructions, imageUrl, gifUrl
                    ) SELECT id, CASE WHEN isCustom = 1 THEN 'local-default' ELSE NULL END,
                        name, category, equipment, targetMuscles, isCustom, primaryMuscles,
                        secondaryMuscles, instructions, imageUrl, gifUrl FROM exercises_legacy
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO exercise_notes(accountId, exerciseId, notes)
                    SELECT 'local-default', id, localNotes FROM exercises_legacy
                    WHERE localNotes IS NOT NULL AND TRIM(localNotes) != ''
                    """.trimIndent(),
                )
                db.execSQL("INSERT INTO routine_folders SELECT 'local-default', * FROM routine_folders_legacy")
                db.execSQL("INSERT INTO routines SELECT 'local-default', * FROM routines_legacy")
                db.execSQL("INSERT INTO routine_exercises SELECT 'local-default', * FROM routine_exercises_legacy")
                db.execSQL("INSERT INTO routine_exercise_sets SELECT 'local-default', * FROM routine_exercise_sets_legacy")
                db.execSQL("INSERT INTO workout_sessions SELECT 'local-default', * FROM workout_sessions_legacy")
                db.execSQL("INSERT INTO workout_sets SELECT 'local-default', * FROM workout_sets_legacy")
                db.execSQL("INSERT INTO training_settings SELECT 'local-default', * FROM training_settings_legacy")

                listOf(
                    "routine_exercise_sets",
                    "routine_exercises",
                    "workout_sets",
                    "workout_sessions",
                    "routines",
                    "routine_folders",
                    "training_settings",
                    "exercises",
                ).forEach { table -> db.execSQL("DROP TABLE `${table}_legacy`") }

                listOf(
                    "CREATE INDEX index_exercises_accountId ON exercises(accountId)",
                    "CREATE INDEX index_exercises_accountId_name ON exercises(accountId, name)",
                    "CREATE INDEX index_exercise_notes_exerciseId ON exercise_notes(exerciseId)",
                    "CREATE INDEX index_routine_folders_accountId_sortOrder_name ON routine_folders(accountId, sortOrder, name)",
                    "CREATE INDEX index_routines_accountId_folderId ON routines(accountId, folderId)",
                    "CREATE INDEX index_routines_accountId_updatedAtEpochMillis ON routines(accountId, updatedAtEpochMillis)",
                    "CREATE INDEX index_routine_exercises_accountId_routineId_sortOrder ON routine_exercises(accountId, routineId, sortOrder)",
                    "CREATE INDEX index_routine_exercises_accountId_exerciseId ON routine_exercises(accountId, exerciseId)",
                    "CREATE INDEX index_routine_exercises_exerciseId ON routine_exercises(exerciseId)",
                    "CREATE INDEX index_routine_exercise_sets_accountId_routineExerciseId_sortOrder ON routine_exercise_sets(accountId, routineExerciseId, sortOrder)",
                    "CREATE INDEX index_workout_sessions_accountId_routineId ON workout_sessions(accountId, routineId)",
                    "CREATE INDEX index_workout_sessions_accountId_startedAtEpochMillis ON workout_sessions(accountId, startedAtEpochMillis)",
                    "CREATE INDEX index_workout_sessions_accountId_status_startedAtEpochMillis ON workout_sessions(accountId, status, startedAtEpochMillis)",
                    "CREATE INDEX index_workout_sets_accountId_sessionId_sortOrder ON workout_sets(accountId, sessionId, sortOrder)",
                    "CREATE INDEX index_workout_sets_accountId_exerciseId ON workout_sets(accountId, exerciseId)",
                    "CREATE INDEX index_workout_sets_exerciseId ON workout_sets(exerciseId)",
                ).forEach(db::execSQL)
            }
        }

    internal val MIGRATION_38_39 =
        object : Migration(38, 39) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO accounts (
                        id, displayName, email, remoteUserId, authProvider, avatarUrl,
                        createdAtEpochMillis, updatedAtEpochMillis
                    ) VALUES ('local-default', 'You', NULL, NULL, 'local', NULL, 0, 0)
                    """.trimIndent(),
                )

                listOf(
                    "body_metrics",
                    "daily_health_summaries",
                    "health_connect_sync_state",
                    "coach_messages",
                    "dashboard_pins",
                ).forEach { table -> db.execSQL("ALTER TABLE `$table` RENAME TO `${table}_legacy`") }

                db.execSQL(
                    """
                    CREATE TABLE body_metrics (
                        accountId TEXT NOT NULL, id TEXT NOT NULL, type TEXT NOT NULL,
                        value REAL NOT NULL, unit TEXT NOT NULL, measuredAtEpochMillis INTEGER NOT NULL,
                        source TEXT NOT NULL, externalId TEXT, PRIMARY KEY(accountId, id),
                        FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE daily_health_summaries (
                        accountId TEXT NOT NULL, dateEpochDay INTEGER NOT NULL, steps INTEGER,
                        activeCaloriesKcal REAL, totalCaloriesKcal REAL, distanceMeters REAL,
                        sleepMinutes INTEGER, exerciseMinutes INTEGER, exerciseSessionCount INTEGER,
                        latestWeightKg REAL, latestBodyFatPercent REAL, restingHeartRateBpm INTEGER,
                        hrvRmssdMillis REAL, updatedAtEpochMillis INTEGER NOT NULL,
                        PRIMARY KEY(accountId, dateEpochDay),
                        FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE health_connect_sync_state (
                        accountId TEXT NOT NULL, key TEXT NOT NULL, isAvailable INTEGER NOT NULL,
                        grantedPermissionsCsv TEXT NOT NULL, lastImportAtEpochMillis INTEGER,
                        lastExportAtEpochMillis INTEGER, lastFailureMessage TEXT,
                        preferredStepsPackage TEXT, PRIMARY KEY(accountId, key),
                        FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE coach_messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, accountId TEXT NOT NULL,
                        dayEpochDay INTEGER NOT NULL, ruleKey TEXT NOT NULL, category TEXT NOT NULL,
                        title TEXT NOT NULL, body TEXT NOT NULL, actionType TEXT, actionData TEXT,
                        firstSeenAtEpochMillis INTEGER NOT NULL, isRead INTEGER NOT NULL,
                        isDismissed INTEGER NOT NULL, source TEXT NOT NULL,
                        FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE dashboard_pins (
                        accountId TEXT NOT NULL, metricId TEXT NOT NULL, position INTEGER NOT NULL,
                        PRIMARY KEY(accountId, metricId),
                        FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )

                db.execSQL(
                    """
                    INSERT INTO body_metrics (
                        accountId, id, type, value, unit, measuredAtEpochMillis, source, externalId
                    ) SELECT 'local-default', id, type, value, unit, measuredAtEpochMillis, source, externalId
                    FROM body_metrics_legacy
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO daily_health_summaries (
                        accountId, dateEpochDay, steps, activeCaloriesKcal, totalCaloriesKcal,
                        distanceMeters, sleepMinutes, exerciseMinutes, exerciseSessionCount,
                        latestWeightKg, latestBodyFatPercent, restingHeartRateBpm,
                        hrvRmssdMillis, updatedAtEpochMillis
                    ) SELECT 'local-default', dateEpochDay, steps, activeCaloriesKcal,
                        totalCaloriesKcal, distanceMeters, sleepMinutes, exerciseMinutes,
                        exerciseSessionCount, latestWeightKg, latestBodyFatPercent,
                        restingHeartRateBpm, hrvRmssdMillis, updatedAtEpochMillis
                    FROM daily_health_summaries_legacy
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO health_connect_sync_state (
                        accountId, `key`, isAvailable, grantedPermissionsCsv,
                        lastImportAtEpochMillis, lastExportAtEpochMillis, lastFailureMessage,
                        preferredStepsPackage
                    ) SELECT 'local-default', `key`, isAvailable, grantedPermissionsCsv,
                        lastImportAtEpochMillis, lastExportAtEpochMillis, lastFailureMessage,
                        preferredStepsPackage FROM health_connect_sync_state_legacy
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO coach_messages (
                        id, accountId, dayEpochDay, ruleKey, category, title, body, actionType,
                        actionData, firstSeenAtEpochMillis, isRead, isDismissed, source
                    ) SELECT id, 'local-default', dayEpochDay, ruleKey, category, title, body,
                        actionType, actionData, firstSeenAtEpochMillis, isRead, isDismissed, source
                    FROM coach_messages_legacy
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO dashboard_pins(accountId, metricId, position)
                    SELECT 'local-default', metricId, position FROM dashboard_pins_legacy
                    """.trimIndent(),
                )

                listOf(
                    "body_metrics",
                    "daily_health_summaries",
                    "health_connect_sync_state",
                    "coach_messages",
                    "dashboard_pins",
                ).forEach { table -> db.execSQL("DROP TABLE `${table}_legacy`") }

                listOf(
                    "CREATE INDEX index_body_metrics_accountId_type_measuredAtEpochMillis ON body_metrics(accountId, type, measuredAtEpochMillis)",
                    "CREATE UNIQUE INDEX index_coach_messages_accountId_dayEpochDay_ruleKey_source ON coach_messages(accountId, dayEpochDay, ruleKey, source)",
                    "CREATE INDEX index_coach_messages_accountId_dayEpochDay_firstSeenAtEpochMillis ON coach_messages(accountId, dayEpochDay, firstSeenAtEpochMillis)",
                    "CREATE INDEX index_dashboard_pins_accountId_position ON dashboard_pins(accountId, position)",
                ).forEach(db::execSQL)
            }
        }

    internal val MIGRATION_39_40 =
        object : Migration(39, 40) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE health_connect_export_records (
                        accountId TEXT NOT NULL,
                        recordType TEXT NOT NULL,
                        localEntityId TEXT NOT NULL,
                        clientRecordId TEXT NOT NULL,
                        clientRecordVersion INTEGER NOT NULL,
                        payloadFingerprint TEXT NOT NULL,
                        providerRecordId TEXT NOT NULL,
                        exportedAtEpochMillis INTEGER NOT NULL,
                        PRIMARY KEY(accountId, recordType, localEntityId),
                        FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX index_health_connect_export_records_accountId_recordType " +
                        "ON health_connect_export_records(accountId, recordType)",
                )
            }
        }

    internal val MIGRATION_40_41 =
        object : Migration(40, 41) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS index_foods_accountId_name")
                db.execSQL("DROP INDEX IF EXISTS index_foods_accountId_brand")
            }
        }

    internal val MIGRATION_41_42 =
        object : Migration(41, 42) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS index_exercises_accountId_name")
                db.execSQL("DROP INDEX IF EXISTS index_workout_sets_accountId_exerciseId")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_workout_sets_accountId_exerciseId_completed_sessionId_sortOrder " +
                        "ON workout_sets(accountId, exerciseId, completed, sessionId, sortOrder)",
                )
            }
        }

    internal val FOOD_PERFORMANCE_INDEX_CALLBACK =
        object : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                createFoodPerformanceIndex(db)
            }
        }

    internal val TRAINING_PERFORMANCE_INDEX_CALLBACK =
        object : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                createTrainingPerformanceIndex(db)
            }
        }

    /**
     * The exact migration instances registered by production, exposed for
     * framework-SQLite tests. Keep this ordered, gap-free, and synchronized
     * with [provideDatabase]; the workflow contract checks production registration.
     */
    internal val ALL_MIGRATIONS: Array<Migration> =
        arrayOf(
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
            MIGRATION_29_30,
            MIGRATION_30_31,
            MIGRATION_31_32,
            MIGRATION_32_33,
            MIGRATION_33_34,
            MIGRATION_34_35,
            MIGRATION_35_36,
            MIGRATION_36_37,
            MIGRATION_37_38,
            MIGRATION_38_39,
            MIGRATION_39_40,
            MIGRATION_40_41,
            MIGRATION_41_42,
        )

    private fun createFoodPerformanceIndex(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_foods_accountId_name_brand_id_nocase " +
                "ON foods(accountId, name COLLATE NOCASE, brand COLLATE NOCASE, id)",
        )
    }

    /** Room cannot represent the NOCASE column collation needed by Training name ordering. */
    private fun createTrainingPerformanceIndex(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_exercises_accountId_isCustom_name_id_nocase " +
                "ON exercises(accountId, isCustom, name COLLATE NOCASE, id)",
        )
    }
}
