// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.likwiid.woop.data.GoalEntity
import com.likwiid.woop.data.GoalStatus
import com.likwiid.woop.data.GoalWithDetails
import com.likwiid.woop.data.WoopRepository
import com.likwiid.woop.reminders.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

data class GoalCard(
    val details: GoalWithDetails,
    val due: Boolean,
    val nextCheckIn: ZonedDateTime?,
)

data class TodayState(
    val loading: Boolean = true,
    val focus: GoalCard? = null,
    val due: List<GoalCard> = emptyList(),
    val active: List<GoalCard> = emptyList(),
    val achieved: List<GoalCard> = emptyList(),
    val letGo: List<GoalCard> = emptyList(),
    val draft: GoalEntity? = null,
    val checkIns: Int = 0,
) {
    val isEmpty: Boolean get() = focus == null && active.isEmpty() && achieved.isEmpty() && letGo.isEmpty() && draft == null
}

class TodayViewModel(
    private val repository: WoopRepository,
    private val scheduler: ReminderScheduler,
) : ViewModel() {
    private val now = MutableStateFlow(ZonedDateTime.now())

    val state: StateFlow<TodayState> = combine(
        repository.goals,
        repository.latestDraft,
        repository.checkInCount,
        now,
    ) { goals, draft, checkIns, time ->
        fun card(d: GoalWithDetails) = GoalCard(d, scheduler.isCheckInDue(d.goal, time), scheduler.nextCheckIn(d.goal, time))
        val active = goals.filter { it.goal.status == GoalStatus.ACTIVE }.map(::card)
        val focus = active.firstOrNull { it.details.goal.pinned } ?: active.firstOrNull()
        TodayState(
            loading = false,
            focus = focus,
            due = active.filter { it.due && it != focus },
            active = active.filter { it != focus },
            achieved = goals.filter { it.goal.status == GoalStatus.ACHIEVED }.map(::card),
            letGo = goals.filter { it.goal.status == GoalStatus.LET_GO }.map(::card),
            draft = draft,
            checkIns = checkIns,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayState())

    /** Re-evaluate "due" badges when the screen comes back to the foreground. */
    fun refresh() {
        now.value = ZonedDateTime.now()
    }

    fun discardDraft(id: Long) = viewModelScope.launch { repository.deleteGoal(id) }
}
