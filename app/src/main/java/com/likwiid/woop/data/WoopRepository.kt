// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop.data

import androidx.room.withTransaction
import com.likwiid.woop.reminders.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class WoopRepository(
    private val db: WoopDatabase,
    private val scheduler: ReminderScheduler,
) {
    private val goalDao = db.goalDao()
    private val promptDao = db.promptDao()

    val goals: Flow<List<GoalWithDetails>> = goalDao.observeGoals()
    val latestDraft: Flow<GoalEntity?> = goalDao.observeLatestDraft()
    val checkInCount: Flow<Int> = goalDao.observeReviewCount()
    val prompts: Flow<List<PromptEntity>> = promptDao.observeAll()

    fun goal(id: Long): Flow<GoalWithDetails?> = goalDao.observeGoal(id)

    suspend fun getGoal(id: Long): GoalWithDetails? = goalDao.getGoal(id)

    suspend fun getPrompt(id: Long): PromptEntity? = promptDao.get(id)

    /** Inserts or updates a goal together with its obstacles; returns the goal id. */
    suspend fun saveGoal(goal: GoalEntity, obstacles: List<ObstacleEntity>): Long {
        val id = db.withTransaction {
            val stamped = goal.copy(updatedAt = System.currentTimeMillis())
            val id = if (stamped.id == 0L) goalDao.insertGoal(stamped) else stamped.id.also { goalDao.updateGoal(stamped) }
            goalDao.deleteObstacles(id)
            goalDao.insertObstacles(obstacles.mapIndexed { i, o -> o.copy(id = 0, goalId = id, position = i) })
            id
        }
        scheduler.scheduleCheckIn(goal.copy(id = id))
        return id
    }

    suspend fun updateGoal(goal: GoalEntity) {
        goalDao.updateGoal(goal.copy(updatedAt = System.currentTimeMillis()))
        scheduler.scheduleCheckIn(goal)
    }

    suspend fun setPinned(goalId: Long, pinned: Boolean) = db.withTransaction {
        if (pinned) goalDao.clearPins()
        goalDao.setPinned(goalId, pinned)
    }

    suspend fun deleteGoal(goalId: Long) {
        scheduler.cancelCheckIn(goalId)
        goalDao.deleteGoal(goalId)
    }

    suspend fun completeReview(
        goal: GoalEntity,
        obstacles: List<ObstacleEntity>,
        review: ReviewEntity,
    ) {
        db.withTransaction {
            goalDao.updateGoal(goal.copy(updatedAt = review.at, lastReviewedAt = review.at))
            goalDao.deleteObstacles(goal.id)
            goalDao.insertObstacles(obstacles.mapIndexed { i, o -> o.copy(id = 0, goalId = goal.id, position = i) })
            goalDao.insertReview(review.copy(goalId = goal.id))
        }
        scheduler.scheduleCheckIn(goal.copy(lastReviewedAt = review.at))
    }

    suspend fun savePrompt(prompt: PromptEntity): Long {
        val rowId = promptDao.upsert(prompt)
        val id = if (prompt.id == 0L) rowId else prompt.id
        scheduler.schedulePrompt(prompt.copy(id = id))
        return id
    }

    suspend fun deletePrompt(id: Long) {
        scheduler.cancelPrompt(id)
        promptDao.delete(id)
    }

    suspend fun rescheduleAllReminders() {
        promptDao.getAll().forEach { scheduler.schedulePrompt(it) }
        goalDao.activeGoals().forEach { scheduler.scheduleCheckIn(it) }
    }

    suspend fun deleteEverything() {
        promptDao.getAll().forEach { scheduler.cancelPrompt(it.id) }
        goalDao.activeGoals().forEach { scheduler.cancelCheckIn(it.id) }
        withContext(Dispatchers.IO) { db.clearAllTables() }
    }

    suspend fun exportJson(): String {
        val goals = JSONArray()
        goalDao.allGoals().forEach { details ->
            val g = details.goal
            goals.put(
                JSONObject()
                    .put("wish", g.wish)
                    .put("outcome", g.outcome)
                    .put("timeframe", g.timeframe.name)
                    .put("likelihood", g.likelihood)
                    .put("status", g.status.name)
                    .put("createdAt", g.createdAt)
                    .put("lastReviewedAt", g.lastReviewedAt ?: JSONObject.NULL)
                    .put("checkInEveryDays", g.checkInEveryDays)
                    .put(
                        "obstacles",
                        JSONArray(
                            details.obstacles.map {
                                JSONObject().put("obstacle", it.text).put("if", it.ifCue).put("then", it.thenAction)
                            },
                        ),
                    )
                    .put(
                        "checkIns",
                        JSONArray(
                            details.reviews.map {
                                JSONObject()
                                    .put("at", it.at)
                                    .put("planUse", it.planUse.name)
                                    .put("momentum", it.momentum.name)
                                    .put("stillDoable", it.stillDoable)
                                    .put("note", it.note)
                            },
                        ),
                    ),
            )
        }
        return JSONObject().put("app", "WOOP").put("version", 1).put("goals", goals).toString(2)
    }
}
