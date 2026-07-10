package com.musfit.security

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertFalse
import org.junit.Test
import org.w3c.dom.Element

class SeedSurfaceManifestContractTest {
    @Test
    fun distributedAndProductionManifestsExposeNoSeedComponentOrAction() {
        listOf("debug", "release").forEach(::assertNoSeedSurface)
    }

    private fun assertNoSeedSurface(variant: String) {
        val manifest = resolveMergedManifest(variant)
        val document = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(manifest)

        val componentTags = listOf("activity", "activity-alias", "receiver", "service", "provider")
        val componentNames = componentTags.flatMap { tag ->
            val nodes = document.getElementsByTagName(tag)
            (0 until nodes.length).mapNotNull { index ->
                (nodes.item(index) as? Element)?.androidName()
            }
        }
        val actions = document.getElementsByTagName("action").let { nodes ->
            (0 until nodes.length).mapNotNull { index ->
                (nodes.item(index) as? Element)?.androidName()
            }
        }

        assertFalse(
            "$variant merged manifest must not install the legacy seed receiver: ${manifest.path}",
            componentNames.any { it.endsWith(".debug.MusFitDebugSeedReceiver") },
        )
        assertFalse(
            "$variant merged manifest must not expose the legacy seed action: ${manifest.path}",
            actions.contains(LEGACY_SEED_ACTION),
        )
    }

    private fun resolveMergedManifest(variant: String): File {
        val taskVariant = variant.replaceFirstChar(Char::uppercaseChar)
        val relativePath =
            "build/intermediates/merged_manifest/$variant/process${taskVariant}MainManifest/AndroidManifest.xml"
        val candidates = listOf(File(relativePath), File("app/$relativePath"), File("../app/$relativePath"))
        return candidates.firstOrNull(File::isFile)
            ?: throw IllegalStateException(
                "Could not find the $variant merged manifest. Checked: ${candidates.joinToString { it.path }}",
            )
    }

    private fun Element.androidName(): String? =
        getAttributeNS(ANDROID_NAMESPACE, "name").takeIf(String::isNotBlank)

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        const val LEGACY_SEED_ACTION = "com.musfit.debug.SEED_TEST_DATA"
    }
}
