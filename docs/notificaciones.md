# Notificaciones — Mono Pulcro

Resumen
-------
Mono Pulcro usa **solo notificaciones locales** en Android. No hay push remoto
(FCM, OneSignal, etc.): todo se programa en el dispositivo con `AlarmManager`
y se muestra vía `NotificationCompat`.

Al tocar una notificación se abre `MainActivity`. No hay deep linking ni
navegación a una tarea concreta.


1. TIPOS DE NOTIFICACIÓN
------------------------
  Recordatorio diario     — Cada día ~20:00. Canal HIGH.
  Recordatorio por tarea  — Hora elegida por el usuario en días programados.
  Celebración             — 30 min después de completar todas las tareas del día.
                            Canal LOW.


2. ARQUITECTURA
---------------
  AlarmManager (setAndAllowWhileIdle, RTC_WAKEUP)
       │
       ▼
  NotificationReceiver (BroadcastReceiver)
       │
       ▼
  NotificationHelper → NotificationManager + NotificationCompat

Archivos:

  notifications/NotificationHelper.kt
      Canales, textos y posteo de notificaciones.
  notifications/NotificationScheduler.kt
      Alarm diario a las 20:00.
  notifications/TaskNotificationScheduler.kt
      Alarms por tarea (hora + días).
  notifications/NotificationReceiver.kt
      Recibe alarms y BOOT_COMPLETED.

Integración:

  MainActivity          — Crea canales, pide permiso, programa alarms al inicio.
  MonkeyViewModel       — Reprograma al CRUD de tareas; dispara celebración.
  TaskEditScreen        — Switch + TimePicker para configurar por tarea.
  data/Task.kt          — notificationEnabled, notificationHour, notificationMinute.


3. PERMISOS
-----------
Manifest:

  POST_NOTIFICATIONS      — Android 13+ (API 33).
  RECEIVE_BOOT_COMPLETED  — Reprogramar alarms tras reinicio.

Runtime (MainActivity.setupNotifications):

  API < 33   → programa alarms directo.
  API 33+    → si no hay permiso, pide POST_NOTIFICATIONS.
               Solo programa si el usuario concede.

Si falla crear canales o programar alarms, se captura la excepción y no se
crashea (dispositivos restrictivos / OEMs).


4. CANALES
----------
Creados en NotificationHelper.createChannels (una vez al arranque):

  mono_reminder_channel     "Recordatorios diarios"      IMPORTANCE_HIGH
  mono_task_channel         "Recordatorios de tareas"    IMPORTANCE_HIGH
  mono_celebration_channel  "Celebraciones"              IMPORTANCE_LOW


5. FLUJO GENERAL
----------------
  App start (MainActivity)
    → createChannels()
    → pedir POST_NOTIFICATIONS si hace falta
    → NotificationScheduler.schedule()           // diario 20:00
    → TaskNotificationScheduler.scheduleAll()    // tareas con notif ON

  Alarm dispara → NotificationReceiver.onReceive(action)
    → Helper muestra (o no, según condiciones)
    → Reprograma el siguiente alarm (diario / tarea)

  Completar última tarea del día (MonkeyViewModel.toggleTask, earned == true)
    → NotificationHelper.showCelebrationNotification()
    → Alarm a now + 30 min → postCelebrationNotification()

  BOOT_COMPLETED
    → Reprograma diario + todas las tareas
    → También reprograma el widget si hay widgets instalados


6. RECORDATORIO DIARIO (20:00)
------------------------------
NotificationScheduler:

  Cancela el alarm previo y agenda el próximo 20:00.
  Si ya pasó hoy → mañana 20:00.
  Usa setAndAllowWhileIdle (inexacto, ~±15 min; no pide SCHEDULE_EXACT_ALARM).

Al disparar (ACTION_DAILY_REMINDER):

  showReminderNotification() y luego reprograma el siguiente día.

Regla: se envía SOLO si el mono del estado principal NO está limpio
(isCleanToday == false). Limpio = todas las tareas de hoy completadas,
o día de descanso (sin tareas programadas hoy).

Si isCleanToday → no se muestra la notificación.

Textos según estado (solo cuando NO está limpio):

  Sin tareas creadas     → "El mono te necesita" / agrega primeras tareas.
  missedDays >= 2        → "Tu racha esta en peligro".
  Hay tareas pendientes  → "El mono te espera" / quedan N tareas.


7. RECORDATORIO POR TAREA
-------------------------
Datos en Task:

  notificationEnabled   default false
  notificationHour      default 9
  notificationMinute    default 0
  scheduledDays         1=Lun … 7=Dom

UI (TaskEditScreen.TaskNotificationSection):

  Switch para activar + TimePicker 24h.

TaskNotificationScheduler:

  scheduleAll   — Recorre tareas: ON → scheduleTask; OFF → cancelTask.
  scheduleTask  — Busca la próxima ocurrencia en los próximos 7 días que
                  coincida con scheduledDays + hora/minuto.
  cancelTask    — Al borrar o desactivar.

Al disparar (ACTION_TASK_REMINDER):

  showTaskReminderNotification(taskId)
  Luego reprograma esa tarea.

Cuándo NO se muestra:

  Tarea inexistente.
  notificationEnabled == false.
  Tarea no programada para hoy.
  Tarea ya completada.

Texto: "Es hora de: {task.name}".


8. CELEBRACIÓN
--------------
Disparo: al completar la última tarea del día (earned == true en toggleTask).

  showCelebrationNotification → AlarmManager +30 min
  Receiver ACTION_CELEBRATION → postCelebrationNotification

Texto:

  streak > 1  → "Racha de N dias. El mono esta feliz!"
  si no       → "Buen trabajo! El mono esta limpio hoy"

Prioridad baja (no interrumpe).


9. IDs Y ACTIONS
----------------
Notification IDs:

  Diario       1001
  Celebración  1002
  Por tarea    5000 + (taskId.hashCode() and 0x7FFF)

Alarm request codes:

  Diario       2001
  Por tarea    3000 + (taskId.hashCode() and 0xFFFF)
  Celebración  1002 (mismo id que la notif)

Actions del receiver:

  com.josem.monopulcro.DAILY_REMINDER
  com.josem.monopulcro.TASK_REMINDER   (+ extra task_id)
  com.josem.monopulcro.CELEBRATION
  android.intent.action.BOOT_COMPLETED


10. TAP EN LA NOTIFICACIÓN
--------------------------
PendingIntent → MainActivity con:

  FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK

Sin extras, sin URI, sin ruta a una pantalla o tarea.


11. DETALLES IMPORTANTES
------------------------
  Local only         — Sin tokens ni servidor.
  Inexact alarms     — setAndAllowWhileIdle; margen ~±15 min a propósito.
  Boot reschedule    — Mitiga OEMs que matan alarms al reiniciar.
  Idempotencia       — schedule cancela el PendingIntent previo antes de crear.
  Auto-cancel        — La notificación se cierra al tocarla.
  Icono              — R.drawable.cara_mono (smallIcon de las 3 notifs).
  Sin tests          — No hay tests unitarios de notificaciones a día de hoy.

Relacionado:

  docs/racha_y_bananas.md       — Completar el día y racha.
  docs/estado_mono_principal.md — isCleanToday / missedDays usados en textos.
