package com.musfit.ui.profile

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.musfit.feature.profile.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AiCoachVariantCopyTest {
    @Test
    fun endpointDefaultsAndHelpCopyMatchTheCurrentVariantPolicy() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val endpointPolicyNote = context.getString(R.string.profile_ai_endpoint_policy_note)
        if (HERMES_DEFAULT_BASE_URL.isBlank()) {
            assertTrue(AI_COACH_BASE_URL_PLACEHOLDER.startsWith("https://"))
            assertTrue(endpointPolicyNote.contains("HTTPS"))
        } else {
            assertEquals("http://10.0.2.2:8080/v1/", HERMES_DEFAULT_BASE_URL)
            assertTrue(AI_COACH_BASE_URL_PLACEHOLDER.startsWith("http://10.0.2.2"))
            assertTrue(endpointPolicyNote.contains("private IP"))
        }
    }
}
