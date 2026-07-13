package com.musfit.testing

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

class SourceTextBehaviorPolicyTest {
    @Test
    fun testsDoNotInspectProductionKotlinSource() {
        val testRoot = resolveTestRoot()
        val productionSourceMarker = listOf("src", "main", "java").joinToString("/")
        val offenders = Files.walk(testRoot).use { paths ->
            paths
                .filter { it.isRegularFile() && it.extension == "kt" }
                .filter { it.readText().contains(productionSourceMarker) }
                .map { testRoot.relativize(it).toString().replace('\\', '/') }
                .sorted()
                .toList()
        }

        assertTrue(
            "UI and behavior tests must execute compiled code instead of reading production source: $offenders",
            offenders.isEmpty(),
        )
    }

    private fun resolveTestRoot(): Path {
        val candidates = listOf(Path.of("src"), Path.of("app/src"))
        return candidates.firstOrNull(Files::isDirectory)
            ?: error("Unable to locate app test source root")
    }
}
