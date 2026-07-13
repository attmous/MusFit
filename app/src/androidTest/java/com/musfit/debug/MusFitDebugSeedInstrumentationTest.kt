package com.musfit.debug

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.musfit.core.di.DatabaseModule
import com.musfit.data.local.entity.LOCAL_DEFAULT_ACCOUNT_ID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MusFitDebugSeedInstrumentationTest {
    @Test
    fun approvedInstrumentationSeedsFoodAndTrainingWhileTargetHasNoLegacySurface() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val callerContext = instrumentation.context
        val targetContext = instrumentation.targetContext
        assertNotEquals(
            "The probe must use the separately installed test package context.",
            targetContext.packageName,
            callerContext.packageName,
        )

        val reset = InstrumentationRegistry.getArguments()
            .getString(RESET_ARGUMENT)
            ?.toBooleanStrictOrNull()
            ?: false
        val summary = MusFitDebugSeeder.create(targetContext).use { seeder ->
            seeder.seed(reset = reset)
        }
        assertTrue(summary.contains("MusFit debug data seeded"))

        val legacyComponent = ComponentName(targetContext.packageName, LEGACY_RECEIVER_CLASS)
        val packageManager = callerContext.packageManager
        assertFalse(
            "The installed target must not contain the legacy receiver component.",
            packageManager.hasReceiver(legacyComponent),
        )
        val matchingReceivers = packageManager.queryLegacySeedReceivers(targetContext.packageName)
        assertTrue(
            "The installed target must not resolve the legacy seed action.",
            matchingReceivers.isEmpty(),
        )

        val database = DatabaseModule.provideDatabase(targetContext.applicationContext)
        try {
            assertNotNull(database.foodDao().getFood(LOCAL_DEFAULT_ACCOUNT_ID, PROBE_FOOD_ID))
            assertNotNull(database.trainingDao().getWorkoutSession(PROBE_WORKOUT_ID))

            // Exercise the former intent shape using the test package context. The
            // component/action absence asserted above is the definitive denial;
            // this call additionally proves the obsolete path remains a no-op.
            callerContext.sendBroadcast(
                Intent(LEGACY_SEED_ACTION)
                    .setComponent(legacyComponent)
                    .putExtra(RESET_ARGUMENT, true),
            )
            instrumentation.waitForIdleSync()
            SystemClock.sleep(250)

            assertNotNull(
                "A legacy external reset attempt must not delete seeded data.",
                database.foodDao().getFood(LOCAL_DEFAULT_ACCOUNT_ID, PROBE_FOOD_ID),
            )
            assertNotNull(
                "A legacy external reset attempt must not delete seeded training data.",
                database.trainingDao().getWorkoutSession(PROBE_WORKOUT_ID),
            )
        } finally {
            database.close()
        }
    }

    private fun PackageManager.hasReceiver(component: ComponentName): Boolean = try {
        if (Build.VERSION.SDK_INT >= 33) {
            getReceiverInfo(component, PackageManager.ComponentInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            getReceiverInfo(component, 0)
        }
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private fun PackageManager.queryLegacySeedReceivers(targetPackage: String) = if (Build.VERSION.SDK_INT >= 33) {
        queryBroadcastReceivers(
            Intent(LEGACY_SEED_ACTION).setPackage(targetPackage),
            PackageManager.ResolveInfoFlags.of(0),
        )
    } else {
        @Suppress("DEPRECATION")
        queryBroadcastReceivers(Intent(LEGACY_SEED_ACTION).setPackage(targetPackage), 0)
    }

    private companion object {
        const val RESET_ARGUMENT = "reset"
        const val LEGACY_SEED_ACTION = "com.musfit.debug.SEED_TEST_DATA"
        const val LEGACY_RECEIVER_CLASS = "com.musfit.debug.MusFitDebugSeedReceiver"
        const val PROBE_FOOD_ID = "debug-food-yogurt"
        const val PROBE_WORKOUT_ID = "debug-workout-active"
    }
}
