package com.musfit.ui.food

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FoodScreen() {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(text = "Food")
        Text(text = "Manual food entry, saved foods, meals, and barcode lookup appear here.")
    }
}
