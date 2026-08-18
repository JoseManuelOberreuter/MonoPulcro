# Tareas y calendario — Mono Pulcro

## Resumen

Las tareas son hábitos de limpieza configurables por días de la semana.
El usuario las marca cada día; el completado diario se reinicia en el reset.
Hay dos vistas en Main: **hoy** y **semana**.

---

## 1. Modelo `Task`

```kotlin
data class Task(
    val id: String = UUID…,
    val name: String,
    val scheduledDays: List<Int>,   // ISO: 1=Lunes … 7=Domingo
    val notificationEnabled: Boolean = false,
    val notificationHour: Int = 9,
    val notificationMinute: Int = 0,
)
```

Persistido en `tasksJson`. Completado del día: clave booleana `done_<id>`
(se borra en `checkAndResetForNewDay`).

---

## 2. Tareas de hoy

`MonkeyStateManager.todayTasks` filtra por `currentGameDate().dayOfWeek`
(incluye `debugDayOffset` en builds debug).

Reglas de limpio/sucio: [`estado_mono_principal.md`](estado_mono_principal.md).

---

## 3. CRUD

| Acción | Manager | Side-effects ViewModel |
|---|---|---|
| Crear | `addTask` | Reprograma notifs por tarea |
| Editar | `updateTask` | Reprograma notifs |
| Borrar | `deleteTask` (+ quita `done_`) | Cancela notif de esa tarea |
| Toggle | `toggleTask` | Widget, sonido, celebración, notif |

**Nota:** agregar/editar/borrar **no** actualizan el widget al instante
(ver [`estado_widget.md`](estado_widget.md)).

---

## 4. UI

- `TaskEditScreen`: nombre (selector de predefinidas + custom), day picker,
  sección de notificación (switch + TimePicker 24h).
- `MainScreen`: lista animada de hoy o calendario semanal.
- `TasksViewMode` persistido (`today` / `week`).

Vista semana: filas por día con progreso; sheet con detalle al tocar un día.

---

## 5. Nombres predefinidos

`data/PredefinedTasks.kt` — catálogo de ~20 nombres (platos, basura, cama,
baño, aspirar, etc.). No son plantillas de rutina empaquetadas; el usuario
elige uno al crear la tarea.

`PredefinedTasks.quickSuggestions` (3 nombres) se usa solo en
`TaskEditScreen` cuando es la primera tarea del usuario (`allTasks.isEmpty()`
+ `isNew`): precarga el nombre y el día de hoy para evitar el formulario en
blanco (dropdown vacío + ningún día), mostrando además chips de sugerencia
rápida. Ver [`analisis_marketing_ago2026.md`](analisis_marketing_ago2026.md)
y [`onboarding_y_tour.md`](onboarding_y_tour.md).

Mejora propuesta: plantillas multi-tarea (ver [`puntos_de_mejora.md`](puntos_de_mejora.md)).

---

## 6. Notificaciones por tarea

Si `notificationEnabled`, `TaskNotificationScheduler` agenda la próxima
ocurrencia en los próximos 7 días que coincida con `scheduledDays` + hora.

Detalle: [`notificaciones.md`](notificaciones.md).

---

## 7. Archivos

| Archivo | Rol |
|---|---|
| `data/Task.kt` | Modelo |
| `data/PredefinedTasks.kt` | Catálogo de nombres |
| `data/MonkeyStateManager.kt` | CRUD + todayTasks + toggle |
| `ui/TaskEditScreen.kt` | Formulario |
| `ui/MainScreen.kt` | Lista / semana |
| `ui/MonkeyViewModel.kt` | Orquestación |

---

Relacionado: [`racha_y_bananas.md`](racha_y_bananas.md) · [`notificaciones.md`](notificaciones.md) · [`onboarding_y_tour.md`](onboarding_y_tour.md)
