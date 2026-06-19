package com.josem.monopulcro.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object NotificationScheduler {

    private const val ALARM_REQUEST_CODE = 2001

    /**
     * Programa (o reprograma) el alarm diario a las 20:00.
     * Si las 8 PM de hoy ya pasaron, agenda para las 8 PM de mañana.
     * Es idempotente: cancela cualquier alarm previo antes de crear uno nuevo.
     */
    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context)

        // Cancelar alarm existente antes de reagendar
        alarmManager.cancel(pendingIntent)

        val triggerAt = nextTriggerMillis()
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent
        )
    }

    /** Cancela el alarm sin reagendar (por si el usuario desactiva notificaciones). */
    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildPendingIntent(context))
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_DAILY_REMINDER
        }
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextTriggerMillis(): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Si las 8 PM de hoy ya pasó, disparar mañana
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }
}
