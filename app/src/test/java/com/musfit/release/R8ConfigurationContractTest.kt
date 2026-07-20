package com.musfit.release

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class R8ConfigurationContractTest {
    @Test
    fun reflectiveMoshiModelsUseExactKeepsWithoutAppWideWildcards() {
        val rules = resolveProjectFile("proguard-rules.pro").readText()

        reflectedMoshiModels.forEach { model ->
            assertTrue(
                "Missing exact reflective Moshi keep for $model",
                rules.contains("-keep class $model { *; }"),
            )
        }
        assertFalse(rules.contains("com.musfit.**"))
        assertFalse(rules.contains("com.musfit.data.**"))
        assertFalse(rules.contains("-dontshrink"))
        assertFalse(rules.contains("-dontoptimize"))
        assertFalse(rules.contains("-dontobfuscate"))
    }

    @Test
    fun manifestDiscoveredComponentRegistrarsKeepConstructors() {
        val rules = resolveProjectFile("proguard-rules.pro").readText()

        assertTrue(
            rules.contains(
                "-keepclassmembers class * implements " +
                    "com.google.firebase.components.ComponentRegistrar {",
            ),
        )
        assertTrue(rules.contains("public <init>();"))
        assertFalse(rules.contains("com.google.mlkit.vision.barcode.internal.zzc"))
        assertFalse(rules.contains("com.google.mlkit.vision.barcode.internal.zzd"))
        assertFalse(rules.contains("com.google.mlkit.vision.barcode.internal.zzg"))
        assertFalse(rules.contains("com.google.mlkit.vision.barcode.internal.**"))
        assertFalse(rules.contains("com.google.mlkit.**"))
    }

    private fun resolveProjectFile(path: String): File {
        val candidates = listOf(File(path), File("app/$path"), File("../app/$path"))
        return candidates.firstOrNull(File::isFile)
            ?: error("Could not find $path: ${candidates.joinToString { it.path }}")
    }

    private companion object {
        val reflectedMoshiModels = listOf(
            "com.musfit.data.remote.coach.OpenAiChatCompletionRequest",
            "com.musfit.data.remote.coach.OpenAiChatMessage",
            "com.musfit.data.remote.coach.OpenAiChatCompletionResponse",
            "com.musfit.data.remote.coach.OpenAiChoice",
            "com.musfit.data.remote.coach.OpenAiErrorResponse",
            "com.musfit.data.remote.coach.OpenAiError",
            "com.musfit.data.remote.food.OpenFoodFactsResponse",
            "com.musfit.data.remote.food.OpenFoodFactsProduct",
            "com.musfit.data.remote.food.OpenFoodFactsNutriments",
            "com.musfit.data.remote.food.OpenFoodFactsSearchResponse",
            "com.musfit.data.remote.food.OpenFoodFactsSearchHit",
            "com.musfit.data.remote.auth.GitHubDeviceCodeResponse",
            "com.musfit.data.remote.auth.GitHubAccessTokenResponse",
            "com.musfit.data.remote.auth.GitHubUserResponse",
            "com.musfit.data.remote.auth.GitHubEmailResponse",
        )
    }
}
