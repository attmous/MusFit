package com.musfit.ui.training

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TrainingScreen() {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(text = "Training")
        Text(text = "Exercises, routines, active workouts, sets, history, and records appear here.")
    }
}
