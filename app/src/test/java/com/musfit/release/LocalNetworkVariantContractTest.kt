package com.musfit.release

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.musfit.BuildConfig
import com.musfit.data.repository.AiCoachProviderKind
import com.musfit.ui.permissions.LOCAL_NETWORK_PERMISSION
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
            assertTrue(requiresLocalNetworkPermission(AiCoachProviderKind.LocalAgent))
        } else {
            assertEquals(PRODUCTION_APPLICATION_ID, BuildConfig.APPLICATION_ID)
            assertTrue(LOCAL_NETWORK_PERMISSION.isEmpty())
            assertFalse(requiresLocalNetworkPermission(AiCoachProviderKind.LocalAgent))
            assertTrue(hasLocalNetworkPermission(context))
        }
    }

    private companion object {
        const val INTERNAL_APPLICATION_ID = "com.musfit.internal"
        const val PRODUCTION_APPLICATION_ID = "com.musfit"
        const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"
    }
}
