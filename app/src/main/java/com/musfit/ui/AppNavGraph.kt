package com.musfit.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.musfit.ui.theme.MusFitMotion
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

/** Spacing between nav items, used both for layout and the sliding-indicator math. */
private val NavItemSpacing = 2.dp

/**
 * M3E-style floating bottom nav: a rounded pill of destinations + a separate rounded-square FAB.
 * A single pill indicator sits behind the items and springs to the selected tab on navigation —
 * the one motion in the bar.
 */
@Composable
private fun FloatingPillNav(
    destinations: List<AppDestination>,
    currentRoute: String,
    onSelect: (AppDestination) -> Unit,
    onFab: () -> Unit,
) {
    val selectedIndex = destinations.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    val selectedAccent = tabAccentFor(destinations[selectedIndex])
    val indicatorColor by animateColorAsState(
        targetValue = selectedAccent.container,
        animationSpec = MusFitMotion.effects(),
        label = "navIndicatorColor",
    )

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
            val density = LocalDensity.current
            var rowSize by remember { mutableStateOf(IntSize.Zero) }
            val count = destinations.size
            val itemWidth = with(density) {
                if (count > 0 && rowSize.width > 0) {
                    (rowSize.width.toDp() - NavItemSpacing * (count - 1)) / count
                } else 0.dp
            }
            val rowHeight = with(density) { rowSize.height.toDp() }
            val indicatorOffset by animateDpAsState(
                targetValue = (itemWidth + NavItemSpacing) * selectedIndex,
                animationSpec = MusFitMotion.spatial(),
                label = "navIndicatorOffset",
            )

            Box(modifier = Modifier.padding(6.dp)) {
                // Sliding pill behind the items — the single nav animation.
                if (itemWidth > 0.dp) {
                    Box(
                        modifier = Modifier
                            .offset(x = indicatorOffset)
                            .width(itemWidth)
                            .height(rowHeight)
                            .clip(RoundedCornerShape(22.dp))
                            .background(indicatorColor),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { rowSize = it },
                    horizontalArrangement = Arrangement.spacedBy(NavItemSpacing),
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
    // Tint crossfades with the sliding pill so the label/icon don't pop mid-slide.
    val iconTint by animateColorAsState(
        targetValue = if (selected) accent.onContainer else MusFitTheme.colors.onSurfaceVariant,
        animationSpec = MusFitMotion.effects(),
        label = "navIconTint",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) accent.color else MusFitTheme.colors.onSurfaceVariant,
        animationSpec = MusFitMotion.effects(),
        label = "navLabelColor",
    )
    Surface(
        onClick = onClick,
        color = Color.Transparent,
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
                tint = iconTint,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = destination.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = labelColor,
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
