package com.musfit.ui.profile

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileAccessibilityContractTest {
    @Test
    fun singleSelectControlsExposeRadioGroupAndSelectedSemantics() {
        val segments = source("ProfileExpressiveUi.kt")
        val health = source("HealthConnectSettingsUi.kt")

        assertTrue(segments.contains(".selectableGroup()"))
        assertTrue(segments.contains("role = Role.RadioButton"))
        assertTrue(health.contains(".selectableGroup()"))
        assertTrue(health.contains("role = Role.RadioButton"))
        assertTrue(health.contains("&& !state.isHealthConnectSyncing"))
    }

    @Test
    fun editableTilesAssociateLabelsAndProtectPasswordSemantics() {
        val profile = source("ProfileExpressiveUi.kt")
        val account = source("AccountUi.kt")
        val coach = source("AiCoachSettingsUi.kt")

        assertTrue(profile.contains("contentDescription = if (unit.isBlank()) label else \"\$label, \$unit\""))
        assertTrue(account.contains(".semantics { contentDescription = label }"))
        assertTrue(coach.contains("contentDescription = label"))
        assertTrue(coach.contains("if (masked) password()"))
    }

    @Test
    fun textActionsKeepMinimumTouchTargets() {
        assertTrue(source("ProfileExpressiveUi.kt").contains("Modifier.heightIn(min = 48.dp)"))
        assertTrue(source("AiCoachSettingsUi.kt").contains("Modifier.heightIn(min = 48.dp)"))
        assertTrue(source("ProfileEditContent.kt").contains("Modifier.heightIn(min = 48.dp)"))
    }

    private fun source(fileName: String): String {
        val candidates = listOf(
            Path.of("src/main/java/com/musfit/ui/profile/$fileName"),
            Path.of("app/src/main/java/com/musfit/ui/profile/$fileName"),
        )
        return candidates.firstOrNull(Files::exists)?.readText()
            ?: error("Unable to locate profile source: $fileName")
    }
}
