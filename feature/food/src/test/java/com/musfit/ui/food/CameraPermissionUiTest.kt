package com.musfit.ui.food

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraPermissionUiTest {
    @Test
    fun requestableDenialOffersPermissionRequest() {
        assertEquals(CameraPermissionAction.RequestPermission, cameraPermissionAction(permanentlyDenied = false))
    }

    @Test
    fun permanentDenialOffersAppSettingsRecovery() {
        assertEquals(CameraPermissionAction.OpenAppSettings, cameraPermissionAction(permanentlyDenied = true))
    }
}
