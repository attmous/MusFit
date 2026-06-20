package com.musfit.ui.health

import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.domain.health.HealthConnectStatus
import com.musfit.domain.health.ImportedDailyHealthSummary
import com.musfit.integrations.healthconnect.HealthConnectGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HealthViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun refreshStatus_showsAvailableWhenGatewayAvailable() = runTest {
        val viewModel = HealthViewModel(FakeHealthConnectGateway())

        viewModel.refreshStatus()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Available", viewModel.state.value.availabilityLabel)
        assertEquals(1, viewModel.state.value.grantedPermissionCount)
    }

    @Test
    fun refreshStatus_showsInstallMessageWhenGatewayUnavailable() = runTest {
        val viewModel = HealthViewModel(
            FakeHealthConnectGateway(
                status = HealthConnectStatus(
                    availability = HealthConnectAvailability.NotInstalled,
                    grantedPermissions = emptySet(),
                ),
            ),
        )

        viewModel.refreshStatus()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Install or update required", viewModel.state.value.availabilityLabel)
        assertEquals(0, viewModel.state.value.grantedPermissionCount)
        assertEquals(
            "Install or update Health Connect to sync health data with MusFit.",
            viewModel.state.value.message,
        )
    }

    private class FakeHealthConnectGateway : HealthConnectGateway {
        constructor(
            status: HealthConnectStatus = HealthConnectStatus(
                availability = HealthConnectAvailability.Available,
                grantedPermissions = setOf("steps"),
            ),
        ) {
            this.status = status
        }

        private val status: HealthConnectStatus

        override suspend fun status() = status

        override suspend fun requestablePermissions(): Set<String> = setOf("steps")

        override suspend fun readDailySummary(date: LocalDate) = ImportedDailyHealthSummary(
            steps = 1200,
            activeCaloriesKcal = 100.0,
            latestWeightKg = null,
            restingHeartRateBpm = null,
        )

        override suspend fun exportWorkout(
            session: com.musfit.data.local.entity.WorkoutSessionEntity,
            sets: List<com.musfit.data.local.entity.WorkoutSetEntity>,
        ): String? = "record-id"
    }
}
