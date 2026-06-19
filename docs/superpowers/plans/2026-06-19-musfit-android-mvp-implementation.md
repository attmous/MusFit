# MusFit Android MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first usable Android-only MusFit MVP with meal tracking, barcode product lookup, structured workout tracking, Health Connect integration, and GitHub debug APK builds.

**Architecture:** Use one native Android app module in Kotlin. Keep Room as the local source of truth, place business rules in pure Kotlin domain files, and isolate Open Food Facts plus Health Connect behind interfaces so future providers can be added without changing screens.

**Tech Stack:** Android Gradle Plugin 9.2.1 with built-in Kotlin support, Gradle 9.4.1, JDK 17, minSdk 28, compileSdk 37, targetSdk 37, Jetpack Compose BOM 2026.04.01, Room 2.8.1, Hilt 2.59.2, AndroidX Hilt 1.3.0, Navigation Compose 2.9.8, Lifecycle 2.11.0, Activity Compose 1.13.0, Health Connect 1.1.0, WorkManager 2.11.2, CameraX 1.6.1, ML Kit Barcode Scanning 17.3.0, Retrofit 3.0.0, Moshi 1.15.2, Kotlin Coroutines 1.10.2, JUnit 4.13.2, Turbine 1.2.1.

## Global Constraints

- Android only. Do not add iOS, web, backend, or shared multiplatform modules.
- Native Kotlin only. Do not use React Native, Flutter, Expo, or WebView-based app shells.
- Use AGP 9 built-in Kotlin support. Do not apply `org.jetbrains.kotlin.android` or `org.jetbrains.kotlin.kapt`; use `com.android.legacy-kapt` for KAPT-backed annotation processors.
- Local Room database is the source of truth for meals, foods, workouts, routines, body metrics, daily summaries, and sync metadata.
- Health Connect is part of the first usable release, but the app must still work when Health Connect is unavailable, denied, or partially permitted.
- Request Health Connect permissions only from the Health tab or an explicit sync action.
- Do not silently overwrite user-entered local data with imported health data.
- Open Food Facts is the first barcode/product provider.
- Barcode lookup results must be editable before saving locally.
- No user accounts, cloud sync, analytics, subscriptions, social features, AI coaching, direct wearable cloud APIs, or Play Store publishing in the MVP.
- GitHub Actions must run build, unit tests, lint, and upload a debug APK artifact.
- Use PowerShell command examples locally and POSIX shell commands in GitHub Actions.

---

## File Structure

Create this Android project structure:

```text
MusFit/
  .github/workflows/android.yml
  .gitignore
  build.gradle.kts
  gradle.properties
  gradle/libs.versions.toml
  gradle/wrapper/gradle-wrapper.properties
  gradlew
  gradlew.bat
  settings.gradle.kts
  app/build.gradle.kts
  app/src/main/AndroidManifest.xml
  app/src/main/java/com/musfit/MusFitApplication.kt
  app/src/main/java/com/musfit/MainActivity.kt
  app/src/main/java/com/musfit/core/di/AppModule.kt
  app/src/main/java/com/musfit/core/time/ClockProvider.kt
  app/src/main/java/com/musfit/domain/model/*.kt
  app/src/main/java/com/musfit/domain/nutrition/*.kt
  app/src/main/java/com/musfit/domain/training/*.kt
  app/src/main/java/com/musfit/domain/health/*.kt
  app/src/main/java/com/musfit/data/local/*.kt
  app/src/main/java/com/musfit/data/local/dao/*.kt
  app/src/main/java/com/musfit/data/local/entity/*.kt
  app/src/main/java/com/musfit/data/repository/*.kt
  app/src/main/java/com/musfit/data/remote/food/*.kt
  app/src/main/java/com/musfit/integrations/healthconnect/*.kt
  app/src/main/java/com/musfit/ui/AppNavGraph.kt
  app/src/main/java/com/musfit/ui/theme/*.kt
  app/src/main/java/com/musfit/ui/today/*.kt
  app/src/main/java/com/musfit/ui/food/*.kt
  app/src/main/java/com/musfit/ui/training/*.kt
  app/src/main/java/com/musfit/ui/health/*.kt
  app/src/main/res/values/strings.xml
  app/src/test/java/com/musfit/**/*.kt
```

Responsibility rules:

- `domain` files contain pure Kotlin models and calculations. They must not import Android, Room, Retrofit, Compose, or Health Connect.
- `data/local` owns Room entities, DAOs, and database wiring.
- `data/remote/food` owns Open Food Facts DTOs and product normalization.
- `integrations/healthconnect` owns Health Connect availability, permissions, record reads, record writes, and mapping.
- `ui` owns Compose screens, ViewModels, and navigation only.

### Task 1: Project Scaffold, Dependency Catalog, And CI

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/musfit/MusFitApplication.kt`
- Create: `app/src/main/java/com/musfit/MainActivity.kt`
- Create: `app/src/main/res/values/strings.xml`
- Create: `.github/workflows/android.yml`
- Create: `.gitignore`

**Interfaces:**
- Produces: a buildable Android app with package `com.musfit`, Hilt enabled, Compose enabled, and an initial `MainActivity`.
- Produces: Gradle tasks `testDebugUnitTest`, `lintDebug`, and `assembleDebug`.

- [ ] **Step 1: Generate the Gradle wrapper**

Run:

```powershell
gradle wrapper --gradle-version 9.4.1
```

Expected: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, and `gradle/wrapper/gradle-wrapper.properties` exist.

- [ ] **Step 2: Write project settings**

Create `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MusFit"
include(":app")
```

Create root `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.legacy.kapt) apply false
    alias(libs.plugins.hilt) apply false
}
```

Create `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
android.defaults.buildfeatures.buildconfig=true
kotlin.code.style=official
```

- [ ] **Step 3: Write the version catalog**

Create `gradle/libs.versions.toml`:

```toml
[versions]
agp = "9.2.1"
composeBom = "2026.04.01"
core = "1.19.0"
activity = "1.13.0"
lifecycle = "2.11.0"
navigation = "2.9.8"
hilt = "2.59.2"
androidxHilt = "1.3.0"
room = "2.8.1"
healthConnect = "1.1.0"
work = "2.11.2"
camera = "1.6.1"
mlkitBarcode = "17.3.0"
retrofit = "3.0.0"
moshi = "1.15.2"
okhttp = "4.12.0"
coroutines = "1.10.2"
junit = "4.13.2"
turbine = "1.2.1"

[libraries]
androidx-core = { module = "androidx.core:core", version.ref = "core" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activity" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation" }
androidx-hilt-navigation-compose = { module = "androidx.hilt:hilt-navigation-compose", version.ref = "androidxHilt" }
androidx-hilt-work = { module = "androidx.hilt:hilt-work", version.ref = "androidxHilt" }
androidx-hilt-compiler = { module = "androidx.hilt:hilt-compiler", version.ref = "androidxHilt" }
androidx-compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { module = "androidx.compose.ui:ui" }
androidx-compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
androidx-compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
androidx-compose-material3 = { module = "androidx.compose.material3:material3" }
androidx-room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
androidx-room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
androidx-room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
androidx-room-testing = { module = "androidx.room:room-testing", version.ref = "room" }
androidx-health-connect = { module = "androidx.health.connect:connect-client", version.ref = "healthConnect" }
androidx-work-runtime = { module = "androidx.work:work-runtime", version.ref = "work" }
androidx-work-testing = { module = "androidx.work:work-testing", version.ref = "work" }
androidx-camera-camera2 = { module = "androidx.camera:camera-camera2", version.ref = "camera" }
androidx-camera-lifecycle = { module = "androidx.camera:camera-lifecycle", version.ref = "camera" }
androidx-camera-view = { module = "androidx.camera:camera-view", version.ref = "camera" }
androidx-camera-mlkit = { module = "androidx.camera:camera-mlkit-vision", version.ref = "camera" }
mlkit-barcode = { module = "com.google.mlkit:barcode-scanning", version.ref = "mlkitBarcode" }
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-compiler", version.ref = "hilt" }
retrofit = { module = "com.squareup.retrofit2:retrofit", version.ref = "retrofit" }
retrofit-moshi = { module = "com.squareup.retrofit2:converter-moshi", version.ref = "retrofit" }
moshi = { module = "com.squareup.moshi:moshi", version.ref = "moshi" }
moshi-kotlin = { module = "com.squareup.moshi:moshi-kotlin", version.ref = "moshi" }
okhttp-logging = { module = "com.squareup.okhttp3:logging-interceptor", version.ref = "okhttp" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
junit = { module = "junit:junit", version.ref = "junit" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-legacy-kapt = { id = "com.android.legacy-kapt", version.ref = "agp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

- [ ] **Step 4: Write the app module build file**

Create `app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.android.legacy.kapt)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.musfit"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.musfit"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

kapt {
    correctErrorTypes = true
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.health.connect)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.mlkit)
    implementation(libs.mlkit.barcode)
    implementation(libs.hilt.android)
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.coroutines.android)

    kapt(libs.hilt.compiler)
    kapt(libs.androidx.hilt.compiler)
    kapt(libs.androidx.room.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.work.testing)
}
```

- [ ] **Step 5: Write manifest and app entry points**

Create `app/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.health.READ_STEPS" />
    <uses-permission android:name="android.permission.health.READ_WEIGHT" />
    <uses-permission android:name="android.permission.health.READ_ACTIVE_CALORIES_BURNED" />
    <uses-permission android:name="android.permission.health.READ_HEART_RATE" />
    <uses-permission android:name="android.permission.health.WRITE_EXERCISE" />

    <queries>
        <package android:name="com.google.android.apps.healthdata" />
    </queries>

    <application
        android:name=".MusFitApplication"
        android:allowBackup="false"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.MusFit">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

Create `app/src/main/java/com/musfit/MusFitApplication.kt`:

```kotlin
package com.musfit

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MusFitApplication : Application()
```

Create `app/src/main/java/com/musfit/MainActivity.kt`:

```kotlin
package com.musfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    Text(text = "MusFit")
                }
            }
        }
    }
}
```

Create `app/src/main/res/values/strings.xml`:

```xml
<resources>
    <string name="app_name">MusFit</string>
</resources>
```

Create `app/src/main/res/values/styles.xml`:

```xml
<resources>
    <style name="Theme.MusFit" parent="android:style/Theme.Material.Light.NoActionBar" />
</resources>
```

Create `app/src/main/res/xml/backup_rules.xml`:

```xml
<full-backup-content>
    <exclude domain="database" path="." />
</full-backup-content>
```

Create `app/src/main/res/xml/data_extraction_rules.xml`:

```xml
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="database" path="." />
    </cloud-backup>
    <device-transfer>
        <exclude domain="database" path="." />
    </device-transfer>
</data-extraction-rules>
```

- [ ] **Step 6: Write CI and ignore rules**

Create `.github/workflows/android.yml`:

```yaml
name: Android

on:
  push:
    branches: [ "master", "main" ]
  pull_request:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"
          cache: gradle

      - name: Make Gradle executable
        run: chmod +x ./gradlew

      - name: Unit tests
        run: ./gradlew testDebugUnitTest

      - name: Lint
        run: ./gradlew lintDebug

      - name: Assemble debug APK
        run: ./gradlew assembleDebug

      - name: Upload debug APK
        uses: actions/upload-artifact@v4
        with:
          name: musfit-debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk
```

Create `.gitignore`:

```gitignore
.gradle/
.idea/
*.iml
local.properties
build/
app/build/
captures/
.DS_Store
```

- [ ] **Step 7: Run build verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Expected: Gradle exits with code 0 and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 8: Commit**

```powershell
git add .github .gitignore build.gradle.kts gradle.properties gradle settings.gradle.kts app
git commit -m "chore: scaffold Android project"
```

### Task 2: Pure Domain Models And Calculations

**Files:**
- Create: `app/src/main/java/com/musfit/domain/model/NutritionModels.kt`
- Create: `app/src/main/java/com/musfit/domain/model/TrainingModels.kt`
- Create: `app/src/main/java/com/musfit/domain/nutrition/NutritionCalculator.kt`
- Create: `app/src/main/java/com/musfit/domain/training/WorkoutCalculator.kt`
- Create: `app/src/test/java/com/musfit/domain/nutrition/NutritionCalculatorTest.kt`
- Create: `app/src/test/java/com/musfit/domain/training/WorkoutCalculatorTest.kt`

**Interfaces:**
- Produces: `NutritionCalculator.calculateMealTotals(items: List<MealItemInput>): NutritionTotals`
- Produces: `WorkoutCalculator.totalVolume(sets: List<WorkoutSetInput>): Double`
- Produces: `WorkoutCalculator.estimatedOneRepMax(weightKg: Double, reps: Int): Double`
- Produces: `WorkoutCalculator.personalRecords(sets: List<WorkoutSetInput>): PersonalRecords`

- [ ] **Step 1: Write failing nutrition tests**

Create `app/src/test/java/com/musfit/domain/nutrition/NutritionCalculatorTest.kt`:

```kotlin
package com.musfit.domain.nutrition

import com.musfit.domain.model.FoodNutrition
import com.musfit.domain.model.MealItemInput
import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionCalculatorTest {
    @Test
    fun calculateMealTotals_scalesPer100gNutritionByQuantity() {
        val items = listOf(
            MealItemInput(
                foodId = "rice",
                quantityGrams = 150.0,
                nutritionPer100g = FoodNutrition(
                    caloriesKcal = 130.0,
                    proteinGrams = 2.7,
                    carbsGrams = 28.0,
                    fatGrams = 0.3,
                ),
            ),
        )

        val totals = NutritionCalculator.calculateMealTotals(items)

        assertEquals(195.0, totals.caloriesKcal, 0.01)
        assertEquals(4.05, totals.proteinGrams, 0.01)
        assertEquals(42.0, totals.carbsGrams, 0.01)
        assertEquals(0.45, totals.fatGrams, 0.01)
    }

    @Test
    fun calculateMealTotals_ignoresZeroAndNegativeQuantities() {
        val items = listOf(
            MealItemInput(
                foodId = "bad",
                quantityGrams = -40.0,
                nutritionPer100g = FoodNutrition(100.0, 10.0, 10.0, 10.0),
            ),
            MealItemInput(
                foodId = "zero",
                quantityGrams = 0.0,
                nutritionPer100g = FoodNutrition(100.0, 10.0, 10.0, 10.0),
            ),
        )

        val totals = NutritionCalculator.calculateMealTotals(items)

        assertEquals(0.0, totals.caloriesKcal, 0.01)
        assertEquals(0.0, totals.proteinGrams, 0.01)
        assertEquals(0.0, totals.carbsGrams, 0.01)
        assertEquals(0.0, totals.fatGrams, 0.01)
    }
}
```

- [ ] **Step 2: Write failing workout tests**

Create `app/src/test/java/com/musfit/domain/training/WorkoutCalculatorTest.kt`:

```kotlin
package com.musfit.domain.training

import com.musfit.domain.model.WorkoutSetInput
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutCalculatorTest {
    @Test
    fun totalVolume_sumsCompletedWeightedSets() {
        val sets = listOf(
            WorkoutSetInput(exerciseId = "bench", reps = 5, weightKg = 100.0, completed = true),
            WorkoutSetInput(exerciseId = "bench", reps = 5, weightKg = 102.5, completed = true),
            WorkoutSetInput(exerciseId = "bench", reps = 5, weightKg = 105.0, completed = false),
        )

        assertEquals(1012.5, WorkoutCalculator.totalVolume(sets), 0.01)
    }

    @Test
    fun estimatedOneRepMax_usesEpleyFormula() {
        assertEquals(120.0, WorkoutCalculator.estimatedOneRepMax(weightKg = 100.0, reps = 6), 0.01)
    }

    @Test
    fun personalRecords_returnsBestWeightRepsAndVolume() {
        val sets = listOf(
            WorkoutSetInput("squat", reps = 8, weightKg = 120.0, completed = true),
            WorkoutSetInput("squat", reps = 3, weightKg = 150.0, completed = true),
            WorkoutSetInput("deadlift", reps = 5, weightKg = 180.0, completed = true),
        )

        val records = WorkoutCalculator.personalRecords(sets)

        assertEquals(180.0, records.heaviestWeightKg, 0.01)
        assertEquals(8, records.maxReps)
        assertEquals(2310.0, records.totalVolumeKg, 0.01)
    }
}
```

- [ ] **Step 3: Run tests and verify failure**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "*NutritionCalculatorTest" --tests "*WorkoutCalculatorTest"
```

Expected: tests fail because the domain model and calculator classes do not exist.

- [ ] **Step 4: Implement domain models**

Create `app/src/main/java/com/musfit/domain/model/NutritionModels.kt`:

```kotlin
package com.musfit.domain.model

data class FoodNutrition(
    val caloriesKcal: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
)

data class MealItemInput(
    val foodId: String,
    val quantityGrams: Double,
    val nutritionPer100g: FoodNutrition,
)

data class NutritionTotals(
    val caloriesKcal: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
)
```

Create `app/src/main/java/com/musfit/domain/model/TrainingModels.kt`:

```kotlin
package com.musfit.domain.model

data class WorkoutSetInput(
    val exerciseId: String,
    val reps: Int,
    val weightKg: Double,
    val completed: Boolean,
)

data class PersonalRecords(
    val heaviestWeightKg: Double,
    val maxReps: Int,
    val totalVolumeKg: Double,
    val bestEstimatedOneRepMaxKg: Double,
)
```

- [ ] **Step 5: Implement calculators**

Create `app/src/main/java/com/musfit/domain/nutrition/NutritionCalculator.kt`:

```kotlin
package com.musfit.domain.nutrition

import com.musfit.domain.model.MealItemInput
import com.musfit.domain.model.NutritionTotals

object NutritionCalculator {
    fun calculateMealTotals(items: List<MealItemInput>): NutritionTotals {
        val validItems = items.filter { it.quantityGrams > 0.0 }
        return NutritionTotals(
            caloriesKcal = validItems.sumOf { it.nutritionPer100g.caloriesKcal * it.quantityGrams / 100.0 },
            proteinGrams = validItems.sumOf { it.nutritionPer100g.proteinGrams * it.quantityGrams / 100.0 },
            carbsGrams = validItems.sumOf { it.nutritionPer100g.carbsGrams * it.quantityGrams / 100.0 },
            fatGrams = validItems.sumOf { it.nutritionPer100g.fatGrams * it.quantityGrams / 100.0 },
        )
    }
}
```

Create `app/src/main/java/com/musfit/domain/training/WorkoutCalculator.kt`:

```kotlin
package com.musfit.domain.training

import com.musfit.domain.model.PersonalRecords
import com.musfit.domain.model.WorkoutSetInput

object WorkoutCalculator {
    fun totalVolume(sets: List<WorkoutSetInput>): Double =
        sets.filter { it.completed && it.reps > 0 && it.weightKg > 0.0 }
            .sumOf { it.reps * it.weightKg }

    fun estimatedOneRepMax(weightKg: Double, reps: Int): Double {
        if (weightKg <= 0.0 || reps <= 0) return 0.0
        return weightKg * (1.0 + reps / 30.0)
    }

    fun personalRecords(sets: List<WorkoutSetInput>): PersonalRecords {
        val completed = sets.filter { it.completed }
        return PersonalRecords(
            heaviestWeightKg = completed.maxOfOrNull { it.weightKg } ?: 0.0,
            maxReps = completed.maxOfOrNull { it.reps } ?: 0,
            totalVolumeKg = totalVolume(completed),
            bestEstimatedOneRepMaxKg = completed.maxOfOrNull {
                estimatedOneRepMax(weightKg = it.weightKg, reps = it.reps)
            } ?: 0.0,
        )
    }
}
```

- [ ] **Step 6: Run tests and verify pass**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "*NutritionCalculatorTest" --tests "*WorkoutCalculatorTest"
```

Expected: all tests in both classes pass.

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/java/com/musfit/domain app/src/test/java/com/musfit/domain
git commit -m "feat: add nutrition and workout calculations"
```

### Task 3: Room Schema, DAOs, And Local Database

**Files:**
- Create: `app/src/main/java/com/musfit/data/local/entity/FoodEntities.kt`
- Create: `app/src/main/java/com/musfit/data/local/entity/TrainingEntities.kt`
- Create: `app/src/main/java/com/musfit/data/local/entity/HealthEntities.kt`
- Create: `app/src/main/java/com/musfit/data/local/dao/FoodDao.kt`
- Create: `app/src/main/java/com/musfit/data/local/dao/TrainingDao.kt`
- Create: `app/src/main/java/com/musfit/data/local/dao/HealthDao.kt`
- Create: `app/src/main/java/com/musfit/data/local/MusFitDatabase.kt`
- Create: `app/src/main/java/com/musfit/core/di/DatabaseModule.kt`
- Modify: `app/build.gradle.kts`

**Interfaces:**
- Consumes: domain models from Task 2.
- Produces: `MusFitDatabase`, `FoodDao`, `TrainingDao`, `HealthDao`.
- Produces: local tables for foods, servings, meals, meal items, barcode products, exercises, routines, routine exercises, workout sessions, workout sets, body metrics, daily health summaries, and Health Connect sync state.

- [ ] **Step 1: Add Room schema export configuration**

Modify `app/build.gradle.kts` inside `android.defaultConfig`:

```kotlin
javaCompileOptions {
    annotationProcessorOptions {
        arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
    }
}
```

Add this source set block inside `android`:

```kotlin
sourceSets {
    getByName("androidTest").assets.srcDir("$projectDir/schemas")
}
```

- [ ] **Step 2: Create entity classes**

Create `app/src/main/java/com/musfit/data/local/entity/FoodEntities.kt`:

```kotlin
package com.musfit.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey val id: String,
    val name: String,
    val brand: String?,
    val defaultServingGrams: Double,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "food_servings",
    foreignKeys = [
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("foodId")],
)
data class FoodServingEntity(
    @PrimaryKey val id: String,
    val foodId: String,
    val label: String,
    val grams: Double,
)

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey val id: String,
    val dateEpochDay: Long,
    val type: String,
    val notes: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "meal_items",
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("mealId"), Index("foodId")],
)
data class MealItemEntity(
    @PrimaryKey val id: String,
    val mealId: String,
    val foodId: String,
    val quantityGrams: Double,
)

@Entity(tableName = "barcode_products", indices = [Index(value = ["barcode"], unique = true)])
data class BarcodeProductEntity(
    @PrimaryKey val id: String,
    val barcode: String,
    val provider: String,
    val providerProductName: String?,
    val providerBrand: String?,
    val rawJson: String,
    val quality: String,
    val linkedFoodId: String?,
    val fetchedAtEpochMillis: Long,
)
```

Create `app/src/main/java/com/musfit/data/local/entity/TrainingEntities.kt`:

```kotlin
package com.musfit.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val equipment: String?,
    val targetMuscles: String,
    val isCustom: Boolean,
)

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val notes: String?,
    val createdAtEpochMillis: Long,
)

@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(entity = RoutineEntity::class, parentColumns = ["id"], childColumns = ["routineId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("routineId"), Index("exerciseId")],
)
data class RoutineExerciseEntity(
    @PrimaryKey val id: String,
    val routineId: String,
    val exerciseId: String,
    val sortOrder: Int,
    val targetSets: Int,
    val targetReps: String?,
)

@Entity(tableName = "workout_sessions", indices = [Index("startedAtEpochMillis")])
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val routineId: String?,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long?,
    val notes: String?,
    val healthConnectRecordId: String?,
    val healthConnectLastExportedAtEpochMillis: Long?,
)

@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(entity = WorkoutSessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("sessionId"), Index("exerciseId")],
)
data class WorkoutSetEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val exerciseId: String,
    val sortOrder: Int,
    val reps: Int?,
    val weightKg: Double?,
    val durationSeconds: Long?,
    val distanceMeters: Double?,
    val rpe: Double?,
    val notes: String?,
    val completed: Boolean,
)
```

Create `app/src/main/java/com/musfit/data/local/entity/HealthEntities.kt`:

```kotlin
package com.musfit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "body_metrics")
data class BodyMetricEntity(
    @PrimaryKey val id: String,
    val type: String,
    val value: Double,
    val unit: String,
    val measuredAtEpochMillis: Long,
    val source: String,
    val externalId: String?,
)

@Entity(tableName = "daily_health_summaries")
data class DailyHealthSummaryEntity(
    @PrimaryKey val dateEpochDay: Long,
    val steps: Long?,
    val activeCaloriesKcal: Double?,
    val latestWeightKg: Double?,
    val restingHeartRateBpm: Long?,
    val updatedAtEpochMillis: Long,
)

@Entity(tableName = "health_connect_sync_state")
data class HealthConnectSyncStateEntity(
    @PrimaryKey val key: String,
    val isAvailable: Boolean,
    val grantedPermissionsCsv: String,
    val lastImportAtEpochMillis: Long?,
    val lastExportAtEpochMillis: Long?,
    val lastFailureMessage: String?,
)
```

- [ ] **Step 3: Create DAOs**

Create `app/src/main/java/com/musfit/data/local/dao/FoodDao.kt`:

```kotlin
package com.musfit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musfit.data.local.entity.BarcodeProductEntity
import com.musfit.data.local.entity.FoodEntity
import com.musfit.data.local.entity.FoodServingEntity
import com.musfit.data.local.entity.MealEntity
import com.musfit.data.local.entity.MealItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM foods ORDER BY name")
    fun observeFoods(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM meals WHERE dateEpochDay = :dateEpochDay ORDER BY createdAtEpochMillis")
    fun observeMealsForDate(dateEpochDay: Long): Flow<List<MealEntity>>

    @Query("SELECT * FROM meal_items WHERE mealId = :mealId")
    fun observeMealItems(mealId: String): Flow<List<MealItemEntity>>

    @Query("SELECT * FROM barcode_products WHERE barcode = :barcode LIMIT 1")
    suspend fun getBarcodeProduct(barcode: String): BarcodeProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFood(food: FoodEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertServing(serving: FoodServingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeal(meal: MealEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMealItem(item: MealItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBarcodeProduct(product: BarcodeProductEntity)
}
```

Create `app/src/main/java/com/musfit/data/local/dao/TrainingDao.kt`:

```kotlin
package com.musfit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musfit.data.local.entity.ExerciseEntity
import com.musfit.data.local.entity.RoutineEntity
import com.musfit.data.local.entity.RoutineExerciseEntity
import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingDao {
    @Query("SELECT * FROM exercises ORDER BY name")
    fun observeExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM routines ORDER BY createdAtEpochMillis DESC")
    fun observeRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM workout_sessions ORDER BY startedAtEpochMillis DESC")
    fun observeWorkoutSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY sortOrder")
    fun observeWorkoutSets(sessionId: String): Flow<List<WorkoutSetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercise(exercise: ExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoutine(routine: RoutineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoutineExercise(routineExercise: RoutineExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkoutSession(session: WorkoutSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkoutSet(set: WorkoutSetEntity)
}
```

Create `app/src/main/java/com/musfit/data/local/dao/HealthDao.kt`:

```kotlin
package com.musfit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musfit.data.local.entity.BodyMetricEntity
import com.musfit.data.local.entity.DailyHealthSummaryEntity
import com.musfit.data.local.entity.HealthConnectSyncStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthDao {
    @Query("SELECT * FROM daily_health_summaries WHERE dateEpochDay = :dateEpochDay LIMIT 1")
    fun observeDailySummary(dateEpochDay: Long): Flow<DailyHealthSummaryEntity?>

    @Query("SELECT * FROM health_connect_sync_state WHERE key = 'health_connect' LIMIT 1")
    fun observeHealthConnectSyncState(): Flow<HealthConnectSyncStateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBodyMetric(metric: BodyMetricEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailySummary(summary: DailyHealthSummaryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHealthConnectSyncState(state: HealthConnectSyncStateEntity)
}
```

- [ ] **Step 4: Create database and DI module**

Create `app/src/main/java/com/musfit/data/local/MusFitDatabase.kt`:

```kotlin
package com.musfit.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.musfit.data.local.dao.FoodDao
import com.musfit.data.local.dao.HealthDao
import com.musfit.data.local.dao.TrainingDao
import com.musfit.data.local.entity.BarcodeProductEntity
import com.musfit.data.local.entity.BodyMetricEntity
import com.musfit.data.local.entity.DailyHealthSummaryEntity
import com.musfit.data.local.entity.ExerciseEntity
import com.musfit.data.local.entity.FoodEntity
import com.musfit.data.local.entity.FoodServingEntity
import com.musfit.data.local.entity.HealthConnectSyncStateEntity
import com.musfit.data.local.entity.MealEntity
import com.musfit.data.local.entity.MealItemEntity
import com.musfit.data.local.entity.RoutineEntity
import com.musfit.data.local.entity.RoutineExerciseEntity
import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity

@Database(
    entities = [
        FoodEntity::class,
        FoodServingEntity::class,
        MealEntity::class,
        MealItemEntity::class,
        BarcodeProductEntity::class,
        ExerciseEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSetEntity::class,
        BodyMetricEntity::class,
        DailyHealthSummaryEntity::class,
        HealthConnectSyncStateEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class MusFitDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun trainingDao(): TrainingDao
    abstract fun healthDao(): HealthDao
}
```

Create `app/src/main/java/com/musfit/core/di/DatabaseModule.kt`:

```kotlin
package com.musfit.core.di

import android.content.Context
import androidx.room.Room
import com.musfit.data.local.MusFitDatabase
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
            .build()

    @Provides
    fun provideFoodDao(database: MusFitDatabase) = database.foodDao()

    @Provides
    fun provideTrainingDao(database: MusFitDatabase) = database.trainingDao()

    @Provides
    fun provideHealthDao(database: MusFitDatabase) = database.healthDao()
}
```

- [ ] **Step 5: Run schema build**

Run:

```powershell
.\gradlew.bat assembleDebug
```

Expected: build passes and `app/schemas/com.musfit.data.local.MusFitDatabase/1.json` exists.

- [ ] **Step 6: Commit**

```powershell
git add app/build.gradle.kts app/schemas app/src/main/java/com/musfit/data app/src/main/java/com/musfit/core/di
git commit -m "feat: add local Room schema"
```

### Task 4: Open Food Facts Lookup And Food Repository

**Files:**
- Create: `app/src/main/java/com/musfit/data/remote/food/FoodProductProvider.kt`
- Create: `app/src/main/java/com/musfit/data/remote/food/OpenFoodFactsApi.kt`
- Create: `app/src/main/java/com/musfit/data/remote/food/OpenFoodFactsModels.kt`
- Create: `app/src/main/java/com/musfit/data/remote/food/OpenFoodFactsProductProvider.kt`
- Create: `app/src/main/java/com/musfit/data/repository/FoodRepository.kt`
- Create: `app/src/main/java/com/musfit/core/di/NetworkModule.kt`
- Create: `app/src/main/java/com/musfit/core/di/RepositoryModule.kt`
- Create: `app/src/test/java/com/musfit/data/remote/food/OpenFoodFactsProductProviderTest.kt`

**Interfaces:**
- Produces: `FoodProductProvider.lookupBarcode(barcode: String): ProductLookupResult`
- Produces: `FoodRepository.saveConfirmedProduct(result: ProductLookupResult.Found, editedName: String, editedBrand: String?, editedNutrition: FoodNutrition): String`
- Consumes: `FoodDao` from Task 3 and `FoodNutrition` from Task 2.

- [ ] **Step 1: Write failing product provider tests**

Create `app/src/test/java/com/musfit/data/remote/food/OpenFoodFactsProductProviderTest.kt`:

```kotlin
package com.musfit.data.remote.food

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenFoodFactsProductProviderTest {
    @Test
    fun normalizeProduct_marksCompleteProductAsFound() = runTest {
        val response = OpenFoodFactsResponse(
            status = 1,
            product = OpenFoodFactsProduct(
                productName = "Greek Yogurt",
                brands = "Example Dairy",
                servingQuantity = 170.0,
                nutriments = OpenFoodFactsNutriments(
                    energyKcal100g = 59.0,
                    proteins100g = 10.0,
                    carbohydrates100g = 3.6,
                    fat100g = 0.4,
                ),
            ),
        )

        val result = OpenFoodFactsProductProvider.normalize(barcode = "1234567890123", response = response)

        assertTrue(result is ProductLookupResult.Found)
        val found = result as ProductLookupResult.Found
        assertEquals("1234567890123", found.barcode)
        assertEquals("Greek Yogurt", found.name)
        assertEquals("Example Dairy", found.brand)
        assertEquals(ProductDataQuality.Complete, found.quality)
        assertEquals(59.0, found.nutritionPer100g.caloriesKcal, 0.01)
    }

    @Test
    fun normalizeProduct_marksMissingMacrosAsIncomplete() = runTest {
        val response = OpenFoodFactsResponse(
            status = 1,
            product = OpenFoodFactsProduct(
                productName = "Mystery Bar",
                brands = null,
                servingQuantity = null,
                nutriments = OpenFoodFactsNutriments(
                    energyKcal100g = null,
                    proteins100g = null,
                    carbohydrates100g = null,
                    fat100g = null,
                ),
            ),
        )

        val result = OpenFoodFactsProductProvider.normalize(barcode = "4000000000000", response = response)

        assertTrue(result is ProductLookupResult.Found)
        assertEquals(ProductDataQuality.Incomplete, (result as ProductLookupResult.Found).quality)
    }

    @Test
    fun normalizeProduct_returnsNotFoundWhenStatusIsZero() = runTest {
        val response = OpenFoodFactsResponse(status = 0, product = null)

        val result = OpenFoodFactsProductProvider.normalize(barcode = "000", response = response)

        assertEquals(ProductLookupResult.NotFound("000"), result)
    }
}
```

- [ ] **Step 2: Run test and verify failure**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "*OpenFoodFactsProductProviderTest"
```

Expected: test compilation fails because remote food classes do not exist.

- [ ] **Step 3: Implement provider models and API**

Create `app/src/main/java/com/musfit/data/remote/food/FoodProductProvider.kt`:

```kotlin
package com.musfit.data.remote.food

import com.musfit.domain.model.FoodNutrition

interface FoodProductProvider {
    suspend fun lookupBarcode(barcode: String): ProductLookupResult
}

sealed interface ProductLookupResult {
    data class Found(
        val barcode: String,
        val name: String,
        val brand: String?,
        val servingQuantityGrams: Double?,
        val nutritionPer100g: FoodNutrition,
        val quality: ProductDataQuality,
        val rawJson: String,
    ) : ProductLookupResult

    data class NotFound(val barcode: String) : ProductLookupResult

    data class Failed(val barcode: String, val message: String) : ProductLookupResult
}

enum class ProductDataQuality {
    Complete,
    Incomplete,
}
```

Create `app/src/main/java/com/musfit/data/remote/food/OpenFoodFactsModels.kt`:

```kotlin
package com.musfit.data.remote.food

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class OpenFoodFactsResponse(
    val status: Int,
    val product: OpenFoodFactsProduct?,
)

@JsonClass(generateAdapter = false)
data class OpenFoodFactsProduct(
    @Json(name = "product_name") val productName: String?,
    val brands: String?,
    @Json(name = "serving_quantity") val servingQuantity: Double?,
    val nutriments: OpenFoodFactsNutriments?,
)

@JsonClass(generateAdapter = false)
data class OpenFoodFactsNutriments(
    @Json(name = "energy-kcal_100g") val energyKcal100g: Double?,
    @Json(name = "proteins_100g") val proteins100g: Double?,
    @Json(name = "carbohydrates_100g") val carbohydrates100g: Double?,
    @Json(name = "fat_100g") val fat100g: Double?,
)
```

Create `app/src/main/java/com/musfit/data/remote/food/OpenFoodFactsApi.kt`:

```kotlin
package com.musfit.data.remote.food

import retrofit2.http.GET
import retrofit2.http.Path

interface OpenFoodFactsApi {
    @GET("api/v2/product/{barcode}.json")
    suspend fun getProduct(@Path("barcode") barcode: String): OpenFoodFactsResponse
}
```

- [ ] **Step 4: Implement Open Food Facts provider**

Create `app/src/main/java/com/musfit/data/remote/food/OpenFoodFactsProductProvider.kt`:

```kotlin
package com.musfit.data.remote.food

import com.musfit.domain.model.FoodNutrition
import com.squareup.moshi.Moshi
import javax.inject.Inject

class OpenFoodFactsProductProvider @Inject constructor(
    private val api: OpenFoodFactsApi,
    private val moshi: Moshi,
) : FoodProductProvider {
    override suspend fun lookupBarcode(barcode: String): ProductLookupResult =
        try {
            normalize(
                barcode = barcode,
                response = api.getProduct(barcode),
                rawJson = "{}",
            )
        } catch (exception: Exception) {
            ProductLookupResult.Failed(barcode = barcode, message = exception.message ?: "Lookup failed")
        }

    companion object {
        fun normalize(
            barcode: String,
            response: OpenFoodFactsResponse,
            rawJson: String = "{}",
        ): ProductLookupResult {
            if (response.status != 1 || response.product == null) {
                return ProductLookupResult.NotFound(barcode)
            }
            val product = response.product
            val nutriments = product.nutriments
            val calories = nutriments?.energyKcal100g
            val protein = nutriments?.proteins100g
            val carbs = nutriments?.carbohydrates100g
            val fat = nutriments?.fat100g
            val hasRequiredNutrition = calories != null && protein != null && carbs != null && fat != null
            return ProductLookupResult.Found(
                barcode = barcode,
                name = product.productName?.takeIf { it.isNotBlank() } ?: "Unnamed product",
                brand = product.brands?.takeIf { it.isNotBlank() },
                servingQuantityGrams = product.servingQuantity,
                nutritionPer100g = FoodNutrition(
                    caloriesKcal = calories ?: 0.0,
                    proteinGrams = protein ?: 0.0,
                    carbsGrams = carbs ?: 0.0,
                    fatGrams = fat ?: 0.0,
                ),
                quality = if (hasRequiredNutrition) ProductDataQuality.Complete else ProductDataQuality.Incomplete,
                rawJson = rawJson,
            )
        }
    }
}
```

- [ ] **Step 5: Implement network module**

Create `app/src/main/java/com/musfit/core/di/NetworkModule.kt`:

```kotlin
package com.musfit.core.di

import com.musfit.data.remote.food.FoodProductProvider
import com.musfit.data.remote.food.OpenFoodFactsApi
import com.musfit.data.remote.food.OpenFoodFactsProductProvider
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkProvidesModule {
    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder().build()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()

    @Provides
    @Singleton
    fun provideOpenFoodFactsApi(okHttpClient: OkHttpClient, moshi: Moshi): OpenFoodFactsApi =
        Retrofit.Builder()
            .baseUrl("https://world.openfoodfacts.org/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenFoodFactsApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindsModule {
    @Binds
    abstract fun bindFoodProductProvider(provider: OpenFoodFactsProductProvider): FoodProductProvider
}
```

- [ ] **Step 6: Implement local FoodRepository**

Create `app/src/main/java/com/musfit/data/repository/FoodRepository.kt`:

```kotlin
package com.musfit.data.repository

import com.musfit.data.local.dao.FoodDao
import com.musfit.data.local.entity.BarcodeProductEntity
import com.musfit.data.local.entity.FoodEntity
import com.musfit.data.local.entity.FoodServingEntity
import com.musfit.data.remote.food.ProductDataQuality
import com.musfit.data.remote.food.ProductLookupResult
import com.musfit.domain.model.FoodNutrition
import java.util.UUID
import javax.inject.Inject

interface FoodRepository {
    suspend fun saveConfirmedProduct(
        result: ProductLookupResult.Found,
        editedName: String,
        editedBrand: String?,
        editedNutrition: FoodNutrition,
    ): String
}

class LocalFoodRepository @Inject constructor(
    private val foodDao: FoodDao,
) : FoodRepository {
    override suspend fun saveConfirmedProduct(
        result: ProductLookupResult.Found,
        editedName: String,
        editedBrand: String?,
        editedNutrition: FoodNutrition,
    ): String {
        val foodId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val servingGrams = result.servingQuantityGrams ?: 100.0
        foodDao.upsertFood(
            FoodEntity(
                id = foodId,
                name = editedName.ifBlank { result.name },
                brand = editedBrand?.ifBlank { null },
                defaultServingGrams = servingGrams,
                caloriesPer100g = editedNutrition.caloriesKcal,
                proteinPer100g = editedNutrition.proteinGrams,
                carbsPer100g = editedNutrition.carbsGrams,
                fatPer100g = editedNutrition.fatGrams,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
            ),
        )
        foodDao.upsertServing(
            FoodServingEntity(
                id = UUID.randomUUID().toString(),
                foodId = foodId,
                label = "${servingGrams.toInt()} g",
                grams = servingGrams,
            ),
        )
        foodDao.upsertBarcodeProduct(
            BarcodeProductEntity(
                id = UUID.randomUUID().toString(),
                barcode = result.barcode,
                provider = "open_food_facts",
                providerProductName = result.name,
                providerBrand = result.brand,
                rawJson = result.rawJson,
                quality = if (result.quality == ProductDataQuality.Complete) "complete" else "incomplete",
                linkedFoodId = foodId,
                fetchedAtEpochMillis = now,
            ),
        )
        return foodId
    }
}
```

Create `app/src/main/java/com/musfit/core/di/RepositoryModule.kt`:

```kotlin
package com.musfit.core.di

import com.musfit.data.repository.FoodRepository
import com.musfit.data.repository.LocalFoodRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindFoodRepository(repository: LocalFoodRepository): FoodRepository
}
```

- [ ] **Step 7: Run tests**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "*OpenFoodFactsProductProviderTest"
```

Expected: all `OpenFoodFactsProductProviderTest` tests pass.

- [ ] **Step 8: Commit**

```powershell
git add app/src/main/java/com/musfit/data/remote app/src/main/java/com/musfit/data/repository app/src/main/java/com/musfit/core/di/NetworkModule.kt app/src/main/java/com/musfit/core/di/RepositoryModule.kt app/src/test/java/com/musfit/data/remote
git commit -m "feat: add Open Food Facts lookup"
```

### Task 5: Health Connect Boundary And Mapping

**Files:**
- Create: `app/src/main/java/com/musfit/domain/health/HealthConnectStatus.kt`
- Create: `app/src/main/java/com/musfit/integrations/healthconnect/HealthConnectGateway.kt`
- Create: `app/src/main/java/com/musfit/integrations/healthconnect/HealthConnectRecordMapper.kt`
- Create: `app/src/main/java/com/musfit/integrations/healthconnect/HealthConnectManager.kt`
- Create: `app/src/main/java/com/musfit/integrations/healthconnect/HealthPermissionsRationaleActivity.kt`
- Create: `app/src/test/java/com/musfit/integrations/healthconnect/HealthConnectRecordMapperTest.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `WorkoutSessionEntity` and `WorkoutSetEntity` from Task 3.
- Produces: `HealthConnectGateway` with availability, permission, read summary, and export workout functions.
- Produces: `HealthConnectRecordMapper.toExerciseSessionRecord(session, sets): ExerciseSessionRecord`.

- [ ] **Step 1: Write failing mapper test**

Create `app/src/test/java/com/musfit/integrations/healthconnect/HealthConnectRecordMapperTest.kt`:

```kotlin
package com.musfit.integrations.healthconnect

import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthConnectRecordMapperTest {
    @Test
    fun toExerciseSessionRecord_mapsStrengthWorkoutTimeRange() {
        val session = WorkoutSessionEntity(
            id = "session-1",
            routineId = null,
            startedAtEpochMillis = 1_700_000_000_000,
            endedAtEpochMillis = 1_700_003_600_000,
            notes = "Push day",
            healthConnectRecordId = null,
            healthConnectLastExportedAtEpochMillis = null,
        )
        val sets = listOf(
            WorkoutSetEntity(
                id = "set-1",
                sessionId = "session-1",
                exerciseId = "bench",
                sortOrder = 0,
                reps = 5,
                weightKg = 100.0,
                durationSeconds = null,
                distanceMeters = null,
                rpe = 8.0,
                notes = null,
                completed = true,
            ),
        )

        val record = HealthConnectRecordMapper.toExerciseSessionRecord(session, sets)

        assertEquals(session.startedAtEpochMillis, record.startTime.toEpochMilli())
        assertEquals(session.endedAtEpochMillis, record.endTime.toEpochMilli())
        assertEquals("MusFit workout: 1 completed sets", record.title)
    }
}
```

- [ ] **Step 2: Run test and verify failure**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "*HealthConnectRecordMapperTest"
```

Expected: test compilation fails because mapper classes do not exist.

- [ ] **Step 3: Implement status model and gateway interface**

Create `app/src/main/java/com/musfit/domain/health/HealthConnectStatus.kt`:

```kotlin
package com.musfit.domain.health

data class HealthConnectStatus(
    val availability: HealthConnectAvailability,
    val grantedPermissions: Set<String>,
)

enum class HealthConnectAvailability {
    Available,
    NotInstalled,
    NotSupported,
}

data class ImportedDailyHealthSummary(
    val steps: Long?,
    val activeCaloriesKcal: Double?,
    val latestWeightKg: Double?,
    val restingHeartRateBpm: Long?,
)
```

Create `app/src/main/java/com/musfit/integrations/healthconnect/HealthConnectGateway.kt`:

```kotlin
package com.musfit.integrations.healthconnect

import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import com.musfit.domain.health.HealthConnectStatus
import com.musfit.domain.health.ImportedDailyHealthSummary
import java.time.LocalDate

interface HealthConnectGateway {
    suspend fun status(): HealthConnectStatus
    suspend fun requestablePermissions(): Set<String>
    suspend fun readDailySummary(date: LocalDate): ImportedDailyHealthSummary
    suspend fun exportWorkout(session: WorkoutSessionEntity, sets: List<WorkoutSetEntity>): String?
}
```

- [ ] **Step 4: Implement Health Connect mapper**

Create `app/src/main/java/com/musfit/integrations/healthconnect/HealthConnectRecordMapper.kt`:

```kotlin
package com.musfit.integrations.healthconnect

import androidx.health.connect.client.records.ExerciseSessionRecord
import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import java.time.Instant
import java.time.ZoneOffset

object HealthConnectRecordMapper {
    fun toExerciseSessionRecord(
        session: WorkoutSessionEntity,
        sets: List<WorkoutSetEntity>,
    ): ExerciseSessionRecord {
        val endMillis = session.endedAtEpochMillis ?: session.startedAtEpochMillis
        val completedSetCount = sets.count { it.completed }
        return ExerciseSessionRecord(
            startTime = Instant.ofEpochMilli(session.startedAtEpochMillis),
            startZoneOffset = ZoneOffset.UTC,
            endTime = Instant.ofEpochMilli(endMillis),
            endZoneOffset = ZoneOffset.UTC,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
            title = "MusFit workout: $completedSetCount completed sets",
            notes = session.notes,
        )
    }
}
```

- [ ] **Step 5: Implement gateway manager**

Create `app/src/main/java/com/musfit/integrations/healthconnect/HealthConnectManager.kt`:

```kotlin
package com.musfit.integrations.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.domain.health.HealthConnectStatus
import com.musfit.domain.health.ImportedDailyHealthSummary
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context,
) : HealthConnectGateway {
    private val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
    )

    override suspend fun status(): HealthConnectStatus {
        val availability = when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.Available
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.NotInstalled
            else -> HealthConnectAvailability.NotSupported
        }
        val granted = if (availability == HealthConnectAvailability.Available) {
            HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions()
        } else {
            emptySet()
        }
        return HealthConnectStatus(availability = availability, grantedPermissions = granted)
    }

    override suspend fun requestablePermissions(): Set<String> = permissions

    override suspend fun readDailySummary(date: LocalDate): ImportedDailyHealthSummary {
        val dayStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val dayEnd = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        val range = TimeRangeFilter.between(dayStart, dayEnd)
        val client = HealthConnectClient.getOrCreate(context)

        val steps = runCatching {
            client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = range,
                ),
            )[StepsRecord.COUNT_TOTAL]
        }.getOrNull()

        val activeCalories = runCatching {
            client.aggregate(
                AggregateRequest(
                    metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                    timeRangeFilter = range,
                ),
            )[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories
        }.getOrNull()

        val latestWeight = runCatching {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = range,
                ),
            ).records.maxByOrNull { it.time }?.weight?.inKilograms
        }.getOrNull()

        val lowestHeartRate = runCatching {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = range,
                ),
            ).records
                .flatMap { it.samples }
                .minOfOrNull { it.beatsPerMinute }
        }.getOrNull()

        return ImportedDailyHealthSummary(
            steps = steps,
            activeCaloriesKcal = activeCalories,
            latestWeightKg = latestWeight,
            restingHeartRateBpm = lowestHeartRate,
        )
    }

    override suspend fun exportWorkout(
        session: WorkoutSessionEntity,
        sets: List<WorkoutSetEntity>,
    ): String? {
        val client = HealthConnectClient.getOrCreate(context)
        val response = client.insertRecords(
            listOf(HealthConnectRecordMapper.toExerciseSessionRecord(session, sets)),
        )
        return response.recordIdsList.firstOrNull()
    }
}
```

Create `app/src/main/java/com/musfit/integrations/healthconnect/HealthPermissionsRationaleActivity.kt`:

```kotlin
package com.musfit.integrations.healthconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class HealthPermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(text = "MusFit Health Connect access")
                    Text(text = "MusFit reads steps, active calories, body weight, and heart rate when you grant access. MusFit writes workouts you log in the app. Data stays on this device in the MVP.")
                }
            }
        }
    }
}
```

- [ ] **Step 6: Register Health Connect rationale activity**

Modify `app/src/main/AndroidManifest.xml` inside `<application>`:

```xml
<activity
    android:name=".integrations.healthconnect.HealthPermissionsRationaleActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE" />
    </intent-filter>
</activity>

<activity-alias
    android:name="ViewPermissionUsageActivity"
    android:exported="true"
    android:permission="android.permission.START_VIEW_PERMISSION_USAGE"
    android:targetActivity=".integrations.healthconnect.HealthPermissionsRationaleActivity">
    <intent-filter>
        <action android:name="android.intent.action.VIEW_PERMISSION_USAGE" />
        <category android:name="android.intent.category.HEALTH_PERMISSIONS" />
    </intent-filter>
</activity-alias>
```

- [ ] **Step 7: Bind gateway**

Create `app/src/main/java/com/musfit/core/di/HealthModule.kt`:

```kotlin
package com.musfit.core.di

import com.musfit.integrations.healthconnect.HealthConnectGateway
import com.musfit.integrations.healthconnect.HealthConnectManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HealthModule {
    @Binds
    @Singleton
    abstract fun bindHealthConnectGateway(manager: HealthConnectManager): HealthConnectGateway
}
```

- [ ] **Step 8: Run tests and build**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "*HealthConnectRecordMapperTest"
.\gradlew.bat assembleDebug
```

Expected: mapper test passes and debug build succeeds.

- [ ] **Step 9: Commit**

```powershell
git add app/src/main/AndroidManifest.xml app/src/main/java/com/musfit/domain/health app/src/main/java/com/musfit/integrations app/src/main/java/com/musfit/core/di/HealthModule.kt app/src/test/java/com/musfit/integrations
git commit -m "feat: add Health Connect integration boundary"
```

### Task 6: Compose Navigation Shell And Theme

**Files:**
- Create: `app/src/main/java/com/musfit/ui/theme/Color.kt`
- Create: `app/src/main/java/com/musfit/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/musfit/ui/AppDestination.kt`
- Create: `app/src/main/java/com/musfit/ui/AppNavGraph.kt`
- Create: `app/src/main/java/com/musfit/ui/today/TodayScreen.kt`
- Create: `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`
- Create: `app/src/main/java/com/musfit/ui/training/TrainingScreen.kt`
- Create: `app/src/main/java/com/musfit/ui/health/HealthScreen.kt`
- Modify: `app/src/main/java/com/musfit/MainActivity.kt`

**Interfaces:**
- Produces: four bottom tabs named Today, Food, Training, Health.
- Produces: starter screens that receive real state in subsequent tasks.

- [ ] **Step 1: Create theme files**

Create `app/src/main/java/com/musfit/ui/theme/Color.kt`:

```kotlin
package com.musfit.ui.theme

import androidx.compose.ui.graphics.Color

val MusFitGreen = Color(0xFF2E7D5B)
val MusFitBlue = Color(0xFF2563EB)
val MusFitInk = Color(0xFF1F2937)
val MusFitSurface = Color(0xFFF7F8FA)
```

Create `app/src/main/java/com/musfit/ui/theme/Theme.kt`:

```kotlin
package com.musfit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = MusFitGreen,
    secondary = MusFitBlue,
    surface = MusFitSurface,
    onSurface = MusFitInk,
)

@Composable
fun MusFitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
```

- [ ] **Step 2: Create destinations and tab graph**

Create `app/src/main/java/com/musfit/ui/AppDestination.kt`:

```kotlin
package com.musfit.ui

enum class AppDestination(val route: String, val label: String) {
    Today(route = "today", label = "Today"),
    Food(route = "food", label = "Food"),
    Training(route = "training", label = "Training"),
    Health(route = "health", label = "Health"),
}
```

Create `app/src/main/java/com/musfit/ui/AppNavGraph.kt`:

```kotlin
package com.musfit.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.musfit.ui.food.FoodScreen
import com.musfit.ui.health.HealthScreen
import com.musfit.ui.today.TodayScreen
import com.musfit.ui.training.TrainingScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val destinations = AppDestination.entries
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppDestination.Today.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(AppDestination.Today.route)
                                launchSingleTop = true
                            }
                        },
                        label = { Text(destination.label) },
                        icon = { Text(destination.label.first().toString()) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Today.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(AppDestination.Today.route) { TodayScreen() }
            composable(AppDestination.Food.route) { FoodScreen() }
            composable(AppDestination.Training.route) { TrainingScreen() }
            composable(AppDestination.Health.route) { HealthScreen() }
        }
    }
}
```

- [ ] **Step 3: Create starter screens**

Create `app/src/main/java/com/musfit/ui/today/TodayScreen.kt`:

```kotlin
package com.musfit.ui.today

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TodayScreen() {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(text = "Today")
        Text(text = "Calories, macros, meals, workouts, and Health Connect metrics appear here.")
    }
}
```

Create equivalent starter screen files:

```kotlin
package com.musfit.ui.food

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FoodScreen() {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(text = "Food")
        Text(text = "Manual food entry, saved foods, meals, and barcode lookup appear here.")
    }
}
```

```kotlin
package com.musfit.ui.training

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TrainingScreen() {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(text = "Training")
        Text(text = "Exercises, routines, active workouts, sets, history, and records appear here.")
    }
}
```

```kotlin
package com.musfit.ui.health

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HealthScreen() {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(text = "Health")
        Text(text = "Health Connect availability, permissions, sync, and imported metrics appear here.")
    }
}
```

- [ ] **Step 4: Wire MainActivity**

Replace `app/src/main/java/com/musfit/MainActivity.kt` content:

```kotlin
package com.musfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.musfit.ui.AppNavGraph
import com.musfit.ui.theme.MusFitTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MusFitTheme {
                AppNavGraph()
            }
        }
    }
}
```

- [ ] **Step 5: Run build**

Run:

```powershell
.\gradlew.bat assembleDebug
```

Expected: debug build succeeds.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/musfit/MainActivity.kt app/src/main/java/com/musfit/ui
git commit -m "feat: add Compose navigation shell"
```

### Task 7: Food Logging UI And ViewModel

**Files:**
- Create: `app/src/main/java/com/musfit/ui/food/FoodViewModel.kt`
- Modify: `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`
- Create: `app/src/test/java/com/musfit/ui/food/FoodViewModelTest.kt`

**Interfaces:**
- Consumes: `FoodProductProvider.lookupBarcode`.
- Produces: Food tab that can search barcode by typed code, display editable result fields, and save the confirmed food locally.

- [ ] **Step 1: Write ViewModel test for editable barcode result**

Create `app/src/test/java/com/musfit/ui/food/FoodViewModelTest.kt`:

```kotlin
package com.musfit.ui.food

import com.musfit.data.remote.food.FoodProductProvider
import com.musfit.data.remote.food.ProductDataQuality
import com.musfit.data.remote.food.ProductLookupResult
import com.musfit.data.repository.FoodRepository
import com.musfit.domain.model.FoodNutrition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FoodViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun lookupBarcode_populatesEditableResult() = runTest {
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = FakeFoodRepository(),
        )

        assertEquals("", viewModel.state.value.barcode)
        viewModel.onBarcodeChanged("123")
        viewModel.lookupBarcode()
        dispatcher.scheduler.advanceUntilIdle()
        val result = viewModel.state.value
        assertEquals("Greek Yogurt", result.productName)
        assertEquals("Example Dairy", result.brand)
        assertEquals(59.0, result.caloriesPer100g, 0.01)
        viewModel.onProductNameChanged("Edited Yogurt")
        viewModel.saveProduct()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Saved food", viewModel.state.value.message)
    }

    private class FakeProductProvider : FoodProductProvider {
        override suspend fun lookupBarcode(barcode: String): ProductLookupResult =
            ProductLookupResult.Found(
                barcode = barcode,
                name = "Greek Yogurt",
                brand = "Example Dairy",
                servingQuantityGrams = 170.0,
                nutritionPer100g = FoodNutrition(59.0, 10.0, 3.6, 0.4),
                quality = ProductDataQuality.Complete,
                rawJson = "{}",
            )
    }

    private class FakeFoodRepository : FoodRepository {
        override suspend fun saveConfirmedProduct(
            result: ProductLookupResult.Found,
            editedName: String,
            editedBrand: String?,
            editedNutrition: FoodNutrition,
        ): String = "food-1"
    }
}
```

- [ ] **Step 2: Run test and verify failure**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "*FoodViewModelTest"
```

Expected: test compilation fails because `FoodViewModel` does not exist.

- [ ] **Step 3: Implement FoodViewModel**

Create `app/src/main/java/com/musfit/ui/food/FoodViewModel.kt`:

```kotlin
package com.musfit.ui.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.data.remote.food.FoodProductProvider
import com.musfit.data.remote.food.ProductLookupResult
import com.musfit.data.repository.FoodRepository
import com.musfit.domain.model.FoodNutrition
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FoodUiState(
    val barcode: String = "",
    val isLoading: Boolean = false,
    val message: String? = null,
    val productName: String = "",
    val brand: String = "",
    val caloriesPer100g: Double = 0.0,
    val proteinPer100g: Double = 0.0,
    val carbsPer100g: Double = 0.0,
    val fatPer100g: Double = 0.0,
    val lookupResult: ProductLookupResult.Found? = null,
)

@HiltViewModel
class FoodViewModel @Inject constructor(
    private val provider: FoodProductProvider,
    private val repository: FoodRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(FoodUiState())
    val state: StateFlow<FoodUiState> = mutableState

    fun onBarcodeChanged(value: String) {
        mutableState.update { it.copy(barcode = value.filter(Char::isDigit)) }
    }

    fun onProductNameChanged(value: String) {
        mutableState.update { it.copy(productName = value) }
    }

    fun onBrandChanged(value: String) {
        mutableState.update { it.copy(brand = value) }
    }

    fun onCaloriesChanged(value: String) {
        mutableState.update { it.copy(caloriesPer100g = value.toDoubleOrNull() ?: 0.0) }
    }

    fun onProteinChanged(value: String) {
        mutableState.update { it.copy(proteinPer100g = value.toDoubleOrNull() ?: 0.0) }
    }

    fun onCarbsChanged(value: String) {
        mutableState.update { it.copy(carbsPer100g = value.toDoubleOrNull() ?: 0.0) }
    }

    fun onFatChanged(value: String) {
        mutableState.update { it.copy(fatPer100g = value.toDoubleOrNull() ?: 0.0) }
    }

    fun lookupBarcode() {
        val barcode = state.value.barcode
        if (barcode.isBlank()) {
            mutableState.update { it.copy(message = "Enter a barcode") }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(isLoading = true, message = null) }
            when (val result = provider.lookupBarcode(barcode)) {
                is ProductLookupResult.Found -> mutableState.update {
                    it.copy(
                        isLoading = false,
                        productName = result.name,
                        brand = result.brand.orEmpty(),
                        caloriesPer100g = result.nutritionPer100g.caloriesKcal,
                        proteinPer100g = result.nutritionPer100g.proteinGrams,
                        carbsPer100g = result.nutritionPer100g.carbsGrams,
                        fatPer100g = result.nutritionPer100g.fatGrams,
                        lookupResult = result,
                        message = if (result.quality.name == "Incomplete") "Review missing nutrition values" else null,
                    )
                }
                is ProductLookupResult.NotFound -> mutableState.update { it.copy(isLoading = false, message = "Product not found") }
                is ProductLookupResult.Failed -> mutableState.update { it.copy(isLoading = false, message = result.message) }
            }
        }
    }

    fun saveProduct() {
        val current = state.value
        val result = current.lookupResult
        if (result == null) {
            mutableState.update { it.copy(message = "Look up a product before saving") }
            return
        }
        viewModelScope.launch {
            repository.saveConfirmedProduct(
                result = result,
                editedName = current.productName,
                editedBrand = current.brand.ifBlank { null },
                editedNutrition = FoodNutrition(
                    caloriesKcal = current.caloriesPer100g,
                    proteinGrams = current.proteinPer100g,
                    carbsGrams = current.carbsPer100g,
                    fatGrams = current.fatPer100g,
                ),
            )
            mutableState.update { it.copy(message = "Saved food", lookupResult = null) }
        }
    }
}
```

- [ ] **Step 4: Implement Food screen**

Replace `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`:

```kotlin
package com.musfit.ui.food

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun FoodScreen(viewModel: FoodViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Food")
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = state.barcode,
                onValueChange = viewModel::onBarcodeChanged,
                label = { Text("Barcode") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            Button(onClick = viewModel::lookupBarcode, enabled = !state.isLoading) {
                Text(if (state.isLoading) "Looking up" else "Lookup")
            }
        }
        if (state.message != null) {
            Text(text = state.message)
        }
        OutlinedTextField(
            value = state.productName,
            onValueChange = viewModel::onProductNameChanged,
            label = { Text("Product") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.brand,
            onValueChange = viewModel::onBrandChanged,
            label = { Text("Brand") },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(text = "Per 100g")
        OutlinedTextField(
            value = state.caloriesPer100g.toString(),
            onValueChange = viewModel::onCaloriesChanged,
            label = { Text("Calories") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        OutlinedTextField(
            value = state.proteinPer100g.toString(),
            onValueChange = viewModel::onProteinChanged,
            label = { Text("Protein g") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        OutlinedTextField(
            value = state.carbsPer100g.toString(),
            onValueChange = viewModel::onCarbsChanged,
            label = { Text("Carbs g") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        OutlinedTextField(
            value = state.fatPer100g.toString(),
            onValueChange = viewModel::onFatChanged,
            label = { Text("Fat g") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        Button(onClick = viewModel::saveProduct, enabled = state.lookupResult != null) {
            Text(text = "Save food")
        }
    }
}
```

- [ ] **Step 5: Run tests and build**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "*FoodViewModelTest"
.\gradlew.bat assembleDebug
```

Expected: ViewModel test passes and debug build succeeds.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/musfit/ui/food app/src/test/java/com/musfit/ui/food
git commit -m "feat: add barcode lookup food screen"
```

### Task 8: Training UI And Session State

**Files:**
- Create: `app/src/main/java/com/musfit/ui/training/TrainingViewModel.kt`
- Modify: `app/src/main/java/com/musfit/ui/training/TrainingScreen.kt`
- Create: `app/src/test/java/com/musfit/ui/training/TrainingViewModelTest.kt`

**Interfaces:**
- Consumes: `WorkoutCalculator`.
- Produces: Training tab with an in-memory active workout, set entry fields, volume, and personal record preview.

- [ ] **Step 1: Write ViewModel test**

Create `app/src/test/java/com/musfit/ui/training/TrainingViewModelTest.kt`:

```kotlin
package com.musfit.ui.training

import org.junit.Assert.assertEquals
import org.junit.Test

class TrainingViewModelTest {
    @Test
    fun addCompletedSet_updatesVolume() {
        val viewModel = TrainingViewModel()

        viewModel.onExerciseChanged("Bench Press")
        viewModel.onRepsChanged("5")
        viewModel.onWeightChanged("100")
        viewModel.addSet()

        val state = viewModel.state.value
        assertEquals(1, state.sets.size)
        assertEquals(500.0, state.totalVolumeKg, 0.01)
    }
}
```

- [ ] **Step 2: Run test and verify failure**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "*TrainingViewModelTest"
```

Expected: test compilation fails because `TrainingViewModel` does not exist.

- [ ] **Step 3: Implement TrainingViewModel**

Create `app/src/main/java/com/musfit/ui/training/TrainingViewModel.kt`:

```kotlin
package com.musfit.ui.training

import androidx.lifecycle.ViewModel
import com.musfit.domain.model.WorkoutSetInput
import com.musfit.domain.training.WorkoutCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class TrainingUiState(
    val exerciseName: String = "",
    val reps: String = "",
    val weightKg: String = "",
    val sets: List<WorkoutSetInput> = emptyList(),
    val totalVolumeKg: Double = 0.0,
    val bestEstimatedOneRepMaxKg: Double = 0.0,
)

class TrainingViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(TrainingUiState())
    val state: StateFlow<TrainingUiState> = mutableState

    fun onExerciseChanged(value: String) {
        mutableState.update { it.copy(exerciseName = value) }
    }

    fun onRepsChanged(value: String) {
        mutableState.update { it.copy(reps = value.filter(Char::isDigit)) }
    }

    fun onWeightChanged(value: String) {
        mutableState.update { it.copy(weightKg = value.filter { char -> char.isDigit() || char == '.' }) }
    }

    fun addSet() {
        val current = state.value
        val reps = current.reps.toIntOrNull() ?: return
        val weight = current.weightKg.toDoubleOrNull() ?: return
        val nextSets = current.sets + WorkoutSetInput(
            exerciseId = current.exerciseName.ifBlank { "custom" },
            reps = reps,
            weightKg = weight,
            completed = true,
        )
        val records = WorkoutCalculator.personalRecords(nextSets)
        mutableState.update {
            it.copy(
                sets = nextSets,
                totalVolumeKg = records.totalVolumeKg,
                bestEstimatedOneRepMaxKg = records.bestEstimatedOneRepMaxKg,
                reps = "",
                weightKg = "",
            )
        }
    }
}
```

- [ ] **Step 4: Implement Training screen**

Replace `app/src/main/java/com/musfit/ui/training/TrainingScreen.kt`:

```kotlin
package com.musfit.ui.training

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun TrainingScreen(viewModel: TrainingViewModel = remember { TrainingViewModel() }) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Training")
        OutlinedTextField(
            value = state.exerciseName,
            onValueChange = viewModel::onExerciseChanged,
            label = { Text("Exercise") },
            modifier = Modifier.fillMaxWidth(),
        )
        Row {
            OutlinedTextField(
                value = state.reps,
                onValueChange = viewModel::onRepsChanged,
                label = { Text("Reps") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = state.weightKg,
                onValueChange = viewModel::onWeightChanged,
                label = { Text("kg") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }
        Button(onClick = viewModel::addSet) {
            Text(text = "Add set")
        }
        Text(text = "Total volume: ${state.totalVolumeKg} kg")
        Text(text = "Best est. 1RM: ${state.bestEstimatedOneRepMaxKg} kg")
        state.sets.forEachIndexed { index, set ->
            Text(text = "${index + 1}. ${set.exerciseId}: ${set.reps} x ${set.weightKg} kg")
        }
    }
}
```

- [ ] **Step 5: Run tests and build**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "*TrainingViewModelTest"
.\gradlew.bat assembleDebug
```

Expected: Training ViewModel test passes and debug build succeeds.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/musfit/ui/training app/src/test/java/com/musfit/ui/training
git commit -m "feat: add training session screen"
```

### Task 9: Health Connect UI And Permission Trigger

**Files:**
- Create: `app/src/main/java/com/musfit/ui/health/HealthViewModel.kt`
- Modify: `app/src/main/java/com/musfit/ui/health/HealthScreen.kt`
- Create: `app/src/test/java/com/musfit/ui/health/HealthViewModelTest.kt`

**Interfaces:**
- Consumes: `HealthConnectGateway.status()`.
- Produces: Health tab status display and explicit permission/sync affordance.

- [ ] **Step 1: Write ViewModel test**

Create `app/src/test/java/com/musfit/ui/health/HealthViewModelTest.kt`:

```kotlin
package com.musfit.ui.health

import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.domain.health.HealthConnectStatus
import com.musfit.domain.health.ImportedDailyHealthSummary
import com.musfit.integrations.healthconnect.HealthConnectGateway
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HealthViewModelTest {
    @Test
    fun refreshStatus_showsAvailableWhenGatewayAvailable() = runTest {
        val viewModel = HealthViewModel(FakeHealthConnectGateway())

        viewModel.refreshStatus()

        assertEquals("Available", viewModel.state.value.availabilityLabel)
    }

    private class FakeHealthConnectGateway : HealthConnectGateway {
        override suspend fun status() = HealthConnectStatus(
            availability = HealthConnectAvailability.Available,
            grantedPermissions = setOf("steps"),
        )

        override suspend fun requestablePermissions(): Set<String> = setOf("steps")

        override suspend fun readDailySummary(date: LocalDate) = ImportedDailyHealthSummary(
            steps = 1200,
            activeCaloriesKcal = 100.0,
            latestWeightKg = null,
            restingHeartRateBpm = null,
        )

        override suspend fun exportWorkout(
            session: com.musfit.data.local.entity.WorkoutSessionEntity,
            sets: List<com.musfit.data.local.entity.WorkoutSetEntity>,
        ): String? = "record-id"
    }
}
```

- [ ] **Step 2: Run test and verify failure**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "*HealthViewModelTest"
```

Expected: test compilation fails because `HealthViewModel` does not exist.

- [ ] **Step 3: Implement HealthViewModel**

Create `app/src/main/java/com/musfit/ui/health/HealthViewModel.kt`:

```kotlin
package com.musfit.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.integrations.healthconnect.HealthConnectGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HealthUiState(
    val availabilityLabel: String = "Unknown",
    val grantedPermissionCount: Int = 0,
    val message: String = "Health Connect sync is off until you grant permissions.",
)

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val gateway: HealthConnectGateway,
) : ViewModel() {
    private val mutableState = MutableStateFlow(HealthUiState())
    val state: StateFlow<HealthUiState> = mutableState

    fun refreshStatus() {
        viewModelScope.launch {
            val status = gateway.status()
            mutableState.update {
                it.copy(
                    availabilityLabel = when (status.availability) {
                        HealthConnectAvailability.Available -> "Available"
                        HealthConnectAvailability.NotInstalled -> "Install or update required"
                        HealthConnectAvailability.NotSupported -> "Not supported"
                    },
                    grantedPermissionCount = status.grantedPermissions.size,
                )
            }
        }
    }
}
```

- [ ] **Step 4: Implement Health screen**

Replace `app/src/main/java/com/musfit/ui/health/HealthScreen.kt`:

```kotlin
package com.musfit.ui.health

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun HealthScreen(viewModel: HealthViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshStatus()
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Health")
        Text(text = "Health Connect: ${state.availabilityLabel}")
        Text(text = "Granted permissions: ${state.grantedPermissionCount}")
        Text(text = "Reads: steps, body weight, active calories, heart rate when granted.")
        Text(text = "Writes: workouts logged in MusFit.")
        Text(text = state.message)
        Button(onClick = viewModel::refreshStatus) {
            Text(text = "Refresh status")
        }
    }
}
```

- [ ] **Step 5: Run tests and build**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "*HealthViewModelTest"
.\gradlew.bat assembleDebug
```

Expected: Health ViewModel test passes and debug build succeeds.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/musfit/ui/health app/src/test/java/com/musfit/ui/health
git commit -m "feat: add Health Connect status screen"
```

### Task 10: Today Dashboard State

**Files:**
- Create: `app/src/main/java/com/musfit/ui/today/TodayViewModel.kt`
- Modify: `app/src/main/java/com/musfit/ui/today/TodayScreen.kt`
- Create: `app/src/test/java/com/musfit/ui/today/TodayViewModelTest.kt`

**Interfaces:**
- Consumes: nutrition and workout calculation outputs.
- Produces: Today tab showing calories, macros, workouts, steps, body weight, and active calories.

- [ ] **Step 1: Write ViewModel test**

Create `app/src/test/java/com/musfit/ui/today/TodayViewModelTest.kt`:

```kotlin
package com.musfit.ui.today

import org.junit.Assert.assertEquals
import org.junit.Test

class TodayViewModelTest {
    @Test
    fun defaultState_isEmptyButReadable() {
        val viewModel = TodayViewModel()

        val state = viewModel.state.value

        assertEquals(0.0, state.caloriesKcal, 0.01)
        assertEquals("No workout logged today", state.trainingSummary)
    }
}
```

- [ ] **Step 2: Run test and verify failure**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "*TodayViewModelTest"
```

Expected: test compilation fails because `TodayViewModel` does not exist.

- [ ] **Step 3: Implement TodayViewModel**

Create `app/src/main/java/com/musfit/ui/today/TodayViewModel.kt`:

```kotlin
package com.musfit.ui.today

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class TodayUiState(
    val caloriesKcal: Double = 0.0,
    val proteinGrams: Double = 0.0,
    val carbsGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
    val steps: Long? = null,
    val activeCaloriesKcal: Double? = null,
    val bodyWeightKg: Double? = null,
    val trainingSummary: String = "No workout logged today",
)

class TodayViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(TodayUiState())
    val state: StateFlow<TodayUiState> = mutableState
}
```

- [ ] **Step 4: Implement Today screen**

Replace `app/src/main/java/com/musfit/ui/today/TodayScreen.kt`:

```kotlin
package com.musfit.ui.today

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TodayScreen(viewModel: TodayViewModel = remember { TodayViewModel() }) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Today")
        Text(text = "Calories: ${state.caloriesKcal}")
        Text(text = "Protein: ${state.proteinGrams}g")
        Text(text = "Carbs: ${state.carbsGrams}g")
        Text(text = "Fat: ${state.fatGrams}g")
        Text(text = "Steps: ${state.steps ?: "Not synced"}")
        Text(text = "Active calories: ${state.activeCaloriesKcal ?: "Not synced"}")
        Text(text = "Body weight: ${state.bodyWeightKg ?: "Not synced"}")
        Text(text = state.trainingSummary)
    }
}
```

- [ ] **Step 5: Run tests and build**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "*TodayViewModelTest"
.\gradlew.bat assembleDebug
```

Expected: Today ViewModel test passes and debug build succeeds.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/musfit/ui/today app/src/test/java/com/musfit/ui/today
git commit -m "feat: add Today dashboard state"
```

### Task 11: Barcode Scanner Screen

**Files:**
- Create: `app/src/main/java/com/musfit/ui/food/BarcodeScannerScreen.kt`
- Modify: `app/src/main/java/com/musfit/ui/AppNavGraph.kt`
- Modify: `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`

**Interfaces:**
- Consumes: Camera permission and ML Kit Barcode Scanning.
- Produces: scanner route that returns the first detected barcode to the Food screen as a typed barcode value.

- [ ] **Step 1: Create scanner composable**

Create `app/src/main/java/com/musfit/ui/food/BarcodeScannerScreen.kt`:

```kotlin
package com.musfit.ui.food

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

@OptIn(ExperimentalGetImage::class)
@Composable
fun BarcodeScannerScreen(
    onBarcodeDetected: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted },
    )
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                )
                .build(),
        )
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(scanner) {
        onDispose {
            scanner.close()
        }
    }

    if (!hasCameraPermission) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(text = "Camera permission is required to scan barcodes.")
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                Text(text = "Grant camera access")
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PreviewView(context)
            },
            update = { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener(
                    {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also { preview ->
                            preview.surfaceProvider = previewView.surfaceProvider
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage == null) {
                                imageProxy.close()
                                return@setAnalyzer
                            }

                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    val barcode = barcodes.firstOrNull()?.rawValue
                                    if (!barcode.isNullOrBlank()) {
                                        onBarcodeDetected(barcode)
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        }

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis,
                        )
                    },
                    ContextCompat.getMainExecutor(context),
                )
            },
        )
        Text(text = "Point camera at barcode")
    }
}
```

- [ ] **Step 2: Add scanner route**

Modify `app/src/main/java/com/musfit/ui/AppNavGraph.kt` by adding a saved scanned barcode state near `val currentRoute`:

```kotlin
var scannedBarcode by rememberSaveable { mutableStateOf<String?>(null) }
```

Add these imports:

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.musfit.ui.food.BarcodeScannerScreen
```

Replace the Food destination with:

```kotlin
composable(AppDestination.Food.route) {
    FoodScreen(
        scannedBarcode = scannedBarcode,
        onScanClick = { navController.navigate("barcode-scanner") },
    )
}
```

Add this scanner destination:

```kotlin
composable("barcode-scanner") {
    BarcodeScannerScreen(
        onBarcodeDetected = { barcode ->
            if (barcode.isNotBlank()) {
                scannedBarcode = barcode
                navController.popBackStack()
            }
        },
    )
}
```

- [ ] **Step 3: Add scan button to Food screen**

In `FoodScreen`, add this parameter:

```kotlin
scannedBarcode: String? = null,
onScanClick: () -> Unit = {},
```

Add this effect after reading state:

```kotlin
LaunchedEffect(scannedBarcode) {
    if (!scannedBarcode.isNullOrBlank()) {
        viewModel.onBarcodeChanged(scannedBarcode)
    }
}
```

Add this button after the lookup button:

```kotlin
Button(onClick = onScanClick) {
    Text("Scan")
}
```

Add this import:

```kotlin
import androidx.compose.runtime.LaunchedEffect
```

- [ ] **Step 4: Run build**

Run:

```powershell
.\gradlew.bat assembleDebug
```

Expected: debug build succeeds. Scanner route requests camera permission, opens CameraX preview, analyzes EAN/UPC frames with ML Kit, and returns the first detected barcode to the Food screen.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/musfit/ui
git commit -m "feat: add barcode scanner route"
```

### Task 12: Final Verification, README, And APK Artifact Check

**Files:**
- Create: `README.md`
- Modify: `.github/workflows/android.yml` only if the local commands reveal a CI path problem.

**Interfaces:**
- Produces: repo instructions for local Android build and GitHub artifact usage.
- Produces: verified debug APK.

- [ ] **Step 1: Write README**

Create `README.md`:

```markdown
# MusFit

MusFit is an Android-only fitness and nutrition tracker inspired by Lifesum and Hevy.

## MVP Scope

- Meal and food tracking
- Barcode product lookup through Open Food Facts
- Structured workout logging
- Health Connect status, permissions, read summary, and workout export boundary
- Local-first Room database
- GitHub Actions debug APK builds

## Local Build

Requirements:

- JDK 17
- Android SDK with API 37
- PowerShell on Windows

Commands:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Privacy

The MVP stores health, meal, and workout data on-device. It has no account system, cloud sync, analytics, subscriptions, social features, or direct wearable cloud API integrations.
```

- [ ] **Step 2: Run full verification**

Run:

```powershell
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug
```

Expected:

```text
BUILD SUCCESSFUL
```

Expected file:

```text
app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **Step 3: Inspect GitHub workflow syntax**

Run:

```powershell
Get-Content -Raw .github/workflows/android.yml
```

Expected: workflow includes checkout, setup-java, `testDebugUnitTest`, `lintDebug`, `assembleDebug`, and `upload-artifact`.

- [ ] **Step 4: Commit**

```powershell
git add README.md .github/workflows/android.yml
git commit -m "docs: add build instructions"
```

## Self-Review Notes

Spec coverage:

- Architecture: Tasks 1, 3, 4, 5, and 6 set up the native Kotlin app, Room, Retrofit, Health Connect, and Compose shell.
- Food tracking and barcode lookup: Tasks 2, 4, 7, and 11 cover nutrition calculations, Open Food Facts lookup, editable UI state, and scanner route.
- Training tracking: Tasks 2 and 8 cover workout volume, personal records, and active workout entry.
- Health Connect: Tasks 5 and 9 cover availability, permissions, daily summary read, workout export mapping, and explicit Health tab status.
- GitHub build: Tasks 1 and 12 cover CI and debug APK artifact.
- Privacy constraints: Tasks 1, 5, 9, and 12 keep data local, disable backup for app data, and document no accounts/cloud/analytics.

Implementation risk:

- Health Connect record APIs can shift across AndroidX releases. If `ExerciseSessionRecord` constructor or constants differ in Health Connect 1.1.0, adjust only `HealthConnectRecordMapper.kt` and rerun `HealthConnectRecordMapperTest`.
- CameraX analyzer code should be tested on a physical Android device because emulator camera barcode recognition can be unreliable.

Reference versions checked on 2026-06-19:

- Android Gradle Plugin 9.2 compatibility: https://developer.android.com/build/releases/agp-9-2-0-release-notes
- Android Studio Panda 4 Patch 1 / AGP 9.2.1: https://androidstudio.googleblog.com/2026/
- AGP 9 built-in Kotlin migration: https://developer.android.com/build/migrate-to-built-in-kotlin
- Compose BOM 2026.04.01: https://android-developers.googleblog.com/2026/04/jetpack-compose-april-2026-updates.html
- Health Connect 1.1.0 stable: https://developer.android.com/jetpack/androidx/releases/health-connect
- Room 2.8.1: https://developer.android.com/jetpack/androidx/releases/room
- Navigation 2.9.8: https://developer.android.com/jetpack/androidx/releases/navigation
- Lifecycle 2.11.0: https://developer.android.com/jetpack/androidx/releases/lifecycle
- CameraX 1.6.1: https://developer.android.com/jetpack/androidx/releases/camera
