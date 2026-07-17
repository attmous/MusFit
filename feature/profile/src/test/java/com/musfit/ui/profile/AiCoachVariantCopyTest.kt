package com.musfit.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiCoachVariantCopyTest {
    @Test
    fun endpointDefaultsAndHelpCopyMatchTheCurrentVariantPolicy() {
        if (HERMES_DEFAULT_BASE_URL.isBlank()) {
            assertTrue(AI_COACH_BASE_URL_PLACEHOLDER.startsWith("https://"))
            assertTrue(AI_COACH_LOCAL_AGENT_SUMMARY.contains("HTTPS"))
            assertTrue(AI_COACH_ENDPOINT_POLICY_NOTE.contains("HTTPS"))
        } else {
            assertEquals("http://10.0.2.2:8080/v1/", HERMES_DEFAULT_BASE_URL)
            assertTrue(AI_COACH_BASE_URL_PLACEHOLDER.startsWith("http://10.0.2.2"))
            assertTrue(AI_COACH_LOCAL_AGENT_SUMMARY.contains("private LAN"))
            assertTrue(AI_COACH_ENDPOINT_POLICY_NOTE.contains("private IP"))
        }
    }
}
