# MusFit intentionally starts from Android's optimized defaults and dependency
# consumer rules. Add only narrow, reproduced rules here; package-wide keeps hide
# release-only defects and defeat W1-REL-03 size and optimization goals.

# Moshi uses reflection for these exact DTOs. The API 37 minified Profile smoke
# reproduced vertical class merging of OpenAiChatCompletionResponse as an
# abstract type during Hilt construction. Keep only the reflected DTOs and
# their members; Room, Hilt, Retrofit, Health Connect, and ML Kit retain their
# own consumer rules.
-keep class com.musfit.data.remote.coach.OpenAiChatCompletionRequest { *; }
-keep class com.musfit.data.remote.coach.OpenAiChatMessage { *; }
-keep class com.musfit.data.remote.coach.OpenAiChatCompletionResponse { *; }
-keep class com.musfit.data.remote.coach.OpenAiChoice { *; }
-keep class com.musfit.data.remote.coach.OpenAiErrorResponse { *; }
-keep class com.musfit.data.remote.coach.OpenAiError { *; }
-keep class com.musfit.data.remote.food.OpenFoodFactsResponse { *; }
-keep class com.musfit.data.remote.food.OpenFoodFactsProduct { *; }
-keep class com.musfit.data.remote.food.OpenFoodFactsNutriments { *; }
-keep class com.musfit.data.remote.food.OpenFoodFactsSearchResponse { *; }
-keep class com.musfit.data.remote.food.OpenFoodFactsSearchHit { *; }
-keep class com.musfit.data.remote.auth.GitHubDeviceCodeResponse { *; }
-keep class com.musfit.data.remote.auth.GitHubAccessTokenResponse { *; }
-keep class com.musfit.data.remote.auth.GitHubUserResponse { *; }
-keep class com.musfit.data.remote.auth.GitHubEmailResponse { *; }

# Firebase discovers component registrars by manifest class name and constructs
# them reflectively. Its consumer rule preserves the class names, but full-mode
# R8 bundled with AGP 9.2.1 can remove the unreferenced zero-argument constructors.
-keepclassmembers class * implements com.google.firebase.components.ComponentRegistrar {
    public <init>();
}
