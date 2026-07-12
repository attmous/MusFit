import com.android.build.api.variant.ApplicationVariantBuilder
import com.android.build.api.variant.DeviceTestBuilder
import com.android.build.api.variant.HostTestBuilder
import com.android.build.api.dsl.ManagedVirtualDevice
import java.io.ByteArrayOutputStream
import java.util.Properties
import javax.inject.Inject
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations

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

// Monotonic build number derived from the git history. Retain the established
// version sequence while release publication is suspended. Falls back to 1 when
// git is unavailable (e.g. a source-only checkout). CI must checkout with full
// history (fetch-depth: 0) or this collapses to 1. See docs/ops/auto-update.md.
//
// This is a ValueSource rather than a configuration-time ProcessBuilder so the
// commit count is a tracked configuration-cache input: Gradle re-runs it on
// every build and invalidates a stale entry when a new commit changes the count,
// instead of freezing the OTA versionCode at a cached value. The configuration
// cache is not enabled yet (see gradle.properties for why); keeping this
// config-cache-safe makes enabling it later a one-line change.
abstract class GitCommitCountValueSource :
    ValueSource<Int, GitCommitCountValueSource.Parameters> {

    interface Parameters : ValueSourceParameters {
        val repositoryRoot: DirectoryProperty
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): Int =
        try {
            val stdout = ByteArrayOutputStream()
            val result = execOperations.exec {
                commandLine("git", "rev-list", "--count", "HEAD")
                workingDir(parameters.repositoryRoot.get().asFile)
                standardOutput = stdout
                errorOutput = ByteArrayOutputStream()
                isIgnoreExitValue = true
            }
            if (result.exitValue == 0) {
                stdout.toString().trim().toIntOrNull() ?: 1
            } else {
                1
            }
        } catch (e: Exception) {
            1
        }
}

val gitCommitCount =
    providers.of(GitCommitCountValueSource::class) {
        parameters {
            repositoryRoot.set(rootDir)
        }
    }

val musfitGoogleWebClientId =
    localConfigValue("MUSFIT_GOOGLE_WEB_CLIENT_ID")

val musfitGitHubOAuthClientId =
    localConfigValue("MUSFIT_GITHUB_OAUTH_CLIENT_ID")

val musfitInternalHermesBaseUrl =
    localConfigValue("MUSFIT_DEBUG_HERMES_BASE_URL", "http://192.168.178.113:8080/v1/")

val musfitInternalHermesModelName =
    localConfigValue("MUSFIT_DEBUG_HERMES_MODEL_NAME", "hermes-agent")

android {
    namespace = "com.musfit"
    compileSdk = 37

    val buildNumber = gitCommitCount.get()

    signingConfigs {
        // Stable public development key for internalDebug only. Never use this
        // signing config for productionRelease or a distribution artifact.
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
        buildConfigField("String", "DATA_TRANSFER_MODE", "full".asBuildConfigString())
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("internal") {
            dimension = "distribution"
            applicationIdSuffix = ".internal"
            versionNameSuffix = "-internal"
            manifestPlaceholders["mainLauncherEnabled"] = true
            manifestPlaceholders["legacyMigrationLauncherEnabled"] = false
            buildConfigField(
                "String",
                "DEBUG_HERMES_BASE_URL",
                musfitInternalHermesBaseUrl.asBuildConfigString(),
            )
            buildConfigField(
                "String",
                "DEBUG_HERMES_MODEL_NAME",
                musfitInternalHermesModelName.asBuildConfigString(),
            )
        }
        create("production") {
            dimension = "distribution"
            applicationId = "com.musfit"
            proguardFiles("proguard-production-reports.pro")
            manifestPlaceholders["mainLauncherEnabled"] = true
            manifestPlaceholders["legacyMigrationLauncherEnabled"] = false
        }
        create("legacyMigration") {
            dimension = "distribution"
            applicationId = "com.musfit"
            versionNameSuffix = "-data-migration"
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles("proguard-legacy-migration-reports.pro")
            buildConfigField("String", "DATA_TRANSFER_MODE", "legacy-export".asBuildConfigString())
            manifestPlaceholders["mainLauncherEnabled"] = false
            manifestPlaceholders["legacyMigrationLauncherEnabled"] = true
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    sourceSets {
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        managedDevices {
            localDevices {
                create("musFitApi28") {
                    device = "Pixel 2"
                    apiLevel = 28
                    systemImageSource = "aosp"
                    testedAbi = "x86"
                }
                create("musFitApi37") {
                    device = "Pixel 2"
                    apiLevel = 37
                    systemImageSource = "google"
                    pageAlignment = ManagedVirtualDevice.PageAlignment.FORCE_16KB_PAGES
                    testedAbi = "x86_64"
                }
            }
            groups {
                create("migrationApi28And37") {
                    targetDevices.add(localDevices["musFitApi28"])
                    targetDevices.add(localDevices["musFitApi37"])
                }
            }
        }
    }

}

val enabledApplicationVariants = mutableSetOf<String>()

androidComponents {
    beforeVariants(selector().all()) { variantBuilder ->
        val distribution = variantBuilder.productFlavors
            .firstOrNull { (dimension, _) -> dimension == "distribution" }
            ?.second
        val enabled =
            (distribution == "internal" && variantBuilder.buildType == "debug") ||
            (distribution == "production" && variantBuilder.buildType == "release") ||
            (distribution == "legacyMigration" && variantBuilder.buildType == "release")
        variantBuilder.enable = enabled
        if (enabled) {
            val applicationVariantBuilder = variantBuilder as ApplicationVariantBuilder
            applicationVariantBuilder.hostTests
                .getValue(HostTestBuilder.UNIT_TEST_TYPE)
                .enable = true
            applicationVariantBuilder.deviceTests
                .getValue(DeviceTestBuilder.ANDROID_TEST_TYPE)
                .enable = distribution == "internal"
        }
    }
    onVariants(selector().all()) { variant ->
        enabledApplicationVariants += variant.name
    }
}

tasks.register("verifyReleaseVariantMatrix") {
    group = "verification"
    description = "Verifies the only enabled installable variants and their canonical task families."
    doLast {
        val expectedVariants = setOf("internalDebug", "productionRelease", "legacyMigrationRelease")
        check(enabledApplicationVariants == expectedVariants) {
            "Enabled application variants must be $expectedVariants, got $enabledApplicationVariants"
        }

        val requiredTasks = setOf(
            "assembleInternalDebug",
            "assembleInternalDebugAndroidTest",
            "testInternalDebugUnitTest",
            "lintInternalDebug",
            "assembleProductionRelease",
            "bundleProductionRelease",
            "testProductionReleaseUnitTest",
            "lintProductionRelease",
            "assembleLegacyMigrationRelease",
            "testLegacyMigrationReleaseUnitTest",
            "lintLegacyMigrationRelease",
        )
        val missingTasks = requiredTasks - project.tasks.names
        check(missingTasks.isEmpty()) { "Missing required variant tasks: $missingTasks" }

        val forbiddenTasks = setOf(
            "assembleProductionDebug",
            "assembleProductionDebugAndroidTest",
            "testProductionDebugUnitTest",
            "lintProductionDebug",
            "assembleInternalRelease",
            "bundleInternalRelease",
            "testInternalReleaseUnitTest",
            "lintInternalRelease",
            "assembleLegacyMigrationDebug",
            "testLegacyMigrationDebugUnitTest",
            "lintLegacyMigrationDebug",
        )
        val presentForbiddenTasks = forbiddenTasks.intersect(project.tasks.names)
        check(presentForbiddenTasks.isEmpty()) {
            "Unsupported variant tasks must remain absent: $presentForbiddenTasks"
        }
    }
}

mapOf(
    "minifyProductionReleaseWithR8" to "productionRelease",
    "minifyLegacyMigrationReleaseWithR8" to "legacyMigrationRelease",
).forEach { (taskName, variantName) ->
    tasks.matching { it.name == taskName }.configureEach {
        doFirst {
            layout.buildDirectory
                .dir("outputs/r8Reports/$variantName")
                .get()
                .asFile
                .mkdirs()
        }
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
    // Room's schema parser uses serialization 1.8.1. Align the app and
    // instrumentation classloaders so MigrationTestHelper cannot resolve the
    // older Lifecycle transitive runtime against Room's generated serializers.
    implementation(platform(libs.kotlinx.serialization.bom))
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

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.room.testing)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.robolectric)
}

// The seed-surface contract reads both installable merged manifests. Keep the
// generating tasks wired to the focused unit-test lane used locally and in CI.
tasks.matching {
    it.name == "testInternalDebugUnitTest" || it.name == "testProductionReleaseUnitTest"
}
    .configureEach {
        dependsOn(
            "processInternalDebugMainManifest",
            "processProductionReleaseMainManifest",
            "processInternalDebugAndroidTestManifest",
        )
    }
