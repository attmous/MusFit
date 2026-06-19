package com.musfit.core.di

import android.content.Context
import androidx.room.Room
import com.musfit.data.local.MusFitDatabase
import com.musfit.data.local.dao.FoodDao
import com.musfit.data.local.dao.HealthDao
import com.musfit.data.local.dao.TrainingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MusFitDatabase =
        Room.databaseBuilder(context, MusFitDatabase::class.java, "musfit.db").build()

    @Provides
    fun provideFoodDao(database: MusFitDatabase): FoodDao = database.foodDao()

    @Provides
    fun provideTrainingDao(database: MusFitDatabase): TrainingDao = database.trainingDao()

    @Provides
    fun provideHealthDao(database: MusFitDatabase): HealthDao = database.healthDao()
}
