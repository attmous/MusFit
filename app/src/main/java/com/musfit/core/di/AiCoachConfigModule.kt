package com.musfit.core.di

import com.musfit.BuildConfig
import com.musfit.data.repository.AiCoachDebugDefaults
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiCoachConfigModule {
    @Provides
    @Singleton
    fun provideAiCoachDebugDefaults(): AiCoachDebugDefaults =
        AiCoachDebugDefaults(
            hermesBaseUrl = BuildConfig.DEBUG_HERMES_BASE_URL,
            hermesModelName = BuildConfig.DEBUG_HERMES_MODEL_NAME,
            hermesApiKey = BuildConfig.DEBUG_HERMES_API_KEY,
        )
}
