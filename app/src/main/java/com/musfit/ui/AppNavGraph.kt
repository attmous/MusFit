package com.musfit.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScope
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.musfit.ui.food.BarcodeScannerScreen
import com.musfit.ui.food.FoodNavigation
import com.musfit.ui.food.NutritionTrendsScreen
import com.musfit.ui.profile.ProfileScreen
import com.musfit.ui.theme.MusFitMotion
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import com.musfit.ui.theme.tabAccentFor
import com.musfit.ui.today.ChatPreviewFab
import com.musfit.ui.today.TodayScreen
import com.musfit.ui.training.TrainingNavigation
import com.musfit.ui.training.TrainingProgressScreen

/**
 * Metrics for the Material 3 Expressive bottom chrome: a floating white pill bar
 * holding the four nav items, with the coach FAB docked inline to its right.
 */
internal object MusFitBottomNavMetrics {
    // The floating row that docks the pill bar + coach FAB on the screen ground.
    val RowTopPadding: Dp = 10.dp
    val RowHorizontalPadding: Dp = 14.dp
    val RowBottomPadding: Dp = 16.dp
    val RowGap: Dp = 10.dp

    // The white pill bar container.
    val BarCornerRadius: Dp = 30.dp
    val BarInnerPadding: Dp = 8.dp
    val BarShadowElevation: Dp = 8.dp

    // Per-item tonal pill + its icon/label.
    val ActivePillRadius: Dp = 22.dp
    val ItemVerticalPadding: Dp = 8.dp
    val IconSize: Dp = 22.dp
    val LabelSpacing: Dp = 3.dp
    val LabelSize: TextUnit = 11.sp

    // The inline coach FAB — a rounded square, the one global azure accent.
    val FabSize: Dp = 58.dp
    val FabCornerRadius: Dp = 22.dp
    val FabIconSize: Dp = 26.dp
    val FabShadowElevation: Dp = 10.dp
}

internal enum class RootNavigationLayout {
    Compact,
    Rail,
    Wide,
}

internal fun rootNavigationLayoutForWidth(width: Dp): RootNavigationLayout = when {
    width < 600.dp -> RootNavigationLayout.Compact
    width < 840.dp -> RootNavigationLayout.Rail
    else -> RootNavigationLayout.Wide
}

@Composable
fun AppNavGraph(
    barcodeScannerContent: @Composable (
        onBarcodeDetected: (String) -> Unit,
        onClose: () -> Unit,
    ) -> Unit = { onBarcodeDetected, onClose ->
        BarcodeScannerScreen(
            onBarcodeDetected = onBarcodeDetected,
            onClose = onClose,
        )
    },
) {
    val backStack = rememberNavBackStack(TodayNavKey)
    val destinations = AppDestination.entries
    var chatPreviewVisible by rememberSaveable { mutableStateOf(false) }
    var rootNavigationChromeVisible by rememberSaveable { mutableStateOf(true) }
    val navigator = AppNavigator(
        backStack = backStack,
        onOpenCoach = { chatPreviewVisible = true },
    )
    val currentBottomRoute = navigator.currentDestination.route

    @Composable
    fun NavigationContent(modifier: Modifier) {
        NavDisplay(
            backStack = backStack,
            modifier = modifier,
            onBack = { navigator.goBack() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<TodayNavKey> {
                    TodayScreen(
                        onOpenFood = {
                            navigator.navigate(AppNavigationAction.SelectTopLevel(AppDestination.Food))
                        },
                        onOpenTraining = {
                            navigator.navigate(AppNavigationAction.SelectTopLevel(AppDestination.Training))
                        },
                        onOpenHealth = {
                            navigator.navigate(AppNavigationAction.SelectTopLevel(AppDestination.Profile))
                        },
                    )
                }
                entry<FoodNavKey> {
                    FoodNavigation(
                        barcodeScannerContent = barcodeScannerContent,
                        onRootNavigationChromeVisibilityChange = { rootNavigationChromeVisible = it },
                    )
                }
                entry<TrainingNavKey> {
                    TrainingNavigation(
                        onOpenCoach = { navigator.navigate(AppNavigationAction.OpenCoach) },
                    )
                }
                entry<ProfileNavKey> {
                    ProfileScreen(
                        onSettingsClick = { navigator.navigate(AppNavigationAction.OpenProfileSettings) },
                        onOpenFood = {
                            navigator.navigate(AppNavigationAction.SelectTopLevel(AppDestination.Food))
                        },
                        onOpenTrainingProgress = { navigator.navigate(AppNavigationAction.OpenTrainingProgress) },
                        onOpenNutritionTrends = { navigator.navigate(AppNavigationAction.OpenNutritionTrends) },
                    )
                }
                entry<ProfileSettingsNavKey> {
                    ProfileSettingsEntry(onBack = { navigator.goBack() })
                }
                entry<TrainingProgressNavKey> {
                    TrainingProgressScreen(onBack = { navigator.goBack() })
                }
                entry<NutritionTrendsNavKey> {
                    NutritionTrendsScreen(onBack = { navigator.goBack() })
                }
            },
        )
    }

    BoxWithConstraints {
        RootNavigationScaffold(
            layout = rootNavigationLayoutForWidth(maxWidth),
            state = RootNavigationState(
                destinations = destinations,
                currentRoute = currentBottomRoute,
                chromeVisible = rootNavigationChromeVisible,
            ),
            callbacks = RootNavigationCallbacks(
                onSelect = { navigator.navigate(AppNavigationAction.SelectTopLevel(it)) },
                onCoachClick = { navigator.navigate(AppNavigationAction.OpenCoach) },
            ),
            content = { modifier -> NavigationContent(modifier) },
        )
    }

    if (chatPreviewVisible) {
        CoachChatEntry(
            onDismiss = { chatPreviewVisible = false },
            onConfigure = {
                navigator.navigate(AppNavigationAction.SelectTopLevel(AppDestination.Profile))
            },
        )
    }
}

internal data class RootNavigationCallbacks(
    val onSelect: (AppDestination) -> Unit,
    val onCoachClick: () -> Unit,
)

internal data class RootNavigationState(
    val destinations: List<AppDestination>,
    val currentRoute: String,
    val chromeVisible: Boolean,
)

@Composable
internal fun RootNavigationScaffold(
    layout: RootNavigationLayout,
    state: RootNavigationState,
    callbacks: RootNavigationCallbacks,
    content: @Composable (Modifier) -> Unit,
) {
    if (layout == RootNavigationLayout.Compact) {
        CompactRootNavigationScaffold(state, callbacks, content)
    } else {
        AdaptiveRootNavigationScaffold(layout, state, callbacks, content)
    }
}

@Composable
private fun CompactRootNavigationScaffold(
    state: RootNavigationState,
    callbacks: RootNavigationCallbacks,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        containerColor = MusFitTheme.colors.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (state.chromeVisible) {
                MusFitBottomNav(
                    destinations = state.destinations,
                    currentRoute = state.currentRoute,
                    onSelect = callbacks.onSelect,
                    onCoachClick = callbacks.onCoachClick,
                )
            }
        },
    ) { innerPadding ->
        val safeContent = if (state.chromeVisible) {
            Modifier.windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            )
        } else {
            Modifier
        }
        content(
            Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .then(safeContent),
        )
    }
}

@Composable
private fun AdaptiveRootNavigationScaffold(
    layout: RootNavigationLayout,
    state: RootNavigationState,
    callbacks: RootNavigationCallbacks,
    content: @Composable (Modifier) -> Unit,
) {
    val scaffoldState = rememberNavigationSuiteScaffoldState()
    LaunchedEffect(state.chromeVisible) {
        if (state.chromeVisible) scaffoldState.show() else scaffoldState.hide()
    }
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            rootNavigationItems(state.destinations, state.currentRoute, callbacks)
        },
        layoutType = if (!state.chromeVisible) {
            NavigationSuiteType.None
        } else {
            when (layout) {
                RootNavigationLayout.Compact -> NavigationSuiteType.None
                RootNavigationLayout.Rail -> NavigationSuiteType.NavigationRail
                RootNavigationLayout.Wide -> NavigationSuiteType.NavigationDrawer
            }
        },
        state = scaffoldState,
        containerColor = MusFitTheme.colors.background,
    ) {
        val safeContent = if (state.chromeVisible) {
            Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
        } else {
            Modifier
        }
        content(safeContent)
    }
}

private fun NavigationSuiteScope.rootNavigationItems(
    destinations: List<AppDestination>,
    currentRoute: String,
    callbacks: RootNavigationCallbacks,
) {
    destinations.forEach { destination ->
        item(
            selected = currentRoute == destination.route,
            onClick = { callbacks.onSelect(destination) },
            icon = {
                Icon(
                    imageVector = if (currentRoute == destination.route) destination.selectedIcon else destination.icon,
                    contentDescription = null,
                )
            },
            label = { Text(destination.label) },
        )
    }
    item(
        selected = false,
        onClick = callbacks.onCoachClick,
        icon = { Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null) },
        label = { Text("Coach") },
    )
}

/**
 * Material 3 Expressive bottom chrome: a floating white pill bar (rounded 30dp,
 * soft shadow, hairline outline) carrying the four nav items, with the coach FAB
 * docked inline to its right. The bar floats on the screen ground — no hairline
 * above it. The active destination fills its whole cell with a tonal pill in the
 * tab accent and swaps its icon to the filled variant; inactive items are quiet.
 */
@Composable
internal fun MusFitBottomNav(
    destinations: List<AppDestination>,
    currentRoute: String,
    onSelect: (AppDestination) -> Unit,
    onCoachClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                start = MusFitBottomNavMetrics.RowHorizontalPadding,
                top = MusFitBottomNavMetrics.RowTopPadding,
                end = MusFitBottomNavMetrics.RowHorizontalPadding,
                bottom = MusFitBottomNavMetrics.RowBottomPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MusFitBottomNavMetrics.RowGap),
    ) {
        NavPillBar(
            destinations = destinations,
            currentRoute = currentRoute,
            onSelect = onSelect,
            modifier = Modifier.weight(1f),
        )
        ChatPreviewFab(onClick = onCoachClick)
    }
}

@Composable
private fun NavPillBar(
    destinations: List<AppDestination>,
    currentRoute: String,
    onSelect: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The card is elevated off the ground: white in light, a lighter-than-ground
    // elevated surface in dark (the ground itself is near-black).
    val barColor = if (isSystemInDarkTheme()) {
        MusFitTheme.colors.surfaceVariant
    } else {
        MusFitTheme.colors.surface
    }
    Surface(
        color = barColor,
        shape = RoundedCornerShape(MusFitBottomNavMetrics.BarCornerRadius),
        shadowElevation = MusFitBottomNavMetrics.BarShadowElevation,
        border = BorderStroke(1.dp, MusFitTheme.colors.outline),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(MusFitBottomNavMetrics.BarInnerPadding),
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
    val contentColor by animateColorAsState(
        targetValue = if (selected) accent.onContainer else MusFitTheme.colors.onSurfaceVariant,
        animationSpec = MusFitMotion.effects(),
        label = "navContentColor",
    )
    Surface(
        color = pillColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(MusFitBottomNavMetrics.ActivePillRadius),
        modifier = Modifier
            .weight(1f)
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick,
            ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = MusFitBottomNavMetrics.ItemVerticalPadding),
        ) {
            Icon(
                imageVector = if (selected) destination.selectedIcon else destination.icon,
                contentDescription = destination.label,
                tint = contentColor,
                modifier = Modifier.size(MusFitBottomNavMetrics.IconSize),
            )
            Spacer(Modifier.height(MusFitBottomNavMetrics.LabelSpacing))
            Text(
                text = destination.label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = MusFitBottomNavMetrics.LabelSize,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}
