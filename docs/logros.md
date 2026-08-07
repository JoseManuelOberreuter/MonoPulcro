# Logros y progresión a largo plazo — Mono Pulcro

Resumen
-------
Sistema de badges permanentes que dan micro-metas independientes de la racha
activa. A diferencia de racha/bananas (que pueden bajar), estos contadores
solo suman y los logros, una vez reclamados, nunca se revierten — le dan al
usuario un registro de progreso que sobrevive a romper la racha o gastar
bananas.


1. CONTADORES DE POR VIDA (nunca bajan)
-----------------------------------------
- `bestStreakCount` — el máximo valor que alcanzó `streakCount`. Se actualiza
  en `MonkeyStateManager.toggleTask()`, en el mismo commit donde se
  incrementa la racha del día (`maxOf(bestStreakCount, newStreak)`). No se
  toca al romperse la racha ni al destildar una tarea.
- `totalBananasEarned` — suma de bananas ganadas por juego (cofre diario,
  bono de hito ×7, cofre de tienda con ad, triplicar cofre, motas
  limpiadas). Deliberadamente NO incluye las bananas de compras IAP (dinero
  real) ni se resta cuando el usuario gasta bananas o destilda una tarea el
  mismo día.
- `totalMotesCleaned` — motas de polvo limpiadas acumuladas (`+1` por mota,
  igual que la banana que otorgan).


2. CATÁLOGO DE LOGROS (`data/Achievement.kt`)
------------------------------------------------
Lista fija en código, no editable por el usuario. 10 logros v1:

| id | Título | Condición |
|---|---|---|
| `streak_3` | Primeros pasos | `bestStreakCount >= 3` |
| `streak_7` | Semana perfecta | `bestStreakCount >= 7` |
| `streak_30` | Mes de oro | `bestStreakCount >= 30` |
| `streak_100` | Leyenda de la limpieza | `bestStreakCount >= 100` |
| `bananas_50` | Ahorrista | `totalBananasEarned >= 50` |
| `bananas_500` | Banana millonario | `totalBananasEarned >= 500` |
| `motes_10` | Manos limpias | `totalMotesCleaned >= 10` |
| `motes_50` | Cazador de polvo | `totalMotesCleaned >= 50` |
| `accessory_first` | Nuevo look | posee ≥1 accesorio |
| `accessory_all` | Guardarropa completo | posee los 12 accesorios de `ACCESSORIES` |


3. EVALUACIÓN: `checkAchievements()`
---------------------------------------
Un único método centralizado en `MonkeyStateManager` (no hooks dispersos):
lee los contadores actuales + el set `achievementsUnlocked`, evalúa cada
condición del catálogo, agrega los nuevos ids al set en un solo `commit()`,
y devuelve la lista de logros recién desbloqueados en esa llamada. Un logro
ya reclamado nunca se re-evalúa ni se puede perder.

Se llama desde `MonkeyViewModel` (vía `applyAchievementUnlocks()`) en 4
puntos: al completar/destildar una tarea (`toggleTask`), al limpiar motas
(`completeDustCleaning`), al comprar un accesorio (`buyAccessory`), y al
refrescar el estado (`refresh()` / `init`, tras `checkAndResetForNewDay()`).


4. UI
------
- El header de `MainScreen` sigue teniendo solo 3 íconos (bananas, tienda,
  racha) — no se agregó un ícono nuevo. El contador de racha (`StreakCounter`)
  ahora es clickeable y abre `StreakScreen` (ruta `streak`).
- `StreakScreen` replica la "forma" visual de `ShopScreen` (mismo
  `TopAppBar` + tabs tipo píldora + `HorizontalPager`) pero en paleta cálida
  (naranja/fuego) en vez del celeste de la tienda. Dos tabs:
  - **Racha**: racha actual, mejor racha histórica, escudos, días perdidos.
  - **Logros**: grilla de 2 columnas con todos los logros (bloqueado = 🔒
    gris, desbloqueado = emoji + color) y el contador
    "`X / 10 desbloqueados`".
- Al desbloquear uno o más logros, `MonkeyUiEffect.ShowAchievementUnlocked`
  encola cada uno y `MainScreen` los muestra de a uno con
  `AchievementUnlockedOverlay` (`ui/MonkeyCleaningOverlay.kt`): tarjeta con
  emoji + título + descripción, aparece con rebote y se desvanece sola.


5. FUERA DE ALCANCE (v1)
---------------------------
- Indicador de logros/nivel en el widget.
- Integración con el tour de onboarding.
- Notificación push al desbloquear un logro.
- Backfill retroactivo: usuarios existentes arrancan los contadores de por
  vida en 0 — su racha/bananas históricas previas a esta versión no cuentan
  para los logros ya pasados (ej. alguien con racha activa de 10 días no
  recibe automáticamente `streak_7`; lo desbloquea la próxima vez que su
  racha vuelva a tocar ese número).


6. ARCHIVOS PRINCIPALES
-----------------------
  data/Achievement.kt              — Modelo + catálogo `Achievements.ALL`.
  data/MonkeyStateManager.kt       — Contadores + `checkAchievements()`.
  ui/MonkeyViewModel.kt            — `applyAchievementUnlocks()`, efecto.
  ui/StreakScreen.kt               — Pantalla de racha + logros (tabs).
  ui/MonkeyCleaningOverlay.kt      — `AchievementUnlockedOverlay`.
  ui/MainScreen.kt                 — Racha clickeable + cola de overlays.
  MainActivity.kt                  — Ruta `streak`.

Relacionado: [`persistencia.md`](persistencia.md) ·
[`racha_y_bananas.md`](racha_y_bananas.md) ·
[`escudos_de_pulcritud.md`](escudos_de_pulcritud.md) ·
[`motas_de_polvo.md`](motas_de_polvo.md) ·
[`ideas_gamificacion.md`](ideas_gamificacion.md) · [`INDEX.md`](INDEX.md)
