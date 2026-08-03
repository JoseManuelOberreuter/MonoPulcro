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
    private const val CHANNEL_TASK        = "mono_task_channel"

    private const val NOTIF_ID_REMINDER    = 1001
    private const val NOTIF_ID_CELEBRATION = 1002
    private const val NOTIF_ID_TASK_BASE   = 5000

    // ─── Inicialización de canales (llamar una sola vez al inicio) ────────────

    fun createChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        NotificationChannel(
            CHANNEL_REMINDER,
            "Recordatorios del mono",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos para cuidar a Mono Pulcro durante el día"
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

        NotificationChannel(
            CHANNEL_TASK,
            "Recordatorios de tareas",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos personalizados por tarea"
            manager.createNotificationChannel(this)
        }
    }

    // ─── Recordatorio de hábito (slots A/B) ───────────────────────────────────

    fun showHabitNotification(context: Context, slot: HabitNotificationSlot) {
        val manager = MonkeyStateManager(context)
        manager.checkAndResetForNewDay()

        val hasAnyTasks = manager.loadTasks().isNotEmpty()
        val todayTasks = manager.todayTasks
        val restDay = hasAnyTasks && todayTasks.isEmpty()
        if (restDay) return

        val streak = manager.streakCount
        val missed = manager.missedDaysCount
        val clean = manager.isCleanToday

        val copy = when (slot.family) {
            HabitNotificationSlot.Family.A -> {
                if (streak <= 0) return
                if (clean) return
                if (todayTasks.isEmpty()) return
                habitCopyA(slot)
            }
            HabitNotificationSlot.Family.B -> {
                if (streak > 0) return
                if (missed < 1) return
                habitCopyB(missed)
            }
        } ?: return

        postNotification(
            context,
            NOTIF_ID_REMINDER,
            CHANNEL_REMINDER,
            copy.first,
            copy.second,
            highPriority = true,
        )
    }

    private fun habitCopyA(slot: HabitNotificationSlot): Pair<String, String>? = when (slot) {
        HabitNotificationSlot.A1 -> "Mono Pulcro te espera" to
            "Ya despertó. Hoy otra vez, ¿lo ayudas?"
        HabitNotificationSlot.A2 -> "Mono Pulcro te busca" to
            "Medio día y todavía no lo has mirado."
        HabitNotificationSlot.A3 -> "Mono Pulcro se ensucia" to
            "Ya casi anochece y tus tareas siguen ahí."
        HabitNotificationSlot.A4 -> "Mono Pulcro sigue esperando" to
            "Son las 9 y sigue sucio. Dale un ratito."
        HabitNotificationSlot.A5 -> "Mono Pulcro: ¿olvidaste marcar?" to
            "Si ya lo limpiaste, márcalo. Si no… aún puedes."
        HabitNotificationSlot.B -> null
    }

    private fun habitCopyB(missedDays: Int): Pair<String, String> = when {
        missedDays <= 1 -> "Mono Pulcro te echa de menos" to
            "Ayer no viniste y ya se nota. Ayúdalo un poco."
        missedDays == 2 -> "Mono Pulcro ya no brilla" to
            "Dos días sin ti. Vuelve un minuto."
        missedDays == 3 -> "Mono Pulcro se siente solo" to
            "Tres días… Una tarea tuya le cambia el día."
        missedDays == 4 -> "Mono Pulcro necesita ayuda" to
            "El polvo le está ganando. Ábrele la app."
        missedDays == 5 -> "Mono Pulcro casi no espera" to
            "Cinco días. Si vuelves, se ilusiona."
        missedDays == 6 -> "No dejes a Mono Pulcro así" to
            "Seis días. Sigue siendo tuyo. Ve a verlo."
        else -> "Mono Pulcro no te olvidó" to
            "Lleva días sucio, pero sigue siendo tuyo."
    }

    // ─── Recordatorio por tarea (hora personalizada) ─────────────────────────

    fun showTaskReminderNotification(context: Context, taskId: String) {
        val manager = MonkeyStateManager(context)
        val task = manager.loadTasks().find { it.id == taskId } ?: return
        if (!task.notificationEnabled) return
        if (manager.todayTasks.none { it.id == taskId }) return
        if (manager.isTaskCompleted(taskId)) return

        val title = "Mono Pulcro"
        val body = "Es hora de: ${task.name}"
        postNotification(
            context,
            notifIdForTask(taskId),
            CHANNEL_TASK,
            title,
            body,
            highPriority = true,
        )
    }

    private fun notifIdForTask(taskId: String): Int =
        NOTIF_ID_TASK_BASE + (taskId.hashCode() and 0x7FFF)

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
        alarmManager.setAndAllowWhileIdle(
            android.app.AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent
        )
    }

    /** Muestra la notificación de celebración inmediatamente (llamado por el receiver). */
    internal fun postCelebrationNotification(context: Context) {
        val manager = MonkeyStateManager(context)
        val streak  = manager.streakCount

        val title = "Todas las tareas completadas"
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
            .setSmallIcon(R.drawable.cara_mono)
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
