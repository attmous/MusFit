package com.musfit.ui.training

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.musfit.data.repository.ExerciseSummary
import com.musfit.data.repository.RoutineDetail
import com.musfit.data.repository.RoutineExerciseInput
import com.musfit.data.repository.RoutineFolder
import com.musfit.data.repository.RoutineSetInput
import com.musfit.data.repository.RoutineSummary
import com.musfit.domain.training.RoutineDisplayCalculator
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import kotlin.math.roundToInt

@Composable
fun TrainingHomeContent(
    hasActiveWorkout: Boolean = false,
    routines: List<RoutineSummary> = emptyList(),
    folders: List<RoutineFolder> = emptyList(),
    folderEditor: RoutineFolderEditorState = RoutineFolderEditorState(),
    accent: TabAccent,
    onStartBlankWorkout: () -> Unit,
    onNewRoutine: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenFolderEditor: (String?) -> Unit = {},
    onFolderNameChange: (String) -> Unit = {},
    onSaveFolder: () -> Unit = {},
    onCancelFolder: () -> Unit = {},
    onDeleteFolder: (String) -> Unit = {},
    onAssignRoutineToFolder: (String, String?) -> Unit = { _, _ -> },
    onStartRoutine: (String) -> Unit = {},
    onEditRoutine: (String?) -> Unit = {},
    onOpenRoutineDetail: (String) -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RoutineHomeQuickActions(
            hasActiveWorkout = hasActiveWorkout,
            accent = accent,
            onStartBlankWorkout = onStartBlankWorkout,
            onNewRoutine = onNewRoutine,
            onOpenLibrary = onOpenLibrary,
        )
        // Your own routines live here, organized into folders you create and drag them into.
        TrainingRoutineContent(
            routines = routines,
            folders = folders,
            folderEditor = folderEditor,
            accent = accent,
            onOpenFolderEditor = onOpenFolderEditor,
            onFolderNameChange = onFolderNameChange,
            onSaveFolder = onSaveFolder,
            onCancelFolder = onCancelFolder,
            onDeleteFolder = onDeleteFolder,
            onAssignRoutineToFolder = onAssignRoutineToFolder,
            onStartRoutine = onStartRoutine,
            onEditRoutine = onEditRoutine,
            onOpenRoutineDetail = onOpenRoutineDetail,
        )
    }
}

@Composable
fun TrainingRoutineContent(
    routines: List<RoutineSummary>,
    folders: List<RoutineFolder> = emptyList(),
    folderEditor: RoutineFolderEditorState = RoutineFolderEditorState(),
    accent: TabAccent,
    onOpenFolderEditor: (String?) -> Unit = {},
    onFolderNameChange: (String) -> Unit = {},
    onSaveFolder: () -> Unit = {},
    onCancelFolder: () -> Unit = {},
    onDeleteFolder: (String) -> Unit = {},
    onAssignRoutineToFolder: (String, String?) -> Unit = { _, _ -> },
    onStartRoutine: (String) -> Unit,
    onEditRoutine: (String?) -> Unit,
    onOpenRoutineDetail: (String) -> Unit,
) {
    var draggedRoutine by remember { mutableStateOf<RoutineDragState?>(null) }
    var dropTargetBounds by remember { mutableStateOf<Map<String?, Rect>>(emptyMap()) }
    var contentBounds by remember { mutableStateOf(Rect(0f, 0f, 0f, 0f)) }
    val activeDropTarget = draggedRoutine?.let { drag ->
        routineFolderDropTargetAt(position = drag.position, targetBounds = dropTargetBounds)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { contentBounds = it.boundsInRoot() },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { onOpenFolderEditor(null) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = accent.container, contentColor = accent.onContainer),
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New folder")
            }
            if (folderEditor.isOpen) {
                RoutineFolderEditorCard(
                    editor = folderEditor,
                    accent = accent,
                    onNameChange = onFolderNameChange,
                    onSave = onSaveFolder,
                    onCancel = onCancelFolder,
                    onDelete = folderEditor.folderId?.let { id -> { onDeleteFolder(id) } },
                )
            }
            val routineGroups = groupRoutineSummariesByFolder(
                routines = routines,
                folders = folders,
            )
            if (draggedRoutine != null && folders.isNotEmpty()) {
                Text(
                    text = "Drop the routine onto a folder to move it, or onto “My routines” to take it out.",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accent.color,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            if (routineGroups.isEmpty()) {
                Surface(
                    color = MusFitTheme.colors.surface,
                    shape = MusFitTheme.shapes.large,
                    border = BorderStroke(0.5.dp, MusFitTheme.colors.outline),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Create a routine and it will appear here. Add folders to group them, then drag routines in.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MusFitTheme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                    )
                }
            }
            if (routineGroups.isNotEmpty()) {
                routineGroups.forEach { group ->
                    // A section is a drop target when it's a user folder (drop in) or the
                    // "My routines" bucket (drop out). Registering the whole section — not just a
                    // chip — lets a dragged routine land wherever the user naturally aims it.
                    val isDroppable = group.isUserFolder || group.title == MY_ROUTINES_GROUP_TITLE
                    val isActiveDropTarget = draggedRoutine != null &&
                        isDroppable &&
                        activeDropTarget == RoutineFolderDropTarget(group.folderId)
                    val sectionModifier = if (isDroppable) {
                        Modifier.onGloballyPositioned { coordinates ->
                            dropTargetBounds = dropTargetBounds + (group.folderId to coordinates.boundsInRoot())
                        }
                    } else {
                        Modifier
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = sectionModifier,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = group.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isActiveDropTarget) accent.color else MusFitTheme.colors.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            if (group.isUserFolder && group.folderId != null) {
                                IconButton(
                                    onClick = { onOpenFolderEditor(group.folderId) },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = "Edit ${group.title}",
                                        tint = MusFitTheme.colors.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                        Surface(
                            color = if (isActiveDropTarget) accent.container else MusFitTheme.colors.surface,
                            shape = MusFitTheme.shapes.large,
                            border = if (isActiveDropTarget) {
                                BorderStroke(1.5.dp, accent.color)
                            } else {
                                BorderStroke(0.5.dp, MusFitTheme.colors.outline)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column {
                                if (group.routines.isEmpty()) {
                                    Text(
                                        text = if (isDroppable && draggedRoutine != null) {
                                            "Drop here to add"
                                        } else {
                                            "No routines yet"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MusFitTheme.colors.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                                    )
                                } else {
                                    group.routines.forEachIndexed { index, routine ->
                                        RoutineRow(
                                            routine = routine,
                                            accent = accent,
                                            folders = folders,
                                            isDragging = draggedRoutine?.routineId == routine.id,
                                            onOpenDetail = { onOpenRoutineDetail(routine.id) },
                                            onStart = { onStartRoutine(routine.id) },
                                            onMoveToFolder = { folderId -> onAssignRoutineToFolder(routine.id, folderId) },
                                            onDragStart = { position ->
                                                draggedRoutine = RoutineDragState(
                                                    routineId = routine.id,
                                                    routineName = routine.name,
                                                    position = position,
                                                )
                                            },
                                            onDrag = { dragAmount ->
                                                draggedRoutine = draggedRoutine?.let { drag ->
                                                    drag.copy(position = drag.position + dragAmount)
                                                }
                                            },
                                            onDragEnd = {
                                                val drag = draggedRoutine
                                                val target = drag?.let {
                                                    routineFolderDropTargetAt(it.position, dropTargetBounds)
                                                }
                                                if (drag != null && target != null) {
                                                    onAssignRoutineToFolder(drag.routineId, target.folderId)
                                                }
                                                draggedRoutine = null
                                            },
                                            onDragCancel = { draggedRoutine = null },
                                        )
                                        if (index < group.routines.lastIndex) {
                                            HorizontalDivider(
                                                thickness = 0.5.dp,
                                                color = MusFitTheme.colors.outline,
                                                modifier = Modifier.padding(start = 61.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        draggedRoutine?.let { drag ->
            RoutineDragLabel(drag = drag, contentBounds = contentBounds, accent = accent)
        }
    }
}

@Composable
private fun RoutineHomeQuickActions(
    hasActiveWorkout: Boolean,
    accent: TabAccent,
    onStartBlankWorkout: () -> Unit,
    onNewRoutine: () -> Unit,
    onOpenLibrary: () -> Unit,
) {
    val actions = routineHomeQuickActions(hasActiveWorkout)
    val secondaryActions = actions.filterNot { it == ROUTINE_HOME_ACTION_START_EMPTY }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (ROUTINE_HOME_ACTION_START_EMPTY in actions) {
            Button(
                onClick = onStartBlankWorkout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.onColor),
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(ROUTINE_HOME_ACTION_START_EMPTY)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            secondaryActions.forEach { action ->
                val onClick: () -> Unit = when (action) {
                    ROUTINE_HOME_ACTION_NEW_ROUTINE -> onNewRoutine
                    ROUTINE_HOME_ACTION_LIBRARY -> onOpenLibrary
                    else -> ({})
                }
                val icon = when (action) {
                    ROUTINE_HOME_ACTION_LIBRARY -> Icons.Outlined.FitnessCenter
                    else -> Icons.Outlined.Add
                }
                Surface(
                    onClick = onClick,
                    color = MusFitTheme.colors.surface,
                    shape = MusFitTheme.shapes.large,
                    border = BorderStroke(0.5.dp, MusFitTheme.colors.outline),
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = accent.color, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = action,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MusFitTheme.colors.onSurface,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Plain, non-organizable routine list used by the routine Library to browse pre-made (starter)
 * routines. Folder management + drag-and-drop live on the Home tab for the user's own routines.
 */
@Composable
fun TrainingRoutineLibraryList(
    routines: List<RoutineSummary>,
    accent: TabAccent,
    onStartRoutine: (String) -> Unit,
    onOpenRoutineDetail: (String) -> Unit,
    heading: String? = "Pre-made routines",
    emptyMessage: String = "No pre-made routines available.",
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (heading != null) {
            Text(
                text = heading,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MusFitTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        Surface(
            color = MusFitTheme.colors.surface,
            shape = MusFitTheme.shapes.large,
            border = BorderStroke(0.5.dp, MusFitTheme.colors.outline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (routines.isEmpty()) {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                )
            } else {
                Column {
                    routines.forEachIndexed { index, routine ->
                        RoutineRow(
                            routine = routine,
                            accent = accent,
                            onOpenDetail = { onOpenRoutineDetail(routine.id) },
                            onStart = { onStartRoutine(routine.id) },
                        )
                        if (index < routines.lastIndex) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MusFitTheme.colors.outline,
                                modifier = Modifier.padding(start = 61.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dense, tappable routine row: tinted leading icon, name + meta + muscle chips, and a compact tonal
 * start button. Tapping the row body opens the routine detail; top-level quick actions carry
 * empty-workout and creation flows, so every per-routine start remains compact.
 */
@Composable
private fun RoutineRow(
    routine: RoutineSummary,
    accent: TabAccent,
    folders: List<RoutineFolder> = emptyList(),
    isDragging: Boolean = false,
    onOpenDetail: () -> Unit,
    onStart: () -> Unit,
    onMoveToFolder: (String?) -> Unit = {},
    onDragStart: (Offset) -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    enableMoveControls: Boolean = folders.isNotEmpty(),
) {
    val estimatedMinutes = RoutineDisplayCalculator.estimatedMinutes(routine.targetSetCount)
    val meta = buildString {
        append("${routine.exerciseCount} exercises · ${routine.targetSetCount} sets")
        if (estimatedMinutes > 0) append(" · ~$estimatedMinutes min")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = if (isDragging) 0.5f else 1f)
            .clickable(onClick = onOpenDetail)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        if (enableMoveControls) {
            RoutineDragHandle(
                routineId = routine.id,
                accent = accent,
                onDragStart = onDragStart,
                onDrag = onDrag,
                onDragEnd = onDragEnd,
                onDragCancel = onDragCancel,
            )
        } else {
            RoutineLeadingIcon(accent = accent)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                routine.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MusFitTheme.colors.onSurface,
            )
            Text(
                meta,
                style = MaterialTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
            routineDescription(routine)?.let { description ->
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
            if (routine.muscleGroups.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    routine.muscleGroups.forEach { muscle -> RoutineMuscleChip(muscle) }
                }
            }
        }
        if (enableMoveControls) {
            RoutineMoveMenu(
                routineName = routine.name,
                folders = folders,
                selectedFolderId = routine.folderId,
                accent = accent,
                onMoveToFolder = onMoveToFolder,
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accent.container)
                .clickable(onClick = onStart),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = "Start ${routine.name}",
                tint = accent.onContainer,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun RoutineLeadingIcon(accent: TabAccent) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(accent.container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.FitnessCenter,
            contentDescription = null,
            tint = accent.onContainer,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun RoutineMoveMenu(
    routineName: String,
    folders: List<RoutineFolder>,
    selectedFolderId: String?,
    accent: TabAccent,
    onMoveToFolder: (String?) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = "Move $routineName",
                tint = MusFitTheme.colors.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            routineFolderMoveTargets(folders).forEach { target ->
                DropdownMenuItem(
                    text = { Text(target.label) },
                    leadingIcon = if (target.folderId == selectedFolderId) {
                        {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = accent.color,
                            )
                        }
                    } else {
                        null
                    },
                    onClick = {
                        open = false
                        onMoveToFolder(target.folderId)
                    },
                )
            }
        }
    }
}

@Composable
private fun RoutineDragHandle(
    routineId: String,
    accent: TabAccent,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(accent.container)
            .onGloballyPositioned { coordinates = it }
            .pointerInput(routineId) {
                detectDragGestures(
                    onDragStart = { offset ->
                        coordinates?.localToRoot(offset)?.let(onDragStart)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragCancel,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.DragIndicator,
            contentDescription = "Move routine",
            tint = accent.onContainer,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun RoutineDragLabel(
    drag: RoutineDragState,
    contentBounds: Rect,
    accent: TabAccent,
) {
    Surface(
        color = accent.color,
        shape = RoundedCornerShape(999.dp),
        shadowElevation = 4.dp,
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (drag.position.x - contentBounds.left + 12f).roundToInt(),
                    y = (drag.position.y - contentBounds.top + 12f).roundToInt(),
                )
            }
            .zIndex(2f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.FitnessCenter,
                contentDescription = null,
                tint = accent.onColor,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = drag.routineName,
                style = MaterialTheme.typography.labelMedium,
                color = accent.onColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.widthIn(max = 220.dp),
            )
        }
    }
}

/**
 * Read-only preview of a routine opened from a row tap: header with a primary Start, summary meta +
 * muscle chips, the exercise list with planned sets x reps, and secondary actions (edit/duplicate/
 * delete, gated the same way as the row overflow used to be).
 */
@Composable
fun RoutineDetailContent(
    detail: RoutineDetail,
    accent: TabAccent,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onOpenExercise: (exerciseId: String, target: String) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    val totalSets = detail.exercises.sumOf { it.targetSets }
    val estimatedMinutes = RoutineDisplayCalculator.estimatedMinutes(totalSets)
    val muscles = RoutineDisplayCalculator.topMuscles(
        detail.exercises.joinToString(",") { it.exercise.primaryMuscles },
        limit = 4,
    )
    val actions = routineCardActions(detail.isStarter)
    val meta = buildString {
        append("${detail.exercises.size} exercises · $totalSets sets")
        if (estimatedMinutes > 0) append(" · ~$estimatedMinutes min")
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onClose, colors = ButtonDefaults.textButtonColors(contentColor = accent.color)) {
                Text("Back")
            }
            Text(
                text = detail.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MusFitTheme.colors.onSurface,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.onColor),
            ) {
                Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Start")
            }
        }
        Text(meta, style = MaterialTheme.typography.bodyMedium, color = MusFitTheme.colors.onSurfaceVariant)
        val folder = detail.folderName?.takeIf { it.isNotBlank() }
        if (folder != null || muscles.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                folder?.let { RoutineProgramTag(label = it, accent = accent) }
                muscles.forEach { muscle -> RoutineMuscleChip(muscle) }
            }
        }
        if (detail.notes?.isNotBlank() == true) {
            Text(detail.notes, style = MaterialTheme.typography.bodySmall, color = MusFitTheme.colors.onSurfaceVariant)
        }
        if (detail.exercises.isEmpty()) {
            Text(
                "This routine has no exercises yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        } else {
            Surface(
                color = MusFitTheme.colors.surface,
                shape = MusFitTheme.shapes.large,
                border = BorderStroke(0.5.dp, MusFitTheme.colors.outline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    detail.exercises.forEachIndexed { index, exercise ->
                        RoutineDetailExerciseRow(
                            position = index + 1,
                            exercise = exercise,
                            accent = accent,
                            onOpen = { target -> onOpenExercise(exercise.exercise.id, target) },
                        )
                        if (index < detail.exercises.lastIndex) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MusFitTheme.colors.outline,
                                modifier = Modifier.padding(start = 66.dp),
                            )
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (ROUTINE_ACTION_EDIT in actions) {
                TextButton(onClick = onEdit, colors = ButtonDefaults.textButtonColors(contentColor = accent.color)) {
                    Text("Edit")
                }
            }
            if (ROUTINE_ACTION_DUPLICATE in actions) {
                TextButton(onClick = onDuplicate, colors = ButtonDefaults.textButtonColors(contentColor = accent.color)) {
                    Text("Duplicate")
                }
            }
            if (ROUTINE_ACTION_DELETE in actions) {
                TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun RoutineDetailExerciseRow(
    position: Int,
    exercise: com.musfit.data.repository.RoutineExerciseDetail,
    accent: TabAccent,
    onOpen: (target: String) -> Unit,
) {
    val target = if (!exercise.targetReps.isNullOrBlank()) {
        "${exercise.targetSets} x ${exercise.targetReps}"
    } else {
        "${exercise.targetSets} sets"
    }
    val setTypeSummary = exercise.setPlans
        .takeIf { it.isNotEmpty() }
        ?.mapIndexed { index, setPlan -> routineSetTypeToken(setPlan.setType, index) }
        ?.joinToString(" ")
    val restSummary = exercise.restSeconds?.let { "Rest ${it}s" }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(target) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val imageUrl = exercise.exercise.imageUrl
        if (imageUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.container),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    position.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent.onContainer,
                )
            }
        } else {
            ExerciseThumb(
                imageUrl = imageUrl,
                contentDescription = exercise.exercise.name,
                accent = accent,
                size = 44.dp,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                exercise.exercise.name,
                style = MaterialTheme.typography.titleSmall,
                color = MusFitTheme.colors.onSurface,
            )
            val subtitle = listOfNotNull(
                exercise.exercise.equipment,
                exercise.exercise.targetMuscles.takeIf(String::isNotBlank),
                restSummary,
                setTypeSummary,
            ).joinToString(" - ")
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MusFitTheme.colors.onSurfaceVariant)
            }
        }
        Text(target, style = MaterialTheme.typography.labelLarge, color = accent.color)
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MusFitTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun RoutineProgramTag(label: String, accent: TabAccent) {
    Surface(color = accent.container, shape = RoundedCornerShape(999.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = accent.onContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun RoutineFolderEditorCard(
    editor: RoutineFolderEditorState,
    accent: TabAccent,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = accent.color,
        focusedLabelColor = accent.color,
        cursorColor = accent.color,
    )
    Surface(
        color = MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.large,
        border = BorderStroke(0.5.dp, MusFitTheme.colors.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (editor.folderId == null) "New folder" else "Edit folder",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MusFitTheme.colors.onSurface,
            )
            OutlinedTextField(
                value = editor.name,
                onValueChange = onNameChange,
                label = { Text("Folder name") },
                singleLine = true,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onSave,
                    enabled = editor.name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.onColor),
                ) {
                    Text("Save")
                }
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = accent.color)
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineMuscleChip(label: String) {
    Surface(
        color = MusFitTheme.colors.surface,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(0.5.dp, MusFitTheme.colors.outline),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MusFitTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun RoutineProgramChip(
    label: String,
    selected: Boolean,
    accent: TabAccent,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = if (selected) accent.container else MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.large,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(0.5.dp, MusFitTheme.colors.outline),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) accent.onContainer else MusFitTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

internal fun nextQuickLogExpanded(current: Boolean): Boolean = !current

internal const val MY_ROUTINES_GROUP_TITLE = "My routines"

internal data class RoutineGroup(
    val title: String,
    val routines: List<RoutineSummary>,
    /** Non-null only for real user folders; null for the "My routines" bucket and legacy name-only groups. */
    val folderId: String? = null,
    /** True only when this group is a user-created folder that can be renamed/deleted and dropped into. */
    val isUserFolder: Boolean = false,
)

internal data class RoutineFolderDropTarget(val folderId: String?)

internal data class RoutineFolderMoveTarget(
    val folderId: String?,
    val label: String,
)

private data class RoutineDragState(
    val routineId: String,
    val routineName: String,
    val position: Offset,
)

internal fun routineFolderDropTargetAt(
    position: Offset,
    targetBounds: Map<String?, Rect>,
): RoutineFolderDropTarget? =
    targetBounds.entries
        .firstOrNull { (_, bounds) -> bounds.contains(position) }
        ?.let { (folderId, _) -> RoutineFolderDropTarget(folderId) }

internal fun routineFolderMoveTargets(folders: List<RoutineFolder>): List<RoutineFolderMoveTarget> =
    listOf(RoutineFolderMoveTarget(folderId = null, label = "My routines")) +
        folders.map { folder -> RoutineFolderMoveTarget(folderId = folder.id, label = folder.name) }

internal fun groupRoutineSummariesByFolder(
    routines: List<RoutineSummary>,
    folders: List<RoutineFolder> = emptyList(),
): List<RoutineGroup> {
    if (folders.isNotEmpty()) {
        val folderGroups = folders.map { folder ->
            RoutineGroup(
                title = folder.name,
                routines = routines.filter { routine -> routine.belongsToFolder(folder) },
                folderId = folder.id,
                isUserFolder = true,
            )
        }
        val remainingGroups = linkedMapOf<String, MutableList<RoutineSummary>>()
        routines
            .filterNot { routine -> folders.any { folder -> routine.belongsToFolder(folder) } }
            .forEach { routine ->
                val title = routine.folderName
                    ?.trim()
                    ?.takeIf(String::isNotBlank)
                    ?: MY_ROUTINES_GROUP_TITLE
                remainingGroups.getOrPut(title) { mutableListOf() } += routine
            }
        return folderGroups + remainingGroups.map { (title, groupedRoutines) ->
            RoutineGroup(title = title, routines = groupedRoutines)
        }
    }

    val groups = linkedMapOf<String, MutableList<RoutineSummary>>()
    routines.forEach { routine ->
        val title = routine.folderName
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?: MY_ROUTINES_GROUP_TITLE
        groups.getOrPut(title) { mutableListOf() } += routine
    }
    return groups.map { (title, groupedRoutines) ->
        RoutineGroup(title = title, routines = groupedRoutines)
    }
}

private fun RoutineSummary.belongsToFolder(folder: RoutineFolder): Boolean {
    val normalizedFolderName = folder.name.trim()
    val normalizedRoutineFolderName = folderName?.trim()
    return folderId == folder.id ||
        normalizedRoutineFolderName?.equals(normalizedFolderName, ignoreCase = true) == true
}

internal fun routineCardActions(isStarter: Boolean): List<String> =
    if (isStarter) {
        listOf(ROUTINE_ACTION_START, ROUTINE_ACTION_EDIT, ROUTINE_ACTION_DUPLICATE)
    } else {
        listOf(ROUTINE_ACTION_START, ROUTINE_ACTION_EDIT, ROUTINE_ACTION_DUPLICATE, ROUTINE_ACTION_DELETE)
    }

internal fun routineHomeQuickActions(hasActiveWorkout: Boolean = false): List<String> =
    buildList {
        if (!hasActiveWorkout) add(ROUTINE_HOME_ACTION_START_EMPTY)
        add(ROUTINE_HOME_ACTION_NEW_ROUTINE)
        add(ROUTINE_HOME_ACTION_LIBRARY)
    }

internal fun routineDescription(routine: RoutineSummary): String? =
    routine.notes?.trim()?.takeIf(String::isNotBlank)
        ?: if (routine.isStarter) "Pre-saved routine" else null

private const val ROUTINE_HOME_ACTION_START_EMPTY = "Start empty workout"
private const val ROUTINE_HOME_ACTION_NEW_ROUTINE = "New routine"
private const val ROUTINE_HOME_ACTION_LIBRARY = "Browse library"
private const val ROUTINE_ACTION_START = "Start"
private const val ROUTINE_ACTION_EDIT = "Edit"
private const val ROUTINE_ACTION_DUPLICATE = "Duplicate"
private const val ROUTINE_ACTION_DELETE = "Delete"

/** Result of validating a routine target field for display (storage still coerces independently). */
sealed interface TargetFieldResult {
    object Valid : TargetFieldResult
    data class Invalid(val message: String) : TargetFieldResult
}

internal fun validateTargetSets(raw: String): TargetFieldResult {
    val sets = raw.trim().toIntOrNull()
    return if (sets != null && sets in 1..20) {
        TargetFieldResult.Valid
    } else {
        TargetFieldResult.Invalid("Enter 1-20 sets")
    }
}

internal fun validateTargetReps(raw: String): TargetFieldResult {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return TargetFieldResult.Valid
    val invalid = TargetFieldResult.Invalid("Use a number or range, e.g. 8 or 8-12")
    if (trimmed.contains('-')) {
        val parts = trimmed.split('-').map { it.trim() }
        if (parts.size != 2) return invalid
        val low = parts[0].toIntOrNull() ?: return invalid
        val high = parts[1].toIntOrNull() ?: return invalid
        return if (low in 1..100 && high in 1..100 && low <= high) TargetFieldResult.Valid else invalid
    }
    val single = trimmed.toIntOrNull() ?: return invalid
    return if (single in 1..100) TargetFieldResult.Valid else invalid
}

internal fun routineEditorCanSave(name: String, exercises: List<RoutineExerciseInput>): Boolean {
    if (name.isBlank()) return false
    if (exercises.isEmpty()) return false
    return exercises.all { exercise ->
        val restValid = exercise.restSeconds == null || exercise.restSeconds in 15..900
        val setPlans = exercise.setPlans.ifEmpty { defaultRoutineEditorSetPlans(exercise.targetSets, exercise.targetReps) }
        validateTargetSets(exercise.targetSets.toString()) is TargetFieldResult.Valid &&
            validateTargetReps(exercise.targetReps.orEmpty()) is TargetFieldResult.Valid &&
            restValid &&
            setPlans.isNotEmpty() &&
            setPlans.all { setPlan ->
                setPlan.setType.lowercase() in setOf("warmup", "working", "failure", "drop") &&
                    validateTargetReps(setPlan.targetReps.orEmpty()) is TargetFieldResult.Valid &&
                    (setPlan.targetWeightKg == null || setPlan.targetWeightKg > 0.0)
            }
    }
}

@Composable
fun TrainingRoutineEditor(
    editor: RoutineEditorState,
    exercises: List<ExerciseSummary>,
    folders: List<RoutineFolder>,
    accent: TabAccent,
    onNameChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onFolderNameChange: (String) -> Unit,
    onOpenExercisePicker: () -> Unit,
    onRemoveExercise: (Int) -> Unit,
    onMoveExerciseUp: (Int) -> Unit,
    onMoveExerciseDown: (Int) -> Unit,
    onTargetSetsChange: (Int, String) -> Unit,
    onTargetRepsChange: (Int, String) -> Unit,
    onRestSecondsChange: (Int, String) -> Unit,
    onAddSet: (Int) -> Unit,
    onRemoveSet: (Int, Int) -> Unit,
    onSetTypeChange: (Int, Int, String) -> Unit,
    onSetRepsChange: (Int, Int, String) -> Unit,
    onSetWeightChange: (Int, Int, String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDuplicate: ((String) -> Unit)? = null,
    onDelete: ((String) -> Unit)? = null,
) {
    val exerciseMap = remember(exercises) { exercises.associateBy { it.id } }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = accent.color,
        focusedLabelColor = accent.color,
        cursorColor = accent.color,
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onCancel, colors = ButtonDefaults.textButtonColors(contentColor = accent.color)) {
                Text("Cancel")
            }
            Text(
                text = if (editor.routineId == null) "New routine" else "Edit routine",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onSave,
                enabled = routineEditorCanSave(editor.name, editor.exercises),
                colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.onColor),
            ) {
                Text("Save")
            }
        }
        OutlinedTextField(
            value = editor.name,
            onValueChange = onNameChange,
            label = { Text("Name") },
            singleLine = true,
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = editor.notes,
            onValueChange = onNotesChange,
            label = { Text("Notes") },
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = editor.folderName,
            onValueChange = onFolderNameChange,
            label = { Text("Folder") },
            singleLine = true,
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth(),
        )
        if (folders.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                folders.forEach { folder ->
                    RoutinePickerChip(
                        label = folder.name,
                        selected = editor.folderName.equals(folder.name, ignoreCase = true),
                        accent = accent,
                        onClick = { onFolderNameChange(folder.name) },
                    )
                }
            }
        }
        Button(
            onClick = onOpenExercisePicker,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = accent.container, contentColor = accent.onContainer),
        ) {
            Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (editor.exercises.isEmpty()) "Add exercises" else "Add more exercises")
        }
        if (editor.exercises.isEmpty()) {
            Text(
                text = "Add at least one exercise to start this routine",
                style = MaterialTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        }
        editor.exercises.forEachIndexed { index, exercise ->
            val exerciseSummary = exerciseMap[exercise.exerciseId]
            val setsError = (validateTargetSets(exercise.targetSets.toString()) as? TargetFieldResult.Invalid)?.message
            val repsError = (validateTargetReps(exercise.targetReps.orEmpty()) as? TargetFieldResult.Invalid)?.message
            RoutineEditorExerciseCard(
                position = index + 1,
                accent = accent,
                fieldColors = fieldColors,
                setsError = setsError,
                repsError = repsError,
                exerciseName = exerciseSummary?.name ?: "Unknown exercise",
                exerciseMeta = exerciseSummary?.let {
                    listOfNotNull(it.equipment, it.targetMuscles.takeIf(String::isNotBlank))
                        .joinToString(" - ")
                }.orEmpty(),
                targetSets = exercise.targetSets.toString(),
                targetReps = exercise.targetReps.orEmpty(),
                restSeconds = exercise.restSeconds?.toString().orEmpty(),
                setPlans = exercise.setPlans.ifEmpty { defaultRoutineEditorSetPlans(exercise.targetSets, exercise.targetReps) },
                canMoveUp = index > 0,
                canMoveDown = index < editor.exercises.lastIndex,
                onMoveUp = { onMoveExerciseUp(index) },
                onMoveDown = { onMoveExerciseDown(index) },
                onRemove = { onRemoveExercise(index) },
                onTargetSetsChange = { onTargetSetsChange(index, it) },
                onTargetRepsChange = { onTargetRepsChange(index, it) },
                onRestSecondsChange = { onRestSecondsChange(index, it) },
                onAddSet = { onAddSet(index) },
                onRemoveSet = { setIndex -> onRemoveSet(index, setIndex) },
                onSetTypeChange = { setIndex, setType -> onSetTypeChange(index, setIndex, setType) },
                onSetRepsChange = { setIndex, value -> onSetRepsChange(index, setIndex, value) },
                onSetWeightChange = { setIndex, value -> onSetWeightChange(index, setIndex, value) },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (editor.routineId != null && onDuplicate != null) {
                TextButton(
                    onClick = { onDuplicate(editor.routineId) },
                    colors = ButtonDefaults.textButtonColors(contentColor = accent.color),
                ) {
                    Text("Duplicate")
                }
            }
            if (editor.routineId != null && !editor.isStarter && onDelete != null) {
                TextButton(
                    onClick = { onDelete(editor.routineId) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
fun RoutineExercisePickerPage(
    exercises: List<ExerciseSummary>,
    currentRoutineExerciseIds: Set<String>,
    selectedExerciseIds: Set<String>,
    searchQuery: String,
    muscleFilter: String?,
    equipmentFilter: String?,
    accent: TabAccent,
    onSearchChange: (String) -> Unit,
    onMuscleFilterChange: (String?) -> Unit,
    onEquipmentFilterChange: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onToggleExercise: (String) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val options = routineExercisePickerOptions(exercises)
    val availableExercises = exercises.filterNot { it.id in currentRoutineExerciseIds }
    val visibleExercises = routineExercisePickerSuggestions(
        exercises = availableExercises,
        selectedExerciseIds = emptySet(),
        query = searchQuery,
        equipmentFilter = equipmentFilter,
        muscleFilter = muscleFilter,
    )
    val filtersActive = searchQuery.isNotBlank() || muscleFilter != null || equipmentFilter != null
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = accent.color,
        focusedLabelColor = accent.color,
        cursorColor = accent.color,
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MusFitTheme.colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Cancel exercise selection",
                        tint = MusFitTheme.colors.onSurface,
                    )
                }
                Text(
                    "Add exercises",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MusFitTheme.colors.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = onConfirm,
                    enabled = selectedExerciseIds.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.onColor),
                ) {
                    Text("OK")
                }
            }
        }
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                label = { Text("Search exercises") },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                },
                singleLine = true,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            RoutinePickerFilterButtons(
                muscleOptions = options.muscles,
                equipmentOptions = options.equipment,
                muscleFilter = muscleFilter,
                equipmentFilter = equipmentFilter,
                accent = accent,
                onMuscleFilterChange = onMuscleFilterChange,
                onEquipmentFilterChange = onEquipmentFilterChange,
            )
        }
        if (filtersActive) {
            item {
                TextButton(onClick = onClearFilters, colors = ButtonDefaults.textButtonColors(contentColor = accent.color)) {
                    Text("Clear filters")
                }
            }
        }
        item {
            Text(
                "${visibleExercises.size} available - ${selectedExerciseIds.size} selected",
                style = MaterialTheme.typography.labelLarge,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        }
        if (visibleExercises.isEmpty()) {
            item {
                Text(
                    "No matching exercises",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
        } else {
            items(
                items = visibleExercises,
                key = { exercise -> exercise.id },
            ) { exercise ->
                val selected = exercise.id in selectedExerciseIds
                RoutineExercisePickerRow(
                    exercise = exercise,
                    selected = selected,
                    accent = accent,
                    onToggle = { onToggleExercise(exercise.id) },
                )
            }
        }
    }
}

@Composable
private fun RoutinePickerFilterButtons(
    muscleOptions: List<String>,
    equipmentOptions: List<String>,
    muscleFilter: String?,
    equipmentFilter: String?,
    accent: TabAccent,
    onMuscleFilterChange: (String?) -> Unit,
    onEquipmentFilterChange: (String?) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        RoutinePickerFilterButton(
            label = muscleFilter ?: "Muscle",
            allLabel = "All muscles",
            options = muscleOptions,
            accent = accent,
            modifier = Modifier.weight(1f),
            onSelected = onMuscleFilterChange,
        )
        RoutinePickerFilterButton(
            label = equipmentFilter ?: "All equipment",
            allLabel = "All equipment",
            options = equipmentOptions,
            accent = accent,
            modifier = Modifier.weight(1f),
            onSelected = onEquipmentFilterChange,
        )
    }
}

@Composable
private fun RoutinePickerFilterButton(
    label: String,
    allLabel: String,
    options: List<String>,
    accent: TabAccent,
    modifier: Modifier = Modifier,
    onSelected: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Button(
            onClick = { expanded = true },
            colors = ButtonDefaults.buttonColors(containerColor = accent.container, contentColor = accent.onContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Icon(imageVector = Icons.Outlined.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 360.dp),
        ) {
            DropdownMenuItem(
                text = { Text(allLabel) },
                onClick = {
                    onSelected(null)
                    expanded = false
                },
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun RoutineExercisePickerRow(
    exercise: ExerciseSummary,
    selected: Boolean,
    accent: TabAccent,
    onToggle: () -> Unit,
) {
    Surface(
        onClick = onToggle,
        color = if (selected) accent.container else MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.medium,
        border = BorderStroke(0.5.dp, if (selected) accent.color else MusFitTheme.colors.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ExerciseThumb(
                imageUrl = exercise.thumbnailUrl(),
                contentDescription = exercise.name,
                accent = accent,
                size = 44.dp,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    exercise.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) accent.onContainer else MusFitTheme.colors.onSurface,
                )
                Text(
                    exercise.pickerMeta(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) accent.onContainer else MusFitTheme.colors.onSurfaceVariant,
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Selected",
                    tint = accent.color,
                )
            }
        }
    }
}

@Composable
private fun RoutineExercisePicker(
    exercises: List<ExerciseSummary>,
    selectedExerciseIds: Set<String>,
    accent: TabAccent,
    selectedCount: Int,
    fieldColors: androidx.compose.material3.TextFieldColors,
    query: String,
    equipmentFilter: String?,
    muscleFilter: String?,
    onQueryChange: (String) -> Unit,
    onEquipmentFilterChange: (String) -> Unit,
    onMuscleFilterChange: (String) -> Unit,
    onClearFilters: () -> Unit,
    onAddExercise: (String) -> Unit,
) {
    val options = routineExercisePickerOptions(exercises)
    val suggestions = routineExercisePickerSuggestions(
        exercises = exercises,
        selectedExerciseIds = selectedExerciseIds,
        query = query,
        equipmentFilter = equipmentFilter,
        muscleFilter = muscleFilter,
        limit = 8,
    )
    val filtersActive = query.isNotBlank() || equipmentFilter != null || muscleFilter != null

    Surface(
        color = MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Saved exercises",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MusFitTheme.colors.onSurface,
                    )
                    Text(
                        text = if (selectedCount > 0) "$selectedCount added to this routine" else "Choose from your exercise library",
                        style = MaterialTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
                if (filtersActive) {
                    TextButton(onClick = onClearFilters, colors = ButtonDefaults.textButtonColors(contentColor = accent.color)) {
                        Text("Clear")
                    }
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("Search exercises") },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                },
                singleLine = true,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
            RoutinePickerChipRow(
                title = "Equipment",
                options = options.equipment.take(8),
                selected = equipmentFilter,
                accent = accent,
                onSelected = onEquipmentFilterChange,
            )
            RoutinePickerChipRow(
                title = "Muscle",
                options = options.muscles.take(10),
                selected = muscleFilter,
                accent = accent,
                onSelected = onMuscleFilterChange,
            )
            if (suggestions.isEmpty()) {
                Text(
                    "No matching exercises",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                )
            } else {
                suggestions.forEach { exercise ->
                    Surface(
                        onClick = { onAddExercise(exercise.id) },
                        color = MusFitTheme.colors.background,
                        shape = MusFitTheme.shapes.medium,
                        border = BorderStroke(0.5.dp, MusFitTheme.colors.outline),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    exercise.name,
                                    color = MusFitTheme.colors.onSurface,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                val meta = listOfNotNull(
                                    exercise.equipment,
                                    exercise.targetMuscles.takeIf(String::isNotBlank),
                                ).joinToString(" - ")
                                if (meta.isNotBlank()) {
                                    Text(
                                        text = meta,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MusFitTheme.colors.onSurfaceVariant,
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = "Add ${exercise.name}",
                                tint = accent.color,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutinePickerChipRow(
    title: String,
    options: List<String>,
    selected: String?,
    accent: TabAccent,
    onSelected: (String) -> Unit,
) {
    if (options.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MusFitTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(end = 2.dp),
        )
        options.forEach { option ->
            RoutinePickerChip(
                label = option,
                selected = selected.equals(option, ignoreCase = true),
                accent = accent,
                onClick = { onSelected(option) },
            )
        }
    }
}

@Composable
private fun RoutinePickerChip(
    label: String,
    selected: Boolean,
    accent: TabAccent,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = if (selected) accent.container else MusFitTheme.colors.background,
        shape = RoundedCornerShape(999.dp),
        border = if (selected) null else BorderStroke(0.5.dp, MusFitTheme.colors.outline),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) accent.onContainer else MusFitTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun RoutineEditorExerciseCard(
    position: Int,
    accent: TabAccent,
    fieldColors: androidx.compose.material3.TextFieldColors,
    setsError: String?,
    repsError: String?,
    exerciseName: String,
    exerciseMeta: String,
    targetSets: String,
    targetReps: String,
    restSeconds: String,
    setPlans: List<RoutineSetInput>,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onTargetSetsChange: (String) -> Unit,
    onTargetRepsChange: (String) -> Unit,
    onRestSecondsChange: (String) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit,
    onSetTypeChange: (Int, String) -> Unit,
    onSetRepsChange: (Int, String) -> Unit,
    onSetWeightChange: (Int, String) -> Unit,
) {
    Surface(
        color = MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accent.container),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        position.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = accent.onContainer,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(exerciseName, style = MaterialTheme.typography.titleSmall, color = MusFitTheme.colors.onSurface)
                    if (exerciseMeta.isNotBlank()) {
                        Text(
                            exerciseMeta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MusFitTheme.colors.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowUp,
                        contentDescription = "Move exercise up",
                        tint = if (canMoveUp) accent.color else MusFitTheme.colors.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "Move exercise down",
                        tint = if (canMoveDown) accent.color else MusFitTheme.colors.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Remove exercise",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = targetSets,
                    onValueChange = onTargetSetsChange,
                    label = { Text("Sets") },
                    singleLine = true,
                    isError = setsError != null,
                    colors = fieldColors,
                    supportingText = setsError?.let { message -> { Text(message) } },
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = targetReps,
                    onValueChange = onTargetRepsChange,
                    label = { Text("Reps") },
                    singleLine = true,
                    isError = repsError != null,
                    colors = fieldColors,
                    supportingText = repsError?.let { message -> { Text(message) } },
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = restSeconds,
                    onValueChange = onRestSecondsChange,
                    label = { Text("Rest sec") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.weight(1f),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Set plan",
                        style = MaterialTheme.typography.labelLarge,
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                    TextButton(onClick = onAddSet, colors = ButtonDefaults.textButtonColors(contentColor = accent.color)) {
                        Icon(imageVector = Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Set")
                    }
                }
                setPlans.forEachIndexed { setIndex, setPlan ->
                    RoutineEditorSetRow(
                        setIndex = setIndex,
                        setPlan = setPlan,
                        accent = accent,
                        fieldColors = fieldColors,
                        canRemove = setPlans.size > 1,
                        onSetTypeChange = { onSetTypeChange(setIndex, it) },
                        onSetRepsChange = { onSetRepsChange(setIndex, it) },
                        onSetWeightChange = { onSetWeightChange(setIndex, it) },
                        onRemove = { onRemoveSet(setIndex) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RoutineEditorSetRow(
    setIndex: Int,
    setPlan: RoutineSetInput,
    accent: TabAccent,
    fieldColors: androidx.compose.material3.TextFieldColors,
    canRemove: Boolean,
    onSetTypeChange: (String) -> Unit,
    onSetRepsChange: (String) -> Unit,
    onSetWeightChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            onClick = { onSetTypeChange(nextRoutineSetType(setPlan.setType)) },
            color = routineSetTypeColor(setPlan.setType, accent),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(42.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    routineSetTypeToken(setPlan.setType, setIndex),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = routineSetTypeContentColor(setPlan.setType, accent),
                )
            }
        }
        OutlinedTextField(
            value = setPlan.targetWeightKg?.formatRoutineWeight().orEmpty(),
            onValueChange = onSetWeightChange,
            label = { Text("Kg") },
            singleLine = true,
            colors = fieldColors,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = setPlan.targetReps.orEmpty(),
            onValueChange = onSetRepsChange,
            label = { Text("Reps") },
            singleLine = true,
            colors = fieldColors,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRemove, enabled = canRemove) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Remove set",
                tint = if (canRemove) MaterialTheme.colorScheme.error else MusFitTheme.colors.onSurfaceVariant,
            )
        }
    }
}

internal data class RoutineExercisePickerOptions(
    val equipment: List<String>,
    val muscles: List<String>,
)

internal fun routineExercisePickerOptions(exercises: List<ExerciseSummary>): RoutineExercisePickerOptions =
    RoutineExercisePickerOptions(
        equipment = exercises.mapNotNull { it.equipment?.trim()?.takeIf(String::isNotBlank) }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() },
        muscles = exercises.flatMap { it.pickerMuscles() }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() },
    )

internal fun routineExercisePickerSuggestions(
    exercises: List<ExerciseSummary>,
    selectedExerciseIds: Set<String>,
    query: String,
    equipmentFilter: String? = null,
    muscleFilter: String? = null,
    limit: Int? = null,
): List<ExerciseSummary> {
    val trimmedQuery = query.trim()
    val available = exercises.filterNot { it.id in selectedExerciseIds }
    val filtered = available.filter { exercise ->
        val matchesQuery = trimmedQuery.isBlank() ||
            exercise.name.contains(trimmedQuery, ignoreCase = true) ||
                exercise.category.contains(trimmedQuery, ignoreCase = true) ||
                exercise.equipment.orEmpty().contains(trimmedQuery, ignoreCase = true) ||
                exercise.targetMuscles.contains(trimmedQuery, ignoreCase = true) ||
                exercise.primaryMuscles.contains(trimmedQuery, ignoreCase = true) ||
                exercise.secondaryMuscles.contains(trimmedQuery, ignoreCase = true)
        val matchesEquipment = equipmentFilter.isNullOrBlank() ||
            exercise.equipment.equals(equipmentFilter, ignoreCase = true)
        val matchesMuscle = muscleFilter.isNullOrBlank() ||
            exercise.pickerMuscles().any { it.equals(muscleFilter, ignoreCase = true) }

        matchesQuery && matchesEquipment && matchesMuscle
    }
    return limit?.let { filtered.take(it) } ?: filtered
}

internal fun defaultRoutineEditorSetPlans(targetSets: Int, targetReps: String?): List<RoutineSetInput> =
    (0 until targetSets.coerceAtLeast(1)).map {
        RoutineSetInput(setType = "working", targetReps = targetReps)
    }

private fun nextRoutineSetType(current: String): String =
    when (current.lowercase()) {
        "warmup" -> "working"
        "working" -> "failure"
        "failure" -> "drop"
        "drop" -> "warmup"
        else -> "working"
    }

private fun routineSetTypeToken(setType: String, setIndex: Int): String =
    when (setType.lowercase()) {
        "warmup" -> "W"
        "failure" -> "F"
        "drop" -> "D"
        else -> (setIndex + 1).toString()
    }

private fun routineSetTypeColor(setType: String, accent: TabAccent): androidx.compose.ui.graphics.Color =
    when (setType.lowercase()) {
        "warmup" -> androidx.compose.ui.graphics.Color(0xFFFFF3CD)
        "failure" -> androidx.compose.ui.graphics.Color(0xFFFFE1DD)
        "drop" -> androidx.compose.ui.graphics.Color(0xFFE0F2FE)
        else -> accent.container
    }

private fun routineSetTypeContentColor(setType: String, accent: TabAccent): androidx.compose.ui.graphics.Color =
    when (setType.lowercase()) {
        "warmup" -> androidx.compose.ui.graphics.Color(0xFF9A6700)
        "failure" -> androidx.compose.ui.graphics.Color(0xFFB42318)
        "drop" -> androidx.compose.ui.graphics.Color(0xFF0277BD)
        else -> accent.onContainer
    }

private fun Double.formatRoutineWeight(): String =
    if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        toString().trimEnd('0').trimEnd('.')
    }

private fun ExerciseSummary.pickerMuscles(): List<String> =
    listOf(targetMuscles, primaryMuscles, secondaryMuscles)
        .flatMap { raw -> raw.split(',', '/', ';') }
        .map { it.trim() }
        .filter { it.isNotBlank() }

private fun ExerciseSummary.thumbnailUrl(): String? = imageUrl ?: gifUrl

private fun ExerciseSummary.pickerMeta(): String =
    listOfNotNull(
        equipment?.displayExerciseToken(),
        category.displayExerciseToken().takeIf(String::isNotBlank),
        primaryMuscles.takeIf(String::isNotBlank)?.displayExerciseToken()
            ?: targetMuscles.takeIf(String::isNotBlank)?.displayExerciseToken(),
        secondaryMuscles.takeIf(String::isNotBlank)?.displayExerciseToken(),
        if (isCustom) "Custom" else "Library",
    ).joinToString(" - ")

private fun String.displayExerciseToken(): String =
    trim()
        .splitToSequence(' ', '-', '_')
        .filter { it.isNotBlank() }
        .joinToString(" ") { token ->
            token.replaceFirstChar { char -> char.titlecase() }
        }
