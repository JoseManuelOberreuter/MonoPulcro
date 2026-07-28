# Calidad y testing — Mono Pulcro

## Estado actual

- Tests unitarios JVM en `app/src/test/` (JUnit + Robolectric).
- CI Android: [`.github/workflows/android.yml`](../.github/workflows/android.yml) en cada push/PR a `main`.
- Crashes en producción: **Android Vitals** en Play Console (sin SDK en la app). Subir `mapping.txt` en cada release.
- ProGuard/R8 activo en release (`isMinifyEnabled = true`).

---

## Ejecutar tests en local

Con Gradle en el PATH (o Android Studio → Run tests en `*Test.kt`):

```bash
gradle :app:testDebugUnitTest
```

Compilar debug:

```bash
gradle :app:assembleDebug
```

Mismo conjunto que en CI: `testDebugUnitTest` + `assembleDebug`.

---

## Qué cubren los tests (v1)

| Archivo | Contenido |
|---|---|
| `data/DustMoteTest.kt` | `dustMotesForCount` |
| `data/MonkeyStateManagerTest.kt` | `isCleanToday`, toggle/revertir, doble cofre, reset diario, escudo, motas |

`MonkeyStateManager` admite `todayProvider`, `prefsOverride` y `chestLootProvider` para asserts deterministas.

Ampliar después: tienda, shop chest, ViewModel, widget, notificaciones.

---

## CI en GitHub

Workflow **Android CI**: JDK 17, Android SDK, Gradle 8.9, luego:

`gradle :app:testDebugUnitTest :app:assembleDebug`

El workflow de Pages ([`pages.yml`](../.github/workflows/pages.yml)) sigue siendo independiente.

---

## Observabilidad

- **Vitals:** no requiere código; distribución por Play + `mapping.txt` opcional pero recomendado.
- Crashlytics/Sentry: opcional, no implementado.

---

## Manual QA (checklist corta)

- Completar día → celebración → duplicar ad (debug)
- Fallar día con escudos → overlay protección
- Fallar sin escudos → overlay racha rota
- Motas + limpieza + banana
- Widget tras toggle y tras reboot (reprograma alarms)
- Cofre tienda 3/3 y día siguiente resetea contador

---

Relacionado: [`persistencia.md`](persistencia.md) · [`puntos_de_mejora.md`](puntos_de_mejora.md) · [`lanzamiento_play_store.md`](lanzamiento_play_store.md)
