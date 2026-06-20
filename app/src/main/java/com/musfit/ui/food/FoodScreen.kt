package com.musfit.ui.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun FoodScreen(viewModel: FoodViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Food",
            style = MaterialTheme.typography.headlineSmall,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.barcode,
                onValueChange = viewModel::onBarcodeChanged,
                label = { Text("Barcode") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )

            Button(
                onClick = viewModel::lookupBarcode,
                enabled = !state.isLoading,
                modifier = Modifier.width(112.dp),
            ) {
                Text(if (state.isLoading) "Loading" else "Lookup")
            }
        }

        state.message?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        OutlinedTextField(
            value = state.productName,
            onValueChange = viewModel::onProductNameChanged,
            label = { Text("Product name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.brand,
            onValueChange = viewModel::onBrandChanged,
            label = { Text("Brand") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = "Per 100 g",
            style = MaterialTheme.typography.titleMedium,
        )

        NutritionField(
            label = "Calories",
            value = state.caloriesPer100g,
            onValueChange = viewModel::onCaloriesChanged,
        )
        NutritionField(
            label = "Protein (g)",
            value = state.proteinPer100g,
            onValueChange = viewModel::onProteinChanged,
        )
        NutritionField(
            label = "Carbs (g)",
            value = state.carbsPer100g,
            onValueChange = viewModel::onCarbsChanged,
        )
        NutritionField(
            label = "Fat (g)",
            value = state.fatPer100g,
            onValueChange = viewModel::onFatChanged,
        )

        Button(
            onClick = viewModel::saveProduct,
            enabled = state.lookupResult != null && !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save food")
        }
    }
}

@Composable
private fun NutritionField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}
