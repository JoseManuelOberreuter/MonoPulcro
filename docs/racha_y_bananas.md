# Racha y bananas — Mono Pulcro

## Resumen

La racha (streak), las bananas y los Escudos de Pulcritud son el núcleo
de la gamificación:

- **Racha** → días consecutivos completando **todas** las tareas del día.
- **Bananas** → moneda virtual. Se gana al completar el día (loot del cofre),
  en hitos de racha, limpiando motas, duplicando con anuncio o abriendo el
  cofre de tienda. Se gasta en la tienda.
- **Escudos** → consumibles (máx. 3). Protegen la racha en el reset diario.
  No completan tareas ni generan loot. Doc: [`escudos_de_pulcritud.md`](escudos_de_pulcritud.md).

Toda la lógica vive en `data/MonkeyStateManager.kt` sobre SharedPreferences
(`monkey_prefs`). No hay backend.

---

## 1. Variables persistidas

Ver tabla completa en [`persistencia.md`](persistencia.md). Claves clave:

| Clave | Significado |
|---|---|
| `streakCount` | Racha actual |
| `bananas` | Saldo (≥ 0) |
| `rewardGivenToday` | Ya se pagó el cofre hoy |
| `rewardBananasToday` | Monto del loot (para revertir / duplicar) |
| `rewardDoubledToday` | Ya se usó el anuncio de duplicar |
| `streakCountedToday` | Hoy sumó a la racha |
| `streakBroken` / `missedDaysCount` | Estado visual del mono |
| `shieldsCount` | Escudos 0–3 |

---

## 2. Cómo se gana la racha y el loot (`toggleTask`)

1. Se invierte `done_<id>`.
2. ¿Todas las tareas de **hoy** completadas?

**Caso A — Última tarea y aún no se pagó hoy:**

```
nuevaRacha   = streakCount + 1
esHito       = nuevaRacha % 7 == 0
lootCofre    = random(1..3) + (3 si esHito)   → 1–3 o 4–6
bananas     += lootCofre
streakCount  = nuevaRacha
rewardGivenToday / streakCountedToday = true
streakBroken = false, missedDays = 0
→ claimShieldMilestonesIfNeeded(nuevaRacha)
→ ViewModel dispara celebración + flujo de cofre (posible duplicar con ad)
```

**Caso B — Desmarcar tras completar el día:**

- Resta `rewardBananasToday` exacto, racha −1, limpia flags de recompensa.
- Si vuelve a marcar todo, se tira un **nuevo** random.

**Caso C — Toggle intermedio:** no toca racha ni bananas.

---

## 3. Reset diario (`checkAndResetForNewDay`)

Si `lastResetDate` ya es hoy → no-op. Si no:

1. Evalúa cada día desde `lastResetDate` hasta ayer (completo / descanso /
   incompleto + escudo / incompleto sin escudo).
2. Borra todos los `done_<taskId>`.
3. Resetea flags del día: reward, streakCounted, bonus, rewardBananas,
   **rewardDoubled**, **shopChestOpensToday**, pending shop chest ad.
4. Guarda `lastResetDate = hoy`.

Matices: día de descanso mantiene racha sin sumar; huecos multi-día pueden
consumir varios escudos; las bananas **nunca** se pierden al fallar.

---

## 4. Fuentes y gastos de bananas

| Fuente | Cantidad | Dónde |
|---|---|---|
| Cofre del día | +1 a +3 | `toggleTask` |
| Hito racha (×7) | +3 extra (total 4–6) | `toggleTask` |
| Duplicar cofre (ad) | total ×3 (extra = 2× loot base) | `tripleChestReward` |
| Limpiar motas | +1 | `rewardDustCleaning` |
| Cofre tienda (ad) | +10 | `completeShopChestReward` (máx. 3/día) |
| Debug | +100 | panel debug |

| Gasto | Cantidad | Dónde |
|---|---|---|
| Escudo de Pulcritud | 100 | `buyShield` |
| Accesorio | precio | `buyAccessory` |

### Precios de accesorios (código actual)

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

Balance resumido: [`economia.md`](economia.md). Tienda/ads: [`tienda_y_anuncios.md`](tienda_y_anuncios.md).

---

## 5. Flujo UI — celebración y cofre

Al marcar la última tarea:

1. `StreakCelebrationOverlay` (fuego, contador de racha) → CTA “Abrir cofre”
2. `ChestCelebrationOverlay` (cofre, burst de bananas, “+N”)
3. Opcional: botón **duplicar** con rewarded ad (`ShowRewardedAdForDouble`)
4. CTA “¡Seguir!” → home

El loot ya está acreditado al completar el día; el overlay lo revela.
El duplicado solo suma si `onUserEarnedReward` confirma.

---

## 6. Relación racha ↔ imagen del mono

Ver [`estado_mono_principal.md`](estado_mono_principal.md):

- `streakBroken` → `mono_sucio_2`
- `missedDays` 3 / 4+ → `mono_sucio_3` / extremos
- Completar el día → limpio, contadores sanos

---

## 7. Anti-exploit

- `rewardGivenToday` bloquea doble pago del cofre base.
- `rewardBananasToday` permite reversión exacta.
- `rewardDoubledToday` bloquea duplicar dos veces el mismo día.
- Nuevo random al re-completar tras desmarcar.
- Piso de bananas en 0.
- Sin tareas → mono sucio y `missedDays` sube.
- Día de descanso: ni suma ni rompe racha.

---

## 8. Archivos involucrados

- `data/MonkeyStateManager.kt` — toggle, reset, escudos, double, shop chest
- `ui/MonkeyViewModel.kt` — celebración, efectos AdMob, escudos
- `ui/MainScreen.kt` — overlays de racha / cofre / racha rota / escudo
- `ui/ShopScreen.kt` — gasto de bananas
- `ads/RewardedAdManager.kt` — carga/show de anuncios
- `widget/` — refleja racha/estado

---

## 9. Flujo resumido

```
[Marcar última tarea] → loot + racha↑ → overlay racha → overlay cofre
                                         └─ opcional: ad duplicar

[Nuevo día] checkAndResetForNewDay
  ├── completo / descanso → racha intacta
  ├── incompleto + escudo → consume 1
  └── incompleto sin escudo → racha=0, streakBroken, overlay
```

---

## 10. Futuro (no implementado)

- Acreditar bananas solo al abrir el cofre (hoy se acreditan al completar).
- XP / niveles del mono.
- Recuperar día anterior pagando bananas (ver `todo.md`).

---

Relacionado: [`escudos_de_pulcritud.md`](escudos_de_pulcritud.md) · [`motas_de_polvo.md`](motas_de_polvo.md) · [`tienda_y_anuncios.md`](tienda_y_anuncios.md) · [`puntos_de_mejora.md`](puntos_de_mejora.md)
