package com.musfit.core.di

import com.musfit.domain.health.HealthConnectGateway
import com.musfit.integrations.healthconnect.HealthConnectManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HealthModule {
    @Binds
    @Singleton
    abstract fun bindHealthConnectGateway(manager: HealthConnectManager): HealthConnectGateway
}
