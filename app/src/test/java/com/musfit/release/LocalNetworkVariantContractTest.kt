package com.musfit.release

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.musfit.BuildConfig
import com.musfit.ui.permissions.LOCAL_NETWORK_PERMISSION
import com.musfit.ui.permissions.LOCAL_NETWORK_PERMISSION_DENIED_MESSAGE
import com.musfit.ui.permissions.hasLocalNetworkPermission
import com.musfit.ui.permissions.requiresLocalNetworkPermission
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
    fun permissionHelpCopyMatchesTheCurrentVariantPolicy() {
        if (BuildConfig.APPLICATION_ID == INTERNAL_APPLICATION_ID) {
            assertTrue(LOCAL_NETWORK_PERMISSION_DENIED_MESSAGE.contains("Local Network"))
        } else {
            assertFalse(LOCAL_NETWORK_PERMISSION_DENIED_MESSAGE.contains("LAN", ignoreCase = true))
        }
    }

    private companion object {
        const val INTERNAL_APPLICATION_ID = "com.musfit.internal"
        const val PRODUCTION_APPLICATION_ID = "com.musfit"
        const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"
    }
}
