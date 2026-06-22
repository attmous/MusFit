package com.musfit.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.musfit.ui.food.BarcodeScannerScreen
import com.musfit.ui.food.FoodScreen
import com.musfit.ui.food.NutritionLabelScannerScreen
import com.musfit.ui.profile.ProfileScreen
import com.musfit.ui.profile.ProfileSettingsScreen
import com.musfit.ui.today.TodayScreen
import com.musfit.ui.training.TrainingScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val destinations = AppDestination.entries
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppDestination.Today.route
    var scannedBarcode by rememberSaveable { mutableStateOf<String?>(null) }
    var scannedLabelText by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(AppDestination.Today.route)
                                launchSingleTop = true
                            }
                        },
                        label = { Text(destination.label) },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Today.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(AppDestination.Today.route) {
                TodayScreen(
                    onOpenFood = {
                        navController.navigate(AppDestination.Food.route) {
                            popUpTo(AppDestination.Today.route)
                            launchSingleTop = true
                        }
                    },
                    onOpenTraining = {
                        navController.navigate(AppDestination.Training.route) {
                            popUpTo(AppDestination.Today.route)
                            launchSingleTop = true
                        }
                    },
                    onOpenHealth = {
                        navController.navigate(AppDestination.Profile.route) {
                            popUpTo(AppDestination.Today.route)
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(AppDestination.Food.route) {
                FoodScreen(
                    scannedBarcode = scannedBarcode,
                    onScanClick = { navController.navigate(BARCODE_SCANNER_ROUTE) },
                    onScannedBarcodeConsumed = { scannedBarcode = null },
                    scannedLabelText = scannedLabelText,
                    onLabelScanClick = { navController.navigate(NUTRITION_LABEL_SCANNER_ROUTE) },
                    onScannedLabelConsumed = { scannedLabelText = null },
                )
            }
            composable(AppDestination.Training.route) { TrainingScreen() }
            composable(AppDestination.Profile.route) {
                ProfileScreen(onSettingsClick = { navController.navigate(PROFILE_SETTINGS_ROUTE) })
            }
            composable(PROFILE_SETTINGS_ROUTE) {
                ProfileSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(BARCODE_SCANNER_ROUTE) {
                BarcodeScannerScreen(
                    onBarcodeDetected = { barcode ->
                        if (barcode.isNotBlank()) {
                            scannedBarcode = barcode
                            navController.popBackStack()
                        }
                    },
                )
            }
            composable(NUTRITION_LABEL_SCANNER_ROUTE) {
                NutritionLabelScannerScreen(
                    onLabelCaptured = { text ->
                        if (text.isNotBlank()) {
                            scannedLabelText = text
                            navController.popBackStack()
                        }
                    },
                )
            }
        }
    }
}
