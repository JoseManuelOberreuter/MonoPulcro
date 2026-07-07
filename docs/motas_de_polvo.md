# Motas de polvo (pelusas) — Mono Pulcro

Resumen
-------
Las motas de polvo son un detalle visual de “engagement” que aparece sobre el
mono con el paso del tiempo. El usuario puede tocar el mono para limpiarlas
con una animación (spray + gotas + paño). Si había motas, al terminar recibe
+1 banana gratis. Sin motas, la animación de limpieza sigue disponible pero
sin recompensa.


1. APARICIÓN Y LÍMITE
---------------------
- Cada 2 horas reales transcurridas se añade 1 mota (máximo 5).
- La primera vez que se sincroniza solo se guarda la hora de referencia; la
  primera mota aparece tras pasar 2 horas desde ese momento.
- Al limpiar, el contador de motas se pone a 0 y el reloj de spawn se reinicia
  (vuelve a contar 2 horas para la siguiente mota).
- syncDustSpawns() se ejecuta al refrescar el estado de la app y al actualizar
  el widget.


2. POSICIONES (FIJAS, NO ALEATORIAS)
------------------------------------
Solo se guarda CUÁNTAS motas hay. Las posiciones y tamaños vienen de slots
predefinidos en DustMote.kt (DustMote.SLOTS), en este orden:

  Slot 1 — Hombro derecho     (x=0.72, y=0.32, 46 dp)
  Slot 2 — Flanco izquierdo   (x=0.30, y=0.54, 44 dp)
  Slot 3 — Flanco derecho     (x=0.66, y=0.52, 40 dp)
  Slot 4 — Pierna izquierda   (x=0.30, y=0.72, 32 dp)
  Slot 5 — Pierna derecha     (x=0.72, y=0.74, 60 dp)

xFrac / yFrac = centro de la mota en el cuadro del mono (0..1), sobre un
área de ~240×240 dp en pantalla principal.

Imagen: res/drawable/mota_polvo.png


3. DÓNDE SE VEN
----------------
- Pantalla principal: overlay encima del mono (mono limpio o sucio).
- Widget: mismas posiciones escaladas al tamaño del widget.
- El widget MUESTRA las motas pero NO permite limpiarlas desde ahí.


4. LIMPIEZA (TOCAR EL MONO)
----------------------------
Al tocar la imagen del mono:

  a) Spray (~3 s) + gotas de agua en 5 ráfagas (animación visual).
  b) Pausa breve, luego paño en zigzag (~1,5 s).
  c) Durante el paño, las motas y gotas se desvanecen suavemente.
  d) Si había motas al tocar:
       - Se borran del almacenamiento.
       - +1 banana.
       - Overlay “+1” con la banana.

Si no hay motas, la animación y los sonidos (spray + window_cleaning) se
reproducen igual, pero no hay banana al final.

Sonidos: assets/sounds/spray_bottle.mp3 → window_cleaning.mp3


5. PERSISTENCIA (SharedPreferences)
------------------------------------
Claves en MonkeyStateManager:

  dustMotesJson      — JSON con la lista canónica de motas (posiciones fijas).
  dustLastSpawnMs    — Última hora en que se generó una mota (epoch ms).
  dustCount          — Legacy; se migra a dustMotesJson si existe.

Constantes:
  MAX_DUST_MOTES = 5
  DUST_SPAWN_INTERVAL_MS = 7_200_000 (2 horas)


6. ARCHIVOS PRINCIPALES
-----------------------
  data/DustMote.kt              — Modelo + SLOTS de posiciones.
  data/MonkeyStateManager.kt   — Spawn, carga, guardado, recompensa.
  ui/DustMotesOverlay.kt        — (en MonkeyCleaningOverlay.kt) Dibuja motas.
  ui/MonkeyCleaningOverlay.kt   — Animación spray / gotas / paño.
  ui/MainScreen.kt              — Tap en mono, overlay, recompensa.
  ui/MonkeyViewModel.kt         — Estado UI y completeDustCleaning().
  widget/MonkeyWidget.kt        — Muestra motas en el widget.


7. DEBUG (panel amarillo en MainScreen)
---------------------------------------
  +2 horas — Avanza el tiempo simulado 2 horas y puede spawnear una mota.
  Reset    — Reinicia estado; útil para probar spawn desde cero.


8. FLUJO RESUMIDO
-----------------

  [App abre / refresh]
         │
         ▼
  syncDustSpawns() ──¿pasaron 2h desde último spawn?──► +1 mota (hasta 5)
         │
         ▼
  Usuario ve motas en mono (app + widget)
         │
         ▼
  Usuario toca mono ──► animación limpieza
         │
         ├── Con motas ──► rewardDustCleaning() ──► +1 banana, motas = 0
         └── Sin motas ──► solo animación


================================================================================
  Última actualización: según código en DustMote.SLOTS (5 slots, sin hombro izq.)
================================================================================
