# Acariciar (y plan a futuro de XP/niveles) — Mono Pulcro

Resumen
-------
De la capa "Tamagotchi" descrita en [`plan_tamagotchi.md`](plan_tamagotchi.md)
solo está **implementado** el gesto de acariciar al mono. XP, niveles y
alimentar siguen siendo diseño, no código (ver §2 más abajo).


1. ACARICIAR (`petMonkey()`) — implementado
------------------------------------------
- Gratis, sin costo de bananas, sin XP (v1 minimal).
- Reusa el **mismo tap** sobre el mono que ya disparaba la limpieza de motas
  de polvo (`MainScreen.kt`, el `clickable` del Box del mono):
  - Si hay motas de polvo → comportamiento sin cambios (limpieza + banana
    por mota, ver [`motas_de_polvo.md`](motas_de_polvo.md)).
  - Si **no** hay motas → el tap acaricia al mono: `MonkeyPettingOverlay`
    (corazones subiendo, ~1 s, ícono Material `Favorite`, sin arte nuevo) +
    `MonkeyViewModel.petMonkey()`.
- Tope diario `MAX_PETS_PER_DAY = 10` (contador `petsToday`, reset diario en
  `checkAndResetForNewDay()` junto al resto de flags del día). Si el tope ya
  se alcanzó, el tap no hace nada (igual que hoy no pasa nada si no hay
  motas).
- Sonido: reusa `SoundManager.playMonkeyCheer()`.

Archivos: `data/MonkeyStateManager.kt` (`petMonkey()`, `petsToday`,
`petsRemainingToday`), `ui/MonkeyViewModel.kt` (`petMonkey()`,
`MonkeyUiState.petsRemainingToday`), `ui/MainScreen.kt` (branch del tap),
`ui/MonkeyCleaningOverlay.kt` (`MonkeyPettingOverlay`).


2. FUERA DE ALCANCE DE ESTA IMPLEMENTACIÓN (sigue siendo plan)
----------------------------------------------------------------
Lo demás en [`plan_tamagotchi.md`](plan_tamagotchi.md) sigue sin
implementar: XP (`monkeyXp`), niveles (`MonkeyLevel.kt`), alimentar
(`feedMonkey()`, costo en bananas), overlays de XP/level-up, pestaña "Nivel"
en `StreakScreen`, y los logros `tamagotchi_first`/`level_5`/`level_10`. Si
se retoma esa capa completa, `plan_tamagotchi.md` sigue siendo la referencia
de diseño.


Relacionado: [`plan_tamagotchi.md`](plan_tamagotchi.md) ·
[`estado_mono_principal.md`](estado_mono_principal.md) ·
[`motas_de_polvo.md`](motas_de_polvo.md) · [`persistencia.md`](persistencia.md) ·
[`INDEX.md`](INDEX.md)
