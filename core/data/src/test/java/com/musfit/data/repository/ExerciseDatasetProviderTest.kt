package com.musfit.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

private const val TEST_GIF_MIRROR_BASE =
    "https://gitlab.stud.idi.ntnu.no/gruppe-1/prog2052-prosjekt/-/raw/main/backend/assets/exercises/"

class ExerciseDatasetProviderTest {
    private fun record(
        image: String = "images/0001-x.jpg",
        gif: String = "videos/0001-x.gif",
        equipment: String = "body weight",
    ) = ExerciseDatasetRecord(
        id = "0001",
        name = "3/4 sit-up",
        category = "waist",
        equipment = equipment,
        target = "abs",
        secondary = "hip flexors, lower back",
        instructions = "Lie flat and curl up.",
        image = image,
        gif = gif,
    )

    @Test
    fun toExerciseEntity_namespacesIdAndBuildsGifMirrorMediaUrls() {
        val entity = record().toExerciseEntity()

        assertEquals("ds-0001", entity.id)
        assertEquals("3/4 sit-up", entity.name)
        assertEquals("abs", entity.targetMuscles)
        assertEquals("abs", entity.primaryMuscles)
        assertEquals("hip flexors, lower back", entity.secondaryMuscles)
        assertFalse(entity.isCustom)
        assertEquals(TEST_GIF_MIRROR_BASE + "x.gif", entity.imageUrl)
        assertEquals(TEST_GIF_MIRROR_BASE + "x.gif", entity.gifUrl)
    }

    @Test
    fun toExerciseEntity_blankMediaAndEquipmentBecomeNull() {
        val entity = record(image = "", gif = "", equipment = "").toExerciseEntity()

        assertNull(entity.imageUrl)
        assertNull(entity.gifUrl)
        assertNull(entity.equipment)
    }

    @Test
    fun exerciseMediaUrl_blankPathIsNull() {
        assertNull(exerciseMediaUrl(""))
        assertNull(exerciseMediaUrl("   "))
    }

    @Test
    fun exerciseMediaUrl_relativeDatasetMediaPathBuildsGifMirrorUrl() {
        assertEquals(TEST_GIF_MIRROR_BASE + "x.gif", exerciseMediaUrl("images/0001-x.jpg"))
        assertEquals(TEST_GIF_MIRROR_BASE + "x.gif", exerciseMediaUrl("videos/0001-x.gif"))
    }

    @Test
    fun exerciseMediaUrl_staleJsDelivrDatasetUrlBuildsGifMirrorUrl() {
        assertEquals(
            TEST_GIF_MIRROR_BASE + "x.gif",
            exerciseMediaUrl(EXERCISE_DATASET_LEGACY_CDN_BASE + "images/0001-x.jpg"),
        )
        assertEquals(
            TEST_GIF_MIRROR_BASE + "x.gif",
            exerciseMediaUrl(EXERCISE_DATASET_LEGACY_CDN_BASE + "videos/0001-x.gif"),
        )
    }

    @Test
    fun exerciseMediaUrl_preservesExerciseGifMirrorUrl() {
        assertEquals(
            TEST_GIF_MIRROR_BASE + "2gPfomN.gif",
            exerciseMediaUrl(" $TEST_GIF_MIRROR_BASE" + "2gPfomN.gif "),
        )
    }

    @Test
    fun exerciseMediaUrl_preservesAbsoluteUrlsOutsideLegacyDatasetCdn() {
        assertEquals("https://example.com/media/x.gif", exerciseMediaUrl(" https://example.com/media/x.gif "))
    }
}
