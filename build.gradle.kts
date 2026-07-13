import io.gitlab.arturbosch.detekt.Detekt

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.compose.compiler) apply false
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
