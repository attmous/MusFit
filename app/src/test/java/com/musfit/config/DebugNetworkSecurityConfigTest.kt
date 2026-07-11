package com.musfit.config

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugNetworkSecurityConfigTest {
    @Test
    fun internalNetworkSecurityConfig_allowsLanCleartextForLocalAgents() {
        val config = resolveDebugNetworkSecurityConfig().readText()

        assertTrue(
            "Internal builds retain the existing LAN HTTP policy until W1-SEC-02 narrows it.",
            config.contains("""<base-config cleartextTrafficPermitted="true">"""),
        )
    }

    private fun resolveDebugNetworkSecurityConfig(): File {
        val relativePath = "src/internal/res/xml/debug_network_security_config.xml"
        val candidates = listOf(File(relativePath), File("app/$relativePath"), File("../app/$relativePath"))
        return candidates.firstOrNull(File::exists)
            ?: throw IllegalStateException("Could not find internal network security config.")
    }
}
