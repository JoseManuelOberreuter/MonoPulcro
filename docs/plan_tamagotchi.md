# Plan — Capa Tamagotchi (alimentar, acariciar, XP y niveles)

Planificación de una nueva capa de gamificación: poder alimentar al mono,
acariciarlo, ganar experiencia (XP) y subir de nivel. Documento de diseño,
aún no implementado.

---

## 1. Contexto y motivación

`docs/ideas_gamificacion.md` ya identificaba como debilidad de retención que
"no hay progresión de largo plazo más allá de bananas" y proponía como idea
Tier 3 "Niveles/XP del mono" (contador acumulado y no reseteable). Este plan
concreta esa idea agregando además dos interacciones directas con el mono
(alimentar, acariciar) en estilo Tamagotchi, que hoy no existen.

Decisiones de diseño ya acordadas:

1. **Sin decay** — alimentar y acariciar solo suman; no se introduce una
   nueva forma de "fallar" además de las tareas diarias existentes.
2. **Alimentar cuesta bananas** — nuevo sink de la economía.
3. **Subir de nivel es solo cosmético/narrativo** (título nuevo, sin efectos
   en economía ni desbloqueos obligatorios de tienda).
4. **Acariciar reusa el tap existente sobre el mono**: si hay motas de polvo,
   el tap limpia motas (comportamiento actual, sin cambios); si NO hay motas,
   el mismo tap acaricia (da XP) en vez de solo reproducir la animación sin
   recompensa como pasa hoy.

Esto extiende patrones que ya existen en el código en vez de inventar
mecanismos nuevos: los "contadores de por vida" (`bestStreakCount`,
`totalBananasEarned`, `totalMotesCleaned`, ver `docs/logros.md`) y el
catálogo de logros (`data/Achievement.kt` + `checkAchievements()`).

---

## 2. Diseño del sistema

### 2.1 XP y niveles

- Nueva key persistida `monkeyXp: Int` (nunca baja, igual que
  `totalBananasEarned`).
- El **nivel se deriva de la XP**, no se persiste aparte — mismo principio
  que "el aspecto del mono no se persiste, se calcula" (`ARCHITECTURE.md`).
  Nuevo archivo `data/MonkeyLevel.kt`, mismo patrón que `Achievement.kt`:
  catálogo fijo `MonkeyLevels.ALL` de `(level, xpRequired, title)`, con
  `MonkeyLevels.forXp(xp): MonkeyLevelInfo` (nivel actual, título, XP para el
  siguiente nivel, progreso 0..1).
- Curva sugerida (ritmo de semanas/meses, no días — comparable al de
  escudos/accesorios en `docs/economia.md`): niveles 1-10 con thresholds fijos
  en tabla (ej. 0, 20, 50, 100, 180, 280, 400, 550, 750, 1000 XP), niveles 11+
  por fórmula (`xp = 1000 + (level-10)*300`). Títulos narrativos simples
  ("Monito curioso" → "Mono pulcro veterano" → …), sin arte nuevo en v1.
- Fuentes de XP (todas ya son eventos existentes en `MonkeyStateManager`,
  solo se les agrega una línea):

| Fuente | XP sugerida | Dónde |
|---|---|---|
| Completar una tarea | `XP_PER_TASK = 2` | `toggleTask` (rama individual) |
| Completar el día (cofre) | `XP_PER_DAY_COMPLETE = 10` | `toggleTask` (rama `allTodayDone`) |
| Alimentar | `XP_PER_FEED = 5` | `feedMonkey()` (nuevo) |
| Acariciar | `XP_PER_PET = 3` | `petMonkey()` (nuevo) |

  La XP **nunca se resta**, ni al destildar una tarea — es de por vida, no un
  saldo (igual que `totalBananasEarned`).

### 2.2 Alimentar

- `fun feedMonkey(): FeedResult` en `MonkeyStateManager`, mismo estilo que
  `buyAccessory` / `rewardDustCleaning`:
  - Cuesta `FEED_COST = 3` bananas (ingreso libre ~2 banana/día según
    `docs/economia.md`; el costo no debe ahogar el resto de la economía).
  - Tope diario `MAX_FEEDS_PER_DAY = 3` vía nuevo contador `feedsToday`
    (mismo patrón que `shopChestOpensToday`), para que alimentar sea un
    ritual diario y no un botón para farmear XP con bananas acumuladas.
  - Falla si no alcanzan las bananas o si ya se agotó el tope de hoy.
  - Si tiene éxito: descuenta bananas, suma `XP_PER_FEED`, incrementa
    `feedsToday`.
  - `feedsToday` se resetea a 0 en `checkAndResetForNewDay()`, junto al
    reset existente de `KEY_SHOP_CHEST_OPENS_TODAY`.

### 2.3 Acariciar

- `fun petMonkey(): Int` (XP otorgada, 0 si no aplica) en
  `MonkeyStateManager`, gratis (sin costo de bananas).
- Tope diario `MAX_PETS_PER_DAY = 10` vía `petsToday` (mismo patrón,
  reseteado en el mismo punto que `feedsToday`) para que no sea spameable
  ahora que el tap "vacío" (sin motas) va a dar XP.
- En `MainScreen.kt`, el `clickable` del mono ya bifurca según
  `dustAtCleanStart = vm.dustMotesForCleaning()`:
  - Con motas → flujo actual sin cambios (limpieza + `completeDustCleaning()`).
  - Sin motas → nuevo flujo: animación de caricia liviana (ej. corazones
    subiendo, ~1s, sin arte nuevo) + `vm.petMonkey()`. Si el tope diario ya se
    alcanzó, no pasa nada (igual que hoy no pasa nada si no hay motas).

### 2.4 UI de nivel

- Reusar `StreakScreen.kt`: agregar una tercera pestaña a `StreakTab` (hoy
  `STREAK`, `ACHIEVEMENTS`) → `LEVEL("Nivel")`, mismo patrón de
  `HorizontalPager` + tabs pill. Muestra nivel actual, título, barra de
  progreso a la XP del siguiente nivel.
- Overlay de subida de nivel: nuevo `MonkeyUiEffect.ShowLevelUp(level, title)`
  en `MonkeyViewModel.kt`, consumido en `MainScreen.kt` con un composable
  nuevo `LevelUpOverlay` en `MonkeyCleaningOverlay.kt`, calcado de
  `AchievementUnlockedOverlay`.
- Botón "Alimentar": ícono cerca del mono en `MainScreen.kt`, deshabilitado
  si `bananas < FEED_COST` o `feedsToday >= MAX_FEEDS_PER_DAY`. Al tocar:
  `vm.feedMonkey()` → si hay éxito, overlay "+N XP" (nuevo `XpRewardOverlay`,
  mismo esqueleto que `BananaRewardOverlay`).
- Sonidos: reusar `SoundManager.playMonkeyCheer()` para alimentar/acariciar
  con éxito — no hace falta asset de audio nuevo.

### 2.5 ViewModel (`MonkeyViewModel.kt`)

- `MonkeyUiState` gana: `monkeyXp`, `monkeyLevel`, `monkeyLevelTitle`,
  `xpForNextLevel`, `feedsRemainingToday`, `petsRemainingToday`.
- `fun feedMonkey()` / `fun petMonkey()`: llaman al manager, si hay éxito
  reproducen sonido, `applyAchievementUnlocks()`, `refreshState()`,
  `updateWidget()`, emiten efecto de XP (y de level-up si corresponde).
- Detección de level-up: comparar `MonkeyLevels.forXp(xpAntes).level` vs
  `forXp(xpDespués).level` tras cada fuente de XP y emitir `ShowLevelUp` si
  cambió — mismo patrón que la detección de logros nuevos.

### 2.6 Logros nuevos (opcional, esfuerzo trivial)

Agregar 2-3 entradas a `Achievements.ALL` / `checkAchievements()`: ej.
"Primer bocado" (alimentar 1 vez), "Nivel 5", "Nivel 10".

---

## 3. Persistencia — nuevas keys

| Clave | Tipo | Notas |
|---|---|---|
| `monkeyXp` | Int | XP de por vida, nunca baja |
| `feedsToday` | Int | 0–3, reset diario |
| `petsToday` | Int | 0–10, reset diario |

Constantes nuevas en el companion object de `MonkeyStateManager`:
`XP_PER_TASK`, `XP_PER_DAY_COMPLETE`, `XP_PER_FEED`, `XP_PER_PET`,
`FEED_COST`, `MAX_FEEDS_PER_DAY`, `MAX_PETS_PER_DAY`, `KEY_MONKEY_XP`,
`KEY_FEEDS_TODAY`, `KEY_PETS_TODAY`.

---

## 4. Archivos a tocar

- `data/MonkeyStateManager.kt` — XP, `feedMonkey()`, `petMonkey()`, reset
  diario de `feedsToday`/`petsToday`, constantes, extender
  `checkAchievements()`.
- `data/MonkeyLevel.kt` (nuevo) — catálogo de niveles/títulos + `forXp()`.
- `data/Achievement.kt` — 2-3 logros nuevos (opcional).
- `ui/MonkeyViewModel.kt` — estado de nivel/XP en `MonkeyUiState`,
  `feedMonkey()`, `petMonkey()`, efecto `ShowLevelUp`.
- `ui/MainScreen.kt` — branch del tap del mono (limpiar vs acariciar), botón
  "Alimentar", overlays de XP y level-up.
- `ui/MonkeyCleaningOverlay.kt` — `XpRewardOverlay` y `LevelUpOverlay`
  nuevos, calcados de `BananaRewardOverlay` / `AchievementUnlockedOverlay`.
- `ui/StreakScreen.kt` — tercera pestaña "Nivel".
- `docs/persistencia.md`, `docs/economia.md`, `docs/ideas_gamificacion.md`,
  `docs/todo.md`, `docs/INDEX.md` — documentar el sistema una vez
  implementado.
- Tests: `app/src/test/kotlin/.../data/MonkeyStateManagerTest.kt` (o archivo
  nuevo) — XP acumulada, cálculo de nivel en los bordes de la tabla,
  `feedMonkey()` (éxito, sin bananas, tope diario agotado), `petMonkey()`
  (éxito, tope diario agotado), reset diario de `feedsToday`/`petsToday`.

---

## 5. Fuera de alcance (v1)

- Indicador de nivel/XP en el widget.
- Decay de hambre/ánimo.
- Ítems de comida comprables en tienda (alimentar es un botón simple con
  costo fijo).
- Notificación push al subir de nivel.
- Backfill retroactivo de XP para usuarios existentes (arrancan en 0, igual
  que pasó con logros).

---

## 6. Verificación al implementar

- `gradle :app:testDebugUnitTest` — nuevos tests unitarios sobre XP, niveles,
  feed/pet y topes diarios.
- `gradle :app:assembleDebug` — build.
- Prueba manual: alimentar hasta agotar el tope diario y hasta quedarse sin
  bananas (botón debe deshabilitarse), acariciar con y sin motas de polvo
  presentes (el tap debe seguir limpiando motas cuando las hay), subir de
  nivel y ver el overlay + pestaña "Nivel" en `StreakScreen`, y verificar que
  `checkAndResetForNewDay()` resetea `feedsToday`/`petsToday` al otro día
  (panel de debug, `debugAdvanceDay`).

---

Relacionado: [`ideas_gamificacion.md`](ideas_gamificacion.md) ·
[`racha_y_bananas.md`](racha_y_bananas.md) · [`logros.md`](logros.md) ·
[`motas_de_polvo.md`](motas_de_polvo.md) · [`persistencia.md`](persistencia.md)
