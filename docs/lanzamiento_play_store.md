# Lanzamiento Play Store — Mono Pulcro

Checklist orientativa para publicar / actualizar en Google Play.
Versión documentada: **1.2.3** (versionCode 19).

---

## 1. Build release

- [ ] `versionCode` / `versionName` incrementados en `app/build.gradle.kts`
- [ ] Firma release configurada (no commitear keystores ni passwords)
- [ ] `./gradlew :app:assembleRelease` o App Bundle (`bundleRelease`)
- [ ] ProGuard: verificar que AdMob / Gson / Glance no se rompen
      (reglas en `proguard-rules.pro` si hace falta)

---

## 2. AdMob y privacidad

- [ ] Application ID, rewarded y native unit de **producción** (no test) en release
- [ ] Declaración de Ad ID / Data safety form en Play Console
- [ ] Política de privacidad publicada: `page/privacidad.html` (GitHub Pages)
- [ ] Enlace a privacidad accesible desde la ficha y, idealmente, desde la app

---

## 3. Permisos (Manifest)

| Permiso | Uso |
|---|---|
| `POST_NOTIFICATIONS` | Recordatorios (runtime API 33+) |
| `RECEIVE_BOOT_COMPLETED` | Reprogramar alarms / widget |
| `INTERNET` / `ACCESS_NETWORK_STATE` | Ads |
| `AD_ID` | AdMob |

No pedir permisos de ubicación ni contactos (la app no los usa).

---

## 4. Ficha de Play Store (ASO)

- [ ] Título / short description con keywords (hábitos, limpieza, hogar)
- [ ] Descripción larga alineada con features reales: tareas, racha, bananas,
      escudos, tienda, widget, notificaciones
- [ ] Screenshots: mono sucio → limpio, tienda, widget, celebración
- [ ] Icono y feature graphic coherentes con el mono
- [ ] Categoría: Productividad / Estilo de vida
- [ ] Content rating questionnaire

---

## 5. Backup y datos

- `android:allowBackup="true"`: decidir si se mantiene o se restringe
  (`dataExtractionRules` / `fullBackupContent`) para evitar restores raros.

---

## 6. Calidad mínima pre-lanzamiento

- [ ] Smoke manual: onboarding → crear tarea → completar día → tienda → widget
- [ ] Probar denegar notificaciones (app no debe crashear)
- [ ] Probar sin red (cofre base OK; ads degradan con `AdUnavailable`)
- [ ] Ideal: tests unitarios críticos + Crashlytics
  (ver [`calidad_y_testing.md`](calidad_y_testing.md))

---

## 7. Post-lanzamiento

- Monitorizar crashes y ANRs
- Reviews: responder y priorizar fricción día 1
- Medir fill rate de rewarded antes de añadir más ads

Roadmap: [`puntos_de_mejora.md`](puntos_de_mejora.md).

---

Relacionado: [`ARCHITECTURE.md`](ARCHITECTURE.md) · [`tienda_y_anuncios.md`](tienda_y_anuncios.md) · [`../page/privacidad.html`](../page/privacidad.html)
