# Profile Tab Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the placeholder Health tab with a Profile "me" tab — profile, goal-driven calorie/macro targets, weight and body-measurement tracking, a read-only Health Connect vitals strip, and a settings sub-screen that rehomes the existing Health Connect sync controls.

**Architecture:** Add `ui/profile` (dashboard + settings + editor composables) over a new `ProfileViewModel` and `ProfileRepository`. `ProfileRepository` persists profile/settings through a new `ProfileDao` and reuses the existing `body_metrics` table (`BodyMetricEntity` + `HealthDao`) for weight and measurements. A pure `EnergyCalculator` turns profile + weight into recommended targets; the ViewModel writes those into Food's existing `FoodRepository.updateFoodGoal`. The Health Connect data layer (`HealthRepository`, `HealthDao`, `domain/health`) is reused unchanged.

**Tech Stack:** Android Kotlin, Jetpack Compose Material 3, Hilt, Room, Kotlin Flow/coroutines, JUnit, Robolectric, kotlinx-coroutines-test, Turbine.

## Global Constraints

- Android-only, local-first. Main package/application id `com.musfit`. No accounts, cloud sync, analytics, subscriptions, social, or cloud AI.
- The data-layer names `HealthRepository`, `HealthDao`, `domain/health/*` stay (they are the Health Connect boundary). Only the **UI** layer is renamed Health → Profile.
- Reuse the existing `body_metrics` table for weight and measurements. Do not add new time-series tables.
- The only new tables are `user_profile` and `app_settings`, added by `MIGRATION_16_17` (DB version 16 → 17). Any schema change must: add and register the migration in `DatabaseModule.kt`, bump `MusFitDatabase` version, and commit the regenerated `app/schemas/com.musfit.data.local.MusFitDatabase/17.json`. There is no destructive fallback.
- v1 is metric-only (kg, cm). Imperial units, theme/dark mode, and backup/export are non-goals (shown as disabled "Later" rows in settings).
- Before Gradle/adb on Windows, run `. .\.superpowers\sdd\android-env.ps1` when that local file exists.
- Focused test run: `.\gradlew.bat testDebugUnitTest --tests "<fqcn>" --no-daemon --console=plain`.
- Full verification (run before claiming completion or pushing): `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain`.
- If Gradle fails because generated output is stale, run `.\scripts\dev\clean-generated.ps1`, then rerun.

---

## File Structure

**UI (new package `app/src/main/java/com/musfit/ui/profile/`):**
- Create `ProfileScreen.kt` — the dashboard (identity, goal & targets, weight, measurements, vitals cards) + log dialogs + profile editor entry.
- Create `ProfileViewModel.kt` — `ProfileUiState` + orchestration of `ProfileRepository`, `HealthRepository`, `FoodRepository`.
- Create `ProfileSettingsScreen.kt` — moved from `HealthScreen.kt`; the relocated Health Connect sync controls + Preferences (Later) + About.
- Create `ProfileSettingsViewModel.kt` — moved from `HealthViewModel.kt`; identical HC status/import/export logic.
- Create `ProfileEditContent.kt` — profile editor (sex, birthdate, height, activity, goal) and weight/measurement log dialogs.

**UI wiring:**
- Modify `app/src/main/java/com/musfit/ui/AppDestination.kt` — `Health` → `Profile` (route `profile`, label `Profile`, `Icons.Outlined.Person`); add `PROFILE_SETTINGS_ROUTE`.
- Modify `app/src/main/java/com/musfit/ui/AppNavGraph.kt` — import Profile screens; render dashboard + settings sub-route.
- Delete `app/src/main/java/com/musfit/ui/health/HealthScreen.kt` and `HealthViewModel.kt`.

**Domain (new package `app/src/main/java/com/musfit/domain/profile/`):**
- Create `ProfileModels.kt` — `Sex`, `ActivityLevel`, `GoalType`, `RecommendedTargets`.
- Create `EnergyCalculator.kt` — BMR/TDEE/target/macro split.
- Create `BodyMetricsCalculator.kt` — BMI, goal progress.

**Data:**
- Create `app/src/main/java/com/musfit/data/local/entity/ProfileEntities.kt` — `UserProfileEntity`, `AppSettingsEntity`.
- Create `app/src/main/java/com/musfit/data/local/dao/ProfileDao.kt`.
- Modify `app/src/main/java/com/musfit/data/local/MusFitDatabase.kt` — register entities, add `profileDao()`, bump version to 17.
- Modify `app/src/main/java/com/musfit/core/di/DatabaseModule.kt` — add+register `MIGRATION_16_17`, add `provideProfileDao`.
- Create `app/src/main/java/com/musfit/data/repository/ProfileRepository.kt` — interface, domain types, `LocalProfileRepository`.
- Modify `app/src/main/java/com/musfit/core/di/RepositoryModule.kt` — bind `ProfileRepository`.

**Tests:**
- Create `app/src/test/java/com/musfit/ui/profile/ProfileSettingsViewModelTest.kt` — moved from `HealthViewModelTest.kt`.
- Delete `app/src/test/java/com/musfit/ui/health/HealthViewModelTest.kt`.
- Create `app/src/test/java/com/musfit/domain/profile/EnergyCalculatorTest.kt`.
- Create `app/src/test/java/com/musfit/domain/profile/BodyMetricsCalculatorTest.kt`.
- Create `app/src/test/java/com/musfit/data/repository/LocalProfileRepositoryTest.kt`.
- Create `app/src/test/java/com/musfit/ui/profile/ProfileViewModelTest.kt`.
- Modify `app/src/test/java/com/musfit/data/local/MusFitDatabaseTest.kt` — add round-trip for `user_profile` + `app_settings`.
- Schema output: `app/schemas/com.musfit.data.local.MusFitDatabase/17.json`.

---

### Task 1: Rename Health UI to Profile + dashboard shell + settings route

**Files:**
- Create: `app/src/main/java/com/musfit/ui/profile/ProfileSettingsViewModel.kt`
- Create: `app/src/main/java/com/musfit/ui/profile/ProfileSettingsScreen.kt`
- Create: `app/src/main/java/com/musfit/ui/profile/ProfileScreen.kt`
- Create: `app/src/test/java/com/musfit/ui/profile/ProfileSettingsViewModelTest.kt`
- Modify: `app/src/main/java/com/musfit/ui/AppDestination.kt`
- Modify: `app/src/main/java/com/musfit/ui/AppNavGraph.kt`
- Delete: `app/src/main/java/com/musfit/ui/health/HealthScreen.kt`, `app/src/main/java/com/musfit/ui/health/HealthViewModel.kt`, `app/src/test/java/com/musfit/ui/health/HealthViewModelTest.kt`

This task is a rename/relocate refactor: behavior is unchanged, so the moved (renamed) ViewModel test is the regression guard.

- [ ] **Step 1: Move the ViewModel test to the new package and class name**

Create `app/src/test/java/com/musfit/ui/profile/ProfileSettingsViewModelTest.kt` as a copy of the current `app/src/test/java/com/musfit/ui/health/HealthViewModelTest.kt` with these edits: `package com.musfit.ui.profile`; every `HealthViewModel(` → `ProfileSettingsViewModel(`; class name `HealthViewModelTest` → `ProfileSettingsViewModelTest`; and update the failure-message assertion string from `"Unable to refresh Health Connect status right now. Try again from the Health tab."` to `"Unable to refresh Health Connect status right now. Try again from the Profile tab."`. Keep the private `FakeHealthRepository` and all other assertions identical. Then delete `app/src/test/java/com/musfit/ui/health/HealthViewModelTest.kt`.

- [ ] **Step 2: Run the moved test to verify it fails to compile**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.profile.ProfileSettingsViewModelTest" --no-daemon --console=plain`
Expected: FAIL — unresolved reference `ProfileSettingsViewModel`.

- [ ] **Step 3: Create `ProfileSettingsViewModel` (moved from `HealthViewModel`)**

Create `app/src/main/java/com/musfit/ui/profile/ProfileSettingsViewModel.kt` with the exact contents of the current `HealthViewModel.kt`, changing only: `package com.musfit.ui.profile`; class `HealthViewModel` → `ProfileSettingsViewModel`; `HealthUiState` → `ProfileSettingsUiState`; and the failure message `"... Try again from the Health tab."` → `"... Try again from the Profile tab."`. Keep the constructor (`private val repository: HealthRepository`), all functions (`refreshStatus`, `importToday`, `exportLatestWorkout`), and the private helper extensions unchanged. Then delete `app/src/main/java/com/musfit/ui/health/HealthViewModel.kt`.

- [ ] **Step 4: Run the moved test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.profile.ProfileSettingsViewModelTest" --no-daemon --console=plain`
Expected: PASS.

- [ ] **Step 5: Create `ProfileSettingsScreen` (moved from `HealthScreen`) with a back affordance**

Create `app/src/main/java/com/musfit/ui/profile/ProfileSettingsScreen.kt` with the contents of the current `HealthScreen.kt`, changing: `package com.musfit.ui.profile`; composable `HealthScreen` → `ProfileSettingsScreen(onBack: () -> Unit, viewModel: ProfileSettingsViewModel = hiltViewModel())`; default VM type → `ProfileSettingsViewModel`. Wrap the content in a `Scaffold` with a `TopAppBar` titled "Settings" and a back `IconButton(onClick = onBack)` using `Icons.Outlined.ArrowBack`. Keep the existing status text and the four buttons (Enable sync, Refresh status, Import today, Export latest workout). Add two disabled "Later" rows beneath them — Units (Metric) and Theme (System) — and an "About MusFit" line showing `versionName`. Then delete `app/src/main/java/com/musfit/ui/health/HealthScreen.kt`.

```kotlin
package com.musfit.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    onBack: () -> Unit,
    viewModel: ProfileSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { viewModel.refreshStatus() }

    LaunchedEffect(Unit) { viewModel.refreshStatus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Health Connect & sync", style = MaterialTheme.typography.titleMedium)
            Text("Health Connect: ${state.availabilityLabel}", style = MaterialTheme.typography.bodyMedium)
            Text(state.message, style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = {
                    if (state.canRequestPermissions) permissionLauncher.launch(state.requestablePermissions)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canRequestPermissions,
            ) { Text("Enable Health Connect sync") }
            Button(onClick = viewModel::refreshStatus, modifier = Modifier.fillMaxWidth()) { Text("Refresh status") }
            Button(onClick = viewModel::importToday, modifier = Modifier.fillMaxWidth()) { Text("Import today") }
            Button(onClick = viewModel::exportLatestWorkout, modifier = Modifier.fillMaxWidth()) { Text("Export latest workout") }

            Text("Preferences", style = MaterialTheme.typography.titleMedium)
            Text("Units: Metric (kg, cm) · Later", style = MaterialTheme.typography.bodyMedium)
            Text("Theme: System · Later", style = MaterialTheme.typography.bodyMedium)

            Text("About", style = MaterialTheme.typography.titleMedium)
            Text("MusFit", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
```

- [ ] **Step 6: Create the Profile dashboard shell**

Create `app/src/main/java/com/musfit/ui/profile/ProfileScreen.kt` as a minimal shell with a top bar, a gear action that calls `onSettingsClick`, and a placeholder body. Later tasks replace the body with real cards.

```kotlin
package com.musfit.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onSettingsClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Complete your profile to see your targets.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
```

- [ ] **Step 7: Update `AppDestination` (Profile + person icon + settings route)**

In `app/src/main/java/com/musfit/ui/AppDestination.kt`: replace the `MonitorHeart` import with `import androidx.compose.material.icons.outlined.Person`, add `const val PROFILE_SETTINGS_ROUTE = "profile-settings"` next to the existing route constants, and change the enum entry to:

```kotlin
Profile(route = "profile", label = "Profile", icon = Icons.Outlined.Person),
```

- [ ] **Step 8: Update `AppNavGraph` (render Profile dashboard + settings sub-route)**

In `app/src/main/java/com/musfit/ui/AppNavGraph.kt`: replace `import com.musfit.ui.health.HealthScreen` with `import com.musfit.ui.profile.ProfileScreen` and `import com.musfit.ui.profile.ProfileSettingsScreen`. Replace the line `composable(AppDestination.Health.route) { HealthScreen() }` with:

```kotlin
composable(AppDestination.Profile.route) {
    ProfileScreen(onSettingsClick = { navController.navigate(PROFILE_SETTINGS_ROUTE) })
}
composable(PROFILE_SETTINGS_ROUTE) {
    ProfileSettingsScreen(onBack = { navController.popBackStack() })
}
```

- [ ] **Step 9: Verify build + tests, then commit**

Run: `.\gradlew.bat testDebugUnitTest assembleDebug --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL; `ProfileSettingsViewModelTest` passes; no references to `com.musfit.ui.health` remain.

```bash
git add -A
git commit -m "refactor: rename Health tab UI to Profile and add settings sub-route"
```

---

### Task 2: Profile & settings persistence (entities, DAO, migration 16 → 17)

**Files:**
- Create: `app/src/main/java/com/musfit/data/local/entity/ProfileEntities.kt`
- Create: `app/src/main/java/com/musfit/data/local/dao/ProfileDao.kt`
- Modify: `app/src/main/java/com/musfit/data/local/MusFitDatabase.kt`
- Modify: `app/src/main/java/com/musfit/core/di/DatabaseModule.kt`
- Modify: `app/src/test/java/com/musfit/data/local/MusFitDatabaseTest.kt`
- Schema output: `app/schemas/com.musfit.data.local.MusFitDatabase/17.json`

- [ ] **Step 1: Write a failing DAO round-trip test**

Add to `app/src/test/java/com/musfit/data/local/MusFitDatabaseTest.kt`: a `profileDao` field (`private lateinit var profileDao: ProfileDao`), assign it in `setUp()` (`profileDao = database.profileDao()`), add the imports `com.musfit.data.local.dao.ProfileDao`, `com.musfit.data.local.entity.AppSettingsEntity`, `com.musfit.data.local.entity.UserProfileEntity`, and add this test:

```kotlin
@Test
fun profileDao_roundTripsProfileAndSettings() = runTest {
    val profile = UserProfileEntity(
        id = "user",
        sex = "Male",
        birthDateEpochDay = 9_000L,
        heightCm = 182.0,
        activityLevel = "Moderate",
        goalType = "Lose",
        goalPaceKgPerWeek = 0.5,
        goalWeightKg = 78.0,
        updatedAtEpochMillis = 1_000L,
    )
    val settings = AppSettingsEntity(
        id = "app",
        unitSystem = "metric",
        themeMode = "system",
        updatedAtEpochMillis = 1_000L,
    )

    profileDao.upsertProfile(profile)
    profileDao.upsertSettings(settings)

    assertEquals(profile, profileDao.observeProfile("user").first())
    assertEquals(settings, profileDao.observeSettings("app").first())
    assertEquals(profile, profileDao.getProfile("user"))
    assertNull(profileDao.observeProfile("missing").first())
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.musfit.data.local.MusFitDatabaseTest" --no-daemon --console=plain`
Expected: FAIL — unresolved references `ProfileDao` / `UserProfileEntity` / `AppSettingsEntity`.

- [ ] **Step 3: Create the entities**

Create `app/src/main/java/com/musfit/data/local/entity/ProfileEntities.kt`:

```kotlin
package com.musfit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val sex: String?,
    val birthDateEpochDay: Long?,
    val heightCm: Double?,
    val activityLevel: String,
    val goalType: String,
    val goalPaceKgPerWeek: Double,
    val goalWeightKg: Double?,
    val updatedAtEpochMillis: Long,
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: String,
    val unitSystem: String,
    val themeMode: String,
    val updatedAtEpochMillis: Long,
)
```

- [ ] **Step 4: Create `ProfileDao`**

Create `app/src/main/java/com/musfit/data/local/dao/ProfileDao.kt`:

```kotlin
package com.musfit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musfit.data.local.entity.AppSettingsEntity
import com.musfit.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = :id LIMIT 1")
    fun observeProfile(id: String): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = :id LIMIT 1")
    suspend fun getProfile(id: String): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM app_settings WHERE id = :id LIMIT 1")
    fun observeSettings(id: String): Flow<AppSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: AppSettingsEntity)
}
```

- [ ] **Step 5: Register entities, DAO accessor, and bump the version**

In `app/src/main/java/com/musfit/data/local/MusFitDatabase.kt`: add imports `com.musfit.data.local.dao.ProfileDao`, `com.musfit.data.local.entity.AppSettingsEntity`, `com.musfit.data.local.entity.UserProfileEntity`; add `UserProfileEntity::class,` and `AppSettingsEntity::class,` to the `entities` array; change `version = 16` to `version = 17`; and add `abstract fun profileDao(): ProfileDao`.

- [ ] **Step 6: Add and register `MIGRATION_16_17` and provide the DAO**

In `app/src/main/java/com/musfit/core/di/DatabaseModule.kt`: add `MIGRATION_16_17,` to `addMigrations(...)` (after `MIGRATION_15_16,`), add a `provideProfileDao`, the `ProfileDao` import, and the migration object.

```kotlin
@Provides
fun provideProfileDao(database: MusFitDatabase): ProfileDao = database.profileDao()
```

```kotlin
private val MIGRATION_16_17 =
    object : Migration(16, 17) {
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
```

- [ ] **Step 7: Run the DAO test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.musfit.data.local.MusFitDatabaseTest" --no-daemon --console=plain`
Expected: PASS.

- [ ] **Step 8: Build to regenerate the schema, then commit including `17.json`**

Run: `.\gradlew.bat assembleDebug --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL and a new `app/schemas/com.musfit.data.local.MusFitDatabase/17.json` exists with the `user_profile` and `app_settings` tables.

```bash
git add -A
git commit -m "feat: add user_profile and app_settings tables (migration 16 to 17)"
```

---

### Task 3: Energy & body-metric domain calculators

**Files:**
- Create: `app/src/main/java/com/musfit/domain/profile/ProfileModels.kt`
- Create: `app/src/main/java/com/musfit/domain/profile/EnergyCalculator.kt`
- Create: `app/src/main/java/com/musfit/domain/profile/BodyMetricsCalculator.kt`
- Test: `app/src/test/java/com/musfit/domain/profile/EnergyCalculatorTest.kt`
- Test: `app/src/test/java/com/musfit/domain/profile/BodyMetricsCalculatorTest.kt`

- [ ] **Step 1: Write failing calculator tests**

Create `app/src/test/java/com/musfit/domain/profile/EnergyCalculatorTest.kt`:

```kotlin
package com.musfit.domain.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnergyCalculatorTest {
    @Test
    fun bmr_usesMifflinStJeorForMale() {
        val bmr = EnergyCalculator.basalMetabolicRate(Sex.Male, weightKg = 80.0, heightCm = 180.0, ageYears = 30)
        assertEquals(1780.0, bmr, 0.5)
    }

    @Test
    fun bmr_usesMifflinStJeorForFemale() {
        val bmr = EnergyCalculator.basalMetabolicRate(Sex.Female, weightKg = 65.0, heightCm = 168.0, ageYears = 30)
        assertEquals(1389.0, bmr, 0.5)
    }

    @Test
    fun maintainTargets_equalTdeeAndSplitMacros() {
        val targets = EnergyCalculator.recommendedTargets(
            sex = Sex.Male, weightKg = 80.0, heightCm = 180.0, ageYears = 30,
            activityLevel = ActivityLevel.Moderate, goalType = GoalType.Maintain, goalPaceKgPerWeek = 0.0,
        )
        assertEquals(2759.0, targets.caloriesKcal, 1.0)
        assertEquals(144.0, targets.proteinGrams, 1.0)
        assertEquals(77.0, targets.fatGrams, 1.0)
        assertTrue(targets.carbsGrams > 0.0)
    }

    @Test
    fun loseGoal_subtractsPaceEnergyFromTdee() {
        val maintain = EnergyCalculator.recommendedTargets(
            Sex.Male, 80.0, 180.0, 30, ActivityLevel.Moderate, GoalType.Maintain, 0.0,
        )
        val lose = EnergyCalculator.recommendedTargets(
            Sex.Male, 80.0, 180.0, 30, ActivityLevel.Moderate, GoalType.Lose, 0.5,
        )
        assertEquals(550.0, maintain.caloriesKcal - lose.caloriesKcal, 1.0)
    }

    @Test
    fun loseGoal_neverDropsBelowSafeFloor() {
        val targets = EnergyCalculator.recommendedTargets(
            Sex.Female, 45.0, 150.0, 25, ActivityLevel.Sedentary, GoalType.Lose, 1.0,
        )
        assertTrue(targets.caloriesKcal >= 1200.0)
    }
}
```

Create `app/src/test/java/com/musfit/domain/profile/BodyMetricsCalculatorTest.kt`:

```kotlin
package com.musfit.domain.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BodyMetricsCalculatorTest {
    @Test
    fun bmi_computesAndRoundsToOneDecimal() {
        assertEquals(24.7, BodyMetricsCalculator.bodyMassIndex(weightKg = 80.0, heightCm = 180.0)!!, 0.001)
    }

    @Test
    fun bmi_returnsNullForNonPositiveInputs() {
        assertNull(BodyMetricsCalculator.bodyMassIndex(0.0, 180.0))
        assertNull(BodyMetricsCalculator.bodyMassIndex(80.0, 0.0))
    }

    @Test
    fun goalProgress_isFractionFromStartTowardGoal() {
        assertEquals(0.5, BodyMetricsCalculator.goalProgressFraction(90.0, 84.0, 78.0)!!, 0.001)
    }

    @Test
    fun goalProgress_clampsToZeroOneAndHandlesNoDelta() {
        assertEquals(1.0, BodyMetricsCalculator.goalProgressFraction(90.0, 70.0, 78.0)!!, 0.001)
        assertEquals(0.0, BodyMetricsCalculator.goalProgressFraction(90.0, 95.0, 78.0)!!, 0.001)
        assertNull(BodyMetricsCalculator.goalProgressFraction(78.0, 78.0, 78.0))
    }
}
```

- [ ] **Step 2: Run them to verify they fail**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.musfit.domain.profile.*" --no-daemon --console=plain`
Expected: FAIL — unresolved references to `EnergyCalculator` / `BodyMetricsCalculator` / `Sex` / `ActivityLevel` / `GoalType`.

- [ ] **Step 3: Create the domain models**

Create `app/src/main/java/com/musfit/domain/profile/ProfileModels.kt`:

```kotlin
package com.musfit.domain.profile

enum class Sex { Male, Female }

enum class ActivityLevel(val factor: Double) {
    Sedentary(1.2),
    Light(1.375),
    Moderate(1.55),
    Active(1.725),
    VeryActive(1.9),
}

enum class GoalType { Lose, Maintain, Gain }

data class RecommendedTargets(
    val caloriesKcal: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
)
```

- [ ] **Step 4: Create `EnergyCalculator`**

Create `app/src/main/java/com/musfit/domain/profile/EnergyCalculator.kt`:

```kotlin
package com.musfit.domain.profile

import kotlin.math.max
import kotlin.math.roundToInt

object EnergyCalculator {
    private const val KCAL_PER_KG = 7700.0
    private const val MIN_CALORIES = 1200.0
    private const val PROTEIN_G_PER_KG = 1.8
    private const val FAT_FRACTION = 0.25
    private const val KCAL_PER_G_PROTEIN = 4.0
    private const val KCAL_PER_G_CARB = 4.0
    private const val KCAL_PER_G_FAT = 9.0

    fun basalMetabolicRate(sex: Sex, weightKg: Double, heightCm: Double, ageYears: Int): Double {
        val base = 10.0 * weightKg + 6.25 * heightCm - 5.0 * ageYears
        return when (sex) {
            Sex.Male -> base + 5.0
            Sex.Female -> base - 161.0
        }
    }

    fun totalDailyEnergyExpenditure(bmr: Double, activityLevel: ActivityLevel): Double =
        bmr * activityLevel.factor

    fun recommendedTargets(
        sex: Sex,
        weightKg: Double,
        heightCm: Double,
        ageYears: Int,
        activityLevel: ActivityLevel,
        goalType: GoalType,
        goalPaceKgPerWeek: Double,
    ): RecommendedTargets {
        val tdee = totalDailyEnergyExpenditure(basalMetabolicRate(sex, weightKg, heightCm, ageYears), activityLevel)
        val dailyAdjustment = goalPaceKgPerWeek * KCAL_PER_KG / 7.0
        val targetCalories = when (goalType) {
            GoalType.Maintain -> tdee
            GoalType.Lose -> max(MIN_CALORIES, tdee - dailyAdjustment)
            GoalType.Gain -> tdee + dailyAdjustment
        }
        val proteinGrams = PROTEIN_G_PER_KG * weightKg
        val fatGrams = FAT_FRACTION * targetCalories / KCAL_PER_G_FAT
        val remainingKcal = max(0.0, targetCalories - proteinGrams * KCAL_PER_G_PROTEIN - fatGrams * KCAL_PER_G_FAT)
        val carbsGrams = remainingKcal / KCAL_PER_G_CARB
        return RecommendedTargets(
            caloriesKcal = targetCalories.roundToInt().toDouble(),
            proteinGrams = proteinGrams.roundToInt().toDouble(),
            carbsGrams = carbsGrams.roundToInt().toDouble(),
            fatGrams = fatGrams.roundToInt().toDouble(),
        )
    }
}
```

- [ ] **Step 5: Create `BodyMetricsCalculator`**

Create `app/src/main/java/com/musfit/domain/profile/BodyMetricsCalculator.kt`:

```kotlin
package com.musfit.domain.profile

import kotlin.math.roundToInt

object BodyMetricsCalculator {
    fun bodyMassIndex(weightKg: Double, heightCm: Double): Double? {
        if (weightKg <= 0.0 || heightCm <= 0.0) return null
        val heightM = heightCm / 100.0
        return (weightKg / (heightM * heightM) * 10.0).roundToInt() / 10.0
    }

    fun goalProgressFraction(startWeightKg: Double, currentWeightKg: Double, goalWeightKg: Double): Double? {
        val total = startWeightKg - goalWeightKg
        if (total == 0.0) return null
        return ((startWeightKg - currentWeightKg) / total).coerceIn(0.0, 1.0)
    }
}
```

- [ ] **Step 6: Run the tests to verify they pass, then commit**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.musfit.domain.profile.*" --no-daemon --console=plain`
Expected: PASS.

```bash
git add -A
git commit -m "feat: add EnergyCalculator and BodyMetricsCalculator domain helpers"
```

---

### Task 4: ProfileRepository (profile, settings, weight, measurements, recommended targets)

**Files:**
- Create: `app/src/main/java/com/musfit/data/repository/ProfileRepository.kt`
- Modify: `app/src/main/java/com/musfit/core/di/RepositoryModule.kt`
- Test: `app/src/test/java/com/musfit/data/repository/LocalProfileRepositoryTest.kt`

**Interfaces produced:**
- `data class UserProfile(val sex: Sex?, val birthDateEpochDay: Long?, val heightCm: Double?, val activityLevel: ActivityLevel, val goalType: GoalType, val goalPaceKgPerWeek: Double, val goalWeightKg: Double?)`
- `data class WeightEntry(val measuredAtEpochMillis: Long, val weightKg: Double, val source: String)`
- `data class BodyMeasurement(val type: String, val value: Double, val unit: String, val measuredAtEpochMillis: Long)`
- `data class AppSettings(val unitSystem: String, val themeMode: String)`
- `val MEASUREMENT_TYPES = listOf("waist", "chest", "arms", "thighs", "hips", "body_fat")`

- [ ] **Step 1: Write failing repository tests**

Create `app/src/test/java/com/musfit/data/repository/LocalProfileRepositoryTest.kt`:

```kotlin
package com.musfit.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.musfit.data.local.MusFitDatabase
import com.musfit.domain.profile.ActivityLevel
import com.musfit.domain.profile.GoalType
import com.musfit.domain.profile.Sex
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalProfileRepositoryTest {
    private lateinit var database: MusFitDatabase
    private lateinit var repository: LocalProfileRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MusFitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LocalProfileRepository(
            profileDao = database.profileDao(),
            healthDao = database.healthDao(),
            clock = { 10_000L },
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun observeProfile_returnsDefaultsWhenEmpty() = runTest {
        val profile = repository.observeProfile().first()
        assertEquals(ActivityLevel.Moderate, profile.activityLevel)
        assertEquals(GoalType.Maintain, profile.goalType)
        assertNull(profile.sex)
        assertNull(profile.heightCm)
    }

    @Test
    fun saveProfile_thenObserve_roundTrips() = runTest {
        repository.saveProfile(
            UserProfile(
                sex = Sex.Male,
                birthDateEpochDay = 9_000L,
                heightCm = 182.0,
                activityLevel = ActivityLevel.Active,
                goalType = GoalType.Lose,
                goalPaceKgPerWeek = 0.5,
                goalWeightKg = 78.0,
            ),
        )
        val profile = repository.observeProfile().first()
        assertEquals(Sex.Male, profile.sex)
        assertEquals(182.0, profile.heightCm!!, 0.001)
        assertEquals(GoalType.Lose, profile.goalType)
    }

    @Test
    fun logWeight_thenObserveLatest_returnsNewest() = runTest {
        repository.logWeight(85.0)
        repository.logWeight(84.2)
        val latest = repository.observeLatestWeight().first()
        assertNotNull(latest)
        assertEquals(84.2, latest!!.weightKg, 0.001)
        assertEquals("manual", latest.source)
    }

    @Test
    fun logMeasurement_storesUnderTypeWithUnit() = runTest {
        repository.logMeasurement("waist", 84.0, "cm")
        val recent = repository.observeRecentMeasurements(0L).first()
        assertEquals(84.0, recent["waist"]!!.first().value, 0.001)
        assertEquals("cm", recent["waist"]!!.first().unit)
    }

    @Test
    fun recommendedTargets_nullUntilProfileAndWeightPresent() = runTest {
        assertNull(repository.observeRecommendedTargets().first())
        repository.saveProfile(
            UserProfile(
                sex = Sex.Male,
                birthDateEpochDay = 0L,
                heightCm = 180.0,
                activityLevel = ActivityLevel.Moderate,
                goalType = GoalType.Maintain,
                goalPaceKgPerWeek = 0.0,
                goalWeightKg = 80.0,
            ),
        )
        repository.logWeight(80.0)
        assertNotNull(repository.observeRecommendedTargets().first())
    }

    @Test
    fun observeSettings_returnsDefaultsThenRoundTrips() = runTest {
        assertEquals("metric", repository.observeSettings().first().unitSystem)
        repository.saveSettings(AppSettings(unitSystem = "metric", themeMode = "dark"))
        assertEquals("dark", repository.observeSettings().first().themeMode)
    }
}
```

- [ ] **Step 2: Run them to verify they fail**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.musfit.data.repository.LocalProfileRepositoryTest" --no-daemon --console=plain`
Expected: FAIL — unresolved reference `LocalProfileRepository` / `UserProfile` / `AppSettings`.

- [ ] **Step 3: Create `ProfileRepository` interface, types, and `LocalProfileRepository`**

Create `app/src/main/java/com/musfit/data/repository/ProfileRepository.kt`:

```kotlin
package com.musfit.data.repository

import com.musfit.data.local.dao.HealthDao
import com.musfit.data.local.dao.ProfileDao
import com.musfit.data.local.entity.AppSettingsEntity
import com.musfit.data.local.entity.BodyMetricEntity
import com.musfit.data.local.entity.UserProfileEntity
import com.musfit.domain.profile.ActivityLevel
import com.musfit.domain.profile.BodyMetricsCalculator
import com.musfit.domain.profile.EnergyCalculator
import com.musfit.domain.profile.GoalType
import com.musfit.domain.profile.RecommendedTargets
import com.musfit.domain.profile.Sex
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.Period
import java.util.UUID
import javax.inject.Inject

const val PROFILE_ID = "user"
const val SETTINGS_ID = "app"
const val WEIGHT_METRIC_TYPE = "weight"
val MEASUREMENT_TYPES = listOf("waist", "chest", "arms", "thighs", "hips", "body_fat")

data class UserProfile(
    val sex: Sex?,
    val birthDateEpochDay: Long?,
    val heightCm: Double?,
    val activityLevel: ActivityLevel,
    val goalType: GoalType,
    val goalPaceKgPerWeek: Double,
    val goalWeightKg: Double?,
)

data class WeightEntry(val measuredAtEpochMillis: Long, val weightKg: Double, val source: String)

data class BodyMeasurement(val type: String, val value: Double, val unit: String, val measuredAtEpochMillis: Long)

data class AppSettings(val unitSystem: String, val themeMode: String)

val DEFAULT_USER_PROFILE = UserProfile(
    sex = null,
    birthDateEpochDay = null,
    heightCm = null,
    activityLevel = ActivityLevel.Moderate,
    goalType = GoalType.Maintain,
    goalPaceKgPerWeek = 0.5,
    goalWeightKg = null,
)

val DEFAULT_APP_SETTINGS = AppSettings(unitSystem = "metric", themeMode = "system")

interface ProfileRepository {
    fun observeProfile(): Flow<UserProfile>
    suspend fun saveProfile(profile: UserProfile)
    fun observeRecommendedTargets(): Flow<RecommendedTargets?>
    suspend fun logWeight(weightKg: Double, source: String = "manual")
    fun observeLatestWeight(): Flow<WeightEntry?>
    fun observeWeightSeries(sinceEpochMillis: Long): Flow<List<WeightEntry>>
    suspend fun logMeasurement(type: String, value: Double, unit: String)
    fun observeRecentMeasurements(sinceEpochMillis: Long): Flow<Map<String, List<BodyMeasurement>>>
    fun observeSettings(): Flow<AppSettings>
    suspend fun saveSettings(settings: AppSettings)
}

class LocalProfileRepository @Inject constructor(
    private val profileDao: ProfileDao,
    private val healthDao: HealthDao,
) : ProfileRepository {
    private var clock: () -> Long = { System.currentTimeMillis() }

    internal constructor(profileDao: ProfileDao, healthDao: HealthDao, clock: () -> Long) :
        this(profileDao, healthDao) {
        this.clock = clock
    }

    override fun observeProfile(): Flow<UserProfile> =
        profileDao.observeProfile(PROFILE_ID).map { it?.toUserProfile() ?: DEFAULT_USER_PROFILE }

    override suspend fun saveProfile(profile: UserProfile) {
        profileDao.upsertProfile(profile.toEntity(clock()))
    }

    override fun observeRecommendedTargets(): Flow<RecommendedTargets?> =
        combine(observeProfile(), observeLatestWeight()) { profile, weight ->
            val sex = profile.sex ?: return@combine null
            val height = profile.heightCm ?: return@combine null
            val birth = profile.birthDateEpochDay ?: return@combine null
            val current = weight?.weightKg ?: return@combine null
            EnergyCalculator.recommendedTargets(
                sex = sex,
                weightKg = current,
                heightCm = height,
                ageYears = ageYears(birth),
                activityLevel = profile.activityLevel,
                goalType = profile.goalType,
                goalPaceKgPerWeek = profile.goalPaceKgPerWeek,
            )
        }

    override suspend fun logWeight(weightKg: Double, source: String) {
        require(weightKg.isFinite() && weightKg > 0.0) { "Weight must be positive" }
        healthDao.upsertBodyMetric(
            BodyMetricEntity(
                id = UUID.randomUUID().toString(),
                type = WEIGHT_METRIC_TYPE,
                value = weightKg,
                unit = "kg",
                measuredAtEpochMillis = clock(),
                source = source,
                externalId = null,
            ),
        )
    }

    override fun observeLatestWeight(): Flow<WeightEntry?> =
        healthDao.observeBodyMetrics(WEIGHT_METRIC_TYPE, 0L).map { rows ->
            rows.firstOrNull()?.let { WeightEntry(it.measuredAtEpochMillis, it.value, it.source) }
        }

    override fun observeWeightSeries(sinceEpochMillis: Long): Flow<List<WeightEntry>> =
        healthDao.observeBodyMetrics(WEIGHT_METRIC_TYPE, sinceEpochMillis).map { rows ->
            rows.map { WeightEntry(it.measuredAtEpochMillis, it.value, it.source) }
        }

    override suspend fun logMeasurement(type: String, value: Double, unit: String) {
        require(value.isFinite() && value > 0.0) { "Measurement must be positive" }
        healthDao.upsertBodyMetric(
            BodyMetricEntity(
                id = UUID.randomUUID().toString(),
                type = type,
                value = value,
                unit = unit,
                measuredAtEpochMillis = clock(),
                source = "manual",
                externalId = null,
            ),
        )
    }

    override fun observeRecentMeasurements(sinceEpochMillis: Long): Flow<Map<String, List<BodyMeasurement>>> =
        combine(
            MEASUREMENT_TYPES.map { type ->
                healthDao.observeBodyMetrics(type, sinceEpochMillis).map { rows ->
                    type to rows.map { BodyMeasurement(it.type, it.value, it.unit, it.measuredAtEpochMillis) }
                }
            },
        ) { pairs -> pairs.toMap() }

    override fun observeSettings(): Flow<AppSettings> =
        profileDao.observeSettings(SETTINGS_ID).map { it?.toAppSettings() ?: DEFAULT_APP_SETTINGS }

    override suspend fun saveSettings(settings: AppSettings) {
        profileDao.upsertSettings(
            AppSettingsEntity(
                id = SETTINGS_ID,
                unitSystem = settings.unitSystem,
                themeMode = settings.themeMode,
                updatedAtEpochMillis = clock(),
            ),
        )
    }

    private fun ageYears(birthDateEpochDay: Long): Int {
        val birth = LocalDate.ofEpochDay(birthDateEpochDay)
        val today = LocalDate.ofEpochDay(clock() / 86_400_000L)
        return Period.between(birth, today).years.coerceAtLeast(0)
    }
}

private fun UserProfileEntity.toUserProfile() = UserProfile(
    sex = sex?.let { Sex.valueOf(it) },
    birthDateEpochDay = birthDateEpochDay,
    heightCm = heightCm,
    activityLevel = ActivityLevel.valueOf(activityLevel),
    goalType = GoalType.valueOf(goalType),
    goalPaceKgPerWeek = goalPaceKgPerWeek,
    goalWeightKg = goalWeightKg,
)

private fun UserProfile.toEntity(now: Long) = UserProfileEntity(
    id = PROFILE_ID,
    sex = sex?.name,
    birthDateEpochDay = birthDateEpochDay,
    heightCm = heightCm,
    activityLevel = activityLevel.name,
    goalType = goalType.name,
    goalPaceKgPerWeek = goalPaceKgPerWeek,
    goalWeightKg = goalWeightKg,
    updatedAtEpochMillis = now,
)

private fun AppSettingsEntity.toAppSettings() = AppSettings(unitSystem = unitSystem, themeMode = themeMode)
```

Note: `BodyMetricsCalculator` is imported for use by the ViewModel layer and to keep this file's dependency surface explicit; it is exercised in Task 5. The `ageYears` clock-based computation keeps the recommendation deterministic in tests.

- [ ] **Step 4: Bind the repository in Hilt**

In `app/src/main/java/com/musfit/core/di/RepositoryModule.kt`: add imports for `ProfileRepository` and `LocalProfileRepository`, and add:

```kotlin
@Binds
@Singleton
abstract fun bindProfileRepository(repository: LocalProfileRepository): ProfileRepository
```

- [ ] **Step 5: Run the repository tests to verify they pass, then commit**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.musfit.data.repository.LocalProfileRepositoryTest" --no-daemon --console=plain`
Expected: PASS.

```bash
git add -A
git commit -m "feat: add ProfileRepository for profile, weight, measurements, and targets"
```

---

### Task 5: ProfileViewModel + ProfileUiState

**Files:**
- Create: `app/src/main/java/com/musfit/ui/profile/ProfileViewModel.kt`
- Test: `app/src/test/java/com/musfit/ui/profile/ProfileViewModelTest.kt`

**Interfaces produced:**
- `data class ProfileUiState(...)`, `data class MeasurementRow(...)`, `data class VitalsSummary(...)`
- `ProfileViewModel(profileRepository, healthRepository, foodRepository)` with `saveProfile`, `logWeight`, `logMeasurement`, `applyTargetsToFood`, `dismissMessage`.

- [ ] **Step 1: Write failing ViewModel tests**

Create `app/src/test/java/com/musfit/ui/profile/ProfileViewModelTest.kt`:

```kotlin
package com.musfit.ui.profile

import com.musfit.data.local.entity.DailyHealthSummaryEntity
import com.musfit.data.repository.AppSettings
import com.musfit.data.repository.BodyMeasurement
import com.musfit.data.repository.DEFAULT_APP_SETTINGS
import com.musfit.data.repository.DEFAULT_USER_PROFILE
import com.musfit.data.repository.FoodGoal
import com.musfit.data.repository.FoodGoalMode
import com.musfit.data.repository.FoodRepository
import com.musfit.data.repository.HealthRepository
import com.musfit.data.repository.ProfileRepository
import com.musfit.data.repository.UserProfile
import com.musfit.data.repository.WeightEntry
import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.domain.health.HealthConnectStatus
import com.musfit.domain.health.ImportedDailyHealthSummary
import com.musfit.domain.profile.ActivityLevel
import com.musfit.domain.profile.GoalType
import com.musfit.domain.profile.RecommendedTargets
import com.musfit.domain.profile.Sex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun incompleteProfile_hidesRecommendation() = runTest {
        val viewModel = ProfileViewModel(FakeProfileRepository(), FakeHealthRepo(), FakeFoodGoalRepo())
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.isLoaded)
        assertEquals(false, viewModel.state.value.isProfileComplete)
        assertNull(viewModel.state.value.recommendedTargets)
    }

    @Test
    fun completeProfile_exposesTargetsAndBmi() = runTest {
        val repo = FakeProfileRepository(
            profile = UserProfile(Sex.Male, 0L, 180.0, ActivityLevel.Moderate, GoalType.Maintain, 0.0, 80.0),
            latestWeight = WeightEntry(1_000L, 80.0, "manual"),
            targets = RecommendedTargets(2759.0, 144.0, 270.0, 77.0),
        )
        val viewModel = ProfileViewModel(repo, FakeHealthRepo(), FakeFoodGoalRepo())
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, viewModel.state.value.isProfileComplete)
        assertEquals(2759.0, viewModel.state.value.recommendedTargets!!.caloriesKcal, 0.001)
        assertEquals(24.7, viewModel.state.value.bmi!!, 0.05)
    }

    @Test
    fun applyTargetsToFood_writesGoalPreservingOtherFields() = runTest {
        val food = FakeFoodGoalRepo(
            initial = FoodGoal(
                dailyCaloriesKcal = 2000.0, proteinGrams = 100.0, carbsGrams = 250.0, fatGrams = 60.0,
                fiberGrams = 30.0, sugarGrams = 50.0, saturatedFatGrams = 20.0, sodiumMilligrams = 2300.0,
                mode = FoodGoalMode.Balanced, includeTrainingCalories = true, useNetCarbs = true,
                waterGoalMilliliters = 2500.0,
            ),
        )
        val repo = FakeProfileRepository(
            profile = UserProfile(Sex.Male, 0L, 180.0, ActivityLevel.Moderate, GoalType.Maintain, 0.0, 80.0),
            latestWeight = WeightEntry(1_000L, 80.0, "manual"),
            targets = RecommendedTargets(2759.0, 144.0, 270.0, 77.0),
        )
        val viewModel = ProfileViewModel(repo, FakeHealthRepo(), food)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.applyTargetsToFood()
        dispatcher.scheduler.advanceUntilIdle()

        val saved = food.saved!!
        assertEquals(2759.0, saved.dailyCaloriesKcal, 0.001)
        assertEquals(144.0, saved.proteinGrams, 0.001)
        assertEquals(true, saved.includeTrainingCalories)
        assertEquals(true, saved.useNetCarbs)
        assertEquals(2500.0, saved.waterGoalMilliliters, 0.001)
        assertEquals("Applied your targets to Food goals.", viewModel.state.value.message)
    }

    @Test
    fun vitals_mapFromHealthConnectDailySummary() = runTest {
        val health = FakeHealthRepo(
            summary = DailyHealthSummaryEntity(
                dateEpochDay = LocalDate.now().toEpochDay(),
                steps = 7420L, activeCaloriesKcal = 410.0, latestWeightKg = 84.2,
                restingHeartRateBpm = 58L, updatedAtEpochMillis = 1L,
            ),
        )
        val viewModel = ProfileViewModel(FakeProfileRepository(), health, FakeFoodGoalRepo())
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(7420L, viewModel.state.value.vitals!!.steps)
        assertEquals(58L, viewModel.state.value.vitals!!.restingHeartRateBpm)
    }

    @Test
    fun logWeight_callsRepository() = runTest {
        val repo = FakeProfileRepository()
        val viewModel = ProfileViewModel(repo, FakeHealthRepo(), FakeFoodGoalRepo())
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.logWeight(83.6)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(83.6, repo.loggedWeight!!, 0.001)
    }

    private class FakeProfileRepository(
        private val profile: UserProfile = DEFAULT_USER_PROFILE,
        private val latestWeight: WeightEntry? = null,
        private val targets: RecommendedTargets? = null,
        private val measurements: Map<String, List<BodyMeasurement>> = emptyMap(),
    ) : ProfileRepository {
        var loggedWeight: Double? = null
        override fun observeProfile(): Flow<UserProfile> = flowOf(profile)
        override suspend fun saveProfile(profile: UserProfile) = Unit
        override fun observeRecommendedTargets(): Flow<RecommendedTargets?> = flowOf(targets)
        override suspend fun logWeight(weightKg: Double, source: String) { loggedWeight = weightKg }
        override fun observeLatestWeight(): Flow<WeightEntry?> = flowOf(latestWeight)
        override fun observeWeightSeries(sinceEpochMillis: Long): Flow<List<WeightEntry>> =
            flowOf(listOfNotNull(latestWeight))
        override suspend fun logMeasurement(type: String, value: Double, unit: String) = Unit
        override fun observeRecentMeasurements(sinceEpochMillis: Long): Flow<Map<String, List<BodyMeasurement>>> =
            flowOf(measurements)
        override fun observeSettings(): Flow<AppSettings> = flowOf(DEFAULT_APP_SETTINGS)
        override suspend fun saveSettings(settings: AppSettings) = Unit
    }

    private class FakeHealthRepo(
        private val summary: DailyHealthSummaryEntity? = null,
    ) : HealthRepository {
        override suspend fun status(): HealthConnectStatus =
            HealthConnectStatus(HealthConnectAvailability.Available, setOf("steps"))
        override suspend fun requestablePermissions(): Set<String> = setOf("steps")
        override fun observeDailySummary(date: LocalDate): Flow<DailyHealthSummaryEntity?> = flowOf(summary)
        override suspend fun importDailySummary(date: LocalDate): ImportedDailyHealthSummary =
            ImportedDailyHealthSummary(null, null, null, null)
        override suspend fun exportLatestWorkout(): String? = null
    }

    private class FakeFoodGoalRepo(
        private val initial: FoodGoal = FoodGoal(
            2000.0, 100.0, 250.0, 60.0, 30.0, 50.0, 20.0, 2300.0,
            FoodGoalMode.Balanced, includeTrainingCalories = false,
        ),
    ) : FoodRepository {
        val goalFlow = MutableStateFlow(initial)
        var saved: FoodGoal? = null
        override fun observeFoodGoal(): Flow<FoodGoal> = goalFlow
        override suspend fun updateFoodGoal(goal: FoodGoal) { saved = goal; goalFlow.value = goal }
    }
}
```

Note: `FakeFoodGoalRepo` overrides only `observeFoodGoal` and `updateFoodGoal` because every other `FoodRepository` member has a default implementation.

- [ ] **Step 2: Run them to verify they fail**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.profile.ProfileViewModelTest" --no-daemon --console=plain`
Expected: FAIL — unresolved reference `ProfileViewModel`.

- [ ] **Step 3: Implement `ProfileViewModel`**

Create `app/src/main/java/com/musfit/ui/profile/ProfileViewModel.kt`:

```kotlin
package com.musfit.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.data.local.entity.DailyHealthSummaryEntity
import com.musfit.data.repository.BodyMeasurement
import com.musfit.data.repository.FoodRepository
import com.musfit.data.repository.HealthRepository
import com.musfit.data.repository.MEASUREMENT_TYPES
import com.musfit.data.repository.ProfileRepository
import com.musfit.data.repository.UserProfile
import com.musfit.data.repository.WeightEntry
import com.musfit.domain.profile.ActivityLevel
import com.musfit.domain.profile.BodyMetricsCalculator
import com.musfit.domain.profile.GoalType
import com.musfit.domain.profile.RecommendedTargets
import com.musfit.domain.profile.Sex
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import javax.inject.Inject

data class VitalsSummary(
    val steps: Long?,
    val activeCaloriesKcal: Double?,
    val restingHeartRateBpm: Long?,
)

data class MeasurementRow(
    val type: String,
    val label: String,
    val value: Double?,
    val unit: String,
    val deltaFromPrevious: Double?,
)

data class ProfileUiState(
    val isLoaded: Boolean = false,
    val profile: UserProfile? = null,
    val ageYears: Int? = null,
    val latestWeightKg: Double? = null,
    val bmi: Double? = null,
    val bodyFatPercent: Double? = null,
    val isProfileComplete: Boolean = false,
    val recommendedTargets: RecommendedTargets? = null,
    val weightTrend: List<Double> = emptyList(),
    val goalProgressFraction: Double? = null,
    val measurements: List<MeasurementRow> = emptyList(),
    val vitals: VitalsSummary? = null,
    val message: String? = null,
)

private val MEASUREMENT_LABELS = mapOf(
    "waist" to "Waist", "chest" to "Chest", "arms" to "Arms",
    "thighs" to "Thighs", "hips" to "Hips", "body_fat" to "Body fat",
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val healthRepository: HealthRepository,
    private val foodRepository: FoodRepository,
) : ViewModel() {
    private val today = LocalDate.now()
    private val messageFlow = MutableStateFlow<String?>(null)

    private val dataState: Flow<ProfileUiState> = combine(
        profileRepository.observeProfile(),
        profileRepository.observeRecommendedTargets(),
        profileRepository.observeWeightSeries(0L),
        profileRepository.observeRecentMeasurements(0L),
    ) { profile, targets, weightSeries, measurements ->
        val latestWeight = weightSeries.firstOrNull()?.weightKg
        val bmi = if (latestWeight != null && profile.heightCm != null) {
            BodyMetricsCalculator.bodyMassIndex(latestWeight, profile.heightCm!!)
        } else {
            null
        }
        val bodyFat = measurements["body_fat"]?.firstOrNull()?.value
        val complete = profile.sex != null && profile.heightCm != null &&
            profile.birthDateEpochDay != null && latestWeight != null
        val startWeight = weightSeries.lastOrNull()?.weightKg
        val progress = if (startWeight != null && latestWeight != null && profile.goalWeightKg != null) {
            BodyMetricsCalculator.goalProgressFraction(startWeight, latestWeight, profile.goalWeightKg!!)
        } else {
            null
        }
        ProfileUiState(
            isLoaded = true,
            profile = profile,
            ageYears = profile.birthDateEpochDay?.let { ageYears(it) },
            latestWeightKg = latestWeight,
            bmi = bmi,
            bodyFatPercent = bodyFat,
            isProfileComplete = complete,
            recommendedTargets = targets,
            weightTrend = weightSeries.map { it.weightKg }.reversed(),
            goalProgressFraction = progress,
            measurements = MEASUREMENT_TYPES.map { type -> measurementRow(type, measurements[type].orEmpty()) },
        )
    }

    val state: StateFlow<ProfileUiState> = combine(
        dataState,
        healthRepository.observeDailySummary(today),
        messageFlow,
    ) { base, summary, message ->
        base.copy(vitals = summary?.toVitals(), message = message)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ProfileUiState())

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch { profileRepository.saveProfile(profile) }
    }

    fun logWeight(weightKg: Double) {
        viewModelScope.launch {
            runCatching { profileRepository.logWeight(weightKg) }
                .onFailure { messageFlow.value = it.message ?: "Could not log weight." }
        }
    }

    fun logMeasurement(type: String, value: Double, unit: String) {
        viewModelScope.launch {
            runCatching { profileRepository.logMeasurement(type, value, unit) }
                .onFailure { messageFlow.value = it.message ?: "Could not log measurement." }
        }
    }

    fun applyTargetsToFood() {
        val targets = state.value.recommendedTargets ?: return
        viewModelScope.launch {
            runCatching {
                val current = foodRepository.observeFoodGoal().first()
                foodRepository.updateFoodGoal(
                    current.copy(
                        dailyCaloriesKcal = targets.caloriesKcal,
                        proteinGrams = targets.proteinGrams,
                        carbsGrams = targets.carbsGrams,
                        fatGrams = targets.fatGrams,
                    ),
                )
            }.onSuccess {
                messageFlow.value = "Applied your targets to Food goals."
            }.onFailure {
                messageFlow.value = it.message ?: "Could not apply targets to Food."
            }
        }
    }

    fun dismissMessage() {
        messageFlow.value = null
    }

    private fun measurementRow(type: String, history: List<BodyMeasurement>): MeasurementRow {
        val latest = history.firstOrNull()
        val previous = history.getOrNull(1)
        return MeasurementRow(
            type = type,
            label = MEASUREMENT_LABELS[type] ?: type,
            value = latest?.value,
            unit = latest?.unit ?: if (type == "body_fat") "%" else "cm",
            deltaFromPrevious = if (latest != null && previous != null) latest.value - previous.value else null,
        )
    }

    private fun ageYears(birthDateEpochDay: Long): Int =
        Period.between(LocalDate.ofEpochDay(birthDateEpochDay), today).years.coerceAtLeast(0)
}

private fun DailyHealthSummaryEntity.toVitals() =
    VitalsSummary(steps = steps, activeCaloriesKcal = activeCaloriesKcal, restingHeartRateBpm = restingHeartRateBpm)
```

- [ ] **Step 4: Run the ViewModel tests to verify they pass, then commit**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.profile.ProfileViewModelTest" --no-daemon --console=plain`
Expected: PASS.

```bash
git add -A
git commit -m "feat: add ProfileViewModel orchestrating profile, vitals, and Food apply"
```

---

### Task 6: Profile dashboard cards (read-only)

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/profile/ProfileScreen.kt`

Replace the shell body with the dashboard, binding to `ProfileViewModel`. No automated UI tests; verify by build + on-device check against the approved mockup (identity, goal & targets, weight, measurements, vitals). Use Material 3 `Card`s, `MaterialTheme.typography`, and `LinearProgressIndicator`. Numbers display via a local `format` helper (`if (this % 1.0 == 0.0) toInt().toString() else String.format(Locale.US, "%.1f", this)`), matching `TodayScreen`/`HealthViewModel`.

- [ ] **Step 1: Implement the dashboard composable**

Rewrite `ProfileScreen.kt` so `ProfileScreen(onSettingsClick)` reads `viewModel: ProfileViewModel = hiltViewModel()`, collects `state`, and renders, in a scrolling `Column`:

1. Identity `Card`: an initials circle, `"<age> yrs · <height> cm · <activityLabel>"`, and a 3-column row of `Weight` / `BMI` / `Body fat` (show `"—"` when null). Tapping the card calls `onEditProfile` (added in Task 7).
2. Goal & targets `Card`: goal label + pace; `"<latestWeight> → <goalWeight> kg"`; a `LinearProgressIndicator(progress = goalProgressFraction ?: 0f)`; if `recommendedTargets != null`, the calories number + `"P/C/F"` chips and an `"Apply to Food goals"` `Button(onClick = viewModel::applyTargetsToFood)`; else a `"Complete your profile"` message.
3. Weight `Card`: latest weight + a `"Log"` `TextButton` (wired in Task 8); show `"No weight logged yet"` when null.
4. Measurements `Card`: a 2-column grid of `state.measurements` rows (label + value+unit or `"—"`), and a `"Log"` `TextButton` (wired in Task 8).
5. Vitals `Card` ("From Health Connect"): resting HR / steps / active kcal from `state.vitals`, or `"Connect Health Connect"` text linking via `onSettingsClick` when `vitals == null`.

Render `state.message` (when non-null) as a `Text` line and clear it with a `LaunchedEffect(state.message)` that calls `viewModel.dismissMessage()` after display, or surface it with a `Snackbar`. Keep the top bar + gear from the shell.

Reference the approved mockup for layout/spacing. Keep weights/lengths metric.

- [ ] **Step 2: Build and verify on device**

Run: `.\gradlew.bat assembleDebug --no-daemon --console=plain`
Then: `adb install -r app\build\outputs\apk\debug\app-debug.apk` and `adb shell monkey -p com.musfit -c android.intent.category.LAUNCHER 1`.
Expected: the Profile tab shows the five cards; the gear opens Settings; back returns. (Cards show empty/"—" states until data exists — that is expected before Tasks 7–8.)

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat: build the Profile dashboard cards"
```

---

### Task 7: Profile editor + Apply to Food goals

**Files:**
- Create: `app/src/main/java/com/musfit/ui/profile/ProfileEditContent.kt`
- Modify: `app/src/main/java/com/musfit/ui/profile/ProfileScreen.kt`

- [ ] **Step 1: Implement the profile editor sheet**

Create `ProfileEditContent.kt` with a composable `ProfileEditDialog(initial: UserProfile, onDismiss: () -> Unit, onSave: (UserProfile) -> Unit)` using an `AlertDialog` (or `ModalBottomSheet`) containing:
- Sex: a two-option segmented control / `FilterChip` row mapping to `Sex.Male` / `Sex.Female`.
- Birthdate: a numeric "Age (years)" `OutlinedTextField` for v1 (convert to `birthDateEpochDay` via `LocalDate.now().minusYears(age).toEpochDay()` on save; pre-fill age from `initial.birthDateEpochDay`).
- Height (cm): numeric `OutlinedTextField`.
- Activity level: a dropdown / chip row over `ActivityLevel.values()` (label each: Sedentary, Light, Moderate, Active, Very active).
- Goal type: chip row over `GoalType.values()` (Lose / Maintain / Gain).
- Goal pace (kg/week): chip row of `0.25`, `0.5`, `0.75` (enabled only when goal != Maintain).
- Goal weight (kg): numeric `OutlinedTextField`.
`onSave` builds a `UserProfile` and passes it up. Validate positive height/weight; ignore blank numeric fields (keep them null).

- [ ] **Step 2: Wire the editor and the initial-weight capture into `ProfileScreen`**

In `ProfileScreen.kt`: add `var showEditor by remember { mutableStateOf(false) }`; open it from the identity card tap and from a "Complete your profile" button. On save, call `viewModel.saveProfile(updated)`. Because the recommendation needs a current weight, the editor also exposes a "Current weight (kg)" field on first completion; when present and the user has no weight history yet, call `viewModel.logWeight(currentWeight)` after `saveProfile`. The `"Apply to Food goals"` button is already wired in Task 6.

- [ ] **Step 3: Build, verify on device, commit**

Run: `.\gradlew.bat assembleDebug --no-daemon --console=plain`, install, and confirm: completing the profile makes the goal card show recommended calories/macros, and "Apply to Food goals" updates the Food tab's goal.

```bash
git add -A
git commit -m "feat: add Profile editor and wire recommended-targets apply"
```

---

### Task 8: Weight & measurement logging

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/profile/ProfileEditContent.kt`
- Modify: `app/src/main/java/com/musfit/ui/profile/ProfileScreen.kt`

- [ ] **Step 1: Implement log dialogs**

In `ProfileEditContent.kt` add:
- `LogWeightDialog(prefillKg: Double?, onDismiss, onConfirm: (Double) -> Unit)` — a single numeric `OutlinedTextField` prefilled from `prefillKg` (the latest Health Connect weight when available, read from `state.latestWeightKg` or the daily summary), confirm calls `onConfirm`.
- `LogMeasurementDialog(types: List<Pair<String,String>>, onDismiss, onConfirm: (type: String, value: Double, unit: String) -> Unit)` — a type chip row (waist/chest/arms/thighs/hips → unit `cm`; body_fat → unit `%`) plus a numeric value field.

- [ ] **Step 2: Wire the dialogs into the dashboard**

In `ProfileScreen.kt`: the Weight card "Log" button opens `LogWeightDialog`, confirming with `viewModel.logWeight(value)`. The Measurements card "Log" button opens `LogMeasurementDialog`, confirming with `viewModel.logMeasurement(type, value, unit)`. State updates flow back through the existing `StateFlow`, refreshing trend, BMI, and deltas.

- [ ] **Step 3: Build, verify on device, commit**

Run: `.\gradlew.bat assembleDebug --no-daemon --console=plain`, install, and confirm logging a weight updates the weight card and BMI, and logging a measurement updates its row + delta.

```bash
git add -A
git commit -m "feat: add weight and measurement logging to the Profile tab"
```

---

### Task 9: Full verification and handoff notes

**Files:**
- Modify: `AGENTS.md` (optional: note the Profile tab and DB version 17)

- [ ] **Step 1: Run full verification**

Run: `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL; all unit tests pass; lint clean (address any new warnings introduced by this work).

- [ ] **Step 2: On-device smoke test**

Install and walk the acceptance path: complete profile → see recommended targets → Apply to Food goals (confirm in Food) → log weight (prefilled from HC if connected) → log a measurement → open Settings, run Refresh status / Import today / Export latest workout → back to dashboard.

- [ ] **Step 3: Optional doc touch-up and commit**

If updating docs, note in `AGENTS.md` that the 4th tab is Profile and the DB is at version 17. Commit any verification-driven fixes.

```bash
git add -A
git commit -m "chore: verify Profile tab v1 and update handoff notes"
```

---

## Acceptance Criteria

- The 4th tab reads "Profile" with a person icon and opens the body/goal dashboard; a gear opens a Settings sub-screen and back returns.
- Completing the profile (sex, age/birthdate, height, activity) and goal (intent, pace, goal weight) plus a current weight shows recommended calories + macros.
- "Apply to Food goals" updates Food's goal calories/protein/carbs/fat while preserving mode, training-calorie inclusion, net-carbs, water, and micros; Food can still override.
- Weight logs (prefilled from the latest Health Connect weight when available) update the weight card, trend, BMI, and goal progress; measurements log and show latest value + delta.
- The vitals strip shows resting HR / steps / active calories from the Health Connect daily summary, and prompts to connect when unavailable.
- Health Connect status/import/export work from Settings exactly as before (regression covered by `ProfileSettingsViewModelTest`).
- Migration 16 → 17 ships with `17.json` and the DAO round-trip test; existing installs upgrade without data loss.
- `testDebugUnitTest`, `lintDebug`, and `assembleDebug` all pass.

## Non-Goals (deferred)

- Imperial units / app-wide unit conversion; theme / dark-mode toggle; data backup/export.
- Writing weight back to Health Connect; progress photos; blood pressure / detailed sleep.
- Editing nutrition goals inside Profile (Food remains the editor).
- Accounts, cloud sync, analytics, subscriptions, social, cloud AI.
