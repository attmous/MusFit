package com.musfit.core.di
import com.musfit.BuildConfig
import com.musfit.data.remote.auth.GitHubAuthApi
import com.musfit.data.remote.food.FoodProductProvider
import com.musfit.data.remote.food.OpenFoodFactsApi
import com.musfit.data.remote.food.OpenFoodFactsProductProvider
import com.musfit.data.repository.AuthConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkProvidesModule {
    @Provides
    @Singleton
    fun provideMoshi(): Moshi =
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.NONE
                },
            )
            .build()

    @Provides
    @Singleton
    fun provideAuthConfig(): AuthConfig =
        AuthConfig(
            googleWebClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
            githubOAuthClientId = BuildConfig.GITHUB_OAUTH_CLIENT_ID,
        )

    @Provides
    @Singleton
    fun provideOpenFoodFactsApi(
        okHttpClient: OkHttpClient,
    ): OpenFoodFactsApi =
        Retrofit.Builder()
            .baseUrl("https://world.openfoodfacts.org/")
            .client(okHttpClient)
            .build()
            .create(OpenFoodFactsApi::class.java)

    @Provides
    @Singleton
    fun provideGitHubAuthApi(
        okHttpClient: OkHttpClient,
        moshi: Moshi,
    ): GitHubAuthApi =
        Retrofit.Builder()
            .baseUrl("https://github.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubAuthApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindsModule {
    @Binds
    abstract fun bindFoodProductProvider(provider: OpenFoodFactsProductProvider): FoodProductProvider
}
