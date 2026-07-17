package com.musfit.buildlogic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleGraphRulesTest {
    @Test
    fun intendedModuleGraph_isAccepted() {
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
                    ":integration" to emptySet(),
                    ":integration:healthconnect" to setOf(":core:model"),
                    ":integration:scanner" to emptySet(),
                    ":feature" to emptySet(),
                    ":feature:food" to setOf(
                        ":core:model",
                        ":core:data",
                        ":core:designsystem",
                        ":core:testing",
                        ":integration:scanner",
                    ),
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

    @Test
    fun integrationAdapters_cannotDependOnRoomOrAppPresentation() {
        val violations = ModuleGraphRules.violations(
            mapOf(
                ":integration:healthconnect" to setOf(":core:model", ":core:database"),
                ":integration:scanner" to setOf(":app"),
            ),
        )

        assertEquals(2, violations.size)
        assertTrue(violations.any { it.contains(":integration:healthconnect must not depend on :core:database") })
        assertTrue(violations.any { it.contains(":integration:scanner must not depend on :app") })
    }

    @Test
    fun featureImplementation_cannotDependOnAppOrAnotherFeatureImplementation() {
        val violations = ModuleGraphRules.violations(
            mapOf(
                ":feature:food" to setOf(":core:data", ":app", ":feature:training"),
            ),
        )

        assertEquals(2, violations.size)
        assertTrue(violations.any { it.contains(":feature:food must not depend on :app") })
        assertTrue(violations.any { it.contains(":feature:food must not depend on :feature:training") })
    }
}
