# Tienda y anuncios — Mono Pulcro

## Resumen

La tienda (`ShopScreen`) vende cosméticos y objetos. La monetización usa
**AdMob** + **Google Play Billing** (cofres de bananas de pago):

1. **Rewarded — triplicar** el loot del cofre del día (Main).
2. **Rewarded — cofre de tienda**: ver anuncio → +10 bananas (máx. 3/día).
3. **Native advanced — Tareas diarias**: tarjeta bajo la lista de hoy en Main.
4. **IAP consumibles — cofres de bananas**: 3 packs de pago real en pestaña Objetos.

No hay banners ni interstitial. Offline-first: si un anuncio falla, el flujo
base de bananas sigue funcionando; el nativo simplemente no se muestra.
Los IAP requieren Play Store / productos activos en Console.

---

## 1. Pantalla de tienda

Ruta Compose: `shop`. Pestañas (tap o swipe):

| Pestaña | Contenido |
|---|---|
| Atuendos | Lista `ACCESSORIES` — comprar / equipar |
| Objetos | Cofre rewarded + Escudo (100 bananas) + 3 cofres IAP |

TopAppBar: bananas disponibles. Hint one-shot cuando el usuario puede
permitirse el accesorio más barato no poseído
(`shouldShowShopAffordHint` / `consumeShopAffordHint`).

---

## 2. Accesorios

Definidos en `MonkeyStateManager.ACCESSORIES`:

| ID | Nombre | Precio |
|---|---|---|
| glasses | Lentes | 10 |
| hat | Gorro | 20 |
| chaleco | Chaleco | 30 |
| crown | Corona | 40 |
| payaso | Payaso | 50 |
| vikingo | Vikingo | 60 |
| astronaut | Astronauta | 70 |
| mago | Mago | 80 |
| lazo | Lazo | 90 |
| vampiro | Vampiro | 100 |

- `buyAccessory(id)`: descuenta bananas, añade a `ownedAccessories`.
- `useAccessory(id)`: equipa (solo se ve si `isCleanToday`).
- Preview en tienda: `MonkeyImageResolver.previewForAccessory` (variante `_1`).

El id `gold` está migrado fuera de prefs (obsoleto).

---

## 3. Escudo en tienda

- Precio: `SHIELD_SHOP_PRICE = 100`
- Máximo inventario: `MAX_SHIELDS = 3`
- `buyShield()` con `commit()`; UI muestra “Tienes N/3” o “Máximo”

Reglas de consumo: [`escudos_de_pulcritud.md`](escudos_de_pulcritud.md).

---

## 4. Cofre de tienda (rewarded)

Constantes:

- `SHOP_CHEST_REWARD = 10`
- `MAX_SHOP_CHEST_OPENS_PER_DAY = 3`

Flujo:

```
requestShopChestAd()
  → beginShopChestAd()          // reserva intento (pending flag)
  → ShowRewardedAdForShopChest
  → onUserEarnedReward
      → completeShopChestReward()  // +5 bananas, opensToday++
  → cancel / fail
      → cancelShopChestAd()        // libera reserva sin premiar
```

Al reset diario se ponen a 0 `shopChestOpensToday` y el pending.

---

## 4b. Cofres IAP (Google Play Billing)

Productos consumibles (`BillingClient.ProductType.INAPP`). Catálogo local en
`billing/BananaChestCatalog.kt`; precios solo en Play Console.

| Product ID | Nombre UI | Bananas |
|---|---|---|
| `bananas_chest_small` | Cofre pequeño | 50 |
| `bananas_chest_medium` | Cofre mediano | 150 |
| `bananas_chest_xlarge` | Cofre grande | 400 |

Flujo:

```
BillingManager.start()
  → queryProductDetails (precios localizados)
  → queryPurchasesAsync (recuperar compras sin consumir)
Usuario toca comprar
  → launchBillingFlow
  → PURCHASED
  → grantPurchasedBananas(amount, purchaseToken)  // idempotente
  → consumeAsync
  → overlay de celebración (+N bananas)
```

Archivos: `billing/BillingManager.kt`, `billing/BananaChestCatalog.kt`.
Dependencia: `com.android.billingclient:billing-ktx:7.1.1`.
Permiso: `com.android.vending.BILLING`.

### Play Console (checklist)

1. Perfil de pagos / cuenta de comerciante activa.
2. **Monetizar con Play → Productos → Productos gestionados**.
3. Crear los 3 product IDs de arriba (compra única / consumible).
4. Activar cada producto; definir precios por país.
5. Subir un build con Billing a **prueba interna**.
6. Agregar testers + cuentas de **license testing** para compras sin cargo.

Sin productos activos + APK en un track de prueba, la UI muestra precio "…"
y no puede lanzar la compra.

## 5. Triplicar cofre del día (rewarded)

Tras la celebración de racha / cofre en Main:

```
beginChestFlow(baseBananas)
  → usuario abre cofre (reveal)
  → requestDoubleReward() si canOfferDouble
  → ShowRewardedAdForDouble
  → onAdRewardEarnedForDouble
      → tripleChestReward()   // total ×CHEST_AD_MULTIPLIER (3)
```

`tripleChestReward` exige:

- `rewardGivenToday == true`
- `rewardDoubledToday == false`
- `rewardBananasToday > 0`

Si el anuncio falla o se cierra sin reward: fase `AdUnavailable` /
sin crédito extra; el loot base ya estaba acreditado.

---

## 6. `RewardedAdManager`

Archivo: `ads/RewardedAdManager.kt`.

- Precarga con `RewardedAd.load`
- `show(onEarned, onDismissed, onFailedToShow)`
- Unit ID **test** si el APK es debuggable; **producción** en release
- Application ID en `AndroidManifest` (`APPLICATION_ID` meta-data)
- Permisos: `INTERNET`, `ACCESS_NETWORK_STATE`, `AD_ID`

Estados: `Idle` / `Loading` / `Ready` / `Failed`.

---

## 6b. Native advanced — Tareas diarias

Unidad AdMob: `ca-app-pub-5537054947047840/2685525587` (nombre: Tareas diarias).

Archivos:

- `ads/NativeAdLoader.kt` — carga + bind de assets
- `ui/DailyTasksNativeAd.kt` — Compose + `AndroidView`
- `res/layout/native_ad_tasks.xml` — plantilla compacta

Colocación: debajo de la sección de tareas en `MainScreen`, solo en vista
**Hoy**. Si falla la carga, no se reserva espacio.

Política:

- Atribución visible `"Anuncio"` (`strings.xml` / `ad_attribution`)
- AdChoices arriba a la derecha (`ADCHOICES_TOP_RIGHT`)
- Assets registrados en `NativeAdView` (headline, body, icon, CTA)
- `nativeAd.destroy()` en `DisposableEffect.onDispose`
- Debug usa unit de prueba Google `…/2247696110`

---

## 7. Efectos en ViewModel

`MonkeyUiEffect`:

- `ShowRewardedAdForDouble`
- `ShowRewardedAdForShopChest`

UI observa con `LaunchedEffect` y llama a `RewardedAdManager.show`.
Estado de overlay: `ChestRewardUiState` + `ChestRewardPhase`.

---

## 8. Archivos

| Archivo | Rol |
|---|---|
| `ui/ShopScreen.kt` | UI tienda + tabs + cofres IAP |
| `ui/MainScreen.kt` | Cofre día + botón duplicar |
| `ui/MonkeyViewModel.kt` | Efectos, estado de cofre, Billing |
| `data/MonkeyStateManager.kt` | buy*, double*, shop chest, IAP grant |
| `billing/BillingManager.kt` | Play Billing (query / buy / consume) |
| `billing/BananaChestCatalog.kt` | product IDs → bananas |
| `ads/RewardedAdManager.kt` | AdMob rewarded |
| `ads/NativeAdLoader.kt` | AdMob native (Tareas diarias) |
| `ui/DailyTasksNativeAd.kt` | UI nativo en Main |

---

Relacionado: [`racha_y_bananas.md`](racha_y_bananas.md) · [`economia.md`](economia.md) · [`puntos_de_mejora.md`](puntos_de_mejora.md)
