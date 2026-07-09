package com.musfit.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musfit.ui.theme.MusFitMotion
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.musfit.ui.food.BarcodeScannerScreen
import com.musfit.ui.food.FoodScreen
import com.musfit.ui.food.NutritionLabelScannerScreen
import com.musfit.ui.food.NutritionTrendsScreen
import com.musfit.ui.profile.ProfileScreen
import com.musfit.ui.profile.ProfileSettingsScreen
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import com.musfit.ui.theme.tabAccentFor
import com.musfit.ui.today.ChatPreviewFab
import com.musfit.ui.today.ChatPreviewSheet
import com.musfit.ui.today.TodayScreen
import com.musfit.ui.training.TrainingProgressScreen
import com.musfit.ui.training.TrainingScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val destinations = AppDestination.entries
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppDestination.Today.route
    val currentBottomRoute = bottomDestinationForRoute(currentRoute).route
    var appBackStackEntries by rememberSaveable { mutableStateOf(listOf(AppDestination.Today)) }
    val appBackStack = remember(appBackStackEntries) { AppNavigationStack(appBackStackEntries) }
    var scannedBarcode by rememberSaveable { mutableStateOf<String?>(null) }
    var scannedLabelText by rememberSaveable { mutableStateOf<String?>(null) }
    var chatPreviewVisible by rememberSaveable { mutableStateOf(false) }

    fun navigateToBottomDestination(destination: AppDestination) {
        navController.navigate(destination.route) {
            popUpTo(AppDestination.Today.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun go(destination: AppDestination) {
        AppNavigationStack(appBackStackEntries).also { stack ->
            stack.select(destination)
            appBackStackEntries = stack.entries
        }
        navigateToBottomDestination(destination)
    }

    fun popAppBackStack() {
        AppNavigationStack(appBackStackEntries).also { stack ->
            val previousDestination = stack.pop()
            if (previousDestination != null) {
                appBackStackEntries = stack.entries
                navigateToBottomDestination(previousDestination)
            }
        }
    }

    Scaffold(
        containerColor = MusFitTheme.colors.background,
        bottomBar = {
            MusFitBottomNav(
                destinations = destinations,
                currentRoute = currentBottomRoute,
                onSelect = { go(it) },
            )
        },
        floatingActionButton = { ChatPreviewFab(onClick = { chatPreviewVisible = true }) },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Today.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(AppDestination.Today.route) {
                BottomDestinationBackHandler(canPop = appBackStack.canPop, onBack = { popAppBackStack() })
                TodayScreen(
                    onOpenFood = { go(AppDestination.Food) },
                    onOpenTraining = { go(AppDestination.Training) },
                    onOpenHealth = { go(AppDestination.Profile) },
                )
            }
            composable(AppDestination.Food.route) {
                BottomDestinationBackHandler(canPop = appBackStack.canPop, onBack = { popAppBackStack() })
                FoodScreen(
                    scannedBarcode = scannedBarcode,
                    onScanClick = { navController.navigate(BARCODE_SCANNER_ROUTE) },
                    onScannedBarcodeConsumed = { scannedBarcode = null },
                    scannedLabelText = scannedLabelText,
                    onLabelScanClick = { navController.navigate(NUTRITION_LABEL_SCANNER_ROUTE) },
                    onScannedLabelConsumed = { scannedLabelText = null },
                )
            }
            composable(AppDestination.Training.route) {
                BottomDestinationBackHandler(canPop = appBackStack.canPop, onBack = { popAppBackStack() })
                TrainingScreen(
                    onOpenProgress = { navController.navigate(PROFILE_TRAINING_PROGRESS_ROUTE) },
                    onOpenCoach = { chatPreviewVisible = true },
                )
            }
            composable(AppDestination.Profile.route) {
                BottomDestinationBackHandler(canPop = appBackStack.canPop, onBack = { popAppBackStack() })
                ProfileScreen(
                    onSettingsClick = { navController.navigate(PROFILE_SETTINGS_ROUTE) },
                    onOpenFood = { go(AppDestination.Food) },
                    onOpenTraining = { go(AppDestination.Training) },
                    onOpenTrainingProgress = { navController.navigate(PROFILE_TRAINING_PROGRESS_ROUTE) },
                    onOpenNutritionTrends = { navController.navigate(PROFILE_NUTRITION_TRENDS_ROUTE) },
                )
            }
            composable(PROFILE_SETTINGS_ROUTE) {
                ProfileSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(PROFILE_TRAINING_PROGRESS_ROUTE) {
                TrainingProgressScreen(onBack = { navController.popBackStack() })
            }
            composable(PROFILE_NUTRITION_TRENDS_ROUTE) {
                NutritionTrendsScreen(onBack = { navController.popBackStack() })
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

    if (chatPreviewVisible) {
        ChatPreviewSheet(onDismiss = { chatPreviewVisible = false })
    }
}

@Composable
private fun BottomDestinationBackHandler(
    canPop: Boolean,
    onBack: () -> Unit,
) {
    BackHandler(enabled = canPop, onBack = onBack)
}

/**
 * Material 3 style bottom nav on the plain surface: no floating white card, just a
 * hairline top edge. The active destination gets a tonal pill behind its icon (in
 * the tab's accent container) with a dark label; inactive items are quiet gray.
 */
@Composable
private fun MusFitBottomNav(
    destinations: List<AppDestination>,
    currentRoute: String,
    onSelect: (AppDestination) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().background(MusFitTheme.colors.background)) {
        HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            destinations.forEach { destination ->
                NavBarItem(
                    destination = destination,
                    selected = currentRoute == destination.route,
                    accent = tabAccentFor(destination),
                    onClick = { onSelect(destination) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RowScope.NavBarItem(
    destination: AppDestination,
    selected: Boolean,
    accent: TabAccent,
    onClick: () -> Unit,
) {
    val pillColor by animateColorAsState(
        targetValue = if (selected) accent.container else Color.Transparent,
        animationSpec = MusFitMotion.effects(),
        label = "navPillColor",
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) accent.onContainer else MusFitTheme.colors.onSurfaceVariant,
        animationSpec = MusFitMotion.effects(),
        label = "navIconTint",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) accent.onContainer else MusFitTheme.colors.onSurfaceVariant,
        animationSpec = MusFitMotion.effects(),
        label = "navLabelColor",
    )
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.weight(1f),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 6.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(pillColor)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Icon(
                    destination.icon,
                    contentDescription = destination.label,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = destination.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                color = labelColor,
                maxLines = 1,
            )
        }
    }
}
