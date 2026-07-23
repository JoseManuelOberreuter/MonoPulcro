# Racha y bananas — Mono Pulcro

Resumen
-------
La racha (streak), las bananas y los Escudos de Pulcritud son el núcleo
de la gamificación:

  - RACHA    → días consecutivos completando TODAS las tareas del día.
               Es el progreso emocional (fuego en el header y celebración).
  - BANANAS  → moneda virtual. Se gana al abrir el cofre del día (loot
               aleatorio), en hitos de racha y limpiando motas de polvo.
               Se gasta en la tienda.
  - ESCUDOS  → consumibles (máx. 3). Protegen la racha cuando el día
               evaluado en el reset diario habría roto la racha. No
               completan tareas ni generan recompensas de cofre.

Toda la lógica vive en data/MonkeyStateManager.kt sobre SharedPreferences
(archivo "monkey_prefs"). No hay backend: todo es local y síncrono.


1. VARIABLES PERSISTIDAS (SharedPreferences)
--------------------------------------------
  Clave                Tipo     Significado
  ─────────────────────────────────────────────────────────────────────────
  streakCount          Int      Racha actual (días seguidos completados).
  bananas              Int      Bananas disponibles (nunca baja de 0).
  rewardGivenToday     Bool     Ya se pagó la recompensa del día de hoy.
  rewardBananasToday   Int      Cuántas bananas pagó el cofre hoy (1–6).
                                Se usa para revertir exacto al desmarcar.
  streakCountedToday   Bool     Hoy ya sumó a la racha (lo usa el reset
                                para saber si ayer se completó).
  streakBonusGiven     Bool     La recompensa de hoy incluyó el bono x7.
  streakBroken         Bool     Se rompió la racha (día fallido reciente);
                                afecta imagen del mono y frases.
  missedDaysCount      Int      Días fallidos acumulados desde la última
                                vez que se completó un día.
  lastResetDate        String   Fecha (yyyy-MM-dd) del último reset diario.
  done_<taskId>        Bool     Checkbox de cada tarea, se borra a diario.
  shieldsCount         Int      Escudos de Pulcritud disponibles (0–3).
  shieldsInitialized   Bool     Ya se otorgaron los 3 escudos iniciales.
  shieldMilestonesClaimed  Set  Hitos de racha que ya dieron escudos
                                ("7","30","60","90","180","365").
  lastShieldProtectedDate  String  Día (yyyy-MM-dd) ya protegido (idempotencia).
  pendingShieldUsedMessage Bool  Mostrar snackbar de protección en la UI.


2. CÓMO SE GANA LA RACHA Y EL LOOT DEL COFRE (toggleTask)
----------------------------------------------------------
Cada marca/desmarca de tarea pasa por MonkeyStateManager.toggleTask(id):

  1. Se invierte el estado de la tarea (done_<id>).
  2. Se evalúa: ¿todas las tareas de HOY están completadas?
     (todayTasks = tareas cuyo scheduledDays incluye el día actual, 1=Lun…7=Dom)

  CASO A — Se marcó la última tarea pendiente y aún no se pagó hoy
           (newState && allTodayDone && !rewardGivenToday):

     nuevaRacha   = streakCount + 1
     esHito       = nuevaRacha % 7 == 0          (7, 14, 21, …)
     lootCofre    = random(1..3) + (3 si esHito)  → entre 1 y 3, o 4 y 6 en x7
     bananas     += lootCofre
     rewardBananasToday = lootCofre
     streakCount  = nuevaRacha
     rewardGivenToday   = true
     streakCountedToday = true
     streakBroken       = false
     missedDaysCount    = 0
     streakBonusGiven   = esHito
     → claimShieldMilestonesIfNeeded(nuevaRacha) (escudos one-shot, capped)
     → devuelve true → el ViewModel dispara la celebración.

  CASO B — Se desmarcó una tarea DESPUÉS de haber completado el día
           (!newState && rewardGivenToday):

     awarded = rewardBananasToday   (revierte el monto exacto del cofre)
     bananas = max(0, bananas − awarded)
     streakCount = max(0, streakCount − 1)
     rewardGivenToday / streakCountedToday / streakBonusGiven = false
     rewardBananasToday = 0
     → anti-exploit de marcar/desmarcar.
     → si vuelve a marcar todo, se tira un NUEVO random(1..3) para el cofre.

  CASO C — Cualquier otro toggle intermedio: no toca racha ni bananas.

IMPORTANTE: la recompensa se paga UNA sola vez por día (rewardGivenToday).
El número aleatorio se fija al completar el día; el overlay del cofre solo
lo revela visualmente.


3. RESET DIARIO (checkAndResetForNewDay)
----------------------------------------
Se ejecuta al crear el MonkeyViewModel (inicio de app). Si lastResetDate
ya es hoy, no hace nada. Si cambió el día:

  1. EVALÚA EL DÍA ANTERIOR (el de lastResetDate):

     ┌────────────────────────────────────┬─────────────────────────────────┐
     │ Situación de ayer                  │ Efecto                          │
     ├────────────────────────────────────┼─────────────────────────────────┤
     │ streakCountedToday == true         │ Racha sana: streakBroken=false, │
     │ (completó todo)                    │ missedDays=0                    │
     │ No existe ninguna tarea en la app  │ missedDays+1 (mono se ensucia   │
     │                                    │ igual, incentivo a crear)       │
     │ Ayer era día de descanso           │ Sin cambios: racha intacta      │
     │ (0 tareas programadas ese día)     │ pero NO suma                    │
     │ Tenía tareas y NO completó todas   │ Si hay escudo: consume 1,       │
     │                                    │ conserva racha, mensaje UI.     │
     │                                    │ Si no: streakCount=0,           │
     │                                    │ streakBroken, missedDays+1      │
     └────────────────────────────────────┴─────────────────────────────────┘

  2. Borra todos los done_<taskId> (día nuevo en blanco).
  3. Resetea rewardGivenToday, streakCountedToday, streakBonusGiven,
     rewardBananasToday.
  4. Guarda lastResetDate = hoy.

Matices importantes:

  - DÍA DE DESCANSO: mantiene la racha pero no la incrementa.
  - Si la app estuvo cerrada VARIOS días, el reset solo evalúa el día de
    lastResetDate (un solo día). Un escudo protege como máximo ese día.
  - Las bananas NUNCA se pierden al fallar un día; solo la racha vuelve a 0
    (salvo que un escudo la proteja).
  - Día evaluado = lastResetDate (no lastReset+1). Zona: LocalDate del dispositivo.


3.1 ESCUDOS DE PULCRITUD
------------------------
Resumen: consumibles (máx. 3) que protegen la racha en el día evaluado
del reset si había tareas incompletas. Doc completo:

  → docs/escudos_de_pulcritud.md


4. FUENTES Y GASTOS DE BANANAS
------------------------------
  Fuente                                   Cantidad   Dónde
  ─────────────────────────────────────────────────────────────────────────
  Cofre del día (loot aleatorio)            +1 a +3   toggleTask (caso A)
  Hito de racha (múltiplos de 7)            +3 extra  toggleTask (caso A)
                                            (total 4–6 en hito)
  Limpiar motas de polvo (tap al mono)      +1        rewardDustCleaning()
                                                      (ver docs/motas_de_polvo.md)
  [Debug] botón "100 bananas"               +100      panel de debug

  Gasto                                    Cantidad   Dónde
  ─────────────────────────────────────────────────────────────────────────
  Escudo de Pulcritud (tienda, 1º ítem)      100     buyShield()
  Comprar accesorio en la tienda           precio     buyAccessory()

  Precios actuales (ACCESSORIES en MonkeyStateManager):

    glasses    Lentes       5
    hat        Gorro       12
    chaleco    Chaleco     20
    crown      Corona      30
    payaso     Payaso      40
    astronaut  Astronauta  60

  Reversa: desmarcar una tarea tras completar el día descuenta el monto
  exacto guardado en rewardBananasToday (piso en 0).


5. FLUJO EN LA UI — DOS PANTALLAS (MonkeyViewModel + MainScreen)
----------------------------------------------------------------
Al marcar la última tarea del día:

  MainScreen ──► vm.toggleTask(id)
                    │  (loot ya calculado y acreditado en prefs)
                    ▼
             ¿earned == true?
                    │
                    ▼
      StreakCelebration(previousStreak, newStreak,
                        bananasEarned = manager.lastRewardBananas,
                        isMilestone)
                    │  (viaja en MonkeyUiState.celebration; MainScreen la
                    │   copia a estado local y llama consumeCelebration())
                    ▼
      PANTALLA 1: StreakCelebrationOverlay  (naranja, fuego)
                    + notificación + grito_mono.mp3 + haptic
                    │
                    │  CTA "Abrir cofre" (fade-out → transición)
                    ▼
      PANTALLA 2: ChestCelebrationOverlay   (dorado, cofre protagonista)
                    │
                    ▼
      Vuelve a la home


5.1 PANTALLA 1 — StreakCelebrationOverlay (racha)
-------------------------------------------------
Fondo degradado naranja (StreakBgTop → StreakBgBottom).

  1. Fade-in del fondo.
  2. Llama de fuego entra con rebote.
  3. Contador anima previousStreak → newStreak (650 ms).
  4. Punch de la llama + partículas de fuego + sonido + haptic.
  5. Titular ("¡Tu mono sigue impecable!" / "¡Meta alcanzada!…" si es hito).
  6. Preview del cofre CERRADO (cofre_cerrado.png, pequeño) + "¡Ganaste un cofre!"
  7. CTA "Abrir cofre" → fade-out y navega a la pantalla 2.

La racha es el héroe; el cofre solo se anuncia, no se abre aquí.


5.2 PANTALLA 2 — ChestCelebrationOverlay (cofre)
-------------------------------------------------
Fondo degradado dorado (ChestBgTop → ChestBgBottom). Pantalla completa
dedicada al cofre, al estilo Duolingo.

  1. Fade-in + cofre grande centrado (180 dp) con glow radial.
  2. Titular "¡Tu cofre te espera!" + "Toca el cofre para abrirlo".
  3. CTA inferior "Abrir cofre" (mismo gesto que tocar el cofre o el fondo).
  4. Al abrir:
     - Shake horizontal de anticipación (4 ciclos).
     - Swap a cofre_abierto.png con punch elástico.
     - BURST de bananas: 6–12 partículas de banana salen del cofre en
       arco radial (ángulos y distancias aleatorias, rotación, fade).
     - Sonido caja registradora + haptic.
     - Chip "+N" con el loot real (bananasEarned) + "¡bonus x7!" si aplica.
     - Titular cambia a "¡Tesoro encontrado!"
  5. CTA "¡Seguir!" → vuelve a la home.

El número mostrado (+N) coincide con rewardBananasToday ya acreditado.
Las partículas visuales son decorativas; el contador muestra el monto real.


6. RELACIÓN RACHA ↔ ESTADO DEL MONO
------------------------------------
La racha alimenta la imagen del mono (ver docs/estado_mono_principal.md):

  streakBroken == true      → mono_sucio_2 y frases de alerta.
  missedDays 3 / 4+         → mono_sucio_3 / estados extremos.
  Completar el día          → limpio, missedDays=0, streakBroken=false.

El texto bajo el mono también cambia según racha/missedDays (TIPS_PHRASES,
SUCIO1_PHRASES, SUCIO2_PHRASES en MainScreen.kt).


7. CASOS BORDE Y REGLAS ANTI-EXPLOIT
-------------------------------------
  - Doble pago imposible: rewardGivenToday bloquea repetir la recompensa.
  - Reversión exacta: rewardBananasToday guarda el monto del random para
    descontarlo tal cual al desmarcar (no asume siempre +1).
  - Nuevo random al re-completar: si desmarcas y vuelves a marcar todo,
    se tira otro random(1..3) independiente.
  - Piso de bananas en 0: nunca queda saldo negativo al revertir.
  - Sin tareas creadas → isCleanToday=false y missedDays sube a diario.
  - Día de descanso: ni suma ni rompe racha.


8. ARCHIVOS INVOLUCRADOS
------------------------
  data/MonkeyStateManager.kt  — toggleTask (loot random), checkAndResetForNewDay,
                                escudos (init/consumo/hitos), lastRewardBananas,
                                buyAccessory.
  ui/MonkeyViewModel.kt       — StreakCelebration, efecto ShowShieldProtectedMessage.
  ui/MainScreen.kt            — StreakCelebrationOverlay (pantalla 1),
                                ChestCelebrationOverlay (pantalla 2),
                                header (bananas, escudos N/3, racha).
  ui/ShopScreen.kt            — Gasto de bananas en accesorios.
  notifications/…             — Notificación al completar el día.
  widget/…                    — Refleja racha/estado en el widget.


9. FLUJO RESUMIDO
-----------------

  [Usuario marca tareas]
        │
        ▼
  toggleTask ──último check──► loot=random(1..3)+bonus, racha+1, bananas+=loot
        │
        ▼
  Pantalla racha (fuego) ──► "Abrir cofre" ──► Pantalla cofre (dorada)
        │                                          │
        │                                          ▼
        │                                    tap → burst bananas → "+N"
        │                                          │
        └──────────────────────────────────────────┘
                          "¡Seguir!" → home

  [Nuevo día] checkAndResetForNewDay
        ├── ayer completo      → racha se conserva
        ├── ayer descanso      → racha intacta (no suma)
        ├── ayer incompleto + escudo → consume 1, racha intacta
        ├── ayer incompleto sin escudo → racha=0, streakBroken, missedDays+1
        └── limpia checks y flags del día


10. FUTURO (no implementado)
---------------------------
  - Mover el pago de bananas al momento exacto de abrir el cofre (hoy se
    acredita al completar el día, el cofre solo revela).
  - Rewarded Ads opt-in para duplicar loot del cofre.
  - XP / niveles del mono como progreso a largo plazo.


================================================================================
  Relacionado: docs/estado_mono_principal.md (imagen del mono)
               docs/motas_de_polvo.md (banana extra por limpieza)
               docs/puntos_de_mejora.md (§6 monetización futura)
================================================================================
