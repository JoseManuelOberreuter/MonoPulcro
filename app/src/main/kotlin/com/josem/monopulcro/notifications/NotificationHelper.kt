package com.josem.monopulcro.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.josem.monopulcro.MainActivity
import com.josem.monopulcro.R
import com.josem.monopulcro.data.MonkeyStateManager

object NotificationHelper {

    private const val CHANNEL_REMINDER    = "mono_reminder_channel"
    private const val CHANNEL_CELEBRATION = "mono_celebration_channel"

    private const val NOTIF_ID_REMINDER    = 1001
    private const val NOTIF_ID_CELEBRATION = 1002

    // ─── Inicialización de canales (llamar una sola vez al inicio) ────────────

    fun createChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        NotificationChannel(
            CHANNEL_REMINDER,
            "Recordatorios diarios",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Recuerda completar tus tareas de limpieza"
            manager.createNotificationChannel(this)
        }

        NotificationChannel(
            CHANNEL_CELEBRATION,
            "Celebraciones",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Confirmacion de tareas completadas"
            manager.createNotificationChannel(this)
        }
    }

    // ─── Recordatorio diario (llamado por el alarm a las 8 PM) ────────────────

    fun showReminderNotification(context: Context) {
        val manager = MonkeyStateManager(context)

        val allTasks   = manager.loadTasks()
        val todayTasks = manager.todayTasks
        val isClean    = manager.isCleanToday
        val missedDays = manager.missedDaysCount

        // Si ya completó todo hoy o es día de descanso (tiene tareas otros días), no notificar
        if (isClean && allTasks.isNotEmpty() && todayTasks.isEmpty()) return
        if (isClean) return

        val (title, body) = when {
            allTasks.isEmpty() -> Pair(
                "El mono te necesita \uD83D\uDC12",
                "Agrega tus primeras tareas para empezar tu racha"
            )
            missedDays >= 2 -> Pair(
                "\u26A0\uFE0F Tu racha esta en peligro",
                "Llevas $missedDays dias sin limpiar. El mono esta muy triste"
            )
            todayTasks.isNotEmpty() -> {
                val pending = todayTasks.count { !manager.isTaskCompleted(it.id) }
                Pair(
                    "El mono te espera \uD83D\uDC12",
                    "Te quedan $pending tarea${if (pending != 1) "s" else ""} por completar hoy"
                )
            }
            else -> return
        }

        postNotification(context, NOTIF_ID_REMINDER, CHANNEL_REMINDER, title, body, highPriority = true)
    }

    // ─── Celebración 30 min después de completar todas las tareas ────────────

    /**
     * Programa la notificación de celebración para 30 minutos más tarde.
     * Usa AlarmManager para que funcione aunque el usuario cierre la app.
     */
    fun showCelebrationNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = android.content.Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_CELEBRATION
        }
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            NOTIF_ID_CELEBRATION,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + 30 * 60 * 1000L
        alarmManager.setExactAndAllowWhileIdle(
            android.app.AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent
        )
    }

    /** Muestra la notificación de celebración inmediatamente (llamado por el receiver). */
    internal fun postCelebrationNotification(context: Context) {
        val manager = MonkeyStateManager(context)
        val streak  = manager.streakCount

        val title = "\uD83C\uDF89 Todas las tareas completadas"
        val body  = if (streak > 1)
            "Racha de $streak dias. El mono esta feliz!"
        else
            "Buen trabajo! El mono esta limpio hoy"

        postNotification(context, NOTIF_ID_CELEBRATION, CHANNEL_CELEBRATION, title, body, highPriority = false)
    }

    // ─── Helper interno ───────────────────────────────────────────────────────

    private fun postNotification(
        context: Context,
        notifId: Int,
        channelId: String,
        title: String,
        body: String,
        highPriority: Boolean
    ) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notifId, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.mono_pulcro)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(
                if (highPriority) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_LOW
            )
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notifId, notification)
    }
}
