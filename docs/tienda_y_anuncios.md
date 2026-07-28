# Tienda y anuncios — Mono Pulcro

## Resumen

La tienda (`ShopScreen`) vende cosméticos y objetos. La monetización real
usa **AdMob rewarded** en dos flujos:

1. **Duplicar** el loot del cofre del día (Main, tras completar tareas).
2. **Cofre de tienda**: ver anuncio → +5 bananas (máx. 3/día).

No hay banners ni interstitial. Offline-first: sin anuncio, el flujo base
de bananas sigue funcionando.

---

## 1. Pantalla de tienda

Ruta Compose: `shop`. Pestañas (tap o swipe):

| Pestaña | Contenido |
|---|---|
| Atuendos | Lista `ACCESSORIES` — comprar / equipar |
| Objetos | Cofre rewarded + Escudo de Pulcritud (100 bananas) |

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

- `SHOP_CHEST_REWARD = 5`
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

## 5. Duplicar cofre del día (rewarded)

Tras la celebración de racha / cofre en Main:

```
beginChestFlow(baseBananas)
  → usuario abre cofre (reveal)
  → requestDoubleReward() si canOfferDouble
  → ShowRewardedAdForDouble
  → onAdRewardEarnedForDouble
      → doubleChestReward()   // +rewardBananasToday otra vez, flag doubled
```

`doubleChestReward` exige:

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
| `ui/ShopScreen.kt` | UI tienda + tabs |
| `ui/MainScreen.kt` | Cofre día + botón duplicar |
| `ui/MonkeyViewModel.kt` | Efectos y estado de cofre |
| `data/MonkeyStateManager.kt` | buy*, double*, shop chest |
| `ads/RewardedAdManager.kt` | AdMob |

---

Relacionado: [`racha_y_bananas.md`](racha_y_bananas.md) · [`economia.md`](economia.md) · [`puntos_de_mejora.md`](puntos_de_mejora.md)
