# Calidad y testing — Mono Pulcro

## Estado actual

- **Sin** carpetas `test/` / `androidTest/` con tests de la app.
- CI de GitHub Actions: solo publica la landing (`page/`) — no compila Android.
- Sin crash reporting en release.
- ProGuard/R8 activo en release (`isMinifyEnabled = true`).

---

## 1. Qué testear primero (unit)

`MonkeyStateManager` ya es testeable sin device:

```kotlin
MonkeyStateManager(
    context,
    todayProvider = { fixedDate },
    prefsOverride = inMemoryOrRobolectricPrefs,
)
```

Casos prioritarios:

| Área | Escenarios |
|---|---|
| `isCleanToday` | 0 tareas; descanso; parcial; todas hechas |
| `toggleTask` | loot 1–3; hito ×7; revertir; no doble pago |
| `doubleChestReward` | ok / ya duplicado / sin reward |
| `checkAndResetForNewDay` | mismo día; +1 incompleto; multi-día; descanso |
| Escudos | consume; agota; idempotencia `lastShieldProtectedDate`; hitos |
| Dust | primer sync sin mota; +1 cada 2 h; máx 5; reward |
| Shop chest | begin / complete / cancel; tope 3 |

---

## 2. Tests de UI / instrumentados (después)

- Tour / onboarding smoke
- Navegación Main ↔ Shop ↔ TaskEdit
- Widget: al menos que `provideGlance` no lance con prefs vacías

---

## 3. CI sugerido

```
on: pull_request, push
jobs:
  - ./gradlew :app:testDebugUnitTest
  - ./gradlew :app:assembleDebug
```

Opcional: lint (`lintDebug`) cuando el baseline esté limpio.

---

## 4. Observabilidad

- Integrar Firebase Crashlytics o Sentry en el flavor release.
- No loguear IDs de AdMob / PII.

---

## 5. Manual QA (checklist corta)

- Completar día → celebración → duplicar ad (debug unit)
- Fallar día con escudos → overlay protección
- Fallar sin escudos → overlay racha rota
- Motas + limpieza + banana
- Widget tras toggle y tras reboot (reprograma alarms)
- Cofre tienda 3/3 y día siguiente resetea contador

---

Relacionado: [`persistencia.md`](persistencia.md) · [`puntos_de_mejora.md`](puntos_de_mejora.md) · [`lanzamiento_play_store.md`](lanzamiento_play_store.md)
