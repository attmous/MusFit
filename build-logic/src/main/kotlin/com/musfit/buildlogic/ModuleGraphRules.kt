package com.musfit.buildlogic

internal object ModuleGraphRules {
    private val allowedDependencies = mapOf(
        ":core" to emptySet(),
        ":core:model" to emptySet(),
        ":core:database" to emptySet(),
        ":core:designsystem" to setOf(":core:model"),
        ":core:testing" to setOf(":core:model", ":core:designsystem"),
        ":benchmark" to setOf(":app"),
        ":baselineprofile" to setOf(":app"),
    )

    fun violations(edges: Map<String, Set<String>>): List<String> = buildList {
        edges.forEach { (source, targets) ->
            if (source == ":app") return@forEach
            val allowed = allowedDependencies[source]
                ?: run {
                    add("$source is not registered in the MusFit module graph")
                    return@forEach
                }
            targets.filterNot(allowed::contains).forEach { target ->
                add("$source must not depend on $target; allowed: ${allowed.sorted()}")
            }
        }
    }.sorted()
}
