package com.musfit.security

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedHelperContractTest {
    @Test
    fun helperUsesNonDistributedInstrumentationInsteadOfAppBroadcast() {
        val helper = resolveRepoFile("scripts/android/install-seed-musfit.ps1").readText()
        val debugManifest = resolveRepoFile("app/src/debug/AndroidManifest.xml").readText()

        assertTrue(helper.contains("assembleDebugAndroidTest"))
        assertTrue(helper.contains("app-debug-androidTest.apk"))
        assertTrue(helper.contains("am instrument"))
        assertTrue(helper.contains("com.musfit.test/androidx.test.runner.AndroidJUnitRunner"))
        assertFalse(helper.contains("am broadcast"))
        assertFalse(helper.contains("MusFitDebugSeedReceiver"))
        assertFalse(helper.contains("com.musfit.debug.SEED_TEST_DATA"))
        assertFalse(debugManifest.contains("<receiver"))
        assertFalse(debugManifest.contains("com.musfit.debug.SEED_TEST_DATA"))
    }

    @Test
    fun ciDistributesOnlyTheTargetApkNotTheInstrumentationApk() {
        val workflow = resolveRepoFile(".github/workflows/android.yml").readText()

        assertTrue(workflow.contains("path: app/build/outputs/apk/debug/app-debug.apk"))
        assertFalse(workflow.contains("app-debug-androidTest.apk"))
        assertFalse(workflow.contains("outputs/apk/androidTest"))
    }

    private fun resolveRepoFile(relativePath: String): File {
        val candidates = listOf(File(relativePath), File("../$relativePath"), File("../../$relativePath"))
        return candidates.firstOrNull(File::isFile)
            ?: throw IllegalStateException(
                "Could not find $relativePath. Checked: ${candidates.joinToString { it.path }}",
            )
    }
}
