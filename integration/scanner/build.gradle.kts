plugins {
    id("musfit.android.library")
    id("musfit.test")
}

android {
    namespace = "com.musfit.integration.scanner"
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
        }
    }
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.mlkit)
    implementation(libs.mlkit.barcode)
    implementation(libs.mlkit.text)
    testImplementation(libs.junit)
}
