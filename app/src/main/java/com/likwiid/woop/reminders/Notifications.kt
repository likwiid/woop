// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop.reminders

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.likwiid.woop.MainActivity
import com.likwiid.woop.R
import com.likwiid.woop.data.GoalWithDetails
import com.likwiid.woop.data.PromptEntity

object Notifications {
    private const val CHANNEL_PROMPTS = "prompts"
    private const val CHANNEL_CHECK_INS = "checkins"
    private const val CHECK_IN_ID_OFFSET = 1_000_000

    const val DEST_CREATE = "create"
    fun destReview(goalId: Long) = "review/$goalId"
    fun destGoal(goalId: Long) = "goal/$goalId"

    fun createChannels(context: Context) {
        NotificationManagerCompat.from(context).createNotificationChannelsCompat(
            listOf(
                NotificationChannelCompat.Builder(CHANNEL_PROMPTS, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    .setName("Reminders to WOOP")
                    .setDescription("Nudges to write a new WOOP")
                    .build(),
                NotificationChannelCompat.Builder(CHANNEL_CHECK_INS, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    .setName("Check-ins")
                    .setDescription("Reminders to re-read and review a WOOP")
                    .build(),
            ),
        )
    }

    fun canNotify(context: Context): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun showPrompt(context: Context, prompt: PromptEntity) {
        post(
            context = context,
            id = prompt.id.toInt(),
            channel = CHANNEL_PROMPTS,
            title = prompt.label.ifBlank { "Time to WOOP" },
            text = "Five minutes to turn a wish into a plan.",
            action = "Start a WOOP",
            dest = DEST_CREATE,
        )
    }

    fun showCheckIn(context: Context, details: GoalWithDetails) {
        val plan = details.firstPlan
        // Rehearsing the if-then in the notification itself keeps the cue fresh.
        val text = if (plan != null) {
            "If ${plan.ifCue.trim().trimEnd('.')}, then I will ${plan.thenAction.trim().trimEnd('.')}."
        } else {
            "How is it going? A check-in takes about a minute."
        }
        post(
            context = context,
            id = CHECK_IN_ID_OFFSET + details.goal.id.toInt(),
            channel = CHANNEL_CHECK_INS,
            title = "Check in: ${details.goal.wish}",
            text = text,
            action = "Check in",
            dest = destReview(details.goal.id),
        )
    }

    private fun post(
        context: Context,
        id: Int,
        channel: String,
        title: String,
        text: String,
        action: String,
        dest: String,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val open = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_DEST, dest)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pi = PendingIntent.getActivity(
            context,
            id,
            open,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_woop)
            .setColor(ContextCompat.getColor(context, R.color.woop_wish))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .addAction(0, action, pi)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Notifications were turned off; nothing else to do.
        }
    }
}
