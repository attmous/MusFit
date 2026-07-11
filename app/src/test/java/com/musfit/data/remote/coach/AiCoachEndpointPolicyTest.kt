package com.musfit.data.remote.coach

import com.musfit.BuildConfig
import java.net.URI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import okhttp3.Request

class AiCoachEndpointPolicyTest {
    @Test
    fun httpsIsAcceptedForPublicPrivateLoopbackIpv4AndIpv6InEveryVariant() {
        val urls = listOf(
            "https://api.example.com/v1/",
            "https://localhost:8443/v1/",
            "https://127.0.0.1:8443/v1/",
            "https://10.0.2.2:8443/v1/",
            "https://192.168.1.8:8443/v1/",
            "https://[::1]:8443/v1/",
            "https://[fd12:3456:789a::1]:8443/v1/",
            "https://[2001:db8::1]:8443/v1/",
        )

        urls.forEach { url ->
            val endpoint = AiCoachEndpointPolicy.requireAllowed(url)
            assertEquals(url, endpoint.normalizedBaseUrl)
        }
    }

    @Test
    fun httpPolicyMatchesTheExactVariantMatrix() {
        val internalAllowed = listOf(
            "http://localhost:8080/v1/" to AiCoachEndpointRoute.Loopback,
            "http://127.0.0.1:8080/v1/" to AiCoachEndpointRoute.Loopback,
            "http://127.255.255.254:8080/v1/" to AiCoachEndpointRoute.Loopback,
            "http://10.0.0.0:8080/v1/" to AiCoachEndpointRoute.PrivateLan,
            "http://10.255.255.255:8080/v1/" to AiCoachEndpointRoute.PrivateLan,
            "http://172.16.0.0:8080/v1/" to AiCoachEndpointRoute.PrivateLan,
            "http://172.31.255.255:8080/v1/" to AiCoachEndpointRoute.PrivateLan,
            "http://192.168.0.0:8080/v1/" to AiCoachEndpointRoute.PrivateLan,
            "http://192.168.255.255:8080/v1/" to AiCoachEndpointRoute.PrivateLan,
            "http://[::1]:8080/v1/" to AiCoachEndpointRoute.Loopback,
            "http://[fc00::]:8080/v1/" to AiCoachEndpointRoute.PrivateLan,
            "http://[fdff:ffff:ffff:ffff:ffff:ffff:ffff:ffff]:8080/v1/" to
                AiCoachEndpointRoute.PrivateLan,
        )

        internalAllowed.forEach { (url, expectedRoute) ->
            if (isInternal) {
                assertEquals(expectedRoute, AiCoachEndpointPolicy.requireAllowed(url).route)
            } else {
                assertRejected(url)
            }
        }
    }

    @Test
    fun httpRejectsPublicAdjacentReservedLinkLocalAndMulticastAddresses() {
        val rejected = listOf(
            "http://9.255.255.255/v1/",
            "http://11.0.0.0/v1/",
            "http://172.15.255.255/v1/",
            "http://172.32.0.0/v1/",
            "http://192.167.255.255/v1/",
            "http://192.169.0.0/v1/",
            "http://0.0.0.0/v1/",
            "http://169.254.1.1/v1/",
            "http://224.0.0.1/v1/",
            "http://255.255.255.255/v1/",
            "http://[::]/v1/",
            "http://[fe80::1]/v1/",
            "http://[ff02::1]/v1/",
            "http://[2001:db8::1]/v1/",
            "http://[::ffff:192.168.1.8]/v1/",
            "http://[::ffff:8.8.8.8]/v1/",
        )

        rejected.forEach(::assertRejected)
    }

    @Test
    fun httpRejectsDnsAndObfuscatedHostConfusionWithoutResolution() {
        val rejected = listOf(
            "http://example.com/v1/",
            "http://rock-5-itx/v1/",
            "http://coach.local/v1/",
            "http://coach.lan/v1/",
            "http://coach.home/v1/",
            "http://coach.fritz.box/v1/",
            "http://localhost.evil/v1/",
            "http://127.0.0.1.evil/v1/",
            "http://2130706433/v1/",
            "http://0x7f000001/v1/",
            "http://0177.0.0.1/v1/",
            "http://127.1/v1/",
            "http://127.0.1/v1/",
            "http://[fe80::1%25wlan0]/v1/",
        )

        rejected.forEach(::assertRejected)
    }

    @Test
    fun authorityAndUrlConfusionAreRejectedBeforeNormalization() {
        val rejected = listOf(
            "https://user:pass@api.example.com/v1/",
            "http://localhost@evil.example/v1/",
            "http://127.0.0.1#@evil.example/v1/",
            "http://127.0.0.1?next=http://evil.example/v1/",
            "http:///v1/",
            "http://[::1/v1/",
            "ftp://127.0.0.1/v1/",
            "not a url",
        )

        rejected.forEach(::assertRejected)
    }

    @Test
    fun routingAndPermissionClassificationUseTheValidatedUrlNotProviderLabels() {
        assertFalse(AiCoachEndpointPolicy.requiresPrivateLanRouting("https://api.example.com/v1/"))
        assertFalse(AiCoachEndpointPolicy.requiresPrivateLanRouting("https://localhost:8443/v1/"))
        assertEquals(
            isInternal,
            AiCoachEndpointPolicy.requiresPrivateLanRouting("https://192.168.1.8:8443/v1/"),
        )
        assertFalse(AiCoachEndpointPolicy.requiresPrivateLanRouting("http://coach.local/v1/"))
    }

    @Test
    fun normalizedBaseUrlIsCanonicalAndRetainsOnlyTheBasePath() {
        val endpoint = AiCoachEndpointPolicy.requireAllowed("  HTTPS://API.EXAMPLE.COM/v1  ")

        assertEquals("https://api.example.com/v1/", endpoint.normalizedBaseUrl)
        assertEquals("https://api.example.com/v1/models", endpoint.resolve("models").toString())
    }

    @Test
    fun requestSetupMeasurement_remainsPureAndBounded() {
        val urls = listOf(
            "https://127.0.0.1:8443/v1/",
            "https://192.168.1.8:8443/v1/",
            "https://api.example.com/v1/",
        )
        repeat(5) {
            runLegacySetupBatch(urls)
            runSetupBatch(urls)
        }
        val legacySamples = LongArray(51)
        val securedSamples = LongArray(51)
        legacySamples.indices.forEach { sample ->
            if (sample % 2 == 0) {
                legacySamples[sample] = timed { runLegacySetupBatch(urls) }
                securedSamples[sample] = timed { runSetupBatch(urls) }
            } else {
                securedSamples[sample] = timed { runSetupBatch(urls) }
                legacySamples[sample] = timed { runLegacySetupBatch(urls) }
            }
        }
        val legacy = legacySamples.sorted()
        val secured = securedSamples.sorted()
        val legacyMedian = legacy[legacy.size / 2]
        val legacyP90 = legacy[(legacy.size * 9) / 10]
        val securedMedian = secured[secured.size / 2]
        val securedP90 = secured[(secured.size * 9) / 10]
        val securedOverheadP90 = (securedP90 - legacyP90).coerceAtLeast(0L)
        val securedOverheadPerRequestNs = securedOverheadP90 / 10_000.0

        println(
            "W1-SEC-02 full-request-setup 10000-setups " +
                "legacyMedianNs=$legacyMedian legacyP90Ns=$legacyP90 " +
                "securedMedianNs=$securedMedian securedP90Ns=$securedP90 " +
                "securedOverheadPerRequestNs=$securedOverheadPerRequestNs",
        )
        // Shared hosted runners add enough jitter that a small legacy-vs-secured delta is not a stable CI gate.
        // Keep the comparative metrics above, and reject only pathological setup cost: less than 25 us per
        // validation, or less than 75 us when extrapolated across all three defense-in-depth validation points.
        assertTrue(
            "Pure request setup unexpectedly exceeded 250 ms for 10,000 calls: $securedP90 ns",
            securedP90 < 250_000_000L,
        )
    }

    private fun runSetupBatch(urls: List<String>) {
        repeat(10_000) { index ->
            val endpoint = AiCoachEndpointPolicy.requireAllowed(urls[index % urls.size])
            Request.Builder()
                .url(endpoint.resolve("models"))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer dummy-measurement-key")
                .build()
        }
    }

    private fun runLegacySetupBatch(urls: List<String>) {
        repeat(10_000) { index ->
            val baseUrl = urls[index % urls.size]
            val host = URI(baseUrl).host.orEmpty().trim('[', ']')
            host.isLegacyLocalNetworkHost()
            Request.Builder()
                .url(baseUrl.trimEnd('/') + "/models")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer dummy-measurement-key")
                .build()
        }
    }

    private fun timed(block: () -> Unit): Long {
        val started = System.nanoTime()
        block()
        return System.nanoTime() - started
    }

    private fun String.isLegacyLocalNetworkHost(): Boolean {
        val host = lowercase()
        if (host == "localhost" || host == "127.0.0.1" || host == "::1") return false
        if (host.endsWith(".local") || host.endsWith(".lan") || host.endsWith(".home") || host.endsWith(".fritz.box")) {
            return true
        }
        if (!host.contains('.')) return true
        if (host.contains(':')) return host.startsWith("fc") || host.startsWith("fd") || host.startsWith("fe80")
        val octets = host.split('.').map { it.toIntOrNull() ?: return false }
        if (octets.size != 4 || octets.any { it !in 0..255 }) return false
        return octets[0] == 10 ||
            (octets[0] == 172 && octets[1] in 16..31) ||
            (octets[0] == 192 && octets[1] == 168) ||
            (octets[0] == 169 && octets[1] == 254)
    }

    private fun assertRejected(url: String) {
        try {
            AiCoachEndpointPolicy.requireAllowed(url)
            fail("Expected endpoint to be rejected: $url")
        } catch (_: IllegalArgumentException) {
            // Expected: the pure policy rejects before DNS or request setup.
        }
    }

    private companion object {
        val isInternal: Boolean = BuildConfig.APPLICATION_ID == "com.musfit.internal"
    }
}
