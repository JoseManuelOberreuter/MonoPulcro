# Widget de pantalla de inicio — Mono Pulcro

Documentación del widget Glance. Versión de app: **1.2.3** (versionCode 19).

## Resumen

El widget muestra en la pantalla de inicio:

- Imagen del mono según el mismo criterio que la app (`MonkeyImageResolver`).
- Motas de polvo encima del mono (escaladas desde la pantalla principal).
- Racha: icono de fuego + número de días.

Al tocar cualquier parte del widget se abre `MainActivity`. No hay acciones
rápidas (marcar tareas, limpiar polvo, etc.) desde el widget.

---

## 1. Archivos involucrados

| Archivo | Rol |
|---|---|
| `widget/MonkeyWidget.kt` | UI Glance y lectura de estado |
| `widget/MonkeyWidgetReceiver.kt` | Receiver + `updateWidget()` |
| `widget/WidgetUpdateScheduler.kt` | Alarm horario |
| `widget/WidgetUpdateReceiver.kt` | Tick: reset + sync polvo + update |
| `res/xml/monkey_widget_info.xml` | Tamaño, resize, periodo |
| `res/layout/widget_initial_layout.xml` | Placeholder de carga |
| `data/MonkeyStateManager.kt` | Fuente de verdad |
| `ui/MonkeyImageResolver.kt` | Drawable del mono |

Dependencias: `glance-appwidget:1.1.1`, `glance-material3:1.1.1`.

---

## 2. Configuración (`monkey_widget_info.xml`)

| Parámetro | Valor |
|---|---|
| Tamaño mínimo | 180 × 140 dp |
| Celdas objetivo | 2 × 2 |
| Redimensionable | horizontal y vertical |
| Categoría | home_screen |
| `updatePeriodMillis` | 3_600_000 (1 h, respaldo del sistema) |

---

## 3. Flujo de datos (`provideGlance`)

```
provideGlance()
  → MonkeyStateManager
  → checkAndResetForNewDay()
  → syncDustSpawns()
  → lee streak, accessory, isClean, streakBroken, missedDays, dustMotes
  → MonkeyImageResolver.resolve(...)
  → provideContent { WidgetContent(...) }
```

---

## 4. Contenido visual

`SizeMode.Responsive` + breakpoints 180×140, 260×140, 180×260, 260×260.

- Mono: ~58 % del lado útil, entre 72 y 128 dp.
- Racha: fuego + número (naranja `#FF6D00`).
- Fondo: `0xFFFFF8F0`.

---

## 5. Imagen del mono

Misma función que MainScreen. Prioridad: limpio+accesorio → pulcro →
extremos → sucio_3 → sucio_2 → sucio_1.

Diferencias vs app:

| Aspecto | App | Widget |
|---|---|---|
| Tamaño | ~220–240 dp | 72–128 dp |
| Mensajes / bananas / checklist | Sí | No |
| Motas | Limpiables | Solo visual |

Variantes de imagen: rotación cada **3 horas** (igual que la app).

---

## 6. Motas de polvo

Misma lista que la app; `syncDustSpawns()` en cada refresh.
Escala: `monkeyDp / MAIN_MONKEY_DP`. Detalle: [`motas_de_polvo.md`](motas_de_polvo.md).

---

## 7. Actualización

### Manual — `MonkeyWidgetReceiver.updateWidget`

Actualiza todas las instancias Glance en `Dispatchers.IO`.

### Horaria — `WidgetUpdateScheduler` (1 h)

Se programa en `onEnabled`, `onUpdate`, `BOOT_COMPLETED`, al abrir la app
si hay widget, y tras cada tick. Se cancela en `onDisabled`.

### Desde `MonkeyViewModel`

| Evento | ¿Actualiza? |
|---|---|
| Marcar/desmarcar tarea | Sí |
| Limpiar motas | Sí |
| Comprar / equipar accesorio | Sí |
| Agregar / editar / borrar tarea | No (hasta tick o abrir app) |

---

## 8. Manifest

`MonkeyWidgetReceiver` exportado con `APPWIDGET_UPDATE` + meta-data del provider.
`WidgetUpdateReceiver` no exportado.

---

## 9. Limitaciones conocidas

1. CRUD de tareas no refresca el widget al instante.
2. Sin progreso N/M, bananas ni escudos.
3. Solo abre la app; sin acciones Glance.
4. Colores hardcodeados (Glance Material 3 declarado pero poco usado).

Mejoras propuestas: [`puntos_de_mejora.md`](puntos_de_mejora.md).

---

Relacionado: [`estado_mono_principal.md`](estado_mono_principal.md) · [`motas_de_polvo.md`](motas_de_polvo.md)
