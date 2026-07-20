import com.android.build.api.dsl.ManagedVirtualDevice

plugins {
    id("musfit.android.compose")
    id("musfit.test")
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
}

val benchmarkThumbnailBase64 =
    listOf(
        "iVBORw0KGgoAAAANSUhEUgAAAQAAAAEACAYAAABccqhmAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAA",
        "DsMAAA7DAcdvqGQAAAU/SURBVHhe7d0hrt0GGITRrLDL6CKyhqyhsAWFpcFhDQkqDSupSopTOfDKZCI7cmbOSIf84F49Pfuj",
        "980///73Bdj05vUA7BAAGCYAMEwAYJgAwDABgGECAMMEAIYJAAwTABgmADBMAGCYAMAwAYBhAgDDBACGCQAMEwAYJgAwTABg",
        "mADAMAGAYQIAw24PwIePn7/88vufwDc43p/Xd+pKtwfg+CN++vlX4Bsc78/rO3UlAYAHEwAYJgAwTABgmADAMAGAYQIAwwQA",
        "hgkADBMAGCYAMEwAYJgAwDABgGECAMMEAIYJAAwTABgmADBMAGCYAMAwAYBhAgDDBACGCQAMEwAYJgAwTABgmADAMAGAYQIA",
        "wwQAhgkADBMAGCYAMEwAYJgAwDABgGECQOTtu/dffvvj022Oz3/9Tu4jAESOl/TOHZ//+p3cRwCICEAXASAiAF0EgIgAdBEA",
        "IgLQRQCICEAXASAiAF0EgIgAdBEAIgLQRQCICEAXASAiAF0EgIgAdBEAIgLQRQCICEAXASAiAF0EgIgAdBEAIgLQRQCICEAX",
        "ASAiAF0EgIgAdBEAIgLQRQCICEAXASAiAF0EgIgAdBEAIgLQRQCICEAXASAiAF0EgIgAdBEAIgLQRQCICEAXASAiAF0EgIgA",
        "dBEAIgLQRQCICEAXASAiAF0EgIgAdBEAIgLQRQCICEAXASAiAF0EgIgAdBEAIgLQRQCICEAXASAiAF0EgIgAdBEAIgLQRQCI",
        "CEAXASAiAF0EgIgAdBEAIgLQRQCICEAXASAiAF0E4GZv373/+lC3+PTX36/v7KU7Pv/1O39kx///9Zl4EgG42fEQ2O6O///r",
        "M/EkAnAzAdieAJwcryQA9uQJwMnxSgJgT54AnByvJAD25AnAyfFKAmBPngCcHK8kAPbkCcDJ8UoCYE+eAJwcryQA9uQJwMnx",
        "SgJgT54AnByvJAD25AnAyfFKAmBPngCcHK8kAPbkCcDJ8UoCYE+eAJwcryQA9uQJwMnxSgJgT54AnByvJAD25AnAyfFKAmBP",
        "ngCcHK8kAPbkCcDJ8UoCYE+eAJwcryQA9uQJwMnxSgJgT54AnByvJAD25AnAyfFKTw+A3wXI5ncBvi8BIHI81Hfu+PzX7+Q+",
        "AkBEALoIABEB6CIARASgiwAQEYAuAkBEALoIABEB6CIARASgiwAQEYAuAkBEALoIABEB6CIARASgiwAQEYAuAkBEALoIABEB",
        "6CIARASgiwAQEYAuAkBEALoIABEB6CIARASgiwAQEYAuAkBEALoIABEB6CIARASgiwAQEYAuAkBEALoIABEB6CIARASgiwAQ",
        "EYAuAkBEALoIABEB6CIARASgiwAQEYAuAkBEALoIABEB6CIARASgiwAQEYAuAkBEALoIABEB6CIARASgiwAQEYAuAkBEALoI",
        "ABEB6CIARASgiwAQEYAuAkBEALoIABEB6CIARASgiwAQEYAuAkBEALoIABEB6CIARN6+e//1Jb3L8fmv38l9BACGCQAMEwAY",
        "JgAwTABgmADAMAGAYQIAwwQAhgkADBMAGCYAMEwAYJgAwDABgGECAMMEAIYJAAwTABgmADBMAGCYAMAwAYBhAgDDBACGCQAM",
        "EwAYJgAwTABgmADAMAGAYQIAwwQAhgkADBMAGCYAMEwAYJgAwDABgGE/fAA+fPz89Y8Acsf78/pOXen2AADPJQAwTABgmADA",
        "MAGAYQIAwwQAhgkADBMAGCYAMEwAYJgAwDABgGECAMMEAIYJAAwTABgmADBMAGCYAMAwAYBhAgDDBACG/Q+yc+ZXodRzwQAA",
        "AABJRU5ErkJggg==",
    ).joinToString(separator = "")

android {
    namespace = "com.musfit.feature.training"
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "BENCHMARK_THUMBNAIL_BASE64", "\"\"")
    }
    flavorDimensions += "distribution"
    productFlavors {
        create("internal") { dimension = "distribution" }
        create("production") { dimension = "distribution" }
        create("legacyMigration") { dimension = "distribution" }
    }
    buildTypes {
        debug { enableUnitTestCoverage = true }
        create("benchmark") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
            buildConfigField("String", "BENCHMARK_THUMBNAIL_BASE64", "\"$benchmarkThumbnailBase64\"")
        }
    }
    buildFeatures { buildConfig = true }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        managedDevices {
            localDevices {
                create("trainingApi28") {
                    device = "Pixel 2"
                    apiLevel = 28
                    systemImageSource = "google"
                    require64Bit = true
                    testedAbi = "x86_64"
                }
                create("trainingApi37") {
                    device = "Pixel 2"
                    apiLevel = 37
                    systemImageSource = "google"
                    pageAlignment = ManagedVirtualDevice.PageAlignment.FORCE_16KB_PAGES
                    testedAbi = "x86_64"
                }
            }
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(platform(libs.kotlinx.serialization.bom))
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.compose.material3.adaptive.navigation3)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.hilt.android)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)

    ksp(libs.hilt.compiler)

    testImplementation(project(":core:testing"))
    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
