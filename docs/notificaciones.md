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
  Hábito familia A     — Con racha y tareas pendientes: 09:00, 12:00, 18:00, 21:00, 23:00.
  Hábito familia B     — Sin racha y missedDays >= 1: ~10:30 según días perdidos.
  Recordatorio por tarea — Hora elegida por el usuario en días programados.
  Celebración            — 30 min después de completar todas las tareas del día.
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

  notifications/HabitNotificationSlot.kt
      Enum de slots A1–A5 y B (hora + requestCode).
  notifications/NotificationHelper.kt
      Canales, textos y posteo de notificaciones.
  notifications/NotificationScheduler.kt
      Agenda / cancela slots de hábito según racha y estado.
  notifications/TaskNotificationScheduler.kt
      Alarms por tarea (hora + días).
  notifications/NotificationReceiver.kt
      Recibe alarms y BOOT_COMPLETED.

Integración:

  MainActivity          — Crea canales, pide permiso, programa alarms al inicio.
  MonkeyViewModel       — Reprograma al CRUD / al completar el día; dispara celebración.
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

  mono_reminder_channel     "Recordatorios del mono"     IMPORTANCE_HIGH
  mono_task_channel         "Recordatorios de tareas"    IMPORTANCE_HIGH
  mono_celebration_channel  "Celebraciones"              IMPORTANCE_LOW


5. FLUJO GENERAL
----------------
  App start (MainActivity / ViewModel)
    → createChannels()
    → pedir POST_NOTIFICATIONS si hace falta
    → NotificationScheduler.schedule()           // slots A/B según estado
    → TaskNotificationScheduler.scheduleAll()    // tareas con notif ON

  Alarm hábito → NotificationReceiver (ACTION_HABIT_REMINDER + slot_id)
    → checkAndResetForNewDay()
    → Helper muestra solo si aplica el slot
    → Reprograma (schedule)

  Completar última tarea del día (MonkeyViewModel.toggleTask, earned == true)
    → NotificationScheduler.schedule()  // cancela A de hoy (modo limpio → mañana)
    → NotificationHelper.showCelebrationNotification()
    → Alarm a now + 30 min → postCelebrationNotification()

  BOOT_COMPLETED
    → Reprograma hábito + todas las tareas
    → También reprograma el widget si hay widgets instalados


6. HÁBITO FAMILIA A (con racha)
------------------------------
Se agenda si `streakCount > 0`.

  Si hoy hay tareas pendientes → slots de hoy que aún no pasaron.
  Si ya está limpio o es día de descanso → todos los A pasan a mañana.

Al disparar, se muestra SOLO si:

  streak > 0
  !isCleanToday
  hay tareas programadas hoy

Textos:

  09:00  Mono Pulcro te espera
         Ya despertó. Hoy otra vez, ¿lo ayudas?
  12:00  Mono Pulcro te busca
         Medio día y todavía no lo has mirado.
  18:00  Mono Pulcro se ensucia
         Ya casi anochece y tus tareas siguen ahí.
  21:00  Mono Pulcro sigue esperando
         Son las 9 y sigue sucio. Dale un ratito.
  23:00  Mono Pulcro: ¿olvidaste marcar?
         Si ya lo limpiaste, márcalo. Si no… aún puedes.


7. HÁBITO FAMILIA B (sin racha)
------------------------------
Se agenda si `streakCount == 0` y `missedDaysCount >= 1`, a las **10:30**.

Textos según missedDays:

  1   Mono Pulcro te echa de menos
      Ayer no viniste y ya se nota. Ayúdalo un poco.
  2   Mono Pulcro ya no brilla
      Dos días sin ti. Vuelve un minuto.
  3   Mono Pulcro se siente solo
      Tres días… Una tarea tuya le cambia el día.
  4   Mono Pulcro necesita ayuda
      El polvo le está ganando. Ábrele la app.
  5   Mono Pulcro casi no espera
      Cinco días. Si vuelves, se ilusiona.
  6   No dejes a Mono Pulcro así
      Seis días. Sigue siendo tuyo. Ve a verlo.
  7+  Mono Pulcro no te olvidó
      Lleva días sucio, pero sigue siendo tuyo.

A y B no se mezclan: con racha solo A; sin racha y con missed solo B.


8. RECORDATORIO POR TAREA
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


9. CELEBRACIÓN
--------------
Disparo: al completar la última tarea del día (earned == true en toggleTask).

  showCelebrationNotification → AlarmManager +30 min
  Receiver ACTION_CELEBRATION → postCelebrationNotification

Texto:

  streak > 1  → "Racha de N dias. El mono esta feliz!"
  si no       → "Buen trabajo! El mono esta limpio hoy"

Prioridad baja (no interrumpe).


10. IDs Y ACTIONS
-----------------
Notification IDs:

  Hábito       1001  (reutiliza el id del viejo recordatorio diario)
  Celebración  1002
  Por tarea    5000 + (taskId.hashCode() and 0x7FFF)

Alarm request codes:

  Legacy diario  2001 (se cancela al migrar)
  A1–A5          2101–2105
  B              2110
  Por tarea      3000 + (taskId.hashCode() and 0xFFFF)
  Celebración    1002 (mismo id que la notif)

Actions del receiver:

  com.josem.monopulcro.HABIT_REMINDER  (+ extra slot_id)
  com.josem.monopulcro.DAILY_REMINDER  (legacy → solo reprograma)
  com.josem.monopulcro.TASK_REMINDER   (+ extra task_id)
  com.josem.monopulcro.CELEBRATION
  android.intent.action.BOOT_COMPLETED


11. TAP EN LA NOTIFICACIÓN
--------------------------
PendingIntent → MainActivity con:

  FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK

Sin extras, sin URI, sin ruta a una pantalla o tarea.


12. DETALLES IMPORTANTES
------------------------
  Local only         — Sin tokens ni servidor.
  Inexact alarms     — setAndAllowWhileIdle; margen ~±15 min a propósito.
  Boot reschedule    — Mitiga OEMs que matan alarms al reiniciar.
  Idempotencia       — schedule cancela PendingIntents previos antes de crear.
  Auto-cancel        — La notificación se cierra al tocarla.
  Icono              — R.drawable.cara_mono (smallIcon).
  Reset en alarm     — showHabitNotification llama checkAndResetForNewDay().
  Sin tests          — No hay tests unitarios de notificaciones a día de hoy.

Relacionado:

- [`racha_y_bananas.md`](racha_y_bananas.md) — Completar el día y racha
- [`estado_mono_principal.md`](estado_mono_principal.md) — `isCleanToday` / `missedDays`
- [`tareas_y_calendario.md`](tareas_y_calendario.md) — modelo Task y UI
- [`INDEX.md`](INDEX.md)
