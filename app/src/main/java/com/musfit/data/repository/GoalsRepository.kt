package com.musfit.data.repository

import com.musfit.data.local.dao.UserGoalsDao
import com.musfit.data.local.entity.UserGoalsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Cross-cutting Today goals (not held in `food_goals`). Defaults apply when nothing is saved yet. */
data class UserGoals(
    val stepGoal: Long = 10_000L,
    val weeklySessionTarget: Int = 4,
    val targetWeightKg: Double = 0.0,
)

interface GoalsRepository {
    fun observeUserGoals(): Flow<UserGoals>

    suspend fun updateUserGoals(goals: UserGoals)
}

class LocalGoalsRepository @Inject constructor(
    private val userGoalsDao: UserGoalsDao,
) : GoalsRepository {
    private var clock: () -> Long = { System.currentTimeMillis() }

    override fun observeUserGoals(): Flow<UserGoals> =
        userGoalsDao.observeUserGoals(DEFAULT_ID).map { entity ->
            entity?.let { UserGoals(it.stepGoal, it.weeklySessionTarget, it.targetWeightKg) } ?: UserGoals()
        }

    override suspend fun updateUserGoals(goals: UserGoals) {
        userGoalsDao.upsertUserGoals(
            UserGoalsEntity(
                id = DEFAULT_ID,
                stepGoal = goals.stepGoal.coerceAtLeast(0L),
                weeklySessionTarget = goals.weeklySessionTarget.coerceAtLeast(0),
                targetWeightKg = goals.targetWeightKg.coerceAtLeast(0.0),
                updatedAtEpochMillis = clock(),
            ),
        )
    }

    private companion object {
        const val DEFAULT_ID = "default"
    }
}
