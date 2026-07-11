package com.musfit.release

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.musfit.BuildConfig
import com.musfit.ui.permissions.LOCAL_NETWORK_PERMISSION_DENIED_MESSAGE
import com.musfit.ui.permissions.LOCAL_NETWORK_PERMISSION
import com.musfit.ui.permissions.hasLocalNetworkPermission
import com.musfit.ui.permissions.requiresLocalNetworkPermission
import com.musfit.ui.profile.AI_COACH_BASE_URL_PLACEHOLDER
import com.musfit.ui.profile.AI_COACH_ENDPOINT_POLICY_NOTE
import com.musfit.ui.profile.AI_COACH_LOCAL_AGENT_SUMMARY
import com.musfit.ui.profile.HERMES_DEFAULT_BASE_URL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LocalNetworkVariantContractTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun localNetworkPermissionSurfaceExistsOnlyInTheInternalVariant() {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", 37)
        if (BuildConfig.APPLICATION_ID == INTERNAL_APPLICATION_ID) {
            assertEquals(ACCESS_LOCAL_NETWORK, LOCAL_NETWORK_PERMISSION)
            assertTrue(requiresLocalNetworkPermission("http://192.168.1.8:8080/v1/"))
            assertTrue(requiresLocalNetworkPermission("https://[fd12:3456::1]:8443/v1/"))
            assertFalse(requiresLocalNetworkPermission("http://127.0.0.1:8080/v1/"))
            assertFalse(requiresLocalNetworkPermission("https://api.example.com/v1/"))
            assertFalse(requiresLocalNetworkPermission("http://coach.local/v1/"))
        } else {
            assertEquals(PRODUCTION_APPLICATION_ID, BuildConfig.APPLICATION_ID)
            assertTrue(LOCAL_NETWORK_PERMISSION.isEmpty())
            assertFalse(requiresLocalNetworkPermission("https://192.168.1.8:8443/v1/"))
            assertFalse(requiresLocalNetworkPermission("http://192.168.1.8:8080/v1/"))
            assertTrue(hasLocalNetworkPermission(context))
        }
    }

    @Test
    fun endpointDefaultsAndHelpCopyMatchTheCurrentVariantPolicy() {
        if (BuildConfig.APPLICATION_ID == INTERNAL_APPLICATION_ID) {
            assertEquals("http://10.0.2.2:8080/v1/", HERMES_DEFAULT_BASE_URL)
            assertTrue(AI_COACH_BASE_URL_PLACEHOLDER.startsWith("http://10.0.2.2"))
            assertTrue(AI_COACH_LOCAL_AGENT_SUMMARY.contains("private LAN"))
            assertTrue(AI_COACH_ENDPOINT_POLICY_NOTE.contains("private IP"))
            assertTrue(LOCAL_NETWORK_PERMISSION_DENIED_MESSAGE.contains("Local Network"))
        } else {
            assertTrue(HERMES_DEFAULT_BASE_URL.isBlank())
            assertTrue(AI_COACH_BASE_URL_PLACEHOLDER.startsWith("https://"))
            assertTrue(AI_COACH_LOCAL_AGENT_SUMMARY.contains("HTTPS"))
            assertTrue(AI_COACH_ENDPOINT_POLICY_NOTE.contains("HTTPS"))
            assertFalse(LOCAL_NETWORK_PERMISSION_DENIED_MESSAGE.contains("LAN", ignoreCase = true))
        }
    }

    private companion object {
        const val INTERNAL_APPLICATION_ID = "com.musfit.internal"
        const val PRODUCTION_APPLICATION_ID = "com.musfit"
        const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"
    }
}
