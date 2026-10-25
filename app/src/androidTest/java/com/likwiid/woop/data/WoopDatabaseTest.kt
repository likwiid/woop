// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop.data

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.likwiid.woop.reminders.ReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs against an in-memory database on a device or emulator.
 *
 * The repository talks to the real AlarmManager, keyed by goal id. These tests start goal ids at
 * [FIRST_TEST_ID] and use goals without check-ins, so they never set or cancel a real reminder.
 */
@RunWith(AndroidJUnit4::class)
class WoopDatabaseTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: WoopDatabase
    private lateinit var goals: GoalDao
    private lateinit var prompts: PromptDao
    private lateinit var repository: WoopRepository

    @get:Rule
    val migrations = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), WoopDatabase::class.java)

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(context, WoopDatabase::class.java).build()
        goals = db.goalDao()
        prompts = db.promptDao()
        repository = WoopRepository(db, ReminderScheduler(context))
        // Pushes AUTOINCREMENT past any id a real goal could have.
        goals.insertGoal(goal(id = FIRST_TEST_ID - 1))
        goals.deleteGoal(FIRST_TEST_ID - 1)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun draftsStayOutOfTheGoalListButShowAsLatestDraft() = runBlocking {
        val active = goals.insertGoal(goal(wish = "Active", status = GoalStatus.ACTIVE, updatedAt = 1))
        goals.insertGoal(goal(wish = "Old draft", updatedAt = 2))
        val newDraft = goals.insertGoal(goal(wish = "New draft", updatedAt = 3))

        assertEquals(listOf(active), goals.observeGoals().first().map { it.goal.id })
        assertEquals(newDraft, goals.observeLatestDraft().first()?.id)
    }

    @Test
    fun goalListPutsThePinnedGoalFirstThenMostRecent() = runBlocking {
        val older = goals.insertGoal(goal(status = GoalStatus.ACTIVE, updatedAt = 1))
        val newer = goals.insertGoal(goal(status = GoalStatus.ACTIVE, updatedAt = 2))
        val pinned = goals.insertGoal(goal(status = GoalStatus.ACHIEVED, updatedAt = 0, pinned = true))

        assertEquals(listOf(pinned, newer, older), goals.observeGoals().first().map { it.goal.id })
    }

    @Test
    fun detailsSortObstaclesByPositionAndReviewsNewestFirst() = runBlocking {
        val id = goals.insertGoal(goal(status = GoalStatus.ACTIVE))
        goals.insertObstacles(
            listOf(
                ObstacleEntity(goalId = id, position = 1, text = "Second", ifCue = "if 2", thenAction = "then 2"),
                ObstacleEntity(goalId = id, position = 0, text = "First", ifCue = "if 1", thenAction = ""),
            ),
        )
        goals.insertReview(review(id, at = 10))
        goals.insertReview(review(id, at = 30))
        goals.insertReview(review(id, at = 20))

        val details = goals.getGoal(id)!!
        assertEquals(listOf("First", "Second"), details.obstacles.map { it.text })
        assertEquals(listOf(30L, 20L, 10L), details.reviews.map { it.at })
        // The first obstacle has no "then" yet, so the plan shown is the second one.
        assertEquals("Second", details.firstPlan?.text)
    }

    @Test
    fun deletingAGoalDeletesItsObstaclesAndCheckIns() = runBlocking {
        val keep = goals.insertGoal(goal(status = GoalStatus.ACTIVE))
        val remove = goals.insertGoal(goal(status = GoalStatus.ACTIVE))
        goals.insertObstacles(listOf(ObstacleEntity(goalId = remove, position = 0, text = "o", ifCue = "i", thenAction = "t")))
        goals.insertReview(review(keep, at = 1))
        goals.insertReview(review(remove, at = 2))

        goals.deleteGoal(remove)

        assertNull(goals.getGoal(remove))
        assertEquals(1, goals.observeReviewCount().first())
        val orphans = db.query("SELECT COUNT(*) FROM obstacles WHERE goalId = $remove", null).use { it.moveToFirst(); it.getInt(0) }
        assertEquals(0, orphans)
    }

    @Test
    fun promptUpsertInsertsThenUpdatesAndListsByTime() = runBlocking {
        val evening = prompts.upsert(PromptEntity(label = "Evening", hour = 20))
        prompts.upsert(PromptEntity(label = "Morning", hour = 7))
        prompts.upsert(PromptEntity(id = evening, label = "Evening", hour = 21, enabled = false))

        val all = prompts.observeAll().first()
        assertEquals(listOf("Morning", "Evening"), all.map { it.label })
        assertEquals(21, all.last().hour)
        assertEquals(false, all.last().enabled)
    }

    @Test
    fun saveGoalReplacesObstaclesAndRenumbersThem() = runBlocking {
        val id = repository.saveGoal(
            goal(wish = "Run", status = GoalStatus.ACTIVE),
            listOf(obstacle("Tired"), obstacle("Rain"), obstacle("Busy")),
        )
        assertTrue(id >= FIRST_TEST_ID)

        val saved = repository.getGoal(id)!!
        repository.saveGoal(saved.goal.copy(wish = "Run 5 km"), listOf(saved.obstacles[2], saved.obstacles[0]))

        val updated = repository.getGoal(id)!!
        assertEquals("Run 5 km", updated.goal.wish)
        assertEquals(listOf("Busy", "Tired"), updated.obstacles.map { it.text })
        assertEquals(listOf(0, 1), updated.obstacles.map { it.position })
    }

    @Test
    fun completeReviewStoresTheCheckInAndTheTunedPlan() = runBlocking {
        val id = repository.saveGoal(goal(status = GoalStatus.ACTIVE), listOf(obstacle("Phone", then = "Old plan")))
        val details = repository.getGoal(id)!!

        repository.completeReview(
            details.goal,
            listOf(details.obstacles[0].copy(thenAction = "Phone in the kitchen")),
            review(goalId = 0, at = 5_000, planUse = PlanUse.MISSED, momentum = Momentum.STUCK, note = "Too vague"),
        )

        val after = repository.getGoal(id)!!
        assertEquals(5_000L, after.goal.lastReviewedAt)
        assertEquals("Phone in the kitchen", after.firstPlan?.thenAction)
        assertEquals(1, after.reviews.size)
        assertEquals(id, after.reviews[0].goalId)
        assertEquals(PlanUse.MISSED, after.reviews[0].planUse)
        assertEquals("Too vague", after.reviews[0].note)
        assertEquals(1, repository.checkInCount.first())
    }

    @Test
    fun pinningAGoalUnpinsTheOthers() = runBlocking {
        val first = repository.saveGoal(goal(status = GoalStatus.ACTIVE), emptyList())
        val second = repository.saveGoal(goal(status = GoalStatus.ACTIVE), emptyList())

        repository.setPinned(first, true)
        repository.setPinned(second, true)

        assertEquals(false, repository.getGoal(first)!!.goal.pinned)
        assertEquals(true, repository.getGoal(second)!!.goal.pinned)
    }

    @Test
    fun exportIncludesGoalsAndCheckInsButNotDrafts() = runBlocking {
        val id = repository.saveGoal(goal(wish = "Read", status = GoalStatus.ACTIVE), listOf(obstacle("Phone")))
        repository.saveGoal(goal(wish = "Half-written"), emptyList())
        goals.insertReview(review(id, at = 1, note = "Helped"))

        val json = JSONObject(repository.exportJson())
        val exported = json.getJSONArray("goals")
        assertEquals(1, exported.length())
        val g = exported.getJSONObject(0)
        assertEquals("Read", g.getString("wish"))
        assertEquals("Phone", g.getJSONArray("obstacles").getJSONObject(0).getString("obstacle"))
        assertEquals("Helped", g.getJSONArray("checkIns").getJSONObject(0).getString("note"))
    }

    @Test
    fun deleteEverythingEmptiesTheDatabase() = runBlocking {
        val id = repository.saveGoal(goal(status = GoalStatus.ACTIVE), listOf(obstacle("x")))
        goals.insertReview(review(id, at = 1))

        // Only prompts with test ids, so the real prompt alarms stay untouched.
        prompts.upsert(PromptEntity(id = FIRST_TEST_ID, enabled = false))
        repository.deleteEverything()

        assertTrue(goals.allGoals().isEmpty())
        assertTrue(prompts.getAll().isEmpty())
        assertEquals(0, goals.observeReviewCount().first())
    }

    /** Fails if the entities drift from the exported schema without a version bump and a migration. */
    @Test
    fun entitiesMatchTheExportedSchema() {
        migrations.createDatabase(SCHEMA_TEST_DB, 1).close()
        migrations.runMigrationsAndValidate(SCHEMA_TEST_DB, 1, true).close()
    }

    private fun goal(
        id: Long = 0,
        wish: String = "Wish",
        status: GoalStatus = GoalStatus.DRAFT,
        updatedAt: Long = 0,
        pinned: Boolean = false,
    ) = GoalEntity(id = id, wish = wish, status = status, pinned = pinned, createdAt = 0, updatedAt = updatedAt)

    private fun obstacle(text: String, then: String = "act") =
        ObstacleEntity(goalId = 0, position = 0, text = text, ifCue = text, thenAction = then)

    private fun review(
        goalId: Long,
        at: Long,
        planUse: PlanUse = PlanUse.USED,
        momentum: Momentum = Momentum.MOVING,
        note: String = "",
    ) = ReviewEntity(goalId = goalId, at = at, planUse = planUse, momentum = momentum, stillDoable = true, note = note)

    private companion object {
        const val FIRST_TEST_ID = 900_000L
        const val SCHEMA_TEST_DB = "schema-test.db"
    }
}
