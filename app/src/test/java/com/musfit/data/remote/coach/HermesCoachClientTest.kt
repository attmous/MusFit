package com.musfit.data.remote.coach

import com.musfit.data.repository.AiCoachConnection
import com.musfit.data.repository.AiCoachProviderKind
import com.musfit.data.repository.LocalAgentKind
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HermesCoachClientTest {
    @Test
    fun shouldPreferLocalNetwork_forPrivateLanHermesEndpoint() {
        val connection = hermesConnection("http://192.168.178.113:8080/v1/")

        assertTrue(connection.shouldPreferLocalNetwork())
    }

    @Test
    fun shouldPreferLocalNetwork_forLocalHostName() {
        val connection = hermesConnection("http://rock-5-itx:8080/v1/")

        assertTrue(connection.shouldPreferLocalNetwork())
    }

    @Test
    fun shouldPreferLocalNetwork_ignoresLoopbackEndpoint() {
        val connection = hermesConnection("http://127.0.0.1:8080/v1/")

        assertFalse(connection.shouldPreferLocalNetwork())
    }

    @Test
    fun shouldPreferLocalNetwork_ignoresPublicApiCompatibleEndpoint() {
        val connection = hermesConnection(
            baseUrl = "https://api.example.com/v1/",
            providerKind = AiCoachProviderKind.OpenAiCompatible,
            localAgentKind = LocalAgentKind.Custom,
        )

        assertFalse(connection.shouldPreferLocalNetwork())
    }

    private fun hermesConnection(
        baseUrl: String,
        providerKind: AiCoachProviderKind = AiCoachProviderKind.LocalAgent,
        localAgentKind: LocalAgentKind = LocalAgentKind.HermesAgent,
    ): AiCoachConnection =
        AiCoachConnection(
            providerKind = providerKind,
            baseUrl = baseUrl,
            modelName = "hermes-agent",
            localAgentKind = localAgentKind,
            apiKey = "test-key",
        )
}
