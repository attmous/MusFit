package com.musfit.ui.food

import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay

data class FoodNavigationActions(
    val open: (FoodNavKey) -> Unit = {},
    val replace: (FoodNavKey) -> Unit = {},
    val back: () -> Unit = {},
)

@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Suppress("LongMethod")
fun FoodNavigation(
    barcodeScannerContent: @Composable (
        onBarcodeDetected: (String) -> Unit,
        onClose: () -> Unit,
    ) -> Unit = { onBarcodeDetected, onClose ->
        BarcodeScannerScreen(
            onBarcodeDetected = onBarcodeDetected,
            onClose = onClose,
        )
    },
    onRootNavigationChromeVisibilityChange: (Boolean) -> Unit = {},
    viewModel: FoodViewModel = hiltViewModel(),
) {
    val backStack = rememberNavBackStack(FoodDiaryNavKey)
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(
        shouldHandleSinglePaneLayout = false,
    )
    var scannedBarcode by rememberSaveable { mutableStateOf<String?>(null) }
    var scannedLabelText by rememberSaveable { mutableStateOf<String?>(null) }
    val navigator = FoodNavigator(
        backStack = backStack,
        onResult = { result ->
            when (result) {
                is FoodNavigationResult.BarcodeDetected -> scannedBarcode = result.barcode
                is FoodNavigationResult.NutritionLabelCaptured -> scannedLabelText = result.text
            }
        },
    )
    var isRecipeBackConfirmationPending by rememberSaveable { mutableStateOf(false) }
    val currentKey = navigator.currentKey
    val routeState by viewModel.routeState.collectAsStateWithLifecycle()
    var routeEstablished by remember(currentKey) { mutableStateOf(false) }

    LaunchedEffect(currentKey) {
        if (isRecipeBackConfirmationPending && currentKey !is FoodRecipeEditorNavKey) {
            isRecipeBackConfirmationPending = false
        }
        onRootNavigationChromeVisibilityChange(currentKey.showsRootNavigationChrome())
    }
    DisposableEffect(Unit) {
        onDispose { onRootNavigationChromeVisibilityChange(true) }
    }

    // The key is the durable source of destination identity. Rebuild the screen-specific
    // ViewModel content from its ID-only arguments after process recreation and whenever a
    // child pop reveals a parent whose transient content was cleared by the child.
    LaunchedEffect(currentKey) {
        prepareFoodRoute(viewModel, currentKey)
    }
    LaunchedEffect(currentKey, routeState) {
        if (routeState.matchesFoodNavKey(currentKey)) {
            routeEstablished = true
        } else if (routeEstablished) {
            // Successful save/delete operations clear their transient content asynchronously.
            // Pop the corresponding typed destination only after that content was observed, so a
            // validation or repository failure leaves the user on the editor with its message.
            navigator.back()
        }
    }
    val navigation = FoodNavigationActions(
        open = navigator::open,
        replace = navigator::replace,
        back = {
            val key = navigator.currentKey
            if (key is FoodRecipeEditorNavKey && key.shouldConfirmRecipeDiscard(viewModel.state.value)) {
                isRecipeBackConfirmationPending = true
            } else {
                closeFoodRoute(viewModel, key)
                navigator.back()
            }
        },
    )

    if (isRecipeBackConfirmationPending) {
        DiscardRecipeChangesDialog(
            onDismiss = { isRecipeBackConfirmationPending = false },
            onConfirm = {
                isRecipeBackConfirmationPending = false
                val key = navigator.currentKey
                if (key is FoodRecipeEditorNavKey) {
                    closeFoodRoute(viewModel, key)
                    navigator.back()
                }
            },
        )
    }

    @Composable
    fun FoodEntry(key: FoodNavKey) {
        FoodScreen(
            renderedKey = key,
            scannedBarcode = scannedBarcode,
            onScanClick = { navigator.open(FoodBarcodeScannerNavKey) },
            onScannedBarcodeConsumed = { scannedBarcode = null },
            scannedLabelText = scannedLabelText,
            onLabelScanClick = { navigator.open(FoodNutritionLabelScannerNavKey) },
            onScannedLabelConsumed = { scannedLabelText = null },
            navigation = navigation,
            viewModel = viewModel,
        )
    }

    NavDisplay(
        backStack = backStack,
        onBack = navigation.back,
        sceneStrategies = listOf(listDetailStrategy),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<FoodDiaryNavKey> { FoodEntry(it) }
            entry<FoodMealDetailNavKey> { FoodEntry(it) }
            entry<FoodAddNavKey> { FoodEntry(it) }
            entry<FoodDatabaseNavKey>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = { Text("Select a saved food") },
                ),
            ) { FoodEntry(it) }
            entry<FoodDetailNavKey>(metadata = ListDetailSceneStrategy.detailPane()) { FoodEntry(it) }
            entry<FoodDiaryEntryEditorNavKey> { FoodEntry(it) }
            entry<FoodSavedFoodEditorNavKey> { FoodEntry(it) }
            entry<FoodNutritionLabelReviewNavKey> { FoodEntry(it) }
            entry<FoodBarcodeComparisonNavKey> { FoodEntry(it) }
            entry<FoodFastingTimerNavKey> { FoodEntry(it) }
            entry<FoodGoalEditorNavKey> { FoodEntry(it) }
            entry<FoodRecipeBrowserNavKey>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = { Text("Select a recipe") },
                ),
            ) { FoodEntry(it) }
            entry<FoodRecipeEditorNavKey>(metadata = ListDetailSceneStrategy.detailPane()) { FoodEntry(it) }
            entry<FoodMealTemplatesNavKey> { FoodEntry(it) }
            entry<FoodMealTemplateEditorNavKey> { FoodEntry(it) }
            entry<FoodMealSettingsNavKey> { FoodEntry(it) }
            entry<FoodMealDefinitionEditorNavKey> { FoodEntry(it) }
            entry<FoodShoppingListNavKey> { FoodEntry(it) }
            entry<FoodWaterNavKey> { FoodEntry(it) }
            entry<FoodHealthConnectNavKey> { FoodEntry(it) }
            entry<FoodBarcodeScannerNavKey> {
                barcodeScannerContent(
                    { barcode ->
                        if (barcode.isNotBlank()) {
                            navigator.complete(FoodNavigationResult.BarcodeDetected(barcode))
                        }
                    },
                    navigation.back,
                )
            }
            entry<FoodNutritionLabelScannerNavKey> {
                NutritionLabelScannerScreen(
                    onLabelCaptured = { text ->
                        if (text.isNotBlank()) {
                            navigator.complete(FoodNavigationResult.NutritionLabelCaptured(text))
                        }
                    },
                )
            }
        },
    )
}

internal fun FoodNavKey.showsRootNavigationChrome(): Boolean = when (this) {
    FoodBarcodeScannerNavKey,
    FoodNutritionLabelScannerNavKey,
    -> false

    else -> true
}

internal fun FoodNavKey.shouldConfirmRecipeDiscard(state: FoodUiState): Boolean = this is FoodRecipeEditorNavKey && state.hasUnsavedRecipeEditorChanges()

private fun closeFoodRoute(viewModel: FoodViewModel, key: FoodNavKey) {
    when (key) {
        is FoodMealDetailNavKey -> viewModel.closeMealDetail()

        FoodDiaryNavKey,
        FoodBarcodeScannerNavKey,
        FoodNutritionLabelScannerNavKey,
        -> Unit

        else -> viewModel.closeAddFood()
    }
}

@Suppress("CyclomaticComplexMethod")
private fun prepareFoodRoute(viewModel: FoodViewModel, key: FoodNavKey) {
    val state = viewModel.state.value
    when (key) {
        FoodDiaryNavKey -> {
            if (state.selectedMealDetailId != null) viewModel.closeMealDetail()
            if (state.isAddPanelVisible) viewModel.closeAddFood()
        }

        is FoodMealDetailNavKey -> {
            if (state.selectedMealDetailId != key.mealType) viewModel.openMealDetail(key.mealType)
        }

        is FoodAddNavKey -> {
            if (!state.isAddPanelVisible || state.sheetMode != FoodSheetMode.AddFood || state.mealType != key.mealType) {
                viewModel.openAddFood(key.mealType)
            }
        }

        FoodDatabaseNavKey -> if (state.sheetMode != FoodSheetMode.FoodDatabase) viewModel.openFoodDatabase()

        is FoodDetailNavKey -> {
            if (state.selectedSavedFoodDetail?.id != key.foodId) viewModel.openSavedFoodDetail(key.foodId)
        }

        is FoodDiaryEntryEditorNavKey -> {
            if (state.diaryEntryEditor?.id != key.entryId) viewModel.openDiaryEntryEditor(key.entryId)
        }

        is FoodSavedFoodEditorNavKey -> {
            if (state.savedFoodEditor?.id != key.foodId || state.sheetMode != FoodSheetMode.SavedFoodEditor) {
                if (key.foodId == null) viewModel.openNewSavedFoodEditor() else viewModel.openSavedFoodEditor(key.foodId)
            }
        }

        FoodNutritionLabelReviewNavKey -> {
            if (state.sheetMode != FoodSheetMode.NutritionLabelScan) viewModel.openNutritionLabelScan()
        }

        FoodBarcodeComparisonNavKey -> if (state.sheetMode != FoodSheetMode.BarcodeComparison) viewModel.openBarcodeComparison()

        FoodFastingTimerNavKey -> if (state.sheetMode != FoodSheetMode.FastingTimer) viewModel.openFastingTimer()

        FoodGoalEditorNavKey -> if (state.sheetMode != FoodSheetMode.GoalEditor) viewModel.openGoalEditor()

        FoodRecipeBrowserNavKey -> if (state.sheetMode != FoodSheetMode.RecipeBrowser) viewModel.openRecipeBrowser()

        is FoodRecipeEditorNavKey -> {
            if (state.sheetMode != FoodSheetMode.RecipeEditor || state.recipeEditor?.editingRecipeId != key.recipeId) {
                viewModel.openRecipeEditor(key.recipeId)
            }
        }

        FoodMealTemplatesNavKey -> if (state.sheetMode != FoodSheetMode.MealTemplates) viewModel.openMealTemplates()

        is FoodMealTemplateEditorNavKey -> {
            if (state.mealTemplateEditor?.id != key.templateId) viewModel.openMealTemplateEditor(key.templateId)
        }

        FoodMealSettingsNavKey -> {
            if (state.sheetMode != FoodSheetMode.MealSettings || state.editingMealDefinitionId != null) {
                viewModel.openMealSettings()
            }
        }

        is FoodMealDefinitionEditorNavKey -> {
            if (state.editingMealDefinitionId != key.mealId) viewModel.openMealDefinitionEditor(key.mealId)
        }

        FoodShoppingListNavKey -> if (state.sheetMode != FoodSheetMode.ShoppingList) viewModel.openShoppingList()

        FoodWaterNavKey -> if (state.sheetMode != FoodSheetMode.Water) viewModel.openWaterSheet()

        FoodHealthConnectNavKey -> if (state.sheetMode != FoodSheetMode.HealthConnect) viewModel.openHealthConnectSheet()

        FoodBarcodeScannerNavKey,
        FoodNutritionLabelScannerNavKey,
        -> Unit
    }
}

@Suppress("CyclomaticComplexMethod")
internal fun FoodRouteUiState.matchesFoodNavKey(key: FoodNavKey): Boolean = when (key) {
    FoodDiaryNavKey -> !hasActiveSurface

    is FoodMealDetailNavKey -> selectedMealDetailId == key.mealType

    is FoodAddNavKey -> isAddPanelVisible && sheetMode == FoodSheetMode.AddFood && mealType == key.mealType

    FoodDatabaseNavKey -> isAddPanelVisible && sheetMode == FoodSheetMode.FoodDatabase

    is FoodDetailNavKey -> isAddPanelVisible && selectedSavedFoodDetailId == key.foodId

    is FoodDiaryEntryEditorNavKey -> isAddPanelVisible && diaryEntryEditorId == key.entryId

    is FoodSavedFoodEditorNavKey -> {
        isAddPanelVisible && sheetMode == FoodSheetMode.SavedFoodEditor && savedFoodEditorId == key.foodId
    }

    FoodNutritionLabelReviewNavKey -> isAddPanelVisible && sheetMode == FoodSheetMode.NutritionLabelScan

    FoodBarcodeComparisonNavKey -> isAddPanelVisible && sheetMode == FoodSheetMode.BarcodeComparison

    FoodFastingTimerNavKey -> isAddPanelVisible && sheetMode == FoodSheetMode.FastingTimer

    FoodGoalEditorNavKey -> isAddPanelVisible && sheetMode == FoodSheetMode.GoalEditor

    FoodRecipeBrowserNavKey -> isAddPanelVisible && sheetMode == FoodSheetMode.RecipeBrowser

    is FoodRecipeEditorNavKey -> {
        isAddPanelVisible && sheetMode == FoodSheetMode.RecipeEditor && recipeEditorId == key.recipeId
    }

    FoodMealTemplatesNavKey -> {
        isAddPanelVisible && sheetMode == FoodSheetMode.MealTemplates && mealTemplateEditorId == null
    }

    is FoodMealTemplateEditorNavKey -> isAddPanelVisible && mealTemplateEditorId == key.templateId

    FoodMealSettingsNavKey -> {
        isAddPanelVisible && sheetMode == FoodSheetMode.MealSettings && editingMealDefinitionId == null
    }

    is FoodMealDefinitionEditorNavKey -> isAddPanelVisible && editingMealDefinitionId == key.mealId

    FoodShoppingListNavKey -> isAddPanelVisible && sheetMode == FoodSheetMode.ShoppingList

    FoodWaterNavKey -> isAddPanelVisible && sheetMode == FoodSheetMode.Water

    FoodHealthConnectNavKey -> isAddPanelVisible && sheetMode == FoodSheetMode.HealthConnect

    FoodBarcodeScannerNavKey,
    FoodNutritionLabelScannerNavKey,
    -> true
}
