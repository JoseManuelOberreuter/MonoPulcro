# Audio — Mono Pulcro

## Resumen

Feedback sonoro vía `audio/SoundManager` (singleton). Combina **SoundPool**
(sonidos cortos precargados) y **MediaPlayer** (clips más largos / secuencias).

No hay ajuste de volumen ni mute en UI (mejora: pantalla de ajustes).

---

## 1. Assets (`app/src/main/assets/sounds/`)

| Archivo | Método | Cuándo |
|---|---|---|
| `campanitas_intro.mp3` | `playIntroJingle()` | Splash |
| `pop_tarea.mp3` | `playTaskPop()` | Marcar tarea |
| `cash_register.mp3` | `playCashRegister()` | Abrir cofre / recompensa |
| `grito_mono.mp3` | `playMonkeyCheer()` | Celebración de racha |
| `spray_bottle.mp3` | parte de `playCleaningSequence()` | Inicio limpieza |
| `window_cleaning.mp3` | parte de `playCleaningSequence()` | Paño / limpieza |

---

## 2. Detalles de implementación

- SoundPool: max 4 streams; usage `ASSISTANCE_SONIFICATION`.
- Fallo al cargar un asset → id 0 (no crashea; simplemente no suena).
- `playCleaningSequence` y el intro usan corrutinas en `Dispatchers.IO`.

---

## 3. Archivos

- `audio/SoundManager.kt`
- Llamadas desde `SplashScreen`, `MainScreen` / ViewModel según acción

---

Relacionado: [`motas_de_polvo.md`](motas_de_polvo.md) · [`onboarding_y_tour.md`](onboarding_y_tour.md)
