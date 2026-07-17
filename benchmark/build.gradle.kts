import com.android.build.api.dsl.ManagedVirtualDevice

plugins {
    alias(libs.plugins.android.test)
}

android {
    namespace = "com.musfit.benchmark"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        missingDimensionStrategy("distribution", "production")
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] =
            "EMULATOR,LOW-BATTERY,UNLOCKED"
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    buildTypes {
        create("benchmark") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
    }

    testOptions.managedDevices {
        localDevices {
            create("benchmarkApi28") {
                device = "Pixel 2"
                apiLevel = 28
                systemImageSource = "google"
                require64Bit = true
                testedAbi = "x86_64"
            }
            create("benchmarkApi37") {
                device = "Pixel 2"
                apiLevel = 37
                systemImageSource = "google"
                testedAbi = "x86_64"
                pageAlignment = ManagedVirtualDevice.PageAlignment.FORCE_16KB_PAGES
            }
        }
        groups {
            create("benchmarkApi28And37") {
                targetDevices.add(localDevices["benchmarkApi28"])
                targetDevices.add(localDevices["benchmarkApi37"])
            }
        }
    }
}

androidComponents {
    beforeVariants(selector().all()) { variantBuilder ->
        variantBuilder.enable = variantBuilder.buildType == "benchmark"
    }
}

dependencies {
    implementation(libs.androidx.benchmark.macro)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.uiautomator)
}
