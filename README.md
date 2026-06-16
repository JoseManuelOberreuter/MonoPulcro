# Mono Pulcro 🐒

Aplicación Android nativa para mantener hábitos de limpieza del hogar. Cada día completas tus tareas, el mono se pone contento y ganas bananas. Si no las haces, el mono se ensucia.

## ¿Qué hace?

- **Lista de tareas diarias**: lavar platos, hacer la cama, sacar la basura, limpiar el baño.
- **Racha de días**: se acumula cada día que completas todas las tareas.
- **Bananas como recompensa**: ganas 1 banana por día limpio. Con 10 bananas puedes comprarle lentes al mono.
- **Widget para la pantalla de inicio**: muestra el estado del mono y tu racha sin abrir la app.
- **Reset automático diario**: las tareas se reinician cada día y el sistema detecta si completaste o no el día anterior.

## Pantallas

| Pantalla | Descripción |
|---|---|
| `SplashScreen` | Pantalla de carga inicial |
| `MainScreen` | Pantalla principal con el mono, las tareas y el contador |

## Estructura del proyecto

```
app/src/main/
├── kotlin/com/josem/monopulcro/
│   ├── MainActivity.kt              # Entry point
│   ├── data/
│   │   └── MonkeyStateManager.kt    # Lógica de estado (SharedPreferences)
│   ├── ui/
│   │   ├── MainScreen.kt            # UI principal (Jetpack Compose)
│   │   ├── MonkeyViewModel.kt       # ViewModel
│   │   ├── SplashScreen.kt          # Splash
│   │   └── theme/Theme.kt           # Tema visual
│   └── widget/
│       ├── MonkeyWidget.kt          # Widget de pantalla de inicio
│       └── MonkeyWidgetReceiver.kt  # Receptor de eventos del widget
└── res/
    ├── drawable/                    # Assets del mono y splash
    └── xml/monkey_widget_info.xml   # Configuración del widget
```

## Stack técnico

- **Lenguaje**: Kotlin
- **UI**: Jetpack Compose
- **Estado**: ViewModel + StateFlow
- **Persistencia**: SharedPreferences
- **Widget**: Glance (Jetpack)
- **Min SDK**: Android 8.0 (API 26)

## Cómo correr el proyecto

1. Clona el repositorio:
   ```bash
   git clone https://github.com/JoseManuelOberreuter/MonoPulcro.git
   ```
2. Abre el proyecto en Android Studio.
3. Sincroniza Gradle.
4. Ejecuta en un emulador o dispositivo físico (Android 8.0+).

## Assets

Las imágenes del mono están en `/img` y en `app/src/main/res/drawable/`:

- `mono_pulcro` — mono limpio y feliz
- `mono_cool` — mono con lentes (desbloqueable)
- `mono_sucio_1/2/3` — estados de suciedad progresiva
- `banana` / `fuego` — íconos de recompensa y racha
