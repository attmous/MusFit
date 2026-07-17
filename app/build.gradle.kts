import com.android.build.api.dsl.ManagedVirtualDevice
import com.android.build.api.variant.ApplicationVariantBuilder
import com.android.build.api.variant.DeviceTestBuilder
import com.android.build.api.variant.HostTestBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test
import org.gradle.process.ExecOperations
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import java.io.ByteArrayOutputStream
import java.util.Properties
import javax.inject.Inject

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.androidx.baselineprofile)
    jacoco
}

jacoco {
    toolVersion = "0.8.14"
}

tasks.withType<Test>().configureEach {
    extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

fun String.asBuildConfigString(): String = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val localProperties =
    Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.isFile) {
            file.inputStream().use(::load)
        }
    }

fun localConfigValue(
    name: String,
    defaultValue: String = "",
): String =
    providers
        .gradleProperty(name)
        .orElse(providers.environmentVariable(name))
        .orElse(localProperties.getProperty(name).orEmpty().ifBlank { defaultValue })
        .get()

// Monotonic build number derived from the git history. Retain the established
// version sequence while release publication is suspended. A missing Git
// executable, shallow checkout, command failure, or invalid count fails closed;
// CI must checkout with fetch-depth: 0. See docs/ops/auto-update.md.
//
// This is a ValueSource rather than a configuration-time ProcessBuilder so the
// commit count is a tracked configuration-cache input: Gradle re-runs it on
// every build and invalidates a stale entry when a new commit changes the count,
// instead of freezing the OTA versionCode at a cached value. The configuration
// cache is not enabled yet (see gradle.properties for why); keeping this
// config-cache-safe makes enabling it later a one-line change.
abstract class GitCommitCountValueSource : ValueSource<Int, GitCommitCountValueSource.Parameters> {
    interface Parameters : ValueSourceParameters {
        val repositoryRoot: DirectoryProperty
        val gitExecutable: org.gradle.api.provider.Property<String>
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    private fun runGit(vararg arguments: String): String {
        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()
        val executable = parameters.gitExecutable.get()
        val result =
            try {
                execOperations.exec {
                    commandLine(executable, *arguments)
                    workingDir(parameters.repositoryRoot.get().asFile)
                    standardOutput = stdout
                    errorOutput = stderr
                    isIgnoreExitValue = true
                }
            } catch (failure: Exception) {
                throw GradleException("Could not execute '$executable' while deriving versionCode.", failure)
            }
        if (result.exitValue != 0) {
            throw GradleException(
                "Git versionCode command failed (${arguments.joinToString(" ")}): " +
                    stderr.toString().trim(),
            )
        }
        return stdout.toString().trim()
    }

    override fun obtain(): Int {
        val shallow = runGit("rev-parse", "--is-shallow-repository")
        if (shallow != "false") {
            throw GradleException("MusFit versionCode requires a non-shallow Git checkout; got '$shallow'.")
        }
        val count = runGit("rev-list", "--count", "HEAD").toIntOrNull()
        if (count == null || count <= 0) {
            throw GradleException("Git returned an invalid MusFit versionCode: '$count'.")
        }
        return count
    }
}

val gitCommitCount =
    providers.of(GitCommitCountValueSource::class) {
        parameters {
            repositoryRoot.set(rootDir)
            gitExecutable.set(providers.gradleProperty("musfit.gitExecutable").orElse("git"))
        }
    }

abstract class VerifyReleaseVariantMatrixTask : DefaultTask() {
    @get:Input
    abstract val enabledVariants: SetProperty<String>

    @get:Input
    abstract val expectedVariants: SetProperty<String>

    @get:Input
    abstract val availableTaskNames: SetProperty<String>

    @get:Input
    abstract val requiredTaskNames: SetProperty<String>

    @get:Input
    abstract val forbiddenTaskNames: SetProperty<String>

    @TaskAction
    fun verify() {
        val expected = expectedVariants.get()
        val enabled = enabledVariants.get()
        check(enabled == expected) {
            "Enabled application variants must be $expected, got $enabled"
        }

        val available = availableTaskNames.get()
        val missing = requiredTaskNames.get() - available
        check(missing.isEmpty()) { "Missing required variant tasks: $missing" }

        val forbidden = forbiddenTaskNames.get().intersect(available)
        check(forbidden.isEmpty()) {
            "Unsupported variant tasks must remain absent: $forbidden"
        }
    }
}

abstract class EnsureDirectoryTask : DefaultTask() {
    @get:OutputDirectory
    abstract val directory: DirectoryProperty

    @TaskAction
    fun create() {
        directory.get().asFile.mkdirs()
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
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
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
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
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
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
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
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        unitTests.isIncludeAndroidResources = true
        managedDevices {
            localDevices {
                create("musFitApi28") {
                    device = "Pixel 2"
                    apiLevel = 28
                    systemImageSource = "google"
                    require64Bit = true
                    testedAbi = "x86_64"
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
                create("criticalJourneysApi28And37") {
                    targetDevices.add(localDevices["musFitApi28"])
                    targetDevices.add(localDevices["musFitApi37"])
                }
            }
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = true
        baseline = file("lint-baseline.xml")
        // Network-dependent version availability is owned by W2-DEPS-01's
        // stable-first Dependabot policy, verification metadata, and 5% guard.
        disable +=
            setOf(
                "AndroidGradlePluginVersion",
                "GradleDependency",
                "NewerVersionAvailable",
            )
    }
}

val enabledApplicationVariants = mutableSetOf<String>()

androidComponents {
    beforeVariants(selector().all()) { variantBuilder ->
        val distribution =
            variantBuilder.productFlavors
                .firstOrNull { (dimension, _) -> dimension == "distribution" }
                ?.second
        val enabled =
            (distribution == "internal" && variantBuilder.buildType == "debug") ||
                (distribution == "production" && variantBuilder.buildType == "benchmark") ||
                (distribution == "production" && variantBuilder.buildType == "benchmarkRelease") ||
                (distribution == "production" && variantBuilder.buildType == "nonMinifiedRelease") ||
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

val verifyReleaseVariantMatrix =
    tasks.register<VerifyReleaseVariantMatrixTask>("verifyReleaseVariantMatrix") {
        group = "verification"
        description = "Verifies the only enabled installable variants and their canonical task families."
        expectedVariants.set(
            setOf(
                "internalDebug",
                "productionBenchmark",
                "productionBenchmarkRelease",
                "productionNonMinifiedRelease",
                "productionRelease",
                "legacyMigrationRelease",
            ),
        )
        requiredTaskNames.set(
            setOf(
                "assembleInternalDebug",
                "assembleInternalDebugAndroidTest",
                "testInternalDebugUnitTest",
                "lintInternalDebug",
                "assembleProductionRelease",
                "assembleProductionBenchmark",
                "bundleProductionRelease",
                "testProductionReleaseUnitTest",
                "lintProductionRelease",
                "assembleLegacyMigrationRelease",
                "testLegacyMigrationReleaseUnitTest",
                "lintLegacyMigrationRelease",
            ),
        )
        forbiddenTaskNames.set(
            setOf(
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
            ),
        )
    }

gradle.projectsEvaluated {
    verifyReleaseVariantMatrix.configure {
        enabledVariants.set(enabledApplicationVariants)
        availableTaskNames.set(tasks.names)
    }
}

mapOf(
    "minifyProductionReleaseWithR8" to "productionRelease",
    "minifyLegacyMigrationReleaseWithR8" to "legacyMigrationRelease",
).forEach { (taskName, variantName) ->
    val prepareReports =
        tasks.register<EnsureDirectoryTask>(
            "prepare${variantName.replaceFirstChar(Char::uppercaseChar)}R8Reports",
        ) {
            directory.set(layout.buildDirectory.dir("outputs/r8Reports/$variantName"))
        }
    tasks.matching { it.name == taskName }.configureEach {
        dependsOn(prepareReports)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(platform(libs.androidx.compose.bom))
    // Room's schema parser uses serialization 1.8.1. Align the app and
    // instrumentation classloaders so MigrationTestHelper cannot resolve the
    // older Lifecycle transitive runtime against Room's generated serializers.
    implementation(platform(libs.kotlinx.serialization.bom))
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.androidx.health.connect)
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
    implementation(libs.androidx.profileinstaller)

    ksp(libs.hilt.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.uiautomator)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestUtil(libs.androidx.test.orchestrator)

    testImplementation(libs.junit)
    testImplementation(project(":core:testing"))
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi.core)
    testImplementation(libs.roborazzi.compose)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)

    baselineProfile(project(":baselineprofile"))
}

baselineProfile {
    automaticGenerationDuringBuild = false
    saveInSrc = true
    mergeIntoMain = true
    filter {
        include("com.musfit.**")
    }
    warnings {
        disabledVariants = false
    }
}

roborazzi {
    outputDir.set(file("src/testInternalDebug/screenshots"))
    compare.outputDir.set(layout.buildDirectory.dir("outputs/roborazzi-comparison"))
}

// The seed-surface contract reads both installable merged manifests. Keep the
// generating tasks wired to the focused unit-test lane used locally and in CI.
tasks
    .matching {
        it.name == "testInternalDebugUnitTest" || it.name == "testProductionReleaseUnitTest"
    }.configureEach {
        dependsOn(
            "processInternalDebugMainManifest",
            "processProductionReleaseMainManifest",
            "processLegacyMigrationReleaseMainManifest",
            "processInternalDebugAndroidTestManifest",
        )
    }
