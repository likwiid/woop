// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.likwiid.woop.data.GoalEntity
import com.likwiid.woop.data.GoalStatus
import com.likwiid.woop.data.GoalWithDetails
import com.likwiid.woop.data.WoopRepository
import com.likwiid.woop.reminders.ReminderScheduler
import com.likwiid.woop.ui.components.toEntities
import com.likwiid.woop.ui.create.ObstacleDraft
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

data class DetailState(
    val loading: Boolean = true,
    val details: GoalWithDetails? = null,
    val due: Boolean = false,
    val nextCheckIn: ZonedDateTime? = null,
)

class GoalDetailViewModel(
    private val repository: WoopRepository,
    private val scheduler: ReminderScheduler,
    savedState: SavedStateHandle,
) : ViewModel() {
    val goalId: Long = savedState.get<Long>("goalId") ?: 0L

    val state: StateFlow<DetailState> = repository.goal(goalId)
        .map { d ->
            val now = ZonedDateTime.now()
            DetailState(
                loading = false,
                details = d,
                due = d != null && scheduler.isCheckInDue(d.goal, now),
                nextCheckIn = d?.let { scheduler.nextCheckIn(it.goal, now) },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailState())

    private fun current() = state.value.details

    fun setWish(text: String) = editGoal { it.copy(wish = text.trim()) }

    fun setOutcome(text: String) = editGoal { it.copy(outcome = text.trim()) }

    fun setPlans(plans: List<ObstacleDraft>) {
        val d = current() ?: return
        viewModelScope.launch { repository.saveGoal(d.goal, plans.toEntities(d.goal.id)) }
    }

    fun setCheckIn(everyDays: Int, hour: Int, minute: Int) =
        editGoal { it.copy(checkInEveryDays = everyDays, checkInHour = hour, checkInMinute = minute) }

    fun setStatus(status: GoalStatus) = editGoal { g ->
        // Reactivating starts the check-in clock again from today.
        val restarted = if (status == GoalStatus.ACTIVE && g.status != GoalStatus.ACTIVE) g.copy(lastReviewedAt = System.currentTimeMillis()) else g
        restarted.copy(status = status, pinned = if (status == GoalStatus.ACTIVE) g.pinned else false)
    }

    fun togglePin() {
        val d = current() ?: return
        viewModelScope.launch { repository.setPinned(d.goal.id, !d.goal.pinned) }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteGoal(goalId)
            onDeleted()
        }
    }

    private fun editGoal(block: (GoalEntity) -> GoalEntity) {
        val d = current() ?: return
        viewModelScope.launch { repository.updateGoal(block(d.goal)) }
    }
}
