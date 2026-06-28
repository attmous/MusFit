package com.musfit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.musfit.ui.food.BarcodeScannerScreen
import com.musfit.ui.food.FoodScreen
import com.musfit.ui.food.NutritionLabelScannerScreen
import com.musfit.ui.profile.ProfileScreen
import com.musfit.ui.profile.ProfileSettingsScreen
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import com.musfit.ui.theme.tabAccentFor
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

    fun go(route: String) {
        navController.navigate(route) {
            popUpTo(AppDestination.Today.route)
            launchSingleTop = true
        }
    }

    Scaffold(
        containerColor = MusFitTheme.colors.background,
        bottomBar = {
            FloatingPillNav(
                destinations = destinations,
                currentRoute = currentRoute,
                onSelect = { go(it.route) },
                onFab = { go(AppDestination.Food.route) },
            )
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Today.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(AppDestination.Today.route) {
                TodayScreen(
                    onOpenFood = { go(AppDestination.Food.route) },
                    onOpenTraining = { go(AppDestination.Training.route) },
                    onOpenHealth = { go(AppDestination.Profile.route) },
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

/** M3E-style floating bottom nav: a rounded pill of destinations + a separate rounded-square FAB. */
@Composable
private fun FloatingPillNav(
    destinations: List<AppDestination>,
    currentRoute: String,
    onSelect: (AppDestination) -> Unit,
    onFab: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            color = MusFitTheme.colors.surface,
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 4.dp,
            modifier = Modifier.weight(1f),
        ) {
            Row(
                modifier = Modifier.padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                destinations.forEach { destination ->
                    NavPillItem(
                        destination = destination,
                        selected = currentRoute == destination.route,
                        accent = tabAccentFor(destination),
                        onClick = { onSelect(destination) },
                    )
                }
            }
        }
        val fab = tabAccentFor(AppDestination.Today)
        FabSquare(color = fab.color, contentColor = fab.onColor, onClick = onFab)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RowScope.NavPillItem(
    destination: AppDestination,
    selected: Boolean,
    accent: TabAccent,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = if (selected) accent.container else Color.Transparent,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.weight(1f),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            Icon(
                destination.icon,
                contentDescription = destination.label,
                tint = if (selected) accent.onContainer else MusFitTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = destination.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) accent.color else MusFitTheme.colors.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FabSquare(color: Color, contentColor: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = color,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 4.dp,
        modifier = Modifier.size(56.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = "Add food",
                tint = contentColor,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}
