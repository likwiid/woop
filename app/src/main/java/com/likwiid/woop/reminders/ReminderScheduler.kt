// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.likwiid.woop.data.GoalEntity
import com.likwiid.woop.data.GoalStatus
import com.likwiid.woop.data.PromptEntity
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun schedulePrompt(prompt: PromptEntity, from: ZonedDateTime = ZonedDateTime.now()) {
        val next = if (prompt.enabled) Schedule.nextPrompt(from, prompt.hour, prompt.minute, prompt.daysMask) else null
        val pi = pendingIntent(KIND_PROMPT, prompt.id)
        if (next == null) alarmManager.cancel(pi) else setAlarm(next, pi)
    }

    fun scheduleCheckIn(goal: GoalEntity, from: ZonedDateTime = ZonedDateTime.now()) {
        val next = if (goal.status == GoalStatus.ACTIVE) {
            Schedule.nextCheckIn(from, checkInBase(goal), goal.checkInEveryDays, goal.checkInHour, goal.checkInMinute)
        } else {
            null
        }
        val pi = pendingIntent(KIND_CHECK_IN, goal.id)
        if (next == null) alarmManager.cancel(pi) else setAlarm(next, pi)
    }

    fun nextCheckIn(goal: GoalEntity, from: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime? =
        if (goal.status != GoalStatus.ACTIVE) null
        else Schedule.nextCheckIn(from, checkInBase(goal), goal.checkInEveryDays, goal.checkInHour, goal.checkInMinute)

    fun isCheckInDue(goal: GoalEntity, now: ZonedDateTime = ZonedDateTime.now()): Boolean =
        goal.status == GoalStatus.ACTIVE &&
            Schedule.isCheckInDue(now, checkInBase(goal), goal.checkInEveryDays, goal.checkInHour, goal.checkInMinute)

    fun cancelPrompt(id: Long) = alarmManager.cancel(pendingIntent(KIND_PROMPT, id))

    fun cancelCheckIn(goalId: Long) = alarmManager.cancel(pendingIntent(KIND_CHECK_IN, goalId))

    private fun setAlarm(at: ZonedDateTime, pi: PendingIntent) {
        val millis = at.toInstant().toEpochMilli()
        if (canScheduleExact()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
                return
            } catch (_: SecurityException) {
                // Permission was revoked between the check and the call; fall through to inexact.
            }
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
    }

    private fun pendingIntent(kind: String, id: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setData(Uri.parse("woop://reminder/$kind/$id"))
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    companion object {
        const val KIND_PROMPT = "prompt"
        const val KIND_CHECK_IN = "checkin"

        fun checkInBase(goal: GoalEntity): ZonedDateTime =
            Instant.ofEpochMilli(goal.lastReviewedAt ?: goal.createdAt).atZone(ZoneId.systemDefault())
    }
}
