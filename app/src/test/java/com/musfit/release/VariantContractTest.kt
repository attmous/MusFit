package com.musfit.release

import com.musfit.BuildConfig
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class VariantContractTest {
    @Test
    fun currentVariantHasOneOfTheTwoApprovedInstallIdentities() {
        assertTrue(
            "Only the side-by-side internal identity and production identity are supported",
            BuildConfig.APPLICATION_ID == INTERNAL_APPLICATION_ID ||
                BuildConfig.APPLICATION_ID == PRODUCTION_APPLICATION_ID,
        )

        if (BuildConfig.APPLICATION_ID == INTERNAL_APPLICATION_ID) {
            assertTrue("The internal variant must remain debuggable", BuildConfig.DEBUG)
        } else {
            assertFalse("The production-shaped variant must be non-debuggable", BuildConfig.DEBUG)
            assertTrue(
                "The production-shaped variant must not compile a developer Hermes base URL",
                BuildConfig.DEBUG_HERMES_BASE_URL.isBlank(),
            )
            assertTrue(
                "The production-shaped variant must not compile a developer Hermes model",
                BuildConfig.DEBUG_HERMES_MODEL_NAME.isBlank(),
            )
            assertTrue(
                "The production-shaped variant must not compile a developer Hermes credential",
                BuildConfig.DEBUG_HERMES_API_KEY.isBlank(),
            )
        }
    }

    @Test
    fun mergedManifestsSeparateInternalAndProductionSurfaces() {
        val internal = manifestContract("internalDebug")
        val production = manifestContract("productionRelease")

        assertEquals(INTERNAL_APPLICATION_ID, internal.applicationId)
        assertTrue(internal.debuggable)
        assertTrue(internal.permissions.contains(LOCAL_NETWORK_PERMISSION))
        assertEquals(DEBUG_NETWORK_CONFIG, internal.networkSecurityConfig)
        assertTrue(internal.hasUsesCleartextTrafficAttribute)
        assertTrue(internal.usesCleartextTraffic)

        assertEquals(PRODUCTION_APPLICATION_ID, production.applicationId)
        assertFalse(production.debuggable)
        assertFalse(production.permissions.contains(LOCAL_NETWORK_PERMISSION))
        assertTrue(production.networkSecurityConfig.isBlank())
        assertTrue("Production must explicitly disable platform cleartext", production.hasUsesCleartextTrafficAttribute)
        assertFalse(production.usesCleartextTraffic)
    }

    @Test
    fun destructiveSeedInstrumentationTargetsOnlyTheInternalIdentity() {
        val manifest = resolveInstrumentationManifest()
        val document = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(manifest)
        val instrumentation = document.getElementsByTagName("instrumentation").item(0) as Element

        assertEquals(INTERNAL_TEST_APPLICATION_ID, document.documentElement.getAttribute("package"))
        assertEquals(INTERNAL_APPLICATION_ID, instrumentation.androidAttribute("targetPackage"))
        assertEquals(
            "androidx.test.runner.AndroidJUnitRunner",
            instrumentation.androidAttribute("name"),
        )
    }

    private fun manifestContract(variant: String): ManifestContract {
        val manifest = resolveMergedManifest(variant)
        val document = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(manifest)
        val root = document.documentElement
        val application = document.getElementsByTagName("application").item(0) as Element
        val permissionNodes = document.getElementsByTagName("uses-permission")

        return ManifestContract(
            applicationId = root.getAttribute("package"),
            debuggable = application.androidAttribute("debuggable").toBoolean(),
            networkSecurityConfig = application.androidAttribute("networkSecurityConfig"),
            hasUsesCleartextTrafficAttribute = application.hasAttributeNS(
                ANDROID_NAMESPACE,
                "usesCleartextTraffic",
            ),
            usesCleartextTraffic = application.androidAttribute("usesCleartextTraffic").toBoolean(),
            permissions = (0 until permissionNodes.length)
                .mapNotNull { index ->
                    (permissionNodes.item(index) as? Element)
                        ?.androidAttribute("name")
                        ?.takeIf(String::isNotBlank)
                }
                .toSet(),
        )
    }

    private fun resolveMergedManifest(variant: String): File {
        val taskVariant = variant.replaceFirstChar(Char::uppercaseChar)
        val relativePath =
            "build/intermediates/merged_manifest/$variant/process${taskVariant}MainManifest/AndroidManifest.xml"
        val candidates = listOf(File(relativePath), File("app/$relativePath"), File("../app/$relativePath"))
        return candidates.firstOrNull(File::isFile)
            ?: error("Could not find the $variant merged manifest: ${candidates.joinToString { it.path }}")
    }

    private fun resolveInstrumentationManifest(): File {
        val relativePath =
            "build/intermediates/packaged_manifests/internalDebugAndroidTest/" +
                "processInternalDebugAndroidTestManifest/AndroidManifest.xml"
        val candidates = listOf(File(relativePath), File("app/$relativePath"), File("../app/$relativePath"))
        return candidates.firstOrNull(File::isFile)
            ?: error("Could not find the internal seed instrumentation manifest: ${candidates.joinToString { it.path }}")
    }

    private fun Element.androidAttribute(name: String): String =
        getAttributeNS(ANDROID_NAMESPACE, name)

    private data class ManifestContract(
        val applicationId: String,
        val debuggable: Boolean,
        val networkSecurityConfig: String,
        val hasUsesCleartextTrafficAttribute: Boolean,
        val usesCleartextTraffic: Boolean,
        val permissions: Set<String>,
    )

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        const val INTERNAL_APPLICATION_ID = "com.musfit.internal"
        const val INTERNAL_TEST_APPLICATION_ID = "com.musfit.internal.test"
        const val PRODUCTION_APPLICATION_ID = "com.musfit"
        const val LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"
        const val DEBUG_NETWORK_CONFIG = "@xml/debug_network_security_config"
    }
}
