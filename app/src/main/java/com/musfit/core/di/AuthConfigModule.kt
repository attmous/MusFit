package com.musfit.core.di

import com.musfit.BuildConfig
import com.musfit.data.repository.AuthConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthConfigModule {
    @Provides
    @Singleton
    fun provideAuthConfig(): AuthConfig = AuthConfig(
        googleWebClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
        githubOAuthClientId = BuildConfig.GITHUB_OAUTH_CLIENT_ID,
    )
}
