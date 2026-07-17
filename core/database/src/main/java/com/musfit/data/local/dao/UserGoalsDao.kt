package com.musfit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musfit.data.local.entity.UserGoalsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserGoalsDao {
    @Query("SELECT * FROM user_goals WHERE id = :id LIMIT 1")
    fun observeUserGoals(id: String): Flow<UserGoalsEntity?>

    @Query("SELECT * FROM user_goals WHERE id = :id LIMIT 1")
    suspend fun getUserGoals(id: String): UserGoalsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserGoals(entity: UserGoalsEntity)
}
