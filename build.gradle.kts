import io.gitlab.arturbosch.detekt.Detekt

plugins {
    id("musfit.architecture")
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.androidx.baselineprofile) apply false
    alias(libs.plugins.cyclonedx)
    alias(libs.plugins.spotless)
    alias(libs.plugins.detekt)
}

spotless {
    ratchetFrom("origin/master")
    kotlin {
        target(
            "app/src/**/*.kt",
            "baselineprofile/src/**/*.kt",
            "benchmark/src/**/*.kt",
            "build-logic/src/**/*.kt",
            "core/*/src/**/*.kt",
        )
        targetExclude("app/src/main/generated/**")
        ktlint("1.8.0").editorConfigOverride(
            mapOf("ktlint_function_naming_ignore_when_annotated_with" to "Composable"),
        )
    }
    kotlinGradle {
        target(
            "*.gradle.kts",
            "app/*.gradle.kts",
            "baselineprofile/*.gradle.kts",
            "benchmark/*.gradle.kts",
            "build-logic/*.gradle.kts",
            "build-logic/src/**/*.gradle.kts",
            "core/*/*.gradle.kts",
        )
        ktlint("1.8.0")
    }
    format("repositoryText") {
        target(
            "*.md",
            ".github/**/*.yaml",
            ".github/**/*.yml",
            "app/src/**/*.xml",
            "config/**/*.json",
            "config/**/*.yaml",
            "config/**/*.yml",
            "docs/**/*.md",
        )
        targetExclude(
            "app/lint-baseline.xml",
            "config/detekt-baseline.xml",
            "gradle/verification-metadata.xml",
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
}

detekt {
    source.setFrom(
        files(
            "app/src/main/java",
            "app/src/internal/java",
            "benchmark/src/main/java",
            "baselineprofile/src/main/java",
            "core/model/src/main/kotlin",
            "core/database/src/main/java",
            "core/network/src/main/java",
            "core/data/src/main/java",
            "core/designsystem/src/main/kotlin",
            "core/testing/src/main/kotlin",
        ),
    )
    config.setFrom(files("config/detekt.yml"))
    baseline = file("config/detekt-baseline.xml")
    buildUponDefaultConfig = true
    parallel = true
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "17"
    exclude("**/build/**", "**/generated/**")
    reports {
        html.required.set(true)
        xml.required.set(true)
        sarif.required.set(true)
        md.required.set(false)
    }
}

val verifyBuildLogic =
    tasks.register("verifyBuildLogic") {
        group = "verification"
        description = "Runs the convention plugin and module-graph test suite."
        dependsOn(gradle.includedBuild("build-logic").task(":test"))
    }

tasks.register("verifyCoreModules") {
    group = "verification"
    description = "Verifies build logic, module edges, and every shared core module."
    dependsOn(
        verifyBuildLogic,
        "verifyModuleGraph",
        ":core:model:test",
        ":core:database:testDebugUnitTest",
        ":core:database:lintDebug",
        ":core:database:assembleDebugAndroidTest",
        ":core:network:testInternalDebugUnitTest",
        ":core:network:testProductionDebugUnitTest",
        ":core:network:lintInternalDebug",
        ":core:network:lintProductionRelease",
        ":core:data:testInternalDebugUnitTest",
        ":core:data:testProductionDebugUnitTest",
        ":core:data:lintInternalDebug",
        ":core:data:lintProductionRelease",
        ":integration:healthconnect:testInternalDebugUnitTest",
        ":integration:healthconnect:testProductionDebugUnitTest",
        ":integration:healthconnect:lintInternalDebug",
        ":integration:healthconnect:lintProductionRelease",
        ":integration:scanner:testInternalDebugUnitTest",
        ":integration:scanner:testProductionDebugUnitTest",
        ":integration:scanner:lintInternalDebug",
        ":integration:scanner:lintProductionRelease",
        ":integration:scanner:assembleInternalDebugAndroidTest",
        ":integration:healthconnect:assembleInternalDebugAndroidTest",
        ":core:data:assembleInternalDebugAndroidTest",
        ":core:designsystem:testDebugUnitTest",
        ":core:testing:test",
    )
}
