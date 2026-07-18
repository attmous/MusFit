package com.musfit.integrations.healthconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musfit.configureMusFitEdgeToEdge
import com.musfit.ui.theme.MusFitTheme

class HealthPermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureMusFitEdgeToEdge()
        setContent {
            MusFitTheme {
                HealthPermissionsRationaleScreen()
            }
        }
    }
}

@Composable
internal fun HealthPermissionsRationaleScreen(
    items: List<HealthPermissionRationaleItem> = HealthPermissionInventory.rationaleItems,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MusFitTheme.colors.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "MusFit Health Connect access",
            style = MusFitTheme.typography.headlineSmall,
            color = MusFitTheme.colors.onSurface,
        )
        Text(
            text = "You choose each data type in Health Connect. MusFit uses only the access " +
                "you grant, and you can change it at any time.",
            style = MusFitTheme.typography.bodyMedium,
            color = MusFitTheme.colors.onSurfaceVariant,
        )
        HealthPermissionRationaleSection(
            title = "MusFit can read",
            items = items.filter { it.access == HealthPermissionAccess.Read },
        )
        HealthPermissionRationaleSection(
            title = "MusFit can write",
            items = items.filter { it.access == HealthPermissionAccess.Write },
        )
    }
}

@Composable
private fun HealthPermissionRationaleSection(
    title: String,
    items: List<HealthPermissionRationaleItem>,
) {
    Text(
        text = title,
        style = MusFitTheme.typography.titleMedium,
        color = MusFitTheme.colors.onSurface,
    )
    items.forEach { item ->
        Text(
            text = "\u2022 ${item.label}: ${item.purpose}",
            style = MusFitTheme.typography.bodyMedium,
            color = MusFitTheme.colors.onSurfaceVariant,
        )
    }
}
