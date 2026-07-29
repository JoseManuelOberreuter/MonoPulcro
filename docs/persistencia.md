# Persistencia — Mono Pulcro

## Resumen

Todo el estado vive en SharedPreferences `monkey_prefs`
(`MonkeyStateManager.PREFS_NAME`). Serialización JSON con Gson para listas.
No hay Room ni backend.

El constructor acepta `todayProvider` y `prefsOverride` (útil para tests).

---

## 1. Claves de dominio

### Racha y bananas

| Clave | Tipo | Default / notas |
|---|---|---|
| `streakCount` | Int | 0 |
| `bananas` | Int | 0 (≥ 0) |
| `rewardGivenToday` | Bool | false |
| `rewardBananasToday` | Int | loot del día (revertir / duplicar) |
| `rewardDoubledToday` | Bool | ya se duplicó con ad |
| `streakCountedToday` | Bool | hoy sumó racha |
| `streakBonusGiven` | Bool | hito ×7 en el loot de hoy |
| `streakBroken` | Bool | afecta imagen del mono |
| `missedDaysCount` | Int | días fallidos acumulados |
| `lastResetDate` | String | `yyyy-MM-dd` |

### Tareas

| Clave | Tipo | Notas |
|---|---|---|
| `tasksJson` | String | `List<Task>` en JSON |
| `done_<taskId>` | Bool | completado hoy; se borra en reset |
| `tasksViewMode` | String | `today` \| `week` |

### Accesorios

| Clave | Tipo | Notas |
|---|---|---|
| `ownedAccessories` | StringSet | ids comprados |
| `equippedAccessory` | String | id equipado; vacío = ninguno |

Migración al init: si `gold` está owned/equipped → se elimina.

### Escudos

| Clave | Tipo | Notas |
|---|---|---|
| `shieldsCount` | Int | 0–3 |
| `shieldsInitialized` | Bool | grant inicial de 3 (one-shot) |
| `shieldMilestonesClaimed` | StringSet | `"7"`, `"30"`, … |
| `lastShieldProtectedDate` | String | día ya protegido |
| `pendingShieldUsedMessage` | Bool | overlay pendiente |
| `shieldsUsedAccumulator` | Int | usos en el hueco actual |
| `pendingStreakBrokenMessage` | Bool | overlay racha rota |
| `pendingBrokenStreakCount` | Int | racha perdida (animación) |
| `pendingBrokenShieldsUsed` | Int | escudos usados antes de romper |

### Motas de polvo

| Clave | Tipo | Notas |
|---|---|---|
| `dustMotesJson` | String | lista canónica (slots fijos) |
| `dustLastSpawnMs` | Long | epoch ms último spawn |
| `dustCount` | Int | **legacy**; se migra a JSON |

### Tienda / ads

| Clave | Tipo | Notas |
|---|---|---|
| `shopChestOpensToday` | Int | 0–3; reset diario |
| `pendingShopChestAd` | Bool | reserva antes del anuncio |
| `shopAffordHintConsumed` | Bool | hint one-shot de “puedes comprar” |

### Onboarding / tour

| Clave | Tipo | Notas |
|---|---|---|
| `onboardingDone` | Bool | onboarding o tour completado |
| `mainTourPending` | Bool | tour de Main pendiente |

### Debug

| Clave | Tipo | Notas |
|---|---|---|
| `debugDayOffset` | Int | días sumados a `LocalDate.now()` |

---

## 2. Constantes relacionadas

```
MAX_DUST_MOTES = 5
DUST_SPAWN_INTERVAL_MS = 7_200_000   // 2 h
MAX_SHIELDS = 3
INITIAL_SHIELDS = 3
SHIELD_SHOP_PRICE = 100
SHOP_CHEST_REWARD = 10
MAX_SHOP_CHEST_OPENS_PER_DAY = 3
```

---

## 3. Escritura: `apply` vs `commit`

- La mayoría de mutaciones UI usan `apply()`.
- Reset diario, escudos (init/consumo/compra) y shop chest usan `commit()`
  para visibilidad inmediata ante app + widget concurrentes.

---

## 4. Modelo `Task` (JSON)

```kotlin
data class Task(
    id: String,
    name: String,
    scheduledDays: List<Int>,      // ISO 1=Lun … 7=Dom
    notificationEnabled: Boolean,
    notificationHour: Int,
    notificationMinute: Int,
)
```

---

## 5. Robustez

- Parseo JSON defensivo → lista vacía si falla.
- `debugResetAllPrefs()` hace `clear()` + `ensureShieldsInitialized()` + reset.
- Backup Android: `android:allowBackup="true"` en el Manifest
  (ver riesgos en [`puntos_de_mejora.md`](puntos_de_mejora.md)).

---

Relacionado: [`racha_y_bananas.md`](racha_y_bananas.md) · [`escudos_de_pulcritud.md`](escudos_de_pulcritud.md) · [`motas_de_polvo.md`](motas_de_polvo.md)
