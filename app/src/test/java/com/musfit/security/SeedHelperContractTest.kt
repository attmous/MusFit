package com.musfit.security

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedHelperContractTest {
    @Test
    fun helperUsesNonDistributedInstrumentationInsteadOfAppBroadcast() {
        val helper = resolveRepoFile("scripts/android/install-seed-musfit.ps1").readText()
        val internalManifest = resolveRepoFile("app/src/internal/AndroidManifest.xml").readText()

        assertTrue(helper.contains("assembleInternalDebugAndroidTest"))
        assertTrue(helper.contains("app-internal-debug-androidTest.apk"))
        assertTrue(helper.contains("am instrument"))
        assertTrue(helper.contains("\$testPackage = \"com.musfit.internal.test\""))
        assertTrue(helper.contains("\$testPackage/com.musfit.test.MusFitAndroidJUnitRunner"))
        assertTrue(helper.contains("\$mainComponent = \"com.musfit.internal/com.musfit.MainActivity\""))
        assertTrue(helper.contains("am start -W -n \$mainComponent"))
        assertFalse(helper.contains("am broadcast"))
        assertFalse(helper.contains("shell monkey"))
        assertFalse(helper.contains("MusFitDebugSeedReceiver"))
        assertFalse(helper.contains("com.musfit.debug.SEED_TEST_DATA"))
        assertFalse(internalManifest.contains("<receiver"))
        assertFalse(internalManifest.contains("com.musfit.debug.SEED_TEST_DATA"))
    }

    @Test
    fun ciRetainsOnlyAnInternalVerificationApkAndPublishesNoRelease() {
        val workflow = resolveRepoFile(".github/workflows/android.yml").readText()

        assertTrue(workflow.contains("path: app/build/outputs/apk/internal/debug/app-internal-debug.apk"))
        assertFalse(workflow.contains("app-internal-debug-androidTest.apk"))
        assertFalse(workflow.contains("outputs/apk/androidTest"))
        assertFalse(workflow.contains("softprops/action-gh-release"))
        assertFalse(workflow.contains("Publish GitHub Release"))
    }

    private fun resolveRepoFile(relativePath: String): File {
        val candidates = listOf(File(relativePath), File("../$relativePath"), File("../../$relativePath"))
        return candidates.firstOrNull(File::isFile)
            ?: throw IllegalStateException(
                "Could not find $relativePath. Checked: ${candidates.joinToString { it.path }}",
            )
    }
}
