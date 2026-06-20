package com.musfit.ui.today

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TodayScreen() {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(text = "Today")
        Text(text = "Calories, macros, meals, workouts, and Health Connect metrics appear here.")
    }
}
