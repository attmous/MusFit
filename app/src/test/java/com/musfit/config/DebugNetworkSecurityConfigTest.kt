package com.musfit.config

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugNetworkSecurityConfigTest {
    @Test
    fun debugNetworkSecurityConfig_allowsLanCleartextForLocalAgents() {
        val config = resolveDebugNetworkSecurityConfig().readText()

        assertTrue(
            "Debug builds must allow HTTP LAN endpoints such as Radxa Hermes at 192.168.x.x.",
            config.contains("""<base-config cleartextTrafficPermitted="true">"""),
        )
    }

    private fun resolveDebugNetworkSecurityConfig(): File {
        val relativePath = "src/debug/res/xml/debug_network_security_config.xml"
        val candidates = listOf(File(relativePath), File("app/$relativePath"), File("../app/$relativePath"))
        return candidates.firstOrNull(File::exists)
            ?: throw IllegalStateException("Could not find debug network security config.")
    }
}
