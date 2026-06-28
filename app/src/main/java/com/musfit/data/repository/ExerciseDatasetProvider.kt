package com.musfit.data.repository

import android.content.Context
import com.musfit.data.local.entity.ExerciseEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Exercise catalog sourced from github.com/hasaneyldrm/exercises-dataset (educational / non-commercial;
 * media copyright belongs to the original holders). Only the factual text catalog ships in the APK;
 * thumbnails and GIFs are never bundled or redistributed — they are referenced by URL and fetched on
 * demand from the jsDelivr CDN below (then cached by Coil), so no copyrighted media lives in the app.
 */
const val EXERCISE_DATASET_CDN_BASE = "https://cdn.jsdelivr.net/gh/hasaneyldrm/exercises-dataset@main/"
const val EXERCISE_DATASET_ID_PREFIX = "ds-"
private const val EXERCISE_DATASET_ASSET = "exercises_dataset.json"

/** One row of the bundled catalog (English text only; image/gif are repo-relative paths). */
data class ExerciseDatasetRecord(
    val id: String,
    val name: String,
    val category: String,
    val equipment: String,
    val target: String,
    val secondary: String,
    val instructions: String,
    val image: String,
    val gif: String,
)

/** Loads the bundled exercise catalog for one-time import into Room. */
fun interface ExerciseDatasetProvider {
    suspend fun load(): List<ExerciseDatasetRecord>
}

class AssetExerciseDatasetProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : ExerciseDatasetProvider {
    private val adapter by lazy {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val type = Types.newParameterizedType(List::class.java, ExerciseDatasetRecord::class.java)
        moshi.adapter<List<ExerciseDatasetRecord>>(type)
    }

    override suspend fun load(): List<ExerciseDatasetRecord> = withContext(Dispatchers.IO) {
        // Degrade gracefully: a missing/corrupt asset must never crash first launch — the app
        // simply runs without dataset media until a valid catalog is present.
        runCatching {
            val json = context.assets.open(EXERCISE_DATASET_ASSET).bufferedReader().use { it.readText() }
            adapter.fromJson(json).orEmpty()
        }.getOrDefault(emptyList())
    }
}

/** Builds an absolute CDN media URL from a repo-relative path, or null when the path is blank. */
fun exerciseMediaUrl(relativePath: String): String? =
    relativePath.takeIf { it.isNotBlank() }?.let { EXERCISE_DATASET_CDN_BASE + it }

/** Maps a dataset record to a library [ExerciseEntity] (namespaced id + absolute CDN media URLs). */
fun ExerciseDatasetRecord.toExerciseEntity(): ExerciseEntity =
    ExerciseEntity(
        id = "$EXERCISE_DATASET_ID_PREFIX$id",
        name = name,
        category = category.ifBlank { "general" },
        equipment = equipment.takeIf { it.isNotBlank() },
        targetMuscles = target,
        isCustom = false,
        primaryMuscles = target,
        secondaryMuscles = secondary,
        instructions = instructions.takeIf { it.isNotBlank() },
        imageUrl = exerciseMediaUrl(image),
        gifUrl = exerciseMediaUrl(gif),
    )

/**
 * Curated map from a built-in starter exercise name to the dataset id whose media best represents
 * it. Hand-picked, not fuzzy-matched: token-overlap matching produced wrong demos (e.g. "Barbell
 * Row" -> an upright row), so each starter is mapped to an exact, sensible variant. Used to backfill
 * thumbnails/animations onto the starter exercises so existing routines show media immediately.
 */
val STARTER_EXERCISE_DATASET_IDS: Map<String, String> = mapOf(
    "Back Squat" to "0043",
    "Barbell Bench Press" to "0025",
    "Barbell Row" to "0027",
    "Deadlift" to "0032",
    "Dumbbell Biceps Curl" to "0294",
    "Dumbbell Shoulder Press" to "0405",
    "Incline Dumbbell Press" to "0314",
    "Lat Pulldown" to "2330",
    "Leg Curl" to "0586",
    "Leg Extension" to "0585",
    "Leg Press" to "0739",
    "Overhead Press" to "0091",
    "Romanian Deadlift" to "0085",
    "Seated Cable Row" to "0861",
    "Triceps Pushdown" to "0241",
)
