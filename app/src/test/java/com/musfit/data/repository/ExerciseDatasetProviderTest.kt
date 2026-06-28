package com.musfit.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

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
    fun toExerciseEntity_namespacesIdAndBuildsAbsoluteCdnUrls() {
        val entity = record().toExerciseEntity()

        assertEquals("ds-0001", entity.id)
        assertEquals("3/4 sit-up", entity.name)
        assertEquals("abs", entity.targetMuscles)
        assertEquals("abs", entity.primaryMuscles)
        assertEquals("hip flexors, lower back", entity.secondaryMuscles)
        assertFalse(entity.isCustom)
        assertEquals(
            "https://cdn.jsdelivr.net/gh/hasaneyldrm/exercises-dataset@main/images/0001-x.jpg",
            entity.imageUrl,
        )
        assertEquals(
            "https://cdn.jsdelivr.net/gh/hasaneyldrm/exercises-dataset@main/videos/0001-x.gif",
            entity.gifUrl,
        )
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
        assertEquals(EXERCISE_DATASET_CDN_BASE + "images/x.jpg", exerciseMediaUrl("images/x.jpg"))
    }
}
