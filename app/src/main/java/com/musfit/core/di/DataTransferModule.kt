package com.musfit.core.di

import com.musfit.data.transfer.AndroidDataTransferRepository
import com.musfit.data.transfer.DataTransferRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataTransferModule {
    @Binds
    @Singleton
    abstract fun bindDataTransferRepository(repository: AndroidDataTransferRepository): DataTransferRepository
}
