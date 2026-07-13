import com.android.build.api.dsl.ManagedVirtualDevice

plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.musfit.baselineprofile"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        missingDimensionStrategy("distribution", "production")
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    testOptions.managedDevices.localDevices {
        create("baselineProfileApi37") {
            device = "Pixel 2"
            apiLevel = 37
            systemImageSource = "google"
            testedAbi = "x86_64"
            pageAlignment = ManagedVirtualDevice.PageAlignment.FORCE_16KB_PAGES
        }
    }
}

baselineProfile {
    managedDevices += "baselineProfileApi37"
    useConnectedDevices = false
}

dependencies {
    implementation(libs.androidx.benchmark.macro)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.uiautomator)
}
