package com.musfit.benchmark

import android.os.Build
import android.util.Log
import androidx.benchmark.macro.MacrobenchmarkScope

internal const val EXERCISE_BROWSE_ITEM_TARGET = 100

private const val IMAGE_BENCHMARK_LOG_TAG = "MusFitImageBenchmark"
private const val EXERCISE_BROWSE_MAX_SWIPE_COUNT = 34
private const val EXERCISE_BROWSE_MEASURE_SWIPE_MARGIN = 4
private const val EXERCISE_BROWSE_WARM_PAGE_MILLIS = 350L
private const val EXERCISE_BROWSE_MEASURE_PAGE_MILLIS = 100L

internal data class ExerciseImageBrowsePlan(
    val swipeCount: Int,
    val firstPageAnchorExerciseId: String,
    val warmedExerciseIds: Set<String>,
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
