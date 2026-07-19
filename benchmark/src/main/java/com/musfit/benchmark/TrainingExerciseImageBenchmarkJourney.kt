package com.musfit.benchmark

import android.graphics.Rect
import android.os.Build
import android.util.Log
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import java.util.regex.Pattern

internal const val EXERCISE_BROWSE_ITEM_TARGET = 100

private const val IMAGE_BENCHMARK_LOG_TAG = "MusFitImageBenchmark"
private const val EXERCISE_BROWSE_MAX_SWIPE_COUNT = 34
private const val EXERCISE_BROWSE_MEASURE_SWIPE_MARGIN = 4
private const val EXERCISE_BROWSE_SCROLL_STEPS = 24
private const val EXERCISE_BROWSE_IDLE_TIMEOUT_MILLIS = 100L
private const val TRAINING_HOME_STEP_TIMEOUT_MILLIS = 2_000L
private const val EXERCISE_BROWSE_WARM_PAGE_MILLIS = 350L
private const val EXERCISE_BROWSE_MEASURE_PAGE_MILLIS = 100L
private const val TRAINING_HOME_MAX_BACK_COUNT = 4
private const val UI_SNAPSHOT_MAX_ATTEMPTS = 5
private const val UI_SNAPSHOT_RETRY_DELAY_MILLIS = 25L
private val exerciseCatalogReadyResource = Pattern.compile(
    "^(?:[^:]+:id/)?training-exercise-thumbnail-" +
        "(?:item-(?:initial|loading)|loaded-(?:memory_cache|memory|disk|network))-ds-0001\$",
)
private val exerciseThumbnailPendingRegex =
    Regex("^training-exercise-thumbnail-item-(?:initial|loading)-(.+)$")
private val exerciseThumbnailLoadedRegex =
    Regex("^training-exercise-thumbnail-loaded-(?:memory_cache|memory|disk|network)-(.+)$")
private val exerciseThumbnailMemoryRegex =
    Regex("^training-exercise-thumbnail-loaded-memory_cache-(.+)$")
private val exerciseThumbnailErrorRegex =
    Regex("^training-exercise-thumbnail-error-[^-]+-(.+)$")
private val exerciseThumbnailPlaceholderRegex =
    Regex("^training-exercise-thumbnail-placeholder-(.+)$")
private val exerciseThumbnailPendingResource = resourceSelector(exerciseThumbnailPendingRegex)
private val exerciseThumbnailLoadedResource = resourceSelector(exerciseThumbnailLoadedRegex)
private val exerciseThumbnailMemoryResource = resourceSelector(exerciseThumbnailMemoryRegex)
private val exerciseThumbnailErrorResource = resourceSelector(exerciseThumbnailErrorRegex)
private val exerciseThumbnailPlaceholderResource = resourceSelector(exerciseThumbnailPlaceholderRegex)
private val exerciseThumbnailRowResource =
    Pattern.compile("^(?:[^:]+:id/)?training-exercise-thumbnail-.+\$")

internal data class ExerciseImageBrowsePlan(
    val swipeCount: Int,
    val firstPageAnchorExerciseId: String,
    val warmedExerciseIds: Set<String>,
)

private data class ExerciseResourceSnapshot(
    val resourceName: String,
    val top: Int,
    val left: Int,
)

internal fun MacrobenchmarkScope.stabilizeExerciseImageBenchmarkDevice() {
    // The managed Google APIs image can start Photos indexing during a trace. It saturated both
    // emulator CPUs in the controlled comparison even though the benchmark never opens Photos.
    // Refuse physical devices, then verify the package state before measurement begins.
    val isEmulator = device.executeShellCommand("getprop ro.kernel.qemu").trim() == "1"
    check(isEmulator) { "Refusing to disable Google Photos on a non-emulator device." }
    check(Build.VERSION.SDK_INT == 28 || Build.VERSION.SDK_INT >= 37) {
        "Unexpected benchmark emulator API ${Build.VERSION.SDK_INT}."
    }
    device.executeShellCommand("pm disable-user --user 0 com.google.android.apps.photos")
    val disabledPackages = device.executeShellCommand(
        "pm list packages --user 0 -d com.google.android.apps.photos",
    )
    check("package:com.google.android.apps.photos" in disabledPackages) {
        "Google Photos was not confirmed disabled on the benchmark emulator."
    }
}

private fun resourceSelector(tagRegex: Regex): Pattern {
    val tagPattern = tagRegex.pattern.removePrefix("^").removeSuffix("$")
    return Pattern.compile("^(?:[^:]+:id/)?$tagPattern\$")
}

internal fun MacrobenchmarkScope.openTrainingExerciseImageBrowse() {
    visitDestination("Training")
    val newRoutine = returnToTrainingHome()
    newRoutine.clickAction()
    device.waitForIdle()
    waitForDescription("Routine name")

    clickText("Add exercise")
    waitForText("Add exercises")
    findExerciseBrowseList()
    waitForExerciseCatalog()
    device.waitForIdle()
}

/**
 * Visits [EXERCISE_BROWSE_ITEM_TARGET] distinct image-bearing exercises before measurement, then
 * returns the lazy list to its first-page anchor. The benchmark-only byte fixture removes network
 * and encoded-disk variability while preserving distinct decoded-memory entries per exercise.
 */
internal fun MacrobenchmarkScope.warmExerciseImageBrowse(): ExerciseImageBrowsePlan {
    val firstPageAnchorExerciseId = checkNotNull(visibleExerciseRowIds().firstOrNull()) {
        "The first exercise page did not expose a stable row resource."
    }
    val firstPageExerciseIds = awaitVisibleExerciseImagesLoaded(allowNoImages = true)
    val loadedExerciseIds = linkedSetOf<String>().apply { addAll(firstPageExerciseIds) }
    var swipeCount = 0
    while (
        loadedExerciseIds.size < EXERCISE_BROWSE_ITEM_TARGET &&
        swipeCount < EXERCISE_BROWSE_MAX_SWIPE_COUNT
    ) {
        swipeExerciseBrowse(down = true)
        swipeCount += 1
        Thread.sleep(EXERCISE_BROWSE_WARM_PAGE_MILLIS)
        loadedExerciseIds += awaitVisibleExerciseImagesLoaded(allowNoImages = true)
    }
    check(loadedExerciseIds.size >= EXERCISE_BROWSE_ITEM_TARGET) {
        "Exercise browse exposed only ${loadedExerciseIds.size} distinct image-bearing exercises after " +
            "$swipeCount swipes; expected at least $EXERCISE_BROWSE_ITEM_TARGET."
    }
    val warmedExerciseIds = loadedExerciseIds
        .take(EXERCISE_BROWSE_ITEM_TARGET)
        .toCollection(linkedSetOf())

    rewindExerciseImageBrowse(swipeCount, firstPageAnchorExerciseId)

    val memoryCacheHits = linkedSetOf<String>()
    var cacheSwipeCount = 0
    while (
        !memoryCacheHits.containsAll(warmedExerciseIds) &&
        cacheSwipeCount < EXERCISE_BROWSE_MAX_SWIPE_COUNT
    ) {
        memoryCacheHits += awaitVisibleExerciseImagesLoaded(
            requireMemoryCache = true,
            allowNoImages = true,
            targetExerciseIds = warmedExerciseIds,
        )
        if (memoryCacheHits.containsAll(warmedExerciseIds)) break
        swipeExerciseBrowse(down = true)
        cacheSwipeCount += 1
        Thread.sleep(EXERCISE_BROWSE_MEASURE_PAGE_MILLIS)
    }
    check(memoryCacheHits.containsAll(warmedExerciseIds)) {
        val missingIds = warmedExerciseIds - memoryCacheHits
        "Only ${EXERCISE_BROWSE_ITEM_TARGET - missingIds.size} of $EXERCISE_BROWSE_ITEM_TARGET " +
            "warmed exercises reported Coil memory-cache hits; missing=${missingIds.sorted()}."
    }
    logCacheHits(
        phase = "setup",
        renderedCount = warmedExerciseIds.size,
        memoryCacheHitCount = memoryCacheHits.size,
        swipeCount = cacheSwipeCount,
    )
    rewindExerciseImageBrowse(cacheSwipeCount, firstPageAnchorExerciseId)
    return ExerciseImageBrowsePlan(
        swipeCount = cacheSwipeCount,
        firstPageAnchorExerciseId = firstPageAnchorExerciseId,
        warmedExerciseIds = warmedExerciseIds,
    )
}

internal fun MacrobenchmarkScope.browseWarmedExerciseImages(plan: ExerciseImageBrowsePlan) {
    check(plan.swipeCount > 0) { "The warmed exercise browse did not require any measured scrolling." }
    val measuredMemoryCacheHits = linkedSetOf<String>()
    val firstPageExerciseIds = visibleExerciseRowIds()
    check(plan.firstPageAnchorExerciseId in firstPageExerciseIds) {
        "Measured exercise browse did not start at ${plan.firstPageAnchorExerciseId}."
    }
    val firstPageLoadedExerciseIds = awaitVisibleExerciseImagesLoaded(
        requireMemoryCache = true,
        allowNoImages = true,
        targetExerciseIds = plan.warmedExerciseIds,
    )
    measuredMemoryCacheHits += firstPageLoadedExerciseIds
    val measuredSwipeLimit = minOf(
        EXERCISE_BROWSE_MAX_SWIPE_COUNT,
        plan.swipeCount + EXERCISE_BROWSE_MEASURE_SWIPE_MARGIN,
    )
    var measuredSwipeCount = 0
    while (
        !measuredMemoryCacheHits.containsAll(plan.warmedExerciseIds) &&
        measuredSwipeCount < measuredSwipeLimit
    ) {
        swipeExerciseBrowse(down = true)
        measuredSwipeCount += 1
        Thread.sleep(EXERCISE_BROWSE_MEASURE_PAGE_MILLIS)
        measuredMemoryCacheHits += awaitVisibleExerciseImagesLoaded(
            requireMemoryCache = true,
            allowNoImages = true,
            targetExerciseIds = plan.warmedExerciseIds,
        )
    }
    check(measuredMemoryCacheHits.containsAll(plan.warmedExerciseIds)) {
        val missingIds = plan.warmedExerciseIds - measuredMemoryCacheHits
        "Measured exercise browse rendered only " +
            "${EXERCISE_BROWSE_ITEM_TARGET - missingIds.size} of $EXERCISE_BROWSE_ITEM_TARGET " +
            "warmed images from memory; missing=${missingIds.sorted()}."
    }
    logCacheHits(
        phase = "measured",
        renderedCount = plan.warmedExerciseIds.size,
        memoryCacheHitCount = measuredMemoryCacheHits.size,
        swipeCount = measuredSwipeCount,
    )
}

private fun logCacheHits(
    phase: String,
    renderedCount: Int,
    memoryCacheHitCount: Int,
    swipeCount: Int,
) {
    Log.i(
        IMAGE_BENCHMARK_LOG_TAG,
        "phase=$phase rendered=$renderedCount target=$EXERCISE_BROWSE_ITEM_TARGET " +
            "memoryCacheHits=$memoryCacheHitCount swipes=$swipeCount",
    )
}

private fun MacrobenchmarkScope.returnToTrainingHome(): UiObject2 {
    repeat(TRAINING_HOME_MAX_BACK_COUNT) {
        device.wait(Until.findObject(By.text("New routine")), TRAINING_HOME_STEP_TIMEOUT_MILLIS)
            ?.let { return it }
        device.pressBack()
        device.waitForIdle(EXERCISE_BROWSE_IDLE_TIMEOUT_MILLIS)
        Thread.sleep(250)
    }
    return device.wait(Until.findObject(By.text("New routine")), UI_TIMEOUT_MILLIS)
        ?: error("Training home did not expose 'New routine'.")
}

private fun MacrobenchmarkScope.clickText(text: String) {
    val node = device.wait(Until.findObject(By.text(text)), UI_TIMEOUT_MILLIS)
        ?: error("Text '$text' was not found.")
    node.clickAction()
    device.waitForIdle()
}

private fun MacrobenchmarkScope.waitForText(text: String) {
    checkNotNull(device.wait(Until.findObject(By.text(text)), UI_TIMEOUT_MILLIS)) {
        "Text '$text' did not appear."
    }
}

private fun MacrobenchmarkScope.waitForDescription(description: String) {
    checkNotNull(device.wait(Until.findObject(By.desc(description)), UI_TIMEOUT_MILLIS)) {
        "Content description '$description' did not appear."
    }
}

private fun MacrobenchmarkScope.findExerciseBrowseList(): UiObject2 = device.wait(Until.findObject(By.scrollable(true)), UI_TIMEOUT_MILLIS)
    ?: error("The exercise browse list did not become scrollable.")

private fun MacrobenchmarkScope.waitForExerciseCatalog() {
    check(device.wait(Until.hasObject(By.res(exerciseCatalogReadyResource)), UI_TIMEOUT_MILLIS)) {
        "The exercise catalog did not finish importing its ds-0001 gate record; " +
            "nodes=${visibleExerciseNodeSnapshot()}."
    }
}

@Suppress("ComplexCondition")
private fun MacrobenchmarkScope.awaitVisibleExerciseImagesLoaded(
    requireMemoryCache: Boolean = false,
    allowNoImages: Boolean = false,
    targetExerciseIds: Set<String>? = null,
): Set<String> {
    val deadline = System.currentTimeMillis() + UI_TIMEOUT_MILLIS
    var pending = emptySet<String>()
    var loaded = emptySet<String>()
    var memory = emptySet<String>()
    var errors = emptySet<String>()
    var placeholders = emptySet<String>()
    do {
        pending = visibleExerciseIds(exerciseThumbnailPendingResource, exerciseThumbnailPendingRegex)
        loaded = visibleExerciseIds(exerciseThumbnailLoadedResource, exerciseThumbnailLoadedRegex)
        memory = visibleExerciseIds(exerciseThumbnailMemoryResource, exerciseThumbnailMemoryRegex)
        errors = visibleExerciseIds(exerciseThumbnailErrorResource, exerciseThumbnailErrorRegex)
        placeholders = visibleExerciseIds(exerciseThumbnailPlaceholderResource, exerciseThumbnailPlaceholderRegex)
        check(errors.isEmpty()) { "Exercise thumbnail loads failed: ${errors.sorted()}." }
        val relevantPending = targetExerciseIds?.let(pending::intersect) ?: pending
        val relevantLoaded = targetExerciseIds?.let(loaded::intersect) ?: loaded
        val relevantMemory = targetExerciseIds?.let(memory::intersect) ?: memory
        val noRelevantImages = relevantPending.isEmpty() && relevantLoaded.isEmpty()
        if (allowNoImages && noRelevantImages && (placeholders.isNotEmpty() || targetExerciseIds != null)) {
            return emptySet()
        }
        if (
            relevantPending.isEmpty() &&
            relevantLoaded.isNotEmpty() &&
            (!requireMemoryCache || relevantMemory.containsAll(relevantLoaded))
        ) {
            return if (requireMemoryCache) relevantMemory else relevantLoaded
        }
        Thread.sleep(50)
    } while (System.currentTimeMillis() < deadline)
    val expectation = if (requireMemoryCache) "Coil's memory-cache state" else "Coil's success state"
    val nodeSnapshot = visibleExerciseNodeSnapshot()
    error(
        "Visible exercise thumbnails did not all reach $expectation: " +
            "pending=${pending.sorted()}, loaded=${loaded.sorted()}, memory=${memory.sorted()}, " +
            "errors=${errors.sorted()}, placeholders=${placeholders.sorted()}, nodes=$nodeSnapshot.",
    )
}

private fun MacrobenchmarkScope.visibleExerciseNodeSnapshot(): List<String> = withStableUiSnapshot("visible exercise node diagnostics") {
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
        device.waitForIdle(EXERCISE_BROWSE_IDLE_TIMEOUT_MILLIS)
        Thread.sleep(UI_SNAPSHOT_RETRY_DELAY_MILLIS)
    },
    capture = snapshot,
)

internal fun <T> retryStaleUiSnapshot(
    description: String,
    maxAttempts: Int = UI_SNAPSHOT_MAX_ATTEMPTS,
    onRetry: () -> Unit = { Thread.sleep(UI_SNAPSHOT_RETRY_DELAY_MILLIS) },
    capture: () -> T,
): T {
    require(maxAttempts > 0) { "maxAttempts must be positive." }
    var lastFailure: StaleObjectException? = null
    repeat(maxAttempts) { attempt ->
        try {
            return capture()
        } catch (failure: StaleObjectException) {
            lastFailure = failure
            if (attempt < maxAttempts - 1) {
                onRetry()
            }
        }
    }
    throw IllegalStateException(
        "Could not capture $description after $maxAttempts attempts because the UI hierarchy kept changing.",
        lastFailure,
    )
}

private fun MacrobenchmarkScope.visibleExerciseRowIds(): Set<String> = visibleExerciseResources(exerciseThumbnailRowResource)
    .mapNotNullTo(linkedSetOf()) { resource ->
        exerciseThumbnailPendingRegex.matchEntire(resource)?.groupValues?.get(1)
            ?: exerciseThumbnailLoadedRegex.matchEntire(resource)?.groupValues?.get(1)
            ?: exerciseThumbnailErrorRegex.matchEntire(resource)?.groupValues?.get(1)
            ?: exerciseThumbnailPlaceholderRegex.matchEntire(resource)?.groupValues?.get(1)
    }

private fun MacrobenchmarkScope.rewindExerciseImageBrowse(
    swipeCount: Int,
    firstPageAnchorExerciseId: String,
) {
    repeat(swipeCount + 1) {
        swipeExerciseBrowse(down = false)
    }
    device.waitForIdle(EXERCISE_BROWSE_IDLE_TIMEOUT_MILLIS)
    Thread.sleep(500)
    val visibleExerciseIds = visibleExerciseRowIds()
    check(firstPageAnchorExerciseId in visibleExerciseIds) {
        "Exercise browse did not return to its first-page anchor $firstPageAnchorExerciseId."
    }
}

private fun MacrobenchmarkScope.swipeExerciseBrowse(down: Boolean) {
    val bounds = findExerciseBrowseList().visibleBounds
    val horizontalCenter = bounds.centerX()
    val upperY = bounds.top + bounds.height() / 5
    val lowerY = bounds.bottom - bounds.height() / 5
    val startY = if (down) lowerY else upperY
    val endY = if (down) upperY else lowerY
    check(device.swipe(horizontalCenter, startY, horizontalCenter, endY, EXERCISE_BROWSE_SCROLL_STEPS)) {
        "Could not ${if (down) "advance" else "rewind"} the exercise browse list."
    }
    device.waitForIdle(EXERCISE_BROWSE_IDLE_TIMEOUT_MILLIS)
}
