package com.josem.monopulcro.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.josem.monopulcro.data.MonkeyStateManager

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                NotificationScheduler.schedule(context)
                TaskNotificationScheduler.scheduleAll(context)
            }
            ACTION_DAILY_REMINDER -> {
                NotificationHelper.showReminderNotification(context)
                NotificationScheduler.schedule(context)
            }
            ACTION_TASK_REMINDER -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
                NotificationHelper.showTaskReminderNotification(context, taskId)
                val task = MonkeyStateManager(context).loadTasks().find { it.id == taskId }
                if (task != null) {
                    TaskNotificationScheduler.scheduleTask(context, task)
                }
            }
            ACTION_CELEBRATION -> {
                NotificationHelper.postCelebrationNotification(context)
            }
        }
    }

    companion object {
        const val ACTION_DAILY_REMINDER = "com.josem.monopulcro.DAILY_REMINDER"
        const val ACTION_TASK_REMINDER  = "com.josem.monopulcro.TASK_REMINDER"
        const val ACTION_CELEBRATION    = "com.josem.monopulcro.CELEBRATION"
        const val EXTRA_TASK_ID         = "task_id"
    }
}
