package com.josem.monopulcro.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object WidgetUpdateScheduler {

    private const val ALARM_REQUEST_CODE = 3001
    private const val HOUR_MS = 3_600_000L

    /**
     * Programa (o reprograma) un alarm en ~1 hora para refrescar el widget.
     * Idempotente: cancela cualquier alarm previo antes de crear uno nuevo.
     */
    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context)
        alarmManager.cancel(pendingIntent)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + HOUR_MS,
            pendingIntent
        )
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildPendingIntent(context))
    }

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, WidgetUpdateReceiver::class.java).apply {
            action = WidgetUpdateReceiver.ACTION_WIDGET_HOURLY_UPDATE
        }
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
