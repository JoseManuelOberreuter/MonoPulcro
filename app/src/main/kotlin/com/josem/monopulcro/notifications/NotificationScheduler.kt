package com.josem.monopulcro.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.josem.monopulcro.data.MonkeyStateManager
import java.util.Calendar

/**
 * Programa los recordatorios de hábito (familias A/B).
 * Idempotente: cancela alarms previos y agenda los próximos relevantes.
 */
object NotificationScheduler {

    /** Request code del alarm diario legacy (20:00); se cancela al migrar. */
    private const val LEGACY_DAILY_REQUEST_CODE = 2001

    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelAll(context, alarmManager)

        val manager = MonkeyStateManager(context)
        manager.checkAndResetForNewDay()

        val hasAnyTasks = manager.loadTasks().isNotEmpty()
        val todayTasks = manager.todayTasks
        val restDay = hasAnyTasks && todayTasks.isEmpty()
        val streak = manager.streakCount
        val missed = manager.missedDaysCount
        val clean = manager.isCleanToday

        when {
            // Familia A: racha activa. Si limpio o día de descanso → solo mañana.
            streak > 0 -> {
                val allowToday = !clean && !restDay
                for (slot in HabitNotificationSlot.entries.filter { it.family == HabitNotificationSlot.Family.A }) {
                    val triggerAt = nextTriggerMillis(slot.hour, slot.minute, allowToday = allowToday)
                    setAlarm(context, alarmManager, slot, triggerAt)
                }
            }
            // Familia B: sin racha y con días perdidos
            streak == 0 && missed >= 1 -> {
                val triggerAt = nextTriggerMillis(
                    HabitNotificationSlot.B.hour,
                    HabitNotificationSlot.B.minute,
                    allowToday = true,
                )
                setAlarm(context, alarmManager, HabitNotificationSlot.B, triggerAt)
            }
        }
    }

    /** Cancela todos los alarms de hábito (legacy + slots A/B). */
    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelAll(context, alarmManager)
    }

    private fun cancelAll(context: Context, alarmManager: AlarmManager) {
        alarmManager.cancel(legacyPendingIntent(context))
        HabitNotificationSlot.entries.forEach { slot ->
            alarmManager.cancel(buildPendingIntent(context, slot))
        }
    }

    private fun setAlarm(
        context: Context,
        alarmManager: AlarmManager,
        slot: HabitNotificationSlot,
        triggerAt: Long,
    ) {
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            buildPendingIntent(context, slot),
        )
    }

    private fun buildPendingIntent(context: Context, slot: HabitNotificationSlot): PendingIntent {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_HABIT_REMINDER
            putExtra(NotificationReceiver.EXTRA_SLOT_ID, slot.id)
        }
        return PendingIntent.getBroadcast(
            context,
            slot.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun legacyPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_DAILY_REMINDER
        }
        return PendingIntent.getBroadcast(
            context,
            LEGACY_DAILY_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * Próximo disparo a [hour]:[minute].
     * Si [allowToday] es false, o esa hora ya pasó, agenda para mañana.
     */
    internal fun nextTriggerMillis(
        hour: Int,
        minute: Int,
        allowToday: Boolean,
        now: Calendar = Calendar.getInstance(),
    ): Long {
        val target = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!allowToday || target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }
}
