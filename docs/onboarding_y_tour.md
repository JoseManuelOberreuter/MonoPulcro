# Onboarding y tour — Mono Pulcro

## Resumen

Hay dos experiencias de primera vez:

1. **Onboarding** (`OnboardingScreen`) — pager antes de Main si el usuario
   no ha completado onboarding ni tiene tour pendiente.
2. **Tour de Main** (`MainScreenTourOverlay`) — spotlight sobre UI real
   cuando `mainTourPending && !onboardingCompleted`.

Flags en prefs: `onboardingDone`, `mainTourPending`
(ver [`persistencia.md`](persistencia.md)).

---

## 1. Arranque (`MainActivity`)

```
startOnboarding = !onboardingCompleted && !shouldShowMainTour
```

- Splash Compose → NavHost.
- Si `startOnboarding` → ruta `onboarding`.
- Si no → `main` (el tour puede mostrarse encima si está pendiente).

---

## 2. Onboarding (2 páginas)

| # | Tema | Copy (resumen) |
|---|---|---|
| 1 | Bienvenida | App de hábitos de limpieza |
| 2 | Reencuadre | Las tareas son reales (no un juego); el mono solo refleja tu progreso |

CTA final ("Empezar") y botón **"Saltar"** (visible en toda página no-final,
arriba a la derecha) llaman ambos a `vm.seedFirstTasks()` y después
`onFinished()` → navega directo a `ROUTE_MAIN`. **Ya no pasa por
`TaskEditScreen`**: `seedFirstTasks()` crea las 3 tareas de
`PredefinedTasks.quickSuggestions` (programadas los 7 días) si
`allTasks.isEmpty()`, loguea `tasks_seeded` y marca el tour de Main
pendiente. El formulario manual (`ROUTE_TASK_NEW`) queda solo para
tareas siguientes, o si el usuario borra las 3 sembradas y agrega otra
(`TaskEditScreen` sigue precargando sugerencia + día de hoy en ese caso,
ver [`tareas_y_calendario.md`](tareas_y_calendario.md)).

Reducido de 5 a 2 páginas, agregado el skip, y reemplazado el formulario
manual por siembra automática en 2026-08 tras detectar que
`onboarding_complete`/`task_created` tenían menos eventos que cualquier
otro evento del funnel — la mayoría de los usuarios abandonaba durante el
carrusel pasivo o el formulario en blanco, antes de completar una tarea
real. Ver [`analisis_marketing_ago2026.md`](analisis_marketing_ago2026.md).

---

## 3. Tour de Main (7 pasos)

Orden (`MainTourStep`):

1. **BANANAS** — contador
2. **SHOP** — botón tienda
3. **STREAK** — racha
4. **MONKEY** — mono / polvo
5. **ADD_TASK** — agregar tareas
6. **VIEW_MODE** — hoy vs semana
7. **TASKS** — lista de hoy

`completeMainTour()` limpia `mainTourPending` y pone `onboardingDone = true`.

**No bloquea interacción.** El overlay (`MainScreenTourOverlay`) es puramente
visual — no tiene gesture-blocker de pantalla completa, así que el usuario
puede tocar la UI real (marcar una tarea, abrir la tienda, etc.) en
cualquier momento del tour, no solo al terminarlo. Cambiado en 2026-08:
antes `interactionLocked` incluía `showTour`, y el usuario no podía marcar
su primera tarea recién sembrada hasta cerrar el tour de 7 pasos — justo lo
opuesto de lo que buscaba la siembra automática. Ver
`MainScreen.kt` — `interactionLocked` / `rewardFlowActive`.

---

## 4. Hint de tienda

Independiente del tour: la primera vez que el usuario puede pagar el
accesorio más barato no poseído, Main puede destacar el botón de tienda
(`shouldShowShopAffordHint`).

---

## 5. Archivos

| Archivo | Rol |
|---|---|
| `ui/OnboardingScreen.kt` | Pager inicial |
| `ui/MainScreenTourOverlay.kt` | Spotlight + pasos |
| `ui/MainScreen.kt` | Anclas del tour + hint shop |
| `data/MonkeyStateManager.kt` | Flags y complete* |
| `MainActivity.kt` | Decisión de ruta inicial |

---

Relacionado: [`tareas_y_calendario.md`](tareas_y_calendario.md) · [`tienda_y_anuncios.md`](tienda_y_anuncios.md)
