plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.compose.gradle.plugin)
    testImplementation(libs.junit)
}

gradlePlugin {
    plugins {
        register("musfitArchitecture") {
            id = "musfit.architecture"
            implementationClass = "com.musfit.buildlogic.MusFitArchitecturePlugin"
        }
    }
}

tasks.test {
    useJUnit()
}
