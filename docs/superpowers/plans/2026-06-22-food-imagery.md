# Food Imagery (Slice 3) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Persist Open Food Facts image URLs on saved foods (Room migration v15), surface them on diary entries and saved foods, and render food photos with Coil + a category fallback.

**Architecture:** The provider already yields `Found.imageUrl`. Thread it: `FoodEntity.imageUrl` (migration) → save flows persist it → the diary join + `FoodDiaryEntryRow` carry it → repository models (`SavedFoodItem`, `FoodDiaryEntry`) → UI states → a `FoodThumb` Coil composable.

**Tech Stack:** Kotlin, Room (migration + exported schema), Coil Compose, Robolectric tests, Gradle, adb.

**Spec:** `docs/superpowers/specs/2026-06-22-food-imagery-design.md`

**Prereq:** `. .\.superpowers\sdd\android-env.ps1`

---

## Task 1: Coil dependency

- [ ] **Step 1** — `gradle/libs.versions.toml`: add under `[versions]` `coil = "2.7.0"`; under `[libraries]`
  `coil-compose = { module = "io.coil-kt:coil-compose", version.ref = "coil" }`.
- [ ] **Step 2** — `app/build.gradle.kts`: `implementation(libs.coil.compose)`.
- [ ] **Step 3** — Confirm INTERNET permission: `grep INTERNET app/src/main/AndroidManifest.xml`. If absent, add
  `<uses-permission android:name="android.permission.INTERNET" />` above `<application>`.
- [ ] **Step 4** — Verify: `.\gradlew.bat :app:compileDebugKotlin --no-daemon --console=plain` → BUILD SUCCESSFUL.
- [ ] **Step 5** — Commit: `build: add Coil for image loading`.

---

## Task 2: `FoodEntity.imageUrl` + migration v15

**Files:** `FoodEntities.kt`, `core/di/DatabaseModule.kt`, `data/local/MusFitDatabase.kt`, new migration test.

- [ ] **Step 1** — `FoodEntities.kt`, add to `FoodEntity` (after `magnesiumMgPer100g`):

```kotlin
    val imageUrl: String? = null,
```

- [ ] **Step 2** — `DatabaseModule.kt`: add the migration object (next to `MIGRATION_13_14`):

```kotlin
    private val MIGRATION_14_15 =
        object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE foods ADD COLUMN imageUrl TEXT")
            }
        }
```

and add `MIGRATION_14_15,` to the `.addMigrations(...)` list.

- [ ] **Step 3** — `MusFitDatabase.kt`: bump `version = 14` → `version = 15`.

- [ ] **Step 4: Write the migration test** — create
  `app/src/test/java/com/musfit/data/local/MusFitMigrationTest.kt`:

```kotlin
package com.musfit.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.musfit.core.di.databaseMigrations
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MusFitMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MusFitDatabase::class.java,
    )

    @Test
    fun migrate14To15_addsImageUrlAndKeepsData() {
        val dbName = "migration-test"
        helper.createDatabase(dbName, 14).apply {
            execSQL(
                "INSERT INTO foods (id, name, brand, defaultServingGrams, caloriesPer100g, proteinPer100g, " +
                    "carbsPer100g, fatPer100g, createdAtEpochMillis, updatedAtEpochMillis, isFavorite, " +
                    "fiberPer100g, sugarPer100g, saturatedFatPer100g, sodiumMgPer100g, potassiumMgPer100g, " +
                    "calciumMgPer100g, ironMgPer100g, vitaminDMcgPer100g, vitaminCMgPer100g, magnesiumMgPer100g) " +
                    "VALUES ('f1','Egg',NULL,50,155,13,1,11,0,0,0,0,0,0,0,0,0,0,0,0,0)",
            )
            close()
        }
        val db = helper.runMigrationsAndValidate(dbName, 15, true, *databaseMigrations)
        db.query("SELECT id, imageUrl FROM foods WHERE id = 'f1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("imageUrl")))
        }
        db.close()
    }
}
```

  To give the test the migration array, expose it from `DatabaseModule.kt`: pull the migrations into a
  top-level `val databaseMigrations = arrayOf(MIGRATION_1_2, …, MIGRATION_14_15)` and use
  `.addMigrations(*databaseMigrations)` in `provideDatabase`. (The migration `object`s must be top-level or
  accessible; if they are `private val` inside the object, make `databaseMigrations` a top-level `internal val`
  in the same file referencing them.)

- [ ] **Step 5: Build (exports schema 15)** — `.\gradlew.bat :app:compileDebugKotlin --no-daemon --console=plain`,
  then `.\gradlew.bat testDebugUnitTest --tests "com.musfit.data.local.MusFitMigrationTest" --no-daemon --console=plain`.
  Expected: BUILD SUCCESSFUL; `app/schemas/com.musfit.data.local.MusFitDatabase/15.json` is generated.

- [ ] **Step 6: Commit** — include the generated schema: `git add app/schemas/.../15.json` + the changed files.
  `feat(data): add foods.imageUrl with migration v15`.

---

## Task 3: Carry `imageUrl` through the DAO diary rows

**Files:** `data/local/dao/FoodDao.kt`

- [ ] **Step 1** — Add to `FoodDiaryEntryRow` (after `foodCategory`): `val imageUrl: String?,`.
- [ ] **Step 2** — In every diary query that builds `FoodDiaryEntryRow` (the five `@Query` strings feeding
  `observeFoodDiaryEntryRowsForDate`, `observeFoodDiaryEntryRowsForDateRange`,
  `getFoodDiaryEntryRowsForDateRange`, `getFoodDiaryEntryRowsForDate`, `getFoodDiaryEntryRowsForDateAndMeal`),
  add the projection `"foods.imageUrl AS imageUrl, "` alongside the other `foods.*` columns (e.g. right after
  `"foods.category AS foodCategory, "`). Each query selects columns explicitly — add the line to each.
- [ ] **Step 3** — Verify: `:app:compileDebugKotlin` → BUILD SUCCESSFUL (Room validates the projection matches
  the Row).
- [ ] **Step 4** — Commit: `feat(data): select foods.imageUrl in diary rows`.

---

## Task 4: Repository — persist + expose `imageUrl`

**Files:** `data/repository/FoodRepository.kt`

- [ ] **Step 1** — Add `val imageUrl: String? = null` to `FoodLogInput` (line ~75) and `SavedFoodUpsertInput`
  (line ~135); add `val imageUrl: String?` to `SavedFoodItem` (line ~149) and `FoodDiaryEntry` (line ~163).
- [ ] **Step 2** — At each `FoodEntity(` construction (lines ~745, ~1363, ~1475, ~1610), add
  `imageUrl = <source>` where a source exists: barcode/scanned and online-import paths pass the
  `Found.imageUrl` / `FoodLogInput.imageUrl` / `SavedFoodUpsertInput.imageUrl`; manual creation passes `null`.
  Read each site to wire the correct source.
- [ ] **Step 3** — In `toSavedFoodItem` (line ~2165) add `imageUrl = imageUrl,` (from the `FoodEntity`). In the
  `FoodDiaryEntry(` mapping (line ~2056, from a `FoodDiaryEntryRow`) add `imageUrl = row.imageUrl,` (match the
  row variable name in that function).
- [ ] **Step 4** — Wherever a scanned `Found` is turned into `FoodLogInput`/save input in the ViewModel-facing
  API, pass its `imageUrl` through. (Grep `FoodLogInput(` and `SavedFoodUpsertInput(` call sites in the repo and
  ViewModel; thread `imageUrl`.)
- [ ] **Step 5: Repository round-trip test** — in `LocalFoodRepositoryTest`, add a test: upsert a saved food with
  `imageUrl = "http://x/y.jpg"`, read it back via the saved-foods flow, assert `imageUrl` is set; log it to a
  meal and assert the diary entry exposes the same `imageUrl`.
- [ ] **Step 6** — Verify: `.\gradlew.bat testDebugUnitTest --no-daemon --console=plain` → BUILD SUCCESSFUL.
- [ ] **Step 7** — Commit: `feat(data): persist and expose food imageUrl`.

---

## Task 5: ViewModel + UI state

**Files:** `ui/food/FoodViewModel.kt`

- [ ] **Step 1** — Add `val imageUrl: String? = null` to `SavedFoodUiState` and `FoodMealEntryUiState`.
- [ ] **Step 2** — Where these are built from `SavedFoodItem` / `FoodDiaryEntry`, map `imageUrl = it.imageUrl`.
- [ ] **Step 3** — Verify: `:app:compileDebugKotlin` → BUILD SUCCESSFUL.
- [ ] **Step 4** — Commit: `feat(food): expose imageUrl in UI state`.

---

## Task 6: Render with Coil (`FoodThumb`)

**Files:** `ui/food/FoodScreen.kt`

- [ ] **Step 1** — Add the composable (imports: `coil.compose.AsyncImage`, `androidx.compose.ui.layout.ContentScale`):

```kotlin
@Composable
private fun FoodThumb(
    imageUrl: String?,
    fallback: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(MusFitTheme.shapes.medium)
            .background(MusFitTheme.colors.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl.isNullOrBlank()) {
            Icon(imageVector = fallback, contentDescription = null, tint = MusFitTheme.colors.brand)
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
```

(Import `androidx.compose.ui.unit.Dp`.)

- [ ] **Step 2** — Use `FoodThumb` in `DiaryEntryRow` (prepend a `FoodThumb(entry.imageUrl, Icons.Outlined.Restaurant)`
  before the name/macros column, with `Arrangement.spacedBy` spacing), `SavedFoodDatabaseRow`, the
  `FoodDatabasePreview` rows, and `FoodDetailPanel` (larger `size`). Replace `FoodAvatar` usages in the
  barcode/online result rows with `FoodThumb(result.imageUrl, Icons.Outlined.Restaurant)` (thread `imageUrl`
  onto those result UI states if not already present).
- [ ] **Step 3: Full gate** — `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain`
  → BUILD SUCCESSFUL.
- [ ] **Step 4** — Commit: `feat(food): render food photos with Coil and fallback`.

---

## Task 7: Verify & screenshots

- [ ] **Step 1** — Build gate green.
- [ ] **Step 2** — Install; import an online food / scan a barcode that has a photo; confirm the thumbnail
  renders on the diary row and in the food database; confirm manual/quick foods show the category fallback.
- [ ] **Step 3** — Screenshot Food with a photo-backed entry and the food database.

## Definition of Done

- `FoodEntity.imageUrl` + `MIGRATION_14_15`; DB v15; `15.json` committed; migration + round-trip tests pass.
- Image URLs persist on scanned/imported foods and surface on diary entries and saved foods.
- `FoodThumb` renders OFF photos via Coil with category fallback across diary, database, detail, and results.
- Build gate green; screenshots captured.
