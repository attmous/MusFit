plugins {
    id("musfit.android.compose")
    id("musfit.test")
}

android {
    namespace = "com.musfit.core.designsystem"
}

dependencies {
    api(project(":core:model"))
    api(platform(libs.androidx.compose.bom))
    implementation(platform(libs.kotlinx.serialization.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.tooling.preview)
    api(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.material.icons.extended)

    testImplementation(libs.junit)
}
