package com.musfit.core.di

import com.musfit.data.repository.FoodRepository
import com.musfit.data.repository.LocalFoodRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindFoodRepository(repository: LocalFoodRepository): FoodRepository
}
