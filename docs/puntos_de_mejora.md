# Puntos de mejora — Mono Pulcro

Documento vivo de mejoras técnicas, de producto y de documentación.
Sustituye el análisis anterior centrado en v1.0.10.

**Fecha:** Julio 2026  
**Versión analizada:** 1.2.3 (versionCode 19)  
**Alcance:** código + docs + landing + monetización existente (AdMob rewarded)

Pendientes concretos de producto también en [`todo.md`](todo.md).

---

## 1. Resumen ejecutivo

Mono Pulcro es un MVP maduro de hábitos de limpieza con gamificación local
(racha, bananas, escudos, tienda, motas, widget, notificaciones, AdMob
rewarded). El loop central está bien cerrado.

Los mayores retornos ahora **no** son “más mecánicas”, sino:

1. **Calidad y confianza** (tests, crash reporting, docs al día)
2. **Retención post-semana-1** (logros, historial, metas)
3. **Adquisición orgánica** (compartir racha, ASO, plantillas día 1)
4. **Pulido UX** (ajustes, i18n, splash, overlays de `todo.md`)

---

## 2. Deuda documental

| Acción | Por qué |
|---|---|
| Mantener README / INDEX alineados con features | Onboarding, shop, ads, escudos |
| Unificar precios/accesorios con el código | Evitar tablas contradictorias |
| Documentar rotación de imagen cada 3 h | Antes se decía “diaria” |
| Completar docs de tienda, persistencia, economía | Huecos de dominio |

Índice: [`INDEX.md`](INDEX.md).

---

## 3. Mejoras técnicas (Android)

### P0 — Estabilidad y regresiones

- **Tests unitarios** de `MonkeyStateManager` (ya admite `todayProvider` +
  `prefsOverride`):
  - `isCleanToday` (0 tareas / descanso / todas hechas)
  - `toggleTask` (loot, reversión, anti-doble-pago, `tripleChestReward`)
  - `checkAndResetForNewDay` multi-día + consumo de escudos
  - `syncDustSpawns` / `rewardDustCleaning`
  - shop chest (máx. 3/día, cancel vs complete)
- **CI Android** en GitHub Actions: `assembleDebug` + unit tests en PR
  (hoy solo hay workflow de Pages)
- **Crashlytics / Sentry** en release

### P1 — Arquitectura sostenible

- Extraer de `MonkeyStateManager` (~730 LOC):
  `TaskRepository`, `StreakEngine`, `EconomyStore`, `DustSpawner`, `ShieldService`
- SharedPreferences OK para MVP; si crece el estado → DataStore o Room
  con migración explícita
- Refrescar widget tras CRUD de tareas (hoy solo toggle / polvo / tienda)
- Deep link desde notificaciones a `task_edit/{id}`
- Revisar `android:allowBackup="true"` vs privacidad / restore inesperado

### P2 — Calidad de producto en código

- Strings hardcodeados → `strings.xml` (+ base EN)
- Unificar colores (`Theme.kt` vs hardcodes en MainScreen / widget)
- Accesibilidad TalkBack (`contentDescription` en imágenes/overlays)
- Pantalla de **Ajustes**: hora recordatorio, mute audio, reset progreso,
  enlace a privacidad
- Completar variantes de assets si alguna skin queda corta

Ver también [`calidad_y_testing.md`](calidad_y_testing.md).

---

## 4. Mejoras de producto / UX

### Ya en `todo.md`

- [ ] Splash con cara del mono
- [ ] Recuperar tareas del día anterior pagando bananas
- [ ] Overlay de confirmación de compra en tienda

### Retención (semana 2+)

- Logros visibles (1er día, racha 7/30, primera compra, primer escudo)
- Calendario / historial de días limpios (compartible)
- Meta semanal (“5 de 7 días”) con banana extra
- Mejor racha histórica + días limpios del mes
- Escudos visibles en header de Main (hoy solo en tienda)

### Activación día 1

- Plantillas de rutina (“Depto”, “Casa con niños”, “Fin de semana”)
  empaquetando `PredefinedTasks`
- Skip en onboarding
- Primera tarea sugerida en un tap

### Widget

- Mostrar bananas o progreso N/M tareas
- Acción rápida Glance (marcar 1 tarea / abrir tienda)
- Ver limitaciones actuales: [`estado_widget.md`](estado_widget.md)

### Notificaciones

- Deep link a tarea
- Recordatorio inteligente: “quedan 2 antes de las 21:00”
- Aviso post-racha-rota (además del overlay al abrir)

---

## 5. Monetización (AdMob ya existe — optimizar)

Estado actual:

- Triplicar bananas del cofre diario (rewarded, 1×/día vía `rewardDoubledToday`)
- Cofre de tienda: +5 bananas, máx. 3/día

Mejoras:

- Medir eCPR / fill rate; **no** añadir banners en Main (rompe el loop)
- Cap diario claro en UI (“3/3 cofres hoy”)
- Freemium cosmético solo tras tracción (packs temáticos de pago)
- No vender ventaja de racha con IAP (rompe fairness del hábito)

Detalle: [`tienda_y_anuncios.md`](tienda_y_anuncios.md) · [`economia.md`](economia.md).

---

## 6. Adquisición y distribución

- Play Store listing + ASO (hábitos, limpieza, checklist hogar)
- Botón “Compartir mi racha” (imagen mono + texto + link store)
- Actualizar landing `page/` con features reales (tienda, escudos, widget)
- Screenshots que muestren el mono sucio → limpio

Checklist: [`lanzamiento_play_store.md`](lanzamiento_play_store.md).

---

## 7. Funciones nuevas con potencial

### Alta prioridad

1. Plantillas de rutina — reduce abandono día 1
2. Compartir progreso / retos — adquisición orgánica
3. Recuperar día anterior con bananas — recuperación emocional
4. Recordatorios inteligentes

### Media prioridad

1. Calendario / historial visual
2. Widget interactivo
3. Pantalla de ajustes + mute

### Baja prioridad (mayor coste)

1. Sync en la nube / multi-dispositivo
2. iOS
3. Modo hogar / roommates (mono compartido)
4. IA para sugerir tareas

---

## 8. Roadmap sugerido (90 días)

```
Semana 1     Docs sync + tests críticos reset/racha/escudos
Semana 2–3   Crash reporting + CI + splash/cara mono
Semana 4     Plantillas de rutina + overlay compra
Mes 2        Compartir racha + logros/estadísticas básicas
Mes 2–3      Recuperar día anterior + ajustes
Mes 3+       Widget interactivo / modo hogar solo si hay tracción
```

---

## 9. Prioridad resumida

| Prioridad | Mejora | Impacto |
|---|---|---|
| 🔴 P0 | Tests + CI en GitHub | En marcha — ver [`calidad_y_testing.md`](calidad_y_testing.md) |
| 🔴 P0 | Mantener docs y listing al día | Menos regresiones / descubrimiento |
| 🟡 P1 | Plantillas + compartir racha | Activación + viralidad |
| 🟡 P1 | Logros / historial / ajustes | Retención |
| 🟡 P1 | Recuperar día (`todo.md`) | Recuperación post-fallo |
| 🟢 P2 | Refactor StateManager | Velocidad de desarrollo |
| 🟢 P2 | i18n + a11y + widget actions | Pulido / escala |
| 🟢 P2 | IAP cosméticos | Ingresos post-tracción |

---

## 10. Fuera de alcance (por ahora)

- Backend / cuentas / sync multi-dispositivo
- iOS
- IA para sugerir tareas
- Push remoto (FCM): las notificaciones locales bastan si se pulen

---

## 11. Referencias de código

- Dominio: `data/MonkeyStateManager.kt`
- UI: `ui/MainScreen.kt`, `ShopScreen.kt`, `MonkeyViewModel.kt`
- Ads: `ads/RewardedAdManager.kt`
- Widget: `widget/MonkeyWidget.kt`
- Arquitectura: [`ARCHITECTURE.md`](ARCHITECTURE.md)
- Índice docs: [`INDEX.md`](INDEX.md)
