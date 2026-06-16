package com.josem.monopulcro.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.josem.monopulcro.R
import com.josem.monopulcro.data.MonkeyStateManager
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val WaveColor = Color(0xFF7DD3FC)

@Composable
fun MainScreen(vm: MonkeyViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    var showCelebration by remember { mutableStateOf(false) }

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
                    StreakCounter(streak = state.streak)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Imagen del mono ────────────────────────────────────────────
                Image(
                    painter = painterResource(
                        id = resolveMonkeyImage(state.isCleanToday, state.hasGlasses, state.streakBroken, state.missedDaysCount)
                    ),
                    contentDescription = "Mono Pulcro",
                    modifier = Modifier.size(220.dp)
                )

                Text(
                    text = when {
                        state.isCleanToday && state.streak > 0 -> "¡${state.streak} días limpio! 🎉"
                        state.isCleanToday -> "¡Limpio por hoy!"
                        else -> "Hay tareas pendientes..."
                    },
                    fontSize = 15.sp,
                    color = if (state.isCleanToday) Color(0xFF16A34A) else Color(0xFF92400E),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(28.dp))

                // ── Lista de tareas ────────────────────────────────────────────
                Text(
                    text = "Tareas del día",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    color = Color(0xFF1E293B)
                )

                MonkeyStateManager.TASKS.forEachIndexed { index, taskName ->
                    TaskCheckboxRow(
                        taskName = taskName,
                        isChecked = state.taskStates.getOrElse(index) { false },
                        onCheckedChange = { vm.toggleTask(index) }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                GlassesSection(
                    hasGlasses = state.hasGlasses,
                    bananas = state.bananas,
                    onBuyGlasses = { vm.buyGlasses() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ── DEBUG (quitar antes de publicar) ──────────────────────────
                DebugPanel(vm)

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // ── Celebración pantalla completa ──────────────────────────────────────
        if (showCelebration) {
            FireCelebrationOverlay(onFinished = { showCelebration = false })
        }
    }
}

// ─── Datos de cada partícula de fuego ────────────────────────────────────────

private data class FireParticle(
    val xFrac: Float,       // posición horizontal (0..1 del ancho de pantalla)
    val sizeDp: Float,      // tamaño en dp
    val delayMs: Long,      // retraso de inicio
    val durationMs: Int     // duración del viaje hacia arriba
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

// ─── Overlay de celebración ───────────────────────────────────────────────────

@Composable
private fun FireCelebrationOverlay(onFinished: () -> Unit) {
    val overlayAlpha  = remember { Animatable(0f) }
    // Cada partícula tiene su propio viaje (en dp, negativo = hacia arriba)
    val travels = remember { FIRE_PARTICLES.map { Animatable(0f) } }
    val alphas  = remember { FIRE_PARTICLES.map { Animatable(0f) } }
    val plusOneY     = remember { Animatable(0f) }
    val plusOneAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        overlayAlpha.animateTo(1f, tween(150))

        // Todas las partículas suben en paralelo con distintos delays
        coroutineScope {
            FIRE_PARTICLES.forEachIndexed { i, p ->
                launch {
                    delay(p.delayMs)
                    alphas[i].animateTo(1f, tween(80))
                    travels[i].animateTo(
                        targetValue = -720f,   // sube ~720dp
                        animationSpec = tween(p.durationMs, easing = EaseOut)
                    )
                    alphas[i].animateTo(0f, tween(220))
                }
            }
            // +1 sube desde el centro inferior
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
            // Centra cada fuego en su posición X
            val xOffset = widthDp * p.xFrac - sizeDp / 2

            Image(
                painter = painterResource(R.drawable.fuego),
                contentDescription = null,
                modifier = Modifier
                    .size(sizeDp)
                    .align(Alignment.BottomStart)
                    .offset(x = xOffset)
                    .graphicsLayer {
                        // travels en dp → convertir a px para graphicsLayer
                        translationY = with(density) { travels[i].value.dp.toPx() }
                        alpha = alphas[i].value
                    }
            )
        }

        // Indicadores +1 (fuego y banana) flotando hacia arriba juntos
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
            // Banana +1
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.banana),
                    contentDescription = null,
                    modifier = Modifier.size(60.dp)
                )
                Text(
                    text = "+1",
                    fontSize = 62.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            // Fuego +1
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.fuego),
                    contentDescription = null,
                    modifier = Modifier.size(70.dp)
                )
                Text(
                    text = "+1",
                    fontSize = 62.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
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
private fun TaskCheckboxRow(
    taskName: String,
    isChecked: Boolean,
    onCheckedChange: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                color = if (isChecked) Color(0xFFDCFCE7) else Color(0xFFF8FAFC),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = { onCheckedChange() },
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF16A34A),
                uncheckedColor = Color(0xFFCBD5E1)
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = taskName,
            fontSize = 16.sp,
            color = if (isChecked) Color(0xFF15803D) else Color(0xFF334155),
            fontWeight = if (isChecked) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun GlassesSection(
    hasGlasses: Boolean,
    bananas: Int,
    onBuyGlasses: () -> Unit
) {
    if (hasGlasses) {
        Text(
            text = "😎 ¡El mono ya tiene sus lentes!",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF7C3AED),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Compra los lentes por 🍌 10 bananas",
                fontSize = 14.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onBuyGlasses,
                enabled = bananas >= 10,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C3AED),
                    disabledContainerColor = Color(0xFFE2E8F0)
                )
            ) {
                Text(
                    text = if (bananas >= 10) "😎 Comprar lentes" else "🍌 $bananas / 10 bananas",
                    fontSize = 15.sp,
                    color = if (bananas >= 10) Color.White else Color(0xFF94A3B8)
                )
            }
        }
    }
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
        Text(
            text = "⚙️ DEBUG",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF856404)
        )
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

// ─── Helper imagen ────────────────────────────────────────────────────────────

private fun resolveMonkeyImage(
    isClean: Boolean,
    hasGlasses: Boolean,
    streakBroken: Boolean = false,
    missedDays: Int = 0
): Int = when {
    isClean && hasGlasses  -> R.drawable.mono_cool
    isClean                -> R.drawable.mono_pulcro
    missedDays >= 3        -> R.drawable.mono_sucio_3   // 3+ días sin limpiar
    streakBroken           -> R.drawable.mono_sucio_2   // racha rota (1-2 días)
    hasGlasses             -> R.drawable.mono_sucio_2   // lentes pero sin limpiar
    else                   -> R.drawable.mono_sucio_1
}
