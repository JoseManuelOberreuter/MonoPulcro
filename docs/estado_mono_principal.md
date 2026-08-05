# Estado principal del mono — Mono Pulcro

## Resumen

El mono de la pantalla principal refleja cómo va el usuario con sus tareas del
día y su historial reciente. No es un estado guardado como imagen fija: se
**calcula** en tiempo real a partir de tareas, racha y días perdidos, y luego
`MonkeyImageResolver` elige qué drawable mostrar.

Las motas de polvo son independientes: se dibujan encima del mono (limpio o
sucio). Ver [`motas_de_polvo.md`](motas_de_polvo.md).

---

## 1. ¿Cuándo está limpio o sucio? (`isCleanToday`)

Definido en `MonkeyStateManager.isCleanToday`:

| Situación | Resultado |
|---|---|
| Sin ninguna tarea creada | Siempre **sucio** (incentivo a crear tareas) |
| Día de descanso (ninguna tarea programada hoy) | **Limpio** |
| Hay tareas hoy | **Limpio** solo si **todas** están marcadas |

“Día de descanso” = ninguna tarea tiene el día de la semana actual en
`scheduledDays` (1=Lun … 7=Dom).

---

## 2. Variables de estado (SharedPreferences)

| Clave / getter | Significado |
|---|---|
| `streakCount` | Racha de días completados seguidos |
| `streakBroken` | `true` si se perdió la racha (día fallido reciente) |
| `missedDaysCount` | Días sin completar desde la última vez limpia |
| `rewardGivenToday` | Ya se dio el loot del cofre hoy |
| `streakCountedToday` | Hoy contó para la racha |
| `lastResetDate` | Última fecha de reset diario |
| `equippedAccessory` | Accesorio equipado (ids de tienda) |
| `ownedAccessories` | Set de accesorios comprados |

Esquema completo: [`persistencia.md`](persistencia.md).

---

## 3. Elección de imagen (`MonkeyImageResolver.resolve`)

Entrada: `isClean`, `equippedAccessory`, `streakBroken`, `missedDays`.

Prioridad (de arriba a abajo):

1. **LIMPIO + accesorio equipado** → variante del accesorio (tabla abajo).
2. **LIMPIO sin accesorio** → `mono_pulcro_1/2/3` (variante cada 3 h).
3. **SUCIO + missedDays ≥ 4** → extremo aleatorio:
   `mono_sucio_cansado`, `enfermo`, `frustrado`, `llorando`.
4. **SUCIO + missedDays == 3** → `mono_sucio_3`.
5. **SUCIO + streakBroken** → `mono_sucio_2`.
6. **SUCIO (resto)** → `mono_sucio_1`.

Si el mono está sucio, el accesorio equipado **no** se muestra.

> Nota: el accesorio `gold` / mono de oro fue retirado; al abrir la app se
> limpia de prefs (`migrateRemoveGoldAccessory`).

---

## 4. Accesorios (solo en estado limpio)

Precios actuales (`MonkeyStateManager.ACCESSORIES`):

| ID | Nombre | Precio | Drawables |
|---|---|---|---|
| glasses | Lentes | 10 | `mono_cool_1/2/3` |
| hat | Gorro | 20 | `mono_gorro_1/2/3` |
| chaleco | Chaleco | 30 | `mono_chaleco_1/2/3` |
| crown | Corona | 40 | `mono_corona_1/2/3` |
| payaso | Payaso | 50 | `mono_payaso_1/2/3` |
| vikingo | Vikingo | 60 | `mono_vikingo_1/2/3` |
| astronaut | Astronauta | 70 | `mono_astronauta_1/2/3` |
| mago | Mago | 80 | `mono_mago_1/2/3/4` |
| lazo | Lazo | 90 | `mono_lazo_1/2/3/4` |
| vampiro | Vampiro | 100 | `mono_vampiro_1/2/3` |
| elegante | Elegante | 110 | `mono_elegante_1/2/3` |
| cocinero | Cocinero | 120 | `mono_cocinero_1/2/3` |

Cada accesorio con variantes usa la misma lógica de rotación cada 3 horas.

---

## 5. Variante cada 3 horas (`variantRandom`)

Para mono limpio y accesorios con varias imágenes:

```
slot = nowMs / (3 * 60 * 60 * 1000)
seed = slot XOR hash(accesorio)
→ misma imagen durante el bloque de 3 h; cambia al siguiente bloque
```

Splash, onboarding y placeholders usan `DEFAULT_PULCRO = mono_pulcro_1`.

---

## 6. Completar tareas y recompensa

Al marcar la última tarea del día (`toggleTask`):

- Loot del cofre: `random(1..3)`; en hito de racha (múltiplos de 7): +3 extra → total 4–6
- +1 a la racha
- `streakBroken = false`, `missedDaysCount = 0`
- Posible grant de escudos por hito (ver [`escudos_de_pulcritud.md`](escudos_de_pulcritud.md))
- Overlay de celebración + opción de **duplicar** con anuncio rewarded

Si se desmarca una tarea después de completar el día: se revierte el monto
exacto del cofre y la racha −1.

Detalle de economía: [`racha_y_bananas.md`](racha_y_bananas.md),
[`tienda_y_anuncios.md`](tienda_y_anuncios.md).

---

## 7. Reset diario (`checkAndResetForNewDay`)

Se ejecuta al iniciar el ViewModel, desde el widget y en debug.

Al cambiar de día (`lastResetDate != hoy`):

1. Evalúa **cada día** desde `lastResetDate` hasta ayer (escudos / ruptura).
2. Borra los flags `done_<taskId>`.
3. Resetea flags del día (recompensa, streak counted, doble anuncio, cofre tienda).

El mono puede amanecer sucio si fallaste ayer (salvo protección de escudo).

---

## 8. Texto bajo el mono (`MainScreen`)

| Situación | Mensaje |
|---|---|
| Sin tareas | "¡Agrega tareas para empezar!" |
| Día de descanso | "¡Hoy es día de descanso! 😎" |
| Limpio hoy | Frases motivacionales (`TIPS_PHRASES`) |
| missedDays ≥ 2 o streakBroken | Frases de alerta (`SUCIO2_PHRASES`) |
| missedDays == 1 | Frases suaves (`SUCIO1_PHRASES`) |
| Sucio con tareas | "Hay tareas pendientes..." |

---

## 9. Pantalla principal (layout del mono)

- Caja ~240×240 dp, imagen ~220×220 dp.
- Sombra radial debajo (Canvas).
- Motas de polvo: overlay encima si hay (`DustMotesOverlay`).
- Tap en el mono: animación de limpieza (spray/paño), independiente del
  estado limpio/sucio de tareas.

Header: bananas (izq), icono tienda (centro), racha fuego (der).

---

## 10. Widget

Usa los mismos datos → `MonkeyImageResolver.resolve(...)`.
Muestra la misma imagen de estado y las motas (sin limpieza).
Ver [`estado_widget.md`](estado_widget.md).

---

## 11. Archivos principales

| Archivo | Rol |
|---|---|
| `data/MonkeyStateManager.kt` | Tareas, racha, limpio/sucio, reset |
| `ui/MonkeyImageResolver.kt` | Árbol de decisión de drawable |
| `ui/MonkeyViewModel.kt` | `MonkeyUiState`, `refreshState` |
| `ui/MainScreen.kt` | Mono, texto, celebración, limpieza |
| `ui/ShopScreen.kt` | Compra y preview de accesorios |
| `widget/MonkeyWidget.kt` | Mismo resolver en el widget |

---

## 12. Debug (panel amarillo, solo `BuildConfig.DEBUG`)

Controles de días/escudos: ver [`escudos_de_pulcritud.md`](escudos_de_pulcritud.md) §10.

- Día perdido / ganado → avanza día y dispara reset
- +1/−1 escudo, Reset prefs
- +100 bananas / +2h polvo

---

## 13. Flujo resumido

```
[App abre]
     │
     ▼
checkAndResetForNewDay() ──¿cambió el día?──► evalúa hueco, limpia checks
     │
     ▼
isCleanToday + streakBroken + missedDays + accesorio
     │
     ▼
MonkeyImageResolver.resolve() ──► drawable del mono
```

---

## 14. Progresión visual de suciedad

| Condición | Imagen |
|---|---|
| Tareas pendientes hoy | `mono_sucio_1` |
| Racha rota | `mono_sucio_2` |
| 3 días perdidos | `mono_sucio_3` |
| 4+ días perdidos | estado extremo |

Completar el día resetea `missedDaysCount` y `streakBroken`.

---

Relacionado: [`motas_de_polvo.md`](motas_de_polvo.md) · [`racha_y_bananas.md`](racha_y_bananas.md) · [`INDEX.md`](INDEX.md)
