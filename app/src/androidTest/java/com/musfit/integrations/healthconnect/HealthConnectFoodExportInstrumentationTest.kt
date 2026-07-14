package com.musfit.integrations.healthconnect

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.musfit.domain.health.HealthConnectAvailability
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class HealthConnectFoodExportInstrumentationTest {
    @Test
    fun grantedFoodPermissionsInsertThenDeleteExactlyOneNutritionAndHydrationRecord() = runBlocking {
        assumeTrue(
            "Run explicitly with -e $OPT_IN_ARGUMENT true on a disposable Health emulator.",
            InstrumentationRegistry.getArguments()
                .getString(OPT_IN_ARGUMENT)
                ?.toBooleanStrictOrNull() == true,
        )

        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = HealthConnectManager(targetContext)
        val status = manager.status()
        assertEquals(HealthConnectAvailability.Available, status.availability)
        assertEquals(
            manager.foodRequestablePermissions(),
            status.grantedPermissions.intersect(manager.foodRequestablePermissions()),
        )

        val date = LocalDate.now().minusDays(PROBE_DAYS_AGO)
        val nutritionClientId = HealthConnectRecordIdentity.forNutrition(
            PROBE_ACCOUNT_ID,
            PROBE_MEAL_ID,
            version = 1,
        ).clientRecordId
        val hydrationClientId = HealthConnectRecordIdentity.forHydration(
            PROBE_ACCOUNT_ID,
            date,
            version = 1,
        ).clientRecordId
        val client = HealthConnectClient.getOrCreate(targetContext)
        val authoredRecords = setOf(
            HealthConnectAuthoredRecord(HealthConnectAuthoredRecordType.Nutrition, nutritionClientId),
            HealthConnectAuthoredRecord(HealthConnectAuthoredRecordType.Hydration, hydrationClientId),
        )
        var deletionConfirmed = false

        try {
            val result = manager.exportFood(
                HealthConnectFoodExportPayload(
                    accountId = PROBE_ACCOUNT_ID,
                    date = date,
                    meals = listOf(
                        HealthConnectFoodMealExport(
                            mealType = PROBE_MEAL_TYPE,
                            accountId = PROBE_ACCOUNT_ID,
                            localMealId = PROBE_MEAL_ID,
                            name = "W1 Health permission probe",
                            caloriesKcal = 125.0,
                            proteinGrams = 10.0,
                            carbsGrams = 15.0,
                            fatGrams = 3.0,
                        ),
                    ),
                    hydrationMilliliters = 250.0,
                ),
            )

            assertNotNull(result)
            assertEquals(1, result?.nutritionRecordCount)
            assertEquals(1, result?.hydrationRecordCount)
            assertEquals(PROBE_MEAL_ID, result?.nutritionProviderRecordIds?.keys?.single())
            assertNotNull(result?.hydrationProviderRecordId)
            assertEquals(
                HealthConnectDeleteResult.Complete(authoredRecords),
                manager.deleteAuthoredRecords(authoredRecords),
            )
            deletionConfirmed = true
        } finally {
            if (!deletionConfirmed) {
                runCatching {
                    client.deleteRecords(
                        NutritionRecord::class,
                        recordIdsList = emptyList(),
                        clientRecordIdsList = listOf(nutritionClientId),
                    )
                }
                runCatching {
                    client.deleteRecords(
                        HydrationRecord::class,
                        recordIdsList = emptyList(),
                        clientRecordIdsList = listOf(hydrationClientId),
                    )
                }
            }
        }
    }

    private companion object {
        const val OPT_IN_ARGUMENT = "runHealthConnectExport"
        const val PROBE_DAYS_AGO = 20L
        const val PROBE_MEAL_TYPE = "w1-hc-01"
        const val PROBE_MEAL_ID = "w3-hc-01-probe-meal"
        const val PROBE_ACCOUNT_ID = "w3-hc-01-probe-account"
    }
}
