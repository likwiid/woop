// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Transaction
    @Query("SELECT * FROM goals WHERE status != 'DRAFT' ORDER BY pinned DESC, updatedAt DESC")
    fun observeGoals(): Flow<List<GoalWithDetails>>

    @Transaction
    @Query("SELECT * FROM goals WHERE id = :id")
    fun observeGoal(id: Long): Flow<GoalWithDetails?>

    @Transaction
    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoal(id: Long): GoalWithDetails?

    @Transaction
    @Query("SELECT * FROM goals WHERE status != 'DRAFT' ORDER BY createdAt")
    suspend fun allGoals(): List<GoalWithDetails>

    @Query("SELECT * FROM goals WHERE status = 'DRAFT' ORDER BY updatedAt DESC LIMIT 1")
    fun observeLatestDraft(): Flow<GoalEntity?>

    @Query("SELECT * FROM goals WHERE status = 'ACTIVE'")
    suspend fun activeGoals(): List<GoalEntity>

    @Insert
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoal(id: Long)

    @Query("UPDATE goals SET pinned = 0")
    suspend fun clearPins()

    @Query("UPDATE goals SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("DELETE FROM obstacles WHERE goalId = :goalId")
    suspend fun deleteObstacles(goalId: Long)

    @Insert
    suspend fun insertObstacles(obstacles: List<ObstacleEntity>)

    @Insert
    suspend fun insertReview(review: ReviewEntity): Long

    @Query("SELECT COUNT(*) FROM reviews")
    fun observeReviewCount(): Flow<Int>
}

@Dao
interface PromptDao {
    @Query("SELECT * FROM prompts ORDER BY hour, minute")
    fun observeAll(): Flow<List<PromptEntity>>

    @Query("SELECT * FROM prompts")
    suspend fun getAll(): List<PromptEntity>

    @Query("SELECT * FROM prompts WHERE id = :id")
    suspend fun get(id: Long): PromptEntity?

    @Upsert
    suspend fun upsert(prompt: PromptEntity): Long

    @Query("DELETE FROM prompts WHERE id = :id")
    suspend fun delete(id: Long)
}
