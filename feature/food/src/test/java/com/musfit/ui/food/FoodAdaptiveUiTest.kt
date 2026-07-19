package com.musfit.ui.food

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import com.musfit.ui.components.gridGroupShape
import com.musfit.ui.theme.MusFitTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w400dp-h800dp-mdpi")
class FoodAdaptiveUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun addFood_lazilyRendersAndScrollsOneThousandKeyedFoods() {
        val foods = List(1_000) { index -> savedFood("food-$index", "Food $index") }
        setAddFoodContent(
            FoodUiState(
                foodDatabaseQuery = "Food",
                visibleSavedFoods = foods,
            ),
        )

        compose.onNodeWithText("Food 999").assertDoesNotExist()
        val scanner = compose.onNode(hasContentDescription("Scan barcode"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        assertTrue(scanner.fetchSemanticsNode().boundsInRoot.height >= 48.dp.value)
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Food 999"))
        compose.onNodeWithText("Food 999").assertExists()
    }

    @Test
    fun addFood_savedFoodsRetainFourDpGroupedRowSpacing() {
        setAddFoodContent(
            FoodUiState(
                foodDatabaseQuery = "Food",
                visibleSavedFoods = listOf(
                    savedFood("food-0", "Food 0"),
                    savedFood("food-1", "Food 1"),
                ),
            ),
        )

        val firstBounds = compose.onNode(hasText("Food 0") and hasClickAction()).fetchSemanticsNode().boundsInRoot
        val secondBounds = compose.onNode(hasText("Food 1") and hasClickAction()).fetchSemanticsNode().boundsInRoot
        val gap = secondBounds.top - firstBounds.bottom

        assertTrue("Expected 4dp grouped-row gap, but was $gap", abs(gap - 4.dp.value) <= 1f)
    }

    @Test
    fun foodDatabase_lazilyRendersAndScrollsFiveHundredDuplicateGroups() {
        val duplicateGroups = List(500) { index ->
            FoodDuplicateGroupUiState(
                primaryFoodId = "primary-$index",
                duplicateFoodIds = listOf("duplicate-$index"),
                title = "Duplicate group $index",
                reason = "Same barcode",
            )
        }
        compose.setContent {
            MusFitTheme {
                FoodDatabasePanel(
                    state = FoodUiState(duplicateFoodGroups = duplicateGroups),
                    onSearchChanged = {},
                    onSearchOnlineClick = {},
                    onNewFoodClick = {},
                    onBarcodeCompareClick = {},
                    onOpenFoodDetailClick = {},
                    onEditFoodClick = {},
                    onSaveOnlineFoodClick = {},
                    onImportStarterFoodsClick = {},
                    onNutritionLabelScanClick = {},
                    onMergeDuplicateFoodsClick = { _, _ -> },
                    onFavoriteClick = { _, _ -> },
                    onReportFoodClick = {},
                )
            }
        }

        compose.onNodeWithText("Duplicate group 499").assertDoesNotExist()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Duplicate group 499"))
        compose.onNodeWithText("Duplicate group 499").assertExists()
    }

    @Test
    fun recipeDiscovery_lazilyRendersFiveHundredItemsInTwoCardRows() {
        val recipes = List(500) { index -> recipeDiscoveryItem(index) }
        setRecipeBrowserContent(
            FoodUiState(
                recipeDiscovery = RecipeDiscoveryUiState(
                    items = recipes,
                    visibleItems = recipes,
                ),
            ),
        )

        val lazyList = compose.onNode(hasScrollToIndexAction())
        lazyList.performScrollToNode(hasText("Recipe idea 1"))
        lazyList.performScrollToNode(hasText("Recipe idea 3"))
        val firstBounds = compose.onNode(hasText("Recipe idea 1") and hasClickAction()).fetchSemanticsNode().boundsInRoot
        val secondBounds = compose.onNode(hasText("Recipe idea 2") and hasClickAction()).fetchSemanticsNode().boundsInRoot
        val thirdBounds = compose.onNode(hasText("Recipe idea 3") and hasClickAction()).fetchSemanticsNode().boundsInRoot
        assertTrue(abs(firstBounds.top - secondBounds.top) <= 1f)
        assertTrue(firstBounds.left < secondBounds.left)
        assertTrue(abs(firstBounds.width - secondBounds.width) <= 1f)
        assertTrue("Expected 8dp grid column gap", abs(secondBounds.left - firstBounds.right - 8.dp.value) <= 1f)
        assertTrue("Expected 8dp grid row gap", abs(thirdBounds.top - firstBounds.bottom - 8.dp.value) <= 1f)

        compose.onNodeWithText("Recipe idea 499").assertDoesNotExist()
        lazyList.performScrollToNode(hasText("Recipe idea 499"))
        compose.onNodeWithText("Recipe idea 499").assertExists()
    }

    @Test
    fun recipeDiscovery_filterAndReturnPreserveVisibleRecipeAnchor() {
        val recipes = List(500) { index -> recipeDiscoveryItem(index) }
        val initialState = FoodUiState(
            recipeDiscovery = RecipeDiscoveryUiState(
                items = recipes,
                visibleItems = recipes,
            ),
        )
        val recipeListState = LazyGridState()
        var visibleRecipes by mutableStateOf(recipes)
        setRecipeBrowserContent(
            state = initialState,
            recipeListState = recipeListState,
            stateProvider = {
                initialState.copy(
                    recipeDiscovery = initialState.recipeDiscovery.copy(visibleItems = visibleRecipes),
                )
            },
        )

        // Four full-span entries precede discovery. Recipe 0 is featured, so
        // idea-401 is discovery item 400 and global lazy-grid item 404.
        runBlocking { recipeListState.scrollToItem(404) }
        compose.waitForIdle()
        val targetKey = "discovery-idea-401"
        val originalOffset = recipeListState.visibleItemOffset(targetKey)

        compose.runOnIdle { visibleRecipes = recipes.drop(1) }
        compose.waitForIdle()
        assertEquals(originalOffset, recipeListState.visibleItemOffset(targetKey))

        compose.runOnIdle { visibleRecipes = recipes }
        compose.waitForIdle()
        val restoredOffset = recipeListState.visibleItemOffset(targetKey)
        assertTrue(
            "Expected $targetKey to remain in the same viewport after return: " +
                "original=$originalOffset restored=$restoredOffset",
            abs(restoredOffset - originalOffset) < recipeListState.layoutInfo.viewportSize.height,
        )
    }

    @Test
    fun recipeDiscovery_oddFinalCardUsesActualPartialRowShape() {
        assertEquals(
            gridGroupShape(row = 1, rowCount = 2, column = 0, columnCount = 1),
            recipeGridShape(index = 2, itemCount = 3),
        )
    }

    @Test
    fun myRecipesToggle_revealsAndLazilyScrollsFiveHundredSavedRecipes() {
        val recipes = List(500) { index -> savedRecipe(index) }
        setRecipeBrowserContent(
            FoodUiState(
                recipes = recipes,
                recipeDiscovery = RecipeDiscoveryUiState(items = emptyList(), visibleItems = emptyList()),
            ),
        )

        compose.onNodeWithText("My recipes", substring = true).performClick()
        compose.waitForIdle()
        compose.onNodeWithText("MY RECIPES").assertExists()
        compose.onNodeWithText("Saved recipe 499").assertDoesNotExist()

        val firstBounds = compose.onNode(hasText("Saved recipe 0") and hasClickAction()).fetchSemanticsNode().boundsInRoot
        val secondBounds = compose.onNode(hasText("Saved recipe 1") and hasClickAction()).fetchSemanticsNode().boundsInRoot
        assertTrue("Expected 4dp saved-recipe row gap", abs(secondBounds.top - firstBounds.bottom - 4.dp.value) <= 1f)

        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Saved recipe 499"))
        compose.onNodeWithText("Saved recipe 499").assertExists()
    }

    @Test
    fun selectableChip_exposesSelectedRadioRoleAndMinimumTouchTarget() {
        compose.setContent {
            MusFitTheme {
                SelectableChip(text = "Favorites", selected = true, onClick = {})
            }
        }

        val node = compose.onNodeWithText("Favorites")
            .assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        assertTrue(node.fetchSemanticsNode().boundsInRoot.height >= 48.dp.value)
    }

    @Test
    fun selectableChip_actionExposesButtonRoleWithoutSelectionState() {
        compose.setContent {
            MusFitTheme {
                SelectableChip(
                    text = "Recipes",
                    selected = null,
                    onClick = {},
                )
            }
        }

        val node = compose.onNodeWithText("Recipes")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .fetchSemanticsNode()
        assertFalse(node.config.contains(SemanticsProperties.Selected))
        assertTrue(node.boundsInRoot.height >= 48.dp.value)
    }

    @Test
    fun foodSheetHeader_closeMeetsMinimumTouchTarget() {
        compose.setContent {
            MusFitTheme {
                FoodSheetHeader(title = "Food", onClose = {})
            }
        }

        val close = compose.onNode(hasContentDescription("Close"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        assertTrue(close.fetchSemanticsNode().boundsInRoot.height >= 48.dp.value)
        assertTrue(close.fetchSemanticsNode().boundsInRoot.width >= 48.dp.value)
    }

    @Test
    fun foodDatabase_narrowPaneKeepsIdentityAndAllActionsAccessible() {
        val almonds = savedFood("almonds", "Almonds")
        compose.setContent {
            MusFitTheme {
                FoodDatabasePanel(
                    state = FoodUiState(savedFoods = listOf(almonds), visibleSavedFoods = listOf(almonds)),
                    onSearchChanged = {},
                    onSearchOnlineClick = {},
                    onNewFoodClick = {},
                    onBarcodeCompareClick = {},
                    onOpenFoodDetailClick = {},
                    onEditFoodClick = {},
                    onSaveOnlineFoodClick = {},
                    onImportStarterFoodsClick = {},
                    onNutritionLabelScanClick = {},
                    onMergeDuplicateFoodsClick = { _, _ -> },
                    onFavoriteClick = { _, _ -> },
                    onReportFoodClick = {},
                )
            }
        }

        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Almonds"))
        compose.onNodeWithText("Almonds").assertExists()
        compose.onNode(hasContentDescription("Add Almonds to favorites")).assertExists()
        compose.onNode(hasContentDescription("Report Almonds")).assertExists()
        compose.onNode(hasContentDescription("View Almonds details")).assertExists()
        compose.onNode(hasContentDescription("Edit Almonds")).assertExists()
        listOf(
            "Add Almonds to favorites",
            "Report Almonds",
            "View Almonds details",
            "Edit Almonds",
        ).forEach { description ->
            val bounds = compose.onNode(hasContentDescription(description)).fetchSemanticsNode().boundsInRoot
            assertTrue("Expected 48dp-wide target for $description: $bounds", bounds.width >= 48.dp.value)
            assertTrue("Expected 48dp-high target for $description: $bounds", bounds.height >= 48.dp.value)
        }
    }

    @Test
    fun foodDatabase_savedRowsRemainContiguousAfterLazyMigration() {
        val foods = listOf(
            savedFood("almonds", "Almonds"),
            savedFood("bananas", "Bananas"),
        )
        compose.setContent {
            MusFitTheme {
                FoodDatabasePanel(
                    state = FoodUiState(savedFoods = foods, visibleSavedFoods = foods),
                    onSearchChanged = {},
                    onSearchOnlineClick = {},
                    onNewFoodClick = {},
                    onBarcodeCompareClick = {},
                    onOpenFoodDetailClick = {},
                    onEditFoodClick = {},
                    onSaveOnlineFoodClick = {},
                    onImportStarterFoodsClick = {},
                    onNutritionLabelScanClick = {},
                    onMergeDuplicateFoodsClick = { _, _ -> },
                    onFavoriteClick = { _, _ -> },
                    onReportFoodClick = {},
                )
            }
        }

        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("food-database-row-bananas"))
        val firstBounds = compose.onNodeWithTag("food-database-row-almonds").fetchSemanticsNode().boundsInRoot
        val secondBounds = compose.onNodeWithTag("food-database-row-bananas").fetchSemanticsNode().boundsInRoot

        assertTrue("Expected contiguous saved-food rows", abs(secondBounds.top - firstBounds.bottom) <= 1f)
    }

    @Test
    fun mealTemplates_rowsRemainDividerContiguousAfterLazyMigration() {
        val templates = listOf(
            MealTemplateUiState("template-0", "Breakfast", "breakfast", itemSummary = "2 foods"),
            MealTemplateUiState("template-1", "Lunch", "lunch", itemSummary = "3 foods"),
        )
        compose.setContent {
            MusFitTheme {
                MealTemplatesPanel(
                    state = FoodUiState(mealTemplates = templates),
                    onTemplateClick = {},
                    onEditClick = {},
                    onDuplicateClick = {},
                    onDeleteClick = {},
                    onFavoriteClick = { _, _ -> },
                    onNameChanged = {},
                    onMealTypeChanged = {},
                    onTemplateItemQuantityChanged = { _, _ -> },
                    onTemplateItemRemoveClick = {},
                    onTemplateItemFoodChanged = {},
                    onTemplateNewItemQuantityChanged = {},
                    onTemplateAddItemClick = {},
                    onSaveEditClick = {},
                )
            }
        }

        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("meal-template-row-template-1"))
        val firstBounds = compose.onNodeWithTag("meal-template-row-template-0").fetchSemanticsNode().boundsInRoot
        val secondBounds = compose.onNodeWithTag("meal-template-row-template-1").fetchSemanticsNode().boundsInRoot
        val dividerGap = secondBounds.top - firstBounds.bottom

        assertTrue("Expected only the divider between template rows, but was $dividerGap", dividerGap in 0f..2f)
    }

    @Test
    fun foodDetail_narrowPaneWrapsServingsAndKeepsMacroLabelsSingleLine() {
        val almonds = savedFood("almonds", "Almonds").copy(
            servings = listOf(
                SavedFoodServingUiState("handful", "small handful", 30.0),
                SavedFoodServingUiState("ounce", "one-ounce serving", 28.0),
            ),
        )
        compose.setContent {
            MusFitTheme {
                Box(modifier = Modifier.width(260.dp)) {
                    FoodDetailPanel(
                        state = FoodUiState(
                            selectedSavedFoodDetail = almonds,
                            savedFoodQuantityGrams = "30",
                        ),
                        onEditClick = {},
                        onLogClick = {},
                        onFavoriteClick = {},
                        onReportClick = {},
                        onCorrectClick = {},
                        onQuantityChanged = {},
                        onServingSelected = { _, _ -> },
                    )
                }
            }
        }

        val gramsBounds = compose.onNodeWithText("grams").fetchSemanticsNode().boundsInRoot
        val ounceBounds = compose.onNodeWithText("one-ounce serving · 28 g").fetchSemanticsNode().boundsInRoot
        assertTrue("Expected serving chips to wrap: grams=$gramsBounds ounce=$ounceBounds", ounceBounds.top > gramsBounds.top)

        val macroLabelHeights = listOf("Carbs", "Protein", "Fat").map { label ->
            compose.onNodeWithText(label).fetchSemanticsNode().boundsInRoot.height
        }
        assertTrue(macroLabelHeights.max() - macroLabelHeights.min() <= 1f)
    }

    @Test
    fun recipePlanTarget_narrowPaneStacksDateAndMealControls() {
        compose.setContent {
            MusFitTheme {
                Box(modifier = Modifier.width(260.dp)) {
                    RecipePlanTargetCard(
                        state = FoodUiState(recipeServingsToLog = "1"),
                        mealTitle = "Breakfast",
                        onPreviousDayClick = {},
                        onNextDayClick = {},
                        onTodayClick = {},
                        onMealChanged = {},
                        onServingsChanged = {},
                    )
                }
            }
        }

        val todayBounds = compose.onNodeWithText("Today", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val servingsBounds = compose.onNodeWithText("Serv").fetchSemanticsNode().boundsInRoot
        val previousBounds = compose.onNode(hasContentDescription("Previous day")).fetchSemanticsNode().boundsInRoot
        val nextBounds = compose.onNode(hasContentDescription("Next day")).fetchSemanticsNode().boundsInRoot
        assertTrue(servingsBounds.top > todayBounds.top)
        assertTrue(
            "Expected readable Today label, today=$todayBounds previous=$previousBounds next=$nextBounds",
            todayBounds.width >= 48.dp.value,
        )
        compose.onNode(hasContentDescription("Previous day")).assertExists()
        compose.onNode(hasContentDescription("Next day")).assertExists()
        compose.onNodeWithText("Breakfast").assertExists()
    }

    @Test
    fun recipeFeaturedCard_narrowPaneStacksReadableSummaryAboveAction() {
        compose.setContent {
            MusFitTheme {
                Box(modifier = Modifier.width(260.dp)) {
                    RecipeFeaturedCard(
                        item = RecipeDiscoveryItemUiState(
                            id = "salmon",
                            title = "Salmon power bowl",
                            subtitle = "High-protein dinner",
                            category = "Dinner",
                            servingName = "Bowl",
                            servingGrams = 420.0,
                            caloriesKcal = 646.0,
                            proteinGrams = 41.9,
                            carbsGrams = 52.0,
                            fatGrams = 22.0,
                            tagLabels = listOf("High protein"),
                            isFavorite = false,
                            isSavedRecipe = true,
                            programRelevant = true,
                            sourceRecipeId = "recipe-salmon",
                        ),
                        mealTitle = "Breakfast",
                        isSaving = false,
                        onPlanClick = {},
                        onReviewClick = {},
                    )
                }
            }
        }

        val titleBounds = compose.onNodeWithText("Salmon power bowl").fetchSemanticsNode().boundsInRoot
        val actionBounds = compose.onNodeWithText("Plan breakfast").fetchSemanticsNode().boundsInRoot
        assertTrue(actionBounds.top > titleBounds.bottom)
        assertTrue(titleBounds.width >= 120.dp.value)
        assertTrue(actionBounds.height >= 48.dp.value)
    }

    @Test
    fun expandedRecipeEditor_hidesBackAffordanceAndKeepsTitle() {
        setRecipeBrowserContent(
            state = FoodUiState(
                sheetMode = FoodSheetMode.RecipeEditor,
                recipeEditor = RecipeEditorState(),
            ),
            showEditorBack = false,
        )

        compose.onNodeWithText("New recipe").assertExists()
        compose.onNode(hasContentDescription("Back")).assertDoesNotExist()
    }

    @Test
    fun compactRecipeEditor_backAffordanceRequestsNavigationBack() {
        var backRequests = 0
        setRecipeBrowserContent(
            state = FoodUiState(
                sheetMode = FoodSheetMode.RecipeEditor,
                recipeEditor = RecipeEditorState(name = "Draft dinner"),
            ),
            onHomeClick = { backRequests += 1 },
        )

        val back = compose.onNode(hasContentDescription("Back"))
        val bounds = back.fetchSemanticsNode().boundsInRoot
        assertTrue("Expected 48dp-wide Back target: $bounds", bounds.width >= 48.dp.value)
        assertTrue("Expected 48dp-high Back target: $bounds", bounds.height >= 48.dp.value)
        back.performClick()

        assertEquals(1, backRequests)
    }

    @Test
    fun recipeDraftChangeDetection_distinguishesCleanAndModifiedEditors() {
        assertFalse(FoodUiState(recipeEditor = RecipeEditorState()).hasUnsavedRecipeEditorChanges())
        assertTrue(
            FoodUiState(recipeEditor = RecipeEditorState(name = "New recipe"))
                .hasUnsavedRecipeEditorChanges(),
        )

        val saved = savedRecipe(0)
        val cleanEditor = RecipeEditorState(
            editingRecipeId = saved.id,
            name = saved.name,
            category = saved.category.orEmpty(),
            servingName = saved.servingName,
            servingGrams = saved.servingGrams.formatInputNumber(),
            servingsCount = saved.servings.formatInputNumber(),
            cookedYieldGrams = saved.cookedYieldGrams.formatInputNumber(),
            ingredients = saved.ingredients,
        )
        val cleanState = FoodUiState(recipes = listOf(saved), recipeEditor = cleanEditor)
        assertFalse(cleanState.hasUnsavedRecipeEditorChanges())
        assertTrue(cleanState.copy(recipeEditor = cleanEditor.copy(name = "Changed")).hasUnsavedRecipeEditorChanges())
    }

    private fun LazyGridState.visibleItemOffset(key: String): Int = layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }?.offset?.y
        ?: error("Expected $key to remain visible; keys=${layoutInfo.visibleItemsInfo.map { it.key }}")

    private fun setRecipeBrowserContent(
        state: FoodUiState,
        showEditorBack: Boolean = true,
        onHomeClick: () -> Unit = {},
        recipeListState: LazyGridState? = null,
        stateProvider: (() -> FoodUiState)? = null,
    ) {
        compose.setContent {
            MusFitTheme {
                RecipeBrowserScreen(
                    state = stateProvider?.invoke() ?: state,
                    showEditorBack = showEditorBack,
                    recipeListState = recipeListState ?: rememberLazyGridState(),
                    onCloseClick = {},
                    onForwardClick = {},
                    onHomeClick = onHomeClick,
                    onPreviousDayClick = {},
                    onNextDayClick = {},
                    onTodayClick = {},
                    onMealChanged = {},
                    onServingsChanged = {},
                    onNameChanged = {},
                    onCategoryChanged = {},
                    onServingNameChanged = {},
                    onServingsCountChanged = {},
                    onCookedYieldChanged = {},
                    onIngredientFoodChanged = {},
                    onIngredientServingChoiceSelected = {},
                    onIngredientQuantityChanged = {},
                    onAddIngredientClick = {},
                    onEditRecipeClick = {},
                    onDuplicateRecipeClick = {},
                    onFavoriteClick = { _, _ -> },
                    onSearchQueryChanged = {},
                    onDiscoveryFilterChanged = {},
                    onDiscoveryItemClick = {},
                    onLogRecipeClick = {},
                    onPlanRecipeClick = {},
                    onReviewIdeaClick = {},
                    onSaveClick = {},
                    onDeleteClick = {},
                )
            }
        }
    }

    private fun setAddFoodContent(state: FoodUiState) {
        compose.setContent {
            MusFitTheme {
                AddFoodScreen(
                    state = state,
                    onBack = {},
                    onQueryChange = {},
                    onScanClick = {},
                    onTabSelected = {},
                    onFoodClick = {},
                    onQuickTrack = {},
                    onAdjustGoals = {},
                    onCopyYesterday = {},
                    onSaveTemplate = {},
                    onScanLabel = {},
                    onProductNameChanged = {},
                    onBrandChanged = {},
                    onQuantityChanged = {},
                    onAmountServingChoiceSelected = {},
                    onCaloriesChanged = {},
                    onProteinChanged = {},
                    onCarbsChanged = {},
                    onFatChanged = {},
                    onSaveProduct = {},
                    onLogFood = {},
                    onCreateRecipe = {},
                )
            }
        }
    }

    private fun recipeDiscoveryItem(index: Int) = RecipeDiscoveryItemUiState(
        id = "idea-$index",
        title = "Recipe idea $index",
        subtitle = "Recipe subtitle $index",
        category = "Dinner",
        servingName = "Bowl",
        servingGrams = 400.0,
        caloriesKcal = 500.0,
        proteinGrams = 30.0,
        carbsGrams = 45.0,
        fatGrams = 18.0,
        tagLabels = listOf("Quick"),
        isFavorite = false,
        isSavedRecipe = false,
        programRelevant = false,
    )

    private fun savedRecipe(index: Int) = RecipeUiState(
        id = "saved-recipe-$index",
        name = "Saved recipe $index",
        category = "Dinner",
        servingName = "Bowl",
        servingGrams = 400.0,
        servings = 2.0,
        cookedYieldGrams = 800.0,
        caloriesPerServingKcal = 500.0,
        proteinPerServingGrams = 30.0,
        carbsPerServingGrams = 45.0,
        fatPerServingGrams = 18.0,
        itemSummary = "2 ingredients",
    )

    private fun savedFood(id: String, name: String) = SavedFoodUiState(
        id = id,
        name = name,
        brand = null,
        defaultServingGrams = 100.0,
        caloriesPer100g = 100.0,
        proteinPer100g = 10.0,
        carbsPer100g = 10.0,
        fatPer100g = 2.0,
        caloriesPerServingKcal = 100.0,
        proteinPerServingGrams = 10.0,
        carbsPerServingGrams = 10.0,
        fatPerServingGrams = 2.0,
    )
}
