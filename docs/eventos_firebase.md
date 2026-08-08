# Eventos de Firebase Analytics — Mono Pulcro

## Resumen

`firebase-analytics` (ver [`persistencia.md`](persistencia.md) /
`app/build.gradle.kts`) está instrumentado a través de un único wrapper,
`analytics/AnalyticsLogger.kt`. Cubre activación (onboarding → primera
tarea), uso recurrente (tareas, cofres) y monetización (tienda, compras).

No hay backend propio — los eventos van directo a Firebase. La app sigue
siendo offline-first: si no hay logger inicializado o no hay red, `log()`
simplemente no hace nada (el SDK de Firebase encola/reenvía por su cuenta;
`AnalyticsLogger` no agrega su propio manejo de errores).

---

## 1. `AnalyticsLogger`

Archivo: `analytics/AnalyticsLogger.kt`. Único punto de entrada a
`FirebaseAnalytics` — ninguna otra clase importa el SDK de Firebase
directamente, salvo `BillingManager` (ver sección 5).

```kotlin
object AnalyticsLogger {
    fun init(context: Context)                                   // MainActivity.onCreate
    fun log(event: String, params: Map<String, Any> = emptyMap())
    fun logTaskCompleted(taskId: String)                          // task_completed (+ first_task_completed)
}
```

- `init` guarda `FirebaseAnalytics.getInstance(...)` con el `applicationContext`.
- `log` traduce `Map<String, Any>` a `Bundle` (`Int`/`Long`/`Double`/`Boolean`
  tipados, el resto como `String`) y llama a `logEvent`.
- Nombres de evento/parámetro: `snake_case`, constantes en `AnalyticsLogger.Events`
  y `AnalyticsLogger.Params`. Para el evento estándar de compra se usan las
  constantes de `FirebaseAnalytics.Event`/`Param` directamente (ver sección 5).
- Todas las llamadas se hacen desde `MonkeyViewModel` o `Composable`s de
  navegación/UI — **nunca** desde `MonkeyStateManager` (esa clase se
  mantiene libre de dependencias de Android/SDKs externos).

---

## 2. Onboarding

| Evento | Cuándo | Parámetros | Hook |
|---|---|---|---|
| `onboarding_start` | Se compone `OnboardingScreen` | — | `ui/OnboardingScreen.kt` — `LaunchedEffect(Unit)` |
| `onboarding_complete` | Usuario toca "Agregar mi primera tarea" | — | `ui/OnboardingScreen.kt` — `onClick` del botón de la última página |

`OnboardingScreen` solo se compone cuando `AppNavigation` arranca en
`ROUTE_ONBOARDING` (`startOnboarding = !onboardingCompleted && !shouldShowMainTour`
en `MainActivity.kt`), así que `onboarding_start` ya queda acotado a usuarios
nuevos sin lógica extra.

---

## 3. Tareas

| Evento | Cuándo | Parámetros | Hook |
|---|---|---|---|
| `task_created` | Se guarda una tarea nueva | `days_count` (`task.scheduledDays.size`) | `ui/MonkeyViewModel.kt` — `addTask()`, tras `manager.addTask(task)` |
| `task_completed` | Una tarea pasa de incompleta a completa | `task_id` | `ui/MonkeyViewModel.kt` — `toggleTask()`, cuando `wasDone == false` |
| `first_task_completed` | La primera vez en la vida de la app que se dispara `task_completed` | — | `AnalyticsLogger.logTaskCompleted()` — flag propio en `SharedPreferences("analytics_prefs")`, independiente de `monkey_prefs` |

`task_completed` se basa en `wasDone` (estado de la tarea individual antes
del toggle), **no** en el valor de retorno de `manager.toggleTask()` — ese
booleano indica si se ganó la recompensa del día completo (cofre), que es un
evento distinto. Al desmarcar una tarea no se loguea nada.

---

## 4. Cofres y cosméticos

| Evento | Cuándo | Parámetros | Hook |
|---|---|---|---|
| `chest_opened` | Se revela el cofre del día (loot ya calculado) | `bananas` | `ui/MonkeyViewModel.kt` — `onChestRevealed()` |
| `cosmetic_unlocked` | Compra de accesorio en tienda exitosa | `accessory_id`, `price` (de `MonkeyStateManager.ACCESSORIES`) | `ui/MonkeyViewModel.kt` — `buyAccessory()`, dentro del `if (manager.buyAccessory(id))` |

`chest_opened` se loguea una sola vez por apertura, en el reveal — no al
iniciar la animación del cofre (`beginChestFlow`) ni al triplicar el loot vía
anuncio (`onAdRewardEarnedForDouble`, sin evento propio por ahora).

---

## 5. Tienda y monetización

| Evento | Cuándo | Parámetros | Hook |
|---|---|---|---|
| `store_opened` | Se navega a `ShopScreen` | `source = "shop_button"` | `MainActivity.kt` — `onOpenShop`, antes de `navController.navigate(ROUTE_SHOP)` |
| `purchase_started` | Usuario toca comprar un cofre IAP | `product_id` | `ui/MonkeyViewModel.kt` — `buyBananaChest()`, antes de `billingManager.launchPurchase` |
| `purchase` (evento estándar GA4) | Compra confirmada y otorgada | `currency`, `value` (de `ProductDetails.oneTimePurchaseOfferDetails`), `transaction_id` = `purchaseToken`, `product_id` | `billing/BillingManager.kt` — `processPurchase()`, solo si `onPurchaseReady(...)` devuelve no-nulo |

`purchase` usa `FirebaseAnalytics.Event.PURCHASE` /
`FirebaseAnalytics.Param.{CURRENCY,VALUE,TRANSACTION_ID}` directamente (no
pasan por `AnalyticsLogger.Events`/`Params`) para que el valor monetario
alimente reportes de ingresos out-of-the-box. `value` sale de
`priceAmountMicros / 1_000_000.0`; `currency` de `priceCurrencyCode` — ambos
del `ProductDetails` consultado por `BillingManager`, no de
`billing/BananaChestCatalog.kt` (que solo mapea `productId` → bananas, sin
precio real). El chequeo de `onPurchaseReady(...) != null` evita loguear
compras ya procesadas (reintentos de `queryPurchasesAsync`).

---

## 6. Archivos

| Archivo | Rol |
|---|---|
| `analytics/AnalyticsLogger.kt` | Wrapper único de Firebase Analytics; catálogo de eventos/parámetros |
| `MainActivity.kt` | `AnalyticsLogger.init`; `store_opened` |
| `ui/OnboardingScreen.kt` | `onboarding_start`, `onboarding_complete` |
| `ui/MonkeyViewModel.kt` | `task_created`, `task_completed`, `chest_opened`, `cosmetic_unlocked`, `purchase_started` |
| `data/MonkeyStateManager.kt` | Fuente de `task.scheduledDays`, `ACCESSORIES` (precio) — sin dependencia de Analytics |
| `billing/BillingManager.kt` | `purchase` (evento estándar GA4) |

---

## 7. Pendiente / fuera de alcance

- `achievement_unlocked` (al entrar un logro a `achievementQueue` en
  `ui/MainScreen.kt`) — mismo patrón que el resto, no implementado aún.
- DebugView de Firebase Console para verificar eventos en dispositivo:
  `adb shell setprop debug.firebase.analytics.app com.josem.monopulcro`.

---

Relacionado: [`tienda_y_anuncios.md`](tienda_y_anuncios.md) ·
[`racha_y_bananas.md`](racha_y_bananas.md) · [`logros.md`](logros.md) ·
[`onboarding_y_tour.md`](onboarding_y_tour.md)
