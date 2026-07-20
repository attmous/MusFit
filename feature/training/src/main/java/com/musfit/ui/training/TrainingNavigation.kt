package com.musfit.ui.training

import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.musfit.feature.training.R

data class TrainingNavigationActions(
    val open: (TrainingNavKey) -> Unit = {},
    val back: () -> Unit = {},
    val popThrough: (TrainingNavKey) -> Unit = {},
    val resetTo: (List<TrainingNavKey>) -> Unit = {},
)

@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun TrainingNavigation(
    onOpenCoach: () -> Unit = {},
    viewModel: TrainingViewModel = hiltViewModel(),
) {
    TrainingNavigationHost(
        onOpenCoach = onOpenCoach,
        viewModel = viewModel,
    )
}

@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun TrainingNavigationHost(
    onOpenCoach: () -> Unit = {},
    viewModel: TrainingViewModel,
    paneScaffoldDirectiveOverride: PaneScaffoldDirective? = null,
) {
    val backStack = rememberNavBackStack(TrainingHomeNavKey)
    val navigator = TrainingNavigator(
        backStack = backStack,
        onRoutesRemoved = { routes -> routes.forEach { closeTrainingRoute(viewModel, it) } },
    )
    val listDetailStrategy = paneScaffoldDirectiveOverride?.let { directive ->
        rememberListDetailSceneStrategy<NavKey>(
            shouldHandleSinglePaneLayout = false,
            directive = directive,
        )
    } ?: rememberListDetailSceneStrategy(
        shouldHandleSinglePaneLayout = false,
    )
    val currentKey = navigator.currentKey

    // Destination keys own the durable route identity. Rehydrate ID-backed content after
    // process recreation and after a child pop reveals a parent whose transient state was reset.
    LaunchedEffect(currentKey) {
        prepareTrainingRoute(viewModel, currentKey) {
            if (navigator.currentKey == currentKey) navigator.back()
        }
    }
    val navigation = TrainingNavigationActions(
        open = navigator::open,
        back = { navigator.back() },
        popThrough = { key -> navigator.popThrough(key) },
        resetTo = navigator::resetTo,
    )

    @Composable
    fun TrainingEntry(key: TrainingNavKey) {
        TrainingScreen(
            routeKey = key,
            navigation = navigation,
            viewModel = viewModel,
            onOpenCoach = onOpenCoach,
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
            entry<TrainingHomeNavKey> { TrainingEntry(it) }
            entry<TrainingRoutineLibraryNavKey>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = { Text(stringResource(R.string.training_select_routine)) },
                ),
            ) { TrainingEntry(it) }
            entry<TrainingRoutineDetailNavKey>(metadata = ListDetailSceneStrategy.detailPane()) { TrainingEntry(it) }
            entry<TrainingRoutineEditorNavKey> { TrainingEntry(it) }
            entry<TrainingExerciseDetailNavKey>(metadata = ListDetailSceneStrategy.extraPane()) { TrainingEntry(it) }
            entry<TrainingExercisePickerNavKey> { TrainingEntry(it) }
            entry<TrainingHistoryNavKey>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = { Text(stringResource(R.string.training_select_workout)) },
                ),
            ) { TrainingEntry(it) }
            entry<TrainingWorkoutHistoryDetailNavKey>(metadata = ListDetailSceneStrategy.detailPane()) { TrainingEntry(it) }
            entry<TrainingActiveWorkoutNavKey> { TrainingEntry(it) }
            entry<TrainingProgressFeatureNavKey> {
                TrainingProgressScreen(onBack = navigation.back)
            }
        },
    )
}

private fun closeTrainingRoute(viewModel: TrainingViewModel, key: TrainingNavKey) {
    when (key) {
        TrainingActiveWorkoutNavKey -> viewModel.closeActiveWorkoutRoute()

        TrainingExercisePickerNavKey -> viewModel.closeRoutineExercisePicker()

        is TrainingRoutineEditorNavKey -> viewModel.closeRoutineEditor()

        is TrainingExerciseDetailNavKey -> viewModel.closeExerciseDetail()

        is TrainingRoutineDetailNavKey -> viewModel.closeRoutineDetail()

        TrainingRoutineLibraryNavKey -> viewModel.closeRoutineLibraryPage()

        is TrainingWorkoutHistoryDetailNavKey -> viewModel.closeWorkoutDetail()

        TrainingHomeNavKey,
        TrainingHistoryNavKey,
        TrainingProgressFeatureNavKey,
        -> Unit
    }
}

private fun prepareTrainingRoute(
    viewModel: TrainingViewModel,
    key: TrainingNavKey,
    onMissing: () -> Unit,
) {
    val state = viewModel.state.value
    when (key) {
        TrainingHomeNavKey,
        TrainingHistoryNavKey,
        TrainingProgressFeatureNavKey,
        TrainingActiveWorkoutNavKey,
        -> Unit

        TrainingRoutineLibraryNavKey -> viewModel.openRoutineLibraryPage()

        is TrainingRoutineDetailNavKey -> {
            if (state.selectedRoutineDetail?.id != key.routineId) viewModel.openRoutineDetail(key.routineId, onMissing)
        }

        is TrainingRoutineEditorNavKey -> {
            if (!state.routineEditor.isOpen || state.routineEditor.routineId != key.routineId) {
                viewModel.openRoutineEditor(key.routineId, onMissing)
            }
        }

        is TrainingExerciseDetailNavKey -> {
            if (state.selectedExerciseDetail?.id != key.exerciseId || state.exerciseDetailTarget != key.target) {
                viewModel.openRoutineExerciseDetail(key.exerciseId, key.target, onMissing)
            }
        }

        // The picker draft is part of restored ViewModel state. Reset it only from the
        // explicit user action that starts a new picker session, not while rehydrating
        // an already-current destination after host or process recreation.
        TrainingExercisePickerNavKey -> Unit

        is TrainingWorkoutHistoryDetailNavKey -> {
            if (state.selectedWorkoutDetail?.summary?.sessionId != key.sessionId) {
                viewModel.openWorkoutDetail(key.sessionId, onMissing)
            }
        }
    }
}
