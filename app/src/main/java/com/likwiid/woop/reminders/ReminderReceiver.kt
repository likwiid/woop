// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.likwiid.woop.WoopApp
import com.likwiid.woop.data.GoalStatus
import com.likwiid.woop.widget.FocusWidget
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/** Fires for one reminder, shows its notification, then books the next occurrence. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val segments = intent.data?.pathSegments ?: return
        val kind = segments.getOrNull(0) ?: return
        val id = segments.getOrNull(1)?.toLongOrNull() ?: return
        val container = (context.applicationContext as WoopApp).container
        val pending = goAsync()
        container.appScope.launch {
            try {
                // A minute of slack so a slightly early wake-up never re-books the same slot.
                val from = ZonedDateTime.now().plusMinutes(1)
                when (kind) {
                    ReminderScheduler.KIND_PROMPT -> {
                        val prompt = container.repository.getPrompt(id) ?: return@launch
                        if (prompt.enabled) Notifications.showPrompt(context, prompt)
                        container.scheduler.schedulePrompt(prompt, from)
                    }
                    ReminderScheduler.KIND_CHECK_IN -> {
                        val details = container.repository.getGoal(id) ?: return@launch
                        val goal = details.goal
                        if (goal.status == GoalStatus.ACTIVE && goal.checkInEveryDays > 0) {
                            Notifications.showCheckIn(context, details)
                        }
                        container.scheduler.scheduleCheckIn(goal, from)
                        // Nothing in the database changed, but the widget should now show the check-in.
                        FocusWidget.refresh(context)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
