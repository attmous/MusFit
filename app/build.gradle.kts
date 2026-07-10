import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.android.legacy.kapt)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
}

fun String.asBuildConfigString(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) {
        file.inputStream().use(::load)
    }
}

fun localConfigValue(name: String, defaultValue: String = ""): String =
    providers.gradleProperty(name)
        .orElse(providers.environmentVariable(name))
        .orElse(localProperties.getProperty(name).orEmpty().ifBlank { defaultValue })
        .get()

// Monotonic build number derived from the git history so every master build
// gets a higher versionCode than the last — required for over-the-air (Obtainium)
// updates to install over the previously distributed build. Falls back to 1 when
// git is unavailable (e.g. a source-only checkout). CI must checkout with full
// history (fetch-depth: 0) or this collapses to 1. See docs/ops/auto-update.md.
fun gitCommitCount(): Int =
    try {
        val process = ProcessBuilder("git", "rev-list", "--count", "HEAD")
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        output.toIntOrNull() ?: 1
    } catch (e: Exception) {
        1
    }

val musfitGoogleWebClientId =
    localConfigValue("MUSFIT_GOOGLE_WEB_CLIENT_ID")

val musfitGitHubOAuthClientId =
    localConfigValue("MUSFIT_GITHUB_OAUTH_CLIENT_ID")

val musfitDebugHermesBaseUrl =
    localConfigValue("MUSFIT_DEBUG_HERMES_BASE_URL", "http://192.168.178.113:8080/v1/")

val musfitDebugHermesModelName =
    localConfigValue("MUSFIT_DEBUG_HERMES_MODEL_NAME", "hermes-agent")

val musfitDebugHermesApiKey =
    localConfigValue("MUSFIT_DEBUG_HERMES_API_KEY")

android {
    namespace = "com.musfit"
    compileSdk = 37

    val buildNumber = gitCommitCount()

    signingConfigs {
        // Reuse the committed MusFit debug keystore (a copy of the standard Android
        // debug key) for EVERY build — local and CI. A stable signature is what lets
        // Obtainium install a CI-built update over a build flashed from this machine
        // without an uninstall. Debug-only key with public credentials; safe to commit.
        getByName("debug") {
            storeFile = file("keystore/musfit.debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    defaultConfig {
        applicationId = "com.musfit"
        minSdk = 28
        targetSdk = 37
        versionCode = buildNumber
        versionName = "0.1.0.$buildNumber"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", musfitGoogleWebClientId.asBuildConfigString())
        buildConfigField("String", "GITHUB_OAUTH_CLIENT_ID", musfitGitHubOAuthClientId.asBuildConfigString())
        buildConfigField("String", "DEBUG_HERMES_BASE_URL", "".asBuildConfigString())
        buildConfigField("String", "DEBUG_HERMES_MODEL_NAME", "".asBuildConfigString())
        buildConfigField("String", "DEBUG_HERMES_API_KEY", "".asBuildConfigString())
    }

    buildTypes {
        debug {
            buildConfigField("String", "DEBUG_HERMES_BASE_URL", musfitDebugHermesBaseUrl.asBuildConfigString())
            buildConfigField("String", "DEBUG_HERMES_MODEL_NAME", musfitDebugHermesModelName.asBuildConfigString())
            buildConfigField("String", "DEBUG_HERMES_API_KEY", musfitDebugHermesApiKey.asBuildConfigString())
        }
    }

    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

kapt {
    correctErrorTypes = true
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
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
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.health.connect)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.mlkit)
    implementation(libs.mlkit.barcode)
    implementation(libs.mlkit.text)
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
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.robolectric)
}
