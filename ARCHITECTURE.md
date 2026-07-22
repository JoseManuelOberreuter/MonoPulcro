# Arquitectura del Proyecto

## Resumen Ejecutivo

**Mono Pulcro** es una aplicación Android nativa orientada a hábitos de limpieza del hogar. El usuario gestiona tareas diarias; al completarlas, mantiene una racha, gana bananas (moneda virtual) y el mono mascota refleja el progreso visualmente. Si falla, el mono se ensucia de forma progresiva.

El paradigma arquitectónico es **cliente monolítico offline-first**: toda la lógica de negocio y el estado viven en el dispositivo. No hay backend propio ni sincronización remota. La persistencia es local (`SharedPreferences` + Gson). La UI sigue **MVVM** con Jetpack Compose, un único `ViewModel` como orquestador y capas satélite para widget, notificaciones locales, audio y anuncios recompensados.

---

## Tech Stack y Herramientas

| Categoría | Tecnología / Librería | Uso / Propósito |
|---|---|---|
| Lenguaje | Kotlin 2.0 | Código de la aplicación |
| Plataforma | Android (minSdk 26, target/compileSdk 35) | Runtime y APIs del sistema |
| Build | Gradle + AGP 8.5 | Compilación y empaquetado |
| UI | Jetpack Compose + Material 3 | Pantallas declarativas |
| Navegación | Navigation Compose | Rutas entre onboarding, main, edición de tareas y tienda |
| Estado UI | ViewModel + StateFlow / SharedFlow | Estado reactivo y efectos one-shot |
| Persistencia | SharedPreferences + Gson | Estado del mono, tareas, rachas y bananas |
| Widget | Glance AppWidget | Widget de pantalla de inicio |
| Notificaciones | AlarmManager + NotificationCompat | Recordatorios locales (sin push remoto) |
| Monetización | Google Mobile Ads (AdMob) | Anuncios rewarded para duplicar recompensa |
| Audio | SoundPool (SoundManager) | Feedback sonoro de acciones |
| Landing | HTML / CSS / JS estático (`page/`) | Sitio público y política de privacidad |
| CI | GitHub Actions (`.github/`) | Automatización del repositorio |

---

## Estructura de Directorios

```
MonoPulcro/
├── app/                              # Módulo Android único
│   └── src/main/
│       ├── AndroidManifest.xml       # Activity, receivers, permisos, AdMob
│       ├── kotlin/com/josem/monopulcro/
│       │   ├── MainActivity.kt       # Entry point, splash, NavHost, permisos
│       │   ├── ads/                  # RewardedAdManager (AdMob)
│       │   ├── audio/                # SoundManager
│       │   ├── data/                 # Modelos y MonkeyStateManager (dominio + persistencia)
│       │   ├── notifications/        # Canales, schedulers y BroadcastReceiver
│       │   ├── ui/                   # Compose screens, ViewModel, theme, overlays
│       │   └── widget/               # Glance widget y actualización periódica
│       ├── res/                      # Drawables, layouts, values, xml del widget
│       └── assets/sounds/            # Recursos de audio
├── docs/                             # Documentación técnica de dominio
├── page/                             # Landing estática (GitHub Pages)
├── img/                              # Assets de referencia del mono
├── gradle/                           # Wrapper de Gradle
├── build.gradle.kts                  # Plugins raíz
└── settings.gradle.kts               # Nombre del proyecto e include :app
```

Carpetas clave del código Kotlin:

| Paquete | Responsabilidad |
|---|---|
| `data/` | Modelos (`Task`, `DustMote`), catálogo de tareas y `MonkeyStateManager` (reglas de racha, bananas, reset diario, tienda) |
| `ui/` | Pantallas Compose, `MonkeyViewModel`, resolución de imagen del mono y tema |
| `widget/` | Presentación Glance y sincronización con el estado local |
| `notifications/` | Programación y entrega de notificaciones locales |
| `ads/` | Carga y visualización de anuncios rewarded |
| `audio/` | Reproducción de efectos de sonido |

---

## Patrón de Arquitectura y Capas

La app organiza el código en capas claras, sin inyección de dependencias externa ni repositorios abstractos: el tamaño del MVP favorece un `StateManager` concreto y un `ViewModel` central.

### 1. Presentación (UI)

- Pantallas Compose: `SplashScreen`, `OnboardingScreen`, `MainScreen`, `TaskEditScreen`, `ShopScreen`.
- Componentes auxiliares: overlays de tour, limpieza de motas, tema Material 3.
- `MonkeyImageResolver` deriva el drawable del mono a partir del estado (limpio/sucio, racha, accesorios).
- Observa `StateFlow` del ViewModel; no escribe en persistencia directamente.

### 2. Orquestación (ViewModel)

- `MonkeyViewModel` (`AndroidViewModel`) es el único controlador de dominio en UI.
- Expone `MonkeyUiState`, `ChestRewardUiState` y efectos (`MonkeyUiEffect`) para AdMob.
- Traduce acciones de usuario (toggle de tarea, CRUD, compra, limpieza de polvo) en llamadas a `MonkeyStateManager` y en side-effects (widget, notificaciones, sonidos).

### 3. Dominio y persistencia (`data/`)

- `MonkeyStateManager` concentra reglas de negocio y almacenamiento:
  - tareas y completado diario
  - reset al cambiar de día
  - racha, bananas, cofre y duplicado de recompensa
  - accesorios de tienda
  - motas de polvo
  - flags de onboarding / tour
- Serialización de listas con Gson; flags y contadores en claves de SharedPreferences (`monkey_prefs`).

### 4. Infraestructura satélite

| Módulo | Rol |
|---|---|
| `notifications/` | Alarmas locales (diario 20:00, por tarea, celebración); `BOOT_COMPLETED` reprograma |
| `widget/` | Lectura del mismo estado local; refresh por Glance y por `AlarmManager` horario |
| `ads/` | Anuncios rewarded para duplicar bananas del cofre |
| `audio/` | Feedback inmediato sin acoplarse a la lógica de negocio |

### 5. Entrada de aplicación

- `MainActivity`: splash nativo, edge-to-edge, inicialización de AdMob, canales y permisos de notificación, grafo de navegación Compose.

---

## Flujo de Datos

Flujo principal: marcar una tarea como completada.

1. El usuario toca una tarea en `MainScreen`.
2. La UI invoca `MonkeyViewModel.toggleTask(taskId)`.
3. El ViewModel consulta y muta el estado vía `MonkeyStateManager.toggleTask`.
4. `MonkeyStateManager` invierte el flag `done_<taskId>`, evalúa si todas las tareas de hoy están hechas y, si corresponde, incrementa racha, genera loot del cofre y actualiza bananas.
5. El ViewModel reconstruye `MonkeyUiState` (`refreshState`), opcionalmente emite `StreakCelebration` y reproduce sonido.
6. Se actualiza el widget (`MonkeyWidgetReceiver.updateWidget`) para reflejar mono y racha.
7. Si el día quedó completo, se muestra notificación de celebración (`NotificationHelper`).
8. Compose recompone: nueva lista de tareas, imagen del mono, contadores y, si aplica, flujo del cofre / anuncio rewarded.

Flujo de arranque (reset diario):

1. Al crear el ViewModel se llama `checkAndResetForNewDay()`.
2. Si la fecha guardada no es hoy, se evalúa el día anterior (completado o fallido), se ajustan racha / `missedDays` / `streakBroken` y se limpian flags diarios de tareas.
3. Se sincronizan motas de polvo y se publica el estado inicial a la UI.

Flujo de notificación local:

1. `AlarmManager` dispara un PendingIntent.
2. `NotificationReceiver` recibe el broadcast.
3. `NotificationHelper` publica la notificación en el canal correspondiente.
4. Al tocar la notificación se abre `MainActivity` (sin deep link a una tarea).

---

## Decisiones Clave y Convenciones

### Estado

- Un solo origen de verdad persistente: `MonkeyStateManager` sobre SharedPreferences.
- La UI no guarda estado de dominio; solo refleja `StateFlow` derivado.
- El aspecto del mono no se persiste como imagen: se **calcula** en tiempo real (`isCleanToday`, racha rota, días perdidos, accesorio equipado).
- Side-effects one-shot (mostrar anuncio) usan `SharedFlow`, no estado durable.

### Persistencia y alcance offline

- Sin backend, sin cuentas, sin sincronización en la nube.
- Las tareas se serializan a JSON; el completado diario usa claves booleanas por `taskId` que se reinician cada día.
- Migraciones puntuales viven en el init del manager (por ejemplo, limpieza del accesorio `gold` obsoleto).

### Errores y robustez

- Lectura JSON defensiva: fallo de parseo devuelve lista vacía.
- Programación de alarmas y creación de canales envueltas en `try/catch` para no crashear en dispositivos restrictivos.
- Fallo de AdMob degrada el flujo del cofre (`AdUnavailable`) sin bloquear la recompensa base.

### Patrones utilizados

- **MVVM** con Compose.
- **Unidirectional data flow**: acción UI → ViewModel → StateManager → nuevo estado → UI.
- **Offline-first / local-only**.
- **Derived state** para imagen del mono y limpieza del día.
- **BroadcastReceivers** para widget, reboot y alarmas.
- Navegación por rutas string en `NavHost` (`onboarding`, `main`, `task_new`, `task_edit/{taskId}`, `shop`).

### Convenciones de dominio

- Días de la semana en ISO: `1 = lunes` … `7 = domingo` (`Task.scheduledDays`).
- Día de descanso (cero tareas programadas hoy) cuenta como limpio.
- Sin tareas creadas, el mono permanece sucio (incentivo a configurar hábitos).
- Accesorios de tienda solo se reflejan en la imagen cuando el mono está limpio.
- Documentación de reglas de negocio detallada en `docs/` (estado del mono, widget, racha/bananas, motas, notificaciones).

### Monetización

- AdMob rewarded opcional al abrir el cofre del día.
- IDs de prueba en builds debug; ID de producción en release.
- La recompensa duplicada solo se aplica si el callback `onUserEarnedReward` confirma la visualización.
)
