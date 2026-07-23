# Estado principal del mono — Mono Pulcro

Resumen
-------
El mono de la pantalla principal refleja cómo va el usuario con sus tareas del
día y su historial reciente. No es un estado guardado como imagen fija: se
CALCULA en tiempo real a partir de tareas, racha y días perdidos, y luego
MonkeyImageResolver elige qué drawable mostrar.

Las motas de polvo son independientes: se dibujan encima del mono (limpio o
sucio). Ver docs/motas_de_polvo.md.


1. ¿CUÁNDO ESTÁ LIMPIO O SUCIÓ? (isCleanToday)
------------------------------------------------
Definido en MonkeyStateManager.isCleanToday:

  Sin ninguna tarea creada     → SIEMPRE sucio (incentivo a crear tareas).
  Día de descanso (ninguna     → LIMPIO (no hay nada que hacer hoy).
  tarea programada para hoy)
  Hay tareas hoy                 → LIMPIO solo si TODAS están marcadas.

“Día de descanso” = ninguna tarea tiene el día de la semana actual en su
lista scheduledDays (1=Lun … 7=Dom).


2. VARIABLES DE ESTADO (SharedPreferences)
------------------------------------------
  streakCount      — Racha de días completados seguidos.
  streakBroken     — true si se perdió la racha (día fallido reciente).
  missedDaysCount  — Contador de días sin completar (sube al fallar).
  rewardGivenToday — Ya se dio la banana del día al completar todo.
  streakCountedToday — Hoy contó para la racha (evita doble penalización).
  lastResetDate    — Última fecha en que se hizo reset diario.
  equippedAccessory — Accesorio equipado (glasses, hat, crown, astronaut, gold).
  ownedAccessories — Set de accesorios comprados en la tienda.


3. ELECCIÓN DE IMAGEN (MonkeyImageResolver.resolve)
---------------------------------------------------
Entrada: isClean, equippedAccessory, streakBroken, missedDays

Prioridad (de arriba a abajo):

  A) LIMPIO + accesorio equipado
     → Variante del accesorio (ver tabla abajo).
     → En pantalla principal, si el accesorio es "gold" y está limpio, se usa
       GoldMonkeyImage (brillos + pila de oro) en lugar del Image normal.

  B) LIMPIO sin accesorio
     → mono_pulcro_1 / _2 / _3 (variante aleatoria DIARIA).

  C) SUCIO + missedDays >= 4
     → Estado extremo aleatorio entre:
        mono_sucio_cansado, mono_sucio_enfermo,
        mono_sucio_frustrado, mono_sucio_llorando

  D) SUCIO + missedDays == 3
     → mono_sucio_3

  E) SUCIO + streakBroken == true
     → mono_sucio_2

  F) SUCIO (resto: tareas pendientes hoy, 1 día perdido, etc.)
     → mono_sucio_1

IMPORTANTE: Si el mono está sucio, el accesorio equipado NO se muestra en la
imagen (solo aplica cuando isCleanToday == true).


4. ACCESORIOS (solo en estado limpio)
-------------------------------------
  ID          Nombre        Precio   Drawables
  ─────────────────────────────────────────────────────────
  glasses     Lentes          10     mono_cool_1 / _2 / _3
  hat         Gorro           20     mono_gorro_1 / _2 / _3
  chaleco     Chaleco         30     mono_chaleco_1 / _2 / _3
  crown       Corona          40     mono_corona_1 / _2 / _3
  payaso      Payaso          50     mono_payaso_1 / _2 / _3
  vikingo     Vikingo         60     mono_vikingo_1 / _2 / _3
  astronaut   Astronauta      70     mono_astronauta_1 / _2 / _3
  mago        Mago            80     mono_mago_1 / _2 / _3 / _4

Cada accesorio con variantes usa la misma lógica “diaria”: una variante por
día, estable hasta medianoche.


5. VARIANTE DIARIA (dailyRandom)
--------------------------------
Para mono limpio y accesorios con varias imágenes:

  seed = día actual (epoch day) XOR hash del accesorio
  → Misma imagen todo el día, cambia al día siguiente.

Splash, onboarding y placeholders usan DEFAULT_PULCRO = mono_pulcro_1.


6. COMPLETAR TAREAS Y RECOMPENSA
--------------------------------
Al marcar la última tarea del día (toggleTask):

  +1 banana
  +1 a la racha (streakCount)
  streakBroken = false, missedDaysCount = 0
  Cada 7 días de racha: +3 bananas extra (hito semanal)

Si se desmarca una tarea después de haber completado el día:
  Se revierte la banana, la racha -1, y el bono semanal si aplicaba.

Al completar el día:
  - Sonido pop al marcar cada tarea.
  - Overlay de fuego (FireCelebrationOverlay) + grito_mono.mp3.
  - Notificación de celebración.


7. RESET DIARIO (checkAndResetForNewDay)
----------------------------------------
Se ejecuta al iniciar el ViewModel y al simular día ganado (debug).

Al cambiar de día (lastResetDate != hoy):

  1. Evalúa AYER:
     - Si ayer completaste todo (streakCounted) → racha sana, missedDays = 0.
     - Si ayer era día de descanso → sin penalización.
     - Si ayer tenías tareas y no las completaste → racha = 0, streakBroken,
       missedDays + 1.
     - Si no hay tareas en la app → missedDays + 1 (mono se ensucia igual).

  2. Borra el estado “completado” de todas las tareas (nuevo día en blanco).

  3. Resetea flags del día: rewardGiven, streakCounted, streakBonus.

El mono puede amanecer sucio aunque ayer lo tuvieras limpio si fallaste.


8. TEXTO BAJO EL MONO (MainScreen)
----------------------------------
  Sin tareas              → "¡Agrega tareas para empezar!"
  Día de descanso         → "¡Hoy es día de descanso! 😎"
  Limpio hoy              → Frases motivacionales (TIPS_PHRASES, rotan).
  missedDays >= 2 o       → Frases de alerta fuerte (SUCIO2_PHRASES).
  streakBroken
  missedDays == 1         → Frases suaves (SUCIO1_PHRASES).
  Sucio con tareas        → "Hay tareas pendientes..."

Colores del texto van acorde (verde limpio, rojo/marrón sucio, etc.).


9. PANTALLA PRINCIPAL (layout del mono)
---------------------------------------
  - Caja 240×240 dp, imagen 220×220 dp.
  - Sombra radial debajo (Canvas).
  - Mono de oro: GoldMonkeyImage con pila_de_oro y brillos animados.
  - Motas de polvo: overlay encima si hay (DustMotesOverlay).
  - Tap en el mono: animación de limpieza (spray/paño), independiente del
    estado limpio/sucio de tareas.

Header: bananas (izq), icono mono (centro), racha fuego (der).


10. WIDGET
----------
Usa los mismos datos: isCleanToday, streakBroken, missedDaysCount,
equippedAccessory → MonkeyImageResolver.resolve(...).

Muestra la misma imagen de estado y las motas de polvo (sin limpieza).


11. ARCHIVOS PRINCIPALES
------------------------
  data/MonkeyStateManager.kt   — Lógica de tareas, racha, limpio/sucio, reset.
  ui/MonkeyImageResolver.kt    — Árbol de decisión de drawable.
  ui/MonkeyViewModel.kt        — Estado UI (MonkeyUiState), refreshState.
  ui/MainScreen.kt             — Dibuja mono, texto, celebración, tap limpieza.
  ui/GoldMonkeyImage.kt        — Mono de oro con brillos y pila.
  ui/ShopScreen.kt             — Compra y preview de accesorios.
  widget/MonkeyWidget.kt       — Mismo resolver en el widget.


12. DEBUG (panel amarillo, solo BuildConfig.DEBUG)
--------------------------------------------------
Panel en MainScreen. Controles de días/escudos: ver docs/escudos_de_pulcritud.md §10.

  Día perdido →  — Marca incompleto, avanza día, dispara reset (prueba escudo).
  Día ganado →   — Marca completado, avanza día (no consume escudo).
  +1/−1 escudo   — Ajusta contador (0–3).
  Reset prefs    — Borra preferencias y re-inicializa escudos.
  +100 bananas / +2h polvo — utilidades varias.


13. FLUJO RESUMIDO
------------------

  [App abre]
       │
       ▼
  checkAndResetForNewDay() ──¿cambió el día?──► evalúa ayer, limpia checks
       │
       ▼
  isCleanToday + streakBroken + missedDays + accesorio
       │
       ▼
  MonkeyImageResolver.resolve() ──► drawable del mono
       │
       ├── Usuario completa tareas ──► limpio + banana + racha ↑
       ├── Usuario no completa ──► sucio (mono_sucio_*)
       ├── Nuevo día sin completar ayer ──► missedDays ↑, posible mono_sucio_2+
       └── Equipa accesorio (si limpio) ──► variante con accesorio


14. PROGRESIÓN VISUAL DE SUCIEDAD
---------------------------------
  Tareas pendientes hoy        → mono_sucio_1
  Racha rota (día fallido)     → mono_sucio_2
  3 días perdidos acumulados   → mono_sucio_3
  4+ días perdidos             → estado extremo (cansado/enfermo/frustrado/llorando)

Completar el día resetea missedDaysCount y streakBroken a valores sanos.


================================================================================
  Relacionado: docs/motas_de_polvo.md (capa visual encima del mono)
================================================================================
