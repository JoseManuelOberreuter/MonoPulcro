package com.josem.monopulcro.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class MainTourStep {
    BANANAS,
    SHOP,
    STREAK,
    MONKEY,
    ADD_TASK,
    VIEW_MODE,
    TASKS,
}

data class MainTourStepContent(
    val title: String,
    val description: String,
)

val MAIN_TOUR_STEPS = listOf(
    MainTourStepContent(
        title = "Tus bananas",
        description = "Gánalas completando todas tus tareas del día."
    ),
    MainTourStepContent(
        title = "La tienda",
        description = "Compra accesorios para tu mono con tus bananas."
    ),
    MainTourStepContent(
        title = "Tu racha",
        description = "Mantén el hábito completando tareas día a día."
    ),
    MainTourStepContent(
        title = "Tu mono",
        description = "Si no lo limpias, se pondrá sucio con el tiempo."
    ),
    MainTourStepContent(
        title = "Más tareas",
        description = "Agrega nuevas tareas desde aquí."
    ),
    MainTourStepContent(
        title = "Vista semanal",
        description = "Cambia entre la lista de hoy y el calendario de la semana para ver qué toca cada día."
    ),
    MainTourStepContent(
        title = "Tareas de hoy",
        description = "Márcalas al completarlas para mantener a tu mono pulcro."
    ),
)

@Composable
fun MainScreenTourOverlay(
    stepIndex: Int,
    targetBounds: Rect?,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val step = MAIN_TOUR_STEPS.getOrNull(stepIndex) ?: return
    val isLastStep = stepIndex == MAIN_TOUR_STEPS.lastIndex
    val density = LocalDensity.current
    val paddingPx = with(density) { 10.dp.toPx() }
    val cornerPx = with(density) { 16.dp.toPx() }

    val transition = rememberInfiniteTransition(label = "tourPulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tourPulseScale"
    )
    val ringAlpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tourRingAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { /* bloquea taps en el scrim */ }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (targetBounds != null) {
                val hole = Rect(
                    left = targetBounds.left - paddingPx,
                    top = targetBounds.top - paddingPx,
                    right = targetBounds.right + paddingPx,
                    bottom = targetBounds.bottom + paddingPx,
                )
                val center = hole.center
                val scaledHole = Rect(
                    left = center.x - (hole.width / 2f) * pulse,
                    top = center.y - (hole.height / 2f) * pulse,
                    right = center.x + (hole.width / 2f) * pulse,
                    bottom = center.y + (hole.height / 2f) * pulse,
                )
                val spotlightPath = Path().apply {
                    fillType = PathFillType.EvenOdd
                    addRect(Rect(Offset.Zero, size))
                    addRoundRect(
                        RoundRect(
                            rect = scaledHole,
                            cornerRadius = CornerRadius(cornerPx, cornerPx)
                        )
                    )
                }
                drawPath(spotlightPath, Color.Black.copy(alpha = 0.72f))
                drawRoundRect(
                    color = Color(0xFF0EA5E9).copy(alpha = ringAlpha),
                    topLeft = Offset(scaledHole.left, scaledHole.top),
                    size = scaledHole.size,
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    style = Stroke(width = with(density) { 3.dp.toPx() })
                )
            } else {
                drawRect(Color.Black.copy(alpha = 0.72f))
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = step.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                textAlign = TextAlign.Center
            )
            Text(
                text = step.description,
                fontSize = 15.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Button(
                onClick = { if (isLastStep) onFinish() else onNext() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
            ) {
                Text(
                    text = if (isLastStep) "¡Empezar!" else "Siguiente",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}
