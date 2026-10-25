// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.likwiid.woop.WoopApp
import kotlinx.coroutines.launch

/** Alarms don't survive reboots, clock changes or app updates, so rebuild them all. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as WoopApp).container
        val pending = goAsync()
        container.appScope.launch {
            try {
                container.repository.rescheduleAllReminders()
            } finally {
                pending.finish()
            }
        }
    }
}
