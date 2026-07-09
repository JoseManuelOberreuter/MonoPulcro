# Puntos de mejora — Mono Pulcro

Análisis de producto, UX y técnica para la app Android de hábitos de limpieza del hogar.

**Fecha:** Julio 2026  
**Versión analizada:** 1.0.10 (versionCode 11)

---

## Resumen

Mono Pulcro tiene un loop de gamificación sólido (tareas → mono limpio → bananas → tienda → racha) y está más avanzada de lo que refleja el README. Los principales gaps están en **distribución**, **viralidad**, **retención a largo plazo** y **pulido pre-lanzamiento**, no en la mecánica central.

---

## 2. Retención después de la semana 1

**Situación actual:** El loop funciona bien al inicio, pero puede volverse repetitivo cuando el usuario ya compró accesorios y domina la mecánica.

**Mejoras:**

- Metas semanales (ej. "completa 5 de 7 días").
- Sistema de logros visibles (primer día, racha 7, racha 30, primera compra).
- Nuevos accesorios por temporadas o eventos.
- Integrar assets ya preparados pero no conectados (ej. `mono_payaso_1/2/3.png`).
- Progreso visible a largo plazo: días limpios del mes, mejor racha histórica.
- Mini-desafíos semanales con recompensa extra de bananas.

---



## 3. UX y pulido de producto


| Área          | Problema                                  | Mejora sugerida                                                       |
| ------------- | ----------------------------------------- | --------------------------------------------------------------------- |
| Configuración | No hay pantalla de ajustes                | Hora del recordatorio, silenciar sonidos, reiniciar progreso          |
| Accesibilidad | Algunas imágenes sin `contentDescription` | Mejorar soporte TalkBack                                              |
| Localización  | Textos hardcodeados en Kotlin             | Mover a `strings.xml`; preparar inglés                                |
| Tema visual   | Colores inline vs `Theme.kt`              | Unificar diseño; considerar modo oscuro                               |
| Onboarding    | Sin opción "Saltar"                       | Reducir abandono de usuarios impacientes                              |
| Widget        | Solo informativo                          | Permitir marcar tareas desde el widget                                |
| Documentación | README desactualizado                     | Reflejar tienda, notificaciones, tour, motas de polvo, precios reales |


---



## 4. Viralidad y adquisición

**Situación actual:** No hay compartir logros, invitaciones ni contenido social. Para apps de hábitos, esto suele ser el mayor motor de nuevos usuarios.

**Mejoras:**

- Botón "Compartir mi racha" (imagen del mono + texto tipo "Llevo 14 días con Mono Pulcro").
- Reto de 7 días compartible con amigos o familia.
- Deep link a Play Store desde la imagen compartida.
- Calendario visual de cumplimiento (estilo contribuciones de GitHub), fácil de compartir.

---



## 5. Técnico y calidad

**Situación actual:** Sin tests automatizados, sin CI Android, sin crash reporting. Persistencia en SharedPreferences + Gson, adecuada para MVP pero limitada a largo plazo.

**Mejoras:**

- Tests unitarios para lógica crítica: reset diario, racha, compras, motas de polvo.
- CI Android (build + tests en cada PR).
- Crash reporting (Firebase Crashlytics, Sentry u otro).
- Migración de datos más robusta si crece la complejidad del estado.
- Eliminar código muerto (ej. `completeOnboarding()` no usado).
- Completar variantes de assets faltantes (ej. astronauta `_2/_3`).
- Revisar `android:allowBackup="true"` vs expectativas de privacidad del usuario.

---



## 6. Monetización (opcional, post-lanzamiento)

**Situación actual:** Sin ingresos. La moneda (bananas) es 100 % virtual y local.

**Opciones que encajan con el producto:**


| Modelo             | Descripción                                               |
| ------------------ | --------------------------------------------------------- |
| Freemium           | App gratis + accesorios premium o packs temáticos         |
| Suscripción ligera | Estadísticas avanzadas, temas, recordatorios inteligentes |
| Ads no intrusivos  | Banner fuera de la pantalla principal                     |


**Recomendación:** No monetizar al inicio. Priorizar retención y reviews. Introducir freemium con cosméticos cuando haya tracción.

---



## 7. Funciones nuevas con potencial de atraer usuarios



### Alta prioridad

1. **Modo hogar / pareja / roommates** — Tareas asignadas por persona; mono compartido o individual. La limpieza es un problema social; mucha gente busca apps para repartir tareas.
2. **Plantillas de rutina** — "Departamento pequeño", "Casa con niños", "Limpieza fin de semana". Reduce fricción del primer día.
3. **Compartir progreso y retos** — Tarjeta visual para WhatsApp/Instagram. Adquisición orgánica sin pagar ads.
4. **Recordatorios inteligentes** — "Te quedan 2 tareas antes de las 21:00"; aviso cuando el mono se ensucia tras perder la racha.



### Media prioridad

1. **Calendario / historial visual** — Ver qué días se cumplieron las tareas.
2. **Widget interactivo** — Completar tareas sin abrir la app.
3. **Integración con asistentes** — "Ok Google, ¿qué me falta limpiar hoy?" (nicho, pero memorable en marketing).



### Baja prioridad (mayor coste)

1. Sincronización en la nube / multi-dispositivo.
2. Versión iOS (duplica audiencia, mucho esfuerzo).
3. IA para sugerir tareas según tamaño o tipo de hogar.

---



## Roadmap sugerido (60 días)

```
Semana 1–2  → Publicar en Play Store + ASO + enlace en landing
Semana 3–4  → Plantillas predefinidas + compartir racha
Mes 2       → Logros/estadísticas + integrar accesorio payaso
Mes 3       → Modo hogar compartido (si hay tracción)
```

---



## Prioridad resumida


| Prioridad | Área                         | Impacto                |
| --------- | ---------------------------- | ---------------------- |
| 🔴 Alta   | Publicar en Play Store       | Descubrimiento         |
| 🔴 Alta   | Plantillas de rutina         | Reduce abandono día 1  |
| 🔴 Alta   | Compartir racha              | Viralidad orgánica     |
| 🟡 Media  | Logros y estadísticas        | Retención largo plazo  |
| 🟡 Media  | Pantalla de ajustes          | Pulido y confianza     |
| 🟡 Media  | Tests + CI + crash reporting | Estabilidad pre-escala |
| 🟢 Baja   | Monetización                 | Ingresos post-tracción |
| 🟢 Baja   | iOS / nube                   | Crecimiento futuro     |


---



## Referencias en el código

- Lógica de estado: `app/src/main/kotlin/com/josem/monopulcro/data/MonkeyStateManager.kt`
- UI principal: `app/src/main/kotlin/com/josem/monopulcro/ui/MainScreen.kt`
- Tienda y accesorios: `app/src/main/kotlin/com/josem/monopulcro/ui/ShopScreen.kt`
- Landing: `page/index.html`
- README (desactualizado): `README.md`

