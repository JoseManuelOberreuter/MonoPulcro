package com.josem.monopulcro.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.josem.monopulcro.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val WaveColor = Color(0xFF7DD3FC)

@Composable
fun MainScreen(
    onAddTask: () -> Unit,
    onEditTask: (taskId: String) -> Unit,
    onOpenShop: () -> Unit,
    vm: MonkeyViewModel = viewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var showCelebration by remember { mutableStateOf(false) }

    // Refresca el estado al volver a esta pantalla (ej. después de editar/borrar una tarea)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.justEarnedBanana) {
        if (state.justEarnedBanana) {
            showCelebration = true
            vm.consumeBananaEvent()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) { drawBottomWaves(WaveColor) }

        Scaffold(containerColor = Color.Transparent) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Encabezado ─────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BananaCounter(count = state.bananas)
                    ShopButton(onClick = onOpenShop)
                    StreakCounter(streak = state.streak)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Imagen del mono ────────────────────────────────────────────
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(240.dp)
                ) {
                    Canvas(modifier = Modifier.size(230.dp)) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colorStops = arrayOf(
                                    0.0f to Color(0x40000000),
                                    0.45f to Color(0x22000000),
                                    0.75f to Color(0x0C000000),
                                    1.0f to Color.Transparent
                                ),
                                center = center,
                                radius = size.minDimension / 2
                            )
                        )
                    }
                    Image(
                        painter = painterResource(
                            id = resolveMonkeyImage(
                                state.isCleanToday,
                                state.equippedAccessory,
                                state.streakBroken,
                                state.missedDaysCount
                            )
                        ),
                        contentDescription = "Mono Pulcro",
                        modifier = Modifier.size(220.dp)
                    )
                }

                Text(
                    text = when {
                        state.allTasks.isEmpty()                       -> "¡Agrega tareas para empezar!"
                        state.todayTasks.isEmpty()                     -> "¡Hoy es día de descanso! 😎"
                        state.isCleanToday && state.streak > 0         -> "${state.streak} días pulcro!"
                        state.isCleanToday                             -> "Pulcro por hoy!"
                        else                                           -> "Hay tareas pendientes..."
                    },
                    fontSize = 15.sp,
                    color = when {
                        state.allTasks.isEmpty()   -> Color(0xFF7C3AED)
                        state.todayTasks.isEmpty() -> Color(0xFF0369A1)
                        state.isCleanToday         -> Color(0xFF16A34A)
                        else                       -> Color(0xFF92400E)
                    },
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(28.dp))

                // ── Sección tareas ─────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tareas de hoy",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(WaveColor, RoundedCornerShape(10.dp))
                            .clickable { onAddTask() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Agregar tarea",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Lista dinámica de tareas de hoy
                if (state.allTasks.isEmpty()) {
                    EmptyTasksHint(onAddTask)
                } else if (state.todayTasks.isEmpty()) {
                    RestDayCard()
                } else {
                    state.todayTasks.forEach { taskState ->
                        TaskCheckboxRow(
                            taskName   = taskState.task.name,
                            isChecked  = taskState.isCompleted,
                            onChecked  = { vm.toggleTask(taskState.task.id) },
                            onEdit     = { onEditTask(taskState.task.id) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                DebugPanel(vm)

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (showCelebration) {
            FireCelebrationOverlay(onFinished = { showCelebration = false })
        }
    }
}

// ─── Estados vacíos ───────────────────────────────────────────────────────────

@Composable
private fun EmptyTasksHint(onAddTask: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0F9FF), RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("🐒", fontSize = 36.sp)
        Text(
            text = "Aún no tienes tareas. ¡Agrega la primera!",
            fontSize = 14.sp,
            color = Color(0xFF0369A1),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
        TextButton(onClick = onAddTask) {
            Text("+ Agregar tarea", color = Color(0xFF0EA5E9), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RestDayCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0FDF4), RoundedCornerShape(16.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No hay tareas programadas para hoy. ¡Disfruta tu descanso! 🎉",
            fontSize = 14.sp,
            color = Color(0xFF16A34A),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

// ─── Fila de tarea ────────────────────────────────────────────────────────────

@Composable
private fun TaskCheckboxRow(
    taskName: String,
    isChecked: Boolean,
    onChecked: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                color = if (isChecked) Color(0xFFDCFCE7) else Color(0xFFF8FAFC),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked  = isChecked,
            onCheckedChange = { onChecked() },
            colors = CheckboxDefaults.colors(
                checkedColor   = Color(0xFF16A34A),
                uncheckedColor = Color(0xFFCBD5E1)
            )
        )
        Text(
            text = taskName,
            fontSize = 16.sp,
            color  = if (isChecked) Color(0xFF15803D) else Color(0xFF334155),
            fontWeight = if (isChecked) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onEdit,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Editar",
                tint = Color(0xFFCBD5E1),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ─── Partículas de fuego ──────────────────────────────────────────────────────

private data class FireParticle(
    val xFrac: Float,
    val sizeDp: Float,
    val delayMs: Long,
    val durationMs: Int
)

private val FIRE_PARTICLES = listOf(
    FireParticle(xFrac = 0.05f, sizeDp = 90f,  delayMs = 0L,   durationMs = 950),
    FireParticle(xFrac = 0.20f, sizeDp = 115f, delayMs = 110L, durationMs = 1050),
    FireParticle(xFrac = 0.35f, sizeDp = 135f, delayMs = 55L,  durationMs = 1000),
    FireParticle(xFrac = 0.52f, sizeDp = 105f, delayMs = 190L, durationMs = 920),
    FireParticle(xFrac = 0.66f, sizeDp = 125f, delayMs = 75L,  durationMs = 1080),
    FireParticle(xFrac = 0.80f, sizeDp = 98f,  delayMs = 145L, durationMs = 960),
    FireParticle(xFrac = 0.92f, sizeDp = 110f, delayMs = 220L, durationMs = 1020),
)

@Composable
private fun FireCelebrationOverlay(onFinished: () -> Unit) {
    val overlayAlpha  = remember { Animatable(0f) }
    val travels = remember { FIRE_PARTICLES.map { Animatable(0f) } }
    val alphas  = remember { FIRE_PARTICLES.map { Animatable(0f) } }
    val plusOneY     = remember { Animatable(0f) }
    val plusOneAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        overlayAlpha.animateTo(1f, tween(150))

        coroutineScope {
            FIRE_PARTICLES.forEachIndexed { i, p ->
                launch {
                    delay(p.delayMs)
                    alphas[i].animateTo(1f, tween(80))
                    travels[i].animateTo(-720f, tween(p.durationMs, easing = EaseOut))
                    alphas[i].animateTo(0f, tween(220))
                }
            }
            launch {
                delay(160L)
                plusOneAlpha.animateTo(1f, tween(140))
                plusOneY.animateTo(-280f, tween(1050, easing = EaseOut))
            }
        }

        delay(150)
        overlayAlpha.animateTo(0f, tween(350))
        onFinished()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = overlayAlpha.value }
            .background(Color.Black.copy(alpha = 0.18f))
    ) {
        val density = LocalDensity.current
        val widthDp = with(density) { constraints.maxWidth.toDp() }

        FIRE_PARTICLES.forEachIndexed { i, p ->
            val sizeDp = p.sizeDp.dp
            val xOffset = widthDp * p.xFrac - sizeDp / 2
            Image(
                painter = painterResource(R.drawable.fuego),
                contentDescription = null,
                modifier = Modifier
                    .size(sizeDp)
                    .align(Alignment.BottomStart)
                    .offset(x = xOffset)
                    .graphicsLayer {
                        translationY = with(density) { travels[i].value.dp.toPx() }
                        alpha = alphas[i].value
                    }
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
                .graphicsLayer {
                    translationY = with(density) { plusOneY.value.dp.toPx() }
                    alpha = plusOneAlpha.value
                }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.banana),
                    contentDescription = null,
                    modifier = Modifier.size(60.dp)
                )
                Text("+1", fontSize = 62.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.fuego),
                    contentDescription = null,
                    modifier = Modifier.size(70.dp)
                )
                Text("+1", fontSize = 62.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }
    }
}

// ─── Sub-composables ──────────────────────────────────────────────────────────

@Composable
private fun BananaCounter(count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.banana),
            contentDescription = "Bananas",
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$count",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFEA580C)
        )
    }
}

@Composable
private fun StreakCounter(streak: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.fuego),
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$streak",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFEA580C)
        )
    }
}

@Composable
private fun ShopButton(onClick: () -> Unit) {
    Image(
        painter = painterResource(R.drawable.cara_mono),
        contentDescription = "Tienda",
        modifier = Modifier
            .size(40.dp)
            .clickable { onClick() }
    )
}

// ─── DEBUG ────────────────────────────────────────────────────────────────────

@Composable
private fun DebugPanel(vm: MonkeyViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF3CD), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("⚙️ DEBUG", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF856404))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DebugButton("😴 Día perdido", Color(0xFFDC3545)) { vm.debugMissedDay() }
            DebugButton("✅ Día ganado",  Color(0xFF198754)) { vm.debugCompletedDay() }
            DebugButton("🗑 Reset",        Color(0xFF6C757D)) { vm.debugReset() }
        }
    }
}

@Composable
private fun DebugButton(label: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(label, fontSize = 11.sp, color = Color.White)
    }
}

// ─── Ondas decorativas ────────────────────────────────────────────────────────

private fun DrawScope.drawBottomWaves(color: Color) {
    val w = size.width
    val h = size.height

    drawPath(
        path = Path().apply {
            moveTo(0f, h * 0.80f)
            cubicTo(w * 0.20f, h * 0.74f, w * 0.55f, h * 0.84f, w * 0.80f, h * 0.77f)
            cubicTo(w * 0.90f, h * 0.74f, w * 0.95f, h * 0.78f, w, h * 0.76f)
            lineTo(w, h); lineTo(0f, h); close()
        },
        color = color.copy(alpha = 0.10f)
    )
    drawPath(
        path = Path().apply {
            moveTo(0f, h * 0.87f)
            cubicTo(w * 0.25f, h * 0.81f, w * 0.60f, h * 0.91f, w * 0.85f, h * 0.84f)
            cubicTo(w * 0.93f, h * 0.81f, w * 0.97f, h * 0.86f, w, h * 0.84f)
            lineTo(w, h); lineTo(0f, h); close()
        },
        color = color.copy(alpha = 0.20f)
    )
    drawPath(
        path = Path().apply {
            moveTo(0f, h * 0.93f)
            cubicTo(w * 0.30f, h * 0.87f, w * 0.65f, h * 0.96f, w * 0.88f, h * 0.90f)
            cubicTo(w * 0.94f, h * 0.88f, w * 0.97f, h * 0.92f, w, h * 0.90f)
            lineTo(w, h); lineTo(0f, h); close()
        },
        color = color.copy(alpha = 0.35f)
    )
}

// ─── Helper imagen del mono ───────────────────────────────────────────────────

private fun resolveMonkeyImage(
    isClean: Boolean,
    equippedAccessory: String?,
    streakBroken: Boolean = false,
    missedDays: Int = 0
): Int = when {
    isClean && equippedAccessory == "glasses"   -> R.drawable.mono_cool
    isClean && equippedAccessory == "hat"       -> R.drawable.mono_gorro
    isClean && equippedAccessory == "crown"     -> R.drawable.mono_corona
    isClean && equippedAccessory == "astronaut" -> R.drawable.mono_astronauta
    isClean                                     -> R.drawable.mono_pulcro
    missedDays >= 3                             -> R.drawable.mono_sucio_3
    streakBroken                                -> R.drawable.mono_sucio_2
    else                                        -> R.drawable.mono_sucio_1
}
