// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class Timeframe(val label: String, val hint: String) {
    TODAY("Today", "Something you can do by tonight"),
    WEEK("This week", "A step you can take in the next 7 days"),
    MONTH("This month", "A result a few weeks away"),
    LONGER("Bigger picture", "A long-term wish, months or years out"),
}

enum class GoalStatus { DRAFT, ACTIVE, ACHIEVED, LET_GO }

/** Did the obstacle show up since the last check-in, and did the if-then fire? */
enum class PlanUse { USED, MISSED, NOT_YET }

enum class Momentum { MOVING, STUCK, DONE }

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wish: String = "",
    val outcome: String = "",
    val timeframe: Timeframe = Timeframe.WEEK,
    /** 1 (very unlikely) to 5 (very likely); 0 = not answered yet. */
    val likelihood: Int = 0,
    val status: GoalStatus = GoalStatus.DRAFT,
    val pinned: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val lastReviewedAt: Long? = null,
    /** 0 = no check-in reminders. */
    val checkInEveryDays: Int = 0,
    val checkInHour: Int = 19,
    val checkInMinute: Int = 0,
)

@Entity(
    tableName = "obstacles",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("goalId")],
)
data class ObstacleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val position: Int,
    val text: String,
    val ifCue: String,
    val thenAction: String,
)

@Entity(
    tableName = "reviews",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("goalId")],
)
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val at: Long,
    val planUse: PlanUse,
    val momentum: Momentum,
    val stillDoable: Boolean,
    val note: String,
)

/** A reminder to sit down and write a new WOOP. */
@Entity(tableName = "prompts")
data class PromptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String = "",
    val hour: Int = 8,
    val minute: Int = 0,
    /** Monday = bit 0 … Sunday = bit 6. */
    val daysMask: Int = 0b1111111,
    val enabled: Boolean = true,
)

data class GoalWithDetails(
    @Embedded val goal: GoalEntity,
    @Relation(parentColumn = "id", entityColumn = "goalId")
    val obstacleRows: List<ObstacleEntity>,
    @Relation(parentColumn = "id", entityColumn = "goalId")
    val reviewRows: List<ReviewEntity>,
) {
    val obstacles: List<ObstacleEntity> get() = obstacleRows.sortedBy { it.position }
    val reviews: List<ReviewEntity> get() = reviewRows.sortedByDescending { it.at }
    val firstPlan: ObstacleEntity? get() = obstacles.firstOrNull { it.thenAction.isNotBlank() }
}
