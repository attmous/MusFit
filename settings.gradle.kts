pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MusFit"
include(":app")
include(":benchmark")
include(":baselineprofile")
include(":core:model")
include(":core:database")
include(":core:network")
include(":core:data")
include(":core:designsystem")
include(":core:testing")
