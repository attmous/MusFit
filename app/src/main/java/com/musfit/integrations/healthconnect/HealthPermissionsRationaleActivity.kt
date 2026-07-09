package com.musfit.integrations.healthconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musfit.ui.theme.MusFitTheme

class HealthPermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MusFitTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MusFitTheme.colors.background)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "MusFit Health Connect access",
                        style = MusFitTheme.typography.headlineSmall,
                        color = MusFitTheme.colors.onSurface,
                    )
                    Text(
                        text = "MusFit reads steps, active calories, body weight, and resting heart rate " +
                            "when you grant access. MusFit writes workouts you log in the app. " +
                            "Data stays on this device in the MVP.",
                        style = MusFitTheme.typography.bodyMedium,
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
