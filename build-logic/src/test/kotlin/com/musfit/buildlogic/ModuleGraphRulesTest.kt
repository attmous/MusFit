package com.musfit.buildlogic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleGraphRulesTest {
    @Test
    fun intendedCoreGraph_isAccepted() {
        assertEquals(
            emptyList<String>(),
            ModuleGraphRules.violations(
                mapOf(
                    ":app" to setOf(":core:model", ":core:designsystem"),
                    ":core" to emptySet(),
                    ":core:model" to emptySet(),
                    ":core:database" to emptySet(),
                    ":core:network" to setOf(":core:model"),
                    ":core:data" to setOf(":core:model", ":core:database", ":core:network"),
                    ":core:designsystem" to setOf(":core:model"),
                    ":core:testing" to setOf(":core:model", ":core:designsystem"),
                ),
            ),
        )
    }

    @Test
    fun deliberateForbiddenGraph_isRejected() {
        val violations = ModuleGraphRules.violations(
            mapOf(
                ":core:model" to setOf(":app"),
                ":core:designsystem" to setOf(":app"),
                ":unregistered" to emptySet(),
            ),
        )

        assertEquals(3, violations.size)
        assertTrue(violations.any { it.contains(":core:model must not depend on :app") })
        assertTrue(violations.any { it.contains(":core:designsystem must not depend on :app") })
        assertTrue(violations.any { it.contains(":unregistered is not registered") })
    }
}
