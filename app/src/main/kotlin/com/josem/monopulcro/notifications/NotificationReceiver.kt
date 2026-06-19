package com.josem.monopulcro.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                NotificationScheduler.schedule(context)
            }
            ACTION_DAILY_REMINDER -> {
                NotificationHelper.showReminderNotification(context)
                NotificationScheduler.schedule(context)
            }
            ACTION_CELEBRATION -> {
                NotificationHelper.postCelebrationNotification(context)
            }
        }
    }

    companion object {
        const val ACTION_DAILY_REMINDER = "com.josem.monopulcro.DAILY_REMINDER"
        const val ACTION_CELEBRATION    = "com.josem.monopulcro.CELEBRATION"
    }
}
