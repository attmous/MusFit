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
import org.junit.Assert.assertTrue
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
        assertEquals(setOf("steps"), viewModel.state.value.requestablePermissions)
        assertEquals(true, viewModel.state.value.canRequestPermissions)
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
        assertTrue(viewModel.state.value.requestablePermissions.isEmpty())
        assertEquals(1, viewModel.state.value.requestablePermissionCount)
        assertEquals(false, viewModel.state.value.canRequestPermissions)
        assertEquals(
            "Install or update Health Connect to sync health data with MusFit.",
            viewModel.state.value.message,
        )
    }

    @Test
    fun refreshStatus_hidesPermissionLauncherWhenGatewayNotSupported() = runTest {
        val viewModel = HealthViewModel(
            FakeHealthConnectGateway(
                status = HealthConnectStatus(
                    availability = HealthConnectAvailability.NotSupported,
                    grantedPermissions = emptySet(),
                ),
                requestablePermissions = setOf("steps", "weight"),
            ),
        )

        viewModel.refreshStatus()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Not supported", viewModel.state.value.availabilityLabel)
        assertTrue(viewModel.state.value.requestablePermissions.isEmpty())
        assertEquals(2, viewModel.state.value.requestablePermissionCount)
        assertEquals(false, viewModel.state.value.canRequestPermissions)
    }

    @Test
    fun refreshStatus_showsEnableSyncMessageWhenNoPermissionsGranted() = runTest {
        val viewModel = HealthViewModel(
            FakeHealthConnectGateway(
                status = HealthConnectStatus(
                    availability = HealthConnectAvailability.Available,
                    grantedPermissions = emptySet(),
                ),
                requestablePermissions = setOf("steps", "weight"),
            ),
        )

        viewModel.refreshStatus()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            "No Health Connect permissions are granted. Tap Enable Health Connect sync to choose what MusFit can access.",
            viewModel.state.value.message,
        )
        assertEquals(setOf("steps", "weight"), viewModel.state.value.requestablePermissions)
    }

    @Test
    fun refreshStatus_showsPartialGrantMessageWhenSomePermissionsGranted() = runTest {
        val viewModel = HealthViewModel(
            FakeHealthConnectGateway(
                status = HealthConnectStatus(
                    availability = HealthConnectAvailability.Available,
                    grantedPermissions = setOf("steps"),
                ),
                requestablePermissions = setOf("steps", "weight"),
            ),
        )

        viewModel.refreshStatus()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            "Some Health Connect permissions are granted. Tap Enable Health Connect sync to review or add access.",
            viewModel.state.value.message,
        )
    }

    @Test
    fun refreshStatus_resetsStaleSuccessStateAfterFailure() = runTest {
        var failNextRefresh = false
        val gateway = object : HealthConnectGateway {
            override suspend fun status(): HealthConnectStatus {
                if (failNextRefresh) throw IllegalStateException("boom")
                return HealthConnectStatus(
                    availability = HealthConnectAvailability.Available,
                    grantedPermissions = setOf("steps"),
                )
            }

            override suspend fun requestablePermissions(): Set<String> = setOf("steps", "weight")

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
        val viewModel = HealthViewModel(gateway)

        viewModel.refreshStatus()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Available", viewModel.state.value.availabilityLabel)
        assertEquals(1, viewModel.state.value.grantedPermissionCount)
        assertEquals(setOf("steps", "weight"), viewModel.state.value.requestablePermissions)
        assertEquals(true, viewModel.state.value.canRequestPermissions)

        failNextRefresh = true
        viewModel.refreshStatus()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Unknown", viewModel.state.value.availabilityLabel)
        assertEquals(0, viewModel.state.value.grantedPermissionCount)
        assertEquals(0, viewModel.state.value.requestablePermissionCount)
        assertTrue(viewModel.state.value.requestablePermissions.isEmpty())
        assertEquals(false, viewModel.state.value.canRequestPermissions)
        assertEquals(
            "Unable to refresh Health Connect status right now. Try again from the Health tab.",
            viewModel.state.value.message,
        )
    }

    private class FakeHealthConnectGateway : HealthConnectGateway {
        constructor(
            status: HealthConnectStatus = HealthConnectStatus(
                availability = HealthConnectAvailability.Available,
                grantedPermissions = setOf("steps"),
            ),
            requestablePermissions: Set<String> = setOf("steps"),
            statusException: Throwable? = null,
            requestablePermissionsException: Throwable? = null,
        ) {
            this.status = status
            this.requestablePermissions = requestablePermissions
            this.statusException = statusException
            this.requestablePermissionsException = requestablePermissionsException
        }

        private val status: HealthConnectStatus
        private val requestablePermissions: Set<String>
        private val statusException: Throwable?
        private val requestablePermissionsException: Throwable?

        override suspend fun status(): HealthConnectStatus {
            statusException?.let { throw it }
            return status
        }

        override suspend fun requestablePermissions(): Set<String> {
            requestablePermissionsException?.let { throw it }
            return requestablePermissions
        }

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
