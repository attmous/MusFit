package com.musfit.ui.training

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.musfit.data.repository.ExerciseSummary
import com.musfit.data.repository.RoutineDetail
import com.musfit.data.repository.RoutineExerciseInput
import com.musfit.data.repository.RoutineFolder
import com.musfit.data.repository.RoutineSetInput
import com.musfit.data.repository.RoutineSummary
import com.musfit.domain.training.RoutineDisplayCalculator
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.NeutralOutline
import com.musfit.ui.theme.NeutralOutlineDark
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
    showLibraryLink: Boolean = true,
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
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Your own routines live here, organized into folders you create and drag them into.
        // Creation and folder/library actions are quiet text links under the list (mock 3c).
        TrainingRoutineContent(
            routines = routines,
            folders = folders,
            folderEditor = folderEditor,
            accent = accent,
            onOpenLibrary = onOpenLibrary,
            showLibraryLink = showLibraryLink,
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
        TrainingHomePrimaryActions(
            hasActiveWorkout = hasActiveWorkout,
            accent = accent,
            onStartBlankWorkout = onStartBlankWorkout,
            onNewRoutine = onNewRoutine,
        )
    }
}

@Composable
fun TrainingRoutineContent(
    routines: List<RoutineSummary>,
    folders: List<RoutineFolder> = emptyList(),
    folderEditor: RoutineFolderEditorState = RoutineFolderEditorState(),
    accent: TabAccent,
    onOpenLibrary: () -> Unit = {},
    showLibraryLink: Boolean = true,
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
                Text(
                    text = "Create a routine and it will appear here. Add folders to group them, then drag routines in.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
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
                        // No resting card chrome — rows sit on the surface; the tonal
                        // fill appears only while this section is an active drop target.
                        Surface(
                            color = if (isActiveDropTarget) accent.container else Color.Transparent,
                            shape = MusFitTheme.shapes.large,
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
                                                thickness = 1.dp,
                                                color = MusFitTheme.colors.outline,
                                                modifier = Modifier.padding(start = 69.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            RoutineOrganizeActions(
                accent = accent,
                onNewFolder = { onOpenFolderEditor(null) },
                onOpenLibrary = onOpenLibrary,
                showLibraryLink = showLibraryLink,
            )
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
        }
        draggedRoutine?.let { drag ->
            RoutineDragLabel(drag = drag, contentBounds = contentBounds, accent = accent)
        }
    }
}

/**
 * The mock's quiet creation row under the routine list: "+ New routine" and
 * "empty workout" as plain accent text links. When a workout is already running
 * the Resume banner above owns the primary action, so this collapses to just
 * "+ New routine".
 */
@Composable
private fun TrainingHomePrimaryActions(
    hasActiveWorkout: Boolean,
    accent: TabAccent,
    onStartBlankWorkout: () -> Unit,
    onNewRoutine: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onNewRoutine) {
            Icon(imageVector = Icons.Outlined.Add, contentDescription = null, tint = accent.color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("New routine", color = accent.color, fontWeight = FontWeight.Medium, maxLines = 1)
        }
        if (!hasActiveWorkout) {
            Text(
                text = "or",
                style = MaterialTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
            TextButton(onClick = onStartBlankWorkout) {
                Text("empty workout", color = accent.color, fontWeight = FontWeight.Medium, maxLines = 1)
            }
        }
    }
}

/**
 * Quiet footer actions under the routine list: create a folder to organize routines, or browse the
 * pre-made routine library. Kept as low-emphasis text links so the top of the tab stays a single CTA.
 */
@Composable
private fun RoutineOrganizeActions(
    accent: TabAccent,
    onNewFolder: () -> Unit,
    onOpenLibrary: () -> Unit,
    showLibraryLink: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onNewFolder) {
            Icon(imageVector = Icons.Outlined.Add, contentDescription = null, tint = accent.color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("New folder", color = accent.color, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.weight(1f))
        if (showLibraryLink) {
            TextButton(onClick = onOpenLibrary) {
                Icon(imageVector = Icons.Outlined.FitnessCenter, contentDescription = null, tint = accent.color, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Browse library", color = accent.color, fontWeight = FontWeight.Medium)
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
        if (routines.isEmpty()) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
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
                            thickness = 1.dp,
                            color = MusFitTheme.colors.outline,
                            modifier = Modifier.padding(start = 69.dp),
                        )
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
            RoutineLeadingIcon(name = routine.name)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                routine.name,
                style = MaterialTheme.typography.titleSmall,
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
        IconButton(onClick = onStart, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Filled.PlayCircle,
                contentDescription = "Start ${routine.name}",
                tint = accent.color,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

/** 46dp neutral letter circle (mock 3c): the routine's initial on a quiet warm fill. */
@Composable
private fun RoutineLeadingIcon(name: String) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(MusFitTheme.colors.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleSmall,
            color = MusFitTheme.colors.onSurface,
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
            .size(46.dp)
            .clip(CircleShape)
            .background(MusFitTheme.colors.surfaceVariant)
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
            tint = MusFitTheme.colors.onSurfaceVariant,
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
                animateGif = false,
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

internal fun routineDescription(routine: RoutineSummary): String? =
    routine.notes?.trim()?.takeIf(String::isNotBlank)
        ?: if (routine.isStarter) "Pre-saved routine" else null

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

/** Collapsed-row subline (mock 5f): "3 × 8 · 150s rest" — one line, nothing else. */
internal fun routineExerciseSubline(exercise: RoutineExerciseInput): String {
    val sets = if (exercise.setPlans.isNotEmpty()) exercise.setPlans.size else exercise.targetSets
    val reps = exercise.targetReps?.trim()?.takeIf(String::isNotBlank)
    val base = if (reps != null) "$sets × $reps" else "$sets ${if (sets == 1) "set" else "sets"}"
    val rest = exercise.restSeconds?.let { "${it}s rest" }
    return listOfNotNull(base, rest).joinToString(" · ")
}

/** One quiet sentence describing what the per-set plan holds beyond straight working sets. */
internal fun setPlanSummaryLabel(setPlans: List<RoutineSetInput>): String {
    if (setPlans.isEmpty()) return "Straight sets"
    val parts = mutableListOf<String>()
    val warmups = setPlans.count { it.setType.equals("warmup", ignoreCase = true) }
    if (warmups == 1) parts += "1 warm-up set"
    if (warmups > 1) parts += "$warmups warm-up sets"
    val failureIndex = setPlans.indexOfFirst { it.setType.equals("failure", ignoreCase = true) }
    when {
        failureIndex == 0 -> parts += "First set to failure"
        failureIndex > 0 -> parts += "Set ${failureIndex + 1} to failure"
    }
    val drops = setPlans.count { it.setType.equals("drop", ignoreCase = true) }
    if (drops == 1) parts += "1 drop set"
    if (drops > 1) parts += "$drops drop sets"
    return if (parts.isEmpty()) "Straight sets" else parts.joinToString(" · ")
}

/** Editor meta line (mock 5f): "Strength block · 5 exercises · ~40 min". */
internal fun routineEditorMetaLine(editor: RoutineEditorState): String {
    val block = editor.folderName.trim().takeIf(String::isNotBlank)
        ?: if (editor.isStarter) "Pre-made routine" else "Routine"
    val exerciseCount = editor.exercises.size
    val totalSets = editor.exercises.sumOf { exercise ->
        if (exercise.setPlans.isNotEmpty()) exercise.setPlans.size else exercise.targetSets
    }
    val estimatedMinutes = RoutineDisplayCalculator.estimatedMinutes(totalSets)
    return buildString {
        append(block)
        append(" · $exerciseCount ${if (exerciseCount == 1) "exercise" else "exercises"}")
        if (estimatedMinutes > 0) append(" · ~$estimatedMinutes min")
    }
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

/**
 * Routine editor, decluttered (mock 5f): back + the routine name as the page title (tap to
 * rename) + filled Save; one meta line with a Details link (folder/notes moved into a sheet);
 * exercises as collapsed hairline rows that expand one-at-a-time into a white card with
 * Sets/Reps/Rest fields and a per-set plan behind a link. Drag handles reorder.
 */
@Composable
fun TrainingRoutineEditor(
    editor: RoutineEditorState,
    exercises: List<ExerciseSummary>,
    accent: TabAccent,
    onNameChange: (String) -> Unit,
    onNotesChange: (String) -> Unit = {},
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
    // Accordion: at most one exercise expanded; the id survives reorders (indices don't).
    var expandedExerciseId by rememberSaveable(editor.routineId) { mutableStateOf<String?>(null) }
    var detailsOpen by rememberSaveable { mutableStateOf(false) }
    val currentExercises = rememberUpdatedState(editor.exercises)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Cancel",
                tint = MusFitTheme.colors.onSurface,
                modifier = Modifier.size(24.dp).clickable(onClick = onCancel),
            )
            RoutineEditorTitleField(
                value = editor.name,
                onValueChange = onNameChange,
                accent = accent,
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = routineEditorMetaLine(editor),
                style = MaterialTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Details",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = accent.color,
                modifier = Modifier.clickable { detailsOpen = true },
            )
        }

        HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)

        if (editor.exercises.isEmpty()) {
            Text(
                text = "Add at least one exercise to start this routine",
                style = MaterialTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        }

        Column {
            editor.exercises.forEachIndexed { index, exercise ->
                val exerciseSummary = exerciseMap[exercise.exerciseId]
                val exerciseName = exerciseSummary?.name ?: "Unknown exercise"
                if (expandedExerciseId == exercise.exerciseId) {
                    RoutineEditorExpandedCard(
                        exerciseIndex = index,
                        exercise = exercise,
                        exerciseName = exerciseName,
                        exerciseMeta = listOfNotNull(
                            exerciseSummary?.equipment,
                            exerciseSummary?.targetMuscles?.takeIf(String::isNotBlank),
                        ).joinToString(" · ") { it.lowercase(java.util.Locale.US) },
                        accent = accent,
                        fieldColors = fieldColors,
                        onCollapse = { expandedExerciseId = null },
                        onTargetSetsChange = { onTargetSetsChange(index, it) },
                        onTargetRepsChange = { onTargetRepsChange(index, it) },
                        onRestSecondsChange = { onRestSecondsChange(index, it) },
                        onAddSet = { onAddSet(index) },
                        onRemoveSet = { setIndex -> onRemoveSet(index, setIndex) },
                        onSetTypeChange = { setIndex, setType -> onSetTypeChange(index, setIndex, setType) },
                        onSetRepsChange = { setIndex, value -> onSetRepsChange(index, setIndex, value) },
                        onSetWeightChange = { setIndex, value -> onSetWeightChange(index, setIndex, value) },
                        onRemoveExercise = {
                            expandedExerciseId = null
                            onRemoveExercise(index)
                        },
                    )
                } else {
                    RoutineEditorCollapsedRow(
                        exercise = exercise,
                        exerciseName = exerciseName,
                        currentIndexOf = { id -> currentExercises.value.indexOfFirst { it.exerciseId == id } },
                        onExpand = { expandedExerciseId = exercise.exerciseId },
                        onDragStarted = { expandedExerciseId = null },
                        onMoveUp = onMoveExerciseUp,
                        onMoveDown = onMoveExerciseDown,
                    )
                    if (index < editor.exercises.lastIndex &&
                        editor.exercises.getOrNull(index + 1)?.exerciseId != expandedExerciseId
                    ) {
                        HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
                    }
                }
            }
            if (editor.exercises.isNotEmpty()) {
                HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenExercisePicker)
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = accent.color,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Add exercise",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = accent.color,
                )
            }
        }
    }

    if (detailsOpen) {
        RoutineEditorDetailsSheet(
            editor = editor,
            accent = accent,
            onNotesChange = onNotesChange,
            onDuplicate = onDuplicate,
            onDelete = onDelete,
            onDismiss = { detailsOpen = false },
        )
    }
}

/** Collapsed exercise row (mock 5f): drag handle, name + one subline, expand chevron. */
@Composable
private fun RoutineEditorCollapsedRow(
    exercise: RoutineExerciseInput,
    exerciseName: String,
    currentIndexOf: (String) -> Int,
    onExpand: () -> Unit,
    onDragStarted: () -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onExpand)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RoutineEditorDragHandle(
            exerciseId = exercise.exerciseId,
            currentIndexOf = currentIndexOf,
            onDragStarted = onDragStarted,
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exerciseName,
                style = MaterialTheme.typography.titleSmall,
                color = MusFitTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = routineExerciseSubline(exercise),
                style = MaterialTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        Icon(
            imageVector = Icons.Outlined.KeyboardArrowDown,
            contentDescription = "Expand $exerciseName",
            tint = MusFitTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Reorder handle: vertical drag accumulates; each row-height crossed moves the exercise one
 * position through the existing move-up/move-down actions.
 */
@Composable
private fun RoutineEditorDragHandle(
    exerciseId: String,
    currentIndexOf: (String) -> Int,
    onDragStarted: () -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
) {
    Icon(
        imageVector = Icons.Outlined.DragIndicator,
        contentDescription = "Reorder exercise",
        tint = pickerOutlineColor(),
        modifier = Modifier
            .size(20.dp)
            .pointerInput(exerciseId) {
                val rowHeightPx = 64.dp.toPx()
                var accumulated = 0f
                detectDragGestures(
                    onDragStart = {
                        accumulated = 0f
                        onDragStarted()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulated += dragAmount.y
                        while (accumulated > rowHeightPx) {
                            onMoveDown(currentIndexOf(exerciseId))
                            accumulated -= rowHeightPx
                        }
                        while (accumulated < -rowHeightPx) {
                            onMoveUp(currentIndexOf(exerciseId))
                            accumulated += rowHeightPx
                        }
                    },
                )
            },
    )
}

/** Expanded exercise (mock 5f): white 20dp card with Sets/Reps/Rest fields and the per-set plan link. */
@Composable
private fun RoutineEditorExpandedCard(
    exerciseIndex: Int,
    exercise: RoutineExerciseInput,
    exerciseName: String,
    exerciseMeta: String,
    accent: TabAccent,
    fieldColors: androidx.compose.material3.TextFieldColors,
    onCollapse: () -> Unit,
    onTargetSetsChange: (String) -> Unit,
    onTargetRepsChange: (String) -> Unit,
    onRestSecondsChange: (String) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit,
    onSetTypeChange: (Int, String) -> Unit,
    onSetRepsChange: (Int, String) -> Unit,
    onSetWeightChange: (Int, String) -> Unit,
    onRemoveExercise: () -> Unit,
) {
    var perSetPlanOpen by rememberSaveable(exercise.exerciseId) { mutableStateOf(false) }
    val setsError = (validateTargetSets(exercise.targetSets.toString()) as? TargetFieldResult.Invalid)?.message
    val repsError = (validateTargetReps(exercise.targetReps.orEmpty()) as? TargetFieldResult.Invalid)?.message
    val setPlans = exercise.setPlans.ifEmpty { defaultRoutineEditorSetPlans(exercise.targetSets, exercise.targetReps) }

    Surface(
        color = MusFitTheme.colors.surface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCollapse),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.DragIndicator,
                    contentDescription = null,
                    tint = pickerOutlineColor(),
                    modifier = Modifier.size(20.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exerciseName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MusFitTheme.colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (exerciseMeta.isNotBlank()) {
                        Text(
                            text = exerciseMeta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MusFitTheme.colors.onSurfaceVariant,
                            modifier = Modifier.padding(top = 1.dp),
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowUp,
                    contentDescription = "Collapse $exerciseName",
                    tint = MusFitTheme.colors.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                RoutineEditorMiniField(
                    label = "Sets",
                    value = exercise.targetSets.toString(),
                    isError = setsError != null,
                    onValueChange = onTargetSetsChange,
                    modifier = Modifier.weight(1f),
                )
                RoutineEditorMiniField(
                    label = "Reps",
                    value = exercise.targetReps.orEmpty(),
                    isError = repsError != null,
                    onValueChange = onTargetRepsChange,
                    modifier = Modifier.weight(1f),
                )
                RoutineEditorMiniField(
                    label = "Rest",
                    value = exercise.restSeconds?.toString().orEmpty(),
                    suffix = "s",
                    onValueChange = onRestSecondsChange,
                    modifier = Modifier.weight(1f),
                )
            }
            listOfNotNull(setsError, repsError).firstOrNull()?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = setPlanSummaryLabel(setPlans),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp),
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
                Text(
                    text = if (perSetPlanOpen) "Hide per-set plan" else "Per-set plan",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp),
                    fontWeight = FontWeight.Medium,
                    color = accent.color,
                    modifier = Modifier.clickable { perSetPlanOpen = !perSetPlanOpen },
                )
            }

            if (perSetPlanOpen) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        TextButton(onClick = onAddSet, colors = ButtonDefaults.textButtonColors(contentColor = accent.color)) {
                            Icon(imageVector = Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Set")
                        }
                        TextButton(
                            onClick = onRemoveExercise,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Text("Remove exercise")
                        }
                    }
                }
            }
        }
    }
}

/** Quiet tonal input (mock 5f): tiny label over a 16sp value on a 14dp-radius fill. */
@Composable
private fun RoutineEditorMiniField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    isError: Boolean = false,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MusFitTheme.colors.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = if (isError) MaterialTheme.colorScheme.error else MusFitTheme.colors.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = MusFitTheme.colors.onSurface,
                ),
                cursorBrush = SolidColor(MusFitTheme.colors.onSurface),
                modifier = Modifier.weight(1f, fill = false).widthIn(min = 20.dp),
            )
            if (suffix != null && value.isNotBlank()) {
                Text(
                    text = suffix,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MusFitTheme.colors.onSurface,
                )
            }
        }
    }
}

/** The Details sheet behind the meta line: folder (read-only), notes, duplicate/delete. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutineEditorDetailsSheet(
    editor: RoutineEditorState,
    accent: TabAccent,
    onNotesChange: (String) -> Unit,
    onDuplicate: ((String) -> Unit)?,
    onDelete: ((String) -> Unit)?,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MusFitTheme.colors.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Details",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Normal,
                color = MusFitTheme.colors.onSurface,
            )
            editor.folderName.takeIf { it.isNotBlank() }?.let { folder ->
                Text(
                    text = "Folder · $folder",
                    style = MaterialTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = editor.notes,
                onValueChange = onNotesChange,
                label = { Text("Notes") },
                minLines = 2,
                shape = MusFitTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (editor.routineId != null && onDuplicate != null) {
                    TextButton(
                        onClick = {
                            onDismiss()
                            onDuplicate(editor.routineId)
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = accent.color),
                    ) {
                        Text("Duplicate")
                    }
                }
                if (editor.routineId != null && !editor.isStarter && onDelete != null) {
                    TextButton(
                        onClick = {
                            onDismiss()
                            onDelete(editor.routineId)
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

/**
 * The routine name rendered as the page title (mock 5f): a borderless [BasicTextField]
 * styled as the 24sp regular heading — tapping it renames in place.
 */
@Composable
private fun RoutineEditorTitleField(
    value: String,
    onValueChange: (String) -> Unit,
    accent: TabAccent,
    modifier: Modifier = Modifier,
) {
    val titleStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Normal)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = titleStyle.copy(color = MusFitTheme.colors.onSurface),
        cursorBrush = SolidColor(accent.color),
        singleLine = true,
        modifier = modifier,
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = "Name your routine",
                        style = titleStyle,
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
                innerTextField()
            }
        },
    )
}

/**
 * Exercise picker, decluttered (mock 5d): search pill + one `tune` filter button with an
 * active-count badge, an active-filter summary line, plain selection rows (no thumbnails,
 * no sublines), and a full-width "Add N exercises" bar. Filters live in the sheet (5e).
 */
@Composable
fun RoutineExercisePickerPage(
    exercises: List<ExerciseSummary>,
    currentRoutineExerciseIds: Set<String>,
    selectedExerciseIds: Set<String>,
    searchQuery: String,
    filters: TrainingPickerFilters,
    filterSheetOpen: Boolean,
    loggedExerciseIds: Set<String>,
    customExerciseEditor: ExerciseEditorState,
    accent: TabAccent,
    onSearchChange: (String) -> Unit,
    onOpenFilters: () -> Unit,
    onCloseFilters: () -> Unit,
    onToggleEquipment: (String) -> Unit,
    onToggleMuscle: (String) -> Unit,
    onOnlyDoneChange: (Boolean) -> Unit,
    onResetFilters: () -> Unit,
    onClearFilters: () -> Unit,
    onToggleExercise: (String) -> Unit,
    onOpenCustomExercise: () -> Unit,
    onCloseCustomExercise: () -> Unit,
    onCustomExerciseNameChange: (String) -> Unit,
    onCustomExerciseCategoryChange: (String) -> Unit,
    onCustomExerciseEquipmentChange: (String) -> Unit,
    onCustomExerciseTargetMusclesChange: (String) -> Unit,
    onSaveCustomExercise: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val options = routineExercisePickerOptions(exercises)
    val availableExercises = exercises.filterNot { it.id in currentRoutineExerciseIds }
    val visibleExercises = routineExercisePickerSuggestions(
        exercises = availableExercises,
        selectedExerciseIds = emptySet(),
        query = searchQuery,
        filters = filters,
        loggedExerciseIds = loggedExerciseIds,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MusFitTheme.colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Cancel exercise selection",
                tint = MusFitTheme.colors.onSurface,
                modifier = Modifier.size(24.dp).clickable(onClick = onCancel),
            )
            Text(
                "Add exercises",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Normal,
                color = MusFitTheme.colors.onSurface,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PickerSearchField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.weight(1f),
            )
            PickerFilterButton(
                activeCount = filters.activeCount,
                accent = accent,
                onClick = onOpenFilters,
            )
        }

        if (filters.isActive) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = pickerFilterSummary(filters),
                    style = MaterialTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.clickable(onClick = onClearFilters),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = "Clear",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = accent.color,
                    )
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Clear filters",
                        tint = accent.color,
                        modifier = Modifier.size(15.dp),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${visibleExercises.size} results",
                    style = MaterialTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            if (visibleExercises.isEmpty()) {
                item {
                    Text(
                        "No matching exercises",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MusFitTheme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 14.dp),
                    )
                }
            }
            itemsIndexed(
                items = visibleExercises,
                key = { _, exercise -> exercise.id },
            ) { index, exercise ->
                RoutineExercisePickerRow(
                    exercise = exercise,
                    selected = exercise.id in selectedExerciseIds,
                    accent = accent,
                    onToggle = { onToggleExercise(exercise.id) },
                )
                if (index < visibleExercises.lastIndex) {
                    HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
                }
            }
            item {
                HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
                Text(
                    text = "+ Create custom exercise",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = accent.color,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenCustomExercise)
                        .padding(vertical = 14.dp),
                )
            }
        }

        Button(
            onClick = onConfirm,
            enabled = selectedExerciseIds.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.onColor),
            modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
        ) {
            Text(pickerConfirmLabel(selectedExerciseIds.size), modifier = Modifier.padding(vertical = 4.dp))
        }
    }

    if (filterSheetOpen) {
        ExercisePickerFilterSheet(
            equipmentOptions = topPickerEquipment(exercises, EQUIPMENT_OPTION_LIMIT),
            muscleOptions = topPickerMuscles(exercises, MUSCLE_OPTION_LIMIT),
            filters = filters,
            resultCount = visibleExercises.size,
            accent = accent,
            onToggleEquipment = onToggleEquipment,
            onToggleMuscle = onToggleMuscle,
            onOnlyDoneChange = onOnlyDoneChange,
            onReset = onResetFilters,
            onDismiss = onCloseFilters,
        )
    }

    if (customExerciseEditor.isOpen) {
        CustomExerciseSheet(
            editor = customExerciseEditor,
            accent = accent,
            onNameChange = onCustomExerciseNameChange,
            onCategoryChange = onCustomExerciseCategoryChange,
            onEquipmentChange = onCustomExerciseEquipmentChange,
            onTargetMusclesChange = onCustomExerciseTargetMusclesChange,
            onSave = onSaveCustomExercise,
            onDismiss = onCloseCustomExercise,
        )
    }
}

/** Rounded neutral search pill matching the app's sheet search fields. */
@Composable
private fun PickerSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MusFitTheme.colors.surfaceVariant)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = MusFitTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = "Search exercises",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MusFitTheme.colors.onSurface),
                cursorBrush = SolidColor(MusFitTheme.colors.onSurface),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 48dp tonal `tune` button; a small accent badge carries the active-filter count. */
@Composable
private fun PickerFilterButton(
    activeCount: Int,
    accent: TabAccent,
    onClick: () -> Unit,
) {
    Box {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(accent.container)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Tune,
                contentDescription = "Filters",
                tint = accent.onContainer,
                modifier = Modifier.size(21.dp),
            )
        }
        if (activeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-3).dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(accent.color),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = activeCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accent.onColor,
                )
            }
        }
    }
}

/**
 * The filter sheet (mock 5e) — the one place chips are allowed in the Training module:
 * wrapping equipment/muscle pills, an "only exercises I've done" switch, and a live-count
 * apply button.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ExercisePickerFilterSheet(
    equipmentOptions: List<String>,
    muscleOptions: List<String>,
    filters: TrainingPickerFilters,
    resultCount: Int,
    accent: TabAccent,
    onToggleEquipment: (String) -> Unit,
    onToggleMuscle: (String) -> Unit,
    onOnlyDoneChange: (Boolean) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MusFitTheme.colors.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Normal,
                    color = MusFitTheme.colors.onSurface,
                )
                Text(
                    text = "Reset",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = accent.color,
                    modifier = Modifier.clickable(onClick = onReset),
                )
            }

            FilterPillSection(
                title = "Equipment",
                options = equipmentOptions,
                selected = filters.equipment,
                accent = accent,
                onToggle = onToggleEquipment,
            )
            HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
            FilterPillSection(
                title = "Muscle group",
                options = muscleOptions,
                selected = filters.muscles,
                accent = accent,
                onToggle = onToggleMuscle,
            )
            HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Only exercises I've done",
                        style = MaterialTheme.typography.titleSmall,
                        color = MusFitTheme.colors.onSurface,
                    )
                    Text(
                        text = "Hides the full library",
                        style = MaterialTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }
                Switch(
                    checked = filters.onlyDone,
                    onCheckedChange = onOnlyDoneChange,
                    colors = SwitchDefaults.colors(checkedTrackColor = accent.color),
                )
            }

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.onColor),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Show $resultCount ${if (resultCount == 1) "exercise" else "exercises"}",
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterPillSection(
    title: String,
    options: List<String>,
    selected: Set<String>,
    accent: TabAccent,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MusFitTheme.colors.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                val isSelected = selected.any { it.equals(option, ignoreCase = true) }
                Surface(
                    onClick = { onToggle(option) },
                    color = if (isSelected) accent.container else MusFitTheme.colors.background,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = accent.onContainer,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Text(
                            text = option.displayExerciseToken(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            color = if (isSelected) accent.onContainer else MusFitTheme.colors.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** Bottom-sheet home for the custom exercise editor, opened from the picker's final row. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomExerciseSheet(
    editor: ExerciseEditorState,
    accent: TabAccent,
    onNameChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onEquipmentChange: (String) -> Unit,
    onTargetMusclesChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MusFitTheme.colors.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Custom exercise",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Normal,
                color = MusFitTheme.colors.onSurface,
            )
            OutlinedTextField(
                value = editor.name,
                onValueChange = onNameChange,
                label = { Text("Name") },
                singleLine = true,
                shape = MusFitTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = editor.category,
                    onValueChange = onCategoryChange,
                    label = { Text("Category") },
                    singleLine = true,
                    shape = MusFitTheme.shapes.medium,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = editor.equipment,
                    onValueChange = onEquipmentChange,
                    label = { Text("Equipment") },
                    singleLine = true,
                    shape = MusFitTheme.shapes.medium,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = editor.targetMuscles,
                onValueChange = onTargetMusclesChange,
                label = { Text("Target muscles") },
                singleLine = true,
                shape = MusFitTheme.shapes.medium,
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
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = accent.color)
                }
            }
        }
    }
}

/**
 * Minimal picker row (mock 5d): a 26dp selection circle, the exercise name, and one
 * trailing muscle word — no thumbnails, sublines, or badges.
 */
@Composable
private fun RoutineExercisePickerRow(
    exercise: ExerciseSummary,
    selected: Boolean,
    accent: TabAccent,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(accent.color),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Selected",
                    tint = accent.onColor,
                    modifier = Modifier.size(16.dp),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .border(1.5.dp, pickerOutlineColor(), CircleShape),
            )
        }
        Text(
            text = exercise.name,
            style = MaterialTheme.typography.titleSmall,
            color = MusFitTheme.colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = pickerTrailingMuscles(exercise),
            style = MaterialTheme.typography.bodySmall,
            color = MusFitTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
internal fun pickerOutlineColor(): Color =
    if (isSystemInDarkTheme()) NeutralOutlineDark else NeutralOutline

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
    filters: TrainingPickerFilters = TrainingPickerFilters(),
    loggedExerciseIds: Set<String> = emptySet(),
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
        val matchesEquipment = filters.equipment.isEmpty() ||
            filters.equipment.any { exercise.equipment.equals(it, ignoreCase = true) }
        val matchesMuscle = filters.muscles.isEmpty() ||
            exercise.pickerMuscles().any { muscle ->
                filters.muscles.any { it.equals(muscle, ignoreCase = true) }
            }
        val matchesDone = !filters.onlyDone || exercise.id in loggedExerciseIds

        matchesQuery && matchesEquipment && matchesMuscle && matchesDone
    }
    return limit?.let { filtered.take(it) } ?: filtered
}

/** "Barbell · Quads" — title-cased active filters for the picker's summary line. */
internal fun pickerFilterSummary(filters: TrainingPickerFilters): String =
    buildList {
        addAll(filters.equipment.map { it.displayExerciseToken() })
        addAll(filters.muscles.map { it.displayExerciseToken() })
        if (filters.onlyDone) add("Done before")
    }.joinToString(" · ")

internal fun pickerConfirmLabel(selectedCount: Int): String = when (selectedCount) {
    0 -> "Add exercises"
    1 -> "Add 1 exercise"
    else -> "Add $selectedCount exercises"
}

/** Trailing muscle word(s) on a picker row — first two muscles, lowercase, per mock 5d. */
internal fun pickerTrailingMuscles(exercise: ExerciseSummary): String =
    exercise.pickerMuscles()
        .map { it.lowercase() }
        .distinct()
        .take(2)
        .joinToString(", ")

/** The filter sheet's muscle pills: the catalog's most common muscles, not an alphabetical slice. */
internal fun topPickerMuscles(exercises: List<ExerciseSummary>, limit: Int): List<String> =
    exercises.flatMap { it.pickerMuscles().map { muscle -> muscle.lowercase() }.distinct() }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .take(limit)
        .map { it.key }

/** The filter sheet's equipment pills, ranked by how much of the catalog each covers. */
internal fun topPickerEquipment(exercises: List<ExerciseSummary>, limit: Int): List<String> =
    exercises.mapNotNull { it.equipment?.trim()?.takeIf(String::isNotBlank)?.lowercase() }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .take(limit)
        .map { it.key }

internal const val EQUIPMENT_OPTION_LIMIT = 8
internal const val MUSCLE_OPTION_LIMIT = 12

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

private fun String.displayExerciseToken(): String =
    trim()
        .splitToSequence(' ', '-', '_')
        .filter { it.isNotBlank() }
        .joinToString(" ") { token ->
            token.replaceFirstChar { char -> char.titlecase() }
        }
