# Feedback y roadmap de gamificación — Mono Pulcro

Mono Pulcro depende casi enteramente de la gamificación para retener usuarios
(no hay red social, no hay contenido editorial que renueve el interés; es un
hábito diario sostenido por el mono/bananas/racha). Este doc resume un
análisis del sistema actual y propone ideas nuevas, priorizadas por impacto de
retención vs. esfuerzo, reusando la infraestructura ya existente en el código.


1. QUÉ HAY HOY (RESUMEN)
-------------------------
- **Racha + bananas + cofre diario**: completar todas las tareas del día da un
  cofre (1-3 bananas, 4-6 en hitos ×7), con reversión exacta si se destilda.
- **Escudos de Pulcritud**: hasta 3, se otorgan en hitos de racha
  (7/30/60/90/180/365) y absorben días perdidos antes de romper la racha.
- **Motas de polvo**: mecánica pasiva de "decay" (1 cada 2h, máx 5) que se
  limpia tocando al mono, +1 banana por mota.
- **Tienda**: 12 accesorios cosméticos (10-120 bananas), sin categorías ni
  desbloqueos por logro — todo es "comprar con banana".
- **Monetización**: ads recompensados (triplicar cofre, cofre de tienda 3x/día)
  + 3 tiers de IAP. Patrones anti-exploit sólidos (idempotencia por token,
  reservas antes de mostrar el ad, caps diarios).
- **Notificaciones**: solo recordatorios de tareas/racha rota — ningún evento
  de gamificación (logro, escudo ganado, hito) dispara una notificación hoy.
- **Widget**: muestra mono, racha y motas — no bananas, escudos ni tienda.


2. FEEDBACK HONESTO
--------------------

**Fortalezas:**
- El loop núcleo (tarea → mono limpio → banana → racha) es simple y correcto;
  los escudos son una idea de anti-frustración bien resuelta (evitan castigar
  un solo mal día).
- La infraestructura técnica es más rica de lo que se ve en pantalla: hay
  patrones reutilizables (overlays de celebración parametrizados, reservas de
  ad, migraciones de accesorios, resolución de imagen por estado) que bajan
  mucho el costo de las ideas nuevas.

**Debilidades / riesgos de retención:**
1. **Todo-o-nada**: la racha es binaria por día. Un usuario que falla un día
   sin escudos disponibles pierde *toda* la racha — sigue siendo el mayor
   punto de abandono típico en apps de hábitos. Hoy no hay una meta más blanda
   (semanal) que compense esto.
2. **No hay progresión de largo plazo más allá de bananas**: no existen
   logros, niveles ni "mejor racha histórica". Un usuario de 200 días de racha
   tiene la misma sensación de progreso que uno de 20 (aparte del número), y
   si rompe la racha, pierde literalmente toda evidencia de su logro pasado.
3. **La recompensa es monótona**: banana → accesorio cosmético es el único
   destino de la moneda. Sin variedad de objetivos (colección completa,
   desafíos rotativos), el "por qué seguir" se apoya solo en no perder la
   racha, no en tener algo nuevo que perseguir.
4. **Notificaciones subutilizadas**: solo regañan ("te falta", "perdiste la
   racha"). No celebran ni recuerdan micro-logros, que es donde la
   gamificación genera el mayor "pull" para volver a abrir la app.
5. **Cero elemento social/estatus**, ni siquiera asincrónico (compartir un
   logro). Sin backend esto es más limitado, pero hay opciones 100%
   client-side.


3. IDEAS PROPUESTAS (priorizadas por impacto × esfuerzo)
----------------------------------------------------------

**Tier 1 — bajo esfuerzo, alto impacto (implementables ya con lo que existe):**

1. **Racha máxima histórica ("mejor racha")** — persistir `bestStreakCount`,
   actualizarlo cuando `streakCount` lo supere, mostrarlo junto a la racha
   actual (app + widget). Nunca se pierde aunque se rompa la racha activa —
   le da al usuario algo permanente que perseguir. Esfuerzo: trivial (una key
   nueva + una comparación en el punto donde ya se actualiza `streakCount`).
2. **Bonus por colección completa** — si el usuario posee los 12 accesorios,
   otorgar un cofre especial o un accesorio exclusivo/título. Ya existen
   `ACCESSORIES` y el set de comprados; solo falta el chequeo y una
   celebración (reusando `BananaRewardOverlay`).
3. **Notificaciones de "buenas noticias"** — hoy `NotificationScheduler` solo
   tiene familias de recordatorio/culpa. Agregar disparos puntuales cuando se
   gana un escudo por hito o se alcanza una racha redonda (7/30/etc.) para
   traer de vuelta al usuario con un mensaje positivo, no solo de urgencia.

**Tier 2 — esfuerzo medio, impacto alto (mecánica nueva pero acotada):**

4. **Sistema de logros/badges** — ya está en el backlog del propio equipo
   (`todo.md`, `puntos_de_mejora.md` mencionan "Logros de racha de días") pero
   no implementado. Persistir un set de IDs desbloqueados, engancharlo a
   eventos que ya existen (streak, bananas totales acumuladas, cantidad de
   motas limpiadas, accesorios comprados) y celebrar con un overlay nuevo en
   el mismo estilo que `BananaRewardOverlay`. Es la pieza que más "engagement
   de vuelta" suele generar en apps de hábito, porque da micro-metas
   constantes en vez de depender solo de no romper la racha.
5. **Meta semanal ("5 de 7 días")** — barra de progreso secundaria,
   independiente de la racha diaria estricta, que da una recompensa si se
   cumple X de 7 días en la semana. Amortigua el problema #1 del feedback
   (todo-o-nada) sin tocar las reglas de racha existentes. El toggle
   día/semana que ya existe en la UI es un buen punto de apoyo visual.
6. **Historial/calendario de días limpios** — vista tipo heatmap de los
   últimos N días (limpio/sucio/descanso), aprovechando que el estado diario
   ya se puede derivar. Refuerza la sensación de progreso incluso en semanas
   con algún tropiezo, y es una superficie natural para mostrar los logros del
   punto 4.

**Tier 3 — esfuerzo medio-alto, impacto por variedad/re-enganche periódico:**

7. **Desafíos rotativos (diarios/semanales)** — ej. "limpiá al mono 3 veces
   esta semana", "no faltes ningún día". Da objetivos frescos sin depender de
   subir el número de la racha. Puede reusar el patrón de reserva de ads y el
   overlay de recompensa ya existentes.
8. **Niveles/XP del mono** — un contador acumulado (tareas totales
   completadas, no reseteable) que sube de "nivel" y desbloquea variantes
   visuales o títulos narrativos. `MonkeyImageResolver` ya soporta variantes
   de estado por accesorio, así que el patrón de "más variantes visuales
   según progreso" ya tiene precedente técnico.
9. **Eventos estacionales** — accesorios de tiempo limitado (Navidad,
   Halloween) reusando el mecanismo de compra/equipar ya existente, para
   generar picos de re-apertura en fechas puntuales. Costo real está en el
   arte, no en la lógica.
10. **Compartir racha (sin backend)** — generar una imagen del mono + racha
    actual y disparar el share intent nativo de Android. Es una "medalla"
    para el usuario y potencial canal de adquisición orgánica, 100%
    client-side.


4. RELACIONADO
---------------
[`puntos_de_mejora.md`](puntos_de_mejora.md) · [`todo.md`](todo.md) ·
[`racha_y_bananas.md`](racha_y_bananas.md) ·
[`escudos_de_pulcritud.md`](escudos_de_pulcritud.md) ·
[`tienda_y_anuncios.md`](tienda_y_anuncios.md) · [`INDEX.md`](INDEX.md)
