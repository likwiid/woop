// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop

import android.content.Context
import com.likwiid.woop.data.Prefs
import com.likwiid.woop.data.WoopDatabase
import com.likwiid.woop.data.WoopRepository
import com.likwiid.woop.reminders.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Hand-rolled dependency container; small enough that a DI framework would only add build time. */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val prefs = Prefs(context)
    val scheduler = ReminderScheduler(context)
    val repository = WoopRepository(WoopDatabase.build(context), scheduler)
}
