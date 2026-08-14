# XP, niveles, alimentar y acariciar — Mono Pulcro

Resumen
-------
Capa de gamificación estilo Tamagotchi: el mono gana experiencia (XP) de
forma acumulada (de por vida, nunca baja) al completar tareas, al ser
alimentado y al ser acariciado. La XP determina un **nivel** puramente
cosmético/narrativo (título), sin efecto en la economía ni en la racha.
No hay decay: alimentar y acariciar solo suman, no hay una nueva forma de
"fallar" además de las tareas diarias.


1. XP: FUENTES Y ACUMULACIÓN
-----------------------------
`monkeyXp` es un contador de por vida (igual que `totalBananasEarned`):
nunca se resta, ni siquiera al destildar una tarea.

| Fuente | XP | Dónde |
|---|---|---|
| Completar una tarea individual | +2 (`XP_PER_TASK`) | `toggleTask()`, cada vez que se marca una tarea |
| Completar todas las tareas del día | +10 extra (`XP_PER_DAY_COMPLETE`) | `toggleTask()`, al marcar la última tarea del día |
| Alimentar al mono | +5 (`XP_PER_FEED`) | `feedMonkey()` |
| Acariciar al mono | +3 (`XP_PER_PET`) | `petMonkey()` |

Ejemplo: un día con una sola tarea programada, al completarla se otorgan
2 (tarea) + 10 (día completo) = 12 XP en el mismo `toggleTask()`.


2. NIVELES (`data/MonkeyLevel.kt`)
------------------------------------
El nivel **no se persiste**: se deriva de `monkeyXp` en tiempo real con
`MonkeyLevels.forXp(xp)`, igual que el resto de la app calcula estado
derivado en vez de guardarlo (ver `estado_mono_principal.md`).

Catálogo fijo (niveles 1-10, títulos narrativos, sin arte nuevo):

| Nivel | XP requerida | Título |
|---|---|---|
| 1 | 0 | Monito curioso |
| 2 | 20 | Monito aplicado |
| 3 | 50 | Mono cuidadoso |
| 4 | 100 | Mono ordenado |
| 5 | 180 | Mono pulcro |
| 6 | 280 | Mono brillante |
| 7 | 400 | Mono ejemplar |
| 8 | 550 | Mono maestro |
| 9 | 750 | Mono leyenda |
| 10 | 1000 | Mono pulcro veterano |

Nivel 11+: sin tope, +300 XP por nivel (`xpRequerida = 1000 + (nivel-10)*300`),
reutilizando el título del nivel 10.

`MonkeyLevelInfo` expone `level`, `title`, `xpForCurrentLevel`,
`xpForNextLevel` y `progress` (0..1) para la barra de progreso en la UI.


3. ALIMENTAR (`feedMonkey()`)
-------------------------------
- Cuesta `FEED_COST = 3` bananas.
- Tope diario `MAX_FEEDS_PER_DAY = 3` (contador `feedsToday`, reset diario
  junto al resto de flags del día en `checkAndResetForNewDay()`).
- Falla (no descuenta ni otorga XP) si no alcanzan las bananas o si ya se
  agotó el tope de hoy.
- Botón "🍌 Alimentar" en `MainScreen`, deshabilitado si no hay bananas
  suficientes o si se llegó al tope diario.


4. ACARICIAR (`petMonkey()`)
------------------------------
- Gratis, sin costo de bananas.
- Reusa el **mismo tap** sobre el mono que ya disparaba la limpieza de motas
  de polvo:
  - Si hay motas de polvo → comportamiento sin cambios (limpieza + banana
    por mota, ver [`motas_de_polvo.md`](motas_de_polvo.md)).
  - Si **no** hay motas → el tap acaricia al mono (animación de corazones,
    ~1 s) y otorga XP.
- Tope diario `MAX_PETS_PER_DAY = 10` (contador `petsToday`, mismo reset
  diario que `feedsToday`), para que no sea spameable ahora que el tap
  "vacío" ya no es un no-op.


5. LOGROS RELACIONADOS
------------------------
Se agregaron al catálogo de `Achievements.ALL` (ver [`logros.md`](logros.md)):

| id | Título | Condición |
|---|---|---|
| `tamagotchi_first` | Primer mimo | `monkeyXp > 0` (alimentó o acarició al menos una vez) |
| `level_5` | Mono creciendo | Nivel del mono ≥ 5 |
| `level_10` | Mono veterano | Nivel del mono ≥ 10 |

Evaluados en el mismo `checkAchievements()` centralizado que el resto de
logros.


6. UI
------
- **Botón "Alimentar"**: bajo la imagen del mono en `MainScreen`, junto al
  badge "Nivel N · Título".
- **Overlay de XP**: "+N XP" (`XpRewardOverlay`), al alimentar o acariciar
  con éxito.
- **Overlay de subida de nivel**: `LevelUpOverlay`, calcado del patrón visual
  de `AchievementUnlockedOverlay` (tarjeta con rebote/fade), se dispara
  comparando el nivel antes/después de cada fuente de XP
  (`MonkeyViewModel.awardXpAndDetectLevelUp`).
- **Pestaña "Nivel"** en `StreakScreen` (junto a "Racha" y "Logros"): nivel
  actual, título, barra de progreso a la próxima XP, y alimentaciones/
  caricias restantes hoy.


7. FUERA DE ALCANCE (v1)
---------------------------
- Indicador de nivel/XP en el widget.
- Decay de hambre/ánimo (esta capa es deliberadamente solo positiva).
- Ítems de comida comprables en tienda (alimentar es un botón simple con
  costo fijo).
- Notificación push al subir de nivel.
- Backfill retroactivo: usuarios existentes arrancan `monkeyXp` en 0.


8. ARCHIVOS PRINCIPALES
-----------------------
  data/MonkeyLevel.kt              — Catálogo de niveles + `forXp()`.
  data/MonkeyStateManager.kt       — `monkeyXp`, `feedMonkey()`, `petMonkey()`,
                                      reset diario, XP en `toggleTask()`.
  data/Achievement.kt              — Logros `tamagotchi_first`/`level_5`/`level_10`.
  ui/MonkeyViewModel.kt            — Estado de nivel/XP, `feedMonkey()`,
                                      `petMonkey()`, efecto `ShowLevelUp`.
  ui/MainScreen.kt                 — Branch del tap (limpiar vs acariciar),
                                      botón alimentar, overlays.
  ui/MonkeyCleaningOverlay.kt      — `XpRewardOverlay`, `LevelUpOverlay`,
                                      `MonkeyPettingOverlay`.
  ui/StreakScreen.kt               — Pestaña "Nivel".


Relacionado: [`ideas_gamificacion.md`](ideas_gamificacion.md) ·
[`racha_y_bananas.md`](racha_y_bananas.md) · [`logros.md`](logros.md) ·
[`motas_de_polvo.md`](motas_de_polvo.md) · [`persistencia.md`](persistencia.md) ·
[`economia.md`](economia.md) · [`INDEX.md`](INDEX.md)
