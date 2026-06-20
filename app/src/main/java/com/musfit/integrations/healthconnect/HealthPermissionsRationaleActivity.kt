package com.musfit.integrations.healthconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class HealthPermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(text = "MusFit Health Connect access")
                    Text(
                        text = "MusFit reads steps, active calories, body weight, and heart rate " +
                            "when you grant access. MusFit writes workouts you log in the app. " +
                            "Data stays on this device in the MVP.",
                    )
                }
            }
        }
    }
}
