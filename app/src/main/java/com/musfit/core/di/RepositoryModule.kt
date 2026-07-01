package com.musfit.core.di

import com.musfit.data.repository.AccountRepository
import com.musfit.data.repository.AssetExerciseDatasetProvider
import com.musfit.data.repository.CoachRepository
import com.musfit.data.repository.ExerciseDatasetProvider
import com.musfit.data.repository.FoodRepository
import com.musfit.data.repository.GoalsRepository
import com.musfit.data.repository.HealthRepository
import com.musfit.data.repository.LocalAccountRepository
import com.musfit.data.repository.LocalCoachRepository
import com.musfit.data.repository.LocalFoodRepository
import com.musfit.data.repository.LocalGoalsRepository
import com.musfit.data.repository.LocalHealthRepository
import com.musfit.data.repository.LocalProfileRepository
import com.musfit.data.repository.LocalTrainingRepository
import com.musfit.data.repository.ProfileRepository
import com.musfit.data.repository.TrainingRepository
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
    abstract fun bindAccountRepository(repository: LocalAccountRepository): AccountRepository

    @Binds
    @Singleton
    abstract fun bindCoachRepository(repository: LocalCoachRepository): CoachRepository

    @Binds
    @Singleton
    abstract fun bindFoodRepository(repository: LocalFoodRepository): FoodRepository

    @Binds
    @Singleton
    abstract fun bindTrainingRepository(repository: LocalTrainingRepository): TrainingRepository

    @Binds
    @Singleton
    abstract fun bindHealthRepository(repository: LocalHealthRepository): HealthRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(repository: LocalProfileRepository): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindGoalsRepository(repository: LocalGoalsRepository): GoalsRepository

    @Binds
    abstract fun bindExerciseDatasetProvider(
        provider: AssetExerciseDatasetProvider,
    ): ExerciseDatasetProvider
}
