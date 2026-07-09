package com.smithware.tidypilot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smithware.tidypilot.data.AppSettingsEntity
import com.smithware.tidypilot.data.ReminderPlanner
import com.smithware.tidypilot.data.TidyPilotDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val dao = TidyPilotDatabase.get(context).dao()
                val settings = dao.settingsOnce() ?: AppSettingsEntity()
                if (!settings.reminderEnabled) {
                    TidyReminderManager.cancelSchedule(context)
                    return@launch
                }
                ReminderPlanner
                    .plan(settings, dao.activeTasksOnce(), dao.activeRoomsOnce(), dao.completionsOnce())
                    .forEach { TidyReminderManager.showReminder(context, it) }
                TidyReminderManager.scheduleNext(context, settings)
            } finally {
                pending.finish()
            }
        }
    }
}
