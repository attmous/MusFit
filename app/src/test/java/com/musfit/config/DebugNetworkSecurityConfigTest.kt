package com.musfit.config

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DebugNetworkSecurityConfigTest {
    @Test
    fun internalNetworkSecurityConfig_defersCidrsToTheAppPolicyAndTrustsOnlySystemCas() {
        val config = resolveDebugNetworkSecurityConfig().readText()

        assertTrue(
            "Android network-security XML cannot express IP CIDRs; the request-boundary policy is the actual host gate.",
            config.contains("""cleartextTrafficPermitted="true"""),
        )
        assertTrue(config.contains("""tools:ignore="InsecureBaseConfiguration"""))
        assertTrue(config.contains("""<certificates src="system" />"""))
        assertFalse(config.contains("""<certificates src="user"""))
        assertFalse(config.contains("overridePins"))
    }

    private fun resolveDebugNetworkSecurityConfig(): File {
        val relativePath = "src/internal/res/xml/debug_network_security_config.xml"
        val candidates = listOf(File(relativePath), File("app/$relativePath"), File("../app/$relativePath"))
        return candidates.firstOrNull(File::exists)
            ?: throw IllegalStateException("Could not find internal network security config.")
    }
}
