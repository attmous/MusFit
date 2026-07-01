package com.musfit.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.musfit.data.local.MusFitDatabase
import com.musfit.data.local.entity.CoachMessageEntity
import com.musfit.domain.coach.CoachAction
import com.musfit.domain.coach.CoachMessageCandidate
import com.musfit.domain.coach.CoachMessageCategory
import com.musfit.domain.today.TodayMetric
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class LocalCoachRepositoryTest {
    private lateinit var database: MusFitDatabase
    private lateinit var repository: LocalCoachRepository
    private var nowMillis = 1_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MusFitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LocalCoachRepository(database, database.coachDao()) { nowMillis }
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun candidate(
        ruleKey: String = "protein_gap",
        title: String = "Protein's behind",
        body: String = "28 g to go.",
        action: CoachAction? = CoachAction.OpenFood,
    ) = CoachMessageCandidate(ruleKey, CoachMessageCategory.Nutrition, title, body, action)

    @Test
    fun sync_insertsNewCandidateUnreadWithFirstSeenTimestamp() = runTest {
        val day = LocalDate.of(2026, 7, 1)
        nowMillis = 5_000L

        repository.syncToday(day, listOf(candidate()))

        val feed = repository.observeFeed().first()
        assertEquals(1, feed.size)
        assertEquals("protein_gap", feed[0].ruleKey)
        assertEquals(5_000L, feed[0].firstSeenAtEpochMillis)
        assertEquals(false, feed[0].isRead)
        assertEquals(day, feed[0].day)
    }

    @Test
    fun sync_refreshesContentButNeverTouchesFirstSeenOrReadState() = runTest {
        val day = LocalDate.of(2026, 7, 1)
        nowMillis = 5_000L
        repository.syncToday(day, listOf(candidate(body = "28 g to go.")))
        repository.markAllRead()

        nowMillis = 9_000L
        repository.syncToday(day, listOf(candidate(body = "12 g to go.")))

        val message = repository.observeFeed().first().single()
        assertEquals("12 g to go.", message.body)
        assertEquals(5_000L, message.firstSeenAtEpochMillis)
        assertEquals(true, message.isRead)
    }

    @Test
    fun sync_noOpResyncLeavesRowIdentical() = runTest {
        val day = LocalDate.of(2026, 7, 1)
        repository.syncToday(day, listOf(candidate()))
        val before = repository.observeFeed().first().single()

        repository.syncToday(day, listOf(candidate()))

        val after = repository.observeFeed().first().single()
        assertEquals(before, after)
    }

    @Test
    fun sync_refreshesActionFieldsWhenRuleActionChanges() = runTest {
        val day = LocalDate.of(2026, 7, 1)
        repository.syncToday(day, listOf(candidate(action = CoachAction.StartRoutine("r1"))))

        repository.syncToday(day, listOf(candidate(action = CoachAction.OpenTraining)))

        val message = repository.observeFeed().first().single()
        assertEquals(CoachAction.OpenTraining, message.action)
    }

    @Test
    fun dismiss_hidesMessageAndSurvivesResync() = runTest {
        val day = LocalDate.of(2026, 7, 1)
        repository.syncToday(day, listOf(candidate()))
        val id = repository.observeFeed().first().single().id

        repository.dismiss(id)
        repository.syncToday(day, listOf(candidate()))

        assertTrue(repository.observeFeed().first().isEmpty())
    }

    @Test
    fun sync_neverTouchesAiRowsWithSameRuleKey() = runTest {
        val day = LocalDate.of(2026, 7, 1)
        database.coachDao().insert(
            CoachMessageEntity(
                dayEpochDay = day.toEpochDay(),
                ruleKey = "protein_gap",
                category = "Nutrition",
                title = "ai title",
                body = "ai body",
                actionType = null,
                actionData = null,
                firstSeenAtEpochMillis = 42L,
                isRead = false,
                isDismissed = false,
                source = "ai",
            ),
        )

        repository.syncToday(day, listOf(candidate()))

        val feed = repository.observeFeed().first()
        assertEquals(2, feed.size)
        val aiMessage = feed.single { it.source == "ai" }
        assertEquals("ai title", aiMessage.title)
        assertEquals("ai body", aiMessage.body)
        assertEquals(42L, aiMessage.firstSeenAtEpochMillis)
    }

    @Test
    fun sync_prunesMessagesOlderThan90Days() = runTest {
        val oldDay = LocalDate.of(2026, 3, 1)
        val today = LocalDate.of(2026, 7, 1) // 122 days later
        repository.syncToday(oldDay, listOf(candidate(ruleKey = "old_rule")))

        repository.syncToday(today, listOf(candidate()))

        val feed = repository.observeFeed().first()
        assertEquals(listOf("protein_gap"), feed.map { it.ruleKey })
    }

    @Test
    fun feed_ordersNewestDayFirstThenNewestFirstSeen() = runTest {
        val day1 = LocalDate.of(2026, 6, 30)
        val day2 = LocalDate.of(2026, 7, 1)
        nowMillis = 1_000L
        repository.syncToday(day1, listOf(candidate(ruleKey = "a")))
        nowMillis = 2_000L
        repository.syncToday(day2, listOf(candidate(ruleKey = "b")))
        nowMillis = 3_000L
        repository.syncToday(day2, listOf(candidate(ruleKey = "b"), candidate(ruleKey = "c")))

        val feed = repository.observeFeed().first()
        assertEquals(listOf("c", "b", "a"), feed.map { it.ruleKey })
    }

    @Test
    fun pins_roundTripInOrderAndEmptyFallsBackToDefaults() = runTest {
        // In-memory DB is created at version 26 (no migration ran), so no seeded rows:
        assertEquals(TodayMetric.DEFAULT_PINS, repository.observeDashboardPins().first())

        repository.saveDashboardPins(listOf(TodayMetric.Weight, TodayMetric.Water))
        assertEquals(
            listOf(TodayMetric.Weight, TodayMetric.Water),
            repository.observeDashboardPins().first(),
        )
    }

    @Test
    fun actionSerialization_unknownTypeParsesToNullAndRoutineFallsBackWithoutData() {
        assertNull(parseAction("some_future_ai_action", null))
        assertEquals(CoachAction.OpenTraining, parseAction("start_routine", null))
        assertEquals(CoachAction.StartRoutine("r1"), parseAction("start_routine", "r1"))
        assertEquals(CoachAction.OpenFood, parseAction("open_food", null))
    }
}
