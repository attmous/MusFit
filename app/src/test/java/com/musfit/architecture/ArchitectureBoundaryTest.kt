package com.musfit.architecture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.jar.JarFile

class ArchitectureBoundaryTest {
    @Test
    fun compiledProductionClasses_haveNoForbiddenArchitectureEdges() {
        assertEquals(emptyList<String>(), ArchitectureBoundaryRules.violations(compiledMusFitReferences()))
    }

    @Test
    fun deliberateForbiddenEdges_areRejected() {
        val violations = ArchitectureBoundaryRules.violations(
            mapOf(
                "com/musfit/ui/food/FoodUi" to setOf(
                    "com/musfit/data/remote/food/TransportDto",
                    "com/musfit/ui/profile/ProfileScreen",
                ),
                "com/musfit/integrations/healthconnect/HealthAdapter" to
                    setOf("com/musfit/data/local/entity/WorkoutSessionEntity"),
                "com/musfit/data/repository/Repository" to
                    setOf("com/musfit/integrations/healthconnect/HealthConnectManager"),
            ),
        )

        assertEquals(4, violations.size)
        assertTrue(violations.any { it.contains("UI must not reference remote transport") })
        assertTrue(violations.any { it.contains("Feature UI must not reference another feature UI") })
        assertTrue(violations.any { it.contains("Integration adapters must not reference Room entities") })
        assertTrue(violations.any { it.contains("Data must depend on inward ports, not concrete integrations") })
    }

    private fun compiledMusFitReferences(): Map<String, Set<String>> {
        val protectionDomain = requireNotNull(Class.forName("com.musfit.MainActivity").protectionDomain)
        val location = requireNotNull(protectionDomain.codeSource).location
        val root = File(location.toURI())
        return if (root.isDirectory) {
            root.walkTopDown()
                .filter { file -> file.isFile && file.extension == "class" }
                .filter { file -> file.relativeTo(root).invariantSeparatorsPath.startsWith("com/musfit/") }
                .associate { file ->
                    val className = file.relativeTo(root).invariantSeparatorsPath.removeSuffix(".class")
                    className to classReferences(file.readBytes())
                }
        } else {
            JarFile(root).use { jar ->
                jar.entries().asSequence()
                    .filter { entry -> !entry.isDirectory && entry.name.startsWith("com/musfit/") && entry.name.endsWith(".class") }
                    .associate { entry ->
                        entry.name.removeSuffix(".class") to classReferences(jar.getInputStream(entry).readBytes())
                    }
            }
        }
    }

    private fun classReferences(bytes: ByteArray): Set<String> = INTERNAL_CLASS_NAME.findAll(bytes.toString(StandardCharsets.ISO_8859_1))
        .map { match -> match.value.substringBefore('$') }
        .toSet()

    private companion object {
        val INTERNAL_CLASS_NAME = Regex("com/musfit/[A-Za-z0-9_/$]+")
    }
}

private object ArchitectureBoundaryRules {
    private val featureClassPattern = Regex("com/musfit/ui/(food|profile|today|training)(?:/.*)?")

    fun violations(classes: Map<String, Set<String>>): List<String> = buildList {
        classes.forEach { (className, references) ->
            if (className.startsWith("com/musfit/ui/")) {
                references.filter(::isRemoteTransportReference).forEach { referenced ->
                    add("$className: UI must not reference remote transport: $referenced")
                }

                val sourceFeature = featureClassPattern.matchEntire(className)?.groupValues?.get(1)
                if (sourceFeature != null) {
                    references.forEach { referenced ->
                        val targetFeature = featureClassPattern.matchEntire(referenced)?.groupValues?.get(1)
                        if (targetFeature != null && targetFeature != sourceFeature) {
                            add("$className: Feature UI must not reference another feature UI: $referenced")
                        }
                    }
                }
            }

            if (className.startsWith("com/musfit/integrations/")) {
                references.filter { it.startsWith("com/musfit/data/local/entity/") }.forEach { referenced ->
                    add("$className: Integration adapters must not reference Room entities: $referenced")
                }
            }

            if (className.startsWith("com/musfit/data/")) {
                references.filter { it.startsWith("com/musfit/integrations/") }.forEach { referenced ->
                    add("$className: Data must depend on inward ports, not concrete integrations: $referenced")
                }
            }
        }
    }.distinct().sorted()

    private fun isRemoteTransportReference(className: String): Boolean {
        if (className.startsWith("com/musfit/data/remote/food/")) return true
        if (!className.startsWith("com/musfit/data/remote/")) return false
        return className.substringAfterLast('/').endsWith("Dto") ||
            className.substringAfterLast('/').endsWith("Request") ||
            className.substringAfterLast('/').endsWith("Response")
    }
}
