# Análisis de marketing — agosto 2026

Snapshot de la primera campaña de adquisición paga (Google Ads UAC) cruzado con AdMob, Firebase Analytics y Google Play Console. Generado el 2026-08-17 a partir de exports manuales; no se actualiza solo — para repetir este análisis en el futuro, volver a exportar los 4 reportes y comparar contra estas cifras.

## Fuentes y período

| Fuente | Rango cubierto | Export |
|---|---|---|
| Google Ads (series temporales) | 8–17 ago 2026 | CSV descargado de la consola de Ads |
| AdMob (reporte por día) | 21 jul – 16 ago 2026 | CSV descargado de la consola de AdMob (UTF-16) |
| Firebase Analytics (eventos) | Ventana no explícita (~equivalente a la campaña) | Vista "Eventos" de Firebase, columna Índice temático |
| Google Play Console | Últimos 28 días vs. 28 días anteriores | Vista "Descripción general" |

## Resumen ejecutivo

La campaña de Google Ads está funcionando bien en su propio terreno: el CPI bajó de CLP 62 a CLP ~16 en seis días y el volumen escala rápido. **El problema no está en la adquisición ni en la monetización — está en la activación.** De 311 usuarios nuevos (Firebase), solo 3 completaron alguna vez una tarea (0,96%), y Play Console confirma el mismo patrón desde otra fuente: se pierden usuarios (7,5/día) casi tan rápido como se adquieren (14/día), y el DAU promedio (5,43) es ínfimo comparado con el volumen de instalaciones. Seguir escalando el gasto en Ads ahora mismo compra instalaciones que en su gran mayoría no llegan a usar el producto.

Además, hay una acción de mantenimiento urgente y sin relación con marketing: `billing-ktx` está en 7.1.1 y Google exige v8+ para publicar actualizaciones a partir del **31 de agosto de 2026**.

## 1. Google Ads — campaña de instalación

Datos 11–17 ago (8–10 ago sin actividad):

| Día | Clics | Impresiones | Costo (CLP) | Instalaciones | CTR | CPI (CLP) |
|---|---|---|---|---|---|---|
| 11 ago | 667 | 5.593 | 4.157 | 67 | 11,9% | 62,1 |
| 12 ago | 182 | 8.041 | 2.449 | 55 | 2,3% | 44,5 |
| 13 ago | 289 | 7.295 | 2.367 | 85 | 4,0% | 27,9 |
| 14 ago | 386 | 8.497 | 2.162 | 97 | 4,5% | 22,3 |
| 15 ago | 837 | 13.927 | 2.227 | 131 | 6,0% | 17,0 |
| 16 ago | 1.413 | 21.224 | 2.243 | 139 | 6,7% | 16,1 |
| 17 ago* | 869 | 13.855 | 1.377 | 80 | 6,3% | 17,2 |

\*Día parcial (fecha del análisis).

**Totales:** 4.643 clics, 78.432 impresiones, CLP 16.982, 654 instalaciones. CPI promedio ~CLP 26.

- El gasto diario está tope-ado (~CLP 2.200–2.450) desde el día 12; el crecimiento de clics/impresiones/instalaciones con presupuesto plano es la señal típica de que el algoritmo salió de la fase de aprendizaje.
- La conversión clic→instalación bajó de ~30% (días 12-13) a ~9-10% (días 16-17) mientras el CTR subía — al escalar el alcance, entra tráfico de menor intención, aunque el CPI total sigue mejorando porque el CPC cae más rápido.

## 2. AdMob — monetización con ads

Total del período (21 jul – 16 ago, 26 días): **USD 0,85** en ingresos. Volumen muy bajo (impresiones de un solo o doble dígito la mayoría de los días) — el eCPM diario es ruidoso y no debe leerse como tendencia confiable a este volumen.

Últimos 6 días (11–16 ago, coinciden con el arranque fuerte de Ads):

| Día | Ingresos | eCPM | Solicitudes | % coincidencia | % publicación | Impresiones | Clics |
|---|---|---|---|---|---|---|---|
| 11 ago | 0,07 | 2,73 | 45 | 100% | 57,8% | 26 | 1 |
| 12 ago | 0,01 | 0,42 | 32 | 100% | 78,1% | 25 | 1 |
| 13 ago | 0,04 | 1,55 | 59 | 79,7% | 55,3% | 26 | 3 |
| 14 ago | 0,07 | 2,63 | 63 | 81,0% | 49,0% | 25 | 3 |
| 15 ago | 0,03 | 0,46 | 174 | 79,3% | 39,9% | 55 | 2 |
| 16 ago | 0,05 | 0,33 | 535 | 52,5% | 50,5% | 142 | 3 |

- Las solicitudes de anuncio se multiplicaron por 12 (45→535) justo cuando explotó la instalación paga, pero el % de coincidencia cayó a 52,5% (mínimo del histórico) y el eCPM a USD 0,33–0,46 (también mínimos). Más volumen, pero de peor calidad.
- Descalce instalaciones vs. impresiones de ads: el 16 ago hubo 139 instalaciones nuevas (Ads) pero solo 142 impresiones de anuncio *en total ese día* — ~1 impresión por instalación nueva, cuando un usuario activo debería generar bastantes más a lo largo del día. Es la primera señal de que los usuarios nuevos no están llegando a las pantallas donde se disparan anuncios.
- ROI día 1 irrelevante en esta etapa: CLP 15.605 gastados en Ads (11–16 ago) generaron USD 0,27 en AdMob. Normal para una app gratis en esta ventana — lo que importa es la retención, no el ROAS de día 1.
- Varias columnas del export (ARPU, ARPDAU, DAU, Usuarios que vieron un anuncio) vienen vacías — probablemente por volumen insuficiente para que AdMob las calcule, o falta seleccionar esas dimensiones al exportar.

## 3. Firebase Analytics — funnel de activación

Ventana con 311 usuarios totales, 2.401 eventos:

| Evento | Usuarios | % sobre 311 | Eventos/usuario |
|---|---|---|---|
| first_open | 310 | 99,7% | 1,00 |
| session_start | 297 | 95,5% | 1,54 |
| screen_view | 291 | 93,6% | 2,12 |
| user_engagement | 267 | 85,9% | 2,63 |
| **task_completed** | **3** | **0,96%** | 16,00 |
| app_remove | 114 | 36,7% | 1,36 |
| ad_reward | 12 | 3,9% | 1,92 |
| chest_opened | 2 | 0,6% | 7,50 |
| cosmetic_unlocked | 1 | 0,3% | 12,00 |
| store_opened | 2 | 0,6% | 45,00 |

**Hallazgo central:** el funnel se mantiene sano hasta `user_engagement` (86-100% de usuarios), y colapsa específicamente en `task_completed` — la acción núcleo del loop de hábito (mono, racha, bananas). Solo 3 de 311 usuarios la completaron alguna vez. `app_remove` en 37% de los usuarios confirma abandono temprano alto. `store_opened` y `chest_opened` tienen eventos/usuario altísimos con casi ningún usuario único — probablemente cuentas de prueba/dev, no señal orgánica.

## 4. Google Play Console — KPIs de publicación

Últimos 28 días vs. 28 días anteriores:

| Métrica | Valor | Delta |
|---|---|---|
| Adquisiciones de dispositivos | 403 | >+999% |
| Primer acceso de dispositivos | 20 | +43% |
| Dispositivos activos por mes | 28 | +180% |
| Ingresos / ARPPU / compradores | sin datos | — |

Tendencias de KPI (promedio diario, últimos 28 días):

| Métrica | Promedio | Delta |
|---|---|---|
| Adquisiciones de usuarios | 14/día | >+999% |
| Pérdida de usuarios | 7,5/día | >+999% |
| Instalaciones totales | 397 (vs. 384 período anterior) | ↑ |
| Dispositivos activos | 44,4 | +696% |
| DAU | 5,43 | +187% |
| MAU | 18,3 | +182% |
| Visitantes en ficha de Play Store | 34 | >+999% |
| Adquisiciones desde la ficha | 3,57 | +567% |
| % conversión desde la ficha | 13,1% | −9,5pp |

- **Gap entre Ads y Play Console:** Ads reporta 654 instalaciones en la semana; Play Console solo confirma ~400 adquisiciones de dispositivo reales en una ventana más larga (28 días). Una diferencia de este tamaño (Ads >60% por encima de lo que Play Console valida) amerita revisar contra qué evento está optimizando la campaña de Ads — si el objetivo de conversión es más laxo que un `first_open` real, se está pagando por "instalaciones" que Play Store no termina de confirmar.
- **Pérdida de usuarios (7,5/día) es más de la mitad de la adquisición (14/día).** Coincide con el 37% de `app_remove` visto en Firebase — dos fuentes independientes confirmando el mismo problema.
- La caída de 9,5pp en conversión desde la ficha de Play Store, con tráfico a la ficha +999%, es esperable: al meter tráfico pagado masivo entra gente de menor intención, diluyendo la tasa de conversión. No es alarmante por sí sola, pero refuerza que el tráfico nuevo es de menor calidad promedio.

## Conclusión

El cuello de botella no es adquisición ni monetización — es **activación**. La app está pagando por instalar usuarios que en su enorme mayoría nunca completan una tarea y se van antes de generar valor (ni gamificación, ni impresiones de ads, ni compras).

## Acciones recomendadas (priorizadas)

1. **Urgente / sin relación con marketing:** actualizar `billing-ktx` de 7.1.1 a v8+ (idealmente v9) antes del **31 de agosto de 2026** — Google bloquea la publicación de actualizaciones por debajo de v8 desde esa fecha (extensión posible hasta el 1 de noviembre si se solicita a tiempo). Afecta `app/build.gradle.kts:85` y el módulo `billing/` (cofres de bananas).
2. **No escalar más el gasto de Ads** hasta resolver la activación — cada instalación nueva tiene ~1% de probabilidad de generar valor real hoy.
3. **Revisar el onboarding/tour** (`MonkeyStateManager`, flags de onboarding — ver `docs/onboarding_y_tour.md`) para identificar la fricción entre abrir la app y completar la primera tarea. Hipótesis a validar: ¿hay una tarea de ejemplo precargada? ¿el tour explica claramente la acción "completar tarea"? ¿el usuario entiende que debe configurar tareas antes de que el mono pueda estar limpio?
4. **Instrumentar un evento intermedio** (p. ej. `task_created` o interacción con el tour) entre `first_open` y `task_completed` para ubicar con más precisión dónde se cae el usuario.
5. Revisar el objetivo de conversión configurado en la campaña de Google Ads, dado el gap con las cifras de Play Console.

## Preguntas abiertas

- ¿A qué evento está optimizando exactamente la campaña de Google Ads (first_open, in-app purchase, evento custom)?
- ¿El 37% de `app_remove` es consistente con benchmarks previos de la app (pre-campaña), o es un salto asociado al tráfico pagado?
- ¿`store_opened`/`chest_opened` con pocos usuarios pero muchos eventos son cuentas de prueba conocidas? Si no, vale la pena investigarlas.
