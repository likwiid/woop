// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop

import android.app.Application
import com.likwiid.woop.reminders.Notifications
import com.likwiid.woop.widget.FocusWidget
import kotlinx.coroutines.launch

class WoopApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
        // Force-stop and some OEM task killers drop alarms; re-booking on every launch is cheap.
        container.appScope.launch { container.repository.rescheduleAllReminders() }
        // The widget only redraws when told to; any change to goals, plans or check-ins tells it.
        container.appScope.launch { container.repository.goals.collect { FocusWidget.refresh(this@WoopApp) } }
    }
}
