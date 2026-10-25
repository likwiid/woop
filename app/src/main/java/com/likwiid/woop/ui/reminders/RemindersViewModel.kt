// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.likwiid.woop.data.GoalStatus
import com.likwiid.woop.data.PromptEntity
import com.likwiid.woop.data.WoopRepository
import com.likwiid.woop.reminders.ReminderScheduler
import com.likwiid.woop.reminders.Schedule
import com.likwiid.woop.ui.today.GoalCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.ZonedDateTime

data class RemindersState(
    val loading: Boolean = true,
    val prompts: List<PromptEntity> = emptyList(),
    val checkIns: List<GoalCard> = emptyList(),
    val preciseTiming: Boolean = true,
)

/** One-tap starting points; anchoring a prompt to an existing routine makes it easier to act on. */
data class PromptPreset(val label: String, val hour: Int, val minute: Int, val daysMask: Int)

val promptPresets = listOf(
    PromptPreset("Plan my day", 8, 0, Schedule.ALL_DAYS),
    PromptPreset("Plan my week", 19, 0, Schedule.dayBit(DayOfWeek.SUNDAY)),
    PromptPreset("Workday WOOP", 7, 30, Schedule.WEEKDAYS),
)

class RemindersViewModel(
    private val repository: WoopRepository,
    private val scheduler: ReminderScheduler,
) : ViewModel() {
    private val precise = MutableStateFlow(scheduler.canScheduleExact())

    val state: StateFlow<RemindersState> = combine(repository.prompts, repository.goals, precise) { prompts, goals, exact ->
        val now = ZonedDateTime.now()
        RemindersState(
            loading = false,
            prompts = prompts,
            checkIns = goals
                .filter { it.goal.status == GoalStatus.ACTIVE }
                .map { GoalCard(it, scheduler.isCheckInDue(it.goal, now), scheduler.nextCheckIn(it.goal, now)) },
            preciseTiming = exact,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RemindersState())

    /** The exact-alarm grant can change in system settings while we're in the background. */
    fun refresh() {
        val exact = scheduler.canScheduleExact()
        if (exact != precise.value) {
            precise.value = exact
            viewModelScope.launch { repository.rescheduleAllReminders() }
        }
    }

    fun save(prompt: PromptEntity) = viewModelScope.launch { repository.savePrompt(prompt) }

    fun addPreset(preset: PromptPreset) = save(
        PromptEntity(label = preset.label, hour = preset.hour, minute = preset.minute, daysMask = preset.daysMask),
    )

    fun setEnabled(prompt: PromptEntity, enabled: Boolean) = save(prompt.copy(enabled = enabled))

    fun delete(id: Long) = viewModelScope.launch { repository.deletePrompt(id) }
}
