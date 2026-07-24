# Escudos de Pulcritud — Mono Pulcro

Resumen
-------
Los Escudos de Pulcritud protegen la racha cuando el usuario no completó
las tareas del día que el sistema está evaluando en el reset diario.

  - Cada escudo protege exactamente **un día**.
  - El escudo **no** completa tareas.
  - El escudo **no** genera bananas ni loot del cofre.
  - Solo evita que ese día perdido rompa la racha.

Máximo almacenado: **3 escudos**. Contador en UI: `N/3`.

Toda la lógica vive en `data/MonkeyStateManager.kt` sobre SharedPreferences
(`monkey_prefs`). No hay backend.


1. INICIALIZACIÓN
-----------------
Al crear `MonkeyStateManager` se llama `ensureShieldsInitialized()`:

  - Si `shieldsInitialized == false`:
      shieldsCount = 3
      shieldsInitialized = true
      (escritura atómica con commit)
  - Si ya está inicializado: no hace nada.

Aplica a usuarios nuevos y a existentes al actualizar. Es one-shot e
idempotente: reabrir la app o recrear el manager no vuelve a otorgar escudos.


2. LÍMITE
---------
Constantes en `MonkeyStateManager.Companion`:

  MAX_SHIELDS     = 3
  INITIAL_SHIELDS = 3

Ningún grant (inicial ni por hito) puede superar el máximo:

  grant = min(cantidad, MAX_SHIELDS − shieldsCount)


3. CUÁNDO SE CONSUME UN ESCUDO
------------------------------
Dentro de `checkAndResetForNewDay()`, al evaluar **cada** día desde
`lastResetDate` inclusive hasta ayer (`today − 1`).

Zona horaria: `LocalDate` del dispositivo (igual que la racha).

Un día es **protegible** solo si:

  1. No está marcado como completado (solo el primer día del hueco puede
     tener `streakCountedToday == true`),
  2. hay tareas en la app,
  3. había al menos una tarea programada ese día de la semana (DOW ISO),
  4. no todas esas tareas estaban hechas (días posteriores al primero
     del hueco se consideran incompletos si no se abrió la app).

Casos que **no** consumen escudo:

  - Día ya completado (`streakCountedToday` en el primer día del hueco).
  - Día de descanso (tareas en la app, ninguna ese DOW) → racha intacta.
  - Cero tareas en la app → solo sube `missedDays` (no rompe racha hoy).
  - Mismo día calendario otra vez (`lastResetDate == hoy`) → no-op.

Si la app estuvo cerrada varios días, el reset recorre **todos** los días
del hueco y puede consumir **varios** escudos (uno por día incompleto).
Si se agotan a mitad del hueco, la racha se rompe y los días siguientes
solo suman `missedDays`.


4. ALGORITMO DE CONSUMO
-----------------------
Por cada día protegible del hueco:

  si lastShieldProtectedDate == díaEvaluado
      → ya protegido (idempotente); no re-consumir ni romper racha
  si shieldsCount > 0
      → shieldsCount −= 1
      → lastShieldProtectedDate = díaEvaluado
      → streakBroken = false
      → shieldsUsedAccumulator += 1
      → pendingShieldUsedMessage = true (si la racha sobrevive al hueco)
      → streakCount intacto; missedDays no sube
  si no
      → guardar streak previo; streakCount = 0, streakBroken = true
      → missedDays += 1
      → pendingStreakBrokenMessage (+ lostStreak + shieldsUsed)
      → pendingShieldUsedMessage = false

La mutación del reset usa `commit()` para visibilidad inmediata ante
llamadas concurrentes (app + widget).


5. HITOS DE RACHA (GANAR ESCUDOS)
---------------------------------
Al completar el día (`toggleTask`, caso recompensa), se llama
`claimShieldMilestonesIfNeeded(newStreak)`.

Hitos one-shot (independientes del bonus de bananas cada 7 días):

  Racha    Escudos
  ──────   ───────
  7        +1
  30       +1
  60       +1
  90       +2
  180      +2
  365      +3

Reglas:

  - Cada umbral se registra en `shieldMilestonesClaimed` una sola vez.
  - Si el inventario está lleno, grant = 0 pero el hito **sí** se marca
    (evita reintentos al bajar del tope).
  - Desmarcar tareas el mismo día (revertir racha) **no** revierte hitos.
  - Reabrir la app no vuelve a entregar hitos ya reclamados.


6. PERSISTENCIA (SharedPreferences)
------------------------------------
  Clave                       Tipo      Significado
  ─────────────────────────────────────────────────────────────────────
  shieldsCount                Int       Escudos disponibles (0–3).
  shieldsInitialized          Bool      Ya se dio el grant inicial de 3.
  shieldMilestonesClaimed     StringSet Ids de hito reclamados
                                        ("7","30","60","90","180","365").
  lastShieldProtectedDate     String    Día yyyy-MM-dd ya protegido.
  pendingShieldUsedMessage    Bool      Overlay de protección pendiente.
  shieldsUsedAccumulator      Int       Escudos usados en el hueco actual
                                        (hasta overlay o ruptura).
  pendingStreakBrokenMessage  Bool      Overlay de racha rota pendiente.
  pendingBrokenStreakCount    Int       Racha perdida (animación N→0).
  pendingBrokenShieldsUsed    Int       Escudos usados antes de romper.


7. TIENDA Y UI
--------------
Tienda (`ShopScreen`) con pestañas (tap o swipe):

  Atuendos — accesorios cosméticos.
  Objetos  — Cofre de bananas: ver anuncio → +5 bananas (máx. 3/día).
             Escudo de Pulcritud (100 bananas, `SHIELD_SHOP_PRICE`).
             Muestra "Tienes N/3". Comprar o "Máximo" si ya hay 3.

Bananas disponibles: arriba a la derecha en el TopAppBar.
No hay contador de escudos en el header de la pantalla principal.

Al usar un escudo (y la racha sobrevive):

  ViewModel emite `MonkeyUiEffect.ShowShieldProtectedMessage`
  → Overlay a pantalla completa (`ShieldProtectionOverlay`), estilo
    celebración de racha/cofre: escudo grande, "¡Racha protegida!",
    CTA "¡Seguir!".

Si en el mismo hueco se usaron escudos y la racha igual se rompió:

  ViewModel emite `ShowStreakBrokenMessage(lostStreak, shieldsUsed)`
  → `StreakBrokenOverlay`: contador N→0, copy de escudos usados si > 0,
    CTA "Continuar". No se muestra el overlay de escudo.

El widget no muestra escudos (v1). Sí puede ejecutar el reset y consumir
uno o más; el overlay aparece al abrir/reanudar la app.


8. ARCHIVOS PRINCIPALES
-----------------------
  data/MonkeyStateManager.kt   — init, consumo, hitos, buyShield, claves prefs.
  ui/MonkeyViewModel.kt        — shieldsCount en MonkeyUiState; efecto UI.
  ui/ShopScreen.kt             — ShieldShopCard (primer ítem, 100 bananas).
  ui/MainScreen.kt             — ShieldProtectionOverlay / StreakBrokenOverlay.

  res/drawable/escudo_pulcritud.png


9. FLUJO RESUMIDO
-----------------

  [Primera vez / actualización]
         │
         ▼
  ensureShieldsInitialized() ──► 3/3 escudos (una sola vez)

  [Completar todas las tareas del día]
         │
         ▼
  streak++ → claimShieldMilestonesIfNeeded (capped a 3)

  [Cambio de día — checkAndResetForNewDay]
         │
         ▼
  Evalúa cada día lastReset…ayer
         │
         ├── completo / descanso / sin obligaciones → sin escudo
         ├── incompleto + escudo → consume 1, racha intacta
         └── incompleto sin escudo → racha = 0, streakBroken,
                                     pending StreakBrokenOverlay


10. DEBUG (panel amarillo, solo BuildConfig.DEBUG)
--------------------------------------------------
En MainScreen, al final del scroll, panel "DEBUG · Escudos / Días".

Estado mostrado: fecha de juego, offset, lastReset, racha, escudos,
streakCounted, broken, tareas hoy, missedDays, último día protegido.

Botones clave:

  Día perdido →     Marca el día actual como incompleto, avanza +1 día
                    y corre checkAndResetForNewDay. Debe consumir 1 escudo
                    si hay tareas programadas ese DOW.
  Día ganado →      Marca streakCounted=true, avanza +1 día. No consume escudo.
  Avanzar tal cual → Avanza con el estado actual (counted/done_*).
  Offset=0          Vuelve la fecha de juego a hoy real.
  +1 / −1 escudo    Ajusta el contador (0–3).
  Racha=6 (hito)    Deja streak en 6; al completar el día se dispara hito 7.
  +100 bananas / +2h polvo / Reset prefs

La fecha de juego es LocalDate.now() + debugDayOffset (prefs).
todayTasks y el reset usan esa fecha.


11. FUERA DE ALCANCE
--------------------
  - Recuperar tareas de días anteriores pagando bananas (otro sistema).
  - Historial detallado de cada protección (solo lastShieldProtectedDate).
  - Mostrar escudos en el widget.


================================================================================
  Relacionado: docs/racha_y_bananas.md (reset diario y racha)
               docs/estado_mono_principal.md (imagen del mono)
================================================================================
