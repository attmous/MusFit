package com.musfit.data.local

import androidx.room.RoomDatabase
import com.musfit.data.local.dao.FoodDao
import com.musfit.data.local.dao.HealthDao
import com.musfit.data.local.dao.TrainingDao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusFitDatabaseTest {
    @Test
    fun database_exposesExpectedDaosAndGeneratedImplementation() {
        assertTrue(RoomDatabase::class.java.isAssignableFrom(MusFitDatabase::class.java))
        assertEquals(FoodDao::class.java, MusFitDatabase::class.java.getMethod("foodDao").returnType)
        assertEquals(TrainingDao::class.java, MusFitDatabase::class.java.getMethod("trainingDao").returnType)
        assertEquals(HealthDao::class.java, MusFitDatabase::class.java.getMethod("healthDao").returnType)
        assertTrue(
            RoomDatabase::class.java.isAssignableFrom(
                Class.forName("com.musfit.data.local.MusFitDatabase_Impl"),
            ),
        )
    }
}
