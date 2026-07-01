package com.musfit.data.repository

import androidx.room.withTransaction
import com.musfit.data.local.MusFitDatabase
import com.musfit.data.local.dao.CoachDao
import com.musfit.data.local.entity.CoachMessageEntity
import com.musfit.data.local.entity.DashboardPinEntity
import com.musfit.domain.coach.CoachAction
import com.musfit.domain.coach.CoachMessageCandidate
import com.musfit.domain.coach.CoachMessageCategory
import com.musfit.domain.today.TodayMetric
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/** A coach feed message as read by the UI (dismissed rows are filtered out). */
data class CoachMessage(
    val id: Long,
    val day: LocalDate,
    val ruleKey: String,
    val category: CoachMessageCategory,
    val title: String,
    val body: String,
    val action: CoachAction?,
    val firstSeenAtEpochMillis: Long,
    val isRead: Boolean,
    val source: String,
)

const val COACH_SOURCE_RULES = "rules"
internal const val COACH_RETENTION_DAYS = 90L

interface CoachRepository {
    fun observeFeed(): Flow<List<CoachMessage>>

    /**
     * Transactional upsert of today's candidates, deduped by (day, ruleKey, source):
     * new keys insert with firstSeenAt = now; existing keys refresh content only when
     * changed; dismissed rows are tombstones and never resurrected. Prunes rows older
     * than [COACH_RETENTION_DAYS]. Never back-fills prior days.
     */
    suspend fun syncToday(day: LocalDate, candidates: List<CoachMessageCandidate>)

    suspend fun dismiss(id: Long)

    suspend fun markAllRead()

    fun observeDashboardPins(): Flow<List<TodayMetric>>

    suspend fun saveDashboardPins(ordered: List<TodayMetric>)
}

class LocalCoachRepository @Inject constructor(
    private val database: MusFitDatabase,
    private val coachDao: CoachDao,
) : CoachRepository {
    private var clock: () -> Long = { System.currentTimeMillis() }

    internal constructor(
        database: MusFitDatabase,
        coachDao: CoachDao,
        clock: () -> Long,
    ) : this(database, coachDao) {
        this.clock = clock
    }

    override fun observeFeed(): Flow<List<CoachMessage>> =
        coachDao.observeFeed().map { rows -> rows.mapNotNull { it.toMessage() } }

    override suspend fun syncToday(day: LocalDate, candidates: List<CoachMessageCandidate>) {
        val dayEpochDay = day.toEpochDay()
        database.withTransaction {
            val existing =
                coachDao.getMessagesForDay(dayEpochDay, COACH_SOURCE_RULES).associateBy { it.ruleKey }
            candidates.forEach { candidate ->
                val row = existing[candidate.ruleKey]
                when {
                    row == null ->
                        coachDao.insert(
                            CoachMessageEntity(
                                dayEpochDay = dayEpochDay,
                                ruleKey = candidate.ruleKey,
                                category = candidate.category.name,
                                title = candidate.title,
                                body = candidate.body,
                                actionType = candidate.action?.type(),
                                actionData = candidate.action?.data(),
                                firstSeenAtEpochMillis = clock(),
                                isRead = false,
                                isDismissed = false,
                                source = COACH_SOURCE_RULES,
                            ),
                        )
                    row.isDismissed -> Unit // tombstone: never resurrect
                    else -> {
                        val refreshed = row.copy(
                            title = candidate.title,
                            body = candidate.body,
                            actionType = candidate.action?.type(),
                            actionData = candidate.action?.data(),
                        )
                        if (refreshed != row) coachDao.update(refreshed)
                    }
                }
            }
            coachDao.prune(dayEpochDay - COACH_RETENTION_DAYS)
        }
    }

    override suspend fun dismiss(id: Long) = coachDao.dismiss(id)

    override suspend fun markAllRead() = coachDao.markAllRead()

    override fun observeDashboardPins(): Flow<List<TodayMetric>> =
        coachDao.observePins().map { rows ->
            rows.sortedBy { it.position }
                .mapNotNull { TodayMetric.fromId(it.metricId) }
                .ifEmpty { TodayMetric.DEFAULT_PINS }
        }

    override suspend fun saveDashboardPins(ordered: List<TodayMetric>) {
        val pins = ordered.ifEmpty { TodayMetric.DEFAULT_PINS }
        coachDao.replacePins(pins.mapIndexed { index, metric -> DashboardPinEntity(metric.id, index) })
    }
}

private fun CoachMessageEntity.toMessage(): CoachMessage? {
    val parsedCategory = runCatching { CoachMessageCategory.valueOf(category) }.getOrNull() ?: return null
    return CoachMessage(
        id = id,
        day = LocalDate.ofEpochDay(dayEpochDay),
        ruleKey = ruleKey,
        category = parsedCategory,
        title = title,
        body = body,
        action = parseAction(actionType, actionData),
        firstSeenAtEpochMillis = firstSeenAtEpochMillis,
        isRead = isRead,
        source = source,
    )
}

private const val ACTION_OPEN_FOOD = "open_food"
private const val ACTION_OPEN_TRAINING = "open_training"
private const val ACTION_OPEN_PROFILE = "open_profile"
private const val ACTION_START_ROUTINE = "start_routine"

internal fun CoachAction.type(): String = when (this) {
    CoachAction.OpenFood -> ACTION_OPEN_FOOD
    CoachAction.OpenTraining -> ACTION_OPEN_TRAINING
    CoachAction.OpenHealth -> ACTION_OPEN_PROFILE
    is CoachAction.StartRoutine -> ACTION_START_ROUTINE
}

internal fun CoachAction.data(): String? = (this as? CoachAction.StartRoutine)?.routineId

/** Unknown action types (e.g. future `source = 'ai'` writers) render without a button. */
internal fun parseAction(type: String?, data: String?): CoachAction? = when (type) {
    ACTION_OPEN_FOOD -> CoachAction.OpenFood
    ACTION_OPEN_TRAINING -> CoachAction.OpenTraining
    ACTION_OPEN_PROFILE -> CoachAction.OpenHealth
    ACTION_START_ROUTINE -> data?.let { CoachAction.StartRoutine(it) } ?: CoachAction.OpenTraining
    else -> null
}
