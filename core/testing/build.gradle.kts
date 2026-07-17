plugins {
    id("musfit.kotlin.library")
    id("musfit.test")
}

dependencies {
    api(project(":core:model"))
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
}
