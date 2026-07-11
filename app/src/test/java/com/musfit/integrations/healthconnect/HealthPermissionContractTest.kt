package com.musfit.integrations.healthconnect

import android.content.Context
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.test.core.app.ApplicationProvider
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.w3c.dom.Element

@RunWith(RobolectricTestRunner::class)
class HealthPermissionContractTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun mergedManifestsDeclareExactlyEveryRequestedHealthPermission() = runTest {
        val manager = HealthConnectManager(context)
        val requestedPermissions =
            manager.requestablePermissions() + manager.foodRequestablePermissions()

        listOf("internalDebug", "legacyMigrationRelease", "productionRelease").forEach { variant ->
            assertEquals(
                "$variant merged Health permissions must exactly match the requested inventory",
                requestedPermissions,
                healthPermissionsIn(resolveMergedManifest(variant)),
            )
        }
    }

    @Test
    fun rationaleInventoryExactlyDescribesEveryRequestedPermissionInPlainLanguage() = runTest {
        val manager = HealthConnectManager(context)
        val requestedPermissions =
            manager.requestablePermissions() + manager.foodRequestablePermissions()
        val items = HealthPermissionInventory.rationaleItems

        assertEquals(items.size, items.map { it.permission }.distinct().size)
        assertEquals(requestedPermissions, items.map { it.permission }.toSet())
        assertEquals(expectedRationales(), items.associate { it.permission to it.expectedRationale() })
        assertFalse(
            items.any { item ->
                IMPLEMENTATION_JARGON.any { word ->
                    item.label.contains(word, ignoreCase = true) ||
                        item.purpose.contains(word, ignoreCase = true)
                }
            },
        )
    }

    private fun expectedRationales(): Map<String, ExpectedRationale> = mapOf(
        HealthPermission.getReadPermission(StepsRecord::class) to
            ExpectedRationale(
                HealthPermissionAccess.Read,
                "Steps",
                "Show daily movement and progress.",
            ),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class) to
            ExpectedRationale(
                HealthPermissionAccess.Read,
                "Active calories",
                "Show energy burned through activity.",
            ),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class) to
            ExpectedRationale(
                HealthPermissionAccess.Read,
                "Total calories",
                "Show total energy burned during the day.",
            ),
        HealthPermission.getReadPermission(DistanceRecord::class) to
            ExpectedRationale(
                HealthPermissionAccess.Read,
                "Distance",
                "Show distance-based activity progress.",
            ),
        HealthPermission.getReadPermission(SleepSessionRecord::class) to
            ExpectedRationale(
                HealthPermissionAccess.Read,
                "Sleep",
                "Show sleep duration in your health summary.",
            ),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class) to
            ExpectedRationale(
                HealthPermissionAccess.Read,
                "Exercise sessions",
                "Show completed exercise and time spent training.",
            ),
        HealthPermission.getReadPermission(WeightRecord::class) to
            ExpectedRationale(
                HealthPermissionAccess.Read,
                "Weight",
                "Show weight measurements and trends.",
            ),
        HealthPermission.getReadPermission(BodyFatRecord::class) to
            ExpectedRationale(
                HealthPermissionAccess.Read,
                "Body fat",
                "Show body-fat measurements and trends.",
            ),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class) to
            ExpectedRationale(
                HealthPermissionAccess.Read,
                "Resting heart rate",
                "Show resting heart-rate trends.",
            ),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class) to
            ExpectedRationale(
                HealthPermissionAccess.Read,
                "Heart rate variability",
                "Show recovery trends from heart-rate variability.",
            ),
        HealthPermission.getWritePermission(ExerciseSessionRecord::class) to
            ExpectedRationale(
                HealthPermissionAccess.Write,
                "Workouts",
                "Save workouts you log in MusFit.",
            ),
        HealthPermission.getWritePermission(NutritionRecord::class) to
            ExpectedRationale(
                HealthPermissionAccess.Write,
                "Meals and nutrition",
                "Save meals and their nutrition when you choose Food sync.",
            ),
        HealthPermission.getWritePermission(HydrationRecord::class) to
            ExpectedRationale(
                HealthPermissionAccess.Write,
                "Water",
                "Save water totals when you choose Food sync.",
            ),
    )

    private fun HealthPermissionRationaleItem.expectedRationale() = ExpectedRationale(
        access = access,
        label = label,
        purpose = purpose,
    )

    private fun healthPermissionsIn(manifest: File): Set<String> {
        val document = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(manifest)
        val nodes = document.getElementsByTagName("uses-permission")
        return (0 until nodes.length)
            .mapNotNull { index ->
                (nodes.item(index) as? Element)
                    ?.getAttributeNS(ANDROID_NAMESPACE, "name")
                    ?.takeIf { it.startsWith(HEALTH_PERMISSION_PREFIX) }
            }
            .toSet()
    }

    private fun resolveMergedManifest(variant: String): File {
        val taskVariant = variant.replaceFirstChar(Char::uppercaseChar)
        val relativePath =
            "build/intermediates/merged_manifest/$variant/process${taskVariant}MainManifest/AndroidManifest.xml"
        val candidates = listOf(File(relativePath), File("app/$relativePath"), File("../app/$relativePath"))
        return candidates.firstOrNull(File::isFile)
            ?: error("Could not find the $variant merged manifest: ${candidates.joinToString { it.path }}")
    }

    private data class ExpectedRationale(
        val access: HealthPermissionAccess,
        val label: String,
        val purpose: String,
    )

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        const val HEALTH_PERMISSION_PREFIX = "android.permission.health."
        val IMPLEMENTATION_JARGON = setOf("API", "SDK", "record", "database", "payload")
    }
}
