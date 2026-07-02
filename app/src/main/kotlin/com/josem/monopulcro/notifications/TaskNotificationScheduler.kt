package com.josem.monopulcro.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.josem.monopulcro.data.MonkeyStateManager
import com.josem.monopulcro.data.Task
import java.util.Calendar

object TaskNotificationScheduler {

    private const val REQUEST_CODE_BASE = 3000

    fun scheduleAll(context: Context) {
        val tasks = MonkeyStateManager(context).loadTasks()
        tasks.forEach { task ->
            if (task.notificationEnabled) scheduleTask(context, task)
            else cancelTask(context, task.id)
        }
    }

    fun scheduleTask(context: Context, task: Task) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context, task.id)
        alarmManager.cancel(pendingIntent)

        val triggerAt = nextTriggerMillis(task) ?: return
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent,
        )
    }

    fun cancelTask(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildPendingIntent(context, taskId))
    }

    internal fun nextTriggerMillis(task: Task, now: Calendar = Calendar.getInstance()): Long? {
        if (!task.notificationEnabled || task.scheduledDays.isEmpty()) return null

        for (offset in 0..7) {
            val candidate = Calendar.getInstance().apply {
                timeInMillis = now.timeInMillis
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, task.notificationHour)
                set(Calendar.MINUTE, task.notificationMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val isoDay = calendarDayToIso(candidate.get(Calendar.DAY_OF_WEEK))
            if (isoDay in task.scheduledDays && candidate.timeInMillis > now.timeInMillis) {
                return candidate.timeInMillis
            }
        }
        return null
    }

    private fun buildPendingIntent(context: Context, taskId: String): PendingIntent {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_TASK_REMINDER
            putExtra(NotificationReceiver.EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeFor(taskId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun requestCodeFor(taskId: String): Int =
        REQUEST_CODE_BASE + (taskId.hashCode() and 0xFFFF)

    private fun calendarDayToIso(calendarDay: Int): Int = when (calendarDay) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        Calendar.SUNDAY -> 7
        else -> 1
    }
}
