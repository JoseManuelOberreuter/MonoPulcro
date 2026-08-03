# Mono Pulcro 🐒

Aplicación Android nativa para mantener hábitos de limpieza del hogar. Cada día completas tus tareas, el mono se pone contento y ganas bananas. Si no las haces, el mono se ensucia.

**Versión actual:** 1.2.5 (versionCode 21)

## ¿Qué hace?

- **Lista de tareas diarias** con días programados y vista semanal.
- **Racha de días** consecutivos al completar todas las tareas del día.
- **Bananas** como moneda: loot del cofre al completar el día, motas de polvo y anuncios rewarded.
- **Escudos de Pulcritud** que protegen la racha si fallas un día.
- **Tienda**: accesorios cosméticos, escudos y cofre por anuncio (máx. 3/día).
- **Widget** de pantalla de inicio con el estado del mono y la racha.
- **Notificaciones locales**: recordatorios de hábito (mañana/tarde/noche), por tarea y celebración.
- **Reset automático diario** (incluye huecos de varios días sin abrir la app).

## Pantallas

| Pantalla | Descripción |
|---|---|
| `SplashScreen` | Carga inicial + jingle |
| `OnboardingScreen` | Primeras veces (hábitos, bananas, racha, polvo, tienda) |
| `MainScreen` | Mono, tareas (hoy/semana), racha, bananas, overlays |
| `TaskEditScreen` | Crear / editar tarea + notificación por tarea |
| `ShopScreen` | Atuendos y objetos (escudo, cofre rewarded) |

## Estructura del proyecto

```
page/                                # Sitio público (GitHub Pages)
├── index.html                       # Landing
├── privacidad.html                  # Política de privacidad
└── assets/                          # Imágenes del sitio

docs/                                # Documentación técnica (ver docs/INDEX.md)
├── INDEX.md
├── ARCHITECTURE.md
├── puntos_de_mejora.md
└── … (dominio, tienda, persistencia, etc.)

app/src/main/
├── kotlin/com/josem/monopulcro/
│   ├── MainActivity.kt              # Entry point, NavHost, permisos
│   ├── ads/                         # Rewarded + Native AdMob
│   ├── audio/                       # SoundManager
│   ├── data/                        # Task, DustMote, MonkeyStateManager
│   ├── notifications/               # Canales, schedulers, receiver
│   ├── ui/                          # Compose screens, ViewModel, theme
│   └── widget/                      # Glance widget + refresh horario
└── res/
    ├── drawable/                    # Assets del mono, cofre, polvo, etc.
    └── xml/monkey_widget_info.xml
```

## Stack técnico

| Área | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Estado | ViewModel + StateFlow / SharedFlow |
| Persistencia | SharedPreferences + Gson |
| Widget | Glance AppWidget |
| Notificaciones | AlarmManager + NotificationCompat |
| Ads | Google Mobile Ads (rewarded + native) |
| Min / target SDK | 26 / 36 |

Documentación de arquitectura: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md). Índice completo: [`docs/INDEX.md`](docs/INDEX.md).

## Cómo correr el proyecto

1. Clona el repositorio:
   ```bash
   git clone https://github.com/JoseManuelOberreuter/MonoPulcro.git
   ```
2. Abre el proyecto en Android Studio.
3. Sincroniza Gradle.
4. Ejecuta en un emulador o dispositivo físico (Android 8.0+).

## Assets del mono

Imágenes en `app/src/main/res/drawable/` (y referencias en `/img`):

- `mono_pulcro_1/2/3` — limpio (variante cada 3 h)
- Accesorios: lentes, gorro, chaleco, corona, payaso, vikingo, astronauta, mago, lazo
- `mono_sucio_1/2/3` + estados extremos (cansado, enfermo, frustrado, llorando)
- `banana`, `fuego`, `mota_polvo`, `cofre_cerrado` / `cofre_abierto`, `escudo_pulcritud`

## Documentación

Empieza por [`docs/INDEX.md`](docs/INDEX.md). Para backlog de producto y técnica: [`docs/puntos_de_mejora.md`](docs/puntos_de_mejora.md) y [`docs/todo.md`](docs/todo.md).
