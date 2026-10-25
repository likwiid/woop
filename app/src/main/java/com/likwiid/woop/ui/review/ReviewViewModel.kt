// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop.ui.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.likwiid.woop.data.GoalStatus
import com.likwiid.woop.data.GoalWithDetails
import com.likwiid.woop.data.Momentum
import com.likwiid.woop.data.PlanUse
import com.likwiid.woop.data.ReviewEntity
import com.likwiid.woop.data.WoopRepository
import com.likwiid.woop.reminders.ReminderScheduler
import com.likwiid.woop.ui.components.toDraft
import com.likwiid.woop.ui.components.toEntities
import com.likwiid.woop.ui.create.ObstacleDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/**
 * A check-in re-runs the contrast (re-read and re-imagine), then asks what happened and only
 * branches into tuning when something didn't work.
 */
enum class ReviewStep { REREAD, PLAN_USE, MOMENTUM, TUNE, DOABLE, NOTE, DONE }

enum class Doable { YES, SMALLER, LET_GO }

data class ReviewState(
    val loading: Boolean = true,
    val missing: Boolean = false,
    val details: GoalWithDetails? = null,
    val step: ReviewStep = ReviewStep.REREAD,
    val history: List<ReviewStep> = emptyList(),
    val planUse: PlanUse? = null,
    val momentum: Momentum? = null,
    val plans: List<ObstacleDraft> = emptyList(),
    val doable: Doable? = null,
    val wish: String = "",
    val note: String = "",
    val saving: Boolean = false,
    val nextCheckIn: ZonedDateTime? = null,
) {
    val progress: Int get() = when (step) {
        ReviewStep.REREAD -> 1
        ReviewStep.PLAN_USE, ReviewStep.MOMENTUM -> 2
        ReviewStep.TUNE, ReviewStep.DOABLE -> 3
        ReviewStep.NOTE, ReviewStep.DONE -> 4
    }
}

class ReviewViewModel(
    private val repository: WoopRepository,
    private val scheduler: ReminderScheduler,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val goalId: Long = savedState.get<Long>("goalId") ?: 0L
    private val _state = MutableStateFlow(ReviewState())
    val state: StateFlow<ReviewState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val d = repository.getGoal(goalId)
            _state.value = if (d == null || d.goal.status == GoalStatus.DRAFT) {
                ReviewState(loading = false, missing = true)
            } else {
                ReviewState(
                    loading = false,
                    details = d,
                    plans = d.obstacles.map { it.toDraft() }.ifEmpty { listOf(ObstacleDraft()) },
                    wish = d.goal.wish,
                )
            }
        }
    }

    private fun go(step: ReviewStep) = _state.update { it.copy(step = step, history = it.history + it.step) }

    /** Ignores a second tap that lands after the step already moved on. */
    private fun goFrom(expected: ReviewStep, step: ReviewStep) {
        if (_state.value.step == expected) go(step)
    }

    /** Returns false when there is nowhere to go back to. */
    fun back(): Boolean {
        val s = _state.value
        if (s.step == ReviewStep.DONE || s.history.isEmpty()) return false
        _state.update { it.copy(step = it.history.last(), history = it.history.dropLast(1)) }
        return true
    }

    fun reread() = goFrom(ReviewStep.REREAD, ReviewStep.PLAN_USE)

    fun pickPlanUse(use: PlanUse) {
        if (_state.value.step != ReviewStep.PLAN_USE) return
        _state.update { it.copy(planUse = use) }
        go(ReviewStep.MOMENTUM)
    }

    fun pickMomentum(m: Momentum) {
        if (_state.value.step != ReviewStep.MOMENTUM) return
        _state.update { it.copy(momentum = m) }
        val s = _state.value
        go(
            when {
                m == Momentum.DONE -> ReviewStep.NOTE
                s.planUse == PlanUse.MISSED || m == Momentum.STUCK -> ReviewStep.TUNE
                else -> ReviewStep.NOTE
            },
        )
    }

    fun setPlans(plans: List<ObstacleDraft>) = _state.update { it.copy(plans = plans) }

    fun tuned() = goFrom(ReviewStep.TUNE, if (_state.value.momentum == Momentum.STUCK) ReviewStep.DOABLE else ReviewStep.NOTE)

    fun pickDoable(d: Doable) {
        if (_state.value.step != ReviewStep.DOABLE) return
        _state.update { it.copy(doable = d, wish = if (d == Doable.SMALLER) it.wish else it.details?.goal?.wish.orEmpty()) }
        if (d != Doable.SMALLER) go(ReviewStep.NOTE)
    }

    fun setWish(text: String) = _state.update { it.copy(wish = text) }

    fun doableDone() = goFrom(ReviewStep.DOABLE, ReviewStep.NOTE)

    fun setNote(text: String) = _state.update { it.copy(note = text) }

    fun finish() {
        val s = _state.value
        val d = s.details ?: return
        if (s.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val status = when {
                s.momentum == Momentum.DONE -> GoalStatus.ACHIEVED
                s.doable == Doable.LET_GO -> GoalStatus.LET_GO
                else -> GoalStatus.ACTIVE
            }
            val goal = d.goal.copy(
                wish = s.wish.trim().ifBlank { d.goal.wish },
                status = status,
                pinned = d.goal.pinned && status == GoalStatus.ACTIVE,
            )
            repository.completeReview(
                goal = goal,
                obstacles = s.plans.toEntities(goal.id),
                review = ReviewEntity(
                    goalId = goal.id,
                    at = now,
                    planUse = s.planUse ?: PlanUse.NOT_YET,
                    momentum = s.momentum ?: Momentum.MOVING,
                    stillDoable = s.doable != Doable.LET_GO,
                    note = s.note.trim(),
                ),
            )
            val next = scheduler.nextCheckIn(goal.copy(lastReviewedAt = now))
            _state.update { it.copy(saving = false, step = ReviewStep.DONE, nextCheckIn = next) }
        }
    }
}
