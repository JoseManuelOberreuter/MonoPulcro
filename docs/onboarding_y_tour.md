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

## 2. Onboarding (5 páginas)

| # | Tema | Copy (resumen) |
|---|---|---|
| 1 | Bienvenida | App de hábitos de limpieza |
| 2 | Bananas | Completa el día y gana recompensa |
| 3 | Racha | No la rompas |
| 4 | Pelusas | Toca el mono para limpiar y ganar banana extra |
| 5 | Tienda | Gasta bananas en accesorios |

CTA final: agregar primera tarea (`onAddFirstTask` → normalmente navega a
crear tarea y marca tour pendiente según flujo del ViewModel / Main).

No hay botón “Saltar” (mejora en [`puntos_de_mejora.md`](puntos_de_mejora.md)).

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
