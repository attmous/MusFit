package com.musfit.benchmark

import android.graphics.Rect
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import java.util.regex.Pattern

private const val UI_SNAPSHOT_IDLE_TIMEOUT_MILLIS = 100L
internal val exerciseCatalogReadyResource = Pattern.compile(
    "^(?:[^:]+:id/)?training-exercise-thumbnail-" +
        "(?:item-(?:initial|loading)|loaded-(?:memory_cache|memory|disk|network))-ds-0001\$",
)
private val exerciseThumbnailPendingRegex =
    Regex("^training-exercise-thumbnail-item-(?:initial|loading)-(.+)\$")
private val exerciseThumbnailLoadedRegex =
    Regex("^training-exercise-thumbnail-loaded-(?:memory_cache|memory|disk|network)-(.+)\$")
private val exerciseThumbnailMemoryRegex =
    Regex("^training-exercise-thumbnail-loaded-memory_cache-(.+)\$")
private val exerciseThumbnailErrorRegex =
    Regex("^training-exercise-thumbnail-error-[^-]+-(.+)\$")
private val exerciseThumbnailPlaceholderRegex =
    Regex("^training-exercise-thumbnail-placeholder-(.+)\$")
private val exerciseThumbnailPendingResource = resourceSelector(exerciseThumbnailPendingRegex)
private val exerciseThumbnailLoadedResource = resourceSelector(exerciseThumbnailLoadedRegex)
private val exerciseThumbnailMemoryResource = resourceSelector(exerciseThumbnailMemoryRegex)
private val exerciseThumbnailErrorResource = resourceSelector(exerciseThumbnailErrorRegex)
private val exerciseThumbnailPlaceholderResource = resourceSelector(exerciseThumbnailPlaceholderRegex)
private val exerciseThumbnailRowResource =
    Pattern.compile("^(?:[^:]+:id/)?training-exercise-thumbnail-.+\$")

private data class ExerciseResourceSnapshot(
    val resourceName: String,
    val top: Int,
    val left: Int,
)

private data class VisibleExerciseImages(
    val pending: Set<String>,
    val loaded: Set<String>,
    val memory: Set<String>,
    val errors: Set<String>,
    val placeholders: Set<String>,
)

private data class RelevantExerciseImages(
    val pending: Set<String>,
    val loaded: Set<String>,
    val memory: Set<String>,
    val absenceExplained: Boolean,
)

private fun resourceSelector(tagRegex: Regex): Pattern {
    val tagPattern = tagRegex.pattern.removePrefix("^").removeSuffix("\$")
    return Pattern.compile("^(?:[^:]+:id/)?$tagPattern\$")
}

internal fun MacrobenchmarkScope.awaitVisibleExerciseImagesLoaded(
    requireMemoryCache: Boolean = false,
    allowNoImages: Boolean = false,
    targetExerciseIds: Set<String>? = null,
): Set<String> {
    val deadline = System.currentTimeMillis() + UI_TIMEOUT_MILLIS
    var images: VisibleExerciseImages
    do {
        images = captureVisibleExerciseImages()
        check(images.errors.isEmpty()) { "Exercise thumbnail loads failed: ${images.errors.sorted()}." }
        images.loadedResultOrNull(requireMemoryCache, allowNoImages, targetExerciseIds)?.let { return it }
        Thread.sleep(50)
    } while (System.currentTimeMillis() < deadline)

    val expectation = if (requireMemoryCache) "Coil's memory-cache state" else "Coil's success state"
    val nodeSnapshot = visibleExerciseNodeSnapshot()
    error(
        "Visible exercise thumbnails did not all reach $expectation: " +
            "pending=${images.pending.sorted()}, loaded=${images.loaded.sorted()}, " +
            "memory=${images.memory.sorted()}, errors=${images.errors.sorted()}, " +
            "placeholders=${images.placeholders.sorted()}, nodes=$nodeSnapshot.",
    )
}

private fun MacrobenchmarkScope.captureVisibleExerciseImages() = VisibleExerciseImages(
    pending = visibleExerciseIds(exerciseThumbnailPendingResource, exerciseThumbnailPendingRegex),
    loaded = visibleExerciseIds(exerciseThumbnailLoadedResource, exerciseThumbnailLoadedRegex),
    memory = visibleExerciseIds(exerciseThumbnailMemoryResource, exerciseThumbnailMemoryRegex),
    errors = visibleExerciseIds(exerciseThumbnailErrorResource, exerciseThumbnailErrorRegex),
    placeholders = visibleExerciseIds(exerciseThumbnailPlaceholderResource, exerciseThumbnailPlaceholderRegex),
)

private fun VisibleExerciseImages.loadedResultOrNull(
    requireMemoryCache: Boolean,
    allowNoImages: Boolean,
    targetExerciseIds: Set<String>?,
): Set<String>? {
    val relevant = relevantTo(targetExerciseIds)
    val noRelevantImages = relevant.pending.isEmpty() && relevant.loaded.isEmpty()
    val emptyPageAccepted = allowNoImages && noRelevantImages
    return when {
        emptyPageAccepted && relevant.absenceExplained -> emptySet()
        relevant.pending.isNotEmpty() -> null
        relevant.loaded.isEmpty() -> null
        requireMemoryCache && !relevant.memory.containsAll(relevant.loaded) -> null
        requireMemoryCache -> relevant.memory
        else -> relevant.loaded
    }
}

private fun VisibleExerciseImages.relevantTo(targetExerciseIds: Set<String>?) = RelevantExerciseImages(
    pending = targetExerciseIds?.let(pending::intersect) ?: pending,
    loaded = targetExerciseIds?.let(loaded::intersect) ?: loaded,
    memory = targetExerciseIds?.let(memory::intersect) ?: memory,
    absenceExplained = placeholders.isNotEmpty() || targetExerciseIds != null,
)

internal fun MacrobenchmarkScope.visibleExerciseNodeSnapshot(): List<String> = withStableUiSnapshot("visible exercise node diagnostics") {
    val listBounds = findExerciseBrowseList().visibleBounds
    device.findObjects(By.pkg(TARGET_PACKAGE)).mapNotNull { node ->
        val bounds = node.visibleBounds
        if (listBounds.contains(bounds.centerX(), bounds.centerY())) {
            "res=${node.resourceName},text=${node.text}," +
                "desc=${node.contentDescription},class=${node.className}"
        } else {
            null
        }
    }.take(30)
}

private fun MacrobenchmarkScope.visibleExerciseIds(
    resourcePattern: Pattern,
    resourceRegex: Regex,
): Set<String> = visibleExerciseResources(resourcePattern)
    .mapNotNullTo(linkedSetOf()) { resource ->
        resourceRegex.matchEntire(resource)?.groupValues?.get(1)
    }

private fun MacrobenchmarkScope.visibleExerciseResources(resourcePattern: Pattern): Set<String> = withStableUiSnapshot("visible exercise resources") {
    val listBounds = findExerciseBrowseList().visibleBounds
    val verticalMargin = listBounds.height() / 10
    val innerTop = listBounds.top + verticalMargin
    val innerBottom = listBounds.bottom - verticalMargin
    device.findObjects(By.res(resourcePattern))
        .mapNotNull { node ->
            // Refresh the live handle once, then copy everything needed for filtering and
            // sorting. If Compose replaces the semantics node during that refresh, the whole
            // partial snapshot is discarded and retried.
            val nodeInfo = node.accessibilityNodeInfo
            val bounds = Rect().also(nodeInfo::getBoundsInScreen)
            val resourceName = nodeInfo.viewIdResourceName?.substringAfterLast('/')
            if (
                resourceName != null &&
                bounds.centerX() in listBounds.left..listBounds.right &&
                bounds.centerY() in innerTop..innerBottom
            ) {
                ExerciseResourceSnapshot(
                    resourceName = resourceName,
                    top = bounds.top,
                    left = bounds.left,
                )
            } else {
                null
            }
        }
        .sortedWith(compareBy(ExerciseResourceSnapshot::top, ExerciseResourceSnapshot::left))
        .mapTo(linkedSetOf(), ExerciseResourceSnapshot::resourceName)
}

private fun <T> MacrobenchmarkScope.withStableUiSnapshot(
    label: String,
    snapshot: () -> T,
): T = retryStaleUiSnapshot(
    description = label,
    onRetry = {
        device.waitForIdle(UI_SNAPSHOT_IDLE_TIMEOUT_MILLIS)
        Thread.sleep(UI_SNAPSHOT_RETRY_DELAY_MILLIS)
    },
    capture = snapshot,
)

internal fun MacrobenchmarkScope.visibleExerciseRowIds(): Set<String> = visibleExerciseResources(exerciseThumbnailRowResource)
    .mapNotNullTo(linkedSetOf()) { resource ->
        exerciseThumbnailPendingRegex.matchEntire(resource)?.groupValues?.get(1)
            ?: exerciseThumbnailLoadedRegex.matchEntire(resource)?.groupValues?.get(1)
            ?: exerciseThumbnailErrorRegex.matchEntire(resource)?.groupValues?.get(1)
            ?: exerciseThumbnailPlaceholderRegex.matchEntire(resource)?.groupValues?.get(1)
    }
