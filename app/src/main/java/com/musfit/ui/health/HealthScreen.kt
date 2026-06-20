package com.musfit.ui.health

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HealthScreen() {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(text = "Health")
        Text(text = "Health Connect availability, permissions, sync, and imported metrics appear here.")
    }
}
